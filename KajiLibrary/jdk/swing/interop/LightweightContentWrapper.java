package jdk.swing.interop;

import java.awt.Component;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DropTarget;
import java.awt.dnd.InvalidDnDOperationException;

import javax.swing.JComponent;

/**
 * Lo que el otro juego de herramientas graficas tiene que saber hacer para hospedar a Swing.
 *
 * <h2>Como se incrusta Swing en algo que no es AWT</h2>
 *
 * <p>No se incrusta: se dibuja aparte y se copia. Swing pinta sobre una matriz de pixeles en memoria
 * --{@link #imageBufferReset} la entrega, {@link #imageUpdated} avisa que parte cambio-- y el otro
 * juego de herramientas la muestra donde quiera. Ninguna ventana del sistema cambia de dueno, que es
 * lo que hace que esto funcione en cualquier plataforma.
 *
 * <p>{@link #paintLock} y {@link #paintUnlock} existen porque los dos lados tocan esa misma matriz
 * desde hilos distintos: uno la escribe cuando repinta, el otro la lee cuando muestra.
 *
 * <h2>Los tamanos</h2>
 *
 * <p>Un componente de Swing sabe cuanto quiere medir, pero quien decide es el contenedor, que aca es
 * del otro lado. Los tres avisos de tamano --{@link #preferredSizeChanged} y los otros dos-- son
 * como esa preferencia cruza la frontera.
 *
 * <h2>El foco</h2>
 *
 * <p>{@link #focusGrabbed} y {@link #focusUngrabbed} son para los menus emergentes: mientras uno
 * esta abierto se lleva todos los eventos del mouse, incluso los que caen afuera, porque hacer clic
 * afuera tiene que cerrarlo.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Todos los metodos son abstractos, tambien en el JDK: esta clase es lo que el hospedador
 * implementa, no lo que se le da hecho.
 *
 * @since 9
 */
public abstract class LightweightContentWrapper {

    /** Uno. */
    public LightweightContentWrapper() {
    }

    /**
     * Entrega la matriz de pixeles sobre la que Swing va a pintar.
     *
     * @param data los pixeles
     * @param width el ancho en pixeles
     * @param height el alto en pixeles
     * @param linestride cuantos enteros hay de una fila a la siguiente
     * @param bufferWidth el ancho de la matriz, que puede ser mayor que el visible
     * @param bufferHeight el alto de la matriz
     */
    public abstract void imageBufferReset(int[] data, int width, int height, int linestride,
            int bufferWidth, int bufferHeight);

    /**
     * Lo mismo, con la escala de la pantalla.
     *
     * <p>La escala llega aparte porque el tamano en pixeles y el tamano en puntos dejaron de ser el
     * mismo numero: en una pantalla al doble de densidad la matriz mide el doble que el componente.
     *
     * @param data los pixeles
     * @param width el ancho en pixeles
     * @param height el alto en pixeles
     * @param linestride cuantos enteros hay de una fila a la siguiente
     * @param bufferWidth el ancho de la matriz
     * @param bufferHeight el alto de la matriz
     * @param scaleX la escala horizontal de la pantalla
     * @param scaleY la escala vertical
     */
    public abstract void imageBufferReset(int[] data, int width, int height, int linestride,
            int bufferWidth, int bufferHeight, double scaleX, double scaleY);

    /**
     * El componente de Swing que se esta hospedando.
     *
     * @return el componente
     */
    public abstract JComponent getComponent();

    /** Toma la matriz de pixeles; nadie mas la toca hasta {@link #paintUnlock}. */
    public abstract void paintLock();

    /** Suelta la matriz de pixeles. */
    public abstract void paintUnlock();

    /**
     * Avisa que el componente cambio de tamano o de lugar.
     *
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param width el ancho
     * @param height el alto
     */
    public abstract void imageReshaped(int x, int y, int width, int height);

    /**
     * Avisa que esa parte de la matriz cambio y hay que volver a mostrarla.
     *
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param width el ancho
     * @param height el alto
     */
    public abstract void imageUpdated(int x, int y, int width, int height);

    /** Avisa que un menu emergente se llevo todos los eventos del mouse. */
    public abstract void focusGrabbed();

    /** Avisa que los devolvio. */
    public abstract void focusUngrabbed();

    /**
     * Avisa cuanto querria medir el componente.
     *
     * @param width el ancho preferido
     * @param height el alto preferido
     */
    public abstract void preferredSizeChanged(int width, int height);

    /**
     * Avisa cuanto es lo mas que puede medir.
     *
     * @param width el ancho maximo
     * @param height el alto maximo
     */
    public abstract void maximumSizeChanged(int width, int height);

    /**
     * Avisa cuanto es lo menos que puede medir.
     *
     * @param width el ancho minimo
     * @param height el alto minimo
     */
    public abstract void minimumSizeChanged(int width, int height);

    /**
     * Un reconocedor de gestos de arrastre del tipo pedido.
     *
     * <p>Lo fabrica el hospedador porque el gesto lo detecta el, con sus eventos de mouse: los de
     * AWT nunca llegan.
     *
     * @param <T> el tipo de reconocedor
     * @param abstractRecognizerClass que tipo de reconocedor se pide
     * @param ds el origen de arrastre al que va a avisarle
     * @param actor el componente que se vigila
     * @param srcActions las acciones que el origen permite
     * @param dgl a quien avisarle cuando el gesto ocurra
     * @return el reconocedor
     */
    public abstract <T extends DragGestureRecognizer> T createDragGestureRecognizer(
            Class<T> abstractRecognizerClass, DragSource ds, Component actor, int srcActions,
            DragGestureListener dgl);

    /**
     * El lado del origen de un arrastre que arranco con ese gesto.
     *
     * @param dge el gesto
     * @return el envoltorio del origen
     * @throws InvalidDnDOperationException si ya hay un arrastre andando
     */
    public abstract DragSourceContextWrapper createDragSourceContext(DragGestureEvent dge)
            throws InvalidDnDOperationException;

    /**
     * Registra un destino de arrastre.
     *
     * @param dt el destino
     */
    public abstract void addDropTarget(DropTarget dt);

    /**
     * Lo saca.
     *
     * @param dt el destino
     */
    public abstract void removeDropTarget(DropTarget dt);
}
