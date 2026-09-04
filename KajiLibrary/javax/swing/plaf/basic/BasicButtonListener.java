package javax.swing.plaf.basic;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * El escucha que convierte mouse, foco y cambios de propiedad en cambios del modelo del boton.
 *
 * <p>Es la mitad "entrada" del aspecto basico: {@code BasicButtonUI} pinta lo que el modelo dice,
 * y este objeto escribe en el modelo lo que el mouse hace. Nunca pinta ni decide que se ve; solo
 * arma, aprieta, suelta y desarma, y el modelo hace el resto, incluida la accion.
 *
 * <h2>Lo que no esta</h2>
 *
 * <p>Las acciones por teclado —espacio aprieta, el mnemonico con Alt— viven en
 * {@code InputMap}/{@code ActionMap}, que no estan; {@link #installKeyboardActions},
 * {@link #uninstallKeyboardActions} y {@link #updateMnemonicBinding} no tienen donde
 * registrarlas, y {@code getInputMap} no esta porque no hay que devolver. El foco tampoco toca al
 * boton por omision del dialogo, porque no hay {@code JRootPane}.
 */
public class BasicButtonListener implements MouseListener, MouseMotionListener, FocusListener,
        ChangeListener, PropertyChangeListener {

    private long ultimaPresion = -1;
    private boolean descartarSoltado = false;

    public BasicButtonListener(AbstractButton b) {
    }

    /** Cambio una propiedad del boton: solo importa si dejo de rellenar su area. */
    public void propertyChange(PropertyChangeEvent e) {
        String propiedad = e.getPropertyName();
        if (AbstractButton.CONTENT_AREA_FILLED_CHANGED_PROPERTY.equals(propiedad)) {
            checkOpacity((AbstractButton) e.getSource());
        } else if (AbstractButton.MNEMONIC_CHANGED_PROPERTY.equals(propiedad)) {
            updateMnemonicBinding((AbstractButton) e.getSource());
        }
    }

    /** Un boton que rellena su area es opaco, y uno que no, no: las dos propiedades van juntas. */
    protected void checkOpacity(AbstractButton b) {
        b.setOpaque(b.isContentAreaFilled());
    }

    /** Nada que instalar; ver la nota de la clase. */
    public void installKeyboardActions(JComponent c) {
    }

    public void uninstallKeyboardActions(JComponent c) {
    }

    /** Nada que renovar; ver la nota de la clase. */
    public void updateMnemonicBinding(AbstractButton b) {
    }

    /** Cambio el modelo: el boton se repinta. */
    public void stateChanged(ChangeEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        b.repaint();
    }

    public void focusGained(FocusEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        b.repaint();
    }

    /** Perder el foco suelta y desarma: un boton sin foco no puede quedar a medio apretar. */
    public void focusLost(FocusEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        ButtonModel modelo = b.getModel();
        modelo.setPressed(false);
        modelo.setArmed(false);
        b.repaint();
    }

    public void mouseMoved(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    /**
     * Bajo el boton izquierdo: arma y aprieta, y pide el foco.
     *
     * <p>Dos presiones mas cercanas que el umbral del boton cuentan como una: la segunda se ignora,
     * y tambien el soltado que le sigue. Es la defensa contra el doble click accidental en un boton
     * que hace algo irreversible.
     */
    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            AbstractButton b = (AbstractButton) e.getSource();
            if (b.contains(e.getX(), e.getY())) {
                long umbral = b.getMultiClickThreshhold();
                long anterior = ultimaPresion;
                long ahora = e.getWhen();
                ultimaPresion = ahora;
                if (anterior != -1 && ahora - anterior < umbral) {
                    descartarSoltado = true;
                    return;
                }
                ButtonModel modelo = b.getModel();
                if (!modelo.isEnabled()) {
                    return;
                }
                if (!modelo.isArmed()) {
                    modelo.setArmed(true);
                }
                modelo.setPressed(true);
                if (!b.hasFocus() && b.isRequestFocusEnabled()) {
                    b.requestFocus();
                }
            }
        }
    }

    /** Solto el boton izquierdo: suelta y desarma; si seguia armado, el modelo dispara la accion. */
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (descartarSoltado) {
                descartarSoltado = false;
                return;
            }
            AbstractButton b = (AbstractButton) e.getSource();
            ButtonModel modelo = b.getModel();
            modelo.setPressed(false);
            modelo.setArmed(false);
        }
    }

    /** Entro el cursor: rollover si el boton lo muestra, y rearmar si venia apretado. */
    public void mouseEntered(MouseEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        ButtonModel modelo = b.getModel();
        if (b.isRolloverEnabled() && !SwingUtilities.isLeftMouseButton(e)) {
            modelo.setRollover(true);
        }
        if (modelo.isPressed()) {
            modelo.setArmed(true);
        }
    }

    /** Salio el cursor: se desarma; soltar afuera ya no dispara nada. */
    public void mouseExited(MouseEvent e) {
        AbstractButton b = (AbstractButton) e.getSource();
        ButtonModel modelo = b.getModel();
        if (b.isRolloverEnabled()) {
            modelo.setRollover(false);
        }
        modelo.setArmed(false);
    }
}
