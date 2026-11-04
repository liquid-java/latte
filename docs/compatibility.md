# LiquidJava port status

The verifier and test runner were implemented independently. Only the upstream fixture sources and their license were copied from LiquidJava commit `92963a7548f75fc42d318c7826679f4edb50918a`. The snapshot contains 410 Java files, grouped into 311 independently analyzed inputs. The imported source files and their expected-error comments remain byte-for-byte unchanged.

## Implemented functionality

The pipeline checks refinements on parameters, locals, fields, assignments, calls and returns. It supports path conditions, early returns, declared loop invariants, inferred inductive bounds, refinement aliases, ghost functions, reference predicates, independent state sets, state transitions, `old`, and return-dependent transitions.

The follow-up implementation adds:

- External library contracts shared by Latte and LiquidTypeChecking, including ownership, overloads, constructors, inherited members, fields, state transitions and reference predicates. A per-model registry validates Java signatures and publishes bindings on the Spoon AST. The CLI and embedding API accept library classpaths and multiple source inputs. See [external specifications](external-refinements.md).

- IEEE 754 `float` and `double` arithmetic, rounding, signed zero, NaN, infinities, Java remainder, numeric promotion and narrowing conversions. Java integral operations retain finite-width overflow, signed division/remainder and masked shift counts.
- Explicit normal, return, throw, break, continue and yield completions. Catches and finally blocks preserve the state at the relevant completion; finally can replace an earlier return. Resource closing, switch fall-through, switch expressions and common implicit exceptions are modeled. Deferred lambda bodies and method references are checked against their functional contracts.
- Compositional return-alias summaries, including local assignments, conditional returns, delegated returns and recursion. A finite path domain widens recursive paths to reachable subgraphs. Allocation recency distinguishes the newest object at a site from older instances. The original `MyNodeComplete` regression now passes.
- Behavioral checks for explicit overrides: an override cannot strengthen input predicates or weaken output/state guarantees. Parameter names are bound by position. Multiple inherited contracts are checked separately and accepted as complete alternatives at calls. Source override effects and inherited state declarations are included.
- Implicit field functions, such as `count(this)` or `count()`, tied to Java field storage. Concrete field equations in state postconditions are checked against the body. Field initializers start from Java defaults. `@PrivateRefinement` is an implementation postcondition, verified on normal completion and kept out of the public call contract.
- Persistent array contents with symbolic select/store operations, Java defaults, initializer values, aliases, branch joins, bounds and invalidation after mutation. Latte keeps separate slots for literal indices and a default summary for other slots. Symbolic writes preserve all possible targets; precise overwrites detach only that slot. Null remains a possible value across joins. State changes through symbolic reads are conditional on the selected object, and fresh allocations are distinct from previously existing objects. Escapes include every reachable element. Array iteration and source return summaries retain element aliases in AST metadata.
- Method mutation summaries computed to a fixed point and stored on Spoon declarations. Pure helper methods preserve unrelated object state; mutating borrowed calls invalidate the affected receiver, arguments and reachable objects. Inferred loop bounds must hold initially and on every back edge; break exits retain their own conditions.
- Source constructor parameters use method ownership rules. Constructor field stores retain caller argument identities, including through delegated constructors. Free transfers preserve the reachable heap and refinement state under the new owner while consuming caller aliases. Field defaults, instance initialization, early normal returns and argument mutations are included. Constructor calls inside methods and instance initializer effects contribute to mutation summaries.
- Local lambdas and bound method references preserve captured ownership through immutable heap edges. Escape propagation reaches captures transitively through nested closures and containing objects. Known local invocations use current object state, check functional contracts and propagate alias mutations; their effects are included in loop analysis. Borrowed captures may remain local but cannot escape or pass to owning parameters. Callbacks with deferred or hidden execution retain conservative state checks.
- Analysis-only ownership release inference at `@Free` calls and returns. Incoming ownership edges from provably unobservable owners can be forgotten without inserting Java field clears. Current object state and consumption of all transferred aliases are preserved, including transfers to constructors and local callbacks.
- Source and nested constants, static imports, floating constants, compound constant expressions, `instanceof`, boxed operand conversions and contextual types for unresolved operands. User code is not executed to resolve constants.

Latte ownership facts remain attached to the AST under `latte.analysis.facts.v1`. Method effects use `latte.analysis.effects.v1`; constructor transfer mappings use `latte.analysis.construction.v1`. Closures and local invocation snapshots use `latte.analysis.closure.v1` and `latte.analysis.closure.call.v1`. The refinement pass consumes those facts on the same model. Escapes through exceptional branches and loop breaks are included in the ownership joins.

Abstract ghost transitions remain specifications. Their consistency, preconditions, framing and use are checked; they are not a claim that arbitrary abstract state has a concrete Java representation. Field-backed ghosts and private postconditions do have concrete body obligations.

## Reproducible accounting

At this checkpoint:

