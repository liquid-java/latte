import liquidjava.specification.Refinement;
import specification.*;

class BorrowedAndUnique {
    @Unique @Refinement("_ != null") Object owned;

    BorrowedAndUnique(@Free @Refinement("_ != null") Object value) {
        owned = value;
    }

    void borrow(@Borrowed @Refinement("_ != null") Object value) {
        @Refinement("_ != null") Object localAlias = value;
    }

    void use() {
        @Refinement("_ != null") Object local = new Object();
        borrow(local);
        @Refinement("_ != null") Object stillLocal = local;
    }
}
