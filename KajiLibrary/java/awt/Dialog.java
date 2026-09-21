package java.awt;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * A window subordinate to another one: a dialog.
 *
 * <p>What sets it apart from a {@link Frame} is **modality**: a modal dialog blocks the other
 * windows while it is open, and {@link #setVisible} does not come back until it is closed. That
 * call which does not come back is what makes it possible to write a confirmation dialog as though
 * it were a function.
 *
 * <p>The scope of the blocking is declared with {@link ModalityType}, and it is not a detail:
 * blocking the whole application when blocking one document would have been enough is the
 * difference between an editor that lets one go on working in the other tabs and one that does not.
 *
 * <p>Exclusion is the other side: {@link ModalExclusionType} lets a window **not** be blocked. It
 * is what a progress bar or a log window needs, having to go on updating while a dialog is open.
 *
 * <p><strong>Here it blocks nothing.</strong> A modal dialog is implemented by stacking an event
 * loop that filters the user's input towards the other windows, and with no windowing system there
 * is no input to filter nor windows to block. {@link #setVisible} comes back right away, and the
 * modality state is kept and reported as it was asked for.
 */
public class Dialog extends Window {

    private static final long serialVersionUID = 5920926903803293709L;

    /** How much a modal dialog blocks. */
    public static enum ModalityType {

        /** It blocks nothing. */
        MODELESS,

        /** It blocks the windows of the same document. */
        DOCUMENT_MODAL,

        /** It blocks the whole application. */
        APPLICATION_MODAL,

        /** It blocks everything running on the same virtual machine. */
        TOOLKIT_MODAL
    }

    /** Which modal dialogs a window is excluded from. */
    public static enum ModalExclusionType {

        /** From none: it is blocked like all the others. */
        NO_EXCLUDE,

        /** From the ones that block the application. */
        APPLICATION_EXCLUDE,

        /** From all of them, including the ones that block the virtual machine. */
        TOOLKIT_EXCLUDE
    }

    /** The modality used when a "modal" dialog is asked for without saying of which type. */
    public static final ModalityType DEFAULT_MODALITY_TYPE = ModalityType.APPLICATION_MODAL;

    private String title;
    private boolean resizable = true;
    private boolean undecorated;
    private ModalityType modalityType = ModalityType.MODELESS;

    /**
     * A dialog with no title that belongs to that frame.
     *
     * @throws HeadlessException if there is no screen
     */
    public Dialog(Frame owner) {
        this(owner, "", false);
    }

    /**
     * With that modality.
     *
     * @throws HeadlessException if there is no screen
     */
    public Dialog(Frame owner, boolean modal) {
        this(owner, "", modal);
    }

    /**
     * With that title.
     *
     * @throws HeadlessException if there is no screen
     */
    public Dialog(Frame owner, String title) {
        this(owner, title, false);
    }

    /**
     * With a title and a modality.
     *
     * @throws HeadlessException if there is no screen
     */
    public Dialog(Frame owner, String title, boolean modal) {
        this(owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS, null);
    }

    /**
     * With a title, a modality and a graphics configuration.
     *
     * @throws IllegalArgumentException if the configuration is not a screen one
     */
    public Dialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
        this(owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS, gc);
    }

    /**
     * A dialog that belongs to another dialog.
     *
     * @throws HeadlessException if there is no screen
     */
    public Dialog(Dialog owner) {
        this(owner, "", false);
    }

    /**
     * With that title.
     *
     * @throws HeadlessException if there is no screen
     */
    public Dialog(Dialog owner, String title) {
        this(owner, title, false);
    }

    /**
     * With a title and a modality.
     *
     * @throws HeadlessException if there is no screen
     */
    public Dialog(Dialog owner, String title, boolean modal) {
        this((Window) owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS, null);
    }

    /**
     * With a title, a modality and a graphics configuration.
     *
     * @throws IllegalArgumentException if the configuration is not a screen one
     */
    public Dialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) {
        this((Window) owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS, gc);
    }

    /**
     * A dialog that belongs to that window.
     *
     * @throws HeadlessException if there is no screen
     */
    public Dialog(Window owner) {
        this(owner, "", ModalityType.MODELESS, null);
    }

    /**
     * With that modality.
     *
     * @throws HeadlessException if there is no screen
     */
    public Dialog(Window owner, ModalityType modalityType) {
        this(owner, "", modalityType, null);
    }

    /**
     * With that title.
     *
     * @throws HeadlessException if there is no screen
     */
    public Dialog(Window owner, String title) {
        this(owner, title, ModalityType.MODELESS, null);
    }

    /**
     * With a title and a modality.
     *
     * @throws HeadlessException if there is no screen
     */
    public Dialog(Window owner, String title, ModalityType modalityType) {
        this(owner, title, modalityType, null);
    }

    /**
     * The general constructor.
     *
     * @throws IllegalArgumentException if the owner is not a frame, a dialog or a window, or if the
     *     graphics configuration is not a screen one
     */
    public Dialog(Window owner, String title, ModalityType modalityType,
            GraphicsConfiguration gc) {
        super(owner, gc);
        this.title = title;
        this.modalityType = modalityType == null ? ModalityType.MODELESS : modalityType;
    }

    /** Notifies that it can be shown. */
    public void addNotify() {
        super.addNotify();
    }

    /** Whether it blocks the other windows. */
    public boolean isModal() {
        return this.modalityType != ModalityType.MODELESS;
    }

    /**
     * Makes it modal or not.
     *
     * <p>With `true` it uses {@link #DEFAULT_MODALITY_TYPE}; to choose the scope there is
     * {@link #setModalityType}.
     */
    public void setModal(boolean modal) {
        this.setModalityType(modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS);
    }

    /** How much it blocks. */
    public ModalityType getModalityType() {
        return this.modalityType;
    }

    /**
     * Changes how much it blocks.
     *
     * <p>With `null` it is left with no modality. Changing it while the dialog is open has no
     * effect until the next time it is shown.
     */
    public void setModalityType(ModalityType type) {
        if (type == null) {
            this.modalityType = ModalityType.MODELESS;
        } else {
            this.modalityType = type;
        }
    }

    /** The text of the title bar. */
    public String getTitle() {
        return this.title;
    }

    /** Changes its title. */
    public void setTitle(String title) {
        String old = this.title;
        this.title = title;
        this.firePropertyChange("title", old, title);
    }

    /**
     * Shows it or hides it.
     *
     * <p>With a modal dialog, the JDK **does not come back** from here until it is closed. Here it
     * comes back right away: blocking means stacking an event loop that filters the input towards
     * the other windows, and with no windowing system there is no input nor windows to filter.
     */
    public void setVisible(boolean b) {
        super.setVisible(b);
    }

    /**
     * Shows it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setVisible}.
     */
    @Deprecated
    public void show() {
        super.show();
    }

    /**
     * Hides it.
     *
     * @deprecated it is from the 1.0 model. Use {@link #setVisible}.
     */
    @Deprecated
    public void hide() {
        super.hide();
    }

    /**
     * Sends it to the back.
     *
     * <p>A modal dialog cannot go behind the window it blocks: it would be invisible and the user
     * would have no way of closing it.
     */
    public void toBack() {
        if (this.isModal()) {
            return;
        }
        super.toBack();
    }

    /** Whether the user can change its size. */
    public boolean isResizable() {
        return this.resizable;
    }

    /** Declares whether the user can change its size. */
    public void setResizable(boolean resizable) {
        boolean old;
        synchronized (this) {
            old = this.resizable;
            this.resizable = resizable;
        }
        this.firePropertyChange("resizable", old, resizable);
    }

    /**
     * Takes its decoration away.
     *
     * @throws IllegalComponentStateException if the dialog can already be shown
     */
    public void setUndecorated(boolean undecorated) {
        synchronized (this.getTreeLock()) {
            if (this.isDisplayable()) {
                throw new IllegalComponentStateException("The dialog is displayable.");
            }
            this.undecorated = undecorated;
        }
    }

    /** Whether it has no decoration. */
    public boolean isUndecorated() {
        return this.undecorated;
    }

    /**
     * Changes its opacity.
     *
     * @throws IllegalComponentStateException if the dialog is decorated
     */
    public void setOpacity(float opacity) {
        synchronized (this.getTreeLock()) {
            if (opacity < 1.0f && !this.isUndecorated()) {
                throw new IllegalComponentStateException("The dialog is decorated");
            }
            super.setOpacity(opacity);
        }
    }

    /**
     * Clips its shape.
     *
     * @throws IllegalComponentStateException if the dialog is decorated
     */
    public void setShape(Shape shape) {
        synchronized (this.getTreeLock()) {
            if (shape != null && !this.isUndecorated()) {
                throw new IllegalComponentStateException("The dialog is decorated");
            }
            super.setShape(shape);
        }
    }

    /**
     * Changes its background.
     *
     * @throws IllegalComponentStateException if transparency is asked for on a decorated dialog
     */
    public void setBackground(Color bgColor) {
        synchronized (this.getTreeLock()) {
            if (bgColor != null && bgColor.getAlpha() < 255 && !this.isUndecorated()) {
                throw new IllegalComponentStateException("The dialog is decorated");
            }
            super.setBackground(bgColor);
        }
    }

    protected String paramString() {
        String s = super.paramString() + "," + this.modalityType;
        if (this.title != null) {
            s = s + ",title=" + this.title;
        }
        return s;
    }

    /** The accessibility information of this dialog. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTDialog();
        }
        return this.accessibleContext;
    }

    /** The accessibility of a dialog. */
    protected class AccessibleAWTDialog extends AccessibleAWTWindow {

        /** For the subclasses. */
        protected AccessibleAWTDialog() {
        }

        /** It is a dialog. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.DIALOG;
        }

        /** The ones of a window, plus more if it is modal and if it can be resized. */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (Dialog.this.isResizable()) {
                s.add(AccessibleState.RESIZABLE);
            }
            if (Dialog.this.isModal()) {
                s.add(AccessibleState.MODAL);
            }
            return s;
        }
    }
}
