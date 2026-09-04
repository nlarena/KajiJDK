package java.awt.datatransfer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Un lugar donde dejar algo para que otro lo tome.
 *
 * <p>Tiene **un** contenido y **un** dueño por vez. Poner algo nuevo desaloja lo anterior y le avisa
 * a su dueño, que es la única señal que tiene para soltar lo que estaba guardando.
 *
 * <p>Esta clase es un portapapeles **privado**: vive dentro del programa y sirve para mover datos
 * entre partes de la misma aplicación. El del sistema, el que se comparte con otros programas, lo
 * entrega el {@code Toolkit}, y ahí es donde entra {@link FlavorMap} a traducir formatos.
 *
 * <p>{@link #getContents} toma un parámetro que **no usa**. Está en la API desde 1.1, donde iba a
 * servir para identificar quién pedía; nunca se usó para nada y quedó. Pasarle `null` es lo normal.
 */
public class Clipboard {

    /** El dueño actual. */
    protected ClipboardOwner owner;

    /** Lo que hay adentro. */
    protected Transferable contents;

    private final String name;
    private final List<FlavorListener> flavorListeners = new ArrayList<FlavorListener>();

    /** Con el nombre dado, para poder distinguirlo en la depuración. */
    public Clipboard(String name) {
        this.name = name;
    }

    /** Cómo se llama. */
    public String getName() {
        return this.name;
    }

    /**
     * Pone contenido nuevo y desaloja al dueño anterior.
     *
     * <p>El aviso al dueño anterior sale **antes** de cambiar el contenido, para que todavía pueda
     * mirar lo que estaba guardando.
     */
    public synchronized void setContents(Transferable contents, ClipboardOwner owner) {
        ClipboardOwner anterior = this.owner;
        Transferable anteriorContenido = this.contents;
        this.owner = owner;
        this.contents = contents;
        if (anterior != null && anterior != owner) {
            anterior.lostOwnership(this, anteriorContenido);
        }
        this.avisarCambio();
    }

    /**
     * Lo que hay adentro.
     *
     * @param requestor no se usa; está en la API desde 1.1 y nunca tuvo efecto
     * @return el contenido, o `null` si no hay
     */
    public synchronized Transferable getContents(Object requestor) {
        return this.contents;
    }

    /**
     * En qué formatos se puede pedir lo que hay.
     *
     * @throws IllegalStateException si el portapapeles no está disponible
     */
    public DataFlavor[] getAvailableDataFlavors() {
        Transferable c = this.getContents(null);
        if (c == null) {
            return new DataFlavor[0];
        }
        return c.getTransferDataFlavors();
    }

    /**
     * Si lo que hay se puede pedir en ese formato.
     *
     * @throws NullPointerException si el formato es `null`
     * @throws IllegalStateException si el portapapeles no está disponible
     */
    public boolean isDataFlavorAvailable(DataFlavor flavor) {
        if (flavor == null) {
            throw new NullPointerException("flavor");
        }
        Transferable c = this.getContents(null);
        if (c == null) {
            return false;
        }
        return c.isDataFlavorSupported(flavor);
    }

    /**
     * Lo que hay, en ese formato.
     *
     * @throws NullPointerException si el formato es `null`
     * @throws IllegalStateException si el portapapeles no está disponible
     * @throws UnsupportedFlavorException si lo que hay no se puede dar en ese formato
     * @throws IOException si los datos ya no están
     */
    public Object getData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor == null) {
            throw new NullPointerException("flavor");
        }
        Transferable c = this.getContents(null);
        if (c == null) {
            throw new UnsupportedFlavorException(flavor);
        }
        return c.getTransferData(flavor);
    }

    /**
     * Suma alguien a quien avisarle de los cambios.
     *
     * <p>Un `null` se ignora en silencio, que es lo que hace el JDK.
     */
    public synchronized void addFlavorListener(FlavorListener listener) {
        if (listener == null) {
            return;
        }
        this.flavorListeners.add(listener);
    }

    /** Saca a ese oyente; un `null` se ignora. */
    public synchronized void removeFlavorListener(FlavorListener listener) {
        if (listener == null) {
            return;
        }
        this.flavorListeners.remove(listener);
    }

    /** Los oyentes registrados. */
    public synchronized FlavorListener[] getFlavorListeners() {
        return this.flavorListeners.toArray(new FlavorListener[this.flavorListeners.size()]);
    }

    /** Le avisa a todos los oyentes que el contenido cambió. */
    private void avisarCambio() {
        if (this.flavorListeners.isEmpty()) {
            return;
        }
        FlavorEvent e = new FlavorEvent(this);
        FlavorListener[] copia = this.getFlavorListeners();
        for (int i = 0; i < copia.length; i++) {
            copia[i].flavorsChanged(e);
        }
    }
}
