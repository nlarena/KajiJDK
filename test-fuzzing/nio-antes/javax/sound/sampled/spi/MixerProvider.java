package javax.sound.sampled.spi;

import javax.sound.sampled.Mixer;

/**
 * KajiLibrary's javax.sound.sampled.spi.MixerProvider -- provides audio devices.
 *
 * <p>What is implemented by whoever connects the platform with real sound cards, or whoever writes
 * a software mixer. It is registered as a service and {@code AudioSystem} finds it by itself.
 *
 * <p>{@link #getMixer} with null has a special meaning: <b>the default mixer</b> of this provider.
 * It is what lets {@code AudioSystem.getMixer(null)} work without anybody having to choose by name.
 *
 * <p>{@link #isMixerSupported} comes implemented over {@link #getMixerInfo}; a subclass need not
 * touch it.
 */
public abstract class MixerProvider {

    /** For the subclasses. */
    protected MixerProvider() {
    }

    /** Whether this provider has that mixer. */
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

    /** The mixers it provides. */
    public abstract Mixer.Info[] getMixerInfo();

    /**
     * That mixer; null asks for the default one. See the class note.
     *
     * @throws IllegalArgumentException if it has none like that
     */
    public abstract Mixer getMixer(Mixer.Info info);
}
