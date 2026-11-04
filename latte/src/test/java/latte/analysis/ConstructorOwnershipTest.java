package latte.analysis;

import static latte.analysis.LatteAnalysisTest.check;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import latte.api.Diagnostic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import spoon.reflect.code.CtFieldWrite;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.visitor.filter.TypeFilter;

class ConstructorOwnershipTest {
  @Test
  void storedUniqueArgumentsRetainTheirIdentity() {
    var result =
        check(
            """
        import specification.*;
        class Box {
          @Unique Object value;
          Box(@Unique Object value) { this.value = value; }
          @Borrowed Object get() { return value; }
          static void use() {
            Object original = new Object();
            Box box = new Box(original);
            Object selected = box.get();
          }
        }
        """);
    assertEquals(List.of(), result.diagnostics());
    assertEquals(result.local("original").value(), result.local("selected").value());
    assertEquals(Permission.UNIQUE, result.local("selected").permission());
  }

  @Test
  void callsKeepIndependentBindingsAndDoNotOverwriteDeclarationFacts() {
    var result =
        check(
            """
        import specification.*;
        class Box {
          @Unique Object value;
          Box(@Unique Object value) { this.value = value; }
          static void use() {
            Object first = new Object(), second = new Object();
            Box a = new Box(first), b = new Box(second);
            Object one = a.value, two = b.value;
          }
        }
        """);
    assertEquals(List.of(), result.diagnostics());
    assertEquals(result.local("first").value(), result.local("one").value());
    assertEquals(result.local("second").value(), result.local("two").value());
    CtConstructor<?> ctor =
        result.model().getElements(new TypeFilter<>(CtConstructor.class)).stream()
            .filter(c -> !c.getParameters().isEmpty())
            .findFirst()
            .orElseThrow();
    var write = ctor.getElements(new TypeFilter<>(CtFieldWrite.class)).get(0);
    var assignment = write.getParent(spoon.reflect.code.CtAssignment.class);
    assertEquals(
        LatteFacts.on(ctor.getParameters().get(0)).orElseThrow().value(),
        LatteFacts.on(assignment.getAssignment()).orElseThrow().value());
  }

  @Test
  void freeArgumentsAreUsableThroughTheNewOwnerButConsumedInTheCaller() {
    var result =
        check(
            """
        import specification.*;
        class Box {
          @Unique Object value;
          Box(@Free Object value) { this.value = value; }
          static void use() {
            Object original = new Object(); Object oldAlias = original;
            Box box = new Box(original);
            Object owned = box.value;
            Object stale = oldAlias;
          }
        }
        """);
    assertEquals(Permission.UNIQUE, result.local("owned").permission());
    assertEquals(Permission.BOTTOM, result.local("stale").permission());
    assertTrue(result.diagnostics().stream().anyMatch(d -> d.message().contains("consumed")));
  }

  @Test
  void constructorStoresUpdateBorrowedArguments() {
    var result =
        check(
            """
        import specification.*;
        class Cell { @Unique Object value; }
        class Store {
          Store(@Borrowed Cell cell, @Unique Object value) { cell.value = value; }
          static void use() {
            Cell cell = new Cell(); Object original = new Object();
            new Store(cell, original);
            Object selected = cell.value;
          }
        }
        """);
    assertEquals(List.of(), result.diagnostics());
    assertEquals(result.local("original").value(), result.local("selected").value());
  }

  @Test
  void thisAndSuperDelegationPreserveArgumentAliases() {
    var result =
        check(
            """
        import specification.*;
        class Base {
          @Unique Object value;
          Base(@Unique Object value) { this.value = value; }
        }
        class Box extends Base {
          Box(@Unique Object value) { this(value, 0); }
          Box(@Unique Object value, int ignored) { super(value); }
          static void use() {
            Object original = new Object(); Box box = new Box(original);
            Object selected = box.value;
          }
        }
        """);
    assertEquals(List.of(), result.diagnostics());
    assertEquals(result.local("original").value(), result.local("selected").value());
  }

  @Test
  void earlyReturnsRetainTheDefaultFieldValue() {
    var result =
        check(
            """
        import specification.*;
        class Box {
          @Unique Object value;
          Box(boolean stop, @Unique Object value) {
            if (stop) return;
            this.value = value;
          }
          static void use(boolean stop) {
            Object original = new Object(); Box box = new Box(stop, original);
            Object selected = box.value;
          }
        }
        """);
    assertEquals(List.of(), result.diagnostics());
    assertEquals(
        result.local("original").value().objects(), result.local("selected").value().objects());
    assertTrue(result.local("selected").value().nullable());
  }

