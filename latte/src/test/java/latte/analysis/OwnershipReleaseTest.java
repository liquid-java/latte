package latte.analysis;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import latte.api.Diagnostic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import spoon.Launcher;
import spoon.reflect.code.*;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.compiler.VirtualFile;

class OwnershipReleaseTest {
  private static final String PREFIX =
      """
      import specification.*;
      final class Box implements AutoCloseable {
        @Unique Object value;
        Box(@Unique Object value) { this.value = value; }
        void accept(@Free Object value) {}
        public void close() {}
      }
      class Sample {
        @Unique Box head;
        static void consume(@Free Object value) {}
        static void inspect(@Borrowed Object value) {}
        static void publish(Object value) {}
        static void both(@Free Object value, @Borrowed Box owner) {}
        @Free static Object move(@Free Object value) { return value; }
        static void nested(@Borrowed Box owner, @Free Object value) {}
        static boolean condition(@Free Object value) { return true; }
      """;

  private static LatteAnalysisTest.Checked check(String methods) {
    return LatteAnalysisTest.check(PREFIX + methods + "}");
  }

  static Stream<String> unobservableOwners() {
    return Stream.of(
        "void f() { Box box = new Box(new Object()); consume(box.value); }",
        "void f() { Box box = new Box(new Object()); Object value = box.value; consume(value); }",
        "void f() { Box box = new Box(new Object()); Box alias = box; consume(box.value); }",
        "void f() { Box box = new Box(new Object()); Object value = box.value; box = null; consume(value); inspect(box); }",
        "@Free Object f() { Box box = new Box(new Object()); return box.value; }",
        "@Free Object f(@Free Box box) { return box.value; }",
        "@Free Object f() { Box old = head; Object value = old.value; head = null; return value; }",
        "void f() { Box box = new Box(new Object()); Runnable dead = () -> inspect(box); consume(box.value); }",
        "void f(boolean c) { if(c) { Box box = new Box(new Object()); consume(box.value); } }",
        "void f() { Box box = new Box(new Object()); Box outer = new Box(box); consume(box.value); }");
  }

  @ParameterizedTest
  @MethodSource("unobservableOwners")
  void infersReleaseOnlyAfterTheOwnerBecomesUnobservable(String source) {
    var result = check(source);
    assertEquals(java.util.List.of(), result.diagnostics());
    assertTrue(
        result.model().getElements(new TypeFilter<>(CtStatement.class)).stream()
            .anyMatch(statement -> OwnershipRelease.on(statement).isPresent()));
  }

  static Stream<String> observableOwners() {
    return Stream.of(
        "void f() { Box box = new Box(new Object()); consume(box.value); inspect(box); }",
        "void f() { Box box = new Box(new Object()); Box alias = box; consume(box.value); inspect(alias); }",
        "void f() { Box box = new Box(new Object()); Box outer = new Box(box); consume(box.value); inspect(outer); }",
        "@Free Object f() { return head.value; }",
        "@Free Object f(@Borrowed Box box) { return box.value; }",
        "@Free Object f(@Unique Box box) { return box.value; }",
        "@Free Object f(Box box) { return box.value; }",
        "void f() { Box box = new Box(new Object()); publish(box); consume(box.value); }",
        "void f() { Box box = new Box(new Object()); Runnable later = () -> inspect(box); consume(box.value); later.run(); }",
        "void f() { Box box = new Box(new Object()); consume(box.value); Runnable later = () -> inspect(box); }",
        "void f() { Box box = new Box(new Object()); Runnable escaped = () -> inspect(box); publish(escaped); consume(box.value); }",
        "void f(boolean c) { Box box = new Box(new Object()); consume(box.value); if(c) inspect(box); }",
        "void f() { Box box = new Box(new Object()); try { consume(box.value); } finally { inspect(box); } }",
        "@Free Object f() { Box box = new Box(new Object()); try { return box.value; } finally { inspect(box); } }",
        "void f() { Box box = new Box(new Object()); try { consume(box.value); } catch(RuntimeException ex) { inspect(box); } }",
        "void f() { Box box = new Box(new Object()); while(box.value != null) { consume(box.value); } }",
        "void f() { Box box = new Box(new Object()); try(Box resource = box) { consume(box.value); } }",
        "void f() { Box box = new Box(new Object()); synchronized(box) { consume(box.value); } }",
        "void f() { Box box = new Box(new Object()); box.accept(box.value); }",
        "void f() { Box box = new Box(new Object()); both(box.value, box); }",
        "void f() { Box box = new Box(new Object()); nested(box, move(box.value)); }",
        "void f() { Box box = new Box(new Object()); if(condition(box.value)) inspect(box); }",
        "void f(@Unique Box box) { Box old = box; box = null; consume(old.value); }",
        "void f() { Box box = new Box(new Object()); class Captor { void read() { inspect(box); } } Captor captor = new Captor(); consume(box.value); captor.read(); }");
  }

