package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.Patch -- la direccion de un sonido en un banco.
 *
 * <p>Dos numeros: el banco y el programa. MIDI define 128 programas, que se quedaron cortos casi
 * enseguida; los bancos son el parche que agrego el estandar para tener 128 veces mas.
 *
 * <p>Por eso hacen falta dos: el numero de programa solo es ambiguo, y un {@code Patch} es lo que
 * identifica un sonido sin ambiguedad dentro de un {@link Soundbank}.
 *
 * <p>Es inmutable.
 */
public class Patch {

    /** Que banco. */
    private final int bank;

    /** Que programa dentro del banco. */
    private final int program;

    /**
     * @param bank el banco
     * @param program el programa dentro del banco
     */
    public Patch(int bank, int program) {
        this.bank = bank;
        this.program = program;
    }

    /** Que banco. */
    public int getBank() {
        return this.bank;
    }

    /** Que programa. */
    public int getProgram() {
        return this.program;
    }
}
