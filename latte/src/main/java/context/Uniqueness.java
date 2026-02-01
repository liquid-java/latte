package context;

public enum Uniqueness {
    BOTTOM (1),
    SHARED (2),
    UNIQUE (3),
    BORROWED (4),
    IMMUTABLE(5),
    FREE (6);
    // ALIAS (6),

    private final int order;

    Uniqueness(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }

    // Custom comparison method
    public boolean isLessEqualThan(Uniqueness other) {
        return this.order <= other.order;
    }
    // Custom comparison method
    public boolean isGreaterEqualThan(Uniqueness other) {
        if((this == UNIQUE && other == BORROWED) || (other == UNIQUE && this == BORROWED)) 
            return true;
        return this.order >= other.order;
    }


}
