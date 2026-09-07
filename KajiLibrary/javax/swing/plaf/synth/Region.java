package javax.swing.plaf.synth;

/**
 * Una parte de un componente que se dibuja por separado.
 *
 * <h2>Que es una region</h2>
 *
 * <p>Un componente no es una sola superficie. Una barra de desplazamiento tiene la pista, el pulgar
 * y dos botones; una pestana tiene la solapa y el area. Cada una de esas partes puede tener su
 * propio color, su propio borde y su propio pintor, y una region es como se la nombra.
 *
 * <h2>Region y subregion</h2>
 *
 * <p>Una region con {@link #isSubregion()} en falso corresponde a un componente entero y tiene una
 * clase de interfaz grafica propia. Una subregion es una parte de otro componente y no la tiene: no
 * hay un {@code ScrollBarThumbUI}, el pulgar lo dibuja la interfaz de la barra.
 *
 * <p>La diferencia importa al buscar el estilo: para una region se pregunta por el componente, para
 * una subregion hay que decir ademas de que parte se trata.
 *
 * <h2>Por que son constantes y no un enum</h2>
 *
 * <p>Porque un aspecto grafico puede definir las suyas heredando de esta clase, y de un enum no se
 * hereda. Es el mismo motivo por el que el constructor es {@code protected} y no privado.
 *
 * @since 1.5
 */
public class Region {

    /** ArrowButton */
    public static final Region ARROW_BUTTON = new Region("ArrowButton", false);

    /** Button */
    public static final Region BUTTON = new Region("Button", false);

    /** CheckBox */
    public static final Region CHECK_BOX = new Region("CheckBox", false);

    /** CheckBoxMenuItem */
    public static final Region CHECK_BOX_MENU_ITEM = new Region("CheckBoxMenuItem", false);

    /** ColorChooser */
    public static final Region COLOR_CHOOSER = new Region("ColorChooser", false);

    /** ComboBox */
    public static final Region COMBO_BOX = new Region("ComboBox", false);

    /** DesktopPane */
    public static final Region DESKTOP_PANE = new Region("DesktopPane", false);

    /** DesktopIcon */
    public static final Region DESKTOP_ICON = new Region("DesktopIcon", false);

    /** EditorPane */
    public static final Region EDITOR_PANE = new Region("EditorPane", false);

    /** FileChooser */
    public static final Region FILE_CHOOSER = new Region("FileChooser", false);

    /** FormattedTextField */
    public static final Region FORMATTED_TEXT_FIELD = new Region("FormattedTextField", false);

    /** InternalFrame */
    public static final Region INTERNAL_FRAME = new Region("InternalFrame", false);

    /** InternalFrameTitlePane */
    public static final Region INTERNAL_FRAME_TITLE_PANE =
            new Region("InternalFrameTitlePane", false);

    /** Label */
    public static final Region LABEL = new Region("Label", false);

    /** List */
    public static final Region LIST = new Region("List", false);

    /** Menu */
    public static final Region MENU = new Region("Menu", false);

    /** MenuBar */
    public static final Region MENU_BAR = new Region("MenuBar", false);

    /** MenuItem */
    public static final Region MENU_ITEM = new Region("MenuItem", false);

    /** MenuItemAccelerator, una subregion */
    public static final Region MENU_ITEM_ACCELERATOR = new Region("MenuItemAccelerator", true);

    /** OptionPane */
    public static final Region OPTION_PANE = new Region("OptionPane", false);

    /** Panel */
    public static final Region PANEL = new Region("Panel", false);

    /** PasswordField */
    public static final Region PASSWORD_FIELD = new Region("PasswordField", false);

    /** PopupMenu */
    public static final Region POPUP_MENU = new Region("PopupMenu", false);

    /** PopupMenuSeparator */
    public static final Region POPUP_MENU_SEPARATOR = new Region("PopupMenuSeparator", false);

    /** ProgressBar */
    public static final Region PROGRESS_BAR = new Region("ProgressBar", false);

    /** RadioButton */
    public static final Region RADIO_BUTTON = new Region("RadioButton", false);

    /** RadioButtonMenuItem */
    public static final Region RADIO_BUTTON_MENU_ITEM = new Region("RadioButtonMenuItem", false);

    /** RootPane */
    public static final Region ROOT_PANE = new Region("RootPane", false);

    /** ScrollBar */
    public static final Region SCROLL_BAR = new Region("ScrollBar", false);

    /** ScrollBarTrack, una subregion */
    public static final Region SCROLL_BAR_TRACK = new Region("ScrollBarTrack", true);

    /** ScrollBarThumb, una subregion */
    public static final Region SCROLL_BAR_THUMB = new Region("ScrollBarThumb", true);

    /** ScrollPane */
    public static final Region SCROLL_PANE = new Region("ScrollPane", false);

    /** Separator */
    public static final Region SEPARATOR = new Region("Separator", false);

    /** Slider */
    public static final Region SLIDER = new Region("Slider", false);

    /** SliderTrack, una subregion */
    public static final Region SLIDER_TRACK = new Region("SliderTrack", true);

    /** SliderThumb, una subregion */
    public static final Region SLIDER_THUMB = new Region("SliderThumb", true);

    /** Spinner */
    public static final Region SPINNER = new Region("Spinner", false);

    /** SplitPane */
    public static final Region SPLIT_PANE = new Region("SplitPane", false);

    /** SplitPaneDivider, una subregion */
    public static final Region SPLIT_PANE_DIVIDER = new Region("SplitPaneDivider", true);

    /** TabbedPane */
    public static final Region TABBED_PANE = new Region("TabbedPane", false);

