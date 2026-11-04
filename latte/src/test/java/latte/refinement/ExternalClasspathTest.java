package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import javax.tools.ToolProvider;
import latte.api.Verification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExternalClasspathTest {
  @TempDir Path directory;

  @Test
  void readsLibraryJarAndSeparateSpecificationsWithoutInitializingLibrary() throws Exception {
    var source = directory.resolve("Counter.java");
    Files.writeString(
        source,
        """
        package library;
        public class Counter {
          static { if (true) throw new AssertionError("Library must not be initialized"); }
          public static int increment(int number) { return number + 1; }
        }
        """);
    var classes = Files.createDirectory(directory.resolve("classes"));
    assertEquals(
        0,
        ToolProvider.getSystemJavaCompiler()
            .run(null, null, null, "-d", classes.toString(), source.toString()));
    var jar = directory.resolve("library.jar");
    try (var output = new JarOutputStream(Files.newOutputStream(jar))) {
      output.putNextEntry(new JarEntry("library/Counter.class"));
      Files.copy(classes.resolve("library/Counter.class"), output);
      output.closeEntry();
    }
    var specification = directory.resolve("Spec.java");
    Files.writeString(
        specification,
        """
        import liquidjava.specification.*;
        @ExternalRefinementsFor("library.Counter") interface Spec {
          @Refinement("_ == n + 1") int increment(@Refinement("_ < 2147483647") int n);
        }
        """);
    var application = directory.resolve("Application.java");
    Files.writeString(
        application,
        """
        import liquidjava.specification.*;
        class Application { void f() {
          @Refinement("_ == 4") int result = library.Counter.increment(3);
        }}
        """);
    var result =
        Verification.verify(
            List.of(specification.toString(), application.toString()), List.of(jar.toString()));
    assertTrue(result.accepted(), result.diagnostics().toString());

    var process =
        new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-cp",
                System.getProperty("java.class.path"),
                "latte.api.Main",
                "--classpath",
                jar.toString(),
                specification.toString(),
                application.toString())
            .redirectErrorStream(true)
            .start();
    assertTrue(process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS));
    var output =
        new String(
            process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    assertEquals(0, process.exitValue(), output);
    assertTrue(output.contains("verified"), output);
  }
}
