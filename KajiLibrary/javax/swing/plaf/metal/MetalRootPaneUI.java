package javax.swing.plaf.metal;

import java.beans.PropertyChangeEvent;

import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicRootPaneUI;

/**
 * El panel raiz de Metal, que es el que sabe decorar una ventana.
 *
 * <p>Metal es el unico aspecto de Java que puede dibujar el marco y la barra de titulo de una
 * ventana <em>del lado de Java</em>, en vez de dejarselos al sistema. Eso es lo que hace
 * {@code JFrame.setDefaultLookAndFeelDecorated(true)}, y lo que lo implementa es esta clase: mira
 * {@link JRootPane#getWindowDecorationStyle} y, cuando no es {@code NONE}, pone una barra de
 * titulo propia como hijo del panel raiz y le cambia el borde.
 *
 * <p>El estilo puede cambiar en caliente, y por eso {@link #propertyChange} escucha
 * {@code "windowDecorationStyle"}: un dialogo que pasa de normal a de error tiene que cambiar de
 * marco sin volver a crearse.
 *
 * <h2>Lo que queda dicho y no tapado</h2>
 *
 * <p>La decoracion no se arma. Necesita una ventana de verdad -- para sacarle el borde al sistema,
 * arrastrarla y cambiarle el tamano -- y esta VM no tiene ventanas. Lo que si esta es todo lo que
 * se puede contestar sin una: el UI se instala, escucha el cambio de estilo y se desinstala.
 */
public class MetalRootPaneUI extends BasicRootPaneUI {

    public MetalRootPaneUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalRootPaneUI();
    }

    public void installUI(JComponent c) {
        super.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    public void propertyChange(PropertyChangeEvent e) {
        super.propertyChange(e);
    }
}
