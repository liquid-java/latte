package latte.jdk;

import com.google.gson.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.*;
import java.util.zip.ZipFile;
import latte.analysis.Annotations;
import latte.analysis.Jdk21Coverage.Member;
import latte.analysis.Jdk21Coverage.TypeEntry;
import spoon.Launcher;
import spoon.reflect.declaration.CtType;

/**
 * Rebuild the exhaustive API ledger using the pinned Java 21 runtime, its sources and API index.
 */
public final class Jdk21Inventory {
  private static final Gson JSON =
      new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
  private static final String DOCS = "https://docs.oracle.com/en/java/javase/21/docs/api/";

  public record ApiType(String target, String packageName, String module, String documentation) {}

  public record PackageEntry(String name, String module, List<TypeEntry> types) {}

  public record PackageIndex(
      String name,
      String module,
      String file,
      int types,
      int members,
      int refinedMembers,
      int conservativeMembers) {}

  public record Index(
      int javaRelease,
      String runtimeVersion,
      String apiIndexSource,
      String apiIndexSha256,
      String sourceArchiveSha256,
      String conservativePolicy,
      List<PackageIndex> packages) {}

  private Jdk21Inventory() {}

  public static void main(String[] arguments) throws Exception {
    if (Runtime.version().feature() != 21)
      throw new IllegalStateException("Rebuild this catalog with Java 21");
    if (arguments.length != 3)
      throw new IllegalArgumentException(
          "Usage: Jdk21Inventory <catalog directory> <allclasses-index.html> <JDK src.zip>");
    var root = Path.of(arguments[0]);
    var index = Path.of(arguments[1]);
    var archive = Path.of(arguments[2]);
    var documented = includingNestedTypes(documentedTypes(Files.readString(index)));
    try (var sources = new ZipFile(archive.toFile())) {
      Jdk21EnumContracts.supply(root, documented, sources);
    }
    var contracts = contracts(root);
    var names = documented.stream().map(ApiType::target).collect(Collectors.toSet());
    var byPackage =
        documented.stream()
            .collect(
                Collectors.groupingBy(ApiType::packageName, TreeMap::new, Collectors.toList()));
    var packages = new ArrayList<PackageIndex>();
    try (var sources = new ZipFile(archive.toFile())) {
      for (var group : byPackage.entrySet()) {
        var types = new ArrayList<TypeEntry>();
        for (var type : group.getValue()) types.add(describe(type, contracts, names, sources));
        var entry =
            new PackageEntry(group.getKey(), group.getValue().get(0).module(), List.copyOf(types));
        String file = "api/" + group.getKey() + ".json";
        write(root.resolve(file), entry);
        var members = types.stream().flatMap(t -> t.members().stream()).toList();
        int refined = (int) members.stream().filter(m -> m.policy().equals("refined")).count();
        packages.add(
            new PackageIndex(
                entry.name(),
                entry.module(),
                file,
                types.size(),
                members.size(),
                refined,
                members.size() - refined));
      }
    }
    Files.write(
        root.resolve("api-types.txt"),
        documented.stream().map(type -> type.module() + " " + type.target()).toList(),
        StandardCharsets.UTF_8);
    write(
        root.resolve("inventory.json"),
        new Index(
            21,
            Runtime.version().toString(),
            DOCS + "allclasses-index.html",
            hash(Files.readAllBytes(index)),
            hash(Files.readAllBytes(archive)),
            "Reference inputs and receivers may escape and mutate; reference results may alias any reachable or external object; primitive results retain only their Java type; exceptional and global effects are unknown. No undocumented purity, freshness, non-nullness or typestate promise is made.",
            List.copyOf(packages)));
    summaries(root, packages, contracts);
    Jdk21EffectCatalog.write(root);
    Jdk21Migration.write(root, packages);
    System.out.printf(
        "Indexed %d packages, %d types, %d declared or explicitly refined members%n",
        packages.size(),
        documented.size(),
        packages.stream().mapToInt(PackageIndex::members).sum());
  }

  public static List<ApiType> documentedTypes(String html) {
    var pattern =
        Pattern.compile(
            "<div class=\"col-first[^\"]*\"><a href=\"(java\\.[^/]+)/([^\"]+)\\.html\"");
    return pattern
        .matcher(html)
        .results()
        .map(
            match -> {
              String path = match.group(2);
              int slash = path.lastIndexOf('/');
              String packageName = path.substring(0, slash).replace('/', '.');
              String name = path.substring(slash + 1).replace('.', '$');
              return new ApiType(
                  packageName + "." + name,
                  packageName,
                  match.group(1),
                  DOCS + match.group(1) + "/" + path + ".html");
            })
        .distinct()
        .sorted(Comparator.comparing(ApiType::target))
        .toList();
  }

