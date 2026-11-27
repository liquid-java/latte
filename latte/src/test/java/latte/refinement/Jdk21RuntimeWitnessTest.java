package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.*;
import javax.crypto.spec.*;
import latte.api.Jdk21Contracts;
import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import spoon.Launcher;
import spoon.reflect.declaration.CtType;

/** Execute the JDK and check its answers against the shipped predicates, not copied formulas. */
class Jdk21RuntimeWitnessTest {
  private static Map<String, CtType<?>> specifications;

  @BeforeAll
  static void readCatalog() {
    var launcher = new Launcher();
    launcher.getEnvironment().setComplianceLevel(21);
    Jdk21Contracts.addTo(launcher, List.of("all"));
    specifications =
        launcher.buildModel().getAllTypes().stream()
            .collect(
                Collectors.toMap(
                    t -> t.getAnnotation(ExternalRefinementsFor.class).value(), t -> t));
  }

  private void witness(
      Class<?> owner, String name, Object receiver, Class<?>[] types, Object... arguments)
      throws Exception {
    var method =
        specifications.get(owner.getName()).getMethodsByName(name).stream()
            .filter(
                m ->
                    m.getParameters().stream()
                        .map(p -> p.getType().getTypeErasure().getQualifiedName())
                        .toList()
                        .equals(Arrays.stream(types).map(Class::getTypeName).toList()))
            .findFirst()
            .orElseThrow();
    Object result;
    try {
      result = owner.getMethod(name, types).invoke(receiver, arguments);
    } catch (InvocationTargetException failure) {
      // These math contracts deliberately describe only normal completion.
      if ((owner == Math.class || owner == StrictMath.class)
          && failure.getCause() instanceof ArithmeticException
          && (name.endsWith("Exact") || name.equals("floorMod") || name.equals("ceilMod"))) return;
      throw failure;
    }
    if (method.getAnnotation(specification.Free.class) != null) {
      // The catalog's Free results are defensive array copies, including empty arrays.
      assertNotSame(receiver, result);
      for (var argument : arguments) assertNotSame(argument, result);
      assertNotSame(result, owner.getMethod(name, types).invoke(receiver, arguments));
    }
    var bindings = new HashMap<String, Term>();
    var references = new IdentityHashMap<Object, Term>();
    bindings.put("_", term(result, references));
    for (int i = 0; i < arguments.length; i++)
      bindings.put(method.getParameters().get(i).getSimpleName(), term(arguments[i], references));
    for (var prefix : List.of("Float", "Double")) {
      var sort = prefix.equals("Float") ? Term.Sort.FLOAT32 : Term.Sort.FLOAT64;
      bindings.put(prefix + ".POSITIVE_INFINITY", new Term.Literal("Infinity", sort));
      bindings.put(prefix + ".NEGATIVE_INFINITY", new Term.Literal("-Infinity", sort));
    }
    var assumptions = Term.TRUE;
    for (var reference : references.entrySet())
      if (reference.getKey().getClass().isArray())
        assumptions =
            Term.and(
                assumptions,
                Term.equal(
                    new Term.Call("length", List.of(reference.getValue())),
                    Term.integer(Array.getLength(reference.getKey()))));
    for (var reference : references.values())
      assumptions = Term.and(assumptions, Term.not(Term.equal(reference, Term.NULL)));
    var annotation = method.getAnnotation(Refinement.class);
    assertNotNull(annotation, method::toString);
    try (var prover =
        new Prover(Map.of("length", new Prover.Function(List.of(Term.Sort.REF), Term.Sort.INT)))) {
      var proof =
          prover.prove(assumptions, PredicateParser.parse(annotation.value()).substitute(bindings));
      assertTrue(
          proof.proven(),
          () ->
              owner.getName()
                  + "."
                  + name
                  + Arrays.toString(arguments)
                  + " returned "
                  + result
                  + "; "
                  + annotation.value()
                  + "; "
                  + proof.detail());
    }
  }

