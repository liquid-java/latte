# Latte and Liquid Type Checking

Latte now runs as an ownership and aliasing pass before an independently implemented refinement checker. Both passes use the same Spoon AST. The old mutable checker has been replaced; no LiquidJava verifier implementation was copied.

The verifier now covers floating-point arithmetic, exception and switch flow, deferred bodies, recursive aliases, allocation recency, inherited contracts, field-backed state, arrays and loop/effect analysis. The imported suite has **180 exact matches out of 311 inputs**; this is not full LiquidJava compatibility. All 410 upstream Java fixtures are preserved. See [compatibility status](docs/compatibility.md) for the implemented behavior, policy differences and precision limits.

## Build and run

Use JDK 17 or newer and Maven 3.9 or newer:

```sh
cd latte
mvn clean verify -Pcompatibility
java -jar target/latte-cli.jar ../examples/refinements/LocalAliases.java
java -jar target/latte-cli.jar ../examples/refinements/StaleAliasRejected.java
```

The first example succeeds. The second reports a state refinement failure and exits with status 1. Add `--explain` to include the proof obligation and solver counterexample. Each path is a separate analysis; pass a source directory to analyze related classes together. Parsing failure and unsupported analysis also produce a nonzero exit status.

The small library jar is `target/latte-0.0.1-SNAPSHOT.jar`. The runnable `latte-cli.jar` also includes Spoon and Z3, including the native solver libraries supplied by `z3-turnkey`.

`mvn test` runs 522 tests, including the focused tests, legacy Latte regressions and 180 promoted upstream regression inputs. A separate imported-fixture test enforces the intentional fresh-reference comparison difference. `mvn verify -Pcompatibility` also runs **every** imported input and writes `target/liquidjava-compatibility.json`. A successful build does not mean that all upstream expectations match.

## Ownership policy

| Value | Refinements |
| --- | --- |
| Java primitive | Allowed |
| Locally allocated object, including controlled local aliases | Allowed |
| `@Borrowed` parameter | Allowed while the borrow remains valid |
| `@Unique` object or field | Allowed while ownership remains controlled |
| `@Free` argument | Allowed; transfer consumes all aliases in the caller |
| Unannotated reference parameter or field | Shared by default; rejected |
| Explicitly `@Shared` local or object | Rejected |
| Object passed to unknown code or otherwise escaping local control | Shared; further refinements rejected |

Local declarations and assignments preserve object identity. An alias is not a fresh object. Refinement state updates through one alias are visible through the others. Borrowing does not mean purity: a mutating borrowed call invalidates affected object state. A borrowed object cannot escape through a shared store or unknown call.

Lambdas and bound method references hold their captures as heap references. Creating a local closure preserves the captured objects' permissions. Escaping the closure, or a containing object, propagates sharing to its reachable captures. Borrowed captures are allowed locally and rejected if the closure escapes or is transferred to an owning parameter. Known local invocations check the current captured state and propagate mutations to other aliases, including across loop iterations. See [local capture examples](examples/refinements/LocalCaptures.java). Callbacks whose execution is hidden behind another method, and escaped callbacks, are checked with conservative state assumptions.

Source constructors apply the same parameter permission, transfer and distinctness rules as methods. Arguments stored in fields retain their aliases, including through `this(...)` and `super(...)` delegation. A `@Free` argument becomes usable through its new owner while its old caller aliases remain consumed. Constructor argument mutations invalidate affected refinement state; instance initializer effects are included. See [constructor examples](examples/refinements/ConstructorAliases.java).

