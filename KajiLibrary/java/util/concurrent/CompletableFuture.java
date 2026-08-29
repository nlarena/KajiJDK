package java.util.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

// A future you can complete yourself, and chain work onto. It is both halves at once: a
// {@link Future}, so a thread may block for the value, and a {@link CompletionStage}, so a
// caller may instead register what should happen when the value arrives and never block at
// all. The second half is the reason it exists — a pipeline of stages occupies no thread
// while it waits.
//
// The whole machine is three things:
//
//   * one `result` slot, null while pending. A successful null is stored as the sentinel
//     NIL and a failure as a CfFailure box, so "pending", "completed with null" and
//     "completed exceptionally" stay three distinguishable states in a single field;
//   * a list of dependent steps, drained exactly once when the slot is filled;
//   * {@link CfStep}, one class that carries out every combinator. The forty-odd public
//     methods differ only in an opcode and which function object they hold, so writing
//     forty near-identical inner classes would be repetition, not clarity.
//
// Completion is idempotent: the first `complete`/`completeExceptionally`/`cancel` wins and
// the rest return false. Dependents are fired outside the monitor — a dependent's action is
// arbitrary user code, and running it under the lock that every other stage needs would
// serialise the whole graph and invite deadlock.
//
// What is deliberately simpler than HotSpot: there is no work-stealing pool underneath, no
// lock-free Treiber stack of completions, and no stack-overflow trampolining for deep
// chains. The default executor is a thread-per-task executor. None of that is observable
// through the public surface, which is what has to be faithful.
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {

    // Stands in for a successful null, so that `result != null` means "settled".
    static final Object NIL = new Object();

    // Opcodes for CfStep. One class, one switch, instead of one class per combinator.
    static final int APPLY = 1;
    static final int ACCEPT = 2;
    static final int RUN = 3;
    static final int COMBINE = 4;
    static final int ACCEPT_BOTH = 5;
    static final int RUN_AFTER_BOTH = 6;
    static final int APPLY_EITHER = 7;
    static final int ACCEPT_EITHER = 8;
    static final int RUN_AFTER_EITHER = 9;
    static final int COMPOSE = 10;
    static final int HANDLE = 11;
    static final int WHEN = 12;
    static final int RECOVER = 13;
    static final int RELAY = 14;

    private static Executor commonExecutor;

    private final Object lock = new Object();
    private Object result;
    private boolean cancelled;
    private List<Runnable> dependents = new ArrayList<Runnable>();

    public CompletableFuture() {
    }

    // ---------------------------------------------------------------- internals

    // The raw slot, sentinel and failure box included. Package-private: CfStep reads it.
    final Object rawResult() {
        Object r;
        synchronized (lock) {
            r = result;
        }
        return r;
    }

    // Fill the slot if it is still empty, and drain the dependents. Returns whether this
    // call is the one that settled the future.
    final boolean settle(Object raw) {
        List<Runnable> toFire = null;
        synchronized (lock) {
            if (result == null) {
                result = raw;
                toFire = dependents;
                dependents = null;
                lock.notifyAll();
            }
        }
        if (toFire != null) {
            int i = 0;
            while (i < toFire.size()) {
                Runnable step = toFire.get(i);
                step.run();
                i = i + 1;
            }
        }
        return toFire != null;
    }

    // Register `step` to run when this future settles — or run it now if it already has.
    // The decision is made under the monitor, the run happens outside it.
    final void onSettled(Runnable step) {
        boolean already;
        synchronized (lock) {
            already = result != null;
            if (!already) {
                dependents.add(step);
            }
        }
        if (already) {
            step.run();
        }
    }

    // The failure carried by a raw slot, or null if it holds a value.
    static Throwable failureOf(Object raw) {
        Throwable t = null;
        if (raw instanceof CfFailure) {
            CfFailure box = (CfFailure) raw;
            t = box.cause;
        }
        return t;
    }

    // The value carried by a raw slot, undoing the NIL sentinel.
    static Object valueOf(Object raw) {
        Object v = raw;
        if (raw == NIL || raw instanceof CfFailure) {
            v = null;
        }
        return v;
    }

    // Box a value for the slot.
    static Object boxValue(Object v) {
        Object raw = v;
        if (v == null) {
            raw = NIL;
        }
        return raw;
    }

    // Attach a step, wiring it to whichever sources it needs.
    private <U> CompletableFuture<U> chain(int kind, Object fn, CompletableFuture other,
                                           Executor executor) {
        CompletableFuture<U> out = this.<U>newIncompleteFuture();
        CfStep step = new CfStep(kind, this, other, out, fn, executor);
        if (other != null) {
            other.onSettled(step);
        }
        this.onSettled(step);
        return out;
    }

    private Executor asyncExecutor(Executor executor) {
        Executor e = executor;
        if (e == null) {
            e = defaultExecutor();
        }
        return e;
    }

    // ---------------------------------------------------------------- factories

    public static <U> CompletableFuture<U> completedFuture(U value) {
        CompletableFuture<U> f = new CompletableFuture<U>();
        f.settle(boxValue(value));
        return f;
    }

    public static <U> CompletableFuture<U> failedFuture(Throwable ex) {
        CompletableFuture<U> f = new CompletableFuture<U>();
        f.settle(new CfFailure(ex));
        return f;
    }

    public static <U> CompletionStage<U> completedStage(U value) {
        return CompletableFuture.<U>completedFuture(value);
    }

    public static <U> CompletionStage<U> failedStage(Throwable ex) {
        return CompletableFuture.<U>failedFuture(ex);
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return CompletableFuture.<U>supplyAsync(supplier, null);
    }

    public static <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier, Executor executor) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        CompletableFuture<U> out = new CompletableFuture<U>();
        Executor e = executor;
        if (e == null) {
            e = defaultCommonExecutor();
        }
        e.execute(new CfSupplyTask(out, supplier));
        return out;
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable) {
        return runAsync(runnable, null);
    }

    public static CompletableFuture<Void> runAsync(Runnable runnable, Executor executor) {
        if (runnable == null) {
            throw new NullPointerException();
        }
        CompletableFuture<Void> out = new CompletableFuture<Void>();
        Executor e = executor;
        if (e == null) {
            e = defaultCommonExecutor();
        }
        e.execute(new CfRunTask(out, runnable));
        return out;
    }

    // Every stage of this class runs on threads from here unless the caller names an
    // executor. A thread per task: honest, and the only thing this runtime can offer
    // without a work-stealing pool underneath.
    static Executor defaultCommonExecutor() {
        Executor e;
        synchronized (CompletableFuture.NIL) {
            if (commonExecutor == null) {
                commonExecutor = new CfThreadPerTaskExecutor();
            }
            e = commonExecutor;
        }
        return e;
    }

    public Executor defaultExecutor() {
        return defaultCommonExecutor();
    }

    // The hook subclasses override so that combinators keep returning *their* type.
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return new CompletableFuture<U>();
    }

    // ---------------------------------------------------------------- settling

    public boolean complete(T value) {
        return settle(boxValue(value));
    }

    public boolean completeExceptionally(Throwable ex) {
        if (ex == null) {
            throw new NullPointerException();
        }
        return settle(new CfFailure(ex));
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean won = settle(new CfFailure(new CancellationException()));
        if (won) {
            synchronized (lock) {
                cancelled = true;
            }
        }
        return isCancelled();
    }

    // Overwrite the outcome regardless of what is already there — a debugging tool, and the
    // only way to break out of an erroneously completed future.
    public void obtrudeValue(T value) {
        synchronized (lock) {
            result = boxValue(value);
            cancelled = false;
        }
        settle(boxValue(value));
    }

    public void obtrudeException(Throwable ex) {
        if (ex == null) {
            throw new NullPointerException();
        }
        CfFailure box = new CfFailure(ex);
        synchronized (lock) {
            result = box;
            cancelled = false;
        }
        settle(box);
    }

    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier) {
        return completeAsync(supplier, null);
    }

    public CompletableFuture<T> completeAsync(Supplier<? extends T> supplier, Executor executor) {
        if (supplier == null) {
            throw new NullPointerException();
        }
        Executor e = asyncExecutor(executor);
        e.execute(new CfSupplyTask(this, supplier));
        return this;
    }

    // Arm a timer that fails this future if nothing else has settled it first.
    public CompletableFuture<T> orTimeout(long timeout, TimeUnit unit) {
        long millis = unit.toMillis(timeout);
        CfTimeout timer = new CfTimeout(this, millis, false, null);
        timer.arm();
        return this;
    }

    public CompletableFuture<T> completeOnTimeout(T value, long timeout, TimeUnit unit) {
        long millis = unit.toMillis(timeout);
        CfTimeout timer = new CfTimeout(this, millis, true, value);
        timer.arm();
        return this;
    }

    public static Executor delayedExecutor(long delay, TimeUnit unit) {
        return delayedExecutor(delay, unit, null);
    }

    public static Executor delayedExecutor(long delay, TimeUnit unit, Executor executor) {
        long millis = unit.toMillis(delay);
        return new CfDelayedExecutor(millis, executor);
    }

    // ---------------------------------------------------------------- reading

    public boolean isDone() {
        return rawResult() != null;
    }

    public boolean isCancelled() {
        boolean c;
        synchronized (lock) {
            c = cancelled;
        }
        return c;
    }

    public boolean isCompletedExceptionally() {
        return failureOf(rawResult()) != null;
    }

    // Block until settled. No `throws` clause: restating a classpath interface's throws is
    // rejected (finding #104) and the descriptor is the same without it.
    private Object await() {
        Object r;
        synchronized (lock) {
            while (result == null) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
            }
            r = result;
        }
        return r;
    }

    public T get() throws ExecutionException {
        Object r = await();
        Throwable ex = failureOf(r);
        if (ex != null) {
            throw new ExecutionException(ex);
        }
        return (T) valueOf(r);
    }

    public T get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {
        long millis = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + millis;
        Object r;
        synchronized (lock) {
            long remaining = millis;
            while (result == null && remaining > 0L) {
                try {
                    lock.wait(remaining);
                } catch (InterruptedException e) {
                    throw new CompletionException(e);
                }
                remaining = deadline - System.currentTimeMillis();
            }
            r = result;
        }
        if (r == null) {
            throw new TimeoutException();
        }
        Throwable ex = failureOf(r);
        if (ex != null) {
            throw new ExecutionException(ex);
        }
        return (T) valueOf(r);
    }

    // Same wait as get(), but failures come out unchecked — the form a pipeline wants.
    public T join() {
        Object r = await();
        Throwable ex = failureOf(r);
        if (ex != null) {
            throw wrap(ex);
        }
        return (T) valueOf(r);
    }

    public T getNow(T valueIfAbsent) {
        Object r = rawResult();
        T v = valueIfAbsent;
        if (r != null) {
            Throwable ex = failureOf(r);
            if (ex != null) {
                throw wrap(ex);
            }
            v = (T) valueOf(r);
        }
        return v;
    }

    public T resultNow() {
        Object r = rawResult();
        if (r == null || failureOf(r) != null) {
            throw new IllegalStateException("future has no result");
        }
        return (T) valueOf(r);
    }

    public Throwable exceptionNow() {
        Object r = rawResult();
        Throwable ex = failureOf(r);
        if (ex == null) {
            throw new IllegalStateException("future did not complete exceptionally");
        }
        return ex;
    }

    public int getNumberOfDependents() {
        int n;
        synchronized (lock) {
            if (dependents == null) {
                n = 0;
            } else {
                n = dependents.size();
            }
        }
        return n;
    }

    // A CancellationException passes through unwrapped, as the JDK does; anything else is
    // boxed once so the caller can always ask for the cause.
    static CompletionException wrap(Throwable ex) {
        CompletionException wrapped;
        if (ex instanceof CompletionException) {
            wrapped = (CompletionException) ex;
        } else {
            wrapped = new CompletionException(ex);
        }
        return wrapped;
    }

    public String toString() {
        Object r = rawResult();
        String state;
        if (r == null) {
            state = "[Not completed]";
        } else if (failureOf(r) != null) {
            state = "[Completed exceptionally]";
        } else {
            state = "[Completed normally]";
        }
        return "java.util.concurrent.CompletableFuture" + state;
    }

    // ---------------------------------------------------------------- views

    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    // An independent future that settles the same way this one does — a defensive handoff,
    // so a caller cannot complete *your* future.
    public CompletableFuture<T> copy() {
        return this.<T>chain(RELAY, null, null, null);
    }

    // A stage that refuses to be blocked on or completed: only the CompletionStage half.
    public CompletionStage<T> minimalCompletionStage() {
        return this.<T>chain(RELAY, null, null, null);
    }

    // ---------------------------------------------------------------- value in, value out

    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        return this.<U>chain(APPLY, fn, null, null);
    }

    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
        return this.<U>chain(APPLY, fn, null, defaultExecutor());
    }

    public <U> CompletableFuture<U> thenApplyAsync(Function<? super T, ? extends U> fn,
                                                   Executor executor) {
        return this.<U>chain(APPLY, fn, null, asyncExecutor(executor));
    }

    // ---------------------------------------------------------------- value in, nothing out

    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        return this.<Void>chain(ACCEPT, action, null, null);
    }

    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action) {
        return this.<Void>chain(ACCEPT, action, null, defaultExecutor());
    }

    public CompletableFuture<Void> thenAcceptAsync(Consumer<? super T> action,
                                                   Executor executor) {
        return this.<Void>chain(ACCEPT, action, null, asyncExecutor(executor));
    }

    // ---------------------------------------------------------------- nothing in, nothing out

    public CompletableFuture<Void> thenRun(Runnable action) {
        return this.<Void>chain(RUN, action, null, null);
    }

    public CompletableFuture<Void> thenRunAsync(Runnable action) {
        return this.<Void>chain(RUN, action, null, defaultExecutor());
    }

    public CompletableFuture<Void> thenRunAsync(Runnable action, Executor executor) {
        return this.<Void>chain(RUN, action, null, asyncExecutor(executor));
    }

    // ---------------------------------------------------------------- both must settle

    public <U, V> CompletableFuture<V> thenCombine(CompletionStage<? extends U> other,
                                                   BiFunction<? super T, ? super U, ? extends V> fn) {
        return this.<V>chain(COMBINE, fn, stageOf(other), null);
    }

    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                        BiFunction<? super T, ? super U, ? extends V> fn) {
        return this.<V>chain(COMBINE, fn, stageOf(other), defaultExecutor());
    }

    public <U, V> CompletableFuture<V> thenCombineAsync(CompletionStage<? extends U> other,
                                                        BiFunction<? super T, ? super U, ? extends V> fn,
                                                        Executor executor) {
        return this.<V>chain(COMBINE, fn, stageOf(other), asyncExecutor(executor));
    }

    public <U> CompletableFuture<Void> thenAcceptBoth(CompletionStage<? extends U> other,
                                                      BiConsumer<? super T, ? super U> action) {
        return this.<Void>chain(ACCEPT_BOTH, action, stageOf(other), null);
    }

    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                                                           BiConsumer<? super T, ? super U> action) {
        return this.<Void>chain(ACCEPT_BOTH, action, stageOf(other), defaultExecutor());
    }

    public <U> CompletableFuture<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                                                           BiConsumer<? super T, ? super U> action,
                                                           Executor executor) {
        return this.<Void>chain(ACCEPT_BOTH, action, stageOf(other), asyncExecutor(executor));
    }

    public CompletableFuture<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
        return this.<Void>chain(RUN_AFTER_BOTH, action, stageOf(other), null);
    }

    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
        return this.<Void>chain(RUN_AFTER_BOTH, action, stageOf(other), defaultExecutor());
    }

    public CompletableFuture<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action,
                                                     Executor executor) {
        return this.<Void>chain(RUN_AFTER_BOTH, action, stageOf(other), asyncExecutor(executor));
    }

    // ---------------------------------------------------------------- first one wins

    public <U> CompletableFuture<U> applyToEither(CompletionStage<? extends T> other,
                                                  Function<? super T, U> fn) {
        return this.<U>chain(APPLY_EITHER, fn, stageOf(other), null);
    }

    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other,
                                                       Function<? super T, U> fn) {
        return this.<U>chain(APPLY_EITHER, fn, stageOf(other), defaultExecutor());
    }

    public <U> CompletableFuture<U> applyToEitherAsync(CompletionStage<? extends T> other,
                                                       Function<? super T, U> fn,
                                                       Executor executor) {
        return this.<U>chain(APPLY_EITHER, fn, stageOf(other), asyncExecutor(executor));
    }

    public CompletableFuture<Void> acceptEither(CompletionStage<? extends T> other,
                                                Consumer<? super T> action) {
        return this.<Void>chain(ACCEPT_EITHER, action, stageOf(other), null);
    }

    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other,
                                                     Consumer<? super T> action) {
        return this.<Void>chain(ACCEPT_EITHER, action, stageOf(other), defaultExecutor());
    }

    public CompletableFuture<Void> acceptEitherAsync(CompletionStage<? extends T> other,
                                                     Consumer<? super T> action,
                                                     Executor executor) {
        return this.<Void>chain(ACCEPT_EITHER, action, stageOf(other), asyncExecutor(executor));
    }

    public CompletableFuture<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
        return this.<Void>chain(RUN_AFTER_EITHER, action, stageOf(other), null);
    }

    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
        return this.<Void>chain(RUN_AFTER_EITHER, action, stageOf(other), defaultExecutor());
    }

    public CompletableFuture<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action,
                                                       Executor executor) {
        return this.<Void>chain(RUN_AFTER_EITHER, action, stageOf(other), asyncExecutor(executor));
    }

    // ---------------------------------------------------------------- flattening

    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        return this.<U>chain(COMPOSE, fn, null, null);
    }

    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
        return this.<U>chain(COMPOSE, fn, null, defaultExecutor());
    }

    public <U> CompletableFuture<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
                                                     Executor executor) {
        return this.<U>chain(COMPOSE, fn, null, asyncExecutor(executor));
    }

    // ---------------------------------------------------------------- outcome-aware

    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        return this.<U>chain(HANDLE, fn, null, null);
    }

    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
        return this.<U>chain(HANDLE, fn, null, defaultExecutor());
    }

    public <U> CompletableFuture<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn,
                                                Executor executor) {
        return this.<U>chain(HANDLE, fn, null, asyncExecutor(executor));
    }

    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        return this.<T>chain(WHEN, action, null, null);
    }

    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
        return this.<T>chain(WHEN, action, null, defaultExecutor());
    }

    public CompletableFuture<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action,
                                                  Executor executor) {
        return this.<T>chain(WHEN, action, null, asyncExecutor(executor));
    }

    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        return this.<T>chain(RECOVER, fn, null, null);
    }

    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn) {
        return this.<T>chain(RECOVER, fn, null, defaultExecutor());
    }

    public CompletableFuture<T> exceptionallyAsync(Function<Throwable, ? extends T> fn,
                                                   Executor executor) {
        return this.<T>chain(RECOVER, fn, null, asyncExecutor(executor));
    }

    // Recovery whose replacement is itself a stage. `handle` lifts the outcome to a
    // stage-of-stage and `thenCompose` flattens it; the intermediate is bound to a local
    // rather than chained (finding #219).
    public CompletableFuture<T> exceptionallyCompose(
            Function<Throwable, ? extends CompletionStage<T>> fn) {
        return composeRecovery(fn);
    }

    public CompletableFuture<T> exceptionallyComposeAsync(
            Function<Throwable, ? extends CompletionStage<T>> fn) {
        return composeRecovery(fn);
    }

    public CompletableFuture<T> exceptionallyComposeAsync(
            Function<Throwable, ? extends CompletionStage<T>> fn, Executor executor) {
        return composeRecovery(fn);
    }

    // The body of the three exceptionallyCompose* methods, under a name of its own. Calling
    // one public overload from another is not possible here: a covariant override of an
    // interface method leaves the *name* ambiguous to this compiler, since the interface's
    // declaration and this class's are both candidates. Same reason the two steps below go
    // straight to `chain` instead of through handle()/thenCompose().
    private CompletableFuture<T> composeRecovery(
            Function<Throwable, ? extends CompletionStage<T>> fn) {
        ExceptionallyComposer<T> lift = new ExceptionallyComposer<T>(this, fn);
        StageIdentity<T> flatten = new StageIdentity<T>();
        CompletableFuture nested = this.chain(HANDLE, lift, null, null);
        CompletableFuture out = nested.chain(COMPOSE, flatten, null, null);
        return out;
    }

    // ---------------------------------------------------------------- fan-in

    // Settles when every input has; carries no value, only the first failure seen.
    public static CompletableFuture<Void> allOf(CompletableFuture<?>... cfs) {
        CompletableFuture<Void> out = new CompletableFuture<Void>();
        int n = cfs.length;
        if (n == 0) {
            out.settle(NIL);
        } else {
            CfGate gate = new CfGate(out, n, true);
            int i = 0;
            while (i < n) {
                CompletableFuture<?> f = cfs[i];
                CfGateStep step = new CfGateStep(gate, f);
                f.onSettled(step);
                i = i + 1;
            }
        }
        return out;
    }

    // Settles as soon as any input does, with that input's outcome.
    public static CompletableFuture<Object> anyOf(CompletableFuture<?>... cfs) {
        CompletableFuture<Object> out = new CompletableFuture<Object>();
        int n = cfs.length;
        int i = 0;
        CfGate gate = new CfGate(out, n, false);
        while (i < n) {
            CompletableFuture<?> f = cfs[i];
            CfGateStep step = new CfGateStep(gate, f);
            f.onSettled(step);
            i = i + 1;
        }
        return out;
    }

    // Every combinator takes a CompletionStage but works on a CompletableFuture; this is
    // the one place the conversion happens. Bound to a local because chaining a call onto
    // the result of an interface call is finding #219.
    static CompletableFuture stageOf(CompletionStage<?> stage) {
        CompletableFuture f = null;
        if (stage != null) {
            f = stage.toCompletableFuture();
        }
        return f;
    }
}

