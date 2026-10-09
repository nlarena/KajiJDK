package java.util.concurrent;

// Decouples *submitting* work from *running* it: the caller hands over a Runnable and the
// executor decides on which thread, and when, it runs. Everything else in this package's
// execution framework is a refinement of this one method.
public interface Executor {

    void execute(Runnable command);
}
