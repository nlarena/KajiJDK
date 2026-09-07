package java.net;

import java.io.IOException;

// The one that turns a response's body into a Java object.
//
// A `ContentHandler` is what lets `url.getContent()` return an `Image` and not an `InputStream`: it
// is chosen by the response's MIME type, and it knows how to read that type.
//
// ===========================================================================================
// IT GOES IN WHOLE, AND IT IS NOT A CONCESSION
// ===========================================================================================
//
// It is abstract and its only abstract method --`getContent(URLConnection)`-- is written by whoever
// extends it. This class does not read from the network: it receives a connection someone else has
// already opened and asks it for the stream. The only thing of its own it contributes is the
// `Class[]` overload, which is pure filtering over the abstract method's result.
//
// What KajiJDK does not have is a CATALOGUE of handlers --the JDK ships its own for `text/plain`,
// `image/gif` and the rest, in internal packages. That is not part of this class: it belongs to
// `URLConnection`, which here only consults the factory the application installs.
public abstract class ContentHandler {

    public ContentHandler() {
    }

    /**
     * Reads {@code urlc}'s body and returns it as an object.
     *
     * @throws IOException if the read fails
     */
    public abstract Object getContent(URLConnection urlc) throws IOException;

    /**
     * Like {@link #getContent(URLConnection)}, but only if the result is of one of the requested
     * types; otherwise null.
     *
     * <p>It exists so that the caller can say "give me this **if** you can give it as an `Image`",
     * and not have to do the `instanceof` and discard after having read everything. That it returns
     * null instead of throwing is on purpose: not finding the requested type is not an I/O error.
     */
    public Object getContent(URLConnection urlc, Class[] classes) throws IOException {
        Object obj = this.getContent(urlc);
        int i = 0;
        while (i < classes.length) {
            if (classes[i].isInstance(obj)) {
                return obj;
            }
            i = i + 1;
        }
        return null;
    }
}