  private Term term(Object value, IdentityHashMap<Object, Term> references) {
    if (value == null) return Term.NULL;
    if (value instanceof Boolean bool) return bool ? Term.TRUE : Term.FALSE;
    if (value instanceof Character character) return Term.integer(character);
    if (value instanceof Float number)
      return new Term.Literal(number.toString(), Term.Sort.FLOAT32);
    if (value instanceof Double number)
      return new Term.Literal(number.toString(), Term.Sort.FLOAT64);
    if (value instanceof Byte
        || value instanceof Short
        || value instanceof Integer
        || value instanceof Long) return Term.integer(((Number) value).longValue());
    return references.computeIfAbsent(
        value, key -> new Term.Symbol("object" + references.size(), Term.Sort.REF));
  }

  private void observers(Class<?> owner, Object receiver, String... names) throws Exception {
    for (var name : names) witness(owner, name, receiver, new Class<?>[0]);
  }

  @Test
  void integralMathIncludesOverflowBoundaries() throws Exception {
    for (var owner : List.of(Math.class, StrictMath.class)) {
      for (int value : new int[] {Integer.MIN_VALUE, -7, -1, 0, 1, 7, Integer.MAX_VALUE}) {
        for (var name :
            List.of("abs", "absExact", "negateExact", "incrementExact", "decrementExact"))
          witness(owner, name, null, new Class<?>[] {int.class}, value);
        for (int other : new int[] {Integer.MIN_VALUE, -3, -1, 0, 1, 3, Integer.MAX_VALUE})
          for (var name :
              List.of("min", "max", "addExact", "subtractExact", "multiplyExact", "floorMod"))
            witness(owner, name, null, new Class<?>[] {int.class, int.class}, value, other);
      }
      for (long value : new long[] {Long.MIN_VALUE, -7, -1, 0, 1, 7, Long.MAX_VALUE}) {
        for (var name :
            List.of(
                "abs", "absExact", "negateExact", "incrementExact", "decrementExact", "toIntExact"))
          witness(owner, name, null, new Class<?>[] {long.class}, value);
        for (long other : new long[] {Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE})
          for (var name :
              List.of("min", "max", "addExact", "subtractExact", "multiplyExact", "floorMod"))
            witness(owner, name, null, new Class<?>[] {long.class, long.class}, value, other);
      }
    }
  }

  @Test
  void java21IntegralContractsIncludeSaturationDivisionOverflowAndBothDivisorSigns()
      throws Exception {
    for (var owner : List.of(Math.class, StrictMath.class)) {
      for (int value : new int[] {Integer.MIN_VALUE, -7, -1, 0, 1, 7, Integer.MAX_VALUE}) {
        for (int divisor : new int[] {Integer.MIN_VALUE, -3, -1, 0, 1, 3, Integer.MAX_VALUE})
          for (var name : List.of("ceilMod", "floorDivExact", "ceilDivExact"))
            witness(owner, name, null, new Class<?>[] {int.class, int.class}, value, divisor);
      }
      for (long value : new long[] {Long.MIN_VALUE, -7, -1, 0, 1, 7, Long.MAX_VALUE}) {
        for (long divisor : new long[] {Long.MIN_VALUE, -3, -1, 0, 1, 3, Long.MAX_VALUE})
          for (var name : List.of("ceilMod", "floorDivExact", "ceilDivExact"))
            witness(owner, name, null, new Class<?>[] {long.class, long.class}, value, divisor);
        witness(
            owner, "clamp", null, new Class<?>[] {long.class, int.class, int.class}, value, -2, 2);
        witness(
            owner,
            "clamp",
            null,
            new Class<?>[] {long.class, int.class, int.class},
            value,
            Integer.MIN_VALUE,
            Integer.MAX_VALUE);
        witness(
            owner,
            "clamp",
            null,
            new Class<?>[] {long.class, long.class, long.class},
            value,
            -2L,
            2L);
        witness(
            owner,
            "clamp",
            null,
            new Class<?>[] {long.class, long.class, long.class},
            value,
            Long.MIN_VALUE,
            Long.MAX_VALUE);
        witness(
            owner,
            "clamp",
            null,
            new Class<?>[] {long.class, long.class, long.class},
            value,
            0L,
            0L);
      }
    }
  }

