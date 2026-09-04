package java.awt.datatransfer;

import java.io.IOException;
import java.io.StringReader;

/**
 * Una cadena lista para copiar al portapapeles.
 *
 * <p>Es la implementación de {@link Transferable} que resuelve el noventa por ciento de los casos, y
 * está escrita para que copiar texto sea una línea.
 *
 * <p>Implementa además {@link ClipboardOwner} sin hacer nada al perder el portapapeles: una cadena
 * ya está en memoria y no hay recurso que soltar. Es el aviso que se puede ignorar con la conciencia
 * tranquila.
 */
public class StringSelection implements Transferable, ClipboardOwner {

    private static final int STRING = 0;
    private static final int PLAIN_TEXT = 1;

    private static final DataFlavor[] flavors = {
        DataFlavor.stringFlavor,
        DataFlavor.plainTextFlavor
    };

    private final String data;

    /** Con la cadena a transferir. */
    public StringSelection(String data) {
        this.data = data;
    }

    /** Texto de Java primero, texto plano después. */
    public DataFlavor[] getTransferDataFlavors() {
        return flavors.clone();
    }

    /** Si es uno de los dos formatos de texto. */
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (int i = 0; i < flavors.length; i++) {
            if (flavor.equals(flavors[i])) {
                return true;
            }
        }
        return false;
    }

    /**
     * La cadena, o un lector sobre ella.
     *
     * <p>El formato de texto plano entrega un {@code Reader} y no la cadena: es lo que declara su
     * clase de representación, y devolver la cadena rompería a quien confíe en ella.
     *
     * @throws UnsupportedFlavorException si el formato no es de texto
     * @throws IOException nunca en la práctica: la cadena ya está en memoria
     */
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        if (flavor.equals(flavors[STRING])) {
            return this.data;
        }
        if (flavor.equals(flavors[PLAIN_TEXT])) {
            return new StringReader(this.data == null ? "" : this.data);
        }
        throw new UnsupportedFlavorException(flavor);
    }

    /** No hace nada: una cadena en memoria no tiene recursos que soltar. */
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
