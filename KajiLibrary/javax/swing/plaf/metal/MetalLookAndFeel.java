package javax.swing.plaf.metal;

import java.awt.Component;
import java.awt.Container;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;
import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicLookAndFeel;

/**
 * El aspecto grafico Metal, el unico que Java trae y se ve igual en todos lados.
 *
 * <h2>Cuarenta metodos estaticos que no guardan nada</h2>
 *
 * <p>Casi toda esta clase es una fachada del tema: {@link #getControl} llama a
 * {@code getCurrentTheme().getControl()} y no hace nada mas. Estan porque las treinta y pico de
 * clases {@code Metal*UI} preguntan colores todo el tiempo, y escribir
 * {@code MetalLookAndFeel.getControlShadow()} en vez de buscar el tema es la diferencia entre que
 * se lea o no.
 *
 * <p>El tema es <strong>estatico</strong>: hay uno solo por maquina virtual y
 * {@link #setCurrentTheme} lo cambia para todos. Eso es lo que permite que un programa cambie de
 * paleta con una linea, y tambien la razon de que no se pueda tener dos ventanas con temas
 * distintos.
 *
 * <h2>El aspecto se arma en tres pasos</h2>
 *
 * <p>{@link #initClassDefaults} dice que clase dibuja cada componente,
 * {@link #initSystemColorDefaults} pone los nombres de color del sistema
 * -- {@code "control"}, {@code "window"}, {@code "menuText"} -- y
 * {@link #initComponentDefaults} pone todo lo demas. Los tres escriben sobre la misma tabla y en
 * ese orden, asi que el ultimo puede pisar al primero.
 *
 * <p>Lo notable de {@code initClassDefaults} es que **veintitres** de las cuarenta y tres entradas
 * apuntan a clases de {@code plaf.basic} y no a clases de Metal. Metal no redefine lo que no
 * necesita cambiar: una lista, una tabla, un menu contextual se ven igual, y escribir un
 * {@code MetalListUI} que no agregue nada seria una clase de mas en cada arranque.
 *
 * <h2>Lo que queda dicho y no tapado</h2>
 *
 * <p>{@link #initComponentDefaults} pone las entradas que se derivan del tema -- colores, fuentes,
 * margenes, bordes -- pero no las mil y pico del JDK: faltan los atajos de teclado, que necesitan
 * la lista completa de acciones por componente, y las entradas de icono, que son imagenes del jar.
 * La tabla del JDK tiene 642 entradas y esta muchas menos; las que estan son las mismas.
 *
 * <p>{@link #getSupportsWindowDecorations} contesta que si -- es lo que contesta el JDK -- aunque
 * decorar una ventana de verdad necesite un escritorio que esta VM no tiene.
 */
public class MetalLookAndFeel extends BasicLookAndFeel {

    private static MetalTheme temaActual;
    private static LayoutStyle estilo;

    public MetalLookAndFeel() {
    }

    public String getName() {
        return "Metal";
    }

    public String getID() {
        return "Metal";
    }

    public String getDescription() {
        return "The Java(tm) Look and Feel";
    }

    /** No: Metal no imita a ningun sistema, y ese es el punto. */
    public boolean isNativeLookAndFeel() {
        return false;
    }

    /** Siempre; Metal anda donde ande Java. */
    public boolean isSupportedLookAndFeel() {
        return true;
    }

    public boolean getSupportsWindowDecorations() {
        return true;
    }

    /** Ocean; ver {@link OceanTheme}. */
    protected void createDefaultTheme() {
        if (temaActual == null) {
            setCurrentTheme(new OceanTheme());
        }
    }

    /**
     * Cambia el tema de toda la maquina virtual.
     *
     * @param theme el tema
     * @throws NullPointerException si es nulo; el mensaje es el del JDK y esta medido
     */
    public static void setCurrentTheme(MetalTheme theme) {
        if (theme == null) {
            throw new NullPointerException("Can't have null theme");
        }
        temaActual = theme;
    }

