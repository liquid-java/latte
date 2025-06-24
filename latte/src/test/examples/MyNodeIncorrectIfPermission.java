package latte;

import specification.Free;
import specification.Unique;
import specification.Shared;
import specification.Borrowed;

class MyNodeIfNoElse {

    @Unique Object value;

    public @Unique Object test(@Shared Object v1, boolean cond) {
        Object n;
        n = new Object();

        this.value = n;
        if (cond) {
            this.value = v1;
        }

        return this.value;
    }
}