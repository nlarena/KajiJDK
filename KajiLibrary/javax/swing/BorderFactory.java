package javax.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;

import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.StrokeBorder;
import javax.swing.border.TitledBorder;

/**
 * Fabrica bordes, y comparte los que no tienen estado.
 *
 * <h2>Para que existe si los constructores son publicos</h2>
 *
 * <p>Por el compartir. Un borde no guarda a que componente pertenece -- se le pasa el componente en
 * cada dibujada --, asi que dos botones con el mismo borde pueden usar el <em>mismo objeto</em>. Los
 * que no llevan ningun parametro se arman una sola vez y se devuelven siempre, y en una pantalla con
 * doscientos componentes eso es doscientos objetos que no se crean.
 *
 * <p>Los que si llevan parametros se crean cada vez, porque no hay nada que compartir. Nada obliga a
 * pasar por aca: los constructores publicos siguen estando y hacen lo mismo, solo que sin compartir.
 *
 * <h2>Un borde compartido no se puede tocar</h2>
 *
 * <p>Es la contracara y conviene tenerla presente: el que devuelve {@link #createEtchedBorder} lo
 * estan usando otros. Los bordes de esta biblioteca son inmutables, asi que no hay como equivocarse;
 * el cuidado va para quien escriba uno propio.
 */
public class BorderFactory {

    private BorderFactory() {
    }

    static final Border sharedRaisedBevel = new BevelBorder(BevelBorder.RAISED);
    static final Border sharedLoweredBevel = new BevelBorder(BevelBorder.LOWERED);
    static final Border sharedEtchedBorder = new EtchedBorder();
    static final Border emptyBorder = new EmptyBorder(0, 0, 0, 0);

    /** Una linea de un pixel de ese color. */
    public static Border createLineBorder(Color color) {
        return new LineBorder(color, 1);
    }

    /** Una linea de ese grosor. */
    public static Border createLineBorder(Color color, int thickness) {
        return new LineBorder(color, thickness);
    }

    /** Una linea, con las esquinas redondeadas o no. */
    public static Border createLineBorder(Color color, int thickness, boolean rounded) {
        return new LineBorder(color, thickness, rounded);
    }

    /** El relieve que sobresale; compartido. */
    public static Border createRaisedBevelBorder() {
        return createSharedBevel(BevelBorder.RAISED);
    }

    /** El relieve que se hunde; compartido. */
    public static Border createLoweredBevelBorder() {
        return createSharedBevel(BevelBorder.LOWERED);
    }

    /**
     * El relieve de ese tipo.
     *
     * @throws IllegalArgumentException si el tipo no es RAISED ni LOWERED.
     */
    public static Border createBevelBorder(int type) {
        return createSharedBevel(type);
    }

    /** El relieve con esos dos colores. */
    public static Border createBevelBorder(int type, Color highlight, Color shadow) {
        return new BevelBorder(type, highlight, shadow);
    }

    /** El relieve con los cuatro colores puestos a mano. */
    public static Border createBevelBorder(int type, Color highlightOuter, Color highlightInner,
            Color shadowOuter, Color shadowInner) {
        return new BevelBorder(type, highlightOuter, highlightInner, shadowOuter, shadowInner);
    }

    /** El compartido si el tipo es uno de los dos; si no, uno nuevo. */
    static Border createSharedBevel(int type) {
        if (type == BevelBorder.RAISED) {
            return sharedRaisedBevel;
        } else if (type == BevelBorder.LOWERED) {
            return sharedLoweredBevel;
        }
        return null;
    }

    /** El relieve suave que sobresale. */
    public static Border createRaisedSoftBevelBorder() {
        return new SoftBevelBorder(BevelBorder.RAISED);
    }

    /** El relieve suave que se hunde. */
    public static Border createLoweredSoftBevelBorder() {
        return new SoftBevelBorder(BevelBorder.LOWERED);
    }

    /** El relieve suave de ese tipo; nulo si el tipo no es ninguno de los dos. */
    public static Border createSoftBevelBorder(int type) {
        if (type == BevelBorder.RAISED) {
            return createRaisedSoftBevelBorder();
        } else if (type == BevelBorder.LOWERED) {
            return createLoweredSoftBevelBorder();
        }
        return null;
    }

    public static Border createSoftBevelBorder(int type, Color highlight, Color shadow) {
        return new SoftBevelBorder(type, highlight, shadow);
    }

    public static Border createSoftBevelBorder(int type, Color highlightOuter,
            Color highlightInner, Color shadowOuter, Color shadowInner) {
        return new SoftBevelBorder(type, highlightOuter, highlightInner, shadowOuter,
                shadowInner);
    }

    /** El surco hundido; compartido. */
    public static Border createEtchedBorder() {
        return sharedEtchedBorder;
    }

    /** El surco con esos dos colores. */
    public static Border createEtchedBorder(Color highlight, Color shadow) {
        return new EtchedBorder(highlight, shadow);
    }

    /** El surco de ese tipo; el hundido es el compartido. */
    public static Border createEtchedBorder(int type) {
        if (type == EtchedBorder.LOWERED) {
            return sharedEtchedBorder;
        }
        return new EtchedBorder(type);
    }

    public static Border createEtchedBorder(int type, Color highlight, Color shadow) {
        return new EtchedBorder(type, highlight, shadow);
    }