    /** El tema; si nadie puso ninguno, Ocean. */
    public static MetalTheme getCurrentTheme() {
        if (temaActual == null) {
            temaActual = new OceanTheme();
        }
        return temaActual;
    }

    /**
     * Si el tema actual es Ocean.
     *
     * <p>Un puñado de clases de Metal se dibujan distinto con Ocean que con Steel, y esta es la
     * pregunta que hacen. La mas visible es el desplegable: con Steel es un boton entero y con
     * Ocean es un campo con una flecha al lado.
     */
    static boolean usandoOcean() {
        return getCurrentTheme() instanceof OceanTheme;
    }

    // ---- la fachada del tema ----

    public static ColorUIResource getWhite() {
        return getCurrentTheme().getWhite();
    }

    public static ColorUIResource getBlack() {
        return getCurrentTheme().getBlack();
    }

    public static ColorUIResource getControl() {
        return getCurrentTheme().getControl();
    }

    public static ColorUIResource getControlShadow() {
        return getCurrentTheme().getControlShadow();
    }

    public static ColorUIResource getControlDarkShadow() {
        return getCurrentTheme().getControlDarkShadow();
    }

    public static ColorUIResource getControlHighlight() {
        return getCurrentTheme().getControlHighlight();
    }

    public static ColorUIResource getControlInfo() {
        return getCurrentTheme().getControlInfo();
    }

    public static ColorUIResource getControlDisabled() {
        return getCurrentTheme().getControlDisabled();
    }

    public static ColorUIResource getControlTextColor() {
        return getCurrentTheme().getControlTextColor();
    }

    public static ColorUIResource getPrimaryControl() {
        return getCurrentTheme().getPrimaryControl();
    }

    public static ColorUIResource getPrimaryControlShadow() {
        return getCurrentTheme().getPrimaryControlShadow();
    }

    public static ColorUIResource getPrimaryControlDarkShadow() {
        return getCurrentTheme().getPrimaryControlDarkShadow();
    }

    public static ColorUIResource getPrimaryControlHighlight() {
        return getCurrentTheme().getPrimaryControlHighlight();
    }

    public static ColorUIResource getPrimaryControlInfo() {
        return getCurrentTheme().getPrimaryControlInfo();
    }

    public static ColorUIResource getSystemTextColor() {
        return getCurrentTheme().getSystemTextColor();
    }

    public static ColorUIResource getUserTextColor() {
        return getCurrentTheme().getUserTextColor();
    }

    public static ColorUIResource getInactiveControlTextColor() {
        return getCurrentTheme().getInactiveControlTextColor();
    }

    public static ColorUIResource getInactiveSystemTextColor() {
        return getCurrentTheme().getInactiveSystemTextColor();
    }

    public static ColorUIResource getHighlightedTextColor() {
        return getCurrentTheme().getHighlightedTextColor();
    }

    public static ColorUIResource getTextHighlightColor() {
        return getCurrentTheme().getTextHighlightColor();
    }

    public static ColorUIResource getWindowBackground() {
        return getCurrentTheme().getWindowBackground();
    }

    public static ColorUIResource getDesktopColor() {
        return getCurrentTheme().getDesktopColor();
    }

    public static ColorUIResource getFocusColor() {
        return getCurrentTheme().getFocusColor();
    }

    public static ColorUIResource getMenuBackground() {
        return getCurrentTheme().getMenuBackground();
    }

    public static ColorUIResource getMenuForeground() {
        return getCurrentTheme().getMenuForeground();
    }

    public static ColorUIResource getMenuSelectedBackground() {
        return getCurrentTheme().getMenuSelectedBackground();
    }

    public static ColorUIResource getMenuSelectedForeground() {
        return getCurrentTheme().getMenuSelectedForeground();
    }

