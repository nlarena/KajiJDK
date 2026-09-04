package java.awt.datatransfer;

import java.io.IOException;

/**
 * Algo que se puede transferir: al portapapeles, o arrastrando.
 *
 * <p>La idea central es que un mismo dato se ofrece en **varios formatos** a la vez, y quien lo
 * recibe elige el que entiende. Copiar una selección de una planilla ofrece a la vez el texto plano,
 * el HTML con formato y el objeto nativo: pegarla en un editor de texto trae lo primero y pegarla en
 * la misma planilla trae lo último, sin que nadie tenga que convertir nada de más.
 *
 * <p>Por eso los datos se piden **por formato** y no de una vez: convertir cuesta, y sólo se paga la
 * conversión del formato que efectivamente se pidió.
 */
public interface Transferable {

    /** En qué formatos se puede entregar, del mejor al peor. */
    DataFlavor[] getTransferDataFlavors();

    /** Si se puede entregar en ese formato. */
    boolean isDataFlavorSupported(DataFlavor flavor);

    /**
     * Los datos en ese formato.
     *
     * @throws UnsupportedFlavorException si el formato no se admite
     * @throws IOException si los datos ya no están disponibles
     */
    Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException;
}
