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
 * The Metal look and feel, the only one Java ships that looks the same everywhere.
 *
 * <h2>Forty static methods that keep nothing</h2>
 *
 * <p>Almost all of this class is a facade over the theme: {@link #getControl} calls
 * {@code getCurrentTheme().getControl()} and does nothing else. They are there because the
 * thirty-odd {@code Metal*UI} classes ask for colours all the time, and writing
 * {@code MetalLookAndFeel.getControlShadow()} instead of looking up the theme is the difference
 * between reading and not reading.
 *
 * <p>The theme is <strong>static</strong>: there is one per virtual machine and
 * {@link #setCurrentTheme} changes it for everybody. That is what allows a program to change
 * palette with one line, and also the reason two windows with different themes cannot be had.
 *
 * <h2>The look and feel is assembled in three steps</h2>
 *
 * <p>{@link #initClassDefaults} says which class draws each component,
 * {@link #initSystemColorDefaults} sets the system colour names
 * -- {@code "control"}, {@code "window"}, {@code "menuText"} -- and
 * {@link #initComponentDefaults} sets everything else. The three write over the same table and
 * in that order, so the last one can overwrite the first.
 *
 * <p>What is notable about {@code initClassDefaults} is that **twenty-three** of the
 * forty-three entries point at classes of {@code plaf.basic} and not at Metal classes. Metal
 * does not redefine what it does not need to change: a list, a table, a context menu look the
 * same, and writing a {@code MetalListUI} that added nothing would be one more class at every
 * start-up.
 *
 * <h2>What is said and not covered up</h2>
 *
 * <p>{@link #initComponentDefaults} sets the entries derived from the theme -- colours, fonts,
 * margins, borders -- but not the JDK's thousand-odd: the keyboard shortcuts are missing, which
 * need the complete list of actions per component, and the icon entries, which are images from
 * the jar. The JDK's table has 642 entries and this one many fewer; those that are there are the
 * same.
 *
 * <p>{@link #getSupportsWindowDecorations} answers yes -- it is what the JDK answers -- even
 * though really decorating a window needs a desktop this VM does not have.
 */
public class MetalLookAndFeel extends BasicLookAndFeel {

    private static MetalTheme currentTheme;
    private static LayoutStyle style;

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

    /** No: Metal imitates no system, and that is the point. */
    public boolean isNativeLookAndFeel() {
        return false;
    }

    /** Always; Metal runs wherever Java runs. */
    public boolean isSupportedLookAndFeel() {
        return true;
    }

    public boolean getSupportsWindowDecorations() {
        return true;
    }

    /** Ocean; ver {@link OceanTheme}. */
    protected void createDefaultTheme() {
        if (currentTheme == null) {
            setCurrentTheme(new OceanTheme());
        }
    }

    /**
     * It changes the theme of the whole virtual machine.
     *
     * @param theme the theme
     * @throws NullPointerException if it is null; the message is the JDK's and is measured
     */
    public static void setCurrentTheme(MetalTheme theme) {
        if (theme == null) {
            throw new NullPointerException("Can't have null theme");
        }
        currentTheme = theme;
    }

    /** The theme; if nobody set one, Ocean. */
    public static MetalTheme getCurrentTheme() {
        if (currentTheme == null) {
            currentTheme = new OceanTheme();
        }
        return currentTheme;
    }

    /**
     * Whether the current theme is Ocean.
     *
     * <p>A handful of Metal classes draw themselves differently with Ocean than with Steel, and
     * this is the question they ask. The most visible is the combo box: with Steel it is a whole
     * button and with Ocean it is a field with an arrow beside it.
     */
    static boolean usingOcean() {
        return getCurrentTheme() instanceof OceanTheme;
    }

    // ---- the theme's facade ----

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
     * A colour from Metal's table, by its key.
     *
     * <p>It looks first at the installed look and feel and, if there is none, at the table this
     * look and feel would generate. The {@code Metal*UI} classes use it to take their colours with
     * their component's prefix, and from there comes a difference that is surprising and is
     * measured: {@code "CheckBox.focus"} exists and {@code "CheckBox.select"} does not, so a
     * check box has a focus colour and has no selection colour. It is not an oversight -- a check
     * box is not filled when chosen, a tick is drawn on it -- but it is only seen by looking at the
     * table.
     *
     * @param key the key, with a prefix
     * @return the colour, or {@code null} if that key is not there
     */
    static java.awt.Color tableColor(String key) {
        java.awt.Color c = javax.swing.UIManager.getColor(key);
        if (c != null) {
            return c;
        }
        Object o = ownTable().get(key);
        return (o instanceof java.awt.Color) ? (java.awt.Color) o : null;
    }

    private static UIDefaults ownTable;
    private static MetalTheme tableTheme;

    /** This look and feel's table, rebuilt if the theme changed. */
    private static UIDefaults ownTable() {
        MetalTheme t = getCurrentTheme();
        if (ownTable == null || tableTheme != t) {
            UIDefaults table = new UIDefaults();
            new MetalLookAndFeel().initComponentDefaults(table);
            // And what the theme adds on top, which is where the tabs' colours come from.
            t.addCustomEntriesToTable(table);
            ownTable = table;
            tableTheme = t;
        }
        return ownTable;
    }

    // ---- the table ----

    public UIDefaults getDefaults() {
        createDefaultTheme();
        UIDefaults table = super.getDefaults();
        getCurrentTheme().addCustomEntriesToTable(table);
        return table;
    }

    /** Twenty of Metal's and twenty-three of the basic one's; see the class note. */
    protected void initClassDefaults(UIDefaults table) {
        super.initClassDefaults(table);
        String metal = "javax.swing.plaf.metal.Metal";
        String basic = "javax.swing.plaf.basic.Basic";
        Object[] pairs = {
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

            // What Metal does not change is drawn with the basic one; see the class note.
            "CheckBoxMenuItemUI", basic + "CheckBoxMenuItemUI",
            "ColorChooserUI", basic + "ColorChooserUI",
            "DesktopPaneUI", basic + "DesktopPaneUI",
            "EditorPaneUI", basic + "EditorPaneUI",
            "FormattedTextFieldUI", basic + "FormattedTextFieldUI",
            "ListUI", basic + "ListUI",
            "MenuItemUI", basic + "MenuItemUI",
            "MenuUI", basic + "MenuUI",
            "OptionPaneUI", basic + "OptionPaneUI",
            "PanelUI", basic + "PanelUI",
            "PasswordFieldUI", basic + "PasswordFieldUI",
            "PopupMenuUI", basic + "PopupMenuUI",
            "RadioButtonMenuItemUI", basic + "RadioButtonMenuItemUI",
            "SpinnerUI", basic + "SpinnerUI",
            "TableHeaderUI", basic + "TableHeaderUI",
            "TableUI", basic + "TableUI",
            "TextAreaUI", basic + "TextAreaUI",
            "TextPaneUI", basic + "TextPaneUI",
            "ToolBarSeparatorUI", basic + "ToolBarSeparatorUI",
            "ViewportUI", basic + "ViewportUI",
        };
        table.putDefaults(pairs);
    }

    /**
     * The system colour names, all taken from the theme.
     *
     * <p>A native look and feel would ask the desktop for them; Metal does not, and that is why it
     * looks the same everywhere. The twenty-six names are {@code java.awt.SystemColor}'s.
     */
    protected void initSystemColorDefaults(UIDefaults table) {
        MetalTheme t = getCurrentTheme();
        Object[] pairs = {
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
        table.putDefaults(pairs);
    }

    /** The colours, the fonts and the margins per component; see the class note. */
    protected void initComponentDefaults(UIDefaults table) {
        super.initComponentDefaults(table);
        MetalTheme t = getCurrentTheme();
        ColorUIResource control = t.getControl();
        ColorUIResource text = t.getControlTextColor();
        ColorUIResource disabledText = t.getInactiveControlTextColor();
        ColorUIResource window = t.getWindowBackground();
        ColorUIResource selection = t.getTextHighlightColor();
        ColorUIResource chosenText = t.getHighlightedTextColor();
        FontUIResource controlFont = t.getControlTextFont();
        FontUIResource systemFont = t.getSystemTextFont();
        FontUIResource userFont = t.getUserTextFont();
        FontUIResource menuFont = t.getMenuTextFont();
        FontUIResource smallFont = t.getSubTextFont();

        Object[] pairs = {
            "Button.background", control,
            "Button.foreground", text,
            "Button.disabledText", disabledText,
            "Button.font", controlFont,
            "Button.focus", t.getFocusColor(),
            "Button.select", t.getControlShadow(),
            "Button.margin", new java.awt.Insets(2, 14, 2, 14),

            "ToggleButton.background", control,
            "ToggleButton.foreground", text,
            "ToggleButton.disabledText", disabledText,
            "ToggleButton.font", controlFont,
            "ToggleButton.focus", t.getFocusColor(),
            "ToggleButton.select", t.getControlShadow(),

            "CheckBox.background", control,
            "CheckBox.foreground", text,
            "CheckBox.disabledText", disabledText,
            "CheckBox.font", controlFont,
            "CheckBox.focus", t.getFocusColor(),

            "RadioButton.background", control,
            "RadioButton.foreground", text,
            "RadioButton.disabledText", disabledText,
            "RadioButton.font", controlFont,
            "RadioButton.focus", t.getFocusColor(),
            "RadioButton.select", t.getPrimaryControl(),

            // "CheckBox.select" is not there, and it is not an oversight: a check box is not
                        // filled when chosen, a tick is drawn on it. Measured against the JDK's
                        // table.

            "Label.background", control,
            "Label.foreground", t.getSystemTextColor(),
            "Label.disabledForeground", disabledText,
            "Label.font", controlFont,

            "Panel.background", control,
            "Panel.foreground", t.getUserTextColor(),
            "Panel.font", controlFont,

            "Separator.background", t.getSeparatorBackground(),
            "Separator.foreground", t.getSeparatorForeground(),

            "TextField.background", window,
            "TextField.foreground", t.getUserTextColor(),
            "TextField.inactiveForeground", disabledText,
            "TextField.selectionBackground", selection,
            "TextField.selectionForeground", chosenText,
            "TextField.caretForeground", t.getUserTextColor(),
            "TextField.font", userFont,

            "TextArea.background", window,
            "TextArea.foreground", t.getUserTextColor(),
            "TextArea.selectionBackground", selection,
            "TextArea.selectionForeground", chosenText,
            "TextArea.font", userFont,

            "List.background", window,
            "List.foreground", t.getUserTextColor(),
            "List.selectionBackground", selection,
            "List.selectionForeground", chosenText,
            "List.font", controlFont,

            "Table.background", window,
            "Table.foreground", t.getUserTextColor(),
            "Table.selectionBackground", selection,
            "Table.selectionForeground", chosenText,
            "Table.font", userFont,

            "Tree.background", window,
            "Tree.foreground", t.getUserTextColor(),
            "Tree.textBackground", window,
            "Tree.textForeground", t.getUserTextColor(),
            "Tree.selectionBackground", selection,
            "Tree.selectionForeground", chosenText,
            "Tree.hash", t.getPrimaryControl(),
            "Tree.font", userFont,
            "Tree.rowHeight", Integer.valueOf(0),

            "Menu.background", t.getMenuBackground(),
            "Menu.foreground", t.getMenuForeground(),
            "Menu.selectionBackground", t.getMenuSelectedBackground(),
            "Menu.selectionForeground", t.getMenuSelectedForeground(),
            "Menu.disabledForeground", t.getMenuDisabledForeground(),
            "Menu.acceleratorForeground", t.getAcceleratorForeground(),
            "Menu.acceleratorSelectionForeground", t.getAcceleratorSelectedForeground(),
            "Menu.font", menuFont,
            "Menu.acceleratorFont", smallFont,

            "MenuBar.background", t.getMenuBackground(),
            "MenuBar.foreground", t.getMenuForeground(),
            "MenuBar.font", menuFont,

            "MenuItem.background", t.getMenuBackground(),
            "MenuItem.foreground", t.getMenuForeground(),
            "MenuItem.selectionBackground", t.getMenuSelectedBackground(),
            "MenuItem.selectionForeground", t.getMenuSelectedForeground(),
            "MenuItem.disabledForeground", t.getMenuDisabledForeground(),
            "MenuItem.acceleratorForeground", t.getAcceleratorForeground(),
            "MenuItem.font", menuFont,
            "MenuItem.acceleratorFont", smallFont,

            "PopupMenu.background", t.getMenuBackground(),
            "PopupMenu.foreground", t.getMenuForeground(),
            "PopupMenu.font", menuFont,

            "ScrollBar.background", control,
            "ScrollBar.foreground", control,
            "ScrollBar.track", t.getControlShadow(),
            "ScrollBar.thumb", t.getPrimaryControlShadow(),
            "ScrollBar.thumbShadow", t.getPrimaryControlDarkShadow(),
            "ScrollBar.thumbHighlight", t.getPrimaryControl(),
            "ScrollBar.width", Integer.valueOf(17),

            "ScrollPane.background", control,
            "ScrollPane.foreground", text,

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
            "TabbedPane.foreground", text,
            "TabbedPane.highlight", t.getControlHighlight(),
            "TabbedPane.shadow", t.getControlShadow(),
            "TabbedPane.darkShadow", t.getControlDarkShadow(),
            "TabbedPane.focus", t.getPrimaryControlDarkShadow(),
            "TabbedPane.font", controlFont,

            "ToolBar.background", t.getMenuBackground(),
            "ToolBar.foreground", t.getMenuForeground(),
            "ToolBar.font", menuFont,

            "ToolTip.background", t.getPrimaryControl(),
            "ToolTip.foreground", t.getPrimaryControlInfo(),
            "ToolTip.backgroundInactive", control,
            "ToolTip.foregroundInactive", t.getControlDarkShadow(),
            "ToolTip.font", systemFont,

            "ProgressBar.background", control,
            "ProgressBar.foreground", t.getPrimaryControlShadow(),
            "ProgressBar.selectionBackground", t.getPrimaryControlDarkShadow(),
            "ProgressBar.selectionForeground", control,
            "ProgressBar.font", controlFont,

            "ComboBox.background", control,
            "ComboBox.foreground", text,
            "ComboBox.buttonBackground", control,
            "ComboBox.buttonShadow", t.getControlShadow(),
            "ComboBox.buttonDarkShadow", t.getControlDarkShadow(),
            "ComboBox.buttonHighlight", t.getControlHighlight(),
            "ComboBox.selectionBackground", t.getPrimaryControlShadow(),
            "ComboBox.selectionForeground", t.getControlTextColor(),
            "ComboBox.font", controlFont,

            "InternalFrame.activeTitleBackground", t.getWindowTitleBackground(),
            "InternalFrame.activeTitleForeground", t.getWindowTitleForeground(),
            "InternalFrame.inactiveTitleBackground", t.getWindowTitleInactiveBackground(),
            "InternalFrame.inactiveTitleForeground", t.getWindowTitleInactiveForeground(),
            "InternalFrame.titleFont", t.getWindowTitleFont(),

            "Desktop.background", t.getDesktopColor(),

            "OptionPane.background", control,
            "OptionPane.foreground", text,
            "OptionPane.messageForeground", text,
            "OptionPane.font", controlFont,

            "Viewport.background", control,
            "Viewport.foreground", t.getUserTextColor(),
        };
        table.putDefaults(pairs);
    }

    /** Metal has no sounds of its own. */
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
        if (style == null) {
            style = new MetalLayoutStyle();
        }
        return style;
    }

    /**
     * The spacing Metal recommends.
     *
     * <p>They are the same numbers {@code LayoutStyle} uses with no look and feel installed, and it
     * is measured: six pixels between related things, twelve between unrelated ones, and twelve
     * against the container's edge.
     */
    private static class MetalLayoutStyle extends LayoutStyle {

        public int getPreferredGap(JComponent component1, JComponent component2,
                ComponentPlacement type, int position, Container parent) {
            // A null component comes out as NullPointerException; see LayoutStyle.
            component1.getWidth();
            component2.getWidth();
            if (type == null) {
                throw new NullPointerException("type");
            }
            checkPosition(position);
            if (type == ComponentPlacement.INDENT
                    && (position == SwingConstants.EAST || position == SwingConstants.WEST)) {
                return 12;
            }
            return (type == ComponentPlacement.UNRELATED) ? 12 : 6;
        }

        public int getContainerGap(JComponent component, int position, Container parent) {
            component.getWidth();
            checkPosition(position);
            return 12;
        }

        private static void checkPosition(int position) {
            if (position != SwingConstants.NORTH && position != SwingConstants.SOUTH
                    && position != SwingConstants.EAST && position != SwingConstants.WEST) {
                throw new IllegalArgumentException("Invalid position");
            }
        }
    }
}
