package java.beans;

import java.applet.Applet;
import java.beans.beancontext.BeanContext;

/**
 * Whoever prepares a bean that turns out to be an applet, when {@link Beans#instantiate} brings it.
 *
 * <p>A loose applet is no use: it needs a {@link java.applet.AppletStub} to tell it where it is and
 * a context to host it, and that is normally put there by the browser. When the applet is created by
 * `Beans.instantiate` there is no browser, so someone has to do that work: it is this type. The two
 * steps are kept apart because they go at different moments —preparing before entering the context,
 * starting afterwards.
 *
 * <p>Here neither of the two methods ever gets called: an {@link Applet} cannot be constructed with
 * no screen. The type is here in full all the same, because `Beans.instantiate` names it.
 *
 * @deprecated the applet model has been deprecated since Java 9 and marked for removal since 17.
 */
@Deprecated(since = "9", forRemoval = true)
public interface AppletInitializer {

    /**
     * Prepares the applet: it gives it the stub and whatever else it needs to be able to run.
     *
     * @param bCtxt the context that is going to host it, or `null`
     */
    void initialize(Applet newAppletBean, BeanContext bCtxt);

    /** It starts it: this is the moment to call {@code start()}. */
    void activate(Applet newApplet);
}
