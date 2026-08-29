/**
 * Exercises java.lang.ScopedValue. Every method returns the number of things that came out
 * wrong, so 0 is a pass.
 *
 * The same source runs on our VM through run-headless and against the JDK 25 through `main`
 * (with --enable-preview there), so the two answers can be compared instead of trusted.
 */
public class ScopedTest {

    static final ScopedValue<String> WHO = ScopedValue.newInstance();
    static final ScopedValue<String> WHERE = ScopedValue.newInstance();

    /** Bound inside the operation, unbound outside it. */
    public static int dentroYFuera() {
        int bad = 0;
        if (ScopedTest.WHO.isBound()) {
            bad = bad + 1;
        }
        Peek peek = new Peek(ScopedTest.WHO);
        ScopedValue.where(ScopedTest.WHO, "ana").run(peek);
        if (!peek.seen().equals("ana")) {
            bad = bad + 1;
        }
        if (ScopedTest.WHO.isBound()) {
            bad = bad + 1;
        }
        return bad;
    }

    /** An unbound read is refused, not answered with null. */
    public static int sinLigar() {
        int bad = 0;
        if (!ScopedTest.WHO.orElse("nadie").equals("nadie")) {
            bad = bad + 1;
        }
        boolean refused = false;
        try {
            String gone = ScopedTest.WHO.get();
            if (gone != null) {
                bad = bad + 1;
            }
        } catch (RuntimeException expected) {
            refused = true;
        }
        if (!refused) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Two bindings at once, through the carrier chain. */
    public static int dosALaVez() {
        int bad = 0;
        PeekTwo peek = new PeekTwo(ScopedTest.WHO, ScopedTest.WHERE);
        ScopedValue.where(ScopedTest.WHO, "ana").where(ScopedTest.WHERE, "casa").run(peek);
        if (!peek.first().equals("ana")) {
            bad = bad + 1;
        }
        if (!peek.second().equals("casa")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** An inner binding hides the outer one, and the outer one comes back. */
    public static int anidado() {
        int bad = 0;
        Nested inner = new Nested(ScopedTest.WHO);
        ScopedValue.where(ScopedTest.WHO, "afuera").run(inner);
        if (!inner.outerBefore().equals("afuera")) {
            bad = bad + 1;
        }
        if (!inner.innerSeen().equals("adentro")) {
            bad = bad + 1;
        }
        if (!inner.outerAfter().equals("afuera")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** call() hands back what the operation produced. */
    public static int devuelveValor() {
        int bad = 0;
        Answer op = new Answer(ScopedTest.WHO);
        String got = ScopedValue.where(ScopedTest.WHO, "ana").call(op);
        if (got == null || !got.equals("hola ana")) {
            bad = bad + 1;
        }
        if (ScopedTest.WHO.isBound()) {
            bad = bad + 1;
        }
        return bad;
    }

    /** An operation that throws still leaves the scope unbound on the way out. */
    public static int desligaAlFallar() {
        int bad = 0;
        Blow op = new Blow();
        boolean thrown = false;
        try {
            ScopedValue.where(ScopedTest.WHO, "ana").run(op);
        } catch (IllegalStateException expected) {
            thrown = true;
        }
        if (!thrown) {
            bad = bad + 1;
        }
        if (ScopedTest.WHO.isBound()) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The carrier can be read without running anything. */
    public static int leerElPortador() {
        int bad = 0;
        ScopedValue.Carrier carrier = ScopedValue.where(ScopedTest.WHO, "ana");
        if (!carrier.get(ScopedTest.WHO).equals("ana")) {
            bad = bad + 1;
        }
        if (ScopedTest.WHO.isBound()) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Everything at once, so one call answers "does it work". */
    public static int todo() {
        return ScopedTest.dentroYFuera() + ScopedTest.sinLigar() + ScopedTest.dosALaVez()
                + ScopedTest.anidado() + ScopedTest.devuelveValor() + ScopedTest.desligaAlFallar()
                + ScopedTest.leerElPortador();
    }

    public static void main(String[] args) {
        System.out.println("dentroYFuera      " + ScopedTest.dentroYFuera());
        System.out.println("sinLigar          " + ScopedTest.sinLigar());
        System.out.println("dosALaVez         " + ScopedTest.dosALaVez());
        System.out.println("anidado           " + ScopedTest.anidado());
        System.out.println("devuelveValor     " + ScopedTest.devuelveValor());
        System.out.println("desligaAlFallar   " + ScopedTest.desligaAlFallar());
        System.out.println("leerElPortador    " + ScopedTest.leerElPortador());
        System.out.println("TOTAL             " + ScopedTest.todo());
    }
}


/** Reads one scoped value from inside the scope. */
final class Peek implements Runnable {

    private final ScopedValue<String> key;
    private String value;

    Peek(ScopedValue<String> key) {
        this.key = key;
        this.value = null;
    }

    @Override
    public void run() {
        this.value = this.key.get();
    }

    String seen() {
        return this.value;
    }
}


/** Reads two scoped values from inside the scope. */
final class PeekTwo implements Runnable {

    private final ScopedValue<String> a;
    private final ScopedValue<String> b;
    private String first;
    private String second;

    PeekTwo(ScopedValue<String> a, ScopedValue<String> b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public void run() {
        this.first = this.a.get();
        this.second = this.b.get();
    }

    String first() {
        return this.first;
    }

    String second() {
        return this.second;
    }
}


/** Rebinds the same value in an inner scope and records what is visible when. */
final class Nested implements Runnable {

    private final ScopedValue<String> key;
    private String before;
    private String inner;
    private String after;

    Nested(ScopedValue<String> key) {
        this.key = key;
    }

    @Override
    public void run() {
        this.before = this.key.get();
        Peek deeper = new Peek(this.key);
        ScopedValue.where(this.key, "adentro").run(deeper);
        this.inner = deeper.seen();
        this.after = this.key.get();
    }

    String outerBefore() {
        return this.before;
    }

    String innerSeen() {
        return this.inner;
    }

    String outerAfter() {
        return this.after;
    }
}


/** An operation that produces a value from the scoped value. */
final class Answer implements ScopedValue.CallableOp<String, RuntimeException> {

    private final ScopedValue<String> key;

    Answer(ScopedValue<String> key) {
        this.key = key;
    }

    @Override
    public String call() {
        return "hola " + this.key.get();
    }
}


/** An operation that throws, to check the scope is unbound anyway. */
final class Blow implements Runnable {

    @Override
    public void run() {
        throw new IllegalStateException("se rompio");
    }
}
