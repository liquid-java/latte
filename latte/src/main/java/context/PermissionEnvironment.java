package context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Permission Environment class to store the permissions of the variables in scope
 * Σ ::= ∅ | 𝜈: 𝛼 | 𝜈: ⊥
 */
public class PermissionEnvironment {

    private LinkedList<Map<SymbolicValue, UniquenessAnnotation>> permEnv;

    public PermissionEnvironment() {
        permEnv = new LinkedList<Map<SymbolicValue, UniquenessAnnotation>>(); 
    }

    /**
     * Add a new symbolic value to the environment
     * @param symb
     * @param ann
     */
    public void add(SymbolicValue symb, UniquenessAnnotation ann) {
        permEnv.getFirst().put(symb, ann);
    }

    /**
     * Removes the symbolic value in the key from the environment
     * @param symb
     */
    private void remove(SymbolicValue symb) {
        for (Map<SymbolicValue, UniquenessAnnotation> innerMap : permEnv) {
            if (innerMap.containsKey(symb)) {
                innerMap.remove(symb);
                return;
            }
        }
    }

    /**
     * Get the permission of the symbolic value
     * @param symb
     * @return the permission of the symbolic value or null if it does not exist
     */
    public UniquenessAnnotation get(SymbolicValue symb) {
        for (Map<SymbolicValue, UniquenessAnnotation> innerMap : permEnv) {
            if (innerMap.containsKey(symb)) {
                return innerMap.get(symb);
            }
        }
        return null;
    }


    /**
     * Get all symbolic values with Unique as the permission
     * @return
     */
    List<SymbolicValue> getUniqueValues() {
        List<SymbolicValue> values = new ArrayList<SymbolicValue>();
        permEnv.forEach( innerMap -> {
            innerMap.keySet().forEach(key -> {
                if (innerMap.get(key).isUnique()) {
                    values.add(key);
                }
            });
        });
        return values;
    }

    /**
     * Remove the values of the given list from the environment
     * @param removed
     */
    void removeValues(List<SymbolicValue> removed) {
        for (SymbolicValue symbolicValue : removed) 
            remove(symbolicValue);
    }

    /**
	 * Enter a new scope
	 */
	public void enterScope() {
		permEnv.addFirst(new HashMap<SymbolicValue, UniquenessAnnotation>());
	}
	
	/**
	 * Exit the current scope
	 */
	public void exitScope() {
		permEnv.removeFirst();
	}

    /**
     * Uses the current permission of a symbolic value as another permission
     * @return returns true if the permission of vvPerm can be used as fieldPerm
     */
    public boolean usePermissionAs(SymbolicValue v, UniquenessAnnotation vPerm, UniquenessAnnotation expectedPerm) {
        switch (expectedPerm.annotation) {
            case FREE:
                if (vPerm.isGreaterEqualThan(Uniqueness.FREE)) {
                    add(v, new UniquenessAnnotation(Uniqueness.BOTTOM));
                    return true;
                }                
                break;
            case BORROWED:
                if (vPerm.isGreaterEqualThan(Uniqueness.BORROWED))
                    return true;
            case UNIQUE:
                if (vPerm.isGreaterEqualThan(Uniqueness.FREE)){
                    add(v, new UniquenessAnnotation(Uniqueness.UNIQUE));
                    return true;
                } else if (vPerm.annotationEquals(Uniqueness.UNIQUE)){
                    return true;
                }
                break;
            case IMMUTABLE:
                if (vPerm.isGreaterEqualThan(Uniqueness.IMMUTABLE)){
                    return true;
                }
                break;
            case SHARED:
                if (vPerm.annotationEquals(Uniqueness.SHARED) || vPerm.annotationEquals(Uniqueness.FREE)){
                    add(v, new UniquenessAnnotation(Uniqueness.SHARED));
                    return true;
                }
            case BOTTOM:
                add(v, vPerm);
                return true;
        }
        return false;
    }

    /**
	 * Clone the permission environment at a certain moment in time
	 * @return
	 */
	public PermissionEnvironment cloneLast() {
        PermissionEnvironment clone = new PermissionEnvironment();
        // clone.enterScope();
        // map.getFirst().forEach((k, v) -> {
        //     clone.add(k, v);
        // });
        for (Map<SymbolicValue, UniquenessAnnotation> innerMap : permEnv) {
            Map<SymbolicValue, UniquenessAnnotation> newMap = new HashMap<>();
            innerMap.forEach((k, v) -> {
                newMap.put(k, v);
            });
            clone.permEnv.add(newMap);
        }
        return clone;
    }

    /**
     * String representation of the Permission Environment
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Symbolic Value to Uniqueness Annotations:\n");

        for (int i = 0; i < permEnv.size(); i++) {
            Map<SymbolicValue, UniquenessAnnotation> currentMap = permEnv.get(i);
            sb.append("  Map ").append(i + 1).append(":\n");

            for (Map.Entry<SymbolicValue, UniquenessAnnotation> entry : currentMap.entrySet()) {
                sb.append("    ")
                .append(entry.getKey().toString()) // Key
                .append(" -> ")
                .append(entry.getValue().toString()) // Value
                .append("\n");
            }
        }

        return sb.toString();
    }
}
