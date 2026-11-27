package latte.jdk21.javax.xml.catalog;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.xml.catalog.CatalogFeatures$Feature")
public interface CatalogFeatures_FeatureSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 4")
  int ordinal();
}
