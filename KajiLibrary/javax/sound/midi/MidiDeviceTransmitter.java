package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MidiDeviceTransmitter -- a transmitter that knows which device it
 * belongs to.
 *
 * <p>The mirror of {@link MidiDeviceReceiver}, with the same reasons and the same way of querying
 * it.
 */
public interface MidiDeviceTransmitter extends Transmitter {

    /** Which device it belongs to. */
    MidiDevice getMidiDevice();
}
