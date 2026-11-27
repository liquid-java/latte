package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.ToolProvider;
import latte.api.Diagnostic;
import latte.api.Jdk21Contracts;
import latte.api.Verification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spoon.Launcher;
import spoon.support.compiler.VirtualFile;

class Jdk21ContractsTest {
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

  private void accepts(String packages, String source) {
    var result = check(packages, source);
    assertTrue(result.accepted(), result.diagnostics().toString());
  }

  private void rejects(String packages, String source, Diagnostic.Kind kind) {
    var result = check(packages, source);
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == kind),
        result.diagnostics().toString());
    assertFalse(
        result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.SYNTAX),
        result.diagnostics().toString());
  }

  @Test
  void documentedExamplesHaveExactlyTheirExpectedDiagnostics() throws Exception {
    try (var files = Files.list(Path.of("../examples/jdk21"))) {
      for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        var expected = new java.util.ArrayList<String>();
        var lines = Files.readAllLines(file);
        for (int i = 0; i < lines.size(); i++)
          if (lines.get(i).contains("// Expect: "))
            expected.add((i + 1) + ":" + lines.get(i).split("// Expect: ")[1].trim());
        var result = Verification.verify(List.of(file.toString()), List.of(), List.of("all"));
        assertEquals(
            expected.stream().sorted().toList(),
            result.diagnostics().stream().map(d -> d.line() + ":" + d.kind()).sorted().toList(),
            file + ": " + result.diagnostics());
      }
    }
  }

  @Test
  void completeCatalogHasValidJavaSignaturesAndPredicates() {
    accepts("all", "class Sample {}");
  }

  @Test
  void specificationsCompileAgainstJava21(@TempDir Path output) throws Exception {
    try (var paths = Files.walk(Path.of("../specifications/jdk21"));
        var files = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null)) {
      var sources = paths.filter(p -> p.toString().endsWith(".java")).map(Path::toFile).toList();
      var options =
          List.of("--release", "21", "-classpath", "target/classes", "-d", output.toString());
      assertTrue(
          ToolProvider.getSystemJavaCompiler()
              .getTask(null, files, null, options, null, files.getJavaFileObjectsFromFiles(sources))
              .call());
    }
  }

  @Test
  void catalogManifestAccountsForEverySpecification() throws Exception {
    var root = Path.of("../specifications/jdk21");
    var catalog =
        JsonParser.parseString(Files.readString(root.resolve("catalog.json"))).getAsJsonObject();
    assertEquals(21, catalog.get("javaRelease").getAsInt());
    var entries = catalog.getAsJsonArray("specifications");
    var paths = new java.util.HashSet<Path>();
    var targets = new java.util.HashSet<String>();
    for (var value : entries) {
      var entry = value.getAsJsonObject();
      assertTrue(targets.add(entry.get("target").getAsString()));
      var path = root.resolve(entry.get("file").getAsString());
      assertTrue(paths.add(path));
      assertTrue(Files.exists(path));
      assertTrue(
          entry
              .get("documentation")
              .getAsString()
              .startsWith("https://docs.oracle.com/en/java/javase/21/"));
      assertTrue(entry.get("sourceSha256").getAsString().matches("[0-9a-f]{64}"));
    }
    try (var all = Files.walk(root)) {
      assertEquals(
          paths,
          all.filter(p -> p.toString().endsWith(".java"))
              .collect(java.util.stream.Collectors.toSet()));
    }
  }

  @Test
  void packageInventoryIsCompleteAndAgreesWithTheCatalog() throws Exception {
    var root = Path.of("../specifications/jdk21");
    var inventory =
        JsonParser.parseString(Files.readString(root.resolve("packages.json")))
            .getAsJsonObject()
            .getAsJsonArray("packages");
    assertEquals(178, inventory.size());
    var names = new java.util.HashSet<String>();
    var targets = new java.util.HashSet<String>();
    var selected = new java.util.HashSet<String>();
    for (var value : inventory) {
      var entry = value.getAsJsonObject();
      var name = entry.get("package").getAsString();
      assertTrue(names.add(name));
      assertTrue(entry.get("module").getAsString().startsWith("java."));
      var types = entry.getAsJsonArray("targets");
      assertEquals("covered", entry.get("status").getAsString());
      assertEquals(
          entry.get("members").getAsInt(),
          entry.get("refinedMembers").getAsInt() + entry.get("conservativeMembers").getAsInt());
      selected.add(name);
      for (var type : types) {
        assertTrue(type.getAsString().startsWith(name + "."));
        assertTrue(targets.add(type.getAsString()));
      }
    }
    assertEquals(Jdk21Contracts.packages(), selected);
    var catalog =
        JsonParser.parseString(Files.readString(root.resolve("catalog.json")))
            .getAsJsonObject()
            .getAsJsonArray("specifications");
    assertEquals(
        catalog.asList().stream()
            .map(v -> v.getAsJsonObject().get("target").getAsString())
            .collect(java.util.stream.Collectors.toSet()),
        targets);
  }

  @Test
  void packageSelectionIsExactAndRejectsTypos() {
    assertTrue(Jdk21Contracts.packages().contains("java.util"));
    assertThrows(
        IllegalArgumentException.class,
        () -> Jdk21Contracts.addTo(new Launcher(), List.of("java.utli")));
    assertThrows(
        IllegalArgumentException.class,
        () -> Jdk21Contracts.addTo(new Launcher(), List.of("all", "typo")));
    var launcher = new Launcher();
    Jdk21Contracts.addTo(launcher, List.of("java.util", "java.util"));
    assertTrue(
        launcher.buildModel().getAllTypes().stream()
            .allMatch(t -> t.getPackage().getQualifiedName().equals("latte.jdk21.java.util")));
  }

  @Test
  void arithmeticAndIeeeSpecialValuesAreUsable() {
    accepts(
        "java.lang",
        """
        class Sample { void f() {
          @Refinement("_ == -2147483648") int minimum = Math.abs(Integer.MIN_VALUE);
          @Refinement("_ == 7") long sum = Math.addExact(3L, 4L);
          @Refinement("_ >= -6 && _ <= 0") int remainder = Math.floorMod(3, -7);
          @Refinement("_") boolean nan = Double.isNaN(Double.NaN);
          @Refinement("!_") boolean finite = Float.isFinite(Float.POSITIVE_INFINITY);
          @Refinement("_ == 4294967295") long unsigned = Integer.toUnsignedLong(-1);
        }}
        """);
    rejects(
        "java.lang",
        """
        class Sample { void f() {
          @Refinement("_ >= 0") int incorrect = Math.abs(Integer.MIN_VALUE);
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void java21ArithmeticContractsProveExactResultsAndRejectInvalidClampRanges() {
    accepts(
        "java.lang",
        """
        class Sample { void f() {
          @Refinement("_ == 100") int high = Math.clamp(Long.MAX_VALUE, 0, 100);
          @Refinement("_ == -10") long low = StrictMath.clamp(Long.MIN_VALUE, -10L, 10L);
          @Refinement("_ == -3") int floor = Math.floorDivExact(-7, 3);
          @Refinement("_ == -2") long ceiling = StrictMath.ceilDivExact(-7L, 3L);
          @Refinement("_ <= 0 && _ > -3") int remainder = Math.ceilMod(7, 3);
        }}
        """);
    rejects(
        "java.lang",
        "class Sample { void f() { Math.clamp(2L, 10, 0); } }",
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void java21SequencedEndsUpdateTheSameLocalListState() {
    accepts(
        "java.util",
        """
        class Sample { void f() {
          var list = new java.util.ArrayList<Integer>(); var alias = list;
          alias.addFirst(1); list.addLast(2);
          @Refinement("_ == 2") int size = alias.size();
          list.getFirst();
        }}
        """);
    rejects(
        "java.util",
        "class Sample { void f() { new java.util.ArrayList<Integer>().getFirst(); }}",
        Diagnostic.Kind.STATE);
    rejects(
        "java.util",
        "class Sample { void f() { new java.util.ArrayList<Integer>().removeLast(); }}",
        Diagnostic.Kind.STATE);
  }

  @Test
  void java21ViewsRemainSharedAndVirtualThreadsCannotRetainBorrowedCaptures() {
    rejects(
        "java.util",
        """
        class Sample { void f(java.util.List<Integer> list) {
          @Refinement("_ != null") java.util.List<Integer> reversed = list.reversed();
        }}
        """,
        Diagnostic.Kind.SHARED_REFINEMENT);
    rejects(
        "java.lang",
        """
        class Sample { void f(@Borrowed int[] values) {
          Thread.startVirtualThread(() -> values[0]++);
        }}
        """,
        Diagnostic.Kind.PERMISSION);
  }

  @Test
  void enumBoundsWorkOnSharedReferencesAndUnprovedCounterBoundsAreRejected() {
    accepts(
        "java.lang,java.util,java.lang.annotation,java.time",
        """
        class Sample { void f(java.lang.annotation.RetentionPolicy policy, java.time.Month month) {
          @Refinement("_ >= 0 && _ < 3") int retention = policy.ordinal();
          @Refinement("_ >= 0 && _ < 12") int monthIndex = month.ordinal();
        }}
        """);
    rejects(
        "java.lang",
        """
        class Sample { void f(Thread thread) {
          @Refinement("_ > 0") long id = thread.threadId();
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
    rejects(
        "java.util",
        """
        class Sample { void f(java.util.Collection<?> values) {
          @Refinement("_ >= 0") int count = values.size();
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
    rejects(
        "java.util",
        """
        class Sample { void f(java.util.Map<?, ?> values) {
          @Refinement("_ >= 0") int count = values.size();
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void localListAliasesShareSizeTransitions() {
    accepts(
        "java.util",
        """
        class Sample { void f() {
          var list = new java.util.ArrayList<Integer>(); var alias = list;
          alias.add(1);
          @Refinement("_ == 1") int size = list.size();
          alias.clear();
          @Refinement("_") boolean empty = list.isEmpty();
        }}
        """);
    rejects(
        "java.util",
        """
        class Sample { void f() {
          var list = new java.util.ArrayList<Integer>(); list.get(0);
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void sharedListCannotAcquireStateRefinements() {
    rejects(
        "java.util",
        "class Sample { void f(java.util.ArrayList<Integer> list) { list.size(); }}",
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void primitiveArrayCopiesAreOwnedAndMutationsInvalidateContents() {
    accepts(
        "java.util",
        """
        class Sample { void f(@Borrowed @Refinement("_ != null") int[] values) {
          @Refinement("_ != null && length(_) == 3") int[] copy = java.util.Arrays.copyOf(values, 3);
        }}
        """);
    rejects(
        "java.util",
        """
        class Sample { void f() {
          int[] values = {0}; java.util.Arrays.fill(values, 7);
          @Refinement("_ == 0") int stale = values[0];
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void rangeGuardsRejectOverflowAndNegativeLengths() {
    rejects(
        "java.util",
        """
        class Sample { void f() {
          java.util.Objects.checkFromIndexSize(2147483647, 1, 2147483647);
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
    rejects(
        "java.util",
        "class Sample { void f() { java.util.Arrays.copyOf(new int[1], -1); }}",
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void outputStreamCopiesAndCloseDoesNotDisableWriting() {
    accepts(
        "java.io",
        """
        class Sample { void f() throws java.io.IOException {
          var out = new java.io.ByteArrayOutputStream(); var alias = out;
          out.write(7); alias.close(); alias.write(8);
          @Refinement("_ == 2") int size = out.size();
          @Refinement("_ != null && length(_) == 2") byte[] copy = out.toByteArray();
          out.reset(); @Refinement("_ == 0") int empty = alias.size();
        }}
        """);
  }

  @Test
  void inputStreamRetainsItsArray() {
    rejects(
        "java.io",
        """
        class Sample { void f(@Borrowed byte[] bytes) {
          new java.io.ByteArrayInputStream(bytes);
        }}
        """,
        Diagnostic.Kind.PERMISSION);
  }

  @Test
  void calendarAndNumericObserversWorkOnSharedValues() {
    accepts(
        "java.time,java.math,java.net",
        """
        class Sample { void f(java.time.LocalDate day, java.time.Duration duration,
            java.math.BigDecimal value, java.net.URI uri) {
          @Refinement("_ >= 1 && _ <= 12") int month = day.getMonthValue();
          @Refinement("_ >= 0 && _ < 1000000000") int nanos = duration.getNano();
          @Refinement("_ >= 1") int precision = value.precision();
          @Refinement("_ >= -1") int port = uri.getPort();
        }}
        """);
  }

  @Test
  void copyingCryptoConstructorsAcceptBorrowedArrays() {
    accepts(
        "javax.crypto.spec",
        """
        class Sample { void f(@Borrowed @Refinement("_ != null") byte[] iv) {
          var parameters = new javax.crypto.spec.GCMParameterSpec(128, iv);
          @Refinement("_ != null") byte[] copy = parameters.getIV();
        }}
        """);
  }

  @Test
  void interfaceContractsReachXmlImplementations() {
    accepts(
        "org.xml.sax",
        """
        class Sample { void f(org.xml.sax.helpers.AttributesImpl attributes) {
          @Refinement("_ >= 0") int count = attributes.getLength();
        }}
        """);
  }

  @Test
  void unlistedFactoryDoesNotPromiseUniqueCachedValues() {
    rejects(
        "java.lang",
        """
        class Sample { void f() {
          @Refinement("_ != null") Integer cached = Integer.valueOf(1);
        }}
        """,
        Diagnostic.Kind.SHARED_REFINEMENT);
  }
}
