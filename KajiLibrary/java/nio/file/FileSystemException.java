package java.nio.file;

import java.io.IOException;

// La base de las excepciones de este paquete: una falla sobre uno o dos archivos.
//
// **Por que dos archivos y no uno.** `copy` y `move` fallan sobre un par, y saber cual de los dos
// era el problema es la mitad del diagnostico. `getFile()` es el origen y `getOtherFile()` el
// destino; en las operaciones de un solo archivo el segundo queda en `null`.
//
// **Por que se guarda la razon aparte del mensaje.** `getMessage()` arma el texto juntando las tres
// partes, pero quien atrapa la excepcion suele querer la ruta cruda --para reintentar, o para
// mostrarla en otro idioma-- y sacarla del mensaje formateado seria fragil.
public class FileSystemException extends IOException {

    private static final long serialVersionUID = -3055425747967319812L;

    private final String file;
    private final String other;
    private final String reason;

    /**
     * Una falla sobre un solo archivo, sin explicacion.
     *
     * @param file el archivo, o `null` si no se sabe
     */
    public FileSystemException(String file) {
        super((String) null);
        this.file = file;
        this.other = null;
        this.reason = null;
    }

    /**
     * Una falla con todo el detalle.
     *
     * @param file el archivo, o `null`
     * @param other el otro archivo, o `null`
     * @param reason por que fallo, o `null`
     */
    public FileSystemException(String file, String other, String reason) {
        super((String) null);
        this.file = file;
        this.other = other;
        this.reason = reason;
    }

    /** El archivo, o `null`. */
    public String getFile() {
        return this.file;
    }

    /** El otro archivo, o `null`. */
    public String getOtherFile() {
        return this.other;
    }

    /** La razon, o `null`. */
    public String getReason() {
        return this.reason;
    }

    /**
     * El mensaje armado: `archivo -> otro: razon`, salteando las partes que falten.
     *
     * <p>Se calcula aca y no en el constructor porque las tres partes son finales: el resultado es
     * siempre el mismo y no hay estado que guardar.
     */
    public String getMessage() {
        if (this.file == null && this.other == null) {
            return this.reason;
        }
        StringBuilder sb = new StringBuilder();
        if (this.file != null) {
            sb.append(this.file);
        }
        if (this.other != null) {
            sb.append(" -> ");
            sb.append(this.other);
        }
        if (this.reason != null) {
            sb.append(": ");
            sb.append(this.reason);
        }
        return sb.toString();
    }
}
