package javax.swing.text.html.parser;

/**
 * Una entidad de la DTD: un nombre que se reemplaza por un texto.
 *
 * <h2>Las dos clases de entidad</h2>
 *
 * <p>Una entidad <em>general</em> es la que escribe el autor de la pagina: <code>&amp;amp;</code>
 * se reemplaza por <code>&amp;</code>. Una entidad de <em>parametro</em> solo existe dentro de la
 * DTD y sirve para no repetir listas de elementos.
 *
 * <p>Las dos comparten el campo {@code type}, y la diferencia va en dos bits aparte
 * ({@link DTDConstants#GENERAL} y {@link DTDConstants#PARAMETER}). Por eso {@link #getType}
 * enmascara: el numero crudo trae el tipo y la clase mezclados, y quien pregunta por el tipo no
 * quiere los bits de la clase.
 */
public final class Entity implements DTDConstants {

    /** El nombre, sin el {@code &} ni el {@code ;}. */
    public String name;

    /** El tipo, con los bits de clase todavia adentro; ver {@link #getType}. */
    public int type;

    /** El texto por el que se reemplaza. */
    public char[] data;

    /** Una entidad con ese nombre, tipo y contenido. */
    public Entity(String name, int type, char[] data) {
        this.name = name;
        this.type = type;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    /** El tipo, ya sin los bits que dicen si es general o de parametro. */
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

    /** El contenido como cadena. */
    public String getString() {
        return new String(data, 0, data.length);
    }

    /**
     * El numero de tipo que corresponde a ese nombre.
     *
     * <p>Un nombre desconocido da {@code CDATA}, por el mismo motivo que en
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
