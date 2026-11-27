package latte.jdk;

/** Explicit precision boundaries for every package, grouped where the same effects apply. */
final class Jdk21PackagePolicies {
  private Jdk21PackagePolicies() {}

  static String note(String module, String name) {
    return switch (module) {
      case "java.base" -> base(name);
      case "java.desktop", "java.datatransfer" ->
          "GUI and media objects, listeners and views may be shared; event-thread, native-resource, rendering and callback protocols are not inferred.";
      case "java.compiler" ->
          "Compiler models and tool-provider results may alias compiler-owned graphs; processing rounds, callbacks and provider effects remain unknown.";
      case "java.instrument" ->
          "Agents can transform classes and retain callbacks; no purity, class-layout stability or redefinition guarantee is supplied.";
      case "java.logging" ->
          "Loggers, handlers and configuration may be global; handlers, filters and formatters can retain arguments and invoke callbacks.";
      case "java.management", "java.management.rmi" ->
          "Management observations are snapshots; MBeans, notifications, remote endpoints and scheduled callbacks can change or retain shared state.";
      case "java.naming" ->
          "Names, directory entries, lookup results and providers may share mutable state; network effects, retained values and callbacks are unknown.";
      case "java.net.http" ->
          "HTTP requests, responses, body publishers and subscribers may retain shared data; asynchronous execution, cancellation and callbacks are unmodeled.";
      case "java.prefs" ->
          "Preference stores and listener registrations can outlive local scope; persistence, external updates and callback timing are unknown.";
      case "java.rmi" ->
          "Remote calls, registries and distributed garbage collection may invoke arbitrary remote code; identity, lifetime and failure protocols are unknown.";
      case "java.scripting" ->
          "Script engines, bindings and evaluated code may retain or mutate shared objects and call back into Java.";
      case "java.security.jgss", "java.security.sasl", "java.xml.crypto" ->
          "Authentication, signing and provider protocols are not proved; credentials, callbacks, keys and message objects may be retained or shared.";
      case "java.smartcardio" ->
          "Selected APDU contracts describe copied bytes and scalar fields; device communication, channel lifetimes and card protocols remain unknown.";
      case "java.sql", "java.sql.rowset", "java.transaction.xa" ->
          "Selected scalar contracts are provider obligations; cursor, transaction, connection and exceptional-state protocols and retained SQL values remain unknown.";
      case "java.xml" ->
          "XML nodes, live views, parser inputs, resolvers and handlers may share mutable state; parser-event, callback and external-entity protocols are unmodeled.";
      default ->
          throw new IllegalArgumentException("Missing package policy for " + module + "/" + name);
    };
  }

  private static String base(String name) {
    return switch (name) {
      case "java.lang" ->
          "Selected arithmetic, String and enum contracts apply; thread-ID counter overflow, virtual-thread publication, scoped values, string-template processors and reflection effects otherwise stay conservative.";
      case "java.lang.foreign" ->
          "Java 21 preview API: segment aliases, arena lifetime, native access, alignment, confinement and downcall/upcall effects are unmodeled; no memory-safety or ownership guarantee is asserted.";
      case "java.lang.annotation" ->
          "Enum ordinal bounds apply; annotation proxies and metadata results carry no additional identity or freshness promise.";
      case "java.lang.constant",
              "java.lang.invoke",
              "java.lang.module",
              "java.lang.reflect",
              "java.lang.runtime" ->
          "Descriptors and metadata can be interned or shared; method handles, reflection and runtime bootstraps may invoke arbitrary code or retain inputs.";
      case "java.lang.ref" ->
          "Garbage collection, cleaners and reference queues depend on external reachability; clearing, resurrection, callback timing and lifetimes are unknown.";
      case "java.io",
              "java.net",
              "java.net.spi",
              "java.nio",
              "java.nio.channels",
              "java.nio.channels.spi",
              "java.nio.file",
              "java.nio.file.attribute",
              "java.nio.file.spi",
              "javax.net" ->
          "Only selected scalar/copy contracts apply; backing storage, views, providers, file/network effects and resource lifetimes may escape local scope.";
      case "java.nio.charset", "java.nio.charset.spi" ->
          "Charsets and coders may retain state and use provider code; encoding contents, error transitions and buffer aliases remain unmodeled.";
      case "java.math" ->
          "Selected scalar bounds apply; BigInteger cache writes are receiver-local, while BigDecimal precision may grow global caches. Immutable values may be shared; scale can be negative and arbitrary-precision contents are not modeled.";
      case "java.text", "java.text.spi" ->
          "Locale data, formatting/parsing state, providers and iterators may be shared; text contents, parse success and index validity are not inferred.";
      case "java.time",
              "java.time.chrono",
              "java.time.format",
              "java.time.temporal",
              "java.time.zone" ->
          "Selected calendar/normalized-field and enum bounds apply; factories may cache values and no universal chronology, parsing or time-zone relation is imposed.";
      case "java.util" ->
          "Primitive array reads/copies and slot writes have explicit effects; ArrayList subList captures its backing and shares size updates. Other views, elements, shallow copies and callback effects remain conservative.";
      case "java.util.concurrent", "java.util.concurrent.atomic", "java.util.concurrent.locks" ->
          "Observations are snapshots; concurrent interference, atomic/lock relations, task scopes, asynchronous completion and retained callbacks have no stable-state guarantee.";
      case "java.util.function", "java.util.stream" ->
          "Callbacks and lazy pipelines may retain captures and invoke arbitrary code; invocation count, timing, order and returned aliases are unknown.";
      case "java.util.jar", "java.util.zip" ->
          "Selected sizes/checksums and copying borrows apply; compressed sizes can be signed, backing buffers may alias, and archive/signing/resource protocols remain unknown.";
      case "java.util.random" ->
          "Selected half-open ranges apply; generator state may mutate and no seed relation, distribution theorem or stream ownership is asserted.";
      case "java.util.regex" ->
          "Selected group/sentinel bounds apply on normal return; match success, retained character sequences and matcher lifecycle are not modeled.";
      case "java.util.spi" ->
          "Service providers and locale objects may be shared and invoke or retain callbacks; no provider purity or result freshness is assumed.";
      case "java.security",
              "java.security.cert",
              "java.security.interfaces",
              "java.security.spec",
              "javax.crypto",
              "javax.crypto.interfaces",
              "javax.crypto.spec",
              "javax.net.ssl",
              "javax.security.auth",
              "javax.security.auth.callback",
              "javax.security.auth.login",
              "javax.security.auth.spi",
              "javax.security.auth.x500",
              "javax.security.cert" ->
          "Selected defensive-copy/scalar contracts apply; algorithm/provider requirements, credential/key ownership, authentication/TLS protocols and callbacks remain unknown.";
      default -> throw new IllegalArgumentException("Missing java.base package policy for " + name);
    };
  }
}
