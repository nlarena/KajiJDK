package javax.xml.crypto;

/**
 * KajiLibrary's javax.xml.crypto.URIDereferencer -- resolves a {@link URIReference} into data.
 *
 * <p>One method, and it is the most important control point of the package. Validating a signature
 * means fetching what the references point to, and those references were written by <b>whoever
 * signed</b>.
 *
 * <p>Without a dereferencer of one's own, validating a signature of unknown origin can make the
 * program read local files or make network requests nobody asked for -- the same problem as XXE,
 * under another name. Putting in one that only resolves references internal to the document is the
 * usual defence.
 *
 * <p>It is installed in the {@link XMLCryptoContext}, so it holds for the whole validation.
 */
public interface URIDereferencer {

    /**
     * The data that reference points to.
     *
     * @throws URIReferenceException if it cannot be resolved
     */
    Data dereference(URIReference uriReference, XMLCryptoContext context)
        throws URIReferenceException;
}
