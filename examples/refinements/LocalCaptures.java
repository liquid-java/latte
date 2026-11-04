import liquidjava.specification.*;

/** Captures stay controlled until the closure, or an object containing it, escapes. */
class LocalCaptures {
    @StateSet({"open", "closed"})
    static class Resource {
        @StateRefinement(to = "open(this)") Resource() {}
        @StateRefinement(from = "open(this)") void read() {}
        @StateRefinement(from = "open(this)", to = "closed(this)") void close() {}
    }

    void localRead() {
        Resource resource = new Resource();
        Runnable callback = () -> resource.read();
        callback.run();
        resource.read();
    }

    void localMutation() {
        Resource resource = new Resource();
        Resource alias = resource;
        Runnable callback = resource::close;
        resource.read();
        callback.run();
        alias.read(); // Expect: STATE
    }

    static void publish(Runnable callback) {}

    void escapingCaptor() {
        Resource resource = new Resource();
        Runnable callback = () -> { Object captured = resource; };
        resource.read();
        publish(callback);
        resource.read(); // Expect: SHARED_REFINEMENT
    }
}
