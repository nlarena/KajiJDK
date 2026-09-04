package jdk.internal.vm;

/**
 * KajiLibrary's jdk.internal.vm.TranslatedException — una excepción que cruzó una frontera.
 *
 * <p>Existe para el compilador JIT escrito en Java (JVMCI): cuando una excepción nace del otro lado
 * de esa frontera, el objeto original **no se puede traer** —vive en otro montón, o su clase no está
 * cargada de este lado— así que se codifica a bytes, se pasa, y se reconstruye. Lo que no se puede
 * reconstruir se representa con una de éstas, que conserva el nombre de la clase y el mensaje.
 *
 * <p>El constructor es de paquete: nadie de afuera fabrica una. Aparecen sólo al decodificar.
 */
public final class TranslatedException extends Exception {

    private final String claseOriginal;

    TranslatedException(Throwable original) {
        super(original == null ? null : original.toString());
        this.claseOriginal = original == null ? null : original.getClass().getName();
    }

    /**
     * No captura la pila, y devuelve `this`.
     *
     * <p>Es la parte con intención de toda la clase. La pila de una excepción traducida sería la del
     * **decodificador** --el lugar donde se reconstruyó-- y no la del punto donde la excepción
     * original ocurrió, que es lo único que a alguien le importaría. Una pila que apunta al lugar
     * equivocado es peor que ninguna: se lee como si fuera la verdadera.
     */
    public Throwable fillInStackTrace() {
        return this;
    }

    /** El nombre de la clase original, o `null`. De paquete: es detalle de la traducción. */
    String claseOriginal() {
        return this.claseOriginal;
    }
}
