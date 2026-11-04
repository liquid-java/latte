import liquidjava.specification.*;

interface Nonnegative {
    @Refinement("_ >= 0")
    int apply(@Refinement("_ > 0") int input);
}

interface PositiveSupplier {
    @Refinement("_ > 0") int get();
}

class InheritedAndDeferredContracts implements Nonnegative {
    // The override accepts more inputs and promises a stronger result.
    @Refinement("_ > 0")
    public int apply(@Refinement("_ >= 0") int renamed) { return 1; }

    @Refinement("_ > 0")
    static int positive() { return 5; }

    static void example() {
        PositiveSupplier lambda = () -> 2;
        PositiveSupplier reference = InheritedAndDeferredContracts::positive;
    }
}
