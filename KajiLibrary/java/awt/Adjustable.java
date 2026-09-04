package java.awt;

import java.awt.event.AdjustmentListener;

/**
 * Algo que representa un valor dentro de un rango y se puede mover: una barra de desplazamiento.
 *
 * <p>El modelo tiene cuatro números y conviene distinguirlos. El **valor** es dónde está; el
 * **mínimo** y el **máximo** son los extremos; y la **extensión visible** es cuánto se ve de una
 * vez, que es lo que hace que el pulgar de una barra tenga tamaño en vez de ser un punto.
 *
 * <p>De la extensión sale una regla que sorprende: el valor nunca llega al máximo. Con un rango de 0
 * a 100 y una extensión de 20, el valor máximo posible es 80, porque desde ahí ya se está viendo
 * hasta el 100.
 */
public interface Adjustable {

    /** Orientación horizontal. */
    int HORIZONTAL = 0;

    /** Orientación vertical. */
    int VERTICAL = 1;

    /** Sin orientación definida. */
    int NO_ORIENTATION = 2;

    /** Horizontal o vertical. */
    int getOrientation();

    /** Cambia el extremo inferior del rango. */
    void setMinimum(int min);

    /** El extremo inferior del rango. */
    int getMinimum();

    /** Cambia el extremo superior del rango. */
    void setMaximum(int max);

    /** El extremo superior del rango. */
    int getMaximum();

    /** Cambia cuánto se mueve con un paso chico. */
    void setUnitIncrement(int u);

    /** Cuánto se mueve con un paso chico. */
    int getUnitIncrement();

    /** Cambia cuánto se mueve con un paso grande. */
    void setBlockIncrement(int b);

    /** Cuánto se mueve con un paso grande. */
    int getBlockIncrement();

    /** Cambia cuánto se ve de una vez. */
    void setVisibleAmount(int v);

    /** Cuánto se ve de una vez. */
    int getVisibleAmount();

    /**
     * Cambia dónde está.
     *
     * <p>Un valor fuera de `[mínimo, máximo - extensión]` se recorta a ese rango.
     */
    void setValue(int v);

    /** Dónde está. */
    int getValue();

    /** Suma alguien a quien avisarle de los cambios. */
    void addAdjustmentListener(AdjustmentListener l);

    /** Saca a ese oyente. */
    void removeAdjustmentListener(AdjustmentListener l);
}
