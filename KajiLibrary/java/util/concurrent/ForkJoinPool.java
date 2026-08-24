package java.util.concurrent;

public class ForkJoinPool extends AbstractExecutorService {
    public void execute(Runnable command) {
    }
    public void shutdown() {
    }
    public boolean isShutdown() {
        return false;
    }
    public boolean isTerminated() {
        return false;
    }
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return false;
    }
    void pushTask(ForkJoinTask task) {
    }
    public static ForkJoinPool commonPool() {
        return null;
    }
    int nextWorkerIndex() {
        return 0;
    }
    void runWorker(ForkJoinWorkerThread w) {
    }
}
