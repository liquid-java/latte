package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.support.compiler.VirtualFile;

class ControlFlowTest {
  private Verification.Result check(String source) {
    return Verification.verify(
        new VirtualFile(
            "import liquidjava.specification.*; import specification.*; " + source,
            "Control.java"));
  }

  private void accepts(String source) {
    var result = check(source);
    assertTrue(result.accepted(), result.diagnostics().toString());
  }

  private void rejects(String source) {
    var result = check(source);
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                d -> d.kind() == Diagnostic.Kind.REFINEMENT || d.kind() == Diagnostic.Kind.STATE),
        result.diagnostics().toString());
  }

  @Test
  void finallyRunsOnNormalAndExceptionalPaths() {
    accepts(
        "class Control{void f(boolean b){int x=0;try{if(b)throw new IllegalArgumentException();x=1;}catch(IllegalArgumentException e){x=2;}finally{x=3;}@Refinement(\"_==3\") int result=x;}}");
  }

  @Test
  void overridingFinallyReturnReplacesTheOriginalResult() {
    accepts("class Control{@Refinement(\"_>0\") int f(){try{return -1;}finally{return 2;}}}");
  }

  @Test
  void returnExpressionIsEvaluatedBeforeFinally() {
    accepts("class Control{@Refinement(\"_==1\") int f(){int x=1;try{return x;}finally{x=2;}}}");
  }

  @Test
  void finallyUpdatesAreVisibleToPostconditions() {
    rejects(
        "class Control{int limit;@Refinement(\"_>limit\") int f(){limit=0;try{return 1;}finally{limit=2;}}}");
  }

  @Test
  void nestedHandlersPreserveRethrows() {
    accepts(
        "class Control{void f(){int n=0;try{try{throw new IllegalArgumentException();}catch(IllegalArgumentException e){throw e;}}catch(RuntimeException e){n=2;}@Refinement(\"_==2\") int result=n;}}");
  }

  @Test
  void catchReceivesTheStateAtTheThrow() {
    accepts(
        "class Control{void f(){int x=0;try{x=4;throw new IllegalArgumentException();}catch(RuntimeException e){@Refinement(\"_==4\") int result=x;}}}");
  }

  @Test
  void integerDivisionCreatesAnExceptionalPath() {
    accepts(
        "class Control{void f(){int x=0;try{x=8/0;}catch(ArithmeticException e){x=2;}@Refinement(\"_==2\") int result=x;}}");
  }

  @Test
  void implicitExceptionInCatchRunsFinallyBeforeOuterHandler() {
    accepts(
        "class Control{void f(){int x=0;try{try{throw new IllegalArgumentException();}catch(IllegalArgumentException e){int ignored=1/0;}finally{x=5;}}catch(ArithmeticException e){@Refinement(\"_==5\") int result=x;}}}");
  }

  @Test
  void breakRunsFinallyBeforeLeavingTheLoop() {
    accepts(
        "class Control{void f(){int x=0;while(true){try{x=1;break;}finally{x=2;}}@Refinement(\"_==2\") int result=x;}}");
  }

  @Test
  void labeledBreakContinuesOutsideTheBlock() {
    rejects(
        "class Control{void f(){int x=0;outer:{x=1;break outer;}@Refinement(\"_==0\") int result=x;}}");
  }

  @Test
  void switchesPreserveFallthroughAndDefault() {
    accepts(
        "class Control{void f(){int x=0;switch(1){case 1:x=2;case 2:x+=3;break;default:x=0;}@Refinement(\"_==5\") int result=x;}}");
  }

  @Test
  void switchExpressionsPreserveYieldedValues() {
    accepts(
        "class Control{void f(){int x=switch(2){case 1->1;case 2->{int n=4;yield n+1;}default->0;};@Refinement(\"_==5\") int result=x;}}");
    rejects(
        "class Control{void f(){int x=switch(2){case 1->1;default->0;};@Refinement(\"_>0\") int result=x;}}");
  }

  @Test
  void uncheckedCallExceptionsAreNotAssumedToEstablishTheirNormalPostcondition() {
    rejects(
        "class Control{static void risky(){}void f(){int x=0;try{x=2;unknown.Library.run();x=3;}catch(RuntimeException e){@Refinement(\"_==3\") int result=x;}}}");
  }

  @Test
  void resourcesCloseOnAllExits() {
    rejects(
        "@StateSet({\"open\",\"closed\"}) class Resource implements AutoCloseable{@StateRefinement(to=\"open(this)\") Resource(){}@StateRefinement(from=\"open(this)\",to=\"closed(this)\") public void close(){} @StateRefinement(from=\"open(this)\") void read(){}} class Control{void f(){Resource resource=new Resource();try(resource){resource.read();}resource.read();}}");
  }

  @Test
  void anEscapeOnTheThrowingBranchReachesTheCatch() {
    var result =
        check(
            "class Control{static void leak(Object x){}void f(boolean b){Object x=new Object();try{if(b){leak(x);throw new IllegalStateException();}}catch(RuntimeException e){@Refinement(\"_!=null\")Object alias=x;}}}");
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.SHARED_REFINEMENT),
        result.diagnostics().toString());
  }

  @Test
  void anEscapeOnABreakPathReachesTheLoopExit() {
    var result =
        check(
            "class Control{static void leak(Object x){}void f(boolean b,boolean c){Object x=new Object();while(b){if(c){leak(x);break;}}@Refinement(\"_!=null\")Object alias=x;}}");
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.SHARED_REFINEMENT),
        result.diagnostics().toString());
  }

  @Test
  void nullFieldReadsReachTheCatch() {
    accepts(
        "class Control{int field;void f(){Control c=null;int x=0;try{x=c.field;}catch(NullPointerException e){x=2;}@Refinement(\"_==2\")int n=x;}}");
  }

  @Test
  void nullCallsEvaluateArgumentsBeforeThrowing() {
    accepts(
        "class Control{void target(int n){}void f(){Control c=null;int x=0;try{c.target(x=3);}catch(NullPointerException e){@Refinement(\"_==3\")int n=x;}}}");
  }

  @Test
  void throwingNullThrowsNullPointerException() {
    accepts(
        "class Control{void f(){int x=0;try{throw null;}catch(NullPointerException e){x=2;}@Refinement(\"_==2\")int n=x;}}");
  }

  @Test
  void compoundDivisionByZeroThrows() {
    accepts(
        "class Control{void f(){int x=3;try{x/=0;}catch(ArithmeticException e){x=2;}@Refinement(\"_==2\")int n=x;}}");
  }

  @Test
  void arrayStoresEvaluateTheValueBeforeBoundsChecks() {
    accepts(
        "class Control{void f(){int[] a=null;int x=0;try{a[0]=(x=3);}catch(NullPointerException e){@Refinement(\"_==3\")int n=x;}}}");
  }

  @Test
  void negativeInnerDimensionsThrow() {
    accepts(
        "class Control{void f(){int x=0;try{int[][] a=new int[1][-1];}catch(NegativeArraySizeException e){x=2;}@Refinement(\"_==2\")int n=x;}}");
  }
}
