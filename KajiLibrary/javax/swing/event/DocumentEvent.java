package javax.swing.event;

import javax.swing.text.Document;
import javax.swing.text.Element;

/**
 * A change in a document.
 *
 * <h2>Why it is an interface and not a class</h2>
 *
 * <p>Because the document emits it <strong>while</strong> it applies the change, and building an
 * object with all the data up front would be wasted work if nobody listens. Being an interface,
 * the implementation can compute {@link #getChange} only when somebody asks for it.
 *
 * <h2>{@link ElementChange}, which is the expensive part</h2>
 *
 * <p>Inserting text does not only change characters: it may split a paragraph in two, or join two
 * into one. The structural change is described per element, and only for those that actually
 * changed -- hence {@code getChange} returns {@code null} for those that did not.
 */
public interface DocumentEvent {

    /** Where the change started. */
    int getOffset();

    /** How many characters it spans. */
    int getLength();

    /** The document that changed. */
    Document getDocument();

    /** Whether it was an insertion, a removal or a change of attributes. */
    EventType getType();

    /** How the structure under {@code elem} changed, or {@code null} if it did not. */
    ElementChange getChange(Element elem);

    /** How an element's children changed. */
    public interface ElementChange {

        /** The element whose children changed. */
        Element getElement();

        /** From which child. */
        int getIndex();

        /** The children that left. */
        Element[] getChildrenRemoved();

        /** The children that arrived. */
        Element[] getChildrenAdded();
    }

    /**
     * What kind of change it was.
     *
     * <p>Named constants and not an enum, and so it is in the JDK: the class predates Java having
     * enums, and changing it now would break the serialization of whoever stored it.
     */
    public static final class EventType {

        /** Text was inserted. */
        public static final EventType INSERT = new EventType("INSERT");

        /** Text was removed. */
        public static final EventType REMOVE = new EventType("REMOVE");

        /** Attributes changed, without the text changing. */
        public static final EventType CHANGE = new EventType("CHANGE");

        private String type;

        private EventType(String type) {
            this.type = type;
        }

        public String toString() {
            return this.type;
        }
    }
}
