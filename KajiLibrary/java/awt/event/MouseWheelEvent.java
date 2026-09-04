package java.awt.event;

import java.awt.Component;

/**
 * Se movió la rueda del ratón.
 *
 * <p>La rueda no informa píxeles sino **muescas**, y cuánto vale una muesca lo decide el sistema, no
 * la aplicación. Por eso hay dos números: la rotación —cuántas muescas— y la cantidad de
 * desplazamiento —cuántas unidades por muesca quiere el usuario, según su configuración—.
 * {@link #getUnitsToScroll} multiplica los dos y es lo que casi siempre se quiere.
 *
 * <p>{@link #getPreciseWheelRotation} existe para las ruedas y los trackpads que informan
 * fracciones: con ellos {@link #getWheelRotation} redondea y un desplazamiento suave se ve a
 * saltos.
 */
public class MouseWheelEvent extends MouseEvent {

    private static final long serialVersionUID = 6459879390515399677L;

    /** Desplazar por bloques: una pantalla por muesca. */
    public static final int WHEEL_BLOCK_SCROLL = 1;

    /** Desplazar por unidades, según la configuración del usuario. */
    public static final int WHEEL_UNIT_SCROLL = 0;

    private final int scrollType;
    private final int scrollAmount;
    private final int wheelRotation;
    private final double preciseWheelRotation;

    /**
     * Con la rotación entera.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public MouseWheelEvent(Component source, int id, long when, int modifiers, int x, int y,
            int clickCount, boolean popupTrigger, int scrollType, int scrollAmount,
            int wheelRotation) {
        this(source, id, when, modifiers, x, y, 0, 0, clickCount, popupTrigger, scrollType,
                scrollAmount, wheelRotation, wheelRotation);
    }

    /**
     * Como el anterior, con la posición en pantalla.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public MouseWheelEvent(Component source, int id, long when, int modifiers, int x, int y,
            int xAbs, int yAbs, int clickCount, boolean popupTrigger, int scrollType,
            int scrollAmount, int wheelRotation) {
        this(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount, popupTrigger, scrollType,
                scrollAmount, wheelRotation, wheelRotation);
    }

    /**
     * Con la rotación fraccionaria.
     *
     * @throws IllegalArgumentException si la fuente es `null`
     */
    public MouseWheelEvent(Component source, int id, long when, int modifiers, int x, int y,
            int xAbs, int yAbs, int clickCount, boolean popupTrigger, int scrollType,
            int scrollAmount, int wheelRotation, double preciseWheelRotation) {
        super(source, id, when, modifiers, x, y, xAbs, yAbs, clickCount, popupTrigger,
                MouseEvent.NOBUTTON);
        this.scrollType = scrollType;
        this.scrollAmount = scrollAmount;
        this.wheelRotation = wheelRotation;
        this.preciseWheelRotation = preciseWheelRotation;
    }

    /** Por unidades o por bloques. */
    public int getScrollType() {
        return this.scrollType;
    }

    /** Cuántas unidades por muesca quiere el usuario. */
    public int getScrollAmount() {
        return this.scrollAmount;
    }

    /** Cuántas muescas se movió; negativo es hacia arriba. */
    public int getWheelRotation() {
        return this.wheelRotation;
    }

    /** Cuántas muescas se movió, con fracciones. */
    public double getPreciseWheelRotation() {
        return this.preciseWheelRotation;
    }

    /**
     * Cuántas unidades hay que desplazar.
     *
     * @return el producto de las muescas por las unidades, o 0 si el desplazamiento es por bloques
     */
    public int getUnitsToScroll() {
        return this.scrollAmount * this.wheelRotation;
    }

    public String paramString() {
        String tipo;
        if (this.scrollType == WHEEL_UNIT_SCROLL) {
            tipo = "WHEEL_UNIT_SCROLL";
        } else if (this.scrollType == WHEEL_BLOCK_SCROLL) {
            tipo = "WHEEL_BLOCK_SCROLL";
        } else {
            tipo = "unknown scroll type";
        }
        return super.paramString() + ",scrollType=" + tipo + ",scrollAmount=" + this.scrollAmount
                + ",wheelRotation=" + this.wheelRotation + ",preciseWheelRotation="
                + this.preciseWheelRotation;
    }
}
