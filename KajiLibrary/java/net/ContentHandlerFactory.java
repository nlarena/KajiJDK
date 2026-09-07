package java.net;

// From a MIME type to the object that knows how to interpret a body of that type.
//
// The factory is `URLConnection.getContent()`'s extension point: with none installed, a connection
// does not know how to turn bytes into objects and says so by throwing. With one, `getContent()` over
// an `image/png` can return an image instead of a stream.
public interface ContentHandlerFactory {

    /**
     * The handler for {@code mimetype}, or null if this factory does not know that type.
     *
     * <p>Returning null is not an error: it means "not me", and whoever asked goes on looking.
     */
    ContentHandler createContentHandler(String mimetype);
}
