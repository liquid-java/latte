package latte.compatibility;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Every promoted upstream fixture is a required, unchanged regression test. */
class ImportedSuiteTest {
  static Stream<String> supported() throws Exception {
    return Files.readAllLines(Path.of("src/test/resources/liquidjava/supported-inputs.txt"))
        .stream()
        .filter(line -> !line.isBlank() && !line.startsWith("#"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("supported")
  void matchesUpstreamExpectationsExactly(String input) throws Exception {
    var outcome = CompatibilityAudit.run(CompatibilityAudit.ROOT.resolve(input));
    assertTrue(outcome.matches(), outcome.toString());
  }
}
