package java.util.concurrent;

// Thrown when a thread waiting at a {@link CyclicBarrier} finds the barrier broken —
// because it was reset, or because another waiting party gave up or failed.
public class BrokenBarrierException extends Exception {

    public BrokenBarrierException() {
        super();
    }

    public BrokenBarrierException(String message) {
        super(message);
    }
}
