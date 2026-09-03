package java.util.jar;

import java.io.IOException;
import java.security.CodeSigner;
import java.security.cert.Certificate;
import java.util.zip.ZipEntry;

/**
 * Una entrada de un JAR: una entrada de ZIP, mas lo que el manifiesto y la firma dicen de ella.
 *
 * <h2>Lo que queda afuera, y por que</h2>
 *
 * <p>Toda la superficie esta, pero dos metodos devuelven siempre `null` y hay que decirlo aca y no
 * en una nota al pie: <b>{@link #getCertificates()} y {@link #getCodeSigners()} nunca devuelven nada</b>,
 * porque este paquete no verifica firmas. El motivo esta entero en la cabecera de {@link JarFile};
 * lo que importa desde aca es que la consecuencia es **cerrada**, no abierta: un JAR firmado se ve
 * como un JAR sin firmar, asi que quien decide confianza mirando la firma rechaza en vez de aceptar.
 *
 * <p>{@link #getRealName()} devuelve el mismo nombre que `getName()` salvo para las entradas que
 * `JarFile` resuelve por version --las de `META-INF/versions/`--, que son las unicas en las que los
 * dos nombres difieren.
 */
public class JarEntry extends ZipEntry {

    Attributes attr;
    Certificate[] certs;
    CodeSigner[] signers;

    /** Una entrada nueva con ese nombre. */
    public JarEntry(String name) {
        super(name);
    }

    /** Una entrada de JAR con los datos de esa entrada de ZIP. */
    public JarEntry(ZipEntry ze) {
        super(ze);
    }

    /** Una copia. */
    public JarEntry(JarEntry je) {
        super(je);
        this.attr = je.attr;
        this.certs = copiar(je.certs);
        this.signers = copiarFirmantes(je.signers);
    }

    /**
     * Los atributos que el manifiesto le asigna a esta entrada, o `null` si no tiene seccion propia.
     *
     * <p>Una entrada suelta --construida con `new JarEntry(nombre)`-- no conoce ningun manifiesto y
     * devuelve `null`. Las que salen de un {@link JarFile} o de un {@link JarInputStream} si.
     */
    public Attributes getAttributes() throws IOException {
        return this.attr;
    }

    /** Siempre `null`: no se verifican firmas. Ver la cabecera de la clase. */
    public Certificate[] getCertificates() {
        return copiar(this.certs);
    }

    /** Siempre `null`: no se verifican firmas. Ver la cabecera de la clase. */
    public CodeSigner[] getCodeSigners() {
        return copiarFirmantes(this.signers);
    }

    /** El nombre real de la entrada dentro del archivo. */
    public String getRealName() {
        return getName();
    }

    private static Certificate[] copiar(Certificate[] a) {
        if (a == null) {
            return null;
        }
        Certificate[] c = new Certificate[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }

    private static CodeSigner[] copiarFirmantes(CodeSigner[] a) {
        if (a == null) {
            return null;
        }
        CodeSigner[] c = new CodeSigner[a.length];
        System.arraycopy(a, 0, c, 0, a.length);
        return c;
    }
}
