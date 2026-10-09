package javax.imageio.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.imageio.ImageTypeSpecifier;

/**
 * KajiLibrary's javax.imageio.metadata.IIOMetadataFormatImpl -- half the work of writing a
 * metadata schema.
 *
 * <p>It implements the twenty-five {@code getXxx} of {@link IIOMetadataFormat} over an internal
 * table, and exposes <b>protected</b> methods to fill it in. A subclass declares its schema in the
 * constructor, with calls to {@link #addElement} and {@link #addAttribute}, and inherits all the
 * queries.
 *
 * <p>Only {@link IIOMetadataFormat#canNodeAppear} stays abstract, which is the only thing that
 * depends on the concrete image type and that no table can answer.
 *
 * <h2>The building methods are protected on purpose</h2>
 *
 * <p>A schema is built once, in the constructor, and does not change afterwards. If
 * {@code addElement} were public, anyone could modify the schema of a format already in use and
 * leave trees that were valid yesterday invalid today.
 *
 * <h2>The descriptions come from a resource bundle</h2>
 *
 * <p>{@link #setResourceBaseName} says which bundle to take the texts of
 * {@code getElementDescription} and {@code getAttributeDescription} from, looked up by the
 * element's name or by {@code element/attribute}. Without a bundle, or if the key is missing,
 * those methods return null -- which is right: there is no description, rather than a made-up
 * one.
 *
 * <h2>Adding a child is two calls</h2>
 *
 * <p>{@link #addElement} <b>declares</b> the element; {@link #addChildElement} <b>hangs</b> it from
 * another. The two versions of {@code addElement} that take a parent do both at once, and are the
 * ones used almost always. The split exists to be able to declare an element that is a child of
 * several.
 */
public abstract class IIOMetadataFormatImpl implements IIOMetadataFormat {

    /** The name of the format common to all readers and writers. */
    public static final String standardMetadataFormatName = "javax_imageio_1.0";

    /** The standard format, built only once. */
    private static IIOMetadataFormat standardFormat = null;

    /** What the root is called. */
    private final String rootName;

    /** Where to take the texts from, or null. */
    private String resourceBaseName = getClass().getName() + "Resources";

    /** The elements, by name. */
    private final Map<String, Element> elements = new HashMap<String, Element>();

    /**
     * A schema with that root and that child policy.
     *
     * @throws IllegalArgumentException if the name is null or the policy is not one of the six, or
     *     if it is {@link #CHILD_POLICY_REPEAT} -- that is what the other constructor is for
     */
    public IIOMetadataFormatImpl(String rootName, int childPolicy) {
        if (rootName == null) {
            throw new IllegalArgumentException("rootName == null!");
        }
        if (childPolicy < CHILD_POLICY_EMPTY || childPolicy > CHILD_POLICY_MAX
            || childPolicy == CHILD_POLICY_REPEAT) {
            throw new IllegalArgumentException("Invalid value for childPolicy!");
        }
        this.rootName = rootName;
        Element root = new Element(rootName);
        root.childPolicy = childPolicy;
        this.elements.put(rootName, root);
    }

    /**
     * Same, with a repeatable root.
     *
     * @throws IllegalArgumentException if the name is null or the limits do not add up
     */
    public IIOMetadataFormatImpl(String rootName, int minChildren, int maxChildren) {
        if (rootName == null) {
            throw new IllegalArgumentException("rootName == null!");
        }
        if (minChildren < 0) {
            throw new IllegalArgumentException("minChildren < 0!");
        }
        if (minChildren > maxChildren) {
            throw new IllegalArgumentException("minChildren > maxChildren!");
        }
        this.rootName = rootName;
        Element root = new Element(rootName);
        root.childPolicy = CHILD_POLICY_REPEAT;
        root.minChildren = minChildren;
        root.maxChildren = maxChildren;
        this.elements.put(rootName, root);
    }

    /** Where to take the texts from; null turns them off. See the class note. */
    protected void setResourceBaseName(String resourceBaseName) {
        this.resourceBaseName = resourceBaseName;
    }

    /** Where they come from. */
    protected String getResourceBaseName() {
        return this.resourceBaseName;
    }

