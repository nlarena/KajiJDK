package javax.swing.plaf.synth;

/**
 * A part of a component that is drawn separately.
 *
 * <h2>What a region is</h2>
 *
 * <p>A component is not a single surface. A scroll bar has the track, the thumb and two buttons;
 * a tabbed pane has the tab and the area. Each of those parts may have its own colour, its own
 * border and its own painter, and a region is how it is named.
 *
 * <h2>Region and subregion</h2>
 *
 * <p>A region with {@link #isSubregion()} at false corresponds to a whole component and has a
 * look and feel class of its own. A subregion is a part of another component and does not have
 * one: there is no {@code ScrollBarThumbUI}, the thumb is drawn by the bar's look and feel.
 *
 * <p>The difference matters when looking up the style: for a region the component is asked for,
 * for a subregion which part it is about has to be said as well.
 *
 * <h2>Why they are constants and not an enum</h2>
 *
 * <p>Because a look and feel may define its own by inheriting from this class, and an enum cannot
 * be inherited from. It is the same reason the constructor is {@code protected} and not private.
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

    /** MenuItemAccelerator, a subregion */
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

    /** ScrollBarTrack, a subregion */
    public static final Region SCROLL_BAR_TRACK = new Region("ScrollBarTrack", true);

    /** ScrollBarThumb, a subregion */
    public static final Region SCROLL_BAR_THUMB = new Region("ScrollBarThumb", true);

    /** ScrollPane */
    public static final Region SCROLL_PANE = new Region("ScrollPane", false);

    /** Separator */
    public static final Region SEPARATOR = new Region("Separator", false);

    /** Slider */
    public static final Region SLIDER = new Region("Slider", false);

    /** SliderTrack, a subregion */
    public static final Region SLIDER_TRACK = new Region("SliderTrack", true);

    /** SliderThumb, a subregion */
    public static final Region SLIDER_THUMB = new Region("SliderThumb", true);

    /** Spinner */
    public static final Region SPINNER = new Region("Spinner", false);

    /** SplitPane */
    public static final Region SPLIT_PANE = new Region("SplitPane", false);

    /** SplitPaneDivider, a subregion */
    public static final Region SPLIT_PANE_DIVIDER = new Region("SplitPaneDivider", true);

    /** TabbedPane */
    public static final Region TABBED_PANE = new Region("TabbedPane", false);

    /** TabbedPaneTab, a subregion */
    public static final Region TABBED_PANE_TAB = new Region("TabbedPaneTab", true);

    /** TabbedPaneTabArea, a subregion */
    public static final Region TABBED_PANE_TAB_AREA = new Region("TabbedPaneTabArea", true);

    /** TabbedPaneContent, a subregion */
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

    /** ToolBarContent, a subregion */
    public static final Region TOOL_BAR_CONTENT = new Region("ToolBarContent", true);

    /** ToolBarDragWindow */
    public static final Region TOOL_BAR_DRAG_WINDOW = new Region("ToolBarDragWindow", false);

    /** ToolTip */
    public static final Region TOOL_TIP = new Region("ToolTip", false);

    /** ToolBarSeparator */
    public static final Region TOOL_BAR_SEPARATOR = new Region("ToolBarSeparator", false);

    /** Tree */
    public static final Region TREE = new Region("Tree", false);

    /** TreeCell, a subregion */
    public static final Region TREE_CELL = new Region("TreeCell", true);

    /** Viewport */
    public static final Region VIEWPORT = new Region("Viewport", false);

    /** All of them, so that they can be looked up by look and feel. */
    private static final Region[] ALL = {
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
     * A region with that name.
     *
     * <p>The look and feel's identifier is deduced from the name, which is the convention: the
     * {@code Button} region is drawn with whatever is under the key {@code ButtonUI}. A subregion
     * does not have one, because it is not drawn by a look and feel of its own.
     */
    private Region(String name, boolean subregion) {
        this(name, subregion ? null : name + "UI", subregion);
    }

    /**
     * A region with that name and that look and feel class.
     *
     * @param name the name
     * @param ui the look and feel's key, or {@code null} if it has none
     * @param subregion whether it is part of another component
     * @throws NullPointerException if {@code name} is {@code null}
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
     * Whether it is part of another component.
     *
     * @return true if it is a subregion
     */
    public boolean isSubregion() {
        return subregion;
    }

    /**
     * The name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * The region whose look and feel is called that, or {@code null}.
     *
     * <p>It is not part of the API. The search is linear over fifty-seven constants and is done
     * once per component on installing, not per repaint: building a map would cost more than it
     * saves.
     */
    static Region byUI(String ui) {
        if (ui == null) {
            return null;
        }
        for (int i = 0; i < ALL.length; i++) {
            if (ui.equals(ALL[i].ui)) {
                return ALL[i];
            }
        }
        return null;
    }

    /**
     * The name.
     *
     * @return the name
     */
    @Override
    public String toString() {
        return name;
    }
}
