package specification;

import java.lang.annotation.*;

/** Complete write footprint of an external call. An empty set preserves existing storage. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Modifies {
  /** Receiver/parameter roots, optionally followed by .* or [*]. */
  String[] value();
}
