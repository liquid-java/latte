package context;

import java.lang.annotation.Annotation;

import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtTypedElement;
import spoon.reflect.reference.CtTypeReference;

/**
 * Matched the annotation to the uniqueness enum type type
 */
public class UniquenessAnnotation {
	
	Uniqueness annotation;
	CtElement path;

	public UniquenessAnnotation(CtElement element) {
		for (CtAnnotation<? extends Annotation> ann : element.getAnnotations()) {
	        String an = ann.getActualAnnotation().annotationType().getCanonicalName();
	        if (an.contentEquals("specification.Unique")) {
	           this.annotation = Uniqueness.UNIQUE;
	        } 
	        else if (an.contentEquals("specification.Shared")) {
		       this.annotation = Uniqueness.SHARED;
		    } 
	        else if (an.contentEquals("specification.Borrowed")) {
			   this.annotation = Uniqueness.BORROWED;
			}
			else if (an.contentEquals("specification.Free")) {
				this.annotation = Uniqueness.FREE;
			 }
	        
	    }
		if (element instanceof CtTypedElement){ // TODO: change for typed param here when changing java version
			CtTypedElement<?> typed = (CtTypedElement<?>) element;
			CtTypeReference<?> typeRef = typed.getType();  
			if(typeRef != null && typeRef.isPrimitive())
				this.annotation = Uniqueness.IMMUTABLE;                                                                           
		}
		if (annotation == null) this.annotation = Uniqueness.SHARED; //Default
	}
	
	public static UniquenessAnnotation forPrimitives() {
		return new UniquenessAnnotation(Uniqueness.IMMUTABLE);
	}

	public UniquenessAnnotation(Uniqueness at) {	
		annotation = at;
	}

	public void setToBottom() {
		annotation = Uniqueness.BOTTOM;
	}
	
	public void setToUniquePath(CtElement path) {
		this.annotation = Uniqueness.UNIQUE;
		this.path = path;
	}
	
	public boolean isFree(){
		return annotation.equals(Uniqueness.FREE);
	}

	public boolean isOwned() {
		return annotation.equals(Uniqueness.BORROWED);
	}
	
	public boolean isShared() {
		return annotation.equals(Uniqueness.SHARED);
	}
	
	public boolean isUnique() {
		return annotation.equals(Uniqueness.UNIQUE);
	}
	
	public boolean isBottom() {
		return annotation.equals(Uniqueness.BOTTOM);
	}

	public boolean isImmutable() {
		return annotation.equals(Uniqueness.IMMUTABLE);
	}

	public boolean isLessEqualThan(Uniqueness other) {
		return annotation.isLessEqualThan(other);
	}

	public boolean isGreaterEqualThan(Uniqueness other) {
		return annotation.isGreaterEqualThan(other);
	}

	public boolean annotationEquals(Uniqueness un) {
        return annotation.equals(un);
    }

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof UniquenessAnnotation) {
			UniquenessAnnotation other = (UniquenessAnnotation) obj;
			return annotation.equals(other.annotation);
		}
		return false;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(annotation.name());
		if(path != null)
			sb.append(path.toString());
		return sb.toString();
	}

	/**
	 * In unification, we can unify the annotations if they are the same, or if one of them is free we can
	 * join it to the other one. If none of the above is true, we cannot unify and return bottom.
	 * @param a1
	 * @param a2
	 * @return
	 */
    public static UniquenessAnnotation unifyAnnotation(UniquenessAnnotation a1, UniquenessAnnotation a2) {
		if (a1.annotation.equals(a2.annotation)) 
			return a1;
		else if (a1.isFree() && a2.isLessEqualThan(Uniqueness.FREE))
			return a2;
		else if (a2.isFree() && a1.isLessEqualThan(Uniqueness.FREE))
			return a1;
		else
			return new UniquenessAnnotation(Uniqueness.BOTTOM);
    }



}


