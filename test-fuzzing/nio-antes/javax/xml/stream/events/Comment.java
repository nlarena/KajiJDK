package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.Comment -- a comment {@code <!-- ... -->}.
 *
 * <p>A comment has no meaning for the document and StAX still delivers it, because the other option
 * --throwing it away-- makes it impossible to rewrite a file without losing it. Many XML-based
 * formats use comments as a licence, a generation mark or instructions for tools; a transformation
 * that deleted them silently would be a transformation nobody wants.
 *
 * <p>The text {@link #getText()} returns is what is inside, without the delimiters and <b>without
 * any escaping</b>: inside a comment there are no entities nor markup, only the rule that {@code
 * --} cannot appear.
 */
public interface Comment extends XMLEvent {

    /**
     * The text of the comment, without {@code <!--} nor {@code -->}.
     *
     * @return the content; never null, it can be the empty string
     */
    String getText();
}
