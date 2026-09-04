package java.net;

import java.io.IOException;
import java.security.cert.Certificate;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * La conexion a una URL {@code jar:}, que nombra un archivo dentro de un `.jar`.
 *
 * <p>La forma de la URL es {@code jar:<url del jar>!/<entrada>}, y el separador {@code !/} es lo que
 * la parte: lo de antes es una URL cualquiera --normalmente {@code file:} o {@code http:}-- y lo de
 * despues, la ruta adentro del archivo. La entrada puede faltar, y entonces la URL nombra al `.jar`
 * entero: {@code jar:file:/x/a.jar!/} es valida y su {@link #getEntryName} es `null`.
 *
 * <h2>Por que esta clase es abstracta y aun asi hace casi todo</h2>
 *
 * <p>Lo unico que no sabe hacer es **conseguir el archivo**: eso depende del protocolo de adentro --
 * un {@code file:} se abre, un {@code http:} se descarga y se cachea-- y por eso {@link #getJarFile}
 * queda para la subclase. Todo lo demas se puede escribir una vez y aca esta: partir la URL,
 * encontrar la entrada, leer el manifiesto, sacar los atributos y los certificados. Es el reparto
 * que hace el JDK y es el correcto -- ocho de los diez miembros no dependen del transporte.
 *
 * <h2>Los atributos: dos metodos que se confunden</h2>
 *
 * <p>{@link #getAttributes} son los de **la entrada** y {@link #getMainAttributes} los de **la
 * seccion principal del manifiesto**, que valen para todo el archivo. Un `.jar` firmado guarda el
 * resumen de cada archivo en su propia seccion, y ahi la diferencia deja de ser academica.
 */
public abstract class JarURLConnection extends URLConnection {

    /**
     * La conexion a la URL del `.jar` en si.
     *
     * <p>Es `null` hasta que una subclase la abra. Esta declarada acá y `protected` porque el JDK lo
     * hace: es el punto por donde una subclase le pasa opciones --tiempos de espera, cabeceras-- a
     * la conexion de adentro.
     */
    protected URLConnection jarFileURLConnection;

    private final URL jarFileURL;
    private final String entryName;

    /**
     * Parte la URL en el `.jar` y la entrada.
     *
     * @throws MalformedURLException si no tiene la forma {@code jar:<url>!/<entrada>}
     */
    protected JarURLConnection(URL url) throws MalformedURLException {
        super(url);
        String spec = url.getFile();
        int sep = spec.indexOf("!/");
        if (sep == -1) {
            // El `!/` no es decorativo: sin el no se sabe donde termina la URL de adentro, y una URL
            // `jar:` sin separador no nombra nada. El JDK tira exactamente acá.
            throw new MalformedURLException("no !/ in spec");
        }
        this.jarFileURL = new URL(spec.substring(0, sep));
        String resto = spec.substring(sep + 2);
        // Una entrada vacia significa "el archivo entero", y eso es `null` y no `""`: el contrato
        // distingue las dos cosas, y un `""` se leeria como una entrada con nombre vacio.
        this.entryName = resto.isEmpty() ? null : JarURLConnection.decodificar(resto);
    }

    // El nombre de la entrada viene percent-encoded, como cualquier parte de una URL. Sin decodificar,
    // un archivo con un espacio en el nombre se busca como `a%20b.txt` y no se encuentra nunca.
    private static String decodificar(String s) {
        if (s.indexOf('%') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                int hi = Character.digit(s.charAt(i + 1), 16);
                int lo = Character.digit(s.charAt(i + 2), 16);
                if (hi >= 0 && lo >= 0) {
                    sb.append((char) ((hi << 4) | lo));
                    i = i + 3;
                    continue;
                }
            }
            sb.append(c);
            i = i + 1;
        }
        return sb.toString();
    }

    /** La URL del `.jar`, sin la parte de la entrada. */
    public URL getJarFileURL() {
        return this.jarFileURL;
    }

    /** El nombre de la entrada, o `null` si la URL nombra al archivo entero. */
    public String getEntryName() {
        return this.entryName;
    }

    /**
     * El `.jar` abierto.
     *
     * <p>Lo unico que esta clase no puede hacer sola: depende del protocolo de adentro. Ver la nota
     * de la clase.
     *
     * @throws IOException si no se pudo abrir
     */
    public abstract JarFile getJarFile() throws IOException;

    /**
     * El manifiesto del `.jar`, o `null` si no tiene.
     *
     * <p>Un `.jar` sin manifiesto es perfectamente valido --es un zip-- asi que `null` no es un
     * error sino la respuesta.
     *
     * @throws IOException si no se pudo abrir el archivo
     */
    public Manifest getManifest() throws IOException {
        return this.getJarFile().getManifest();
    }

    /**
     * La entrada que la URL nombra, o `null` si nombra al archivo entero **o si no existe**.
     *
     * <p>Los dos casos dan `null`, y eso se lee mal pero es el contrato: esta clase no comprueba que
     * la entrada exista. Quien lo hace es el manejador de protocolo concreto, al conectar, y por eso
     * un `jar:` de verdad falla con `FileNotFoundException` mucho antes de llegar acá.
     *
     * <p>Escribí este método tirando para una entrada ausente --parecía mejor que devolver `null`--
     * y el JDK me corrigió: la prueba de comportamiento no coincidió corriendo contra `java` de
     * verdad. Distinguir los dos `null` es trabajo de la subclase, no de esta.
     *
     * @throws IOException si no se pudo abrir el archivo
     */
    public JarEntry getJarEntry() throws IOException {
        if (this.entryName == null) {
            return null;
        }
        return this.getJarFile().getJarEntry(this.entryName);
    }

    /**
     * Los atributos **de la entrada**, o `null` si la URL nombra al archivo entero.
     *
     * <p>Ver la nota de la clase sobre la diferencia con {@link #getMainAttributes}.
     *
     * @throws IOException si no se pudo abrir el archivo, o si la entrada no existe
     */
    public Attributes getAttributes() throws IOException {
        JarEntry e = this.getJarEntry();
        return e == null ? null : e.getAttributes();
    }

    /**
     * Los atributos de la seccion principal del manifiesto, o `null` si no hay manifiesto.
     *
     * @throws IOException si no se pudo abrir el archivo
     */
    public Attributes getMainAttributes() throws IOException {
        Manifest m = this.getManifest();
        return m == null ? null : m.getMainAttributes();
    }

    /**
     * Los certificados con que se firmo la entrada, o `null`.
     *
     * <p><strong>Solo valen despues de leer la entrada entera</strong>, y eso no es un detalle de
     * esta implementacion sino como funciona la firma de un `.jar`: el resumen se comprueba mientras
     * se leen los bytes, asi que preguntar antes devuelve `null` aunque el archivo este firmado. El
     * JDK dice lo mismo.
     *
     * @throws IOException si no se pudo abrir el archivo, o si la entrada no existe
     */
    public Certificate[] getCertificates() throws IOException {
        JarEntry e = this.getJarEntry();
        return e == null ? null : e.getCertificates();
    }
}
