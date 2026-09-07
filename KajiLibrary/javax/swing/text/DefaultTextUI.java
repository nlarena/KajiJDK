package javax.swing.text;

import javax.swing.plaf.basic.BasicTextUI;

/**
 * El nombre viejo de {@link BasicTextUI}.
 *
 * <p>No agrega nada: existe solo para que el codigo escrito antes de que la clase se mudara a
 * <code>javax.swing.plaf.basic</code> siga compilando. Es una clase vacia a proposito, y borrarla
 * romperia ese codigo sin ganar nada.
 *
 * @deprecated Usar {@link BasicTextUI}.
 */
@Deprecated
public abstract class DefaultTextUI extends BasicTextUI {

    /** Nada que construir; ver la nota de la clase. */
    protected DefaultTextUI() {
    }
}
