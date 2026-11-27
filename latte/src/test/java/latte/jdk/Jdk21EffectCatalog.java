package latte.jdk;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import latte.analysis.Annotations;
import spoon.Launcher;
import spoon.reflect.declaration.CtMethod;

/** Reproducible effect/ownership ledger; annotations, not method names, determine precision. */
final class Jdk21EffectCatalog {
  private static final Gson JSON =
      new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();

  record Summary(
      String target,
      String signature,
      List<String> modifies,
      Map<String, String> returnsView,
      String resultOwnership,
      Map<String, String> parameterOwnership,
      String evidence) {}

  record PackageEffects(
      String name,
      int apiMembers,
      long readOnlyFootprints,
      long mutatingFootprints,
      long viewResults,
      long withoutFootprint,
      List<Summary> summaries) {}

  record Catalog(int javaRelease, String omittedFootprintPolicy, List<PackageEffects> packages) {}

  private Jdk21EffectCatalog() {}

  static Catalog collect(Path root) throws IOException {
    var manifest =
        JsonParser.parseString(Files.readString(root.resolve("catalog.json")))
            .getAsJsonObject()
            .getAsJsonArray("specifications");
    var entries = new HashMap<String, JsonObject>();
    manifest.forEach(
        e -> entries.put(e.getAsJsonObject().get("target").getAsString(), e.getAsJsonObject()));
    var launcher = new Launcher();
    launcher.getEnvironment().setNoClasspath(true);
    launcher.getEnvironment().setComplianceLevel(21);
    launcher.addInputResource(root.toString());
    var grouped = new TreeMap<String, List<Summary>>();
    for (var type : launcher.buildModel().getAllTypes()) {
      if (!Annotations.has(type, "ExternalRefinementsFor")) continue;
      var target = Annotations.strings(type, "ExternalRefinementsFor", "value").getFirst();
      var entry = Objects.requireNonNull(entries.get(target), target);
      var summaries =
          grouped.computeIfAbsent(entry.get("package").getAsString(), p -> new ArrayList<>());
      for (var method : type.getMethods()) {
        boolean explicit = Annotations.has(method, "Modifies");
        List<String> footprint = explicit ? Annotations.strings(method, "Modifies", "value") : null;
        var view = new TreeMap<String, String>();
        if (Annotations.has(method, "ReturnsView"))
          for (var key : List.of("of", "from", "toExclusive"))
            view.put(key, Annotations.strings(method, "ReturnsView", key).getFirst());
        var parameters = new TreeMap<String, String>();
        method
            .getParameters()
            .forEach(
                p ->
                    parameters.put(
                        p.getSimpleName(),
                        p.getType().isPrimitive()
                            ? "scalar"
                            : Annotations.permission(p).name().toLowerCase(Locale.ROOT)));
        var signature = signature(target, method);
        String ownership =
            signature.startsWith("<init>(")
                ? "fresh-receiver"
                : method.getType().isPrimitive()
                    ? "scalar"
                    : !view.isEmpty()
                        ? "derived-from-backing"
                        : Annotations.permission(method).name().toLowerCase(Locale.ROOT);
        String evidence = explicit ? "effectReview" : "conservativeEffectReason";
        if (!entry.has(evidence))
          throw new IllegalStateException("Missing " + evidence + ": " + target + "#" + signature);
        summaries.add(
            new Summary(
                target,
                signature,
                footprint,
                view.isEmpty() ? null : view,
                ownership,
                parameters,
                entry.get(evidence).getAsString()));
      }
    }
    var inventory =
        JsonParser.parseString(Files.readString(root.resolve("inventory.json")))
            .getAsJsonObject()
            .getAsJsonArray("packages");
    var packages = new ArrayList<PackageEffects>();
    for (var value : inventory) {
      var entry = value.getAsJsonObject();
      var name = entry.get("name").getAsString();
      var summaries =
          grouped.getOrDefault(name, List.of()).stream()
              .sorted(Comparator.comparing(Summary::target).thenComparing(Summary::signature))
              .toList();
      long readOnly =
          summaries.stream().filter(s -> s.modifies() != null && s.modifies().isEmpty()).count();
      long writes =
          summaries.stream().filter(s -> s.modifies() != null && !s.modifies().isEmpty()).count();
      int members = entry.get("members").getAsInt();
      packages.add(
          new PackageEffects(
              name,
              members,
              readOnly,
              writes,
              summaries.stream().filter(s -> s.returnsView() != null).count(),
              members - readOnly - writes,
              summaries));
    }
    return new Catalog(
        21,
        "Omitted @Modifies means unknown writes, including globals, on normal and exceptional exits. "
            + "It never means an empty footprint. Unspecified reference inputs may escape; results may alias. "
            + "An empty footprint grants neither freshness nor stable observations under concurrent interference.",
        List.copyOf(packages));
  }

  private static String signature(String target, CtMethod<?> method) {
    String simple =
        target.substring(Math.max(target.lastIndexOf('.'), target.lastIndexOf('$')) + 1);
    return (method.getSimpleName().equals(simple) ? "<init>" : method.getSimpleName())
        + "("
        + method.getParameters().stream()
            .map(p -> p.getType().getTypeErasure().getQualifiedName())
            .collect(Collectors.joining(","))
        + ")";
  }

  static void write(Path root) throws IOException {
    var catalog = collect(root);
    Files.writeString(root.resolve("effects.json"), JSON.toJson(catalog) + "\n");
    var table =
        new StringBuilder("# Java 21 effects and ownership by package\n\n")
            .append(
                "Generated from the reviewed Java annotations and catalog evidence. Counts cover the same API entries as [the coverage inventory](jdk21-coverage.md). ")
            .append(
                "Every package is listed; a zero means no stronger footprint is supplied, not that the package is pure. ")
            .append(
                "The [machine-readable ledger](../specifications/jdk21/effects.json) records each declared summary, parameter/result ownership, view relationship and review rationale.\n\n")
            .append(
                "`@Modifies({})` preserves existing storage on normal and exceptional exits. Nonempty footprints name possible writes; `@Free` separately describes fresh results. ")
            .append(
                "`@ReturnsView` derives ownership from a captured backing List. Entries without a footprint retain conservative effects, even when they have scalar refinements. ")
            .append(
                "Read-only concurrent observations remain snapshots. See [the annotation semantics](external-refinements.md) and [review boundaries](jdk21-contracts.md#effect-and-ownership-review).\n\n")
            .append(
                "| Package | API entries | Empty footprint | Write footprint | View results | Without footprint |\n| --- | ---: | ---: | ---: | ---: | ---: |\n");
    for (var p : catalog.packages())
      table
          .append("| ")
          .append(p.name())
          .append(" | ")
          .append(p.apiMembers())
          .append(" | ")
          .append(p.readOnlyFootprints())
          .append(" | ")
          .append(p.mutatingFootprints())
          .append(" | ")
          .append(p.viewResults())
          .append(" | ")
          .append(p.withoutFootprint())
          .append(" |\n");
    table.append(
        "\nView results overlap footprint counts. Entries without footprints include fields and conservative API members; their original per-member policies remain in `api/*.json`.\n");
    Files.writeString(root.resolve("../../docs/jdk21-effects.md"), table);
  }
}
