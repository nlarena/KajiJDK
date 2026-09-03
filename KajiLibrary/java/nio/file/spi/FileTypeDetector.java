package java.nio.file.spi;

import java.io.IOException;
import java.nio.file.Path;

// El punto de extension para adivinar el tipo MIME de un archivo.
//
// **KajiJDK no instala ninguno, y por eso `Files.probeContentType` no existe.** Un detector serio
// mira la extension contra una tabla y/o los primeros bytes contra firmas conocidas; escribir la
// tabla es facil, pero la spec dice que el resultado sale de los detectores **instalados**, y
// KajiJDK no tiene el mecanismo de servicios que los instala. Un `probeContentType` que devolviera
// `"text/plain"` para todo `.txt` y `null` para el resto contestaria por un detector que nadie
// registro. La clase abstracta esta para que quien quiera escribir uno tenga de que heredar.
public abstract class FileTypeDetector {

    /** Para las subclases. */
    protected FileTypeDetector() {
    }

    /**
     * El tipo MIME de `path`, o `null` si este detector no lo reconoce.
     *
     * <p>`null` es una respuesta valida y esperada: significa "yo no se", y deja que el siguiente
     * detector de la cadena pruebe.
     */
    public abstract String probeContentType(Path path) throws IOException;
}
