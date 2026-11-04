package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.support.compiler.VirtualFile;

class InheritanceTest {
  private Verification.Result check(String source) {
    return Verification.verify(
        new VirtualFile(
            "import liquidjava.specification.*;import specification.*;" + source,
            "Inheritance.java"));
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
  void overrideMayWeakenInputsAndStrengthenResults() {
    accepts(
        "interface Parent{@Refinement(\"_>=0\")int f(@Refinement(\"_>0\")int n);}class Child implements Parent{@Refinement(\"_>0\")public int f(@Refinement(\"_>=0\")int renamed){return 1;}}");
  }

  @Test
  void strongerInputIsRejectedEvenWhenBodyIsValid() {
    rejects(
        "interface Parent{@Refinement(\"_>=0\")int f(@Refinement(\"_>=0\")int n);}class Child implements Parent{@Refinement(\"_>=0\")public int f(@Refinement(\"_>0\")int n){return n;}}");
  }

  @Test
  void weakerOutputIsRejectedEvenWhenBodyIsValid() {
    rejects(
        "interface Parent{@Refinement(\"_>0\")int f();}class Child implements Parent{@Refinement(\"_>=0\")public int f(){return 0;}}");
  }

  @Test
  void inheritedContractsUseParameterPositions() {
    accepts(
        "interface A{@Refinement(\"_==first\")int f(int first,int second);}interface B{@Refinement(\"_==second\")int f(int second,int first);}class Child implements A,B{public int f(int x,int y){return x;}void test(){@Refinement(\"_==4\")int n=f(4,7);}}");
  }

  @Test
  void multipleInheritedInputsAreAlternatives() {
    accepts(
        "interface A{@Refinement(\"_>0\")int f(@Refinement(\"_>0\")int x);}interface B{@Refinement(\"_<0\")int f(@Refinement(\"_<0\")int x);}class Child implements A,B{public int f(int x){return x;}void test(){@Refinement(\"_<0\")int n=f(-2);@Refinement(\"_>0\")int p=f(2);}}");
    rejects(
        "interface A{int f(@Refinement(\"_>0\")int x);}interface B{int f(@Refinement(\"_<0\")int x);}class Child implements A,B{public int f(int x){return x;}void test(){f(0);}}");
  }

  @Test
  void intermediateContractIsChecked() {
    rejects(
        "interface A{@Refinement(\"_>=0\")int f();}abstract class B implements A{@Refinement(\"_>0\")public abstract int f();}class C extends B{@Refinement(\"_>=0\")public int f(){return 0;}}");
  }

  @Test
  void inheritedGhostsReferToTheSameState() {
    accepts(
        "@StateSet({\"ready\",\"done\"})interface A{@StateRefinement(from=\"ready(this)\",to=\"done(this)\")void f();}class C implements A{@StateRefinement(to=\"ready(this)\")C(){}@StateRefinement(from=\"ready(this)\",to=\"done(this)\")public void f(){}@StateRefinement(from=\"done(this)\")void use(){}static void test(){C c=new C();c.f();c.use();}}");
  }

  @Test
  void inheritedFieldInvariantsAreAvailableInSubclassMethods() {
    accepts(
        "class Parent{@Refinement(\"_>0\")protected int count=1;}class Child extends Parent{@Refinement(\"_>0\")int get(){return count;}}");
  }

  @Test
  void overridesCannotConsumeABorrowedParameter() {
    var r =
        check(
            "interface Parent{void f(@Borrowed @Refinement(\"_!=null\")Object x);}class Child implements Parent{public void f(@Free @Refinement(\"_!=null\")Object x){}}");
    assertTrue(
        r.diagnostics().stream().anyMatch(d -> d.kind() == Diagnostic.Kind.PERMISSION),
        r.diagnostics().toString());
  }
}
