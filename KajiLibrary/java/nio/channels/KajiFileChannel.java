package java.nio.channels;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import jdk.internal.io.Fs;

/**
 * La implementacion de {@link FileChannel} sobre los nativos de archivo de esta VM.
 *
 * <p>Es lo que {@link FileChannel#open} devuelve, y no es publica a proposito: nadie deberia
 * escribir su nombre. El porque de que cada operacion toque el disco --y el precio que eso tiene--
 * esta explicado en la cabecera de {@link FileChannel}; aca solo esta el como.
 *
 * <p>Las tres reglas que se repiten y conviene leer una sola vez:
 *
 * <ul>
 *   <li>toda lectura arranca por {@link #contenido()}, que va al disco. No hay cache, asi que no hay
 *       cache que invalidar ni momento en el que lo que se ve deje de ser lo que hay;
 *   <li>toda escritura es leer-modificar-escribir el archivo entero. Por eso vive en un solo lugar,
 *       {@link #volcar}: repartida en cinco metodos, tarde o temprano uno se olvidaria de conservar
 *       la cola del archivo y truncaria datos ajenos;
 *   <li>escribir mas alla del final rellena con ceros, que es lo que hace un archivo ralo del JDK
 *       cuando se lo lee.
 * </ul>
 */
final class KajiFileChannel extends FileChannel {

    // El limite de un `byte[]`. Un archivo mas grande que esto no entra en un arreglo, y el error
    // tiene que salir como tal y no como un `NegativeArraySizeException` desde las entranias.
    private static final long TOPE = 2147483639L;

    private final String ruta;
    private final boolean leer;
    private final boolean escribir;
    private final boolean anexar;
    private final boolean borrarAlCerrar;

    private long pos = 0;

    private KajiFileChannel(String ruta, boolean leer, boolean escribir, boolean anexar,
            boolean borrarAlCerrar) {
        this.ruta = ruta;
        this.leer = leer;
        this.escribir = escribir;
        this.anexar = anexar;
        this.borrarAlCerrar = borrarAlCerrar;
    }

    // ---- apertura --------------------------------------------------------------------------------

    static FileChannel abrir(Path path, OpenOption[] options) throws IOException {
        if (path == null) {
            throw new NullPointerException();
        }
        String p = path.toString();

        boolean leer = false;
        boolean escribir = false;
        boolean anexar = false;
        boolean truncar = false;
        boolean crear = false;
        boolean crearNuevo = false;
        boolean borrarAlCerrar = false;

        int i = 0;
        while (i < options.length) {
            OpenOption o = options[i];
            if (o == null) {
                throw new NullPointerException();
            }
            if (o == StandardOpenOption.READ) {
                leer = true;
            } else if (o == StandardOpenOption.WRITE) {
                escribir = true;
            } else if (o == StandardOpenOption.APPEND) {
                anexar = true;
            } else if (o == StandardOpenOption.TRUNCATE_EXISTING) {
                truncar = true;
            } else if (o == StandardOpenOption.CREATE) {
                crear = true;
            } else if (o == StandardOpenOption.CREATE_NEW) {
                crearNuevo = true;
            } else if (o == StandardOpenOption.DELETE_ON_CLOSE) {
                borrarAlCerrar = true;
            } else if (o == StandardOpenOption.SYNC || o == StandardOpenOption.DSYNC) {
                // Se aceptan porque se cumplen: este canal escribe al disco en cada `write`. No hay
                // bandera que guardar; lo que piden ya es como funciona.
            } else if (o == LinkOption.NOFOLLOW_LINKS) {
                // Sin enlaces en el modelo, no seguirlos es lo unico que se puede hacer.
            } else {
                // `SPARSE` cae aca. Ver la nota de `FileChannel.open`.
                throw new UnsupportedOperationException(String.valueOf(o) + " not supported");
            }
            i = i + 1;
        }

        if (anexar && leer) {
            throw new IllegalArgumentException("READ + APPEND not allowed");
        }
        if (anexar && truncar) {
            throw new IllegalArgumentException("APPEND + TRUNCATE_EXISTING not allowed");
        }
        // Sin ninguna opcion de acceso, se lee. Es lo que dice el JDK y lo que espera cualquiera que
        // llame `open(path)` a secas.
        if (!leer && !escribir && !anexar) {
            leer = true;
        }

        boolean existe = (Fs.stat(p) & Fs.EXISTE) != 0;
        if (crearNuevo && existe) {
            throw new FileAlreadyExistsException(p);
        }
        boolean puedeCrear = (crear || crearNuevo) && (escribir || anexar);
        if (!existe && !puedeCrear) {
            throw new NoSuchFileException(p);
        }
        if (!existe) {
            if (!Fs.writeAllBytes(p, new byte[0], false)) {
                throw new IOException("no se pudo crear " + p);
            }
        } else if (truncar && (escribir || anexar)) {
            if (!Fs.writeAllBytes(p, new byte[0], false)) {
                throw new IOException("no se pudo truncar " + p);
            }
        }

        KajiFileChannel c = new KajiFileChannel(p, leer, escribir || anexar, anexar, borrarAlCerrar);
        return c;
    }