    /** Un titulo, sin borde alrededor. */
    public static TitledBorder createTitledBorder(String title) {
        return new TitledBorder(title);
    }

    /** Ese borde con lugar para un titulo. */
    public static TitledBorder createTitledBorder(Border border) {
        return new TitledBorder(border);
    }

    /** Ese borde con ese titulo. */
    public static TitledBorder createTitledBorder(Border border, String title) {
        return new TitledBorder(border, title);
    }

    /** Con el titulo puesto de ese lado. */
    public static TitledBorder createTitledBorder(Border border, String title,
            int titleJustification, int titlePosition) {
        return new TitledBorder(border, title, titleJustification, titlePosition);
    }

    /** Y con esa tipografia. */
    public static TitledBorder createTitledBorder(Border border, String title,
            int titleJustification, int titlePosition, Font titleFont) {
        return new TitledBorder(border, title, titleJustification, titlePosition, titleFont);
    }

    /** Y con ese color. */
    public static TitledBorder createTitledBorder(Border border, String title,
            int titleJustification, int titlePosition, Font titleFont, Color titleColor) {
        return new TitledBorder(border, title, titleJustification, titlePosition, titleFont,
                titleColor);
    }

    /**
     * Un borde que no se ve y no ocupa nada; compartido.
     *
     * <p>Sirve para sacarle el borde a un componente sin dejarlo en nulo: nulo significa "el que
     * ponga el aspecto", y esto significa "ninguno".
     */
    public static Border createEmptyBorder() {
        return emptyBorder;
    }

    /** Un borde invisible que igual ocupa ese lugar; es como se pone margen sin acomodador. */
    public static Border createEmptyBorder(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    /** Dos bordes vacios, uno adentro del otro. */
    public static CompoundBorder createCompoundBorder() {
        return new CompoundBorder();
    }

    /** Uno adentro del otro; el de afuera se dibuja primero. */
    public static CompoundBorder createCompoundBorder(Border outsideBorder,
            Border insideBorder) {
        return new CompoundBorder(outsideBorder, insideBorder);
    }

    /** Un marco de ese color con esos cuatro grosores. */
    public static MatteBorder createMatteBorder(int top, int left, int bottom, int right,
            Color color) {
        return new MatteBorder(top, left, bottom, right, color);
    }

    /** Un marco hecho repitiendo ese icono. */
    public static MatteBorder createMatteBorder(int top, int left, int bottom, int right,
            Icon tileIcon) {
        return new MatteBorder(top, left, bottom, right, tileIcon);
    }

    /**
     * Un borde dibujado con ese trazo, del color del componente.
     *
     * @throws NullPointerException si el trazo es nulo
     */
    public static Border createStrokeBorder(BasicStroke stroke) {
        return new StrokeBorder(stroke);
    }

    /**
     * Un borde dibujado con ese trazo y esa pintura.
     *
     * @throws NullPointerException si el trazo es nulo
     */
    public static Border createStrokeBorder(BasicStroke stroke, Paint paint) {
        return new StrokeBorder(stroke, paint);
    }

    /**
     * Una linea de guiones de esa pintura, con las medidas de siempre.
     *
     * @throws NullPointerException si la pintura es nula
     */
    public static Border createDashedBorder(Paint paint) {
        return createDashedBorder(paint, 1.0f, 1.0f, 1.0f, false);
    }

    /**
     * Guiones de ese largo y con ese hueco.
     *
     * @throws NullPointerException si la pintura es nula
     * @throws IllegalArgumentException si alguna medida no es positiva
     */
    public static Border createDashedBorder(Paint paint, float length, float spacing) {
        return createDashedBorder(paint, 1.0f, length, spacing, false);
    }

    /**
     * El borde de guiones completo.
     *
     * <p>Con {@code rounded} en cierto los guiones llevan las puntas y las esquinas redondeadas; el
     * largo y el hueco se miden en multiplos del grosor, no en pixeles, para que un borde mas grueso
     * lleve guiones proporcionalmente mas largos.
     *
     * @throws NullPointerException si la pintura es nula
     * @throws IllegalArgumentException si alguna medida no es positiva
     */
    public static Border createDashedBorder(Paint paint, float thickness, float length,
            float spacing, boolean rounded) {
        boolean shared = !rounded && thickness == 1.0f && length == 1.0f && spacing == 1.0f;
        if (shared && paint == null) {
            // Sin pintura y con las medidas de siempre no hay nada que distinga a este borde de
            // otro igual, asi que se podria compartir. No se comparte: el JDK arma uno nuevo, y
            // devolver el mismo objeto cambiaria una comparacion por identidad que alguien puede
            // estar haciendo.
            return new StrokeBorder(trazo(thickness, length, spacing, rounded), null);
        }
        return new StrokeBorder(trazo(thickness, length, spacing, rounded), paint);
    }

    /** El trazo punteado que corresponde a esas medidas. */
    private static BasicStroke trazo(float thickness, float length, float spacing,
            boolean rounded) {
        int cap = rounded ? BasicStroke.CAP_ROUND : BasicStroke.CAP_SQUARE;
        int join = rounded ? BasicStroke.JOIN_ROUND : BasicStroke.JOIN_MITER;
        float[] array = {thickness * length, thickness * spacing};
        return new BasicStroke(thickness, cap, join, thickness * 2.0f, array, 0.0f);
    }
}
