package javax.sound.sampled;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * KajiLibrary's javax.sound.sampled.AudioFormat -- how the audio data is encoded.
 *
 * <p>It describes a strip of sound bytes: with which encoding, at which rate, with how many bits
 * per sample, how many channels, and in which byte order.
 *
 * <h2>Sample, frame, and why they are two things</h2>
 *
 * <p>A <b>sample</b> is one value of one channel; a <b>frame</b> is all the samples of one instant.
 * In 16-bit stereo, the sample is 2 bytes and the frame is 4.
 *
 * <p>The distinction matters because the positions and lengths of this package are measured in
 * frames, not in bytes nor in samples. Confusing them gives audio at double speed or with the
 * channels crossed.
 *
 * <h2>{@link #matches} is not symmetric</h2>
 *
 * <p>It is the surprising part. {@code a.matches(b)} asks whether <b>b</b> describes something
 * compatible with a, treating the {@link AudioSystem#NOT_SPECIFIED}s <b>of b</b> as wildcards.
 *
 * <p>So a concrete format matches an underspecified one, and not the other way round. It is right
 * for what it is used for --asking a line whether it accepts what I have-- and it has to be read in
 * the right order.
 *
 * <p>The byte order is only compared when there are more than 8 bits per sample: with one byte per
 * sample there is no order to argue about.
 *
 * <h2>The properties</h2>
 *
 * <p>The optional map carries what does not fit in the fixed fields: the bit rate of a compressed
 * format, the quality, whether it is variable-rate. The keys are defined by convention and an
 * implementation can add its own.
 */
public class AudioFormat {

    /** The encoding. */
    protected AudioFormat.Encoding encoding;

    /** Samples per second, or {@link AudioSystem#NOT_SPECIFIED}. */
    protected float sampleRate;

    /** Bits per sample, or {@link AudioSystem#NOT_SPECIFIED}. */
    protected int sampleSizeInBits;

    /** How many channels. */
    protected int channels;

    /** Bytes per frame. See the class note. */
    protected int frameSize;

    /** Frames per second. */
    protected float frameRate;

    /** Whether the most significant byte goes first. */
    protected boolean bigEndian;

    /** What does not fit in the fixed fields; read-only. */
    private HashMap<String, Object> properties;

    /**
     * The full constructor.
     *
     * @param frameSize bytes per frame; see the class note
     */
    public AudioFormat(AudioFormat.Encoding encoding, float sampleRate, int sampleSizeInBits,
                       int channels, int frameSize, float frameRate, boolean bigEndian) {
        this.encoding = encoding;
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.frameSize = frameSize;
        this.frameRate = frameRate;
        this.bigEndian = bigEndian;
        this.properties = null;
    }

    /**
     * Likewise, with properties.
     *
     * @param properties it is copied; later changes to the map do not affect the format
     * @throws NullPointerException if the map is null
     */
    public AudioFormat(AudioFormat.Encoding encoding, float sampleRate, int sampleSizeInBits,
                       int channels, int frameSize, float frameRate, boolean bigEndian,
                       Map<String, Object> properties) {
        this(encoding, sampleRate, sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
        this.properties = new HashMap<String, Object>(properties);
    }

    /**
     * The shortcut for linear PCM, which is the normal case.
     *
     * <p>It deduces the encoding from the signed boolean, and computes the frame size and rate:
     * {@code (bits + 7) / 8 * channels} bytes per frame, and the frame rate equal to the sample
     * rate. It is the only coherent thing in PCM.
     */
    public AudioFormat(float sampleRate, int sampleSizeInBits, int channels, boolean signed,
                       boolean bigEndian) {
        this(signed ? Encoding.PCM_SIGNED : Encoding.PCM_UNSIGNED,
             sampleRate, sampleSizeInBits, channels,
             (channels == AudioSystem.NOT_SPECIFIED
              || sampleSizeInBits == AudioSystem.NOT_SPECIFIED)
                 ? AudioSystem.NOT_SPECIFIED
                 : ((sampleSizeInBits + 7) / 8) * channels,
             sampleRate, bigEndian);
    }

    /** The encoding. */
    public AudioFormat.Encoding getEncoding() {
        return this.encoding;
    }

    /** Samples per second. */
    public float getSampleRate() {
        return this.sampleRate;
    }

    /** Bits per sample. */
    public int getSampleSizeInBits() {
        return this.sampleSizeInBits;
    }

    /** How many channels. */
    public int getChannels() {
        return this.channels;
    }

    /** Bytes per frame. See the class note. */
    public int getFrameSize() {
        return this.frameSize;
    }

    /** Frames per second. */
    public float getFrameRate() {
        return this.frameRate;
    }

    /** Whether the most significant byte goes first. */
    public boolean isBigEndian() {
        return this.bigEndian;
    }

    /** The properties, read-only; empty if there are none. */
    public Map<String, Object> properties() {
        Map<String, Object> ret;
        if (this.properties == null) {
            ret = new HashMap<String, Object>(0);
        } else {
            ret = new HashMap<String, Object>(this.properties);
        }
        return Collections.unmodifiableMap(ret);
    }

    /** A property, or null. */
    public Object getProperty(String key) {
        if (this.properties == null) {
            return null;
        }
        return this.properties.get(key);
    }

    /**
     * Whether that format is compatible with this one.
     *
     * <p>See the class note: it is not symmetric, and the wildcards are <b>the argument's</b>.
     */
    public boolean matches(AudioFormat format) {
        if (format.getEncoding() == null || getEncoding() == null) {
            return false;
        }
        if (!format.getEncoding().equals(getEncoding())) {
            return false;
        }
        if (format.getChannels() != AudioSystem.NOT_SPECIFIED
            && format.getChannels() != getChannels()) {
            return false;
        }
        if (format.getSampleRate() != (float) AudioSystem.NOT_SPECIFIED
            && format.getSampleRate() != getSampleRate()) {
            return false;
        }
        if (format.getSampleSizeInBits() != AudioSystem.NOT_SPECIFIED
            && format.getSampleSizeInBits() != getSampleSizeInBits()) {
            return false;
        }
        if (format.getFrameRate() != (float) AudioSystem.NOT_SPECIFIED
            && format.getFrameRate() != getFrameRate()) {
            return false;
        }
        if (format.getFrameSize() != AudioSystem.NOT_SPECIFIED
            && format.getFrameSize() != getFrameSize()) {
            return false;
        }
        // With one byte per sample there is no byte order to argue about.
        return getSampleSizeInBits() <= 8
            || format.isBigEndian() == isBigEndian();
    }

    /**
     * A readable description.
     *
     * <p>The encoding, and then the parts that are known, separated by commas. The frame rate only
     * appears if it differs from the sample rate --in PCM they are equal and repeating it would be
     * noise-- and the byte order only for PCM with more than 8 bits per sample or an unknown sample
     * size. (The note said the byte order shows whenever there are more than 8 bits; the code, like
     * JDK 25, leaves it out for a 16-bit ULAW format.)
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (getEncoding() != null) {
            sb.append(getEncoding().toString()).append(' ');
        }
        StringBuilder parts = new StringBuilder();
        appendPart(parts, sampleRateText());
        appendPart(parts, sampleSizeText());
        appendPart(parts, channelsText());
        appendPart(parts, frameSizeText());
        String rate = frameRateText();
        if (rate != null) {
            appendPart(parts, rate);
        }
        String endian = endianText();
        if (endian != null) {
            appendPart(parts, endian);
        }
        return sb.append(parts).toString();
    }

    /** Adds a part, with the comma that goes with it. */
    private static void appendPart(StringBuilder sb, String part) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(part);
    }

    private String sampleRateText() {
        if (getSampleRate() == (float) AudioSystem.NOT_SPECIFIED) {
            return "unknown sample rate";
        }
        return getSampleRate() + " Hz";
    }

    private String sampleSizeText() {
        if (getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED) {
            return "unknown bits per sample";
        }
        return getSampleSizeInBits() + " bit";
    }

    private String channelsText() {
        if (getChannels() == 1) {
            return "mono";
        }
        if (getChannels() == 2) {
            return "stereo";
        }
        if (getChannels() == AudioSystem.NOT_SPECIFIED) {
            return "unknown number of channels";
        }
        return getChannels() + " channels";
    }

    private String frameSizeText() {
        if (getFrameSize() == AudioSystem.NOT_SPECIFIED) {
            return "unknown frame size";
        }
        return getFrameSize() + " bytes/frame";
    }

    /** Only if it differs from the sample rate; null if there is nothing to say. */
    private String frameRateText() {
        if (Math.abs(getSampleRate() - getFrameRate()) <= 0.00001) {
            return null;
        }
        if (getFrameRate() == (float) AudioSystem.NOT_SPECIFIED) {
            return "unknown frame rate";
        }
        return getFrameRate() + " frames/second";
    }

    /** Only for PCM of more than 8 bits, or of unknown size; null if it does not apply. */
    private String endianText() {
        if (getEncoding() == null) {
            return null;
        }
        boolean pcm = getEncoding().equals(Encoding.PCM_SIGNED)
            || getEncoding().equals(Encoding.PCM_UNSIGNED);
        if (!pcm) {
            return null;
        }
        if (getSampleSizeInBits() <= 8 && getSampleSizeInBits() != AudioSystem.NOT_SPECIFIED) {
            return null;
        }
        if (isBigEndian()) {
            return "big-endian";
        }
        return "little-endian";
    }

    /**
     * An audio encoding.
     *
     * <p>It is not an enum on purpose: the constructor is public so that a provider can declare
     * encodings the platform does not know. The five constants are the ones the JDK names.
     *
     * <p>Equality is by name, so a custom encoding with the same name as a standard one <b>is</b>
     * the standard one.
     */
    public static class Encoding {

        /** Signed linear PCM. The most common. */
        public static final Encoding PCM_SIGNED = new Encoding("PCM_SIGNED");

        /** Unsigned linear PCM; the usual one in 8 bits. */
        public static final Encoding PCM_UNSIGNED = new Encoding("PCM_UNSIGNED");

        /** Floating-point PCM. */
        public static final Encoding PCM_FLOAT = new Encoding("PCM_FLOAT");

        /** Mu-law logarithmic compression, from telephony. */
        public static final Encoding ULAW = new Encoding("ULAW");

        /** A-law logarithmic compression, from telephony. */
        public static final Encoding ALAW = new Encoding("ALAW");

        /** The name, which is the identity. */
        private final String name;

        /** @param name the name; it is the only thing that tells one encoding from another */
        public Encoding(String name) {
            this.name = name;
        }

        /** By name. */
        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Encoding)) {
                return false;
            }
            Encoding other = (Encoding) obj;
            if (this.name == null) {
                return other.name == null;
            }
            return this.name.equals(other.name);
        }

        /** The name's. */
        @Override
        public final int hashCode() {
            if (this.name == null) {
                return 0;
            }
            return this.name.hashCode();
        }

        /** The name. */
        @Override
        public final String toString() {
            return this.name;
        }
    }
}
