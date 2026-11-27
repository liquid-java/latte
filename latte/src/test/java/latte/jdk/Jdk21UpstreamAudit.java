package latte.jdk;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import latte.analysis.Jdk21Coverage;
import latte.analysis.LatteAnalysis;
import latte.api.Diagnostic;
import latte.api.Jdk21Contracts;
import latte.refinement.LiquidTypeChecking;
import spoon.Launcher;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * Isolated worker for the optional OpenJDK source audit. Does not execute upstream code. Both modes
 * run both analysis passes, even when the upstream source contains no LiquidJava annotations.
 */
public final class Jdk21UpstreamAudit {
  private Jdk21UpstreamAudit() {}

  public record Use(int line, String target, String signature, String policy) {}

  public record Outcome(
      String status, List<Diagnostic> diagnostics, List<Use> uses, String failure) {}

  public static void main(String[] args) throws Exception {
    if (args.length != 4)
      throw new IllegalArgumentException(
          "Expected <source-root> <relative-file> <all|none> <output>");
    if (!List.of("all", "none").contains(args[2]))
      throw new IllegalArgumentException("Expected all or none");
    var root = Path.of(args[0]).toAbsolutePath().normalize();
    var source = root.resolve(args[1]).normalize();
    if (!source.startsWith(root) || !Files.isRegularFile(source))
      throw new IllegalArgumentException("Missing source within the supplied root: " + source);
    var outcome = analyze(root, source, args[2].equals("all"));
    Files.writeString(
        Path.of(args[3]), new GsonBuilder().setPrettyPrinting().create().toJson(outcome) + "\n");
  }

  public static Outcome analyze(Path root, Path source, boolean contracts) {
    var diagnostics = new ArrayList<Diagnostic>();
    var uses = new ArrayList<Use>();
    String stage = "model";
    try {
      var launcher = new Launcher();
      launcher.getEnvironment().setComplianceLevel(21);
      launcher.getEnvironment().setNoClasspath(true);
      launcher.getEnvironment().setCommentEnabled(false);
      launcher.addInputResource(source.toString());
      if (contracts) Jdk21Contracts.addTo(launcher, List.of("all"));
      var model = launcher.buildModel();
      stage = "ownership";
      diagnostics.addAll(new LatteAnalysis().analyze(model));
      for (var reference : model.getElements(new TypeFilter<>(CtExecutableReference.class))) {
        var position = reference.getPosition();
        if (!position.isValidPosition() && reference.isParentInitialized())
          position = reference.getParent().getPosition();
        if (!position.isValidPosition()
            || !position.getFile().toPath().toAbsolutePath().normalize().equals(source)) continue;
        int line = position.getLine();
        Jdk21Coverage.use(reference)
            .ifPresent(use -> uses.add(new Use(line, use.target(), use.signature(), use.policy())));
      }
      stage = "refinement";
      diagnostics.addAll(new LiquidTypeChecking().analyze(model));
      return new Outcome("analyzed", relative(root, diagnostics), List.copyOf(uses), null);
    } catch (RuntimeException | StackOverflowError | AssertionError failure) {
      return new Outcome(
          stage + "-error",
          relative(root, diagnostics),
          List.copyOf(uses),
          failure.getClass().getName() + ": " + failure.getMessage());
    }
  }

  private static List<Diagnostic> relative(Path root, List<Diagnostic> diagnostics) {
    return diagnostics.stream()
        .map(
            d -> {
              var path = Path.of(d.file()).toAbsolutePath().normalize();
              return new Diagnostic(
                  d.kind(),
                  d.message(),
                  d.detail(),
                  path.startsWith(root) ? root.relativize(path).toString() : d.file(),
                  d.line(),
                  d.column());
            })
        .distinct()
        .toList();
  }
}
