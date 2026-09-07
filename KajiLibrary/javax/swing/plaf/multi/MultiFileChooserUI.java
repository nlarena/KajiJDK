package javax.swing.plaf.multi;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FileChooserUI;

/**
 * La interfaz grafica de FileChooser que reparte cada llamada entre varias.
 *
 * <h2>Que es</h2>
 *
 * <p>Guarda una lista de interfaces graficas de FileChooser y le pasa cada operacion a todas. Lo
 * que devuelve es lo que contesto la primera, que es la del aspecto principal; las demas se enteran
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
public class MultiFileChooserUI extends FileChooserUI {

    /**
     * Las interfaces graficas que se manejan, en orden.
     *
     * <p>La primera es la principal. El orden lo fija {@link MultiLookAndFeel#createUIs} y no es un
     * detalle: es lo que decide quien contesta.
     */
    protected Vector<ComponentUI> uis = new Vector<ComponentUI>();

    /** Una sin ninguna interfaz; las agrega {@link #createUI}. */
    public MultiFileChooserUI() {
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
        MultiFileChooserUI mui = new MultiFileChooserUI();
        return MultiLookAndFeel.createUIs(mui, mui.uis, a);
    }


    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jFileChooser el {@code JFileChooser}
     * @return lo que haya contestado la primera
     */
    public FileFilter getAcceptAllFileFilter(JFileChooser jFileChooser) {
        FileFilter returnValue =
                ((FileChooserUI) uis.elementAt(0)).getAcceptAllFileFilter(jFileChooser);
        for (int i = 1; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).getAcceptAllFileFilter(jFileChooser);
        }
        return returnValue;
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jFileChooser el {@code JFileChooser}
     * @return lo que haya contestado la primera
     */
    public FileView getFileView(JFileChooser jFileChooser) {
        FileView returnValue = ((FileChooserUI) uis.elementAt(0)).getFileView(jFileChooser);
        for (int i = 1; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).getFileView(jFileChooser);
        }
        return returnValue;
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jFileChooser el {@code JFileChooser}
     * @return lo que haya contestado la primera
     */
    public String getApproveButtonText(JFileChooser jFileChooser) {
        String returnValue = ((FileChooserUI) uis.elementAt(0)).getApproveButtonText(jFileChooser);
        for (int i = 1; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).getApproveButtonText(jFileChooser);
        }
        return returnValue;
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jFileChooser el {@code JFileChooser}
     * @return lo que haya contestado la primera
     */
    public String getDialogTitle(JFileChooser jFileChooser) {
        String returnValue = ((FileChooserUI) uis.elementAt(0)).getDialogTitle(jFileChooser);
        for (int i = 1; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).getDialogTitle(jFileChooser);
        }
        return returnValue;
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jFileChooser el {@code JFileChooser}
     */
    public void rescanCurrentDirectory(JFileChooser jFileChooser) {
        for (int i = 0; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).rescanCurrentDirectory(jFileChooser);
        }
    }

    /**
     * Se lo pide a todas y contesta la primera.
     *
     * @param jFileChooser el {@code JFileChooser}
     * @param file el {@code java.io.File}
     */
    public void ensureFileIsVisible(JFileChooser jFileChooser, java.io.File file) {
        for (int i = 0; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).ensureFileIsVisible(jFileChooser, file);
        }
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
        boolean returnValue = ((FileChooserUI) uis.elementAt(0)).contains(jComponent, i2, i3);
        for (int i = 1; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).contains(jComponent, i2, i3);
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
            ((FileChooserUI) uis.elementAt(i)).update(graphics, jComponent);
        }
    }

    /**
     * Se instala sobre el componente.
     *
     * @param jComponent el {@code JComponent}
     */
    public void installUI(JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).installUI(jComponent);
        }
    }

    /**
     * Se desinstala del componente.
     *
     * @param jComponent el {@code JComponent}
     */
    public void uninstallUI(JComponent jComponent) {
        for (int i = 0; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).uninstallUI(jComponent);
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
            ((FileChooserUI) uis.elementAt(i)).paint(graphics, jComponent);
        }
    }

    /**
     * El tamano que preferiria tener.
     *
     * @param jComponent el {@code JComponent}
     * @return lo que haya contestado la primera
     */
    public Dimension getPreferredSize(JComponent jComponent) {
        Dimension returnValue = ((FileChooserUI) uis.elementAt(0)).getPreferredSize(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).getPreferredSize(jComponent);
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
        Dimension returnValue = ((FileChooserUI) uis.elementAt(0)).getMinimumSize(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).getMinimumSize(jComponent);
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
        Dimension returnValue = ((FileChooserUI) uis.elementAt(0)).getMaximumSize(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).getMaximumSize(jComponent);
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
        int returnValue = ((FileChooserUI) uis.elementAt(0)).getAccessibleChildrenCount(jComponent);
        for (int i = 1; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).getAccessibleChildrenCount(jComponent);
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
        Accessible returnValue =
                ((FileChooserUI) uis.elementAt(0)).getAccessibleChild(jComponent, i2);
        for (int i = 1; i < uis.size(); i++) {
            ((FileChooserUI) uis.elementAt(i)).getAccessibleChild(jComponent, i2);
        }
        return returnValue;
    }
}
