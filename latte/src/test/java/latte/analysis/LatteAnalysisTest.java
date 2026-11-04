package latte.analysis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import latte.api.Diagnostic;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.compiler.VirtualFile;

class LatteAnalysisTest {
  record Checked(CtModel model, List<Diagnostic> diagnostics) {
    LatteFacts local(String name) {
      return model.getElements(new TypeFilter<>(CtLocalVariable.class)).stream()
          .filter(v -> v.getSimpleName().equals(name))
          .findFirst()
          .flatMap(LatteFacts::on)
          .orElseThrow();
    }
  }

  static Checked check(String source) {
    var launcher = new Launcher();
    launcher.getEnvironment().setNoClasspath(true);
    launcher.getEnvironment().setComplianceLevel(17);
    launcher.addInputResource(new VirtualFile(source, "Sample.java"));
    var model = launcher.buildModel();
    return new Checked(model, new LatteAnalysis().analyze(model));
  }

  @Test
  void assignmentPreservesIdentityWithoutConsumingTheSource() {
    var checked =
        check(
            "class Sample { void f() { Object a = new Object(); Object b; b = a; Object marker = b; } }");
    assertEquals(List.of(), checked.diagnostics());
    assertEquals(checked.local("a").value(), checked.local("marker").value());
    assertEquals(Permission.FREE, checked.local("marker").permission());
  }

  @Test
  void reassignmentDetachesOnlyThatAlias() {
    var checked =
        check(
            "class Sample { void f() { Object a = new Object(); Object b = a; b = new Object(); Object marker = b; } }");
    assertNotEquals(checked.local("a").value(), checked.local("marker").value());
  }

  @Test
  void unknownCallsEscapeEveryAliasButPreserveEarlierSnapshots() {
    var checked =
        check(
            "class Sample { void f() { java.util.Properties a = new java.util.Properties(); Object b = a; System.setProperties(a); Object marker = b; } }");
    assertEquals(Permission.FREE, checked.local("b").permission());
    assertEquals(Permission.SHARED, checked.local("marker").permission());
  }

  @Test
  void borrowedCannotEscapeThroughUnknownCode() {
    var checked =
        check(
            "import specification.Borrowed; class Sample { void f(@Borrowed Object a) { System.out.println(a); } }");
    assertTrue(
        checked.diagnostics().stream().anyMatch(d -> d.message().contains("Borrowed reference")));
  }

  @Test
  void branchesJoinAllPossibleAliases() {
    var checked =
        check(
            "class Sample { void f(boolean c) { Object a = new Object(); Object b = new Object(); Object x; if(c) { x=a; } else { x=b; } Object marker=x; } }");
    assertEquals(List.of(), checked.diagnostics());
    assertEquals(2, checked.local("marker").value().objects().size());
  }

  @Test
  void nestedIfWithoutBracesDoesNotCrash() {
    var checked =
        check(
            "class Sample { void f(boolean c) { Object a=new Object(); Object b=a; if(c) if(c) b=new Object(); Object marker=b; } }");
    assertEquals(List.of(), checked.diagnostics());
    assertEquals(2, checked.local("marker").value().objects().size());
  }

  @Test
  void loopIncludesZeroIterationsAndIteratedAliases() {
    var checked =
        check(
            "class Sample { void f(boolean c) { Object a=new Object(); Object b=new Object(); while(c) { a=b; } Object marker=a; } }");
    assertEquals(List.of(), checked.diagnostics());
    assertEquals(2, checked.local("marker").value().objects().size());
  }

  @Test
  void nestedFieldsAndPrimitiveArraysHaveFacts() {
    var checked =
        check(
            "import specification.*; class Sample { @Unique Sample next; @Unique int[] data; void f() { Sample n=this.next.next; int[] a=this.data; int marker=a[0]; } }");
    assertEquals(List.of(), checked.diagnostics());
    assertEquals(Permission.UNIQUE, checked.local("n").permission());
    assertEquals(Permission.IMMUTABLE, checked.local("marker").permission());
  }

  @Test
  void reachabilityExploresAllEdgesAndCycles() {
    var root = Reference.object("root");
    var left = Reference.object("left");
    var right = Reference.object("right");
    var state =
        FlowState.empty()
            .write(root, "left", left)
            .write(root, "right", right)
            .write(right, "back", root);
    assertEquals(Set.of("root", "left", "right"), state.reachable(root));
    assertTrue(state.overlaps(root, right));
    assertTrue(state.overlaps(right, root));
  }

