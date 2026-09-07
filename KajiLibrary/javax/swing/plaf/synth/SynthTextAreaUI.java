package javax.swing.plaf.synth;

import java.awt.Graphics;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.JTextComponent;

/**
 * El area de texto de Synth.
 *
 * <p>No hereda del campo de texto -- son ramas distintas ya en el basico -- pero repite los mismos
 * cuatro metodos. Es de los pocos lugares donde el paquete duplica codigo, y por la misma razon
 * que en Metal: la jerarquia del aspecto basico manda.
 */
public class SynthTextAreaUI extends javax.swing.plaf.basic.BasicTextAreaUI implements SynthUI, PropertyChangeListener {

    private SynthStyle style;

    public static ComponentUI createUI(JComponent c) {
        return new SynthTextAreaUI();
    }

    public SynthContext getContext(JComponent c) {
        return getContext(c, SynthLookAndFeel.estadoDe(c));
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
        return new SynthContext(c, (r != null) ? r : Region.TEXT_AREA, style, state, true);
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
                    .paintTextAreaBackground(context, g, 0, 0, c.getWidth(), c.getHeight());
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
                    .paintTextAreaBorder(context, g, x, y, w, h);
        }
    }

    /** Cualquier cambio puede querer otro estilo; ver {@link SynthLookAndFeel#actualizar}. */
    public void propertyChange(PropertyChangeEvent e) {
        Object o = e.getSource();
        if (o instanceof JComponent) {
            updateStyle((JComponent) o);
        }
    }

    public SynthTextAreaUI() {
    }

    protected String getPropertyPrefix() {
        return "TextArea.";
    }

    /**
     * Instala los valores del estilo.
     *
     * <p>El componente se pide con {@code getComponent()} y no se guarda: es el mismo que el
     * basico ya tiene, y tenerlo dos veces seria una copia que se puede desincronizar.
     */
    protected void installDefaults() {
        super.installDefaults();
        JTextComponent c = getComponent();
        if (c != null) {
            updateStyle(c);
            c.addPropertyChangeListener(this);
        }
    }

    protected void uninstallDefaults() {
        JTextComponent c = getComponent();
        if (c != null) {
            c.removePropertyChangeListener(this);
        }
        style = null;
        super.uninstallDefaults();
    }
}
