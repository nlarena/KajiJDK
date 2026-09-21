package javax.xml.validation;

import java.io.IOException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * KajiLibrary's javax.xml.validation.Validator -- validates a document against a schema.
 *
 * <p>It is obtained from {@link Schema#newValidator} and is <b>not</b> thread-safe. See the note of
 * {@link Schema} on what is worth keeping and what is worth making.
 *
 * <h2>The second argument is not the error output</h2>
 *
 * <p>{@link #validate(Source, Result)} confuses the first time: the {@code Result} is not where the
 * errors go --that is the {@link ErrorHandler}-- but <b>the same document, augmented</b>.
 * Validating with XML Schema adds information that was not in the original: the default values of
 * the missing attributes, and the type of each element. That result is what is called the
 * post-schema-validation infoset, and without this parameter it would be lost.
 *
 * <h2>Without an error handler nobody finds out</h2>
 *
 * <p>Without an {@link ErrorHandler}, a validation error is thrown as {@link SAXException} and
 * stops there. With one, they are all reported and the handler decides whether to go on. For
 * reviewing a document that is the difference between knowing the first problem and knowing all of
 * them.
 *
 * <p>The other half is that a {@code warning} without a handler is <b>silently discarded</b>.
 * Setting one that at least logs is the reasonable minimum.
 */
public abstract class Validator {

    /** For the subclasses. */
    protected Validator() {
    }

    /**
     * Leaves the validator as freshly made.
     *
     * <p>It is abstract and has no default, unlike in {@code DocumentBuilder}: here reuse is the
     * normal thing and an implementation has to be able to clean itself.
     */
    public abstract void reset();

    /**
     * Validates and discards the augmented result.
     *
     * @throws SAXException if the document does not validate and there is no error handler to
     *     absorb it
     */
    public void validate(Source source) throws SAXException, IOException {
        validate(source, null);
    }

    /**
     * Validates and leaves the augmented result in {@code result}.
     *
     * @param result where the document with the default values and the types goes; null to discard
     *     it. See the class note: it is not where the errors go
     */
    public abstract void validate(Source source, Result result) throws SAXException, IOException;

    /** Who receives the errors and warnings. See the class note. */
    public abstract void setErrorHandler(ErrorHandler errorHandler);

    /** Ver {@link #setErrorHandler}. */
    public abstract ErrorHandler getErrorHandler();

    /** Who resolves the external resources the schema or the document name. */
    public abstract void setResourceResolver(LSResourceResolver resourceResolver);

    /** Ver {@link #setResourceResolver}. */
    public abstract LSResourceResolver getResourceResolver();

    /**
     * The value of a flag.
     *
     * <p>By default it knows none. The one every implementation has to recognize is
     * {@code javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING}.
     *
     * @throws SAXNotRecognizedException if it does not know that name
     */
    public boolean getFeature(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    /**
     * Changes a flag.
     *
     * @throws SAXNotRecognizedException if it does not know that name
     */
    public void setFeature(String name, boolean value)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    /**
     * Changes a property.
     *
     * @throws SAXNotRecognizedException if it does not know that name
     */
    public void setProperty(String name, Object object)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }

    /**
     * The value of a property.
     *
     * @throws SAXNotRecognizedException if it does not know that name
     */
    public Object getProperty(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException {
        if (name == null) {
            throw new NullPointerException("the name parameter is null");
        }
        throw new SAXNotRecognizedException(name);
    }
}
