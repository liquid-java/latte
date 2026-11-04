import liquidjava.specification.*;

/** Known slots retain identities; symbolic indices preserve the conditional state of each object. */
class ReferenceArrayAliases {
    @StateSet({"open", "closed"})
    static class Resource {
        @StateRefinement(to = "open(this)") Resource() {}
        @StateRefinement(from = "open(this)") void read() {}
        @StateRefinement(from = "open(this)", to = "closed(this)") void close() {}
    }

    void separateSlots() {
        Resource first = new Resource();
        Resource second = new Resource();
        Resource[] box = new Resource[2];
        box[0] = first;
        box[1] = second;
        Resource alias = box[0];
        alias.close();
        second.read();
        first.read(); // Expect: STATE
    }

    void symbolicIndex(@Refinement("_ >= 0 && _ < 2") int i) {
        Resource first = new Resource();
        Resource second = new Resource();
        Resource[] box = {first, second};
        box[i].close();
        if (i != 0) first.read();
        if (i != 1) second.read();
        first.read(); // Expect: STATE
    }

    void overwrite() {
        Resource first = new Resource();
        Resource second = new Resource();
        Resource[] box = {first};
        Resource saved = box[0];
        box[0] = second;
        saved.close();
        box[0].read();
        first.read(); // Expect: STATE
    }
}
