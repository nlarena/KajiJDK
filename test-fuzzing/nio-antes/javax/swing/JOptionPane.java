package javax.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.OptionPaneUI;

/**
 * The pane of the standard dialogs: notices, questions and requests for a datum.
 *
 * <h2>The pane and the dialog are different things</h2>
 *
 * <p>{@code JOptionPane} is a {@link JComponent}: the message, the icon and the buttons. The
 * {@code showXxxDialog} methods are a shortcut that builds the pane, puts it into a
 * {@link JDialog}, shows it and returns the answer. The shortcut can be skipped and the pane put
 * wherever one likes, which is what {@link #createDialog} exists for.
 *
 * <h2>The four axes</h2>
 *
 * <p>The <em>message</em> is what is said. The <em>message type</em>
 * ({@link #ERROR_MESSAGE} and company) chooses the icon. The <em>option type</em>
 * ({@link #YES_NO_OPTION} and company) chooses the buttons. The <em>options</em> replace them
 * with ones of one's own. They are independent: one may have an error icon with yes and no
 * buttons.
 *
 * <h2>What the shortcuts return</h2>
 *
 * <p>With standard buttons, the constant of the button that was pressed. With options of one's
 * own, the <em>index</em> in the array. And if the user closed the window without pressing
 * anything, {@link #CLOSED_OPTION}, which holds the same as {@link #PLAIN_MESSAGE} and as
 * {@link #DEFAULT_OPTION} but does not mean the same: confusing them is the classic mistake
 * with this class. One always has to compare against the constant that corresponds to the
 * option type that was asked for.
 *
 * <h2>Here nothing blocks</h2>
 *
 * <p>In the real JDK, {@code showConfirmDialog} does not return until the user answers, because
 * the modal dialog stacks an event loop. This library does not hand out window events: as in
 * {@link java.awt.Dialog}, {@code setVisible} returns at once. The shortcuts therefore build
 * everything, show it and return {@link #CLOSED_OPTION} (or null, the text input ones), which
 * is what corresponds to a dialog that closed without anything being chosen.
 *
 * <p>It is said here and not disguised: a method that returned an invented
 * {@link #YES_OPTION} would be much worse than one that tells the truth.
 */
public class JOptionPane extends JComponent implements Accessible {

    private static final String uiClassID = "OptionPaneUI";

    /**
     * The value the pane has before the user chooses.
     *
     * <p>A sentinel of its own is needed because null is a legitimate answer, and because the pane
     * has to tell "it has not answered yet" from "it answered null".
     */
    public static final Object UNINITIALIZED_VALUE = "uninitializedValue";

    /** A single button, the one the look and feel proposes. */
    public static final int DEFAULT_OPTION = -1;

    /** Yes and No. */
    public static final int YES_NO_OPTION = 0;

    /** Yes, No and Cancel. */
    public static final int YES_NO_CANCEL_OPTION = 1;

    /** OK and Cancel. */
    public static final int OK_CANCEL_OPTION = 2;

    /** They pressed Yes. */
    public static final int YES_OPTION = 0;

    /** They pressed No. */
    public static final int NO_OPTION = 1;

    /** They pressed Cancel. */
    public static final int CANCEL_OPTION = 2;

    /** They pressed OK. */
    public static final int OK_OPTION = 0;

    /** They closed the window without choosing; see the class note. */
    public static final int CLOSED_OPTION = -1;

    /** Error icon. */
    public static final int ERROR_MESSAGE = 0;

    /** Information icon. */
    public static final int INFORMATION_MESSAGE = 1;

    /** Warning icon. */
    public static final int WARNING_MESSAGE = 2;

    /** Question icon. */
    public static final int QUESTION_MESSAGE = 3;

    /** No icon. */
    public static final int PLAIN_MESSAGE = -1;

