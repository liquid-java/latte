package latte.jdk21.javax.xml.xpath;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.xml.xpath.XPathEvaluationResult$XPathResultType")
public interface XPathEvaluationResult_XPathResultTypeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 6")
  int ordinal();
}
