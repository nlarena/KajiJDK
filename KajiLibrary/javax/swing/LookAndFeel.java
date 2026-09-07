package javax.swing;

import javax.swing.plaf.UIResource;

/**
 * Un aspecto: el conjunto de {@code ComponentUI} y valores por omision que dan a Swing una cara.
 *
 * <h2>Lo que hay y lo que no</h2>
 *
 * <p>Esta biblioteca tiene <strong>un solo aspecto</strong>: el basico, con los valores por
 * omision medidos en Metal, instalados directamente por cada {@code updateUI}.
 *
 * <p>Esta nota decia que tampoco estaban los metodos que consultan las tablas de
 * {@link UIManager}, porque no habia {@code UIManager} donde registrarlas y sin tablas solo podrian
 * mentir. Ya lo hay, junto con {@link UIDefaults}, asi que estan todos: los que instalan colores,
 * tipografia y bordes, los que arman mapas de teclas, y {@link #getLayoutStyle}.
 *
 * <p>Lo que si esta es lo que no depende de tablas: {@link #installProperty}, que es como un
 * aspecto pone una propiedad <em>sin pisar lo que el usuario puso</em>, y
 * {@link #uninstallBorder}, que quita un borde solo si es del aspecto. Los dos se apoyan en
 * {@link UIResource}, que es la manera de distinguir lo uno de lo otro.
 */
public abstract class LookAndFeel {

    public LookAndFeel() {
    }

    /**
     * Pone una propiedad en el componente, salvo que el usuario ya la haya puesto.
     *
     * <p>Es la regla de convivencia entre aspecto y programador: el aspecto propone, el programador
     * dispone. El componente recuerda cuales propiedades le puso el programador, y esta llamada
     * respeta esas. Las propiedades admitidas dependen del componente; una que no admite es un
     * {@code IllegalArgumentException}.
     */
    public static void installProperty(JComponent c, String propertyName, Object propertyValue) {
        c.setUIProperty(propertyName, propertyValue);
    }

    /** Quita el borde del componente si lo puso un aspecto; uno del usuario se queda. */
    public static void uninstallBorder(JComponent c) {
        if (c.getBorder() instanceof UIResource) {
            c.setBorder(null);
        }
    }

    /**
     * El icono deshabilitado que corresponde a ese icono: ninguno.
     *
     * <p>El JDK fabrica uno agrisado cuando el icono es un {@code ImageIcon}, y {@code null} para
     * cualquier otro. Sin {@code ImageIcon}, la respuesta es siempre la segunda, y quien la recibe
     * —{@code AbstractButton}, {@code JLabel}— pinta el icono normal.
     */
    public Icon getDisabledIcon(JComponent component, Icon icon) {
        return null;
    }

    /** El icono deshabilitado y seleccionado: ninguno, por lo mismo que {@link #getDisabledIcon}. */
    public Icon getDisabledSelectedIcon(JComponent component, Icon icon) {
        return null;
    }

    /** Un nombre corto para mostrar, como "Metal". */
    public abstract String getName();

    /** Un identificador estable, como "Metal"; el nombre puede cambiar, este no. */
    public abstract String getID();

    /** Una linea que lo describe. */
    public abstract String getDescription();

    /** Si este aspecto puede decorar las ventanas el mismo: no, este no. */
    public boolean getSupportsWindowDecorations() {
        return false;
    }

    /** Si es el aspecto nativo de la plataforma. */
    public abstract boolean isNativeLookAndFeel();

    /** Si este aspecto puede usarse en esta plataforma. */
    public abstract boolean isSupportedLookAndFeel();

    /** Se llama al instalarlo; no hay nada que preparar. */
    public void initialize() {
    }

    /** Se llama al desinstalarlo; no hay nada que soltar. */
    /**
     * La tabla de valores de este aspecto.
     *
     * <p>Devuelve {@code null} salvo que la subclase la arme. No es una omision: un aspecto que no
     * define valores propios usa los que ya estan en {@link UIManager}, y devolver una tabla vacia
     * en lugar de {@code null} los borraria a todos al instalarse.
     *
     * @return la tabla, o {@code null} si este aspecto no tiene una propia
     */
    public UIDefaults getDefaults() {
        return null;
    }