  @Test
  void everyJdkEnumConstantSatisfiesItsGeneratedOrdinalContract() throws Exception {
    int enums = 0;
    for (var target : specifications.keySet()) {
      var owner = Class.forName(target, false, ClassLoader.getSystemClassLoader());
      if (!owner.isEnum()) continue;
      enums++;
      for (var constant : owner.getEnumConstants())
        witness(owner, "ordinal", constant, new Class<?>[0]);
    }
    assertTrue(enums > 50);
  }

  @Test
  void floatingClassificationIncludesNanInfinitiesAndSignedZero() throws Exception {
    for (double value :
        new double[] {
          Double.NaN,
          Double.NEGATIVE_INFINITY,
          -Double.MAX_VALUE,
          -0.0,
          0,
          Double.MIN_VALUE,
          Double.MAX_VALUE,
          Double.POSITIVE_INFINITY
        }) {
      for (var name : List.of("isNaN", "isFinite", "isInfinite")) {
        witness(Double.class, name, null, new Class<?>[] {double.class}, value);
        witness(Float.class, name, null, new Class<?>[] {float.class}, (float) value);
      }
      witness(
          Double.class, "compare", null, new Class<?>[] {double.class, double.class}, value, 0.0);
      witness(
          Float.class,
          "compare",
          null,
          new Class<?>[] {float.class, float.class},
          (float) value,
          0.0f);
    }
  }

  @Test
  void bitAndUnicodeBoundsIncludeSentinelsAndInvalidCodePoints() throws Exception {
    var random = new Random(17);
    for (int i = 0; i < 24; i++) {
      int value = i == 0 ? 0 : random.nextInt();
      for (var name :
          List.of("bitCount", "numberOfLeadingZeros", "numberOfTrailingZeros", "signum")) {
        witness(Integer.class, name, null, new Class<?>[] {int.class}, value);
        witness(Long.class, name, null, new Class<?>[] {long.class}, random.nextLong());
      }
      witness(Integer.class, "toUnsignedLong", null, new Class<?>[] {int.class}, value);
      for (var name : List.of("toUnsignedInt", "toUnsignedLong")) {
        witness(Byte.class, name, null, new Class<?>[] {byte.class}, (byte) value);
        witness(Short.class, name, null, new Class<?>[] {short.class}, (short) value);
      }
    }
    for (int code :
        new int[] {-1, 0, 48, 0x00bd, 0xffff, 0x10000, 0x10ffff, 0x110000, Integer.MAX_VALUE}) {
      for (var name :
          List.of(
              "charCount",
              "isValidCodePoint",
              "isBmpCodePoint",
              "isSupplementaryCodePoint",
              "getNumericValue"))
        witness(Character.class, name, null, new Class<?>[] {int.class}, code);
      for (int radix : new int[] {-1, 2, 10, 36, 37})
        witness(Character.class, "digit", null, new Class<?>[] {int.class, int.class}, code, radix);
    }
  }

  @Test
  void primitiveCopiesAndSearchesRespectLengthBounds() throws Exception {
    for (Object array :
        List.of(
            new byte[0],
            new short[] {-2, 0, 4},
            new int[] {-2, 0, 4},
            new long[] {-2, 0, 4},
            new char[] {0, 9},
            new float[] {-0.0f, 0.0f, Float.NaN},
            new double[] {0, Double.POSITIVE_INFINITY},
            new boolean[0])) {
      for (int size : new int[] {0, 1, 8})
        witness(
            Arrays.class,
            "copyOf",
            null,
            new Class<?>[] {array.getClass(), int.class},
            array,
            size);
      if (array.getClass() != boolean[].class) {
        var component = array.getClass().getComponentType();
        Object key = Array.get(Array.newInstance(component, 1), 0);
        witness(
            Arrays.class,
            "binarySearch",
            null,
            new Class<?>[] {array.getClass(), component},
            array,
            key);
      }
    }
  }

