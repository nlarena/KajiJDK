package java.awt.datatransfer;

import java.util.EventObject;

/**
 * What is on a clipboard has changed.
 *
 * <p>It does not say **what** changed: only that the contents are now something else. Whoever
 * receives it has to ask the clipboard, and that is on purpose — between the notice and the query
 * the contents may have changed again, and an event that carried the data would be lying half the
 * time.
 */
public class FlavorEvent extends EventObject {

    private static final long serialVersionUID = -5842664112252414548L;

    /**
     * With the clipboard that changed.
     *
     * @throws IllegalArgumentException if it is `null`
     */
    public FlavorEvent(Clipboard source) {
        super(source);
    }
}
