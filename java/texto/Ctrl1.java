import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoundedRangeModel;
import javax.swing.ButtonGroup;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.JToolTip;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * El deslizante, la barra de progreso, los items de menu con estado y la barra de herramientas,
 * contra el JDK.
 *
 * <p>Lo que se compara no es tanto el estado como los <em>avisos</em>: estos cuatro controles
 * reenvian los cambios de su modelo como cambios propios, y un aviso que no llega -- o que llega
 * con el origen equivocado -- deja la pantalla mostrando lo viejo sin que nada falle.
 *
 * <p>Los tamanos preferidos quedan afuera: los decide el aspecto instalado, que esta biblioteca no
 * tiene.
 */
public class Ctrl1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Anota cada aviso de cambio y de que objeto vino. */
    static class Espia implements ChangeListener {

        private final StringBuilder log = new StringBuilder();
        private final Object esperado;

        Espia(Object esperado) {
            this.esperado = esperado;
        }

        public void stateChanged(ChangeEvent e) {
            log.append(e.getSource() == esperado ? " ok" : " ORIGEN-RARO");
        }

        String vaciar() {
            String s = log.toString();
            log.setLength(0);
            return s;
        }
    }

    /** Anota los cambios de propiedad, salvo los que dispara el aspecto del JDK. */
    static class EspiaProp implements PropertyChangeListener {

        private final StringBuilder log = new StringBuilder();

        public void propertyChange(PropertyChangeEvent e) {
            String n = e.getPropertyName();
            if ("ancestor".equals(n) || "UI".equals(n) || "labelTable".equals(n)) {
                return;
            }
            log.append(" ").append(n);
        }

        String vaciar() {
            String s = log.toString();
            log.setLength(0);
            return s;
        }
    }

    static String rango(BoundedRangeModel m) {
        return m.getMinimum() + "<=" + m.getValue() + "(+" + m.getExtent() + ")<="
                + m.getMaximum() + " ajustando=" + m.getValueIsAdjusting();
    }

    static void deslizante() {
        linea("--- el deslizante ---");
        JSlider s = new JSlider();
        linea("por omision " + rango(s.getModel()) + " orientacion=" + s.getOrientation()
                + " horizontal=" + SwingConstants.HORIZONTAL);
        linea("clase de aspecto=" + s.getUIClassID() + " invertido=" + s.getInverted()
                + " riel=" + s.getPaintTrack() + " marcas=" + s.getPaintTicks()
                + " etiquetas=" + s.getPaintLabels());
        linea("marcas grandes=" + s.getMajorTickSpacing() + " chicas=" + s.getMinorTickSpacing()
                + " salta=" + s.getSnapToTicks() + " tabla=" + s.getLabelTable());

        Espia espia = new Espia(s);
        EspiaProp prop = new EspiaProp();
        s.addChangeListener(espia);
        s.addPropertyChangeListener(prop);
        prop.vaciar();

        s.setValue(30);
        linea("en 30 " + rango(s.getModel()) + " |" + espia.vaciar() + " |" + prop.vaciar());
        s.setValue(30);
        linea("repetido |" + espia.vaciar() + " |" + prop.vaciar());
        // Pasarse recorta contra el maximo menos la extension.
        s.setValue(500);
        linea("en 500 " + rango(s.getModel()) + " |" + espia.vaciar() + " |" + prop.vaciar());
        s.setValue(-500);
        linea("en -500 " + rango(s.getModel()) + " |" + espia.vaciar() + " |" + prop.vaciar());

        s.setExtent(10);
        linea("extension 10 " + rango(s.getModel()) + " |" + espia.vaciar()
                + " |" + prop.vaciar());
        s.setValue(95);
        linea("y en 95 " + rango(s.getModel()) + " |" + espia.vaciar() + " |" + prop.vaciar());
        s.setExtent(0);
        espia.vaciar();
        prop.vaciar();

        s.setMinimum(20);
        linea("piso 20 " + rango(s.getModel()) + " |" + espia.vaciar() + " |" + prop.vaciar());
        s.setMaximum(60);
        linea("techo 60 " + rango(s.getModel()) + " |" + espia.vaciar() + " |" + prop.vaciar());

        s.setValueIsAdjusting(true);
        linea("ajustando " + rango(s.getModel()) + " |" + espia.vaciar() + " |" + prop.vaciar());
        s.setValueIsAdjusting(false);
        linea("listo |" + espia.vaciar() + " |" + prop.vaciar());

        s.setOrientation(SwingConstants.VERTICAL);
        linea("vertical=" + s.getOrientation() + " |" + prop.vaciar());
        try {
            s.setOrientation(9);
            linea("orientacion 9 aceptada");
        } catch (IllegalArgumentException e) {
            linea("orientacion 9 rechazada: " + e.getMessage());
        }
        s.setInverted(true);
        linea("invertido=" + s.getInverted() + " |" + prop.vaciar());
        s.setPaintTicks(true);
        s.setMajorTickSpacing(10);
        s.setMinorTickSpacing(5);
        linea("marcas " + s.getMajorTickSpacing() + "/" + s.getMinorTickSpacing()
                + " |" + prop.vaciar());
        s.setSnapToTicks(true);
        linea("salta=" + s.getSnapToTicks() + " |" + prop.vaciar());

        // Prender las etiquetas con marcas grandes definidas las arma solo.
        s.setPaintLabels(true);
        Dictionary<?, ?> tabla = s.getLabelTable();
        linea("etiquetas armadas=" + (tabla != null) + " cuantas=" + cuantas(tabla)
                + " |" + prop.vaciar());
        linea("claves=" + claves(tabla));

        Hashtable<Integer, JComponent> propias = s.createStandardLabels(20, 20);
        linea("propias=" + claves(propias));
        try {
            s.createStandardLabels(0);
            linea("paso 0 aceptado");
        } catch (IllegalArgumentException e) {
            linea("paso 0 rechazado: " + e.getMessage());
        }
        try {
            s.createStandardLabels(5, 1000);
            linea("arranque fuera de rango aceptado");
        } catch (IllegalArgumentException e) {
            linea("arranque fuera de rango rechazado: " + e.getMessage());
        }

        // Cambiar el modelo muda el oyente: el modelo viejo ya no despierta al control.
        //
        // El salto a la marca se apaga antes: en el JDK lo hace el aspecto instalado --al recalcular
        // la posicion de la perilla corrige el valor-- y aca no hay aspecto. Con el prendido se
        // estaria comparando el aspecto y no el control.
        s.setSnapToTicks(false);
        BoundedRangeModel viejo = s.getModel();
        DefaultBoundedRangeModel nuevo = new DefaultBoundedRangeModel(5, 0, 0, 10);
        s.setModel(nuevo);
        linea("modelo nuevo " + rango(s.getModel()) + " |" + espia.vaciar()
                + " |" + prop.vaciar());
        viejo.setValue(7);
        linea("el viejo ya no avisa |" + espia.vaciar());
        nuevo.setValue(8);
        linea("el nuevo si " + rango(s.getModel()) + " |" + espia.vaciar());
        // `setModel(null)` no entra en la comparacion: el JDK lo acepta --no valida-- y su aspecto
        // instalado revienta enseguida al escuchar el cambio. Lo que se comparara seria el aspecto.

        try {
            new JSlider(9);
            linea("constructor con orientacion 9 aceptado");
        } catch (IllegalArgumentException e) {
            linea("constructor con orientacion 9 rechazado");
        }
        JSlider r = new JSlider(10, 20);
        linea("de 10 a 20 " + rango(r.getModel()));
        JSlider q = new JSlider(new DefaultBoundedRangeModel(3, 1, 0, 9));
        linea("con modelo " + rango(q.getModel()) + " orientacion=" + q.getOrientation());
    }

    static int cuantas(Dictionary<?, ?> d) {
        if (d == null) {
            return -1;
        }
        return d.size();
    }

    /** Las claves ordenadas, que es lo unico comparable de una tabla dispersa. */
    static String claves(Dictionary<?, ?> d) {
        if (d == null) {
            return "-";
        }
        int[] k = new int[d.size()];
        int i = 0;
        Enumeration<?> e = d.keys();
        while (e.hasMoreElements()) {
            k[i] = ((Integer) e.nextElement()).intValue();
            i = i + 1;
        }
        java.util.Arrays.sort(k);
        return java.util.Arrays.toString(k);
    }

    static void progreso() {
        linea("--- la barra de progreso ---");
        JProgressBar b = new JProgressBar();
        linea("por omision " + rango(b.getModel()) + " orientacion=" + b.getOrientation());
        linea("clase de aspecto=" + b.getUIClassID() + " borde=" + b.isBorderPainted()
                + " texto=" + b.isStringPainted() + " indeterminada=" + b.isIndeterminate());
        linea("proporcion=" + b.getPercentComplete() + " texto=" + b.getString());

        Espia espia = new Espia(b);
        EspiaProp prop = new EspiaProp();
        b.addChangeListener(espia);
        b.addPropertyChangeListener(prop);
        prop.vaciar();

        b.setValue(25);
        linea("en 25 proporcion=" + b.getPercentComplete() + " texto=" + b.getString()
                + " |" + espia.vaciar() + " |" + prop.vaciar());
        b.setValue(100);
        linea("lleno proporcion=" + b.getPercentComplete() + " texto=" + b.getString()
                + " |" + espia.vaciar());
        // Un rango vacio no divide por cero: da cero.
        b.setMinimum(50);
        b.setMaximum(50);
        linea("rango vacio " + rango(b.getModel()) + " proporcion=" + b.getPercentComplete()
                + " |" + espia.vaciar() + " |" + prop.vaciar());
        b.setMinimum(0);
        b.setMaximum(200);
        b.setValue(50);
        linea("de 0 a 200 en 50 proporcion=" + b.getPercentComplete() + " texto=" + b.getString());
        espia.vaciar();
        prop.vaciar();

        b.setString("cargando");
        linea("texto propio=" + b.getString() + " |" + prop.vaciar());
        b.setString(null);
        linea("de vuelta al porcentaje=" + b.getString() + " |" + prop.vaciar());
        b.setStringPainted(true);
        linea("se dibuja=" + b.isStringPainted() + " |" + prop.vaciar());
        b.setBorderPainted(false);
        linea("sin borde=" + b.isBorderPainted() + " |" + prop.vaciar());
        b.setIndeterminate(true);
        linea("indeterminada=" + b.isIndeterminate() + " |" + prop.vaciar());
        b.setOrientation(SwingConstants.VERTICAL);
        linea("vertical=" + b.getOrientation() + " |" + prop.vaciar());
        try {
            b.setOrientation(9);
            linea("orientacion 9 aceptada");
        } catch (IllegalArgumentException e) {
            linea("orientacion 9 rechazada: " + e.getMessage());
        }

        JProgressBar c = new JProgressBar(10, 20);
        linea("de 10 a 20 " + rango(c.getModel()) + " proporcion=" + c.getPercentComplete());
        JProgressBar d = new JProgressBar(new DefaultBoundedRangeModel(5, 0, 0, 10));
        linea("con modelo " + rango(d.getModel()) + " proporcion=" + d.getPercentComplete());
    }

    static void items() {
        linea("--- items de menu con estado ---");
        JCheckBoxMenuItem t = new JCheckBoxMenuItem("tilde");
        linea("tilde texto=" + t.getText() + " estado=" + t.getState()
                + " elegido=" + t.isSelected() + " aspecto=" + t.getUIClassID());
        linea("objetos elegidos=" + (t.getSelectedObjects() == null ? "-"
                : java.util.Arrays.toString(t.getSelectedObjects())));
        t.setState(true);
        linea("prendido estado=" + t.getState() + " elegido=" + t.isSelected());
        linea("objetos elegidos=" + java.util.Arrays.toString(t.getSelectedObjects()));
        // Los dos nombres escriben el mismo estado.
        t.setSelected(false);
        linea("apagado por setSelected estado=" + t.getState());
        JCheckBoxMenuItem t2 = new JCheckBoxMenuItem("otro", true);
        linea("nacido prendido=" + t2.getState() + " enfocable=" + t2.isFocusable());

        JRadioButtonMenuItem a = new JRadioButtonMenuItem("a", true);
        JRadioButtonMenuItem bb = new JRadioButtonMenuItem("b");
        linea("radio a=" + a.isSelected() + " b=" + bb.isSelected()
                + " aspecto=" + a.getUIClassID());
        // Sin grupo, prender uno no apaga al otro.
        bb.setSelected(true);
        linea("sin grupo a=" + a.isSelected() + " b=" + bb.isSelected());
        a.setSelected(false);
        bb.setSelected(false);
        ButtonGroup g = new ButtonGroup();
        g.add(a);
        g.add(bb);
        a.setSelected(true);
        linea("con grupo, prendo a: a=" + a.isSelected() + " b=" + bb.isSelected());
        bb.setSelected(true);
        linea("prendo b: a=" + a.isSelected() + " b=" + bb.isSelected()
                + " cuantos=" + g.getButtonCount());
    }

    static void herramientas() {
        linea("--- la barra de herramientas ---");
        JToolBar b = new JToolBar();
        linea("por omision nombre=" + b.getName() + " orientacion=" + b.getOrientation()
                + " flotante=" + b.isFloatable() + " borde=" + b.isBorderPainted()
                + " aspecto=" + b.getUIClassID() + " rollover=" + b.isRollover());
        linea("margen=" + b.getMargin());

        EspiaProp prop = new EspiaProp();
        b.addPropertyChangeListener(prop);
        prop.vaciar();

        JButton uno = new JButton("uno");
        JButton dos = new JButton("dos");
        b.add(uno);
        b.add(dos);
        linea("dos botones cuantos=" + b.getComponentCount()
                + " indice de dos=" + b.getComponentIndex(dos)
                + " en 0=" + ((JButton) b.getComponentAtIndex(0)).getText());
        linea("fuera de rango=" + b.getComponentAtIndex(9)
                + " ajeno=" + b.getComponentIndex(new JButton("x")));

        b.addSeparator();
        JToolBar.Separator sep = (JToolBar.Separator) b.getComponentAtIndex(2);
        // El tamano del separador sin medida propia lo pone el aspecto, asi que no se compara.
        linea("separador aspecto=" + sep.getUIClassID()
                + " orientacion=" + sep.getOrientation()
                + " vertical=" + JSeparator.VERTICAL);
        b.addSeparator(new Dimension(7, 9));
        JToolBar.Separator sep2 = (JToolBar.Separator) b.getComponentAtIndex(3);
        linea("separador propio=" + sep2.getSeparatorSize() + " preferido="
                + sep2.getPreferredSize() + " minimo=" + sep2.getMinimumSize()
                + " maximo=" + sep2.getMaximumSize());

        // Girar la barra gira los separadores que se agreguen despues.
        b.setOrientation(SwingConstants.VERTICAL);
        b.addSeparator(new Dimension(3, 3));
        JToolBar.Separator sep3 = (JToolBar.Separator) b.getComponentAtIndex(4);
        linea("vertical=" + b.getOrientation() + " separador nuevo orientacion="
                + sep3.getOrientation() + " horizontal=" + JSeparator.HORIZONTAL);
        prop.vaciar();

        // Agregar una accion arma el boton y lo devuelve.
        Action accion = new AbstractAction("guardar") {
            public void actionPerformed(java.awt.event.ActionEvent e) {
            }
        };
        accion.putValue(Action.SHORT_DESCRIPTION, "guarda todo");
        JButton hecho = b.add(accion);
        linea("boton de accion texto=" + hecho.getText() + " ayuda=" + hecho.getToolTipText()
                + " habilitado=" + hecho.isEnabled()
                + " texto horizontal=" + hecho.getHorizontalTextPosition()
                + " vertical=" + hecho.getVerticalTextPosition());
        linea("quedo en la barra=" + (b.getComponentIndex(hecho) >= 0));

        b.setFloatable(false);
        linea("no flotante=" + b.isFloatable() + " |" + prop.vaciar());
        b.setBorderPainted(false);
        linea("sin borde=" + b.isBorderPainted() + " |" + prop.vaciar());
        b.setRollover(true);
        linea("rollover=" + b.isRollover());
        b.setMargin(new java.awt.Insets(1, 2, 3, 4));
        linea("margen=" + b.getMargin() + " |" + prop.vaciar());
        try {
            b.setOrientation(9);
            linea("orientacion 9 aceptada");
        } catch (IllegalArgumentException e) {
            linea("orientacion 9 rechazada: " + e.getMessage());
        }
        JToolBar con = new JToolBar("mi barra", SwingConstants.VERTICAL);
        linea("con nombre=" + con.getName() + " orientacion=" + con.getOrientation());
    }

    static void ayuda() {
        linea("--- el cartel de ayuda ---");
        JLabel etiqueta = new JLabel("hola");
        etiqueta.setToolTipText("soy una etiqueta");
        JToolTip t = etiqueta.createToolTip();
        linea("componente=" + (t.getComponent() == etiqueta) + " texto=" + t.getTipText()
                + " aspecto=" + t.getUIClassID() + " opaco=" + t.isOpaque());
        t.setTipText("otro");
        linea("texto puesto=" + t.getTipText());
        JToolTip solo = new JToolTip();
        linea("suelto componente=" + solo.getComponent() + " texto=" + solo.getTipText());
    }

    public static int run() {
        deslizante();
        progreso();
        items();
        herramientas();
        ayuda();
        return 0;
    }
}
