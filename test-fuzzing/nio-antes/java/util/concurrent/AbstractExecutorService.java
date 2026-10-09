package java.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
// The batch operations (invokeAll / invokeAny) are derived here too, and for the same reason:
// they are `submit` plus a waiting policy, and every pool would otherwise write the same two
// loops. invokeAny leans on {@link ExecutorCompletionService} rather than polling the futures,
// so "the first one to succeed" costs one blocking take instead of a scan.
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

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>();
        boolean finished = false;
        try {
            Iterator<? extends Callable<T>> it = tasks.iterator();
            while (it.hasNext()) {
                Callable<T> task = it.next();
                RunnableFuture<T> f = this.<T>newTaskFor(task);
                futures.add(f);
                execute(f);
            }
            for (int i = 0; i < futures.size(); i++) {
                Future<T> f = futures.get(i);
                if (!f.isDone()) {
                    try {
                        f.get();
                    } catch (CancellationException e) {
                        // A finished task is a finished task; how it finished is the caller's
                        // business to read off the Future, not ours to react to here.
                    } catch (ExecutionException e) {
                        // Same.
                    }
                }
            }
            finished = true;
        } finally {
            // Only on the abnormal path -- an interrupt, or a task that could not be submitted.
            // Leaving submitted tasks running after throwing would let them outlive the call that
            // created them, with nobody holding their Futures.
            if (!finished) {
                cancelAll(futures);
            }
        }
        return futures;
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
                                         TimeUnit unit) throws InterruptedException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        // The deadline is fixed once, before anything is submitted, and every wait is derived from
        // it. Giving each task its own fresh timeout would let N slow tasks take N times the
        // timeout the caller asked for.
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>();
        boolean finished = false;
        try {
            Iterator<? extends Callable<T>> it = tasks.iterator();
            while (it.hasNext()) {
                Callable<T> task = it.next();
                RunnableFuture<T> f = this.<T>newTaskFor(task);
                futures.add(f);
                execute(f);
            }
            boolean expired = false;
            for (int i = 0; i < futures.size(); i++) {
                Future<T> f = futures.get(i);
                if (!expired && !f.isDone()) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0L) {
                        expired = true;
                    } else {
                        try {
                            f.get(remaining, TimeUnit.NANOSECONDS);
                        } catch (CancellationException e) {
                            // Finished; nothing to decide.
                        } catch (ExecutionException e) {
                            // Finished; nothing to decide.
                        } catch (TimeoutException e) {
                            expired = true;
                        }
                    }
                }
            }
            if (expired) {
                // Cancelled, not dropped: the caller gets a Future per task either way, and a
                // cancelled one says "this did not finish in time" where a missing one says nothing.
                cancelAll(futures);
            }
            finished = true;
        } finally {
            if (!finished) {
                cancelAll(futures);
            }
        }
        return futures;
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        T result;
        try {
            result = this.<T>doInvokeAny(tasks, false, 0L);
        } catch (TimeoutException e) {
            // Unreachable: the untimed call never sets a deadline. Declared only because the shared
            // body has to declare it for the timed caller.
            throw new IllegalStateException("timeout without a deadline");
        }
        return result;
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return this.<T>doInvokeAny(tasks, true, unit.toNanos(timeout));
    }

    /**
     * The body of both invokeAny forms.
     *
     * <p>Results are collected through an {@link ExecutorCompletionService}, so each finished task
     * announces itself and "whichever finished first" is one {@code take} away. The alternative --
     * scanning the futures for one that is done -- either spins or blocks on an arbitrary task while
     * a different one is already finished.
     *
     * <p>A failure is not an answer: the loop keeps taking until a task succeeds or every task has
     * reported. Only when all of them have failed does the last failure become the thrown cause.
     */
    private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks, boolean timed, long nanos)
            throws InterruptedException, ExecutionException, TimeoutException {
        if (tasks == null) {
            throw new NullPointerException();
        }
        int total = tasks.size();
        if (total == 0) {
            throw new IllegalArgumentException("no tasks");
        }
        long deadline = System.nanoTime() + nanos;
        ExecutorCompletionService<T> service = new ExecutorCompletionService<T>(this);
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>();
        T result = null;
        boolean have = false;
        ExecutionException lastFailure = null;
        try {
            Iterator<? extends Callable<T>> it = tasks.iterator();
            while (it.hasNext()) {
                futures.add(service.submit(it.next()));
            }
            int pending = total;
            while (!have && pending > 0) {
                Future<T> done;
                if (timed) {
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0L) {
                        throw new TimeoutException();
                    }
                    done = service.poll(remaining, TimeUnit.NANOSECONDS);
                    if (done == null) {
                        throw new TimeoutException();
                    }
                } else {
                    done = service.take();
                }
                pending = pending - 1;
                try {
                    result = done.get();
                    have = true;
                } catch (ExecutionException e) {
                    lastFailure = e;
                } catch (RuntimeException e) {
                    lastFailure = new ExecutionException(e);
                }
            }
        } finally {
            // Whatever the outcome -- a winner, a timeout, an interrupt -- the tasks still running
            // are of no use to anybody, and invokeAny promises to stop them.
            cancelAll(futures);
        }
        if (!have) {
            if (lastFailure == null) {
                lastFailure = new ExecutionException(new IllegalStateException("no task succeeded"));
            }
            throw lastFailure;
        }
        return result;
    }

    // Cancels every future that has not finished. Written over the raw List because the element
    // type is irrelevant here -- only the Future interface is used.
    private static void cancelAll(List futures) {
        for (int i = 0; i < futures.size(); i++) {
            Future f = (Future) futures.get(i);
            f.cancel(true);
        }
    }
}