  /** The Javadoc all-classes index omits some visible member classes (notably Swing inners). */
  private static List<ApiType> includingNestedTypes(List<ApiType> indexed) throws Exception {
    var types = new TreeMap<String, ApiType>();
    var pending = new ArrayDeque<>(indexed);
    while (!pending.isEmpty()) {
      var entry = pending.removeFirst();
      if (types.putIfAbsent(entry.target(), entry) != null) continue;
      var actual = Class.forName(entry.target(), false, ClassLoader.getSystemClassLoader());
      for (var nested : actual.getDeclaredClasses()) {
        if (!visible(nested.getModifiers()) || nested.isSynthetic()) continue;
        pending.add(
            new ApiType(
                nested.getName(),
                entry.packageName(),
                entry.module(),
                DOCS
                    + entry.module()
                    + "/"
                    + entry.packageName().replace('.', '/')
                    + "/"
                    + nested.getName().substring(entry.packageName().length() + 1).replace('$', '.')
                    + ".html"));
      }
    }
    return List.copyOf(types.values());
  }

  private static Map<String, Set<String>> contracts(Path root) {
    var launcher = new Launcher();
    launcher.getEnvironment().setNoClasspath(true);
    launcher.getEnvironment().setComplianceLevel(21);
    launcher.addInputResource(root.toString());
    return launcher.buildModel().getAllTypes().stream()
        .filter(type -> Annotations.has(type, "ExternalRefinementsFor"))
        .collect(
            Collectors.toMap(
                type -> Annotations.strings(type, "ExternalRefinementsFor", "value").get(0),
                Jdk21Inventory::signatures));
  }

  private static Set<String> signatures(CtType<?> specification) {
    String target = Annotations.strings(specification, "ExternalRefinementsFor", "value").get(0);
    String simple =
        target.substring(Math.max(target.lastIndexOf('.'), target.lastIndexOf('$')) + 1);
    var members =
        specification.getMethods().stream()
            .map(
                method ->
                    (method.getSimpleName().equals(simple) ? "<init>" : method.getSimpleName())
                        + "("
                        + method.getParameters().stream()
                            .map(p -> p.getType().getTypeErasure().getQualifiedName())
                            .collect(Collectors.joining(","))
                        + ")")
            .collect(Collectors.toSet());
    specification.getFields().forEach(field -> members.add(field.getSimpleName()));
    return Set.copyOf(members);
  }

