package latte.refinement;

import java.util.*;
import java.util.regex.Pattern;
import latte.analysis.Annotations;
import latte.api.Diagnostic;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.*;
import spoon.reflect.visitor.filter.TypeFilter;

/** Immutable declaration index. Contracts are collected before any body is checked. */
public record Contracts(
    Map<String, Prover.Function> functions,
    Map<String, Alias> aliases,
    Map<String, List<List<String>>> states,
    Set<String> ghosts,
    Map<String, CtField<?>> fields,
    Map<String, List<String>> ancestors,
    List<Diagnostic> diagnostics) {
  public record Alias(
      List<String> parameters, List<Term.Sort> sorts, Term predicate, CtElement site) {
    public Alias {
      parameters = List.copyOf(parameters);
      sorts = List.copyOf(sorts);
    }
  }

  public Contracts {
    functions = Map.copyOf(functions);
    aliases = Map.copyOf(aliases);
    states = Map.copyOf(states);
    ghosts = Set.copyOf(ghosts);
    fields = Map.copyOf(fields);
    ancestors = Map.copyOf(ancestors);
    diagnostics = List.copyOf(diagnostics);
  }

  public static Contracts collect(CtModel model) {
    var functions = new HashMap<String, Prover.Function>();
    functions.put("length", new Prover.Function(List.of(Term.Sort.REF), Term.Sort.INT));
    var aliases = new HashMap<String, Alias>();
    var states = new HashMap<String, List<List<String>>>();
    var ghosts = new HashSet<String>();
    var fields = new HashMap<String, CtField<?>>();
    var ancestors = new HashMap<String, List<String>>();
    var diagnostics = new ArrayList<Diagnostic>();
    for (CtType<?> type : model.getElements(new TypeFilter<>(CtType.class))) {
      ancestors.put(type.getQualifiedName(), hierarchy(type, new LinkedHashSet<>()));
      for (var field : type.getFields())
        if (!field.isStatic()) {
          String name = qualify(field, field.getSimpleName());
          fields.put(name, field);
          functions.put(
              name,
              new Prover.Function(
                  List.of(Term.Sort.REF), sort(field.getType().getQualifiedName())));
          ghosts.add(name);
        }
      var names = Annotations.strings(type, "StateSet", "value");
      if (!names.isEmpty()) {
        if (names.size() != new HashSet<>(names).size())
          diagnostics.add(Diagnostic.at(Diagnostic.Kind.SYNTAX, "Duplicate state name", type));
        states.put(
            type.getQualifiedName(),
            Annotations.named(type, "StateSet").stream()
                .map(
                    a ->
                        Annotations.strings(a.getValues().get("value")).stream()
                            .map(n -> type.getQualifiedName() + "#" + n)
                            .toList())
                .toList());
        for (String name : names) {
          String qualified = type.getQualifiedName() + "#" + name;
          functions.put(qualified, new Prover.Function(List.of(Term.Sort.REF), Term.Sort.BOOL));
          ghosts.add(qualified);
        }
      }
    }
    for (var element : model.getElements(new TypeFilter<>(CtElement.class))) {
      for (var annotation : Annotations.named(element, "StateRefinement")) {
        for (String key : List.of("from", "to"))
          for (String text : Annotations.strings(annotation.getValues().get(key))) {
            if (text.isBlank()) continue;
            try {
              PredicateParser.parse(text);
            } catch (IllegalArgumentException failure) {
              diagnostics.add(
                  Diagnostic.at(Diagnostic.Kind.SYNTAX, failure.getMessage(), annotation));
            }
          }
      }
      if (Annotations.has(element, "ExternalRefinementsFor"))
        diagnostics.add(
            Diagnostic.at(
                Diagnostic.Kind.UNSUPPORTED, "External refinements are not implemented", element));
      for (String text : Annotations.strings(element, "RefinementAlias", "value")) {
        try {
          var match =
              Pattern.compile("(?:type\\s+)?(\\w+)\\s*\\(([^)]*)\\)\\s*\\{(.*)}", Pattern.DOTALL)
                  .matcher(text);
          if (!match.matches()) throw new IllegalArgumentException("Invalid alias declaration");
          var parameters = new ArrayList<String>();
          var sorts = new ArrayList<Term.Sort>();
          for (String parameter : match.group(2).split(","))
            if (!parameter.isBlank()) {
              var parts = parameter.trim().split("\\s+");
              if (parts.length != 2)
                throw new IllegalArgumentException("Alias parameters require a type and a name");
              parameters.add(parts[1]);
              sorts.add(sort(parts[0]));
            }
          if (parameters.isEmpty())
            throw new IllegalArgumentException("Alias requires at least one parameter");
          aliases.put(
              qualify(element, match.group(1)),
              new Alias(parameters, sorts, PredicateParser.parse(match.group(3)), element));
        } catch (IllegalArgumentException failure) {
          diagnostics.add(Diagnostic.at(Diagnostic.Kind.SYNTAX, failure.getMessage(), element));
        }
      }
      for (String annotation : List.of("Ghost", "RefinementPredicate")) {
        for (String text : Annotations.strings(element, annotation, "value")) {
          try {
            var match =
                Pattern.compile("(\\w+)\\s+(\\w+)(?:\\s*\\(([^)]*)\\))?").matcher(text.trim());
            if (!match.matches())
              throw new IllegalArgumentException("Invalid ghost or predicate declaration");
            if (annotation.equals("Ghost") && match.group(3) != null && !match.group(3).isBlank())
              throw new IllegalArgumentException(
                  "A ghost describes one receiver and cannot declare extra parameters");
            var parameters = new ArrayList<Term.Sort>();
            if (annotation.equals("Ghost")) parameters.add(Term.Sort.REF);
            if (match.group(3) != null)
              for (String p : match.group(3).split(","))
                if (!p.isBlank()) parameters.add(sort(p.trim().split("\\s+")[0]));
            String name = qualify(element, match.group(2));
            functions.put(name, new Prover.Function(parameters, sort(match.group(1))));
            if (annotation.equals("Ghost") || parameters.contains(Term.Sort.REF)) ghosts.add(name);
          } catch (IllegalArgumentException failure) {
            diagnostics.add(Diagnostic.at(Diagnostic.Kind.SYNTAX, failure.getMessage(), element));
          }
        }
      }
    }
    var inheritedStates = new HashMap<String, List<List<String>>>();
    ancestors.forEach(
        (type, lineage) ->
            inheritedStates.put(
                type,
                lineage.stream()
                    .flatMap(name -> states.getOrDefault(name, List.of()).stream())
                    .distinct()
                    .toList()));
    return new Contracts(
        functions, aliases, inheritedStates, ghosts, fields, ancestors, diagnostics);
  }

  private static List<String> hierarchy(CtType<?> type, Set<String> visited) {
    if (type == null || !visited.add(type.getQualifiedName())) return List.of();
    var result = new ArrayList<String>();
    result.add(type.getQualifiedName());
    if (type.getSuperclass() != null)
      result.addAll(hierarchy(type.getSuperclass().getTypeDeclaration(), visited));
    for (var parent : type.getSuperInterfaces())
      result.addAll(hierarchy(parent.getTypeDeclaration(), visited));
    return List.copyOf(result);
  }

  public boolean belongsTo(String function, String type) {
    return ancestors.getOrDefault(type, List.of(type)).stream()
        .anyMatch(owner -> function.startsWith(owner + "#"));
  }

  public String function(String type, String simple) {
    return ancestors.getOrDefault(type, List.of(type)).stream()
        .map(owner -> owner + "#" + simple)
        .filter(name -> functions.containsKey(name) || aliases.containsKey(name))
        .findFirst()
        .orElse(simple);
  }

  public static String qualify(CtElement element, String name) {
    var type = element instanceof CtType<?> t ? t : element.getParent(CtType.class);
    return type == null ? name : type.getQualifiedName() + "#" + name;
  }

  public static List<CtExecutable<?>> sources(CtExecutable<?> executable) {
    if (executable == null) return List.of();
    if (!(executable instanceof CtMethod<?> method) || annotated(method))
      return List.of(executable);
    var inherited = inherited(method);
    return inherited.isEmpty() ? List.of(executable) : inherited;
  }

  public static List<CtExecutable<?>> inherited(CtMethod<?> method) {
    var result = new LinkedHashSet<CtExecutable<?>>();
    var type = method.getDeclaringType();
    var parents = new ArrayList<spoon.reflect.reference.CtTypeReference<?>>();
    if (type.getSuperclass() != null) parents.add(type.getSuperclass());
    parents.addAll(type.getSuperInterfaces());
    for (var parent : parents)
      inherited(method, parent.getTypeDeclaration(), new HashSet<>(), result);
    return List.copyOf(result);
  }

  private static void inherited(
      CtMethod<?> method, CtType<?> type, Set<String> visited, Set<CtExecutable<?>> result) {
    if (type == null || !visited.add(type.getQualifiedName())) return;
    var matches =
        type.getMethodsByName(method.getSimpleName()).stream()
            .filter(
                candidate ->
                    !candidate.isStatic()
                        && !candidate.isPrivate()
                        && candidate.getParameters().size() == method.getParameters().size())
            .filter(candidate -> method.isOverriding(candidate))
            .filter(Contracts::annotated)
            .toList();
    if (!matches.isEmpty()) {
      result.addAll(matches);
      return;
    }
    if (type.getSuperclass() != null)
      inherited(method, type.getSuperclass().getTypeDeclaration(), visited, result);
    for (var parent : type.getSuperInterfaces())
      inherited(method, parent.getTypeDeclaration(), visited, result);
  }

  public static CtExecutable<?> source(CtExecutable<?> executable) {
    return sources(executable).stream().findFirst().orElse(null);
  }

  public static boolean annotated(CtExecutable<?> executable) {
    return Annotations.has(executable, "Refinement")
        || Annotations.has(executable, "StateRefinement")
        || executable.getParameters().stream().anyMatch(p -> Annotations.has(p, "Refinement"));
  }

  public static Term.Sort sort(String type) {
    return switch (type) {
      case "boolean" -> Term.Sort.BOOL;
      case "byte", "short", "int", "long", "char" -> Term.Sort.INT;
      case "float" -> Term.Sort.FLOAT32;
      case "double" -> Term.Sort.FLOAT64;
      default -> Term.Sort.REF;
    };
  }
}
