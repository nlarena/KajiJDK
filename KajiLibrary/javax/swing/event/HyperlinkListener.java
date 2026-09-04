package javax.swing.event;

import java.util.EventListener;

/**
 * Quien quiere enterarse de que pasa algo con un enlace.
 */
public interface HyperlinkListener extends EventListener {

    /** El mouse entro, salio, o se activo el enlace. */
    void hyperlinkUpdate(HyperlinkEvent e);
}
