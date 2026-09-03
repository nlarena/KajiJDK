package java.util.zip;

import java.io.InputStream;

// Reads the ZIP container as a STREAM: entry after entry, front to back, without ever seeking.
// That constraint is what separates it from `ZipFile`, and it has a real consequence — a
// streaming reader cannot use the central directory at the end of the archive, so it has to
// trust each entry's local header, and when the local header says "sizes unknown" (flag bit 3)
// it must find the end of the data by other means.
//
// Una entrada escrita con **descriptor de datos** se lee igual: el final de los datos lo marca el
// propio flujo deflate --el inflater sabe cuando termino-- y el descriptor de 16 bytes que viene
// despues se saltea, tomando de el el CRC y los tamanios que la cabecera local no traia.
//
// Lo unico delicado es que el inflater **lee de mas**: cuando termina, los bytes que no consumio ya
// salieron del flujo de abajo. `Inflater.getRemaining()` dice cuantos son, y se los devuelve a una
// pequenia cola de relectura para que el descriptor y la cabecera siguiente se lean enteros. Sin
// eso, todo lo que viene despues arranca corrido.
//
// The `throws IOException` clauses are omitted throughout (finding #104).
public class ZipInputStream extends InflaterInputStream {

    private static final int LOCAL_SIG = 0x04034b50;
    private static final int DESCRIPTOR_SIG = 0x08074b50;

    private ZipEntry current;
    private long remaining;
    private boolean entryEof;
    // Si la entrada actual traia sus tamanios en un descriptor **despues** de los datos.
    private boolean conDescriptor;
    // Los bytes que el inflater leyo de mas y hay que volver a mirar. Es una cola diminuta --nunca
    // mas que el buffer de relleno-- y existe porque el inflater consume por bloques.
    private byte[] relectura = new byte[0];
    private int relecturaAt;

    // El charset con el que se decodifican los **nombres de entrada**. UTF-8 por defecto, que es lo
    // que dice el JDK y lo que produce cualquier herramienta moderna.
    private final java.nio.charset.Charset charset;

