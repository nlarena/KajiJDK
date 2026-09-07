package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import java.awt.Dimension;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;

/**
 * El boton de Synth, y la raiz de otras tres clases.
 *
 * <p>De aca salen {@link SynthToggleButtonUI}, {@link SynthRadioButtonUI} y
 * {@link SynthCheckBoxUI}, en cadena, y cada una cambia una sola cosa: el prefijo del que leen sus
 * valores. Es lo contrario de lo que pasa en Metal, donde el boton y el conmutador no comparten
 * una linea porque heredan de distinto lado; aca la cadena esta bien puesta desde el principio.
 *
 * <h2>El estado es mas que encendido o apagado</h2>
 *
 * <p>Un boton es el componente donde mas se nota para que sirve {@link SynthContext}: al estado
 * general -- encendido, apagado, con el foco -- le suma lo que dice su modelo. Apretado, con el
 * cursor encima, elegido, y {@code DEFAULT} si es el boton por omision del dialogo. Cada
 * combinacion puede tener su propia imagen de fondo, y de eso vive el aspecto.
 *
 * <h2>Tres metodos para un icono</h2>
 *
 * <p>{@link #getIcon} es el que se dibuja: el del boton si lo tiene, y si no el del estilo.
 * {@link #getDefaultIcon} es el del estilo solo. {@link #getSizingIcon} es el que se usa para
 * <strong>medir</strong>, y no siempre es el mismo que se dibuja: si el icono cambia de tamano
 * entre estados, medir con el de ahora haria que el boton cambiara de tamano al pasarle el mouse
 * por encima.
 */
public class SynthButtonUI extends javax.swing.plaf.basic.BasicButtonUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthButtonUI();
    }

    public SynthContext getContext(JComponent c) {
        return getContext(c, estadoDelBoton(c));
    }

    /**
     * El contexto con ese estado.
     *
     * <p>La region sale del componente y no de una constante fija, y eso importa en las cadenas de
     * herencia: {@code SynthCheckBoxUI} hereda este metodo de {@code SynthButtonUI} y tiene que
     * contestar {@code CheckBox}, no {@code Button}. Medido.
     */
    private SynthContext getContext(JComponent c, int state) {
        Region r = SynthLookAndFeel.getRegion(c);
        return new SynthContext(c, (r != null) ? r : Region.BUTTON, style, state, true);
    }

    /** Le pide el estilo a la fabrica; revienta si no hay, y esta medido. */
    private void updateStyle(JComponent c) {
        style = SynthLookAndFeel.actualizar(getContext(c, SynthConstants.ENABLED));
    }

    /**
     * Dibuja el fondo y despues el contenido.
     *
     * <p>Synth separa las dos cosas: el fondo lo pinta el estilo -- que sabe en que estado esta el
     * componente -- y el contenido lo pinta el aspecto basico. Por eso {@code update} no es
     * {@code paint} con un relleno adelante, como en el basico, sino dos pasos distintos.
     */
    public void update(Graphics g, JComponent c) {
        SynthContext context = getContext(c);
        if (context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintButtonBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
        }
        paint(context, g);
    }

    public void paint(Graphics g, JComponent c) {
        paint(getContext(c), g);
    }

    protected void paint(SynthContext context, Graphics g) {
        super.paint(g, context.getComponent());
    }

    /** El borde lo dibuja el estilo, no un {@code Border}; ver {@link SynthUI}. */
    public void paintBorder(SynthContext context, Graphics g, int x, int y, int w, int h) {
        if (context != null && context.getStyle() != null) {
            context.getStyle().getPainter(context)
                    .paintButtonBorder(context, g, x, y, w, h);
        }
    }

    /** Cualquier cambio puede querer otro estilo; ver {@link SynthLookAndFeel#actualizar}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthButtonUI() {
    }

    protected void installDefaults(AbstractButton b) {
        updateStyle(b);
    }

    protected void uninstallDefaults(AbstractButton b) {
        style = null;
    }

    protected void installListeners(AbstractButton b) {
        super.installListeners(b);
        b.addPropertyChangeListener(this);
    }

    protected void uninstallListeners(AbstractButton b) {
        b.removePropertyChangeListener(this);
        super.uninstallListeners(b);
    }

    /** El estado general mas lo que diga el modelo; ver la nota de la clase. */
    private int estadoDelBoton(JComponent c) {
        if (!(c instanceof AbstractButton)) {
            return SynthLookAndFeel.estadoDe(c);
        }
        ButtonModel m = ((AbstractButton) c).getModel();
        int state = SynthConstants.ENABLED;
        // Apretado no se SUMA a encendido: lo reemplaza. Un boton apretado no esta ademas
        // encendido; esta apretado, que es otro dibujo. Medido: da 4 y no 5.
        if (m.isPressed()) {
            state = m.isArmed() ? SynthConstants.PRESSED : SynthConstants.MOUSE_OVER;
        }
        if (m.isRollover()) {
            state |= SynthConstants.MOUSE_OVER;
        }
        if (m.isSelected()) {
            state |= SynthConstants.SELECTED;
        }
        // Y apagado pisa todo lo anterior, apretado incluido. Tambien medido.
        if (!c.isEnabled()) {
            state = SynthConstants.DISABLED;
        }
        if (c.isFocusOwner()) {
            state |= SynthConstants.FOCUSED;
        }
        if (c instanceof javax.swing.JButton && ((javax.swing.JButton) c).isDefaultButton()) {
            state |= SynthConstants.DEFAULT;
        }
        return state;
    }

    /** El del boton, o el del estilo si el boton no tiene. */
    protected Icon getIcon(AbstractButton b) {
        Icon i = b.getIcon();
        return (i != null) ? i : getDefaultIcon(b);
    }

    /** El del estilo; nulo sin estilo. */
    protected Icon getDefaultIcon(AbstractButton b) {
        SynthContext context = getContext(b);
        if (context.getStyle() == null) {
            return null;
        }
        return context.getStyle().getIcon(context, getPropertyPrefix() + "icon");
    }

    /** El de medir, que no siempre es el de dibujar; ver la nota de la clase. */
    protected Icon getSizingIcon(AbstractButton b) {
        Icon i = getDefaultIcon(b);
        return (i != null) ? i : getIcon(b);
    }

    public int getBaseline(JComponent c, int width, int height) {
        return super.getBaseline(c, width, height);
    }

    public Dimension getPreferredSize(JComponent c) {
        return super.getPreferredSize(c);
    }

    public Dimension getMinimumSize(JComponent c) {
        return super.getMinimumSize(c);
    }

    public Dimension getMaximumSize(JComponent c) {
        return super.getMaximumSize(c);
    }
}
