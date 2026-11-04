package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.support.compiler.VirtualFile;

class ConstantsAndTypesTest {
  private Verification.Result check(String source) {
    return Verification.verify(
        new VirtualFile(
            "import liquidjava.specification.*;import specification.*;" + source, "Sample.java"));
  }

  private void accepts(String source) {
    var r = check(source);
    assertTrue(r.accepted(), r.diagnostics().toString());
  }

  @Test
  void nestedFloatingConstantsResolveInPredicates() {
    accepts(
        "class Sample{static class Limits{static final double MAX=10.0;}static void use(@Refinement(\"_<Limits.MAX\")double n){}void f(){use(5.0);}}");
  }

  @Test
  void staticImportsResolveWithoutExecutingUserCode() {
    accepts(
        "import static java.lang.Integer.MAX_VALUE;class Sample{void f(){@Refinement(\"_<MAX_VALUE\")int n=1;}}");
  }

  @Test
  void arithmeticAndBitwiseConstantsUseJavaSemantics() {
    accepts(
        "class Sample{static final int MASK=(1<<5)-1;static final int OVERFLOW=Integer.MAX_VALUE+1;void f(){@Refinement(\"_==MASK\")int n=31;@Refinement(\"_==OVERFLOW\")int m=Integer.MIN_VALUE;}}");
  }

  @Test
  void aliasesRejectNarrowingArguments() {
    var r =
        check(
            "@RefinementAlias(\"Positive(int x){x>0}\")class Sample{void f(){@Refinement(\"Positive(_)\")double n=2;}}");
    assertTrue(
        r.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.ARGUMENT),
        r.diagnostics().toString());
  }

  @Test
  void instanceofIsAConsistentNullableTypeTest() {
    accepts(
        "class Sample{void f(Object x){if(x instanceof String){@Refinement(\"_==true\")boolean b=x instanceof String;}@Refinement(\"_==false\")boolean n=null instanceof String;}}");
  }

  @Test
  void unknownBoxedResultsCanBeUsedInNumericConditions() {
    accepts(
        "class Sample{void f(java.util.List<Integer> values){if(values.get(0)>0){@Refinement(\"_>0\")int n=1;}}}");
  }

  @Test
  void unresolvedCallsUseTheRequiredOperandType() {
    accepts(
        "class Sample{void f(missing.Type x,boolean b){if(b&&x.ok()){@Refinement(\"_>0\")int n=1;}if(x.size()>2){@Refinement(\"_>0\")int n=1;}int c=0;c+=x.size();}}");
  }
}
