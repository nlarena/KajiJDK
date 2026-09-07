package javax.swing.text.html.parser;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/**
 * La descripcion de un lenguaje de marcas: que elementos hay y como se anidan.
 *
 * <h2>Para que sirve tener una DTD</h2>
 *
 * <p>El HTML que se escribe de verdad esta lleno de etiquetas sin cerrar y de anidamientos que no
 * se declararon. Un analizador que solo mirara las etiquetas escritas no podria armar un arbol.
 * Este si, porque tiene la DTD: sabe que un <code>&lt;p&gt;</code> cierra al anterior, que un
 * <code>&lt;li&gt;</code> va adentro de una lista aunque no se haya abierto, y que dentro de un
 * <code>&lt;a&gt;</code> no puede haber otro.
 *
 * <h2>Los once elementos que son campos</h2>
 *
 * <p>{@link #html}, {@link #head}, {@link #body} y los demas estan como campos finales porque el
 * analizador los necesita a cada paso y buscarlos por nombre en cada decision seria caro. No son
 * mas importantes que los otros: son los que se consultan seguido.
 *
 * <p>El primero, {@link #pcdata}, no es una etiqueta: es el texto suelto. Que sea un elemento como
 * cualquier otro es lo que permite escribir en un modelo de contenido que un parrafo lleva texto,
 * sin ningun caso especial.
 *
 * <h2>Como se llena</h2>
 *
 * <p>Con los metodos {@code define...} y {@code def...}, o leyendo el formato binario con
 * {@link #read}. Los {@code def...} protegidos son los que usa {@link #read}; los
 * {@code define...} publicos son para armarla a mano. La diferencia es que los primeros aceptan
 * nombres y los segundos objetos ya armados.
 */
public class DTD implements DTDConstants {

    /** El nombre de la DTD, por ejemplo {@code html32}. */
    public String name;

    /** Los elementos, indexados por su {@link Element#index}. */
    public Vector<Element> elements = new Vector<Element>();

    /** Los elementos por nombre. */
    public Hashtable<String, Element> elementHash = new Hashtable<String, Element>();

    /**
     * Las entidades, por nombre y por numero de caracter.
     *
     * <p>Las claves son de dos tipos a proposito: una cadena para <code>&amp;amp;</code> y un
     * {@link Integer} para <code>&amp;#38;</code>. Dos tablas separadas serian mas prolijas y
     * obligarian a preguntar dos veces en cada busqueda.
     */
    public Hashtable<Object, Entity> entityHash = new Hashtable<Object, Entity>();