// The failure box. Its existence is what lets one field hold three states.
final class CfFailure {

    final Throwable cause;

    CfFailure(Throwable cause) {
        this.cause = cause;
    }
}

// One dependent step of the graph. Its opcode says what to compute; its `src`, `other` and
// `out` say from where and to where. Firing is idempotent — `either` steps are reached from
// two sources and must run once.
final class CfStep implements Runnable {

    private final int kind;
    private final CompletableFuture src;
    private final CompletableFuture other;
    private final CompletableFuture out;
    private final Object fn;
    private final Executor executor;
    private boolean fired;

    CfStep(int kind, CompletableFuture src, CompletableFuture other, CompletableFuture out,
           Object fn, Executor executor) {
        this.kind = kind;
        this.src = src;
        this.other = other;
        this.out = out;
        this.fn = fn;
        this.executor = executor;
    }

    // Called once per source that settles. Decides whether the step is ready, claims it,
    // and then either computes here or hands the computation to the executor.
    public void run() {
        if (!ready()) {
            return;
        }
        boolean mine = false;
        synchronized (this) {
            if (!fired) {
                fired = true;
                mine = true;
            }
        }
        if (!mine) {
            return;
        }
        if (executor == null) {
            fire();
        } else {
            executor.execute(new CfFire(this));
        }
    }

