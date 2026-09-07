package javax.swing;

import java.awt.Component;
import java.awt.event.ActionListener;

/**
 * La parte editable de una lista desplegable.
 *
 * <p>Una lista desplegable editable es dos cosas pegadas: la lista y un campo donde escribir. Este
 * es el campo, y esta aparte para poder reemplazarlo -- por uno con formato, por uno con
 * autocompletado -- sin tocar la lista.
 *
 * <p>{@link #selectAll} esta en la interfaz porque la lista lo llama al abrirse: seleccionar todo
 * hace que escribir reemplace lo que habia, que es lo que se espera al elegir de una lista.
 */
public interface ComboBoxEditor {

    /** El componente donde se escribe. */
    Component getEditorComponent();

    /** Pone ese valor para editar. */
    void setItem(Object anObject);

    /** Lo que hay escrito ahora. */
    Object getItem();

    void selectAll();

    /** Avisa cuando el usuario termina de editar, normalmente con Enter. */
    void addActionListener(ActionListener l);

    void removeActionListener(ActionListener l);
}