    // ---- lo de abajo -----------------------------------------------------------------------------

    private byte[] contenido() throws IOException {
        byte[] b = Fs.readAllBytes(this.ruta);
        if (b == null) {
            // Pasa si el archivo desaparecio despues de abrir. No es un caso raro de laboratorio:
            // otro proceso puede borrarlo en cualquier momento, y devolver un arreglo vacio lo
            // haria pasar por un archivo que quedo en cero.
            throw new IOException("no se pudo leer " + this.ruta);
        }
        return b;
    }

    // Escribe `len` bytes de `datos` a partir de `desde`, conservando todo lo que ya habia antes y
    // despues de ese tramo. Devuelve `len`.
    private int volcar(long desde, byte[] datos, int off, int len) throws IOException {
        byte[] viejo = this.contenido();
        long fin = desde + len;
        if (fin > TOPE) {
            throw new IOException("archivo demasiado grande para esta VM");
        }
        int nuevoLargo = (int) Math.max((long) viejo.length, fin);
        byte[] nuevo;
        if (nuevoLargo == viejo.length) {
            nuevo = viejo;
        } else {
            // El relleno intermedio queda en cero solo: `new byte[]` ya los pone, que es justo el
            // hueco de ceros que corresponde cuando se escribe mas alla del final.
            nuevo = new byte[nuevoLargo];
            System.arraycopy(viejo, 0, nuevo, 0, viejo.length);
        }
        System.arraycopy(datos, off, nuevo, (int) desde, len);
        if (!Fs.writeAllBytes(this.ruta, nuevo, false)) {
            throw new IOException("no se pudo escribir " + this.ruta);
        }
        return len;
    }

    private void exigirAbierto() throws IOException {
        if (!this.isOpen()) {
            throw new ClosedChannelException();
        }
    }

    private void exigirLectura() throws IOException {
        this.exigirAbierto();
        if (!this.leer) {
            throw new NonReadableChannelException();
        }
    }

    private void exigirEscritura() throws IOException {
        this.exigirAbierto();
        if (!this.escribir) {
            throw new NonWritableChannelException();
        }
    }

    // Saca de `src` lo que le quede, como arreglo, y deja la posicion del buffer al final. Todas las
    // escrituras pasan por aca para que el avance de la posicion del buffer sea uno solo y no cinco.
    private static byte[] drenar(ByteBuffer src) {
        int n = src.remaining();
        byte[] b = new byte[n];
        src.get(b, 0, n);
        return b;
    }

    // ---- lectura ---------------------------------------------------------------------------------

    public int read(ByteBuffer dst) throws IOException {
        this.exigirLectura();
        int n = this.leerEn(dst, this.pos);
        if (n > 0) {
            this.pos = this.pos + n;
        }
        return n;
    }

