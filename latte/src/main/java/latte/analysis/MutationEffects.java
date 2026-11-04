package latte.analysis;

import java.util.*;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.visitor.filter.TypeFilter;

/** Executable effects computed to a fixed point and published on Spoon declarations. */
public final class MutationEffects {
  public static final String KEY = "latte.analysis.effects.v1";

  public record Effect(Map<Integer, Set<String>> roots, boolean globals) {
    public Effect {
      var copy = new HashMap<Integer, Set<String>>();
      roots.forEach((root, fields) -> copy.put(root, Set.copyOf(fields)));
      roots = Map.copyOf(copy);
    }

    public boolean mutates() {
      return globals || !roots.isEmpty();
    }

    public boolean receiver() {
      return roots.containsKey(-1);
    }

    public boolean parameter(int index) {
      return roots.containsKey(index);
    }

    Effect add(int root, String field) {
      var next = new HashMap<>(roots);
      var fields = new HashSet<>(roots.getOrDefault(root, Set.of()));
      fields.add(field);
      next.put(root, fields);
      return new Effect(next, globals);
    }

    Effect join(Effect other) {
      var next = new Effect(roots, globals || other.globals);
      for (var entry : other.roots.entrySet())
        for (String field : entry.getValue()) next = next.add(entry.getKey(), field);
      return next;
    }
  }

  private MutationEffects() {}

  public static Effect on(CtExecutable<?> method) {
    if (method != null && method.getMetadata(KEY) instanceof Effect effect) return effect;
    var roots = new HashMap<Integer, Set<String>>();
    roots.put(-1, Set.of("*"));
    if (method != null)
      for (int i = 0; i < method.getParameters().size(); i++)
        if (!method.getParameters().get(i).getType().isPrimitive()) roots.put(i, Set.of("*"));
    return new Effect(roots, true);
  }

  static void collect(CtModel model, ReturnAliases aliases) {
    var methods =
        model.getElements(new TypeFilter<>(CtExecutable.class)).stream()
            .filter(e -> e instanceof CtMethod<?> || e instanceof CtConstructor<?>)
            .toList();
    Map<CtExecutable<?>, Effect> effects = new HashMap<>();
    for (CtExecutable<?> method : methods) {
      var effect = method.getBody() == null ? on(method) : new Effect(Map.of(), false);
      if (Annotations.named(method, "StateRefinement").stream()
          .anyMatch(
              a ->
                  Annotations.strings(a.getValues().get("to")).stream()
                      .anyMatch(text -> !text.isBlank()))) effect = effect.add(-1, "@state");
      effects.put(method, effect);
    }
    for (; ; ) {
      var next = new HashMap<>(effects);
      for (CtExecutable<?> method : methods) {
        if (method.getBody() == null) continue;
        var effect = effects.get(method);
        if (method instanceof CtConstructor<?> constructor)
          for (var field : constructor.getDeclaringType().getFields())
            if (!field.isStatic() && field.getDefaultExpression() != null)
              effect = effect.add(ReturnAliases.RECEIVER, field.getReference().getQualifiedName());
        for (var write : elements(method, CtFieldWrite.class)) {
          effect =
              write.getVariable().isStatic()
                  ? new Effect(effect.roots(), true)
                  : add(
                      effect,
                      aliases.origins(write.getTarget(), method),
                      write.getVariable().getQualifiedName());
        }
        for (var write : elements(method, CtArrayWrite.class))
          effect = add(effect, aliases.origins(write.getTarget(), method), "[]");
        for (CtInvocation<?> call : elements(method, CtInvocation.class)) {
          var declaration = call.getExecutable().getDeclaration();
          if (call.getExecutable().isConstructor()
              && call.getExecutable()
                  .getDeclaringType()
                  .getQualifiedName()
                  .equals("java.lang.Object")) continue;
          var callee =
              declaration == null ? on(null) : effects.getOrDefault(declaration, on(declaration));
          if (callee.globals()) effect = new Effect(effect.roots(), true);
          for (var root : callee.roots().entrySet()) {
            var actual =
                root.getKey() == -1
                    ? call.getTarget()
                    : root.getKey() < call.getArguments().size()
                        ? call.getArguments().get(root.getKey())
                        : null;
            for (String field : root.getValue())
              effect = add(effect, aliases.origins(actual, method), field);
          }
          if (declaration == null)
            for (var argument : call.getArguments())
              effect = add(effect, aliases.origins(argument, method), "*");
        }
        for (CtConstructorCall<?> call : elements(method, CtConstructorCall.class)) {
          var declaration = call.getExecutable().getDeclaration();
          if (call.getType() != null
              && call.getType().getQualifiedName().equals("java.lang.Object")) continue;
          var callee =
              declaration == null ? on(null) : effects.getOrDefault(declaration, on(declaration));
          if (callee.globals()) effect = new Effect(effect.roots(), true);
          for (int i = 0; i < call.getArguments().size(); i++) {
            var origin = aliases.origins(call.getArguments().get(i), method);
            for (String field : callee.roots().getOrDefault(i, Set.of()))
              effect = add(effect, origin, field);
            if (callee.globals()) effect = add(effect, origin, "*");
          }
        }
        // A virtual call must account for every source override, including inherited
        // implementations.
        if (method instanceof CtMethod<?> base
            && !base.isFinal()
            && !base.isStatic()
            && !base.isPrivate())
          for (CtExecutable<?> candidate : methods)
            if (candidate instanceof CtMethod<?> override
                && override != base
                && override.isOverriding(base)) effect = effect.join(effects.get(override));
        next.put(method, effect);
      }
      if (next.equals(effects)) {
        next.forEach((method, effect) -> method.putMetadata(KEY, effect));
        return;
      }
      effects = next;
    }
  }

  /** Include instance initialization in constructors, but never execute nested deferred bodies. */
  private static <T extends CtElement> List<T> elements(CtExecutable<?> owner, Class<T> kind) {
    var roots = new ArrayList<CtElement>();
    roots.add(owner.getBody());
    if (owner instanceof CtConstructor<?> constructor)
      for (var member : constructor.getDeclaringType().getTypeMembers()) {
        if (member instanceof CtField<?> field
            && !field.isStatic()
            && field.getDefaultExpression() != null) roots.add(field.getDefaultExpression());
        else if (member instanceof CtAnonymousExecutable initializer && !initializer.isStatic())
          roots.add(initializer.getBody());
      }
    return roots.stream()
        .flatMap(
            root ->
                root.getElements(new TypeFilter<>(kind)).stream()
                    .filter(
                        node ->
                            node.getParent(CtExecutable.class) == root.getParent(CtExecutable.class)
                                && node.getParent(CtType.class) == root.getParent(CtType.class)))
        .toList();
  }

  private static Effect add(Effect effect, Set<ReturnAliases.Path> paths, String field) {
    for (var path : paths) {
      if (path.root() == ReturnAliases.EXTERNAL) effect = new Effect(effect.roots(), true);
      else if (path.root() >= ReturnAliases.RECEIVER)
        effect = effect.add(path.root(), path.fields().isEmpty() ? field : "*");
    }
    return effect;
  }
}