    public static ColorUIResource getMenuDisabledForeground() {
        return getCurrentTheme().getMenuDisabledForeground();
    }

    public static ColorUIResource getAcceleratorForeground() {
        return getCurrentTheme().getAcceleratorForeground();
    }

    public static ColorUIResource getAcceleratorSelectedForeground() {
        return getCurrentTheme().getAcceleratorSelectedForeground();
    }

    public static ColorUIResource getSeparatorBackground() {
        return getCurrentTheme().getSeparatorBackground();
    }

    public static ColorUIResource getSeparatorForeground() {
        return getCurrentTheme().getSeparatorForeground();
    }

    public static ColorUIResource getWindowTitleBackground() {
        return getCurrentTheme().getWindowTitleBackground();
    }

    public static ColorUIResource getWindowTitleForeground() {
        return getCurrentTheme().getWindowTitleForeground();
    }

    public static ColorUIResource getWindowTitleInactiveBackground() {
        return getCurrentTheme().getWindowTitleInactiveBackground();
    }

    public static ColorUIResource getWindowTitleInactiveForeground() {
        return getCurrentTheme().getWindowTitleInactiveForeground();
    }

    public static FontUIResource getControlTextFont() {
        return getCurrentTheme().getControlTextFont();
    }

    public static FontUIResource getSystemTextFont() {
        return getCurrentTheme().getSystemTextFont();
    }

    public static FontUIResource getUserTextFont() {
        return getCurrentTheme().getUserTextFont();
    }

    public static FontUIResource getMenuTextFont() {
        return getCurrentTheme().getMenuTextFont();
    }

    public static FontUIResource getWindowTitleFont() {
        return getCurrentTheme().getWindowTitleFont();
    }

    public static FontUIResource getSubTextFont() {
        return getCurrentTheme().getSubTextFont();
    }

    /**
     * Un color de la tabla de Metal, por su clave.
     *
     * <p>Mira primero el aspecto instalado y, si no hay ninguno, la tabla que este aspecto
     * generaria. Las clases {@code Metal*UI} la usan para sacar sus colores con el prefijo de su
     * componente, y de ahi sale una diferencia que sorprende y esta medida: {@code "CheckBox.focus"}
     * existe y {@code "CheckBox.select"} no, asi que una casilla tiene color de foco y no tiene
     * color de seleccion. No es un olvido -- una casilla no se rellena al elegirse, se le dibuja
     * una tilde -- pero solo se ve mirando la tabla.
     *
     * @param clave la clave, con prefijo
     * @return el color, o {@code null} si esa clave no esta
     */
    static java.awt.Color colorDeLaTabla(String clave) {
        java.awt.Color c = javax.swing.UIManager.getColor(clave);
        if (c != null) {
            return c;
        }
        Object o = tablaPropia().get(clave);
        return (o instanceof java.awt.Color) ? (java.awt.Color) o : null;
    }

    private static UIDefaults tablaPropia;
    private static MetalTheme temaDeLaTabla;

    /** La tabla de este aspecto, rearmada si cambio el tema. */
    private static UIDefaults tablaPropia() {
        MetalTheme t = getCurrentTheme();
        if (tablaPropia == null || temaDeLaTabla != t) {
            UIDefaults tabla = new UIDefaults();
            new MetalLookAndFeel().initComponentDefaults(tabla);
            // Y lo que el tema agrega encima, que es de donde salen los colores de las solapas.
            t.addCustomEntriesToTable(tabla);
            tablaPropia = tabla;
            temaDeLaTabla = t;
        }
        return tablaPropia;
    }

    // ---- la tabla ----

    public UIDefaults getDefaults() {
        createDefaultTheme();
        UIDefaults table = super.getDefaults();
        getCurrentTheme().addCustomEntriesToTable(table);
        return table;
    }

