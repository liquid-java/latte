package typechecking;

import context.ClassLevelMaps;
import context.PermissionEnvironment;
import context.SymbolicEnvironment;
import context.UniquenessAnnotation;
import specification.ExternalRefinementsFor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * First pass that collects permission annotations (@unique, @shared, etc.)
 * for external method parameters and stores them in ClassLevelMaps.
 */
public class ExternalRefinementFirstPass extends LatteAbstractChecker {

    private CtTypeReference<?> currentExtRefTarget = null;

    public ExternalRefinementFirstPass(SymbolicEnvironment symbEnv,
                                       PermissionEnvironment permEnv,
                                       ClassLevelMaps maps) {
        super(symbEnv, permEnv, maps);
        logInfo("[ External Refinements Pass started ]");
        enterScopes();
    }

    @Override
    public <T> void visitCtInterface(CtInterface<T> ctInterface) {
        logInfo("Visiting interface: " + ctInterface.getSimpleName(), ctInterface);
        // @ExternalRefinementsFor annotation check
        CtAnnotation<?> ann = ctInterface.getAnnotation(ctInterface.getFactory().Type().createReference("specification.ExternalRefinementsFor"));

        if (ann == null) {
            return;
        }

        CtExpression<?> expr = ann.getValues().get("value");

        if (expr instanceof CtLiteral<?> && ((CtLiteral<?>) expr).getValue() instanceof String) {
            currentExtRefTarget = ctInterface.getFactory().Type().createReference((String) ((CtLiteral<?>) expr).getValue());
        } else {
            logWarning("Expected a string literal in @ExternalRefinementsFor");
            return;
        }

        if (currentExtRefTarget == null) {
            logWarning("No target class specified in @ExternalRefinementsFor");
            return;
        }

        if (ctInterface.isPublic()) {
            logInfo("Processing external interface: " + ctInterface.getQualifiedName(), ctInterface);
            scan(ctInterface.getMethods());
        }
        super.visitCtInterface(ctInterface);

        currentExtRefTarget = null;
    }

    @Override
    public <T> void visitCtMethod(CtMethod<T> method) {
        logInfo("Visiting method: " + method.getSimpleName(), method);

        if (currentExtRefTarget == null) {
            return;
        }

        CtBlock<?> body = method.getBody();
        if (body != null && !body.isImplicit()) return;

        List<CtParameter<?>> parameters = method.getParameters();
        List<UniquenessAnnotation> paramAnnotations = new ArrayList<>();

        for (CtParameter<?> param : parameters) {
            UniquenessAnnotation ua = new UniquenessAnnotation(param);
            paramAnnotations.add(ua);
        }

        Pair<String, Integer> methodSig = Pair.of(method.getSimpleName(), parameters.size());

        logInfo("Registering external refinements for: " + currentExtRefTarget, method);

        maps.addExternalMethod(currentExtRefTarget, method);
        maps.addExternalMethodParamPermissions(
                currentExtRefTarget, methodSig.getLeft(), methodSig.getRight(), paramAnnotations
        );
        super.visitCtMethod(method);

        logInfo("Collected annotations for method: " + methodSig + " => " + paramAnnotations, method);
        super.visitCtMethod(method);
    }

}