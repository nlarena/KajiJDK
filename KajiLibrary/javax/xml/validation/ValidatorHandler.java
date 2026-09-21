package javax.xml.validation;

import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * KajiLibrary's javax.xml.validation.ValidatorHandler -- validates while reading.
 *
 * <p>It is a {@link ContentHandler} that validates what arrives and passes it on to another {@code
 * ContentHandler}. It plugs into the middle of a SAX chain, and from there come its two advantages
 * over {@link Validator}: it does not need the whole document in memory, and whoever receives the
 * output receives it already validated.
 *
 * <h2>What comes out is not the same as what went in</h2>
 *
 * <p>The handler further down receives the attributes with the <b>default values</b> the schema
 * put, not only the ones that were written. It is what is wanted --building the tree already
 * complete-- and it is the reason it is advisable to put the validator before and not after the one
 * that builds.
 *
 * <p>{@link #getTypeInfoProvider} is what makes this really worthwhile: it lets one know, for each
 * element and attribute that goes by, what type it was according to the schema. See there the rules
 * on when it can be asked.
 *
 * <h2>A detail that bites</h2>
 *
 * <p>This class implements {@code ContentHandler} and also has {@link #setContentHandler}. It is
 * not the same: what it implements is the <b>input</b> --what the reader sends it-- and what is set
 * with the setter is the <b>output</b>. Passing itself as output builds an infinite loop.
 */
public abstract class ValidatorHandler implements ContentHandler {

    /** For the subclasses. */
    protected ValidatorHandler() {
    }

    /** Where the validated content goes. See the class note: it is not the input. */
    public abstract void setContentHandler(ContentHandler receiver);

    /** Ver {@link #setContentHandler}. */
    public abstract ContentHandler getContentHandler();

    /** Who receives the validation errors and warnings. */
    public abstract void setErrorHandler(ErrorHandler errorHandler);

    /** Ver {@link #setErrorHandler}. */
    public abstract ErrorHandler getErrorHandler();

    /** Who resolves the external resources the schema names. */
    public abstract void setResourceResolver(LSResourceResolver resourceResolver);

    /** Ver {@link #setResourceResolver}. */
    public abstract LSResourceResolver getResourceResolver();

    /** The types of what is going by right now; see {@link TypeInfoProvider}. */
    public abstract TypeInfoProvider getTypeInfoProvider();

    /**
     * The value of a flag.
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
