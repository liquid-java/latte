package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import latte.analysis.OwnershipRelease;
import latte.api.Diagnostic;
import latte.api.Verification;
import org.junit.jupiter.api.Test;
import spoon.reflect.code.CtStatement;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.compiler.VirtualFile;

class OwnershipReleaseRefinementTest {
  private static final String PREFIX =
      """
      import specification.*;
      import liquidjava.specification.*;
      @StateSet({"open", "closed"}) class Resource {
        @StateRefinement(to="open(this)") Resource() {}
        @StateRefinement(from="open(this)") void read() {}
        @StateRefinement(from="open(this)", to="closed(this)") void close() {}
      }
      class Box {
        @Unique Resource value;
        Box(@Unique Resource value) { this.value = value; }
      }
      class Owner {
        @Unique Resource value;
        Owner(@Free Resource value) { this.value = value; }
      }
      interface Sink { void accept(@Free @Refinement("open(_)") Resource value); }
      class Sample {
        static void consume(@Free @Refinement("open(_)") Resource value) { value.read(); }
      """;

  private Verification.Result check(String methods) {
    return Verification.verify(new VirtualFile(PREFIX + methods + "}", "Sample.java"));
  }

  @Test
  void constructorTransferPreservesTheChildState() {
    var result =
        check(
            "void f() { Box box = new Box(new Resource()); Owner owner = new Owner(box.value); owner.value.read(); }");
    assertTrue(result.accepted(), result.diagnostics().toString());
  }

  @Test
  void aFreeReturnRetainsItsRefinementObligation() {
    var result =
        check(
            "@Free @Refinement(\"open(_)\") Resource f() { Box box = new Box(new Resource()); return box.value; }");
    assertTrue(result.accepted(), result.diagnostics().toString());
  }

  @Test
  void aFreeMethodArgumentRetainsItsCurrentState() {
    var result = check("void f() { Box box = new Box(new Resource()); consume(box.value); }");
    assertTrue(result.accepted(), result.diagnostics().toString());
  }

  @Test
  void inferenceDoesNotRestoreAnEarlierState() {
    var result =
        check(
            "void f() { Box box = new Box(new Resource()); box.value.close(); consume(box.value); }");
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.REFINEMENT),
        result.diagnostics().toString());
  }

  @Test
  void constructorTransferStillConsumesAllOldChildAliases() {
    var result =
        check(
            "void f() { Box box = new Box(new Resource()); Resource stale = box.value; Owner owner = new Owner(box.value); stale.read(); }");
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.PERMISSION),
        result.diagnostics().toString());
  }

  @Test
  void aBorrowedCallDoesNotReleaseOwnership() {
    var result =
        check(
            "static void read(@Borrowed @Refinement(\"open(_)\") Resource r) { r.read(); } void f() { Box box = new Box(new Resource()); read(box.value); }");
    assertTrue(result.accepted(), result.diagnostics().toString());
    assertTrue(
        result.model().getElements(new TypeFilter<>(CtStatement.class)).stream()
            .noneMatch(statement -> OwnershipRelease.on(statement).isPresent()));
  }

  @Test
  void localCallbackTransferPreservesTheChildState() {
    var result =
        check(
            "void f() { Box box = new Box(new Resource()); Sink sink = value -> value.read(); sink.accept(box.value); }");
    assertTrue(result.accepted(), result.diagnostics().toString());
  }

  @Test
  void capturesOfTheInvokedCallbackAreObservableDuringTheCall() {
    var result =
        check(
            "void f() { Box box = new Box(new Resource()); Sink sink = value -> box.value.read(); sink.accept(box.value); }");
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.PERMISSION),
        result.diagnostics().toString());
  }

  @Test
  void bindingsMentionedOnlyInAnnotationsAreStillObservable() {
    var result =
        check(
            "void f() { Box box = new Box(new Resource()); consume(box.value); @Refinement(\"box != null\") int marker = 0; }");
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.PERMISSION),
        result.diagnostics().toString());
    assertTrue(
        result.model().getElements(new TypeFilter<>(CtStatement.class)).stream()
            .noneMatch(statement -> OwnershipRelease.on(statement).isPresent()));
  }
}
