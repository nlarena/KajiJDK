import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

// H6 volume: CompletableFuture.thenCompose — flattens CF<CF<U>> → CF<U>. supplyAsync produces
// CcBox(21); thenCompose maps it to ANOTHER async future (on the pool) that doubles it to CcBox(42);
// get() returns the flattened 42 (not a nested future). green ≡ os-gil ≡ os.
class CcBox {
    int v;

    CcBox(int v) {
        this.v = v;
    }
}

class CcSupply21 implements Supplier<CcBox> {
    public CcBox get() {
        return new CcBox(21);
    }
}

class CcDoubleSupplier implements Supplier<CcBox> {
    int v;

    CcDoubleSupplier(int v) {
        this.v = v;
    }

    public CcBox get() {
        return new CcBox(v * 2);
    }
}

class CcAsyncDoubler implements Function<CcBox, CompletableFuture<CcBox>> {
    ThreadPoolExecutor pool;

    public CompletableFuture<CcBox> apply(CcBox b) {
        return CompletableFuture.supplyAsync(new CcDoubleSupplier(b.v), pool);
    }
}

public class CcComposeTest {
    static int run() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        CcAsyncDoubler dbl = new CcAsyncDoubler();
        dbl.pool = pool;
        CompletableFuture<CcBox> cf = CompletableFuture.supplyAsync(new CcSupply21(), pool);
        CompletableFuture<CcBox> cf2 = cf.thenCompose(dbl);
        int result = 0;
        try {
            CcBox b = cf2.get();
            result = b.v; // 42
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
