package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.support.compiler.VirtualFile;

class ExternalSpecificationsTest {
  private Verification.Result check(String source) {
    return Verification.verify(
        new VirtualFile(
            "import liquidjava.specification.*; import specification.*;\n" + source,
            "Sample.java"));
  }

  private void accepts(String source) {
    var result = check(source);
    assertTrue(result.accepted(), result.diagnostics().toString());
  }

  private void rejects(String source, Diagnostic.Kind kind) {
    var result = check(source);
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == kind),
        result.diagnostics().toString());
  }

  private static final String LIST =
      """
      @ExternalRefinementsFor("java.util.ArrayList")
      @RefinementPredicate("int size(ArrayList list)")
      interface ListSpec<E> {
        @StateRefinement(to="size(this) == 0") void ArrayList();
        @StateRefinement(to="size(this) == size(old(this)) + 1") boolean add(E value);
        @StateRefinement(from="true") @Refinement("_ == size(this)") int size();
        @StateRefinement(from="size(this) > 0", to="size(this) == size(old(this)) - 1")
        E remove(@Refinement("_ >= 0 && _ < size(this)") int index);
      }
      """;

  @Test
  void libraryConstructorsAndTransitionsReachLocalAliases() {
    accepts(
        LIST
            + """
        class Sample { void f() {
          java.util.ArrayList<Integer> list = new java.util.ArrayList<>();
          java.util.ArrayList<Integer> alias = list;
          alias.add(3);
          @Refinement("_ == 1") int count = list.size();
        }}
        """);
  }

  @Test
  void externalParameterRefinementsAreChecked() {
    rejects(
        LIST
            + "class Sample { void f() { java.util.ArrayList<Integer> list = new java.util.ArrayList<>(); list.add(3); list.remove(1); }}",
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void sharedLibraryReceiversStillRejectStateRefinements() {
    rejects(
        LIST + "class Sample { void f(java.util.ArrayList<Integer> list) { list.size(); }}",
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void resolvesStaticOverloadsByTheirJavaSignature() {
    accepts(
        """
        @ExternalRefinementsFor("java.lang.Math") interface MathSpec {
          @Refinement("_ == (left > right ? left : right)") int max(int left, int right);
          @Refinement("_ == (a > b ? a : b)") long max(long a, long b);
        }
        class Sample { void f() {
          @Refinement("_ == 7") int first = Math.max(3, 7);
          @Refinement("_ == 5000000000") long second = Math.max(5000000000L, 7L);
        }}
        """);
  }

  @Test
  void borrowedLibraryParametersDoNotEscapeButCanMutate() {
    rejects(
        """
        @ExternalRefinementsFor("java.util.Arrays") interface ArraysSpec {
          void fill(@Borrowed int[] values, int value);
        }
        class Sample { void f() {
          int[] values = { 0 };
          java.util.Arrays.fill(values, 3);
          @Refinement("_ == 0") int stale = values[0];
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void freeExternalParametersConsumeAllCallerAliases() {
    rejects(
        """
        @ExternalRefinementsFor("java.util.Objects") interface ObjectsSpec {
          @Free @Refinement("_ != null") Object requireNonNull(@Free Object object);
        }
        class Sample { void f() {
          Object original = new Object(); Object alias = original;
          @Refinement("_ != null") Object result = java.util.Objects.requireNonNull(original);
          Object stale = alias;
        }}
        """,
        Diagnostic.Kind.PERMISSION);
  }

  @Test
  void externalConstructorParametersHaveOwnershipContracts() {
    accepts(
        """
        @ExternalRefinementsFor("java.io.ByteArrayInputStream") interface InputSpec {
          void ByteArrayInputStream(@Borrowed byte[] bytes);
        }
        class Sample { void f(@Borrowed byte[] bytes) {
          new java.io.ByteArrayInputStream(bytes);
        }}
        """);
  }

  @Test
  void rejectsMisspelledMemberSignatures() {
    rejects(
        "@ExternalRefinementsFor(\"java.util.ArrayList\") interface Spec { void ad(int n); }",
        Diagnostic.Kind.SYNTAX);
  }

  @Test
  void rejectsIncompatibleReturnTypes() {
    rejects(
        "@ExternalRefinementsFor(\"java.lang.Math\") interface Spec { int max(long a, long b); }",
        Diagnostic.Kind.SYNTAX);
  }

  @Test
  void rejectsDuplicateTargetSpecifications() {
    rejects(
        "@ExternalRefinementsFor(\"java.lang.Math\") interface One {} @ExternalRefinementsFor(\"java.lang.Math\") interface Two {}",
        Diagnostic.Kind.SYNTAX);
  }

  @Test
  void rejectsUnknownTargetWithoutCrashing() {
    rejects(
        "@ExternalRefinementsFor(\"missing.Library\") interface Spec {}", Diagnostic.Kind.SYNTAX);
  }

  @Test
  void rejectsAnnotationOnAMethod() {
    rejects(
        "class Sample { @ExternalRefinementsFor(\"java.lang.Math\") void f() {} }",
        Diagnostic.Kind.SYNTAX);
  }

  @Test
  void externalBodiesAreOnlyPlaceholders() {
    accepts(
        """
        @ExternalRefinementsFor("java.lang.Math") class Spec {
          @Refinement("_ >= 0") public int abs(@Refinement("_ > -2147483648") int x) { return -1; }
        }
        class Sample { void f() { @Refinement("_ >= 0") int n = Math.abs(-3); }}
        """);
  }

  @Test
  void unspecifiedOverloadDoesNotUseAnotherOverloadsContract() {
    rejects(
        """
        @ExternalRefinementsFor("java.lang.Math") interface Spec {
          @Refinement("_ == 7") int max(int a, int b);
        }
        class Sample { void f() { @Refinement("_ == 7") long n = Math.max(3L, 7L); }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void inheritedInterfaceContractsApplyToConcreteReceivers() {
    accepts(
        """
        @ExternalRefinementsFor("java.util.Collection") interface Spec<E> {
          @StateRefinement(from="true") @Refinement("_ >= 0") int size();
        }
        class Sample { void f() {
          java.util.ArrayList<String> list = new java.util.ArrayList<>();
          @Refinement("_ >= 0") int n = list.size();
        }}
        """);
  }

  @Test
  void borrowedReceiversRetainStateContracts() {
    accepts(
        LIST
            + """
        class Sample { void f(@Borrowed java.util.ArrayList<Integer> list) {
          int before = list.size(); list.add(1);
          @Refinement("_ == before + 1") int after = list.size();
        }}
        """);
  }

  @Test
  void unlistedCallsInvalidateLibraryState() {
    rejects(
        LIST
            + """
        class Sample { void f() {
          java.util.ArrayList<Integer> list = new java.util.ArrayList<>();
          list.clear(); @Refinement("_ == 0") int n = list.size();
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void constructorsNeverInheritTheirParentsExternalContract() {
    rejects(
        """
        class Parent { Parent(int n) {} }
        class Child extends Parent { Child(int n) { super(n); } }
        @ExternalRefinementsFor("Parent") interface Spec {
          @Refinement("false") void Parent(int n);
        }
        class Sample { void f() { Child c = new Child(1); @Refinement("false") int bad = 1; }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void constructorPreconditionsAreEnforced() {
    rejects(
        """
        @ExternalRefinementsFor("java.util.ArrayList") interface Spec {
          void ArrayList(@Refinement("_ >= 0") int capacity);
        }
        class Sample { void f() { new java.util.ArrayList<>(-1); }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void externalConstructorMutationInvalidatesLoopEntryFacts() {
    rejects(
        """
        @ExternalRefinementsFor("java.io.ByteArrayInputStream") interface Spec {
          void ByteArrayInputStream(@Borrowed byte[] bytes);
        }
        class Sample { void f(boolean again) {
          byte[] bytes = {0};
          while (again) {
            @Refinement("_ == 0") byte value = bytes[0];
            new java.io.ByteArrayInputStream(bytes);
          }
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void callbacksUseExternalMethodContracts() {
    accepts(
        """
        @ExternalRefinementsFor("java.lang.Math") interface Spec {
          @Refinement("_ == (x >= 0 ? x : (x == -2147483648 ? x : -x))") int abs(int x);
        }
        class Sample { void f() {
          java.util.function.IntUnaryOperator absolute = Math::abs;
          @Refinement("_ >= 0") int n = absolute.applyAsInt(-3);
        }}
        """);
  }

  @Test
  void sharedExternalReturnsCannotCarryRefinements() {
    rejects(
        """
        @ExternalRefinementsFor("java.util.Objects") interface Spec {
          @Refinement("_ != null") Object requireNonNull(Object x);
        }
        """,
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void primitiveExternalFieldsCarryTheirDeclaredInvariant() {
    accepts(
        """
        @ExternalRefinementsFor("java.awt.Point") interface Spec {
          @Refinement("_ >= 0") int x = -999;
        }
        class Sample { void f() { java.awt.Point point = new java.awt.Point(1,2);
          @Refinement("_ >= 0") int n = point.x;
        }}
        """);
    rejects(
        """
        @ExternalRefinementsFor("java.awt.Point") interface Spec {
          @Refinement("_ >= 0") int x = -999;
        }
        class Sample { void f() { java.awt.Point point = new java.awt.Point(1,2); point.x = -1; }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void staticFieldPlaceholdersAreNotRuntimeValues() {
    rejects(
        """
        class Library { public static int count = 4; }
        @ExternalRefinementsFor("Library") interface Spec {
          @Refinement("_ >= 0") int count = -999;
        }
        class Sample { void f() { @Refinement("_ == -999") int n = Library.count; }}
        """,
        Diagnostic.Kind.REFINEMENT);
    accepts(
        """
        class Library { public static int count = 4; }
        @ExternalRefinementsFor("Library") interface Spec {
          @Refinement("_ >= 0") int count = -999;
        }
        class Sample { void f() { @Refinement("_ >= 0") int n = Library.count; }}
        """);
  }

  @Test
  void rejectsInvalidExternalFieldTypes() {
    rejects(
        "@ExternalRefinementsFor(\"java.awt.Point\") interface Spec { long x = 0; }",
        Diagnostic.Kind.SYNTAX);
  }

  @Test
  void externalStateSetsUseLibraryTypeIdentity() {
    accepts(
        """
        @ExternalRefinementsFor("java.io.ByteArrayInputStream") @StateSet({"open", "closed"})
        interface Spec {
          @StateRefinement(to="open(this)") void ByteArrayInputStream(byte[] bytes);
          @StateRefinement(from="open(this)", to="closed(this)") void close();
          @StateRefinement(from="closed(this)") @Refinement("_ >= 0") int available();
        }
        class Sample { void f() {
          java.io.ByteArrayInputStream stream = new java.io.ByteArrayInputStream(new byte[0]);
          stream.close(); @Refinement("_ >= 0") int n = stream.available();
        }}
        """);
  }

  @Test
  void borrowedArgumentsRemainLocallyRefinable() {
    accepts(
        """
        @ExternalRefinementsFor("java.util.Arrays") interface Spec {
          void fill(@Borrowed int[] values, int value);
        }
        class Sample { void f() {
          int[] values = { 0 }; java.util.Arrays.fill(values, 3);
          @Refinement("_ != null") int[] alias = values;
        }}
        """);
  }

  @Test
  void unspecifiedOwnershipEscapesArguments() {
    rejects(
        """
        @ExternalRefinementsFor("java.util.Arrays") interface Spec {
          void fill(int[] values, int value);
        }
        class Sample { void f() {
          int[] values = { 0 }; java.util.Arrays.fill(values, 3);
          @Refinement("_ != null") int[] alias = values;
        }}
        """,
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void freeResultsRemainRefinableAfterTransfer() {
    accepts(
        """
        @ExternalRefinementsFor("java.util.Objects") interface Spec {
          @Free @Refinement("_ != null") Object requireNonNull(@Free Object x);
        }
        class Sample { void f() {
          Object x = new Object();
          @Refinement("_ != null") Object result = java.util.Objects.requireNonNull(x);
        }}
        """);
  }

  @Test
  void uniqueExternalArgumentsRejectBorrowedReferences() {
    rejects(
        """
        @ExternalRefinementsFor("java.util.Arrays") interface Spec {
          void fill(@Unique int[] values, int value);
        }
        class Sample { void f(@Borrowed int[] values) {
          java.util.Arrays.fill(values, 3);
        }}
        """,
        Diagnostic.Kind.PERMISSION);
  }

  @Test
  void retainsBindingsAndOwnershipOnSpoonNodesAcrossReanalysis() {
    var result =
        check(
            LIST
                + """
        class Sample { void f() {
          java.util.ArrayList<Integer> list = new java.util.ArrayList<>(); list.add(1);
        }}
        """);
    assertTrue(result.accepted(), result.diagnostics().toString());
    var calls =
        result
            .model()
            .getElements(
                new spoon.reflect.visitor.filter.TypeFilter<>(
                    spoon.reflect.code.CtInvocation.class));
    var call =
        calls.stream()
            .filter(c -> c.getExecutable().getSimpleName().equals("add"))
            .findFirst()
            .orElseThrow();
    var contract = latte.analysis.ExternalSpecifications.resolve(call.getExecutable());
    assertEquals(
        "ListSpec", contract.getParent(spoon.reflect.declaration.CtType.class).getSimpleName());
    assertTrue(latte.analysis.LatteFacts.on(call.getTarget()).orElseThrow().refinable());
    assertTrue(new latte.analysis.LatteAnalysis().analyze(result.model()).isEmpty());
    assertTrue(new LiquidTypeChecking().analyze(result.model()).isEmpty());
    rejects(
        "class Sample { void f() { @Refinement(\"_ == 3\") int n = Math.max(2, 3); }}",
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void varargsContractsRequireAnExplicitArray() {
    String specification =
        """
        @ExternalRefinementsFor("java.lang.String") interface Spec {
          String format(String format, @Borrowed Object... values);
        }
        """;
    accepts(
        specification
            + """
        class Sample { void f() { String.format("%s", new Object[]{new Object()}); }}
        """);
    rejects(
        specification
            + """
        class Sample { void f() { String.format("%s", new Object()); }}
        """,
        Diagnostic.Kind.UNSUPPORTED);
  }

  @Test
  void validatesUnusedExternalFieldPredicates() {
    rejects(
        """
        @ExternalRefinementsFor("java.awt.Point") interface Spec {
          @Refinement("_ >= missing") int x = 0;
        }
        """,
        Diagnostic.Kind.SYNTAX);
  }

  @Test
  void staticMethodsCannotDeclareReceiverState() {
    rejects(
        """
        @ExternalRefinementsFor("java.lang.Math") interface Spec {
          @StateRefinement(from="true") int abs(int value);
        }
        """,
        Diagnostic.Kind.SYNTAX);
  }

  @Test
  void hiddenFieldsDoNotInheritTheirParentsContract() {
    rejects(
        """
        class Parent { int x; }
        class Child extends Parent { int x; }
        @ExternalRefinementsFor("Parent") interface Spec {
          @Refinement("_ >= 0") int x = 0;
        }
        class Sample { void f(@Borrowed Child child) {
          child.x = -1; @Refinement("_ >= 0") int bad = child.x;
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void hiddenStaticMethodsDoNotInheritTheirParentsContract() {
    rejects(
        """
        class Parent { static int value() { return 1; } }
        class Child extends Parent { static int value() { return 2; } }
        @ExternalRefinementsFor("Parent") interface Spec {
          @Refinement("_ == 1") int value();
        }
        class Sample { void f() { @Refinement("_ == 1") int bad = Child.value(); }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void rejectsMembersThatJavaDoesNotInherit() {
    var result =
        Verification.verify(
            "src/test/resources/liquidjava/testSuite/classes/inherited_external_visibility");
    assertEquals(3, result.diagnostics().size(), result.diagnostics().toString());
    assertTrue(result.diagnostics().stream().allMatch(d -> d.kind() == Diagnostic.Kind.SYNTAX));
  }

  @Test
  void externalCallsDoNotImplyGlobalPurity() {
    rejects(
        """
        class Library { static void invoke() { Sample.counter = 2; } }
        @ExternalRefinementsFor("Library") interface Spec { void invoke(); }
        class Sample { static int counter; void f() {
          counter = 1; Library.invoke(); @Refinement("_ == 1") int stale = counter;
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void externalReferenceFieldsUseOwnershipAndSharedPolicy() {
    accepts(
        """
        class Library { Object value; }
        @ExternalRefinementsFor("Library") interface Spec {
          @Unique @Refinement("_ != null") Object value = null;
        }
        class Sample { void f(@Borrowed Library library) {
          @Refinement("_ != null") Object value = library.value;
        }}
        """);
    rejects(
        """
        class Library { Object value; }
        @ExternalRefinementsFor("Library") interface Spec {
          @Refinement("_ != null") Object value = null;
        }
        """,
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void placeholderBodiesDoNotSupplyReturnAliasFacts() {
    var result =
        check(
            """
        @ExternalRefinementsFor("java.util.Objects") class Spec {
          @Borrowed @Refinement("_ != null") Object requireNonNull(@Borrowed Object x) { return null; }
        }
        class Sample { void f() {
          Object original = new Object(); Object result = java.util.Objects.requireNonNull(original);
        }}
        """);
    assertTrue(result.accepted(), result.diagnostics().toString());
    var local =
        result
            .model()
            .getElements(
                new spoon.reflect.visitor.filter.TypeFilter<>(
                    spoon.reflect.code.CtLocalVariable.class))
            .stream()
            .filter(v -> v.getSimpleName().equals("result"))
            .findFirst()
            .orElseThrow();
    var facts = latte.analysis.LatteFacts.on(local).orElseThrow();
    assertFalse(facts.value().objects().isEmpty());
  }

  @Test
  void varargsMethodReferenceAdaptersAreRejectedExplicitly() {
    rejects(
        """
        @ExternalRefinementsFor("java.lang.String") interface Spec {
          String format(String format, Object... values);
        }
        interface Formatter { String apply(String pattern, Object value); }
        class Sample { void f() { Formatter formatter = String::format; }}
        """,
        Diagnostic.Kind.UNSUPPORTED);
  }
}
