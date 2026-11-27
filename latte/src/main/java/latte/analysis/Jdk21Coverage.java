package latte.analysis;

import java.util.*;
import java.util.stream.Collectors;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.*;
import spoon.reflect.visitor.filter.TypeFilter;

/** Immutable Java 21 coverage ledger on the same AST as ownership and refinement information. */
public record Jdk21Coverage(Set<String> packages, Map<String, TypeEntry> types) {
  public static final String KEY = "latte.jdk21.coverage.v1";
  private static final String MEMBER = KEY + ".member";

  public record Member(
      String kind,
      String signature,
      String result,
      String declaredBy,
      int modifiers,
      boolean varargs,
      String policy,
      String reason) {}

  public record TypeEntry(
      String target,
      String kind,
      List<String> parents,
      String documentation,
      String sourceEntry,
      String sourceSha256,
      List<Member> members) {
    public TypeEntry {
      parents = List.copyOf(parents);
      members = List.copyOf(members);
    }
  }

  /** Effective policy at this use site; a catalog entry alone is not a refinement guarantee. */
  public record Use(String target, String signature, String policy, String reason) {}

  public Jdk21Coverage {
    packages = Set.copyOf(packages);
    types = Map.copyOf(types);
  }

  public static Optional<Jdk21Coverage> on(CtElement element) {
    return element.getFactory().getModel().getRootPackage().getMetadata(KEY)
            instanceof Jdk21Coverage coverage
        ? Optional.of(coverage)
        : Optional.empty();
  }

  public static Optional<Use> use(CtElement element) {
    return element.getMetadata(MEMBER) instanceof Use use ? Optional.of(use) : Optional.empty();
  }

  /** Resolve inherited declarations without making a constructor inheritable. */
  public Optional<Member> member(String target, String signature) {
    var queue = new ArrayDeque<String>();
    var visited = new HashSet<String>();
    queue.add(target);
    while (!queue.isEmpty()) {
      var name = queue.removeFirst();
      if (!visited.add(name)) continue;
      var type = types.get(name);
      if (type == null) continue;
      var member =
          type.members().stream()
              .filter(m -> m.signature().equals(signature))
              .filter(
                  m ->
                      name.equals(target)
                          || !type.kind().equals("interface")
                          || !m.kind().endsWith("method")
                          || !java.lang.reflect.Modifier.isStatic(m.modifiers()))
              .findFirst();
      if (member.isPresent()) return member;
      if (!signature.startsWith("<init>(")) queue.addAll(type.parents());
    }
    return Optional.empty();
  }

  /** Called after external specifications are bound; it never creates trusted declarations. */
  public static void annotate(CtModel model) {
    on(model.getRootPackage())
        .ifPresent(
            coverage -> {
              for (CtExecutableReference<?> reference :
                  model.getElements(new TypeFilter<>(CtExecutableReference.class))) {
                reference.putMetadata(MEMBER, null);
                // Array constructors are synthetic language operations without a declaring class.
                if (reference.isConstructor()
                    && reference.getDeclaringType() instanceof CtArrayTypeReference<?>) continue;
                var actual = reference.getExecutableDeclaration();
                var parameters =
                    actual == null
                        ? reference.getParameters()
                        : actual.getParameters().stream().map(CtParameter::getType).toList();
                String signature =
                    (reference.isConstructor() ? "<init>" : reference.getSimpleName())
                        + parameters.stream()
                            .map(t -> ((CtTypeReference<?>) t).getTypeErasure().getQualifiedName())
                            .collect(Collectors.joining(",", "(", ")"));
                var binding =
                    ExternalSpecifications.binding(ExternalSpecifications.resolve(reference));
                boolean sourceDeclaration =
                    Optional.ofNullable(actual)
                        .map(executable -> executable.getParent(CtType.class))
                        .filter(type -> !type.isShadow())
                        .isPresent();
                if (sourceDeclaration && binding.isEmpty()) continue;
                var owner = reference.getDeclaringType();
                if (!reference.isStatic()
                    && !reference.isConstructor()
                    && reference.isParentInitialized()
                    && reference.getParent() instanceof CtInvocation<?> call
                    && call.getTarget() != null
                    && call.getTarget().getType() != null) owner = call.getTarget().getType();
                coverage
                    .classify(owner, signature, binding.isPresent())
                    .ifPresent(use -> reference.putMetadata(MEMBER, use));
              }
              for (CtFieldReference<?> reference :
                  model.getElements(new TypeFilter<>(CtFieldReference.class))) {
                reference.putMetadata(MEMBER, null);
                var contract = ExternalSpecifications.resolve(reference);
                coverage
                    .classify(
                        reference.getDeclaringType(),
                        reference.getSimpleName(),
                        contract != null && ExternalSpecifications.isSpecification(contract))
                    .ifPresent(use -> reference.putMetadata(MEMBER, use));
              }
            });
  }

  private Optional<Use> classify(CtTypeReference<?> owner, String signature, boolean bound) {
    if (owner == null) return Optional.empty();
    // A source subclass may inherit a library member. Source overrides retain their own analysis.
    var candidates =
        ExternalSpecifications.lineage(owner).stream().map(CtType::getQualifiedName).toList();
    if (candidates.isEmpty()) candidates = List.of(owner.getQualifiedName());
    for (var target : candidates) {
      int dot = target.lastIndexOf('.');
      if (dot < 0 || !packages.contains(target.substring(0, dot))) continue;
      var entry = member(target, signature);
      return Optional.of(
          new Use(
              target,
              signature,
              bound ? "refined" : entry.isPresent() ? "conservative" : "outside-baseline",
              bound
                  ? "bound-external-contract"
                  : entry
                      .map(
                          member ->
                              member.policy().equals("refined")
                                  ? "external-contract-not-selected"
                                  : member.reason())
                      .orElse("not-in-java-se-21-inventory")));
    }
    return Optional.empty();
  }
}
