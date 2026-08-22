package java.util.concurrent;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicInteger;

// Minimal java.util.concurrent.CompletableFuture — an async result you can complete once (normally
// OR exceptionally) and chain callbacks onto. Completion publishes the outcome under this object's
// monitor, wakes blocked `get`/`join`, and fires any registered dependents (each maps/consumes the
// value and completes ITS downstream future); a dependent added after completion runs immediately.
// A dependent PROPAGATES an upstream failure: if this future completed exceptionally, the dependent
// fails its downstream future with the same Throwable instead of running the callback. `exceptionally`
// is the recovery point — it turns a failure back into a value. `thenCombine` waits for two futures
// and merges their results. `supplyAsync` runs a task on an `Executor`, capturing any thrown Throwable
// as exceptional completion. (Simplified vs. the JDK: `get` surfaces failure as an unchecked
// RuntimeException rather than a checked ExecutionException; `join` swallows interruption.)
public class CompletableFuture<T> {
    private Object result;
    private Throwable ex; // non-null iff completed exceptionally
    private boolean done;
    private Node dependents; // LIFO stack of actions to fire on completion

    private static final class Node {
        final Runnable action;
        final Node next;

        Node(Runnable action, Node next) {
            this.action = action;
            this.next = next;
        }
    }

    public CompletableFuture() {
    }

    // Shared completion path: record the outcome if not already done, wake waiters, and fire
    // dependents after releasing the monitor. Returns true on the winning call.
    private boolean finish(Object value, Throwable exception) {
        Node toRun;
        synchronized (this) {
            if (done) {
                return false;
            }
            result = value;
            ex = exception;
            done = true;
            toRun = dependents;
            dependents = null;
            notifyAll();
        }
        fire(toRun);
        return true;
    }

    // Complete normally with `value` if not already done. Returns true on the winning call.
    public boolean complete(T value) {
        return finish(value, null);
    }

    // Complete exceptionally with `exception` if not already done. Returns true on the winning call.
    public boolean completeExceptionally(Throwable exception) {
        return finish(null, exception);
    }

    @SuppressWarnings("unchecked")
    public T get() throws InterruptedException {
        synchronized (this) {
            while (!done) {
                wait();
            }
            if (ex != null) {
                throw asUnchecked(ex);
            }
            return (T) result;
        }
    }

    // Like get() but doesn't declare InterruptedException (the fluent-style blocking read).
    public T join() {
        try {
            return get();
        } catch (InterruptedException e) {
            return null;
        }
    }

    public boolean isDone() {
        synchronized (this) {
            return done;
        }
    }

    // Surface a stored failure as an unchecked throwable (no Throwable-cause ctor available here, so
    // a non-RuntimeException failure loses its cause — sufficient for the supported use).
    private static RuntimeException asUnchecked(Throwable t) {
        if (t instanceof RuntimeException) {
            return (RuntimeException) t;
        }
        return new RuntimeException();
    }

    // Register `action` to run when this future completes — immediately if it already has.
    private void whenDone(Runnable action) {
        boolean runNow = false;
        synchronized (this) {
            if (done) {
                runNow = true;
            } else {
                dependents = new Node(action, dependents);
            }
        }
        if (runNow) {
            action.run();
        }
    }

    private static void fire(Node node) {
        while (node != null) {
            node.action.run();
            node = node.next;
        }
    }

    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<U> thenApply(Function<T, U> fn) {
        final CompletableFuture<U> out = new CompletableFuture<U>();
        whenDone(new Runnable() {
            public void run() {
                if (ex != null) {
                    out.completeExceptionally(ex);
                } else {
                    out.complete(fn.apply((T) result));
                }
            }
        });
        return out;
    }

    // Like thenApply, but the function itself returns a CompletableFuture — so this **flattens**
    // (CF<CF<U>> → CF<U>): when this completes, apply `fn` to get an inner future, and complete the
    // returned future with the inner one's result once *it* completes. Composes two async stages.
    @SuppressWarnings("unchecked")
    public <U> CompletableFuture<U> thenCompose(Function<T, CompletableFuture<U>> fn) {
        final CompletableFuture<U> out = new CompletableFuture<U>();
        whenDone(new Runnable() {
            public void run() {
                if (ex != null) {
                    out.completeExceptionally(ex);
                    return;
                }
                final CompletableFuture<U> inner = fn.apply((T) result);
                inner.whenDone(new Runnable() {
                    public void run() {
                        if (inner.ex != null) {
                            out.completeExceptionally(inner.ex); // same-class access to inner's fields
                        } else {
                            out.complete((U) inner.result);
                        }
                    }
                });
            }
        });
        return out;
    }

    public CompletableFuture<Void> thenRun(Runnable action) {
        final CompletableFuture<Void> out = new CompletableFuture<Void>();
        whenDone(new Runnable() {
            public void run() {
                if (ex != null) {
                    out.completeExceptionally(ex);
                } else {
                    action.run();
                    out.complete(null);
                }
            }
        });
        return out;
    }

    // Recovery point: the returned future completes with THIS future's value on success, or with
    // `fn.apply(throwable)` if this future failed. (A failure is turned back into a value.)
    @SuppressWarnings("unchecked")
    public CompletableFuture<T> exceptionally(Function<Throwable, T> fn) {
        final CompletableFuture<T> out = new CompletableFuture<T>();
        whenDone(new Runnable() {
            public void run() {
                if (ex != null) {
                    out.complete(fn.apply(ex));
                } else {
                    out.complete((T) result);
                }
            }
        });
        return out;
    }

    // Wait for BOTH this and `other`; complete the returned future with `fn.apply(thisValue, otherValue)`.
    // If either input fails, the returned future fails with that Throwable. `fn` runs exactly once, when
    // the second of the two completes (an AtomicInteger gate makes the two callbacks converge).
    @SuppressWarnings("unchecked")
    public <U, V> CompletableFuture<V> thenCombine(final CompletableFuture<U> other, final BiFunction<T, U, V> fn) {
        final CompletableFuture<V> out = new CompletableFuture<V>();
        final AtomicInteger remaining = new AtomicInteger(2);
        Runnable gate = new Runnable() {
            public void run() {
                if (remaining.decrementAndGet() != 0) {
                    return; // the other input hasn't completed yet
                }
                if (ex != null) {
                    out.completeExceptionally(ex);
                } else if (other.ex != null) {
                    out.completeExceptionally(other.ex);
                } else {
                    out.complete(fn.apply((T) result, (U) other.result));
                }
            }
        };
        whenDone(gate);
        other.whenDone(gate);
        return out;
    }

    public static <U> CompletableFuture<U> supplyAsync(final Supplier<U> supplier, Executor executor) {
        final CompletableFuture<U> cf = new CompletableFuture<U>();
        executor.execute(new Runnable() {
            public void run() {
                try {
                    cf.complete(supplier.get());
                } catch (Throwable t) {
                    cf.completeExceptionally(t);
                }
            }
        });
        return cf;
    }

    public static CompletableFuture<Void> runAsync(final Runnable command, Executor executor) {
        final CompletableFuture<Void> cf = new CompletableFuture<Void>();
        executor.execute(new Runnable() {
            public void run() {
                try {
                    command.run();
                    cf.complete(null);
                } catch (Throwable t) {
                    cf.completeExceptionally(t);
                }
            }
        });
        return cf;
    }
}
