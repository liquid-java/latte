package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.support.compiler.VirtualFile;

class ArraysAndClosuresTest {
  private Verification.Result check(String source) {
    return Verification.verify(
        new VirtualFile(
            "import liquidjava.specification.*;import specification.*;" + source, "Sample.java"));
  }

  private void accepts(String source) {
    var r = check(source);
    assertTrue(r.accepted(), r.diagnostics().toString());
  }

  private void rejects(String source) {
    var r = check(source);
    assertTrue(
        r.diagnostics().stream()
            .anyMatch(
                d ->
                    d.kind() == Diagnostic.Kind.REFINEMENT
                        || d.kind() == Diagnostic.Kind.SHARED_REFINEMENT),
        r.diagnostics().toString());
  }

  @Test
  void arraysRetainElementsAndDefaults() {
    accepts(
        "class Sample{void f(){int[] a=new int[4];@Refinement(\"_==0\")int zero=a[2];a[1]=7;@Refinement(\"_==7\")int n=a[1];@Refinement(\"_==0\")int other=a[3];}}");
  }

  @Test
  void arrayAliasesObserveWrites() {
    accepts(
        "class Sample{void f(){int[] a={2,3};int[] b=a;b[0]++;@Refinement(\"_==3\")int n=a[0];}}");
    rejects(
        "class Sample{void f(){int[] a={2,3};int[] b=a;b[0]=4;@Refinement(\"_==2\")int n=a[0];}}");
  }

  @Test
  void symbolicIndicesUseStoreSelect() {
    accepts(
        "class Sample{void f(@Refinement(\"_>=0&&_<4\")int i){int[] a=new int[4];a[i]=9;@Refinement(\"_==9\")int n=a[i];}}");
  }

  @Test
  void arrayBoundsProduceCatchPaths() {
    accepts(
        "class Sample{void f(){int x=0;try{int[] a=new int[2];x=a[3];}catch(ArrayIndexOutOfBoundsException e){x=5;}@Refinement(\"_==5\")int n=x;}}");
  }

  @Test
  void mutatingArrayCallsDiscardElements() {
    rejects(
        "class Sample{static void set(@Borrowed int[] a){a[0]=3;}void f(){int[] a={2};set(a);@Refinement(\"_==2\")int n=a[0];}}");
  }

  @Test
  void floatArrayValuesKeepIeeeBehavior() {
    accepts("class Sample{void f(){float[] a={Float.NaN};@Refinement(\"_!=_\")float n=a[0];}}");
  }

  @Test
  void lambdaBodyRefinementsAreVerified() {
    accepts(
        "class Sample{void f(){int captured=4;Runnable r=()->{@Refinement(\"_==4\")int n=captured;};}}");
    rejects("class Sample{void f(){Runnable r=()->{@Refinement(\"_>0\")int n=-1;};}}");
  }

  @Test
  void lambdaImplementsItsFunctionalContract() {
    accepts(
        "interface Positive{@Refinement(\"_>0\")int get();}class Sample{void f(){Positive p=()->1;}}");
    rejects(
        "interface Positive{@Refinement(\"_>0\")int get();}class Sample{void f(){Positive p=()->-1;}}");
  }

  @Test
  void methodReferenceCannotStrengthenInputs() {
    rejects(
        "interface Any{int apply(int x);}class Sample{static int positive(@Refinement(\"_>0\")int n){return n;}void f(){Any a=Sample::positive;}}");
  }

  @Test
  void methodReferencePreservesTheResultContract() {
    accepts(
        "interface Positive{@Refinement(\"_>0\")int get();}class Sample{@Refinement(\"_>0\")static int positive(){return 1;}void f(){Positive a=Sample::positive;}}");
    rejects(
        "interface Positive{@Refinement(\"_>0\")int get();}class Sample{@Refinement(\"_>=0\")static int zero(){return 0;}void f(){Positive a=Sample::zero;}}");
  }

  @Test
  void conditionalArrayUpdatesJoinTheirContents() {
    accepts(
        "class Sample{void f(boolean b){int[] a={1};if(b)a[0]=2;else a[0]=3;@Refinement(\"_>=2&&_<=3\")int n=a[0];}}");
  }
}
