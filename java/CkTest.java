import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiFunction;
import java.util.function.Supplier;

// H6 volume: CompletableFuture.thenCombine — two independent futures run on the pool (CkBox(20) and
// CkBox(22)); thenCombine waits for BOTH and merges their results with a BiFunction that sums them.
// The internal AtomicInteger gate ensures the merge runs exactly once, when the second completes.
// get() returns the combined value. Deterministic → green ≡ os-gil ≡ os = 42.
class CkBox {
    int v;

    CkBox(int v) {
        this.v = v;
    }
}

class CkSupply20 implements Supplier<CkBox> {
    public CkBox get() {
        return new CkBox(20);
    }
}

class CkSupply22 implements Supplier<CkBox> {
    public CkBox get() {
        return new CkBox(22);
    }
}

class CkSum implements BiFunction<CkBox, CkBox, CkBox> {
    public CkBox apply(CkBox a, CkBox b) {
        return new CkBox(a.v + b.v);
    }
}

public class CkTest {
    static int run() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(2);
        CompletableFuture<CkBox> a = CompletableFuture.supplyAsync(new CkSupply20(), pool);
        CompletableFuture<CkBox> b = CompletableFuture.supplyAsync(new CkSupply22(), pool);
        CompletableFuture<CkBox> both = a.thenCombine(b, new CkSum());
        int result = 0;
        try {
            CkBox r = both.get(); // blocks until both futures completed and were merged
            result = r.v; // 42
        } catch (InterruptedException e) {
        }
        pool.shutdown();
        try {
            pool.awaitTermination();
        } catch (InterruptedException e) {
        }
        return result; // 42
    }
}