- **180 of 311** imported inputs match the upstream expected diagnostics exactly. External specifications add 15 exact matches to the previous 165. Fresh-reference reasoning retains the intentional difference described below.
- All 180 exact matches are required parameterized tests in `supported-inputs.txt`. `ReferenceFreshnessTest` separately requires the corrected diagnostics for the unchanged reference-comparison fixture.
- **522 tests** cover these imported inputs, focused regressions, examples and the legacy Latte suite.
- Every imported input completes without an uncaught analysis exception.
- Every upstream negative input is rejected, except the two explicit semantic differences below.
- External declarations are checked instead of rejected wholesale. Exact-match accounting still distinguishes ownership-policy differences, invalid specifications and precision limits.

Run from `latte/`:

```sh
mvn clean verify -Pcompatibility
```

Matches require the same diagnostic count, source files and lines, and corresponding diagnostic categories. Syntax failures do not count as refinement failures. The audit writes `target/liquidjava-compatibility.json` with expected and actual diagnostics, source locations, crashes and exact-match status for every input.

The `--check` gate fails for a crash, a regression in the promoted set, or a newly accepted upstream negative input. Known mismatches remain visible and are not counted as compatibility successes. To require complete parity, use `--strict` (currently expected to fail):

```sh
mvn exec:java -Dexec.mainClass=latte.compatibility.CompatibilityAudit \
    -Dexec.classpathScope=test -Dexec.args=--strict
```

## Compatibility differences and precision limits

External refinement specifications now bind to actual Java library declarations for both passes; see [the contract guide](external-refinements.md). Invalid targets and member signatures are errors, including fixtures where upstream expected only a warning. Shared reference refinements are rejected under the requested ownership policy. Existing upstream examples with unannotated reference parameters, shared fields or externally managed objects need suitable ownership contracts before their object-state refinements can be retained.

Some upstream expectations assume mathematical arithmetic for Java expressions. The new checker rejects those claims when overflow or IEEE rounding supplies a counterexample. Examples include `CorrectFunctionDeclarations`, `CorrectOperatorAssignments`, `CorrectSimpleIfElse` and `CorrectChars`. Predicate-only decimal expressions use mathematical reals; when combined with a floating Java value, they are converted to that floating sort.

Two upstream negative fixtures are valid with precise local values:

- `ErrorDependentRefinement.java`: local values 5, 50 and 21 prove that the middle value lies between the other two.
- `ErrorInstanceVarInRefinement.java`: local values 6 and 9 prove that the second is greater than the first.

These are named exceptions to the negative-input gate, not exceptions for entire categories of errors.

`ErrorIfReferenceComparison.java` still reports its two feasible double-close errors (lines 30 and 39). Its third expected error, at line 21, assumes that a preexisting parameter can equal a newly allocated object. Java allocation freshness rules out that branch. The unchanged fixture is checked by a dedicated regression test and remains a mismatch in the upstream diagnostic audit.

Reference-array examples are in `examples/refinements/ReferenceArrayAliases.java`. Known local elements remain refinable, including borrowed and unique objects. Arbitrary index expressions are summarized by Latte and constrained by symbolic select/store operations in LiquidTypeChecking; this can lose ownership precision. Unknown external elements and implicit rows of multidimensional allocations remain shared summaries, with distinct possible values on different reads. Mutations involving unknown element sets conservatively invalidate object state. Explicit nested array initializers retain their individual array identities. Library containers such as `ArrayList` can now have external ownership and state contracts. These do not infer arbitrary element ownership or aliases from a size specification.

The analysis remains conservative. Recursive return paths widen after four field steps. Older objects at a repeated allocation site are summarized as shared, so refinements requiring their individual identities may be rejected. Loop inference uses a finite set of candidate bounds rather than arbitrary invariants. Exception and unknown-call effects can lose precision. Unspecified field updates are invalidated rather than inferred as exact method postconditions. Multiple inherited cases guarantee the facts common to the applicable alternatives. Solver timeouts are verification failures, never successful proofs.

Constructor aliases and transfers are demonstrated in [ConstructorAliases.java](../examples/refinements/ConstructorAliases.java). Recursive constructor expansion currently fails explicitly with `UNSUPPORTED`; constructors without source bodies have conservative effects.

Full diagnostic parity is still incomplete. The report includes ownership-policy differences, Java arithmetic differences, conservative rejections and differences in diagnostic placement/count. No-classpath Spoon analysis also does not replace Java compilation. The checkpoint should not be represented as a complete or production-ready Java verifier.

## Inferred ownership releases

An `@Free` parameter or result requires a transfer of ownership. Previously, an owned field could prevent this transfer even when the containing object could never be observed again. Latte now forgets those unobservable ownership edges before checking the transfer. For example:

```java
static final class Box {
    @Unique Object value;
    Box(@Unique Object value) { this.value = value; }
}

@Free Object extract() {
    Box box = new Box(new Object());
    return box.value; // Accepted without box.value = null.
}
```

