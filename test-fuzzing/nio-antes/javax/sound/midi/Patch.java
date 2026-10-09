package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.Patch -- the address of a sound in a bank.
 *
 * <p>Two numbers: the bank and the program. MIDI defines 128 programs, which fell short almost
 * right away; banks are the patch the standard added to have 128 times more.
 *
 * <p>That is why two are needed: the program number alone is ambiguous, and a {@code Patch} is what
 * identifies a sound unambiguously within a {@link Soundbank}.
 *
 * <p>It is immutable.
 */
public class Patch {

    /** Which bank. */
    private final int bank;

    /** Which program within the bank. */
    private final int program;

    /**
     * @param bank the bank
     * @param program the program within the bank
     */
    public Patch(int bank, int program) {
        this.bank = bank;
        this.program = program;
    }

    /** Which bank. */
    public int getBank() {
        return this.bank;
    }

    /** Which program. */
    public int getProgram() {
        return this.program;
    }
}