    public static final String ICON_PROPERTY = "icon";
    public static final String MESSAGE_PROPERTY = "message";
    public static final String VALUE_PROPERTY = "value";
    public static final String OPTIONS_PROPERTY = "options";
    public static final String INITIAL_VALUE_PROPERTY = "initialValue";
    public static final String MESSAGE_TYPE_PROPERTY = "messageType";
    public static final String OPTION_TYPE_PROPERTY = "optionType";
    public static final String SELECTION_VALUES_PROPERTY = "selectionValues";
    public static final String INITIAL_SELECTION_VALUE_PROPERTY = "initialSelectionValue";
    public static final String INPUT_VALUE_PROPERTY = "inputValue";
    public static final String WANTS_INPUT_PROPERTY = "wantsInput";

    /** The icon; if it is null the look and feel chooses it according to the message type. */
    protected transient Icon icon;

    /** The message. */
    protected transient Object message;

    /** The buttons of one's own, or null for the standard ones. */
    protected transient Object[] options;

    /** Which one starts with the focus. */
    protected transient Object initialValue;

    /** Which icon goes. */
    protected int messageType;

    /** Which buttons go. */
    protected int optionType;

    /** What the user chose; see {@link #UNINITIALIZED_VALUE}. */
    protected transient Object value;

    /** The list's options, when a datum from a set is asked for. */
    protected transient Object[] selectionValues;

    /** What the user typed or chose from the list. */
    protected transient Object inputValue;

    /** Which one comes chosen from the start in the list. */
    protected transient Object initialSelectionValue;

    /** Whether besides the message a datum is asked for. */
    protected boolean wantsInput;

    private static Frame rootFrame = null;

    /** A pane with a test message, which is what the JDK shows. */
    public JOptionPane() {
        this("JOptionPane message");
    }

    /** A pane with that message. */
    public JOptionPane(Object message) {
        this(message, PLAIN_MESSAGE);
    }

    /** With that message and that icon. */
    public JOptionPane(Object message, int messageType) {
        this(message, messageType, DEFAULT_OPTION);
    }

    /** With that message, that icon and those buttons. */
    public JOptionPane(Object message, int messageType, int optionType) {
        this(message, messageType, optionType, null);
    }

    /** With an icon of one's own. */
    public JOptionPane(Object message, int messageType, int optionType, Icon icon) {
        this(message, messageType, optionType, icon, null);
    }

    /** With buttons of one's own. */
    public JOptionPane(Object message, int messageType, int optionType, Icon icon,
            Object[] options) {
        this(message, messageType, optionType, icon, options, null);
    }

    /**
     * The complete constructor.
     *
     * @throws RuntimeException if the message type or the option one are not valid.
     */
    public JOptionPane(Object message, int messageType, int optionType, Icon icon,
            Object[] options, Object initialValue) {
        this.message = message;
        this.options = options;
        this.initialValue = initialValue;
        this.icon = icon;
        setMessageType(messageType);
        setOptionType(optionType);
        value = UNINITIALIZED_VALUE;
        inputValue = UNINITIALIZED_VALUE;
        updateUI();
    }

    /**
     * It shows a request for text over the usual window.
     *
     * @return what they typed, or null; see the class note.
     * @throws HeadlessException if there is no screen.
     */
    public static String showInputDialog(Object message) throws HeadlessException {
        return showInputDialog(null, message);
    }

    /** With that text already set. */
    public static String showInputDialog(Object message, Object initialSelectionValue) {
        return showInputDialog(null, message, initialSelectionValue);
    }

    /**
     * Over that window.
     *
     * @throws HeadlessException if there is no screen.
     */
    public static String showInputDialog(Component parentComponent, Object message)
            throws HeadlessException {
        return showInputDialog(parentComponent, message, "Input", QUESTION_MESSAGE);
    }

    /** Over that window and with that text already set. */
    public static String showInputDialog(Component parentComponent, Object message,
            Object initialSelectionValue) {
        return (String) showInputDialog(parentComponent, message, "Input", QUESTION_MESSAGE, null,
                null, initialSelectionValue);
    }

    /**
     * With a title and an icon of one's own.
     *
     * @throws HeadlessException if there is no screen.
     */
    public static String showInputDialog(Component parentComponent, Object message, String title,
            int messageType) throws HeadlessException {
        return (String) showInputDialog(parentComponent, message, title, messageType, null, null,
                null);
    }

