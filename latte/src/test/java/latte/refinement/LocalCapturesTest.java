package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import latte.api.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import spoon.support.compiler.VirtualFile;

class LocalCapturesTest {
  private Verification.Result check(String members, String body) {
    return Verification.verify(
        new VirtualFile(
            """
        import specification.*;
        import liquidjava.specification.*;
        class Sample {
          interface Action { void run(); }
          @StateSet({"open", "closed"})
          static class Resource {
            @StateRefinement(to="open(this)") Resource() {}
            @StateRefinement(from="open(this)") void read() {}
            @StateRefinement(from="open(this)", to="closed(this)") void close() {}
          }
          %s
          static void use() {
            Resource resource = new Resource();
            %s
          }
        }
        """
                .formatted(members, body),
            "Sample.java"));
  }

  private void accepts(String members, String body) {
    var result = check(members, body);
    assertTrue(result.accepted(), result.diagnostics().toString());
  }

  private void rejects(String members, String body, Diagnostic.Kind kind) {
    var result = check(members, body);
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == kind),
        result.diagnostics().toString());
    assertFalse(
        result.diagnostics().stream()
            .anyMatch(
                d -> d.kind() == Diagnostic.Kind.SYNTAX || d.kind() == Diagnostic.Kind.UNSUPPORTED),
        result.diagnostics().toString());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Action callback = () -> resource.read();",
        "Runnable callback = () -> resource.read();",
        "Action callback = resource::read;"
      })
  void localReadCallbacksPreserveState(String callback) {
    accepts("", callback + " callback.run(); resource.read();");
  }

  @Test
  void creatingAClosureDoesNotExecuteItsMutations() {
    accepts("", "Action callback = () -> resource.close(); resource.read();");
  }

  @ParameterizedTest
  @ValueSource(strings = {"() -> resource.close()", "resource::close"})
  void mutationThroughCallbackReachesOtherAliases(String callback) {
    rejects(
        "",
        "Resource alias = resource; Action callback = "
            + callback
            + "; callback.run(); alias.read();",
        Diagnostic.Kind.STATE);
  }

  @ParameterizedTest
  @ValueSource(strings = {"() -> resource.read()", "resource::read"})
  void callbackChecksTheStateAtInvocation(String callback) {
    rejects(
        "",
        "Action callback = " + callback + "; resource.close(); callback.run();",
        Diagnostic.Kind.STATE);
  }

  @Test
  void repeatedCallbackCallsObservePriorMutations() {
    rejects(
        "",
        "Action callback = () -> resource.close(); callback.run(); callback.run();",
        Diagnostic.Kind.STATE);
  }

  @Test
  void sharedStoreEscapesTheCaptures() {
    rejects(
        "static Action published;",
        "Action callback = () -> resource.read(); published = callback; resource.read();",
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void escapingANestedCaptorEscapesItsResources() {
    rejects(
        "static Action published;",
        "Action inner = () -> resource.read(); Action outer = () -> inner.run();"
            + " published = outer; resource.read();",
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void borrowedCapturesCanBeUsedLocally() {
    accepts(
        """
        static void borrow(@Borrowed @Refinement("open(_)") Resource resource) {
          Action callback = () -> resource.read(); callback.run(); resource.read();
        }
        """,
        "borrow(resource);");
  }

  @Test
  void borrowedCapturesCannotEscape() {
    rejects(
        """
        static Action published;
        static void borrow(@Borrowed Resource resource) {
          Action callback = () -> { Object alias = resource; }; published = callback;
        }
        """,
        "",
        Diagnostic.Kind.PERMISSION);
  }

  @Test
  void passingToSharedCodeEscapesCaptures() {
    rejects(
        "static void publish(Action callback) {}",
        "Action callback = () -> resource.read(); publish(callback); resource.read();",
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void localClosureAliasKeepsItsCaptures() {
    accepts(
        "",
        "Action callback = () -> resource.read(); Action alias = callback; alias.run(); resource.read();");
  }

  @Test
  void nestedLocalClosuresKeepState() {
    accepts(
        "",
        "Action inner = () -> resource.read(); Action outer = () -> inner.run(); outer.run(); resource.read();");
  }

  @Test
  void localCallbacksValidateParameterAndResultContracts() {
    accepts(
        "interface Positive { @Refinement(\"_ > 0\") int apply(@Refinement(\"_ > 0\") int x); }",
        "Positive p = x -> x; @Refinement(\"_ == 3\") int result = p.apply(3);");
    rejects(
        "interface Positive { int apply(@Refinement(\"_ > 0\") int x); }",
        "Positive p = x -> x; p.apply(0);",
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void nestedCallbackInvocationsKeepSeparateOwnershipFrames() {
    accepts(
        "",
        "Action outer = () -> { Action inner = () -> resource.read(); inner.run(); };"
            + " outer.run(); outer.run(); resource.read();");
  }

  @Test
  void capturedThisRetainsItsCurrentFieldAliases() {
    accepts(
        """
        @Unique Resource value;
        void local(@Unique @Refinement("open(_)") Resource first,
                   @Unique @Refinement("open(_)") Resource second) {
          value = first; Action callback = () -> value.close(); value = second;
          callback.run(); first.read();
        }
        """,
        "");
  }

  @Test
  void boundMethodReferencesKeepTheirOriginalReceiver() {
    accepts(
        """
        static class Box { @Unique Resource value; }
        """,
        "Box box = new Box(); box.value = resource; Action callback = box.value::close;"
            + " Resource replacement = new Resource(); box.value = replacement; callback.run(); replacement.read();");
  }

  @Test
  void invokingAClosureAfterItsCaptureWasConsumedIsRejected() {
    rejects(
        "static void take(@Free Resource resource) {}",
        "Action callback = () -> resource.read(); take(resource); callback.run();",
        Diagnostic.Kind.PERMISSION);
  }

  @Test
  void callbackEffectsAreIncludedInLoopInvariants() {
    rejects(
        "",
        "Action callback = () -> resource.close(); for (int i = 0; i < 2; i++) callback.run();",
        Diagnostic.Kind.STATE);
  }

  @Test
  void readOnlyCallbacksCanRunInLoops() {
    accepts(
        "",
        "Action callback = () -> resource.read(); for (int i = 0; i < 2; i++) callback.run(); resource.read();");
  }

  @Test
  void borrowedCapturesCannotBeTransferredToAnOwningParameter() {
    rejects(
        """
        static void take(@Free Action callback) {}
        static void borrow(@Borrowed Object object) {
          Action callback = () -> { Object alias = object; }; take(callback);
        }
        """,
        "",
        Diagnostic.Kind.PERMISSION);
  }

  @Test
  void passingToAnUnusedBorrowedParameterDoesNotEscape() {
    accepts(
        "static void inspect(@Borrowed Action callback) {}",
        "Action callback = () -> resource.read(); inspect(callback); resource.read(); callback.run();");
  }

  @Test
  void borrowingACallbackDoesNotAssumeItRunsOnlyOnce() {
    rejects(
        "static void twice(@Borrowed Action callback) { callback.run(); callback.run(); }",
        "Action callback = () -> resource.close(); twice(callback);",
        Diagnostic.Kind.STATE);
  }

  @Test
  void constructorStoredClosuresKeepTheirActualCapturedArguments() {
    accepts(
        """
        static class Holder {
          @Unique Action callback;
          Holder(@Unique @Refinement("open(_)") Resource resource) {
            callback = () -> resource.read();
          }
        }
        """,
        "Holder holder = new Holder(resource); holder.callback.run(); resource.read();");
  }

  @Test
  void capturesDoNotGainPermissionFromAFunctionalParameterContract() {
    rejects(
        """
        interface Borrow { void use(@Borrowed @Refinement("open(_)") Resource resource); }
        static void shared(Resource resource) {
          Borrow callback = r -> r.read(); callback.use(resource);
        }
        """,
        "",
        Diagnostic.Kind.PERMISSION);
  }

  @Test
  void returningACapturedAliasPreservesItsState() {
    accepts(
        "interface Getter { @Borrowed Resource get(); }",
        "Getter getter = () -> resource; getter.get().read();");
    rejects(
        "interface Getter { @Borrowed Resource get(); }",
        "Getter getter = () -> resource; getter.get().close(); resource.read();",
        Diagnostic.Kind.STATE);
  }

  @Test
  void sharedClosureResultsEscapeTheReturnedObject() {
    rejects(
        "interface Getter { Resource get(); }",
        "Getter getter = () -> resource; Resource shared = getter.get(); resource.read();",
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void freeCallbackParametersAreUsableInsideTheBody() {
    accepts(
        "interface Take { void take(@Free @Refinement(\"open(_)\") Resource resource); }",
        "Take callback = value -> value.read(); callback.take(resource);");
  }

  @Test
  void freeClosureResultsKeepStateUnderTheirNewOwner() {
    accepts(
        "interface Getter { @Free Resource get(); }",
        "Getter getter = () -> resource; Resource owned = getter.get(); owned.read();");
    rejects(
        "interface Getter { @Free Resource get(); }",
        "Getter getter = () -> resource; Resource owned = getter.get(); resource.read();",
        Diagnostic.Kind.PERMISSION);
  }

  @Test
  void constructorReferencesCreateFreshObjects() {
    accepts(
        "interface Factory { @Borrowed Resource create(); }",
        "Factory factory = Resource::new; Resource created = factory.create(); created.read();");
  }
}
