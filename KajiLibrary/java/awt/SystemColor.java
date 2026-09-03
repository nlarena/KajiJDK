package java.awt;

/**
 * KajiLibrary's java.awt.SystemColor -- los colores del escritorio, por nombre en vez de por valor.
 *
 * <p>Las veintiseis constantes no son colores fijos sino <b>papeles</b>: {@code window} es "el fondo
 * de una ventana", no "blanco". Un programa que dibuja con {@code SystemColor.window} y
 * {@code SystemColor.windowText} se ve bien en un tema claro y en uno oscuro sin cambiar una linea;
 * uno que escribe blanco y negro a mano se rompe en el segundo.
 *
 * <h2>Los valores son vivos, y por eso la clase es rara</h2>
 *
 * <p>Cada constante es un objeto <b>unico y mutable por dentro</b>: cuando el usuario cambia el tema,
 * el mismo objeto empieza a devolver otro color. Por eso {@code SystemColor.window} se puede guardar
 * en un campo y seguir siendo correcto, y por eso {@link #toString()} imprime el indice y no el
 * valor -- el valor de hoy no dice nada sobre el de manana.
 *
 * <p>De ahi sale tambien la firma que sorprende: el constructor es privado y las constantes se
 * comparan por identidad, no por RGB. Dos papeles distintos pueden tener hoy el mismo color.
 *
 * <h2>Que devuelve aca</h2>
 *
 * <p><b>Los valores por omision</b>: los mismos que el JDK entrega cuando corre sin escritorio
 * ({@code java.awt.headless}). No es una eleccion de esta biblioteca sino la unica posible -- no hay
 * ningun sistema de ventanas al que preguntarle --, y es exactamente lo que hace el JDK en esa
 * situacion. Cuando haya un toolkit, la tabla se actualiza y los mismos objetos empiezan a contestar
 * los colores del tema, sin que nada de lo que ya se escribio tenga que cambiar.
 */
public final class SystemColor extends Color implements java.io.Serializable {

    private static final long serialVersionUID = 4503142729533789064L;

    /** El fondo del escritorio. */
    public static final int DESKTOP = 0;

    /** El fondo de la barra de titulo de la ventana activa. */
    public static final int ACTIVE_CAPTION = 1;

    /** El texto de la barra de titulo de la ventana activa. */
    public static final int ACTIVE_CAPTION_TEXT = 2;

    /** El borde de la barra de titulo de la ventana activa. */
    public static final int ACTIVE_CAPTION_BORDER = 3;

    /** El fondo de la barra de titulo de una ventana inactiva. */
    public static final int INACTIVE_CAPTION = 4;

    /** El texto de la barra de titulo de una ventana inactiva. */
    public static final int INACTIVE_CAPTION_TEXT = 5;

    /** El borde de la barra de titulo de una ventana inactiva. */
    public static final int INACTIVE_CAPTION_BORDER = 6;

    /** El fondo de una ventana. */
    public static final int WINDOW = 7;

    /** El borde de una ventana. */
    public static final int WINDOW_BORDER = 8;

    /** El texto de una ventana. */
    public static final int WINDOW_TEXT = 9;

    /** El fondo de un menu. */
    public static final int MENU = 10;

    /** El texto de un menu. */
    public static final int MENU_TEXT = 11;

    /** El fondo de un campo de texto. */
    public static final int TEXT = 12;

    /** El texto de un campo de texto. */
    public static final int TEXT_TEXT = 13;

    /** El fondo del texto seleccionado. */
    public static final int TEXT_HIGHLIGHT = 14;

    /** El texto seleccionado. */
    public static final int TEXT_HIGHLIGHT_TEXT = 15;

    /** El texto deshabilitado. */
    public static final int TEXT_INACTIVE_TEXT = 16;

    /** El fondo de un control. */
    public static final int CONTROL = 17;

    /** El texto de un control. */
    public static final int CONTROL_TEXT = 18;

    /** El realce de un control, del lado iluminado. */
    public static final int CONTROL_HIGHLIGHT = 19;

    /** El realce claro de un control. */
    public static final int CONTROL_LT_HIGHLIGHT = 20;

    /** La sombra de un control. */
    public static final int CONTROL_SHADOW = 21;

    /** La sombra oscura de un control. */
    public static final int CONTROL_DK_SHADOW = 22;

    /** El fondo del canal de una barra de desplazamiento. */
    public static final int SCROLLBAR = 23;

    /** El fondo de una ayuda emergente. */
    public static final int INFO = 24;

    /** El texto de una ayuda emergente. */
    public static final int INFO_TEXT = 25;

    /** Cuantos papeles hay. Es el largo de la tabla, no un color. */
    public static final int NUM_COLORS = 26;

    // La tabla viva. Es `static` y mutable a proposito: cuando haya un toolkit y el tema cambie, se
    // actualiza aca y las veintiseis constantes empiezan a contestar los colores nuevos sin que
    // nadie tenga que volver a pedirlas. Los valores de arranque son los que usa el JDK sin
    // escritorio; ver la nota de la clase.
    private static int[] systemColors = {
        0xFF005C5C,  // desktop
        0xFF000080,  // activeCaption
        0xFFFFFFFF,  // activeCaptionText
        0xFFC0C0C0,  // activeCaptionBorder
        0xFF808080,  // inactiveCaption
        0xFFC0C0C0,  // inactiveCaptionText
        0xFFC0C0C0,  // inactiveCaptionBorder
        0xFFFFFFFF,  // window
        0xFF000000,  // windowBorder
        0xFF000000,  // windowText
        0xFFC0C0C0,  // menu
        0xFF000000,  // menuText
        0xFFC0C0C0,  // text
        0xFF000000,  // textText
        0xFF000080,  // textHighlight
        0xFFFFFFFF,  // textHighlightText
        0xFF808080,  // textInactiveText
        0xFFC0C0C0,  // control
        0xFF000000,  // controlText
        0xFFFFFFFF,  // controlHighlight
        0xFFE0E0E0,  // controlLtHighlight
        0xFF808080,  // controlShadow
        0xFF000000,  // controlDkShadow
        0xFFE0E0E0,  // scrollbar
        0xFFE0E000,  // info
        0xFF000000,  // infoText
    };

