# Java 21 standard-library contracts

The opt-in catalog covers **all 178 Java SE 21 packages**, with an explicit policy for every public/protected API member. The [package-by-package inventory](jdk21-coverage.md) records **4,055 API types and 47,030 member entries**: **501 declarations** provide stronger contracts; **46,529 entries** use conservative defaults. Ordinary inherited members are resolved through indexed ancestors rather than duplicated at every subtype.

This completes **conservative API coverage**, not precise behavioral specifications for every operation. Explicit mutation footprints and captured List views are supported; arbitrary callback effects, concurrent interference and other returned-alias families remain conservative. Every conservative entry states that no stronger guarantee is supplied. These entries are mechanically classified from the API surface and source archive; they do not represent a manual review of every implementation body.

Contracts are independently written from the [Java SE 21 API specification](https://docs.oracle.com/en/java/javase/21/docs/api/index.html) and the Java source at [OpenJDK `jdk-21.0.13.1+2`](https://github.com/openjdk/jdk21u/tree/jdk-21.0.13.1%2B2). Each specification has links to both; [catalog.json](../specifications/jdk21/catalog.json) records the source file's SHA-256, review rationale and evidence level. The 501 declarations include 110 generated enum ordinal bounds and 391 other contracts. The enum rule follows `Enum.ordinal` and compiler-assigned constant positions; it does not assert enum freshness. The declarations are trusted contracts, not machine-checked proofs of OpenJDK. Native implementations have only their Java wrappers reviewed. For abstract APIs, the contract states the documented obligations on providers; it does not certify every JDBC driver, filesystem provider, GUI model or XML implementation.

The [effect and ownership inventory](jdk21-effects.md) separately accounts for **384 empty footprints**, **59 write footprints**, and **one captured List-view result** across all packages. The view also has an empty footprint. **58 of the 501 stronger declarations deliberately omit `@Modifies`**: their scalar guarantees remain usable, but their effects are unknown. Together with the conservative API entries, 46,587 entries have no explicit footprint. An omitted annotation never means purity.

## Use

From the repository root, after building the Maven module:

```sh
java -jar latte/target/latte-cli.jar --jdk all examples/jdk21/LibraryContracts.java
java -jar latte/target/latte-cli.jar --jdk all examples/jdk21/RejectedContracts.java
java -jar latte/target/latte-cli.jar --jdk java.lang,java.util src/main/java
```

The first example is accepted. The second deliberately reports the five diagnostics marked in its source. [ConservativeLibrary.java](../examples/jdk21/ConservativeLibrary.java) demonstrates calls governed by defaults; [RejectedConservativeContracts.java](../examples/jdk21/RejectedConservativeContracts.java) shows rejected assumptions about captures, aliases, constructors and concurrent observations. Package names select exactly that package, not descendants. `all` selects the complete conservative inventory and every stronger contract. Every listed Java SE package is selectable, including packages with no stronger declarations. Unknown package names are errors. The Java 21 examples [Java21Contracts.java](../examples/jdk21/Java21Contracts.java) and [RejectedJava21Contracts.java](../examples/jdk21/RejectedJava21Contracts.java) exercise saturation, exact division, sequenced-list ends, reversed views, enum ordinals and virtual-thread captures. Repeat `--jdk` to combine selections. Contracts are packaged in both JARs and require no source checkout or network access at verification time.

The default remains application-supplied specifications only. Select packages explicitly so applications can maintain alternative library policies. Including a bundled specification and an application specification for the same target remains a duplicate-target error; neither silently overrides the other.

```java
var result = latte.api.Verification.verify(
    List.of("src/main/java"), List.of(), Set.of("java.lang", "java.util"));

// Or, before building a caller-configured Spoon model:
latte.api.Jdk21Contracts.addTo(launcher, Set.of("all"));
var packages = latte.api.Jdk21Contracts.packages();
```

The loader adds stronger specification sources to the application's Spoon launcher and attaches an immutable coverage ledger to the model. After binding external specifications, it classifies executable and field references on that same AST. `Jdk21Coverage.on(element)` returns the ledger; `Jdk21Coverage.use(reference)` returns the effective target, erased signature, policy and reason. The metadata key is `latte.jdk21.coverage.v1`. A member listed as refined in the inventory is effective only when its specification package is selected. A supplied application contract can strengthen a conservative entry; the ordinary duplicate-target check still applies if a bundled stronger specification exists.

Conservative entries use unknown-library handling rather than empty trusted declarations. Calls may retain or mutate reference arguments and receivers; borrowed references and borrowed captures cannot escape. Reference results may alias inputs or shared external objects. Selected unmodeled JDK constructors also make their receiver shared because allocation does not rule out publication during construction. Stronger audited constructors retain their specified behavior. Java language facts, such as literal values and compile-time constants, still apply independently of the catalog.

Use Java 21 when validating this baseline. On later runtimes, selected package members absent from the ledger are marked `outside-baseline` and retain conservative handling; they do not acquire Java 21 guarantees. The catalog covers the exported Java SE API in `java.*` modules, including packages named `javax.*`, `org.w3c.*` and `org.xml.*`. JDK-specific `jdk.*` modules, non-exported internals and third-party libraries are outside this catalog. The inventory includes Java 21 preview APIs, including `java.lang.foreign`, scoped values and structured task scopes, with conservative policies. This does not enable preview source syntax or establish their lifecycle, confinement or synchronization guarantees.

## What the contracts express

| Area | Selected guarantees |
| --- | --- |
| `java.lang` | Exact integral operations on normal return, floor/ceiling division and integral clamp, the `MIN_VALUE` exception, IEEE classification, owned String construction and character-array copies |
| `java.util` | ArrayList size transitions, Java 21 first/last operations and captured subList views; explicit effects for selected ArrayList and primitive Arrays operations; index guards |
| `java.io`, NIO | ByteArrayOutputStream byte counts and copies; retaining ByteArrayInputStream constructor; stream read bounds; buffer bounds and file-channel snapshots |
| `java.math`, `java.time` | Scalar observer bounds, normalized nanoseconds and calendar ranges; cached values remain shared |
| Enums across 42 packages | Bounded ordinal results for all 110 public/protected Java SE enums; constants and factory results remain shared |
| Concurrency | Snapshot counts and estimates, without persistent state guarantees across interference |
| ZIP and regex | Unsigned checksum values, optional-group sentinels and normal-return match positions |
| JDBC and XML | Selected count/sentinel contracts on interfaces; DOM results remain aliases to live documents |
| Desktop, naming, management | Selected scalar counts and ranges, without GUI-thread, listener or external-resource protocols |
| Crypto parameters and smart-card formats | Defensive-copy constructors/results and unsigned APDU field bounds; no cryptographic or device correctness claim |

Stateful contracts require a locally controlled receiver. This example demonstrates the Latte handoff:

```java
var list = new java.util.ArrayList<Integer>();
var alias = list;
alias.add(42);
@Refinement("_ == 1") int size = list.size();
```

Calling the stateful `size()` contract on an unannotated shared ArrayList parameter is rejected. A scalar result bound such as `LocalDate.getMonthValue()` does not depend on a retained receiver-state fact, so it can be used on a shared date. These are different kinds of contracts.

Postconditions and state transitions describe **normal return**. They neither promise successful completion nor describe exceptional-state transitions. Explicit argument refinements select a sufficient non-throwing domain for that particular check; a caller must prove them even if it intended to catch the corresponding exception. Other failures, such as allocation failure, invalid receiver state or provider exceptions, remain possible. Not every Java precondition is expressed.

## Decisions from comparing documentation with code

| Tempting contract | Decision and evidence |
| --- | --- |
| `Math.abs(x) >= 0` | False for the minimum signed integer. Both int and long contracts preserve that result; `absExact` instead throws. [Math API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Math.html) |
| ByteArrayInputStream constructor borrows its array | False: it retains the original array. Its constructor parameter is shared, so a borrowed argument cannot escape into it. `close()` is a no-op. [Implementation](https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/io/ByteArrayInputStream.java) |
| ByteArrayInputStream `available()` and `skip()` always return nonnegative values | Omitted. The offset constructor does not validate all ranges: `new ByteArrayInputStream(new byte[1], 3, 0)` yields `available() == -2` and `skip(1) == -2`. A useful stronger contract would need a validated-construction invariant. [Implementation](https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/io/ByteArrayInputStream.java) |
| `ZipEntry.getCompressedSize() >= -1` | Omitted: `setCompressedSize(-9)` stores `-9` directly. ZIP64 sizes and compression method bounds are also left unspecified. The unsigned CRC bound is retained. [Implementation](https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/zip/ZipEntry.java) |
| `URI.getPort() <= 65535` | Omitted: `http://example.test:70000/` parses with port 70000. The URI contract only bounds the result below by the absent-port sentinel `-1`. [URI API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/URI.html) |
| Positive thread IDs and nonnegative sizes for arbitrary collections/maps | Omitted without a non-overflow invariant: `ThreadIdentifiers.next()` uses unchecked atomic long addition, while HashMap and LinkedList update int size counters without saturation. The catalog does not assume the execution or allocation limits needed to rule out wraparound. ArrayList's separately modeled, array-backed size remains available. [Thread source](https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/lang/Thread.java), [HashMap source](https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/HashMap.java), [LinkedList source](https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/java/util/LinkedList.java) |
| All numeric-looking observations are nonnegative | False for BigDecimal scale, semaphore permits, ParsePosition indices and negative-duration seconds. Only justified fields are constrained. These cases have runtime regression witnesses. |
| Immutable factory results are `@Free` | False for cached boxing and time values. Factories, collection views and fluent aliases receive no blanket freshness promise. `Objects.requireNonNull` is also omitted: it returns an existing alias. |
| Any copied array input is `@Borrowed` | Only primitive-array copies are included. A shallow reference-array copy retains element references and needs a more precise retention contract. |
| `BasicFileAttributes.size()` is universally nonnegative | Omitted: the API leaves nonregular file sizes unspecified. A conditional regular-file contract needs additional state. [API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/attribute/BasicFileAttributes.html) |

Ownership contracts distinguish retained input from copying input. `GCMParameterSpec`, `IvParameterSpec` and APDU constructors copy primitive arrays; their array getters return fresh copies. `ArrayList.add` retains its element, and `get`/`remove` do not promise fresh elements. Size contracts do not establish which inserted element a later retrieval aliases. Callback-bearing operations, higher-order streams, arbitrary mutable graphs and concurrent protocols still require more precise effect/alias summaries.

## Conservative behavior and precision limits

| Behavior | Conservative policy |
| --- | --- |
| Callbacks, listeners, reflection and service providers | May retain arguments and captures, invoke arbitrary code and mutate reachable state. No purity or non-retention inference from method names. |
| Collections, streams, views and cached factory results | Returned objects may alias existing objects; no freshness, element identity or independent mutable state is assumed. |
| Threads, atomics, locks and asynchronous completion | No persistent relation between observations or operations; stronger selected scalar bounds describe only an individual result. |
| Files, sockets, processes, GUI and native resources | No environment, lifecycle, thread-affinity or successful-completion promise without a selected contract. |
| Constructors without stronger contracts | Arguments may be retained, and the receiver may be published. Fresh allocation does not establish exclusive ownership after construction. |
| Exceptional completion | Existing conservative exception/effect handling applies. Stronger postconditions describe normal return only. |
| Fields and constants | No additional value or ownership refinement; ordinary Java typing and compile-time constant reasoning still apply. |

For example, passing a local list to an unmodeled library operation invalidates refinements through all its local aliases. A view returned by `Collections.unmodifiableList` is shared, and an unmodeled `AtomicInteger.get()` cannot establish that its result is zero. These policies can reject safe programs; greater precision requires an audited contract or an extension of the effect/alias language.

## Effect and ownership review

Every supplied declaration was reconsidered with the [external annotation syntax](external-refinements.md). The generated [package table](jdk21-effects.md) includes all 178 packages, including those whose operations still require conservative handling. `effects.json` is derived from the actual annotations and the evidence in `catalog.json`; generation fails if a declaration has neither a footprint review nor a reason for retaining unknown effects.

| Family | Supplied summary | Boundary |
| --- | --- | --- |
| Arithmetic, primitive wrappers, `Objects` guards, enum ordinals | `@Modifies({})` for selected operations | `Math.random` and `StrictMath.random` mutate a shared generator and retain unknown effects. |
| Primitive `Arrays` overloads | Copies, equality, hashing, comparison, mismatch and binary search have empty footprints; fill and sequential sort write `array[*]`. Copies return `@Free` arrays. | Range copies may pad beyond the source end. Sortedness and exact copied/filled contents are not established. Object/comparator/callback overloads remain conservative. |
| `String` | Selected observers and character-array construction/copies have empty footprints; `getChars` writes `destination[*]` and borrows its destination. | Charset/provider paths, returned String aliases and lazy hash caches are not assigned unsupported purity/freshness guarantees. |
| `ArrayList` | Existing receiver transitions have explicit effects; `subList` uses `@ReturnsView`. | Only the reviewed ArrayList view implementation has precise view-operation handling. General `List.subList`, reversed/wrapper views and iterators remain conservative. |
| In-memory streams and readers | Constructors preserve existing storage; cursor/buffer changes name `this.*`; bulk reads also name `destination[*]`; output-stream copies are `@Free`. | `ByteArrayInputStream` still retains its input. An empty constructor footprint does not imply borrowing or copying. I/O providers and external resources retain unknown effects. |
| Checksums | Fresh constructors and `getValue` have empty footprints; scalar/byte-array updates and reset write `this.*`, preserving borrowed input bytes. | ByteBuffer overloads and their memory sessions remain conservative. |
| Numeric, temporal and metadata observers | Direct observers have empty footprints; BigInteger bit observers name receiver cache writes. | `BigDecimal.precision` can grow a global powers-of-ten cache; `signum` can dispatch through a stored BigInteger. Their effects remain unknown. |
| Crypto parameters and APDU values | Reviewed defensive constructors and copied-byte results preserve inputs, with `@Borrowed` inputs and `@Free` copies. | No promise about cryptographic providers or device operations. |
| Concurrent counters, GUI/XML/JDBC/provider APIs | Selected concrete snapshot reads have empty footprints; abstract/provider observations retain unknown effects. | Empty footprints do not prevent interference or equate successive results. `Color` channel getters call overridable `getRGB`; a channel name does not prove purity. |

For example, the catalog now contains:

```java
@Modifies({})
@Free
@Refinement("_ != null && length(_) == to - from")
byte[] copyOfRange(@Borrowed @Refinement("_ != null") byte[] array,
    @Refinement("_ >= 0 && _ <= length(array)") int from,
    @Refinement("_ >= from") int to);

@Modifies({"this.*", "destination[*]"})
@Refinement("_ >= -1 && _ <= count")
int read(@Borrowed @Refinement("_ != null") byte[] destination,
    @Refinement("_ >= 0 && count >= 0 && _ + count <= length(destination)") int offset,
    int count);
```

These summaries preserve unrelated local facts on normal and exceptional exits, while invalidating written storage through its local aliases. Constructor footprints are applied by Latte as well as LiquidTypeChecking. Ownership remains independent: a retained argument still escapes even if constructing the receiver writes no pre-existing storage. Checked examples are in [EffectAndOwnershipContracts.java](../examples/jdk21/EffectAndOwnershipContracts.java). The reduced [copy-then-write case](../examples/jdk21-audit/ConservativeArrayCopy.java) from the upstream audit now verifies; historical audit counts have not been recomputed.

## Java 21 migration review

The build uses `--release 21`, Spoon defaults to compliance level 21, and both CI configurations select Temurin 21. Verification targets stable Java 21 syntax; accepting the Java 21 baseline does not add semantic support for every syntax feature or enable previews.

The [package-by-package coverage table](jdk21-coverage.md) supplies counts and a precision boundary for every one of the 178 packages. [migration-from17.json](../specifications/jdk21/migration-from17.json) records **56 added types, 2 removed types, 796 added declarations, 21 removed declarations and 80 changed declarations** against the saved Java 17 baseline. These are declaration changes, so a removed declaration may remain exposed through inheritance. Inherited copies and the new ordinal refinements are excluded from the declaration delta.

Review of carried contracts found equivalent implementation changes in floor modulus, floating-point classification, random generation, calendar switches and checksum range checks. The String character-array copy, ByteArrayInputStream retention and ByteArrayOutputStream copy paths retain their original ownership behavior. ByteBuffer checksum overloads now manage memory sessions; those overloads remain conservative. Constructor/parameter spelling changes do not affect erased signatures. Boundary witnesses and the original counterexamples run on the pinned Java 21 runtime.

`java.lang.foreign` receives conservative coverage for segment aliases, arena lifetime, alignment, native access and thread confinement. Sequenced collection views, virtual-thread builders and callbacks, scoped values, structured task scopes, resolver providers and preview string-template processors receive no unsupported ownership, alias, lifetime or effect promise. Integer clamp and exact floor/ceiling division have stronger contracts; floating clamp's signed-zero/NaN details remain unmodeled.

## Validation and regeneration

```sh
cd latte
mvn clean verify -Pcompatibility
```

The suite runs on Java 21. It independently enumerates the runtime's unqualified Java SE module exports and public/protected nested types, compares that complete surface to the ledger, and checks constructors, overloads, fields and inherited methods by reflection. It validates all stronger signatures and predicates, compiles declarations with `--release 21`, checks resource/manifest counts, verifies AST policies and exact accepted/rejected example diagnostics, and checks that selections remain model-local. Runtime witnesses test shipped annotation predicates against actual JDK results, including overflow, NaN/infinity, signed zero, sentinels, defensive copies and counterexamples to tempting bounds. Finite witnesses supplement source review; they are not proofs. Tests need no network, database server, display or smart-card device.

The separate [OpenJDK test-source audit](jdk21-upstream-audit.md) compares unchanged upstream tests with and without the catalog. It records introduced diagnostics, inconclusive analyses and reduced reproductions. It is not part of the normal CI suite and does not equate a passing source analysis with proving upstream assertions.

[inventory.json](../specifications/jdk21/inventory.json) records the Java runtime version, official index hash and source-archive hash. The compressed `baseline-java17.json.gz` preserves the prior ledger so migration evidence can be regenerated offline. Each per-package `api/*.json` records source paths and hashes, documentation links, declaring types, erased source signatures, modifiers, varargs status, policy and reason. The API index omits some visible nested classes, including Swing inner classes, so generation recursively includes public/protected member classes. Non-static inner constructors omit the JVM's synthetic enclosing-instance parameter. Members exposed by non-API ancestors are recorded explicitly; ordinary inheritance is represented by parent links. Constructors and static interface methods are not inherited.

To regenerate, use the pinned OpenJDK **21.0.13.1+2** runtime, its `lib/src.zip`, and a saved copy of the [official Java 21 class index](https://docs.oracle.com/en/java/javase/21/docs/api/allclasses-index.html):

```sh
# From the Maven module, with JAVA_HOME and PATH pointing to the pinned JDK:
mvn test-compile exec:java \
  -Dexec.mainClass=latte.jdk.Jdk21Inventory \
  -Dexec.classpathScope=test \
  "-Dexec.args=../specifications/jdk21 /path/to/allclasses-index.html $JAVA_HOME/lib/src.zip"
```

Generation supplies or updates enum ordinal contracts, refreshes source provenance, and writes `inventory.json`, `api-types.txt`, all per-package ledgers, `packages.json`, `effects.json`, both coverage/effect tables and `migration-from17.json`. The baseline archive is an input, not regenerated from the current JDK. The generator reads the stronger Java specifications to assign refined policies, so a new declaration cannot silently remain classified as conservative. Hashes distinguish regenerated inputs; generated output has no timestamp.

To strengthen a member, read its Java 21 Javadoc and every relevant construction/mutation path in the pinned source. Check exceptional return, overflow, sentinels, overrides, caching, retention and callbacks. Add the smallest justified contract, its evidence to `catalog.json`, a runtime boundary witness when executable locally, and an application verification regression. Regenerate the ledger and rerun verification. Re-audit affected contracts when changing the baseline or implementation assumptions.
