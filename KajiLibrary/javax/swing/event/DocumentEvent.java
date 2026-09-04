package javax.swing.event;

import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 * Un cambio en un documento.
 *
 * <h2>Por que es una interfaz y no una clase</h2>
 *
 * <p>Porque el documento lo emite <strong>mientras</strong> aplica el cambio, y armar un objeto con
 * todos los datos por adelantado seria trabajo tirado si nadie escucha. Siendo interfaz, la
 * implementacion puede calcular {@link #getChange} recien cuando alguien lo pide.
 *
 * <h2>{@link ElementChange}, que es la parte cara</h2>
 *
 * <p>Insertar texto no solo cambia caracteres: puede partir un parrafo en dos, o unir dos en uno. El
 * cambio estructural se describe por elemento, y solo para los que efectivamente cambiaron — de ahi
 * que {@code getChange} devuelva {@code null} para los que no.
 */
public interface DocumentEvent {

    /** Donde empezo el cambio. */
    int getOffset();

    /** Cuantos caracteres abarca. */
    int getLength();

    /** El documento que cambio. */
    Document getDocument();

    /** Si fue insercion, borrado o cambio de atributos. */
    EventType getType();

    /** Como cambio la estructura debajo de {@code elem}, o {@code null} si no cambio. */
    ElementChange getChange(Element elem);

    /** Como cambiaron los hijos de un elemento. */
    public interface ElementChange {

        /** El elemento cuyos hijos cambiaron. */
        Element getElement();

        /** Desde que hijo. */
        int getIndex();

        /** Los hijos que se fueron. */
        Element[] getChildrenRemoved();

        /** Los hijos que llegaron. */
        Element[] getChildrenAdded();
    }

    /**
     * Que clase de cambio fue.
     *
     * <p>Constantes con nombre y no un enum, y asi es en el JDK: la clase es anterior a que Java
     * tuviera enums, y cambiarla ahora romperia la serializacion de quien la guardo.
     */
    public static final class EventType {

        /** Se inserto texto. */
        public static final EventType INSERT = new EventType("INSERT");

        /** Se borro texto. */
        public static final EventType REMOVE = new EventType("REMOVE");

        /** Cambiaron atributos, sin cambiar el texto. */
        public static final EventType CHANGE = new EventType("CHANGE");

        private String tipo;

        private EventType(String tipo) {
            this.tipo = tipo;
        }

        public String toString() {
            return this.tipo;
        }
    }
}
