package latte;

import specification.Free;
import specification.Unique;
import specification.Shared;

/*
 * This file is part of the Latte test suite.
 */
class MyNode {

    @Unique Object value;
    @Unique Node next;

    /**
     * Constructor for the Node class using @Free value and next nodes
     * @param value
     * @param next
     */
    public MyNode (@Free Object value, @Free Node next) {
        this.value = value;
        this.next = next;
    }

    public @Unique Object test(@Free Object v1, @Free Object v2, @Free Node n1, boolean c1){
        @Shared MyNode n = new MyNode(v1, n1);
        Object o1 = new Object();
        Node n2 = new Node();

        if (this.test(n, o1, n2)) {
            this.value = v2;
        } else {
            this.value = v2;
        }
        return this.value;
    }

    public boolean test2(@Shared MyNode node, @Free Object o, @Free Node next){
        @Free MyNode mn = new MyNode(o, next);
        node = mn;

        return node == this;
    }
}
