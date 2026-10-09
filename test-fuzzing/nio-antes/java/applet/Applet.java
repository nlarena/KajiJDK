package java.applet;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Panel;
import java.net.URL;
import java.util.Locale;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * A small program that runs **inside a web page**, drawn by the browser.
 *
 * <p>It is a {@link Panel} with a life cycle the browser drives: {@link #init} on loading it,
 * {@link #start} every time the page is shown, {@link #stop} every time it stops being seen,
 * {@link #destroy} on discarding it. An applet has no `main`: what it does it does in those four
 * methods, and the rest —parameters, images, sounds, the status bar— it asks the browser for through
 * its {@link AppletStub}.
 *
 * <p><strong>With no screen it cannot be constructed.</strong> The constructor throws
 * {@link HeadlessException}, just as in the JDK: an applet exists to be shown by a browser, and with
 * no windowing system there is neither browser nor surface. It is the same decision as in
 * {@code TrayIcon}: no other class hangs off an applet, so there is no reason to diverge from the JDK
 * as there is in {@code Window}. The instance methods are declared because they are part of the
 * class, but there is no instance to call them on.
 *
 * <p>The whole package is in this tree for a single reason: {@code java.beans.AppletInitializer} and
 * one of the forms of {@code java.beans.Beans.instantiate} name this class, and without it
 * `java.beans` could not be closed. They are four small types and they could be written whole.
 *
 * @deprecated the applet model has been deprecated since Java 9 and marked for removal since 17: the
 *     browsers stopped running them.
 */
@Deprecated(since = "9", forRemoval = true)
public class Applet extends Panel {

    private static final long serialVersionUID = -5836846270535785031L;

    /** The browser's representative, or `null` until the browser sets it. */
    private transient AppletStub stub;

    /** The accessibility, built at the first request. */
    AccessibleContext accessibleContext;

    /**
     * An applet.
     *
     * @throws HeadlessException if there is no screen, which is to say always here
     */
    public Applet() throws HeadlessException {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
    }

    /**
     * Sets the browser's representative.
     *
     * <p>It is `final` and the browser calls it, not the applet: it is the only link between the two
     * and an applet that changed it on itself would be left talking to itself.
     */
    public final void setStub(AppletStub stub) {
        this.stub = stub;
    }

    /**
     * Whether it is running, that is, between {@link #start} and {@link #stop}.
     *
     * @return `false` too while it has no browser: without one it never started
     */
    public boolean isActive() {
        return this.stub != null && this.stub.isActive();
    }

    /**
     * The address of the page containing it.
     *
     * @throws NullPointerException if it has no browser yet
     */
    public URL getDocumentBase() {
        return this.stub.getDocumentBase();
    }

    /**
     * The address its code was downloaded from.
     *
     * @throws NullPointerException if it has no browser yet
     */
    public URL getCodeBase() {
        return this.stub.getCodeBase();
    }

    /**
     * The value of a parameter set in the HTML.
     *
     * @return the value, or `null` if there is no parameter with that name
     * @throws NullPointerException if it has no browser yet
     */
    public String getParameter(String name) {
        return this.stub.getParameter(name);
    }

    /**
     * The browser, seen as the context.
     *
     * @throws NullPointerException if it has no browser yet
     */
    public AppletContext getAppletContext() {
        return this.stub.getAppletContext();
    }

    /**
     * Asks the browser for that size.
     *
     * <p>It overrides {@code Component}'s because an applet does not decide its own size: the browser
     * does, and the request has to go through it.
     */
    public void resize(int width, int height) {
        Dimension d = this.size();
        if (d.width != width || d.height != height) {
            super.resize(width, height);
            if (this.stub != null) {
                this.stub.appletResize(width, height);
            }
        }
    }

    /** The same, with a dimension. */
    public void resize(Dimension d) {
        this.resize(d.width, d.height);
    }

    /**
     * Whether it is a validation root.
     *
     * @return `true`: an applet is the root of its own tree, so validation has no reason to climb
     *     beyond it
     */
    public boolean isValidateRoot() {
        return true;
    }

    /** Writes in the browser's status bar. */
    public void showStatus(String msg) {
        this.getAppletContext().showStatus(msg);
    }

    /** An image at that address; the loading starts only when somebody draws it. */
    public Image getImage(URL url) {
        return this.getAppletContext().getImage(url);
    }

    /**
     * An image at that address relative to another.
     *
     * @return the image, or `null` if the address is malformed
     */
    public Image getImage(URL url, String name) {
        try {
            return this.getImage(new URL(url, name));
        } catch (java.net.MalformedURLException e) {
            return null;
        }
    }

    /**
     * A sound at that address, with no need for an applet or a browser.
     *
     * <p>It is `static` because it is the only entry to this package's sound that is of use outside a
     * browser, and that is why it was called from ordinary programs. Here the clip is assembled but
     * cannot sound: its three verbs throw, with the reason given, because this library has no audio
     * engine. Returning a clip that "plays" in silence would be promising a sound that is not there.
     *
     * <p>A `null` address **is accepted**, just as in the JDK: the clip is lazy and does not touch the
     * address until somebody plays it, so there is nothing here to fail yet.
     */
    public static final AudioClip newAudioClip(URL url) {
        return new SilentClip(url);
    }

    /** A sound at that address, through the browser. */
    public AudioClip getAudioClip(URL url) {
        return this.getAppletContext().getAudioClip(url);
    }

    /**
     * A sound at that address relative to another.
     *
     * @return the clip, or `null` if the address is malformed
     */
    public AudioClip getAudioClip(URL url, String name) {
        try {
            return this.getAudioClip(new URL(url, name));
        } catch (java.net.MalformedURLException e) {
            return null;
        }
    }

    /**
     * Who made it and what for.
     *
     * @return `null` in the base; each applet overrides this with its author, version and description
     */
    public String getAppletInfo() {
        return null;
    }

    /**
     * The applet's language.
     *
     * <p>An applet with no language of its own uses the system's, not its parent's as an ordinary
     * component would: its "parent" is the browser, which is not a component.
     */
    public Locale getLocale() {
        Locale l = super.getLocale();
        if (l == null) {
            return Locale.getDefault();
        }
        return l;
    }

    /**
     * Which parameters it understands.
     *
     * @return `null` in the base; each applet overrides this with rows of {name, type, description}
     */
    public String[][] getParameterInfo() {
        return null;
    }

    /** Plays the sound at that address, once. */
    public void play(URL url) {
        AudioClip clip = this.getAudioClip(url);
        if (clip != null) {
            clip.play();
        }
    }

    /** Plays the sound at that address relative to another, once. */
    public void play(URL url, String name) {
        AudioClip clip = this.getAudioClip(url, name);
        if (clip != null) {
            clip.play();
        }
    }

    /** The browser calls it on loading; the base one does nothing. */
    public void init() {
    }

    /** The browser calls it every time the page is shown; the base one does nothing. */
    public void start() {
    }

    /** The browser calls it every time the page stops being seen; the base one does nothing. */
    public void stop() {
    }

    /** The browser calls it on discarding it; the base one does nothing. */
    public void destroy() {
    }

    /** The applet's accessibility. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleApplet();
        }
        return this.accessibleContext;
    }

    /**
     * An applet, for accessibility, is a frame: the root of a tree of components the user sees as one
     * unit.
     */
    protected class AccessibleApplet extends AccessibleAWTPanel {

        /** For the subclasses. */
        protected AccessibleApplet() {
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.FRAME;
        }
    }

    /**
     * A clip that cannot sound.
     *
     * <p>It keeps the address, which is the only thing known about it, and throws in all three verbs
     * with the reason: there is no audio engine. It is what suits a sound that exists but cannot be
     * played, which is different from a sound that does not exist.
     */
    private static final class SilentClip implements AudioClip {

        private final URL url;

        private SilentClip(URL url) {
            this.url = url;
        }

        public void play() {
            throw new UnsupportedOperationException(
                    "this library has no audio engine; it cannot play " + this.url);
        }

        public void loop() {
            this.play();
        }

        /** Stopping something that never sounded is not an error: it does nothing. */
        public void stop() {
        }

        public String toString() {
            return "AudioClip[" + this.url + "]";
        }
    }
}
