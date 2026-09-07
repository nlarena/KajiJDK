package javax.swing.plaf.multi;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.TreePath;

/**
 * La interfaz grafica de Tree que reparte cada llamada entre varias.
 *
 * <h2>Que es</h2>
 *
 * <p>Guarda una lista de interfaces graficas de Tree y le pasa cada operacion a todas. Lo que
 * devuelve es lo que contesto la primera, que es la del aspecto principal; las demas se enteran
 * igual, y de eso se trata.
 *
 * <h2>Para que sirve tener varias</h2>
 *
 * <p>Para colgarle a un aspecto grafico otro que no dibuja: un lector de pantalla, un registrador
 * de lo que el usuario hace, una ayuda que sigue al foco. Esos observadores necesitan las mismas
 * llamadas que la interfaz de verdad --instalarse, enterarse de cada dibujo-- y no tienen por que
 * saber que hay otro.
 *
 * <p>Sin este mecanismo habria que envolver cada aspecto a mano. Con el, se los enumera en una
 * propiedad y {@link MultiLookAndFeel} arma la lista.
 *
 * <h2>Por que la primera manda</h2>
 *
 * <p>Un metodo devuelve un solo valor y hay varias respuestas. Elegir la primera --y no combinarlas
 * ni quedarse con la ultima-- es lo que hace que el aspecto principal siga mandando: los auxiliares
 * miran, no deciden.
 */
public class MultiTreeUI extends TreeUI {

    /**
     * Las interfaces graficas que se manejan, en orden.
     *
     * <p>La primera es la principal. El orden lo fija {@link MultiLookAndFeel#createUIs} y no es un
     * detalle: es lo que decide quien contesta.
     */
    protected Vector<ComponentUI> uis = new Vector<ComponentUI>();

    /** Una sin ninguna interfaz; las agrega {@link #createUI}. */
    public MultiTreeUI() {
    }

    /**
     * Las interfaces graficas que se manejan.
     *
     * @return un arreglo nuevo, con la principal primero
     */
    public ComponentUI[] getUIs() {
        return MultiLookAndFeel.uisToArray(uis);
    }

