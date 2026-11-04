package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import latte.api.Verification;
import org.junit.jupiter.api.Test;

class ExamplesTest {
  @Test
  void everyExampleHasExactlyItsDocumentedDiagnostics() throws Exception {
    try (var files = Files.list(Path.of("../examples/refinements"))) {
      for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        var expected = new ArrayList<String>();
        var lines = Files.readAllLines(file);
        for (int i = 0; i < lines.size(); i++)
          if (lines.get(i).contains("// Expect: "))
            expected.add((i + 1) + ":" + lines.get(i).split("// Expect: ")[1].trim());
        var result = Verification.verify(file.toString());
        var actual =
            result.diagnostics().stream().map(d -> d.line() + ":" + d.kind()).sorted().toList();
        assertEquals(
            expected.stream().sorted().toList(), actual, file + ": " + result.diagnostics());
      }
    }
  }
}
