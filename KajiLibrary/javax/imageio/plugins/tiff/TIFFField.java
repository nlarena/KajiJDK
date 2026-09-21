package javax.imageio.plugins.tiff;

import javax.imageio.metadata.IIOMetadataNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.TIFFField -- a tag with its value.
 *
 * <p>{@link TIFFTag} describes a tag; this is an <b>instance</b> of that tag in a concrete file,
 * with the data.
 *
 * <h2>The data is an {@link Object} and its class depends on the type</h2>
 *
 * <p>It is what has to be understood before touching {@link #getData}:
 *
 * <table border="1">
 * <caption>Which array each type carries</caption>
 * <tr><th>type</th><th>class of the data</th></tr>
 * <tr><td>BYTE, SBYTE, UNDEFINED</td><td>{@code byte[]}</td></tr>
 * <tr><td>ASCII</td><td>{@code String[]}</td></tr>
 * <tr><td>SHORT</td><td>{@code char[]}</td></tr>
 * <tr><td>SSHORT</td><td>{@code short[]}</td></tr>
 * <tr><td>LONG, IFD_POINTER</td><td>{@code long[]}</td></tr>
 * <tr><td>SLONG</td><td>{@code int[]}</td></tr>
 * <tr><td>RATIONAL</td><td>{@code long[][]}</td></tr>
 * <tr><td>SRATIONAL</td><td>{@code int[][]}</td></tr>
 * <tr><td>FLOAT</td><td>{@code float[]}</td></tr>
 * <tr><td>DOUBLE</td><td>{@code double[]}</td></tr>
 * </table>
 *
 * <p>The two that surprise are SHORT in {@code char[]} --because a TIFF short is <b>unsigned</b>,
 * and {@code char} is Java's only unsigned integer-- and LONG in {@code long[]}, for the same
 * reason: a TIFF long is 32 unsigned bits, which do not fit in an {@code int}.
 *
 * <h2>The plural {@code getAsXxx()} are casts, not conversions</h2>
 *
 * <p>Except {@link #getAsInts}, which does convert from {@code char[]} and {@code short[]}, the
 * methods that return the whole array are <b>casts</b>: {@link #getAsDoubles} on a RATIONAL throws
 * {@link ClassCastException}, it does not divide.
 *
 * <p>The ones that take an index --{@link #getAsLong}, {@link #getAsDouble}-- do convert, and are
 * the safe way to read a value without knowing what type it came in.
 *
 * <h2>Rationals</h2>
 *
 * <p>A RATIONAL is <b>two</b> integers: numerator and denominator. {@link #getAsRational} returns
 * the pair; {@link #getAsDouble} does the division. A TIFF's resolution is stored that way, which
 * is why 300 dots per inch shows up as {@code 300/1}.
 *
 * <p>{@link #getValueAsString} of a rational returns {@code "72/1"}, not {@code "72.0"}: it is
 * the format's textual form, not the result of the division.
 */
public final class TIFFField implements Cloneable {

    /** What each type is called. Position 0 is not used. */
    private static final String[] TYPE_NAMES = {
        null, "Byte", "Ascii", "Short", "Long", "Rational", "SByte", "Undefined", "SShort",
        "SLong", "SRational", "Float", "Double", "IFDPointer",
    };

    /** Which tag it is. */
    private final TIFFTag tag;

    /** What type the data is. */
    private final int type;

    /** How many values. */
    private final int count;

    /** The array; its class depends on the type. See the class note. */
    private Object data;

    /** The directory it points to, if it is a pointer tag. */
    private TIFFDirectory dir = null;

    /**
     * A tag with its data.
     *
     * <p>The array is <b>not</b> copied, but it is validated: it has to be of the class the type
     * asks for and exactly {@code count} long.
     *
     * @param type one of the {@code TIFF_} constants of {@link TIFFTag}
     * @param count how many values
     * @param data the array of the class that corresponds to the type
     * @throws NullPointerException if the tag or the data are null
     * @throws IllegalArgumentException if the type does not exist, if it does not work for that
     *     tag, if the count is negative, if the type is a pointer and the count is not one, or if
     *     the array is not of the class or length the type asks for
     */
    public TIFFField(TIFFTag tag, int type, int count, Object data) {
        if (tag == null) {
            throw new NullPointerException("tag == null!");
        }
        if (type < TIFFTag.MIN_DATATYPE || type > TIFFTag.MAX_DATATYPE) {
            throw new IllegalArgumentException("Unknown data type " + type);
        }
        if (!tag.isDataTypeOK(type)) {
            throw new IllegalArgumentException("Illegal data type " + type + " for "
                + tag.getName() + " tag");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count < 0!");
        }
        if (type == TIFFTag.TIFF_IFD_POINTER && count != 1) {
            throw new IllegalArgumentException("Type is TIFF_IFD_POINTER and count != 1");
        }
        if (data == null) {
            throw new NullPointerException("data == null!");
        }
        if (!isDataOK(type, count, data)) {
            throw new IllegalArgumentException("Illegal class or length for data array");
        }
        this.tag = tag;
        this.type = type;
        this.count = count;
        this.data = data;
    }

    /**
     * A tag without data: an empty array of the type, of the requested length.
     *
     * @throws NullPointerException if the tag is null
     * @throws IllegalArgumentException if the type does not work or the count is negative
     */
    public TIFFField(TIFFTag tag, int type, int count) {
        this(tag, type, count, createArrayForType(type, count));
    }

    /**
     * A single integer value, with the smallest type that holds it.
     *
     * <p>It picks SHORT if the value fits in 16 unsigned bits and LONG otherwise. Careful: it picks
     * <b>before</b> looking at which types the tag accepts, so a 7 in a tag that only accepts LONG
     * does not end up as LONG but as an {@link IllegalArgumentException}. It is like that in the
     * JDK too.
     *
     * @throws NullPointerException if the tag is null
     * @throws IllegalArgumentException if the value is negative, or if the chosen type does not
     *     work for that tag
     */
    public TIFFField(TIFFTag tag, long value) {
        if (tag == null) {
            throw new NullPointerException("tag == null!");
        }
        if (value < 0) {
            throw new IllegalArgumentException("value < 0!");
        }
        if (value > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("value > 0xffffffff!");
        }
        this.tag = tag;
        this.count = 1;
        if (value < 0x10000L) {
            this.type = TIFFTag.TIFF_SHORT;
            this.data = new char[] { (char) value };
        } else {
            this.type = TIFFTag.TIFF_LONG;
            this.data = new long[] { value };
        }
        if (!tag.isDataTypeOK(this.type)) {
            throw new IllegalArgumentException("Illegal data type " + TYPE_NAMES[this.type]
                + " for tag \"" + tag.getName() + "\"");
        }
    }

    /**
     * A pointer tag, with the directory it points to.
     *
     * @param type it has to be {@link TIFFTag#TIFF_LONG} or {@link TIFFTag#TIFF_IFD_POINTER}
     * @param offset the position of the directory in the file; it has to be positive
     * @throws NullPointerException if the tag or the directory are null
     * @throws IllegalArgumentException if the type is not one of those two, if it does not work for
     *     the tag, or if the offset is not positive
     */
    public TIFFField(TIFFTag tag, int type, long offset, TIFFDirectory dir) {
        if (type != TIFFTag.TIFF_LONG && type != TIFFTag.TIFF_IFD_POINTER) {
            throw new IllegalArgumentException("type " + type
                + " is neither TIFFTag.TIFF_LONG nor TIFFTag.TIFF_IFD_POINTER");
        }
        if (tag == null) {
            throw new NullPointerException("tag == null!");
        }
        if (!tag.isDataTypeOK(type)) {
            throw new IllegalArgumentException("Illegal data type " + type + " for "
                + tag.getName() + " tag");
        }
        // A zero or negative offset is not a possible position: the first eight bytes of a TIFF are
        // the header, so no directory starts at 0.
        if (offset <= 0) {
            throw new IllegalArgumentException("offset " + offset + " is non-positive");
        }
        if (dir == null) {
            throw new NullPointerException("dir == null");
        }
        this.tag = tag;
        this.type = type;
        this.count = 1;
        this.data = new long[] { offset };
        this.dir = dir;
    }

    /** Which tag it is. */
    public TIFFTag getTag() {
        return this.tag;
    }

    /** Its number. */
    public int getTagNumber() {
        return this.tag.getNumber();
    }

    /** What type the data is. */
    public int getType() {
        return this.type;
    }

    /**
     * What that type is called: {@code "Rational"}, {@code "SLong"}...
     *
     * @throws IllegalArgumentException if it is not one of the thirteen
     */
    public static String getTypeName(int dataType) {
        if (dataType < TIFFTag.MIN_DATATYPE || dataType > TIFFTag.MAX_DATATYPE) {
            throw new IllegalArgumentException("Unknown data type " + dataType);
        }
        return TYPE_NAMES[dataType];
    }

    /**
     * The number of that type, or -1 if it does not exist.
     *
     * <p>Case-sensitive: {@code "SRational"} yes, {@code "SRATIONAL"} no.
     */
    public static int getTypeByName(String typeName) {
        int i = TIFFTag.MIN_DATATYPE;
        while (i <= TIFFTag.MAX_DATATYPE) {
            if (TYPE_NAMES[i].equals(typeName)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /**
     * An empty array of the class that type asks for. See the class note.
     *
     * @throws IllegalArgumentException if the type is not one of the thirteen, if the count is
     *     negative, or if the type is a pointer and the count is not one
     */
    public static Object createArrayForType(int dataType, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0!");
        }
        // A pointer points to one directory, not several: the count is always one.
        if (dataType == TIFFTag.TIFF_IFD_POINTER && count != 1) {
            throw new IllegalArgumentException("Type is TIFF_IFD_POINTER and count != 1");
        }
        if (dataType == TIFFTag.TIFF_BYTE || dataType == TIFFTag.TIFF_SBYTE
            || dataType == TIFFTag.TIFF_UNDEFINED) {
            return new byte[count];
        }
        if (dataType == TIFFTag.TIFF_ASCII) {
            return new String[count];
        }
        if (dataType == TIFFTag.TIFF_SHORT) {
            return new char[count];
        }
        if (dataType == TIFFTag.TIFF_SSHORT) {
            return new short[count];
        }
        if (dataType == TIFFTag.TIFF_LONG || dataType == TIFFTag.TIFF_IFD_POINTER) {
            return new long[count];
        }
        if (dataType == TIFFTag.TIFF_SLONG) {
            return new int[count];
        }
        if (dataType == TIFFTag.TIFF_RATIONAL) {
            return new long[count][2];
        }
        if (dataType == TIFFTag.TIFF_SRATIONAL) {
            return new int[count][2];
        }
        if (dataType == TIFFTag.TIFF_FLOAT) {
            return new float[count];
        }
        if (dataType == TIFFTag.TIFF_DOUBLE) {
            return new double[count];
        }
        throw new IllegalArgumentException("Unknown data type " + dataType);
    }

    /**
     * This field as a tree of the TIFF native format.
     *
     * <p>The normal shape is a {@code TIFFField} with the number and the name, a child with the
     * <b>plural</b> of the type --{@code TIFFLongs}-- and one grandchild per value. The
     * pluralization is not cosmetic: it is how the native format tells the field apart from its
     * values.
     *
     * <p>UNDEFINED is the exception: it has no plural and no node per value, but a single {@code
     * TIFFUndefined} with all the bytes comma-separated and <b>unsigned</b>. It makes sense --an
     * UNDEFINED can have thousands of bytes and an element for each is not worth it-- but it has to
     * be kept in mind when walking the tree.
     */
    public Node getAsNativeNode() {
        IIOMetadataNode field = new IIOMetadataNode("TIFFField");
        field.setAttribute("number", Integer.toString(getTagNumber()));
        String tagName = this.tag.getName();
        if (tagName != null) {
            field.setAttribute("name", tagName);
        }
        if (this.type == TIFFTag.TIFF_UNDEFINED) {
            IIOMetadataNode values = new IIOMetadataNode("TIFFUndefined");
            byte[] bytes = (byte[]) this.data;
            StringBuilder text = new StringBuilder();
            int i = 0;
            while (i < this.count) {
                if (i > 0) {
                    text.append(',');
                }
                text.append(bytes[i] & 0xFF);
                i = i + 1;
            }
            values.setAttribute("value", text.toString());
            field.appendChild(values);
            return field;
        }
        String typeName = TYPE_NAMES[this.type];
        IIOMetadataNode values = new IIOMetadataNode("TIFF" + typeName + "s");
        int i = 0;
        while (i < this.count) {
            IIOMetadataNode value = new IIOMetadataNode("TIFF" + typeName);
            value.setAttribute("value", getValueAsString(i));
            values.appendChild(value);
            i = i + 1;
        }
        field.appendChild(values);
        return field;
    }

    /**
     * Reads a field from a native format tree. The inverse of {@link #getAsNativeNode}.
     *
     * <p>The node's {@code name} attribute is ignored: the name comes from resolving the number
     * against {@code tagSet}. If there is no set, or the number is not in it, the tag is left
     * anonymous -- {@link TIFFTag#UNKNOWN_TAG_NAME}, count -1 and the type the tree carries as the
     * only accepted type.
     *
     * @param tagSet which set to resolve the number against; null leaves it anonymous
     * @throws IllegalArgumentException if the node is null or does not have the expected shape
     * @throws NullPointerException if the {@code number} attribute or a value's {@code value} is
     *     missing
     */
    public static TIFFField createFromMetadataNode(TIFFTagSet tagSet, Node node) {
        if (node == null) {
            // Wrapped on purpose: the method is specified to throw IllegalArgumentException,
            // but the JDK keeps the NullPointerException inside as the cause and this copies that.
            throw new IllegalArgumentException(new NullPointerException("node == null!"));
        }
        String name = node.getNodeName();
        if (!"TIFFField".equals(name)) {
            throw new IllegalArgumentException("!name.equals(\"TIFFField\")");
        }
        int number = Integer.parseInt(attribute(node, "number"));
        Node valuesNode = firstElement(node);
        if (valuesNode == null) {
            throw new IllegalArgumentException("TIFFField node has no children");
        }
        String container = valuesNode.getNodeName();
        int type;
        int count;
        Object data;
        if ("TIFFUndefined".equals(container)) {
            type = TIFFTag.TIFF_UNDEFINED;
            String[] pieces = attribute(valuesNode, "value").split(",");
            count = pieces.length;
            byte[] bytes = new byte[count];
            int i = 0;
            while (i < count) {
                bytes[i] = (byte) Integer.parseInt(pieces[i].trim());
                i = i + 1;
            }
            data = bytes;
        } else {
            // The JDK trims four in front and one behind without looking; a name that is not
            // "TIFF<type>s" then falls into getTypeByName, with the trim visible in the message.
            String typeName = container.substring(4, container.length() - 1);
            type = getTypeByName(typeName);
            if (type == -1) {
                throw new IllegalArgumentException("typeName = " + typeName);
            }
            NodeList children = valuesNode.getChildNodes();
            count = countElements(children);
            data = count == 0 ? null : createArrayForType(type, count);
            int at = 0;
            int i = 0;
            while (i < children.getLength()) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    setFromString(data, at, attribute(child, "value"));
                    at = at + 1;
                }
                i = i + 1;
            }
        }
        TIFFTag tag = null;
        if (tagSet != null) {
            tag = tagSet.getTag(number);
        }
        if (tag == null) {
            tag = new TIFFTag(TIFFTag.UNKNOWN_TAG_NAME, number, 1 << type);
        }
        try {
            return new TIFFField(tag, type, count, data);
        } catch (NullPointerException e) {
            // A container without values leaves the data null; the constructor then throws a
            // NullPointerException that this method has to present as an invalid argument.
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Whether the values are integers.
     *
     * <p>Rationals, floating point and text are not; UNDEFINED is, because it is bytes.
     */
    public boolean isIntegral() {
        return this.type != TIFFTag.TIFF_ASCII && this.type != TIFFTag.TIFF_RATIONAL
            && this.type != TIFFTag.TIFF_SRATIONAL && this.type != TIFFTag.TIFF_FLOAT
            && this.type != TIFFTag.TIFF_DOUBLE;
    }

    /** How many values. */
    public int getCount() {
        return this.count;
    }

    /** The array, not copied. See the class note about its class. */
    public Object getData() {
        return this.data;
    }

    /**
     * The data as bytes. It is a cast.
     *
     * @throws ClassCastException if the type does not store bytes
     */
    public byte[] getAsBytes() {
        return (byte[]) this.data;
    }

    /**
     * As chars; it is the SHORT type. It is a cast. See the class note.
     *
     * @throws ClassCastException if the type is not SHORT
     */
    public char[] getAsChars() {
        return (char[]) this.data;
    }

    /**
     * As signed shorts. It is a cast.
     *
     * @throws ClassCastException if the type is not SSHORT
     */
    public short[] getAsShorts() {
        return (short[]) this.data;
    }

    /**
     * As ints.
     *
     * <p>The only plural one that converts: it accepts {@code char[]} and {@code short[]} as well
     * as {@code int[]}. LONG does not fit in an int and you have to ask for {@link #getAsLongs}.
     *
     * @throws ClassCastException if the data is not char[], short[] or int[]
     */
    public int[] getAsInts() {
        if (this.data instanceof int[]) {
            return (int[]) this.data;
        }
        if (this.data instanceof char[]) {
            char[] source = (char[]) this.data;
            int[] result = new int[source.length];
            int i = 0;
            while (i < source.length) {
                result[i] = source[i] & 0xFFFF;
                i = i + 1;
            }
            return result;
        }
        if (this.data instanceof short[]) {
            short[] source = (short[]) this.data;
            int[] result = new int[source.length];
            int i = 0;
            while (i < source.length) {
                result[i] = source[i];
                i = i + 1;
            }
            return result;
        }
        throw new ClassCastException("Data not char[], short[], or int[]!");
    }

    /**
     * As longs. It is a cast.
     *
     * @throws ClassCastException if the type is not LONG or IFD_POINTER
     */
    public long[] getAsLongs() {
        return (long[]) this.data;
    }

    /**
     * As four-byte floating point. It is a cast.
     *
     * @throws ClassCastException if the type is not FLOAT
     */
    public float[] getAsFloats() {
        return (float[]) this.data;
    }

    /**
     * As eight-byte floating point. It is a cast: it does not divide rationals.
     *
     * @throws ClassCastException if the type is not DOUBLE
     */
    public double[] getAsDoubles() {
        return (double[]) this.data;
    }

    /**
     * As signed pairs. It is a cast.
     *
     * @throws ClassCastException if the type is not SRATIONAL
     */
    public int[][] getAsSRationals() {
        return (int[][]) this.data;
    }

    /**
     * As unsigned pairs. It is a cast.
     *
     * @throws ClassCastException if the type is not RATIONAL
     */
    public long[][] getAsRationals() {
        return (long[][]) this.data;
    }

    /**
     * Value number {@code index} as an int, converting whatever is needed.
     *
     * @throws ClassCastException if the type cannot be converted
     */
    public int getAsInt(int index) {
        return (int) getAsLong(index);
    }

    /**
     * As a long, converting: a byte is read unsigned, a float is truncated, a rational is divided
     * and truncated, and a text is parsed.
     *
     * @throws ClassCastException if the type cannot be converted
     * @throws NumberFormatException if it is text and not a number
     */
    public long getAsLong(int index) {
        if (this.data instanceof byte[]) {
            // SBYTE is the format's only signed byte; BYTE and UNDEFINED are read without sign.
            byte value = ((byte[]) this.data)[index];
            return this.type == TIFFTag.TIFF_SBYTE ? value : value & 0xFF;
        }
        if (this.data instanceof char[]) {
            return ((char[]) this.data)[index] & 0xFFFF;
        }
        if (this.data instanceof short[]) {
            return ((short[]) this.data)[index];
        }
        if (this.data instanceof int[]) {
            return ((int[]) this.data)[index];
        }
        if (this.data instanceof long[]) {
            return ((long[]) this.data)[index];
        }
        if (this.data instanceof String[]) {
            return Long.parseLong(((String[]) this.data)[index]);
        }
        return (long) getAsDouble(index);
    }

    /**
     * As floating point, converting.
     *
     * @throws ClassCastException if the type cannot be converted
     */
    public float getAsFloat(int index) {
        return (float) getAsDouble(index);
    }

    /**
     * As eight-byte floating point, converting: a rational is divided here, and a text is parsed.
     *
     * @throws ClassCastException if the type cannot be converted
     * @throws NumberFormatException if it is text and not a number
     */
    public double getAsDouble(int index) {
        if (this.data instanceof float[]) {
            return ((float[]) this.data)[index];
        }
        if (this.data instanceof double[]) {
            return ((double[]) this.data)[index];
        }
        if (this.data instanceof long[][]) {
            long[] pair = ((long[][]) this.data)[index];
            return (double) pair[0] / (double) pair[1];
        }
        if (this.data instanceof int[][]) {
            int[] pair = ((int[][]) this.data)[index];
            return (double) pair[0] / (double) pair[1];
        }
        if (this.data instanceof String[]) {
            return Double.parseDouble(((String[]) this.data)[index]);
        }
        return getAsLong(index);
    }

    /**
     * Text number {@code index}.
     *
     * @throws ClassCastException if the type is not ASCII
     */
    public String getAsString(int index) {
        return ((String[]) this.data)[index];
    }

    /**
     * Signed pair number {@code index}.
     *
     * @throws ClassCastException if the type is not SRATIONAL
     */
    public int[] getAsSRational(int index) {
        return ((int[][]) this.data)[index];
    }

    /**
     * Unsigned pair number {@code index}.
     *
     * @throws ClassCastException if the type is not RATIONAL
     */
    public long[] getAsRational(int index) {
        return ((long[][]) this.data)[index];
    }

    /**
     * Value number {@code index} in the format's textual form.
     *
     * <p>A rational comes out as {@code "72/1"}, undivided; a byte comes out unsigned. See the
     * class note.
     */
    public String getValueAsString(int index) {
        if (this.data instanceof String[]) {
            return ((String[]) this.data)[index];
        }
        if (this.data instanceof long[][]) {
            long[] pair = ((long[][]) this.data)[index];
            return pair[0] + "/" + pair[1];
        }
        if (this.data instanceof int[][]) {
            int[] pair = ((int[][]) this.data)[index];
            return pair[0] + "/" + pair[1];
        }
        if (this.data instanceof float[]) {
            return Float.toString(((float[]) this.data)[index]);
        }
        if (this.data instanceof double[]) {
            return Double.toString(((double[]) this.data)[index]);
        }
        return Long.toString(getAsLong(index));
    }

    /** Whether this tag points to another directory. */
    public boolean hasDirectory() {
        return this.dir != null;
    }

    /** The directory it points to, or null if it is not a pointer. */
    public TIFFDirectory getDirectory() {
        return this.dir;
    }

    /**
     * A copy with its own array.
     *
     * <p>The data is really copied --including the pairs of a rational, element by element--: a
     * copy that shared the array would not be a copy. The tag is shared, because it is immutable.
     */
    @Override
    public TIFFField clone() throws CloneNotSupportedException {
        TIFFField copy = (TIFFField) super.clone();
        copy.data = cloneData(this.data);
        return copy;
    }

    /** Copies the array, deeply if it is one of pairs. */
    private static Object cloneData(Object data) {
        if (data instanceof long[][]) {
            long[][] source = (long[][]) data;
            long[][] result = new long[source.length][];
            int i = 0;
            while (i < source.length) {
                result[i] = source[i].clone();
                i = i + 1;
            }
            return result;
        }
        if (data instanceof int[][]) {
            int[][] source = (int[][]) data;
            int[][] result = new int[source.length][];
            int i = 0;
            while (i < source.length) {
                result[i] = source[i].clone();
                i = i + 1;
            }
            return result;
        }
        if (data instanceof byte[]) {
            return ((byte[]) data).clone();
        }
        if (data instanceof char[]) {
            return ((char[]) data).clone();
        }
        if (data instanceof short[]) {
            return ((short[]) data).clone();
        }
        if (data instanceof int[]) {
            return ((int[]) data).clone();
        }
        if (data instanceof long[]) {
            return ((long[]) data).clone();
        }
        if (data instanceof float[]) {
            return ((float[]) data).clone();
        }
        if (data instanceof double[]) {
            return ((double[]) data).clone();
        }
        if (data instanceof String[]) {
            return ((String[]) data).clone();
        }
        return data;
    }

    /**
     * Whether the array is of the class and length the type asks for.
     *
     * <p>The length is checked just like the class: a {@code char[5]} with {@code count} 1 lies
     * about how many values there are, and the field would describe its own content wrongly.
     */
    private static boolean isDataOK(int type, int count, Object data) {
        if (type == TIFFTag.TIFF_BYTE || type == TIFFTag.TIFF_SBYTE
            || type == TIFFTag.TIFF_UNDEFINED) {
            return data instanceof byte[] && ((byte[]) data).length == count;
        }
        if (type == TIFFTag.TIFF_ASCII) {
            return data instanceof String[] && ((String[]) data).length == count;
        }
        if (type == TIFFTag.TIFF_SHORT) {
            return data instanceof char[] && ((char[]) data).length == count;
        }
        if (type == TIFFTag.TIFF_SSHORT) {
            return data instanceof short[] && ((short[]) data).length == count;
        }
        if (type == TIFFTag.TIFF_LONG || type == TIFFTag.TIFF_IFD_POINTER) {
            return data instanceof long[] && ((long[]) data).length == count;
        }
        if (type == TIFFTag.TIFF_SLONG) {
            return data instanceof int[] && ((int[]) data).length == count;
        }
        if (type == TIFFTag.TIFF_RATIONAL) {
            if (!(data instanceof long[][])) {
                return false;
            }
            long[][] pairs = (long[][]) data;
            if (pairs.length != count) {
                return false;
            }
            int i = 0;
            while (i < pairs.length) {
                if (pairs[i].length != 2) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        }
        if (type == TIFFTag.TIFF_SRATIONAL) {
            if (!(data instanceof int[][])) {
                return false;
            }
            int[][] pairs = (int[][]) data;
            if (pairs.length != count) {
                return false;
            }
            int i = 0;
            while (i < pairs.length) {
                if (pairs[i].length != 2) {
                    return false;
                }
                i = i + 1;
            }
            return true;
        }
        if (type == TIFFTag.TIFF_FLOAT) {
            return data instanceof float[] && ((float[]) data).length == count;
        }
        return data instanceof double[] && ((double[]) data).length == count;
    }

    /**
     * The value of that attribute.
     *
     * @throws NullPointerException if the attribute is not there, as in the JDK
     */
    private static String attribute(Node node, String name) {
        NamedNodeMap attrs = node.getAttributes();
        return attrs.getNamedItem(name).getNodeValue();
    }

    /** The first child that is an element, or null. */
    private static Node firstElement(Node node) {
        NodeList children = node.getChildNodes();
        int i = 0;
        while (i < children.getLength()) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return children.item(i);
            }
            i = i + 1;
        }
        return null;
    }

    /** How many elements there are in the list. */
    private static int countElements(NodeList children) {
        int total = 0;
        int i = 0;
        while (i < children.getLength()) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                total = total + 1;
            }
            i = i + 1;
        }
        return total;
    }

    /** Puts a value read from the tree in the position it belongs to. */
    private static void setFromString(Object data, int index, String value) {
        if (data instanceof String[]) {
            ((String[]) data)[index] = value;
        } else if (data instanceof byte[]) {
            ((byte[]) data)[index] = (byte) Integer.parseInt(value);
        } else if (data instanceof char[]) {
            ((char[]) data)[index] = (char) Long.parseLong(value);
        } else if (data instanceof short[]) {
            ((short[]) data)[index] = (short) Integer.parseInt(value);
        } else if (data instanceof int[]) {
            ((int[]) data)[index] = Integer.parseInt(value);
        } else if (data instanceof long[]) {
            ((long[]) data)[index] = Long.parseLong(value);
        } else if (data instanceof float[]) {
            ((float[]) data)[index] = Float.parseFloat(value);
        } else if (data instanceof double[]) {
            ((double[]) data)[index] = Double.parseDouble(value);
        } else if (data instanceof long[][]) {
            int slash = value.indexOf('/');
            ((long[][]) data)[index] = new long[] {
                Long.parseLong(value.substring(0, slash)),
                Long.parseLong(value.substring(slash + 1)),
            };
        } else if (data instanceof int[][]) {
            int slash = value.indexOf('/');
            ((int[][]) data)[index] = new int[] {
                Integer.parseInt(value.substring(0, slash)),
                Integer.parseInt(value.substring(slash + 1)),
            };
        }
    }
}
