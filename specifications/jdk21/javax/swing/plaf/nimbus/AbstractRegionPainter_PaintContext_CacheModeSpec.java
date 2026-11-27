package latte.jdk21.javax.swing.plaf.nimbus;

import liquidjava.specification.*;
import specification.*;

@ExternalRefinementsFor("javax.swing.plaf.nimbus.AbstractRegionPainter$PaintContext$CacheMode")
public interface AbstractRegionPainter_PaintContext_CacheModeSpec {
  // Generated enum ordinal bound.
  @Modifies({})
  @Refinement("_ >= 0 && _ < 3")
  int ordinal();
}
