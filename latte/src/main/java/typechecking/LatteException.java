package typechecking;

import spoon.reflect.declaration.CtElement;

public class LatteException extends RuntimeException {
  private CtElement ce;
  private String fullMessage;

  public LatteException(String shortMessage, String fullMessage, CtElement ce) {
    super(shortMessage);
    this.ce = ce;
    this.fullMessage = fullMessage;
  }

  /*
   * Retain the element that caused the exception
   */
  public CtElement getElement() {
    return ce;
  }

  public String getFullMessage() {
    return fullMessage;
  }
}