Latte can infer ownership releases at `@Free` arguments and returns when the old owner is provably unobservable. An abandoned local container need not have its owned field explicitly cleared before its child moves. Surviving aliases, external owners, callbacks, exception cleanup and possible finalization prevent that inference. This only forgets ownership edges in the analysis: Java fields and their refinement values are not changed to `null`. See [the rule and conservative boundaries](docs/compatibility.md#inferred-ownership-releases) and [checked examples](examples/refinements/InferredOwnershipRelease.java).

**Known Latte limitation: branch-dependent aliases.** Latte joins possible aliases without retaining the condition selecting each one. A variable that may refer to either a local object or a shared object remains classified as shared even inside a later matching guard. This can reject safe code. See the [limitation and workaround](docs/compatibility.md#latte-limitation-branch-dependent-aliases) and the [checked example](examples/refinements/BranchOwnershipLimit.java).

**Known Latte limitation: repeated allocations.** When the same allocation expression executes repeatedly, Latte merges older instances into a summary classified as shared. An older object retained in a local variable can therefore lose refinement eligibility even though it never escapes. See the [limitation and example](docs/compatibility.md#latte-limitation-repeated-allocations).

Ownership annotations do not upgrade a shared value into a unique one. A primitive refinement also cannot depend on mutable ghost state reached through a shared reference.

```java
import liquidjava.specification.Refinement;
import specification.Borrowed;

class Example {
    void borrow(@Borrowed @Refinement("_ != null") Object value) {}

    void example() {
        Object original = new Object();
        @Refinement("_ != null") Object alias = original;
        borrow(alias);
    }
}
```

[Examples](examples/refinements) cover accepted local aliases, unique and borrowed references, rejected shared references, escapes, stale aliases, field-backed state, arrays, floating-point behavior, control flow and inherited/deferred contracts. Their expected diagnostic kinds and lines are checked by the test suite.

## External Java libraries

Use `@ExternalRefinementsFor` to provide ownership and refinement contracts for JDK or third-party library methods, constructors and fields. Both passes resolve the same contracts on the Spoon AST. See the [external contract guide](docs/external-refinements.md) and [checked examples](examples/refinements/ExternalLibraryContracts.java). Use `--classpath` for library JARs; multiple input paths are analyzed together.

## AST handoff and embedding

```java
var result = latte.api.Verification.verify("src/main/java");
for (var local : result.model().getElements(
        new spoon.reflect.visitor.filter.TypeFilter<>(
            spoon.reflect.code.CtLocalVariable.class))) {
    latte.analysis.LatteFacts.on(local).ifPresent(facts -> {
        System.out.println(facts.permission());
        System.out.println(facts.after().aliases(facts.value()));
    });
}
```

`LatteFacts.KEY` is `latte.analysis.facts.v1`. Evaluated expressions, declarations and statements retain immutable `before` and `after` ownership stores and their reference identity. `LiquidTypeChecking` reads this metadata; it does not rerun ownership analysis. These facts belong to a particular AST and source position. Reanalyze after editing the AST. An unvisited or unsupported node must not be treated as having a successful ownership proof.

`MutationEffects.KEY` is `latte.analysis.effects.v1`. Source method declarations retain immutable summaries of the receiver, parameters and global storage they can change, including transitive calls and source overrides.

`ConstructionFacts.KEY` is `latte.analysis.construction.v1`. Constructor calls retain mappings from transferred ownership identities to their consumed caller identities, allowing the refinement pass to preserve state under the new owner. Constructor evaluation uses call-specific bindings without overwriting the declaration's published facts.

`OwnershipRelease.KEY` is `latte.analysis.release.v1`. Calls and returns record the ownership edges forgotten because their owners cannot be observed. Their `LatteFacts.after` contains the resulting ownership graph; the metadata does not describe runtime writes or establish null-valued fields.

`ClosureFacts.KEY` is `latte.analysis.closure.v1`. Closures record their captured binding names and whether execution can be deferred or escape. Capture references are stored as immutable `capture:` heap edges in `LatteFacts`. `ClosureInvocation.KEY` is `latte.analysis.closure.call.v1`; local calls retain their possible closure targets, captured references, mutations, ownership transfers and separate body facts for each invocation. LiquidTypeChecking reads these facts from the same AST.

`Verification.Result` retains the model and structured, source-located diagnostics. Analysis does not rewrite source files. The old `api.App.launcher` entry point delegates to this pipeline.

## Implementation

The state stores, points-to sets, predicates and proof results are immutable Java records. Transfer functions produce new states. Spoon supplies declarations, source positions and Java syntax; Z3 is confined to the proof adapter. The orchestration keeps per-analysis diagnostic lists and symbol indexes, with no process-global verification context.

- `latte.analysis`: permissions, heap reachability, aliases and AST metadata.
- `latte.refinement`: predicate parsing/resolution, contracts, Java arithmetic, symbolic flow and the solver adapter.
- `latte.api`: the two-pass pipeline, diagnostics and CLI.
- `liquidjava.specification`: freshly defined annotation API with familiar LiquidJava syntax.

External library contracts are supported through `@ExternalRefinementsFor`; see [syntax, ownership rules and classpath usage](docs/external-refinements.md). Recursive paths, older loop allocations, unknown calls and exceptions are conservatively approximated; loop inference supports a finite set of inductive bounds. Recursive constructor expansion requires a finite heap summary and currently reports `UNSUPPORTED`. Constructors without source bodies retain conservative effects. These approximations can reject valid programs. Unsupported constructs and solver timeouts are failures. The checker remains a prototype, and the complete compatibility report is the source of truth for port coverage.

Run `mvn spotless:apply` after editing Java. Formatting is checked by `mvn verify` and CI.

The repository uses the [MIT license](latte/LICENSE.md). Imported fixtures retain their [upstream license](latte/src/test/resources/liquidjava/LICENSE) and [provenance](latte/src/test/resources/liquidjava/provenance.json).
