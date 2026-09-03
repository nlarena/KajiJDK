package java.util.jar;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Un JAR leido de corrido: un {@link ZipInputStream} que se come el manifiesto antes de entregar la
 * primera entrada.
 *
 * <p>Esa es toda la diferencia, y explica la unica rareza del constructor: para saber si hay
 * manifiesto hay que leer la primera entrada, y si la hay hay que consumirla entera. Cuando el
 * constructor termina, el flujo ya avanzo hasta la entrada siguiente y la tiene guardada; la
 * devuelve el primer `getNextEntry()`.
 *
 * <p>Consecuencia de eso, y esta copiada del JDK a proposito: <b>entre el constructor y el primer
 * `getNextEntry()`, `read` devuelve -1</b>. Los bytes que hay ahi debajo son los de una entrada que
 * el que llama todavia no pidio, y entregarselos seria darle contenido sin decirle de que entrada es.
 *
 * <h2>Lo que queda afuera, y por que</h2>
 *
 * <p>Nada de la superficie publica. Como en {@link JarFile}, el parametro `verify` se acepta y se
 * ignora --no hay verificacion de firmas en esta biblioteca-- y las entradas salen sin certificados
 * ni firmantes; el razonamiento completo, incluido por que el modo de falla es cerrado, esta en la
 * cabecera de `JarFile`.
 */
public class JarInputStream extends ZipInputStream {

    private Manifest man;
    private JarEntry primera;

    public JarInputStream(InputStream in) throws IOException {
        this(in, true);
    }

    public JarInputStream(InputStream in, boolean verify) throws IOException {
        super(in);
        JarEntry e = (JarEntry) super.getNextEntry();
        // Un JAR escrito por `jar` empieza con la entrada de directorio `META-INF/`, que no es el
        // manifiesto pero esta antes que el.
        if (e != null && "META-INF/".equalsIgnoreCase(e.getName())) {
            e = (JarEntry) super.getNextEntry();
        }
        if (e != null && JarFile.MANIFEST_NAME.equalsIgnoreCase(e.getName())) {
            this.man = new Manifest(this);
            super.closeEntry();
            this.primera = (JarEntry) super.getNextEntry();
        } else {
            this.primera = e;
        }
    }

    /** El manifiesto del JAR, o `null` si la primera entrada no era uno. */
    public Manifest getManifest() {
        return this.man;
    }

    /** Avanza a la entrada siguiente, o `null` al final. */
    public ZipEntry getNextEntry() throws IOException {
        JarEntry e;
        if (this.primera == null) {
            e = (JarEntry) super.getNextEntry();
        } else {
            e = this.primera;
            this.primera = null;
        }
        return e;
    }

    /** Lo mismo que {@link #getNextEntry()}, ya con el tipo de este paquete. */
    public JarEntry getNextJarEntry() throws IOException {
        return (JarEntry) getNextEntry();
    }

    /** Lee de la entrada en curso. Devuelve -1 mientras la primera entrada siga sin pedirse. */
    public int read(byte[] b, int off, int len) throws IOException {
        if (this.primera != null) {
            return -1;
        }
        return super.read(b, off, len);
    }

    /**
     * La entrada que fabrica el lector de ZIP de abajo, ya con los atributos que el manifiesto le
     * asigna --si es que a esta altura ya se leyo el manifiesto--.
     */
    protected ZipEntry createZipEntry(String name) {
        JarEntry e = new JarEntry(name);
        if (this.man != null) {
            e.attr = this.man.getAttributes(name);
        }
        return e;
    }
}
