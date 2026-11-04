package latte.analysis;

import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;

/** Finite slot names: literal indices are precise; all other indices use a summary. */
final class ArraySlots {
  static final String ANY = "[]";

  private ArraySlots() {}

  static String at(int index) {
    return "[" + index + "]";
  }

  static String at(CtExpression<?> index) {
    if (index instanceof CtLiteral<?> literal && index.getTypeCasts().isEmpty()) {
      var value = literal.getValue();
      if (value instanceof Integer i) return at(i);
      if (value instanceof Character c) return at(c);
    }
    return ANY;
  }

  static boolean isSlot(String name) {
    return name.startsWith("[") && name.endsWith("]");
  }
}