  @Test
  void constructorFieldInitializersRunBeforeTheBody() {
    var result =
        check(
            """
        import specification.*;
        class Box {
          @Unique Object value = new Object();
          @Unique Object saved;
          Box(@Unique Object replacement) { saved = value; value = replacement; }
          static void use() {
            Object original = new Object(); Box box = new Box(original);
            Object selected = box.value, initial = box.saved;
          }
        }
        """);
    assertEquals(List.of(), result.diagnostics());
    assertEquals(result.local("original").value(), result.local("selected").value());
    assertFalse(result.local("initial").value().nullable());
    assertNotEquals(result.local("original").value(), result.local("initial").value());
  }

  @ParameterizedTest
  @ValueSource(strings = {"new Object()", "fresh()"})
  void differentConstructionsKeepTheirInternalAllocationsSeparate(String initializer) {
    var result =
        check(
            """
        import specification.*;
        class Box {
          @Unique Object value = %s;
          @Free static Object fresh() { return new Object(); }
          static void use() {
            Box a = new Box(), b = new Box();
            Object first = a.value, second = b.value;
          }
        }
        """
                .formatted(initializer));
    assertEquals(List.of(), result.diagnostics());
    assertNotEquals(result.local("first").value(), result.local("second").value());
    assertEquals(Permission.UNIQUE, result.local("first").permission());
    assertEquals(Permission.UNIQUE, result.local("second").permission());
  }

  @Test
  void constructorParametersEnforceTheSameDistinctnessAsMethods() {
    var result =
        check(
            """
        import specification.*;
        class Pair {
          Pair(@Borrowed Object a, @Borrowed Object b) {}
          static void use() { Object value = new Object(); new Pair(value, value); }
        }
        """);
    assertTrue(result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.PERMISSION));
  }

  @Test
  void transferredArraysRetainTheirElementAliasesUnderTheNewOwner() {
    var result =
        check(
            """
        import specification.*;
        class Box {
          @Unique Object[] values;
          Box(@Free Object[] values) { this.values = values; }
          static void use() {
            Object original = new Object(); Object[] array = {original, original};
            Box box = new Box(array);
            Object first = box.values[0], second = box.values[1];
            Object stale = original;
          }
        }
        """);
    assertEquals(result.local("first").value(), result.local("second").value());
    assertTrue(result.local("first").permission().refinable());
    assertEquals(Permission.BOTTOM, result.local("stale").permission());
    assertTrue(result.diagnostics().stream().anyMatch(d -> d.message().contains("consumed")));
  }

  @ParameterizedTest
  @ValueSource(strings = {"int value = change();", "{ change(); }"})
  void constructorEffectsIncludeInstanceInitialization(String initializer) {
    var result =
        check(
            """
        class Sample {
          static int global;
          static int change() { global = 1; return 1; }
          %s
          Sample() {}
          static void use() { new Sample(); }
        }
        """
                .formatted(initializer));
    assertEquals(List.of(), result.diagnostics());
    var type = result.model().getAllTypes().iterator().next();
    assertTrue(MutationEffects.on(type.getMethodsByName("use").get(0)).globals());
    assertTrue(
        MutationEffects.on(result.model().getElements(new TypeFilter<>(CtConstructor.class)).get(0))
            .globals());
  }

  @Test
  void constructorEffectsExcludeDeferredInitializerBodies() {
    var result =
        check(
            """
        class Sample {
          static int global;
          Runnable later = () -> { global = 1; };
          Sample() {}
        }
        """);
    var constructor = result.model().getElements(new TypeFilter<>(CtConstructor.class)).get(0);
    assertFalse(MutationEffects.on(constructor).globals());
  }

  @Test
  void borrowedConstructorParametersCannotEscapeThroughSharedFields() {
    var result =
        check(
            """
        import specification.*;
        class Box {
          Object value;
          Box(@Borrowed Object value) { this.value = value; }
        }
        """);
    assertTrue(result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.PERMISSION));
  }

  @Test
  void recursiveConstructionFailsExplicitlyInsteadOfInventingAHeapSummary() {
    var result =
        check(
            """
        import specification.*;
        class Node {
          @Unique Node next;
          Node(int count) { if (count > 0) next = new Node(count - 1); }
        }
        """);
    assertTrue(
        result.diagnostics().stream()
            .anyMatch(
                d ->
                    d.kind() == Diagnostic.Kind.UNSUPPORTED
                        && d.message().contains("Recursive constructor")),
        result.diagnostics().toString());
  }
}
