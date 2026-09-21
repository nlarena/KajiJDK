package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MidiDeviceReceiver -- a receiver that knows which device it
 * belongs to.
 *
 * <p>It adds one method over {@link Receiver}. It exists because a program that handles several
 * devices ends up with a pile of loose receivers and no way of knowing which is which.
 *
 * <p>It is queried with {@code instanceof}: {@code MidiDevice.getReceiver()} declares a plain
 * {@link Receiver}, although in practice all the JDK's return one of these.
 */
public interface MidiDeviceReceiver extends Receiver {

    /** Which device it belongs to. */
    MidiDevice getMidiDevice();
}
