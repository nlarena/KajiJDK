package pq;

/**
 * Una clase cuyo unico constructor toma un argumento.
 *
 * <p>Es la forma que tiene {@code jdk.internal.vm.vector.VectorSupport$Vector}, que es de donde
 * salio el hallazgo: la carga util es obligatoria, y por eso no hay constructor sin argumentos.
 */
public class Base {

    private final Object carga;

    public Base(Object carga) {
        this.carga = carga;
    }

    public Object carga() {
        return carga;
    }
}
