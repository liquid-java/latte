package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import latte.analysis.*;
import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.compiler.VirtualFile;

class ExternalSummariesTest {
  private static final String ARRAYS =
      """
      @ExternalRefinementsFor("java.util.Arrays") interface ArraysContracts {
        @Modifies("array[*]") void fill(@Borrowed int[] array, int value);
        @Modifies({}) @Free int[] copyOf(@Borrowed int[] array, int newLength);
      }
      """;
  private static final String LIST =
      """
      @ExternalRefinementsFor("java.util.ArrayList") @Ghost("int listSize")
      interface ListContracts<E> {
        @Modifies({}) @StateRefinement(to="listSize(this) == 0") void ArrayList();
        @Modifies("this.*") @StateRefinement(to="listSize(this) == listSize(old(this)) + 1")
        @Refinement("_") boolean add(E value);
        @Modifies({}) @StateRefinement(from="true") @Refinement("_ == listSize(this)") int size();
        @Modifies({}) @ReturnsView(of="this", from="fromIndex", toExclusive="toIndex")
        java.util.List<E> subList(int fromIndex, int toIndex);
      }
      """;

  private Verification.Result check(String source) {
    return Verification.verify(
        new VirtualFile(
            "import java.util.*; import liquidjava.specification.*; import specification.*;\n"
                + source,
            "Sample.java"));
  }

  private Verification.Result accepts(String source) {
    var result = check(source);
    assertTrue(result.accepted(), result.diagnostics().toString());
    return result;
  }

