package javax.swing.table;

import java.awt.Component;

import javax.swing.JTable;

/**
 * Como se dibuja una celda.
 *
 * <h2>Un componente prestado, no uno por celda</h2>
 *
 * <p>El metodo devuelve un {@link Component}, y la tentacion es pensar que cada celda tiene el suyo.
 * No: una tabla de diez mil filas creando diez mil etiquetas seria inviable. Lo que se hace es
 * devolver <strong>siempre el mismo componente</strong>, reconfigurado con el valor de la celda que
 * toca, usarlo para pintar, y pasar a la siguiente.
 *
 * <p>De ahi que el componente devuelto no deba guardarse ni suscribirse a nada: vive lo que dura un
 * pintado. Es el patron que el JDK llama <em>rubber stamp</em>, y explica que todos los parametros
 * —seleccionada, con foco, fila, columna— lleguen juntos: son todo lo que hace falta para
 * reconfigurarlo de una vez.
 */
public interface TableCellRenderer {

    /**
     * Configura y devuelve el componente con el que pintar esa celda.
     *
     * @param isSelected si la celda esta seleccionada
     * @param hasFocus si tiene el foco del teclado
     */
    Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column);
}
