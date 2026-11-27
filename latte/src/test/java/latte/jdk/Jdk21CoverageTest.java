package latte.jdk;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.lang.reflect.Modifier;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import latte.analysis.ExternalSpecifications;
import latte.analysis.Jdk21Coverage;
import latte.analysis.LatteFacts;
import latte.analysis.Permission;
import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.compiler.VirtualFile;

class Jdk21CoverageTest {
  private static final Path ROOT = Path.of("../specifications/jdk21");

  private Verification.Result check(String packages, String source) {
    var launcher = new Launcher();
    launcher.getEnvironment().setNoClasspath(true);
    launcher.getEnvironment().setComplianceLevel(21);
    Jdk21Contracts.addTo(launcher, List.of(packages.split(",")));
    launcher.addInputResource(
        new VirtualFile(
            "import liquidjava.specification.*; import specification.*;\n" + source,
            "Sample.java"));
    return Verification.verify(launcher);
  }

  private Jdk21Coverage ledger() {
    var launcher = new Launcher();
    Jdk21Contracts.addTo(launcher, List.of("all"));
    return Jdk21Coverage.on(launcher.getFactory().getModel().getRootPackage()).orElseThrow();
  }

  @Test
  void newJava21PackagesAndApisHaveExplicitPolicies() {
    var coverage = ledger();
    assertTrue(coverage.packages().contains("java.lang.foreign"));
    for (var target :
        List.of(
            "java.lang.foreign.MemorySegment",
            "java.lang.foreign.Arena",
            "java.lang.ScopedValue",
            "java.lang.StringTemplate",
            "java.util.SequencedCollection",
            "java.util.SequencedMap",
            "java.util.SequencedSet",
            "java.util.concurrent.StructuredTaskScope"))
      assertTrue(coverage.types().containsKey(target), target);
    assertEquals(
        "conservative", coverage.member("java.lang.Thread", "ofVirtual()").orElseThrow().policy());
    assertEquals(
        "conservative", coverage.member("java.util.List", "reversed()").orElseThrow().policy());
    assertEquals(
        "refined",
        coverage
            .member("java.util.ArrayList", "addFirst(java.lang.Object)")
            .orElseThrow()
            .policy());
  }

