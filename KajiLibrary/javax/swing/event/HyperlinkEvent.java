package javax.swing.event;

import java.awt.event.InputEvent;
import java.net.URL;
import java.util.EventObject;

import javax.swing.text.Element;

/**
 * Something happened with a link: the mouse entered, left, or it was activated.
 *
 * <p>It carries the {@link URL} <em>and</em> the description as text, and both are needed: a
 * relative or malformed link gives no URL, and in that case the text is all that is left. A
 * reader that only looks at {@link #getURL} misses precisely the broken links, which are the ones
 * worth reporting.
 *
 * <p>{@link #getSourceElement} is the document element where the link was, so that its formatting
 * can be changed -- highlighting it on hover, for instance.
 */
public class HyperlinkEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    private EventType type;
    private URL u;
    private String desc;
    private Element sourceElement;
    private InputEvent inputEvent;

    /** With the URL alone. */
    public HyperlinkEvent(Object source, EventType type, URL u) {
        this(source, type, u, null, null, null);
    }

    /** With the URL and the description. */
    public HyperlinkEvent(Object source, EventType type, URL u, String desc) {
        this(source, type, u, desc, null, null);
    }

    /** Adding the document element. */
    public HyperlinkEvent(Object source, EventType type, URL u, String desc,
            Element sourceElement) {
        this(source, type, u, desc, sourceElement, null);
    }

    /** Adding the input event that caused it. */
    public HyperlinkEvent(Object source, EventType type, URL u, String desc,
            Element sourceElement, InputEvent inputEvent) {
        super(source);
        this.type = type;
        this.u = u;
        this.desc = desc;
        this.sourceElement = sourceElement;
        this.inputEvent = inputEvent;
    }

    /** What happened with the link. */
    public EventType getEventType() {
        return this.type;
    }

    /** The link's text; all that is left if the URL could not be formed. */
    public String getDescription() {
        return this.desc;
    }

    /** The address, or {@code null} if it could not be formed. */
    public URL getURL() {
        return this.u;
    }

    /** The document element where the link was, or {@code null}. */
    public Element getSourceElement() {
        return this.sourceElement;
    }

    /**
     * The input event that caused it, or {@code null}.
     *
     * <p>It serves for looking at the modifiers: a click with control held usually means "open it
     * somewhere else".
     */
    public InputEvent getInputEvent() {
        return this.inputEvent;
    }

    /** What happened with the link. */
    public static final class EventType {

        /** The mouse entered. */
        public static final EventType ENTERED = new EventType("ENTERED");

        /** The mouse left. */
        public static final EventType EXITED = new EventType("EXITED");

        /** The link was activated. */
        public static final EventType ACTIVATED = new EventType("ACTIVATED");

        private String type;

        private EventType(String type) {
            this.type = type;
        }

        public String toString() {
            return this.type;
        }
    }
}