    /** Lee el archivo, decodificando los nombres en UTF-8. */
    public ZipInputStream(InputStream in) {
        this(in, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Lee el archivo, decodificando los nombres con `charset`.
     *
     * <p>Existe porque **el formato ZIP no dice en que codificacion estan los nombres**. Hay un bit
     * de bandera que promete UTF-8, pero mucho archivo viejo no lo enciende y guarda el nombre en la
     * pagina de codigos del sistema que lo creo. Sin poder elegir, esos nombres se leen mal y no hay
     * forma de arreglarlo desde afuera.
     *
     * @throws NullPointerException si `charset` es `null`
     */
    public ZipInputStream(InputStream in, java.nio.charset.Charset charset) {
        super(in, new Inflater(true));
        if (charset == null) {
            throw new NullPointerException("charset");
        }
        this.charset = charset;
    }

    // Advances to the next entry and returns its metadata, or null at the end of the archive.
    public ZipEntry getNextEntry() throws java.io.IOException {
        closeEntry();
        ZipEntry entry = null;
        int sig = readInt();
        if (sig == LOCAL_SIG) {
            readShort();                       // version needed
            int flags = readShort();
            int method = readShort();
            long dosTime = (long) readInt() & 0xffffffffL;
            long crc = (long) readInt() & 0xffffffffL;
            long csize = (long) readInt() & 0xffffffffL;
            long size = (long) readInt() & 0xffffffffL;
            int nameLen = readShort();
            int extraLen = readShort();
            String name = readString(nameLen);
            skipBytes(extraLen);
            entry = createZipEntry(name);
            entry.setMethod(method);
            entry.setTime(dosTime);
            if ((flags & 8) == 0) {
                entry.setCrc(crc);
                entry.setCompressedSize(csize);
                entry.setSize(size);
                remaining = csize;
                conDescriptor = false;
            } else {
                // Los tamanios vienen en un descriptor **despues** de los datos. El final se sabe
                // por el flujo deflate, no por un contador.
                remaining = -1;
                conDescriptor = true;
            }
            current = entry;
            entryEof = false;
            // El inflater arranca de cero para cada entrada: cada una es un flujo deflate propio.
            inf.reset();
        }
        return entry;
    }

    public void closeEntry() throws java.io.IOException {
        if (current != null) {
            if (remaining > 0) {
                skipBytes((int) remaining);
            } else if (remaining < 0) {
                // Tamanio desconocido: se drena hasta el final del flujo deflate. Es lo unico que
                // dice donde terminan los datos cuando la cabecera local no lo dijo.
                byte[] scratch = new byte[512];
                int n = read(scratch, 0, scratch.length);
                while (n > 0) {
                    n = read(scratch, 0, scratch.length);
                }
            }
            ZipEntry cerrada = current;
            current = null;
            remaining = 0;
            entryEof = true;
            if (conDescriptor) {
                devolverSobrantes();
                leerDescriptor(cerrada);
                conDescriptor = false;
            }
        }
    }

    public int available() throws java.io.IOException {
        int n = 1;
        if (entryEof || current == null) {
            n = 0;
        }
        return n;
    }

    public int read() throws java.io.IOException {
        byte[] one = new byte[1];
        int n = read(one, 0, 1);
        int result = -1;
        if (n == 1) {
            result = one[0] & 0xff;
        }
        return result;
    }

    public int read(byte[] b, int off, int len) throws java.io.IOException {
        int result = -1;
        if (current != null && !entryEof) {
            if (current.getMethod() == ZipEntry.STORED) {
                int want = len;
                if (remaining >= 0 && (long) want > remaining) {
                    want = (int) remaining;
                }
                if (want == 0) {
                    entryEof = true;
                } else {
                    int n = in.read(b, off, want);
                    if (n == -1) {
                        entryEof = true;
                    } else {
                        remaining = remaining - (long) n;
                        result = n;
                    }
                }
            } else {
                int n = readInflated(b, off, len);
                if (n == -1) {
                    entryEof = true;
                } else {
                    result = n;
                }
            }
        }
        return result;
    }

    public long skip(long n) throws java.io.IOException {
        byte[] scratch = new byte[512];
        long skipped = 0;
        boolean done = false;
        while (skipped < n && !done) {
            long left = n - skipped;
            int want = scratch.length;
            if (left < (long) want) {
                want = (int) left;
            }
            int got = read(scratch, 0, want);
            if (got == -1) {
                done = true;
            } else {
                skipped = skipped + (long) got;
            }
        }
        return skipped;
    }

    // The seam a subclass overrides to get its own entry type back from `getNextEntry`.
    protected ZipEntry createZipEntry(String name) {
        return new ZipEntry(name);
    }

    // ---- lectura de campos, little-endian como todo el formato ----

    /**
     * El relleno del inflater, **empezando por la cola de relectura**.
     *
     * <p>Es la otra mitad del arreglo del descriptor. Los bytes que el inflater leyo de mas quedaron
     * en la cola; si el relleno los ignorara y fuera directo al flujo, la entrada siguiente
     * arrancaria salteando justo esos bytes. Con dos entradas seguidas se ve enseguida: la segunda
     * lee cero.
     */
    protected void fill() throws java.io.IOException {
        int pendientes = this.relectura.length - this.relecturaAt;
        if (pendientes <= 0) {
            len = in.read(buf, 0, buf.length);
            if (len > 0) {
                inf.setInput(buf, 0, len);
            }
            return;
        }
        int cuantos = pendientes;
        if (cuantos > buf.length) {
            cuantos = buf.length;
        }
        System.arraycopy(this.relectura, this.relecturaAt, buf, 0, cuantos);
        this.relecturaAt = this.relecturaAt + cuantos;
        int total = cuantos;
        // Si sobra lugar, se completa desde el flujo: un bloque mas grande le da al inflater mas
        // para trabajar y evita una vuelta extra.
        if (total < buf.length) {
            int mas = in.read(buf, total, buf.length - total);
            if (mas > 0) {
                total = total + mas;
            }
        }
        len = total;
        inf.setInput(buf, 0, len);
    }

    // Todas las lecturas de campo pasan por aca: primero lo que el inflater devolvio, despues el
    // flujo. Sin este unico punto de entrada, la mitad de los campos se leerian del lugar
    // equivocado justo despues de una entrada comprimida.
    private int leerByte() throws java.io.IOException {
        if (this.relecturaAt < this.relectura.length) {
            int b = this.relectura[this.relecturaAt] & 0xff;
            this.relecturaAt = this.relecturaAt + 1;
            return b;
        }
        return in.read();
    }

    // Los bytes que el inflater tomo del flujo y no consumio vuelven a la cola.
    private void devolverSobrantes() {
        int sobran = inf.getRemaining();
        if (sobran <= 0) {
            return;
        }
        byte[] nueva = new byte[sobran];
        System.arraycopy(buf, len - sobran, nueva, 0, sobran);
        this.relectura = nueva;
        this.relecturaAt = 0;
    }

    // El registro de 16 bytes que sigue a los datos cuando la cabecera local no traia los tamanios.
    // La firma es **opcional** en el formato, asi que se la mira y solo se la consume si esta.
    private void leerDescriptor(ZipEntry entry) throws java.io.IOException {
        int primero = this.readInt();
        long crc;
        if (primero == DESCRIPTOR_SIG) {
            crc = (long) this.readInt() & 0xffffffffL;
        } else {
            crc = (long) primero & 0xffffffffL;
        }
        long csize = (long) this.readInt() & 0xffffffffL;
        long size = (long) this.readInt() & 0xffffffffL;
        if (entry != null) {
            entry.setCrc(crc);
            entry.setCompressedSize(csize);
            entry.setSize(size);
        }
    }

    private int readInt() throws java.io.IOException {
        int b0 = leerByte();
        int b1 = leerByte();
        int b2 = leerByte();
        int b3 = leerByte();
        int value = 0;
        if (b3 != -1) {
            value = (b0 & 0xff) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24);
        }
        return value;
    }

    private int readShort() throws java.io.IOException {
        int b0 = leerByte();
        int b1 = leerByte();
        return (b0 & 0xff) | ((b1 & 0xff) << 8);
    }

    // Se leen los **bytes** y recien despues se decodifican. Antes se armaba el String caracter a
    // caracter con `(char) (b & 0xff)`, que es Latin-1 disfrazado: un nombre en UTF-8 con acentos
    // volvia partido en dos caracteres por letra.
    private String readString(int len) throws java.io.IOException {
        byte[] raw = new byte[len];
        int i = 0;
        while (i < len) {
            raw[i] = (byte) leerByte();
            i = i + 1;
        }
        return new String(raw, this.charset);
    }

    private void skipBytes(int count) throws java.io.IOException {
        int i = 0;
        while (i < count) {
            leerByte();
            i = i + 1;
        }
    }
}
