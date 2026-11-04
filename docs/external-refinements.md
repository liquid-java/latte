# External library contracts

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

A receiver contract with only `from`, including `from = "true"`, promises to preserve its state. A `to` clause describes a transition. Without a receiver state clause, a specified instance method may change the receiver. Reference parameters may change regardless of their ownership annotation. Calls missing from the specification retain conservative unknown-call behavior.

Specifications are trusted public contracts; library bodies, placeholder bodies and field initializers are not verified as implementations. State transitions and reference results must accurately describe the library. External calls also conservatively invalidate potentially affected application fields; ownership and receiver contracts do not promise global purity. Library internals and element identities are not inferred from a container-size contract. For example, this ArrayList specification does not establish an ownership or alias relationship between an inserted object and a later `get` result.

External fields can carry refinements and ownership annotations. Actual Java declarations determine whether fields are static; placeholder initializer values never describe the library's storage. Writes in application code must satisfy the field contract. A shared reference field still cannot carry a refinement.

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
