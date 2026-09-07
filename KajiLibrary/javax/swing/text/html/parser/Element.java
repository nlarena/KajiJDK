package javax.swing.text.html.parser;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Hashtable;

/**
 * Un elemento declarado en la DTD.
 *
 * <h2>Que sabe de mas que una etiqueta</h2>
 *
 * <p>{@link javax.swing.text.html.HTML.Tag} dice como se muestra una etiqueta. Esto dice como se
 * <em>analiza</em>: que puede ir adentro ({@link #content}), que atributos acepta ({@link #atts}),
 * y si la etiqueta de apertura o la de cierre se pueden omitir ({@link #oStart}, {@link #oEnd}).
 *
 * <p>Ese ultimo par es la razon de ser de todo el analizador con DTD. En HTML se escribe
 * <code>&lt;p&gt;uno&lt;p&gt;dos</code> sin cerrar ningun parrafo, y alguien tiene que saber que
 * eso es legal y donde va el cierre. Ese alguien lee estos campos.
 *
 * <h2>Inclusiones y exclusiones</h2>
 *
 * <p>Dos {@link BitSet} indexados por {@link #index}. La inclusion agrega elementos permitidos
 * adentro de este y de todo lo que cuelgue; la exclusion los prohibe. Sirven para reglas que el
 * modelo de contenido no puede expresar: dentro de un <code>a</code> no puede haber otro
 * <code>a</code>, por hondo que este.
 *
 * <p>Por eso el {@code index} importa y por eso no se puede crear un elemento desde afuera: el
 * numero lo asigna la {@link DTD} que lo contiene, y dos elementos con el mismo indice romperian
 * los dos conjuntos.
 */
public final class Element implements DTDConstants, Serializable {

    /** El numero de este elemento en su DTD; es el indice en los dos {@link BitSet}. */
    public int index;

    /** El nombre, en minusculas. */
    public String name;

    /** Si la etiqueta de apertura se puede omitir. */
    public boolean oStart;

    /** Si la de cierre se puede omitir. */
    public boolean oEnd;

    /** Elementos permitidos adentro, ademas de los del modelo. */
    public BitSet inclusions;

    /** Elementos prohibidos adentro, aunque el modelo los permita. */
    public BitSet exclusions;

    /** {@code EMPTY}, {@code CDATA}, {@code RCDATA}, {@code MODEL} o {@code ANY}. */
    public int type = ANY;

    /** Que puede ir adentro. */
    public ContentModel content;

    /** Los atributos que acepta, encadenados. */
    public AttributeList atts;

    static int maxIndex = 0;

    /** Un lugar para que quien use la DTD cuelgue lo suyo. */
    public Object data;

    Element() {
    }

    /** Solo la DTD crea elementos; ver la nota de la clase. */
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

    /** Si el elemento no lleva nada adentro, como {@code br} o {@code img}. */
    public boolean isEmpty() {
        return type == EMPTY;
    }

    public String toString() {
        return name;
    }

    /** El atributo con ese nombre, o nulo. */
    public AttributeList getAttribute(String name) {
        for (AttributeList a = atts; a != null; a = a.next) {
            if (a.name.equals(name)) {
                return a;
            }
        }
        return null;
    }

    /**
     * El atributo que tiene ese valor entre sus valores permitidos.
     *
     * <p>Sirve para el HTML donde se escribe el valor sin el nombre: en
     * <code>&lt;ul compact&gt;</code>, <code>compact</code> es un valor y hay que averiguar de que
     * atributo. La comparacion no distingue mayusculas porque el HTML tampoco.
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
     * El numero de tipo de contenido que corresponde a ese nombre.
     *
     * <p>Un nombre desconocido da cero, no {@code CDATA}. Es distinto de
     * {@link AttributeList#name2type} a proposito: alla un tipo raro se puede tratar como texto,
     * aca un modelo de contenido que no se entiende no tiene un equivalente razonable.
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
