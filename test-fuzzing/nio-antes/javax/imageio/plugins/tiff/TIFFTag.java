package javax.imageio.plugins.tiff;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * KajiLibrary's javax.imageio.plugins.tiff.TIFFTag -- a TIFF metadata tag.
 *
 * <p>A TIFF file is a list of numbered tags. This class describes <b>one</b> of them: what number
 * it has, what it is called, which data types it accepts and how many values it carries.
 *
 * <h2>The types go in a bit mask</h2>
 *
 * <p>{@link #getDataTypes} does not return a type but an {@code int} where bit {@code n} means type
 * {@code n} is allowed. It is built with {@code 1 << TIFF_SHORT}, and checked with
 * {@link #isDataTypeOK}.
 *
 * <p>The reason is that TIFF allows the same tag to come in several types: an image's height may be
 * {@code SHORT} or {@code LONG} depending on the size. A reader has to accept both.
 *
 * <h2>{@link #getCount} often returns -1</h2>
 *
 * <p>It means "any number", not "none". It is normal for text tags and colour tables, where the
 * count depends on the image.
 *
 * <h2>Tags that point to another directory</h2>
 *
 * <p>The three-argument constructor that takes a {@link TIFFTagSet} builds a <b>pointer</b> tag:
 * its value is not data but the position of another tag directory. It is how TIFF nests metadata
 * --Exif, GPS-- inside a file.
 *
 * <p>A tag like that is recognized with {@link #isIFDPointer}, and its associated set says which
 * tags to expect on the other side.
 *
 * <h2>Value names</h2>
 *
 * <p>Many tags store a number that means something --1 is "uncompressed", 5 is "LZW".
 * {@link #addValueName} lets that translation be registered, and it is a <b>protected</b>
 * operation: only a subclass can call it, typically from its constructor.
 *
 * <p>That is on purpose: the names are part of the tag's definition, not something added to it
 * later.
 */
public class TIFFTag {

    /** 8-bit unsigned integer. */
    public static final int TIFF_BYTE = 1;

    /** Zero-terminated text. */
    public static final int TIFF_ASCII = 2;

    /** 16-bit unsigned integer. */
    public static final int TIFF_SHORT = 3;

    /** 32-bit unsigned integer. */
    public static final int TIFF_LONG = 4;

    /** Two unsigned 32-bit integers: numerator and denominator. */
    public static final int TIFF_RATIONAL = 5;

    /** 8-bit signed integer. */
    public static final int TIFF_SBYTE = 6;

    /** Uninterpreted bytes. */
    public static final int TIFF_UNDEFINED = 7;

    /** 16-bit signed integer. */
    public static final int TIFF_SSHORT = 8;

    /** 32-bit signed integer. */
    public static final int TIFF_SLONG = 9;

    /** Two signed integers: numerator and denominator. */
    public static final int TIFF_SRATIONAL = 10;

    /** 32-bit floating point. */
    public static final int TIFF_FLOAT = 11;

    /** 64-bit floating point. */
    public static final int TIFF_DOUBLE = 12;

    /** Pointer to another directory. See the class note. */
    public static final int TIFF_IFD_POINTER = 13;

    /** The smallest type. */
    public static final int MIN_DATATYPE = 1;

    /** The largest type. */
    public static final int MAX_DATATYPE = 13;

    /** The name given to a tag that is in no known set. */
    public static final String UNKNOWN_TAG_NAME = "UnknownTag";

    /** How many bytes a value of each type takes; position 0 is not used. */
    private static final int[] SIZE_OF_TYPE = {
        0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8, 4,
    };

    /** What it is called. */
    private final String name;

    /** What number it has. */
    private final int number;

    /** The mask of allowed types. See the class note. */
    private final int dataTypes;

    /** How many values, or -1. */
    private final int count;

    /** Which set it points to, if it is a pointer. */
    private final TIFFTagSet tagSet;

    /** The translation from values to names, if there is one. */
    private SortedMap<Integer, String> valueNames = null;

    /**
     * A complete tag.
     *
     * @param dataTypes the type mask; see the class note
     * @param count how many values, or -1 for any
     * @throws NullPointerException if the name is null
     * @throws IllegalArgumentException if the number is negative, the mask has bits out of range,
     *     or the count is negative without being -1
     */
    public TIFFTag(String name, int number, int dataTypes, int count) {
        this(name, number, dataTypes, count, null);
    }

    /**
     * A tag that points to another directory. See the class note.
     *
     * @throws NullPointerException if the name or the set are null
     */
    public TIFFTag(String name, int number, TIFFTagSet tagSet) {
        this(name, number, 1 << TIFF_LONG | 1 << TIFF_IFD_POINTER, 1, checkSet(tagSet));
    }

    /**
     * A tag without a fixed count.
     *
     * @throws NullPointerException if the name is null
     */
    public TIFFTag(String name, int number, int dataTypes) {
        this(name, number, dataTypes, -1, null);
    }

    /** The only real constructor; the three public ones delegate here. */
    private TIFFTag(String name, int number, int dataTypes, int count, TIFFTagSet tagSet) {
        if (name == null) {
            throw new NullPointerException("name == null");
        }
        if (number < 0) {
            throw new IllegalArgumentException("number (" + number + ") < 0");
        }
        if (dataTypes < 0 || (dataTypes & ~((1 << (MAX_DATATYPE + 1)) - 2)) != 0) {
            throw new IllegalArgumentException("dataTypes out of range");
        }
        if (count < -1) {
            throw new IllegalArgumentException("count (" + count + ") < -1");
        }
        this.name = name;
        this.number = number;
        this.dataTypes = dataTypes;
        this.count = count;
        this.tagSet = tagSet;
    }

    /**
     * How many bytes a value of that type takes.
     *
     * @throws IllegalArgumentException if it is not a valid type
     */
    public static int getSizeOfType(int dataType) {
        if (dataType < MIN_DATATYPE || dataType > MAX_DATATYPE) {
            throw new IllegalArgumentException("dataType out of range!");
        }
        return SIZE_OF_TYPE[dataType];
    }

    /** What it is called. */
    public String getName() {
        return this.name;
    }

    /** What number it has. */
    public int getNumber() {
        return this.number;
    }

    /** The mask of allowed types. See the class note. */
    public int getDataTypes() {
        return this.dataTypes;
    }

    /** How many values, or -1 for any. See the class note. */
    public int getCount() {
        return this.count;
    }

    /** Whether that type is allowed. */
    public boolean isDataTypeOK(int dataType) {
        if (dataType < MIN_DATATYPE || dataType > MAX_DATATYPE) {
            return false;
        }
        return (this.dataTypes & (1 << dataType)) != 0;
    }

    /** Which set it points to, or null if it is not a pointer. */
    public TIFFTagSet getTagSet() {
        return this.tagSet;
    }

    /** Whether its value is the position of another directory. See the class note. */
    public boolean isIFDPointer() {
        return this.tagSet != null;
    }

    /** Whether it has names for its values. */
    public boolean hasValueNames() {
        return this.valueNames != null;
    }

    /**
     * Gives a value a name. Protected; see the class note.
     *
     * @param value the number that appears in the file
     * @param name what it means
     */
    protected void addValueName(int value, String name) {
        if (this.valueNames == null) {
            this.valueNames = new TreeMap<Integer, String>();
        }
        this.valueNames.put(Integer.valueOf(value), name);
    }

    /** What that value means, or null if it has no name. */
    public String getValueName(int value) {
        if (this.valueNames == null) {
            return null;
        }
        return this.valueNames.get(Integer.valueOf(value));
    }

    /** The values that have names, sorted; null if there are none. */
    public int[] getNamedValues() {
        if (this.valueNames == null) {
            return null;
        }
        int[] out = new int[this.valueNames.size()];
        int i = 0;
        java.util.Iterator<Integer> it = this.valueNames.keySet().iterator();
        while (it.hasNext()) {
            out[i] = it.next().intValue();
            i = i + 1;
        }
        return out;
    }

    /** The check the pointer constructor has to do before delegating. */
    private static TIFFTagSet checkSet(TIFFTagSet tagSet) {
        if (tagSet == null) {
            throw new NullPointerException("tagSet == null");
        }
        return tagSet;
    }
}