  @Test
  void rangeCopyAndSearchContractsIncludeEmptyRangesPaddingAndExceptionalExits() throws Exception {
    for (Object array :
        List.of(
            new boolean[] {false, true},
            new byte[] {-1, 2},
            new short[] {-1, 2},
            new char[] {0, 65535},
            new int[] {-1, 2},
            new long[] {-1, 2},
            new float[] {-0.0f, Float.NaN},
            new double[] {Double.NEGATIVE_INFINITY, Double.NaN})) {
      var type = array.getClass();
      var component = type.getComponentType();
      Object original = Array.newInstance(component, 2);
      System.arraycopy(array, 0, original, 0, 2);
      for (int from = 0; from <= 2; from++) {
        for (int to : new int[] {from, 2, 4}) {
          witness(
              Arrays.class,
              "copyOfRange",
              null,
              new Class<?>[] {type, int.class, int.class},
              array,
              from,
              to);
          var copy =
              Arrays.class
                  .getMethod("copyOfRange", type, int.class, int.class)
                  .invoke(null, array, from, to);
          assertNotSame(array, copy);
          assertEquals(to - from, Array.getLength(copy));
          assertTrue(Arrays.deepEquals(new Object[] {original}, new Object[] {array}));
        }
        if (component != boolean.class)
          witness(
              Arrays.class,
              "binarySearch",
              null,
              new Class<?>[] {type, int.class, int.class, component},
              array,
              from,
              2,
              Array.get(array, 0));
      }
      witness(Arrays.class, "mismatch", null, new Class<?>[] {type, type}, array, original);
      Object longer = Array.newInstance(component, 3);
      System.arraycopy(array, 0, longer, 0, 2);
      witness(Arrays.class, "mismatch", null, new Class<?>[] {type, type}, array, longer);
      var failure =
          assertThrows(
              java.lang.reflect.InvocationTargetException.class,
              () ->
                  Arrays.class
                      .getMethod("copyOfRange", type, int.class, int.class)
                      .invoke(null, array, -1, 2));
      assertInstanceOf(IndexOutOfBoundsException.class, failure.getCause());
      assertTrue(Arrays.deepEquals(new Object[] {original}, new Object[] {array}));
    }
  }

  @Test
  void bulkReadFootprintsAndStringCopyMatchActualMutations() throws Exception {
    byte[] input = {7, 8};
    var stream = new ByteArrayInputStream(input);
    byte[] destination = {0, 0};
    witness(
        ByteArrayInputStream.class,
        "read",
        stream,
        new Class<?>[] {byte[].class, int.class, int.class},
        destination,
        0,
        1);
    assertArrayEquals(new byte[] {7, 8}, input);
    assertArrayEquals(new byte[] {7, 0}, destination);
    witness(
        ByteArrayInputStream.class,
        "read",
        stream,
        new Class<?>[] {byte[].class, int.class, int.class},
        destination,
        0,
        2);
    witness(
        ByteArrayInputStream.class,
        "read",
        stream,
        new Class<?>[] {byte[].class, int.class, int.class},
        destination,
        0,
        0);
    var reader = new java.io.StringReader("ab");
    char[] chars = {'x', 'y'};
    witness(
        java.io.StringReader.class,
        "read",
        reader,
        new Class<?>[] {char[].class, int.class, int.class},
        chars,
        1,
        1);
    assertArrayEquals(new char[] {'x', 'a'}, chars);
    "bc".getChars(0, 1, chars, 0);
    assertArrayEquals(new char[] {'b', 'a'}, chars);
  }

