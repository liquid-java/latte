package latte.jdk21.javax.tools;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.tools.DocumentationTool$Location")
public interface DocumentationTool_LocationSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
