package javax.swing.plaf.synth;

/**
 * Que color de un componente se esta pidiendo.
 *
 * <h2>Por que hacen falta varios</h2>
 *
 * <p>Un componente no tiene un color: tiene el del fondo, el del texto, el del fondo del texto
 * cuando esta seleccionado, y el del recuadro de foco. Pedirlos por nombre --y no por metodos
 * distintos-- es lo que permite escribir un aspecto grafico como una tabla en vez de como codigo.
 *
 * <h2>El identificador</h2>
 *
 * <p>Cada tipo tiene un numero consecutivo, y {@link #MAX_COUNT} dice cuantos hay. Eso es lo que
 * permite guardar los colores de un estilo en un arreglo indexado en vez de en un mapa, que para
 * algo que se consulta en cada repintado no es lo mismo.
 *
 * <p>Se puede definir uno propio heredando: el constructor le asigna el proximo numero. Por eso
 * {@link #MAX_COUNT} se lee al arrancar y no es una constante de compilacion.
 *
 * @since 1.5
 */
public class ColorType {

    private static int proximo;

    /** El color del primer plano. */
    public static final ColorType FOREGROUND = new ColorType("Foreground");

    /** El color del fondo. */
    public static final ColorType BACKGROUND = new ColorType("Background");

    /** El color del texto. */
    public static final ColorType TEXT_FOREGROUND = new ColorType("TextForeground");

    /** El color de fondo del texto. */
    public static final ColorType TEXT_BACKGROUND = new ColorType("TextBackground");

    /** El color con que se marca el foco. */
    public static final ColorType FOCUS = new ColorType("Focus");

    /** Cuantos tipos hay definidos. */
    public static final int MAX_COUNT = Math.max(FOREGROUND.getID(),
            Math.max(BACKGROUND.getID(), Math.max(TEXT_FOREGROUND.getID(),
                    Math.max(TEXT_BACKGROUND.getID(), FOCUS.getID())))) + 1;

    private final String description;
    private final int id;

    /**
     * Un tipo de color con ese nombre.
     *
     * @param description como se llama
     * @throws NullPointerException si {@code description} es {@code null}
     */
    protected ColorType(String description) {
        if (description == null) {
            throw new NullPointerException("ColorType must have a valid description");
        }
        this.description = description;
        synchronized (ColorType.class) {
            this.id = proximo++;
        }
    }

    /**
     * El numero de este tipo.
     *
     * @return el numero, entre cero y {@link #MAX_COUNT} menos uno
     */
    public final int getID() {
        return id;
    }

    /**
     * El nombre.
     *
     * @return como se llama
     */
    @Override
    public String toString() {
        return description;
    }
}
