import liquidjava.specification.*;

/** All aliases remain visible in this method; they share one typestate. */
class LocalAliases {
    @StateSet({"open", "closed"})
    static class Resource {
        @StateRefinement(to = "open(this)") Resource() {}
        @StateRefinement(from = "open(this)") void read() {}
        @StateRefinement(from = "open(this)", to = "closed(this)") void close() {}
    }

    void use() {
        Resource original = new Resource();
        @Refinement("_ != null") Resource alias = original;
        alias.read();
        original.close();
    }
}
