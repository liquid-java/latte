import liquidjava.specification.*;

/** Latte joins aliases without retaining the guard that selected each possible object. */
class BranchOwnershipLimit {
    @StateSet({"open", "closed"})
    static class Resource {
        @StateRefinement(to = "open(this)") Resource() {}
        @StateRefinement(from = "open(this)") void read() {}
    }

    void joined(boolean chooseLocal, Resource shared) {
        Resource local = new Resource();
        Resource selected = chooseLocal ? local : shared;
        if (chooseLocal) {
            selected.read(); // Expect: SHARED_REFINEMENT
        }
    }

    void keptInsideBranch(boolean chooseLocal, Resource shared) {
        Resource local = new Resource();
        if (chooseLocal) {
            Resource selected = local;
            selected.read();
        }
    }
}
