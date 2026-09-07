package javax.swing.plaf.metal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollPaneUI;

/**
 * El panel de desplazamiento de Metal.
 *
 * <p>Lo unico que agrega es un escucha que mira cuando alguien <em>reemplaza</em> una de las dos
 * barras. Metal les pone a las barras del panel una propiedad -- {@code "JScrollBar.isFreeStanding"}
 * en {@code false} -- que le dice al aspecto de la barra que no se dibuje el borde de afuera,
 * porque el borde ya lo pone el panel. Una barra puesta despues no la tendria y se dibujaria con
 * un marco de mas justo contra el marco del panel.
 *
 * <p>Es un detalle de dos pixeles y es la clase entera. Vale la pena porque el sintoma -- una
 * doble linea en un solo lado, y solo si el programa cambio la barra -- es de los que nadie
 * encuentra mirando el codigo.
 */
public class MetalScrollPaneUI extends BasicScrollPaneUI {

    private PropertyChangeListener cambioDeBarra;

    public MetalScrollPaneUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new MetalScrollPaneUI();
    }

    public void installUI(JComponent c) {
        super.installUI(c);
    }

    public void uninstallUI(JComponent c) {
        super.uninstallUI(c);
    }

    public void installListeners(JScrollPane scrollPane) {
        super.installListeners(scrollPane);
        cambioDeBarra = createScrollBarSwapListener();
        scrollPane.addPropertyChangeListener(cambioDeBarra);
        libre(scrollPane.getHorizontalScrollBar());
        libre(scrollPane.getVerticalScrollBar());
    }

    public void uninstallListeners(JScrollPane scrollPane) {
        super.uninstallListeners((JComponent) scrollPane);
        if (cambioDeBarra != null) {
            scrollPane.removePropertyChangeListener(cambioDeBarra);
            cambioDeBarra = null;
        }
    }

    protected void uninstallListeners(JComponent c) {
        uninstallListeners((JScrollPane) c);
    }

    /** Le avisa a la barra que va pegada al panel y no suelta. */
    private static void libre(JScrollBar barra) {
        if (barra != null) {
            barra.putClientProperty("JScrollBar.isFreeStanding", Boolean.FALSE);
        }
    }

    protected PropertyChangeListener createScrollBarSwapListener() {
        return new CambioDeBarra();
    }

    /** Estatico no: necesita nada del panel, pero el JDK lo hace anonimo y da igual. */
    private static class CambioDeBarra implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent e) {
            String nombre = e.getPropertyName();
            if ("verticalScrollBar".equals(nombre) || "horizontalScrollBar".equals(nombre)) {
                if (e.getOldValue() instanceof JScrollBar) {
                    ((JScrollBar) e.getOldValue())
                            .putClientProperty("JScrollBar.isFreeStanding", null);
                }
                if (e.getNewValue() instanceof JScrollBar) {
                    ((JScrollBar) e.getNewValue())
                            .putClientProperty("JScrollBar.isFreeStanding", Boolean.FALSE);
                }
            }
        }
    }
}
