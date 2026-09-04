package java.awt.dnd;

import java.util.EventObject;

/**
 * La base de los eventos que le llegan a un destino de arrastre.
 *
 * <p>Lo único que trae es el contexto, y es lo único que hace falta: el contexto es por donde el
 * destino contesta —aceptar, rechazar, pedir los datos— y por donde llega a saber sobre qué
 * componente está pasando el arrastre.
 */
public class DropTargetEvent extends EventObject {

    private static final long serialVersionUID = 2821229066521922993L;

    /** Por dónde se contesta. */
    protected DropTargetContext context;

    /**
     * Con el contexto del destino.
     *
     * @throws NullPointerException si el contexto es `null`
     */
    public DropTargetEvent(DropTargetContext dtc) {
        super(dtc.getDropTarget());
        this.context = dtc;
    }

    /** Por dónde contestarle al arrastre. */
    public DropTargetContext getDropTargetContext() {
        return this.context;
    }
}
