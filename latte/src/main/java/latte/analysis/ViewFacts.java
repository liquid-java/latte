package latte.analysis;

import java.util.*;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;

/** Creation and operation summaries shared by ownership and refinement checking through the AST. */
public record ViewFacts(Reference view, Reference backing, ExternalSummaries.View specification) {
  public static final String KEY = "latte.analysis.view.v1";
  public static final String BACKING = ClosureFacts.CAPTURE + "@viewBacking";
  public static final String ARRAY_LIST = ClosureFacts.CAPTURE + "@arrayListView";
  public static final String OPERATION = KEY + ".operation";

  public enum Operation {
    SIZE,
    EMPTY,
    GET,
    SET,
    ADD,
    INSERT,
    REMOVE,
    CLEAR,
    SUBVIEW
  }

  public static Optional<ViewFacts> on(CtElement node) {
    return node != null && node.getMetadata(KEY) instanceof ViewFacts facts
        ? Optional.of(facts)
        : Optional.empty();
  }

  public static Optional<Operation> operation(CtInvocation<?> call) {
    return call.getMetadata(OPERATION) instanceof Operation operation
        ? Optional.of(operation)
        : Optional.empty();
  }

  public static Optional<Operation> classify(
      CtInvocation<?> call, Reference receiver, FlowState state) {
    if (receiver.objects().isEmpty()
        || receiver.objects().stream()
            .anyMatch(id -> state.read(Reference.object(id), BACKING).isEmpty()))
      return Optional.empty();
    if (receiver.objects().stream()
        .anyMatch(id -> state.read(Reference.object(id), ARRAY_LIST).isEmpty()))
      return Optional.empty();
    if (!ExternalSummaries.isList(call.getExecutable().getDeclaringType())) return Optional.empty();
    String signature = call.getExecutable().getSignature();
    return Optional.ofNullable(
        switch (signature) {
          case "size()" -> Operation.SIZE;
          case "isEmpty()" -> Operation.EMPTY;
          case "get(int)" -> Operation.GET;
          case "set(int,java.lang.Object)" -> Operation.SET;
          case "add(java.lang.Object)" -> Operation.ADD;
          case "add(int,java.lang.Object)" -> Operation.INSERT;
          case "remove(int)" -> Operation.REMOVE;
          case "clear()" -> Operation.CLEAR;
          case "subList(int,int)" -> Operation.SUBVIEW;
          default -> null;
        });
  }
}