    public void uninitialize() {
    }

    public String toString() {
        return "[" + getDescription() + " - " + getClass().getName() + "]";
    }

    // -- instalar valores del aspecto ------------------------------------------------------------

    /**
     * Le pone al componente el frente y el fondo de la tabla, si no los tiene puestos a mano.
     *
     * <p>"Puestos a mano" se decide por {@link UIResource}: un color que es recurso de aspecto lo
     * puso el aspecto anterior y se puede pisar; uno que no lo es lo puso el programa y se respeta.
     * Es toda la logica de estos cuatro metodos, y es lo que hace que cambiar de aspecto no borre lo
     * que el programa configuro.
     */
    public static void installColors(JComponent c, String defaultBgName,
            String defaultFgName) {
        java.awt.Color bg = c.getBackground();
        if (bg == null || bg instanceof UIResource) {
            c.setBackground(UIManager.getColor(defaultBgName));
        }
        java.awt.Color fg = c.getForeground();
        if (fg == null || fg instanceof UIResource) {
            c.setForeground(UIManager.getColor(defaultFgName));
        }
    }

    /** Lo mismo, y ademas la tipografia; ver {@link #installColors}. */
    public static void installColorsAndFont(JComponent c, String defaultBgName,
            String defaultFgName, String defaultFontName) {
        java.awt.Font f = c.getFont();
        if (f == null || f instanceof UIResource) {
            c.setFont(UIManager.getFont(defaultFontName));
        }
        installColors(c, defaultBgName, defaultFgName);
    }

    /**
     * Le pone el borde de la tabla, si no tiene uno puesto a mano.
     *
     * @throws NullPointerException si el componente es nulo
     */
    public static void installBorder(JComponent c, String defaultBorderName) {
        javax.swing.border.Border b = c.getBorder();
        if (b == null || b instanceof UIResource) {
            c.setBorder(UIManager.getBorder(defaultBorderName));
        }
    }

    // -- mapas de teclas -------------------------------------------------------------------------

    /**
     * Convierte una lista plana de pares en ataduras de tecla a accion.
     *
     * <p>El arreglo va de a dos: una tecla -- {@link KeyStroke} o su texto -- y el nombre de la
     * accion. Se escribe asi porque un aspecto define cien ataduras y un arreglo literal es mas
     * corto y mas legible que cien llamadas.
     *
     * @throws IllegalArgumentException si el arreglo es nulo o tiene un numero impar de elementos
     */
    public static javax.swing.text.JTextComponent.KeyBinding[] makeKeyBindings(
            Object[] keyBindingList) {
        javax.swing.text.JTextComponent.KeyBinding[] rv =
                new javax.swing.text.JTextComponent.KeyBinding[keyBindingList.length / 2];
        for (int i = 0; i < rv.length; i++) {
            Object o = keyBindingList[2 * i];
            KeyStroke keystroke;
            if (o instanceof KeyStroke) {
                keystroke = (KeyStroke) o;
            } else {
                keystroke = KeyStroke.getKeyStroke((String) o);
            }
            String action = (String) keyBindingList[2 * i + 1];
            rv[i] = new javax.swing.text.JTextComponent.KeyBinding(keystroke, action);
        }
        return rv;
    }

    /**
     * Un mapa de teclas armado con esa lista plana; ver {@link #makeKeyBindings}.
     *
     * <p>El mapa que sale es un recurso de aspecto, y eso importa: es lo que permite reemplazarlo
     * entero al cambiar de aspecto sin tocar las ataduras que puso el programa.
     */
    public static InputMap makeInputMap(Object[] keys) {
        InputMap retMap = new javax.swing.plaf.InputMapUIResource();
        loadKeyBindings(retMap, keys);
        return retMap;
    }

    /** Lo mismo, para las ataduras que valen mientras la ventana tenga el foco. */
    public static ComponentInputMap makeComponentInputMap(JComponent c, Object[] keys) {
        ComponentInputMap retMap = new javax.swing.plaf.ComponentInputMapUIResource(c);
        loadKeyBindings(retMap, keys);
        return retMap;
    }

