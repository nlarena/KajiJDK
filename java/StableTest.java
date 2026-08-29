import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Exercises java.lang.StableValue. Every method returns the number of things that came out
 * wrong, so 0 is a pass.
 *
 * The point of every group is the same question asked of a different shape: is the underlying
 * computation run AT MOST ONCE, and is the answer the same every time afterwards.
 */
public class StableTest {

    /** trySet wins once; the loser does not overwrite. */
    public static int unaSolaVez() {
        StableValue<String> slot = StableValue.of();
        int bad = 0;
        if (slot.isSet()) {
            bad = bad + 1;
        }
        if (!slot.trySet("primero")) {
            bad = bad + 1;
        }
        if (slot.trySet("segundo")) {
            bad = bad + 1;
        }
        if (!slot.isSet()) {
            bad = bad + 1;
        }
        if (!slot.orElseThrow().equals("primero")) {
            bad = bad + 1;
        }
        if (!slot.orElse("otro").equals("primero")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Empty is not the same as holding null: reading it is refused, not answered with null. */
    public static int vacioNoEsNulo() {
        StableValue<String> slot = StableValue.of();
        int bad = 0;
        if (!slot.orElse("caida").equals("caida")) {
            bad = bad + 1;
        }
        boolean refused = false;
        try {
            String gone = slot.orElseThrow();
            if (gone != null) {
                bad = bad + 1;
            }
        } catch (RuntimeException expected) {
            refused = true;
        }
        if (!refused) {
            bad = bad + 1;
        }
        // And a stable value that really holds null answers, rather than refusing.
        StableValue<String> nulo = StableValue.of();
        nulo.setOrThrow(null);
        if (!nulo.isSet()) {
            bad = bad + 1;
        }
        if (nulo.orElseThrow() != null) {
            bad = bad + 1;
        }
        return bad;
    }

    /** setOrThrow refuses the second write instead of ignoring it. */
    public static int segundaEscritura() {
        StableValue<String> slot = StableValue.of("ya");
        int bad = 0;
        if (!slot.orElseThrow().equals("ya")) {
            bad = bad + 1;
        }
        boolean complained = false;
        try {
            slot.setOrThrow("otra");
        } catch (IllegalStateException expected) {
            complained = true;
        }
        if (!complained) {
            bad = bad + 1;
        }
        if (!slot.orElseThrow().equals("ya")) {
            bad = bad + 1;
        }
        return bad;
    }

    /** orElseSet computes once, and the second call does not run the supplier again. */
    public static int calculaUnaVez() {
        StableCounter calls = new StableCounter();
        StableValue<String> slot = StableValue.of();
        Once maker = new Once(calls, "valor");
        int bad = 0;
        if (!slot.orElseSet(maker).equals("valor")) {
            bad = bad + 1;
        }
        if (calls.value() != 1) {
            bad = bad + 1;
        }
        if (!slot.orElseSet(maker).equals("valor")) {
            bad = bad + 1;
        }
        if (calls.value() != 1) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The memoising supplier: same answer, one call. */
    public static int suplidor() {
        StableCounter calls = new StableCounter();
        Supplier<String> lazy = StableValue.supplier(new Once(calls, "hecho"));
        int bad = 0;
        if (calls.value() != 0) {
            bad = bad + 1;
        }
        if (!lazy.get().equals("hecho")) {
            bad = bad + 1;
        }
        if (!lazy.get().equals("hecho")) {
            bad = bad + 1;
        }
        if (calls.value() != 1) {
            bad = bad + 1;
        }
        return bad;
    }

    /** One slot per index, and an index outside the range is an error. */
    public static int porIndice() {
        StableCounter calls = new StableCounter();
        IntFunction<String> square = StableValue.intFunction(4, new Squarer(calls));
        int bad = 0;
        if (!square.apply(2).equals("4")) {
            bad = bad + 1;
        }
        if (!square.apply(2).equals("4")) {
            bad = bad + 1;
        }
        if (calls.value() != 1) {
            bad = bad + 1;
        }
        if (!square.apply(3).equals("9")) {
            bad = bad + 1;
        }
        if (calls.value() != 2) {
            bad = bad + 1;
        }
        boolean refused = false;
        try {
            String gone = square.apply(9);
            if (gone != null) {
                bad = bad + 1;
            }
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        if (!refused) {
            bad = bad + 1;
        }
        return bad;
    }

    /** One slot per declared input, and an undeclared input is an error rather than a cache miss. */
    public static int porEntrada() {
        StableCounter calls = new StableCounter();
        Set<String> inputs = new HashSet<String>();
        inputs.add("a");
        inputs.add("bb");
        Function<String, String> sized = StableValue.function(inputs, new StableSizer(calls));
        int bad = 0;
        if (!sized.apply("a").equals("1")) {
            bad = bad + 1;
        }
        if (!sized.apply("a").equals("1")) {
            bad = bad + 1;
        }
        if (calls.value() != 1) {
            bad = bad + 1;
        }
        if (!sized.apply("bb").equals("2")) {
            bad = bad + 1;
        }
        if (calls.value() != 2) {
            bad = bad + 1;
        }
        boolean refused = false;
        try {
            String gone = sized.apply("ccc");
            if (gone != null) {
                bad = bad + 1;
            }
        } catch (IllegalArgumentException expected) {
            refused = true;
        }
        if (!refused) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The lazy list: nothing is computed until it is read, and never twice. */
    public static int listaPerezosa() {
        StableCounter calls = new StableCounter();
        List<String> lazy = StableValue.list(3, new Squarer(calls));
        int bad = 0;
        if (lazy.size() != 3) {
            bad = bad + 1;
        }
        if (calls.value() != 0) {
            bad = bad + 1;
        }
        if (!lazy.get(2).equals("4")) {
            bad = bad + 1;
        }
        if (calls.value() != 1) {
            bad = bad + 1;
        }
        if (!lazy.get(2).equals("4")) {
            bad = bad + 1;
        }
        if (calls.value() != 1) {
            bad = bad + 1;
        }
        boolean refused = false;
        try {
            lazy.set(0, "x");
        } catch (UnsupportedOperationException expected) {
            refused = true;
        }
        if (!refused) {
            bad = bad + 1;
        }
        return bad;
    }

    /** The lazy map: fixed keys, values on demand. */
    public static int mapaPerezoso() {
        StableCounter calls = new StableCounter();
        Set<String> keys = new HashSet<String>();
        keys.add("a");
        keys.add("bb");
        Map<String, String> lazy = StableValue.map(keys, new StableSizer(calls));
        int bad = 0;
        if (lazy.size() != 2) {
            bad = bad + 1;
        }
        if (calls.value() != 0) {
            bad = bad + 1;
        }
        if (!lazy.containsKey("a")) {
            bad = bad + 1;
        }
        if (lazy.containsKey("zz")) {
            bad = bad + 1;
        }
        if (!lazy.get("bb").equals("2")) {
            bad = bad + 1;
        }
        if (calls.value() != 1) {
            bad = bad + 1;
        }
        if (!lazy.get("bb").equals("2")) {
            bad = bad + 1;
        }
        if (calls.value() != 1) {
            bad = bad + 1;
        }
        if (lazy.get("zz") != null) {
            bad = bad + 1;
        }
        boolean refused = false;
        try {
            lazy.put("c", "3");
        } catch (UnsupportedOperationException expected) {
            refused = true;
        }
        if (!refused) {
            bad = bad + 1;
        }
        return bad;
    }

    /** Everything at once, so one call answers "does it work". */
    public static int todo() {
        return StableTest.unaSolaVez() + StableTest.vacioNoEsNulo() + StableTest.segundaEscritura()
                + StableTest.calculaUnaVez() + StableTest.suplidor() + StableTest.porIndice()
                + StableTest.porEntrada() + StableTest.listaPerezosa() + StableTest.mapaPerezoso();
    }
}


/** A mutable count of how many times an underlying computation actually ran. */
// El auxiliar lleva el prefijo del probe: `java/` es un paquete por defecto **plano**, asi
// que dos fuentes que declaren la misma clase escriben el mismo `.class` y gana la ultima
// compilada -- el resultado de la suite pasa a depender del orden de compilacion (#273).
final class StableCounter {

    private int n;

    StableCounter() {
        this.n = 0;
    }

    void bump() {
        this.n = this.n + 1;
    }

    int value() {
        return this.n;
    }
}


/** A supplier that counts its own calls, so "at most once" is observable. */
final class Once implements Supplier<String> {

    private final StableCounter calls;
    private final String answer;

    Once(StableCounter calls, String answer) {
        this.calls = calls;
        this.answer = answer;
    }

    @Override
    public String get() {
        this.calls.bump();
        return this.answer;
    }
}


/** index -> index squared, counting calls. */
final class Squarer implements IntFunction<String> {

    private final StableCounter calls;

    Squarer(StableCounter calls) {
        this.calls = calls;
    }

    @Override
    public String apply(int index) {
        this.calls.bump();
        return "" + (index * index);
    }
}


/** input -> its length, counting calls. */
final class StableSizer implements Function<String, String> {

    private final StableCounter calls;

    StableSizer(StableCounter calls) {
        this.calls = calls;
    }

    @Override
    public String apply(String input) {
        this.calls.bump();
        return "" + input.length();
    }
}