    /**
     * Declares an element and hangs it from its parent.
     *
     * @throws IllegalArgumentException if the parent does not exist, or the policy does not work
     */
    protected void addElement(String elementName, String parentName, int childPolicy) {
        if (childPolicy < CHILD_POLICY_EMPTY || childPolicy > CHILD_POLICY_MAX
            || childPolicy == CHILD_POLICY_REPEAT) {
            throw new IllegalArgumentException("Invalid value for childPolicy!");
        }
        Element element = getOrCreate(elementName);
        element.childPolicy = childPolicy;
        addChildElement(elementName, parentName);
    }

    /**
     * Same, repeatable.
     *
     * @throws IllegalArgumentException if the parent does not exist or the limits do not add up
     */
    protected void addElement(String elementName, String parentName, int minChildren,
                              int maxChildren) {
        if (minChildren < 0) {
            throw new IllegalArgumentException("minChildren < 0!");
        }
        if (minChildren > maxChildren) {
            throw new IllegalArgumentException("minChildren > maxChildren!");
        }
        Element element = getOrCreate(elementName);
        element.childPolicy = CHILD_POLICY_REPEAT;
        element.minChildren = minChildren;
        element.maxChildren = maxChildren;
        addChildElement(elementName, parentName);
    }

    /**
     * Hangs an already declared element from another. See the class note.
     *
     * @throws IllegalArgumentException if the parent does not exist
     */
    protected void addChildElement(String elementName, String parentName) {
        Element parent = element(parentName);
        getOrCreate(elementName);
        if (!parent.childList.contains(elementName)) {
            parent.childList.add(elementName);
        }
    }

    /**
     * Removes an element from the schema.
     *
     * <p>It also removes it from the child lists of all the others: leaving it dangling would
     * produce a schema that names an element that no longer exists.
     */
    protected void removeElement(String elementName) {
        if (this.elements.remove(elementName) != null) {
            java.util.Iterator<Element> it = this.elements.values().iterator();
            while (it.hasNext()) {
                it.next().childList.remove(elementName);
            }
        }
    }

    /**
     * Declares a free-valued or enumerated attribute.
     *
     * @param dataType one of the {@code DATATYPE_} constants
     * @param required whether it has to be present
     * @param defaultValue what it is worth if not set, or null
     * @throws IllegalArgumentException if the element does not exist or the type does not work
     */
    protected void addAttribute(String elementName, String attrName, int dataType,
                                boolean required, String defaultValue) {
        Element element = element(elementName);
        checkDataType(dataType);
        Attribute attr = new Attribute(attrName);
        attr.valueType = VALUE_ARBITRARY;
        attr.dataType = dataType;
        attr.required = required;
        attr.defaultValue = defaultValue;
        element.attributes.put(attrName, attr);
        element.attrList.add(attrName);
    }

    /**
     * Same, with a closed list of values.
     *
     * @throws IllegalArgumentException if the list is null or empty
     */
    protected void addAttribute(String elementName, String attrName, int dataType,
                                boolean required, String defaultValue,
                                List<String> enumeratedValues) {
        Element element = element(elementName);
        checkDataType(dataType);
        if (enumeratedValues == null) {
            throw new IllegalArgumentException("enumeratedValues == null!");
        }
        if (enumeratedValues.size() == 0) {
            throw new IllegalArgumentException("enumeratedValues is empty!");
        }
        Attribute attr = new Attribute(attrName);
        attr.valueType = VALUE_ENUMERATION;
        attr.dataType = dataType;
        attr.required = required;
        attr.defaultValue = defaultValue;
        attr.enumeratedValues = new ArrayList<String>(enumeratedValues);
        element.attributes.put(attrName, attr);
        element.attrList.add(attrName);
    }

