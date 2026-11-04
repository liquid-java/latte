import liquidjava.specification.*;
import specification.*;

/** Constructors preserve stored argument aliases and consume arguments declared @Free. */
class ConstructorAliases {
    @StateSet({"open", "closed"})
    static class Resource {
        @StateRefinement(to = "open(this)") Resource() {}
        @StateRefinement(from = "open(this)") void read() {}
        @StateRefinement(from = "open(this)", to = "closed(this)") void close() {}
    }

    static class Box {
        @Unique Resource value;
        Box(@Unique Resource value) { this.value = value; }
        @Borrowed Resource get() { return value; }
    }

    static class Owner {
        @Unique Resource value;
        Owner(@Free Resource value) { this.value = value; }
        @Borrowed Resource get() { return value; }
    }

    void storedAlias() {
        Resource original = new Resource();
        Box box = new Box(original);
        box.get().read();
        box.get().close();
        original.read(); // Expect: STATE
    }

    void transferredOwnership() {
        Resource original = new Resource();
        Owner owner = new Owner(original);
        owner.get().read();
        owner.get().close();
        Object stale = original; // Expect: PERMISSION
    }
}