    /**
     * La interfaz grafica para ese componente.
     *
     * <p>Devuelve una de estas solo si hay mas de un aspecto configurado. Con uno solo devuelve ese,
     * sin envolverlo: repartir entre uno no hace falta y costaria una llamada de mas por operacion.
     *
     * @param a el componente
     * @return la interfaz grafica
     */
    public static ComponentUI createUI(JComponent a) {
        MultiTreeUI mui = new MultiTreeUI();
        return MultiLookAndFeel.createUIs(mui, mui.uis, a);
    }


    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jTree el {@code JTree}
     * @param treePath el {@code TreePath}
     * @return lo que haya contestado la primera
     */
    public Rectangle getPathBounds(JTree jTree, TreePath treePath) {
        Rectangle returnValue = ((TreeUI) uis.elementAt(0)).getPathBounds(jTree, treePath);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getPathBounds(jTree, treePath);
        }
        return returnValue;
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jTree el {@code JTree}
     * @param i2 el {@code int}
     * @return lo que haya contestado la primera
     */
    public TreePath getPathForRow(JTree jTree, int i2) {
        TreePath returnValue = ((TreeUI) uis.elementAt(0)).getPathForRow(jTree, i2);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getPathForRow(jTree, i2);
        }
        return returnValue;
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jTree el {@code JTree}
     * @param treePath el {@code TreePath}
     * @return lo que haya contestado la primera
     */
    public int getRowForPath(JTree jTree, TreePath treePath) {
        int returnValue = ((TreeUI) uis.elementAt(0)).getRowForPath(jTree, treePath);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getRowForPath(jTree, treePath);
        }
        return returnValue;
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jTree el {@code JTree}
     * @return lo que haya contestado la primera
     */
    public int getRowCount(JTree jTree) {
        int returnValue = ((TreeUI) uis.elementAt(0)).getRowCount(jTree);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getRowCount(jTree);
        }
        return returnValue;
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jTree el {@code JTree}
     * @param i2 el {@code int}
     * @param i3 el {@code int}
     * @return lo que haya contestado la primera
     */
    public TreePath getClosestPathForLocation(JTree jTree, int i2, int i3) {
        TreePath returnValue = ((TreeUI) uis.elementAt(0)).getClosestPathForLocation(jTree, i2, i3);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getClosestPathForLocation(jTree, i2, i3);
        }
        return returnValue;
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jTree el {@code JTree}
     * @return lo que haya contestado la primera
     */
    public boolean isEditing(JTree jTree) {
        boolean returnValue = ((TreeUI) uis.elementAt(0)).isEditing(jTree);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).isEditing(jTree);
        }
        return returnValue;
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jTree el {@code JTree}
     * @return lo que haya contestado la primera
     */
    public boolean stopEditing(JTree jTree) {
        boolean returnValue = ((TreeUI) uis.elementAt(0)).stopEditing(jTree);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).stopEditing(jTree);
        }
        return returnValue;
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jTree el {@code JTree}
     */
    public void cancelEditing(JTree jTree) {
        for (int i = 0; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).cancelEditing(jTree);
        }
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jTree el {@code JTree}
     * @param treePath el {@code TreePath}
     */
    public void startEditingAtPath(JTree jTree, TreePath treePath) {
        for (int i = 0; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).startEditingAtPath(jTree, treePath);
        }
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jTree el {@code JTree}
     * @return lo que haya contestado la primera
     */
    public TreePath getEditingPath(JTree jTree) {
        TreePath returnValue = ((TreeUI) uis.elementAt(0)).getEditingPath(jTree);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getEditingPath(jTree);
        }
        return returnValue;
    }

    /**
     * Si el punto cae dentro del componente.
     *
     * @param jComponent el {@code JComponent}
     * @param i2 el {@code int}
     * @param i3 el {@code int}
     * @return lo que haya contestado la primera
     */
    public boolean contains(JComponent jComponent, int i2, int i3) {
        boolean returnValue = ((TreeUI) uis.elementAt(0)).contains(jComponent, i2, i3);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).contains(jComponent, i2, i3);
        }
        return returnValue;
    }

    /**
     * Redibuja el fondo y despues el componente.
     *
     * @param graphics el {@code Graphics}
     * @param jComponent el {@code JComponent}
     */
    public void update(Graphics graphics, JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).update(graphics, jComponent);
        }
    }

    /**
     * Se instala sobre el componente.
     *
     * @param jComponent el {@code JComponent}
     */
    public void installUI(JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).installUI(jComponent);
        }
    }

    /**
     * Se desinstala del componente.
     *
     * @param jComponent el {@code JComponent}
     */
    public void uninstallUI(JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).uninstallUI(jComponent);
        }
    }

    /**
     * Dibuja el componente.
     *
     * @param graphics el {@code Graphics}
     * @param jComponent el {@code JComponent}
     */
    public void paint(Graphics graphics, JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).paint(graphics, jComponent);
        }
    }

    /**
     * El tamano que preferiria tener.
     *
     * @param jComponent el {@code JComponent}
     * @return lo que haya contestado la primera
     */
    public Dimension getPreferredSize(JComponent jComponent) {
        Dimension returnValue = ((TreeUI) uis.elementAt(0)).getPreferredSize(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getPreferredSize(jComponent);
        }
        return returnValue;
    }

    /**
     * El tamano mas chico con el que puede.
     *
     * @param jComponent el {@code JComponent}
     * @return lo que haya contestado la primera
     */
    public Dimension getMinimumSize(JComponent jComponent) {
        Dimension returnValue = ((TreeUI) uis.elementAt(0)).getMinimumSize(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getMinimumSize(jComponent);
        }
        return returnValue;
    }

    /**
     * El tamano mas grande que acepta.
     *
     * @param jComponent el {@code JComponent}
     * @return lo que haya contestado la primera
     */
    public Dimension getMaximumSize(JComponent jComponent) {
        Dimension returnValue = ((TreeUI) uis.elementAt(0)).getMaximumSize(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getMaximumSize(jComponent);
        }
        return returnValue;
    }

    /**
     * Cuantos hijos accesibles tiene.
     *
     * @param jComponent el {@code JComponent}
     * @return lo que haya contestado la primera
     */
    public int getAccessibleChildrenCount(JComponent jComponent) {
        int returnValue = ((TreeUI) uis.elementAt(0)).getAccessibleChildrenCount(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getAccessibleChildrenCount(jComponent);
        }
        return returnValue;
    }

    /**
     * El hijo accesible de esa posicion.
     *
     * @param jComponent el {@code JComponent}
     * @param i2 el {@code int}
     * @return lo que haya contestado la primera
     */
    public Accessible getAccessibleChild(JComponent jComponent, int i2) {
        Accessible returnValue = ((TreeUI) uis.elementAt(0)).getAccessibleChild(jComponent, i2);
        for (int i = 1; i < uis.size(); i++) {
            ((TreeUI) uis.elementAt(i)).getAccessibleChild(jComponent, i2);
        }
        return returnValue;
    }
}
