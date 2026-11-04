package liquidjava.specification;

import java.lang.annotation.*;

/** Public annotation syntax consumed by the Spoon verifier. */
@Retention(RetentionPolicy.RUNTIME)
@Target({
  ElementType.TYPE,
  ElementType.FIELD,
  ElementType.PARAMETER,
  ElementType.METHOD,
  ElementType.CONSTRUCTOR,
  ElementType.LOCAL_VARIABLE
})
@Repeatable(RefinementPredicates.class)
public @interface RefinementPredicate {
  String value();
}
