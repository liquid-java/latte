package examples;

import specification.Free;
import specification.Unique;


class PQNode {
    // @Refinement("min(this) == value")
    PQNode(int value, @Free PQNode next) {
        this.value = value;
        this.next = next;
    }

    int value;

    //@Refinement("this.next == null || min(this.next) < this.value")
    @Unique PQNode next;

    // @Refinement(@Borrowed PQNode this, "min(this) <= v")
    void insert(int v) {
        if (v < this.value) {
            PQNode nxt = this.next;
            this.next = null;
            PQNode newNode = new PQNode(this.value, nxt);
            this.value = v; 
            this.next = newNode; // note: if we swap this with the statement above, the refinement is briefly violated
        } else {
            next.insert(v);
        }
    }
}
