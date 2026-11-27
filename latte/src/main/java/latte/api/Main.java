package latte.api;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/** Minimal command-line entry point; nonzero exit status means verification did not succeed. */
public final class Main {
  private Main() {}

  public static void main(String[] arguments) {
    var paths = new ArrayList<String>();
    var classpath = new ArrayList<String>();
    var jdkPackages = new ArrayList<String>();
    boolean explain = false;
    for (int i = 0; i < arguments.length; i++) {
      if (arguments[i].equals("--explain")) explain = true;
      else if (arguments[i].equals("--jdk")
          && i + 1 < arguments.length
          && !arguments[i + 1].startsWith("-")) {
        var selection = Arrays.stream(arguments[++i].split(",", -1)).map(String::strip).toList();
        if (selection.stream().anyMatch(String::isEmpty)) {
          System.err.println("--jdk requires package names or all");
          System.exit(2);
          return;
        }
        jdkPackages.addAll(selection);
      } else if ((arguments[i].equals("--classpath") || arguments[i].equals("-cp"))
          && i + 1 < arguments.length)
        classpath.addAll(Arrays.asList(arguments[++i].split(Pattern.quote(File.pathSeparator))));
      else if (arguments[i].startsWith("-")) {
        System.err.println("Unknown option or missing value: " + arguments[i]);
        System.exit(2);
        return;
      } else paths.add(arguments[i]);
    }
    if (paths.isEmpty()) {
      System.err.println(
          "Usage: latte [--explain] [--classpath <JARs or directories>] [--jdk <package,...|all>] <Java file or source directory>...");
      System.exit(2);
      return;
    }
    boolean accepted = true;
    try {
      var result = Verification.verify(paths, classpath, jdkPackages);
      accepted = result.accepted();
      result
          .diagnostics()
          .forEach(
              d ->
                  System.err.printf(
                      "%s:%d:%d: %s: %s%n", d.file(), d.line(), d.column(), d.kind(), d.message()));
      if (explain)
        result.diagnostics().stream()
            .filter(d -> !d.detail().isEmpty())
            .forEach(d -> System.err.println(d.detail()));
      if (result.accepted()) System.out.println(String.join(", ", paths) + ": verified");
    } catch (RuntimeException failure) {
      accepted = false;
      System.err.println(
          String.join(", ", paths) + ": could not analyze source: " + failure.getMessage());
    }
    if (!accepted) System.exit(1);
  }
}
