package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MidiDeviceTransmitter -- un transmisor que sabe de que dispositivo
 * es.
 *
 * <p>El espejo de {@link MidiDeviceReceiver}, con las mismas razones y la misma forma de consultarlo.
 */
public interface MidiDeviceTransmitter extends Transmitter {

    /** De que dispositivo es. */
    MidiDevice getMidiDevice();
}
