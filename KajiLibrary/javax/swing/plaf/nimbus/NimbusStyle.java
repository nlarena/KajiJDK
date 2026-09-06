package javax.swing.plaf.nimbus;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.Painter;
import javax.swing.UIManager;
import javax.swing.plaf.synth.ColorType;
import javax.swing.plaf.synth.SynthContext;
import javax.swing.plaf.synth.SynthPainter;
import javax.swing.plaf.synth.SynthStyle;

/**
 * El estilo de Nimbus: lee de la tabla en vez de tener valores propios.
 *
 * <h2>De donde saca todo</h2>
 *
 * <p>De {@link UIManager}, con claves armadas por convencion: el color de fondo de un boton
 * apretado esta bajo {@code "Button[Pressed].background"}. Nimbus no guarda una copia; consulta.
 * Eso es lo que hace que cambiar un valor de la tabla se vea inmediatamente en pantalla.
 *
 * <h2>Los tres tamanos</h2>
 *
 * <p>Un componente puede pedir ser grande, chico o miniatura poniendose una propiedad
 * --{@link #LARGE_KEY} y las otras dos-- y Nimbus le escala la tipografia por el factor
 * correspondiente. Es lo que en otros aspectos habria que hacer a mano componente por componente.
 *
 * <p>Los factores no son redondos porque no son una eleccion de gusto: salen de las proporciones de
 * la guia de interfaz de la que Nimbus toma su tamano de referencia.
 *
 * <h2>Los tres pintores</h2>
 *
 * <p>Fondo, primer plano y borde se piden por separado y cada uno puede faltar. Que sean
 * {@link Painter} y no un {@link SynthPainter} es la diferencia central entre Nimbus y el resto de
 * synth: un pintor recibe el tamano en cada llamada y dibuja una figura, en vez de estampar una
 * imagen.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La busqueda en la tabla es real y funciona: se puede poner un color bajo la clave que
 * corresponde y este estilo lo devuelve. Lo que no hay son los pintores concretos de Nimbus, que
 * son las noventa clases privadas del paquete, asi que los tres {@code getPainter} devuelven lo que
 * haya en la tabla y {@code null} si no hay nada.
 *
 * @since 1.7
 */
public final class NimbusStyle extends SynthStyle {

    /** La propiedad con la que un componente pide ser grande. */
    public static final String LARGE_KEY = "large";

    /** La propiedad con la que pide ser chico. */
    public static final String SMALL_KEY = "small";

    /** La propiedad con la que pide ser miniatura. */
    public static final String MINI_KEY = "mini";

    /** Cuanto se agranda la tipografia con {@link #LARGE_KEY}. */
    public static final double LARGE_SCALE = 1.15;

    /** Cuanto se achica con {@link #SMALL_KEY}. */
    public static final double SMALL_SCALE = 0.857;

    /** Cuanto se achica con {@link #MINI_KEY}. */
    public static final double MINI_SCALE = 0.714;

    /**
     * Aplica el estilo al componente.
     *
     * @param ctx que se esta instalando
     */
    @Override
    public void installDefaults(SynthContext ctx) {
        super.installDefaults(ctx);
    }

    /**
     * Los margenes de la region.
     *
     * @param ctx que se esta dibujando
     * @param insets donde escribirlos, o {@code null} para uno nuevo
     * @return los margenes
     */
    @Override
    public Insets getInsets(SynthContext ctx, Insets insets) {
        final Object v = get(ctx, "contentMargins");
        if (!(v instanceof Insets)) {
            return super.getInsets(ctx, insets);
        }
        final Insets m = (Insets) v;
        if (insets == null) {
            return new Insets(m.top, m.left, m.bottom, m.right);
        }
        insets.top = m.top;
        insets.left = m.left;
        insets.bottom = m.bottom;
        insets.right = m.right;
        return insets;
    }

    /**
     * El color que le toca en ese estado.
     *
     * @param ctx que se esta dibujando y en que estado
     * @param type que color se pide
     * @return el color, o {@code null}
     */
    @Override
    protected Color getColorForState(SynthContext ctx, ColorType type) {
        final Object v = get(ctx, nombreDe(type));
        return v instanceof Color ? (Color) v : null;
    }

    /**
     * La tipografia que le toca en ese estado.
     *
     * <p>Si el componente pidio un tamano con una de las tres propiedades, la tipografia sale
     * escalada por el factor correspondiente.
     *
     * @param ctx que se esta dibujando y en que estado
     * @return la tipografia, o {@code null}
     */
    @Override
    protected Font getFontForState(SynthContext ctx) {
        final Object v = get(ctx, "font");
        if (!(v instanceof Font)) {
            return null;
        }
        final Font f = (Font) v;
        final double escala = escalaDe(ctx);
        return escala == 1.0 ? f : f.deriveFont((float) (f.getSize2D() * escala));
    }