    /**
     * The complete request: one may choose from a list instead of typing.
     *
     * <p>If {@code selectionValues} is not null, the look and feel puts a list and not a text
     * field.
     *
     * @return what was chosen, or null; see the class note.
     * @throws HeadlessException if there is no screen.
     */
    public static Object showInputDialog(Component parentComponent, Object message, String title,
            int messageType, Icon icon, Object[] selectionValues, Object initialSelectionValue)
            throws HeadlessException {
        JOptionPane pane = new JOptionPane(message, messageType, OK_CANCEL_OPTION, icon, null,
                null);
        pane.setWantsInput(true);
        pane.setSelectionValues(selectionValues);
        pane.setInitialSelectionValue(initialSelectionValue);
        JDialog dialog = pane.createDialog(parentComponent, title);
        pane.selectInitialValue();
        dialog.setVisible(true);
        dialog.dispose();
        Object value = pane.getInputValue();
        if (value == UNINITIALIZED_VALUE) {
            return null;
        }
        return value;
    }

    /**
     * It shows a notice.
     *
     * @throws HeadlessException if there is no screen.
     */
    public static void showMessageDialog(Component parentComponent, Object message)
            throws HeadlessException {
        showMessageDialog(parentComponent, message, "Message", INFORMATION_MESSAGE);
    }

    /**
     * With a title and an icon type.
     *
     * @throws HeadlessException if there is no screen.
     */
    public static void showMessageDialog(Component parentComponent, Object message, String title,
            int messageType) throws HeadlessException {
        showMessageDialog(parentComponent, message, title, messageType, null);
    }

    /**
     * With an icon of one's own.
     *
     * @throws HeadlessException if there is no screen.
     */
    public static void showMessageDialog(Component parentComponent, Object message, String title,
            int messageType, Icon icon) throws HeadlessException {
        showOptionDialog(parentComponent, message, title, DEFAULT_OPTION, messageType, icon, null,
                null);
    }

    /**
     * It asks Yes / No / Cancel.
     *
     * @return the button's constant, or {@link #CLOSED_OPTION}; see the class note.
     * @throws HeadlessException if there is no screen.
     */
    public static int showConfirmDialog(Component parentComponent, Object message)
            throws HeadlessException {
        return showConfirmDialog(parentComponent, message, "Select an Option",
                YES_NO_CANCEL_OPTION);
    }

    /**
     * With a title and a set of buttons.
     *
     * @throws HeadlessException if there is no screen.
     */
    public static int showConfirmDialog(Component parentComponent, Object message, String title,
            int optionType) throws HeadlessException {
        return showConfirmDialog(parentComponent, message, title, optionType, QUESTION_MESSAGE);
    }

    /**
     * With an icon type.
     *
     * @throws HeadlessException if there is no screen.
     */
    public static int showConfirmDialog(Component parentComponent, Object message, String title,
            int optionType, int messageType) throws HeadlessException {
        return showConfirmDialog(parentComponent, message, title, optionType, messageType, null);
    }

    /**
     * With an icon of one's own.
     *
     * @throws HeadlessException if there is no screen.
     */
    public static int showConfirmDialog(Component parentComponent, Object message, String title,
            int optionType, int messageType, Icon icon) throws HeadlessException {
        return showOptionDialog(parentComponent, message, title, optionType, messageType, icon,
                null, null);
    }

    /**
     * The complete dialog, with buttons of one's own.
     *
     * @return the index in {@code options} of the button that was pressed, or its constant if
     *     {@code options} is null, or {@link #CLOSED_OPTION}.
     * @throws HeadlessException if there is no screen.
     */
    public static int showOptionDialog(Component parentComponent, Object message, String title,
            int optionType, int messageType, Icon icon, Object[] options, Object initialValue)
            throws HeadlessException {
        JOptionPane pane = new JOptionPane(message, messageType, optionType, icon, options,
                initialValue);
        pane.setInitialValue(initialValue);
        JDialog dialog = pane.createDialog(parentComponent, title);
        pane.selectInitialValue();
        dialog.setVisible(true);
        dialog.dispose();
        return translate(pane.getValue(), options);
    }

