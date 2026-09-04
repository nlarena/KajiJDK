package javax.swing.event;

import java.util.EventObject;

/**
 * Un menu se selecciono, se deselecciono o se cancelo.
 *
 * <p>Sin datos mas alla del origen: cual de las tres cosas paso lo dice el metodo del
 * {@link MenuListener} al que llega, no el evento.
 */
public class MenuEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    public MenuEvent(Object source) {
        super(source);
    }
}
