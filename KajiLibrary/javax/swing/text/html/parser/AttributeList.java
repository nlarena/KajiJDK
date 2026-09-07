package javax.swing.text.html.parser;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Un atributo declarado en la DTD, y el siguiente.
 *
 * <h2>Es una lista, no un elemento de una lista</h2>
 *
 * <p>Cada instancia lleva un campo {@code next}: la lista de atributos de un elemento es la cadena
 * que arranca en el primero. No hay una clase "lista" aparte. Es la forma de 1997 y se conserva
 * porque los campos son publicos y cambiarla romperia a cualquiera que los recorra.
 *
 * <h2>Que guarda de cada atributo</h2>
 *
 * <p>El nombre, el <em>tipo</em> ({@code CDATA}, {@code ID}, {@code NUMBER}...), el
 * <em>modificador</em> ({@code REQUIRED}, {@code IMPLIED}, {@code FIXED}...), el valor por omision
 * si lo tiene, y la lista de valores permitidos si es una enumeracion. Los dos primeros son numeros
 * de {@link DTDConstants} y valen lo mismo aunque signifiquen cosas distintas; ver la nota de esa
 * interfaz.
 */
public final class AttributeList implements DTDConstants, Serializable {

    /** El nombre del atributo. */
    public String name;

    /** El tipo: {@code CDATA}, {@code ID}, {@code NUMBER} y demas. */
    public int type;

    /** Los valores permitidos, si es una enumeracion; nulo si no lo es. */
    public Vector<?> values;

    /** El modificador: {@code REQUIRED}, {@code IMPLIED}, {@code FIXED}, {@code CURRENT}. */
    public int modifier;

    /** El valor por omision, si lo tiene. */
    public String value;

    /** El atributo que sigue; ver la nota de la clase. */
    public AttributeList next;

    AttributeList() {
    }

    /** Un atributo con ese nombre y nada mas. */
    public AttributeList(String name) {
        this.name = name;
    }

    /** Un atributo completo, con el siguiente colgado. */
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

    /** Los valores permitidos, o nulo si el atributo no es una enumeracion. */
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
     * El numero de tipo que corresponde a ese nombre.
     *
     * <p>Un nombre que no se conoce da {@code CDATA}: en una DTD, un tipo raro se trata como texto
     * suelto y no como un error, que es lo unico que permite leer una DTD mas nueva que el lector.
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
     * El nombre de ese numero de tipo.
     *
     * <p>Solo los quince tipos de atributo tienen nombre; los demas numeros dan nulo, incluso los
     * que valen algo en otra de las familias de {@link DTDConstants}.
     */
    public static String type2name(int type) {
        // Una tabla y no un `switch`: los quince tipos son 1..15 sin huecos, asi que el indice
        // alcanza. Ademas el `switch` no compila, porque las constantes vienen de un `.class` y
        // nuestro generador todavia no las pliega en un `case` (hallazgo #503).
        if (type >= 1 && type <= TIPOS.length) {
            return TIPOS[type - 1];
        }
        return null;
    }

    /** Los quince tipos de atributo, en el orden de sus numeros. */
    private static final String[] TIPOS = {
        "CDATA", "ENTITY", "ENTITIES", "ID", "IDREF",
        "IDREFS", "NAME", "NAMES", "NMTOKEN", "NMTOKENS",
        "NOTATION", "NUMBER", "NUMBERS", "NUTOKEN", "NUTOKENS"
    };
}
