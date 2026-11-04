package latte.compatibility;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import latte.api.Diagnostic;
import org.junit.jupiter.api.Test;

class ReferenceFreshnessTest {
  @Test
  void aFreshObjectCannotEqualAPreexistingParameter() throws Exception {
    var outcome =
        CompatibilityAudit.run(CompatibilityAudit.ROOT.resolve("ErrorIfReferenceComparison.java"));
    assertNull(outcome.crash());
    // Upstream also expects line 21, but other == new Door() is unreachable in Java.
    // The two feasible double-close errors must still be reported.
    assertEquals(List.of(30, 39), outcome.actual().stream().map(Diagnostic::line).toList());
    assertTrue(outcome.actual().stream().allMatch(d -> d.kind() == Diagnostic.Kind.STATE));
    assertFalse(outcome.matches());
  }
}
