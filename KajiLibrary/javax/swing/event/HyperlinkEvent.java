package javax.swing.event;

import java.awt.event.InputEvent;
import java.net.URL;
import java.util.EventObject;

import javax.swing.text.Element;

/**
 * Algo paso con un enlace: el mouse entro, salio, o se lo activo.
 *
 * <p>Lleva la {@link URL} <em>y</em> la descripcion en texto, y las dos hacen falta: un enlace
 * relativo o mal formado no da URL, y en ese caso el texto es lo unico que queda. Un lector que solo
 * mire {@link #getURL} se pierde justamente los enlaces rotos, que son los que hay que reportar.
 *
 * <p>{@link #getSourceElement} es el elemento del documento donde estaba el enlace, para poder
 * cambiarle el formato — resaltarlo al pasar por encima, por ejemplo.
 */
public class HyperlinkEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    private EventType type;
    private URL u;
    private String desc;
    private Element sourceElement;
    private InputEvent inputEvent;

    /** Con la URL sola. */
    public HyperlinkEvent(Object source, EventType type, URL u) {
        this(source, type, u, null, null, null);
    }

    /** Con la URL y la descripcion. */
    public HyperlinkEvent(Object source, EventType type, URL u, String desc) {
        this(source, type, u, desc, null, null);
    }

    /** Agregando el elemento del documento. */
    public HyperlinkEvent(Object source, EventType type, URL u, String desc,
            Element sourceElement) {
        this(source, type, u, desc, sourceElement, null);
    }

    /** Agregando el evento de entrada que lo provoco. */
    public HyperlinkEvent(Object source, EventType type, URL u, String desc,
            Element sourceElement, InputEvent inputEvent) {
        super(source);
        this.type = type;
        this.u = u;
        this.desc = desc;
        this.sourceElement = sourceElement;
        this.inputEvent = inputEvent;
    }

    /** Que paso con el enlace. */
    public EventType getEventType() {
        return this.type;
    }

    /** El texto del enlace; lo unico que queda si la URL no se pudo formar. */
    public String getDescription() {
        return this.desc;
    }

    /** La direccion, o {@code null} si no se pudo formar. */
    public URL getURL() {
        return this.u;
    }

    /** El elemento del documento donde estaba el enlace, o {@code null}. */
    public Element getSourceElement() {
        return this.sourceElement;
    }

    /**
     * El evento de entrada que lo provoco, o {@code null}.
     *
     * <p>Sirve para mirar los modificadores: un clic con control apretado suele querer decir
     * "abrilo en otro lado".
     */
    public InputEvent getInputEvent() {
        return this.inputEvent;
    }

    /** Que paso con el enlace. */
    public static final class EventType {

        /** El mouse entro. */
        public static final EventType ENTERED = new EventType("ENTERED");

        /** El mouse salio. */
        public static final EventType EXITED = new EventType("EXITED");

        /** Se activo el enlace. */
        public static final EventType ACTIVATED = new EventType("ACTIVATED");

        private String tipo;

        private EventType(String tipo) {
            this.tipo = tipo;
        }

        public String toString() {
            return this.tipo;
        }
    }
}
