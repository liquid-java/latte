package latte.analysis;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Possible object identities, including null; scalar values have no heap identity. */
public record Reference(Set<String> objects, boolean scalar, boolean nullable) {
  private static final String ARRAY_ELEMENTS = "array-elements:";

  public Reference {
    objects = Set.copyOf(objects);
  }

  public Reference(Set<String> objects, boolean scalar) {
    this(objects, scalar, !scalar && objects.isEmpty());
  }

  public static Reference scalarValue() {
    return new Reference(Set.of(), true);
  }

  public static Reference nullValue() {
    return new Reference(Set.of(), false);
  }

  static Reference none() {
    return new Reference(Set.of(), false, false);
  }

  public static Reference object(String identity) {
    return new Reference(Set.of(identity), false);
  }

  /**
   * A set of unknown array elements, never an assertion that different slots contain one object.
   */
  static Reference arrayElements(String owner) {
    return object(ARRAY_ELEMENTS + owner);
  }

  public static boolean summarizesElements(String identity) {
    return identity.startsWith(ARRAY_ELEMENTS);
  }

  public Reference join(Reference other) {
    return new Reference(
        Stream.concat(objects.stream(), other.objects.stream()).collect(Collectors.toSet()),
        scalar && other.scalar,
        nullable || other.nullable);
  }
}
