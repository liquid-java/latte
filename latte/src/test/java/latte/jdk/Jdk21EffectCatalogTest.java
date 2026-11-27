package latte.jdk;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class Jdk21EffectCatalogTest {
  private static final Path ROOT = Path.of("../specifications/jdk21");

  @Test
  void everyPackageAndDeclaredContractHasAnExplicitEffectReview() throws Exception {
    var actual = Jdk21EffectCatalog.collect(ROOT);
    var saved =
        new Gson()
            .fromJson(
                Files.readString(ROOT.resolve("effects.json")), Jdk21EffectCatalog.Catalog.class);
    assertEquals(actual.javaRelease(), saved.javaRelease());
    assertEquals(actual.omittedFootprintPolicy(), saved.omittedFootprintPolicy());
    assertEquals(actual.packages().size(), saved.packages().size());
    for (int i = 0; i < actual.packages().size(); i++) {
      var expected = actual.packages().get(i);
      var recorded = saved.packages().get(i);
      String context = expected.name() + ": regenerate after changing annotations or evidence";
      assertEquals(expected.name(), recorded.name(), context);
      assertEquals(expected.apiMembers(), recorded.apiMembers(), context);
      assertEquals(expected.readOnlyFootprints(), recorded.readOnlyFootprints(), context);
      assertEquals(expected.mutatingFootprints(), recorded.mutatingFootprints(), context);
      assertEquals(expected.withoutFootprint(), recorded.withoutFootprint(), context);
      assertEquals(expected.viewResults(), recorded.viewResults(), context);
      assertEquals(expected.summaries().size(), recorded.summaries().size(), context);
      for (int j = 0; j < expected.summaries().size(); j++)
        assertEquals(expected.summaries().get(j), recorded.summaries().get(j), context);
    }
    assertEquals(178, saved.packages().size());
    assertEquals(
        47030,
        saved.packages().stream().mapToInt(Jdk21EffectCatalog.PackageEffects::apiMembers).sum());
    var declarations = new HashSet<String>();
    for (var p : saved.packages()) {
      assertEquals(
          p.apiMembers(), p.readOnlyFootprints() + p.mutatingFootprints() + p.withoutFootprint());
      for (var s : p.summaries()) {
        assertTrue(declarations.add(s.target() + "#" + s.signature()));
        assertFalse(s.evidence().isBlank());
        if (s.returnsView() != null) {
          assertEquals(List.of(), s.modifies());
          assertEquals("derived-from-backing", s.resultOwnership());
        }
      }
    }
    assertEquals(501, declarations.size());
    try (var stream = getClass().getResourceAsStream("/latte/jdk21/effects.json")) {
      assertNotNull(stream, "Effect reviews must also ship in the JAR");
      assertEquals(
          Files.readString(ROOT.resolve("effects.json")),
          new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
    }
  }

  @Test
  void effectReviewsDistinguishCopiesCachesCallbacksAndViews() throws Exception {
    var summaries = new HashMap<String, Jdk21EffectCatalog.Summary>();
    Jdk21EffectCatalog.collect(ROOT)
        .packages()
        .forEach(
            p -> p.summaries().forEach(s -> summaries.put(s.target() + "#" + s.signature(), s)));
    assertEquals(
        List.of(), summaries.get("java.util.Arrays#copyOfRange(int[],int,int)").modifies());
    assertEquals(
        "free", summaries.get("java.util.Arrays#copyOfRange(int[],int,int)").resultOwnership());
    assertEquals(
        List.of("array[*]"), summaries.get("java.util.Arrays#fill(int[],int,int,int)").modifies());
    assertEquals(List.of("this.*"), summaries.get("java.math.BigInteger#bitLength()").modifies());
    assertEquals(
        List.of(), summaries.get("java.util.concurrent.CountDownLatch#getCount()").modifies());
    assertEquals(List.of(), summaries.get("java.util.ArrayList#getFirst()").modifies());
    assertEquals(List.of(), summaries.get("java.util.ArrayList#getLast()").modifies());
    for (var signature :
        List.of(
            "java.math.BigDecimal#precision()",
            "java.awt.Color#getRed()",
            "java.lang.Math#random()",
            "java.util.Random#nextInt(int)",
            "javax.swing.ListModel#getSize()"))
      assertNull(summaries.get(signature).modifies(), signature);
    for (var signature :
        List.of(
            "java.util.Arrays#sort(java.lang.Object[])",
            "java.util.List#subList(int,int)",
            "java.util.List#reversed()",
            "java.util.Collections#unmodifiableList(java.util.List)"))
      assertFalse(summaries.containsKey(signature), signature);
    assertEquals(
        Map.of("of", "this", "from", "fromIndex", "toExclusive", "toIndex"),
        summaries.get("java.util.ArrayList#subList(int,int)").returnsView());
  }
}
