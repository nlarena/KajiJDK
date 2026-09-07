package javax.swing;

import javax.swing.event.ChangeListener;

/**
 * Un valor con un rango y un ancho: lo que hay detras de una barra de desplazamiento, una barra de
 * progreso o un deslizador.
 *
 * <h2>Cuatro numeros con una regla</h2>
 *
 * <p>Minimo, valor, extension y maximo, siempre en ese orden:
 *
 * <pre>{@code   minimo <= valor <= valor + extension <= maximo}</pre>
 *
 * <p>La <em>extension</em> es lo que se ve de una vez: en una barra de desplazamiento es el alto
 * de la ventana sobre el alto del documento, y es por eso que el pulgar tiene tamano. Un
 * deslizador usa extension cero, y entonces el valor puede llegar hasta el maximo.
 *
 * <p>La regla se mantiene sola: quien pone un numero que la rompe no recibe un error, recibe los
 * numeros acomodados. Subir el minimo por encima del valor arrastra al valor; achicar el maximo
 * achica primero la extension y despues el valor. Es deliberado: un modelo que lanzara excepciones
 * obligaria a cada llamador a ordenar sus cambios, y el orden depende de hacia donde se mueva.
 *
 * <p>{@link #setValueIsAdjusting} marca los cambios de una serie —arrastrar el pulgar— para que
 * quien escucha pueda esperar a que suelte antes de hacer algo caro.
 */
public interface BoundedRangeModel {

    int getMinimum();

    void setMinimum(int newMinimum);

    int getMaximum();

    void setMaximum(int newMaximum);

    int getValue();

    void setValue(int newValue);

    /** Marca el comienzo o el final de una serie de cambios; ver la nota de la interfaz. */
    void setValueIsAdjusting(boolean b);

    boolean getValueIsAdjusting();

    /** Lo que se ve de una vez; ver la nota de la interfaz. */
    int getExtent();

    void setExtent(int newExtent);

    /** Cambia los cuatro numeros de una vez, avisando una sola vez. */
    void setRangeProperties(int value, int extent, int min, int max, boolean adjusting);

    void addChangeListener(ChangeListener x);

    void removeChangeListener(ChangeListener x);
}