    // Two-source steps that need both wait; everything else is ready as soon as it is
    // reached, since it is only reached from a settled source.
    private boolean ready() {
        boolean ok = true;
        if (kind == CompletableFuture.COMBINE || kind == CompletableFuture.ACCEPT_BOTH
                || kind == CompletableFuture.RUN_AFTER_BOTH) {
            ok = src.isDone() && other.isDone();
        }
        return ok;
    }

    // The actual work. A failure anywhere — in a source or in the user's function — settles
    // `out` exceptionally; only HANDLE, WHEN and RECOVER get to look at a failed source.
    void fire() {
        CompletableFuture winner = src;
        if (kind == CompletableFuture.APPLY_EITHER || kind == CompletableFuture.ACCEPT_EITHER
                || kind == CompletableFuture.RUN_AFTER_EITHER) {
            if (!src.isDone()) {
                winner = other;
            }
        }
        Object raw = winner.rawResult();
        Throwable failure = CompletableFuture.failureOf(raw);
        Object value = CompletableFuture.valueOf(raw);
        boolean sees = kind == CompletableFuture.HANDLE || kind == CompletableFuture.WHEN
                || kind == CompletableFuture.RECOVER || kind == CompletableFuture.RELAY;
        if (failure != null && !sees) {
            out.settle(new CfFailure(failure));
            return;
        }
        try {
            apply(raw, value, failure);
        } catch (Throwable t) {
            out.settle(new CfFailure(t));
        }
    }

