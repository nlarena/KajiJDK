package javax.imageio.metadata;

import java.util.Locale;
import javax.imageio.ImageTypeSpecifier;

/**
 * KajiLibrary's javax.imageio.metadata.IIOMetadataFormat -- the schema of a metadata tree.
 *
 * <p>A DTD or an XML schema, expressed as method calls instead of as a document. It says which
 * elements exist, which children each can have, and which attributes with which types and
 * ranges.
 *
 * <p>It exists because a metadata tree <b>can be edited</b>: a program reads it, modifies it and
 * writes it back. Without a schema, the only way to know whether what is left is valid would be to
 * try writing the file and see whether the encoder complains.
 *
 * <h2>The six child policies</h2>
 *
 * <p>It is the part with most content, and the three in the middle get confused:
 *
 * <ul>
 *   <li>{@link #CHILD_POLICY_EMPTY}: no children;
 *   <li>{@link #CHILD_POLICY_ALL}: <b>all</b> the declared children, in that order, mandatory;
 *   <li>{@link #CHILD_POLICY_SOME}: <b>some</b> of the declared ones, in that order. It is
 *       {@code ALL} but optional;
 *   <li>{@link #CHILD_POLICY_CHOICE}: <b>one</b> of the declared ones;
 *   <li>{@link #CHILD_POLICY_SEQUENCE}: any number, in any order, of the declared ones;
 *   <li>{@link #CHILD_POLICY_REPEAT}: any number of <b>a single</b> kind of child, with a minimum
 *       and a maximum. It is the only one where {@link #getElementMinChildren} means something.
 * </ul>
 *
 * <h2>The value types are a mask</h2>
 *
 * <p>{@link #VALUE_RANGE_MIN_INCLUSIVE} is 6, which is {@code VALUE_RANGE | 4}. The range constants
 * are built by combining {@link #VALUE_RANGE} with the two inclusiveness bits, which is why they
 * have to be compared with masks and not with equality.
 *
 * <p>{@link #VALUE_LIST} is separate: it means the attribute is a list of space-separated values,
 * and there {@link #getAttributeListMinLength} and its pair apply.
 *
 * <h2>{@link #canNodeAppear} depends on the image type</h2>
 *
 * <p>It is what makes this schema more expressive than a DTD. A palette node only makes sense in an
 * indexed image, and this method can say so by looking at the concrete
 * {@link ImageTypeSpecifier}.
 */
public interface IIOMetadataFormat {

    /** No children. */
    int CHILD_POLICY_EMPTY = 0;

    /** All the declared ones, in order. See the class note. */
    int CHILD_POLICY_ALL = 1;

    /** Some of the declared ones, in order. */
    int CHILD_POLICY_SOME = 2;

    /** One of the declared ones. */
    int CHILD_POLICY_CHOICE = 3;

    /** Any number and order of the declared ones. */
    int CHILD_POLICY_SEQUENCE = 4;

    /** Any number of a single kind. */
    int CHILD_POLICY_REPEAT = 5;

    /** The last policy; it serves to validate a value. */
    int CHILD_POLICY_MAX = 5;

    /** The attribute carries no value. */
    int VALUE_NONE = 0;

    /** Any value of the declared type. */
    int VALUE_ARBITRARY = 1;

    /** A value between a minimum and a maximum. See the class note. */
    int VALUE_RANGE = 2;

    /** The bit that says the minimum is included. */
    int VALUE_RANGE_MIN_INCLUSIVE_MASK = 4;

    /** The one that says the maximum is included. */
    int VALUE_RANGE_MAX_INCLUSIVE_MASK = 8;

    /** Range with the minimum included. */
    int VALUE_RANGE_MIN_INCLUSIVE = VALUE_RANGE | VALUE_RANGE_MIN_INCLUSIVE_MASK;

    /** Range with the maximum included. */
    int VALUE_RANGE_MAX_INCLUSIVE = VALUE_RANGE | VALUE_RANGE_MAX_INCLUSIVE_MASK;

    /** Range closed on both sides. */
    int VALUE_RANGE_MIN_MAX_INCLUSIVE =
        VALUE_RANGE | VALUE_RANGE_MIN_INCLUSIVE_MASK | VALUE_RANGE_MAX_INCLUSIVE_MASK;

    /** One of a closed list. */
    int VALUE_ENUMERATION = 16;

    /** A list of space-separated values. See the class note. */
    int VALUE_LIST = 32;

    /** The value is text. */
    int DATATYPE_STRING = 0;

    /** It is {@code true} or {@code false}. */
    int DATATYPE_BOOLEAN = 1;

    /** It is an integer. */
    int DATATYPE_INTEGER = 2;

    /** It is a four-byte floating point number. */
    int DATATYPE_FLOAT = 3;

