package latte.jdk21.java.awt.image;

import liquidjava.specification.*;
import specification.*;

/**
 * BufferedImage has a positive raster size and exactly one tile in each direction. This does not
 * apply to asynchronous Image dimensions, which can be unknown. Documentation:
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/image/BufferedImage.html
 * Implementation:
 * https://github.com/openjdk/jdk21u/blob/jdk-21.0.13.1%2B2/src/java.desktop/share/classes/java/awt/image/BufferedImage.java
 */
@ExternalRefinementsFor("java.awt.image.BufferedImage")
public interface BufferedImageSpec {
  @Modifies({})
  @Refinement("_ > 0")
  int getWidth();

  @Modifies({})
  @Refinement("_ > 0")
  int getHeight();

  @Modifies({})
  @Refinement("_ == 1")
  int getNumXTiles();

  @Modifies({})
  @Refinement("_ == 1")
  int getNumYTiles();
}
