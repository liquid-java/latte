package latte.jdk;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;

/** Supplies the same language-guaranteed ordinal bound for every exported Java 21 enum. */
final class Jdk21EnumContracts {
  private static final Gson JSON =
      new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  private static final String DOCS = "https://docs.oracle.com/en/java/javase/21/docs/api/";
  private static final String TAG = "jdk-21.0.13.1+2";

  private Jdk21EnumContracts() {}

  static void supply(Path root, List<Jdk21Inventory.ApiType> types, ZipFile sources)
      throws Exception {
    var runtime = Runtime.version();
    if (!runtime.version().equals(List.of(21, 0, 13, 1)) || runtime.build().orElse(-1) != 2)
      throw new IllegalStateException(
          "Regenerate source provenance with the pinned " + TAG + " runtime");
    var catalog =
        JsonParser.parseString(Files.readString(root.resolve("catalog.json"))).getAsJsonObject();
    var specifications = catalog.getAsJsonArray("specifications");
    var entries = new TreeMap<String, JsonObject>();
    specifications.forEach(
        value ->
            entries.put(
                value.getAsJsonObject().get("target").getAsString(), value.getAsJsonObject()));
    for (var type : types) {
      var actual = Class.forName(type.target(), false, ClassLoader.getSystemClassLoader());
      if (!actual.isEnum()) continue;
      long constants =
          Arrays.stream(actual.getDeclaredFields())
              .filter(java.lang.reflect.Field::isEnumConstant)
              .count();
      String simple =
          type.target().substring(type.packageName().length() + 1).replace('$', '_') + "Spec";
      var entry = entries.get(type.target());
      if (entry == null) {
        entry = new JsonObject();
        entry.addProperty("target", type.target());
        entry.addProperty("package", type.packageName());
        entry.addProperty("module", type.module());
        entry.addProperty("file", type.packageName().replace('.', '/') + "/" + simple + ".java");
        entry.addProperty(
            "review",
            "Enum.ordinal is final, and compiler-created constants are numbered from zero in"
                + " declaration order. The upper bound counts this enum's constants. No freshness"
                + " or ownership promise is made for enum constants or values() arrays.");
        entry.addProperty("evidence", "java-language-enum-rule-and-source");
        entry.addProperty("specifiedMembers", 0);
        entries.put(type.target(), entry);
      }
      var path = root.resolve(entry.get("file").getAsString());
      String source =
          Files.exists(path)
              ? Files.readString(path)
              : "package latte.jdk21."
                  + type.packageName()
                  + ";\n\nimport liquidjava.specification.*;\nimport specification.*;\n\n"
                  + "@ExternalRefinementsFor(\""
                  + type.target()
                  + "\")\npublic interface "
                  + simple
                  + " {\n}\n";
      String marker = "  // Generated enum ordinal bound.\n";
      String method =
          marker
              + "  @Modifies({})\n  @Refinement(\"_ >= 0 && _ < "
              + constants
              + "\")\n  int ordinal();\n";
      int start = source.indexOf(marker);
      if (start >= 0) {
        int end = source.indexOf("int ordinal();", start) + "int ordinal();".length();
        source = source.substring(0, start) + method.stripTrailing() + source.substring(end);
      } else {
        if (source.contains("ordinal()"))
          throw new IllegalStateException("Unmanaged ordinal contract: " + path);
        int end = source.lastIndexOf('}');
        source = source.substring(0, end) + method + source.substring(end);
        entry.addProperty("specifiedMembers", entry.get("specifiedMembers").getAsInt() + 1);
      }
      entry.addProperty("ordinalConstants", constants);
      if (!entry.has("effectReview"))
        entry.addProperty(
            "effectReview",
            "Enum.ordinal is final and reads only the ordinal field. Constants remain shared; this footprint does not grant ownership.");
      entry.addProperty(
          "ordinalRule", "https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.9");
      Files.createDirectories(path.getParent());
      Files.writeString(path, source);
    }
    for (var entry : entries.values()) {
      String target = entry.get("target").getAsString();
      String module = entry.get("module").getAsString();
      String packageName = entry.get("package").getAsString();
      String outer = target.split("\\$")[0];
      String sourcePath = outer.replace('.', '/') + ".java";
      var zipEntry = sources.getEntry(module + "/" + sourcePath);
      if (zipEntry == null) throw new IOException("Missing Java 21 source: " + target);
      String hash;
      try (var stream = sources.getInputStream(zipEntry)) {
        hash =
            HexFormat.of()
                .formatHex(MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes()));
      }
      entry.addProperty(
          "documentation",
          DOCS
              + module
              + "/"
              + packageName.replace('.', '/')
              + "/"
              + target.substring(packageName.length() + 1).replace('$', '.')
              + ".html");
      entry.addProperty(
          "source",
          "https://github.com/openjdk/jdk21u/blob/"
              + TAG.replace("+", "%2B")
              + "/src/"
              + module
              + "/share/classes/"
              + sourcePath);
      entry.addProperty("sourceSha256", hash);
    }
    catalog.add("specifications", JSON.toJsonTree(entries.values()));
    Files.writeString(root.resolve("catalog.json"), JSON.toJson(catalog) + "\n");
  }
}
