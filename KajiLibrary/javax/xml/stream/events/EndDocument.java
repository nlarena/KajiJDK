package javax.xml.stream.events;

/**
 * KajiLibrary's javax.xml.stream.events.EndDocument -- the end of the document.
 *
 * <p>It declares no member of its own, and that is the only interesting thing about it: it is an
 * event whose information ends at existing. It is emitted only once, after the end of the root
 * element and of whatever there is in the epilogue, and after it {@link
 * javax.xml.stream.XMLEventReader#hasNext()} returns false.
 *
 * <p>It is worth it being an event and not simply the end of the loop: whoever accumulates events
 * in a list to rewrite them needs to be able to represent "it ended here", and whoever writes with
 * {@link javax.xml.stream.XMLEventWriter} uses it to close whatever is left open.
 */
public interface EndDocument extends XMLEvent {
}