    /**
     * It turns the value that was left in the pane into the integer the shortcuts return.
     *
     * <p>With buttons of one's own the result is the index; with the standard ones, the integer
     * the look and feel set as the value. Anything else -- the "it did not answer" sentinel
     * included -- is {@link #CLOSED_OPTION}.
     */
    private static int translate(Object chosen, Object[] options) {
        if (chosen == null || chosen == UNINITIALIZED_VALUE) {
            return CLOSED_OPTION;
        }
        if (options == null) {
            if (chosen instanceof Integer) {
                return ((Integer) chosen).intValue();
            }
            return CLOSED_OPTION;
        }
        for (int i = 0; i < options.length; i++) {
            if (options[i] != null && options[i].equals(chosen)) {
                return i;
            }
        }
        return CLOSED_OPTION;
    }

    /**
     * It builds the dialog that contains this pane.
     *
     * <p>The dialog hides itself when the pane changes value: it is what makes pressing a button
     * close the window without the look and feel having to know it.
     *
     * @throws HeadlessException if there is no screen.
     */
    public JDialog createDialog(Component parentComponent, String title) throws HeadlessException {
        Window owner = null;
        if (parentComponent != null) {
            owner = SwingUtilities.getWindowAncestor(parentComponent);
        }
        JDialog dialog;
        if (owner instanceof java.awt.Dialog) {
            dialog = new JDialog((java.awt.Dialog) owner, title, true);
        } else if (owner instanceof Frame) {
            dialog = new JDialog((Frame) owner, title, true);
        } else {
            dialog = new JDialog((Frame) null, title, true);
        }
        buildDialog(dialog);
        return dialog;
    }

    /**
     * It builds the dialog over the usual window.
     *
     * @throws HeadlessException if there is no screen.
     */
    public JDialog createDialog(String title) throws HeadlessException {
        JDialog dialog = new JDialog((Frame) null, title, true);
        buildDialog(dialog);
        return dialog;
    }

    /** It puts the pane inside it and connects it to the value; see {@link #createDialog}. */
    private void buildDialog(JDialog dialog) {
        Container content = dialog.getContentPane();
        content.setLayout(new BorderLayout());
        content.add(this, BorderLayout.CENTER);
        dialog.setResizable(false);
        setValue(UNINITIALIZED_VALUE);
        dialog.addPropertyChangeListener(new ValueListener(this, dialog));
        addPropertyChangeListener(new ValueListener(this, dialog));
        dialog.pack();
    }

    /**
     * It hides the dialog as soon as the pane has a value.
     *
     * <p>It is a named class and not an anonymous one because the pane also registers it on
     * itself, and the event's source has to be able to be told apart.
     */
    private static class ValueListener implements PropertyChangeListener {

        private final JOptionPane panel;
        private final JDialog dialog;

        ValueListener(JOptionPane panel, JDialog dialog) {
            this.panel = panel;
            this.dialog = dialog;
        }

        public void propertyChange(PropertyChangeEvent event) {
            if (event.getSource() != panel) {
                return;
            }
            if (!dialog.isVisible()) {
                return;
            }
            String name = event.getPropertyName();
            boolean isValue = VALUE_PROPERTY.equals(name)
                    || INPUT_VALUE_PROPERTY.equals(name);
            if (isValue && event.getNewValue() != null
                    && event.getNewValue() != UNINITIALIZED_VALUE) {
                dialog.setVisible(false);
            }
        }
    }

    /** Like {@link #showMessageDialog} but with an internal frame. */
    public static void showInternalMessageDialog(Component parentComponent, Object message) {
        showInternalMessageDialog(parentComponent, message, "Message", INFORMATION_MESSAGE);
    }

    /** With a title and an icon type. */
    public static void showInternalMessageDialog(Component parentComponent, Object message,
            String title, int messageType) {
        showInternalMessageDialog(parentComponent, message, title, messageType, null);
    }

