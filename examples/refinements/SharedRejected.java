import liquidjava.specification.Refinement;
import specification.Shared;

class SharedRejected {
    void unsafe(@Shared @Refinement("_ != null") Object shared) {} // Expect: SHARED_REFINEMENT
}
