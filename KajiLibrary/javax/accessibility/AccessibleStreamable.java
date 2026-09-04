package javax.accessibility;

import java.awt.datatransfer.DataFlavor;
import java.io.InputStream;

/**
 * Lo implementa lo que además de verse se puede **leer como un flujo de bytes**.
 *
 * <p>Sirve para que una ayuda técnica se lleve el contenido en su formato nativo —una imagen, un
 * documento— en vez de tener que reconstruirlo a partir de la descripción accesible.
 */
public interface AccessibleStreamable {

    /** En qué formatos se puede entregar el contenido. */
    DataFlavor[] getMimeTypes();

    /**
     * El contenido en ese formato.
     *
     * @return el flujo, o `null` si el formato no se admite
     */
    InputStream getStream(DataFlavor flavor);
}
