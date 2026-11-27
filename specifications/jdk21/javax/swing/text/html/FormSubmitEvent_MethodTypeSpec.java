package latte.jdk21.javax.swing.text.html;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.swing.text.html.FormSubmitEvent$MethodType")
public interface FormSubmitEvent_MethodTypeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
