import specification.*;
import java.util.LinkedList;

public class LinkedListRefinementIncorrect {
    @Unique Object o1;

    public Object test(@Free Object obj) {
        Object o2 = o1;
        LinkedList<Object> list = new LinkedList<>();

        list.add(obj);
        list.add(o2);
    }
}

@ExternalRefinementsFor("java.util.LinkedList")
interface LinkedListRefinements<T> {

    public void add(@Free T t);

    public void add(int n, T t);

    public void remove(@Free T t);
}