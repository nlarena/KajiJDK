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
 * The basic look and feel of an option pane -- a dialog box's content --.
 *
 * <h2>Three strips, and the middle one is the interesting one</h2>
 *
 * <p>From top to bottom: the message with its icon, the input component if there is one, and
 * the buttons. The only thing that is not trivial is the message, because it may be anything: a
 * text, an icon, a component, or an array of all that mixed up.
 * {@link #addMessageComponents} takes it apart recursively and {@link #burstStringInto} cuts
 * long text into lines.
 *
 * <h2>The minimum size is not computed</h2>
 *
 * <p>It is 262 x 90, written down. A dialog smaller than that looks like a mistake even if its
 * content fits, and the number does not depend on anything that can be measured. It is in
 * {@link #MinimumWidth} and {@link #MinimumHeight}, which are public precisely so that a look
 * and feel can look at them.
 *
 * <h2>The buttons are not buttons yet</h2>
 *
 * <p>{@link #getButtons} does not return {@code JButton}s: it returns button
 * <em>descriptions</em>. The difference matters because the option pane accepts being passed
 * any object as an option -- a string, an icon, a ready-made component -- and who decides how
 * it turns into something pressable is {@link #addButtonComponents}, not this list.
 *
 * <p>{@link #getSizeButtonsToSameWidth} says yes: every button of a dialog measures the same,
 * even though "Yes" is much shorter than "Cancel".
 *
 * <h2>No separator</h2>
 *
 * <p>{@link #createSeparator} returns {@code null}. It is a hook for the look and feel that
 * wants a line between the message and the buttons; the basic one does not draw it. Measured.
 *
 * <h2>What is left said</h2>
 *
 * <p>The icons of the four message types -- information, question, warning, error -- are 32 x 32
 * images that come from the look and feel's table. With no table there is none, so
 * {@link #getIconForType} returns {@code null} and the dialog measures less in width than the
 * JDK's. It is the same gap as always and it changes nothing of the structure.
 */
public class BasicOptionPaneUI extends OptionPaneUI {

    /** A dialog's minimum width; see the class note. */
    public static final int MinimumWidth = 262;

    /** And the height. */
    public static final int MinimumHeight = 90;

    protected JOptionPane optionPane;
    protected Dimension minimumSize;

    /** The component where the user types, if the dialog asks for something. */
    protected JComponent inputComponent;

    /** The one that takes the focus on opening. */
    protected Component initialFocusComponent;

    /**
     * Whether the message brought components of its own; whether the dialog can be reused depends
     * on that.
     */
    protected boolean hasCustomComponents;

    protected PropertyChangeListener propertyChangeListener;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.PLAIN, 12);

    public BasicOptionPaneUI() {
    }

    /** A new one per pane: it keeps the component and what it built inside. */
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

    /**
     * Colours, typeface, border and the minimum size; the values are those of {@code OptionPane.*}.
     */
    protected void installDefaults() {
        Color background = optionPane.getBackground();
        if (background == null || background instanceof UIResource) {
            optionPane.setBackground(BACKGROUND);
        }
        Color foreground = optionPane.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            optionPane.setForeground(FOREGROUND);
        }
        Font font = optionPane.getFont();
        if (font == null || font instanceof UIResource) {
            optionPane.setFont(FONT);
        }
        javax.swing.border.Border b = optionPane.getBorder();
        if (b == null || b instanceof UIResource) {
            optionPane.setBorder(new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(
                    0, 0, 0, 0));
        }
        minimumSize = new DimensionUIResource(MinimumWidth, MinimumHeight);
        LookAndFeel.installProperty(optionPane, "opaque", Boolean.TRUE);
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
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

    /** With no shortcuts of its own: Escape and Enter are tied by the dialog that contains it. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    /** A vertical one: the three strips, one below the other. */
    protected LayoutManager createLayoutManager() {
        return new BoxLayout(optionPane, BoxLayout.Y_AXIS);
    }

    /** It builds the three strips; see the class note. */
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

    /** The message's strip: the icon on the left and the message on the right. */
    protected Container createMessageArea() {
        JPanel top = new JPanel();
        top.setBorder(new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(0, 0, 0, 0));
        top.setLayout(new MessageBorderLayout());
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

    /** It gives it the message type's icon, if there is one; see the class note. */
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
     * It takes the message apart and adds it to the strip.
     *
     * <p>An array is walked through; a component is added as it is and it is noted that the
     * message brought things of its own; an icon is wrapped in a label; and anything else is
     * converted to text and cut into lines.
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
     * It cuts that text into lines and adds them one below the other.
     *
     * <p>It cuts at line breaks and, if a line goes over {@code maxll}, also at the last space
     * that fits. With the maximum at infinity -- which is the default -- it only cuts at breaks.
     */
    protected void burstStringInto(Container c, String d, int maxll) {
        int nl = d.indexOf('\n');
        if (nl >= 0) {
            burstStringInto(c, d.substring(0, nl), maxll);
            burstStringInto(c, d.substring(nl + 1), maxll);
            return;
        }
        if (d.length() > maxll && maxll > 0) {
            int cut = d.lastIndexOf(' ', maxll);
            if (cut <= 0) {
                cut = maxll;
            }
            burstStringInto(c, d.substring(0, cut), maxll);
            burstStringInto(c, d.substring(cut).trim(), maxll);
            return;
        }
        JLabel label = new JLabel(d, SwingConstants.LEADING);
        label.setName("OptionPane.label");
        c.add(label);
    }

    /** {@code null}; see the class note. */
    protected Container createSeparator() {
        return null;
    }

    /** The buttons' strip, all of the same width. */
    protected Container createButtonArea() {
        JPanel bottom = new JPanel();
        bottom.setName("OptionPane.buttonArea");
        bottom.setBorder(new javax.swing.plaf.BorderUIResource.EmptyBorderUIResource(6, 0, 0, 0));
        bottom.setLayout(new ButtonLayout(getSizeButtonsToSameWidth()));
        addButtonComponents(bottom, getButtons(), getInitialValueIndex());
        return bottom;
    }

    /**
     * It turns each option into something pressable and adds it.
     *
     * <p>An option that is already a component is added as it is; anything else becomes a button
     * that, when pressed, passes the value on to the pane. That is the point where the dialog is
     * closed, and that is why the listener knows which option is which.
     */
    protected void addButtonComponents(Container container, Object[] buttons,
            int initialIndex) {
        if (buttons == null) {
            return;
        }
        for (int i = 0; i < buttons.length; i++) {
            Object option = buttons[i];
            Component button;
            if (option instanceof Component) {
                button = (Component) option;
                hasCustomComponents = true;
            } else if (option instanceof ButtonDescription) {
                ButtonDescription d = (ButtonDescription) option;
                JButton b = new JButton(d.text);
                b.setName("OptionPane.button");
                if (d.icon != null) {
                    b.setIcon(d.icon);
                }
                if (d.mnemonic != 0) {
                    b.setMnemonic(d.mnemonic);
                }
                b.addActionListener(createButtonActionListener(i));
                button = b;
            } else if (option instanceof Icon) {
                JButton b = new JButton((Icon) option);
                b.setName("OptionPane.button");
                b.addActionListener(createButtonActionListener(i));
                button = b;
            } else {
                JButton b = new JButton(option.toString());
                b.setName("OptionPane.button");
                b.addActionListener(createButtonActionListener(i));
                button = b;
            }
            container.add(button);
            if (i == initialIndex) {
                initialFocusComponent = button;
            }
        }
    }

    /** The one that passes that option's value on to the pane. */
    protected ActionListener createButtonActionListener(int buttonIndex) {
        return new ButtonAction(this, buttonIndex);
    }

    /** The options: the ones the program set, or the ones that correspond to the dialog's type. */
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
                new ButtonDescription("Yes", 'Y'),
                new ButtonDescription("No", 'N'),
            };
        }
        if (type == JOptionPane.YES_NO_CANCEL_OPTION) {
            return new Object[] {
                new ButtonDescription("Yes", 'Y'),
                new ButtonDescription("No", 'N'),
                new ButtonDescription("Cancel", 'C'),
            };
        }
        if (type == JOptionPane.OK_CANCEL_OPTION) {
            return new Object[] {
                new ButtonDescription("OK", 'O'),
                new ButtonDescription("Cancel", 'C'),
            };
        }
        return new Object[] {new ButtonDescription("OK", 'O')};
    }

    /** Yes; see the class note. */
    protected boolean getSizeButtonsToSameWidth() {
        return true;
    }

    /** Which option takes the focus on opening. */
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

    /** The message that has to be shown. */
    protected Object getMessage() {
        inputComponent = null;
        if (optionPane != null) {
            return optionPane.getMessage();
        }
        return null;
    }

    /** The pane's icon, or the one that corresponds to its message type. */
    protected Icon getIcon() {
        Icon mIcon = (optionPane == null) ? null : optionPane.getIcon();
        if (mIcon == null && optionPane != null) {
            mIcon = getIconForType(optionPane.getMessageType());
        }
        return mIcon;
    }

    /** {@code null}; see the class note. */
    protected Icon getIconForType(int messageType) {
        return null;
    }

    /** Infinite: the basic one does not cut the text except at line breaks. Measured. */
    protected int getMaxCharactersPerLineCount() {
        return Integer.MAX_VALUE;
    }

    /** 262 x 90; see the class note. */
    public Dimension getMinimumOptionPaneSize() {
        if (minimumSize == null) {
            return new Dimension(MinimumWidth, MinimumHeight);
        }
        return new Dimension(minimumSize.width, minimumSize.height);
    }

    /** Whatever the content asks for, but never less than the minimum. */
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

    /** It gives the focus to the initial option. */
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

    /** It leaves the input component with whatever value the pane has. */
    protected void resetInputValue() {
        if (inputComponent instanceof javax.swing.JTextField) {
            ((javax.swing.JTextField) inputComponent).setText(
                    (String) optionPane.getInitialSelectionValue());
        }
    }

    /**
     * An option that is not a button yet; see the class note.
     *
     * <p>The JDK calls it {@code ButtonFactory} and it is private; here the name is descriptive
     * because it is not seen: what is seen of the array is its size, not its elements' type.
     */
    private static class ButtonDescription {

        final String text;
        final int mnemonic;
        final Icon icon;

        ButtonDescription(String text, char mnemonic) {
            this.text = text;
            this.mnemonic = mnemonic;
            this.icon = null;
        }
    }

    /** It passes the pressed option's value on to the pane. */
    private static class ButtonAction implements ActionListener {

        private final BasicOptionPaneUI ui;
        private final int index;

        ButtonAction(BasicOptionPaneUI ui, int index) {
            this.ui = ui;
            this.index = index;
        }

        public void actionPerformed(ActionEvent e) {
            JOptionPane op = ui.optionPane;
            if (op == null) {
                return;
            }
            Object[] buttons = ui.getButtons();
            Object value;
            if (op.getOptions() != null && index < op.getOptions().length) {
                value = op.getOptions()[index];
            } else if (buttons != null && index < buttons.length) {
                // With no options of its own, the value is the option's number, which is what
                                // `showConfirmDialog` returns.
                value = Integer.valueOf(index);
            } else {
                value = null;
            }
            op.setValue(value);
        }
    }

    /**
     * The message strip's layout: icon on the left, message in the centre.
     *
     * <p>It is a make-believe {@code BorderLayout} -- it only understands {@code "West"} and
     * {@code "Center"} --, and it is written because the real one stretches the centre
     * vertically and the icon has to stay at the top.
     */
    private static class MessageBorderLayout implements LayoutManager {

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
            int height = parent.getHeight() - in.top - in.bottom;
            if (oeste != null) {
                Dimension o = oeste.getPreferredSize();
                oeste.setBounds(x, in.top, o.width, height);
                x += o.width;
            }
            if (centro != null) {
                centro.setBounds(x, in.top, parent.getWidth() - in.right - x, height);
            }
        }
    }

    /** The buttons' layout: all of the same width and stuck to the right. */
    private static class ButtonLayout implements LayoutManager {

        private final boolean sameWidth;
        private final int gap = 6;

        ButtonLayout(boolean sameWidth) {
            this.sameWidth = sameWidth;
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        private int widthOfEach(Container parent) {
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
            int height = 0;
            int width = 0;
            int each = sameWidth ? widthOfEach(parent) : 0;
            for (int i = 0; i < n; i++) {
                Dimension d = parent.getComponent(i).getPreferredSize();
                height = Math.max(height, d.height);
                width += sameWidth ? each : d.width;
            }
            width += gap * (n - 1);
            Insets in = parent.getInsets();
            return new Dimension(width + in.left + in.right, height + in.top + in.bottom);
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
            int each = sameWidth ? widthOfEach(parent) : 0;
            Dimension pref = preferredLayoutSize(parent);
            int x = (parent.getWidth() - pref.width) / 2 + in.left;
            int height = pref.height - in.top - in.bottom;
            for (int i = 0; i < n; i++) {
                Component c = parent.getComponent(i);
                int w = sameWidth ? each : c.getPreferredSize().width;
                c.setBounds(x, in.top, w, height);
                x += w + gap;
            }
        }
    }

    /** It rebuilds the content when the message, the type or the options change. */
    private static class Handler implements PropertyChangeListener {

        private final BasicOptionPaneUI ui;

        Handler(BasicOptionPaneUI ui) {
            this.ui = ui;
        }

        public void propertyChange(PropertyChangeEvent e) {
            if (e.getSource() != ui.optionPane) {
                return;
            }
            String name = e.getPropertyName();
            if (JOptionPane.ICON_PROPERTY.equals(name)
                    || JOptionPane.MESSAGE_PROPERTY.equals(name)
                    || JOptionPane.OPTIONS_PROPERTY.equals(name)
                    || JOptionPane.INITIAL_VALUE_PROPERTY.equals(name)
                    || JOptionPane.MESSAGE_TYPE_PROPERTY.equals(name)
                    || JOptionPane.OPTION_TYPE_PROPERTY.equals(name)
                    || JOptionPane.WANTS_INPUT_PROPERTY.equals(name)
                    || JOptionPane.SELECTION_VALUES_PROPERTY.equals(name)) {
                ui.uninstallComponents();
                ui.installComponents();
                ui.optionPane.validate();
            } else if (JOptionPane.INITIAL_SELECTION_VALUE_PROPERTY.equals(name)) {
                ui.resetInputValue();
            }
        }
    }
}
