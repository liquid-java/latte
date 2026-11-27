# External library contracts

For supplied JDK contracts, see the [Java 21 catalog and usage guide](jdk21-contracts.md). Select them explicitly with `--jdk`; application specifications continue to work independently.

Place a specification type alongside the application sources, annotated with the library's qualified Java name. The verifier uses the existing annotation syntax for both Latte ownership and LiquidTypeChecking refinements. Library source changes are unnecessary.

```java
@ExternalRefinementsFor("java.util.ArrayList")
@RefinementPredicate("int size(ArrayList list)")
interface ArrayListContracts<E> {
    @StateRefinement(to = "size(this) == 0")
    void ArrayList();

    @StateRefinement(to = "size(this) == size(old(this)) + 1")
    boolean add(E value);

    @StateRefinement(from = "true")
    @Refinement("_ == size(this)")
    int size();
}
```

A constructor is represented by a `void` method with the target class's simple name. Methods use their Java names, parameter types and result types. Parameter names may differ from the library: predicates use the names in the specification. Overloads are matched by erased Java parameter types, including generic bounds. Static library methods can be written as ordinary interface method declarations. Receiver state annotations require an instance method or constructor.

```java
java.util.ArrayList<Integer> list = new java.util.ArrayList<>();
java.util.ArrayList<Integer> alias = list;
alias.add(42);
@Refinement("_ == 1") int count = list.size();
```

Both variables have the same Latte identity, so the transition through `alias` changes the state read through `list`. State predicates and state sets belong to `java.util.ArrayList`, independently of the specification type's name. Contracts on inherited Java members are found through the target's class and interface hierarchy. Constructors are never inherited.

## Ownership and effects

Use `specification.Borrowed`, `Unique`, `Free` and `Shared` on external parameters, results and fields with the same meaning as source contracts. Shared references remain ineligible for refinements. A state contract does not upgrade a shared receiver into an owned one.

```java
@ExternalRefinementsFor("java.util.Arrays")
interface ArrayContracts {
    void fill(@Borrowed int[] values, int value);
}
```

Here Latte retains local ownership of `values`; LiquidTypeChecking invalidates its old contents because borrowing permits mutation. A reference argument without an ownership annotation is shared and can escape. A `@Free` parameter consumes all local aliases, and a `@Free` result can carry refinements. Constructor parameters follow the same rules. The checked [examples](../examples/refinements/ExternalLibraryContracts.java) include valid local aliases and rejected stale values, consumed aliases and shared receivers.

A receiver contract with only `from`, including `from = "true"`, promises to preserve its state. A `to` clause describes a transition. Without a receiver state clause, a specified instance method may change the receiver. Without an explicit mutation footprint, reference parameters may change regardless of their ownership annotation. Calls missing from the specification retain conservative unknown-call behavior.

Specifications are trusted public contracts; library bodies, placeholder bodies and field initializers are not verified as implementations. State transitions and reference results must accurately describe the library. Without an explicit `@Modifies` footprint, external calls also conservatively invalidate potentially affected application fields; ownership and receiver contracts alone do not promise global purity. Library internals and element identities are not inferred from a container-size contract. For example, this ArrayList specification does not establish an ownership or alias relationship between an inserted object and a later `get` result.

External fields can carry refinements and ownership annotations. Actual Java declarations determine whether fields are static; placeholder initializer values never describe the library's storage. Writes in application code must satisfy the field contract. A shared reference field still cannot carry a refinement.

### Explicit mutation footprints: `@Modifies`

`specification.Modifies` supplies a complete write footprint for an external method. It is independent of ownership: borrowing permits mutation, and a mutation footprint does not grant permission to retain references.

```java
@ExternalRefinementsFor("java.util.Arrays")
interface ArraysContracts {
    @Modifies("array[*]")
    void fill(@Borrowed int[] array, int value);

    @Modifies({})
    @Free
    int[] copyOf(@Borrowed int[] array, int newLength);
}
```

`fill` preserves local control of the array while invalidating its old element facts through every local alias. `copyOf` preserves the input and returns a fresh array. A footprint says where writes may happen; it does not establish the resulting element values. Additional refinements describe results and non-throwing argument domains. [ExternalEffectSummaries.java](../examples/refinements/ExternalEffectSummaries.java) includes accepted controls and a rejected stale-element refinement.

| Syntax | Meaning |
| --- | --- |
| Annotation omitted | Existing conservative external effects, including possible global mutation. |
| `@Modifies({})` | No writes to pre-existing observable storage, including globals. Allocation and initialization of fresh storage are allowed. |
| `@Modifies("array[*]")` | May replace the named array's elements; does not permit mutation of objects stored in those elements. |
| `@Modifies("this.*")` | May mutate storage reachable from the receiver. |
| `@Modifies({"first.*", "second[*]"})` | Union of the listed reference-root footprints. A bare root also denotes its reachable storage. |

Footprints apply on normal and exceptional exits. Unlisted storage is preserved. Receiver state transitions must include `this` in the footprint, except constructor transitions that initialize a fresh receiver. A receiver `from` clause without `to` continues to preserve its abstract state; reference-parameter writes remain separate effects.

Roots must be `this` or declared reference parameters. `this` is invalid on static methods, and `[*]` requires an array. Currently supported paths are roots, `root.*`, and `root[*]`; field-specific paths and arbitrary expressions are rejected. Annotation values must be literal strings/string arrays (literal concatenation is allowed); unresolved constant references are rejected, never interpreted as an empty footprint.

