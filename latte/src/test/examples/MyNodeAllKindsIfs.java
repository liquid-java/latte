package latte;

import specification.Free;
import specification.Unique;

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

    public Object test(@Free Object v1, @Free Object v2, boolean c1, boolean c2){
        if (c1) {
            this.value = v1;
        } else if (c2) {
            this.value = v2;
        } else {
            this.value = v1;
        }

        if (c2 || this.value == null) {
            this.value = v1;
        } else {
            this.value = v2;
        }
        if (c1 && this.value == v1) {
            this.value = v2;
        }
        return this.value;
    }
}
