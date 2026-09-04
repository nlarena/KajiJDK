package java.net;

// De un nombre de archivo al tipo MIME que le corresponde.
//
// La interfaz no dice de donde sale el mapeo -- puede ser una tabla de extensiones, `mime.types`
// del sistema, o mirar el contenido. Eso es a proposito: el que consulta solo quiere el tipo.
public interface FileNameMap {

    /** El tipo MIME de {@code fileName}, o null si no se pudo determinar. */
    String getContentTypeFor(String fileName);
}