These annotations are trusted external declarations. Source implementations continue to get inferred effects. A source override that exceeds an external `@Modifies` promise is rejected, including transitive global effects. Missing external summaries retain conservative behavior.

### Captured list views: `@ReturnsView`

`specification.ReturnsView` describes a distinct List view backed by a half-open range of another List:

```java
@ExternalRefinementsFor("java.util.ArrayList")
interface ArrayListContracts<E> {
    @Modifies({})
    @ReturnsView(of = "this", from = "fromIndex", toExclusive = "toIndex")
    java.util.List<E> subList(int fromIndex, int toIndex);
}
```

The result captures its backing list. Creating a local view preserves local control; escaping the view propagates escape to the backing resource. A shared backing produces a non-refinable view, and a view of borrowed storage cannot escape that storage's scope. Result permission annotations (`@Free`, `@Unique`, `@Borrowed`, `@Shared`) must be omitted because ownership is derived from the backing relationship.

`of` accepts `this` or a borrowed List parameter. `from` and `toExclusive` name `int` parameters. Both backing and result must be List types, and creation requires explicit `@Modifies({})`. The range describes normal return; ordinary parameter refinements can additionally require callers to establish a non-throwing domain. Source overrides of a `@ReturnsView` method are currently rejected as unsupported because their backing relationship is not yet checked against the inherited promise.

For the code-reviewed `ArrayList.subList` implementation, the checker supports `size`, `isEmpty`, `get(int)`, `set(int,E)`, `add(E)`, `add(int,E)`, `remove(int)`, `clear`, and nested `subList`. Local aliases share view state. Structural mutations through a view update its size, its ancestors' sizes and the backing list's specified size; sibling or descendant views retain old versions and become invalid. `set` preserves valid view sizes while invalidating affected content state. Structural changes through the backing list invalidate existing views. These checks report `STATE` when a view's validity cannot be established.

Backing-size updates use the equality in the declared `size()` result refinement, without requiring a particular ghost name. If no suitable size summary is available, backing state is invalidated conservatively. Element identities and values remain unspecified/shared; a size relationship does not expose hidden elements. Mutation inside loops can conservatively lose view validity.

Custom factories can publish the same capture/range metadata, but that alone does not establish the mutation effects of their returned implementations' methods. Those operations remain conservative. Iterators, reversed views, callback operations, and other view families also retain conservative handling.

[ExternalListViews.java](../examples/refinements/ExternalListViews.java) demonstrates alias updates, nested views, stale-view rejection and escape propagation. The Java 21 catalog supplies the ArrayList view contract and explicit effects for reviewed arithmetic, arrays, strings, in-memory I/O, checksums, value observers and defensive copies. Its [package-by-package effect inventory](jdk21-effects.md) records precise and conservative choices separately.

### AST metadata shared by both passes

Validated contracts carry `ExternalSummaries.EFFECTS` (`latte.external.effects.v1`) and `ExternalSummaries.VIEW` (`latte.external.view.v1`). Invalid declarations never acquire a trusted view binding. Inferred/effective mutation summaries remain available through `MutationEffects.on`.

A view creation has `ViewFacts.KEY` (`latte.analysis.view.v1`) containing the result identity, backing identity and parameter indices. `LatteFacts` retains the capture edge in its immutable heap under `ViewFacts.BACKING`. Supported view calls carry `ViewFacts.OPERATION`. LiquidTypeChecking reads this metadata and keeps range and version relationships in its persistent, path-sensitive symbolic state. Metadata is rebuilt on reanalysis; it is not a process-global cache.

## Library classpath

The JDK is available automatically. For third-party libraries, pass their JARs or class directories. Multiple input paths are analyzed together, so specifications may live separately from the application:

```sh
java -jar latte/target/latte-cli.jar \
    --classpath 'lib/library.jar:lib/dependency.jar' \
    specs src/main/java
```

The classpath separator is platform-specific (`:` on Unix, `;` on Windows). `-cp` is an alias for `--classpath`.

Embedding uses the same pipeline:

```java
var result = Verification.verify(
    List.of("specs", "src/main/java"),
    List.of("lib/library.jar", "lib/dependency.jar"));
```

`Verification.verify(Launcher)` also accepts a caller-configured Spoon launcher. Reading signatures does not initialize or execute library classes.

The per-model registry is stored on the Spoon root package under `latte.analysis.external.v1`. Resolved executable references retain their external binding; `ExternalSpecifications.resolve(reference)` exposes the contract. Both passes use that lookup, while ordinary ownership, effects and transfer metadata remain on the same AST. Reanalysis rebuilds the registry; there is no process-global specification cache.

## Validation and boundaries

Unresolvable types, missing members, incompatible result or field types, misplaced annotations and duplicate specifications for the same target are errors. The verifier does not silently ignore an invalid contract. Keep one specification type per target and include its dependencies on the classpath.

Expanded varargs calls currently require an explicit array argument when using an external contract. The verifier reports `UNSUPPORTED` for implicit packing rather than applying an array contract to individual elements. Varargs method references require a lambda that passes an explicit array. Ordinary reference-array ownership limitations still apply. Contract validation is not a replacement for Java compilation or a proof of the library implementation.
