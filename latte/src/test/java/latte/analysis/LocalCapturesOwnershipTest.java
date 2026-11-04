package latte.analysis;

import static latte.analysis.LatteAnalysisTest.check;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LocalCapturesOwnershipTest {
  @Test
  void capturesAreReachableWithoutChangingTheirPermission() {
    var result =
        check(
            """
        class Sample { void f() {
          Object resource = new Object(); Runnable callback = () -> { Object used = resource; };
          Object retained = resource;
        }}
        """);
    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    var callback = result.local("callback");
    assertTrue(
        callback
            .after()
            .reachable(callback.value())
            .containsAll(result.local("resource").value().objects()));
    assertEquals(Permission.FREE, result.local("retained").permission());
  }

  @Test
  void escapingAWrapperEscapesCapturesTransitively() {
    var result =
        check(
            """
        import specification.*;
        class Box { @Unique Runnable callback; Box(@Unique Runnable c) { callback = c; } }
        class Sample { static Object published; void f() {
          Object resource = new Object(); Runnable callback = () -> { Object used = resource; };
          Box box = new Box(callback); published = box; Object escaped = resource;
        }}
        """);
    assertTrue(result.diagnostics().isEmpty(), result.diagnostics().toString());
    assertEquals(Permission.SHARED, result.local("escaped").permission());
  }

  @Test
  void boundMethodReferenceCapturesTheEvaluatedReceiver() {
    var result =
        check(
            """
        import specification.*;
        class Resource { void read() {} }
        class Box { @Unique Resource value; }
        class Sample { static Object published; void f() {
          Resource original = new Resource(); Box box = new Box(); box.value = original;
          Runnable callback = box.value::read; box.value = new Resource();
          published = callback; Object escaped = original, retained = box.value;
        }}
        """);
    assertEquals(Permission.SHARED, result.local("escaped").permission());
    assertEquals(Permission.UNIQUE, result.local("retained").permission());
  }

  @Test
  void returningAClosureWithBorrowedCapturesIsRejected() {
    var result =
        check(
            """
        import specification.*;
        class Sample { @Free Runnable f(@Borrowed Object resource) {
          return () -> { Object used = resource; };
        }}
        """);
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.message().contains("capturing a borrowed")),
        result.diagnostics().toString());
  }
}
