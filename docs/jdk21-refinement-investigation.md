# Investigating “Cannot establish refinement”

This diagnostic means that the current symbolic facts do not prove a required predicate. It can indicate an actual contract violation, missing input assumptions, missing specifications, lost facts, or an incomplete solver result. A solver counterexample is a counterexample to the symbolic model, not necessarily an executable Java input.

The [per-site results](../audits/jdk21-upstream/refinement-investigation.json) cover **106 diagnostics in 61 source files** from the original OpenJDK audit after expected-exception filtering. The earlier count of 107 included `String/Exceptions.java:98`, an anonymous `Runnable` passed to `tryCatch` with `NullPointerException.class`. The exception filter now excludes that case. The 40 refinement diagnostics newly exposed by the metadata null-dereference fix are outside this investigation.

| Required predicate | Diagnostics |
| --- | ---: |
| String character index is in bounds | 35 |
| Array length is nonnegative | 29 |
| Random-number bound is positive | 19 |
| ArrayList index is in bounds | 5 |
| Other predicates, including clamp bounds and inherited postconditions | 18 |

These are obligation categories, not mutually exclusive root causes. The recorded solver outcomes are **105 SAT countermodels and one timeout**, at `java/nio/Buffer/Chars.java:55`. Increasing the timeout would not resolve the SAT cases without changing their available facts or model.

## Confirmed missing array-length bounds

All 29 direct array-length obligations require the Java `int` result of an array-length read to be nonnegative. This includes Swing model row/column counts and `Arrays.copyOf(array, array.length)`. `LiquidTypeChecking.expressionValue` translates the length read into an uninterpreted `length(reference)` without asserting `0 <= length(reference) <= Integer.MAX_VALUE`. Conversion to `int` then wraps the unconstrained value.

[ArrayLengthBounds.java](../examples/jdk21-audit/ArrayLengthBounds.java) reproduces the failure with a borrowed array parameter and an explicitly nonnegative result. The solver chooses length **2,147,483,648**, yielding **−2,147,483,648** after conversion. That array length is impossible in Java. A local `new int[3]` control verifies because allocation supplies its exact length. This issue can be fixed independently of tracking mutable array elements or changing the shared-object policy.

## Missing and discarded string facts

[StringFacts.java](../examples/jdk21-audit/StringFacts.java) confirms two separate problems:

- `"abc".charAt(0)` fails: the literal does not establish the catalog's `stringLength` ghost, so the solver can assign it zero. Constructing a String from a known three-character array verifies.
- Two borrowed Strings are checked for equal lengths before a loop. In the loop, `a.charAt(i)` verifies under `i < a.length()`, but `b.charAt(i)` fails. `MutationEffects.on` marks external contracts as potentially modifying globals, and `loopEffects` responds by replacing object-state versions with unknown versions. The equality established before the loop is lost, even though these String operations cannot change either String's length.

The global-effect fallback is deliberately conservative: a receiver state contract does not promise that a method leaves application globals alone. Improving it requires sound effect information for the relevant library operations and/or preservation of immutable facts. Treating every external contract as pure would be unsound. These reductions explain mechanisms behind string failures; they do not establish that all 35 reports have the same cause. The subsequent external-summary implementation adds explicit `@Modifies({})` to the selected String observers: the borrowed-string loop now verifies. The literal-length case remains unresolved. Historical counts above have not been recomputed.

## Missing caller and table information

[HelperBounds.java](../examples/jdk21-audit/HelperBounds.java) shows why a valid caller is insufficient: `unchecked(10)` calls a helper whose unannotated `bound` parameter is checked for arbitrary integers. The helper's `nextInt(bound)` fails. Adding a positive-parameter refinement or a guard makes it verify. The upstream GCM test's success helper similarly lacks a nonnegative `tLen` assumption.

The existing [ClampTable.java](../examples/jdk21-audit/ClampTable.java) demonstrates a different precision loss. A foreach variable is fresh and is not linked to the initialized table's possible rows, so the checker loses `min <= max`. This is distinct from the universal array-length bounds above. Other random-bound failures need facts about returned values; for example, the BigDecimal string-constructor test needs to know that `BigInteger.toString()` is nonempty.

## A correctly rejected test double

`RandomNextDoubleBoundary.java:80` overrides `RandomGenerator.nextDouble()` to return `Double.MAX_VALUE`. [RandomOverride.java](../examples/jdk21-audit/RandomOverride.java) reproduces the rejection: the inherited contract requires a value in `[0, 1)`. This test double deliberately supplies a value outside the API contract. It is neither a solver false alarm nor an expected-exception call.

## Reproduction and next steps

Run the four new examples with Java 21 using `java -jar latte/target/latte-cli.jar --jdk all --explain examples/jdk21-audit/<Example>.java`. Each `// Expect: REFINEMENT` marks a reproduced diagnostic; unmarked controls verify. The examples compile as Java 21 sources with the checker JAR on the classpath. No verifier semantics changed during the original investigation. The subsequent external-summary change updates the String loop example as described above.

The array-length invariant and literal String lengths remain to be fixed. Explicit mutation footprints now preserve facts across the specified library calls and observer loops. Caller contracts and table relationships require separate treatment. The audit also contains other obligations, including an implicit-source-location report, that have not been reduced individually; the category totals must not be reported as counts of confirmed verifier defects.