    /** With an icon of one's own. */
    public static void showInternalMessageDialog(Component parentComponent, Object message,
            String title, int messageType, Icon icon) {
        showInternalOptionDialog(parentComponent, message, title, DEFAULT_OPTION, messageType,
                icon, null, null);
    }

    /** Like {@link #showConfirmDialog} but with an internal frame. */
    public static int showInternalConfirmDialog(Component parentComponent, Object message) {
        return showInternalConfirmDialog(parentComponent, message, "Select an Option",
                YES_NO_CANCEL_OPTION);
    }

    /** With a title and a set of buttons. */
    public static int showInternalConfirmDialog(Component parentComponent, Object message,
            String title, int optionType) {
        return showInternalConfirmDialog(parentComponent, message, title, optionType,
                QUESTION_MESSAGE);
    }

    /** With an icon type. */
    public static int showInternalConfirmDialog(Component parentComponent, Object message,
            String title, int optionType, int messageType) {
        return showInternalConfirmDialog(parentComponent, message, title, optionType, messageType,
                null);
    }

    /** With an icon of one's own. */
    public static int showInternalConfirmDialog(Component parentComponent, Object message,
            String title, int optionType, int messageType, Icon icon) {
        return showInternalOptionDialog(parentComponent, message, title, optionType, messageType,
                icon, null, null);
    }

    /**
     * The complete internal dialog.
     *
     * @return the index of the button that was pressed, or {@link #CLOSED_OPTION}; see the class
     *     note.
     * @throws RuntimeException if the component is neither in a desktop nor has a parent.
     */
    public static int showInternalOptionDialog(Component parentComponent, Object message,
            String title, int optionType, int messageType, Icon icon, Object[] options,
            Object initialValue) {
        JOptionPane pane = new JOptionPane(message, messageType, optionType, icon, options,
                initialValue);
        pane.setInitialValue(initialValue);
        JInternalFrame frame = pane.createInternalFrame(parentComponent, title);
        pane.selectInitialValue();
        frame.setVisible(true);
        return translate(pane.getValue(), options);
    }

    /** Like {@link #showInputDialog} but with an internal frame. */
    public static String showInternalInputDialog(Component parentComponent, Object message) {
        return showInternalInputDialog(parentComponent, message, "Input", QUESTION_MESSAGE);
    }

    /** With a title and an icon type. */
    public static String showInternalInputDialog(Component parentComponent, Object message,
            String title, int messageType) {
        return (String) showInternalInputDialog(parentComponent, message, title, messageType, null,
                null, null);
    }

    /**
     * The complete internal request.
     *
     * @throws RuntimeException if the component is neither in a desktop nor has a parent.
     */
    public static Object showInternalInputDialog(Component parentComponent, Object message,
            String title, int messageType, Icon icon, Object[] selectionValues,
            Object initialSelectionValue) {
        JOptionPane pane = new JOptionPane(message, messageType, OK_CANCEL_OPTION, icon, null,
                null);
        pane.setWantsInput(true);
        pane.setSelectionValues(selectionValues);
        pane.setInitialSelectionValue(initialSelectionValue);
        JInternalFrame frame = pane.createInternalFrame(parentComponent, title);
        pane.selectInitialValue();
        frame.setVisible(true);
        Object value = pane.getInputValue();
        if (value == UNINITIALIZED_VALUE) {
            return null;
        }
        return value;
    }

    /**
     * It builds the internal frame that contains this pane.
     *
     * <p>It is put in the desktop's modal layer so that it stays above the other frames, which is
     * the nearest thing to modality there is inside a desktop.
     *
     * @throws RuntimeException if the component is neither in a desktop nor has a parent.
     */
    public JInternalFrame createInternalFrame(Component parentComponent, String title) {
        Container parent = getDesktopPaneForComponent(parentComponent);
        if (parent == null) {
            if (parentComponent == null) {
                throw new RuntimeException(
                        "JOptionPane: parentComponent does not have a valid parent");
            }
            parent = parentComponent.getParent();
            if (parent == null) {
                throw new RuntimeException(
                        "JOptionPane: parentComponent does not have a valid parent");
            }
        }
        // A dialog closes, it does not grow or shrink or resize.
        JInternalFrame frame = new JInternalFrame(title, false, true, false, false);
        frame.putClientProperty("JInternalFrame.frameType", "optionDialog");
        frame.putClientProperty("JInternalFrame.messageType", Integer.valueOf(getMessageType()));
        frame.getContentPane().add(this, BorderLayout.CENTER);
        if (parent instanceof JDesktopPane) {
            parent.add(frame, JLayeredPane.MODAL_LAYER);
        } else {
            parent.add(frame, BorderLayout.CENTER);
        }
        Dimension measured = frame.getPreferredSize();
        frame.setBounds(0, 0, measured.width, measured.height);
        parent.validate();
        try {
            frame.setSelected(true);
        } catch (PropertyVetoException e) {
            // Its not being able to be activated does not prevent showing it.
        }
        return frame;
    }

