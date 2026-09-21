package javax.sound.sampled.spi;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * KajiLibrary's javax.sound.sampled.spi.FormatConversionProvider -- converts audio from one format
 * to another.
 *
 * <p>Decoding mu-law to PCM, changing the sample rate, going from stereo to mono. It is registered
 * as a service and {@code AudioSystem} finds it by itself.
 *
 * <h2>The conversion is lazy</h2>
 *
 * <p>{@code getAudioInputStream} returns a stream that converts <b>as it is read</b>, not an
 * already converted buffer. That is what allows converting an hour-long file without loading it
 * into memory, and what allows chaining converters.
 *
 * <h2>Encoding against complete format</h2>
 *
 * <p>The methods come in pairs: one takes an {@link AudioFormat.Encoding} and the other a whole
 * {@link AudioFormat}. The first says "turn it into PCM, you choose the rest"; the second says
 * exactly into what. The first is the useful one when only decompressing is needed.
 *
 * <p>The four query methods come implemented over the abstract ones; a subclass only needs the six
 * abstract ones. (The note said five.)
 */
public abstract class FormatConversionProvider {

    /** For the subclasses. */
    protected FormatConversionProvider() {
    }

    /** Which encodings it can start from. */
    public abstract AudioFormat.Encoding[] getSourceEncodings();

    /** Which ones it can arrive at. */
    public abstract AudioFormat.Encoding[] getTargetEncodings();

    /** Whether it can start from that one. */
    public boolean isSourceEncodingSupported(AudioFormat.Encoding sourceEncoding) {
        return contains(getSourceEncodings(), sourceEncoding);
    }

    /** Whether it can arrive at that one. */
    public boolean isTargetEncodingSupported(AudioFormat.Encoding targetEncoding) {
        return contains(getTargetEncodings(), targetEncoding);
    }

    /** Which encodings it can take that concrete format to. */
    public abstract AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat);

    /** Whether it can take that format to that encoding. */
    public boolean isConversionSupported(AudioFormat.Encoding targetEncoding,
                                         AudioFormat sourceFormat) {
        return contains(getTargetEncodings(sourceFormat), targetEncoding);
    }

    /** The concrete formats it can take it to. */
    public abstract AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding,
                                                   AudioFormat sourceFormat);

    /** Whether it can convert between those two formats. */
    public boolean isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        AudioFormat[] formats = getTargetFormats(targetFormat.getEncoding(), sourceFormat);
        int i = 0;
        while (formats != null && i < formats.length) {
            if (targetFormat.matches(formats[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * A stream that converts to that encoding while it is read.
     *
     * @throws IllegalArgumentException if it does not support that conversion
     */
    public abstract AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding,
                                                         AudioInputStream sourceStream);

    /**
     * Likewise, to a concrete format.
     *
     * @throws IllegalArgumentException if it does not support that conversion
     */
    public abstract AudioInputStream getAudioInputStream(AudioFormat targetFormat,
                                                         AudioInputStream sourceStream);

    /** Whether that encoding is in the array. */
    private static boolean contains(AudioFormat.Encoding[] all, AudioFormat.Encoding one) {
        int i = 0;
        while (all != null && i < all.length) {
            if (all[i].equals(one)) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }
}
