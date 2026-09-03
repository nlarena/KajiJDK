package java.util.jar;

import java.io.IOException;
import java.util.zip.ZipEntry;

/**
 * La entrada que devuelve un {@link JarFile}: sabe de que archivo salio, y por eso puede contestar
 * `getAttributes()` mirando el manifiesto en vez de que se lo tengan que cargar de afuera.
 *
 * <p>Es tambien la unica entrada en la que `getName()` y `getRealName()` difieren. Eso pasa con los
 * JAR **multi-release**: el que pide `foo/Bar.class` en un JAR abierto a la version 21 puede recibir
 * los bytes de `META-INF/versions/21/foo/Bar.class`. El nombre que se pidio es el que la entrada
 * dice llamarse --si no, ningun `equals` de nombres cerraria--, y el de adentro del archivo queda en
 * `getRealName()`.
 *
 * <p>En el JDK esto es `JarFile.JarFileEntry`, una clase interna. Aca es de paquete y de primer
 * nivel, con la referencia al `JarFile` explicita: es interno, o sea libre por la regla del contrato,
 * y evita depender de las clases internas no estaticas.
 */
final class EntradaDeJar extends JarEntry {

    private final JarFile duenio;
    private final String real;

    EntradaDeJar(JarFile duenio, ZipEntry ze, String nombre) {
        super(nombre);
        this.duenio = duenio;
        this.real = ze.getName();
        // La entrada se llama como el que la pidio pero **mide** lo que mide adentro del archivo.
        setMethod(ze.getMethod());
        setSize(ze.getSize());
        setCompressedSize(ze.getCompressedSize());
        setTime(ze.getTime());
        setCrc(ze.getCrc());
        setExtra(ze.getExtra());
        setComment(ze.getComment());
    }

    String real() {
        return this.real;
    }

    public String getRealName() {
        return this.real;
    }

    /** La seccion del manifiesto para esta entrada, buscada por su nombre **real**. */
    public Attributes getAttributes() throws IOException {
        Manifest man = this.duenio.getManifest();
        if (man == null) {
            return null;
        }
        return man.getAttributes(this.real);
    }
}
