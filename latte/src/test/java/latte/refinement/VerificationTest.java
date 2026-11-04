package latte.refinement;

import static org.junit.jupiter.api.Assertions.*;

import latte.api.*;
import org.junit.jupiter.api.Test;
import spoon.support.compiler.VirtualFile;

class VerificationTest {
  private Verification.Result check(String source) {
    return Verification.verify(
        new VirtualFile(
            "import liquidjava.specification.*; import specification.*;\n" + source,
            "Sample.java"));
  }

  private void accepts(String source) {
    var result = check(source);
    assertTrue(result.accepted(), result.diagnostics().toString());
  }

  private void rejects(String source, Diagnostic.Kind kind) {
    var result = check(source);
    assertTrue(
        result.diagnostics().stream().anyMatch(d -> d.kind() == kind),
        result.diagnostics().toString());
  }

  @Test
  void provesPrimitiveRefinements() {
    accepts("class Sample { void f() { @Refinement(\"_ > 0\") int x=3; x=4; } }");
  }

  @Test
  void rejectsBadAssignment() {
    rejects(
        "class Sample { void f() { @Refinement(\"_ > 0\") int x=3; x=-1; } }",
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void usesPathConditionsAndEarlyReturns() {
    accepts(
        "class Sample { @Refinement(\"_ > 0\") int f(int x) { if(x<=0) return 1; return x; } }");
  }

  @Test
  void checksParameterAndReturnContracts() {
    accepts(
        "class Sample { @Refinement(\"_ > x\") int next(@Refinement(\"x >= 0 && x < 100\") int x) { return x+1; } void f(){ @Refinement(\"_ > 4\") int y=next(4); } }");
  }

  @Test
  void rejectsBadArguments() {
    rejects(
        "class Sample { void positive(@Refinement(\"_>0\") int x) {} void f(){positive(-1);} }",
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void checksOverflowUsingJavaArithmetic() {
    rejects(
        "class Sample { void f() { int x=Integer.MAX_VALUE; @Refinement(\"_>0\") int y=x+1; } }",
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void javaNegativeDivisionTruncatesTowardZero() {
    accepts(
        "class Sample { void f() { @Refinement(\"_ == -2\") int x=-5/2; @Refinement(\"_ == -1\") int y=-5%2; } }");
  }

  @Test
  void rejectsRefinedSharedParameters() {
    rejects(
        "class Sample { void f(@Refinement(\"_ != null\") Object x) {} }",
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void permitsRefinedBorrowedParameters() {
    accepts(
        "class Sample { void f(@Borrowed @Refinement(\"_ != null\") Object x) { @Refinement(\"_ != null\") Object y=x; } }");
  }

  @Test
  void permitsRefinementsOnControlledLocalAliases() {
    accepts(
        "class Sample { void f() { Object x=new Object(); @Refinement(\"_ != null\") Object y=x; } }");
  }

  @Test
  void rejectsRefinementAfterEscape() {
    rejects(
        "class Sample { void f() { java.util.Properties a=new java.util.Properties(); System.setProperties(a); @Refinement(\"_ != null\") Object b=a; } }",
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void stateChangesAreVisibleThroughAliases() {
    rejects(
        "@StateSet({\"open\",\"closed\"}) class Resource { @StateRefinement(to=\"open(this)\") Resource(){} @StateRefinement(from=\"open(this)\",to=\"closed(this)\") void close(){} @StateRefinement(from=\"open(this)\") void read(){} } class Sample { void f(){Resource a=new Resource(); Resource b; b=a; b.close(); a.read();} }",
        Diagnostic.Kind.STATE);
  }

  @Test
  void validStateProtocolPasses() {
    accepts(
        "@StateSet({\"open\",\"closed\"}) class Resource { @StateRefinement(to=\"open(this)\") Resource(){} @StateRefinement(from=\"open(this)\",to=\"closed(this)\") void close(){} @StateRefinement(from=\"open(this)\") void read(){} } class Sample { void f(){Resource a=new Resource(); Resource b=a; b.read(); a.close();} }");
  }

  @Test
  void sharedReceiverCannotUseStateContract() {
    rejects(
        "@StateSet({\"open\",\"closed\"}) class Resource { @StateRefinement(from=\"open(this)\") void read(){} } class Sample { void f(Resource r){r.read();} }",
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void expandsAliasesWithoutReusingTheOldParser() {
    accepts(
        "@RefinementAlias(\"Positive(int x) { x > 0 }\") class Sample { void f(){ @Refinement(\"Positive(_)\") int x=2; } }");
  }

  @Test
  void malformedPredicatesAreErrors() {
    rejects("class Sample { void f(){ @Refinement(\"_ >\") int x=2; } }", Diagnostic.Kind.SYNTAX);
  }

  @Test
  void externalRefinementsAreExplicitlyUnsupported() {
    rejects(
        "@ExternalRefinementsFor(\"java.util.List\") interface Sample {}",
        Diagnostic.Kind.UNSUPPORTED);
  }

  @Test
  void independentStateSetsDoNotMakeTheContextContradictory() {
    rejects(
        "@StateSet({\"open\",\"closed\"}) @StateSet({\"locked\",\"unlocked\"}) class Resource { @StateRefinement(to=\"closed(this) && unlocked(this)\") Resource(){} @StateRefinement(from=\"unlocked(this)\",to=\"locked(this)\") void lock(){} @StateRefinement(from=\"unlocked(this)\",to=\"open(this)\") void open(){} } class Sample { void f(){Resource r=new Resource();r.lock();r.open();} }",
        Diagnostic.Kind.STATE);
  }

  @Test
  void transitionPreservesUntouchedStateSets() {
    accepts(
        "@StateSet({\"open\",\"closed\"}) @StateSet({\"locked\",\"unlocked\"}) class Resource { @StateRefinement(to=\"closed(this) && unlocked(this)\") Resource(){} @StateRefinement(to=\"open(this)\") void open(){} @StateRefinement(from=\"unlocked(this)\",to=\"locked(this)\") void lock(){} @StateRefinement(from=\"open(this)\") void read(){} } class Sample { void f(){Resource r=new Resource();r.open();r.lock();r.read();} }");
  }

  @Test
  void borrowedReturnRetainsItsArgumentAlias() {
    rejects(
        "@StateSet({\"open\",\"closed\"}) class Resource { @StateRefinement(to=\"open(this)\") Resource(){} @StateRefinement(to=\"closed(this)\") void close(){} @StateRefinement(from=\"open(this)\") void read(){} } class Sample { @Borrowed Resource identity(@Borrowed Resource r){return r;} void f(){Resource a=new Resource();Resource b=identity(a);b.close();a.read();} }",
        Diagnostic.Kind.STATE);
  }

  @Test
  void borrowingDoesNotImplyAbsenceOfMutation() {
    rejects(
        "@StateSet({\"open\",\"closed\"}) class Resource { @StateRefinement(to=\"open(this)\") Resource(){} @StateRefinement(to=\"closed(this)\") void close(){} @StateRefinement(from=\"open(this)\") void read(){} } class Sample { void mutate(@Borrowed Resource r){r.close();} void f(){Resource a=new Resource();mutate(a);a.read();} }",
        Diagnostic.Kind.STATE);
  }

  @Test
  void sourceInterfaceContractsAreInherited() {
    rejects(
        "interface Positive {void accept(@Refinement(\"_>0\") int n);} class Sample implements Positive {public void accept(int renamed){} void f(){accept(-1);} }",
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void explicitSharedLocalCannotCarryARefinement() {
    rejects(
        "class Sample {void f(){@Shared @Refinement(\"_!=null\") Object x=new Object();}}",
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void bitwiseOperationsUseJavaWidthsAndShiftMasks() {
    accepts(
        "class Sample {void f(){@Refinement(\"_==3\") int x=1|2; @Refinement(\"_==1\") int y=1<<32; @Refinement(\"_==2147483647\") int z=-1>>>1;}} ");
  }

  @Test
  void constructorsMustEstablishFieldRefinements() {
    rejects("class Sample {@Refinement(\"_>0\") int n; Sample(){}}", Diagnostic.Kind.REFINEMENT);
    accepts("class Sample {@Refinement(\"_>0\") int n; Sample(){n=1;}}");
  }

  @Test
  void fieldInitializersAreVerified() {
    rejects("class Sample {@Refinement(\"_>0\") int n=-1; Sample(){}}", Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void unusedAliasesMustStillBeWellTyped() {
    rejects(
        "@RefinementAlias(\"Broken(int x) {x > MISSING}\") class Sample {}",
        Diagnostic.Kind.SYNTAX);
  }

  @Test
  void primitiveRefinementsCannotDependOnSharedGhostState() {
    rejects(
        "@Ghost(\"int size\") class Box {} class Sample {void f(Box b){@Refinement(\"size(b)>=0\") int n=1;}}",
        Diagnostic.Kind.SHARED_REFINEMENT);
  }

  @Test
  void fromOnlyMethodsCannotChangeReceiverStateInTheirBodies() {
    rejects(
        "@StateSet({\"open\",\"closed\"}) class Sample {@StateRefinement(to=\"closed(this)\") void close(){} @StateRefinement(from=\"open(this)\") void read(){close();}}",
        Diagnostic.Kind.STATE);
  }

  @Test
  void fieldAliasesShareTypestate() {
    rejects(
        "@StateSet({\"open\",\"closed\"}) class Resource {@StateRefinement(to=\"open(this)\") Resource(){} @StateRefinement(to=\"closed(this)\") void close(){} @StateRefinement(from=\"open(this)\") void read(){}} class Sample {@Unique Resource r; void f(){r=new Resource();Resource alias=r;alias.close();r.read();}}",
        Diagnostic.Kind.STATE);
  }

  @Test
  void referencePredicatesObserveStateVersions() {
    rejects(
        "@RefinementPredicate(\"int state(Resource r)\") class Resource {@StateRefinement(to=\"state(this)==1\") Resource(){} @StateRefinement(to=\"state(this)==2\") void close(){} @StateRefinement(from=\"state(this)==1\") void read(){}} class Sample {void f(){Resource r=new Resource();r.close();r.read();}}",
        Diagnostic.Kind.STATE);
  }

  @Test
  void inconsistentStateTransitionsAreRejected() {
    rejects(
        "@StateSet({\"open\",\"closed\"}) class Sample {@StateRefinement(to=\"open(this)&&closed(this)\") Sample(){} void f(){new Sample();}}",
        Diagnostic.Kind.STATE);
  }

  @Test
  void multidimensionalArrayLengthUsesTheOuterDimension() {
    accepts(
        "class Sample {void f(){int[][] a=new int[2][3];@Refinement(\"_==2\") int n=a.length;}}");
    rejects(
        "class Sample {void f(){int[][] a=new int[2][3];@Refinement(\"_==3\") int n=a.length;}}",
        Diagnostic.Kind.REFINEMENT);
  }

  @Test
  void nestedObjectsAreInvalidatedByMutatingCalls() {
    rejects(
        "@StateSet({\"open\",\"closed\"}) class Resource {@StateRefinement(to=\"open(this)\") Resource(){} @StateRefinement(to=\"closed(this)\") void close(){} @StateRefinement(from=\"open(this)\") void read(){}} class Sample {@Unique Resource r; void mutate(){r.close();}void f(){r=new Resource();Resource old=r;old.read();mutate();old.read();}}",
        Diagnostic.Kind.STATE);
  }
}
