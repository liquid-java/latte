import specification.*;

/** No Java fields are cleared by inference: only unobservable ownership edges are forgotten. */
class InferredOwnershipRelease {
    static final class Box {
        @Unique Object value;
        Box(@Unique Object value) { this.value = value; }
    }

    @Unique Box head;

    static void consume(@Free Object value) {}
    static void inspect(@Borrowed Object value) {}

    @Free Object abandonedLocal() {
        Box box = new Box(new Object());
        return box.value; // Accepted: nothing can observe box again.
    }

    @Free Object detachedNode() {
        Box old = head;
        Object value = old.value;
        head = null; // This clear is still necessary: this survives the call.
        return value; // Accepted without old.value = null: old is now unobservable.
    }

    @Free Object survivingContainer() {
        return head.value; // Expect: PERMISSION
    }

    void survivingLocalAlias() {
        Box box = new Box(new Object());
        Box alias = box;
        consume(box.value); // Expect: PERMISSION
        inspect(alias);
    }
}