  @Test
  void permissionMoveInvalidatesEveryLocalAlias() {
    var checked =
        check(
            "import specification.*; class Sample { void take(@Free Object x) {} void f() { Object a=new Object(); Object b=a; take(a); Object marker=b; } }");
    assertTrue(checked.diagnostics().stream().anyMatch(d -> d.message().contains("consumed")));
  }

  @Test
  void distinctnessUsesPreTransferPermissions() {
    var checked =
        check(
            "import specification.*; class Sample { void take(@Free Object a, @Free Object b) {} void f() { Object x=new Object(); take(x,x); } }");
    assertTrue(checked.diagnostics().stream().anyMatch(d -> d.message().contains("Non-distinct")));
  }

  @Test
  void joinedHeapRetainsUninitializedBranchContents() {
    var checked =
        check(
            "import specification.*; class Sample {@Unique Object child; void f(boolean c){Sample a=new Sample();Sample b=new Sample();a.child=new Object();if(c)b.child=new Object();Sample selected=c?a:b;Object marker=selected.child;}}");
    assertTrue(checked.diagnostics().isEmpty(), checked.diagnostics().toString());
    assertEquals(2, checked.local("marker").value().objects().size());
    assertTrue(checked.local("marker").value().nullable());
  }

  @Test
  void mutatingCalleeInvalidatesCachedFieldContents() {
    var checked =
        check(
            "import specification.*; class Sample {@Unique Object child;void replace(@Free Object x){child=x;}void f(){Object old=child;replace(new Object());Object marker=child;}}");
    assertTrue(checked.diagnostics().isEmpty(), checked.diagnostics().toString());
    assertTrue(
        checked
            .local("marker")
            .value()
            .objects()
            .containsAll(checked.local("old").value().objects()));
    assertTrue(
        checked.local("marker").value().objects().size()
            > checked.local("old").value().objects().size());
  }

  @Test
  void repeatedAllocationsAreNotMistakenForOneFreshObject() {
    var checked = check("class Sample {void f(boolean c){while(c){Object a=new Object();}}}");
    assertTrue(checked.diagnostics().isEmpty(), checked.diagnostics().toString());
    assertEquals(Permission.FREE, checked.local("a").permission());
  }

  @Test
  void allocationRecencyDoesNotUpgradeAnEscapedOlderInstance() {
    var checked =
        check(
            "import specification.*;class Sample{void leak(Object o){}void f(boolean again){Object previous=null;while(again){Object recent=new Object();Object alias=recent;leak(previous);previous=alias;}}}");
    assertTrue(checked.diagnostics().isEmpty(), checked.diagnostics().toString());
    assertEquals(Permission.FREE, checked.local("recent").permission());
    assertEquals(Permission.FREE, checked.local("alias").permission());
    assertTrue(
        checked.local("recent").before().permissions().keySet().stream()
            .anyMatch(id -> id.startsWith("summary:")));
  }

  @Test
  void computedReturnPreservesAllPossibleLocalAliases() {
    var checked =
        check(
            "import specification.*;class Sample{@Borrowed static Object choose(boolean b,@Borrowed Object x,@Borrowed Object y){Object r=b?x:y;return r;}void f(boolean b){Object x=new Object();Object y=new Object();Object marker=choose(b,x,y);}}");
    assertTrue(checked.diagnostics().isEmpty(), checked.diagnostics().toString());
    assertEquals(2, checked.local("marker").value().objects().size());
  }

  @Test
  void recursiveBorrowedIdentityPreservesTheOriginalAlias() {
    var checked =
        check(
            "import specification.*;class Sample{@Borrowed static Object identity(int n,@Borrowed Object x){if(n==0)return x;return identity(n-1,x);}void f(){Object original=new Object();Object marker=identity(4,original);}}");
    assertTrue(checked.diagnostics().isEmpty(), checked.diagnostics().toString());
    assertEquals(checked.local("original").value(), checked.local("marker").value());
  }

  @Test
  void sharedComputedResultsCannotBeUpgradedByABorrowedSignature() {
    var checked =
        check(
            "import specification.*;class Sample{@Borrowed static Object identity(Object x){Object alias=x;return alias;}}");
    assertTrue(
        checked.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.PERMISSION));
  }
}