    /**
     * Same, with a range.
     *
     * @param minInclusive whether the minimum is included
     */
    protected void addAttribute(String elementName, String attrName, int dataType,
                                boolean required, String defaultValue, String minValue,
                                String maxValue, boolean minInclusive, boolean maxInclusive) {
        Element element = element(elementName);
        checkDataType(dataType);
        Attribute attr = new Attribute(attrName);
        int valueType = VALUE_RANGE;
        if (minInclusive) {
            valueType = valueType | VALUE_RANGE_MIN_INCLUSIVE_MASK;
        }
        if (maxInclusive) {
            valueType = valueType | VALUE_RANGE_MAX_INCLUSIVE_MASK;
        }
        attr.valueType = valueType;
        attr.dataType = dataType;
        attr.required = required;
        attr.defaultValue = defaultValue;
        attr.minValue = minValue;
        attr.maxValue = maxValue;
        element.attributes.put(attrName, attr);
        element.attrList.add(attrName);
    }

    /**
     * Same, with a list of space-separated values.
     *
     * @param listMinLength how many at least
     * @throws IllegalArgumentException if the limits do not add up
     */
    protected void addAttribute(String elementName, String attrName, int dataType,
                                boolean required, int listMinLength, int listMaxLength) {
        Element element = element(elementName);
        checkDataType(dataType);
        if (listMinLength < 0 || listMinLength > listMaxLength) {
            throw new IllegalArgumentException("Invalid list bounds!");
        }
        Attribute attr = new Attribute(attrName);
        attr.valueType = VALUE_LIST;
        attr.dataType = dataType;
        attr.required = required;
        attr.listMinLength = listMinLength;
        attr.listMaxLength = listMaxLength;
        element.attributes.put(attrName, attr);
        element.attrList.add(attrName);
    }

    /** A shortcut for a {@code true} or {@code false} attribute. */
    protected void addBooleanAttribute(String elementName, String attrName,
                                       boolean hasDefaultValue, boolean defaultValue) {
        List<String> values = new ArrayList<String>();
        values.add("TRUE");
        values.add("FALSE");
        String defaultVal = null;
        if (hasDefaultValue) {
            if (defaultValue) {
                defaultVal = "TRUE";
            } else {
                defaultVal = "FALSE";
            }
        }
        addAttribute(elementName, attrName, DATATYPE_BOOLEAN, true, defaultVal, values);
    }

    /**
     * Removes an attribute.
     *
     * @throws IllegalArgumentException if the element does not exist
     */
    protected void removeAttribute(String elementName, String attrName) {
        Element element = element(elementName);
        element.attributes.remove(attrName);
        element.attrList.remove(attrName);
    }

    /**
     * Declares that the element carries a user object of that class.
     *
     * @param required whether it has to be present
     * @param defaultValue what object goes if none is set, or null
     */
    protected <T> void addObjectValue(String elementName, Class<T> classType, boolean required,
                                      T defaultValue) {
        Element element = element(elementName);
        ObjectValue value = new ObjectValue();
        value.valueType = VALUE_ARBITRARY;
        value.classType = classType;
        value.defaultValue = defaultValue;
        element.objectValue = value;
    }

    /**
     * Same, with a closed list of objects.
     *
     * @throws IllegalArgumentException if the list is null or empty
     */
    protected <T> void addObjectValue(String elementName, Class<T> classType, boolean required,
                                      T defaultValue, List<? extends T> enumeratedValues) {
        Element element = element(elementName);
        if (enumeratedValues == null) {
            throw new IllegalArgumentException("enumeratedValues == null!");
        }
        if (enumeratedValues.size() == 0) {
            throw new IllegalArgumentException("enumeratedValues is empty!");
        }
        ObjectValue value = new ObjectValue();
        value.valueType = VALUE_ENUMERATION;
        value.classType = classType;
        value.defaultValue = defaultValue;
        value.enumeratedValues = new ArrayList<Object>(enumeratedValues);
        element.objectValue = value;
    }

    /** Same, with a range. */
    protected <T extends Comparable<? super T>> void addObjectValue(String elementName,
                                                                    Class<T> classType,
                                                                    T defaultValue,
                                                                    Comparable<? super T> minValue,
                                                                    Comparable<? super T> maxValue,
                                                                    boolean minInclusive,
                                                                    boolean maxInclusive) {
        Element element = element(elementName);
        ObjectValue value = new ObjectValue();
        int valueType = VALUE_RANGE;
        if (minInclusive) {
            valueType = valueType | VALUE_RANGE_MIN_INCLUSIVE_MASK;
        }
        if (maxInclusive) {
            valueType = valueType | VALUE_RANGE_MAX_INCLUSIVE_MASK;
        }
        value.valueType = valueType;
        value.classType = classType;
        value.defaultValue = defaultValue;
        value.minValue = minValue;
        value.maxValue = maxValue;
        element.objectValue = value;
    }