  private void rejects(String source, Diagnostic.Kind kind) {
    var result = check(source);
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == kind),
        result.diagnostics().toString());
    if (kind != Diagnostic.Kind.SYNTAX)
      assertFalse(
          result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.SYNTAX),
          result.diagnostics().toString());
  }

  @Test
  void copyingPreservesInputAndOtherLocalState() {
    accepts(
        ARRAYS
            + """
              class Sample { void run() {
                int[] a = {7}; int[] b = {9}; int[] alias = a;
                int[] copy = Arrays.copyOf(a, 1);
                @Refinement("_ == 7") int x = alias[0];
                @Refinement("_ == 9") int y = b[0];
                copy[0] = 4;
                @Refinement("_ == 7") int z = a[0];
              }}
              """);
  }

  @Test
  void writesInvalidateAliasesButPreserveOtherArrays() {
    accepts(
        ARRAYS
            + """
              class Sample { void run() {
                int[] a = {7}; int[] b = {9}; int[] alias = a;
                Arrays.fill(a, 1);
                @Refinement("_ == 9") int y = b[0];
              }}
              """);
    rejects(
        ARRAYS
            + """
              class Sample { void run() {
                int[] a = {7}; int[] alias = a; Arrays.fill(a, 1);
                @Refinement("_ == 7") int stale = alias[0];
              }}
              """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void omittedEffectsRemainConservative() {
    rejects(
        ARRAYS.replace("@Modifies({}) @Free", "@Free")
            + """
              class Sample { void run() {
                int[] a = {7}; Arrays.copyOf(a, 1);
                @Refinement("_ == 7") int stale = a[0];
              }}
              """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void emptyFootprintsPreserveFactsAcrossObserverLoops() {
    accepts(
        """
@ExternalRefinementsFor("java.lang.String") @Ghost("int size") interface Strings {
  @Modifies({}) @StateRefinement(from="true") @Refinement("_ == size(this) && _ >= 0") int length();
  @Modifies({}) @StateRefinement(from="true") char charAt(@Refinement("_ >= 0 && _ < size(this)") int i);
}
class Sample { boolean run(@Borrowed String a, @Borrowed String b) {
  if (a.length() != b.length()) return false;
  for (int i=0; i<a.length(); i++) if (a.charAt(i) != b.charAt(i)) return false;
  return true;
}}
""");
  }

  @Test
  void viewCreationKeepsLocalControlAndPublishesCaptureMetadata() {
    var result =
        accepts(
            LIST
                + """
                  class Sample { void run() {
                    ArrayList<Integer> list = new ArrayList<>(); list.add(1); list.add(2);
                    List<Integer> view = list.subList(0, 1);
                    @Refinement("_ == 1") int n = view.size();
                    @Refinement("_ == 2") int original = list.size();
                    list.add(3);
                    @Refinement("_ == 3") int changed = list.size();
                  }}
                  """);
    var call =
        result.model().getElements(new TypeFilter<>(CtInvocation.class)).stream()
            .filter(c -> ViewFacts.on(c).isPresent())
            .findFirst()
            .orElseThrow();
    var view = ViewFacts.on(call).orElseThrow();
    var facts = LatteFacts.on(call).orElseThrow();
    assertEquals(view.backing(), facts.after().read(view.view(), ViewFacts.BACKING).orElseThrow());
    assertTrue(facts.refinable());
    assertEquals(
        Set.of(),
        MutationEffects.on(ExternalSpecifications.resolve(call.getExecutable())).roots().keySet());
  }

  @Test
  void viewMutationsUpdateBackingAndLocalAliases() {
    accepts(
        LIST
            + """
              class Sample { void run() {
                ArrayList<Integer> list = new ArrayList<>(); list.add(1);
                List<Integer> view = list.subList(0, 1); List<Integer> alias = view;
                view.add(2);
                @Refinement("_ == 2") int n = alias.size();
                @Refinement("_ == 2") int m = list.size();
                alias.remove(0);
                @Refinement("_ == 1") int k = view.size();
                @Refinement("_ == 1") int l = list.size();
                view.clear();
                @Refinement("_ == 0") int empty = list.size();
              }}
              """);
  }

  @Test
  void nestedViewsUpdateTheirAncestorsAndBacking() {
    accepts(
        LIST
            + """
class Sample { void run() {
  ArrayList<Integer> list = new ArrayList<>(); list.add(1); list.add(2);
  List<Integer> parent = list.subList(0, 2); List<Integer> child = parent.subList(1, 2);
  child.clear();
  @Refinement("_ == 0") int n = child.size();
  @Refinement("_ == 1") int p = parent.size();
  @Refinement("_ == 1") int m = list.size();
}}
""");
  }

  @Test
  void backingStructuralMutationInvalidatesViews() {
    rejects(
        LIST
            + """
              class Sample { void run() {
                ArrayList<Integer> list = new ArrayList<>();
                List<Integer> view = list.subList(0,0); list.add(1); view.size();
              }}
              """,
        Diagnostic.Kind.STATE);
  }

  @Test
  void siblingStructuralMutationInvalidatesOtherViews() {
    rejects(
        LIST
            + """
              class Sample { void run() {
                ArrayList<Integer> list = new ArrayList<>();
                List<Integer> a = list.subList(0,0); List<Integer> b = list.subList(0,0);
                a.add(1); b.size();
              }}
              """,
        Diagnostic.Kind.STATE);
  }

  @Test
  void sharedBackingDoesNotBecomeRefinable() {
    rejects(
        LIST
            + """
              class Sample { void run(ArrayList<Integer> list) {
                List<Integer> view = list.subList(0,0); view.size();
              }}
              """,
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void escapingViewEscapesBackingAndEscapingBackingDisablesViewRefinements() {
    rejects(
        LIST
            + """
class Sample { static Object escaped; void run() {
  ArrayList<Integer> list = new ArrayList<>(); List<Integer> view = list.subList(0,0);
  escaped = view; list.size();
}}
""",
        Diagnostic.Kind.SHARED_REFINEMENT);
    rejects(
        LIST
            + """
class Sample { static Object escaped; void run() {
  ArrayList<Integer> list = new ArrayList<>(); List<Integer> view = list.subList(0,0);
  escaped = list; view.size();
}}
""",
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void borrowedBackingAllowsLocalViewButCannotEscape() {
    accepts(
        LIST
            + """
              class Sample { void run(@Borrowed ArrayList<Integer> list) {
                List<Integer> view = list.subList(0,0); @Refinement("_ == 0") int n = view.size();
              }}
              """);
    rejects(
        LIST
            + """
              class Sample { static Object escaped; void run(@Borrowed ArrayList<Integer> list) {
                escaped = list.subList(0,0);
              }}
              """,
        Diagnostic.Kind.PERMISSION);
  }

  @Test
  void nonstructuralWritesPreserveSiblingViewSizes() {
    accepts(
        LIST
            + """
              class Sample { void run() {
                ArrayList<Integer> list = new ArrayList<>(); list.add(1); list.add(2);
                List<Integer> a = list.subList(0,2); List<Integer> b = list.subList(1,2);
                List<Integer> child = a.subList(0,1);
                a.set(0,3);
                @Refinement("_ == 2") int n = list.size();
                @Refinement("_ == 1") int m = b.size();
                @Refinement("_ == 1") int k = child.size();
              }}
              """);
  }

  @Test
  void branchSelectedViewsPreserveTheirDistinctLengths() {
    accepts(
        LIST
            + """
              class Sample { void run(boolean flag) {
                ArrayList<Integer> list = new ArrayList<>(); list.add(1); list.add(2);
                List<Integer> a = list.subList(0,1); List<Integer> b = list.subList(0,2);
                List<Integer> selected = flag ? a : b;
                @Refinement("_ == (flag ? 1 : 2)") int n = selected.size();
              }}
              """);
    rejects(
        LIST
            + """
              class Sample { void run(boolean flag) {
                ArrayList<Integer> list = new ArrayList<>(); list.add(1);
                List<Integer> view = list.subList(0,1);
                if (flag) list.add(2);
                view.size();
              }}
              """,
        Diagnostic.Kind.STATE);
  }

  @Test
  void invalidatedViewsDoNotBecomeValidAfterNonstructuralWrites() {
    rejects(
        LIST
            + """
              class Sample { void run() {
                ArrayList<Integer> list = new ArrayList<>(); list.add(1);
                List<Integer> a = list.subList(0,1); list.add(2);
                List<Integer> b = list.subList(0,2); b.set(0,3); a.size();
              }}
              """,
        Diagnostic.Kind.STATE);
  }

  @Test
  void pureOverridesCannotModifyGlobalStorage() {
    rejects(
        """
        @ExternalRefinementsFor("java.util.ArrayList") interface A {
          @Modifies({}) int size();
        }
        class Sample extends ArrayList<Object> {
          static int calls;
          @Override public int size() { calls++; return 0; }
        }
        """,
        Diagnostic.Kind.PERMISSION);
  }

  @Test
  void genericFactoriesPublishCapturesWithoutInventingOperationEffects() {
    var result =
        accepts(
            """
class Factory { static List<Object> slice(List<Object> list, int from, int to) { return list.subList(from,to); } }
@ExternalRefinementsFor("Factory") interface F {
  @Modifies({}) @ReturnsView(of="list", from="from", toExclusive="to")
  List<Object> slice(@Borrowed List<Object> list, int from, int to);
}
class Sample { void run(@Borrowed List<Object> list) {
  List<Object> view = Factory.slice(list, 0, 0);
}}
""");
    assertTrue(
        result.model().getElements(new TypeFilter<>(CtInvocation.class)).stream()
            .anyMatch(c -> ViewFacts.on(c).isPresent()));
  }

  @Test
  void explicitEffectsAlsoPreserveStateInExceptionHandlers() {
    accepts(
        ARRAYS
            + """
              class Sample { void run() {
                int[] a = {7};
                try { Arrays.copyOf(a, -1); } catch (RuntimeException expected) {
                  @Refinement("_ == 7") int n = a[0];
                }
              }}
              """);
  }

  @Test
  void arrayListRuntimeWitnessMatchesViewUpdatesAndInvalidation() {
    var list = new ArrayList<>(List.of(1, 2));
    var parent = list.subList(0, 2);
    var child = parent.subList(1, 2);
    child.clear();
    assertEquals(1, parent.size());
    assertEquals(1, list.size());
    var sibling = list.subList(0, 1);
    parent.set(0, 3);
    assertEquals(3, sibling.get(0));
    parent.add(4);
    assertEquals(2, list.size());
    assertThrows(ConcurrentModificationException.class, sibling::size);
  }

  @Test
  void unresolvedFootprintConstantsCannotSilentlyBecomePure() {
    rejects(
        """
        @ExternalRefinementsFor("java.util.Arrays") interface A {
          String FOOTPRINT = "array[*]";
          @Modifies(FOOTPRINT) void fill(@Borrowed int[] array, int value);
        }
        class Sample {}
        """,
        Diagnostic.Kind.SYNTAX);
  }

  @Test
  void backingSizeUsesTheDeclaredObserverInsteadOfAGhostNamingConvention() {
    accepts(
        LIST.replace("listSize", "count")
            + """
              class Sample { void run() {
                ArrayList<Integer> list = new ArrayList<>(); list.add(1);
                List<Integer> view = list.subList(0, 1); view.add(2);
                @Refinement("_ == 2") int n = list.size();
              }}
              """);
  }

  @Test
  void constructorFootprintsPreserveBorrowedArrayElementIdentities() {
    accepts(
        """
        class Library { Library(Object[] values) {} }
        @ExternalRefinementsFor("Library") interface LibrarySpec {
          @Modifies({}) void Library(@Borrowed Object[] values);
        }
        class Sample {
          @StateSet({"open", "closed"}) static class Resource {
            @StateRefinement(to="open(this)") Resource() {}
            @StateRefinement(from="open(this)") void read() {}
          }
          void run() {
            Resource resource = new Resource(); Resource[] values = {resource};
            new Library(values); values[0].read();
          }
        }
        """);
  }

  @Test
  void invalidSummariesAreRejectedInsteadOfAssumed() {
    for (String annotation :
        List.of("@Modifies(\"missing[*]\")", "@Modifies(\"value[*]\")", "@Modifies(\"this\")"))
      rejects(
          "@ExternalRefinementsFor(\"java.util.Arrays\") interface A { "
              + annotation
              + " void fill(@Borrowed int[] array, int value); } class Sample {}",
          Diagnostic.Kind.SYNTAX);
    rejects(LIST.replace("from=\"fromIndex\"", "from=\"missing\""), Diagnostic.Kind.SYNTAX);
    rejects(LIST.replace("@ReturnsView", "@Free @ReturnsView"), Diagnostic.Kind.SYNTAX);
    rejects("class Sample { @Modifies({}) void method() {} }", Diagnostic.Kind.SYNTAX);
  }
}
