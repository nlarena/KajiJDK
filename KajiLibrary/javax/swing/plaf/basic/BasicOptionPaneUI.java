package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.OptionPaneUI;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de un panel de opciones -- el contenido de un cuadro de dialogo --.
 *
 * <h2>Tres franjas, y la del medio es la interesante</h2>
 *
 * <p>De arriba a abajo: el mensaje con su icono, el componente de entrada si lo hay, y los botones.
 * Lo unico que no es trivial es el mensaje, porque puede ser cualquier cosa: un texto, un icono, un
 * componente, o un arreglo de todo eso mezclado. {@link #addMessageComponents} lo desarma
 * recursivamente y {@link #burstStringInto} corta el texto largo en renglones.
 *
 * <h2>El tamano minimo no se calcula</h2>
 *
 * <p>Son 262 x 90, escritos. Un dialogo mas chico que eso se ve como un error aunque su contenido
 * entre, y el numero no depende de nada que se pueda medir. Esta en {@link #MinimumWidth} y
 * {@link #MinimumHeight}, que son publicos justamente para que un aspecto los pueda mirar.
 *
 * <h2>Los botones no son botones todavia</h2>
 *
 * <p>{@link #getButtons} no devuelve {@code JButton}: devuelve <em>descripciones</em> de boton. La
 * diferencia importa porque el panel de opciones acepta que le pasen cualquier objeto como opcion
 * --una cadena, un icono, un componente ya hecho-- y quien decide como se convierte en algo
 * apretable es {@link #addButtonComponents}, no esta lista.
 *
 * <p>{@link #getSizeButtonsToSameWidth} dice que si: todos los botones de un dialogo miden lo
 * mismo, aunque "Si" sea mucho mas corto que "Cancelar".
 *
 * <h2>Sin separador</h2>
 *
 * <p>{@link #createSeparator} devuelve {@code null}. Es un gancho para el aspecto que quiera una
 * linea entre el mensaje y los botones; el basico no la dibuja. Medido.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>Los iconos de los cuatro tipos de mensaje --informacion, pregunta, advertencia, error-- son
 * imagenes de 32 x 32 que vienen de la tabla del aspecto. Sin tabla no hay ninguna, asi que
 * {@link #getIconForType} devuelve {@code null} y el dialogo mide menos de ancho que el del JDK. Es
 * el mismo hueco de siempre y no cambia nada de la estructura.
 */
public class BasicOptionPaneUI extends OptionPaneUI {

    /** El ancho minimo de un dialogo; ver la nota de la clase. */
    public static final int MinimumWidth = 262;

    /** Y el alto. */
    public static final int MinimumHeight = 90;

    protected JOptionPane optionPane;
    protected Dimension minimumSize;

    /** El componente donde el usuario escribe, si el dialogo pide algo. */
    protected JComponent inputComponent;

    /** El que se lleva el foco al abrir. */
    protected Component initialFocusComponent;

    /** Si el mensaje trajo componentes propios; de eso depende si el dialogo se puede reusar. */
    protected boolean hasCustomComponents;

    protected PropertyChangeListener propertyChangeListener;

    private static final ColorUIResource FONDO = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FRENTE = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicOptionPaneUI() {
    }

    /** Uno nuevo por panel: guarda el componente y lo que armo adentro. */
    public static ComponentUI createUI(JComponent x) {
        return new BasicOptionPaneUI();
    }

    public void installUI(JComponent c) {
        optionPane = (JOptionPane) c;
        installDefaults();
        optionPane.setLayout(createLayoutManager());
        installComponents();
        installListeners();
        installKeyboardActions();
    }

    public void uninstallUI(JComponent c) {
        uninstallComponents();
        optionPane.setLayout(null);
        uninstallKeyboardActions();
        uninstallListeners();
        uninstallDefaults();
        optionPane = null;
    }

    /** Colores, fuente, borde y el tamano minimo; los valores son los de {@code OptionPane.*}. */
    protected void installDefaults() {
        Color fondo = optionPane.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            optionPane.setBackground(FONDO);
        }
        Color frente = optionPane.getForeground();
        if (frente == null || frente instanceof UIResource) {
            optionPane.setForeground(FRENTE);
        }
        Font fuente = optionPane.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            optionPane.setFont(FUENTE);
        }
        javax.swing.border.Border b = optionPane.getBorder();
        if (b == null || b instanceof UIResource) {
            optionPane.setBorder(new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(
                    0, 0, 0, 0));
        }
        minimumSize = new DimensionUIResource(MinimumWidth, MinimumHeight);
        LookAndFeel.installProperty(optionPane, "opaque", Boolean.TRUE);
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        propertyChangeListener = createPropertyChangeListener();
        optionPane.addPropertyChangeListener(propertyChangeListener);
    }

    protected void uninstallListeners() {
        optionPane.removePropertyChangeListener(propertyChangeListener);
        propertyChangeListener = null;
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler(this);
    }

    /** Sin atajos propios: Escape y Enter los ata el dialogo que lo contiene. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    /** Uno vertical: las tres franjas, una debajo de la otra. */
    protected LayoutManager createLayoutManager() {
        return new BoxLayout(optionPane, BoxLayout.Y_AXIS);
    }

    /** Arma las tres franjas; ver la nota de la clase. */
    protected void installComponents() {
        hasCustomComponents = false;
        inputComponent = null;
        initialFocusComponent = null;

        Container messageArea = createMessageArea();
        if (messageArea != null) {
            optionPane.add(messageArea);
        }
        Container separator = createSeparator();
        if (separator != null) {
            optionPane.add(separator);
        }
        optionPane.add(createButtonArea());
        optionPane.applyComponentOrientation(optionPane.getComponentOrientation());
    }

    protected void uninstallComponents() {
        hasCustomComponents = false;
        inputComponent = null;
        initialFocusComponent = null;
        optionPane.removeAll();
    }

    /** La franja del mensaje: el icono a la izquierda y el mensaje a la derecha. */
    protected Container createMessageArea() {
        JPanel top = new JPanel();
        top.setBorder(new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(0, 0, 0, 0));
        top.setLayout(new BorderLayoutDeMensaje());
        addIcon(top);

        JPanel realBody = new JPanel();
        realBody.setName("OptionPane.realBody");
        realBody.setLayout(new GridBagLayout());
        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = 0;
        cons.gridy = 0;
        cons.gridwidth = GridBagConstraints.REMAINDER;
        cons.gridheight = 1;
        cons.anchor = GridBagConstraints.WEST;
        cons.insets = new Insets(0, 0, 3, 0);
        addMessageComponents(realBody, cons, getMessage(), getMaxCharactersPerLineCount(), false);
        top.add(realBody, "Center");
        return top;
    }

    /** Le pone el icono del tipo de mensaje, si lo hay; ver la nota de la clase. */
    protected void addIcon(Container top) {
        Icon sideIcon = getIcon();
        if (sideIcon != null) {
            JLabel iconLabel = new JLabel(sideIcon);
            iconLabel.setName("OptionPane.iconLabel");
            iconLabel.setVerticalAlignment(SwingConstants.TOP);
            top.add(iconLabel, "West");
        }
    }

    /**
     * Desarma el mensaje y lo agrega a la franja.
     *
     * <p>Un arreglo se recorre; un componente se agrega tal cual y se anota que el mensaje traia
     * cosas propias; un icono se envuelve en una etiqueta; y cualquier otra cosa se convierte a
     * texto y se corta en renglones.
     */
    protected void addMessageComponents(Container container, GridBagConstraints cons, Object msg,
            int maxll, boolean internallyCreated) {
        if (msg == null) {
            return;
        }
        if (msg instanceof Component) {
            if (msg instanceof JComponent || !internallyCreated) {
                hasCustomComponents = true;
            }
            cons.fill = GridBagConstraints.BOTH;
            cons.weightx = 1;
            container.add((Component) msg, cons);
            cons.weightx = 0;
            cons.fill = GridBagConstraints.NONE;
            cons.gridy++;
            return;
        }
        if (msg instanceof Object[]) {
            Object[] msgs = (Object[]) msg;
            for (int i = 0; i < msgs.length; i++) {
                addMessageComponents(container, cons, msgs[i], maxll, false);
            }
            return;
        }
        if (msg instanceof Icon) {
            JLabel label = new JLabel((Icon) msg, SwingConstants.CENTER);
            addMessageComponents(container, cons, label, maxll, true);
            return;
        }
        String s = msg.toString();
        if (s.length() <= 0) {
            return;
        }
        burstStringInto(container, s, maxll);
    }

    /**
     * Corta ese texto en renglones y los agrega uno debajo del otro.
     *
     * <p>Corta por salto de linea y, si un renglon pasa de {@code maxll}, tambien por el ultimo
     * espacio que entre. Con el maximo en infinito --que es lo de omision-- solo corta por saltos.
     */
    protected void burstStringInto(Container c, String d, int maxll) {
        int nl = d.indexOf('\n');
        if (nl >= 0) {
            burstStringInto(c, d.substring(0, nl), maxll);
            burstStringInto(c, d.substring(nl + 1), maxll);
            return;
        }
        if (d.length() > maxll && maxll > 0) {
            int corte = d.lastIndexOf(' ', maxll);
            if (corte <= 0) {
                corte = maxll;
            }
            burstStringInto(c, d.substring(0, corte), maxll);
            burstStringInto(c, d.substring(corte).trim(), maxll);
            return;
        }
        JLabel label = new JLabel(d, SwingConstants.LEADING);
        label.setName("OptionPane.label");
        c.add(label);
    }

    /** {@code null}; ver la nota de la clase. */
    protected Container createSeparator() {
        return null;
    }

    /** La franja de los botones, todos del mismo ancho. */
    protected Container createButtonArea() {
        JPanel bottom = new JPanel();
        bottom.setName("OptionPane.buttonArea");
        bottom.setBorder(new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(6, 0, 0, 0));
        bottom.setLayout(new AcomodadorDeBotones(getSizeButtonsToSameWidth()));
        addButtonComponents(bottom, getButtons(), getInitialValueIndex());
        return bottom;
    }

    /**
     * Convierte cada opcion en algo apretable y lo agrega.
     *
     * <p>Una opcion que ya es un componente se agrega tal cual; cualquier otra cosa se vuelve un
     * boton que, al apretarse, le pasa el valor al panel. Ese es el punto donde se cierra el
     * dialogo, y es por eso que el escucha sabe cual opcion es cual.
     */
    protected void addButtonComponents(Container container, Object[] buttons,
            int initialIndex) {
        if (buttons == null) {
            return;
        }
        for (int i = 0; i < buttons.length; i++) {
            Object opcion = buttons[i];
            Component boton;
            if (opcion instanceof Component) {
                boton = (Component) opcion;
                hasCustomComponents = true;
            } else if (opcion instanceof DescripcionDeBoton) {
                DescripcionDeBoton d = (DescripcionDeBoton) opcion;
                JButton b = new JButton(d.texto);
                b.setName("OptionPane.button");
                if (d.icono != null) {
                    b.setIcon(d.icono);
                }
                if (d.mnemonico != 0) {
                    b.setMnemonic(d.mnemonico);
                }
                b.addActionListener(createButtonActionListener(i));
                boton = b;
            } else if (opcion instanceof Icon) {
                JButton b = new JButton((Icon) opcion);
                b.setName("OptionPane.button");
                b.addActionListener(createButtonActionListener(i));
                boton = b;
            } else {
                JButton b = new JButton(opcion.toString());
                b.setName("OptionPane.button");
                b.addActionListener(createButtonActionListener(i));
                boton = b;
            }
            container.add(boton);
            if (i == initialIndex) {
                initialFocusComponent = boton;
            }
        }
    }

    /** El que le pasa al panel el valor de esa opcion. */
    protected ActionListener createButtonActionListener(int buttonIndex) {
        return new AccionDeBoton(this, buttonIndex);
    }

    /** Las opciones: las que puso el programa, o las que corresponden al tipo de dialogo. */
    protected Object[] getButtons() {
        if (optionPane == null) {
            return null;
        }
        Object[] suppliedOptions = optionPane.getOptions();
        if (suppliedOptions != null) {
            return suppliedOptions;
        }
        int type = optionPane.getOptionType();
        if (type == JOptionPane.YES_NO_OPTION) {
            return new Object[] {
                new DescripcionDeBoton("Yes", 'Y'),
                new DescripcionDeBoton("No", 'N'),
            };
        }
        if (type == JOptionPane.YES_NO_CANCEL_OPTION) {
            return new Object[] {
                new DescripcionDeBoton("Yes", 'Y'),
                new DescripcionDeBoton("No", 'N'),
                new DescripcionDeBoton("Cancel", 'C'),
            };
        }
        if (type == JOptionPane.OK_CANCEL_OPTION) {
            return new Object[] {
                new DescripcionDeBoton("OK", 'O'),
                new DescripcionDeBoton("Cancel", 'C'),
            };
        }
        return new Object[] {new DescripcionDeBoton("OK", 'O')};
    }

    /** Si; ver la nota de la clase. */
    protected boolean getSizeButtonsToSameWidth() {
        return true;
    }

    /** Cual opcion se lleva el foco al abrir. */
    protected int getInitialValueIndex() {
        if (optionPane == null) {
            return -1;
        }
        Object iv = optionPane.getInitialValue();
        Object[] options = optionPane.getOptions();
        if (options == null) {
            return 0;
        }
        if (iv == null) {
            return -1;
        }
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(iv)) {
                return i;
            }
        }
        return -1;
    }

    /** El mensaje que hay que mostrar. */
    protected Object getMessage() {
        inputComponent = null;
        if (optionPane != null) {
            return optionPane.getMessage();
        }
        return null;
    }

    /** El icono del panel, o el que corresponda a su tipo de mensaje. */
    protected Icon getIcon() {
        Icon mIcon = (optionPane == null) ? null : optionPane.getIcon();
        if (mIcon == null && optionPane != null) {
            mIcon = getIconForType(optionPane.getMessageType());
        }
        return mIcon;
    }

    /** {@code null}; ver la nota de la clase. */
    protected Icon getIconForType(int messageType) {
        return null;
    }

    /** Infinito: el basico no corta el texto salvo por saltos de linea. Medido. */
    protected int getMaxCharactersPerLineCount() {
        return Integer.MAX_VALUE;
    }

    /** 262 x 90; ver la nota de la clase. */
    public Dimension getMinimumOptionPaneSize() {
        if (minimumSize == null) {
            return new Dimension(MinimumWidth, MinimumHeight);
        }
        return new Dimension(minimumSize.width, minimumSize.height);
    }

    /** Lo que pida el contenido, pero nunca menos que el minimo. */
    public Dimension getPreferredSize(JComponent c) {
        if (c == optionPane) {
            Dimension ourMin = getMinimumOptionPaneSize();
            LayoutManager lm = c.getLayout();
            if (lm != null) {
                Dimension lmSize = lm.preferredLayoutSize(c);
                if (ourMin != null) {
                    return new Dimension(Math.max(lmSize.width, ourMin.width),
                            Math.max(lmSize.height, ourMin.height));
                }
                return lmSize;
            }
            return ourMin;
        }
        return null;
    }

    /** Le da el foco a la opcion inicial. */
    public void selectInitialValue(JOptionPane op) {
        if (initialFocusComponent != null) {
            initialFocusComponent.requestFocus();
            if (initialFocusComponent instanceof JButton) {
                javax.swing.JRootPane root =
                        javax.swing.SwingUtilities.getRootPane(initialFocusComponent);
                if (root != null) {
                    root.setDefaultButton((JButton) initialFocusComponent);
                }
            }
        }
    }

    public boolean containsCustomComponents(JOptionPane op) {
        return hasCustomComponents;
    }

    /** Deja el componente de entrada con el valor que tenga el panel. */
    protected void resetInputValue() {
        if (inputComponent instanceof javax.swing.JTextField) {
            ((javax.swing.JTextField) inputComponent).setText(
                    (String) optionPane.getInitialSelectionValue());
        }
    }

    /**
     * Una opcion que todavia no es un boton; ver la nota de la clase.
     *
     * <p>El JDK la llama {@code ButtonFactory} y es privada; aca el nombre es descriptivo porque no
     * se ve: lo que se ve del arreglo es su tamano, no el tipo de sus elementos.
     */
    private static class DescripcionDeBoton {

        final String texto;
        final int mnemonico;
        final Icon icono;

        DescripcionDeBoton(String texto, char mnemonico) {
            this.texto = texto;
            this.mnemonico = mnemonico;
            this.icono = null;
        }
    }

    /** Le pasa al panel el valor de la opcion apretada. */
    private static class AccionDeBoton implements ActionListener {

        private final BasicOptionPaneUI ui;
        private final int indice;

        AccionDeBoton(BasicOptionPaneUI ui, int indice) {
            this.ui = ui;
            this.indice = indice;
        }

        public void actionPerformed(ActionEvent e) {
            JOptionPane op = ui.optionPane;
            if (op == null) {
                return;
            }
            Object[] botones = ui.getButtons();
            Object valor;
            if (op.getOptions() != null && indice < op.getOptions().length) {
                valor = op.getOptions()[indice];
            } else if (botones != null && indice < botones.length) {
                // Sin opciones propias, el valor es el numero de la opcion, que es lo que
                // `showConfirmDialog` devuelve.
                valor = Integer.valueOf(indice);
            } else {
                valor = null;
            }
            op.setValue(valor);
        }
    }

    /**
     * El acomodador de la franja del mensaje: icono a la izquierda, mensaje en el centro.
     *
     * <p>Es un {@code BorderLayout} de a mentiras -- solo entiende {@code "West"} y
     * {@code "Center"} --, y esta escrito porque el de verdad estira el centro a lo alto y el icono
     * tiene que quedar arriba.
     */
    private static class BorderLayoutDeMensaje implements LayoutManager {

        private Component oeste;
        private Component centro;

        public void addLayoutComponent(String name, Component comp) {
            if ("West".equals(name)) {
                oeste = comp;
            } else {
                centro = comp;
            }
        }

        public void removeLayoutComponent(Component comp) {
            if (comp == oeste) {
                oeste = null;
            } else if (comp == centro) {
                centro = null;
            }
        }

        public Dimension preferredLayoutSize(Container parent) {
            Dimension o = (oeste == null) ? new Dimension(0, 0) : oeste.getPreferredSize();
            Dimension c = (centro == null) ? new Dimension(0, 0) : centro.getPreferredSize();
            Insets in = parent.getInsets();
            return new Dimension(o.width + c.width + in.left + in.right,
                    Math.max(o.height, c.height) + in.top + in.bottom);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        public void layoutContainer(Container parent) {
            Insets in = parent.getInsets();
            int x = in.left;
            int alto = parent.getHeight() - in.top - in.bottom;
            if (oeste != null) {
                Dimension o = oeste.getPreferredSize();
                oeste.setBounds(x, in.top, o.width, alto);
                x += o.width;
            }
            if (centro != null) {
                centro.setBounds(x, in.top, parent.getWidth() - in.right - x, alto);
            }
        }
    }

    /** El acomodador de los botones: todos del mismo ancho y pegados a la derecha. */
    private static class AcomodadorDeBotones implements LayoutManager {

        private final boolean mismoAncho;
        private final int separacion = 6;

        AcomodadorDeBotones(boolean mismoAncho) {
            this.mismoAncho = mismoAncho;
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        private int anchoDeCadaUno(Container parent) {
            int w = 0;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                w = Math.max(w, parent.getComponent(i).getPreferredSize().width);
            }
            return w;
        }

        public Dimension preferredLayoutSize(Container parent) {
            int n = parent.getComponentCount();
            if (n == 0) {
                return new Dimension(0, 0);
            }
            int alto = 0;
            int ancho = 0;
            int cada = mismoAncho ? anchoDeCadaUno(parent) : 0;
            for (int i = 0; i < n; i++) {
                Dimension d = parent.getComponent(i).getPreferredSize();
                alto = Math.max(alto, d.height);
                ancho += mismoAncho ? cada : d.width;
            }
            ancho += separacion * (n - 1);
            Insets in = parent.getInsets();
            return new Dimension(ancho + in.left + in.right, alto + in.top + in.bottom);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return preferredLayoutSize(parent);
        }

        public void layoutContainer(Container parent) {
            int n = parent.getComponentCount();
            if (n == 0) {
                return;
            }
            Insets in = parent.getInsets();
            int cada = mismoAncho ? anchoDeCadaUno(parent) : 0;
            Dimension pref = preferredLayoutSize(parent);
            int x = (parent.getWidth() - pref.width) / 2 + in.left;
            int alto = pref.height - in.top - in.bottom;
            for (int i = 0; i < n; i++) {
                Component c = parent.getComponent(i);
                int w = mismoAncho ? cada : c.getPreferredSize().width;
                c.setBounds(x, in.top, w, alto);
                x += w + separacion;
            }
        }
    }

    /** Rehace el contenido cuando cambia el mensaje, el tipo o las opciones. */
    private static class Handler implements PropertyChangeListener {

        private final BasicOptionPaneUI ui;

        Handler(BasicOptionPaneUI ui) {
            this.ui = ui;
        }

        public void propertyChange(PropertyChangeEvent e) {
            if (e.getSource() != ui.optionPane) {
                return;
            }
            String nombre = e.getPropertyName();
            if (JOptionPane.ICON_PROPERTY.equals(nombre)
                    || JOptionPane.MESSAGE_PROPERTY.equals(nombre)
                    || JOptionPane.OPTIONS_PROPERTY.equals(nombre)
                    || JOptionPane.INITIAL_VALUE_PROPERTY.equals(nombre)
                    || JOptionPane.MESSAGE_TYPE_PROPERTY.equals(nombre)
                    || JOptionPane.OPTION_TYPE_PROPERTY.equals(nombre)
                    || JOptionPane.WANTS_INPUT_PROPERTY.equals(nombre)
                    || JOptionPane.SELECTION_VALUES_PROPERTY.equals(nombre)) {
                ui.uninstallComponents();
                ui.installComponents();
                ui.optionPane.validate();
            } else if (JOptionPane.INITIAL_SELECTION_VALUE_PROPERTY.equals(nombre)) {
                ui.resetInputValue();
            }
        }
    }
}
