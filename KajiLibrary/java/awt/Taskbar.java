package java.awt;

/**
 * La barra de tareas o el dock: donde el sistema muestra el programa mientras corre.
 *
 * <p>Sirve para tres cosas: cambiar el ícono, mostrar progreso, y pedir la atención del usuario
 * —el rebote del dock en macOS, el parpadeo en Windows—.
 *
 * <p><strong>Cada cosa se admite o no por separado</strong>, y de ahí sale {@link #isSupported}: no
 * hay una barra de tareas sino tres o cuatro implementaciones que hacen cosas distintas. Windows
 * tiene progreso en la ventana y no en el ícono del programa; macOS al revés. Preguntar antes es
 * obligatorio, y los métodos tiran si se los llama sin preguntar.
 *
 * <p>Acá no se admite ninguna: {@link #isTaskbarSupported} da `false` y {@link #getTaskbar} tira,
 * que es lo que hace el JDK sin escritorio.
 */
public class Taskbar {

    /** Cada cosa que una barra de tareas puede saber hacer. */
    public static enum Feature {

        /** Un texto chico encima del ícono del programa. */
        ICON_BADGE_TEXT,

        /** Un número encima del ícono del programa. */
        ICON_BADGE_NUMBER,

        /** Una imagen encima del ícono de una ventana. */
        ICON_BADGE_IMAGE_WINDOW,

        /** Cambiar el ícono del programa. */
        ICON_IMAGE,

        /** Un menú propio en el ícono del programa. */
        MENU,

        /** El estado de la barra de progreso de una ventana. */
        PROGRESS_STATE_WINDOW,

        /** El valor de progreso del programa. */
        PROGRESS_VALUE,

        /** El valor de progreso de una ventana. */
        PROGRESS_VALUE_WINDOW,

        /** Pedir la atención del usuario sobre el programa. */
        USER_ATTENTION,

        /** Pedirla sobre una ventana. */
        USER_ATTENTION_WINDOW
    }

    /** En qué estado está una barra de progreso. */
    public static enum State {

        /** Sin barra. */
        OFF,

        /** Avanzando normalmente. */
        NORMAL,

        /** Pausada: se ve pero no avanza. */
        PAUSED,

        /** Sin porcentaje conocido: la barra se mueve sola. */
        INDETERMINATE,

        /** Algo falló: la barra se ve en rojo. */
        ERROR
    }

    /** La única barra, si alguna vez se llega a pedir. */
    private static Taskbar unica;

    /** El menú propio del ícono. */
    private PopupMenu menu;

    /** El ícono del programa. */
    private Image icono;

    /** No se instancia desde afuera. */
    private Taskbar() {
    }

    /**
     * La barra de tareas de esta sesión.
     *
     * @throws UnsupportedOperationException siempre acá: no hay escritorio
     * @throws HeadlessException si no hay pantalla
     */
    public static synchronized Taskbar getTaskbar() {
        if (!isTaskbarSupported()) {
            throw new UnsupportedOperationException("Taskbar API is not supported on the current platform");
        }
        if (unica == null) {
            unica = new Taskbar();
        }
        return unica;
    }

    /**
     * Si esta plataforma tiene barra de tareas manejable.
     *
     * @return `false` siempre
     */
    public static boolean isTaskbarSupported() {
        return false;
    }

    /**
     * Si admite esa función.
     *
     * @return `false` para todas: no hay barra que las haga
     * @throws NullPointerException si la función es `null`
     */
    public boolean isSupported(Feature feature) {
        if (feature == null) {
            throw new NullPointerException("feature");
        }
        return false;
    }

    /**
     * Pide la atención del usuario sobre el programa.
     *
     * @param enabled si prender el aviso o apagarlo
     * @param critical si el aviso es insistente
     * @throws UnsupportedOperationException si no se admite {@link Feature#USER_ATTENTION}
     */
    public void requestUserAttention(boolean enabled, boolean critical) {
        this.exigir(Feature.USER_ATTENTION);
    }

