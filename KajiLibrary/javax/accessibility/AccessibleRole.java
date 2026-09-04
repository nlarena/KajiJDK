package javax.accessibility;

/**
 * Qué **es** un objeto: un botón, una lista, una ventana, una celda de tabla.
 *
 * <p>Es la pregunta más importante que una ayuda técnica le hace a un objeto, porque de la respuesta
 * depende todo lo demás: cómo anunciarlo, qué se puede hacer con él, qué teclas tienen sentido.
 *
 * <p>La lista es larga y aun así abierta: heredando de {@link AccessibleBundle} cualquiera puede
 * agregar el rol que le falte. Eso es a propósito — las interfaces inventan controles nuevos más
 * rápido de lo que una enumeración cerrada podría seguirlos.
 */
public class AccessibleRole extends AccessibleBundle {

    /** La categoría <b>alert</b>. */
    public static final AccessibleRole ALERT = new AccessibleRole("alert");

    /** La categoría <b>AWT component</b>. */
    public static final AccessibleRole AWT_COMPONENT = new AccessibleRole("AWT component");

    /** La categoría <b>canvas</b>. */
    public static final AccessibleRole CANVAS = new AccessibleRole("canvas");

    /** Una casilla que se marca. */
    public static final AccessibleRole CHECK_BOX = new AccessibleRole("check box");

    /** La categoría <b>color chooser</b>. */
    public static final AccessibleRole COLOR_CHOOSER = new AccessibleRole("color chooser");

    /** La categoría <b>column header</b>. */
    public static final AccessibleRole COLUMN_HEADER = new AccessibleRole("column header");

    /** La categoría <b>combo box</b>. */
    public static final AccessibleRole COMBO_BOX = new AccessibleRole("combo box");

    /** La categoría <b>dateeditor</b>. */
    public static final AccessibleRole DATE_EDITOR = new AccessibleRole("dateeditor");

    /** La categoría <b>desktop icon</b>. */
    public static final AccessibleRole DESKTOP_ICON = new AccessibleRole("desktop icon");

    /** La categoría <b>desktop pane</b>. */
    public static final AccessibleRole DESKTOP_PANE = new AccessibleRole("desktop pane");

    /** Un diálogo. */
    public static final AccessibleRole DIALOG = new AccessibleRole("dialog");

    /** La categoría <b>directory pane</b>. */
    public static final AccessibleRole DIRECTORY_PANE = new AccessibleRole("directory pane");

    /** La categoría <b>editbar</b>. */
    public static final AccessibleRole EDITBAR = new AccessibleRole("editbar");

    /** La categoría <b>file chooser</b>. */
    public static final AccessibleRole FILE_CHOOSER = new AccessibleRole("file chooser");

    /** La categoría <b>filler</b>. */
    public static final AccessibleRole FILLER = new AccessibleRole("filler");

    /** La categoría <b>fontchooser</b>. */
    public static final AccessibleRole FONT_CHOOSER = new AccessibleRole("fontchooser");

    /** La categoría <b>footer</b>. */
    public static final AccessibleRole FOOTER = new AccessibleRole("footer");

    /** Una ventana principal, con marco. */
    public static final AccessibleRole FRAME = new AccessibleRole("frame");

    /** La categoría <b>glass pane</b>. */
    public static final AccessibleRole GLASS_PANE = new AccessibleRole("glass pane");

    /** La categoría <b>groupbox</b>. */
    public static final AccessibleRole GROUP_BOX = new AccessibleRole("groupbox");

    /** La categoría <b>header</b>. */
    public static final AccessibleRole HEADER = new AccessibleRole("header");

    /** La categoría <b>HTML container</b>. */
    public static final AccessibleRole HTML_CONTAINER = new AccessibleRole("HTML container");

    /** La categoría <b>hyperlink</b>. */
    public static final AccessibleRole HYPERLINK = new AccessibleRole("hyperlink");

    /** La categoría <b>icon</b>. */
    public static final AccessibleRole ICON = new AccessibleRole("icon");

    /** La categoría <b>internal frame</b>. */
    public static final AccessibleRole INTERNAL_FRAME = new AccessibleRole("internal frame");

    /** Una etiqueta. */
    public static final AccessibleRole LABEL = new AccessibleRole("label");

    /** La categoría <b>layered pane</b>. */
    public static final AccessibleRole LAYERED_PANE = new AccessibleRole("layered pane");

    /** Una lista de elementos. */
    public static final AccessibleRole LIST = new AccessibleRole("list");

    /** La categoría <b>list item</b>. */
    public static final AccessibleRole LIST_ITEM = new AccessibleRole("list item");

    /** Un menú. */
    public static final AccessibleRole MENU = new AccessibleRole("menu");

