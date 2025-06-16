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

    public void test(@Free Object v1, boolean c1){
        if (boolRead(c1)) {
            this.value = v1;
        } else {
            this.value = null;
        }
    }

    private boolean boolRead(boolean b){
        return b;
    }
}
