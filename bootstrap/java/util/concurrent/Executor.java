package java.util.concurrent;

// java.util.concurrent.Executor — the root abstraction: something that runs submitted Runnables,
// decoupling *what* runs from *how/when/which thread* it runs on.
public interface Executor {
    void execute(Runnable command);
}
