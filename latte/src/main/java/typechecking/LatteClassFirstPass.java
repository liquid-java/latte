package typechecking;

import context.ClassLevelMaps;
import context.PermissionEnvironment;
import context.SymbolicEnvironment;
import context.UniquenessAnnotation;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

public class LatteClassFirstPass extends LatteAbstractChecker{

    public LatteClassFirstPass(SymbolicEnvironment se, PermissionEnvironment pe,
            ClassLevelMaps mtc) {
        super(se, pe, mtc);
        logInfo("[ First Class Pass started ]");
        enterScopes();
    }

    @Override
    public <T> void visitCtClass(CtClass<T> ctClass) {
		logInfo("Visiting class: " + ctClass.getSimpleName(), ctClass);
		// Add the class to the type reference and class map
		CtTypeReference<?> typeRef = ctClass.getReference();
		maps.addTypeClass(typeRef, ctClass);
		super.visitCtClass(ctClass);
	}
			
			
	@Override
	public <T> void visitCtField(CtField<T> f) {
		logInfo("Visiting field: " + f.getSimpleName(), f);
		loggingSpaces++;
		CtElement k = f.getParent();
		if (k instanceof CtClass){
			CtClass<?> klass = (CtClass<?>) k;
			maps.addFieldClass(f, klass);
			logInfo(String.format("Added field %s to class %s in the mappings", f.getSimpleName(), klass.getSimpleName()));

			UniquenessAnnotation ua = maps.getFieldAnnotation(f.getSimpleName(), klass.getReference());
			logInfo(String.format("Field %s has annotation %s in mapping", f.getSimpleName(), ua));

		} else {
			logError(String.format("Field %s is not inside a class", f.getSimpleName()), f);
		}

		super.visitCtField(f);
		loggingSpaces--;
	}


	@Override
	public <T> void visitCtMethod(CtMethod<T> m) {
		CtTypeReference<?> type = ((CtType<?>) m.getParent()).getTypeErasure();
		CtType<?> parentType = (CtType<?>) m.getParent();
		if(parentType instanceof CtInterface){
			CtAnnotation<?> ann = parentType.getAnnotation(
					parentType.getFactory().Type().createReference("specification.ExternalRefinementsFor")
			);
			if (ann != null) {
				CtExpression<?> expr = ann.getValues().get("value");

				if (expr instanceof CtLiteral<?> && ((CtLiteral<?>) expr).getValue() instanceof String) {
					String refinedClassName = (String) ((CtLiteral<?>) expr).getValue();
					CtTypeReference<?> refinedTypeRef = parentType.getFactory().Type().createReference(refinedClassName);
					if(maps.hasExternalMethodParamPermissions(refinedTypeRef, m.getSimpleName(), m.getParameters().size()))
						logInfo("External refinement match found for: " + m.getSimpleName() + " in refined class: " + refinedClassName);
						return;
				}
			}
		}
		logInfo("Visiting method: " + m.getSimpleName(), m);
		maps.addMethod((CtClass<?>) m.getParent(), m);
		super.visitCtMethod(m);
	}

	@Override
	public <T> void visitCtConstructor(CtConstructor<T> c) {
		logInfo("Visiting constructor: " + c.getSimpleName(), c);
		maps.addConstructor((CtClass<?>) c.getParent(), c);
		super.visitCtConstructor(c);
	}


}