    /**
     * Pide la atención sobre esa ventana.
     *
     * @throws UnsupportedOperationException si no se admite {@link Feature#USER_ATTENTION_WINDOW}
     * @throws IllegalArgumentException si la ventana es `null`
     */
    public void requestWindowUserAttention(Window w) {
        this.comprobarVentana(w);
        this.exigir(Feature.USER_ATTENTION_WINDOW);
    }

    /**
     * Le pone un menú propio al ícono del programa.
     *
     * @throws UnsupportedOperationException si no se admite {@link Feature#MENU}
     */
    public void setMenu(PopupMenu menu) {
        this.exigir(Feature.MENU);
        this.menu = menu;
    }

    /**
     * El menú propio del ícono.
     *
     * @throws UnsupportedOperationException si no se admite {@link Feature#MENU}
     */
    public PopupMenu getMenu() {
        this.exigir(Feature.MENU);
        return this.menu;
    }

    /**
     * Cambia el ícono del programa.
     *
     * @throws UnsupportedOperationException si no se admite {@link Feature#ICON_IMAGE}
     */
    public void setIconImage(Image image) {
        this.exigir(Feature.ICON_IMAGE);
        this.icono = image;
    }

    /**
     * El ícono del programa.
     *
     * @throws UnsupportedOperationException si no se admite {@link Feature#ICON_IMAGE}
     */
    public Image getIconImage() {
        this.exigir(Feature.ICON_IMAGE);
        return this.icono;
    }

    /**
     * Pone un texto encima del ícono del programa.
     *
     * @param badge el texto, o `null` para sacarlo
     * @throws UnsupportedOperationException si no se admite ni {@link Feature#ICON_BADGE_TEXT} ni
     *     {@link Feature#ICON_BADGE_NUMBER}
     */
    public void setIconBadge(String badge) {
        if (!this.isSupported(Feature.ICON_BADGE_TEXT)
                && !this.isSupported(Feature.ICON_BADGE_NUMBER)) {
            throw new UnsupportedOperationException("The ICON_BADGE_TEXT feature is not supported on the current platform");
        }
    }

    /**
     * Pone una imagen encima del ícono de una ventana.
     *
     * @throws UnsupportedOperationException si no se admite {@link Feature#ICON_BADGE_IMAGE_WINDOW}
     * @throws IllegalArgumentException si la ventana es `null`
     */
    public void setWindowIconBadge(Window w, Image badge) {
        this.comprobarVentana(w);
        this.exigir(Feature.ICON_BADGE_IMAGE_WINDOW);
    }

    /**
     * Muestra el progreso del programa.
     *
     * @param value de 0 a 100; fuera de ese rango, la barra se apaga
     * @throws UnsupportedOperationException si no se admite {@link Feature#PROGRESS_VALUE}
     */
    public void setProgressValue(int value) {
        this.exigir(Feature.PROGRESS_VALUE);
    }

    /**
     * Muestra el progreso de una ventana.
     *
     * @param value de 0 a 100; fuera de ese rango, la barra se apaga
     * @throws UnsupportedOperationException si no se admite {@link Feature#PROGRESS_VALUE_WINDOW}
     * @throws IllegalArgumentException si la ventana es `null`
     */
    public void setWindowProgressValue(Window w, int value) {
        this.comprobarVentana(w);
        this.exigir(Feature.PROGRESS_VALUE_WINDOW);
    }

    /**
     * Cambia el estado de la barra de progreso de una ventana.
     *
     * @throws UnsupportedOperationException si no se admite {@link Feature#PROGRESS_STATE_WINDOW}
     * @throws IllegalArgumentException si la ventana es `null`
     * @throws NullPointerException si el estado es `null`
     */
    public void setWindowProgressState(Window w, State state) {
        this.comprobarVentana(w);
        if (state == null) {
            throw new NullPointerException("state");
        }
        this.exigir(Feature.PROGRESS_STATE_WINDOW);
    }

    /** Tira si esa función no se admite. */
    private void exigir(Feature f) {
        if (!this.isSupported(f)) {
            throw new UnsupportedOperationException("The " + f.name()
                    + " feature is not supported on the current platform");
        }
    }

    /** Que la ventana no sea `null`. */
    private void comprobarVentana(Window w) {
        if (w == null) {
            throw new IllegalArgumentException("Window must not be null");
        }
    }
}
