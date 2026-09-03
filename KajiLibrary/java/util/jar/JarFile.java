package java.util.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Un JAR abierto para acceso aleatorio: un {@link ZipFile} que ademas entiende el manifiesto y las
 * entradas por version.
 *
 * <h2>Lo que queda afuera, y por que</h2>
 *
 * <p><b>No se verifica ninguna firma.</b> El parametro `verify` de los constructores se acepta y se
 * ignora, y {@link JarEntry#getCertificates()} y {@link JarEntry#getCodeSigners()} devuelven siempre
 * `null`. Verificar de verdad pide PKCS#7, cadenas de certificados y validacion de rutas, y nada de
 * eso existe en esta biblioteca; escribir una verificacion a medias seria peor que no tenerla, porque
 * el que la usa cree que algo se comprobo.
 *
 * <p>Lo que importa de esa ausencia es <b>hacia que lado falla</b>, y falla al lado seguro: como los
 * firmantes son siempre `null`, un JAR firmado se ve exactamente igual que uno sin firmar, y el
 * codigo que decide confianza mirando la firma **rechaza**. Lo que no pasa, y hay que saberlo, es que
 * un JAR firmado y despues adulterado tire `SecurityException` al leerlo: aca se lee y ya.
 *
 * <p>Multi-release si esta hecho: con el constructor de cuatro argumentos, `getEntry`/`getJarEntry`
 * buscan `META-INF/versions/N/<nombre>` desde la version pedida hacia abajo antes de caer al nombre
 * base, y {@link #versionedStream()} enumera la vista resuelta.
 *
 * <h2>Dos desviaciones de firma, y el motivo</h2>
 *
 * <p><b>Los constructores no declaran `throws IOException`.</b> Los de `ZipFile` de esta biblioteca
 * tampoco: envuelven el error en `UncheckedIOException`, y como estos delegan en aquellos, declarar
 * la excepcion chequeada prometeria una senializacion que no ocurre. Los **metodos** si la declaran,
 * igual que los de `ZipFile`.
 *
 * <p><b>`entries()` devuelve `Enumeration&lt;ZipEntry&gt;` y `stream()` un `Stream&lt;ZipEntry&gt;`</b>,
 * donde el JDK dice `JarEntry`. No es una eleccion: en el JDK `ZipFile` declara
 * `Enumeration<? extends ZipEntry>`, y el comodin es justamente lo que deja que la subclase estreche;
 * el `ZipFile` de esta biblioteca declara `Enumeration<ZipEntry>` sin comodin, asi que estrechar no
 * compila. Los objetos que salen **si** son `JarEntry`, y castearlos funciona. Arreglarlo de verdad
 * es tocar `java.util.zip.ZipFile`, que es de otra sesion.
 */
public class JarFile extends ZipFile {

    /** `META-INF/MANIFEST.MF`, el unico nombre en el que un JAR busca su manifiesto. */
    public static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

    private static final String META_INF = "META-INF/";
    private static final String VERSIONES = "META-INF/versions/";

    // La version "base" es 8 y no 1.8 ni 0: es la que un JAR sin `META-INF/versions/` representa, y
    // el JDK la imprime como "8".
    private static final int RASGO_BASE = 8;
    private static final Runtime.Version VERSION_BASE = Runtime.Version.parse("8");

    private final Runtime.Version version;
    private final int rasgo;

    private Manifest man;
    private boolean manLeido;
    // 0 = sin averiguar, 1 = si, 2 = no. Averiguarlo pide leer el manifiesto, y eso no se puede
    // hacer en el constructor sin cambiarle la senializacion de errores.
    private int multi;

    /** La version que un JAR sin entradas versionadas representa. Es `8`. */
    public static Runtime.Version baseVersion() {
        return VERSION_BASE;
    }

    /** La version de esta VM, que es la que usan los constructores que no la piden. */
    public static Runtime.Version runtimeVersion() {
        return Runtime.version();
    }

    public JarFile(String name) {
        this(new File(name), true, ZipFile.OPEN_READ, VERSION_BASE);
    }

    public JarFile(String name, boolean verify) {
        this(new File(name), verify, ZipFile.OPEN_READ, VERSION_BASE);
    }

    public JarFile(File file) {
        this(file, true, ZipFile.OPEN_READ, VERSION_BASE);
    }

    public JarFile(File file, boolean verify) {
        this(file, verify, ZipFile.OPEN_READ, VERSION_BASE);
    }

    public JarFile(File file, boolean verify, int mode) {
        this(file, verify, mode, VERSION_BASE);
    }

    /**
     * Abre el JAR resolviendo las entradas versionadas hasta `version`.
     *
     * @throws NullPointerException si `version` es `null`
     */
    public JarFile(File file, boolean verify, int mode, Runtime.Version version) {
        super(file, mode);
        if (version == null) {
            throw new NullPointerException("version");
        }
        // Por debajo de 9 no hay entradas versionadas que valgan, asi que se normaliza a la base:
        // es lo que hace el JDK, y evita que `getVersion()` mienta con un "1.8".
        if (version.feature() < RASGO_BASE) {
            this.version = VERSION_BASE;
        } else {
            this.version = version;
        }
        this.rasgo = this.version.feature();
        this.multi = 0;
    }

    /**
     * La version a la que este JAR resuelve, o la base si no es multi-release.
     *
     * <p>Un JAR normal abierto pidiendo la version 21 sigue contestando `8`: la version solo importa
     * si el manifiesto declara `Multi-Release: true`.
     */
    public final Runtime.Version getVersion() {
        return isMultiRelease() ? this.version : VERSION_BASE;
    }

    /** Si el manifiesto declara `Multi-Release: true`. */
    public final boolean isMultiRelease() {
        if (this.multi == 0) {
            this.multi = 2;
            try {
                Manifest m = getManifest();
                if (m != null) {
                    String v = m.getMainAttributes().getValue(Attributes.Name.MULTI_RELEASE);
                    if (v != null && v.trim().equalsIgnoreCase("true")) {
                        this.multi = 1;
                    }
                }
            } catch (IOException e) {
                // Un manifiesto ilegible no es multi-release. No se propaga porque el JDK tampoco
                // declara excepcion aca.
                this.multi = 2;
            }
        }
        return this.multi == 1;
    }

    /**
     * El manifiesto del JAR, o `null` si no tiene.
     *
     * <p>Se busca primero por el nombre exacto y despues sin distinguir mayusculas, que es lo que
     * hace el JDK: hay archivos en circulacion con `META-INF/manifest.mf`.
     */
    public Manifest getManifest() throws IOException {
        if (!this.manLeido) {
            this.manLeido = true;
            ZipEntry e = entradaDelManifiesto();
            if (e != null) {
                InputStream in = super.getInputStream(e);
                if (in != null) {
                    this.man = new Manifest(in);
                    in.close();
                }
            }
        }
        return this.man;
    }

    private ZipEntry entradaDelManifiesto() {
        ZipEntry e = super.getEntry(MANIFEST_NAME);
        if (e != null) {
            return e;
        }
        Enumeration<ZipEntry> todas = super.entries();
        while (todas.hasMoreElements()) {
            ZipEntry z = todas.nextElement();
            if (MANIFEST_NAME.equalsIgnoreCase(z.getName())) {
                return z;
            }
        }
        return null;
    }

    /** La entrada de ese nombre, resolviendo por version si corresponde. */
    public JarEntry getJarEntry(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (this.rasgo > RASGO_BASE && !name.startsWith(META_INF) && isMultiRelease()) {
            int v = this.rasgo;
            while (v > RASGO_BASE) {
                ZipEntry ze = super.getEntry(VERSIONES + v + "/" + name);
                if (ze != null) {
                    return new EntradaDeJar(this, ze, name);
                }
                v = v - 1;
            }
        }
        ZipEntry ze = super.getEntry(name);
        if (ze == null) {
            return null;
        }
        return new EntradaDeJar(this, ze, name);
    }

    /** Lo mismo que {@link #getJarEntry}: en un JAR toda entrada es una entrada de JAR. */
    public ZipEntry getEntry(String name) {
        return getJarEntry(name);
    }

    /**
     * Todas las entradas del archivo, **sin** resolver por version.
     *
     * <p>Los objetos son `JarEntry`; el tipo estatico es `ZipEntry` por lo que dice la cabecera de
     * la clase. Para la vista resuelta esta {@link #versionedStream()}.
     */
    public Enumeration<ZipEntry> entries() {
        return java.util.Collections.enumeration(listaCruda());
    }

    /** Las mismas que {@link #entries()}, como flujo. */
    public java.util.stream.Stream<ZipEntry> stream() {
        return listaCruda().stream();
    }

    /**
     * La vista **resuelta** de un JAR multi-release: un elemento por nombre base, con los bytes de la
     * version mas alta que no pase de la pedida.
     *
     * <p>En un JAR que no es multi-release es lo mismo que {@link #stream()}.
     */
    public java.util.stream.Stream<ZipEntry> versionedStream() {
        if (!isMultiRelease()) {
            return stream();
        }
        List<String> nombres = new ArrayList<String>();
        Enumeration<ZipEntry> todas = super.entries();
        while (todas.hasMoreElements()) {
            String base = nombreBase(todas.nextElement().getName());
            if (base != null && !nombres.contains(base)) {
                nombres.add(base);
            }
        }
        List<ZipEntry> out = new ArrayList<ZipEntry>();
        for (String n : nombres) {
            JarEntry je = getJarEntry(n);
            if (je != null) {
                out.add(je);
            }
        }
        return out.stream();
    }

    /**
     * El nombre base de una entrada, o `null` si es una entrada versionada que no cuenta: el
     * directorio `META-INF/versions/` en si, uno de version, o una version mas alta que la pedida.
     */
    private String nombreBase(String name) {
        if (!name.startsWith(VERSIONES)) {
            return name;
        }
        int desde = VERSIONES.length();
        int barra = name.indexOf('/', desde);
        if (barra < 0 || barra == name.length() - 1) {
            return null;
        }
        int v;
        try {
            v = Integer.parseInt(name.substring(desde, barra));
        } catch (NumberFormatException e) {
            // Una entrada con una "version" que no es un numero se ignora en silencio, igual que en
            // el JDK: es un archivo mal armado, no un error del que lo lee.
            return null;
        }
        if (v > this.rasgo) {
            return null;
        }
        return name.substring(barra + 1);
    }

    /**
     * El contenido de esa entrada.
     *
     * <p>Si la entrada vino resuelta por version, lo que se abre es el nombre **real**: el de
     * `META-INF/versions/`, no el que la entrada dice llamarse.
     */
    public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
        if (ze == null) {
            throw new NullPointerException("ze");
        }
        if (ze instanceof EntradaDeJar) {
            String real = ((EntradaDeJar) ze).real();
            if (!real.equals(ze.getName())) {
                ZipEntry z = super.getEntry(real);
                if (z == null) {
                    return null;
                }
                return super.getInputStream(z);
            }
        }
        return super.getInputStream(ze);
    }

    private List<ZipEntry> listaCruda() {
        List<ZipEntry> out = new ArrayList<ZipEntry>();
        Enumeration<ZipEntry> todas = super.entries();
        while (todas.hasMoreElements()) {
            ZipEntry z = todas.nextElement();
            out.add(new EntradaDeJar(this, z, z.getName()));
        }
        return out;
    }
}
