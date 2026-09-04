package javax.swing.event;

import java.util.EventObject;

/**
 * El cursor de texto se movio.
 *
 * <p>Lleva <strong>dos</strong> posiciones y no una, y ahi esta todo el contenido de la clase: el
 * <em>punto</em> es donde esta el cursor y la <em>marca</em> es donde empezo la seleccion. Cuando
 * coinciden no hay nada seleccionado.
 *
 * <p>La marca puede ser mayor que el punto —seleccionando hacia atras— asi que quien quiera el rango
 * tiene que ordenarlos. Devolver siempre el menor primero perderia la direccion, que es lo que
 * decide hacia donde crece la seleccion si el usuario sigue arrastrando.
 */
public abstract class CaretEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** @param source de quien es el cursor */
    public CaretEvent(Object source) {
        super(source);
    }

    /** Donde esta el cursor. */
    public abstract int getDot();

    /** Donde empezo la seleccion; igual al punto si no hay seleccion. */
    public abstract int getMark();
}
