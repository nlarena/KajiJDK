package java.util.zip;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

// Acceso **aleatorio** a un archivo comprimido: listar las entradas sin leerlas, y despues abrir
// solo la que se quiere. Esa es toda la diferencia con `ZipInputStream`, y descansa entera en poder
// **saltar**: se va al final del archivo, se lee el directorio central, y desde ahi se conoce el
// desplazamiento de cada entrada.
//
// **Antes esto no se podia y el constructor tiraba.** La nota que estaba aca decia "cuando lleguen
// los nativos de archivo, esto se vuelve un salto mas la misma decodificacion de campos". Llegaron
// --`jdk.internal.io.Fs`-- y eso es exactamente lo que hace ahora.
//
// La diferencia con el JDK, y es la de siempre en esta biblioteca: **el archivo se lee entero de una
// vez**, no hay descriptor abierto ni posicion. Un ZIP de un giga entra en memoria; a cambio no hay
// ningun estado que se pueda quedar colgado, y `close()` no puede perder nada porque no hay nada
// pendiente. Cuando haga falta streaming de verdad, la puerta es agregar un handle en `Fs`: los
// metodos de aca hablan con un `byte[]`, no con el nativo, asi que no se enteran.
//
// El formato, en la parte que importa: al final del archivo hay un registro **EOCD** que dice
// cuantas entradas hay y donde arranca el directorio central; el directorio es una lista de
// cabeceras, una por entrada, cada una con el desplazamiento de su cabecera **local**; y la cabecera
// local dice cuanto miden el nombre y el campo extra, que es lo unico que hace falta para saber
// donde empiezan los datos.
//
// Se busca el EOCD **desde el final hacia atras** porque puede haber un comentario de archivo
// despues, de largo variable: no hay forma de saber donde arranca sin buscar su firma.
//
// **Como se senializa un error**: el JDK declara `throws IOException` en los constructores y en
// `getInputStream`. Aca se envuelve en `UncheckedIOException`, que es la convencion que la biblioteca
// ya fijo en `FileInputStream`/`FileOutputStream`: las bases de `java.io` de aca se escribieron sin
// `throws`, y un override no puede ensanchar las excepciones chequeadas (JLS 8.4.8.3). El motivo no
// se pierde -- la `ZipException` original va adentro.
public class ZipFile implements Closeable {

    public static final int OPEN_READ = 0x1;
    public static final int OPEN_DELETE = 0x4;

    private static final int EOCD_SIG = 0x06054b50;
    private static final int CEN_SIG = 0x02014b50;
    private static final int LOC_SIG = 0x04034b50;

    private final String name;
    private final Charset charset;
    private final byte[] datos;
    private final List<ZipEntry> entries;
    // El desplazamiento de la cabecera **local** de cada entrada, en paralelo a `entries`.
    private final List<Long> offsets;
    private boolean cerrado;

    /** Abre el archivo de ese nombre, con los nombres de entrada en UTF-8. */
    public ZipFile(String name) {
        this(name, StandardCharsets.UTF_8);
    }

