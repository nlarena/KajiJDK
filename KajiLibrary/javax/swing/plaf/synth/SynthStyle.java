package javax.swing.plaf.synth;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.Icon;

/**
 * Como se ve una region: sus colores, su tipografia, sus margenes y quien la dibuja.
 *
 * <h2>Un estilo por region, no por componente</h2>
 *
 * <p>El pulgar de una barra de desplazamiento tiene su propio estilo, distinto del de la pista y del
 * de la barra entera. Eso es lo que permite escribir un aspecto grafico como una tabla de partes en
 * vez de como una clase por componente.
 *
 * <h2>Por que casi todo recibe un {@link SynthContext}</h2>
 *
 * <p>Porque la respuesta depende del estado: el color de un boton apretado no es el mismo que el de
 * uno deshabilitado. El contexto lleva el componente, la region y el estado, y sin los tres no hay
 * respuesta posible.
 *
 * <h2>Los dos abstractos</h2>
 *
 * <p>{@link #getColorForState} y {@link #getFontForState} son lo unico que una subclase esta
 * obligada a escribir. El resto tiene una respuesta razonable por omision, y esa asimetria es
 * deliberada: un estilo tiene que decir de que color es, y puede no tener opinion sobre lo demas.
 *
 * <p>{@link #getColor} envuelve al primero y agrega lo que no depende de la subclase: si el
 * componente esta deshabilitado y no hay color propio para ese estado, se usa el del estado normal.
 * Por eso lo publico no es abstracto y lo abstracto no es publico.
 *
 * @since 1.5
 */
public abstract class SynthStyle {

    /**
     * El pintor que no dibuja nada.
     *
     * <p>Tiene nombre y no es anonima porque va en un inicializador estatico, y ahi nuestro javac
     * todavia no genera la clase sintetica que una anonima necesita.
     */
    private static final class PintorVacio extends SynthPainter {
    }

    private static final SynthGraphicsUtils UTILES = new SynthGraphicsUtils();
    private static final SynthPainter PINTOR = new PintorVacio();

    /** Uno. */
    public SynthStyle() {
    }

    /**
     * Quien hace las cuentas de dibujo.
     *
     * @param context que se esta dibujando
     * @return las utilidades; nunca {@code null}
     */
    public SynthGraphicsUtils getGraphicsUtils(SynthContext context) {
        return UTILES;
    }

    /**
     * El color que le toca.
     *
     * @param context que se esta dibujando y en que estado
     * @param type que color se pide
     * @return el color, o {@code null}
     */
    public Color getColor(SynthContext context, ColorType type) {
        return getColorForState(context, type);
    }

    /**
     * El color que le toca en ese estado.
     *
     * @param context que se esta dibujando y en que estado
     * @param type que color se pide
     * @return el color, o {@code null}
     */
    protected abstract Color getColorForState(SynthContext context, ColorType type);

    /**
     * La tipografia que le toca.
     *
     * @param context que se esta dibujando y en que estado
     * @return la tipografia, o {@code null}
     */
    public Font getFont(SynthContext context) {
        return getFontForState(context);
    }

    /**
     * La tipografia que le toca en ese estado.
     *
     * @param context que se esta dibujando y en que estado
     * @return la tipografia, o {@code null}
     */
    protected abstract Font getFontForState(SynthContext context);

    /**
     * Los margenes de la region.
     *
     * <p>El objeto que se pasa se reusa si no es {@code null}. Es la forma de no crear un objeto por
     * cada consulta en algo que se consulta en cada repintado.
     *
     * @param context que se esta dibujando
     * @param insets donde escribirlos, o {@code null} para uno nuevo
     * @return los margenes
     */
    public Insets getInsets(SynthContext context, Insets insets) {
        if (insets == null) {
            return new Insets(0, 0, 0, 0);
        }
        insets.top = 0;
        insets.bottom = 0;
        insets.left = 0;
        insets.right = 0;
        return insets;
    }

    /**
     * Quien dibuja esta region.
     *
     * @param context que se esta dibujando
     * @return el pintor; nunca {@code null}
     */
    public SynthPainter getPainter(SynthContext context) {
        return PINTOR;
    }

    /**
     * Si la region tapa todo su rectangulo.
     *
     * <p>Decir que si y no hacerlo deja basura en pantalla, porque nadie se molesta en borrar
     * debajo. Por eso lo razonable por omision es que si: una region que no tapa todo lo dice.
     *
     * @param context que se esta dibujando
     * @return cierto si tapa todo
     */
    public boolean isOpaque(SynthContext context) {
        return true;
    }

    /**
     * Un valor cualquiera del estilo, por nombre.
     *
     * <p>Es la puerta de atras: lo que un aspecto quiera guardar y que no encaje en color,
     * tipografia o margen.
     *
     * @param context que se esta dibujando
     * @param key el nombre
     * @return el valor, o {@code null}
     */
    public Object get(SynthContext context, Object key) {
        return null;
    }

    /**
     * Aplica el estilo al componente.
     *
     * <p>Pone el color, el fondo, la tipografia y la opacidad. Solo lo que el estilo tenga: un valor
     * nulo no se instala, para no pisar lo que el programa haya puesto a mano.
     *
     * @param context que se esta instalando
     */
    public void installDefaults(SynthContext context) {
        final javax.swing.JComponent c = context.getComponent();
        final Color frente = getColor(context, ColorType.FOREGROUND);
        if (frente != null) {
            c.setForeground(frente);
        }
        final Color fondo = getColor(context, ColorType.BACKGROUND);
        if (fondo != null) {
            c.setBackground(fondo);
        }
        final Font f = getFont(context);
        if (f != null) {
            c.setFont(f);
        }
        c.setOpaque(isOpaque(context));
    }

    /**
     * Deshace lo que instalo.
     *
     * <p>No hace nada por omision, y no es un olvido: lo que se instalo con
     * {@link #installDefaults} son valores del aspecto, y el proximo aspecto los va a pisar. Una
     * subclase que reserve algo mas --un oyente, un temporizador-- lo suelta aca.
     *
     * @param context que se esta desinstalando
     */
    public void uninstallDefaults(SynthContext context) {
    }

    /**
     * Un valor entero del estilo.
     *
     * @param context que se esta dibujando
     * @param key el nombre
     * @param defaultValue que devolver si no esta
     * @return el valor
     */
    public int getInt(SynthContext context, Object key, int defaultValue) {
        final Object v = get(context, key);
        return v instanceof Number ? ((Number) v).intValue() : defaultValue;
    }

    /**
     * Un valor de verdad del estilo.
     *
     * @param context que se esta dibujando
     * @param key el nombre
     * @param defaultValue que devolver si no esta
     * @return el valor
     */
    public boolean getBoolean(SynthContext context, Object key, boolean defaultValue) {
        final Object v = get(context, key);
        return v instanceof Boolean ? ((Boolean) v).booleanValue() : defaultValue;
    }

    /**
     * Un icono del estilo.
     *
     * @param context que se esta dibujando
     * @param key el nombre
     * @return el icono, o {@code null}
     */
    public Icon getIcon(SynthContext context, Object key) {
        final Object v = get(context, key);
        return v instanceof Icon ? (Icon) v : null;
    }

    /**
     * Un texto del estilo.
     *
     * @param context que se esta dibujando
     * @param key el nombre
     * @param defaultValue que devolver si no esta
     * @return el texto
     */
    public String getString(SynthContext context, Object key, String defaultValue) {
        final Object v = get(context, key);
        return v instanceof String ? (String) v : defaultValue;
    }
}
