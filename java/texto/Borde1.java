import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.OverlayLayout;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

/**
 * La fabrica de bordes, los editores de celda y el acomodador de superposicion, contra el JDK.
 *
 * <p>De la fabrica lo que importa es <em>cual comparte y cual no</em>: eso se compara por identidad,
 * que es la unica manera de verlo. De los editores, cuando empiezan a editar y que avisan. Del
 * acomodador, las cuentas de alineacion, que es todo lo que hace.
 */
public class Borde1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String ins(Insets i) {
        return i.top + "," + i.left + "," + i.bottom + "," + i.right;
    }

    static String clase(Object o) {
        if (o == null) {
            return "-";
        }
        String c = o.getClass().getName();
        int p = c.lastIndexOf('.');
        return c.substring(p + 1);
    }

    static void bordes() {
        linea("--- la fabrica de bordes ---");
        JLabel comp = new JLabel();

        // Los que no llevan parametros se comparten; los que si, no.
        linea("vacio compartido=" + (BorderFactory.createEmptyBorder()
                == BorderFactory.createEmptyBorder()));
        linea("surco compartido=" + (BorderFactory.createEtchedBorder()
                == BorderFactory.createEtchedBorder()));
        linea("relieve arriba compartido=" + (BorderFactory.createRaisedBevelBorder()
                == BorderFactory.createRaisedBevelBorder()));
        linea("relieve abajo compartido=" + (BorderFactory.createLoweredBevelBorder()
                == BorderFactory.createLoweredBevelBorder()));
        linea("surco hundido por tipo es el mismo="
                + (BorderFactory.createEtchedBorder(EtchedBorder.LOWERED)
                        == BorderFactory.createEtchedBorder()));
        linea("surco alzado por tipo es otro="
                + (BorderFactory.createEtchedBorder(EtchedBorder.RAISED)
                        == BorderFactory.createEtchedBorder()));
        linea("relieve por tipo es el mismo="
                + (BorderFactory.createBevelBorder(BevelBorder.RAISED)
                        == BorderFactory.createRaisedBevelBorder()));
        linea("linea NO compartida=" + (BorderFactory.createLineBorder(Color.RED)
                == BorderFactory.createLineBorder(Color.RED)));
        linea("vacio con medidas NO compartido="
                + (BorderFactory.createEmptyBorder(1, 1, 1, 1)
                        == BorderFactory.createEmptyBorder(1, 1, 1, 1)));

        Border linea1 = BorderFactory.createLineBorder(Color.RED);
        linea("linea de 1 clase=" + clase(linea1) + " insets=" + ins(
                linea1.getBorderInsets(comp)) + " opaco=" + linea1.isBorderOpaque());
        Border linea3 = BorderFactory.createLineBorder(Color.BLUE, 3);
        linea("linea de 3 insets=" + ins(linea3.getBorderInsets(comp)));
        Border redonda = BorderFactory.createLineBorder(Color.BLUE, 3, true);
        linea("linea redondeada insets=" + ins(redonda.getBorderInsets(comp))
                + " opaco=" + redonda.isBorderOpaque());

        Border vacio = BorderFactory.createEmptyBorder(1, 2, 3, 4);
        linea("vacio clase=" + clase(vacio) + " insets=" + ins(vacio.getBorderInsets(comp))
                + " opaco=" + vacio.isBorderOpaque());

        Border surco = BorderFactory.createEtchedBorder();
        linea("surco clase=" + clase(surco) + " insets=" + ins(surco.getBorderInsets(comp))
                + " opaco=" + surco.isBorderOpaque());
        Border relieve = BorderFactory.createRaisedBevelBorder();
        linea("relieve clase=" + clase(relieve) + " insets=" + ins(
                relieve.getBorderInsets(comp)) + " opaco=" + relieve.isBorderOpaque());
        Border suave = BorderFactory.createRaisedSoftBevelBorder();
        linea("relieve suave clase=" + clase(suave) + " insets=" + ins(
                suave.getBorderInsets(comp)));
        linea("relieve suave por tipo raro=" + BorderFactory.createSoftBevelBorder(9));
        linea("relieve por tipo raro=" + BorderFactory.createBevelBorder(9));

        MatteBorder marco = BorderFactory.createMatteBorder(1, 2, 3, 4, Color.GREEN);
        linea("marco clase=" + clase(marco) + " insets=" + ins(marco.getBorderInsets(comp))
                + " color=" + marco.getMatteColor() + " opaco=" + marco.isBorderOpaque());

        CompoundBorder doble = BorderFactory.createCompoundBorder(vacio, linea3);
        linea("compuesto insets=" + ins(doble.getBorderInsets(comp))
                + " afuera=" + clase(doble.getOutsideBorder())
                + " adentro=" + clase(doble.getInsideBorder()));
        CompoundBorder vacios = BorderFactory.createCompoundBorder();
        linea("compuesto vacio afuera=" + vacios.getOutsideBorder()
                + " adentro=" + vacios.getInsideBorder());

        TitledBorder titulo = BorderFactory.createTitledBorder("hola");
        // El borde de adentro de un titulo sin borde propio lo pone el aspecto instalado, asi que
        // solo se compara que haya uno.
        linea("titulo=" + titulo.getTitle() + " justificacion=" + titulo.getTitleJustification()
                + " posicion=" + titulo.getTitlePosition()
                + " tiene borde=" + (titulo.getBorder() != null));
        TitledBorder conBorde = BorderFactory.createTitledBorder(linea3, "che",
                TitledBorder.RIGHT, TitledBorder.BOTTOM, new Font("Dialog", Font.BOLD, 12),
                Color.RED);
        linea("titulo completo=" + conBorde.getTitle() + " justificacion="
                + conBorde.getTitleJustification() + " posicion=" + conBorde.getTitlePosition()
                + " color=" + conBorde.getTitleColor() + " fuente=" + conBorde.getTitleFont());

        Border trazo = BorderFactory.createStrokeBorder(new BasicStroke(2.0f));
        linea("trazo clase=" + clase(trazo) + " insets=" + ins(trazo.getBorderInsets(comp))
                + " opaco=" + trazo.isBorderOpaque());
        Border guiones = BorderFactory.createDashedBorder(Color.RED);
        linea("guiones clase=" + clase(guiones) + " insets=" + ins(
                guiones.getBorderInsets(comp)));
        Border guiones2 = BorderFactory.createDashedBorder(Color.RED, 4.0f, 2.0f);
        linea("guiones 4/2 insets=" + ins(guiones2.getBorderInsets(comp)));
        Border guiones3 = BorderFactory.createDashedBorder(Color.RED, 3.0f, 4.0f, 2.0f, true);
        linea("guiones gruesos insets=" + ins(guiones3.getBorderInsets(comp)));
        try {
            BorderFactory.createStrokeBorder(null);
            linea("trazo nulo aceptado");
        } catch (NullPointerException e) {
            linea("trazo nulo rechazado");
        }
        try {
            BorderFactory.createDashedBorder(Color.RED, 0.0f, 1.0f, 1.0f, false);
            linea("grosor cero aceptado");
        } catch (IllegalArgumentException e) {
            linea("grosor cero rechazado");
        }
    }

    /** Anota los avisos del editor. */
    static class Espia implements CellEditorListener {

        private final StringBuilder log = new StringBuilder();

        public void editingStopped(ChangeEvent e) {
            log.append(" termino");
        }

        public void editingCanceled(ChangeEvent e) {
            log.append(" cancelo");
        }

        String vaciar() {
            String s = log.toString();
            log.setLength(0);
            return s;
        }
    }

    /** Un clic con esa cantidad de golpes sobre ese componente. */
    static MouseEvent clic(Component c, int golpes) {
        return new MouseEvent(c, MouseEvent.MOUSE_PRESSED, 0L, 0, 1, 1, golpes, false);
    }

    static void editores() {
        linea("--- los editores de celda ---");
        JTextField campo = new JTextField();
        DefaultCellEditor deTexto = new DefaultCellEditor(campo);
        linea("de texto componente=" + clase(deTexto.getComponent())
                + " clics=" + deTexto.getClickCountToStart()
                + " valor=" + deTexto.getCellEditorValue());
        Espia espia = new Espia();
        deTexto.addCellEditorListener(espia);
        linea("oyentes=" + deTexto.getCellEditorListeners().length);

        // Dos clics para un campo de texto, uno no alcanza.
        linea("un clic edita=" + deTexto.isCellEditable(clic(campo, 1))
                + " dos clics=" + deTexto.isCellEditable(clic(campo, 2))
                + " tres=" + deTexto.isCellEditable(clic(campo, 3)));
        linea("sin evento edita=" + deTexto.isCellEditable(null)
                + " elige celda=" + deTexto.shouldSelectCell(null));

        campo.setText("escrito");
        linea("valor=" + deTexto.getCellEditorValue());
        linea("termina=" + deTexto.stopCellEditing() + " |" + espia.vaciar());
        deTexto.cancelCellEditing();
        linea("cancela |" + espia.vaciar());
        // Apretar Enter en el campo tambien termina la edicion.
        campo.postActionEvent();
        linea("enter |" + espia.vaciar());
        deTexto.setClickCountToStart(1);
        linea("con un clic=" + deTexto.isCellEditable(clic(campo, 1)));
        deTexto.removeCellEditorListener(espia);
        linea("oyentes=" + deTexto.getCellEditorListeners().length);

        JCheckBox tilde = new JCheckBox();
        DefaultCellEditor deTilde = new DefaultCellEditor(tilde);
        Espia espia2 = new Espia();
        deTilde.addCellEditorListener(espia2);
        linea("de tilde componente=" + clase(deTilde.getComponent())
                + " clics=" + deTilde.getClickCountToStart()
                + " pide foco=" + tilde.isRequestFocusEnabled());
        linea("valor inicial=" + deTilde.getCellEditorValue());
        tilde.setSelected(true);
        linea("prendido valor=" + deTilde.getCellEditorValue());
        linea("un clic edita=" + deTilde.isCellEditable(clic(tilde, 1)));
        linea("termina=" + deTilde.stopCellEditing() + " |" + espia2.vaciar());

        JComboBox<String> lista = new JComboBox<String>(new String[] {"a", "b", "c"});
        DefaultCellEditor deLista = new DefaultCellEditor(lista);
        Espia espia3 = new Espia();
        deLista.addCellEditorListener(espia3);
        linea("de lista componente=" + clase(deLista.getComponent())
                + " clics=" + deLista.getClickCountToStart()
                + " marcada=" + lista.getClientProperty("JComboBox.isTableCellEditor"));
        linea("valor inicial=" + deLista.getCellEditorValue());
        lista.setSelectedItem("b");
        linea("elegido b valor=" + deLista.getCellEditorValue() + " |" + espia3.vaciar());
        linea("termina=" + deLista.stopCellEditing() + " |" + espia3.vaciar());
        linea("elige celda con clic=" + deLista.shouldSelectCell(clic(lista, 1)));

        // El delegado carga el valor en el componente.
        //
        // La tabla va en nulo, que alcanza para el campo de texto. Para el tilde no: se le pide a la
        // tabla el dibujante de la celda -- ver la nota de getTableCellEditorComponent -- y sin
        // tabla revienta, tanto aca como en el JDK. Al tilde se lo carga por el camino del arbol,
        // que ademas ejercita que acepte un texto en vez de un booleano.
        deTexto.getTableCellEditorComponent(null, Integer.valueOf(7), false, 0, 0);
        linea("texto cargado con 7=" + deTexto.getCellEditorValue());
        deTexto.getTableCellEditorComponent(null, null, false, 0, 0);
        linea("texto cargado con nulo=[" + deTexto.getCellEditorValue() + "]");

        javax.swing.JTree arbol = new javax.swing.JTree();
        deTilde.getTreeCellEditorComponent(arbol, Boolean.TRUE, false, false, true, 0);
        linea("tilde cargado con TRUE valor=" + deTilde.getCellEditorValue());
        deTilde.getTreeCellEditorComponent(arbol, Boolean.FALSE, false, false, true, 0);
        linea("tilde cargado con FALSE valor=" + deTilde.getCellEditorValue());
        deTilde.getTreeCellEditorComponent(arbol, "otra cosa", false, false, true, 0);
        linea("tilde cargado con texto raro valor=" + deTilde.getCellEditorValue());
        deTexto.getTreeCellEditorComponent(arbol, "del arbol", false, false, true, 0);
        linea("texto desde el arbol=" + deTexto.getCellEditorValue());
    }

    /** Un hijo de medida y alineacion fijas. */
    static JPanel hijo(int w, int h, float ax, float ay) {
        JPanel p = new JPanel();
        p.setMinimumSize(new Dimension(w, h));
        p.setPreferredSize(new Dimension(w, h));
        p.setMaximumSize(new Dimension(w, h));
        p.setAlignmentX(ax);
        p.setAlignmentY(ay);
        return p;
    }

    static String rect(Rectangle r) {
        return r.x + "," + r.y + "," + r.width + "," + r.height;
    }

    static void superponer() {
        linea("--- el acomodador de superposicion ---");
        JPanel caja = new JPanel();
        OverlayLayout ol = new OverlayLayout(caja);
        caja.setLayout(ol);
        linea("destino es la caja=" + (ol.getTarget() == caja));

        JPanel grande = hijo(100, 40, 0.5f, 0.5f);
        JPanel chico = hijo(40, 20, 0.5f, 0.5f);
        caja.add(grande);
        caja.add(chico);

        linea("preferido=" + ol.preferredLayoutSize(caja));
        linea("minimo=" + ol.minimumLayoutSize(caja));
        linea("maximo=" + ol.maximumLayoutSize(caja));
        linea("alineacion x=" + ol.getLayoutAlignmentX(caja)
                + " y=" + ol.getLayoutAlignmentY(caja));

        caja.setSize(200, 100);
        ol.layoutContainer(caja);
        linea("centrados grande=" + rect(grande.getBounds())
                + " chico=" + rect(chico.getBounds()));

        // Con alineaciones distintas quedan corridos: ver la nota de OverlayLayout.
        chico.setAlignmentX(0.0f);
        chico.setAlignmentY(0.0f);
        ol.invalidateLayout(caja);
        linea("preferido con esquina=" + ol.preferredLayoutSize(caja));
        linea("alineacion x=" + ol.getLayoutAlignmentX(caja)
                + " y=" + ol.getLayoutAlignmentY(caja));
        ol.layoutContainer(caja);
        linea("corridos grande=" + rect(grande.getBounds())
                + " chico=" + rect(chico.getBounds()));

        // Un borde corre a todos: el acomodador trabaja adentro de los margenes.
        caja.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        ol.invalidateLayout(caja);
        linea("con margen preferido=" + ol.preferredLayoutSize(caja));
        ol.layoutContainer(caja);
        linea("con margen grande=" + rect(grande.getBounds())
                + " chico=" + rect(chico.getBounds()));

        JPanel otra = new JPanel();
        try {
            ol.layoutContainer(otra);
            linea("otro contenedor aceptado");
        } catch (java.awt.AWTError e) {
            linea("otro contenedor rechazado: " + e.getMessage());
        }
    }

    /** Un verificador que solo acepta texto no vacio. */
    static class NoVacio extends javax.swing.InputVerifier {

        public boolean verify(javax.swing.JComponent c) {
            return ((JTextField) c).getText().length() > 0;
        }
    }

    /** Un verificador que corrige antes de dejar salir; ver la nota de InputVerifier. */
    static class Corrige extends javax.swing.InputVerifier {

        public boolean verify(javax.swing.JComponent c) {
            return ((JTextField) c).getText().length() > 0;
        }

        public boolean shouldYieldFocus(javax.swing.JComponent c) {
            if (!verify(c)) {
                ((JTextField) c).setText("corregido");
            }
            return true;
        }
    }

    static void sueltas() {
        linea("--- las clases chicas ---");

        JTextField campo = new JTextField();
        linea("verificador inicial=" + campo.getInputVerifier()
                + " verifica al recibir=" + campo.getVerifyInputWhenFocusTarget());
        NoVacio v = new NoVacio();
        campo.setInputVerifier(v);
        linea("puesto=" + (campo.getInputVerifier() == v));
        linea("vacio verifica=" + v.verify(campo) + " cede=" + v.shouldYieldFocus(campo)
                + " vale la pena=" + v.verifyTarget(campo));
        campo.setText("algo");
        linea("con texto verifica=" + v.verify(campo) + " cede=" + v.shouldYieldFocus(campo));
        // La forma de dos componentes cae en la de uno mientras nadie la sobreescriba.
        JTextField otro = new JTextField();
        linea("cede hacia otro=" + v.shouldYieldFocus(campo, otro));
        campo.setText("");
        Corrige c = new Corrige();
        linea("corrige: cede=" + c.shouldYieldFocus(campo) + " y dejo=" + campo.getText());
        campo.setInputVerifier(null);
        linea("sacado=" + campo.getInputVerifier());

        // El gris no es el promedio de los tres canales; ver la nota de GrayFilter.
        javax.swing.GrayFilter claro = new javax.swing.GrayFilter(true, 50);
        javax.swing.GrayFilter oscuro = new javax.swing.GrayFilter(false, 50);
        int[] colores = {0xff000000, 0xffffffff, 0xffff0000, 0xff00ff00, 0xff0000ff,
            0x80336699, 0x00ffffff};
        for (int i = 0; i < colores.length; i++) {
            linea("gris de " + Integer.toHexString(colores[i])
                    + " claro=" + Integer.toHexString(claro.filterRGB(0, 0, colores[i]))
                    + " oscuro=" + Integer.toHexString(oscuro.filterRGB(0, 0, colores[i])));
        }
        javax.swing.GrayFilter todo = new javax.swing.GrayFilter(true, 100);
        javax.swing.GrayFilter nada = new javax.swing.GrayFilter(true, 0);
        linea("al 100 de ff336699=" + Integer.toHexString(todo.filterRGB(0, 0, 0xff336699))
                + " al 0=" + Integer.toHexString(nada.filterRGB(0, 0, 0xff336699)));

        javax.swing.CellRendererPane panel = new javax.swing.CellRendererPane();
        linea("panel de dibujantes visible=" + panel.isVisible()
                + " acomodador=" + panel.getLayout() + " hijos=" + panel.getComponentCount());
        JLabel dib = new JLabel("x");
        panel.add(dib);
        linea("con un dibujante hijos=" + panel.getComponentCount()
                + " padre es el panel=" + (dib.getParent() == panel));
        panel.add(dib);
        linea("agregado dos veces hijos=" + panel.getComponentCount());
        panel.invalidate();
        linea("invalidar no hace nada, sigue valido=" + panel.isValid());

        // La anotacion se lee en tiempo de ejecucion.
        javax.swing.SwingContainer an = Anotada.class.getAnnotation(
                javax.swing.SwingContainer.class);
        linea("anotacion valor=" + an.value() + " delegado=" + an.delegate());
        javax.swing.SwingContainer an2 = Simple.class.getAnnotation(
                javax.swing.SwingContainer.class);
        linea("por omision valor=" + an2.value() + " delegado=[" + an2.delegate() + "]");
    }

    /** Con los dos valores puestos. */
    @javax.swing.SwingContainer(value = false, delegate = "getViewport")
    static class Anotada {
    }

    /** Con los de omision. */
    @javax.swing.SwingContainer
    static class Simple {
    }

    public static int run() {
        bordes();
        editores();
        superponer();
        sueltas();
        return 0;
    }
}
