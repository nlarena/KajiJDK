package javax.xml.stream.events;

import java.util.List;

/**
 * KajiLibrary's javax.xml.stream.events.DTD -- the document type declaration, whole.
 *
 * <h2>An event with two faces</h2>
 *
 * <p>{@link #getDocumentTypeDeclaration()} returns the raw text: everything between {@code
 * <!DOCTYPE} and the {@code >} that closes it, internal subset included. It is what is needed to
 * rewrite the document without losing anything, and it is the only thing a parser that does not
 * interpret the DTD can honestly give.
 *
 * <p>{@link #getEntities()} and {@link #getNotations()} are the other face: the DTD already
 * <b>interpreted</b>, as lists of {@link EntityDeclaration} and {@link NotationDeclaration}. A
 * parser that does not read the internal subset cannot build them, and the specification foresaw
 * it: in that case both return null.
 *
 * <p>{@link #getProcessedDTD()} is the way out for implementations that have their own
 * representation of the DTD --a graph of content models, say-- and want to expose it. It returns
 * {@link Object} because there is no common type to promise, so it only serves whoever knows which
 * implementation they are talking to; returning null is the right answer for the others.
 */
public interface DTD extends XMLEvent {

    /**
     * The complete text of the declaration, as it was written.
     *
     * @return the raw declaration; never null
     */
    String getDocumentTypeDeclaration();

    /**
     * The DTD in the implementation's internal representation, if there is one.
     *
     * @return its own representation, or null if the implementation exposes none
     */
    Object getProcessedDTD();

    /**
     * The entities declared in the internal subset.
     *
     * @return the list, or null if the implementation does not interpret the DTD
     */
    List<EntityDeclaration> getEntities();

    /**
     * The notations declared in the internal subset.
     *
     * @return the list, or null if the implementation does not interpret the DTD
     */
    List<NotationDeclaration> getNotations();
}
