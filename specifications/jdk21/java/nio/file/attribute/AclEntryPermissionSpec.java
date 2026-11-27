package latte.jdk21.java.nio.file.attribute;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.nio.file.attribute.AclEntryPermission")
public interface AclEntryPermissionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 14")
  int ordinal();
}
