package latte.analysis;

import java.util.*;
import java.util.stream.Collectors;
import latte.api.Diagnostic;
import spoon.reflect.CtModel;
import spoon.reflect.code.*;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.*;
import spoon.reflect.visitor.filter.TypeFilter;

/** Per-model library contracts, resolved against Java declarations before either analysis runs. */
public record ExternalSpecifications(
    Map<String, Specification> specifications, List<Diagnostic> diagnostics) {
  public static final String KEY = "latte.analysis.external.v1";
  private static final String BINDING = KEY + ".binding";
  private static final String FIELD = KEY + ".field";

  public record Binding(CtExecutable<?> actual, CtMethod<?> contract) {}

  public record Specification(
      CtType<?> target,
      CtType<?> source,
      Map<String, Binding> methods,
      Map<String, CtField<?>> fields) {
    public Specification {
      methods = Map.copyOf(methods);
      fields = Map.copyOf(fields);
    }
  }

  public ExternalSpecifications {
    specifications = Map.copyOf(specifications);
    diagnostics = List.copyOf(diagnostics);
  }

  public static ExternalSpecifications collect(CtModel model) {
    var specifications = new LinkedHashMap<String, Specification>();
    var diagnostics = new ArrayList<Diagnostic>();
    for (var element : model.getElements(new TypeFilter<>(CtElement.class))) {
      element.putMetadata(BINDING, null);
      element.putMetadata(FIELD, null);
      if (Annotations.has(element, "ExternalRefinementsFor") && !(element instanceof CtType<?>))
        error(diagnostics, "ExternalRefinementsFor belongs on a specification type", element);
    }
    for (CtType<?> source : model.getElements(new TypeFilter<>(CtType.class))) {
      if (!Annotations.has(source, "ExternalRefinementsFor")) continue;
      var names = Annotations.strings(source, "ExternalRefinementsFor", "value");
      if (names.size() != 1 || names.get(0).isBlank()) {
        error(diagnostics, "External refinements require one qualified target type", source);
        continue;
      }
      String name = names.get(0);
      CtType<?> target = source.getFactory().Type().createReference(name).getTypeDeclaration();
      if (target == null) {
        error(
            diagnostics,
            "Cannot resolve external type " + name + "; include its library on the classpath",
            source);
        continue;
      }
      if (specifications.containsKey(target.getQualifiedName())) {
        error(diagnostics, "Duplicate external specification for " + name, source);
        continue;
      }
      var methods = new LinkedHashMap<String, Binding>();
      for (CtMethod<?> contract : source.getMethods()) {
        boolean constructor = contract.getSimpleName().equals(target.getSimpleName());
        var candidates = new ArrayList<CtExecutable<?>>();
        if (constructor && target instanceof CtClass<?> clazz)
          candidates.addAll(clazz.getConstructors());
        else if (!constructor) candidates.addAll(target.getAllMethods());
        String signature = signature(contract, constructor);
        var matches =
            candidates.stream()
                .filter(actual -> inheritedMember(target, actual))
                .filter(actual -> signature(actual, constructor).equals(signature))
                .toList();
        if (matches.isEmpty()) {
          error(diagnostics, "No external member " + name + "#" + signature, contract);
          continue;
        }
        var actual =
            matches.stream()
                .filter(member -> member.getParent(CtType.class) == target)
                .findFirst()
                .orElse(matches.get(0));
        if (constructor && !contract.getType().getSimpleName().equals("void")
            || !constructor
                && actual instanceof CtMethod<?> method
                && !erased(method.getType()).equals(erased(contract.getType()))) {
          error(
              diagnostics,
              "External member has an incompatible result type: " + name + "#" + signature,
              contract);
          continue;
        }
        var binding = new Binding(actual, contract);
        if (actual instanceof CtMethod<?> method
            && method.isStatic()
            && Annotations.has(contract, "StateRefinement")) {
          error(
              diagnostics,
              "A static external method has no receiver state; use Refinement for its result",
              contract);
          continue;
        }
        methods.put(signature, binding);
        contract.putMetadata(BINDING, binding);
      }
      var fields = new LinkedHashMap<String, CtField<?>>();
      for (var contract : source.getFields()) {
        var actual =
            target.getAllFields().stream()
                .filter(field -> field.getSimpleName().equals(contract.getSimpleName()))
                .map(CtFieldReference::getFieldDeclaration)
                .filter(Objects::nonNull)
                .filter(field -> inheritedMember(target, field))
                .findFirst()
                .orElse(null);
        if (actual == null || !erased(actual.getType()).equals(erased(contract.getType())))
          error(
              diagnostics,
              "No compatible external field " + name + "#" + contract.getSimpleName(),
              contract);
        else {
          fields.put(contract.getSimpleName(), contract);
          contract.putMetadata(FIELD, actual);
        }
      }
      specifications.put(
          target.getQualifiedName(), new Specification(target, source, methods, fields));
    }
    var result = new ExternalSpecifications(specifications, diagnostics);
    model.getRootPackage().putMetadata(KEY, result);
    for (CtExecutableReference<?> reference :
        model.getElements(new TypeFilter<>(CtExecutableReference.class))) {
      result
          .binding(reference)
          .ifPresent(
              binding -> {
                reference.putMetadata(BINDING, binding);
                if (reference.isParentInitialized()
                    && reference.getParent() instanceof CtAbstractInvocation<?> call
                    && expandedVarargs(binding.actual(), call.getArguments()))
                  diagnostics.add(
                      Diagnostic.at(
                          Diagnostic.Kind.UNSUPPORTED,
                          "External varargs contracts require an explicit array argument",
                          reference.getParent()));
                if (reference.isParentInitialized()
                    && reference.getParent() instanceof CtExecutableReferenceExpression<?, ?>
                    && binding.actual().getParameters().stream().anyMatch(CtParameter::isVarArgs))
                  diagnostics.add(
                      Diagnostic.at(
                          Diagnostic.Kind.UNSUPPORTED,
                          "External varargs method references require a lambda with an explicit array argument",
                          reference.getParent()));
              });
    }
    var collected = new ExternalSpecifications(specifications, diagnostics);
    model.getRootPackage().putMetadata(KEY, collected);
    return collected;
  }

  public static ExternalSpecifications on(CtElement element) {
    var metadata = element.getFactory().getModel().getRootPackage().getMetadata(KEY);
    return metadata instanceof ExternalSpecifications specifications
        ? specifications
        : new ExternalSpecifications(Map.of(), List.of());
  }

  public static boolean isSpecification(CtElement element) {
    if (element == null) return false;
    var type = element instanceof CtType<?> t ? t : element.getParent(CtType.class);
    return type != null && Annotations.has(type, "ExternalRefinementsFor");
  }

  public static boolean isLibrary(CtElement element) {
    var type = element instanceof CtType<?> t ? t : element.getParent(CtType.class);
    return type != null && on(element).specifications().containsKey(type.getQualifiedName());
  }

  public static String owner(CtType<?> type) {
    var names = Annotations.strings(type, "ExternalRefinementsFor", "value");
    return names.size() == 1 ? names.get(0) : type.getQualifiedName();
  }

  public static CtExecutable<?> resolve(CtExecutableReference<?> reference) {
    var binding = reference.getMetadata(BINDING);
    if (binding instanceof Binding resolved) return resolved.contract();
    return on(reference)
        .binding(reference)
        .map(Binding::contract)
        .<CtExecutable<?>>map(contract -> contract)
        .orElse(reference.getDeclaration());
  }

  public static CtField<?> resolve(CtFieldReference<?> reference) {
    var registry = on(reference);
    var actual = reference.getFieldDeclaration();
    for (var type : lineage(reference.getDeclaringType())) {
      var specification = registry.specifications().get(type.getQualifiedName());
      if (specification != null && specification.fields().containsKey(reference.getSimpleName())) {
        var contract = specification.fields().get(reference.getSimpleName());
        if (actual != null
            && contract.getMetadata(FIELD) instanceof CtField<?> specified
            && specified
                .getReference()
                .getQualifiedName()
                .equals(actual.getReference().getQualifiedName())) return contract;
      }
    }
    return reference.getDeclaration();
  }

  public static Optional<Binding> binding(CtExecutable<?> contract) {
    return contract != null && contract.getMetadata(BINDING) instanceof Binding binding
        ? Optional.of(binding)
        : Optional.empty();
  }

  public static boolean constructor(CtExecutable<?> contract) {
    return contract instanceof CtConstructor<?>
        || binding(contract)
            .map(binding -> binding.actual() instanceof CtConstructor<?>)
            .orElse(false);
  }

  public static boolean isStatic(CtField<?> field) {
    return field.getMetadata(FIELD) instanceof CtField<?> actual
        ? actual.isStatic()
        : field.isStatic();
  }

  private Optional<Binding> binding(CtExecutableReference<?> reference) {
    if (specifications.isEmpty()) return Optional.empty();
    var actual = reference.getExecutableDeclaration();
    String signature =
        actual == null
            ? (reference.isConstructor() ? "<init>" : reference.getSimpleName())
                + "("
                + reference.getParameters().stream()
                    .map(ExternalSpecifications::erased)
                    .collect(Collectors.joining(","))
                + ")"
            : signature(actual, reference.isConstructor());
    var receiver = reference.getDeclaringType();
    if (reference.isParentInitialized()
        && reference.getParent() instanceof CtInvocation<?> call
        && !reference.isStatic()
        && !reference.isConstructor()
        && call.getTarget() != null
        && !(call.getTarget() instanceof CtTypeAccess<?>)
        && call.getTarget().getType() != null) receiver = call.getTarget().getType();
    for (var type : lineage(receiver)) {
      var specification = specifications.get(type.getQualifiedName());
      if (specification != null) {
        var binding = specification.methods().get(signature);
        if (binding != null
            && (!reference.isStatic()
                || actual == null
                || binding
                    .actual()
                    .getParent(CtType.class)
                    .getQualifiedName()
                    .equals(actual.getParent(CtType.class).getQualifiedName())))
          return Optional.of(binding);
      }
      if (reference.isConstructor()) break; // Constructors are never inherited.
    }
    return Optional.empty();
  }

  private static boolean expandedVarargs(
      CtExecutable<?> executable, List<CtExpression<?>> arguments) {
    var parameters = executable.getParameters();
    if (parameters.isEmpty() || !parameters.get(parameters.size() - 1).isVarArgs()) return false;
    if (parameters.size() != arguments.size()) return true;
    var argument = arguments.get(arguments.size() - 1);
    if (argument instanceof CtLiteral<?> literal && literal.getValue() == null) return false;
    var type = argument.getType();
    var expected =
        argument
            .getFactory()
            .Type()
            .createReference(erased(parameters.get(parameters.size() - 1).getType()));
    return type == null || !type.isArray() || !type.isSubtypeOf(expected);
  }

  public static List<CtType<?>> lineage(CtTypeReference<?> reference) {
    if (reference == null) return List.of();
    var queue = new ArrayDeque<CtTypeReference<?>>();
    queue.add(reference);
    var visited = new HashSet<String>();
    var result = new ArrayList<CtType<?>>();
    while (!queue.isEmpty()) {
      var next = queue.removeFirst();
      if (!visited.add(next.getQualifiedName())) continue;
      var type = next.getTypeDeclaration();
      if (type == null) continue;
      result.add(type);
      if (type.getSuperclass() != null) queue.addLast(type.getSuperclass());
      type.getSuperInterfaces().stream()
          .sorted(Comparator.comparing(CtTypeReference::getQualifiedName))
          .forEach(queue::addLast);
    }
    return List.copyOf(result);
  }

  /** Spoon includes members that are not inherited under Java's access and static rules. */
  private static boolean inheritedMember(CtType<?> target, CtElement member) {
    var owner = member.getParent(CtType.class);
    if (owner == target) return true;
    if (!(member instanceof CtModifiable modifiers) || modifiers.isPrivate()) return false;
    if (member instanceof CtMethod<?> method && method.isStatic() && owner.isInterface())
      return false;
    if (modifiers.isPublic() || modifiers.isProtected()) return true;
    return packagePath(target, owner, new HashSet<>());
  }

  private static boolean packagePath(CtType<?> type, CtType<?> owner, Set<String> visited) {
    if (type == null || !visited.add(type.getQualifiedName())) return false;
    if (type.getQualifiedName().equals(owner.getQualifiedName())) return true;
    if (!type.getPackage().getQualifiedName().equals(owner.getPackage().getQualifiedName()))
      return false;
    return type.getSuperclass() != null
        && packagePath(type.getSuperclass().getTypeDeclaration(), owner, visited);
  }

  private static String signature(CtExecutable<?> executable, boolean constructor) {
    return (constructor ? "<init>" : executable.getSimpleName())
        + "("
        + executable.getParameters().stream()
            .map(parameter -> erased(parameter.getType()))
            .collect(Collectors.joining(","))
        + ")";
  }

  private static String erased(CtTypeReference<?> type) {
    if (type == null) return "java.lang.Object";
    if (type instanceof CtTypeParameterReference parameter)
      return erased(parameter.getBoundingType());
    if (type instanceof CtArrayTypeReference<?> array)
      return erased(array.getComponentType()) + "[]";
    return type.getQualifiedName();
  }

  private static void error(List<Diagnostic> diagnostics, String message, CtElement site) {
    diagnostics.add(Diagnostic.at(Diagnostic.Kind.SYNTAX, message, site));
  }
}
