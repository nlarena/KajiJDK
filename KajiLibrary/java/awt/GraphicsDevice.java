package java.awt;

/**
 * Una pantalla, una impresora o un buffer en memoria: algo sobre lo que se puede dibujar.
 *
 * <p>Un dispositivo tiene una o varias {@link GraphicsConfiguration}, que son las combinaciones de
 * profundidad de color y capacidades con las que se lo puede usar. La misma pantalla puede ofrecer
 * varias, y ahí es donde se elige.
 *
 * <p><strong>Falta el modo de pantalla completa.</strong> `setFullScreenWindow` y
 * `getFullScreenWindow` toman y devuelven un `java.awt.Window`, que arrastra el árbol entero de
 * componentes de AWT —ventanas, contenedores, distribución, eventos— y queda fuera del alcance de lo
 * que necesita el dibujado de imágenes. Un miembro que falta es un subconjunto legal; uno que
 * miente, no, así que {@link #isFullScreenSupported} contesta `false`, que es la verdad de este
 * dispositivo y no una excusa.
 */
public abstract class GraphicsDevice {

    /** Una pantalla. */
    public static final int TYPE_RASTER_SCREEN = 0;

    /** Una impresora. */
    public static final int TYPE_PRINTER = 1;

    /** Un buffer en memoria. */
    public static final int TYPE_IMAGE_BUFFER = 2;

    private DisplayMode modoOriginal;

    /** Para las subclases. */
    protected GraphicsDevice() {
    }

    /** Los tipos de transparencia que un dispositivo puede o no admitir en sus ventanas. */
    public static enum WindowTranslucency {

        /** Cada píxel es del todo opaco o del todo transparente. */
        PERPIXEL_TRANSPARENT,

        /** La ventana entera tiene una opacidad uniforme. */
        TRANSLUCENT,

        /** Cada píxel tiene su propia opacidad. */
        PERPIXEL_TRANSLUCENT
    }

    /** `TYPE_RASTER_SCREEN`, `TYPE_PRINTER` o `TYPE_IMAGE_BUFFER`. */
    public abstract int getType();

    /** Un identificador del dispositivo. */
    public abstract String getIDstring();

    /** Todas sus configuraciones. */
    public abstract GraphicsConfiguration[] getConfigurations();

    /** La configuración que usa por omisión. */
    public abstract GraphicsConfiguration getDefaultConfiguration();

    /**
     * La configuración que mejor cumple con esos requisitos.
     *
     * @throws NullPointerException si la plantilla es `null`
     */
    public GraphicsConfiguration getBestConfiguration(GraphicsConfigTemplate gct) {
        GraphicsConfiguration[] configs = this.getConfigurations();
        return gct.getBestConfiguration(configs);
    }

    /**
     * Si admite el modo de pantalla completa exclusivo.
     *
     * <p>Contesta `false` porque el modo exclusivo se maneja con ventanas, y esta biblioteca no trae
     * el árbol de componentes de AWT.
     */
    public boolean isFullScreenSupported() {
        return false;
    }

    /** Si se le puede cambiar el modo de pantalla. */
    public boolean isDisplayChangeSupported() {
        return false;
    }

    /**
     * Cambia el modo de pantalla.
     *
     * @throws UnsupportedOperationException si el dispositivo no admite el cambio
     * @throws IllegalArgumentException si el modo no es uno de los que devuelve
     *     {@link #getDisplayModes}
     */
    public void setDisplayMode(DisplayMode dm) {
        if (!this.isDisplayChangeSupported()) {
            throw new UnsupportedOperationException("Cannot change display mode");
        }
        if (dm == null) {
            throw new IllegalArgumentException("Invalid display mode");
        }
        DisplayMode[] modos = this.getDisplayModes();
        for (int i = 0; i < modos.length; i++) {
            if (dm.equals(modos[i])) {
                if (this.modoOriginal == null) {
                    this.modoOriginal = this.getDisplayMode();
                }
                return;
            }
        }
        throw new IllegalArgumentException("Invalid display mode");
    }

    /** El modo de pantalla actual, o `null` si no se sabe. */
    public DisplayMode getDisplayMode() {
        return null;
    }

    /** Los modos de pantalla disponibles. */
    public DisplayMode[] getDisplayModes() {
        DisplayMode[] uno = new DisplayMode[1];
        uno[0] = this.getDisplayMode();
        if (uno[0] == null) {
            return new DisplayMode[0];
        }
        return uno;
    }

    /**
     * Cuánta memoria acelerada queda, o -1 si no se sabe.
     *
     * <p>El -1 es una respuesta: significa que no hay forma de averiguarlo, que es distinto de que
     * no quede nada.
     */
    public int getAvailableAcceleratedMemory() {
        return -1;
    }

    /**
     * Si admite ese tipo de transparencia en las ventanas.
     *
     * @throws NullPointerException si el tipo es `null`
     */
    public boolean isWindowTranslucencySupported(WindowTranslucency translucencyKind) {
        if (translucencyKind == null) {
            throw new NullPointerException("translucencyKind cannot be null");
        }
        return false;
    }

    /**
     * La ventana que está en modo de pantalla completa exclusivo.
     *
     * @return `null` siempre: {@link #isFullScreenSupported} dice que no se admite, así que nunca
     *     hay ninguna
     */
    public Window getFullScreenWindow() {
        return this.ventanaCompleta;
    }

    /**
     * Pone una ventana en pantalla completa exclusiva, o saca la que estuviera pasando `null`.
     *
     * <p>Como {@link #isFullScreenSupported} contesta `false`, esto hace lo que hace el JDK cuando el
     * modo exclusivo no está: **simula**. La ventana se agranda al tamaño de la pantalla y se
     * muestra, sin apoderarse del dispositivo. Acá ni siquiera hay pantalla que medir, así que lo
     * único observable es que la ventana queda anotada y {@link #getFullScreenWindow} la devuelve.
     *
     * <p>Pasar `null` restituye el modo de pantalla original, si es que se lo había cambiado.
     */
    public void setFullScreenWindow(Window w) {
        if (this.ventanaCompleta != null && this.modoOriginal != null) {
            this.setDisplayMode(this.modoOriginal);
            this.modoOriginal = null;
        }
        this.ventanaCompleta = w;
    }

    /** La ventana en pantalla completa, o `null`. */
    private Window ventanaCompleta;
}
