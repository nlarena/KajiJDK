import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;

/**
 * Exercises StructuredTaskScope. Every method returns the number of things that came out wrong,
 * so 0 is a pass.
 *
 * It runs on OUR VM through run-headless, and the SAME source compiles against the JDK 25, where
 * `main` prints the same counts — so the two can be compared instead of trusted. (The JDK's version
 * is a preview API, so compiling and running it there needs --enable-preview.)
 *
 * No try-with-resources anywhere: close() is called by hand, so the test stays inside the subset of
 * the language the rest of this tree is written in.
 *
 * States are compared by NAME and not against the constants. Reading `Subtask.State.SUCCESS` from
 * another file is emitted as a `getfield` over a static and crashes the VM (finding #110), so the
 * idiomatic comparison cannot run here. `name()` reaches the same answer through a method call.
 */
public class ScopeTest {

    /** Three subtasks that all succeed: every one has a result once the scope is joined. */
    public static int todosBien() {
        StructuredTaskScope.Joiner<String, Void> joiner = StructuredTaskScope.Joiner.awaitAll();
        StructuredTaskScope<String, Void> scope = StructuredTaskScope.open(joiner);
        int bad = 0;
        StructuredTaskScope.Subtask<String> a = scope.fork(new Say("a"));
        StructuredTaskScope.Subtask<String> b = scope.fork(new Say("b"));
        StructuredTaskScope.Subtask<String> c = scope.fork(new Say("c"));
        try {
            scope.join();
        } catch (InterruptedException e) {
            bad = bad + 1;
        }
        if (!a.state().name().equals("SUCCESS")) {
            bad = bad + 1;
        }
        if (!a.get().equals("a")) {
            bad = bad + 1;
        }
        if (!b.get().equals("b")) {
            bad = bad + 1;
        }
        if (!c.get().equals("c")) {
            bad = bad + 1;
        }
        if (scope.isCancelled()) {
            bad = bad + 1;
        }
        scope.close();
        return bad;
    }

    /** A subtask that throws: the scope reports it as FAILED and hands back the throwable. */
    public static int unoFalla() {
        StructuredTaskScope.Joiner<String, Void> joiner = StructuredTaskScope.Joiner.awaitAll();
        StructuredTaskScope<String, Void> scope = StructuredTaskScope.open(joiner);
        int bad = 0;
        StructuredTaskScope.Subtask<String> ok = scope.fork(new Say("a"));
        StructuredTaskScope.Subtask<String> bang = scope.fork(new ScopeBoom("roto"));
        try {
            scope.join();
        } catch (InterruptedException e) {
            bad = bad + 1;
        }
        if (!ok.state().name().equals("SUCCESS")) {
            bad = bad + 1;
        }
        if (!bang.state().name().equals("FAILED")) {
            bad = bad + 1;
        }
        Throwable thrown = bang.exception();
        if (thrown == null) {
            bad = bad + 1;
        } else if (!thrown.getMessage().equals("roto")) {
            bad = bad + 1;
        }
        // Asking a failed subtask for a value is a bug in the caller, not a null.
        boolean refused = false;
        try {
            String gone = bang.get();
            if (gone != null) {
                bad = bad + 1;
            }
        } catch (IllegalStateException expected) {
            refused = true;
        }
        if (!refused) {
            bad = bad + 1;
        }
        scope.close();
        return bad;
    }

    /** awaitAllSuccessfulOrThrow: one failure ends the scope, and join reports it. */
    public static int fallaCortaElAlcance() {
        StructuredTaskScope.Joiner<String, Void> joiner =
                StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow();
        StructuredTaskScope<String, Void> scope = StructuredTaskScope.open(joiner);
        int bad = 0;
        StructuredTaskScope.Subtask<String> bang = scope.fork(new ScopeBoom("roto"));
        boolean reported = false;
        try {
            scope.join();
        } catch (InterruptedException e) {
            bad = bad + 1;
        } catch (StructuredTaskScope.FailedException expected) {
            reported = true;
        }
        if (!reported) {
            bad = bad + 1;
        }
        if (!scope.isCancelled()) {
            bad = bad + 1;
        }
        scope.close();
        return bad;
    }

