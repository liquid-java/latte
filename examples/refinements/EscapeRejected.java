import java.util.Properties;
import liquidjava.specification.Refinement;

class EscapeRejected {
    void unsafe() {
        Properties local = new Properties();
        Properties alias = local;
        System.setProperties(local);
        @Refinement("_ != null") Object escaped = alias; // Expect: SHARED_REFINEMENT
    }
}