    /** Veinte de Metal y veintitres del basico; ver la nota de la clase. */
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        String metal = "javax.swing.plaf.metal.Metal";
        String basico = "javax.swing.plaf.basic.Basic";
        Object[] pares = {
            "ButtonUI", metal + "ButtonUI",
            "CheckBoxUI", metal + "CheckBoxUI",
            "ComboBoxUI", metal + "ComboBoxUI",
            "DesktopIconUI", metal + "DesktopIconUI",
            "FileChooserUI", metal + "FileChooserUI",
            "InternalFrameUI", metal + "InternalFrameUI",
            "LabelUI", metal + "LabelUI",
            "MenuBarUI", metal + "MenuBarUI",
            "PopupMenuSeparatorUI", metal + "PopupMenuSeparatorUI",
            "ProgressBarUI", metal + "ProgressBarUI",
            "RadioButtonUI", metal + "RadioButtonUI",
            "RootPaneUI", metal + "RootPaneUI",
            "ScrollBarUI", metal + "ScrollBarUI",
            "ScrollPaneUI", metal + "ScrollPaneUI",
            "SeparatorUI", metal + "SeparatorUI",
            "SliderUI", metal + "SliderUI",
            "SplitPaneUI", metal + "SplitPaneUI",
            "TabbedPaneUI", metal + "TabbedPaneUI",
            "TextFieldUI", metal + "TextFieldUI",
            "ToggleButtonUI", metal + "ToggleButtonUI",
            "ToolBarUI", metal + "ToolBarUI",
            "ToolTipUI", metal + "ToolTipUI",
            "TreeUI", metal + "TreeUI",

            // Lo que Metal no cambia se dibuja con el basico; ver la nota de la clase.
            "CheckBoxMenuItemUI", basico + "CheckBoxMenuItemUI",
            "ColorChooserUI", basico + "ColorChooserUI",
            "DesktopPaneUI", basico + "DesktopPaneUI",
            "EditorPaneUI", basico + "EditorPaneUI",
            "FormattedTextFieldUI", basico + "FormattedTextFieldUI",
            "ListUI", basico + "ListUI",
            "MenuItemUI", basico + "MenuItemUI",
            "MenuUI", basico + "MenuUI",
            "OptionPaneUI", basico + "OptionPaneUI",
            "PanelUI", basico + "PanelUI",
            "PasswordFieldUI", basico + "PasswordFieldUI",
            "PopupMenuUI", basico + "PopupMenuUI",
            "RadioButtonMenuItemUI", basico + "RadioButtonMenuItemUI",
            "SpinnerUI", basico + "SpinnerUI",
            "TableHeaderUI", basico + "TableHeaderUI",
            "TableUI", basico + "TableUI",
            "TextAreaUI", basico + "TextAreaUI",
            "TextPaneUI", basico + "TextPaneUI",
            "ToolBarSeparatorUI", basico + "ToolBarSeparatorUI",
            "ViewportUI", basico + "ViewportUI",
        };
        table.putDefaults(pares);
    }

    /**
     * Los nombres de color del sistema, todos sacados del tema.
     *
     * <p>Un aspecto nativo los pediria al escritorio; Metal no, y por eso se ve igual en todos
     * lados. Los veintiseis nombres son los de {@code java.awt.SystemColor}.
     */
    protected void initSystemColorDefaults(UIDefaults table) {
        MetalTheme t = getCurrentTheme();
        Object[] pares = {
            "desktop", t.getDesktopColor(),
            "activeCaption", t.getWindowTitleBackground(),
            "activeCaptionText", t.getWindowTitleForeground(),
            "activeCaptionBorder", t.getPrimaryControlShadow(),
            "inactiveCaption", t.getWindowTitleInactiveBackground(),
            "inactiveCaptionText", t.getWindowTitleInactiveForeground(),
            "inactiveCaptionBorder", t.getControlShadow(),
            "window", t.getWindowBackground(),
            "windowBorder", t.getControl(),
            "windowText", t.getUserTextColor(),
            "menu", t.getMenuBackground(),
            "menuText", t.getMenuForeground(),
            "text", t.getWindowBackground(),
            "textText", t.getUserTextColor(),
            "textHighlight", t.getTextHighlightColor(),
            "textHighlightText", t.getHighlightedTextColor(),
            "textInactiveText", t.getInactiveSystemTextColor(),
            "control", t.getControl(),
            "controlText", t.getControlTextColor(),
            "controlHighlight", t.getControlHighlight(),
            "controlLtHighlight", t.getControlHighlight(),
            "controlShadow", t.getControlShadow(),
            "controlDkShadow", t.getControlDarkShadow(),
            "scrollbar", t.getControl(),
            "info", t.getPrimaryControl(),
            "infoText", t.getPrimaryControlInfo(),
        };
        table.putDefaults(pares);
    }

    /** Los colores, las fuentes y los margenes por componente; ver la nota de la clase. */
    protected void initComponentDefaults(UIDefaults table) {
        super.initComponentDefaults(table);
        MetalTheme t = getCurrentTheme();
        ColorUIResource control = t.getControl();
        ColorUIResource texto = t.getControlTextColor();
        ColorUIResource apagado = t.getInactiveControlTextColor();
        ColorUIResource ventana = t.getWindowBackground();
        ColorUIResource seleccion = t.getTextHighlightColor();
        ColorUIResource textoElegido = t.getHighlightedTextColor();
        FontUIResource fControl = t.getControlTextFont();
        FontUIResource fSistema = t.getSystemTextFont();
        FontUIResource fUsuario = t.getUserTextFont();
        FontUIResource fMenu = t.getMenuTextFont();
        FontUIResource fChica = t.getSubTextFont();

        Object[] pares = {
            "Button.background", control,
            "Button.foreground", texto,
            "Button.disabledText", apagado,
            "Button.font", fControl,
            "Button.focus", t.getFocusColor(),
            "Button.select", t.getControlShadow(),
            "Button.margin", new java.awt.Insets(2, 14, 2, 14),

            "ToggleButton.background", control,
            "ToggleButton.foreground", texto,
            "ToggleButton.disabledText", apagado,
            "ToggleButton.font", fControl,
            "ToggleButton.focus", t.getFocusColor(),
            "ToggleButton.select", t.getControlShadow(),

            "CheckBox.background", control,
            "CheckBox.foreground", texto,
            "CheckBox.disabledText", apagado,
            "CheckBox.font", fControl,
            "CheckBox.focus", t.getFocusColor(),

            "RadioButton.background", control,
            "RadioButton.foreground", texto,
            "RadioButton.disabledText", apagado,
            "RadioButton.font", fControl,
            "RadioButton.focus", t.getFocusColor(),
            "RadioButton.select", t.getPrimaryControl(),

            // "CheckBox.select" no esta, y no es un olvido: una casilla no se rellena al
            // elegirse, se le dibuja una tilde. Medido contra la tabla del JDK.

            "Label.background", control,
            "Label.foreground", t.getSystemTextColor(),
            "Label.disabledForeground", apagado,
            "Label.font", fControl,

            "Panel.background", control,
            "Panel.foreground", t.getUserTextColor(),
            "Panel.font", fControl,

            "Separator.background", t.getSeparatorBackground(),
            "Separator.foreground", t.getSeparatorForeground(),

            "TextField.background", ventana,
            "TextField.foreground", t.getUserTextColor(),
            "TextField.inactiveForeground", apagado,
            "TextField.selectionBackground", seleccion,
            "TextField.selectionForeground", textoElegido,
            "TextField.caretForeground", t.getUserTextColor(),
            "TextField.font", fUsuario,

            "TextArea.background", ventana,
            "TextArea.foreground", t.getUserTextColor(),
            "TextArea.selectionBackground", seleccion,
            "TextArea.selectionForeground", textoElegido,
            "TextArea.font", fUsuario,

            "List.background", ventana,
            "List.foreground", t.getUserTextColor(),
            "List.selectionBackground", seleccion,
            "List.selectionForeground", textoElegido,
            "List.font", fControl,

            "Table.background", ventana,
            "Table.foreground", t.getUserTextColor(),
            "Table.selectionBackground", seleccion,
            "Table.selectionForeground", textoElegido,
            "Table.font", fUsuario,

            "Tree.background", ventana,
            "Tree.foreground", t.getUserTextColor(),
            "Tree.textBackground", ventana,
            "Tree.textForeground", t.getUserTextColor(),
            "Tree.selectionBackground", seleccion,
            "Tree.selectionForeground", textoElegido,
            "Tree.hash", t.getPrimaryControl(),
            "Tree.font", fUsuario,
            "Tree.rowHeight", Integer.valueOf(0),

            "Menu.background", t.getMenuBackground(),
            "Menu.foreground", t.getMenuForeground(),
            "Menu.selectionBackground", t.getMenuSelectedBackground(),
            "Menu.selectionForeground", t.getMenuSelectedForeground(),
            "Menu.disabledForeground", t.getMenuDisabledForeground(),
            "Menu.acceleratorForeground", t.getAcceleratorForeground(),
            "Menu.acceleratorSelectionForeground", t.getAcceleratorSelectedForeground(),
            "Menu.font", fMenu,
            "Menu.acceleratorFont", fChica,

            "MenuBar.background", t.getMenuBackground(),
            "MenuBar.foreground", t.getMenuForeground(),
            "MenuBar.font", fMenu,

            "MenuItem.background", t.getMenuBackground(),
            "MenuItem.foreground", t.getMenuForeground(),
            "MenuItem.selectionBackground", t.getMenuSelectedBackground(),
            "MenuItem.selectionForeground", t.getMenuSelectedForeground(),
            "MenuItem.disabledForeground", t.getMenuDisabledForeground(),
            "MenuItem.acceleratorForeground", t.getAcceleratorForeground(),
            "MenuItem.font", fMenu,
            "MenuItem.acceleratorFont", fChica,

            "PopupMenu.background", t.getMenuBackground(),
            "PopupMenu.foreground", t.getMenuForeground(),
            "PopupMenu.font", fMenu,

            "ScrollBar.background", control,
            "ScrollBar.foreground", control,
            "ScrollBar.track", t.getControlShadow(),
            "ScrollBar.thumb", t.getPrimaryControlShadow(),
            "ScrollBar.thumbShadow", t.getPrimaryControlDarkShadow(),
            "ScrollBar.thumbHighlight", t.getPrimaryControl(),
            "ScrollBar.width", Integer.valueOf(17),

            "ScrollPane.background", control,
            "ScrollPane.foreground", texto,

            "Slider.background", control,
            "Slider.foreground", t.getPrimaryControlShadow(),
            "Slider.focus", t.getFocusColor(),
            "Slider.highlight", t.getControlHighlight(),
            "Slider.shadow", t.getControlShadow(),

            "SplitPane.background", control,
            "SplitPane.highlight", t.getControlHighlight(),
            "SplitPane.shadow", t.getControlShadow(),
            "SplitPane.darkShadow", t.getControlDarkShadow(),
            "SplitPane.dividerSize", Integer.valueOf(10),

            "TabbedPane.background", control,
            "TabbedPane.foreground", texto,
            "TabbedPane.highlight", t.getControlHighlight(),
            "TabbedPane.shadow", t.getControlShadow(),
            "TabbedPane.darkShadow", t.getControlDarkShadow(),
            "TabbedPane.focus", t.getPrimaryControlDarkShadow(),
            "TabbedPane.font", fControl,

            "ToolBar.background", t.getMenuBackground(),
            "ToolBar.foreground", t.getMenuForeground(),
            "ToolBar.font", fMenu,

            "ToolTip.background", t.getPrimaryControl(),
            "ToolTip.foreground", t.getPrimaryControlInfo(),
            "ToolTip.backgroundInactive", control,
            "ToolTip.foregroundInactive", t.getControlDarkShadow(),
            "ToolTip.font", fSistema,

            "ProgressBar.background", control,
            "ProgressBar.foreground", t.getPrimaryControlShadow(),
            "ProgressBar.selectionBackground", t.getPrimaryControlDarkShadow(),
            "ProgressBar.selectionForeground", control,
            "ProgressBar.font", fControl,

            "ComboBox.background", control,
            "ComboBox.foreground", texto,
            "ComboBox.buttonBackground", control,
            "ComboBox.buttonShadow", t.getControlShadow(),
            "ComboBox.buttonDarkShadow", t.getControlDarkShadow(),
            "ComboBox.buttonHighlight", t.getControlHighlight(),
            "ComboBox.selectionBackground", t.getPrimaryControlShadow(),
            "ComboBox.selectionForeground", t.getControlTextColor(),
            "ComboBox.font", fControl,

            "InternalFrame.activeTitleBackground", t.getWindowTitleBackground(),
            "InternalFrame.activeTitleForeground", t.getWindowTitleForeground(),
            "InternalFrame.inactiveTitleBackground", t.getWindowTitleInactiveBackground(),
            "InternalFrame.inactiveTitleForeground", t.getWindowTitleInactiveForeground(),
            "InternalFrame.titleFont", t.getWindowTitleFont(),

            "Desktop.background", t.getDesktopColor(),

            "OptionPane.background", control,
            "OptionPane.foreground", texto,
            "OptionPane.messageForeground", texto,
            "OptionPane.font", fControl,

            "Viewport.background", control,
            "Viewport.foreground", t.getUserTextColor(),
        };
        table.putDefaults(pares);
    }

    /** Metal no tiene sonidos propios. */
    public void provideErrorFeedback(Component component) {
        super.provideErrorFeedback(component);
    }

    public Icon getDisabledIcon(JComponent component, Icon icon) {
        return super.getDisabledIcon(component, icon);
    }

    public Icon getDisabledSelectedIcon(JComponent component, Icon icon) {
        return super.getDisabledSelectedIcon(component, icon);
    }

    public LayoutStyle getLayoutStyle() {
        if (estilo == null) {
            estilo = new MetalLayoutStyle();
        }
        return estilo;
    }

    /**
     * El espaciado que Metal recomienda.
     *
     * <p>Son los mismos numeros que usa {@code LayoutStyle} sin aspecto instalado, y esta medido:
     * seis pixeles entre cosas relacionadas, doce entre cosas que no lo estan, y doce contra el
     * borde del contenedor.
     */
    private static class MetalLayoutStyle extends LayoutStyle {

        public int getPreferredGap(JComponent component1, JComponent component2,
                ComponentPlacement type, int position, Container parent) {
            // Un componente nulo sale como NullPointerException; ver LayoutStyle.
            component1.getWidth();
            component2.getWidth();
            if (type == null) {
                throw new NullPointerException("type");
            }
            comprobarPosicion(position);
            if (type == ComponentPlacement.INDENT
                    && (position == SwingConstants.EAST || position == SwingConstants.WEST)) {
                return 12;
            }
            return (type == ComponentPlacement.UNRELATED) ? 12 : 6;
        }

        public int getContainerGap(JComponent component, int position, Container parent) {
            component.getWidth();
            comprobarPosicion(position);
            return 12;
        }

        private static void comprobarPosicion(int position) {
            if (position != SwingConstants.NORTH && position != SwingConstants.SOUTH
                    && position != SwingConstants.EAST && position != SwingConstants.WEST) {
                throw new IllegalArgumentException("Invalid position");
            }
        }
    }
}