  private static TypeEntry describe(
      ApiType type, Map<String, Set<String>> contracts, Set<String> documented, ZipFile sources)
      throws Exception {
    var actual = Class.forName(type.target(), false, ClassLoader.getSystemClassLoader());
    if (!actual.getModule().getName().equals(type.module()))
      throw new IllegalStateException("Wrong module for " + type.target());
    var selected = contracts.getOrDefault(type.target(), Set.of());
    var members = new TreeMap<String, Member>();
    for (var constructor : actual.getDeclaredConstructors())
      if (visible(constructor.getModifiers()) && !constructor.isSynthetic()) {
        var signature = signature("<init>", constructorParameters(constructor));
        members.put(
            signature,
            member(
                "constructor",
                signature,
                "void",
                actual.getName(),
                constructor.getModifiers(),
                constructor.isVarArgs(),
                selected));
      }
    for (var method : actual.getDeclaredMethods())
      if (visible(method.getModifiers()) && !method.isSynthetic() && !method.isBridge()) {
        var signature = signature(method.getName(), method.getParameterTypes());
        members.put(
            signature,
            member(
                "method",
                signature,
                method.getReturnType().getTypeName(),
                method.getDeclaringClass().getName(),
                method.getModifiers(),
                method.isVarArgs(),
                selected));
      }
    // An explicit subclass contract may strengthen an inherited method, e.g. Copies.getValue.
    for (var method : actual.getMethods()) {
      var signature = signature(method.getName(), method.getParameterTypes());
      if (!method.isSynthetic()
          && !method.isBridge()
          && (selected.contains(signature)
              || !documented.contains(method.getDeclaringClass().getName()))
          && !members.containsKey(signature))
        members.put(
            signature,
            member(
                "inherited-method",
                signature,
                method.getReturnType().getTypeName(),
                method.getDeclaringClass().getName(),
                method.getModifiers(),
                method.isVarArgs(),
                selected));
    }
    for (var field : actual.getDeclaredFields())
      if (visible(field.getModifiers()) && !field.isSynthetic())
        members.put(
            field.getName(),
            member(
                "field",
                field.getName(),
                field.getType().getTypeName(),
                field.getDeclaringClass().getName(),
                field.getModifiers(),
                false,
                selected));
    for (var field : actual.getFields())
      if (!field.isSynthetic() && !documented.contains(field.getDeclaringClass().getName()))
        members.putIfAbsent(
            field.getName(),
            member(
                "field",
                field.getName(),
                field.getType().getTypeName(),
                field.getDeclaringClass().getName(),
                field.getModifiers(),
                false,
                selected));
    // Public/protected surface inherited through non-API implementation classes is still API.
    for (var parent = actual.getSuperclass(); parent != null; parent = parent.getSuperclass()) {
      if (documented.contains(parent.getName())) continue;
      for (var method : parent.getDeclaredMethods())
        if (visible(method.getModifiers()) && !method.isSynthetic() && !method.isBridge()) {
          var signature = signature(method.getName(), method.getParameterTypes());
          members.putIfAbsent(
              signature,
              member(
                  "inherited-method",
                  signature,
                  method.getReturnType().getTypeName(),
                  parent.getName(),
                  method.getModifiers(),
                  method.isVarArgs(),
                  selected));
        }
      for (var field : parent.getDeclaredFields())
        if (visible(field.getModifiers()) && !field.isSynthetic())
          members.putIfAbsent(
              field.getName(),
              member(
                  "field",
                  field.getName(),
                  field.getType().getTypeName(),
                  parent.getName(),
                  field.getModifiers(),
                  false,
                  selected));
    }
    if (!members.keySet().containsAll(selected))
      throw new IllegalStateException(
          "Contract signatures missing from Java 21: "
              + type.target()
              + " "
              + selected.stream().filter(s -> !members.containsKey(s)).toList());
    var parents =
        Stream.concat(
                Stream.ofNullable(actual.getSuperclass()), Arrays.stream(actual.getInterfaces()))
            .flatMap(parent -> documentedParents(parent, documented))
            .distinct()
            .toList();
    String outer = type.target().split("\\$")[0];
    String sourceEntry = type.module() + "/" + outer.replace('.', '/') + ".java";
    var source = sources.getEntry(sourceEntry);
    if (source == null) throw new IllegalStateException("Missing source for " + type.target());
    byte[] bytes;
    try (var stream = sources.getInputStream(source)) {
      bytes = stream.readAllBytes();
    }
    String kind =
        actual.isAnnotation()
            ? "annotation"
            : actual.isEnum()
                ? "enum"
                : actual.isRecord() ? "record" : actual.isInterface() ? "interface" : "class";
    return new TypeEntry(
        type.target(),
        kind,
        parents,
        type.documentation(),
        sourceEntry,
        hash(bytes),
        List.copyOf(members.values()));
  }

  private static Member member(
      String kind,
      String signature,
      String result,
      String declaredBy,
      int modifiers,
      boolean varargs,
      Set<String> selected) {
    boolean refined = selected.contains(signature);
    String reason =
        refined
            ? "audited-source-contract"
            : kind.equals("field")
                ? "field-type-only-no-value-or-ownership-guarantee"
                : Modifier.isNative(modifiers)
                    ? "native-effects-and-result-unconstrained"
                    : Modifier.isAbstract(modifiers)
                        ? "provider-or-callback-effects-and-result-unconstrained"
                        : "effects-aliases-and-result-unconstrained";
    return new Member(
        kind,
        signature,
        result,
        declaredBy,
        modifiers,
        varargs,
        refined ? "refined" : "conservative",
        reason);
  }

  private static Stream<String> documentedParents(Class<?> type, Set<String> documented) {
    if (documented.contains(type.getName())) return Stream.of(type.getName());
    return Stream.concat(
            Stream.ofNullable(type.getSuperclass()), Arrays.stream(type.getInterfaces()))
        .flatMap(parent -> documentedParents(parent, documented));
  }

