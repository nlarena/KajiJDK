package java.util.concurrent;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

// One step of an asynchronous computation, and a factory for the steps that follow it.
// A stage does not let you *wait*: it lets you say what should happen once it settles. That
// inversion is the whole point — the result is delivered to the continuation instead of
// being fetched by a blocked thread, so a pipeline of stages costs no thread while it waits.
//
// The method zoo is a small grid, and reading it as a grid is the only way it makes sense:
//
//   * what the continuation consumes — `apply` takes the value and returns one, `accept`
//     takes the value and returns nothing, `run` ignores the value entirely;
//   * how many stages it waits for — `then*` chains after this one, `*Both` waits for this
//     and another, `*Either` fires on whichever of the two settles first;
//   * how it runs — plain (on whatever thread completed the previous stage), `Async`
//     (on the default executor), or `Async` with an explicit Executor.
//
// Outside the grid sit the four that treat failure as data: `handle` and `whenComplete` see
// both outcomes, `exceptionally` substitutes a value for a failure, and `*Compose` flattens
// a stage-returning function so pipelines do not nest.
//
// Failure propagates: if a stage completes exceptionally, every dependent stage that is not
// one of those four completes exceptionally too, without invoking its function.
public interface CompletionStage<T> {

    // --- value in, value out ---

    <U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn);

    <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn);

    <U> CompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor);

    // --- value in, nothing out ---

    CompletionStage<Void> thenAccept(Consumer<? super T> action);

    CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action);

    CompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor);

    // --- nothing in, nothing out ---

    CompletionStage<Void> thenRun(Runnable action);

    CompletionStage<Void> thenRunAsync(Runnable action);

    CompletionStage<Void> thenRunAsync(Runnable action, Executor executor);

    // --- both stages must settle ---

    <U, V> CompletionStage<V> thenCombine(CompletionStage<? extends U> other,
                                          BiFunction<? super T, ? super U, ? extends V> fn);

    <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
                                               BiFunction<? super T, ? super U, ? extends V> fn);

    <U, V> CompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
                                               BiFunction<? super T, ? super U, ? extends V> fn,
                                               Executor executor);

    <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
                                             BiConsumer<? super T, ? super U> action);

    <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                                                  BiConsumer<? super T, ? super U> action);

    <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
                                                  BiConsumer<? super T, ? super U> action,
                                                  Executor executor);

    CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action);

    CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action);

    CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action,
                                            Executor executor);

    // --- whichever settles first wins ---

    <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other,
                                         Function<? super T, U> fn);

    <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
                                              Function<? super T, U> fn);

    <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
                                              Function<? super T, U> fn, Executor executor);

    CompletionStage<Void> acceptEither(CompletionStage<? extends T> other,
                                       Consumer<? super T> action);

    CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
                                            Consumer<? super T> action);

    CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
                                            Consumer<? super T> action, Executor executor);

    CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action);

    CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action);

    CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action,
                                              Executor executor);

    // --- flattening: the function itself returns a stage ---

    <U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn);

    <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn);

    <U> CompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
                                            Executor executor);

    // --- outcome-aware: these run whether this stage succeeded or failed ---

    <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);

    <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn);

    <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn,
                                       Executor executor);

    // Like handle, but observes without transforming: the resulting stage carries this
    // stage's own outcome through.
    CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action);

    CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action);

    CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action,
                                         Executor executor);

    // Recovery: on failure, supply a replacement value; on success, pass the value through.
    CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn);

    // The async recovery forms differ from the plain one only in *which thread* runs the
    // recovery function. This default runs it on the completing thread, which is a legal
    // executor choice and the one the base implementation here can honour; a concrete stage
    // that owns an executor is expected to override.
    default CompletionStage<T> exceptionallyAsync(Function<Throwable, ? extends T> fn) {
        return exceptionally(fn);
    }

    default CompletionStage<T> exceptionallyAsync(Function<Throwable, ? extends T> fn,
                                                  Executor executor) {
        return exceptionally(fn);
    }

    // Recovery that itself returns a stage. Done in two moves, as in the JDK: `handle` lifts
    // the outcome into a stage-of-stage, then `thenCompose` flattens it. The intermediate is
    // bound to a local rather than chained — chaining onto a classpath type's result is
    // finding #219, and a local costs nothing.
    default CompletionStage<T> exceptionallyCompose(
            Function<Throwable, ? extends CompletionStage<T>> fn) {
        ExceptionallyComposer<T> lift = new ExceptionallyComposer<T>(this, fn);
        CompletionStage<CompletionStage<T>> nested = handle(lift);
        StageIdentity<T> flatten = new StageIdentity<T>();
        return nested.thenCompose(flatten);
    }

    default CompletionStage<T> exceptionallyComposeAsync(
            Function<Throwable, ? extends CompletionStage<T>> fn) {
        return exceptionallyCompose(fn);
    }

    default CompletionStage<T> exceptionallyComposeAsync(
            Function<Throwable, ? extends CompletionStage<T>> fn, Executor executor) {
        return exceptionallyCompose(fn);
    }

    // The bridge back to the blocking world. A stage that refuses to be waited on (a
    // "minimal" stage) throws UnsupportedOperationException here.
    CompletableFuture<T> toCompletableFuture();
}

// Turns an outcome into a stage: the source stage itself when it succeeded, the recovery
// function's stage when it failed. Named rather than a lambda — a lambda passed to a method
// on a classpath type does not compile (finding #218), and a named class is legible anyway.
final class ExceptionallyComposer<T> implements BiFunction<T, Throwable, CompletionStage<T>> {

    private final CompletionStage<T> source;
    private final Function<Throwable, ? extends CompletionStage<T>> recovery;

    ExceptionallyComposer(CompletionStage<T> source,
                          Function<Throwable, ? extends CompletionStage<T>> recovery) {
        this.source = source;
        this.recovery = recovery;
    }

    public CompletionStage<T> apply(T value, Throwable failure) {
        CompletionStage<T> result;
        if (failure == null) {
            result = source;
        } else {
            result = recovery.apply(failure);
        }
        return result;
    }
}

// Identity on stages — the flattening step of `exceptionallyCompose`.
final class StageIdentity<T> implements Function<CompletionStage<T>, CompletionStage<T>> {

    public CompletionStage<T> apply(CompletionStage<T> stage) {
        return stage;
    }
}
