package javax.swing.text.html.parser;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * An attribute declared in the DTD, and the next one.
 *
 * <h2>It is a list, not an element of a list</h2>
 *
 * <p>Each instance carries a {@code next} field: an element's attribute list is the chain that
 * starts at the first one. There is no separate "list" class. It is the 1997 form and it is
 * kept because the fields are public and changing it would break anybody who walks them.
 *
 * <h2>What it keeps about each attribute</h2>
 *
 * <p>The name, the <em>type</em> ({@code CDATA}, {@code ID}, {@code NUMBER}...), the
 * <em>modifier</em> ({@code REQUIRED}, {@code IMPLIED}, {@code FIXED}...), the default value if
 * it has one, and the list of allowed values if it is an enumeration. The first two are numbers
 * from {@link DTDConstants} and are worth the same even though they mean different things; see
 * that interface's note.
 */
public final class AttributeList implements DTDConstants, Serializable {

    /** The attribute's name. */
    public String name;

    /** The type: {@code CDATA}, {@code ID}, {@code NUMBER} and so on. */
    public int type;

    /** The allowed values, if it is an enumeration; null if it is not. */
    public Vector<?> values;

    /** The modifier: {@code REQUIRED}, {@code IMPLIED}, {@code FIXED}, {@code CURRENT}. */
    public int modifier;

    /** The default value, if it has one. */
    public String value;

    /** The attribute that follows; see the class note. */
    public AttributeList next;

    AttributeList() {
    }

    /** An attribute with that name and nothing else. */
    public AttributeList(String name) {
        this.name = name;
    }

    /** A complete attribute, with the next one hanging from it. */
    public AttributeList(String name, int type, int modifier, String value, Vector<?> values,
            AttributeList next) {
        this.name = name;
        this.type = type;
        this.modifier = modifier;
        this.value = value;
        this.values = values;
        this.next = next;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public int getModifier() {
        return modifier;
    }

    /** The allowed values, or null if the attribute is not an enumeration. */
    public Enumeration<?> getValues() {
        return (values == null) ? null : values.elements();
    }

    public String getValue() {
        return value;
    }

    public AttributeList getNext() {
        return next;
    }

    public String toString() {
        return name;
    }

    /**
     * The type number that corresponds to that name.
     *
     * <p>A name that is not known gives {@code CDATA}: in a DTD, an odd type is treated as loose
     * text and not as an error, which is the only thing that allows reading a DTD newer than the
     * reader.
     */
    public static int name2type(String nm) {
        if ("CDATA".equals(nm)) {
            return CDATA;
        }
        if ("ENTITY".equals(nm)) {
            return ENTITY;
        }
        if ("ENTITIES".equals(nm)) {
            return ENTITIES;
        }
        if ("ID".equals(nm)) {
            return ID;
        }
        if ("IDREF".equals(nm)) {
            return IDREF;
        }
        if ("IDREFS".equals(nm)) {
            return IDREFS;
        }
        if ("NAME".equals(nm)) {
            return NAME;
        }
        if ("NAMES".equals(nm)) {
            return NAMES;
        }
        if ("NMTOKEN".equals(nm)) {
            return NMTOKEN;
        }
        if ("NMTOKENS".equals(nm)) {
            return NMTOKENS;
        }
        if ("NOTATION".equals(nm)) {
            return NOTATION;
        }
        if ("NUMBER".equals(nm)) {
            return NUMBER;
        }
        if ("NUMBERS".equals(nm)) {
            return NUMBERS;
        }
        if ("NUTOKEN".equals(nm)) {
            return NUTOKEN;
        }
        if ("NUTOKENS".equals(nm)) {
            return NUTOKENS;
        }
        return CDATA;
    }

    /**
     * The name of that type number.
     *
     * <p>Only the fifteen attribute types have a name; the other numbers give null, even those
     * that are worth something in another of {@link DTDConstants}' families.
     */
    public static String type2name(int type) {
        // A table and not a `switch`: the fifteen types are 1..15 with no gaps, so the index is
                // enough. Besides, the `switch` does not compile, because the constants come from a
                // `.class` and our generator does not fold them into a `case` yet (finding #503).
        if (type >= 1 && type <= TYPES.length) {
            return TYPES[type - 1];
        }
        return null;
    }

    /** The fifteen attribute types, in the order of their numbers. */
    private static final String[] TYPES = {
        "CDATA", "ENTITY", "ENTITIES", "ID", "IDREF",
        "IDREFS", "NAME", "NAMES", "NMTOKEN", "NMTOKENS",
        "NOTATION", "NUMBER", "NUMBERS", "NUTOKEN", "NUTOKENS"
    };
}
