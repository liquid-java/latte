package latte.refinement;

import java.util.*;
import java.util.function.*;
import spoon.reflect.declaration.CtElement;

/** Resolves immutable predicates using declarations, lexical bindings and object state versions. */
record PredicateResolver(
    Contracts contracts,
    Function<Term, String> referenceType,
    BiFunction<Term, TypingState, Term> versionOf) {
  Term resolve(
      Term term,
      Map<String, Term> bindings,
      TypingState state,
      TypingState old,
      CtElement site,
      int depth) {
    if (depth > 64) throw new IllegalArgumentException("Recursive refinement alias");
    if (term instanceof Term.Name name) {
      var constant = ConstantValues.resolve(name.value(), site).orElse(null);
      if (bindings.containsKey(name.value())) return bindings.get(name.value());
      if (constant != null) return constant;
      int dot = name.value().lastIndexOf('.');
      if (dot > 0) {
        var receiver =
            resolve(
                new Term.Name(name.value().substring(0, dot)),
                bindings,
                state,
                old,
                site,
                depth + 1);
        String type = referenceType.apply(receiver);
        if (type != null) {
          String function = contracts.function(type, name.value().substring(dot + 1));
          if (contracts.fields().containsKey(function))
            return new Term.Call(function, List.of(versionOf.apply(receiver, state)));
        }
      }
      return term;
    }
    if (term instanceof Term.Unary unary)
      return new Term.Unary(
          unary.operator(), resolve(unary.operand(), bindings, state, old, site, depth));
    if (term instanceof Term.Binary binary)
      return new Term.Binary(
          binary.operator(),
          resolve(binary.left(), bindings, state, old, site, depth),
          resolve(binary.right(), bindings, state, old, site, depth));
    if (term instanceof Term.Choice choice)
      return new Term.Choice(
          resolve(choice.condition(), bindings, state, old, site, depth),
          resolve(choice.yes(), bindings, state, old, site, depth),
          resolve(choice.no(), bindings, state, old, site, depth));
    if (term instanceof Term.Call call) {
      if (call.name().equals("old")) {
        if (call.arguments().size() != 1)
          throw new IllegalArgumentException("old expects one argument");
        return versionOf.apply(
            resolve(call.arguments().get(0), bindings, old, old, site, depth), old);
      }
      var arguments =
          call.arguments().stream()
              .map(a -> resolve(a, bindings, state, old, site, depth))
              .toList();
      String name = functionName(call.name(), arguments, site);
      var signature = contracts.functions().get(name);
      if (contracts.ghosts().contains(name)
          && arguments.isEmpty()
          && signature.parameters().size() == 1)
        arguments = List.of(bindings.getOrDefault("this", Term.NULL));
      var alias = contracts.aliases().get(name);
      if (alias != null) {
        if (alias.parameters().size() != arguments.size())
          throw new IllegalArgumentException(
              "Argument mismatch: wrong alias arity: " + call.name());
        var substitution = new HashMap<>(bindings);
        for (int i = 0; i < arguments.size(); i++)
          substitution.put(
              alias.parameters().get(i),
              new Term.Call("require:" + alias.sorts().get(i), List.of(arguments.get(i))));
        return resolve(alias.predicate(), substitution, state, old, site, depth + 1);
      }
      if (contracts.ghosts().contains(name) && !arguments.isEmpty()) {
        var converted = new ArrayList<>(arguments);
        for (int i = 0; i < Math.min(arguments.size(), signature.parameters().size()); i++)
          if (signature.parameters().get(i) == Term.Sort.REF)
            converted.set(i, versionOf.apply(arguments.get(i), state));
        arguments = List.copyOf(converted);
      }
      return new Term.Call(name, arguments);
    }
    return term;
  }

  private String functionName(String name, List<Term> arguments, CtElement site) {
    String qualified = Contracts.qualify(site, name);
    String local =
        qualified.contains("#")
            ? contracts.function(qualified.substring(0, qualified.lastIndexOf('#')), name)
            : name;
    if (contracts.functions().containsKey(local) || contracts.aliases().containsKey(local))
      return local;
    if (!arguments.isEmpty() && referenceType.apply(arguments.get(0)) != null) {
      String typed = contracts.function(referenceType.apply(arguments.get(0)), name);
      if (contracts.functions().containsKey(typed)) return typed;
    }
    var matches =
        java.util.stream.Stream.concat(
                contracts.functions().keySet().stream(), contracts.aliases().keySet().stream())
            .filter(key -> key.endsWith("#" + name))
            .distinct()
            .toList();
    return matches.size() == 1 ? matches.get(0) : name;
  }
}
