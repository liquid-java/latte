package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.support.compiler.VirtualFile;

class ReferenceArraysTest {
  private void check(String parameters, String body, Diagnostic.Kind... expected) {
    var source =
        """
        import liquidjava.specification.*;
        import specification.*;
        class Sample {
          @StateSet({"open", "closed"})
          static class Resource {
            @StateRefinement(to="open(this)") Resource() {}
            @StateRefinement(from="open(this)") void read() {}
            @StateRefinement(from="open(this)", to="closed(this)") void close() {}
          }
          void leak(Object o) {}
          void use(%s) {
            Resource first = new Resource();
            Resource second = new Resource();
            %s
          }
        }
        """
            .formatted(parameters, body);
    var result = Verification.verify(new VirtualFile(source, "Sample.java"));
    assertEquals(
        List.of(expected),
        result.diagnostics().stream().map(Diagnostic::kind).toList(),
        result.diagnostics().toString());
    if (source.contains("// Expect: ")) {
      var lines = source.lines().toList();
      for (var diagnostic : result.diagnostics())
        assertTrue(
            lines.get(diagnostic.line() - 1).contains("// Expect: " + diagnostic.kind()),
            "Diagnostic on an unexpected line: " + diagnostic);
    }
  }

  @Test
  void separateWritesPreserveBothIdentities() {
    check(
        "",
        """
        Resource[] box = new Resource[2];
        box[0] = first;
        box[1] = second;
        Resource alias = box[0];
        alias.close();
        second.read();
        first.read(); // Expect: STATE
        """,
        Diagnostic.Kind.STATE);
  }

  @Test
  void initializersKeepSlotsSeparate() {
    check("", "Resource[] box = {first, second}; box[0].close(); second.read();");
    check(
        "",
        "Resource[] box = {first, second}; first.close(); box[0].read();",
        Diagnostic.Kind.STATE);
  }

  @Test
  void overwritingASlotPreservesPreviouslyRetrievedAliases() {
    check(
        "",
        """
        Resource[] box = {first};
        Resource saved = box[0];
        box[0] = second;
        saved.close();
        box[0].read();
        first.read();
        """,
        Diagnostic.Kind.STATE);
  }

  @Test
  void arrayAliasesObserveSlotWrites() {
    check(
        "",
        """
        Resource[] box = {first, second};
        Resource[] same = box;
        same[1] = first;
        box[1].close();
        second.read();
        first.read();
        """,
        Diagnostic.Kind.STATE);
  }

  @Test
  void symbolicReadsUpdateOnlyTheSelectedObject() {
    check(
        "@Refinement(\"_>=0 && _<2\") int i",
        """
        Resource[] box = {first, second};
        box[i].close();
        if (i != 0) first.read();
        if (i != 1) second.read();
        first.read();
        """,
        Diagnostic.Kind.STATE);
  }

  @Test
  void symbolicWritesRetainOldAndNewCandidates() {
    check(
        "@Refinement(\"_>=0 && _<2\") int i",
        """
        Resource[] box = {first, first};
        box[i] = second;
        box[0].close();
        if (i == 0) first.read();
        if (i != 0) second.read();
        first.read();
        second.read();
        """,
        Diagnostic.Kind.STATE,
        Diagnostic.Kind.STATE);
  }

  @Test
  void aPreciseOverwriteReplacesAnEarlierUnknownWrite() {
    check(
        "@Refinement(\"_>=0 && _<2\") int i",
        """
        Resource[] box = {first, first};
        box[i] = second;
        box[0] = first;
        box[0].close();
        second.read();
        """);
  }

  @Test
  void branchJoinsUseTheUnwrittenSlotsDefault() {
    check(
        "boolean choose",
        """
        Resource[] box = new Resource[2];
        if (choose) box[0] = first;
        else box[1] = second;
        @Refinement("_ == null") Resource missing = choose ? box[1] : box[0];
        Resource selected = choose ? box[0] : box[1];
        selected.close();
        if (choose) second.read();
        else first.read();
        """);
  }

