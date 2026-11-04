package latte.analysis;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Persistent abstract store: transfer functions return new states and never mutate old snapshots.
 */
public record FlowState(
    Map<String, Reference> variables,
    Map<String, Permission> permissions,
    Map<Field, Reference> heap,
    Map<String, Permission> fieldPermissions) {
  public record Field(String owner, String name) {}

  public FlowState {
    variables = Map.copyOf(variables);
    permissions = Map.copyOf(permissions);
    heap = Map.copyOf(heap);
    fieldPermissions = Map.copyOf(fieldPermissions);
  }

  public static FlowState empty() {
    return new FlowState(Map.of(), Map.of(), Map.of(), Map.of());
  }

  public FlowState bind(String variable, Reference value) {
    return new FlowState(with(variables, variable, value), permissions, heap, fieldPermissions);
  }

  public FlowState withVariables(Map<String, Reference> bindings) {
    return new FlowState(bindings, permissions, heap, fieldPermissions);
  }

  /** Forget unreachable ownership edges without asserting that a Java field contains null. */
  public FlowState withoutFields(Set<Field> fields) {
    var remaining = new HashMap<>(heap);
    fields.forEach(remaining::remove);
    return new FlowState(variables, permissions, remaining, fieldPermissions);
  }

  public record Move(FlowState state, Reference value, Map<String, String> origins) {
    public Move {
      origins = Map.copyOf(origins);
    }
  }

  /** Copy the transferred graph into the callee's ownership, retaining consumed caller aliases. */
  public Move moveFrom(FlowState source, Reference value, String site) {
    var names =
        source.reachable(value).stream()
            .collect(Collectors.toMap(id -> id, id -> "owned:" + site + ":" + id));
    java.util.function.Function<Reference, Reference> renamed =
        ref ->
            new Reference(
                ref.objects().stream()
                    .map(id -> names.getOrDefault(id, id))
                    .collect(Collectors.toSet()),
                ref.scalar(),
                ref.nullable());
    var state = this;
    for (var entry : names.entrySet())
      state =
          state.fresh(
              Reference.object(entry.getValue()),
              source.permissions.getOrDefault(entry.getKey(), Permission.SHARED));
    var copied = new HashMap<>(state.heap);
    source.heap.forEach(
        (field, contents) -> {
          if (names.containsKey(field.owner()))
            copied.put(new Field(names.get(field.owner()), field.name()), renamed.apply(contents));
        });
    state = new FlowState(state.variables, state.permissions, copied, fieldPermissions);
    var origins =
        names.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    return new Move(state, renamed.apply(value), origins);
  }

  public FlowState allocate(Reference value, Permission permission) {
    var updated = new HashMap<>(permissions);
    value.objects().forEach(id -> updated.put(id, permission));
    return new FlowState(variables, updated, heap, fieldPermissions);
  }

  /** Separate the most recent allocation from all older instances at this allocation site. */
  public FlowState fresh(Reference value, Permission permission) {
    var state = this;
    for (String recent : value.objects()) {
      if (!state.permissions().containsKey(recent)) continue;
      String summary = "summary:" + recent;
      java.util.function.Function<String, String> rename =
          id ->
              id.equals(recent) || id.startsWith(recent + ".")
                  ? summary + id.substring(recent.length())
                  : id;
      java.util.function.Function<Reference, Reference> references =
          ref ->
              new Reference(
                  ref.objects().stream().map(rename).collect(Collectors.toSet()),
                  ref.scalar(),
                  ref.nullable());
      var variables = new HashMap<String, Reference>();
      state.variables().forEach((key, ref) -> variables.put(key, references.apply(ref)));
      var permissions = new HashMap<String, Permission>();
      state
          .permissions()
          .forEach(
              (id, p) ->
                  permissions.merge(
                      rename.apply(id),
                      rename.apply(id).equals(id) ? p : Permission.SHARED,
                      Permission::join));
      var heap = new HashMap<Field, Reference>();
      state
          .heap()
          .forEach(
              (field, ref) ->
                  heap.merge(
                      new Field(rename.apply(field.owner()), field.name()),
                      references.apply(ref),
                      Reference::join));
      // When older arrays merge, a slot absent from one instance still contributes its default.
      // Otherwise an explicit non-null slot could incorrectly hide another instance's null slot.
      for (var field : state.heap.keySet()) {
        if (!ArraySlots.isSlot(field.name()) || field.name().equals(ArraySlots.ANY)) continue;
        var merged = new Field(rename.apply(field.owner()), field.name());
        for (var entry : state.heap.entrySet()) {
          var defaults = entry.getKey();
          if (defaults.name().equals(ArraySlots.ANY)
              && rename.apply(defaults.owner()).equals(merged.owner())
              && !state.heap.containsKey(new Field(defaults.owner(), field.name())))
            heap.merge(merged, references.apply(entry.getValue()), Reference::join);
        }
      }
      state = new FlowState(variables, permissions, heap, fieldPermissions);
    }
    return state.allocate(value, permission);
  }

  public FlowState write(Reference receiver, String field, Reference value) {
    if (ArraySlots.isSlot(field)) return writeArray(receiver, field, value);
    var updated = new HashMap<>(heap);
    receiver
        .objects()
        .forEach(
            id -> {
              var key = new Field(id, field);
              updated.put(
                  key,
                  receiver.objects().size() == 1 && !id.startsWith("summary:")
                      ? value
                      : updated.getOrDefault(key, value).join(value));
            });
    return new FlowState(variables, permissions, updated, fieldPermissions);
  }

  public Optional<Reference> read(Reference receiver, String field) {
    return receiver.objects().stream()
        .map(id -> read(id, field))
        .filter(Objects::nonNull)
        .reduce(Reference::join);
  }

  private Reference read(String owner, String field) {
    if (field.equals(ArraySlots.ANY))
      return heap.entrySet().stream()
          .filter(e -> e.getKey().owner().equals(owner) && ArraySlots.isSlot(e.getKey().name()))
          .map(Map.Entry::getValue)
          .reduce(Reference::join)
          .orElse(null);
    var exact = heap.get(new Field(owner, field));
    return exact != null || !ArraySlots.isSlot(field)
        ? exact
        : heap.get(new Field(owner, ArraySlots.ANY));
  }

  /** The summary is the default for slots without an explicit entry, not a union of all slots. */
  public FlowState initializeArray(Reference receiver, Reference defaults) {
    var updated = new HashMap<>(heap);
    receiver.objects().forEach(id -> updated.put(new Field(id, ArraySlots.ANY), defaults));
    return new FlowState(variables, permissions, updated, fieldPermissions);
  }

  /** Arrays received from outside this scope have unknown, shared elements. */
  public FlowState ensureArray(Reference receiver) {
    var state = this;
    for (String owner : receiver.objects()) {
      if (state.heap.containsKey(new Field(owner, ArraySlots.ANY))) continue;
      var unknown = Reference.arrayElements(owner);
      state =
          state
              .allocate(unknown, Permission.SHARED)
              .initializeArray(Reference.object(owner), unknown);
    }
    return state;
  }

  private FlowState writeArray(Reference receiver, String slot, Reference value) {
    var state = ensureArray(receiver);
    var updated = new HashMap<>(state.heap);
    for (String owner : receiver.objects()) {
      if (slot.equals(ArraySlots.ANY)) {
        // A symbolic index can name any slot. Preserve every old candidate, including defaults.
        updated.replaceAll(
            (field, old) ->
                field.owner().equals(owner) && ArraySlots.isSlot(field.name())
                    ? old.join(value)
                    : old);
      } else {
        var key = new Field(owner, slot);
        boolean definite = receiver.objects().size() == 1 && !owner.startsWith("summary:");
        updated.put(key, definite ? value : state.read(owner, slot).join(value));
      }
    }
    return new FlowState(variables, state.permissions, updated, fieldPermissions);
  }

  public Permission permission(Reference value) {
    if (value.scalar()) return Permission.IMMUTABLE;
    return value.objects().stream()
        .map(id -> permissions.getOrDefault(id, Permission.SHARED))
        .reduce(Permission::join)
        .orElse(Permission.FREE);
  }

  public Permission transferablePermission(Reference value) {
    var permission = permission(value);
    return permission == Permission.UNIQUE
            && heap.values().stream()
                .noneMatch(r -> !Collections.disjoint(r.objects(), value.objects()))
        ? Permission.FREE
        : permission;
  }

  public Set<String> reachable(Reference value) {
    Set<String> reached = new HashSet<>(value.objects());
    var pending = new ArrayDeque<>(reached);
    while (!pending.isEmpty()) {
      String id = pending.removeFirst();
      heap.entrySet().stream()
          .filter(e -> e.getKey().owner().equals(id))
          .flatMap(e -> e.getValue().objects().stream())
          .forEach(
              child -> {
                if (reached.add(child)) pending.addLast(child);
              });
    }
    return Set.copyOf(reached);
  }

  public boolean overlaps(Reference first, Reference second) {
    return !Collections.disjoint(reachable(first), reachable(second));
  }

  public FlowState changeReachable(Reference value, Permission permission) {
    var updated = new HashMap<>(permissions);
    reachable(value)
        .forEach(
            id -> {
              if (updated.get(id) != Permission.IMMUTABLE) updated.put(id, permission);
            });
    return new FlowState(variables, updated, heap, fieldPermissions);
  }

  public FlowState withFieldPermissions(Map<String, Permission> declared) {
    return new FlowState(variables, permissions, heap, declared);
  }

  private Set<String> liveObjects() {
    var roots = variables.values().stream().reduce(Reference.nullValue(), Reference::join);
    return reachable(roots);
  }

  public FlowState join(FlowState other) {
    var joinedPermissions = new HashMap<>(merge(permissions, other.permissions, Permission::join));
    var joinedHeap = new HashMap<Field, Reference>();
    var keys = new HashSet<>(heap.keySet());
    keys.addAll(other.heap.keySet());
    for (var field : keys) {
      var left = heap.get(field);
      var right = other.heap.get(field);
      if (left == null && !liveObjects().contains(field.owner())) {
        joinedHeap.put(field, right);
        continue;
      }
      if (right == null && !other.liveObjects().contains(field.owner())) {
        joinedHeap.put(field, left);
        continue;
      }
      if (ArraySlots.isSlot(field.name()) && !field.name().equals(ArraySlots.ANY)) {
        if (left == null) left = heap.get(new Field(field.owner(), ArraySlots.ANY));
        if (right == null) right = other.heap.get(new Field(field.owner(), ArraySlots.ANY));
      }
      if (left == null || right == null) {
        var unknown =
            field.name().equals(ArraySlots.ANY)
                ? Reference.arrayElements(field.owner())
                : Reference.object(field.owner() + "." + field.name());
        var permission =
            joinedPermissions.get(field.owner()) == Permission.SHARED
                ? Permission.SHARED
                : fieldPermissions.getOrDefault(field.name(), Permission.SHARED);
        unknown.objects().forEach(id -> joinedPermissions.merge(id, permission, Permission::join));
        if (left == null) left = unknown;
        if (right == null) right = unknown;
      }
      joinedHeap.put(field, left.join(right));
    }
    return new FlowState(
        merge(variables, other.variables, Reference::join),
        joinedPermissions,
        joinedHeap,
        fieldPermissions);
  }

  /** Forget field contents after a call, retaining all possible existing aliases. */
  public FlowState havocHeap(Reference roots, String site) {
    var reachable = reachable(roots);
    var updatedHeap = new HashMap<>(heap);
    var updatedPermissions = new HashMap<>(permissions);
    for (var entry : heap.entrySet()) {
      var field = entry.getKey();
      if (!reachable.contains(field.owner())) continue;
      // A call can mutate a captured object, but cannot rebind a Java closure's captures.
      if (field.name().startsWith(ClosureFacts.CAPTURE)) continue;
      var declared = fieldPermissions.getOrDefault(field.name(), Permission.SHARED);
      if (permissions.get(field.owner()) == Permission.SHARED) declared = Permission.SHARED;
      var permission = declared;
      var candidates =
          reachable.stream()
              .filter(
                  id ->
                      permission == Permission.SHARED
                          || permissions.get(id) == Permission.UNIQUE
                          || permissions.get(id) == Permission.FREE)
              .collect(Collectors.toSet());
      var unknown = "heap:" + site + ":" + field.owner() + ":" + field.name();
      if (field.name().equals(ArraySlots.ANY))
        unknown = Reference.arrayElements(unknown).objects().iterator().next();
      candidates.add(unknown);
      updatedPermissions.put(unknown, permission);
      updatedHeap.put(field, entry.getValue().join(new Reference(candidates, false)));
    }
    return new FlowState(variables, updatedPermissions, updatedHeap, fieldPermissions);
  }

  public Set<String> aliases(Reference value) {
    return variables.entrySet().stream()
        .filter(e -> !Collections.disjoint(e.getValue().objects(), value.objects()))
        .map(Map.Entry::getKey)
        .collect(Collectors.toUnmodifiableSet());
  }

  private static <K, V> Map<K, V> with(Map<K, V> source, K key, V value) {
    var result = new HashMap<>(source);
    result.put(key, value);
    return result;
  }

  private static <K, V> Map<K, V> merge(
      Map<K, V> a, Map<K, V> b, java.util.function.BinaryOperator<V> join) {
    return Stream.concat(a.entrySet().stream(), b.entrySet().stream())
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, join));
  }
}