    /**
     * The system window that contains that component.
     *
     * <p>If there is none, the usual window: a dialog has to hang from something.
     *
     * @throws HeadlessException if there is no screen and the usual window is needed.
     */
    public static Frame getFrameForComponent(Component parentComponent) throws HeadlessException {
        if (parentComponent == null) {
            return getRootFrame();
        }
        if (parentComponent instanceof Frame) {
            return (Frame) parentComponent;
        }
        return getFrameForComponent(parentComponent.getParent());
    }

    /** The desktop that contains that component, or null. */
    public static JDesktopPane getDesktopPaneForComponent(Component parentComponent) {
        if (parentComponent == null) {
            return null;
        }
        if (parentComponent instanceof JDesktopPane) {
            return (JDesktopPane) parentComponent;
        }
        return getDesktopPaneForComponent(parentComponent.getParent());
    }

    /** It fixes the window the dialogs with no parent hang from. */
    public static void setRootFrame(Frame newRootFrame) {
        rootFrame = newRootFrame;
    }

    /**
     * The window the dialogs with no parent hang from.
     *
     * @throws HeadlessException if there is no screen.
     */
    public static Frame getRootFrame() throws HeadlessException {
        if (rootFrame == null) {
            rootFrame = new Frame();
        }
        return rootFrame;
    }

    public void setUI(OptionPaneUI ui) {
        super.setUI(ui);
    }