    private void apply(Object raw, Object value, Throwable failure) {
        if (kind == CompletableFuture.APPLY) {
            Function f = (Function) fn;
            out.settle(CompletableFuture.boxValue(f.apply(value)));
        } else if (kind == CompletableFuture.ACCEPT) {
            Consumer c = (Consumer) fn;
            c.accept(value);
            out.settle(CompletableFuture.NIL);
        } else if (kind == CompletableFuture.RUN) {
            Runnable r = (Runnable) fn;
            r.run();
            out.settle(CompletableFuture.NIL);
        } else if (kind == CompletableFuture.COMBINE) {
            Object second = CompletableFuture.valueOf(other.rawResult());
            Throwable secondFailure = CompletableFuture.failureOf(other.rawResult());
            if (secondFailure != null) {
                out.settle(new CfFailure(secondFailure));
            } else {
                BiFunction bf = (BiFunction) fn;
                out.settle(CompletableFuture.boxValue(bf.apply(value, second)));
            }
        } else if (kind == CompletableFuture.ACCEPT_BOTH) {
            Object second = CompletableFuture.valueOf(other.rawResult());
            BiConsumer bc = (BiConsumer) fn;
            bc.accept(value, second);
            out.settle(CompletableFuture.NIL);
        } else if (kind == CompletableFuture.RUN_AFTER_BOTH) {
            Runnable r = (Runnable) fn;
            r.run();
            out.settle(CompletableFuture.NIL);
        } else if (kind == CompletableFuture.APPLY_EITHER) {
            Function f = (Function) fn;
            out.settle(CompletableFuture.boxValue(f.apply(value)));
        } else if (kind == CompletableFuture.ACCEPT_EITHER) {
            Consumer c = (Consumer) fn;
            c.accept(value);
            out.settle(CompletableFuture.NIL);
        } else if (kind == CompletableFuture.RUN_AFTER_EITHER) {
            Runnable r = (Runnable) fn;
            r.run();
            out.settle(CompletableFuture.NIL);
        } else if (kind == CompletableFuture.COMPOSE) {
            Function f = (Function) fn;
            Object produced = f.apply(value);
            CompletionStage stage = (CompletionStage) produced;
            CompletableFuture inner = CompletableFuture.stageOf(stage);
            CfRelay relay = new CfRelay(inner, out);
            inner.onSettled(relay);
        } else if (kind == CompletableFuture.HANDLE) {
            BiFunction bf = (BiFunction) fn;
            out.settle(CompletableFuture.boxValue(bf.apply(value, failure)));
        } else if (kind == CompletableFuture.WHEN) {
            BiConsumer bc = (BiConsumer) fn;
            bc.accept(value, failure);
            out.settle(raw);
        } else if (kind == CompletableFuture.RECOVER) {
            if (failure == null) {
                out.settle(raw);
            } else {
                Function f = (Function) fn;
                out.settle(CompletableFuture.boxValue(f.apply(failure)));
            }
        } else {
            out.settle(raw);
        }
    }
}

