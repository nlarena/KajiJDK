package javax.swing;

import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;

/**
 * Quien sabe copiar, pegar y arrastrar el contenido de un componente.
 *
 * <h2>La idea</h2>
 *
 * <p>Un componente no sabe copiarse: sabe {@code TransferHandler}. Eso permite que el mismo
 * componente se copie de una forma en una aplicacion y de otra en otra, y que copiar y arrastrar
 * compartan el mismo codigo, porque los dos terminan en un {@link Transferable}.
 *
 * <h2>Lo que hay aca</h2>
 *
 * <p>Esta VM no tiene portapapeles del sistema ni arrastre entre ventanas: no hay pantalla ni
 * gestor de ventanas con quien negociar. Las operaciones que necesitan uno —{@link #exportToClipboard},
 * {@link #importData}, {@link #exportAsDrag}— lo dicen y no hacen nada. Lo que si esta completo es
 * la <em>forma</em>: los tipos, las acciones, el lugar de caida ({@link DropLocation}) y la
 * consulta de si algo se podria importar, que es lo que un componente pregunta para decidir si
 * marca el destino.
 */
public class TransferHandler implements Serializable {

    /** Ninguna operacion. */
    public static final int NONE = 0;

    /** Copiar: el origen se queda con lo suyo. */
    public static final int COPY = 1;

    /** Mover: el origen lo pierde. */
    public static final int MOVE = 2;

    /** Cualquiera de las dos; la decide quien recibe. */
    public static final int COPY_OR_MOVE = 3;

    /** Un enlace a lo que hay en el origen. */
    public static final int LINK = 1073741824;

    private String propertyName;

    private static final Action cutAction = new TransferAction("cut");
    private static final Action copyAction = new TransferAction("copy");
    private static final Action pasteAction = new TransferAction("paste");

    /**
     * Un manejador que transfiere una sola propiedad del componente por su nombre.
     *
     * <p>Es la forma barata de que un componente sea copiable: en vez de escribir un manejador,
     * se dice "lo que se copia de esto es su propiedad texto".
     */
    public TransferHandler(String property) {
        propertyName = property;
    }

    protected TransferHandler() {
        this(null);
    }

    /** La accion de cortar, para poner en un menu o un boton. */
    public static Action getCutAction() {
        return cutAction;
    }

    public static Action getCopyAction() {
        return copyAction;
    }

    public static Action getPasteAction() {
        return pasteAction;
    }

