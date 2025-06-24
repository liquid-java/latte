import specification.*;
import java.util.LinkedList;

public class LinkedListRefinement {
    @Unique Object o1;

    public Object test(Object obj, Object obj2, @Free Object temp) {
        LinkedList<Object> list = new LinkedList<>();

        list.add(obj);
        list.add(obj);
        list.add(1, obj2);

        list.remove(temp);
    }
}

@ExternalRefinementsFor("java.util.LinkedList")
interface LinkedListRefinements2<T> {

    public void add(@Shared T t);

    public void add(int n, @Shared T t);

    public void remove(@Free T t);

}