// Hands a step's body to an executor without letting `run()` re-enter the claim logic.
final class CfFire implements Runnable {

    private final CfStep step;

    CfFire(CfStep step) {
        this.step = step;
    }

    public void run() {
        step.fire();
    }
}

// Copies one future's outcome into another — the tail of `thenCompose`.
final class CfRelay implements Runnable {

    private final CompletableFuture from;
    private final CompletableFuture to;

    CfRelay(CompletableFuture from, CompletableFuture to) {
        this.from = from;
        this.to = to;
    }

    public void run() {
        to.settle(from.rawResult());
    }
}

// The shared counter behind allOf / anyOf. `all` decides whether it waits for the last
// arrival or fires on the first.
final class CfGate {

    private final CompletableFuture out;
    private final boolean all;
    private int remaining;

    CfGate(CompletableFuture out, int count, boolean all) {
        this.out = out;
        this.all = all;
        this.remaining = count;
    }

    void arrived(CompletableFuture source) {
        Object raw = source.rawResult();
        Throwable failure = CompletableFuture.failureOf(raw);
        boolean release;
        synchronized (this) {
            remaining = remaining - 1;
            release = !all || remaining == 0 || failure != null;
        }
        if (release) {
            if (all && failure == null) {
                out.settle(CompletableFuture.NIL);
            } else {
                out.settle(raw);
            }
        }
    }
}

