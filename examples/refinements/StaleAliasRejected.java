import liquidjava.specification.*;

class StaleAliasRejected {
    @StateSet({"open", "closed"})
    static class Resource {
        @StateRefinement(to = "open(this)") Resource() {}
        @StateRefinement(from = "open(this)") void read() {}
        @StateRefinement(from = "open(this)", to = "closed(this)") void close() {}
    }

    void unsafe() {
        Resource original = new Resource();
        Resource alias;
        alias = original;
        alias.close();
        original.read(); // Expect: STATE
    }
}
