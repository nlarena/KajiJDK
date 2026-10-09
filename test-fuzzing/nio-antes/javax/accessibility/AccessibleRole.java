package javax.accessibility;

/**
 * What an object **is**: a button, a list, a window, a table cell.
 *
 * <p>It is the most important question an assistive technology asks an object, because everything
 * else depends on the answer: how to announce it, what can be done with it, which keys make sense.
 *
 * <p>The list is long and still open: by inheriting from {@link AccessibleBundle} anybody can add
 * the role that is missing. That is on purpose -- interfaces invent new controls faster than a
 * closed enumeration could keep up with them.
 */
public class AccessibleRole extends AccessibleBundle {

    /** The <b>alert</b> category. */
    public static final AccessibleRole ALERT = new AccessibleRole("alert");

    /** The <b>AWT component</b> category. */
    public static final AccessibleRole AWT_COMPONENT = new AccessibleRole("AWT component");

    /** The <b>canvas</b> category. */
    public static final AccessibleRole CANVAS = new AccessibleRole("canvas");

    /** A box that gets checked. */
    public static final AccessibleRole CHECK_BOX = new AccessibleRole("check box");

    /** The <b>color chooser</b> category. */
    public static final AccessibleRole COLOR_CHOOSER = new AccessibleRole("color chooser");

    /** The <b>column header</b> category. */
    public static final AccessibleRole COLUMN_HEADER = new AccessibleRole("column header");

    /** The <b>combo box</b> category. */
    public static final AccessibleRole COMBO_BOX = new AccessibleRole("combo box");

    /** The <b>dateeditor</b> category. */
    public static final AccessibleRole DATE_EDITOR = new AccessibleRole("dateeditor");

    /** The <b>desktop icon</b> category. */
    public static final AccessibleRole DESKTOP_ICON = new AccessibleRole("desktop icon");

    /** The <b>desktop pane</b> category. */
    public static final AccessibleRole DESKTOP_PANE = new AccessibleRole("desktop pane");

    /** A dialog. */
    public static final AccessibleRole DIALOG = new AccessibleRole("dialog");

    /** The <b>directory pane</b> category. */
    public static final AccessibleRole DIRECTORY_PANE = new AccessibleRole("directory pane");

    /** The <b>editbar</b> category. */
    public static final AccessibleRole EDITBAR = new AccessibleRole("editbar");

    /** The <b>file chooser</b> category. */
    public static final AccessibleRole FILE_CHOOSER = new AccessibleRole("file chooser");

    /** The <b>filler</b> category. */
    public static final AccessibleRole FILLER = new AccessibleRole("filler");

    /** The <b>fontchooser</b> category. */
    public static final AccessibleRole FONT_CHOOSER = new AccessibleRole("fontchooser");

    /** The <b>footer</b> category. */
    public static final AccessibleRole FOOTER = new AccessibleRole("footer");

    /** A main window, with a frame. */
    public static final AccessibleRole FRAME = new AccessibleRole("frame");

    /** The <b>glass pane</b> category. */
    public static final AccessibleRole GLASS_PANE = new AccessibleRole("glass pane");

    /** The <b>groupbox</b> category. */
    public static final AccessibleRole GROUP_BOX = new AccessibleRole("groupbox");

    /** The <b>header</b> category. */
    public static final AccessibleRole HEADER = new AccessibleRole("header");

    /** The <b>HTML container</b> category. */
    public static final AccessibleRole HTML_CONTAINER = new AccessibleRole("HTML container");

    /** The <b>hyperlink</b> category. */
    public static final AccessibleRole HYPERLINK = new AccessibleRole("hyperlink");

    /** The <b>icon</b> category. */
    public static final AccessibleRole ICON = new AccessibleRole("icon");

    /** The <b>internal frame</b> category. */
    public static final AccessibleRole INTERNAL_FRAME = new AccessibleRole("internal frame");

    /** A label. */
    public static final AccessibleRole LABEL = new AccessibleRole("label");

    /** The <b>layered pane</b> category. */
    public static final AccessibleRole LAYERED_PANE = new AccessibleRole("layered pane");

    /** A list of items. */
    public static final AccessibleRole LIST = new AccessibleRole("list");

    /** The <b>list item</b> category. */
    public static final AccessibleRole LIST_ITEM = new AccessibleRole("list item");

    /** A menu. */
    public static final AccessibleRole MENU = new AccessibleRole("menu");

