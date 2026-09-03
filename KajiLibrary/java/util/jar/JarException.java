package java.util.jar;

import java.util.zip.ZipException;

/**
 * Un JAR mal formado.
 *
 * <p>Extiende `ZipException` --y no `IOException` a secas-- porque un JAR **es** un ZIP: lo que rompe
 * al ZIP rompe al JAR, y quien atrapa `ZipException` alrededor de un archivo comprimido tiene que
 * seguir atrapandolo cuando ese archivo resulta ser un JAR.
 */
public class JarException extends ZipException {

    public JarException() {
        super();
    }

    public JarException(String s) {
        super(s);
    }
}
