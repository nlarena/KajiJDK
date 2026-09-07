package javax.swing;

import javax.swing.event.ListDataListener;

/**
 * Lo que una lista necesita saber de sus datos.
 *
 * <h2>Dos metodos y dos avisos</h2>
 *
 * <p>Cuantos hay y cual es el numero tal. Con eso alcanza para dibujar una lista de un millon de
 * renglones sin tener el millon en memoria: la lista pregunta solo por los que se ven.
 *
 * <p>Los otros dos metodos son para avisar. Sin ellos el modelo podria cambiar y la lista seguiria
 * mostrando lo viejo, porque no tiene forma de darse cuenta sola.
 *
 * @param <E> el tipo de los elementos.
 */
public interface ListModel<E> {

    /** Cuantos elementos hay. */
    int getSize();

    /** El elemento numero tal, contando desde cero. */
    E getElementAt(int index);

    /** Agrega quien quiera enterarse de los cambios. */
    void addListDataListener(ListDataListener l);

    void removeListDataListener(ListDataListener l);
}