final class CfGateStep implements Runnable {

    private final CfGate gate;
    private final CompletableFuture source;

    CfGateStep(CfGate gate, CompletableFuture source) {
        this.gate = gate;
        this.source = source;
    }

    public void run() {
        gate.arrived(source);
    }
}

// Runs a Supplier and settles a future with its outcome — the body of supplyAsync.
final class CfSupplyTask implements Runnable {

    private final CompletableFuture target;
    private final Supplier supplier;

    CfSupplyTask(CompletableFuture target, Supplier supplier) {
        this.target = target;
        this.supplier = supplier;
    }

    public void run() {
        try {
            target.settle(CompletableFuture.boxValue(supplier.get()));
        } catch (Throwable t) {
            target.settle(new CfFailure(t));
        }
    }
}

// Same, for runAsync: the value is always null.
final class CfRunTask implements Runnable {

    private final CompletableFuture target;
    private final Runnable body;

    CfRunTask(CompletableFuture target, Runnable body) {
        this.target = target;
        this.body = body;
    }

    public void run() {
        try {
            body.run();
            target.settle(CompletableFuture.NIL);
        } catch (Throwable t) {
            target.settle(new CfFailure(t));
        }
    }
}

// A thread per task. Simple on purpose: a pool would need a lifecycle nobody can shut down,
// since the default executor of CompletableFuture outlives every stage that uses it.
final class CfThreadPerTaskExecutor implements Executor {

    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        Thread t = new Thread(command);
        t.start();
    }
}