  @Test
  void calendarAndNumericBoundsIncludeNegativeAndExtremeValues() throws Exception {
    for (var date :
        List.of(LocalDate.MIN, LocalDate.MAX, LocalDate.of(2000, 2, 29), LocalDate.of(1900, 2, 28)))
      observers(
          LocalDate.class,
          date,
          "getYear",
          "getMonthValue",
          "getDayOfMonth",
          "getDayOfYear",
          "lengthOfMonth",
          "lengthOfYear");
    for (var time : List.of(LocalTime.MIN, LocalTime.MAX, LocalTime.NOON))
      observers(
          LocalTime.class,
          time,
          "getHour",
          "getMinute",
          "getSecond",
          "getNano",
          "toSecondOfDay",
          "toNanoOfDay");
    for (var value :
        List.of(Duration.ZERO, Duration.ofNanos(-1), Duration.ofSeconds(Long.MIN_VALUE)))
      observers(Duration.class, value, "getNano");
    for (var value : List.of(Instant.MIN, Instant.EPOCH, Instant.MAX))
      observers(Instant.class, value, "getNano");
    for (var value : List.of(ZoneOffset.MIN, ZoneOffset.UTC, ZoneOffset.MAX))
      observers(ZoneOffset.class, value, "getTotalSeconds");
    for (var value :
        List.of(
            BigInteger.ZERO,
            BigInteger.ONE,
            BigInteger.valueOf(-1),
            BigInteger.ONE.shiftLeft(1024).negate()))
      observers(BigInteger.class, value, "signum", "bitLength", "bitCount", "getLowestSetBit");
    for (var value : List.of(BigDecimal.ZERO, new BigDecimal("-1E+100"), new BigDecimal("1E-100")))
      observers(BigDecimal.class, value, "signum", "precision");
  }

  @Test
  void unsignedChecksumsAndOptionalRegexGroupsIncludeTheirSentinels() throws Exception {
    for (Checksum checksum : List.of(new CRC32(), new CRC32C(), new Adler32())) {
      observers(checksum.getClass(), checksum, "getValue");
      for (int value : new int[] {0, -1, 255, Integer.MAX_VALUE}) {
        checksum.update(value);
        observers(checksum.getClass(), checksum, "getValue");
      }
    }
    var match = java.util.regex.Pattern.compile("(a)?b").matcher("b");
    assertTrue(match.find());
    observers(java.util.regex.MatchResult.class, match, "start", "end", "groupCount");
    witness(java.util.regex.MatchResult.class, "start", match, new Class<?>[] {int.class}, 1);
    witness(java.util.regex.MatchResult.class, "end", match, new Class<?>[] {int.class}, 1);
    var entry = new ZipEntry("entry");
    observers(ZipEntry.class, entry, "getCrc");
    entry.setCrc(0xffffffffL);
    observers(ZipEntry.class, entry, "getCrc");
  }

  @Test
  void xmlProviderCountsAndDurationSignsMatchContracts() throws Exception {
    var document =
        javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    var root = document.createElement("root");
    document.appendChild(root);
    observers(org.w3c.dom.NodeList.class, root.getChildNodes(), "getLength");
    root.setAttribute("name", "value");
    observers(org.w3c.dom.NamedNodeMap.class, root.getAttributes(), "getLength");
    observers(org.xml.sax.Attributes.class, new org.xml.sax.helpers.AttributesImpl(), "getLength");
    var reader =
        javax.xml.stream.XMLInputFactory.newFactory()
            .createXMLStreamReader(new StringReader("<root a='b' xmlns:p='urn:p'/>"));
    reader.nextTag();
    observers(
        javax.xml.stream.XMLStreamReader.class, reader, "getAttributeCount", "getNamespaceCount");
    reader.close();
    for (var text : List.of("P0D", "P1D", "-P1D"))
      observers(
          javax.xml.datatype.Duration.class,
          javax.xml.datatype.DatatypeFactory.newInstance().newDuration(text),
          "getSign");
  }

  @Test
  void ownershipPromisesMatchCopyingAndRetentionAtRuntime() throws Exception {
    byte[] input = {1};
    var stream = new ByteArrayInputStream(input);
    input[0] = 9;
    stream.close();
    assertEquals(9, stream.read());
    for (var parameters : List.of(new IvParameterSpec(input), new GCMParameterSpec(128, input))) {
      input[0] = 2;
      byte[] copy = (byte[]) parameters.getClass().getMethod("getIV").invoke(parameters);
      assertEquals(9, copy[0]);
      copy[0] = 3;
      byte[] second = (byte[]) parameters.getClass().getMethod("getIV").invoke(parameters);
      assertEquals(9, second[0]);
      assertNotSame(copy, second);
      observers(parameters.getClass(), parameters, "getIV");
    }
  }

