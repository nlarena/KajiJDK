import java.util.List;

/**
 * Passing a value to a parameter of the IDENTICAL declared type is rejected when that type
 * contains a wildcard: the argument is capture-converted and then never related back to the
 * parameter, so `List<? super T>` is reported as not convertible to `List<? super T>`.
 *
 *   bin/javac.exe --emit -cp KajiLibrary KajiLibrary/repros/finding_253.java
 *
 * Expected: compiles (JLS §5.3 — the argument's type IS the parameter's type).
 * Actual:   `no se encontró un método `paso(List<cap#0 of Object>)` aplicable`.
 *
 * `sinComodin` is the control: the same shape with no wildcard resolves.
 */
public class finding_253<T> {

    /** Forwards a wildcard-typed value to a method declared with that very type. */
    public int reenvia(List<? super T> destino) {
        return this.paso(destino);
    }

    /** The same, through a local of the declared type, in case the local re-anchors it. */
    public int reenviaPorLocal(List<? super T> destino) {
        List<? super T> mismo = destino;
        return this.paso(mismo);
    }

    private int paso(List<? super T> destino) {
        return destino.size();
    }

    /** Control: no wildcard, same shape. */
    public int sinComodin(List<T> destino) {
        return this.pasoExacto(destino);
    }

    private int pasoExacto(List<T> destino) {
        return destino.size();
    }
}
