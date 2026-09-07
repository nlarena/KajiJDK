package java.applet;

import java.net.URL;

/**
 * What the browser tells the applet about where it is running.
 *
 * <p>An applet knows nothing by itself: not which page contains it, nor where it was downloaded from,
 * nor which parameters were set on it in the HTML. All of that the browser gives it through this
 * object, which it sets with {@link Applet#setStub}. It is a "stub" in the old sense: a
 * representative of something that is on the other side.
 *
 * @deprecated the applet model has been deprecated since Java 9 and marked for removal since 17.
 */
@Deprecated(since = "9", forRemoval = true)
public interface AppletStub {

    /** Whether the applet is running, that is, between {@link Applet#start} and {@link Applet#stop}. */
    boolean isActive();

    /** The address of the page containing the applet. */
    URL getDocumentBase();

    /** The address the applet's code was downloaded from. */
    URL getCodeBase();

    /**
     * The value of a parameter from the HTML.
     *
     * @return the value, or `null` if there is no parameter with that name
     */
    String getParameter(String name);

    /** The browser, seen as the applet's context. */
    AppletContext getAppletContext();

    /** Asks the browser to give the applet that size. */
    void appletResize(int width, int height);
}
