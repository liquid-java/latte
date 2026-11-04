package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.compiler.VirtualFile;

class ConstructorAliasesTest {
  private void stateFailureAt(Verification.Result result, String invocation) {
    var call =
        result.model().getElements(new TypeFilter<>(CtInvocation.class)).stream()
            .filter(node -> node.toString().equals(invocation))
            .findFirst()
            .orElseThrow();
    assertEquals(
        List.of(call.getPosition().getLine() + ":STATE"),
        result.diagnostics().stream().map(d -> d.line() + ":" + d.kind()).toList(),
        result.diagnostics().toString());
  }

  private Verification.Result check(String extra, String body) {
    return Verification.verify(
        new VirtualFile(
            """
        import specification.*;
        import liquidjava.specification.*;
        class Sample {
          @StateSet({"open", "closed"})
          static class Resource {
            @StateRefinement(to="open(this)") Resource() {}
            @StateRefinement(from="open(this)") void read() {}
            @StateRefinement(from="open(this)", to="closed(this)") void close() {}
          }
          %s
          static void use() {
            Resource original = new Resource();
            %s
          }
        }
        """
                .formatted(extra, body),
            "Sample.java"));
  }

  private static final String BOX =
      """
      static class Box {
        @Unique Resource value;
        Box(@Unique Resource value) { this.value = value; }
        @Borrowed Resource get() { return value; }
      }
      """;

  @Test
  void constructorAndGetterRetainTheArgumentsState() {
    var result = check(BOX, "Box box = new Box(original); box.get().read();");
    assertTrue(result.accepted(), result.diagnostics().toString());
  }

  @Test
  void changesThroughTheStoredAliasReachTheOriginal() {
    var result = check(BOX, "Box box = new Box(original);\nbox.get().close();\noriginal.read();");
    stateFailureAt(result, "original.read()");
  }

  @Test
  void transferredArgumentsKeepTheirStateInTheNewOwner() {
    var result =
        check(
            BOX.replace("Box(@Unique", "Box(@Free"),
            "Box box = new Box(original); box.get().read(); box.get().close();");
    assertTrue(result.accepted(), result.diagnostics().toString());
  }

  @Test
  void constructorMutationsInvalidateArgumentState() {
    var result =
        check(
            """
        static class Touch {
          Touch(@Borrowed @Refinement("open(_)") Resource resource) { resource.close(); }
        }
        """,
            "new Touch(original);\noriginal.read();");
    stateFailureAt(result, "original.read()");
  }

  @Test
  void methodsPropagateEffectsOfConstructorsTheyCall() {
    var result =
        check(
            """
        static class Touch {
          Touch(@Borrowed @Refinement("open(_)") Resource resource) { resource.close(); }
        }
        static void touch(@Borrowed @Refinement("open(_)") Resource resource) {
          new Touch(resource);
        }
        """,
            "touch(original);\noriginal.read();");
    stateFailureAt(result, "original.read()");
  }

  @Test
  void aTransferDoesNotRestoreStateInvalidatedByTheConstructor() {
    var result =
        check(
            """
        static class Box {
          @Unique Resource value;
          Box(@Free @Refinement("open(_)") Resource value) {
            this.value = value;
            value.close();
          }
          @Borrowed Resource get() { return value; }
        }
        """,
            "Box box = new Box(original);\nbox.get().read();");
    stateFailureAt(result, "box.get().read()");
  }

  @Test
  void transferThroughDelegationRetainsState() {
    var result =
        check(
            """
        static class Base {
          @Unique Resource value;
          Base(@Free Resource value) { this.value = value; }
          @Borrowed Resource get() { return value; }
        }
        static class Box extends Base {
          Box(@Free Resource value) { this(value, 0); }
          Box(@Free Resource value, int ignored) { super(value); }
        }
        """,
            "Box box = new Box(original); box.get().read(); box.get().close();");
    assertTrue(result.accepted(), result.diagnostics().toString());
  }
}
