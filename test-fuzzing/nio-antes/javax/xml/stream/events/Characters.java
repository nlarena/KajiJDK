package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.Characters -- the text between the tags.
 *
 * <h2>One type, three event types</h2>
 *
 * <p>{@link XMLEvent#getEventType()} of a {@code Characters} can return three different constants
 * and all three are this same type:
 *
 * <ul>
 *   <li>{@link javax.xml.stream.XMLStreamConstants#CHARACTERS}, normal text;
 *   <li>{@link javax.xml.stream.XMLStreamConstants#CDATA}, text that came in a
 *       {@code <![CDATA[...]]>} section;
 *   <li>{@link javax.xml.stream.XMLStreamConstants#SPACE}, whitespace the DTD declares ignorable.
 * </ul>
 *
 * <p>{@link #getData()} returns the same in the three cases --the content, already without the
 * CDATA wrapper and with the entities resolved--; what changes is where it came from. The
 * distinction survives because whoever rewrites the document wants to put the {@code CDATA} back
 * where it was: losing it does not change the meaning but it does change the text, and there are
 * pipelines that compare texts.
 *
 * <h2>{@link #isWhiteSpace()} and {@link #isIgnorableWhiteSpace()} are not the same</h2>
 *
 * <p>The first is a question about the characters: it looks at the content and answers whether they
 * are all space. The second is a question about the <b>schema</b>: it answers whether the DTD says
 * only elements can be at that place, in which case the space that appears is indentation and not
 * data.
 *
 * <p>The confusion is costly in that only the second authorizes throwing the event away. Without a
 * DTD there is no way of knowing whether the space between two elements is indentation or the
 * content of a text field left blank, so a non-validating parser --like this library's-- always
 * answers false to {@link #isIgnorableWhiteSpace()}: it does not know, and saying yes would be
 * authorizing data loss.
 */
public interface Characters extends XMLEvent {

    /**
     * The text, with the entity references already resolved.
     *
     * @return the content; never null
     */
    String getData();

    /**
     * Whether the content is all whitespace characters.
     *
     * <p>A question about the characters, not about the schema; see the header.
     *
     * @return true if {@link #getData()} is only space
     */
    boolean isWhiteSpace();

    /**
     * Whether it came inside a {@code <![CDATA[...]]>} section.
     *
     * @return true if it was CDATA
     */
    boolean isCData();

    /**
     * Whether the schema declares this space to be indentation that can be discarded.
     *
     * <p>Only a parser that reads the DTD can answer yes; see the header.
     *
     * @return true if it is ignorable space according to the schema
     */
    boolean isIgnorableWhiteSpace();
}