    public int read(ByteBuffer dst, long position) throws IOException {
        if (position < 0) {
            throw new IllegalArgumentException("posicion negativa");
        }
        this.exigirLectura();
        return this.leerEn(dst, position);
    }

    private int leerEn(ByteBuffer dst, long desde) throws IOException {
        if (dst == null) {
            throw new NullPointerException();
        }
        if (dst.isReadOnly()) {
            throw new java.nio.ReadOnlyBufferException();
        }
        boolean bien = false;
        this.begin();
        try {
            byte[] datos = this.contenido();
            if (desde >= datos.length) {
                bien = true;
                // Fin de archivo es -1 aunque el buffer estuviera lleno; el que no quede lugar es
                // otra historia y va abajo.
                return -1;
            }
            int libres = dst.remaining();
            if (libres == 0) {
                bien = true;
                return 0;
            }
            int n = (int) Math.min((long) libres, (long) datos.length - desde);
            dst.put(datos, (int) desde, n);
            bien = true;
            return n;
        } finally {
            this.end(bien);
        }
    }

    public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
        comprobarRango(dsts, offset, length);
        this.exigirLectura();
        long total = 0;
        int i = offset;
        while (i < offset + length) {
            ByteBuffer d = dsts[i];
            if (d.remaining() > 0) {
                int n = this.read(d);
                if (n < 0) {
                    // Fin de archivo. Si ya se habia leido algo se devuelve eso; si no, -1. Devolver
                    // 0 en el segundo caso haria que un lazo de lectura no terminara nunca.
                    if (total == 0) {
                        return -1;
                    }
                    return total;
                }
                total = total + n;
                if (d.hasRemaining()) {
                    // El buffer no se lleno, y este canal nunca devuelve lecturas cortas por otro
                    // motivo: significa que el archivo se acabo. Seguir con el buffer siguiente
                    // solo repetiria el -1.
                    return total;
                }
            }
            i = i + 1;
        }
        return total;
    }

    // ---- escritura -------------------------------------------------------------------------------

    public int write(ByteBuffer src) throws IOException {
        this.exigirEscritura();
        if (src == null) {
            throw new NullPointerException();
        }
        boolean bien = false;
        this.begin();
        try {
            // En modo anexar la posicion corriente no manda: el destino es siempre el final vigente
            // en el momento de escribir, que es lo unico que hace util a `APPEND`.
            long desde;
            if (this.anexar) {
                desde = this.tamanio();
            } else {
                desde = this.pos;
            }
            byte[] b = drenar(src);
            int n = this.volcar(desde, b, 0, b.length);
            this.pos = desde + n;
            bien = true;
            return n;
        } finally {
            this.end(bien);
        }
    }

    public int write(ByteBuffer src, long position) throws IOException {
        if (position < 0) {
            throw new IllegalArgumentException("posicion negativa");
        }
        this.exigirEscritura();
        if (this.anexar) {
            // Escribir en una posicion elegida contradice lo unico que `APPEND` promete --que todo
            // va al final-- y el JDK lo prohibe por eso mismo.
            throw new IOException("canal abierto en modo APPEND");
        }
        if (src == null) {
            throw new NullPointerException();
        }
        boolean bien = false;
        this.begin();
        try {
            byte[] b = drenar(src);
            int n = this.volcar(position, b, 0, b.length);
            bien = true;
            return n;
        } finally {
            this.end(bien);
        }
    }

    public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
        comprobarRango(srcs, offset, length);
        this.exigirEscritura();
        long total = 0;
        int i = offset;
        while (i < offset + length) {
            ByteBuffer s = srcs[i];
            if (s.remaining() > 0) {
                total = total + this.write(s);
            }
            i = i + 1;
        }
        return total;
    }

    // ---- posicion y tamanio ----------------------------------------------------------------------

    public long position() throws IOException {
        this.exigirAbierto();
        return this.pos;
    }

    public FileChannel position(long newPosition) throws IOException {
        if (newPosition < 0) {
            throw new IllegalArgumentException("posicion negativa");
        }
        this.exigirAbierto();
        this.pos = newPosition;
        return this;
    }

    public long size() throws IOException {
        this.exigirAbierto();
        return this.tamanio();
    }

    private long tamanio() throws IOException {
        return Fs.size(this.ruta);
    }

    public FileChannel truncate(long size) throws IOException {
        if (size < 0) {
            throw new IllegalArgumentException("tamanio negativo");
        }
        this.exigirEscritura();
        byte[] viejo = this.contenido();
        if (size < viejo.length) {
            byte[] nuevo = new byte[(int) size];
            System.arraycopy(viejo, 0, nuevo, 0, (int) size);
            if (!Fs.writeAllBytes(this.ruta, nuevo, false)) {
                throw new IOException("no se pudo truncar " + this.ruta);
            }
        }
        // La posicion se recorta aunque el archivo no haya cambiado de tama&ntilde;o: el contrato es
        // que nunca quede apuntando mas alla del final.
        if (this.pos > size) {
            this.pos = size;
        }
        return this;
    }

    public void force(boolean metaData) throws IOException {
        this.exigirAbierto();
        // Nada que forzar; ver la nota de `FileChannel.force`.
    }

    // ---- transferencias --------------------------------------------------------------------------

    public long transferTo(long position, long count, WritableByteChannel target)
            throws IOException {
        if (position < 0 || count < 0) {
            throw new IllegalArgumentException("posicion o cuenta negativa");
        }
        if (target == null) {
            throw new NullPointerException();
        }
        this.exigirLectura();
        if (!target.isOpen()) {
            throw new ClosedChannelException();
        }
        byte[] datos = this.contenido();
        if (position >= datos.length) {
            return 0;
        }
        int n = (int) Math.min(count, (long) datos.length - position);
        ByteBuffer bb = ByteBuffer.wrap(datos, (int) position, n);
        long escritos = 0;
        while (bb.hasRemaining()) {
            int w = target.write(bb);
            if (w <= 0) {
                break;
            }
            escritos = escritos + w;
        }
        return escritos;
    }

    public long transferFrom(ReadableByteChannel src, long position, long count)
            throws IOException {
        if (position < 0 || count < 0) {
            throw new IllegalArgumentException("posicion o cuenta negativa");
        }
        if (src == null) {
            throw new NullPointerException();
        }
        this.exigirEscritura();
        if (!src.isOpen()) {
            throw new ClosedChannelException();
        }
        if (position > this.tamanio()) {
            // El JDK no agranda el archivo para llegar hasta ahi: si la posicion pasa del final, no
            // se transfiere nada.
            return 0;
        }
        if (count > TOPE) {
            throw new IOException("transferencia demasiado grande para esta VM");
        }
        ByteBuffer bb = ByteBuffer.allocate((int) count);
        long leidos = 0;
        while (bb.hasRemaining()) {
            int n = src.read(bb);
            if (n <= 0) {
                break;
            }
            leidos = leidos + n;
        }
        if (leidos == 0) {
            return 0;
        }
        return this.volcar(position, bb.array(), 0, (int) leidos);
    }

    // ---- cierre ----------------------------------------------------------------------------------

    protected void implCloseChannel() throws IOException {
        if (this.borrarAlCerrar) {
            // Sin `throws` si falla: `DELETE_ON_CLOSE` es una limpieza, y hacer fallar el cierre
            // porque no se pudo limpiar convierte un descuido en un error del programa.
            Fs.delete(this.ruta);
        }
    }

    // ---- comun -----------------------------------------------------------------------------------

    private static void comprobarRango(ByteBuffer[] bufs, int offset, int length) {
        if (bufs == null) {
            throw new NullPointerException();
        }
        if (offset < 0 || length < 0 || length > bufs.length - offset) {
            throw new IndexOutOfBoundsException();
        }
    }
}
