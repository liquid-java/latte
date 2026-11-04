package liquidjava.specification;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({
  ElementType.TYPE,
  ElementType.FIELD,
  ElementType.PARAMETER,
  ElementType.METHOD,
  ElementType.CONSTRUCTOR,
  ElementType.LOCAL_VARIABLE
})
public @interface Ghosts {
  Ghost[] value();
}
