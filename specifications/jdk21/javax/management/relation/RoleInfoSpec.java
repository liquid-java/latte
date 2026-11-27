package latte.jdk21.javax.management.relation;

import liquidjava.specification.*;
import specification.*;

/**
 * Degree -1 is ROLE_CARDINALITY_INFINITY; constructors validate all other degrees as nonnegative.
 * Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.management/javax/management/relation/RoleInfo.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.management/share/classes/javax/management/relation/RoleInfo.java
 */
@ExternalRefinementsFor("javax.management.relation.RoleInfo")
public interface RoleInfoSpec {
  @Modifies({})
  @Refinement("_ >= -1")
  int getMinDegree();

  @Modifies({})
  @Refinement("_ >= -1")
  int getMaxDegree();
}