    /** TabbedPaneTab, una subregion */
    public static final Region TABBED_PANE_TAB = new Region("TabbedPaneTab", true);

    /** TabbedPaneTabArea, una subregion */
    public static final Region TABBED_PANE_TAB_AREA = new Region("TabbedPaneTabArea", true);

    /** TabbedPaneContent, una subregion */
    public static final Region TABBED_PANE_CONTENT = new Region("TabbedPaneContent", true);

    /** Table */
    public static final Region TABLE = new Region("Table", false);

    /** TableHeader */
    public static final Region TABLE_HEADER = new Region("TableHeader", false);

    /** TextArea */
    public static final Region TEXT_AREA = new Region("TextArea", false);

    /** TextField */
    public static final Region TEXT_FIELD = new Region("TextField", false);

    /** TextPane */
    public static final Region TEXT_PANE = new Region("TextPane", false);

    /** ToggleButton */
    public static final Region TOGGLE_BUTTON = new Region("ToggleButton", false);

    /** ToolBar */
    public static final Region TOOL_BAR = new Region("ToolBar", false);

    /** ToolBarContent, una subregion */
    public static final Region TOOL_BAR_CONTENT = new Region("ToolBarContent", true);

    /** ToolBarDragWindow */
    public static final Region TOOL_BAR_DRAG_WINDOW = new Region("ToolBarDragWindow", false);

    /** ToolTip */
    public static final Region TOOL_TIP = new Region("ToolTip", false);

    /** ToolBarSeparator */
    public static final Region TOOL_BAR_SEPARATOR = new Region("ToolBarSeparator", false);

    /** Tree */
    public static final Region TREE = new Region("Tree", false);

    /** TreeCell, una subregion */
    public static final Region TREE_CELL = new Region("TreeCell", true);

    /** Viewport */
    public static final Region VIEWPORT = new Region("Viewport", false);

    /** Todas, para poder buscar por interfaz grafica. */
    private static final Region[] TODAS = {
        ARROW_BUTTON, BUTTON, CHECK_BOX, CHECK_BOX_MENU_ITEM, COLOR_CHOOSER, COMBO_BOX,
        DESKTOP_PANE, DESKTOP_ICON, EDITOR_PANE, FILE_CHOOSER, FORMATTED_TEXT_FIELD, INTERNAL_FRAME,
        INTERNAL_FRAME_TITLE_PANE, LABEL, LIST, MENU, MENU_BAR, MENU_ITEM, MENU_ITEM_ACCELERATOR,
        OPTION_PANE, PANEL, PASSWORD_FIELD, POPUP_MENU, POPUP_MENU_SEPARATOR, PROGRESS_BAR,
        RADIO_BUTTON, RADIO_BUTTON_MENU_ITEM, ROOT_PANE, SCROLL_BAR, SCROLL_BAR_TRACK,
        SCROLL_BAR_THUMB, SCROLL_PANE, SEPARATOR, SLIDER, SLIDER_TRACK, SLIDER_THUMB, SPINNER,
        SPLIT_PANE, SPLIT_PANE_DIVIDER, TABBED_PANE, TABBED_PANE_TAB, TABBED_PANE_TAB_AREA,
        TABBED_PANE_CONTENT, TABLE, TABLE_HEADER, TEXT_AREA, TEXT_FIELD, TEXT_PANE, TOGGLE_BUTTON,
        TOOL_BAR, TOOL_BAR_CONTENT, TOOL_BAR_DRAG_WINDOW, TOOL_TIP, TOOL_BAR_SEPARATOR, TREE,
        TREE_CELL, VIEWPORT,
    };

    private final String name;
    private final String ui;
    private final boolean subregion;

    /**
     * Una region con ese nombre.
     *
     * <p>El identificador de la interfaz grafica se deduce del nombre, que es la convencion: la
     * region {@code Button} se dibuja con lo que este bajo la clave {@code ButtonUI}. Una subregion
     * no tiene, porque no la dibuja una interfaz propia.
     */
    private Region(String name, boolean subregion) {
        this(name, subregion ? null : name + "UI", subregion);
    }

    /**
     * Una region con ese nombre y esa clase de interfaz grafica.
     *
     * @param name el nombre
     * @param ui la clave de la interfaz grafica, o {@code null} si no tiene
     * @param subregion si es parte de otro componente
     * @throws NullPointerException si {@code name} es {@code null}
     */
    protected Region(String name, String ui, boolean subregion) {
        if (name == null) {
            throw new NullPointerException("You must specify a non-null name");
        }
        this.name = name;
        this.ui = ui;
        this.subregion = subregion;
    }

    /**
     * Si es parte de otro componente.
     *
     * @return cierto si es una subregion
     */
    public boolean isSubregion() {
        return subregion;
    }

    /**
     * El nombre.
     *
     * @return el nombre
     */
    public String getName() {
        return name;
    }

    /**
     * La region cuya interfaz grafica se llama asi, o {@code null}.
     *
     * <p>No es parte del API. La busqueda es lineal sobre cincuenta y siete constantes y se hace una
     * vez por componente al instalarse, no por repintado: armar un mapa costaria mas de lo que
     * ahorra.
     */
    static Region porUI(String ui) {
        if (ui == null) {
            return null;
        }
        for (int i = 0; i < TODAS.length; i++) {
            if (ui.equals(TODAS[i].ui)) {
                return TODAS[i];
            }
        }
        return null;
    }

    /**
     * El nombre.
     *
     * @return el nombre
     */
    @Override
    public String toString() {
        return name;
    }
}