The source and executable AST are unchanged. The field still holds its Java value; no null fact is introduced. The inference proves that the old owner cannot be observed, then removes its edge from the ownership graph. Refinements on the child remain available under its new owner, and old aliases of a transferred child remain consumed.

Observability includes the receiver, non-transferred caller parameters, escaped/shared and borrowed objects, live local aliases, arguments and receiver retained by the current call, reachable heap objects and live closure captures. Future-use checking includes loop back edges, branch continuations, catches, finally blocks, resource cleanup, synchronization, local-class captures and names mentioned in annotation predicates or their definitions. A live owner of a descendant also prevents moving the whole graph. An `@Borrowed` argument does not trigger release inference.

A surviving container still needs an explicit clear. For example, `@Free Object take() { return this.value; }` with an owned field remains rejected. Conversely, after explicitly disconnecting an old node from a surviving container, its own fields may no longer need clearing if the old node has no observable aliases. Both cases appear in [InferredOwnershipRelease.java](../examples/refinements/InferredOwnershipRelease.java).

The proof is deliberately conservative. Later syntactic reads keep bindings live even when a more precise control-flow analysis could prove them irrelevant. Nested expressions with retained evaluation temporaries, array-slot releases, and release inference inside caller-specific constructor or callback body evaluation are not currently inferred. Known finalizers and potentially finalizing runtime subtypes count as observers: a directly allocated object has a known runtime class, while a received object of an open type may need an explicit clear. Existing branch-correlation and repeated-allocation limits still apply.

`OwnershipRelease` metadata (`latte.analysis.release.v1`) records forgotten edges on the call or return. `LatteFacts` retains both the earlier ownership snapshot and the graph after release, which the refinement checker consumes. Reanalysis recomputes the metadata.

### Latte limitation: branch-dependent aliases

Latte tracks changes in ownership and aliases through statements, but merging branches loses the relationship between a condition and the object selected on that branch. The merged ownership facts retain the set of possible objects, without the condition associated with each candidate. A later test of the same boolean does not narrow that set.

For example, assume that constructing a `Resource` establishes its `open` state and that `read()` requires that state:

```java
void example(boolean chooseLocal, Resource shared) {
    Resource local = new Resource();
    Resource selected = chooseLocal ? local : shared;

    if (chooseLocal) {
        selected.read(); // Rejected: SHARED_REFINEMENT
    }
}
```

The unannotated reference parameter `shared` has shared ownership. After the conditional assignment, Latte records that `selected` may refer to either `local` or `shared`, so its combined permission is shared. Inside the `if`, Java execution guarantees that `selected` refers to `local`, but Latte no longer has the condition-to-alias relationship needed to recover that fact. LiquidTypeChecking tracks symbolic path conditions, but those conditions cannot override Latte's ownership rejection.

This is a conservative false rejection of safe code. Branches are supported, and joining aliases to controlled local objects does not by itself make them shared; the example loses refinement eligibility because one candidate is shared.

A workaround is to keep the local selection and use together within the branch:

```java
if (chooseLocal) {
    Resource selected = local;
    selected.read(); // Accepted
}
```

Both forms are covered by [BranchOwnershipLimit.java](../examples/refinements/BranchOwnershipLimit.java), whose expected diagnostics are checked by the example test suite. Removing this limitation would require retaining conditions alongside possible aliases and using branch guards to narrow the ownership facts. That support is not currently implemented.

### Latte limitation: repeated allocations

When the same allocation expression executes repeatedly, Latte keeps a separate identity for the newest instance and merges older instances from that source location into a summary classified as shared. This keeps the analysis finite, but can reject refinements on objects whose aliases remain entirely local.

For example, assume that constructing a `Resource` establishes its `open` state and that `read()` requires that state without changing it:

```java
void example() {
    Resource previous = null;

    for (int i = 0; i < 3; i++) {
        Resource current = new Resource();
        current.read(); // Accepted

        if (previous != null) {
            previous.read(); // Rejected: SHARED_REFINEMENT
        }

        previous = current;
    }
}
```

This code is safe: every resource stays local and open. On the second iteration, `current` refers to the new object and `previous` refers to the first object. Latte moves the first object's identity into the older-instance summary, making `previous` shared for ownership checking. LiquidTypeChecking consequently rejects `previous.read()`.

The shared classification reflects lost analysis precision; no object actually escapes this method. The loss occurs even though `previous` retains only one older object at a time. The null check establishes that an object exists, but cannot recover its individual identity or ownership from the summary.

The example was checked with the current verifier and reports `SHARED_REFINEMENT` at `previous.read()`. The equivalent three iterations written explicitly with separate `new Resource()` source locations verify successfully. The limitation concerns repeated execution of one allocation expression; separate allocation sites retain separate identities. Explicitly expanding a fixed loop illustrates the distinction, but is not a general solution for arbitrary iteration counts.

The summarization is implemented in [FlowState.fresh](../latte/src/main/java/latte/analysis/FlowState.java). Preserving refinement eligibility for these older locally controlled instances remains an open precision improvement.
