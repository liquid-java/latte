package latte.analysis;

import static latte.analysis.LatteAnalysisTest.check;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ArrayOwnershipTest {
  @Test
  void preciseSlotsAndUnknownReadsArePublishedOnTheAst() {
    var result =
        check(
            """
        class Sample { void f(int index) {
          Object first = new Object(), second = new Object();
          Object[] array = new Object[2];
          array[0] = first;
          array[1] = second;
          Object zero = array[0], one = array[1], unknown = array[index];
        } }
        """);
    assertEquals(List.of(), result.diagnostics());
    assertEquals(result.local("first").value(), result.local("zero").value());
    assertEquals(result.local("second").value(), result.local("one").value());
    assertEquals(2, result.local("unknown").value().objects().size());
    assertEquals(Permission.FREE, result.local("unknown").permission());
  }

  @Test
  void anUnknownWriteRetainsEverySlotAndAPreciseWriteCanReplaceIt() {
    var result =
        check(
            """
        class Sample { void f(int index) {
          Object first = new Object(), second = new Object();
          Object[] array = {first, null};
          array[index] = second;
          Object zero = array[0], one = array[1];
          array[0] = second;
          Object replaced = array[0];
        } }
        """);
    assertEquals(List.of(), result.diagnostics());
    assertEquals(2, result.local("zero").value().objects().size());
    assertFalse(result.local("zero").value().nullable());
    assertEquals(result.local("second").value().objects(), result.local("one").value().objects());
    assertTrue(result.local("one").value().nullable());
    assertEquals(result.local("second").value(), result.local("replaced").value());
  }

  @Test
  void branchJoinsRetainDefaultsInsteadOfInventingExternalElements() {
    var result =
        check(
            """
        class Sample { void f(boolean choose) {
          Object first = new Object();
          Object[] array = new Object[1];
          if (choose) array[0] = first;
          Object selected = array[0];
        } }
        """);
    assertEquals(List.of(), result.diagnostics());
    assertEquals(
        result.local("first").value().objects(), result.local("selected").value().objects());
    assertTrue(result.local("selected").value().nullable());
    assertEquals(Permission.FREE, result.local("selected").permission());
  }

  @Test
  void sourceReturnSummariesPreserveConstantAndUnknownSlots() {
    var result =
        check(
            """
        import specification.*;
        class Sample {
          static Object first(@Borrowed Object[] a) { return a[0]; }
          static Object at(@Borrowed Object[] a, int i) { return a[i]; }
          void f(int index) {
            Object x = new Object(), y = new Object();
            Object[] a = {x, y};
            Object exact = first(a);
            Object unknown = at(a, index);
          }
        }
        """);
    assertEquals(List.of(), result.diagnostics());
    assertEquals(result.local("x").value(), result.local("exact").value());
    assertEquals(2, result.local("unknown").value().objects().size());
  }

  @Test
  void loopWritesPreserveZeroIterationsAndAllOldCandidates() {
    var result =
        check(
            """
        class Sample { void f(boolean again, int index) {
          Object first = new Object(), second = new Object();
          Object[] a = {first};
          while (again) a[index] = second;
          Object selected = a[0];
        } }
        """);
    assertEquals(List.of(), result.diagnostics());
    assertEquals(2, result.local("selected").value().objects().size());
    assertEquals(Permission.FREE, result.local("selected").permission());
  }

  @Test
  void foreachIncludesElementsWrittenDuringIteration() {
    var result =
        check(
            """
        class Sample { void f() {
          Object first = new Object(), second = new Object();
          Object[] a = {first, first};
          for (Object element : a) {
            Object selected = element;
            a[1] = second;
          }
        } }
        """);
    assertEquals(List.of(), result.diagnostics());
    assertEquals(2, result.local("selected").value().objects().size());
    assertEquals(Permission.FREE, result.local("selected").permission());
  }

  @Test
  void borrowedElementsMayStayLocalButCannotEscapeInAnArray() {
    var safe =
        check(
            """
        import specification.*;
        class Sample { void f(@Borrowed Object value) {
          Object[] a = {value}; Object selected = a[0];
        } }
        """);
    assertEquals(List.of(), safe.diagnostics());
    assertEquals(Permission.BORROWED, safe.local("selected").permission());
    var escaped =
        check(
            """
        import specification.*;
        class Sample { void leak(Object o) {} void f(@Borrowed Object value) {
          Object[] a = new Object[2]; a[0] = value; a[1] = new Object(); leak(a);
        } }
        """);
    assertTrue(
        escaped.diagnostics().stream().anyMatch(d -> d.message().contains("Borrowed reference")));
  }

  @Test
  void transferConsumesAliasesStoredInOtherSlots() {
    var result =
        check(
            """
        import specification.*;
        class Sample { void take(@Free Object o) {} void f() {
          Object value = new Object(); Object[] a = {value, value};
          take(a[0]); Object stale = a[1];
        } }
        """);
    assertTrue(result.diagnostics().stream().anyMatch(d -> d.message().contains("consumed")));
  }

  @Test
  void externallySuppliedElementsRemainShared() {
    var result =
        check(
            """
        import specification.*;
        class Sample { void f(@Borrowed Object[] a) { Object selected = a[0]; } }
        """);
    assertEquals(List.of(), result.diagnostics());
    assertEquals(Permission.SHARED, result.local("selected").permission());
  }

  @Test
  void summarizedOlderArraysRetainMissingSlotsDefaults() {
    var array = Reference.object("array:site");
    var first = Reference.object("first");
    var second = Reference.object("second");
    var state =
        FlowState.empty()
            .fresh(array, Permission.FREE)
            .initializeArray(array, Reference.nullValue())
            .write(array, "[0]", first)
            .fresh(array, Permission.FREE)
            .initializeArray(array, Reference.nullValue())
            .write(array, "[1]", second)
            .fresh(array, Permission.FREE);
    var summary = Reference.object("summary:array:site");
    assertEquals(first.join(Reference.nullValue()), state.read(summary, "[0]").orElseThrow());
    assertEquals(second.join(Reference.nullValue()), state.read(summary, "[1]").orElseThrow());
  }
}
