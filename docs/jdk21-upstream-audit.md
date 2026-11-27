# OpenJDK 21 test-source audit

The bundled JDK specifications do introduce errors in unchanged OpenJDK tests. The original audit below distinguishes intended restrictions, precision limits, and analyzer crashes. The array-constructor metadata null dereference and solver-translation stack overflow have since been fixed; the original run counts remain a historical snapshot. The [effect-summary follow-up](jdk21-effects-followup.md) replays the complete selection with the revised catalog and compares the remaining limitations. An error here does not establish a defect in OpenJDK or a false postcondition in a specification.

## Inputs and method

The source is OpenJDK `jdk-21.0.13.1+2`, commit [`7a7090bd432d6e8b4e6106593957513a84e7add4`](https://github.com/openjdk/jdk21u/tree/7a7090bd432d6e8b4e6106593957513a84e7add4), matching the catalog's source baseline. The audit uses Temurin 21.0.13.1+2 and the current Latte/LiquidTypeChecking implementation.

The main corpus consists of 1,479 Java test sources from `test/jdk` and `test/jaxp`. Selection includes every test-bearing source below a stronger target's `test/jdk/<qualified-type>` directory, plus type-named tests below matching package paths. Both jtreg `@test` headers and TestNG/JUnit `@Test` annotations count. Selection is by path/name; it does not imply that every selected test calls a stronger member. Two additional Java 21 sequenced-collection tests, `Basic.java` and `BasicMap.java`, bring the total to **1,481 sources**. The source trees contain 13,410 test-bearing Java files under the header/annotation rule, so this is a targeted audit, not the entire OpenJDK suite. Compiler/VM tests and native tests are outside this run.

Each source is analyzed independently, unchanged, with Spoon's Java 21 compliance and no-classpath mode. Each is checked twice: with `--jdk all` and without the catalog. The audit explicitly runs both Latte and LiquidTypeChecking in both modes; the regular CLI can skip the latter when there are no refinement annotations. A diagnostic is counted as introduced only when both analyses finish and its file, line, column, kind and message are absent from the baseline. Counterexample text is not part of identity.

These are source analyses, not jtreg executions. TestNG/JUnit data providers, jtreg directives, helper sources and test libraries are not assembled or executed. Unresolved helpers can reduce precision. A successful analysis does not establish that Java `assert`, TestNG assertions or hand-written failure checks hold: those checks have not been translated into refinement obligations. Timeouts, crashes and model failures are not accepted cases. A completed analysis can still contain `SYNTAX` or `UNSUPPORTED` diagnostics.

## Results

The [machine-readable report](../audits/jdk21-upstream/report.json) records all 1,481 inputs, source hashes, statuses, introduced diagnostics and resolved stronger member uses. The [selection manifest](../audits/jdk21-upstream/selection.json) permits an exact input replay.

- **1,389 sources completed both analyses.** Of these, **487 gained type errors** (`PERMISSION`, `SHARED_REFINEMENT` or `REFINEMENT`). There were 568 new permission diagnostics, 490 shared-refinement diagnostics and 116 refinement diagnostics.
- **488 sources gained any diagnostic** when the six new `SYNTAX` diagnostics are included. Those are not counted as type errors.
- **713 sources completed with the catalog and no diagnostics**, versus 942 without it. These counts are not proofs of the upstream test assertions.
- With the catalog, **59 sources hit the metadata null dereference**, **one failed type-erasure resolution**, **one hit the solver-translation stack overflow**, and **30 timed out**. Without the catalog, 30 timed out and none crashed. There are **92 sources without a completed pair**; their partial diagnostics are excluded from differential totals.
- The worker recorded **248 distinct refined target/signature pairs**. This is use-site coverage, not a declaration-coverage percentage: inherited target names can refer to the same contract.

The table groups source directories, not every Java SE package. “New type errors” counts files with at least one new error among completed pairs; “Incomplete” includes crashes and timeouts. Findings apply to this recorded checker/catalog snapshot and workload limits.

| Source family | Sources | Both completed | New type errors | Incomplete with catalog |
| --- | ---: | ---: | ---: | ---: |
| `test/jaxp/javax/xml` | 7 | 7 | 0 | 0 |
| `test/jdk/java/awt` | 486 | 480 | 204 | 6 |
| `test/jdk/java/io` | 11 | 11 | 4 | 0 |
| `test/jdk/java/lang` | 453 | 399 | 107 | 54 |
| `test/jdk/java/math` | 51 | 47 | 5 | 4 |
| `test/jdk/java/net` | 48 | 46 | 31 | 2 |
| `test/jdk/java/nio` | 77 | 74 | 24 | 2 |
| `test/jdk/java/security` | 9 | 9 | 3 | 0 |
| `test/jdk/java/sql` | 1 | 1 | 0 | 0 |
| `test/jdk/java/text` | 7 | 7 | 1 | 0 |
| `test/jdk/java/time` | 62 | 60 | 15 | 2 |
| `test/jdk/java/util` | 172 | 152 | 44 | 20 |
| `test/jdk/javax/crypto` | 3 | 3 | 2 | 0 |
| `test/jdk/javax/imageio` | 1 | 1 | 0 | 0 |
| `test/jdk/javax/naming` | 3 | 3 | 1 | 0 |
| `test/jdk/javax/net` | 2 | 2 | 0 | 0 |
| `test/jdk/javax/smartcardio` | 3 | 3 | 1 | 0 |
| `test/jdk/javax/sql` | 3 | 3 | 0 | 0 |
| `test/jdk/javax/swing` | 82 | 81 | 45 | 1 |

## Excluding expected exceptions

After excluding explicitly exception-expected test scenarios, **483 source files still have new type errors**, down from 487. **16 diagnostics are excluded**, leaving **1,158**: 566 `PERMISSION`, 486 `SHARED_REFINEMENT`, and 106 `REFINEMENT`. The [reviewed filter](../audits/jdk21-upstream/exception-filter.json) records source hashes, exact excluded sites, reasons and remaining file counts. The original audit remains unchanged. These counts correct the initial filter's 484 files / 107 refinement diagnostics: investigation found that `String/Exceptions.java:98` runs inside a custom `tryCatch` helper expecting `NullPointerException`.

The [refinement investigation](jdk21-refinement-investigation.md) classifies the 106 remaining refinement diagnostics and reduces the array-length, string-fact, helper-precondition and inherited-contract cases.

Four files drop out entirely:

- `java/util/Random/RandomTest.java`: the only new error is in `testNextIntBoundedNeg`, annotated to expect `IllegalArgumentException`.
- `javax/crypto/spec/IvParameterSpec/NullIV.java`: the null-IV constructor is explicitly expected to throw.
- `java/lang/String/CompactString/NegativeSize.java`: the test expects oversized String construction to throw `OutOfMemoryError`; the diagnostic in its fall-through code is excluded with that negative scenario.
- `java/lang/String/Exceptions.java`: its null-character-array constructor runs in a custom helper expecting `NullPointerException`.

Exception cases are also removed from `Math/Clamp.java`, `ByteArrayOutputStream/Write.java`, `ArrayList/ArrayManagement.java` and `GCMParameterSpecTest.java`. Those four files remain counted because normal test paths still have errors. In particular, Clamp loses four exception-case diagnostics but retains two valid-table failures.

The filter keeps ordinary setup, error-reporting code, and calls unrelated to an expected exception even when they occur in the same `try` block. For example, `SubListModCount` expects a later view access to throw, while its rejected `root.add(42)` should succeed. Platform-dependent tests with both successful and exceptional outcomes and non-source-located diagnostics are retained. The 92 incomplete analysis pairs remain outside this type-error count; they have not been reclassified as passing.

## Confirmed findings

### Correct rejection of intentional exception tests

[`java/lang/Math/Clamp.java`](https://github.com/openjdk/jdk21u/blob/7a7090bd432d6e8b4e6106593957513a84e7add4/test/jdk/java/lang/Math/Clamp.java) intentionally calls integral clamp with reversed bounds inside exception-checking lambdas. LiquidJava requires `max >= min` at the call. Catching `IllegalArgumentException` does not discharge this requirement. Lines 80, 82, 120 and 122 report `REFINEMENT`.

The reduced [ClampTable.java](../examples/jdk21-audit/ClampTable.java) includes both an accepted literal call and a rejected exception test. This is the documented non-throwing call-domain policy, not evidence that the clamp predicate is false.

### Valid table-driven calls lose relations between columns

The same upstream file reports `REFINEMENT` on valid integral clamp calls at lines 72 and 112. Values come from rows of a two-dimensional array. The verifier does not establish the required relation between the lower and upper bounds in each row through the enhanced-for loop.

[ClampTable.java](../examples/jdk21-audit/ClampTable.java) reproduces the issue without helper methods, test libraries or JDK internals. The literal call verifies; the valid row loop does not. The untouched upstream `Clamp.java` was also compiled and executed successfully on the pinned Java 21 runtime. This confirms a rejection of a passing test; it does not prove every bundled arithmetic contract.

### Unannotated helper parameters are shared

JDK tests supply no Latte ownership annotations. For example, [`java/lang/String/Chars.java`](https://github.com/openjdk/jdk21u/blob/7a7090bd432d6e8b4e6106593957513a84e7add4/test/jdk/java/lang/String/Chars.java) passes an unannotated helper's `char[]` parameter to the copying String constructor at line 84; the parameter is shared, so `PERMISSION` and `SHARED_REFINEMENT` are reported. The same restriction applies to an ordinary `byte[]` parameter passed to `Arrays.copyOf`. [SharedArrayHelper.java](../examples/jdk21-audit/SharedArrayHelper.java) demonstrates the rejected helper, an accepted explicitly borrowed/non-null helper, and an accepted locally allocated array.

This is the configured ownership boundary. Passing runtime tests cannot establish that all callers of an unannotated helper supply a locally controlled object.

### Conservative overloads invalidate later precision

[`java/io/ByteArrayOutputStream/Write.java`](https://github.com/openjdk/jdk21u/blob/7a7090bd432d6e8b4e6106593957513a84e7add4/test/jdk/java/io/ByteArrayOutputStream/Write.java) reports borrowing/refinement failures in its bounds-testing helper at line 54. These include intentionally invalid inputs and a shared receiver/array.

Its ordinary write test also reports new ownership/state failures at lines 116, 117 and 127. [ConservativeArrayCopy.java](../examples/jdk21-audit/ConservativeArrayCopy.java) isolates a contributing cause: `Arrays.copyOfRange(byte[], int, int)` has a conservative policy, so subsequent reasoning loses control of the array and stream state. The first write succeeds; the later write and observation are rejected. This was a missing precise library summary, not a runtime retention claim about `Arrays.copyOfRange`. The subsequent effect-catalog revision supplies the copy and stream-write footprints; the reduced example now verifies. These historical counts have not been recomputed.

[`java/util/ArrayList/SubListModCount.java`](https://github.com/openjdk/jdk21u/blob/7a7090bd432d6e8b4e6106593957513a84e7add4/test/jdk/java/util/ArrayList/SubListModCount.java) provides an even smaller example. `root.subList(0, 0)` has conservative returned-alias handling; the following `root.add(42)` at line 38 reports `SHARED_REFINEMENT`. Both references are local in the test, but the original external contract did not express that relationship. The subsequent `@ReturnsView` and `@Modifies` implementation fixes this case: [LocalListView.java](../examples/jdk21-audit/LocalListView.java) now verifies. The original audit counts are retained. The untouched upstream test also compiles and runs successfully on Java 21. This is a consequence of the chosen conservative treatment of returned views, not a prohibition in Java's ArrayList API.

### Array constructor references crashed catalog metadata analysis (fixed)

In the original audit, [`java/util/Objects/CheckIndex.java`](https://github.com/openjdk/jdk21u/blob/7a7090bd432d6e8b4e6106593957513a84e7add4/test/jdk/java/util/Objects/CheckIndex.java) failed during ownership preprocessing with a `NullPointerException`. [ArrayConstructorReference.java](../examples/jdk21-audit/ArrayConstructorReference.java) reduced it to:

```java
return rows.toArray(Object[][]::new);
```

`Jdk21Coverage.annotate` called `actual.getParent(CtType.class).isShadow()`. Spoon's synthetic array constructor has no enclosing `CtType`, so the lookup returned null. The fix excludes synthetic array constructors from JDK member classification and handles an absent declaring type when identifying source declarations. Ordinary library references retain their effective policies, and conservative library calls do not gain stronger result guarantees from an array-constructor callback.

Two regression tests cover primitive/reference arrays, multiple dimensions, ordinary constructor and method-reference policies, and rejection of an unjustified refinement on `List.toArray`'s result. The reduced example now verifies through the packaged CLI. All 575 tests pass, and the upstream LiquidJava compatibility audit remains at 180/311 exact matches. A separate metadata replay completed for all 59 previously affected OpenJDK sources, covering 117 array constructor references without assigning them JDK contracts.

The [follow-up results](../audits/jdk21-upstream/null-dereference-fix.json) record a full replay of those 59 sources with and without the catalog: **57 completed in both modes, two timed out in both modes, and none crashed**. The remaining timeouts are `Objects/CheckIndex.java` and `Objects/CheckLongIndex.java`, each under a 120-second per-mode limit. Both passed the separate metadata check. Completed analyses can still report ownership, refinement or syntax diagnostics; this fix does not make the JDK test assertions proven.

The supplementary [`SequencedCollection/Basic.java`](https://github.com/openjdk/jdk21u/blob/7a7090bd432d6e8b4e6106593957513a84e7add4/test/jdk/java/util/SequencedCollection/Basic.java) and [`BasicMap.java`](https://github.com/openjdk/jdk21u/blob/7a7090bd432d6e8b4e6106593957513a84e7add4/test/jdk/java/util/SequencedCollection/BasicMap.java) were among the affected sources. Both now complete the full analysis and report diagnostics instead of crashing.

The remaining ownership-stage failure is [`StructuredTaskScope/StructuredTaskScopeTest.java`](https://github.com/openjdk/jdk21u/blob/7a7090bd432d6e8b4e6106593957513a84e7add4/test/jdk/java/util/concurrent/StructuredTaskScope/StructuredTaskScopeTest.java): Spoon cannot resolve a type parameter's erasure. Its baseline analysis completes with diagnostics. This has not been reduced independently of the no-classpath setup and is recorded separately from the confirmed null-dereference defect.

### Solver translation exhausted the Java stack (fixed)

In the original audit, [`java/util/Formatter/Constructors.java`](https://github.com/openjdk/jdk21u/blob/7a7090bd432d6e8b4e6106593957513a84e7add4/test/jdk/java/util/Formatter/Constructors.java) completed the baseline analysis but failed with the catalog during LiquidTypeChecking. An independent CLI replay reproduced `StackOverflowError`, with repeated `Prover.translate` frames at line 80. That original result is inconclusive and remains outside the original type-error counts.

Flattening consecutive conjunctions or disjunctions did not handle deeply alternating operators, nested expressions, or shared path conditions. Translation now visits the complete term graph with an explicit work stack and an identity-based cache scoped to each translation. Children are translated before their parent; every distinct node is translated once. The solver operators and proof acceptance criteria are unchanged.

Two added regression tests failed with stack overflows before the fix. They cover 10,000 nested branch merges with shared conditions, and deep array stores, choices, calls and unary expressions. They check valid proofs, satisfiability and SAT rejection of invalid claims. The existing deep conjunction/disjunction tests remain in place.

The [follow-up replay](../audits/jdk21-upstream/formatter-crash-fix.json) now completes both analysis modes for the untouched Formatter source: the catalog mode reports seven `SHARED_REFINEMENT` diagnostics, and the baseline reports none. Neither mode crashes. All 577 tests and formatting checks pass; compatibility remains at 180/311 exact matches. These are source-verification outcomes, not execution of the upstream test or proof of its assertions.

## Reproduce

Use the pinned OpenJDK checkout and Java 21. From the Maven module, build the worker and packaged checker:

```sh
mvn test-compile package -DskipTests
```

Then, from the repository root:

```sh
python3 scripts/audit-openjdk-tests.py /path/to/jdk21u \
  --revision 7a7090bd432d6e8b4e6106593957513a84e7add4 \
  --output latte/target/openjdk21-audit \
  --workers 16 --timeout 60
```

The default selection regenerates the 1,479-file main corpus. To include the two additional sequenced-collection tests in one run, add `--selection audits/jdk21-upstream/selection.json`. To reproduce their separate run exactly, use `--selection audits/jdk21-upstream/sequenced-selection.json --timeout 120` and a different output directory.

To replay the null-dereference fix, use `--selection audits/jdk21-upstream/null-dereference-selection.json --timeout 120 --workers 12` and a fresh output directory. The recorded follow-up includes source hashes and the rebuilt checker fingerprint; the original audit and exception-filter JSON files are preserved.

The driver records source hashes, the runtime version, catalog/JAR/worker hashes, both outcomes, effective AST call policies, and the differential diagnostics. Each mode runs in its own JVM with a 1 GiB heap limit and a 60-second wall-clock limit. Results are resumable only with identical inputs. The supplementary two-test run used a 120-second limit per mode; both catalog failures occurred before that limit. The process pool is bounded; no upstream test body is executed by this command. Timing and solver-related results can vary under resource contention.

The small reproductions can be checked individually with:

```sh
java -jar latte/target/latte-cli.jar --jdk all examples/jdk21-audit/ClampTable.java
```

`ArrayConstructorReference.java` now verifies after the metadata fix. `ConservativeArrayCopy.java` and `LocalListView.java` verify with the effect/view summaries. `ClampTable.java` and `SharedArrayHelper.java` retain their expected errors. The original audit and exception filter remain historical records; the [follow-up](jdk21-effects-followup.md) records the current outcomes without weakening contracts to accommodate tests.
