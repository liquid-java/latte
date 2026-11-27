package latte.api;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import latte.analysis.Jdk21Coverage;
import spoon.Launcher;
import spoon.support.compiler.VirtualFile;

/** Opt-in Java 21 contracts, loaded as sources into the application's Spoon model. */
public final class Jdk21Contracts {
  private static final String ROOT = "/latte/jdk21/";

  private Jdk21Contracts() {}

  private record Specification(String target, String file) {
    String packageName() {
      return target.substring(0, target.lastIndexOf('.'));
    }
  }

  private record Catalog(int javaRelease, List<Specification> specifications) {}

  private record PackageIndex(String name, String file) {}

  private record Inventory(int javaRelease, List<PackageIndex> packages) {}

  private record PackageEntries(List<Jdk21Coverage.TypeEntry> types) {}

  // Only immutable catalog data is shared; selection and use-site metadata belong to each model.
  private static final class Data {
    static final Inventory INVENTORY =
        new Gson().fromJson(resource("inventory.json"), Inventory.class);
    static final Map<String, Jdk21Coverage.TypeEntry> TYPES =
        INVENTORY.packages().stream()
            .flatMap(
                entry ->
                    new Gson()
                        .fromJson(resource(entry.file()), PackageEntries.class).types().stream())
            .collect(Collectors.toUnmodifiableMap(Jdk21Coverage.TypeEntry::target, type -> type));
  }

  /** Every Java SE 21 package, including packages governed entirely by conservative policies. */
  public static Set<String> packages() {
    if (Data.INVENTORY.javaRelease() != 21)
      throw new IllegalStateException("Expected the Java 21 API inventory");
    return Set.copyOf(Data.INVENTORY.packages().stream().map(PackageIndex::name).toList());
  }

  /** Select exact package names, or {@code all}. Unspecified members stay conservative. */
  public static void addTo(Launcher launcher, Collection<String> packages) {
    var selected = Set.copyOf(packages);
    if (selected.isEmpty()) return;
    var entries = catalog();
    var unknown = new TreeSet<>(selected);
    unknown.removeAll(packages());
    unknown.remove("all");
    if (!unknown.isEmpty())
      throw new IllegalArgumentException("Unknown Java SE 21 packages: " + unknown);
    var previous =
        Jdk21Coverage.on(launcher.getFactory().getModel().getRootPackage())
            .map(Jdk21Coverage::packages)
            .orElse(Set.of());
    var combined = new TreeSet<>(previous);
    combined.addAll(selected.contains("all") ? packages() : selected);
    // Read before modifying the launcher so an incomplete installation cannot load half a catalog.
    var sources =
        entries.stream()
            .filter(
                entry ->
                    combined.contains(entry.packageName())
                        && !previous.contains(entry.packageName()))
            .map(entry -> new VirtualFile(resource(entry.file()), "jdk21/" + entry.file()))
            .toList();
    sources.forEach(launcher::addInputResource);
    launcher
        .getFactory()
        .getModel()
        .getRootPackage()
        .putMetadata(Jdk21Coverage.KEY, new Jdk21Coverage(combined, Data.TYPES));
  }

  private static List<Specification> catalog() {
    var catalog = new Gson().fromJson(resource("catalog.json"), Catalog.class);
    if (catalog.javaRelease() != 21)
      throw new IllegalStateException("Expected the Java 21 contract catalog");
    return List.copyOf(catalog.specifications());
  }

  private static String resource(String path) {
    try (var input = Jdk21Contracts.class.getResourceAsStream(ROOT + path)) {
      if (input == null)
        throw new IllegalStateException("Missing Java 21 contract resource: " + path);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException failure) {
      throw new IllegalStateException("Cannot read Java 21 contract resource: " + path, failure);
    }
  }
}
