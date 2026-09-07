package javax.swing;

import javax.swing.event.ChangeListener;

/**
 * La secuencia por la que se mueve un {@link JSpinner}.
 *
 * <h2>Una secuencia, no un rango</h2>
 *
 * <p>El modelo no dice cuantos elementos hay ni permite ir al numero tal: solo sabe cual es el
 * valor de ahora, cual viene despues y cual venia antes. Con eso alcanza para un control que solo
 * tiene dos flechas, y de paso permite secuencias infinitas -- una fecha, un numero sin tope -- que
 * no se podrian enumerar.
 *
 * <h2>Nulo significa que se acabo</h2>
 *
 * <p>{@link #getNextValue} y {@link #getPreviousValue} devuelven nulo cuando no hay siguiente o
 * anterior. Es lo que el control usa para apagar una flecha. No es un error: es el final de la
 * secuencia.
 *
 * <h2>El valor puede salirse</h2>
 *
 * <p>{@link #setValue} acepta lo que se le de mientras sea del tipo que el modelo entiende, aunque
 * quede fuera de los limites. Recortar en silencio esconderia el error de quien lo puso; el
 * control se entera igual, porque desde ahi las flechas devuelven nulo.
 */
public interface SpinnerModel {

    /** El valor de ahora. */
    Object getValue();

    /**
     * Cambia el valor.
     *
     * @throws IllegalArgumentException si el modelo no entiende ese valor.
     */
    void setValue(Object value);

    /** El siguiente, o nulo si no hay; ver la nota de la interfaz. */
    Object getNextValue();

    /** El anterior, o nulo si no hay. */
    Object getPreviousValue();

    /** Escucha los cambios de valor. */
    void addChangeListener(ChangeListener l);

    void removeChangeListener(ChangeListener l);
}
