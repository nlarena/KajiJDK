package java.util.jar;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Un JAR escrito de corrido: un {@link ZipOutputStream} que arranca poniendo el manifiesto.
 *
 * <p>El orden no es un detalle. El manifiesto tiene que ser la primera entrada del archivo para que
 * un lector secuencial --{@link JarInputStream}, que solo puede ir hacia adelante-- lo encuentre sin
 * haber leido todo lo demas. Por eso el constructor que lo recibe lo escribe ahi mismo.
 *
 * <h2>Lo que queda afuera, y por que</h2>
 *
 * <p>Nada de la superficie publica. Si hay una diferencia de bytes con el JDK que conviene anotar:
 * el JDK le mete a la primera entrada un campo `extra` de cuatro bytes con el numero magico `0xCAFE`.
 * Aca no, porque el `ZipOutputStream` de esta biblioteca no escribe campos `extra` en absoluto
 * --escribe un largo de 0 en la cabecera local--, asi que ponerlo seria escribirlo en un objeto que
 * despues nadie serializa. No hace falta para nada: ninguna herramienta lo exige, y los JAR que
 * escribe este flujo los lee el `java` real.
 */
public class JarOutputStream extends ZipOutputStream {

    public JarOutputStream(OutputStream out) throws IOException {
        super(out);
    }

    /**
     * Escribe el manifiesto como primera entrada y deja el flujo listo para el resto.
     *
     * @throws NullPointerException si `man` es `null`
     */
    public JarOutputStream(OutputStream out, Manifest man) throws IOException {
        super(out);
        if (man == null) {
            throw new NullPointerException("man");
        }
        putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));
        man.write(this);
        closeEntry();
    }

    /** Empieza una entrada nueva. */
    public void putNextEntry(ZipEntry ze) throws IOException {
        super.putNextEntry(ze);
    }
}
