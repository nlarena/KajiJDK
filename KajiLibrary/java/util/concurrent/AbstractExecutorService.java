package java.util.concurrent;

// The skeleton that turns the one-method {@link Executor} contract into the three `submit`
// overloads of {@link ExecutorService}. A subclass writes `execute` plus the lifecycle
// methods; wrapping a task in a {@link Future} and handing it over is derived here, once.
//
// The interesting seam is {@link #newTaskFor}: `submit` never says `new FutureTask`, it
// asks for a {@link RunnableFuture}. Overriding that one method is how a subclass changes
// what a submitted task *is* — {@code ExecutorCompletionService} would use it to make each
// task announce itself on completion, and a scheduled pool uses it to attach a due-time.
// Without the hook, every such variation would have to reimplement submit.
//
// Subset: invokeAll / invokeAny are not declared by KajiLibrary's ExecutorService, so they
// are absent here too.
//
// Every `newTaskFor` call below passes its type argument explicitly (`this.<T>newTaskFor`).
// Our javac cannot infer a generic method's type argument when the actual argument's static
// type is the *enclosing* method's type variable — it reports "restricciones de tipo
// incompatibles". Spelling the type argument out sidesteps the inference entirely.
public abstract class AbstractExecutorService implements ExecutorService {

    protected AbstractExecutorService() {
    }

    // The RunnableFuture a submitted Callable becomes. Override to change the task type.
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<T>(callable);
    }

    // The RunnableFuture a submitted Runnable becomes, completing with `value`.
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<T>(runnable, value);
    }

    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<T> ftask = this.<T>newTaskFor(task);
        execute(ftask);
        return ftask;
    }

    public <T> Future<T> submit(Runnable task, T result) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<T> ftask = this.<T>newTaskFor(task, result);
        execute(ftask);
        return ftask;
    }

    public Future<?> submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        RunnableFuture<Object> ftask = this.<Object>newTaskFor(task, null);
        execute(ftask);
        return ftask;
    }
}