  @Test
  void sizeTransitionsAgreeWithRuntimeMutations() throws Exception {
    var list = new ArrayList<Object>();
    transition(ArrayList.class, "add", list, new Class<?>[] {Object.class}, 1);
    transition(ArrayList.class, "add", list, new Class<?>[] {int.class, Object.class}, 0, 2);
    transition(ArrayList.class, "remove", list, new Class<?>[] {int.class}, 1);
    transition(ArrayList.class, "clear", list, new Class<?>[0]);
    var out = new ByteArrayOutputStream();
    transition(ByteArrayOutputStream.class, "write", out, new Class<?>[] {int.class}, 7);
    transition(
        ByteArrayOutputStream.class,
        "write",
        out,
        new Class<?>[] {byte[].class, int.class, int.class},
        new byte[] {1, 2, 3},
        1,
        2);
    out.close();
    transition(ByteArrayOutputStream.class, "write", out, new Class<?>[] {int.class}, 8);
    transition(ByteArrayOutputStream.class, "reset", out, new Class<?>[0]);
  }

  private void transition(
      Class<?> owner, String name, Object receiver, Class<?>[] types, Object... arguments)
      throws Exception {
    var contract =
        specifications.get(owner.getName()).getMethodsByName(name).stream()
            .filter(
                m ->
                    m.getParameters().stream()
                        .map(p -> p.getType().getTypeErasure().getQualifiedName())
                        .toList()
                        .equals(Arrays.stream(types).map(Class::getTypeName).toList()))
            .findFirst()
            .orElseThrow();
    var annotation = contract.getAnnotation(StateRefinement.class);
    assertNotNull(annotation);
    long before = observedSize(receiver);
    owner.getMethod(name, types).invoke(receiver, arguments);
    long after = observedSize(receiver);
    String ghost = receiver instanceof List<?> ? "listSize" : "byteCount";
    var ref = new Term.Symbol("receiver", Term.Sort.REF);
    var previous = new Term.Call("old", List.of(ref));
    var assumptions =
        Term.and(
            Term.equal(new Term.Call(ghost, List.of(previous)), Term.integer(before)),
            Term.equal(new Term.Call(ghost, List.of(ref)), Term.integer(after)));
    var bindings = new HashMap<String, Term>();
    bindings.put("this", ref);
    var references = new IdentityHashMap<Object, Term>();
    for (int i = 0; i < arguments.length; i++)
      bindings.put(contract.getParameters().get(i).getSimpleName(), term(arguments[i], references));
    try (var prover =
        new Prover(
            Map.of(
                "old",
                new Prover.Function(List.of(Term.Sort.REF), Term.Sort.REF),
                ghost,
                new Prover.Function(List.of(Term.Sort.REF), Term.Sort.INT)))) {
      var proof =
          prover.prove(assumptions, PredicateParser.parse(annotation.to()).substitute(bindings));
      assertTrue(proof.proven(), owner + "." + name + ": " + proof.detail());
    }
  }

  private long observedSize(Object receiver) {
    return receiver instanceof List<?> list
        ? list.size()
        : ((ByteArrayOutputStream) receiver).size();
  }

