package latte.jdk21.java.net.http;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.net.http.HttpClient$Redirect")
public interface HttpClient_RedirectSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
