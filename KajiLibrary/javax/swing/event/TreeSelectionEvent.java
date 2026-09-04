package javax.swing.event;

import java.util.EventObject;

import javax.swing.tree.TreePath;

/**
 * La seleccion del arbol cambio.
 *
 * <h2>Trae la DIFERENCIA, no la seleccion</h2>
 *
 * <p>{@link #getPaths} devuelve los caminos que <em>cambiaron de estado</em>, y el arreglo paralelo
 * {@code areNew} dice si cada uno entro o salio. No es la seleccion actual: para eso hay que
 * preguntarle al arbol.
 *
 * <p>Traer la diferencia es lo que hace barato seleccionar mil filas — el evento describe lo que se
 * movio, no lo que quedo.
 *
 * <p>El camino <em>lider</em> es el ultimo que el usuario toco, y viene con su valor anterior:
 * mueve el foco del teclado, y quien dibuja necesita repintar los dos.
 */
public class TreeSelectionEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** Los caminos que cambiaron de estado. */
    protected TreePath[] paths;

    /** Para cada uno, si entro ({@code true}) o salio. */
    protected boolean[] areNew;

    /** El lider anterior. */
    protected TreePath oldLeadSelectionPath;

    /** El lider nuevo. */
    protected TreePath newLeadSelectionPath;

    /** Con varios caminos. */
    public TreeSelectionEvent(Object source, TreePath[] paths, boolean[] areNew,
            TreePath oldLeadSelectionPath, TreePath newLeadSelectionPath) {
        super(source);
        this.paths = paths;
        this.areNew = areNew;
        this.oldLeadSelectionPath = oldLeadSelectionPath;
        this.newLeadSelectionPath = newLeadSelectionPath;
    }

    /** Con uno solo. */
    public TreeSelectionEvent(Object source, TreePath path, boolean isNew,
            TreePath oldLeadSelectionPath, TreePath newLeadSelectionPath) {
        super(source);
        this.paths = new TreePath[1];
        this.paths[0] = path;
        this.areNew = new boolean[1];
        this.areNew[0] = isNew;
        this.oldLeadSelectionPath = oldLeadSelectionPath;
        this.newLeadSelectionPath = newLeadSelectionPath;
    }

    /** Los caminos que cambiaron, en un arreglo nuevo. */
    public TreePath[] getPaths() {
        int n = this.paths.length;
        TreePath[] copia = new TreePath[n];
        for (int i = 0; i < n; i++) {
            copia[i] = this.paths[i];
        }
        return copia;
    }

    /** El primero de los caminos que cambiaron. */
    public TreePath getPath() {
        return this.paths[0];
    }

    /** Si {@link #getPath} entro en la seleccion. */
    public boolean isAddedPath() {
        return this.areNew[0];
    }

    /** Si ese camino entro en la seleccion. */
    public boolean isAddedPath(TreePath path) {
        for (int i = this.paths.length - 1; i >= 0; i--) {
            if (this.paths[i].equals(path)) {
                return this.areNew[i];
            }
        }
        throw new IllegalArgumentException("ese camino no esta en el evento");
    }

    /** Si el camino numero {@code index} entro en la seleccion. */
    public boolean isAddedPath(int index) {
        if (this.paths == null || index < 0 || index >= this.paths.length) {
            throw new IllegalArgumentException("indice fuera de rango");
        }
        return this.areNew[index];
    }

    /** El lider anterior. */
    public TreePath getOldLeadSelectionPath() {
        return this.oldLeadSelectionPath;
    }

    /** El lider nuevo. */
    public TreePath getNewLeadSelectionPath() {
        return this.newLeadSelectionPath;
    }

    /** Una copia con otro origen, para reenviarlo. */
    public Object cloneWithSource(Object newSource) {
        return new TreeSelectionEvent(newSource, this.paths, this.areNew,
                this.oldLeadSelectionPath, this.newLeadSelectionPath);
    }
}