    /** El fondo del escritorio. */
    public static final SystemColor desktop = new SystemColor((byte) DESKTOP);

    /** El fondo de la barra de titulo de la ventana activa. */
    public static final SystemColor activeCaption = new SystemColor((byte) ACTIVE_CAPTION);

    /** El texto de la barra de titulo de la ventana activa. */
    public static final SystemColor activeCaptionText = new SystemColor((byte) ACTIVE_CAPTION_TEXT);

    /** El borde de la barra de titulo de la ventana activa. */
    public static final SystemColor activeCaptionBorder =
        new SystemColor((byte) ACTIVE_CAPTION_BORDER);

    /** El fondo de la barra de titulo de una ventana inactiva. */
    public static final SystemColor inactiveCaption = new SystemColor((byte) INACTIVE_CAPTION);

    /** El texto de la barra de titulo de una ventana inactiva. */
    public static final SystemColor inactiveCaptionText =
        new SystemColor((byte) INACTIVE_CAPTION_TEXT);

    /** El borde de la barra de titulo de una ventana inactiva. */
    public static final SystemColor inactiveCaptionBorder =
        new SystemColor((byte) INACTIVE_CAPTION_BORDER);

    /** El fondo de una ventana. */
    public static final SystemColor window = new SystemColor((byte) WINDOW);

    /** El borde de una ventana. */
    public static final SystemColor windowBorder = new SystemColor((byte) WINDOW_BORDER);

    /** El texto de una ventana. */
    public static final SystemColor windowText = new SystemColor((byte) WINDOW_TEXT);

    /** El fondo de un menu. */
    public static final SystemColor menu = new SystemColor((byte) MENU);

    /** El texto de un menu. */
    public static final SystemColor menuText = new SystemColor((byte) MENU_TEXT);

    /** El fondo de un campo de texto. */
    public static final SystemColor text = new SystemColor((byte) TEXT);

    /** El texto de un campo de texto. */
    public static final SystemColor textText = new SystemColor((byte) TEXT_TEXT);

    /** El fondo del texto seleccionado. */
    public static final SystemColor textHighlight = new SystemColor((byte) TEXT_HIGHLIGHT);

    /** El texto seleccionado. */
    public static final SystemColor textHighlightText = new SystemColor((byte) TEXT_HIGHLIGHT_TEXT);

    /** El texto deshabilitado. */
    public static final SystemColor textInactiveText = new SystemColor((byte) TEXT_INACTIVE_TEXT);

    /** El fondo de un control. */
    public static final SystemColor control = new SystemColor((byte) CONTROL);

    /** El texto de un control. */
    public static final SystemColor controlText = new SystemColor((byte) CONTROL_TEXT);

    /** El realce de un control. */
    public static final SystemColor controlHighlight = new SystemColor((byte) CONTROL_HIGHLIGHT);

    /** El realce claro de un control. */
    public static final SystemColor controlLtHighlight =
        new SystemColor((byte) CONTROL_LT_HIGHLIGHT);

    /** La sombra de un control. */
    public static final SystemColor controlShadow = new SystemColor((byte) CONTROL_SHADOW);

    /** La sombra oscura de un control. */
    public static final SystemColor controlDkShadow = new SystemColor((byte) CONTROL_DK_SHADOW);

    /** El fondo del canal de una barra de desplazamiento. */
    public static final SystemColor scrollbar = new SystemColor((byte) SCROLLBAR);

    /** El fondo de una ayuda emergente. */
    public static final SystemColor info = new SystemColor((byte) INFO);

    /** El texto de una ayuda emergente. */
    public static final SystemColor infoText = new SystemColor((byte) INFO_TEXT);

    // Cual de los veintiseis papeles es. Es lo unico que el objeto guarda: el color se busca en la
    // tabla cada vez, que es lo que lo hace vivo.
    private final transient int index;

    private SystemColor(byte index) {
        // El super se construye con el valor de arranque; `getRGB` lo vuelve a leer de la tabla, asi
        // que este numero es solo el estado inicial del Color heredado.
        super(systemColors[index]);
        this.index = index;
    }

    /**
     * El color de hoy para este papel.
     *
     * <p>Se lee de la tabla en cada llamada, no del estado heredado de {@link Color}: eso es lo que
     * hace que el mismo objeto siga siendo correcto despues de un cambio de tema.
     */
    @Override
    public int getRGB() {
        return systemColors[this.index];
    }

    /**
     * El indice del papel, no el color.
     *
     * <p>Imprimir el valor seria enganoso: cambia con el tema, y quien lea el texto pensaria que ese
     * numero identifica al objeto. El indice si lo identifica.
     */
    @Override
    public String toString() {
        return getClass().getName() + "[i=" + this.index + "]";
    }
}
