package javax.sound.sampled.spi;

import javax.sound.sampled.Mixer;

/**
 * KajiLibrary's javax.sound.sampled.spi.MixerProvider -- trae dispositivos de audio.
 *
 * <p>Lo que implementa quien conecta la plataforma con placas de sonido reales, o quien escribe un
 * mezclador por software. Se registra como servicio y {@code AudioSystem} lo encuentra solo.
 *
 * <p>{@link #getMixer} con null tiene un significado especial: <b>el mezclador por omision</b> de este
 * proveedor. Es lo que permite que {@code AudioSystem.getMixer(null)} funcione sin que nadie tenga que
 * elegir por nombre.
 *
 * <p>{@link #isMixerSupported} viene implementado sobre {@link #getMixerInfo}; una subclase no
 * necesita tocarlo.
 */
public abstract class MixerProvider {

    /** Para las subclases. */
    protected MixerProvider() {
    }

    /** Si este proveedor tiene ese mezclador. */
    public boolean isMixerSupported(Mixer.Info info) {
        Mixer.Info[] all = getMixerInfo();
        int i = 0;
        while (all != null && i < all.length) {
            if (all[i].equals(info)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /** Los mezcladores que trae. */
    public abstract Mixer.Info[] getMixerInfo();

    /**
     * Ese mezclador; null pide el de omision. Ver la nota de la clase.
     *
     * @throws IllegalArgumentException si no tiene ninguno asi
     */
    public abstract Mixer getMixer(Mixer.Info info);
}
