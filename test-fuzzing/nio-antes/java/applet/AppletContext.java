package java.applet;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * The browser, seen from inside an applet.
 *
 * <p>It is what the applet uses to ask things of the environment hosting it: showing another page,
 * writing in the status bar, loading an image or a sound, and finding the **other applets** of the
 * same page, which is how two applets talk to each other.
 *
 * <p>The three "stream" methods are a drawer shared by the page: one applet leaves a stream under a
 * name and another applet of the same page can read it. It was never much used.
 *
 * @deprecated the applet model has been deprecated since Java 9 and marked for removal since 17.
 */
@Deprecated(since = "9", forRemoval = true)
public interface AppletContext {

    /** A sound at that address. */
    AudioClip getAudioClip(URL url);

    /** An image at that address; the loading starts only when somebody draws it. */
    Image getImage(URL url);

    /**
     * The applet of that page with that name.
     *
     * @return the applet, or `null` if there is none with that name
     */
    Applet getApplet(String name);

    /** Every applet on the page, including the one asking. */
    Enumeration<Applet> getApplets();

    /** Asks the browser to replace the current page with that one. */
    void showDocument(URL url);

    /**
     * Asks the browser to show that page in that window or frame.
     *
     * @param target a frame name, or `_self`, `_parent`, `_top` or `_blank`
     */
    void showDocument(URL url, String target);

    /** Writes in the browser's status bar. */
    void showStatus(String status);

    /**
     * Leaves a stream under that name in the page's drawer.
     *
     * @throws IOException if the stream could not be stored
     */
    void setStream(String key, InputStream stream) throws IOException;

    /**
     * The stream under that name.
     *
     * @return the stream, or `null` if there is none
     */
    InputStream getStream(String key);

    /** The names of every stream in the drawer. */
    Iterator<String> getStreamKeys();
}
