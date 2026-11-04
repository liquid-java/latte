package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.support.compiler.VirtualFile;

class FloatingPointTest {
  private Verification.Result verify(String body) {
    return Verification.verify(
        new VirtualFile(
            "import liquidjava.specification.*; class Numeric {" + body + "}", "Numeric.java"));
  }

  private void accepts(String body) {
    var result = verify(body);
    assertTrue(result.accepted(), result.diagnostics().toString());
  }

  private void rejects(String body) {
    var result = verify(body);
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.REFINEMENT),
        result.diagnostics().toString());
  }

  @Test
  void roundedAdditionDiffersFromRealArithmetic() {
    accepts("void f(){@Refinement(\"_!=0.3\") double x=0.1+0.2;}");
    rejects("void f(){@Refinement(\"_==0.3\") double x=0.1+0.2;}");
  }

  @Test
  void singlePrecisionRoundsAtEachOperation() {
    accepts(
        "void f(){float large=16777216f;@Refinement(\"_==16777216\") float sum=large+1f;@Refinement(\"_==16777217\") double wider=large+1d;}");
  }

  @Test
  void signedZeroAndInfinitiesSurviveOperations() {
    accepts(
        "void f(){double zero=-0.0;@Refinement(\"_==Double.NEGATIVE_INFINITY\") double x=1.0/zero;@Refinement(\"_==Double.POSITIVE_INFINITY\") double y=1.0/0.0;}");
  }

  @Test
  void nanUsesIeeeComparisons() {
    accepts(
        "void f(){@Refinement(\"_!=_\") double x=0.0/0.0;@Refinement(\"!(_>0)&&!(_<=0)\") double y=Double.NaN;}");
    rejects("@Refinement(\"_==_\") double f(double x){return x;}");
  }

  @Test
  void remainderTruncatesRatherThanRoundsTheQuotient() {
    accepts(
        "void f(){@Refinement(\"_==2\") double x=6.0%4.0;@Refinement(\"_==-2\") double y=-6.0%4.0;@Refinement(\"_==3\") double z=3.0%Double.POSITIVE_INFINITY;}");
  }

  @Test
  void castsSaturateAndMapNanToZero() {
    accepts(
        "void f(){@Refinement(\"_==0\") int n=(int)Double.NaN;@Refinement(\"_==Integer.MAX_VALUE\") int high=(int)Double.POSITIVE_INFINITY;@Refinement(\"_==Long.MIN_VALUE\") long low=(long)Double.NEGATIVE_INFINITY;@Refinement(\"_==-1\") byte narrow=(byte)Double.POSITIVE_INFINITY;@Refinement(\"_==-2\") int truncated=(int)-2.9;}");
  }

  @Test
  void narrowingAndWideningUseTheDeclaredTypes() {
    accepts(
        "void f(){double x=16777217d;float f=(float)x;@Refinement(\"_==16777216\") double widened=f;@Refinement(\"_==16777216\") double nested=(double)(float)x;}");
  }

  @Test
  void upstreamFloatingArithmeticRunsUnchanged() {
    var result =
        Verification.verify("src/test/resources/liquidjava/testSuite/CorrectFPArithmetic.java");
    assertTrue(result.accepted(), result.diagnostics().toString());
  }
}
