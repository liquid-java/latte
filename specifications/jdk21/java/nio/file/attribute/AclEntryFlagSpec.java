package latte.jdk21.java.nio.file.attribute;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.nio.file.attribute.AclEntryFlag")
public interface AclEntryFlagSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
