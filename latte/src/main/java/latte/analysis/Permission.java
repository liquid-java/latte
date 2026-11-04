package latte.analysis;

/** FREE permits local aliases; transferring it consumes every alias in the caller. */
public enum Permission {
  BOTTOM,
  SHARED,
  UNIQUE,
  BORROWED,
  FREE,
  IMMUTABLE;

  public boolean refinable() {
    return this != SHARED && this != BOTTOM;
  }

  public Permission join(Permission other) {
    if (this == other) return this;
    if (this == BOTTOM || other == BOTTOM) return BOTTOM;
    if (this == SHARED || other == SHARED) return SHARED;
    if (this == BORROWED || other == BORROWED) return BORROWED;
    if (this == UNIQUE || other == UNIQUE) return UNIQUE;
    return FREE;
  }
}
