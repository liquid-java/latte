package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.support.compiler.VirtualFile;

class LoopsAndEffectsTest {
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
                d -> d.kind() == Diagnostic.Kind.REFINEMENT || d.kind() == Diagnostic.Kind.STATE),
        r.diagnostics().toString());
  }

  @Test
  void infersAnInductiveCounterBound() {
    accepts("class Sample{void f(){int i=0;while(i<10)i++;@Refinement(\"_==10\")int n=i;}}");
  }

  @Test
  void inferenceUsesAllBackEdges() {
    rejects(
        "class Sample{void f(boolean b){int i=0;while(i<10){if(b){i=20;continue;}i++;}@Refinement(\"_==10\")int n=i;}}");
  }

  @Test
  void breakIsNotTheNegationOfTheCondition() {
    rejects(
        "class Sample{void f(boolean b){int i=0;while(i<10){if(b)break;i++;}@Refinement(\"_==10\")int n=i;}}");
  }

  @Test
  void statePreservingLoopCallsKeepTheState() {
    accepts(
        "@StateSet({\"open\",\"closed\"})class R{@StateRefinement(to=\"open(this)\")R(){}@StateRefinement(from=\"open(this)\")void read(){}}class Sample{void f(int n){R r=new R();while(n>0){r.read();n--;}r.read();}}");
  }

  @Test
  void mutationOfArrayAcrossIterationsIsNotForgotten() {
    rejects(
        "class Sample{void f(boolean again){int[] a={0};while(again){@Refinement(\"_==0\")int n=a[0];a[0]=1;}}}");
  }

  @Test
  void pureLocalAssignmentsDoNotInvalidateObjects() {
    accepts(
        "@StateSet({\"open\",\"closed\"})class R{@StateRefinement(to=\"open(this)\")R(){}@StateRefinement(from=\"open(this)\")void read(){}}class Sample{static void pure(@Borrowed R r){int i=1;i++;}void f(){R r=new R();pure(r);r.read();}}");
  }

  @Test
  void onlyTheMutatedArgumentLosesItsState() {
    accepts(
        "@StateSet({\"open\",\"closed\"})class R{@StateRefinement(to=\"open(this)\")R(){}@StateRefinement(to=\"closed(this)\")void close(){}@StateRefinement(from=\"open(this)\")void read(){}}class Sample{static void closeFirst(@Borrowed R a,@Borrowed R b){a.close();}void f(){R a=new R();R b=new R();closeFirst(a,b);b.read();}}");
  }

  @Test
  void indirectMutationStillInvalidatesAliases() {
    rejects(
        "@StateSet({\"open\",\"closed\"})class R{@StateRefinement(to=\"open(this)\")R(){}@StateRefinement(to=\"closed(this)\")void close(){}@StateRefinement(from=\"open(this)\")void read(){}}class Sample{static void close(@Borrowed R a){a.close();}static void delegate(@Borrowed R a){close(a);}void f(){R a=new R();R b=a;delegate(a);b.read();}}");
  }

  @Test
  void fieldInitializersSurviveOtherAllocations() {
    accepts("class Sample{@Refinement(\"_<0\")int port=-1;Object child=new Object();}");
  }

  @Test
  void forwardFieldsHaveTheirJavaDefault() {
    accepts(
        "class Sample{int first=this.second;int second=4;@PrivateRefinement(\"this.first==0&&this.second==4\")Sample(){}}");
  }
}