    /** Abre el archivo de ese nombre, decodificando los nombres con `charset`. */
    public ZipFile(String name, Charset charset) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.name = name;
        this.charset = charset;
        byte[] leidos = jdk.internal.io.Fs.readAllBytes(name);
        if (leidos == null) {
            throw new java.io.UncheckedIOException(
                    new java.io.FileNotFoundException(name + " (no se pudo leer)"));
        }
        this.datos = leidos;
        this.entries = new ArrayList<ZipEntry>();
        this.offsets = new ArrayList<Long>();
        this.leerDirectorio();
    }

    /** Abre ese archivo. */
    public ZipFile(File file) {
        this(file.getPath(), StandardCharsets.UTF_8);
    }

    /** Abre ese archivo con ese charset. */
    public ZipFile(File file, Charset charset) {
        this(file.getPath(), charset);
    }

    /**
     * Abre ese archivo con esos modos.
     *
     * <p>`OPEN_DELETE` se acepta y **no se honra**: pide borrar el archivo al cerrarlo, y como aca
     * se lo lee entero al abrir, borrarlo despues no cambiaria nada de lo que ya se leyo -- pero si
     * borraria un archivo que el que llama quizas todavia quiere. Se prefiere no borrar y decirlo.
     *
     * @throws IllegalArgumentException si `mode` no es una combinacion de `OPEN_READ`/`OPEN_DELETE`
     */
    public ZipFile(File file, int mode) {
        this(file, mode, StandardCharsets.UTF_8);
    }

    /** El de arriba, con charset. */
    public ZipFile(File file, int mode, Charset charset) {
        this(comprobarModo(file, mode).getPath(), charset);
    }

    private static File comprobarModo(File file, int mode) {
        if ((mode & ~(OPEN_READ | OPEN_DELETE)) != 0 || (mode & OPEN_READ) == 0) {
            throw new IllegalArgumentException("Illegal mode: 0x" + Integer.toHexString(mode));
        }
        return file;
    }

    // ---- lectura del formato ----------------------------------------------------------------------

    private int u16(int at) {
        return (this.datos[at] & 0xff) | ((this.datos[at + 1] & 0xff) << 8);
    }

    private int u32(int at) {
        return (this.datos[at] & 0xff) | ((this.datos[at + 1] & 0xff) << 8)
                | ((this.datos[at + 2] & 0xff) << 16) | ((this.datos[at + 3] & 0xff) << 24);
    }

    private long u32sinSigno(int at) {
        return (long) this.u32(at) & 0xffffffffL;
    }

    private void leerDirectorio() {
        int eocd = this.buscarEocd();
        if (eocd < 0) {
            throw new java.io.UncheckedIOException(
                    new ZipException("no es un archivo ZIP: no se encontro el registro final"));
        }
        int cuantas = this.u16(eocd + 10);
        int inicioCen = (int) this.u32sinSigno(eocd + 16);
        int at = inicioCen;
        int i = 0;
        while (i < cuantas && at + 46 <= this.datos.length) {
            if (this.u32(at) != CEN_SIG) {
                throw new java.io.UncheckedIOException(
                        new ZipException("directorio central corrupto en " + at));
            }
            int metodo = this.u16(at + 10);
            int dosTime = this.u32(at + 12);
            long crc = this.u32sinSigno(at + 16);
            long csize = this.u32sinSigno(at + 20);
            long size = this.u32sinSigno(at + 24);
            int largoNombre = this.u16(at + 28);
            int largoExtra = this.u16(at + 30);
            int largoComentario = this.u16(at + 32);
            long offsetLocal = this.u32sinSigno(at + 42);

            byte[] crudo = new byte[largoNombre];
            System.arraycopy(this.datos, at + 46, crudo, 0, largoNombre);
            ZipEntry entrada = new ZipEntry(new String(crudo, this.charset));
            entrada.setMethod(metodo);
            entrada.setTime(dosTime);
            entrada.setCrc(crc);
            entrada.setCompressedSize(csize);
            entrada.setSize(size);
            if (largoComentario > 0) {
                byte[] c = new byte[largoComentario];
                System.arraycopy(this.datos, at + 46 + largoNombre + largoExtra, c, 0,
                        largoComentario);
                entrada.setComment(new String(c, this.charset));
            }
            this.entries.add(entrada);
            this.offsets.add(Long.valueOf(offsetLocal));
            at = at + 46 + largoNombre + largoExtra + largoComentario;
            i = i + 1;
        }
    }

    // Se busca **de atras para adelante** porque despues del EOCD puede haber un comentario de
    // archivo de largo variable, y no hay forma de saber donde arranca sin buscar su firma. El
    // comentario mide como mucho 65535, asi que no hace falta mirar mas atras que eso.
    private int buscarEocd() {
        int minimo = this.datos.length - 22 - 65535;
        if (minimo < 0) {
            minimo = 0;
        }
        int at = this.datos.length - 22;
        while (at >= minimo) {
            if (this.u32(at) == EOCD_SIG) {
                return at;
            }
            at = at - 1;
        }
        return -1;
    }

    // ---- la superficie publica ---------------------------------------------------------------------

    public String getName() {
        return this.name;
    }

    /** El comentario del archivo, o `null` si no tiene. */
    public String getComment() {
        this.comprobarAbierto();
        int eocd = this.buscarEocd();
        if (eocd < 0) {
            return null;
        }
        int largo = this.u16(eocd + 20);
        if (largo == 0) {
            return null;
        }
        byte[] c = new byte[largo];
        System.arraycopy(this.datos, eocd + 22, c, 0, largo);
        return new String(c, this.charset);
    }

    public ZipEntry getEntry(String entryName) {
        this.comprobarAbierto();
        int i = this.indiceDe(entryName);
        if (i < 0) {
            return null;
        }
        return this.entries.get(i);
    }

    private int indiceDe(String entryName) {
        int i = 0;
        while (i < this.entries.size()) {
            if (this.entries.get(i).getName().equals(entryName)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /**
     * Un flujo con el contenido **descomprimido** de esa entrada.
     *
     * <p>El salto a los datos necesita la cabecera **local** y no la del directorio: los largos del
     * nombre y del campo extra pueden diferir entre las dos, y el que vale para saber donde empiezan
     * los bytes es el de la local.
     *
     * @throws NullPointerException si `entry` es `null`
     */
    public InputStream getInputStream(ZipEntry entry) throws java.io.IOException {
        this.comprobarAbierto();
        if (entry == null) {
            throw new NullPointerException("entry");
        }
        int i = this.indiceDe(entry.getName());
        if (i < 0) {
            return null;
        }
        int local = (int) this.offsets.get(i).longValue();
        if (this.u32(local) != LOC_SIG) {
            throw new java.io.UncheckedIOException(
                    new ZipException("cabecera local corrupta en " + local));
        }
        int largoNombre = this.u16(local + 26);
        int largoExtra = this.u16(local + 28);
        int datosAt = local + 30 + largoNombre + largoExtra;
        ZipEntry e = this.entries.get(i);
        int comprimido = (int) e.getCompressedSize();
        byte[] crudo = new byte[comprimido];
        System.arraycopy(this.datos, datosAt, crudo, 0, comprimido);
        if (e.getMethod() == ZipEntry.STORED) {
            return new ByteArrayInputStream(crudo);
        }
        return new InflaterInputStream(new ByteArrayInputStream(crudo), new Inflater(true));
    }

    public Enumeration<ZipEntry> entries() {
        this.comprobarAbierto();
        return new ZipEntryEnumeration(this.entries);
    }

    /** Las entradas como flujo. Es la forma moderna de `entries()`. */
    public java.util.stream.Stream<ZipEntry> stream() {
        this.comprobarAbierto();
        return this.entries.stream();
    }

    public int size() {
        this.comprobarAbierto();
        return this.entries.size();
    }

    /**
     * Cierra el archivo.
     *
     * <p>No hay nada que soltar --el contenido ya esta en memoria-- pero el objeto queda **cerrado**,
     * y usarlo despues falla. Eso no es ceremonia: es lo que hace que el codigo escrito contra esta
     * clase se comporte igual el dia que haya un descriptor de verdad.
     */
    public void close() throws java.io.IOException {
        this.cerrado = true;
    }

    private void comprobarAbierto() {
        if (this.cerrado) {
            throw new IllegalStateException("zip file closed");
        }
    }

    public String toString() {
        return this.name;
    }
}

// The enumeration over the entry list. Top-level and package-private: a nested type is what
// finding #101 trips over, and the gate skips a class with no JDK counterpart.
class ZipEntryEnumeration implements Enumeration<ZipEntry> {

    private final List<ZipEntry> entries;
    private int at;

    ZipEntryEnumeration(List<ZipEntry> entries) {
        this.entries = entries;
        this.at = 0;
    }

    public boolean hasMoreElements() {
        return this.at < this.entries.size();
    }

    public ZipEntry nextElement() {
        if (this.at >= this.entries.size()) {
            throw new java.util.NoSuchElementException();
        }
        ZipEntry e = this.entries.get(this.at);
        this.at = this.at + 1;
        return e;
    }
}
