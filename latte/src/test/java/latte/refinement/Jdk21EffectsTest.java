package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.support.compiler.VirtualFile;

class Jdk21EffectsTest {
  private Verification.Result check(String packages, String source) {
    var launcher = new Launcher();
    launcher.getEnvironment().setComplianceLevel(21);
    launcher.getEnvironment().setNoClasspath(true);
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
  void arithmeticAndNullQueriesPreserveLocalFieldsAndArrays() {
    accepts(
        "java.lang,java.util",
        """
        class Sample { static int field; void run() {
          field = 7; int[] array = {9};
          Math.abs(-5); StrictMath.addExact(1, 2); Integer.bitCount(7);
          java.util.Objects.nonNull(array); java.util.Objects.checkIndex(0, 1);
          @Refinement("_ == 7") int a = field;
          @Refinement("_ == 9") int b = array[0];
        }}
        """);
  }

  @Test
  void everyPrimitiveCopyRangePreservesInputAndHasFreshPaddedLength() {
    for (String type :
        List.of("boolean", "byte", "short", "char", "int", "long", "float", "double"))
      accepts(
          "java.util",
          """
          class Sample { void run() {
            %s[] source = new %s[2];
            %s[] copy = java.util.Arrays.copyOfRange(source, 1, 4);
            @Refinement("_ == 3") int n = copy.length;
            @Refinement("_ == 2") int original = source.length;
            java.util.Arrays.fill(copy, %s);
            @Refinement("_ == %s") %s unchanged = source[0];
          }}
          """
              .formatted(
                  type,
                  type,
                  type,
                  type.equals("boolean") ? "true" : "(" + type + ") 1",
                  type.equals("boolean") ? "false" : "0",
                  type));
  }

  @Test
  void rangeWritesInvalidateAliasesButPreserveUnrelatedArrays() {
    accepts(
        "java.util",
        """
        class Sample { void run() {
          int[] array = {2,1}; int[] other = {9};
          java.util.Arrays.fill(array, 0, 1, 3); java.util.Arrays.sort(array, 0, 2);
          @Refinement("_ == 9") int n = other[0];
        }}
        """);
    rejects(
        "java.util",
        """
        class Sample { void run() {
          int[] array = {7}; int[] alias = array;
          java.util.Arrays.fill(array, 0, 1, 3);
          @Refinement("_ == 7") int stale = alias[0];
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void primitiveComparisonsAreReadOnlyAndNullTolerantWhereDocumented() {
    accepts(
        "java.util",
        """
        class Sample { void run() {
          int[] array = {7}; int[] other = {9};
          java.util.Arrays.equals(array, other); java.util.Arrays.hashCode(array);
          java.util.Arrays.compare(array, (int[]) null);
          java.util.Arrays.equals(array, (int[]) null);
          @Refinement("_ >= -1 && _ <= 1") int m = java.util.Arrays.mismatch(array, other);
          @Refinement("_ == 7") int unchanged = array[0];
          @Refinement("_ >= -2 && _ < 1") int b = java.util.Arrays.binarySearch(array, 0, 1, 7);
        }}
        """);
  }

  @Test
  void outputStreamWritesBorrowInputsWithoutInvalidatingThem() {
    accepts(
        "java.io,java.util",
        """
        class Sample { void run() {
          byte[] input = {7,8}; var output = new java.io.ByteArrayOutputStream();
          output.write(input, 0, 1); java.util.Arrays.copyOfRange(input, 0, 1);
          output.write(input, 1, 1);
          @Refinement("_ == 7") byte n = input[0];
          @Refinement("_ == 2") int count = output.size();
          byte[] copy = output.toByteArray(); copy[0] = 3;
          @Refinement("_ == 2") int still = output.size();
        }}
        """);
  }

  @Test
  void bulkReadsWriteDestinationSlotsAndPreserveOtherStorage() {
    accepts(
        "java.io",
        """
        class Sample { void run() {
          byte[] input = {7}; byte[] destination = {0}; int[] other = {9};
          var stream = new java.io.ByteArrayInputStream(input);
          @Refinement("_ >= -1 && _ <= 1") int count = stream.read(destination, 0, 1);
          @Refinement("_ == 9") int n = other[0];
        }}
        """);
    rejects(
        "java.io",
        """
        class Sample { void run() {
          char[] destination = {'x'}; var reader = new java.io.StringReader("a");
          reader.read(destination, 0, 1);
          @Refinement("_ == 120") char stale = destination[0];
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void stringCopyWritesOnlyItsDestination() {
    accepts(
        "java.lang",
        """
        class Sample { void run() {
          String source = new String(new char[]{'a'}); char[] destination = {'x'}; int[] other = {9};
          source.getChars(0, 1, destination, 0);
          @Refinement("_ == 1") int n = source.length();
          @Refinement("_ == 9") int unchanged = other[0];
        }}
        """);
    rejects(
        "java.lang",
        """
        class Sample { void run() {
          String source = new String(new char[]{'a'}); char[] destination = {'x'};
          source.getChars(0, 1, destination, 0);
          @Refinement("_ == 120") char stale = destination[0];
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void checksumUpdatesAndDefensiveConstructorsPreserveBorrowedInputs() {
    for (String checksum : List.of("CRC32", "CRC32C", "Adler32"))
      accepts(
          "java.util.zip,javax.crypto.spec,javax.smartcardio",
          """
          class Sample { void run() {
            byte[] input = {7,0,0,0}; var crc = new java.util.zip.%s(); crc.update(input,0,4);
            var iv = new javax.crypto.spec.IvParameterSpec(input);
            var gcm = new javax.crypto.spec.GCMParameterSpec(128,input);
            var apdu = new javax.smartcardio.CommandAPDU(input);
            @Refinement("_ == 7") byte n = input[0];
            byte[] copy = iv.getIV(); copy[0] = 1;
            @Refinement("_ == 7") byte still = input[0];
          }}
          """
              .formatted(checksum));
  }

  @Test
  void valueObserversAndReceiverCachesPreserveUnrelatedFields() {
    accepts(
        "java.time,java.math",
        """
        class Sample { static int field; void run(@Borrowed java.time.LocalDate date,
            @Borrowed java.math.BigInteger integer) {
          field = 7; date.getDayOfYear(); integer.bitLength(); integer.bitCount();
          @Refinement("_ == 7") int n = field;
        }}
        """);
  }

  @Test
  void globalCachesRandomnessAndProviderCallbacksKeepUnknownEffects() {
    for (String call :
        List.of("Math.random()", "StrictMath.random()", "decimal.precision()", "model.getSize()"))
      rejects(
          "java.lang,java.math,javax.swing",
          """
          class Sample { static int field; void run(@Borrowed java.math.BigDecimal decimal,
              @Borrowed javax.swing.ListModel<Object> model) {
            field = 7; %s;
            @Refinement("_ == 7") int stale = field;
          }}
          """
              .formatted(call),
          Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void readOnlyConcurrencySummariesDoNotEstablishStableSnapshots() {
    rejects(
        "java.util.concurrent",
        """
        class Sample { void run(@Borrowed java.util.concurrent.CountDownLatch latch) {
          long before = latch.getCount();
          @Refinement("_ == before") long after = latch.getCount();
        }}
        """,
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void bundledViewsPropagateAliasSizesAndEscape() {
    accepts(
        "java.util",
        """
        class Sample { void run() {
          var list = new java.util.ArrayList<Integer>(); list.add(1);
          var view = list.subList(0,1); var alias = view; alias.add(2);
          @Refinement("_ == 2") int n = list.size();
          view.subList(0,1).clear();
          @Refinement("_ == 1") int m = alias.size();
        }}
        """);
    rejects(
        "java.util",
        """
        class Sample { static Object escaped; void run() {
          var list = new java.util.ArrayList<Integer>(); escaped = list.subList(0,0);
          list.size();
        }}
        """,
        Diagnostic.Kind.SHARED_REFINEMENT);
  }
}
