package javax.sound.midi.spi;

import javax.sound.midi.MidiDevice;

/**
 * KajiLibrary's javax.sound.midi.spi.MidiDeviceProvider -- trae dispositivos MIDI.
 *
 * <p>Lo que implementa quien conecta la plataforma con puertos MIDI reales, o quien escribe un
 * sintetizador o un secuenciador por software. Se registra como servicio y {@code MidiSystem} lo
 * encuentra solo.
 *
 * <p>{@link #isDeviceSupported} viene implementado sobre {@link #getDeviceInfo} comparando por
 * igualdad. Como los {@link MidiDevice.Info} se comparan por <b>identidad</b>, eso significa que solo
 * reconoce los objetos que el mismo proveedor devolvio -- que es justamente lo que corresponde.
 */
public abstract class MidiDeviceProvider {

    /** Para las subclases. */
    protected MidiDeviceProvider() {
    }

    /** Si este proveedor tiene ese dispositivo. Ver la nota de la clase. */
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

    /** Los dispositivos que trae. */
    public abstract MidiDevice.Info[] getDeviceInfo();

    /**
     * Ese dispositivo, sin abrir.
     *
     * @throws IllegalArgumentException si no tiene ninguno asi
     */
    public abstract MidiDevice getDevice(MidiDevice.Info info);
}
