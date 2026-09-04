import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

// H6 volume: CompletableFuture pipeline on a ThreadPoolExecutor. supplyAsync runs Supply21 on the
// pool (completes with CfBox(21)); thenApply chains Doubler (→ CfBox(42)); get() blocks until the whole
// chain has run on the pool and returns the final value. A CfBox (plain object) carries the int, so
// no autoboxing is needed. Deterministic → green ≡ os-gil ≡ os = 42.
class CfBox {
    int v;

    CfBox(int v) {
        this.v = v;
    }
}

class Supply21 implements Supplier<CfBox> {
    public CfBox get() {
        return new CfBox(21);
    }
}

class Doubler implements Function<CfBox, CfBox> {
    public CfBox apply(CfBox b) {
        return new CfBox(b.v * 2);
    }
}

public class CfTest {
    static int run() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        CompletableFuture<CfBox> cf = CompletableFuture.supplyAsync(new Supply21(), pool);
        CompletableFuture<CfBox> cf2 = cf.thenApply(new Doubler());
        int result = 0;
        try {
            CfBox b = cf2.get(); // blocks until supply + thenApply ran on the pool
            result = b.v;      // 42
        } catch (InterruptedException e) {
        }
        pool.shutdown();
        try {
            pool.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        return result; // 42
    }
}
