package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.support.compiler.VirtualFile;

class FieldContractsTest {
  private Verification.Result check(String source) {
    return Verification.verify(
        new VirtualFile(
            "import liquidjava.specification.*;import specification.*;" + source, "Fields.java"));
  }

  private void accepts(String source) {
    var r = check(source);
    assertTrue(r.accepted(), r.diagnostics().toString());
  }

  private void rejects(String source) {
    var r = check(source);
    assertTrue(
        r.diagnostics().stream()
            .anyMatch(
                d -> d.kind() == Diagnostic.Kind.REFINEMENT || d.kind() == Diagnostic.Kind.STATE),
        r.diagnostics().toString());
  }

  @Test
  void fieldFunctionsDescribeTheBody() {
    accepts(
        "class Fields{int count;@StateRefinement(to=\"count()==n\") Fields(int n){count=n;}@StateRefinement(from=\"count()<2147483647\",to=\"count()==count(old(this))+1\") void inc(){count++;}}");
  }

  @Test
  void fieldEquationsMustHoldOnReturn() {
    rejects(
        "class Fields{int count;@StateRefinement(to=\"count()==n\") Fields(int n){count=n+1;}}");
  }

  @Test
  void gettersAndPrivatePostconditionsUseConcreteFields() {
    accepts(
        "class Fields{int count;@PrivateRefinement(\"this.count==n\") void set(int n){count=n;}@Refinement(\"_==count()\") int get(){return count;}}");
    rejects(
        "class Fields{int count;@PrivateRefinement(\"this.count==n\") void set(int n){count=n+1;}}");
  }

  @Test
  void callsAndAliasesShareFieldVersions() {
    accepts(
        "class Fields{int count;@StateRefinement(to=\"count()==n\") Fields(int n){count=n;}@StateRefinement(to=\"count()==n\") void set(int n){count=n;}static void f(){Fields a=new Fields(3);Fields b=a;b.set(5);@Refinement(\"_==5\")int n=a.count;}}");
    rejects(
        "class Fields{int count;@StateRefinement(to=\"count()==n\") Fields(int n){count=n;}@StateRefinement(to=\"count()==n\") void set(int n){count=n;}static void f(){Fields a=new Fields(3);Fields b=a;b.set(5);@Refinement(\"_==3\")int n=a.count;}}");
  }

  @Test
  void unchangedFloatingFieldsCanContainNaN() {
    accepts(
        "class Fields{double value;int count;void f(){value=Double.NaN;count=1;@Refinement(\"_!=_\")double n=value;}}");
    rejects(
        "class Fields{double value;int count;void f(){value=Double.NaN;count=1;@Refinement(\"_==_\")double n=value;}}");
  }

  @Test
  void dottedFieldPredicatesCannotBypassSharedOwnership() {
    var r =
        check(
            "class Fields{int count;void f(Fields shared){@Refinement(\"_==shared.count\")int n=shared.count;}}");
    assertTrue(
        r.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.SHARED_REFINEMENT),
        r.diagnostics().toString());
  }

  @Test
  void aliasesInTransitionsFrameTheirExpandedGhosts() {
    accepts(
        "@RefinementAlias(\"Empty(Fields x){state(x)==0}\")@RefinementAlias(\"Full(Fields x){state(x)==1}\")@Ghost(\"int state\")class Fields{@StateRefinement(to=\"Empty(this)\")Fields(){}@StateRefinement(from=\"Empty(this)\",to=\"Full(this)\")void fill(){}@StateRefinement(from=\"Full(this)\")void use(){}static void f(){Fields a=new Fields();a.fill();a.use();}}");
  }
}
