package latte.jdk21.java.util.stream;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("java.util.stream.Collector$Characteristics")
public interface Collector_CharacteristicsSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
