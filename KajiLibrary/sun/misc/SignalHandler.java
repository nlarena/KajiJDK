package sun.misc;

/**
 * Que hacer cuando llega una senal del sistema operativo.
 *
 * <h2>Las dos constantes no son manejadores comunes</h2>
 *
 * <p>{@link #SIG_DFL} y {@link #SIG_IGN} no ejecutan codigo Java: son marcas que le dicen al
 * sistema operativo "haceme lo de siempre" y "no me avises". Pasarlas a {@link Signal#handle}
 * desinstala el manejador de Java en vez de instalar otro.
 *
 * <p>Por eso llamar a su {@link #handle} directamente no tiene sentido, y por eso los nombres son
 * los de C —{@code SIG_DFL}, {@code SIG_IGN}— y no algo mas parecido a Java: representan lo mismo
 * que representan alla.
 *
 * <h2>Lo que se puede hacer adentro</h2>
 *
 * <p>Poco. El manejador corre en un hilo aparte que la VM levanta para esto, pero el proceso puede
 * estar por morirse: con {@code SIGTERM} lo unico razonable es avisarle a alguien y volver rapido.
 * Un manejador que se cuelgue impide que el proceso termine.
 *
 * @since 1.2
 */
public interface SignalHandler {

    /**
     * El manejador que tenia el sistema operativo antes de que Java se metiera.
     *
     * <p>Instalarlo devuelve la senal a su comportamiento normal: con {@code SIGINT}, terminar el
     * proceso.
     */
    SignalHandler SIG_DFL = new Marca("SIG_DFL");

    /** La marca de "ignorar esta senal". */
    SignalHandler SIG_IGN = new Marca("SIG_IGN");

    /**
     * Atiende la senal.
     *
     * @param sig la senal que llego
     */
    void handle(Signal sig);
}

/**
 * Las dos marcas de {@link SignalHandler}.
 *
 * <p>Con nombre y no anonimas por #482: el generador de bytecode no emite una clase anonima que
 * este en el inicializador de un campo. Ademas se lee mejor en un volcado de pila que un
 * {@code SignalHandler$1}.
 */
final class Marca implements SignalHandler {

    private final String nombre;

    Marca(String nombre) {
        this.nombre = nombre;
    }

    public void handle(Signal sig) {
        throw new UnsupportedOperationException(
                nombre + " es una marca para el sistema operativo, no un manejador que se ejecute");
    }

    public String toString() {
        return nombre;
    }
}