// Wraps another executor, delaying each submission. `delayedExecutor` hands these out.
final class CfDelayedExecutor implements Executor {

    private final long millis;
    private final Executor target;

    CfDelayedExecutor(long millis, Executor target) {
        this.millis = millis;
        this.target = target;
    }

    public void execute(Runnable command) {
        if (command == null) {
            throw new NullPointerException();
        }
        Executor e = target;
        if (e == null) {
            e = CompletableFuture.defaultCommonExecutor();
        }
        CfDelayedTask task = new CfDelayedTask(millis, e, command);
        task.start();
    }
}

final class CfDelayedTask extends Thread {

    private final long millis;
    private final Executor target;
    private final Runnable body;

    CfDelayedTask(long millis, Executor target, Runnable body) {
        this.millis = millis;
        this.target = target;
        this.body = body;
    }

    public void run() {
        Thread.sleep(millis);
        target.execute(body);
    }
}

// The timer behind orTimeout / completeOnTimeout. It loses harmlessly if the future settles
// first: `settle` is first-writer-wins.
final class CfTimeout extends Thread {

    private final CompletableFuture target;
    private final long millis;
    private final boolean withValue;
    private final Object value;

    CfTimeout(CompletableFuture target, long millis, boolean withValue, Object value) {
        this.target = target;
        this.millis = millis;
        this.withValue = withValue;
        this.value = value;
    }

    void arm() {
        start();
    }

    public void run() {
        Thread.sleep(millis);
        if (withValue) {
            target.settle(CompletableFuture.boxValue(value));
        } else {
            target.settle(new CfFailure(new TimeoutException()));
        }
    }
}
