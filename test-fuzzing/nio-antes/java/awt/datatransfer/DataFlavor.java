package java.awt.datatransfer;

import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * A format transferred data can be handed over in.
 *
 * <p>It is **two things at once**, and not seeing that is the cause of nearly all the confusion
 * around this class:
 *
 * <ul>
 *   <li>a <strong>MIME type</strong>, which says what the data is — `text/plain`, `image/png`;
 *   <li>a <strong>representation class</strong>, which says which Java object it arrives in —
 *       {@code String}, {@code InputStream}, {@code java.util.List}.
 * </ul>
 *
 * <p>Both are needed because they are different questions. `text/plain` may arrive as a string, as a
 * reader or as a byte stream, and whoever receives it cannot treat them alike. That is why a Java
 * format's MIME type carries the `class=` parameter inside it: it is the MIME type that bears the
 * class.
 *
 * <p>Equality follows that logic and it surprises: two formats are equal if **the MIME type and the
 * class** coincide, and the human-readable name does not count. Renaming a format does not make it
 * another one, because that name is there to be shown to a person.
 *
 * <p>{@link #match} is the loose comparison, the one that ignores the rest of the MIME type's
 * parameters — such as a text's encoding. It serves to ask "is this text?" without demanding that it
 * be exactly the same text.
 */
public class DataFlavor implements Externalizable, Cloneable {

    private static final long serialVersionUID = 8367026044764648243L;

    /** The MIME type of a serialized Java object. */
    public static final String javaSerializedObjectMimeType =
            "application/x-java-serialized-object";

    /**
     * The MIME type of a reference to an object **of this very virtual machine**.
     *
     * <p>Nothing is serialized: the reference is passed. It only serves inside the same process, and
     * that is why such a format does not cross over to the system clipboard.
     */
    public static final String javaJVMLocalObjectMimeType = "application/x-java-jvm-local-objectref";

    /** The MIME type of a reference to a remote object. */
    public static final String javaRemoteObjectMimeType = "application/x-java-remote-object";

    /** Java text, as a {@code String}. */
    public static final DataFlavor stringFlavor =
            new DataFlavor(String.class, "Unicode String");

    /** An image, as a {@code java.awt.Image}. */
    public static final DataFlavor imageFlavor =
            new DataFlavor("image/x-java-image; class=java.awt.Image", "Image");

    /**
     * Plain text, as a {@code java.io.Reader}.
     *
     * @deprecated its MIME type says `charset=unicode`, which is not a real character set, and the
     *     implementations never agreed on what it meant. Use {@link #stringFlavor} or
     *     {@link #getTextPlainUnicodeFlavor}.
     */
    @Deprecated
    public static final DataFlavor plainTextFlavor =
            new DataFlavor("text/plain; charset=unicode; class=java.io.InputStream", "Plain Text");

    /** A list of files, as a {@code java.util.List} of {@code java.io.File}. */
    public static final DataFlavor javaFileListFlavor =
            new DataFlavor("application/x-java-file-list; class=java.util.List",
                    "application/x-java-file-list");

    /** The HTML of what was selected, without the context around it. */
    public static final DataFlavor selectionHtmlFlavor =
            new DataFlavor("text/html; class=java.lang.String; document=selection; "
                    + "charset=Unicode", "HTML Selection");

    /** The HTML of what was selected plus the tags needed for it to make sense. */
    public static final DataFlavor fragmentHtmlFlavor =
            new DataFlavor("text/html; class=java.lang.String; document=fragment; "
                    + "charset=Unicode", "HTML Fragment");

    /** The whole HTML document. */
    public static final DataFlavor allHtmlFlavor =
            new DataFlavor("text/html; class=java.lang.String; document=all; charset=Unicode",
                    "HTML All");

    private String mimeType;
    private String primaryType;
    private String subType;
    private Map<String, String> parameters;
    private Class<?> representationClass;
    private String humanPresentableName;

    /**
     * An empty format, for deserializing.
     *
     * <p>The object that comes out is good for nothing until it is filled in by
     * {@link #readExternal}; it is there only because {@link Externalizable} demands it.
     */
    public DataFlavor() {
        this.mimeType = null;
        this.representationClass = null;
        this.humanPresentableName = null;
        this.parameters = new HashMap<String, String>();
    }

    /**
     * A serialized-object format with that representation class.
     *
     * @throws NullPointerException if the class is `null`
     */
    public DataFlavor(Class<?> representationClass, String humanPresentableName) {
        if (representationClass == null) {
            throw new NullPointerException("representationClass");
        }
        this.build(javaSerializedObjectMimeType + "; class=" + representationClass.getName(),
                humanPresentableName, representationClass);
    }

    /**
     * A format out of its MIME type.
     *
     * @throws IllegalArgumentException if the MIME type is malformed
     * @throws NullPointerException if the MIME type is `null`
     */
    public DataFlavor(String mimeType, String humanPresentableName) {
        if (mimeType == null) {
            throw new NullPointerException("mimeType");
        }
        this.build(mimeType, humanPresentableName, null);
    }

    /**
     * Like the previous one, loading the class with the given loader.
     *
     * @throws ClassNotFoundException if the class of `class=` cannot be loaded
     * @throws IllegalArgumentException if the MIME type is malformed
     * @throws NullPointerException if the MIME type is `null`
     */
    public DataFlavor(String mimeType, String humanPresentableName, ClassLoader classLoader)
            throws ClassNotFoundException {
        if (mimeType == null) {
            throw new NullPointerException("mimeType");
        }
        this.build(mimeType, humanPresentableName, null);
        String name = this.parameters.get("class");
        if (name != null) {
            this.representationClass = tryToLoadClass(name, classLoader);
        }
    }

    /**
     * A format out of its MIME type, with the human-readable name taken from the type itself.
     *
     * @throws ClassNotFoundException if the class of `class=` cannot be loaded
     * @throws NullPointerException if the MIME type is `null`
     */
    public DataFlavor(String mimeType) throws ClassNotFoundException {
        if (mimeType == null) {
            throw new NullPointerException("mimeType");
        }
        this.build(mimeType, null, null);
        String name = this.parameters.get("class");
        if (name != null) {
            this.representationClass = tryToLoadClass(name, null);
        }
    }

    /**
     * Splits the MIME type and stores everything.
     *
     * @throws IllegalArgumentException if the MIME type does not have the form `type/subtype`
     */
    private void build(String mimeType, String humanPresentableName, Class<?> repClass) {
        this.parameters = new HashMap<String, String>();
        String[] parts = mimeType.split(";");
        String base = parts[0].trim();
        int slash = base.indexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException("failed to parse:" + mimeType);
        }
        this.primaryType = base.substring(0, slash).trim().toLowerCase();
        this.subType = base.substring(slash + 1).trim().toLowerCase();
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].trim();
            int eq = p.indexOf('=');
            if (eq > 0) {
                String key = p.substring(0, eq).trim();
                String value = p.substring(eq + 1).trim();
                // The quotes around a MIME parameter's value are not part of the value.
                if (value.length() >= 2 && value.charAt(0) == '"'
                        && value.charAt(value.length() - 1) == '"') {
                    value = value.substring(1, value.length() - 1);
                }
                this.parameters.put(key, value);
            }
        }
        this.mimeType = mimeType;
        if (repClass != null) {
            this.representationClass = repClass;
        } else {
            String name = this.parameters.get("class");
            if (name == null) {
                // Without `class=`, the default representation is a byte stream: it is the only
                // thing any MIME type can be handed over as without knowing anything more about it.
                this.representationClass = InputStream.class;
            } else {
                try {
                    this.representationClass = tryToLoadClass(name, null);
                } catch (ClassNotFoundException e) {
                    this.representationClass = null;
                }
            }
        }
        if (humanPresentableName != null) {
            this.humanPresentableName = humanPresentableName;
        } else {
            this.humanPresentableName = this.primaryType + "/" + this.subType;
        }
    }

    /**
     * Loads a class by name.
     *
     * <p>It tries the given loader, then the thread's context one and lastly this class's. It is the
     * JDK's order, and the one that makes a format defined in a separately loaded module resolvable
     * from another.
     *
     * @throws ClassNotFoundException if none of them finds it
     */
    protected static final Class<?> tryToLoadClass(String className, ClassLoader fallback)
            throws ClassNotFoundException {
        if (fallback != null) {
            try {
                return Class.forName(className, true, fallback);
            } catch (ClassNotFoundException e) {
                // The other loaders are still tried.
            }
        }
        ClassLoader thread = Thread.currentThread().getContextClassLoader();
        if (thread != null) {
            try {
                return Class.forName(className, true, thread);
            } catch (ClassNotFoundException e) {
                // Likewise.
            }
        }
        return Class.forName(className);
    }

    /** The whole MIME type, with its parameters. */
    public String getMimeType() {
        return this.mimeType;
    }

    /** Which Java class the data arrives in. */
    public Class<?> getRepresentationClass() {
        return this.representationClass;
    }

    /** The name to show a person. */
    public String getHumanPresentableName() {
        return this.humanPresentableName;
    }

    /** The part of the MIME type before the slash. */
    public String getPrimaryType() {
        return this.primaryType;
    }

    /** The part after the slash. */
    public String getSubType() {
        return this.subType;
    }

    /**
     * The value of one of the MIME type's parameters.
     *
     * <p>`humanPresentableName` is answered from the human-readable name and not from the
     * parameters, an oddity of the API that is preserved.
     *
     * @return the value, or `null` if the parameter is not there
     */
    public String getParameter(String paramName) {
        if ("humanPresentableName".equals(paramName)) {
            return this.humanPresentableName;
        }
        return this.parameters.get(paramName);
    }

    /** Changes the name to show; it does not change the format's identity. */
    public void setHumanPresentableName(String humanPresentableName) {
        this.humanPresentableName = humanPresentableName;
    }

    /** Equality by MIME type and representation class; the human-readable name does not count. */
    public boolean equals(Object o) {
        return o instanceof DataFlavor && this.equals((DataFlavor) o);
    }

    /** The same, with the type already known. */
    public boolean equals(DataFlavor that) {
        if (that == null) {
            return false;
        }
        if (this == that) {
            return true;
        }
        if (this.representationClass == null) {
            if (that.getRepresentationClass() != null) {
                return false;
            }
        } else if (!this.representationClass.equals(that.getRepresentationClass())) {
            return false;
        }
        if (this.primaryType == null) {
            return that.primaryType == null;
        }
        if (!this.primaryType.equals(that.primaryType) || !this.subType.equals(that.subType)) {
            return false;
        }
        // Text compares the encoding as well: two texts in different character sets are not the
        // same format, even if they say the same thing.
        if (this.isFlavorTextType()) {
            String a = this.getParameter("charset");
            String b = that.getParameter("charset");
            if (a == null) {
                return b == null;
            }
            return a.equalsIgnoreCase(b);
        }
        return true;
    }

    /**
     * Whether the MIME type equals that string.
     *
     * @deprecated it does not compare the representation class, so it calls equal two formats that
     *     hand over different objects. Use {@link #isMimeTypeEqual(String)}.
     */
    @Deprecated
    public boolean equals(String s) {
        if (s == null || this.mimeType == null) {
            return false;
        }
        return this.isMimeTypeEqual(s);
    }

    public int hashCode() {
        int total = 0;
        if (this.primaryType != null) {
            total = total + this.primaryType.hashCode();
        }
        if (this.subType != null) {
            total = total + this.subType.hashCode();
        }
        if (this.representationClass != null) {
            total = total + this.representationClass.hashCode();
        }
        return total;
    }

    /**
     * The loose comparison: same type and subtype and same class, ignoring the rest of the
     * parameters.
     *
     * <p>It is what has to be used to ask "is this text?" without demanding that it be the same text
     * with the same encoding.
     */
    public boolean match(DataFlavor that) {
        if (that == null) {
            return false;
        }
        if (this.primaryType == null) {
            return that.primaryType == null;
        }
        if (!this.primaryType.equals(that.primaryType) || !this.subType.equals(that.subType)) {
            return false;
        }
        if (this.representationClass == null) {
            return that.representationClass == null;
        }
        return this.representationClass.equals(that.representationClass);
    }

    /**
     * Whether the MIME type equals that one, ignoring the parameters.
     *
     * @throws NullPointerException if the string is `null`
     * @throws IllegalArgumentException if the string is not a valid MIME type
     */
    public boolean isMimeTypeEqual(String mimeType) {
        if (mimeType == null) {
            throw new NullPointerException("mimeType");
        }
        if (this.mimeType == null) {
            return false;
        }
        String other = mimeType.split(";")[0].trim().toLowerCase();
        return other.equals(this.primaryType + "/" + this.subType);
    }

    /** The same, against another format. */
    public final boolean isMimeTypeEqual(DataFlavor dataFlavor) {
        if (dataFlavor == null) {
            return false;
        }
        return this.isMimeTypeEqual(dataFlavor.getMimeType());
    }

    /** Whether it is a serialized Java object. */
    public boolean isMimeTypeSerializedObject() {
        return this.isMimeTypeEqual(javaSerializedObjectMimeType);
    }

    /** The class the data arrives in if the MIME type does not say otherwise. */
    public final Class<?> getDefaultRepresentationClass() {
        return InputStream.class;
    }

    /** That class's name. */
    public final String getDefaultRepresentationClassAsString() {
        return this.getDefaultRepresentationClass().getName();
    }

    /** Whether the data arrives as a byte stream. */
    public boolean isRepresentationClassInputStream() {
        return InputStream.class.isAssignableFrom(this.representationClass);
    }

    /** Whether the data arrives as a character reader. */
    public boolean isRepresentationClassReader() {
        return this.representationClass != null
                && Reader.class.isAssignableFrom(this.representationClass);
    }

    /** Whether the data arrives as a character buffer. */
    public boolean isRepresentationClassCharBuffer() {
        return CharBuffer.class.equals(this.representationClass);
    }

    /** Whether the data arrives as a byte buffer. */
    public boolean isRepresentationClassByteBuffer() {
        return ByteBuffer.class.equals(this.representationClass);
    }

    /** Whether the data arrives as a serializable object. */
    public boolean isRepresentationClassSerializable() {
        return this.representationClass != null
                && Serializable.class.isAssignableFrom(this.representationClass);
    }

    /** Whether the data arrives as a remote object. */
    public boolean isRepresentationClassRemote() {
        return this.representationClass != null
                && java.rmi.Remote.class.isAssignableFrom(this.representationClass);
    }

    /** Whether it is a serialized object that also arrives as a serializable class. */
    public boolean isFlavorSerializedObjectType() {
        return this.isRepresentationClassSerializable() && this.isMimeTypeSerializedObject();
    }

    /** Whether it is a reference to a remote object. */
    public boolean isFlavorRemoteObjectType() {
        return this.isRepresentationClassRemote()
                && this.isRepresentationClassSerializable()
                && this.isMimeTypeEqual(javaRemoteObjectMimeType);
    }

    /** Whether it is the file list. */
    public boolean isFlavorJavaFileListType() {
        if (this.mimeType == null || this.representationClass == null) {
            return false;
        }
        return java.util.List.class.isAssignableFrom(this.representationClass)
                && this.isMimeTypeEqual(javaFileListFlavor.mimeType);
    }

    /**
     * Whether the format is text that can be read as characters.
     *
     * <p>It is not enough for the MIME type to begin with `text/`: it also has to be handed over in
     * a class good for reading text. A `text/plain` arriving as any old object is not text for these
     * purposes.
     */
    public boolean isFlavorTextType() {
        if (!"text".equals(this.primaryType)) {
            return false;
        }
        Class<?> c = this.representationClass;
        if (c == null) {
            return false;
        }
        return String.class.equals(c) || Reader.class.isAssignableFrom(c)
                || CharBuffer.class.equals(c) || char[].class.equals(c)
                || InputStream.class.isAssignableFrom(c) || ByteBuffer.class.equals(c)
                || byte[].class.equals(c);
    }

    /** Plain Unicode text, handed over as a {@code String}. */
    public static final DataFlavor getTextPlainUnicodeFlavor() {
        return new DataFlavor("text/plain; charset=UTF-8; class=java.lang.String", "Plain Text");
    }

    /**
     * The best of those text formats.
     *
     * <p>The criterion is the JDK's: the one handing over characters —{@code String},
     * {@code Reader}, {@code CharBuffer}— is preferred over the one handing over bytes, because with
     * bytes the encoding has to be guessed. Between two of the same category the one appearing first
     * wins.
     *
     * @return the best, or `null` if the array is `null`, is empty or brings no text at all
     */
    public static final DataFlavor selectBestTextFlavor(DataFlavor[] availableFlavors) {
        if (availableFlavors == null || availableFlavors.length == 0) {
            return null;
        }
        DataFlavor best = null;
        int bestScore = -1;
        for (int i = 0; i < availableFlavors.length; i++) {
            DataFlavor f = availableFlavors[i];
            if (f == null || !f.isFlavorTextType()) {
                continue;
            }
            Class<?> c = f.getRepresentationClass();
            int score;
            if (String.class.equals(c)) {
                score = 3;
            } else if (Reader.class.isAssignableFrom(c) || CharBuffer.class.equals(c)
                    || char[].class.equals(c)) {
                score = 2;
            } else {
                score = 1;
            }
            if (score > bestScore) {
                bestScore = score;
                best = f;
            }
        }
        return best;
    }

    /**
     * A reader over the text that source hands over in this format.
     *
     * <p>It wraps whatever the source returns —a string, a reader, a stream— in a {@code Reader},
     * decoding with the MIME type's character set if need be.
     *
     * @throws IllegalArgumentException if this format is not a text one
     * @throws UnsupportedFlavorException if the source does not admit it
     * @throws IOException if the data cannot be read
     */
    public Reader getReaderForText(Transferable transferable)
            throws UnsupportedFlavorException, IOException {
        Object data = transferable.getTransferData(this);
        if (data == null) {
            throw new IllegalArgumentException("getTransferData() returned null");
        }
        if (data instanceof Reader) {
            return (Reader) data;
        }
        if (data instanceof String) {
            return new StringReader((String) data);
        }
        if (data instanceof CharBuffer) {
            CharBuffer cb = (CharBuffer) data;
            char[] chars = new char[cb.remaining()];
            cb.get(chars);
            return new java.io.CharArrayReader(chars);
        }
        if (data instanceof char[]) {
            return new java.io.CharArrayReader((char[]) data);
        }
        InputStream in;
        if (data instanceof InputStream) {
            in = (InputStream) data;
        } else if (data instanceof ByteBuffer) {
            ByteBuffer bb = (ByteBuffer) data;
            byte[] bytes = new byte[bb.remaining()];
            bb.get(bytes);
            in = new java.io.ByteArrayInputStream(bytes);
        } else if (data instanceof byte[]) {
            in = new java.io.ByteArrayInputStream((byte[]) data);
        } else {
            throw new IllegalArgumentException("transferable is not a text flavor");
        }
        String charset = this.getParameter("charset");
        if (charset == null) {
            return new java.io.InputStreamReader(in);
        }
        return new java.io.InputStreamReader(in, charset);
    }

    /** Writes the format out to serialize it. */
    public synchronized void writeExternal(ObjectOutput os) throws IOException {
        os.writeObject(this.mimeType);
        os.writeObject(this.humanPresentableName);
    }

    /**
     * Reads a serialized format.
     *
     * @throws ClassNotFoundException if the representation class cannot be loaded
     * @throws IOException if the stream is malformed
     */
    public synchronized void readExternal(ObjectInput is)
            throws IOException, ClassNotFoundException {
        String type = (String) is.readObject();
        String name = (String) is.readObject();
        if (type != null) {
            this.build(type, name, null);
        }
    }

    /**
     * A copy.
     *
     * @throws CloneNotSupportedException never: this class declares `Cloneable`
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Normalizes the value of one of the MIME type's parameters.
     *
     * @deprecated the JDK no longer calls it; normalization is inside the MIME type's parsing.
     */
    @Deprecated
    protected String normalizeMimeTypeParameter(String parameterName, String parameterValue) {
        return parameterValue;
    }

    /**
     * Normalizes a MIME type.
     *
     * @deprecated the JDK no longer calls it, for the same reason.
     */
    @Deprecated
    protected String normalizeMimeType(String mimeType) {
        return mimeType;
    }

    public String toString() {
        return this.getClass().getName() + "[mimetype=" + this.primaryType + "/" + this.subType
                + ";representationclass="
                + (this.representationClass == null ? "null" : this.representationClass.getName())
                + "]";
    }
}
