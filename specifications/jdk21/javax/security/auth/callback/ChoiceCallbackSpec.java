package latte.jdk21.javax.security.auth.callback;

import liquidjava.specification.*;
import specification.*;

/**
 * The constructor validates the default index against a nonempty choice array. Selected indexes are
 * not given these bounds because their setter does not validate each element. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/security/auth/callback/ChoiceCallback.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.base/share/classes/javax/security/auth/callback/ChoiceCallback.java
 */
@ExternalRefinementsFor("javax.security.auth.callback.ChoiceCallback")
public interface ChoiceCallbackSpec {
  @Modifies({})
  @Refinement("_ >= 0")
  int getDefaultChoice();
}
