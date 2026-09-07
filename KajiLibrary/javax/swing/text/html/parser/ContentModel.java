package javax.swing.text.html.parser;

import java.io.Serializable;
import java.util.Vector;

/**
 * Que puede haber adentro de un elemento, segun la DTD.
 *
 * <h2>Un arbol donde el tipo es un caracter</h2>
 *
 * <p>Una regla como <code>(#PCDATA | B | I)*</code> se guarda como un arbol de estos. El campo
 * {@link #type} no es un numero de una lista sino el <em>caracter</em> del operador:
 * <code>'|'</code> para elegir uno, <code>','</code> para uno detras de otro, <code>'&amp;'</code>
 * para todos en cualquier orden, y <code>'*'</code>, <code>'?'</code>, <code>'+'</code> para
 * repetir. El cero marca una hoja, y entonces {@link #content} es un {@link Element}.
 *
 * <p>Guardar el operador como su caracter parece un atajo, y lo es, pero tambien es lo que hace
 * que {@link #toString} pueda reconstruir la regla tal como se escribio.
 *
 * <h2>Los hijos van en una lista enlazada</h2>
 *
 * <p>Para los operadores de varios hijos, {@code content} es el primer hijo y los demas cuelgan de
 * su {@code next}. Igual que en {@link AttributeList}: no hay una clase lista aparte.
 */
public final class ContentModel implements Serializable {

    /** El operador, como caracter; cero si es una hoja. */
    public int type;

    /** Un {@link Element} si es hoja, o el primer hijo si es un operador. */
    public Object content;

    /** El siguiente hermano, cuando este modelo es hijo de un operador. */
    public ContentModel next;

    /** Un modelo vacio, para llenar despues. */
    public ContentModel() {
    }

    /** Una hoja: ese elemento y nada mas. */
    public ContentModel(Element content) {
        this(0, content, null);
    }

    /** Un operador de un solo hijo, como {@code *} o {@code ?}. */
    public ContentModel(int type, ContentModel content) {
        this(type, content, null);
    }

    /** Un modelo con ese operador, ese contenido y ese hermano. */
    public ContentModel(int type, Object content, ContentModel next) {
        this.type = type;
        this.content = content;
        this.next = next;
    }

    /**
     * Si el elemento puede no tener nada adentro.
     *
     * <p>Es lo que decide si una etiqueta se puede cerrar enseguida. Un <code>*</code> o un
     * <code>?</code> siempre pueden estar vacios; una secuencia solo si todos sus miembros pueden.
     */
    public boolean empty() {
        switch (type) {
            case '*':
            case '?':
                return true;

            case '+':
            case '|':
                for (ContentModel m = (ContentModel) content; m != null; m = m.next) {
                    if (m.empty()) {
                        return true;
                    }
                }
                return false;

            case ',':
            case '&':
                for (ContentModel m = (ContentModel) content; m != null; m = m.next) {
                    if (!m.empty()) {
                        return false;
                    }
                }
                return true;

            default:
                return false;
        }
    }

    /** Agrega al vector todos los elementos que aparecen en el modelo. */
    public void getElements(Vector<Element> elemVec) {
        switch (type) {
            case '*':
            case '?':
            case '+':
                ((ContentModel) content).getElements(elemVec);
                break;
            case ',':
            case '|':
            case '&':
                for (ContentModel m = (ContentModel) content; m != null; m = m.next) {
                    m.getElements(elemVec);
                }
                break;
            default:
                elemVec.addElement((Element) content);
        }
    }

    /**
     * Si ese elemento puede ser el primero.
     *
     * <p>Es la pregunta que hace el analizador para decidir si tiene que abrir una etiqueta que el
     * autor se salteo. La secuencia es el caso interesante: se puede seguir mirando el que sigue
     * solo mientras los anteriores puedan estar vacios.
     */
    public boolean first(Object token) {
        switch (type) {
            case '*':
            case '?':
            case '|':
            case '&':
                for (ContentModel m = (ContentModel) content; m != null; m = m.next) {
                    if (m.first(token)) {
                        return true;
                    }
                }
                return false;

            case '+':
                return ((ContentModel) content).first(token);

            case ',':
                for (ContentModel m = (ContentModel) content; m != null; m = m.next) {
                    if (m.first(token)) {
                        return true;
                    }
                    if (!m.empty()) {
                        return false;
                    }
                }
                return false;

            default:
                return (content == token);
        }
    }

    /**
     * El unico elemento que puede ir primero, si hay uno solo.
     *
     * <p>Devuelve nulo cuando hay eleccion o cuando el modelo puede estar vacio: en esos casos no
     * hay un primero forzoso, y devolver uno cualquiera haria que el analizador abriera una
     * etiqueta que el documento no pedia.
     */
    public Element first() {
        switch (type) {
            case '&':
            case '|':
            case '*':
            case '?':
                return null;

            case '+':
            case ',':
                return ((ContentModel) content).first();

            default:
                return (Element) content;
        }
    }

    /** La regla, escrita como en la DTD. */
    public String toString() {
        switch (type) {
            case '*':
                return content + "*";
            case '?':
                return content + "?";
            case '+':
                return content + "+";

            case ',':
            case '|':
            case '&': {
                char[] data = {' ', (char) type, ' '};
                String str = "";
                for (ContentModel m = (ContentModel) content; m != null; m = m.next) {
                    str = str + m;
                    if (m.next != null) {
                        str = str + new String(data);
                    }
                }
                return "(" + str + ")";
            }

            default:
                return content.toString();
        }
    }
}
