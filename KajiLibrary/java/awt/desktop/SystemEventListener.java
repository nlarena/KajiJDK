package java.awt.desktop;

import java.util.EventListener;

/**
 * KajiLibrary's java.awt.desktop.SystemEventListener -- the mark of the desktop's listeners.
 *
 * <p>It declares nothing. It exists so that {@code Desktop.addAppEventListener} has a single
 * parameter instead of one overload per kind of listener, and so that the desktop can hand out each
 * event by asking with {@code instanceof}.
 *
 * <p>The difference from this package's handlers --{@link AboutHandler} and company-- is one of
 * nature, not of form: a listener receives a <b>notice</b> and there may be many; a handler takes a
 * <b>responsibility</b> and there is only one.
 */
public interface SystemEventListener extends EventListener {
}
