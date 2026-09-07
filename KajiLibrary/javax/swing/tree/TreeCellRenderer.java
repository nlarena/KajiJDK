package javax.swing.tree;

import java.awt.Component;

import javax.swing.JTree;

/**
 * Quien convierte un nodo del modelo en algo que se pueda dibujar.
 *
 * <p>La misma idea que {@link javax.swing.ListCellRenderer}: devuelve un componente que se usa de
 * sello, uno por fila visible. Ver esa nota.
 *
 * <p>Lo que agrega son los datos que solo un arbol tiene: si el nodo esta desplegado, si es hoja, y
 * en que fila cae. Con eso el dibujante puede elegir el icono de carpeta abierta, el de carpeta
 * cerrada o el de archivo sin preguntarle nada al modelo.
 */
public interface TreeCellRenderer {

    /** El componente que dibuja ese nodo. */
    Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus);
}