    /**
     * Carga esa lista plana en un mapa que ya existe.
     *
     * <p>Una lista nula no hace nada -- que es lo que hace el JDK --: un aspecto que no define
     * ataduras para un componente no es un error.
     */
    public static void loadKeyBindings(InputMap retMap, Object[] keys) {
        if (keys != null) {
            for (int counter = 0; counter < keys.length; counter = counter + 2) {
                Object keyStrokeO = keys[counter];
                KeyStroke key = (keyStrokeO instanceof KeyStroke)
                        ? (KeyStroke) keyStrokeO : KeyStroke.getKeyStroke((String) keyStrokeO);
                retMap.put(key, keys[counter + 1]);
            }
        }
    }

    /**
     * Un icono que se carga recien cuando alguien lo dibuja.
     *
     * <p>La demora importa: una tabla de aspecto nombra decenas de iconos y una pantalla usa unos
     * pocos. Cargarlos todos al instalar el aspecto seria leer decenas de archivos para nada.
     */
    public static Object makeIcon(final Class<?> baseClass, final String gifFile) {
        return new IconoDemorado(baseClass, gifFile);
    }

    // -- lo que sale del escritorio --------------------------------------------------------------

    /**
     * Un valor de configuracion del escritorio, o el de reserva si no lo hay.
     *
     * <p>Son cosas como la velocidad del cursor o si el sistema pide subrayar los atajos. Esta
     * biblioteca no consulta al escritorio, asi que siempre devuelve el de reserva -- y lo dice, en
     * vez de inventar un numero que parezca del sistema.
     */
    public static Object getDesktopPropertyValue(String systemPropertyName,
            Object fallbackValue) {
        Object value = java.awt.Toolkit.getDefaultToolkit()
                .getDesktopProperty(systemPropertyName);
        if (value == null) {
            return fallbackValue;
        }
        if (value instanceof java.awt.Color) {
            return new javax.swing.plaf.ColorUIResource((java.awt.Color) value);
        }
        if (value instanceof java.awt.Font) {
            return new javax.swing.plaf.FontUIResource((java.awt.Font) value);
        }
        return value;
    }

    /**
     * Como se le avisa al usuario que hizo algo invalido.
     *
     * <p>Lo normal es un pitido. Aca no suena nada: no hay con que. Un aspecto de verdad lo
     * sobreescribe.
     */
    public void provideErrorFeedback(java.awt.Component component) {
        java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
        if (toolkit != null) {
            toolkit.beep();
        }
    }

    /**
     * El espaciado que este aspecto recomienda entre componentes.
     *
     * <p>Nulo -- que es lo de omision -- deja que {@link LayoutStyle#getInstance} use el de siempre;
     * ver la nota de esa clase.
     */
    public LayoutStyle getLayoutStyle() {
        return null;
    }

    /** El icono que se carga al primer dibujado; ver {@link LookAndFeel#makeIcon}. */
    private static class IconoDemorado implements Icon, UIResource, java.io.Serializable {

        private final Class<?> baseClass;
        private final String gifFile;
        private Icon icono;

        IconoDemorado(Class<?> baseClass, String gifFile) {
            this.baseClass = baseClass;
            this.gifFile = gifFile;
        }

        private Icon cargar() {
            if (icono == null) {
                java.net.URL url = baseClass.getResource(gifFile);
                icono = (url == null) ? new IconoVacio() : new ImageIcon(url);
            }
            return icono;
        }

        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            cargar().paintIcon(c, g, x, y);
        }

        public int getIconWidth() {
            return cargar().getIconWidth();
        }

        public int getIconHeight() {
            return cargar().getIconHeight();
        }
    }

    /** Lo que queda cuando el archivo del icono no esta: nada, pero de tamano cero. */
    private static class IconoVacio implements Icon, UIResource, java.io.Serializable {

        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
        }

        public int getIconWidth() {
            return 0;
        }

        public int getIconHeight() {
            return 0;
        }
    }
}