    /** La categoría <b>menu bar</b>. */
    public static final AccessibleRole MENU_BAR = new AccessibleRole("menu bar");

    /** Una opción de menú. */
    public static final AccessibleRole MENU_ITEM = new AccessibleRole("menu item");

    /** La categoría <b>option pane</b>. */
    public static final AccessibleRole OPTION_PANE = new AccessibleRole("option pane");

    /** La categoría <b>page tab</b>. */
    public static final AccessibleRole PAGE_TAB = new AccessibleRole("page tab");

    /** La categoría <b>page tab list</b>. */
    public static final AccessibleRole PAGE_TAB_LIST = new AccessibleRole("page tab list");

    /** La categoría <b>panel</b>. */
    public static final AccessibleRole PANEL = new AccessibleRole("panel");

    /** La categoría <b>paragraph</b>. */
    public static final AccessibleRole PARAGRAPH = new AccessibleRole("paragraph");

    /** La categoría <b>password text</b>. */
    public static final AccessibleRole PASSWORD_TEXT = new AccessibleRole("password text");

    /** La categoría <b>popup menu</b>. */
    public static final AccessibleRole POPUP_MENU = new AccessibleRole("popup menu");

    /** Una barra de progreso. */
    public static final AccessibleRole PROGRESS_BAR = new AccessibleRole("progress bar");

    /** La categoría <b>progress monitor</b>. */
    public static final AccessibleRole PROGRESS_MONITOR = new AccessibleRole("progress monitor");

    /** Un botón que se aprieta. */
    public static final AccessibleRole PUSH_BUTTON = new AccessibleRole("push button");

    /** Una opción excluyente dentro de un grupo. */
    public static final AccessibleRole RADIO_BUTTON = new AccessibleRole("radio button");

    /** La categoría <b>root pane</b>. */
    public static final AccessibleRole ROOT_PANE = new AccessibleRole("root pane");

    /** La categoría <b>row header</b>. */
    public static final AccessibleRole ROW_HEADER = new AccessibleRole("row header");

    /** La categoría <b>ruler</b>. */
    public static final AccessibleRole RULER = new AccessibleRole("ruler");

    /** Una barra de desplazamiento. */
    public static final AccessibleRole SCROLL_BAR = new AccessibleRole("scroll bar");

    /** La categoría <b>scroll pane</b>. */
    public static final AccessibleRole SCROLL_PANE = new AccessibleRole("scroll pane");

    /** La categoría <b>separator</b>. */
    public static final AccessibleRole SEPARATOR = new AccessibleRole("separator");

    /** Un deslizador. */
    public static final AccessibleRole SLIDER = new AccessibleRole("slider");

    /** La categoría <b>spinbox</b>. */
    public static final AccessibleRole SPIN_BOX = new AccessibleRole("spinbox");

    /** La categoría <b>split pane</b>. */
    public static final AccessibleRole SPLIT_PANE = new AccessibleRole("split pane");

    /** La categoría <b>statusbar</b>. */
    public static final AccessibleRole STATUS_BAR = new AccessibleRole("statusbar");

    /** La categoría <b>swing component</b>. */
    public static final AccessibleRole SWING_COMPONENT = new AccessibleRole("swing component");

    /** Una tabla. */
    public static final AccessibleRole TABLE = new AccessibleRole("table");

    /** Un campo de texto. */
    public static final AccessibleRole TEXT = new AccessibleRole("text");

    /** La categoría <b>toggle button</b>. */
    public static final AccessibleRole TOGGLE_BUTTON = new AccessibleRole("toggle button");

    /** La categoría <b>tool bar</b>. */
    public static final AccessibleRole TOOL_BAR = new AccessibleRole("tool bar");

    /** La categoría <b>tool tip</b>. */
    public static final AccessibleRole TOOL_TIP = new AccessibleRole("tool tip");

    /** Un árbol. */
    public static final AccessibleRole TREE = new AccessibleRole("tree");

    /** No se sabe qué es. Es una respuesta, no un error: hay objetos que no encajan. */
    public static final AccessibleRole UNKNOWN = new AccessibleRole("unknown");

    /** La categoría <b>viewport</b>. */
    public static final AccessibleRole VIEWPORT = new AccessibleRole("viewport");

    /** La categoría <b>window</b>. */
    public static final AccessibleRole WINDOW = new AccessibleRole("window");

    /**
     * Con la clave dada.
     *
     * <p>Es protegido porque los roles de fábrica son los de arriba; una subclase puede agregar los
     * suyos, pero nadie debería fabricar roles sueltos que después no se puedan comparar.
     */
    protected AccessibleRole(String key) {
        this.key = key;
    }
}