    /**
     * Si alguno de esos tipos se puede importar a ese componente.
     *
     * @deprecated es {@link #canImport(TransferSupport)}, que ademas sabe donde va a caer.
     */
    @Deprecated
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        return false;
    }

    /** Si lo que se esta arrastrando se puede soltar ahi. */
    public boolean canImport(TransferSupport support) {
        return false;
    }

    /** Que operaciones admite el origen; ninguna sin manejador propio. */
    public int getSourceActions(JComponent c) {
        return NONE;
    }

    /**
     * Lo que hay que transferir desde ese componente.
     *
     * <p>{@code null} si no hay nada; con la propiedad por nombre, el valor de esa propiedad.
     */
    protected Transferable createTransferable(JComponent c) {
        return null;
    }

    /** Se termino de exportar; con {@code MOVE} es donde el origen borra lo suyo. */
    protected void exportDone(JComponent source, Transferable data, int action) {
    }

    /** No hay portapapeles del sistema en esta VM; ver la nota de la clase. */
    public void exportToClipboard(JComponent comp, Clipboard clip, int action) {
        throw new UnsupportedOperationException("esta VM no tiene portapapeles del sistema");
    }

    /** No hay arrastre en esta VM; ver la nota de la clase. */
    public void exportAsDrag(JComponent comp, java.awt.event.InputEvent e, int action) {
        throw new UnsupportedOperationException("esta VM no tiene arrastre");
    }

    /** @deprecated es {@link #importData(TransferSupport)}. */
    @Deprecated
    public boolean importData(JComponent comp, Transferable t) {
        return false;
    }

    /** Nada que importar sin portapapeles ni arrastre; ver la nota de la clase. */
    public boolean importData(TransferSupport support) {
        return false;
    }

    /** La imagen que acompana al cursor mientras se arrastra; ninguna. */
    public void setDragImage(java.awt.Image img) {
    }

    public java.awt.Image getDragImage() {
        return null;
    }

    public void setDragImageOffset(Point p) {
    }

    public Point getDragImageOffset() {
        return new Point(0, 0);
    }

    /**
     * Donde va a caer lo que se esta arrastrando.
     *
     * <p>Cada componente define su propia subclase con lo que le importa —una fila, un indice de
     * texto—; esta solo lleva el punto.
     */
    /**
     * La imagen que se arrastra bajo el puntero.
     *
     * <p>Nulo -- que es lo de siempre -- deja que el sistema muestre su cursor de arrastre. Una
     * subclase devuelve un icono cuando quiere que se vea lo que se esta moviendo.
     */
    public Icon getVisualRepresentation(java.awt.datatransfer.Transferable t) {
        return null;
    }

    public static class DropLocation {

        private final Point dropPoint;

        protected DropLocation(Point dropPoint) {
            if (dropPoint == null) {
                throw new IllegalArgumentException("Location cannot be null");
            }
            this.dropPoint = new Point(dropPoint);
        }

        public final Point getDropPoint() {
            return new Point(dropPoint);
        }

        public String toString() {
            return getClass().getName() + "[dropPoint=" + dropPoint + "]";
        }
    }

    /**
     * El contexto de una transferencia: quien la recibe, que trae y donde cae.
     *
     * <p>Se pasa a {@link #canImport(TransferSupport)} y a {@link #importData(TransferSupport)}
     * en vez de tres argumentos sueltos, para que agregar informacion mas adelante no cambie la
     * firma.
     */
    public static final class TransferSupport {

        private boolean isDrop;
        private java.awt.Component component;
        private DropLocation dropLocation;
        private int dropAction = -1;
        private boolean showDropLocationIsSet;
        private boolean showDropLocation;
        private int sourceSupportedActions;
        private Transferable transferable;

        /** Un contexto de pegado —no de arrastre— sobre ese componente. */
        public TransferSupport(java.awt.Component component, Transferable transferable) {
            if (component == null || transferable == null) {
                throw new NullPointerException("component and transferable must be non-null");
            }
            this.component = component;
            this.transferable = transferable;
            this.isDrop = false;
        }

        /** Si viene de un arrastre; si no, de pegar. */
        public boolean isDrop() {
            return isDrop;
        }

        public java.awt.Component getComponent() {
            return component;
        }

        /** Donde cae; solo tiene sentido en un arrastre. */
        public DropLocation getDropLocation() {
            assureIsDrop();
            return dropLocation;
        }

        /** Si el componente tiene que marcar el destino mientras se arrastra. */
        public void setShowDropLocation(boolean showDropLocation) {
            assureIsDrop();
            this.showDropLocationIsSet = true;
            this.showDropLocation = showDropLocation;
        }

        /** Elige entre copiar y mover cuando el origen admite las dos. */
        public void setDropAction(int dropAction) {
            assureIsDrop();
            this.dropAction = dropAction;
        }

        public int getDropAction() {
            return dropAction == -1 ? getUserDropAction() : dropAction;
        }

        /** La que pidio el usuario con los modificadores del teclado. */
        public int getUserDropAction() {
            assureIsDrop();
            return NONE;
        }

        public int getSourceDropActions() {
            assureIsDrop();
            return sourceSupportedActions;
        }

        public DataFlavor[] getDataFlavors() {
            return transferable.getTransferDataFlavors();
        }

        public boolean isDataFlavorSupported(DataFlavor df) {
            return transferable.isDataFlavorSupported(df);
        }

        public Transferable getTransferable() {
            return transferable;
        }

        private void assureIsDrop() {
            if (!isDrop) {
                throw new IllegalStateException("Not a drop");
            }
        }
    }

    /** Las tres acciones de menu; sin portapapeles, no hacen nada. */
    static class TransferAction extends AbstractAction implements Serializable {

        TransferAction(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    /**
     * Lo implementa lo que tiene un {@link TransferHandler} sin ser un {@link JComponent}.
     *
     * <p>Son las ventanas: {@code JDialog}, {@code JFrame} y {@code JWindow} pueden tener uno y no
     * heredan de {@code JComponent}. Sin esta interfaz, el codigo que busca el manejador de un
     * componente tendria que preguntar por cada clase de ventana.
     *
     * <p>No es publica: es un detalle de como Swing encuentra el manejador, no algo que un programa
     * deba implementar.
     */
    interface HasGetTransferHandler {

        /** El manejador, o nulo. */
        TransferHandler getTransferHandler();
    }
}