    /**
     * Same, with an array of that class.
     *
     * @throws IllegalArgumentException if the limits do not add up
     */
    protected void addObjectValue(String elementName, Class<?> classType, int arrayMinLength,
                                  int arrayMaxLength) {
        Element element = element(elementName);
        if (arrayMinLength < 0 || arrayMinLength > arrayMaxLength) {
            throw new IllegalArgumentException("Invalid array bounds!");
        }
        ObjectValue value = new ObjectValue();
        value.valueType = VALUE_LIST;
        value.classType = classType;
        value.arrayMinLength = arrayMinLength;
        value.arrayMaxLength = arrayMaxLength;
        element.objectValue = value;
    }

    /** Removes that element's user object. */
    protected void removeObjectValue(String elementName) {
        element(elementName).objectValue = null;
    }

    /** What the root is called. */
    public String getRootName() {
        return this.rootName;
    }

    /** The only thing the table cannot answer; see the class note. */
    public abstract boolean canNodeAppear(String elementName, ImageTypeSpecifier imageType);

    /**
     * How many children at least.
     *
     * @throws IllegalArgumentException if the policy is not {@link #CHILD_POLICY_REPEAT}
     */
    public int getElementMinChildren(String elementName) {
        Element element = element(elementName);
        if (element.childPolicy != CHILD_POLICY_REPEAT) {
            throw new IllegalArgumentException("Child policy not CHILD_POLICY_REPEAT!");
        }
        return element.minChildren;
    }

    /**
     * How many at most.
     *
     * @throws IllegalArgumentException if the policy is not {@link #CHILD_POLICY_REPEAT}
     */
    public int getElementMaxChildren(String elementName) {
        Element element = element(elementName);
        if (element.childPolicy != CHILD_POLICY_REPEAT) {
            throw new IllegalArgumentException("Child policy not CHILD_POLICY_REPEAT!");
        }
        return element.maxChildren;
    }

    /** What that element is, or null if there is no text. See the class note. */
    public String getElementDescription(String elementName, Locale locale) {
        element(elementName);
        return resource(elementName, locale);
    }

    /**
     * Which of the six policies.
     *
     * @throws IllegalArgumentException if the element does not exist
     */
    public int getChildPolicy(String elementName) {
        return element(elementName).childPolicy;
    }

    /**
     * Which children it can have; null if it can have none.
     *
     * @throws IllegalArgumentException if the element does not exist
     */
    public String[] getChildNames(String elementName) {
        Element element = element(elementName);
        if (element.childPolicy == CHILD_POLICY_EMPTY) {
            return null;
        }
        return element.childList.toArray(new String[element.childList.size()]);
    }

    /**
     * Which attributes it can have.
     *
     * @throws IllegalArgumentException if the element does not exist
     */
    public String[] getAttributeNames(String elementName) {
        Element element = element(elementName);
        return element.attrList.toArray(new String[element.attrList.size()]);
    }

    /** What shape the value has. */
    public int getAttributeValueType(String elementName, String attrName) {
        return attribute(elementName, attrName).valueType;
    }

    /** What type it is. */
    public int getAttributeDataType(String elementName, String attrName) {
        return attribute(elementName, attrName).dataType;
    }

    /** Whether it has to be present. */
    public boolean isAttributeRequired(String elementName, String attrName) {
        return attribute(elementName, attrName).required;
    }

    /** What it is worth if not set, or null. */
    public String getAttributeDefaultValue(String elementName, String attrName) {
        return attribute(elementName, attrName).defaultValue;
    }

    /**
     * The allowed values.
     *
     * @throws IllegalArgumentException if it is not of enumeration type
     */
    public String[] getAttributeEnumerations(String elementName, String attrName) {
        Attribute attr = attribute(elementName, attrName);
        if (attr.valueType != VALUE_ENUMERATION) {
            throw new IllegalArgumentException("Attribute not an enumeration!");
        }
        return attr.enumeratedValues.toArray(new String[attr.enumeratedValues.size()]);
    }