    /** El texto suelto; ver la nota de la clase. */
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
     * Cuatro mas que el analizador consulta seguido, sin documentar en el JDK.
     *
     * <p>Estan aca por el mismo motivo que los publicos. Pero ademas importa <em>cuando</em> se
     * crean: {@link #getElement(String)} le da a cada elemento nuevo el numero que sigue, asi que
     * crearlos en el constructor les reserva los indices 11 a 14. Un {@link BitSet} de exclusiones
     * guardado con una numeracion y leido con otra apuntaria a los elementos equivocados.
     */
    final Element style = getElement("style");

    final Element link = getElement("link");

    final Element script = getElement("script");

    final Element unknown = getElement("unknown");

    Element html32title = getElement("title");

    /** La version del formato binario que entiende {@link #read}. */
    public static final int FILE_VERSION = 1;

    private static final Hashtable<String, DTD> dtdHash = new Hashtable<String, DTD>();

    /** Una DTD vacia con ese nombre. */
    protected DTD(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** La entidad con ese nombre, o nulo. */
    public Entity getEntity(String name) {
        return entityHash.get(name);
    }

    /** La entidad de ese caracter, o nulo; ver la nota de {@link #entityHash}. */
    public Entity getEntity(int ch) {
        return entityHash.get(Integer.valueOf(ch));
    }

    /**
     * El elemento con ese nombre, creandolo si no existe.
     *
     * <p>Crear en lugar de devolver nulo es a proposito y es lo que hace que se pueda declarar un
     * modelo de contenido que nombre un elemento antes de declararlo. Cuando llegue su declaracion,
     * {@link #defineElement} la va a completar sobre el mismo objeto, y los modelos que ya lo
     * apuntaban van a quedar bien.
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

    /** El elemento con ese numero. */
    public Element getElement(int index) {
        return elements.elementAt(index);
    }

    /**
     * Declara una entidad.
     *
     * <p>Si ya estaba declarada no se toca: en una DTD, la primera declaracion gana, y es lo que
     * permite que un documento redefina una entidad antes de incluir la DTD general.
     */
    public Entity defineEntity(String name, int type, char[] data) {
        Entity ent = entityHash.get(name);
        if (ent == null) {
            ent = new Entity(name, type, data);
            entityHash.put(name, ent);
            // Una entidad general de un solo caracter se indexa tambien por ese caracter, para
            // que `&#38;` encuentre lo mismo que `&amp;`. Un `switch` no compila: las constantes
            // vienen de un `.class` y todavia no se pliegan en un `case` (hallazgo #503).
            int clase = type & 0xFFFF;
            if (((type & GENERAL) != 0) && (data.length == 1)
                    && (clase == CDATA || clase == SDATA)) {
                entityHash.put(Integer.valueOf(data[0]), ent);
            }
        }
        return ent;
    }

    /** Declara un elemento, o completa el que ya se habia creado por nombre. */
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
     * Agrega atributos a un elemento.
     *
     * <p>Los que ya estaban ganan: una segunda declaracion del mismo atributo no lo pisa.
     */
    public void defineAttributes(String name, AttributeList atts) {
        Element e = getElement(name);
        e.atts = atts;
    }

    /** Declara una entidad de un solo caracter. */
    public Entity defEntity(String name, int type, int ch) {
        char[] data = {(char) ch};
        return defineEntity(name, type, data);
    }

    /** Declara una entidad cuyo contenido es ese texto. */
    protected Entity defEntity(String name, int type, String str) {
        int len = str.length();
        char[] data = new char[len];
        str.getChars(0, len, data, 0);
        return defineEntity(name, type, data);
    }

    /** Declara un elemento nombrando sus exclusiones e inclusiones. */
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

    /** Arma un atributo y lo encadena adelante del que se le pase. */
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

    /** Arma un modelo de contenido. */
    protected ContentModel defContentModel(int type, Object obj, ContentModel next) {
        return new ContentModel(type, obj, next);
    }

    public String toString() {
        return name;
    }

    /** Guarda una DTD con ese nombre para que {@link #getDTD} la encuentre. */
    public static void putDTDHash(String name, DTD dtd) {
        dtdHash.put(name, dtd);
    }

    /**
     * La DTD con ese nombre, creandola vacia si no estaba.
     *
     * <p>El nombre se pasa a minusculas antes de buscar. Devolver una DTD vacia en lugar de fallar
     * es lo que hace el JDK, y es lo que permite armarla despues sobre el objeto devuelto.
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
     * Lee una DTD del formato binario.
     *
     * <p>El formato guarda todos los nombres una sola vez en una tabla al principio y despues los
     * nombra por su numero. Es lo que hace que una DTD de HTML entre en veinte kilobytes: los
     * mismos cien nombres aparecen miles de veces.
     *
     * @throws IOException si la version no es {@link #FILE_VERSION} o el archivo esta cortado.
     */
    public void read(DataInputStream in) throws IOException {
        if (in.readInt() != FILE_VERSION) {
            throw new IOException("version mismatch");
        }

        // La tabla de nombres.
        short numNames = in.readShort();
        String[] names = new String[numNames];
        for (int i = 0; i < numNames; i++) {
            names[i] = in.readUTF();
        }

        // Las entidades.
        short numEntities = in.readShort();
        for (int i = 0; i < numEntities; i++) {
            short nameId = in.readShort();
            int type = in.readByte();
            String name = in.readUTF();
            defEntity(names[nameId], type | GENERAL, name);
        }

        // Los elementos.
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
     * Un modelo de contenido, en el orden en que se escribio.
     *
     * <p>El byte de arranque dice de que se trata: cero es el fin, uno un operador con otro modelo
     * adentro, dos una hoja que nombra un elemento.
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

    /** Una lista de nombres; vacia se guarda como cero y se lee como nulo. */
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
     * La lista de atributos de un elemento.
     *
     * <p>Se arma encadenando cada uno adelante del anterior, asi que la lista queda al reves del
     * archivo. Es lo que hace el JDK y lo que esperan los que la recorren.
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
