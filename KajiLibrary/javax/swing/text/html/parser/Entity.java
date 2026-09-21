package javax.swing.text.html.parser;

/**
 * A DTD entity: a name that is replaced by a text.
 *
 * <h2>The two kinds of entity</h2>
 *
 * <p>A <em>general</em> entity is what the page's author writes: <code>&amp;amp;</code> is
 * replaced by <code>&amp;</code>. A <em>parameter</em> entity exists only inside the DTD and
 * serves to avoid repeating lists of elements.
 *
 * <p>Both share the {@code type} field, and the difference goes in two separate bits
 * ({@link DTDConstants#GENERAL} and {@link DTDConstants#PARAMETER}). That is why {@link #getType}
 * masks: the raw number carries the type and the kind mixed together, and whoever asks for the
 * type does not want the kind's bits.
 */
public final class Entity implements DTDConstants {

    /** The name, without the {@code &} or the {@code ;}. */
    public String name;

    /** The type, with the kind bits still inside; see {@link #getType}. */
    public int type;

    /** The text it is replaced by. */
    public char[] data;

    /** An entity with that name, type and content. */
    public Entity(String name, int type, char[] data) {
        this.name = name;
        this.type = type;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    /** The type, already without the bits that say whether it is general or a parameter one. */
    public int getType() {
        return type & 0xFFFF;
    }

    public boolean isParameter() {
        return (type & PARAMETER) != 0;
    }

    public boolean isGeneral() {
        return (type & GENERAL) != 0;
    }

    public char[] getData() {
        return data;
    }

    /** The content as a string. */
    public String getString() {
        return new String(data, 0, data.length);
    }

    /**
     * The type number that corresponds to that name.
     *
     * <p>An unknown name gives {@code CDATA}, for the same reason as in
     * {@link AttributeList#name2type}.
     */
    public static int name2type(String nm) {
        if ("PUBLIC".equals(nm)) {
            return PUBLIC;
        }
        if ("CDATA".equals(nm)) {
            return CDATA;
        }
        if ("SDATA".equals(nm)) {
            return SDATA;
        }
        if ("PI".equals(nm)) {
            return PI;
        }
        if ("STARTTAG".equals(nm)) {
            return STARTTAG;
        }
        if ("ENDTAG".equals(nm)) {
            return ENDTAG;
        }
        if ("MS".equals(nm)) {
            return MS;
        }
        if ("MD".equals(nm)) {
            return MD;
        }
        if ("SYSTEM".equals(nm)) {
            return SYSTEM;
        }
        return CDATA;
    }
}
