package latte.api;

/** Minimal command-line entry point; nonzero exit status means verification did not succeed. */
public final class Main {
  private Main() {}

  public static void main(String[] arguments) {
    if (java.util.Arrays.stream(arguments).noneMatch(a -> !a.equals("--explain"))) {
      System.err.println("Usage: latte [--explain] <Java file or source directory>...");
      System.exit(2);
    }
    boolean explain = java.util.Arrays.asList(arguments).contains("--explain");
    boolean accepted = true;
    for (String path : arguments) {
      if (path.equals("--explain")) continue;
      try {
        var result = Verification.verify(path);
        accepted &= result.accepted();
        result
            .diagnostics()
            .forEach(
                d ->
                    System.err.printf(
                        "%s:%d:%d: %s: %s%n",
                        d.file(), d.line(), d.column(), d.kind(), d.message()));
        if (explain)
          result.diagnostics().stream()
              .filter(d -> !d.detail().isEmpty())
              .forEach(d -> System.err.println(d.detail()));
        if (result.accepted()) System.out.println(path + ": verified");
      } catch (RuntimeException failure) {
        accepted = false;
        System.err.println(path + ": could not analyze source: " + failure.getMessage());
      }
    }
    if (!accepted) System.exit(1);
  }
}
