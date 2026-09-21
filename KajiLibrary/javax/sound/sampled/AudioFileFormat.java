package javax.sound.sampled;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * KajiLibrary's javax.sound.sampled.AudioFileFormat -- what there is in an audio file.
 *
 * <p>It puts together two things that get confused: the <b>file type</b> --WAVE, AU, AIFF-- and the
 * {@link AudioFormat} of the data it carries inside. They are independent: a WAVE can contain
 * 16-bit PCM or 8-bit mu-law.
 *
 * <p>{@link #getFrameLength} and {@link #getByteLength} can be {@link AudioSystem#NOT_SPECIFIED}
 * when the length is not known -- a stream arriving over the network, or a file whose header does
 * not say it. The byte one is also -1 unless the protected constructor is used, so it should not be
 * counted on.
 *
 * <p>It is immutable.
 */
public class AudioFileFormat {

    /** The file type. */
    private final Type type;

    /** The format of the data. */
    private final AudioFormat format;

    /** How many frames, or {@link AudioSystem#NOT_SPECIFIED}. */
    private final int frameLength;

    /** How many bytes in total, or {@link AudioSystem#NOT_SPECIFIED}. */
    private final int byteLength;

    /** What does not fit in the fixed fields. */
    private HashMap<String, Object> properties;

    /**
     * The constructor with a length in bytes, for whoever reads the file.
     *
     * <p>Protected because it only makes sense for a reader: whoever builds a format by hand does
     * not know how many bytes it is going to take.
     */
    protected AudioFileFormat(Type type, int byteLength, AudioFormat format, int frameLength) {
        this.type = type;
        this.byteLength = byteLength;
        this.format = format;
        this.frameLength = frameLength;
        this.properties = null;
    }

    /** The usual one; the byte length stays unspecified. */
    public AudioFileFormat(Type type, AudioFormat format, int frameLength) {
        this(type, AudioSystem.NOT_SPECIFIED, format, frameLength);
    }

    /**
     * Likewise, with properties.
     *
     * @throws NullPointerException if the map is null
     */
    public AudioFileFormat(Type type, AudioFormat format, int frameLength,
                           Map<String, Object> properties) {
        this(type, AudioSystem.NOT_SPECIFIED, format, frameLength);
        this.properties = new HashMap<String, Object>(properties);
    }

    /** The file type. */
    public Type getType() {
        return this.type;
    }

    /** How many bytes, or {@link AudioSystem#NOT_SPECIFIED}. See the class note. */
    public int getByteLength() {
        return this.byteLength;
    }

    /** The format of the data. */
    public AudioFormat getFormat() {
        return this.format;
    }

    /** How many frames, or {@link AudioSystem#NOT_SPECIFIED}. */
    public int getFrameLength() {
        return this.frameLength;
    }

    /** The properties, read-only. */
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

    /** The type with its extension, the data format, and the length if known. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.type != null) {
            sb.append(this.type).append(" (.").append(this.type.getExtension()).append(") file");
        } else {
            sb.append("unknown file format");
        }
        if (this.byteLength != AudioSystem.NOT_SPECIFIED) {
            sb.append(", byte length: ").append(this.byteLength);
        }
        sb.append(", data format: ").append(this.format);
        if (this.frameLength != AudioSystem.NOT_SPECIFIED) {
            sb.append(", frame length: ").append(this.frameLength);
        }
        return sb.toString();
    }

    /**
     * A type of audio file.
     *
     * <p>It is not an enum, for the same reason as {@link AudioFormat.Encoding}: a provider can
     * bring its own types. Each one also carries its usual extension.
     *
     * <p>{@link #AIFC} has a peculiarity that shows in its {@code toString}: it is called {@code
     * "AIFF-C"} and not {@code "AIFC"}. It is AIFF with compression, and the name says so.
     */
    public static class Type {

        /** Microsoft's WAV. */
        public static final Type WAVE = new Type("WAVE", "wav");

        /** Sun's AU. */
        public static final Type AU = new Type("AU", "au");

        /** Apple's AIFF. */
        public static final Type AIFF = new Type("AIFF", "aif");

        /** AIFF with compression. See the class note. */
        public static final Type AIFC = new Type("AIFF-C", "aifc");

        /** The same format as {@link #AU}, with another extension. */
        public static final Type SND = new Type("SND", "snd");

        /** The name, which is the identity. */
        private final String name;

        /** The usual extension, without the dot. */
        private final String extension;

        /**
         * @param name the name; it is the only thing that tells one type from another
         * @param extension the usual extension, without the dot
         */
        public Type(String name, String extension) {
            this.name = name;
            this.extension = extension;
        }

        /** By name; the extension does not count. */
        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Type)) {
                return false;
            }
            Type other = (Type) obj;
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

        /** The usual extension, without the dot. */
        public String getExtension() {
            return this.extension;
        }
    }
}