  @ParameterizedTest
  @MethodSource("observableOwners")
  void keepsObservableOwnershipEdges(String source) {
    var result = check(source);
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.PERMISSION),
        result.diagnostics().toString());
    assertTrue(
        result.model().getElements(new TypeFilter<>(CtStatement.class)).stream()
            .noneMatch(statement -> OwnershipRelease.on(statement).isPresent()));
  }

  @Test
  void consumedChildAliasesRemainUnusable() {
    var result =
        check(
            "void f() { Box box = new Box(new Object()); Object alias = box.value; consume(alias); inspect(alias); }");
    assertTrue(result.diagnostics().stream().anyMatch(d -> d.message().contains("consumed")));
  }

  @Test
  void aSurvivingOwnerOfADescendantPreventsTheMove() {
    var result =
        check(
            "void f() { Object leaf = new Object(); Box inner = new Box(leaf); Box owner = new Box(inner); Box observer = new Box(leaf); consume(owner.value); inspect(observer); }");
    assertTrue(result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.PERMISSION));
  }

  @Test
  void finalizationCanStillObserveAnAbandonedOwner() {
    var result =
        LatteAnalysisTest.check(
            """
        import specification.*;
        class Box {
          @Unique Object value = new Object();
          static Object saved;
          protected void finalize() { saved = value; }
          static void consume(@Free Object value) {}
          static void f() { Box box = new Box(); consume(box.value); }
        }
        """);
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.message().contains("FREE but got UNIQUE")));
  }

  @Test
  void aReferenceBackToTheOwnerKeepsItsFieldsObservable() {
    var result =
        LatteAnalysisTest.check(
            """
        import specification.*;
        class Node {
          @Unique Node next;
          static void consume(@Free Node node) {}
          static void f() {
            Node owner = new Node(); Node child = new Node();
            owner.next = child; child.next = owner;
            consume(child);
          }
        }
        """);
    assertTrue(result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.PERMISSION));
  }

  @Test
  void anOpaqueResultCanHaveAFinalizingSubtype() {
    var result =
        LatteAnalysisTest.check(
            """
        import specification.*;
        class Box { @Unique Object value = new Object(); }
        class FinalizingBox extends Box {
          static Object saved;
          protected void finalize() { saved = value; }
        }
        class Sample {
          @Free static Box factory() { return new FinalizingBox(); }
          static void consume(@Free Object value) {}
          static void f() { Box box = factory(); consume(box.value); }
        }
        """);
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.message().contains("FREE but got UNIQUE")));
  }

  @Test
  void movingAnOwnerDoesNotRemoveItsFinalizer() {
    var result =
        LatteAnalysisTest.check(
            """
        import specification.*;
        class Box {
          @Unique Object value = new Object();
          static Object saved;
          protected void finalize() { saved = value; }
        }
        class Holder {
          @Unique Box box;
          Holder(@Free Box box) { this.box = box; }
        }
        class Sample {
          static void consume(@Free Object value) {}
          static void f() { Holder holder = new Holder(new Box()); consume(holder.box.value); }
        }
        """);
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.message().contains("FREE but got UNIQUE")));
  }

  @Test
  void recordsForgottenEdgesWithoutChangingJavaOrEarlierFacts() {
    var launcher = new Launcher();
    launcher.getEnvironment().setNoClasspath(true);
    launcher.addInputResource(
        new VirtualFile(
            PREFIX + "void f() { Box box = new Box(new Object()); consume(box.value); }}",
            "Sample.java"));
    var model = launcher.buildModel();
    String before = model.getAllTypes().toString();
    var analysis = new LatteAnalysis();
    assertTrue(analysis.analyze(model).isEmpty());
    CtInvocation<?> call =
        model.getElements(new TypeFilter<>(CtInvocation.class)).stream()
            .filter(invocation -> invocation.getExecutable().getSimpleName().equals("consume"))
            .findFirst()
            .orElseThrow();
    var release = OwnershipRelease.on(call).orElseThrow();
    var facts = LatteFacts.on(call).orElseThrow();
    assertFalse(release.fields().isEmpty());
    release
        .fields()
        .forEach(
            field -> {
              assertTrue(facts.before().heap().containsKey(field));
              assertFalse(facts.after().heap().containsKey(field));
            });
    assertEquals(before, model.getAllTypes().toString());
    assertTrue(
        model.getElements(new TypeFilter<>(CtAssignment.class)).stream()
            .noneMatch(
                assignment ->
                    assignment.getAssignment() instanceof CtLiteral<?> literal
                        && literal.getValue() == null));
    assertTrue(analysis.analyze(model).isEmpty());
    assertEquals(release, OwnershipRelease.on(call).orElseThrow());
    var parameter = call.getExecutable().getDeclaration().getParameters().get(0);
    parameter.removeAnnotation(Annotations.named(parameter, "Free").get(0));
    assertTrue(analysis.analyze(model).isEmpty());
    assertTrue(OwnershipRelease.on(call).isEmpty(), "Reanalysis must discard stale release facts");
  }
}