    /** An eight-byte one. */
    int DATATYPE_DOUBLE = 4;

    /** What the root of the tree is called. */
    String getRootName();

    /**
     * Whether that element may appear in the tree of an image of that type.
     *
     * <p>See the class note: it is what a DTD cannot express.
     */
    boolean canNodeAppear(String elementName, ImageTypeSpecifier imageType);

    /**
     * How many children at least.
     *
     * <p>It only means something with {@link #CHILD_POLICY_REPEAT}.
     *
     * @throws IllegalArgumentException if the element does not exist
     */
    int getElementMinChildren(String elementName);

    /**
     * How many at most.
     *
     * @throws IllegalArgumentException if the element does not exist
     */
    int getElementMaxChildren(String elementName);

    /**
     * What that element is, in words.
     *
     * @param locale in which locale, or null for the system's
     */
    String getElementDescription(String elementName, Locale locale);

    /**
     * Which of the six child policies. See the class note.
     *
     * @throws IllegalArgumentException if the element does not exist
     */
    int getChildPolicy(String elementName);

    /**
     * Which children it can have.
     *
     * @return null if the policy is {@link #CHILD_POLICY_EMPTY}
     * @throws IllegalArgumentException if the element does not exist
     */
    String[] getChildNames(String elementName);

    /**
     * Which attributes it can have.
     *
     * @throws IllegalArgumentException if the element does not exist
     */
    String[] getAttributeNames(String elementName);

    /**
     * What shape the value of that attribute has. See the class note about the masks.
     *
     * @throws IllegalArgumentException if the element or the attribute does not exist
     */
    int getAttributeValueType(String elementName, String attrName);

    /**
     * What type it is.
     *
     * @throws IllegalArgumentException if the element or the attribute does not exist
     */
    int getAttributeDataType(String elementName, String attrName);

    /**
     * Whether it has to be present.
     *
     * @throws IllegalArgumentException if the element or the attribute does not exist
     */
    boolean isAttributeRequired(String elementName, String attrName);

    /**
     * What it is worth if not set, or null if there is no default.
     *
     * @throws IllegalArgumentException if the element or the attribute does not exist
     */
    String getAttributeDefaultValue(String elementName, String attrName);

    /**
     * The allowed values.
     *
     * @throws IllegalArgumentException if the attribute is not of enumeration type
     */
    String[] getAttributeEnumerations(String elementName, String attrName);

    /**
     * The minimum of the range, or null if there is no minimum.
     *
     * @throws IllegalArgumentException if the attribute is not of range type
     */
    String getAttributeMinValue(String elementName, String attrName);

    /**
     * The maximum, or null.
     *
     * @throws IllegalArgumentException if the attribute is not of range type
     */
    String getAttributeMaxValue(String elementName, String attrName);

    /**
     * How many values at least in the list.
     *
     * @throws IllegalArgumentException if the attribute is not of list type
     */
    int getAttributeListMinLength(String elementName, String attrName);

    /**
     * How many at most.
     *
     * @throws IllegalArgumentException if the attribute is not of list type
     */
    int getAttributeListMaxLength(String elementName, String attrName);

    /** What that attribute is, in words. */
    String getAttributeDescription(String elementName, String attrName, Locale locale);

    /**
     * What shape that element's user object has.
     *
     * <p>Elements that carry a piece of data that is not text --see
     * {@link IIOMetadataNode#getUserObject}-- declare it here.
     *
     * @return {@link #VALUE_NONE} if that element carries no object
     * @throws IllegalArgumentException if the element does not exist
     */
    int getObjectValueType(String elementName);

    /**
     * What class that object is.
     *
     * @throws IllegalArgumentException if the element carries no object
     */
    Class<?> getObjectClass(String elementName);

    /**
     * What object goes if none is set, or null.
     *
     * @throws IllegalArgumentException if the element carries no object
     */
    Object getObjectDefaultValue(String elementName);

    /**
     * The allowed objects.
     *
     * @throws IllegalArgumentException if the object is not of enumeration type
     */
    Object[] getObjectEnumerations(String elementName);

    /**
     * The minimum, if the object is a range.
     *
     * @throws IllegalArgumentException if it is not of range type
     */
    Comparable<?> getObjectMinValue(String elementName);

    /**
     * The maximum.
     *
     * @throws IllegalArgumentException if it is not of range type
     */
    Comparable<?> getObjectMaxValue(String elementName);

    /**
     * How many elements at least, if the object is an array.
     *
     * @throws IllegalArgumentException if it is not an array
     */
    int getObjectArrayMinLength(String elementName);

    /**
     * How many at most.
     *
     * @throws IllegalArgumentException if it is not an array
     */
    int getObjectArrayMaxLength(String elementName);
}
