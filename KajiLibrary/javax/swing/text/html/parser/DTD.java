package javax.swing.text.html.parser;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * The description of a markup language: which elements there are and how they nest.
 *
 * <h2>What having a DTD is for</h2>
 *
 * <p>The HTML that is really written is full of unclosed tags and of nestings that were not
 * declared. A parser that only looked at the written tags could not build a tree. This one can,
 * because it has the DTD: it knows that a <code>&lt;p&gt;</code> closes the previous one, that a
 * <code>&lt;li&gt;</code> goes inside a list even if none was opened, and that inside an
 * <code>&lt;a&gt;</code> there cannot be another.
 *
 * <h2>The eleven elements that are fields</h2>
 *
 * <p>{@link #html}, {@link #head}, {@link #body} and the others are final fields because the
 * parser needs them at every step and looking them up by name on every decision would be
 * expensive. They are no more important than the others: they are the ones consulted often.
 *
 * <p>The first, {@link #pcdata}, is not a tag: it is loose text. That it is an element like any
 * other is what allows writing in a content model that a paragraph carries text, with no special
 * case.
 *
 * <h2>How it is filled</h2>
 *
 * <p>With the {@code define...} and {@code def...} methods, or by reading the binary format with
 * {@link #read}. The protected {@code def...} ones are what {@link #read} uses; the public
 * {@code define...} ones are for building it by hand. The difference is that the first take
 * names and the second objects that are already built.
 */
public class DTD implements DTDConstants {

    /** The DTD's name, for instance {@code html32}. */
    public String name;

    /** The elements, indexed by their {@link Element#index}. */
    public Vector<Element> elements = new Vector<Element>();

    /** The elements by name. */
    public Hashtable<String, Element> elementHash = new Hashtable<String, Element>();

    /**
     * The entities, by name and by character number.
     *
     * <p>The keys are of two kinds on purpose: a string for <code>&amp;amp;</code> and an
     * {@link Integer} for <code>&amp;#38;</code>. Two separate tables would be tidier and would
     * force asking twice on every lookup.
     */
    public Hashtable<Object, Entity> entityHash = new Hashtable<Object, Entity>();

    /** Loose text; see the class note. */
    public final Element pcdata = getElement("#pcdata");

    public final Element html = getElement("html");
    public final Element meta = getElement("meta");
    public final Element base = getElement("base");
    public final Element isindex = getElement("isindex");
    public final Element head = getElement("head");
    public final Element body = getElement("body");
    public final Element applet = getElement("applet");
    public final Element param = getElement("param");
    public final Element p = getElement("p");
    public final Element title = getElement("title");

    /**
     * Four more that the parser consults often, undocumented in the JDK.
     *
     * <p>They are here for the same reason as the public ones. But besides, <em>when</em> they are
     * created matters: {@link #getElement(String)} gives each new element the next number, so
     * creating them in the constructor reserves indices 11 to 14 for them. A {@link BitSet} of
     * exclusions stored with one numbering and read with another would point at the wrong elements.
     */
    final Element style = getElement("style");

    final Element link = getElement("link");

    final Element script = getElement("script");

    final Element unknown = getElement("unknown");

    Element html32title = getElement("title");

    /** The version of the binary format {@link #read} understands. */
    public static final int FILE_VERSION = 1;

    private static final Hashtable<String, DTD> dtdHash = new Hashtable<String, DTD>();

    /** An empty DTD with that name. */
    protected DTD(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** The entity with that name, or null. */
    public Entity getEntity(String name) {
        return entityHash.get(name);
    }

    /** That character's entity, or null; see {@link #entityHash}'s note. */
    public Entity getEntity(int ch) {
        return entityHash.get(Integer.valueOf(ch));
    }

    /**
     * The element with that name, creating it if it does not exist.
     *
     * <p>Creating instead of returning null is on purpose and it is what makes it possible to
     * declare a content model that names an element before declaring it. When its declaration
     * arrives, {@link #defineElement} will complete it over the same object, and the models that
     * already pointed at it will be right.
     */
    public Element getElement(String name) {
        Element e = elementHash.get(name);
        if (e == null) {
            e = new Element(name, elements.size());
            elements.addElement(e);
            elementHash.put(name, e);
        }
        return e;
    }

    /** The element with that number. */
    public Element getElement(int index) {
        return elements.elementAt(index);
    }

    /**
     * Declares an entity.
     *
     * <p>If it was already declared it is not touched: in a DTD, the first declaration wins, and it
     * is what allows a document to redefine an entity before including the general DTD.
     */
    public Entity defineEntity(String name, int type, char[] data) {
        Entity ent = entityHash.get(name);
        if (ent == null) {
            ent = new Entity(name, type, data);
            entityHash.put(name, ent);
            // A general entity of a single character is also indexed by that character, so that
                        // `&#38;` finds the same as `&amp;`. A `switch` does not compile: the
                        // constants come from a `.class` and are not folded into a `case` yet
                        // (finding #503).
            int cls = type & 0xFFFF;
            if (((type & GENERAL) != 0) && (data.length == 1)
                    && (cls == CDATA || cls == SDATA)) {
                entityHash.put(Integer.valueOf(data[0]), ent);
            }
        }
        return ent;
    }

    /** Declares an element, or completes the one already created by name. */
    public Element defineElement(String name, int type, boolean omitStart, boolean omitEnd,
            ContentModel content, BitSet exclusions, BitSet inclusions, AttributeList atts) {
        Element e = getElement(name);
        e.type = type;
        e.oStart = omitStart;
        e.oEnd = omitEnd;
        e.content = content;
        e.exclusions = exclusions;
        e.inclusions = inclusions;
        e.atts = atts;
        return e;
    }

    /**
     * Adds attributes to an element.
     *
     * <p>Those already there win: a second declaration of the same attribute does not overwrite
     * it.
     */
    public void defineAttributes(String name, AttributeList atts) {
        Element e = getElement(name);
        e.atts = atts;
    }

    /** Declares an entity of a single character. */
    public Entity defEntity(String name, int type, int ch) {
        char[] data = {(char) ch};
        return defineEntity(name, type, data);
    }

    /** Declares an entity whose content is that text. */
    protected Entity defEntity(String name, int type, String str) {
        int len = str.length();
        char[] data = new char[len];
        str.getChars(0, len, data, 0);
        return defineEntity(name, type, data);
    }

    /** Declares an element naming its exclusions and inclusions. */
    protected Element defElement(String name, int type, boolean omitStart, boolean omitEnd,
            ContentModel content, String[] exclusions, String[] inclusions, AttributeList atts) {
        BitSet excl = null;
        if (exclusions != null && exclusions.length > 0) {
            excl = new BitSet();
            for (int i = 0; i < exclusions.length; i++) {
                String str = exclusions[i];
                if (str.length() > 0) {
                    excl.set(getElement(str).getIndex());
                }
            }
        }
        BitSet incl = null;
        if (inclusions != null && inclusions.length > 0) {
            incl = new BitSet();
            for (int i = 0; i < inclusions.length; i++) {
                String str = inclusions[i];
                if (str.length() > 0) {
                    incl.set(getElement(str).getIndex());
                }
            }
        }
        return defineElement(name, type, omitStart, omitEnd, content, excl, incl, atts);
    }

    /** Builds an attribute and chains it in front of the one passed in. */
    protected AttributeList defAttributeList(String name, int type, int modifier, String value,
            String values, AttributeList atts) {
        Vector<String> vals = null;
        if (values != null) {
            vals = new Vector<String>();
            for (java.util.StringTokenizer s = new java.util.StringTokenizer(values, "|");
                    s.hasMoreTokens();) {
                String str = s.nextToken();
                if (str.length() > 0) {
                    vals.addElement(str);
                }
            }
        }
        return new AttributeList(name, type, modifier, value, vals, atts);
    }

    /** Builds a content model. */
    protected ContentModel defContentModel(int type, Object obj, ContentModel next) {
        return new ContentModel(type, obj, next);
    }

    public String toString() {
        return name;
    }

    /** Stores a DTD under that name so that {@link #getDTD} finds it. */
    public static void putDTDHash(String name, DTD dtd) {
        dtdHash.put(name, dtd);
    }

    /**
     * The DTD with that name, creating it empty if it was not there.
     *
     * <p>The name is lowercased before looking up. Returning an empty DTD instead of failing is
     * what the JDK does, and it is what allows building it afterwards over the returned object.
     */
    public static DTD getDTD(String name) throws IOException {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        DTD dtd = dtdHash.get(lower);
        if (dtd == null) {
            dtd = new DTD(lower);
        }
        return dtd;
    }

    /**
     * Reads a DTD from the binary format.
     *
     * <p>The format stores every name once in a table at the start and afterwards names them by
     * their number. It is what makes an HTML DTD fit in twenty kilobytes: the same hundred names
     * appear thousands of times.
     *
     * @throws IOException if the version is not {@link #FILE_VERSION} or the file is truncated.
     */
    public void read(DataInputStream in) throws IOException {
        if (in.readInt() != FILE_VERSION) {
            throw new IOException("version mismatch");
        }

        // The name table.
        short numNames = in.readShort();
        String[] names = new String[numNames];
        for (int i = 0; i < numNames; i++) {
            names[i] = in.readUTF();
        }

        // The entities.
        short numEntities = in.readShort();
        for (int i = 0; i < numEntities; i++) {
            short nameId = in.readShort();
            int type = in.readByte();
            String name = in.readUTF();
            defEntity(names[nameId], type | GENERAL, name);
        }

        // The elements.
        short numElements = in.readShort();
        for (int i = 0; i < numElements; i++) {
            short nameId = in.readShort();
            byte type = in.readByte();
            byte flags = in.readByte();
            ContentModel m = readContentModel(in, names);
            String[] exclusions = readNameArray(in, names);
            String[] inclusions = readNameArray(in, names);
            AttributeList atts = readAttributeList(in, names);
            defElement(names[nameId], type, ((flags & 0x01) != 0), ((flags & 0x02) != 0), m,
                    exclusions, inclusions, atts);
        }
    }

    /**
     * A content model, in the order it was written.
     *
     * <p>The starting byte says what it is about: zero is the end, one an operator with another
     * model inside, two a leaf that names an element.
     */
    private ContentModel readContentModel(DataInputStream in, String[] names) throws IOException {
        byte nodeType = in.readByte();
        switch (nodeType) {
            case 0:
                return null;
            case 1: {
                int type = in.readByte();
                ContentModel content = readContentModel(in, names);
                ContentModel next = readContentModel(in, names);
                return defContentModel(type, content, next);
            }
            case 2: {
                int type = in.readByte();
                Object content = getElement(names[in.readShort()]);
                ContentModel next = readContentModel(in, names);
                return defContentModel(type, content, next);
            }
            default:
                throw new IOException("bad bdtd");
        }
    }

    /** A list of names; empty is stored as zero and read as null. */
    private String[] readNameArray(DataInputStream in, String[] names) throws IOException {
        short numNames = in.readShort();
        if (numNames == 0) {
            return null;
        }
        String[] result = new String[numNames];
        for (int i = 0; i < numNames; i++) {
            result[i] = names[in.readShort()];
        }
        return result;
    }

    /**
     * An element's attribute list.
     *
     * <p>It is built by chaining each one in front of the previous, so the list ends up the
     * reverse of the file. It is what the JDK does and what those who walk it expect.
     */
    private AttributeList readAttributeList(DataInputStream in, String[] names)
            throws IOException {
        AttributeList result = null;
        for (int i = in.readByte(); i > 0; i--) {
            short nameId = in.readShort();
            int type = in.readByte();
            int modifier = in.readByte();
            short valueId = in.readShort();
            String value = (valueId == -1) ? null : names[valueId];
            Vector<String> values = null;
            short numValues = in.readShort();
            if (numValues > 0) {
                values = new Vector<String>(numValues);
                for (int j = 0; j < numValues; j++) {
                    values.addElement(names[in.readShort()]);
                }
            }
            result = new AttributeList(names[nameId], type, modifier, value, values, result);
        }
        return result;
    }
}