  @Test
  void everyPackageHasAPrecisionBoundaryAndReproducibleMigrationEvidence() throws Exception {
    var packages =
        com.google.gson.JsonParser.parseString(Files.readString(ROOT.resolve("packages.json")))
            .getAsJsonObject()
            .getAsJsonArray("packages");
    for (var entry : packages)
      assertFalse(entry.getAsJsonObject().get("conservativePolicy").getAsString().isBlank());
    var migration =
        new Gson()
            .fromJson(
                Files.readString(ROOT.resolve("migration-from17.json")),
                Jdk21Migration.Migration.class);
    assertEquals(21, migration.javaRelease());
    assertEquals(17, migration.baselineJavaRelease());
    assertEquals(
        java.util.HexFormat.of()
            .formatHex(
                java.security.MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(ROOT.resolve("baseline-java17.json.gz")))),
        migration.baselineSha256());
    assertEquals(
        Jdk21Contracts.packages(),
        migration.packages().stream()
            .map(Jdk21Migration.PackageChanges::name)
            .collect(Collectors.toSet()));
    var coverage = ledger();
    for (var entry : migration.packages()) {
      for (var type : entry.addedTypes()) assertTrue(coverage.types().containsKey(type), type);
      for (var type : entry.removedTypes()) assertFalse(coverage.types().containsKey(type), type);
      for (var declaration : entry.addedDeclarations())
        assertEquals(
            declaration.member(),
            coverage.member(declaration.target(), declaration.member().signature()).orElseThrow());
    }
  }

  @Test
  void everyDocumentedTypeAndVisibleDeclarationHasAnExplicitPolicy() throws Exception {
    var coverage = ledger();
    var documented = Files.readAllLines(ROOT.resolve("api-types.txt"));
    assertTrue(documented.size() >= 3726);
    assertEquals(
        documented.stream().map(line -> line.split(" ")[1]).collect(Collectors.toSet()),
        coverage.types().keySet());
    for (var line : documented) {
      var parts = line.split(" ");
      var actual = Class.forName(parts[1], false, ClassLoader.getSystemClassLoader());
      assertEquals(parts[0], actual.getModule().getName());
      var type = coverage.types().get(parts[1]);
      assertTrue(type.sourceSha256().matches("[a-f0-9]{64}"), type.target());
      assertEquals(
          type.members().size(),
          type.members().stream().map(Jdk21Coverage.Member::signature).distinct().count(),
          type.target());
      for (var member : type.members()) {
        assertTrue(Set.of("refined", "conservative").contains(member.policy()), member.toString());
        assertNotNull(member.declaredBy());
      }
      for (var method : actual.getDeclaredMethods())
        if (visible(method.getModifiers()) && !method.isSynthetic() && !method.isBridge())
          declared(
              type,
              Jdk21Inventory.signature(method.getName(), method.getParameterTypes()),
              method.getReturnType().getTypeName(),
              method.getModifiers(),
              method.isVarArgs());
      // Public inherited APIs, including those exposed through undocumented implementation classes.
      for (var method : actual.getMethods())
        if (!method.isSynthetic() && !method.isBridge())
          present(
              coverage,
              actual,
              Jdk21Inventory.signature(method.getName(), method.getParameterTypes()));
      for (var constructor : actual.getDeclaredConstructors())
        if (visible(constructor.getModifiers()) && !constructor.isSynthetic())
          declared(
              type,
              Jdk21Inventory.signature("<init>", Jdk21Inventory.constructorParameters(constructor)),
              "void",
              constructor.getModifiers(),
              constructor.isVarArgs());
      for (var field : actual.getDeclaredFields())
        if (visible(field.getModifiers()) && !field.isSynthetic())
          declared(
              type, field.getName(), field.getType().getTypeName(), field.getModifiers(), false);
      for (var field : actual.getFields())
        if (!field.isSynthetic()) present(coverage, actual, field.getName());
      for (var parent = actual.getSuperclass(); parent != null; parent = parent.getSuperclass()) {
        for (var method : parent.getDeclaredMethods())
          if (Modifier.isProtected(method.getModifiers())
              && !method.isSynthetic()
              && !method.isBridge())
            present(
                coverage,
                actual,
                Jdk21Inventory.signature(method.getName(), method.getParameterTypes()));
        for (var field : parent.getDeclaredFields())
          if (Modifier.isProtected(field.getModifiers()) && !field.isSynthetic())
            present(coverage, actual, field.getName());
      }
    }
  }

  private void declared(
      Jdk21Coverage.TypeEntry type,
      String signature,
      String result,
      int modifiers,
      boolean varargs) {
    var entry =
        type.members().stream()
            .filter(m -> m.signature().equals(signature))
            .findFirst()
            .orElseThrow(() -> new AssertionError(type.target() + "#" + signature));
    assertEquals(type.target(), entry.declaredBy(), entry.toString());
    assertEquals(result, entry.result(), entry.toString());
    assertEquals(modifiers, entry.modifiers(), entry.toString());
    assertEquals(varargs, entry.varargs(), entry.toString());
  }

  private void present(Jdk21Coverage coverage, Class<?> target, String signature) {
    assertTrue(
        coverage.member(target.getName(), signature).isPresent(),
        target.getName() + "#" + signature);
  }

  private boolean visible(int modifiers) {
    return Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers);
  }

  @Test
  void everyExportedJavaSeTypeIsPresentIncludingVisibleNestedClasses() throws Exception {
    var coverage = ledger();
    var runtime = FileSystems.getFileSystem(java.net.URI.create("jrt:/"));
    var expected = new HashSet<String>();
    var packages = new HashSet<String>();
    for (var module : java.lang.module.ModuleFinder.ofSystem().findAll()) {
      var descriptor = module.descriptor();
      if (!descriptor.name().startsWith("java.")) continue;
      for (var exported : descriptor.exports()) {
        if (exported.isQualified()) continue;
        packages.add(exported.source());
        var path =
            runtime.getPath("/modules", descriptor.name(), exported.source().replace('.', '/'));
        try (var files = Files.list(path)) {
          for (var file :
              files
                  .filter(p -> p.toString().endsWith(".class") && !p.toString().contains("$"))
                  .toList()) {
            String simple = file.getFileName().toString().replace(".class", "");
            if (simple.equals("package-info")) continue;
            var type =
                Class.forName(
                    exported.source() + "." + simple, false, ClassLoader.getSystemClassLoader());
            visibleTypes(type, expected);
          }
        }
      }
    }
    assertEquals(packages, coverage.packages());
    assertEquals(expected, coverage.types().keySet());
  }

  private void visibleTypes(Class<?> type, Set<String> result) {
    if (!visible(type.getModifiers()) || type.isSynthetic()) return;
    result.add(type.getName());
    for (var nested : type.getDeclaredClasses()) visibleTypes(nested, result);
  }

  @Test
  void packageCountsAndEveryStrongerSignatureAgreeWithTheInventory() throws Exception {
    var coverage = ledger();
    var inventory =
        new Gson()
            .fromJson(Files.readString(ROOT.resolve("inventory.json")), Jdk21Inventory.Index.class);
    assertEquals(21, inventory.javaRelease());
    assertEquals(178, inventory.packages().size());
    int count = 0;
    for (var entry : inventory.packages()) {
      var contents =
          new Gson()
              .fromJson(
                  Files.readString(ROOT.resolve(entry.file())), Jdk21Inventory.PackageEntry.class);
      assertEquals(entry.name(), contents.name());
      assertEquals(entry.module(), contents.module());
      assertEquals(entry.types(), contents.types().size());
      var members = contents.types().stream().flatMap(type -> type.members().stream()).toList();
      assertEquals(entry.members(), members.size());
      assertEquals(
          entry.refinedMembers(),
          members.stream().filter(m -> m.policy().equals("refined")).count());
      assertEquals(
          entry.conservativeMembers(),
          members.stream().filter(m -> m.policy().equals("conservative")).count());
      count += members.size();
    }
    assertEquals(
        count, coverage.types().values().stream().mapToInt(type -> type.members().size()).sum());
    var result = check("all", "class Sample {}");
    assertTrue(result.accepted(), result.diagnostics().toString());
    var registry = ExternalSpecifications.on(result.model().getRootPackage());
    var refined = new HashSet<String>();
    registry
        .specifications()
        .forEach(
            (target, specification) ->
                specification
                    .methods()
                    .forEach(
                        (signature, binding) -> {
                          assertEquals(
                              "refined", coverage.member(target, signature).orElseThrow().policy());
                          refined.add(target + "#" + signature);
                        }));
    var manifest =
        com.google.gson.JsonParser.parseString(Files.readString(ROOT.resolve("catalog.json")))
            .getAsJsonObject();
    int expectedRefined =
        manifest.getAsJsonArray("specifications").asList().stream()
            .mapToInt(entry -> entry.getAsJsonObject().get("specifiedMembers").getAsInt())
            .sum();
    assertEquals(expectedRefined, refined.size());
    assertEquals(
        refined,
        coverage.types().values().stream()
            .flatMap(
                type ->
                    type.members().stream()
                        .filter(m -> m.policy().equals("refined"))
                        .map(m -> type.target() + "#" + m.signature()))
            .collect(Collectors.toSet()));
  }

  @Test
  void allPackagesCanBeSelectedIndividuallyAndSelectionsDoNotLeak() {
    for (String name : Jdk21Contracts.packages()) {
      var launcher = new Launcher();
      Jdk21Contracts.addTo(launcher, List.of(name));
      assertEquals(
          Set.of(name),
          Jdk21Coverage.on(launcher.getFactory().getModel().getRootPackage())
              .orElseThrow()
              .packages());
    }
    assertTrue(Jdk21Coverage.on(new Launcher().getFactory().getModel().getRootPackage()).isEmpty());
  }

  @Test
  void repeatedSelectionsAreIdempotentAndMetadataSurvivesSpoonBuild() {
    var launcher = new Launcher();
    launcher.getEnvironment().setNoClasspath(true);
    Jdk21Contracts.addTo(launcher, List.of("java.lang"));
    Jdk21Contracts.addTo(launcher, List.of("java.lang", "java.util.stream"));
    var result = Verification.verify(launcher);
    assertTrue(result.accepted(), result.diagnostics().toString());
    assertEquals(
        Set.of("java.lang", "java.util.stream"),
        Jdk21Coverage.on(result.model().getRootPackage()).orElseThrow().packages());
  }

  @Test
  void callsAndFieldsExposeTheirEffectivePolicyOnTheAst() {
    var result =
        check(
            "java.lang,java.util.concurrent.atomic",
            """
        class Sample { void f(java.util.concurrent.atomic.AtomicInteger value) {
          int bitCount = Integer.bitCount(42);
          int snapshot = value.get();
          int maximum = Integer.MAX_VALUE;
        }}
        """);
    assertTrue(result.accepted(), result.diagnostics().toString());
    var calls = result.model().getElements(new TypeFilter<>(CtInvocation.class));
    var bitCount =
        calls.stream()
            .filter(c -> c.getExecutable().getSimpleName().equals("bitCount"))
            .findFirst()
            .orElseThrow();
    var snapshot =
        calls.stream()
            .filter(c -> c.getExecutable().getSimpleName().equals("get"))
            .findFirst()
            .orElseThrow();
    assertEquals("refined", Jdk21Coverage.use(bitCount.getExecutable()).orElseThrow().policy());
    assertEquals(
        "conservative", Jdk21Coverage.use(snapshot.getExecutable()).orElseThrow().policy());
    var maximum =
        result.model().getElements(new TypeFilter<>(CtFieldReference.class)).stream()
            .filter(f -> f.getSimpleName().equals("MAX_VALUE") && f.getPosition().isValidPosition())
            .findFirst()
            .orElseThrow();
    assertEquals("conservative", Jdk21Coverage.use(maximum).orElseThrow().policy());
  }

  @Test
  void arrayConstructorReferencesHaveNoJdkContractAndOtherReferencesKeepTheirPolicies() {
    var result =
        check(
            "all",
            """
        class Sample {
          static int local() { return 1; }
          Object[][] copy(java.util.List<Object[]> rows) {
            return rows.toArray(Object[][]::new);
          }
          void factories() {
            java.util.function.IntFunction<int[]> ints = int[]::new;
            java.util.function.IntFunction<String[]> strings = String[]::new;
            java.util.function.IntFunction<int[][]> matrix = int[][]::new;
            java.util.function.Supplier<java.util.ArrayList<Object>> lists = java.util.ArrayList::new;
            java.util.function.Supplier<java.util.Timer> timers = java.util.Timer::new;
            java.util.function.IntUnaryOperator bits = Integer::bitCount;
            java.util.function.IntSupplier local = Sample::local;
          }
        }
        """);
    assertTrue(result.accepted(), result.diagnostics().toString());
    var references =
        result.model().getElements(new TypeFilter<>(CtExecutableReferenceExpression.class));
    assertEquals(8, references.size());
    int arrays = 0;
    for (var expression : references) {
      var reference = expression.getExecutable();
      var use = Jdk21Coverage.use(reference);
      if (reference.getDeclaringType() instanceof CtArrayTypeReference<?>) {
        arrays++;
        assertTrue(use.isEmpty(), reference.toString());
      } else if (reference.getDeclaringType().getQualifiedName().equals("Sample")) {
        assertTrue(use.isEmpty(), reference.toString());
      } else {
        var expected =
            reference.getDeclaringType().getQualifiedName().equals("java.util.Timer")
                ? "conservative"
                : "refined";
        assertEquals(expected, use.orElseThrow().policy(), reference.toString());
      }
    }
    assertEquals(4, arrays);
    var toArray =
        result.model().getElements(new TypeFilter<>(CtInvocation.class)).stream()
            .filter(call -> call.getExecutable().getSimpleName().equals("toArray"))
            .findFirst()
            .orElseThrow();
    assertEquals("conservative", Jdk21Coverage.use(toArray.getExecutable()).orElseThrow().policy());
  }

  @Test
  void passingAnArrayConstructorReferenceDoesNotStrengthenAConservativeReturn() {
    var result =
        check(
            "all",
            """
        class Sample { void copy(java.util.List<Object[]> rows) {
          @Refinement("_ != null") Object[][] copied = rows.toArray(Object[][]::new);
        }}
        """);
    assertEquals(
        List.of(Diagnostic.Kind.SHARED_REFINEMENT),
        result.diagnostics().stream().map(Diagnostic::kind).toList(),
        result.diagnostics().toString());
  }

  @Test
  void conservativeCallbacksAndRetainingConstructorsRejectBorrowedInputs() {
    var result =
        check(
            "java.util.stream,java.util.concurrent.atomic",
            """
        class Sample {
          void callback(@Borrowed int[] values) {
            java.util.stream.Stream.of(1).map(x -> values[0]++);
          }
          void retained(@Borrowed Object value) { new java.util.concurrent.atomic.AtomicReference<>(value); }
        }
        """);
    assertEquals(
        List.of(Diagnostic.Kind.PERMISSION, Diagnostic.Kind.PERMISSION),
        result.diagnostics().stream().map(Diagnostic::kind).toList(),
        result.diagnostics().toString());
  }

  @Test
  void viewsRemainSharedAndConcurrentObservationsHaveNoInventedBounds() {
    var result =
        check(
            "java.util,java.util.concurrent.atomic",
            """
        class Sample {
          void view() {
            var values = new java.util.ArrayList<Integer>();
            @Refinement("_ != null") java.util.List<Integer> view = java.util.Collections.unmodifiableList(values);
          }
          void concurrent(java.util.concurrent.atomic.AtomicInteger value) {
            @Refinement("_ == 0") int snapshot = value.get();
          }
        }
        """);
    assertEquals(
        List.of(Diagnostic.Kind.SHARED_REFINEMENT, Diagnostic.Kind.REFINEMENT),
        result.diagnostics().stream().map(Diagnostic::kind).toList(),
        result.diagnostics().toString());
  }

  @Test
  void aCustomContractCanStrengthenAConservativePackage() {
    var result =
        check(
            "java.util.concurrent.atomic",
            """
        @ExternalRefinementsFor("java.util.concurrent.atomic.AtomicBoolean")
        interface AtomicBooleanSpec { @Refinement("_ || !_") boolean get(); }
        class Sample { void f(java.util.concurrent.atomic.AtomicBoolean value) {
          @Refinement("_ || !_") boolean snapshot = value.get();
        }}
        """);
    assertTrue(result.accepted(), result.diagnostics().toString());
    var call =
        result.model().getElements(new TypeFilter<>(CtInvocation.class)).stream()
            .filter(c -> c.getExecutable().getSimpleName().equals("get"))
            .findFirst()
            .orElseThrow();
    assertEquals("refined", Jdk21Coverage.use(call.getExecutable()).orElseThrow().policy());
  }

  @Test
  void inheritedMembersAndSourceConstructorsUseJavaSignatures() {
    var coverage = ledger();
    assertTrue(coverage.member("java.util.jar.JarFile", "size()").isPresent());
    assertTrue(coverage.member("java.lang.StringBuilder", "length()").isPresent());
    assertTrue(coverage.member("java.awt.geom.Path2D$Double", "<init>()").isPresent());
    assertTrue(coverage.member("javax.swing.JFrame", "<init>(boolean)").isEmpty());
    assertTrue(coverage.member("java.util.ArrayList", "of()").isEmpty());
  }

  @Test
  void anUnmodeledMutationInvalidatesFactsThroughLocalAliases() {
    var result =
        check(
            "java.util",
            """
        class Sample { void f() {
          var values = new java.util.ArrayList<Integer>();
          var alias = values;
          java.util.Collections.shuffle(alias);
          @Refinement("_ == 0") int stale = values.size();
        }}
        """);
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.SHARED_REFINEMENT),
        result.diagnostics().toString());
    assertFalse(
        result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.SYNTAX),
        result.diagnostics().toString());
  }

  @Test
  void inheritedContractsRequireTheirPackageToBeSelected() {
    var result =
        check(
            "java.util.jar",
            """
        class Sample { void f(java.util.jar.JarFile jar) { int size = jar.size(); } }
        """);
    assertTrue(result.accepted(), result.diagnostics().toString());
    var call =
        result.model().getElements(new TypeFilter<>(CtInvocation.class)).stream()
            .filter(c -> c.getExecutable().getSimpleName().equals("size"))
            .findFirst()
            .orElseThrow();
    var use = Jdk21Coverage.use(call.getExecutable()).orElseThrow();
    assertEquals("conservative", use.policy(), use.toString());
    assertEquals("external-contract-not-selected", use.reason());
  }

  @Test
  void nonStaticMemberConstructorsOmitTheJvmEnclosingInstanceParameter() {
    var result =
        check(
            "javax.swing.text",
            """
        class Sample { void f(javax.swing.text.AbstractDocument document) {
          document.new BranchElement(null, null);
        }}
        """);
    assertTrue(result.accepted(), result.diagnostics().toString());
    var call =
        result.model().getElements(new TypeFilter<>(CtConstructorCall.class)).stream()
            .filter(c -> c.getType().getSimpleName().equals("BranchElement"))
            .findFirst()
            .orElseThrow();
    var use = Jdk21Coverage.use(call.getExecutable()).orElseThrow();
    assertEquals("<init>(javax.swing.text.Element,javax.swing.text.AttributeSet)", use.signature());
    assertEquals("conservative", use.policy());
    var borrowed =
        check(
            "javax.swing.text",
            """
        class Sample { void f(@Borrowed javax.swing.text.AbstractDocument document) {
          document.new BranchElement(null, null);
        }}
        """);
    assertEquals(
        List.of(Diagnostic.Kind.PERMISSION),
        borrowed.diagnostics().stream().map(Diagnostic::kind).toList(),
        borrowed.diagnostics().toString());
  }

  @Test
  void opaqueConstructorsDoNotPromiseThatTheReceiverRemainsLocal() {
    var result =
        check(
            "java.util",
            """
        class Sample { void f() {
          @Refinement("_ != null") java.util.Timer timer = new java.util.Timer();
        }}
        """);
    assertEquals(
        List.of(Diagnostic.Kind.SHARED_REFINEMENT),
        result.diagnostics().stream().map(Diagnostic::kind).toList(),
        result.diagnostics().toString());
    var call =
        result.model().getElements(new TypeFilter<>(CtConstructorCall.class)).stream()
            .filter(c -> c.getType().getSimpleName().equals("Timer"))
            .findFirst()
            .orElseThrow();
    assertEquals(Permission.SHARED, LatteFacts.on(call).orElseThrow().permission());
  }
}
