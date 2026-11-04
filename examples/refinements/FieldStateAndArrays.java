import liquidjava.specification.*;

// Field functions describe Java storage, and aliases observe the same updates.
class FieldStateAndArrays {
    int count;

    @StateRefinement(to = "count() == initial")
    FieldStateAndArrays(int initial) { count = initial; }

    @PrivateRefinement("this.count == next")
    @StateRefinement(to = "count() == next")
    void set(int next) { count = next; }

    static void example() {
        FieldStateAndArrays original = new FieldStateAndArrays(2);
        FieldStateAndArrays alias = original;
        alias.set(5);
        @Refinement("_ == 5") int observed = original.count;

        int[] values = {1, 2};
        int[] same = values;
        same[0] = observed;
        @Refinement("_ == 5") int element = values[0];
    }
}