    /**
     * The minimum of the range.
     *
     * @throws IllegalArgumentException if it is not of range type
     */
    public String getAttributeMinValue(String elementName, String attrName) {
        Attribute attr = attribute(elementName, attrName);
        if ((attr.valueType & VALUE_RANGE) != VALUE_RANGE) {
            throw new IllegalArgumentException("Attribute not a range!");
        }
        return attr.minValue;
    }

    /**
     * The maximum.
     *
     * @throws IllegalArgumentException if it is not of range type
     */
    public String getAttributeMaxValue(String elementName, String attrName) {
        Attribute attr = attribute(elementName, attrName);
        if ((attr.valueType & VALUE_RANGE) != VALUE_RANGE) {
            throw new IllegalArgumentException("Attribute not a range!");
        }
        return attr.maxValue;
    }

    /**
     * How many values at least.
     *
     * @throws IllegalArgumentException if it is not of list type
     */
    public int getAttributeListMinLength(String elementName, String attrName) {
        Attribute attr = attribute(elementName, attrName);
        if (attr.valueType != VALUE_LIST) {
            throw new IllegalArgumentException("Attribute not a list!");
        }
        return attr.listMinLength;
    }

    /**
     * How many at most.
     *
     * @throws IllegalArgumentException if it is not of list type
     */
    public int getAttributeListMaxLength(String elementName, String attrName) {
        Attribute attr = attribute(elementName, attrName);
        if (attr.valueType != VALUE_LIST) {
            throw new IllegalArgumentException("Attribute not a list!");
        }
        return attr.listMaxLength;
    }

    /** What that attribute is, or null. */
    public String getAttributeDescription(String elementName, String attrName, Locale locale) {
        attribute(elementName, attrName);
        return resource(elementName + "/" + attrName, locale);
    }

    /** What shape the user object has; {@link #VALUE_NONE} if there is none. */
    public int getObjectValueType(String elementName) {
        Element element = element(elementName);
        if (element.objectValue == null) {
            return VALUE_NONE;
        }
        return element.objectValue.valueType;
    }

    /**
     * What class it is.
     *
     * @throws IllegalArgumentException if the element carries no object
     */
    public Class<?> getObjectClass(String elementName) {
        return objectValue(elementName).classType;
    }

    /** What object goes if none is set, or null. */
    public Object getObjectDefaultValue(String elementName) {
        return objectValue(elementName).defaultValue;
    }

    /**
     * The allowed objects.
     *
     * @throws IllegalArgumentException if it is not of enumeration type
     */
    public Object[] getObjectEnumerations(String elementName) {
        ObjectValue value = objectValue(elementName);
        if (value.valueType != VALUE_ENUMERATION) {
            throw new IllegalArgumentException("Not an enumeration!");
        }
        return value.enumeratedValues.toArray(new Object[value.enumeratedValues.size()]);
    }

    /**
     * The minimum.
     *
     * @throws IllegalArgumentException if it is not of range type
     */
    public Comparable<?> getObjectMinValue(String elementName) {
        ObjectValue value = objectValue(elementName);
        if ((value.valueType & VALUE_RANGE) != VALUE_RANGE) {
            throw new IllegalArgumentException("Not a range!");
        }
        return value.minValue;
    }

    /**
     * The maximum.
     *
     * @throws IllegalArgumentException if it is not of range type
     */
    public Comparable<?> getObjectMaxValue(String elementName) {
        ObjectValue value = objectValue(elementName);
        if ((value.valueType & VALUE_RANGE) != VALUE_RANGE) {
            throw new IllegalArgumentException("Not a range!");
        }
        return value.maxValue;
    }

    /**
     * How many elements at least.
     *
     * @throws IllegalArgumentException if it is not an array
     */
    public int getObjectArrayMinLength(String elementName) {
        ObjectValue value = objectValue(elementName);
        if (value.valueType != VALUE_LIST) {
            throw new IllegalArgumentException("Not a list!");
        }
        return value.arrayMinLength;
    }

