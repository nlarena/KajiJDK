package java.lang;

// The functional interface behind `Thread(Runnable)` and every `() -> {...}` task. A
// single abstract method, so a lambda can implement it — which is exactly how the common
// `new Thread(() -> ...)` pattern works: the lambda is the target the thread runs.
@FunctionalInterface
public interface Runnable {
    void run();
}