    /** anySuccessfulResultOrThrow: the first success is the answer, and it ends the scope. */
    public static int elPrimeroQueGane() {
        StructuredTaskScope.Joiner<String, String> joiner =
                StructuredTaskScope.Joiner.anySuccessfulResultOrThrow();
        StructuredTaskScope<String, String> scope = StructuredTaskScope.open(joiner);
        int bad = 0;
        scope.fork(new Say("solo"));
        String answer = null;
        try {
            answer = scope.join();
        } catch (InterruptedException e) {
            bad = bad + 1;
        }
        if (answer == null || !answer.equals("solo")) {
            bad = bad + 1;
        }
        scope.close();
        return bad;
    }

    /** A Runnable fork: it has no result, but it still ran and still counts as a success. */
    public static int forkDeRunnable() {
        StructuredTaskScope.Joiner<Object, Void> joiner = StructuredTaskScope.Joiner.awaitAll();
        StructuredTaskScope<Object, Void> scope = StructuredTaskScope.open(joiner);
        int bad = 0;
        Mark mark = new Mark();
        StructuredTaskScope.Subtask<Object> ran = scope.fork(mark);
        try {
            scope.join();
        } catch (InterruptedException e) {
            bad = bad + 1;
        }
        if (!mark.done()) {
            bad = bad + 1;
        }
        if (!ran.state().name().equals("SUCCESS")) {
            bad = bad + 1;
        }
        if (ran.get() != null) {
            bad = bad + 1;
        }
        scope.close();
        return bad;
    }

    /** Forking and closing without joining is a bug in the block, and close says so. */
    public static int cerrarSinJoin() {
        StructuredTaskScope.Joiner<String, Void> joiner = StructuredTaskScope.Joiner.awaitAll();
        StructuredTaskScope<String, Void> scope = StructuredTaskScope.open(joiner);
        int bad = 0;
        scope.fork(new Say("a"));
        boolean complained = false;
        try {
            scope.close();
        } catch (IllegalStateException expected) {
            complained = true;
        }
        if (!complained) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Everything at once, so one call answers "does it work". */
    public static int todo() {
        return ScopeTest.todosBien() + ScopeTest.unoFalla() + ScopeTest.fallaCortaElAlcance()
                + ScopeTest.elPrimeroQueGane() + ScopeTest.forkDeRunnable()
                + ScopeTest.cerrarSinJoin();
    }

    public static void main(String[] args) {
        System.out.println("todosBien             " + ScopeTest.todosBien());
        System.out.println("unoFalla              " + ScopeTest.unoFalla());
        System.out.println("fallaCortaElAlcance   " + ScopeTest.fallaCortaElAlcance());
        System.out.println("elPrimeroQueGane      " + ScopeTest.elPrimeroQueGane());
        System.out.println("forkDeRunnable        " + ScopeTest.forkDeRunnable());
        System.out.println("cerrarSinJoin         " + ScopeTest.cerrarSinJoin());
        System.out.println("TOTAL                 " + ScopeTest.todo());
    }
}


/** A subtask that answers with the word it was given. */
final class Say implements Callable<String> {

    private final String word;

    Say(String word) {
        this.word = word;
    }

    @Override
    public String call() {
        return this.word;
    }
}


// Con el prefijo del probe: `java/` es un paquete por defecto plano y habia tres `Boom` (#273).
/** A subtask that throws. */
final class ScopeBoom implements Callable<String> {

    private final String message;

    ScopeBoom(String message) {
        this.message = message;
    }

    // No `throws Exception`, though Callable allows it: an override that declares the same checked
    // exception as the interface it implements is rejected when the interface comes from the
    // classpath (finding #256). An unchecked throw needs no clause, and the test needs no more.
    @Override
    public String call() {
        throw new IllegalStateException(this.message);
    }
}


/** A Runnable subtask that leaves a trace of having run. */
final class Mark implements Runnable {

    private boolean ran;

    Mark() {
        this.ran = false;
    }

    @Override
    public void run() {
        this.ran = true;
    }

    boolean done() {
        return this.ran;
    }
}