  /** Java source signatures omit a non-static member class's enclosing-instance JVM parameter. */
  public static Class<?>[] constructorParameters(Constructor<?> constructor) {
    var type = constructor.getDeclaringClass();
    var parameters = constructor.getParameterTypes();
    return type.isMemberClass() && !Modifier.isStatic(type.getModifiers())
        ? Arrays.copyOfRange(parameters, 1, parameters.length)
        : parameters;
  }

  private static boolean visible(int modifiers) {
    return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
  }

  private static void summaries(
      Path root, List<PackageIndex> packages, Map<String, Set<String>> contracts)
      throws IOException {
    var summary = new JsonObject();
    summary.addProperty("javaRelease", 21);
    summary.addProperty("inventorySource", DOCS + "allclasses-index.html");
    summary.addProperty("coverage", "exhaustive-api-conservative-defaults");
    var entries = new JsonArray();
    var table =
        new StringBuilder("# Java 21 package coverage\n\n")
            .append(
                "All "
                    + packages.size()
                    + " Java SE 21 packages are selectable. Every documented type and public/protected member has an explicit policy, directly or through its indexed ancestors. ")
            .append(
                "The catalog includes the official all-classes index and recursively discovered public/protected member classes, which that index does not exhaustively list. Counts below include declared members, explicit inherited refinements, and members exposed by non-API ancestors; ordinary inherited members are counted at their declaration.\n\n")
            .append(
                "**Complete coverage means exhaustive classification, not a precise behavioral refinement of every member.** ")
            .append(
                "The "
                    + packages.stream().mapToInt(PackageIndex::refinedMembers).sum()
                    + " reviewed declarations have stronger source contracts. All other entries use conservative unknown-call or field handling, without ownership, purity, value or state promises. ")
            .append(
                "No manual implementation review is claimed for conservative entries. See the [policy, limitations and regeneration instructions](jdk21-contracts.md).\n\n")
            .append(
                "| Package | Module | Types | Members | Refined | Conservative | Precision boundary |\n| --- | --- | ---: | ---: | ---: | ---: | --- |\n");
    for (var entry : packages) {
      var item = new JsonObject();
      item.addProperty("package", entry.name());
      item.addProperty("module", entry.module());
      item.addProperty("status", "covered");
      item.addProperty("reviewLevel", "exhaustive-api-conservative-defaults");
      String documentation =
          DOCS + entry.module() + "/" + entry.name().replace('.', '/') + "/package-summary.html";
      item.addProperty("documentation", documentation);
      item.addProperty("inventory", entry.file());
      item.addProperty("types", entry.types());
      item.addProperty("members", entry.members());
      item.addProperty("refinedMembers", entry.refinedMembers());
      item.addProperty("conservativeMembers", entry.conservativeMembers());
      String policy = Jdk21PackagePolicies.note(entry.module(), entry.name());
      item.addProperty("conservativePolicy", policy);
      item.add(
          "targets",
          JSON.toJsonTree(
              contracts.keySet().stream()
                  .filter(
                      target -> target.substring(0, target.lastIndexOf('.')).equals(entry.name()))
                  .sorted()
                  .toList()));
      entries.add(item);
      table
          .append("| [")
          .append(entry.name())
          .append("](")
          .append(documentation)
          .append(") | ")
          .append(entry.module())
          .append(" | ")
          .append(entry.types())
          .append(" | ")
          .append(entry.members())
          .append(" | ")
          .append(entry.refinedMembers())
          .append(" | ")
          .append(entry.conservativeMembers())
          .append(" | ")
          .append(policy)
          .append(" |\n");
    }
    summary.add("packages", entries);
    write(root.resolve("packages.json"), summary);
    int total = packages.stream().mapToInt(PackageIndex::members).sum();
    int refined = packages.stream().mapToInt(PackageIndex::refinedMembers).sum();
    table
        .append("\nTotal: **")
        .append(total)
        .append(" member entries**, **")
        .append(refined)
        .append(" refined**, **")
        .append(total - refined)
        .append(" conservative**.\n");
    Files.writeString(root.resolve("../../docs/jdk21-coverage.md"), table, StandardCharsets.UTF_8);
  }

  public static String signature(String name, Class<?>[] parameters) {
    return name
        + Arrays.stream(parameters)
            .map(Class::getTypeName)
            .collect(Collectors.joining(",", "(", ")"));
  }

  private static String hash(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
  }

  private static void write(Path path, Object value) throws IOException {
    Files.createDirectories(path.getParent());
    Files.writeString(path, JSON.toJson(value) + "\n", StandardCharsets.UTF_8);
  }
}
