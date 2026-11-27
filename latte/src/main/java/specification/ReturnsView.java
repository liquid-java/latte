package specification;

import java.lang.annotation.*;

/** A fresh List view capturing a receiver or borrowed parameter, with a half-open index range. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ReturnsView {
  String of();

  String from();

  String toExclusive();
}
