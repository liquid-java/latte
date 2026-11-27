package latte.analysis;

import java.util.*;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

/** Validated external summaries stored on Spoon declarations alongside their bindings. */
public final class ExternalSummaries {
  public static final String EFFECTS = "latte.external.effects.v1";
  public static final String VIEW = "latte.external.view.v1";

  public record View(int backing, int from, int toExclusive, boolean arrayListOperations) {}

  private ExternalSummaries() {}

  public static Optional<View> view(CtExecutable<?> method) {
    return method != null && method.getMetadata(VIEW) instanceof View view
        ? Optional.of(view)
        : Optional.empty();
  }

  public static Optional<MutationEffects.Effect> effects(CtExecutable<?> method) {
    return method != null && method.getMetadata(EFFECTS) instanceof MutationEffects.Effect effect
        ? Optional.of(effect)
        : Optional.empty();
  }

  public static void validate(CtMethod<?> contract, CtExecutable<?> actual) {
    if (Annotations.has(contract, "Modifies")) {
      var roots = new HashMap<Integer, Set<String>>();
      if (Annotations.named(contract, "Modifies").size() != 1)
        throw new IllegalArgumentException("Modifies must occur once");
      for (String text :
          literalStrings(
              Annotations.named(contract, "Modifies").getFirst().getValues().get("value"))) {
        String path = text.strip();
        boolean elements = path.endsWith("[*]");
        boolean subtree = path.endsWith(".*");
        String root =
            elements || subtree ? path.substring(0, path.length() - (elements ? 3 : 2)) : path;
        int index = root(contract, actual, root);
        var type = type(contract, actual, index);
        if (type == null || type.isPrimitive())
          throw new IllegalArgumentException("Modifies requires a reference root: " + text);
        if (elements && !type.isArray())
          throw new IllegalArgumentException("Modifies [*] requires an array: " + text);
        roots.merge(
            index,
            Set.of(elements ? "[]" : "*"),
            (a, b) -> {
              var joined = new HashSet<>(a);
              joined.addAll(b);
              return Set.copyOf(joined);
            });
      }
      // A receiver transition may initialize a newly allocated constructor receiver.
      boolean transition =
          Annotations.strings(contract, "StateRefinement", "to").stream()
              .anyMatch(s -> !s.isBlank());
      if (transition && !(actual instanceof CtConstructor<?>) && !roots.containsKey(-1))
        throw new IllegalArgumentException(
            "Modifies must include this for a receiver state transition");
      contract.putMetadata(EFFECTS, new MutationEffects.Effect(roots, false));
    }
    if (Annotations.has(contract, "ReturnsView")) {
      if (Annotations.named(contract, "ReturnsView").size() != 1)
        throw new IllegalArgumentException("ReturnsView must occur once");
      if (actual instanceof CtConstructor<?> || !isList(contract.getType()))
        throw new IllegalArgumentException("ReturnsView requires a List result on a method");
      if (List.of("Free", "Unique", "Borrowed", "Shared").stream()
          .anyMatch(a -> Annotations.has(contract, a)))
        throw new IllegalArgumentException(
            "ReturnsView derives result ownership from its backing; omit result permission"
                + " annotations");
      var effect =
          effects(contract)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException("ReturnsView requires explicit @Modifies({})"));
      if (effect.mutates())
        throw new IllegalArgumentException("ReturnsView creation must preserve existing storage");
      int backing = root(contract, actual, single(contract, "of"));
      if (!isList(type(contract, actual, backing)))
        throw new IllegalArgumentException("ReturnsView backing must be a List");
      if (backing >= 0
          && Annotations.permission(contract.getParameters().get(backing)) != Permission.BORROWED)
        throw new IllegalArgumentException("ReturnsView backing parameter must be Borrowed");
      int from = index(contract, single(contract, "from"));
      int to = index(contract, single(contract, "toExclusive"));
      for (int i : List.of(from, to))
        if (!contract.getParameters().get(i).getType().getQualifiedName().equals("int"))
          throw new IllegalArgumentException("ReturnsView range parameters must have type int");
      boolean arrayList =
          actual.getParent(CtType.class).getQualifiedName().equals("java.util.ArrayList")
              && actual.getSimpleName().equals("subList");
      contract.putMetadata(VIEW, new View(backing, from, to, arrayList));
    }
  }

  private static List<String> literalStrings(CtExpression<?> expression) {
    if (expression instanceof CtLiteral<?> literal && literal.getValue() instanceof String text)
      return List.of(text);
    if (expression instanceof CtNewArray<?> array)
      return array.getElements().stream().flatMap(e -> literalStrings(e).stream()).toList();
    if (expression instanceof CtBinaryOperator<?> binary
        && binary.getKind() == BinaryOperatorKind.PLUS) {
      var left = literalStrings(binary.getLeftHandOperand());
      var right = literalStrings(binary.getRightHandOperand());
      if (left.size() == 1 && right.size() == 1) return List.of(left.getFirst() + right.getFirst());
    }
    throw new IllegalArgumentException(
        "External effect/view summaries require literal strings or string arrays");
  }

  private static String single(CtMethod<?> method, String key) {
    var values =
        literalStrings(Annotations.named(method, "ReturnsView").getFirst().getValues().get(key));
    if (values.size() != 1) throw new IllegalArgumentException("ReturnsView requires " + key);
    return values.getFirst();
  }

  private static int index(CtMethod<?> method, String name) {
    for (int i = 0; i < method.getParameters().size(); i++)
      if (method.getParameters().get(i).getSimpleName().equals(name)) return i;
    throw new IllegalArgumentException("Unknown external summary parameter: " + name);
  }

  private static int root(CtMethod<?> contract, CtExecutable<?> actual, String name) {
    if (!name.equals("this")) return index(contract, name);
    if (actual instanceof CtMethod<?> method && method.isStatic())
      throw new IllegalArgumentException("Static external summary cannot refer to this");
    return -1;
  }

  private static CtTypeReference<?> type(CtMethod<?> contract, CtExecutable<?> actual, int root) {
    return root < 0
        ? actual.getParent(CtType.class).getReference()
        : contract.getParameters().get(root).getType();
  }

  public static boolean isList(CtTypeReference<?> type) {
    return type != null
        && ExternalSpecifications.lineage(type).stream()
            .anyMatch(t -> t.getQualifiedName().equals("java.util.List"));
  }
}
