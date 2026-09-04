package javax.swing.event;

import java.util.EventListener;

import javax.swing.tree.ExpandVetoException;

/**
 * Quien puede <strong>oponerse</strong> a que una rama se abra o se cierre.
 *
 * <h2>La diferencia con {@link TreeExpansionListener}</h2>
 *
 * <p>Aquel avisa cuando ya paso; este pregunta antes. Y la pregunta es real: tirando una
 * {@link ExpandVetoException} el oyente cancela la operacion, y el arbol se queda como estaba.
 *
 * <p>Que la excepcion sea chequeada es lo que obliga al arbol a preverla en vez de asumir que la
 * expansion siempre ocurre. Sirve, por ejemplo, para una rama que carga sus hijos de la red y quiere
 * negarse si no hay conexion — mejor no abrirla que abrirla vacia.
 */
public interface TreeWillExpandListener extends EventListener {

    /** La rama esta por abrirse. */
    void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException;

    /** La rama esta por cerrarse. */
    void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException;
}