    /** The <b>menu bar</b> category. */
    public static final AccessibleRole MENU_BAR = new AccessibleRole("menu bar");

    /** A menu item. */
    public static final AccessibleRole MENU_ITEM = new AccessibleRole("menu item");

    /** The <b>option pane</b> category. */
    public static final AccessibleRole OPTION_PANE = new AccessibleRole("option pane");

    /** The <b>page tab</b> category. */
    public static final AccessibleRole PAGE_TAB = new AccessibleRole("page tab");

    /** The <b>page tab list</b> category. */
    public static final AccessibleRole PAGE_TAB_LIST = new AccessibleRole("page tab list");

    /** The <b>panel</b> category. */
    public static final AccessibleRole PANEL = new AccessibleRole("panel");

    /** The <b>paragraph</b> category. */
    public static final AccessibleRole PARAGRAPH = new AccessibleRole("paragraph");

    /** The <b>password text</b> category. */
    public static final AccessibleRole PASSWORD_TEXT = new AccessibleRole("password text");

    /** The <b>popup menu</b> category. */
    public static final AccessibleRole POPUP_MENU = new AccessibleRole("popup menu");

    /** A progress bar. */
    public static final AccessibleRole PROGRESS_BAR = new AccessibleRole("progress bar");

    /** The <b>progress monitor</b> category. */
    public static final AccessibleRole PROGRESS_MONITOR = new AccessibleRole("progress monitor");

    /** A button that gets pressed. */
    public static final AccessibleRole PUSH_BUTTON = new AccessibleRole("push button");

    /** An exclusive option within a group. */
    public static final AccessibleRole RADIO_BUTTON = new AccessibleRole("radio button");

    /** The <b>root pane</b> category. */
    public static final AccessibleRole ROOT_PANE = new AccessibleRole("root pane");

    /** The <b>row header</b> category. */
    public static final AccessibleRole ROW_HEADER = new AccessibleRole("row header");

    /** The <b>ruler</b> category. */
    public static final AccessibleRole RULER = new AccessibleRole("ruler");

    /** A scroll bar. */
    public static final AccessibleRole SCROLL_BAR = new AccessibleRole("scroll bar");

    /** The <b>scroll pane</b> category. */
    public static final AccessibleRole SCROLL_PANE = new AccessibleRole("scroll pane");

    /** The <b>separator</b> category. */
    public static final AccessibleRole SEPARATOR = new AccessibleRole("separator");

    /** A slider. */
    public static final AccessibleRole SLIDER = new AccessibleRole("slider");

    /** The <b>spinbox</b> category. */
    public static final AccessibleRole SPIN_BOX = new AccessibleRole("spinbox");

    /** The <b>split pane</b> category. */
    public static final AccessibleRole SPLIT_PANE = new AccessibleRole("split pane");

    /** The <b>statusbar</b> category. */
    public static final AccessibleRole STATUS_BAR = new AccessibleRole("statusbar");

    /** The <b>swing component</b> category. */
    public static final AccessibleRole SWING_COMPONENT = new AccessibleRole("swing component");

    /** A table. */
    public static final AccessibleRole TABLE = new AccessibleRole("table");

    /** A text field. */
    public static final AccessibleRole TEXT = new AccessibleRole("text");

    /** The <b>toggle button</b> category. */
    public static final AccessibleRole TOGGLE_BUTTON = new AccessibleRole("toggle button");

    /** The <b>tool bar</b> category. */
    public static final AccessibleRole TOOL_BAR = new AccessibleRole("tool bar");

    /** The <b>tool tip</b> category. */
    public static final AccessibleRole TOOL_TIP = new AccessibleRole("tool tip");

    /** A tree. */
    public static final AccessibleRole TREE = new AccessibleRole("tree");

    /** It is not known what it is. It is an answer, not an error: some objects do not fit. */
    public static final AccessibleRole UNKNOWN = new AccessibleRole("unknown");

    /** The <b>viewport</b> category. */
    public static final AccessibleRole VIEWPORT = new AccessibleRole("viewport");

    /** The <b>window</b> category. */
    public static final AccessibleRole WINDOW = new AccessibleRole("window");

    /**
     * With the given key.
     *
     * <p>It is protected because the built-in roles are the ones above; a subclass can add its own,
     * but nobody should make loose roles that then cannot be compared.
     */
    protected AccessibleRole(String key) {
        this.key = key;
    }
}
