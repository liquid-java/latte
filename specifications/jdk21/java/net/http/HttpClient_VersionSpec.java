package latte.jdk21.java.net.http;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.net.http.HttpClient$Version")
public interface HttpClient_VersionSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 2")
  int ordinal();
}
