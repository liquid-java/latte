# Effect-summary follow-up

The new `@Modifies` and `@ReturnsView` summaries improve precision across library calls. They do not resolve Latte's saved branch-dependent alias or repeated-allocation limitations. A replay of the original OpenJDK corpus finds specific improvements, but more diagnostics overall because the expanded catalog checks more calls.

The [full comparison](../audits/jdk21-upstream/effects-followup.json) records all source hashes, checker fingerprints, outcomes, reviewed exception exclusions and changed diagnostics. The [reproduction evidence](../audits/jdk21-upstream/effects-reproductions.json) contains the small examples, results and controlled comparison harness.

## What improved

For four reduced examples, the comparison uses the same checker and all 501 current stronger declarations. The control removes only `Modifies` and `ReturnsView` annotations from the Spoon model. This isolates the contribution of the new summaries from other catalog changes and earlier crash fixes.

| Case | Without effect/view annotations | Current summaries |
| --- | --- | --- |
| [Array copy followed by another stream write](../examples/jdk21-audit/ConservativeArrayCopy.java) | Later write rejected for non-distinct arguments/receiver | Verifies |
| [Local `ArrayList.subList`](../examples/jdk21-audit/LocalListView.java) | Subsequent backing-list mutation rejected as shared | Verifies |
| [String loop](../examples/jdk21-audit/StringFacts.java) | `charAt` rejected inside a loop guarded by `text.length()` | Loop verifies; separate literal case still fails |
| External constructor borrowing an array of local resources | Subsequent element-state check rejected as shared | Element remains refinable with an empty constructor footprint |

The constructor source is embedded as `ConstructorFootprint.java` in the reproduction evidence. Its specification borrows an `Object[]` with `@Modifies({})`; after construction, `values[0].read()` retains its resource's known open state.

These improvements also appear in unchanged upstream sources:

- `Arrays/FloatDoubleOrder.java` now completes with **zero diagnostics**, clearing eight previous ownership diagnostics.
- `ByteArrayOutputStream/Write.java` loses all five previous diagnostics at the second write and following observation, lines 116–117. The file still has other errors, including the conservative `writeBytes` path and newly checked `copyOfRange` calls.
- `ArrayList/SubListModCount.java` now accepts the ordinary backing-list mutation at line 38. Invalid-view operations are checked separately.
- Two `charAt` proof failures in `String/StringRacyConstructor.java` and one in `ProcessBuilder/ReaderWriterTest.java` disappear. Other diagnostics on those lines remain, so these are discharged proof obligations, not fully accepted statements.

## Limitations that remain

| Limitation | Current result |
| --- | --- |
| Branch-dependent aliases | Still rejected: after merging local/shared alternatives, testing the original condition does not recover the local alias. [Existing example](../examples/refinements/BranchOwnershipLimit.java), line 15. |
| Repeated allocation at one source site | Still rejected: the older object becomes a shared summary even when it remains local. The embedded `RepeatedAllocationLimit.java` rejects `previous.read()`; separate allocation sites pass. |
| Literal String facts | The literal `charAt` case in `StringFacts.java`, line 7, still fails. Preserving existing facts does not establish missing literal-length facts. |
| Relations between table columns | The valid row loop in [ClampTable.java](../examples/jdk21-audit/ClampTable.java), line 11, still fails. |
| Missing library summaries | Calls without a precise summary still lose facts conservatively. Of 47,030 inventoried API entries, 443 have explicit footprints and one has a view summary; the view overlaps the footprint count. Complete catalog coverage is not complete behavioral modeling. |

Numeric-bound obligations remain deferred. Shared-object refinement rejection and information hiding remain the requested ownership policy. The positive local-capture and inferred-release examples still work, while their deliberately invalid escape/transfer cases remain rejected.

### Read-only calls still require distinct borrowed arguments

This is a concrete remaining integration gap:

```java
int[] values = {1, 2};
java.util.Arrays.equals(values, values);
// PERMISSION: Non-distinct parameters or receiver in call
```

The shipped declaration has `@Modifies({})` and borrowed parameters. Both arguments here refer to one controlled local array. The equivalent comparison of two separately allocated local arrays passes. Latte's distinctness check does not yet consult the empty footprint. The evidence includes both methods in `ReadOnlyAliasedArguments.java`.

A useful next change would allow overlapping borrowed inputs for verified read-only calls, while retaining the checks required by ownership transfers. This does not require changing the policy for shared object refinements.

