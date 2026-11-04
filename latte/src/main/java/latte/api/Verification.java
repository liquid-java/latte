package latte.api;

import java.util.*;
import latte.analysis.LatteAnalysis;
import latte.refinement.LiquidTypeChecking;
import spoon.Launcher;
import spoon.compiler.SpoonResource;
import spoon.reflect.CtModel;

/** One Spoon model, two passes. The returned model retains Latte metadata for tools and tests. */
public final class Verification {
  private Verification() {}

  public record Result(CtModel model, List<Diagnostic> diagnostics) {
    public Result {
      diagnostics = List.copyOf(diagnostics);
    }

    public boolean accepted() {
      return diagnostics.isEmpty();
    }
  }

  public static Result verify(String path) {
    return verify(List.of(path), List.of());
  }

  /**
   * Analyze application sources and external specifications together, with optional library JARs.
   */
  public static Result verify(List<String> paths, List<String> classpath) {
    var launcher = launcher();
    if (!classpath.isEmpty())
      launcher.getEnvironment().setSourceClasspath(classpath.toArray(String[]::new));
    paths.forEach(launcher::addInputResource);
    return verify(launcher);
  }

  public static Result verify(SpoonResource resource) {
    var launcher = launcher();
    launcher.addInputResource(resource);
    return verify(launcher);
  }

  public static Result verify(Launcher launcher) {
    var model = launcher.buildModel();
    var diagnostics = new ArrayList<>(new LatteAnalysis().analyze(model));
    boolean hasRefinements =
        model
            .getElements(
                new spoon.reflect.visitor.filter.TypeFilter<>(
                    spoon.reflect.declaration.CtAnnotation.class))
            .stream()
            .anyMatch(
                a ->
                    Set.of(
                            "Refinement",
                            "StateRefinement",
                            "StateSet",
                            "Ghost",
                            "RefinementAlias",
                            "RefinementPredicate",
                            "PrivateRefinement",
                            "ExternalRefinementsFor")
                        .contains(a.getAnnotationType().getSimpleName()));
    if (hasRefinements) diagnostics.addAll(new LiquidTypeChecking().analyze(model));
    return new Result(model, diagnostics.stream().distinct().toList());
  }

  private static Launcher launcher() {
    var launcher = new Launcher();
    launcher.getEnvironment().setNoClasspath(true);
    launcher.getEnvironment().setComplianceLevel(17);
    launcher.getEnvironment().setCommentEnabled(false);
    return launcher;
  }
}
