import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

// H6 volume: CompletableFuture.exceptionally — the supplier THROWS on the pool, so supplyAsync
// completes the future exceptionally (captures the Throwable). exceptionally() is the recovery
// point: it sees the failure and turns it back into CeBox(99). get() therefore returns the
// recovered value, never throwing. Deterministic → green ≡ os-gil ≡ os = 99.
class CeBox {
    int v;

    CeBox(int v) {
        this.v = v;
    }
}

class CeFail implements Supplier<CeBox> {
    public CeBox get() {
        throw new RuntimeException(); // fails while running on the pool
    }
}

class CeRecover implements Function<Throwable, CeBox> {
    public CeBox apply(Throwable t) {
        return new CeBox(99); // recover the failure to a value
    }
}

public class CeTest {
    static int run() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        CompletableFuture<CeBox> cf = CompletableFuture.supplyAsync(new CeFail(), pool);
        CompletableFuture<CeBox> recovered = cf.exceptionally(new CeRecover());
        int result = 0;
        try {
            CeBox b = recovered.get(); // failure captured on the pool, recovered to 99
            result = b.v;
        } catch (InterruptedException e) {
        }
        pool.shutdown();
        try {
            pool.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        return result; // 99
    }
}