    /**
     * Quien dibuja esta region.
     *
     * @param ctx que se esta dibujando
     * @return el pintor
     */
    @Override
    public SynthPainter getPainter(SynthContext ctx) {
        return super.getPainter(ctx);
    }

    /**
     * Si la region tapa todo su rectangulo.
     *
     * @param ctx que se esta dibujando
     * @return lo que diga la tabla, o falso
     */
    @Override
    public boolean isOpaque(SynthContext ctx) {
        final Object v = get(ctx, "opaque");
        return v instanceof Boolean ? ((Boolean) v).booleanValue() : false;
    }

    /**
     * Un valor cualquiera del estilo.
     *
     * <p>La clave se arma con la region, el estado y el nombre; ver la nota de la clase. Si no esta
     * la version con estado se prueba la version sin el, que es como se escribe un valor que vale
     * para todos los estados.
     *
     * @param ctx que se esta dibujando
     * @param key el nombre
     * @return el valor, o {@code null}
     */
    @Override
    public Object get(SynthContext ctx, Object key) {
        final String region = ctx.getRegion().getName();
        final String nombre = String.valueOf(key);
        final Object conEstado = UIManager.get(region + estadoDe(ctx) + "." + nombre);
        return conEstado != null ? conEstado : UIManager.get(region + "." + nombre);
    }

    /**
     * El pintor del fondo.
     *
     * @param ctx que se esta dibujando
     * @return el pintor, o {@code null}
     */
    @SuppressWarnings("unchecked")
    public Painter<Object> getBackgroundPainter(SynthContext ctx) {
        final Object v = get(ctx, "backgroundPainter");
        return v instanceof Painter ? (Painter<Object>) v : null;
    }

    /**
     * El pintor del primer plano.
     *
     * @param ctx que se esta dibujando
     * @return el pintor, o {@code null}
     */
    @SuppressWarnings("unchecked")
    public Painter<Object> getForegroundPainter(SynthContext ctx) {
        final Object v = get(ctx, "foregroundPainter");
        return v instanceof Painter ? (Painter<Object>) v : null;
    }

    /**
     * El pintor del borde.
     *
     * @param ctx que se esta dibujando
     * @return el pintor, o {@code null}
     */
    @SuppressWarnings("unchecked")
    public Painter<Object> getBorderPainter(SynthContext ctx) {
        final Object v = get(ctx, "borderPainter");
        return v instanceof Painter ? (Painter<Object>) v : null;
    }

    /** El nombre con que un tipo de color aparece en la tabla. */
    private static String nombreDe(ColorType type) {
        if (type == ColorType.BACKGROUND) {
            return "background";
        }
        if (type == ColorType.FOREGROUND) {
            return "foreground";
        }
        if (type == ColorType.TEXT_BACKGROUND) {
            return "textBackground";
        }
        if (type == ColorType.TEXT_FOREGROUND) {
            return "textForeground";
        }
        if (type == ColorType.FOCUS) {
            return "focus";
        }
        return String.valueOf(type);
    }

    /**
     * La parte de la clave que nombra el estado, como {@code "[Pressed]"}.
     *
     * <p>Vacia para el estado normal: en la tabla, lo que vale siempre se escribe sin corchetes.
     */
    private static String estadoDe(SynthContext ctx) {
        final int s = ctx.getComponentState();
        if ((s & javax.swing.plaf.synth.SynthConstants.DISABLED) != 0) {
            return "[Disabled]";
        }
        if ((s & javax.swing.plaf.synth.SynthConstants.PRESSED) != 0) {
            return "[Pressed]";
        }
        if ((s & javax.swing.plaf.synth.SynthConstants.MOUSE_OVER) != 0) {
            return "[MouseOver]";
        }
        if ((s & javax.swing.plaf.synth.SynthConstants.FOCUSED) != 0) {
            return "[Focused]";
        }
        if ((s & javax.swing.plaf.synth.SynthConstants.SELECTED) != 0) {
            return "[Selected]";
        }
        return "";
    }

    /** Por cuanto hay que multiplicar la tipografia, segun lo que el componente haya pedido. */
    private static double escalaDe(SynthContext ctx) {
        final Object tam = ctx.getComponent().getClientProperty("JComponent.sizeVariant");
        if (LARGE_KEY.equals(tam)) {
            return LARGE_SCALE;
        }
        if (SMALL_KEY.equals(tam)) {
            return SMALL_SCALE;
        }
        if (MINI_KEY.equals(tam)) {
            return MINI_SCALE;
        }
        return 1.0;
    }
}
