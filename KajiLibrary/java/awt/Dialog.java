package java.awt;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * Una ventana subordinada a otra: un diálogo.
 *
 * <p>Lo que la distingue de un {@link Frame} es la **modalidad**: un diálogo modal bloquea a las
 * demás ventanas mientras está abierto, y {@link #setVisible} no vuelve hasta que se cierra. Esa
 * llamada que no vuelve es lo que permite escribir un diálogo de confirmación como si fuera una
 * función.
 *
 * <p>El alcance del bloqueo se declara con {@link ModalityType}, y no es un detalle: bloquear la
 * aplicación entera cuando bastaba con bloquear un documento es la diferencia entre un editor que
 * deja seguir trabajando en las otras pestañas y uno que no.
 *
 * <p>La exclusión es la contracara: {@link ModalExclusionType} le permite a una ventana **no**
 * bloquearse. Es lo que necesita una barra de progreso o una ventana de registro que tiene que seguir
 * actualizándose mientras hay un diálogo abierto.
 *
 * <p><strong>Acá no bloquea nada.</strong> Un diálogo modal se implementa apilando un bucle de
 * eventos que filtra la entrada del usuario hacia las demás ventanas, y sin sistema de ventanas no
 * hay entrada que filtrar ni ventanas que bloquear. {@link #setVisible} vuelve enseguida, y el estado
 * de modalidad se guarda y se informa como se pidió.
 */
public class Dialog extends Window {

    private static final long serialVersionUID = 5920926903803293709L;

    /** Cuánto bloquea un diálogo modal. */
    public static enum ModalityType {

        /** No bloquea nada. */
        MODELESS,

        /** Bloquea las ventanas del mismo documento. */
        DOCUMENT_MODAL,

        /** Bloquea toda la aplicación. */
        APPLICATION_MODAL,

        /** Bloquea todo lo que corra en la misma máquina virtual. */
        TOOLKIT_MODAL
    }

    /** De qué modales queda excluida una ventana. */
    public static enum ModalExclusionType {

        /** De ninguno: se bloquea como todas. */
        NO_EXCLUDE,

        /** De los que bloquean la aplicación. */
        APPLICATION_EXCLUDE,

        /** De todos, incluidos los que bloquean la máquina virtual. */
        TOOLKIT_EXCLUDE
    }

    /** La modalidad que se usa cuando se pide un diálogo "modal" sin decir de qué tipo. */
    public static final ModalityType DEFAULT_MODALITY_TYPE = ModalityType.APPLICATION_MODAL;

    private String title;
    private boolean resizable = true;
    private boolean undecorated;
    private ModalityType modalityType = ModalityType.MODELESS;

    /**
     * Un diálogo sin título que pertenece a ese marco.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Dialog(Frame owner) {
        this(owner, "", false);
    }

    /**
     * Con esa modalidad.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Dialog(Frame owner, boolean modal) {
        this(owner, "", modal);
    }

    /**
     * Con ese título.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Dialog(Frame owner, String title) {
        this(owner, title, false);
    }

    /**
     * Con título y modalidad.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Dialog(Frame owner, String title, boolean modal) {
        this(owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS, null);
    }

    /**
     * Con título, modalidad y configuración gráfica.
     *
     * @throws IllegalArgumentException si la configuración no es de una pantalla
     */
    public Dialog(Frame owner, String title, boolean modal, GraphicsConfiguration gc) {
        this(owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS, gc);
    }

    /**
     * Un diálogo que pertenece a otro diálogo.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Dialog(Dialog owner) {
        this(owner, "", false);
    }

    /**
     * Con ese título.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Dialog(Dialog owner, String title) {
        this(owner, title, false);
    }

    /**
     * Con título y modalidad.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Dialog(Dialog owner, String title, boolean modal) {
        this((Window) owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS, null);
    }

    /**
     * Con título, modalidad y configuración gráfica.
     *
     * @throws IllegalArgumentException si la configuración no es de una pantalla
     */
    public Dialog(Dialog owner, String title, boolean modal, GraphicsConfiguration gc) {
        this((Window) owner, title, modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS, gc);
    }

    /**
     * Un diálogo que pertenece a esa ventana.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Dialog(Window owner) {
        this(owner, "", ModalityType.MODELESS, null);
    }

    /**
     * Con esa modalidad.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Dialog(Window owner, ModalityType modalityType) {
        this(owner, "", modalityType, null);
    }

    /**
     * Con ese título.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Dialog(Window owner, String title) {
        this(owner, title, ModalityType.MODELESS, null);
    }

    /**
     * Con título y modalidad.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public Dialog(Window owner, String title, ModalityType modalityType) {
        this(owner, title, modalityType, null);
    }

    /**
     * El constructor general.
     *
     * @throws IllegalArgumentException si el dueño no es un marco, un diálogo o una ventana, o si la
     *     configuración gráfica no es de una pantalla
     */
    public Dialog(Window owner, String title, ModalityType modalityType,
            GraphicsConfiguration gc) {
        super(owner, gc);
        this.title = title;
        this.modalityType = modalityType == null ? ModalityType.MODELESS : modalityType;
    }

    /** Avisa que puede mostrarse. */
    public void addNotify() {
        super.addNotify();
    }

    /** Si bloquea a las demás ventanas. */
    public boolean isModal() {
        return this.modalityType != ModalityType.MODELESS;
    }

    /**
     * Lo hace modal o no.
     *
     * <p>Con `true` usa {@link #DEFAULT_MODALITY_TYPE}; para elegir el alcance está
     * {@link #setModalityType}.
     */
    public void setModal(boolean modal) {
        this.setModalityType(modal ? DEFAULT_MODALITY_TYPE : ModalityType.MODELESS);
    }

    /** Cuánto bloquea. */
    public ModalityType getModalityType() {
        return this.modalityType;
    }

    /**
     * Cambia cuánto bloquea.
     *
     * <p>Con `null` queda sin modalidad. Cambiarlo mientras el diálogo está abierto no tiene efecto
     * hasta la próxima vez que se lo muestre.
     */
    public void setModalityType(ModalityType type) {
        if (type == null) {
            this.modalityType = ModalityType.MODELESS;
        } else {
            this.modalityType = type;
        }
    }

    /** El texto de la barra de título. */
    public String getTitle() {
        return this.title;
    }

    /** Le cambia el título. */
    public void setTitle(String title) {
        String viejo = this.title;
        this.title = title;
        this.firePropertyChange("title", viejo, title);
    }

    /**
     * Lo muestra o lo esconde.
     *
     * <p>Con un diálogo modal, el JDK **no vuelve** de acá hasta que se lo cierre. Acá vuelve
     * enseguida: bloquear significa apilar un bucle de eventos que filtre la entrada hacia las demás
     * ventanas, y sin sistema de ventanas no hay entrada ni ventanas que filtrar.
     */
    public void setVisible(boolean b) {
        super.setVisible(b);
    }

    /**
     * Lo muestra.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setVisible}.
     */
    @Deprecated
    public void show() {
        super.show();
    }

    /**
     * Lo esconde.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #setVisible}.
     */
    @Deprecated
    public void hide() {
        super.hide();
    }

    /**
     * Lo manda atrás.
     *
     * <p>Un diálogo modal no puede irse atrás de la ventana que bloquea: sería invisible y el usuario
     * no tendría cómo cerrarlo.
     */
    public void toBack() {
        if (this.isModal()) {
            return;
        }
        super.toBack();
    }

    /** Si el usuario puede cambiarle el tamaño. */
    public boolean isResizable() {
        return this.resizable;
    }

    /** Declara si el usuario puede cambiarle el tamaño. */
    public void setResizable(boolean resizable) {
        boolean viejo;
        synchronized (this) {
            viejo = this.resizable;
            this.resizable = resizable;
        }
        this.firePropertyChange("resizable", viejo, resizable);
    }

    /**
     * Le saca la decoración.
     *
     * @throws IllegalComponentStateException si el diálogo ya puede mostrarse
     */
    public void setUndecorated(boolean undecorated) {
        synchronized (this.getTreeLock()) {
            if (this.isDisplayable()) {
                throw new IllegalComponentStateException("The dialog is displayable.");
            }
            this.undecorated = undecorated;
        }
    }

    /** Si no tiene decoración. */
    public boolean isUndecorated() {
        return this.undecorated;
    }

    /**
     * Le cambia la opacidad.
     *
     * @throws IllegalComponentStateException si el diálogo está decorado
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
     * Le recorta la forma.
     *
     * @throws IllegalComponentStateException si el diálogo está decorado
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
     * Le cambia el fondo.
     *
     * @throws IllegalComponentStateException si se pide transparencia sobre un diálogo decorado
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

    /** La información de accesibilidad de este diálogo. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTDialog();
        }
        return this.accessibleContext;
    }

    /** La accesibilidad de un diálogo. */
    protected class AccessibleAWTDialog extends AccessibleAWTWindow {

        /** Para las subclases. */
        protected AccessibleAWTDialog() {
        }

        /** Es un diálogo. */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.DIALOG;
        }

        /** Los de una ventana, más si es modal y si se puede redimensionar. */
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