  @Test
  void desktopDataAndNamingObserversNeedNoDisplayOrNetwork() throws Exception {
    for (var color :
        List.of(java.awt.Color.BLACK, java.awt.Color.WHITE, new java.awt.Color(1, 127, 255, 0)))
      observers(java.awt.Color.class, color, "getRed", "getGreen", "getBlue", "getAlpha");
    var image = new java.awt.image.BufferedImage(2, 3, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    observers(
        java.awt.image.BufferedImage.class,
        image,
        "getWidth",
        "getHeight",
        "getNumXTiles",
        "getNumYTiles");
    observers(java.awt.print.Book.class, new java.awt.print.Book(), "getNumberOfPages");
    var model = new javax.swing.table.DefaultTableModel(2, 3);
    observers(javax.swing.table.TableModel.class, model, "getRowCount", "getColumnCount");
    observers(javax.swing.ListModel.class, new javax.swing.DefaultListModel<>(), "getSize");
    var document = new javax.swing.text.PlainDocument();
    document.insertString(0, "abc", null);
    observers(javax.swing.text.Document.class, document, "getLength");
    observers(
        javax.print.attribute.standard.Copies.class,
        new javax.print.attribute.standard.Copies(1),
        "getValue");
    observers(javax.naming.Name.class, new javax.naming.CompositeName("a/b"), "size");
    observers(
        javax.naming.directory.Attribute.class,
        new javax.naming.directory.BasicAttribute("a", "b"),
        "size");
    observers(
        javax.management.relation.RoleInfo.class,
        new javax.management.relation.RoleInfo(
            "role", Object.class.getName(), true, true, 0, -1, "description"),
        "getMinDegree",
        "getMaxDegree");
    observers(
        javax.management.timer.Timer.class,
        new javax.management.timer.Timer(),
        "getNbNotifications");
    observers(
        javax.security.auth.callback.ChoiceCallback.class,
        new javax.security.auth.callback.ChoiceCallback("choose", new String[] {"a"}, 0, false),
        "getDefaultChoice");
    observers(
        javax.sql.rowset.serial.SerialBlob.class,
        new javax.sql.rowset.serial.SerialBlob(new byte[0]),
        "length");
    observers(
        javax.sql.rowset.serial.SerialClob.class,
        new javax.sql.rowset.serial.SerialClob(new char[] {'a'}),
        "length");
    try (var input =
        new javax.imageio.stream.MemoryCacheImageInputStream(
            new ByteArrayInputStream(new byte[] {-1, -1, -1}))) {
      observers(
          javax.imageio.stream.ImageInputStream.class,
          input,
          "getBitOffset",
          "getStreamPosition",
          "readUnsignedByte",
          "readUnsignedShort");
    }
  }

  @Test
  void smartCardContractsDescribeBytesWithoutNeedingAHardwareDevice() throws Exception {
    for (int expected : new int[] {0, 1, 256, 65536}) {
      var command = new javax.smartcardio.CommandAPDU(255, 128, 127, 0, expected);
      observers(
          javax.smartcardio.CommandAPDU.class,
          command,
          "getCLA",
          "getINS",
          "getP1",
          "getP2",
          "getNc",
          "getNe",
          "getBytes",
          "getData");
      var copy = command.getBytes();
      copy[0] = 0;
      assertEquals(255, command.getCLA());
    }
    byte[] bytes = {1, (byte) 0x90, 0};
    var response = new javax.smartcardio.ResponseAPDU(bytes);
    bytes[0] = 2;
    assertEquals(1, response.getData()[0]);
    observers(
        javax.smartcardio.ResponseAPDU.class,
        response,
        "getNr",
        "getSW",
        "getSW1",
        "getSW2",
        "getBytes",
        "getData");
  }

  @Test
  void omittedBoundsHaveConcreteCounterexamples() throws Exception {
    var entry = new ZipEntry("entry");
    entry.setCompressedSize(-9);
    assertEquals(-9, entry.getCompressedSize());
    assertTrue(
        specifications
            .get(ZipEntry.class.getName())
            .getMethodsByName("getCompressedSize")
            .isEmpty());
    var stream = new ByteArrayInputStream(new byte[1], 3, 0);
    assertEquals(-2, stream.available());
    assertEquals(-2, stream.skip(1));
    assertTrue(
        specifications
            .get(ByteArrayInputStream.class.getName())
            .getMethodsByName("available")
            .isEmpty());
    assertTrue(
        specifications
            .get(ByteArrayInputStream.class.getName())
            .getMethodsByName("skip")
            .isEmpty());
    var uri = URI.create("http://example.test:70000/");
    assertEquals(70000, uri.getPort());
    observers(URI.class, uri, "getPort");
    assertEquals(-5, new java.util.concurrent.Semaphore(-5).availablePermits());
    assertEquals(-5, new java.text.ParsePosition(-5).getIndex());
    assertTrue(new BigDecimal("1E+10").scale() < 0);
    assertSame(Integer.valueOf(1), Integer.valueOf(1));
    assertSame(LocalTime.of(0, 0), LocalTime.of(0, 0));
  }
}
