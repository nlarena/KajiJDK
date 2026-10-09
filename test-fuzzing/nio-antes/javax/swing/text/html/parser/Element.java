package javax.swing.text.html.parser;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Hashtable;

/**
 * An element declared in the DTD.
 *
 * <h2>What it knows beyond a tag</h2>
 *
 * <p>{@link javax.swing.text.html.HTML.Tag} says how a tag is shown. This says how it is
 * <em>parsed</em>: what may go inside ({@link #content}), which attributes it accepts
 * ({@link #atts}), and whether the opening or the closing tag may be omitted ({@link #oStart},
 * {@link #oEnd}).
 *
 * <p>That last pair is the whole reason for a DTD-driven parser. In HTML one writes
 * <code>&lt;p&gt;one&lt;p&gt;two</code> without closing any paragraph, and somebody has to know
 * that this is legal and where the closing goes. That somebody reads these fields.
 *
 * <h2>Inclusions and exclusions</h2>
 *
 * <p>Two {@link BitSet}s indexed by {@link #index}. The inclusion adds elements allowed inside
 * this one and everything hanging from it; the exclusion forbids them. They serve for rules the
 * content model cannot express: inside an <code>a</code> there cannot be another <code>a</code>,
 * however deep.
 *
 * <p>That is why the {@code index} matters and why an element cannot be created from outside: the
 * number is assigned by the {@link DTD} that contains it, and two elements with the same index
 * would break both sets.
 */
public final class Element implements DTDConstants, Serializable {

    /** This element's number in its DTD; it is the index into both {@link BitSet}s. */
    public int index;

    /** The name, in lower case. */
    public String name;

    /** Whether the opening tag may be omitted. */
    public boolean oStart;

    /** Whether the closing one may be omitted. */
    public boolean oEnd;

    /** Elements allowed inside, besides those of the model. */
    public BitSet inclusions;

    /** Elements forbidden inside, even if the model allows them. */
    public BitSet exclusions;

    /** {@code EMPTY}, {@code CDATA}, {@code RCDATA}, {@code MODEL} or {@code ANY}. */
    public int type = ANY;

    /** What may go inside. */
    public ContentModel content;

    /** The attributes it accepts, chained. */
    public AttributeList atts;

    static int maxIndex = 0;

    /** A place for whoever uses the DTD to hang their own. */
    public Object data;

    Element() {
    }

    /** Only the DTD creates elements; see the class note. */
    Element(String name, int index) {
        this.name = name;
        this.index = index;
        maxIndex = Math.max(maxIndex, index);
    }

    public String getName() {
        return name;
    }

    public boolean omitStart() {
        return oStart;
    }

    public boolean omitEnd() {
        return oEnd;
    }

    public int getType() {
        return type;
    }

    public ContentModel getContent() {
        return content;
    }

    public AttributeList getAttributes() {
        return atts;
    }

    public int getIndex() {
        return index;
    }

    /** Whether the element carries nothing inside, such as {@code br} or {@code img}. */
    public boolean isEmpty() {
        return type == EMPTY;
    }

    public String toString() {
        return name;
    }

    /** The attribute with that name, or null. */
    public AttributeList getAttribute(String name) {
        for (AttributeList a = atts; a != null; a = a.next) {
            if (a.name.equals(name)) {
                return a;
            }
        }
        return null;
    }

    /**
     * The attribute that has that value among its allowed values.
     *
     * <p>It serves for the HTML where the value is written without the name: in
     * <code>&lt;ul compact&gt;</code>, <code>compact</code> is a value and which attribute it
     * belongs to has to be found out. The comparison ignores case because HTML does too.
     */
    public AttributeList getAttributeByValue(String name) {
        for (AttributeList a = atts; a != null; a = a.next) {
            if ((a.values != null) && (a.values.contains(name))) {
                return a;
            }
        }
        return null;
    }

    static Hashtable<String, Integer> contentTypes = new Hashtable<String, Integer>();

    /**
     * The content type number that corresponds to that name.
     *
     * <p>An unknown name gives zero, not {@code CDATA}. It is different from
     * {@link AttributeList#name2type} on purpose: there an odd type can be treated as text, here a
     * content model that is not understood has no reasonable equivalent.
     */
    public static int name2type(String nm) {
        Integer val = contentTypes.get(nm);
        return (val != null) ? val.intValue() : 0;
    }

    static {
        contentTypes.put("CDATA", Integer.valueOf(CDATA));
        contentTypes.put("RCDATA", Integer.valueOf(RCDATA));
        contentTypes.put("EMPTY", Integer.valueOf(EMPTY));
        contentTypes.put("ANY", Integer.valueOf(ANY));
    }
}
