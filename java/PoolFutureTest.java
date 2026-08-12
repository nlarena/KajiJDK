import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.Future;

// H6 volume: submit() + Future.get(). A submitted task sets out[0]=42; the main thread blocks in
// future.get() until the pool has actually run it, then reads the result → 42. Exercises the
// FutureTask completion handshake (run() marks done + notifyAll; get() waits). green ≡ os-gil ≡ os.
class SetTask implements Runnable {
    int[] out;

    public void run() {
        out[0] = 42;
    }
}

public class PoolFutureTest {
    static int run() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(2);
        int[] out = new int[1];
        SetTask t = new SetTask();
        t.out = out;
        Future<?> f = pool.submit(t);
        try {
            f.get(); // block until the task ran
        } catch (InterruptedException e) {
        }
        pool.shutdown();
        try {
            pool.awaitTermination();
        } catch (InterruptedException e) {
        }
        return out[0]; // 42
    }
}