    public OptionPaneUI getUI() {
        return (OptionPaneUI) ui;
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * The message.
     *
     * <p>It does not have to be text: a {@link Component} is shown as it is, and an array is
     * stacked line by line. It is what allows a password field to be put inside a notice without
     * writing a whole dialog.
     */
    public void setMessage(Object newMessage) {
        Object oldMessage = message;
        message = newMessage;
        firePropertyChange(MESSAGE_PROPERTY, oldMessage, message);
    }

    public Object getMessage() {
        return message;
    }

    /** The icon; null lets the look and feel choose it according to the message type. */
    public void setIcon(Icon newIcon) {
        Object oldIcon = icon;
        icon = newIcon;
        firePropertyChange(ICON_PROPERTY, oldIcon, icon);
    }

    public Icon getIcon() {
        return icon;
    }

    /**
     * What the user chose.
     *
     * <p>Setting it is what closes the dialog: whoever builds the dialog listens to this
     * property.
     */
    public void setValue(Object newValue) {
        Object oldValue = value;
        value = newValue;
        firePropertyChange(VALUE_PROPERTY, oldValue, value);
    }

    /** What was chosen, or {@link #UNINITIALIZED_VALUE} if it has not answered yet. */
    public Object getValue() {
        return value;
    }

    /** The buttons of one's own; null leaves the standard ones. */
    public void setOptions(Object[] newOptions) {
        Object[] oldOptions = options;
        options = newOptions;
        firePropertyChange(OPTIONS_PROPERTY, oldOptions, options);
    }

    /** A copy of the buttons of one's own, or null. */
    public Object[] getOptions() {
        if (options != null) {
            int optionCount = options.length;
            Object[] retOptions = new Object[optionCount];
            System.arraycopy(options, 0, retOptions, 0, optionCount);
            return retOptions;
        }
        return options;
    }

    /** Which button starts with the focus. */
    public void setInitialValue(Object newInitialValue) {
        Object oldIV = initialValue;
        initialValue = newInitialValue;
        firePropertyChange(INITIAL_VALUE_PROPERTY, oldIV, initialValue);
    }

    public Object getInitialValue() {
        return initialValue;
    }

    /**
     * Which icon goes.
     *
     * @throws RuntimeException if it is not one of the five types.
     */
    public void setMessageType(int newType) {
        if (newType != ERROR_MESSAGE && newType != INFORMATION_MESSAGE
                && newType != WARNING_MESSAGE && newType != QUESTION_MESSAGE
                && newType != PLAIN_MESSAGE) {
            throw new RuntimeException("JOptionPane: type must be one of JOptionPane.ERROR_MESSAGE,"
                    + " JOptionPane.INFORMATION_MESSAGE, JOptionPane.WARNING_MESSAGE,"
                    + " JOptionPane.QUESTION_MESSAGE or JOptionPane.PLAIN_MESSAGE");
        }
        int oldType = messageType;
        messageType = newType;
        firePropertyChange(MESSAGE_TYPE_PROPERTY, oldType, messageType);
    }

    public int getMessageType() {
        return messageType;
    }

    /**
     * Which buttons go.
     *
     * @throws RuntimeException if it is not one of the four sets.
     */
    public void setOptionType(int newType) {
        if (newType != DEFAULT_OPTION && newType != YES_NO_OPTION
                && newType != YES_NO_CANCEL_OPTION && newType != OK_CANCEL_OPTION) {
            throw new RuntimeException("JOptionPane: option type must be one of"
                    + " JOptionPane.DEFAULT_OPTION, JOptionPane.YES_NO_OPTION,"
                    + " JOptionPane.YES_NO_CANCEL_OPTION or JOptionPane.OK_CANCEL_OPTION");
        }
        int oldType = optionType;
        optionType = newType;
        firePropertyChange(OPTION_TYPE_PROPERTY, oldType, optionType);
    }

    public int getOptionType() {
        return optionType;
    }

    /**
     * The list's options.
     *
     * <p>Setting them switches {@link #setWantsInput} on: a list to choose from makes no sense if
     * a datum is not being asked for.
     */
    public void setSelectionValues(Object[] newValues) {
        Object[] oldValues = selectionValues;
        selectionValues = newValues;
        firePropertyChange(SELECTION_VALUES_PROPERTY, oldValues, newValues);
        if (selectionValues != null) {
            setWantsInput(true);
        }
    }

    public Object[] getSelectionValues() {
        return selectionValues;
    }

    public void setInitialSelectionValue(Object newValue) {
        Object oldValue = initialSelectionValue;
        initialSelectionValue = newValue;
        firePropertyChange(INITIAL_SELECTION_VALUE_PROPERTY, oldValue, newValue);
    }

    public Object getInitialSelectionValue() {
        return initialSelectionValue;
    }

    /** What the user typed or chose; the look and feel sets it. */
    public void setInputValue(Object newValue) {
        Object oldValue = inputValue;
        inputValue = newValue;
        firePropertyChange(INPUT_VALUE_PROPERTY, oldValue, newValue);
    }

    public Object getInputValue() {
        return inputValue;
    }

    /**
     * How many characters fit in a line of the message.
     *
     * <p>With {@link Integer#MAX_VALUE} it is never cut, which is what the base look and feel
     * returns: cutting is a presentation decision and each look and feel takes it on its own.
     */
    public int getMaxCharactersPerLineCount() {
        return Integer.MAX_VALUE;
    }

    /** Whether besides the message a datum is asked for. */
    public void setWantsInput(boolean newValue) {
        boolean oldValue = wantsInput;
        wantsInput = newValue;
        firePropertyChange(WANTS_INPUT_PROPERTY, oldValue, newValue);
    }

    public boolean getWantsInput() {
        return wantsInput;
    }

    /** It asks the look and feel to give the focus to the initial value. */
    public void selectInitialValue() {
        OptionPaneUI ui = getUI();
        if (ui != null) {
            ui.selectInitialValue(this);
        }
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
