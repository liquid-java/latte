package latte.compatibility;

import com.google.gson.GsonBuilder;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import latte.api.*;

/** Executes the untouched upstream fixtures and records every mismatch, including crashes. */
public final class CompatibilityAudit {
  public static final Path ROOT = Path.of("src/test/resources/liquidjava/testSuite");

  public record Expectation(String file, int line, String title) {}

  public record Outcome(
      String input,
      List<Expectation> expected,
      List<Diagnostic> actual,
      String crash,
      boolean matches) {}

  public static List<Path> inputs() throws Exception {
    try (var files = Files.walk(ROOT)) {
      var paths = files.toList();
      var result = new ArrayList<Path>();
      for (var path : paths) {
        if (Files.isDirectory(path) && !path.equals(ROOT)) {
          try (var children = Files.list(path)) {
            if (children.noneMatch(Files::isDirectory)) result.add(path);
          }
        } else if (path.toString().endsWith(".java")) {
          boolean parentIsLeaf;
          try (var children = Files.list(path.getParent())) {
            parentIsLeaf = children.noneMatch(Files::isDirectory);
          }
          if (path.getParent().equals(ROOT) || !parentIsLeaf) result.add(path);
        }
      }
      return result.stream().sorted().toList();
    }
  }

  public static Outcome run(Path path) throws Exception {
    var expected = new ArrayList<Expectation>();
    List<Path> sources;
    if (Files.isDirectory(path)) {
      try (var files = Files.walk(path)) {
        sources = files.filter(p -> p.toString().endsWith(".java")).toList();
      }
    } else sources = List.of(path);
    var pattern =
        Pattern.compile("//\\s*Expect:\\s*(.*?\\b(?:Error|Warning)\\b)", Pattern.CASE_INSENSITIVE);
    for (var source : sources) {
      var lines = Files.readAllLines(source);
      for (int line = 0; line < lines.size(); line++) {
        var matcher = pattern.matcher(lines.get(line));
        while (matcher.find())
          expected.add(
              new Expectation(source.getFileName().toString(), line + 1, matcher.group(1)));
      }
    }
    try {
      var result = Verification.verify(path.toString());
      var actual = result.diagnostics();
      var remaining = new ArrayList<>(actual);
      boolean matches = expected.size() == actual.size();
      for (var expectation : expected) {
        var found =
            remaining.stream()
                .filter(
                    d ->
                        Path.of(d.file()).getFileName().toString().equals(expectation.file())
                            && d.line() == expectation.line()
                            && compatible(expectation.title(), d.kind()))
                .findFirst();
        if (found.isEmpty()) matches = false;
        else remaining.remove(found.get());
      }
      matches &= remaining.isEmpty();
      return new Outcome(ROOT.relativize(path).toString(), expected, actual, null, matches);
    } catch (RuntimeException failure) {
      return new Outcome(
          ROOT.relativize(path).toString(),
          expected,
          List.of(),
          failure.getClass().getSimpleName() + ": " + failure.getMessage(),
          false);
    }
  }

  private static boolean compatible(String title, Diagnostic.Kind kind) {
    return switch (kind) {
      case REFINEMENT -> title.equalsIgnoreCase("Refinement Error");
      case ARGUMENT -> title.equalsIgnoreCase("Argument Mismatch Error");
      case STATE -> title.equalsIgnoreCase("State Refinement Error");
      case SYNTAX ->
          Set.of("syntax error", "invalid refinement error", "not found error")
              .contains(title.toLowerCase(Locale.ROOT));
      default -> false;
    };
  }

  public static void main(String[] arguments) throws Exception {
    var results = new ArrayList<Outcome>();
    for (var input : inputs()) {
      var outcome = run(input);
      results.add(outcome);
      System.out.printf("%s %s%n", outcome.matches() ? "MATCH" : "MISMATCH", outcome.input());
    }
    Files.createDirectories(Path.of("target"));
    Files.writeString(
        Path.of("target/liquidjava-compatibility.json"),
        new GsonBuilder().setPrettyPrinting().create().toJson(results));
    long matched = results.stream().filter(Outcome::matches).count();
    System.out.printf(
        "Exact matches: %d/%d. Full results: target/liquidjava-compatibility.json%n",
        matched, results.size());
    if (arguments.length > 0 && arguments[0].equals("--check")) {
      var supported =
          Files.readAllLines(Path.of("src/test/resources/liquidjava/supported-inputs.txt"));
      // These old negative cases are valid with precise local values; see docs/compatibility.md.
      var intentionalAcceptance =
          Set.of("ErrorDependentRefinement.java", "ErrorInstanceVarInRefinement.java");
      var regressions =
          results.stream()
              .filter(
                  outcome ->
                      outcome.crash() != null
                          || supported.contains(outcome.input()) && !outcome.matches()
                          || !outcome.expected().isEmpty()
                              && outcome.actual().isEmpty()
                              && !intentionalAcceptance.contains(outcome.input()))
              .toList();
      if (!regressions.isEmpty())
        throw new AssertionError("Compatibility safety regressions: " + regressions);
    }
    if (arguments.length > 0 && arguments[0].equals("--strict") && matched != results.size())
      System.exit(1);
  }
}