    /**
     * How many at most.
     *
     * @throws IllegalArgumentException if it is not an array
     */
    public int getObjectArrayMaxLength(String elementName) {
        ObjectValue value = objectValue(elementName);
        if (value.valueType != VALUE_LIST) {
            throw new IllegalArgumentException("Not a list!");
        }
        return value.arrayMaxLength;
    }

    /** The schema of {@code javax_imageio_1.0}; always the same instance. */
    public static IIOMetadataFormat getStandardFormatInstance() {
        synchronized (IIOMetadataFormatImpl.class) {
            if (standardFormat == null) {
                standardFormat = new StandardMetadataFormat();
            }
            return standardFormat;
        }
    }

    /** The element, or fails if it is not there. */
    private Element element(String elementName) {
        if (elementName == null) {
            throw new IllegalArgumentException("element name == null!");
        }
        Element element = this.elements.get(elementName);
        if (element == null) {
            throw new IllegalArgumentException("No such element: " + elementName);
        }
        return element;
    }

    /** The element, creating it if needed. */
    private Element getOrCreate(String elementName) {
        if (elementName == null) {
            throw new IllegalArgumentException("element name == null!");
        }
        Element element = this.elements.get(elementName);
        if (element == null) {
            element = new Element(elementName);
            this.elements.put(elementName, element);
        }
        return element;
    }

    /** The attribute, or fails. */
    private Attribute attribute(String elementName, String attrName) {
        Element element = element(elementName);
        if (attrName == null) {
            throw new IllegalArgumentException("attribute name == null!");
        }
        Attribute attr = element.attributes.get(attrName);
        if (attr == null) {
            throw new IllegalArgumentException("No such attribute: " + attrName);
        }
        return attr;
    }

    /** The user object, or fails. */
    private ObjectValue objectValue(String elementName) {
        Element element = element(elementName);
        if (element.objectValue == null) {
            throw new IllegalArgumentException("No object within element " + elementName);
        }
        return element.objectValue;
    }

    /** The text for that key, or null. */
    private String resource(String key, Locale locale) {
        if (this.resourceBaseName == null) {
            return null;
        }
        Locale where = locale;
        if (where == null) {
            where = Locale.getDefault();
        }
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(this.resourceBaseName, where,
                                                             getClass().getClassLoader());
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            // There is no description. Returning null is right: the contract allows it, and making
            // up a text would be worse than having none.
            return null;
        }
    }

    /** That the data type is one of the five. */
    private static void checkDataType(int dataType) {
        if (dataType < DATATYPE_STRING || dataType > DATATYPE_DOUBLE) {
            throw new IllegalArgumentException("Invalid value for dataType!");
        }
    }

    /** An element of the schema. */
    private static final class Element {

        /** What it is called. */
        final String name;

        /** Which of the six policies. */
        int childPolicy = CHILD_POLICY_EMPTY;

        /** Only with {@link #CHILD_POLICY_REPEAT}. */
        int minChildren = 0;

        /** Same. */
        int maxChildren = 0;

        /** Which children, in declaration order. */
        final List<String> childList = new ArrayList<String>();

        /** The attributes, by name. */
        final Map<String, Attribute> attributes = new HashMap<String, Attribute>();

        /** Their names, in declaration order: the map does not keep it. */
        final List<String> attrList = new ArrayList<String>();

        /** The user object, or null. */
        ObjectValue objectValue = null;

        Element(String name) {
            this.name = name;
        }
    }

    /** An attribute of the schema. */
    private static final class Attribute {

        final String name;

        int valueType = VALUE_ARBITRARY;

        int dataType = DATATYPE_STRING;

        boolean required = false;

        String defaultValue = null;

        List<String> enumeratedValues = null;

        String minValue = null;

        String maxValue = null;

        int listMinLength = 0;

        int listMaxLength = Integer.MAX_VALUE;

        Attribute(String name) {
            this.name = name;
        }
    }

    /** An element's declared user object. */
    private static final class ObjectValue {

        int valueType = VALUE_NONE;

        Class<?> classType = null;

        Object defaultValue = null;

        List<Object> enumeratedValues = null;

        Comparable<?> minValue = null;

        Comparable<?> maxValue = null;

        int arrayMinLength = 0;

        int arrayMaxLength = Integer.MAX_VALUE;
    }
}
