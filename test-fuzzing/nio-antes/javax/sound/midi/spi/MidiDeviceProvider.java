package javax.sound.midi.spi;

import javax.sound.midi.MidiDevice;

/**
 * KajiLibrary's javax.sound.midi.spi.MidiDeviceProvider -- provides MIDI devices.
 *
 * <p>What is implemented by whoever connects the platform with real MIDI ports, or whoever writes a
 * software synthesizer or sequencer. It is registered as a service and {@code MidiSystem} finds it
 * by itself.
 *
 * <p>{@link #isDeviceSupported} comes implemented over {@link #getDeviceInfo}, comparing by
 * equality. Since {@link MidiDevice.Info}s are compared by <b>identity</b>, that means it only
 * recognizes the objects the same provider returned -- which is precisely what is right.
 */
public abstract class MidiDeviceProvider {

    /** For the subclasses. */
    protected MidiDeviceProvider() {
    }

    /** Whether this provider has that device. See the class note. */
    public boolean isDeviceSupported(MidiDevice.Info info) {
        MidiDevice.Info[] all = getDeviceInfo();
        int i = 0;
        while (all != null && i < all.length) {
            if (all[i].equals(info)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /** The devices it provides. */
    public abstract MidiDevice.Info[] getDeviceInfo();

    /**
     * That device, not opened.
     *
     * @throws IllegalArgumentException if it has none like that
     */
    public abstract MidiDevice getDevice(MidiDevice.Info info);
}