### View validity remains conservative

In `SubListModCount.java`, lines 40, 47, 53 and 59 intentionally exercise invalid views and expect `ConcurrentModificationException`; their new state diagnostics are excluded from the comparison. Line 45 creates a nested view from an already invalid parent outside those exception handlers. The checker rejects that creation, while the pinned OpenJDK implementation creates the view and detects invalidation at a later operation. The unchanged standalone test was executed successfully on Java 21.

This is a boundary between the checker model and implementation behavior. The [Java 21 API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ArrayList.html#subList(int,int)) leaves sublist semantics undefined following structural modification of the backing list outside the view. The passing implementation test therefore does not establish an API guarantee that the contract must promise. See the [pinned implementation](https://github.com/openjdk/jdk21u/blob/7a7090bd432d6e8b4e6106593957513a84e7add4/src/java.base/share/classes/java/util/ArrayList.java).

## Full replay

The replay uses exactly the original **1,481 source files**, with verified identical hashes, Java 21, 16 workers and a 60-second limit per analysis mode. Both Latte and LiquidTypeChecking run with and without the catalog. **1,460 files complete both modes**, including all **1,389 previously comparable files** and 71 previously inconclusive files. The catalog run has 20 timeouts and the existing Spoon type-erasure failure in `StructuredTaskScopeTest.java`. The earlier metadata null dereference and solver stack overflow do not recur. Completion improvements include those earlier fixes and timing effects; they cannot all be attributed to the new annotations.

For the same 1,389 completed files, after the historical exception exclusions and seven newly reviewed exception diagnostics:

| Diagnostic category | Before | Current |
| --- | ---: | ---: |
| Permission | 566 | 679 |
| Shared refinement | 486 | 488 |
| State | 0 | 1 |
| **Ownership/state total, excluding refinement proofs** | **1,052** | **1,168** |
| Files with ownership/state diagnostics | 476 | 482 |
| Refinement proof obligations, reported separately | 106 | 120 |
| All type diagnostics | 1,158 | 1,288 |
| Files with any type diagnostic | 483 | 490 |

The newly excluded diagnostics are three on the explicitly null `String.getChars` destination and four expected invalid-view operations. The common-cohort additions were reviewed for exception contexts; ordinary assertions, failure-reporting code and generic error catches were retained.

There are **23 absent old diagnostics**, but nine are on lines that still have type errors. Four of those merely move to another call on the same line. **Fourteen old diagnostics disappear from now-clear lines**, and one formerly failing file, `FloatDoubleOrder.java`, becomes wholly diagnostic-free. Eight previously clear files gain type errors.

The 122 newly reported permission diagnostics comprise 99 shared-to-borrowed mismatches, 21 non-distinct-input errors and two override-footprint errors. Expanded array contracts expose shared helper inputs and conservatively modeled return values; the read-only alias example above independently demonstrates unnecessary distinctness. The override checks also expose effects beyond a promised footprint: one test explicitly writes to another output stream, while another constructs an exception whose effects are not summarized. These categories are not evidence of 122 Java library defects.

The embedded `OverrideConstructorEffects.java` isolates the latter case: an `ArrayList.add` override that only forwards to `super.add` passes; adding a conditional `throw new Error(...)` causes an override-footprint rejection. Unknown constructor effects remain conservative, even when the affected allocation is only used as an exception.

The additional 71 completed files have no comparable previous diagnostic baseline. Their new diagnostics are recorded, but have not received an exhaustive expected-exception review; they are excluded from the before/after table. No passing source analysis establishes the upstream test assertions, and this replay is not execution of the full jtreg suite.

## Reproduce

Build the checker and audit worker as described in the [original audit](jdk21-upstream-audit.md#reproduce). From the repository root, with Java 21 selected:

```sh
python3 scripts/audit-openjdk-tests.py /path/to/jdk21u \
  --revision 7a7090bd432d6e8b4e6106593957513a84e7add4 \
  --selection audits/jdk21-upstream/selection.json \
  --output latte/target/openjdk21-effects-followup \
  --workers 16 --timeout 60
```

The reproduction JSON embeds `EffectAblation` and the standalone sources. Compile the harness against `latte/target/latte-cli.jar`; its arguments are the source path, `normal` or `without-summaries`, and the output JSON path. Existing ownership examples use the CLI without `--jdk`; JDK examples use `--jdk all`.
