package latte.jdk;

import com.google.gson.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import latte.analysis.Jdk21Coverage.Member;
import latte.analysis.Jdk21Coverage.TypeEntry;

/** Reproducible package-by-package declaration changes; these are not behavioral equivalences. */
final class Jdk21Migration {
  private static final Gson JSON =
      new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  record Baseline(int javaRelease, List<Jdk21Inventory.PackageEntry> packages) {}

  record Declaration(String target, Member member) {}

  record ChangedDeclaration(String target, Member before, Member after) {}

  record PackageChanges(
      String name,
      List<String> addedTypes,
      List<String> removedTypes,
      List<Declaration> addedDeclarations,
      List<Declaration> removedDeclarations,
      List<ChangedDeclaration> changedDeclarations) {}

  record Migration(
      int javaRelease,
      int baselineJavaRelease,
      String baselineSha256,
      String interpretation,
      List<PackageChanges> packages) {}

  private Jdk21Migration() {}

  static void write(Path root, List<Jdk21Inventory.PackageIndex> index) throws Exception {
    var path = root.resolve("baseline-java17.json.gz");
    Baseline baseline;
    try (var input = new GZIPInputStream(Files.newInputStream(path))) {
      baseline =
          JSON.fromJson(new String(input.readAllBytes(), StandardCharsets.UTF_8), Baseline.class);
    }
    if (baseline.javaRelease() != 17)
      throw new IllegalArgumentException("Expected Java 17 migration baseline");
    var previous =
        baseline.packages().stream()
            .collect(Collectors.toMap(Jdk21Inventory.PackageEntry::name, Function.identity()));
    var current = new TreeMap<String, Jdk21Inventory.PackageEntry>();
    for (var entry : index)
      current.put(
          entry.name(),
          JSON.fromJson(
              Files.readString(root.resolve(entry.file())), Jdk21Inventory.PackageEntry.class));
    var names = new TreeSet<>(previous.keySet());
    names.addAll(current.keySet());
    var changes = new ArrayList<PackageChanges>();
    for (var name : names) {
      var before =
          previous.getOrDefault(name, new Jdk21Inventory.PackageEntry(name, "", List.of()));
      var after = current.getOrDefault(name, new Jdk21Inventory.PackageEntry(name, "", List.of()));
      var oldTypes = before.types().stream().map(TypeEntry::target).collect(Collectors.toSet());
      var newTypes = after.types().stream().map(TypeEntry::target).collect(Collectors.toSet());
      var oldMembers = declarations(before);
      var newMembers = declarations(after);
      var modified =
          newMembers.entrySet().stream()
              .filter(entry -> oldMembers.containsKey(entry.getKey()))
              .filter(
                  entry ->
                      !sameDeclaration(
                          oldMembers.get(entry.getKey()).member(), entry.getValue().member()))
              .map(
                  entry ->
                      new ChangedDeclaration(
                          entry.getValue().target(),
                          oldMembers.get(entry.getKey()).member(),
                          entry.getValue().member()))
              .toList();
      changes.add(
          new PackageChanges(
              name,
              newTypes.stream().filter(type -> !oldTypes.contains(type)).sorted().toList(),
              oldTypes.stream().filter(type -> !newTypes.contains(type)).sorted().toList(),
              newMembers.entrySet().stream()
                  .filter(entry -> !oldMembers.containsKey(entry.getKey()))
                  .map(Map.Entry::getValue)
                  .toList(),
              oldMembers.entrySet().stream()
                  .filter(entry -> !newMembers.containsKey(entry.getKey()))
                  .map(Map.Entry::getValue)
                  .toList(),
              modified));
    }
    var migration =
        new Migration(
            21,
            17,
            HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))),
            "Changes to declarations, excluding inherited copies. A removed declaration may remain available through inheritance. Enum ordinal refinements are not new JDK declarations. Signatures and modifiers do not prove behavioral equivalence.",
            List.copyOf(changes));
    Files.writeString(root.resolve("migration-from17.json"), JSON.toJson(migration) + "\n");
  }

  private static Map<String, Declaration> declarations(Jdk21Inventory.PackageEntry entry) {
    return entry.types().stream()
        .flatMap(
            type ->
                type.members().stream()
                    .filter(member -> type.target().equals(member.declaredBy()))
                    .map(member -> new Declaration(type.target(), member)))
        .collect(
            Collectors.toMap(
                declaration -> declaration.target() + "#" + declaration.member().signature(),
                Function.identity(),
                (left, right) -> {
                  throw new IllegalStateException("Duplicate declaration");
                },
                TreeMap::new));
  }

  private static boolean sameDeclaration(Member before, Member after) {
    return before.kind().equals(after.kind())
        && before.result().equals(after.result())
        && before.modifiers() == after.modifiers()
        && before.varargs() == after.varargs();
  }
}
