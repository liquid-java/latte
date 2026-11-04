import liquidjava.specification.*;

class ControlFlowAndFloatingPoint {
    @Refinement("_ == 3")
    static int cleanup() {
        try { return 1; }
        finally { return 3; }
    }

    static void example() {
        @Refinement("_ != 0.3") double rounded = 0.1 + 0.2;
        @Refinement("_ != _") double nan = Double.NaN;
        int count = 0;
        while (count < 10) count++;
        @Refinement("_ == 10") int completed = count;
        int selected = switch (completed) {
            case 10 -> cleanup();
            default -> 0;
        };
        @Refinement("_ == 3") int result = selected;
    }
}