  @Test
  void nullableCandidatesDoNotMakeAFeasiblePathDisappear() {
    check(
        "@Refinement(\"_>=0 && _<2\") int i",
        """
        Resource[] box = {first, null};
        Resource selected = box[i];
        if (selected == null) { @Refinement("_ > 0") int wrong = -1; }
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void clearingASlotDoesNotEraseOtherReferencesToTheObject() {
    check(
        "",
        """
        Resource[] box = {first, first};
        box[0] = null;
        @Refinement("_ == null") Resource cleared = box[0];
        box[1].close();
        first.read();
        """,
        Diagnostic.Kind.STATE);
  }

  @Test
  void escapingAnArrayEscapesEveryStoredObject() {
    check(
        "",
        """
        Resource[] box = new Resource[2];
        box[0] = first;
        box[1] = second;
        leak(box);
        first.read();
        second.read();
        """,
        Diagnostic.Kind.SHARED_REFINEMENT,
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void aDetachedObjectDoesNotEscapeWithItsFormerArray() {
    check("", "Resource[] box = {first}; box[0] = second; leak(box); first.read();");
  }

  @Test
  void conditionalArrayReceiversRetainBothPossibleTargets() {
    check(
        "boolean choose",
        """
        Resource[] left = {first};
        Resource[] right = {second};
        Resource[] selected = choose ? left : right;
        selected[0] = first;
        right[0].close();
        if (choose) first.read();
        else second.read();
        first.read();
        """,
        Diagnostic.Kind.STATE);
  }

  @Test
  void nestedArrayInitializersPreserveElementAliases() {
    check(
        "",
        """
        Resource[][] box = {{first}, {second}};
        box[0][0].close();
        box[1][0].read();
        first.read();
        """,
        Diagnostic.Kind.STATE);
  }

  @Test
  void aSymbolicStoreCanBeReadBackAtTheSameIndex() {
    check(
        "@Refinement(\"_>=0 && _<2\") int i",
        """
        Resource[] box = new Resource[2];
        box[i] = first;
        box[i].close();
        second.read();
        first.read(); // Expect: STATE
        """,
        Diagnostic.Kind.STATE);
  }

  @Test
  void replacingANullableSlotDoesNotEraseTheNullPathOfOtherSlots() {
    check(
        "boolean choose",
        """
        Resource[] box = {first, null};
        if (choose) box[1] = second;
        Resource selected = box[1];
        if (!choose && selected == null) {
          @Refinement("_ > 0") int wrong = -1; // Expect: REFINEMENT
        }
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void externalArraySlotsAreNotAssumedToContainTheSameObject() {
    check(
        "@Borrowed Object[] external",
        """
        Object left = external[0];
        Object same = external[0];
        @Refinement("_ == true") boolean equal = left == same;
        """);
    check(
        "@Borrowed Object[] external",
        """
        Object left = external[0];
        Object right = external[1];
        @Refinement("_ == true") boolean wrong = left == right; // Expect: REFINEMENT
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void implicitInnerArraysAreNotAssumedToBeTheSameRow() {
    check(
        "",
        """
        int[][] matrix = new int[2][2];
        matrix[0][0] = 9;
        @Refinement("_ == 9") int wrong = matrix[1][0]; // Expect: REFINEMENT
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void mutatingAnExternalArrayInvalidatesPreviouslyReadElements() {
    var source =
        """
        import liquidjava.specification.*;
        import specification.*;
        class Sample {
          static class Cell { int count; }
          void increment(@Borrowed Cell[] array) { array[0].count++; }
          void use(@Borrowed Cell[] array) {
            Cell selected = array[0];
            int before = selected.count;
            increment(array);
            @Refinement("_ == before") int stale = selected.count;
          }
        }
        """;
    var result = Verification.verify(new VirtualFile(source, "Sample.java"));
    assertEquals(
        List.of(Diagnostic.Kind.REFINEMENT),
        result.diagnostics().stream().map(Diagnostic::kind).toList(),
        result.diagnostics().toString());
  }

  @Test
  void borrowedAndUniqueObjectsRetainTheirStateThroughLocalArrays() {
    for (String ownership : List.of("Borrowed", "Unique"))
      check(
          "@" + ownership + " @Refinement(\"open(_)\") Resource resource",
          """
          Resource[] box = {resource};
          box[0].close();
          resource.read(); // Expect: STATE
          """,
          Diagnostic.Kind.STATE);
  }
}
