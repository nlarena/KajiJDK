package java.io;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

// KajiLibrary's java.io.RandomAccessFile -- leer y escribir un archivo en cualquier orden.
//
// Es la unica clase de `java.io` que no es un flujo: los demas van para adelante y esta se mueve. De
// ahi sale todo lo que la distingue -- `seek`, `getFilePointer`, `setLength` -- y tambien que
// implemente `DataInput` **y** `DataOutput` a la vez, que ningun flujo hace.
//
// <h2>Sobre que esta construida, y por que sobre eso</h2>
//
// **Delega entera en un `java.nio.channels.FileChannel`.** No guarda ni la posicion ni los bytes:
// los pide. La razon no es ahorrar codigo sino que `getChannel()` no pueda mentir.
//
// El contrato de `getChannel()` dice que la posicion del canal y el puntero del archivo son **el
// mismo numero**: mover uno mueve el otro. Con una posicion propia aca y otra en el canal, ese
// contrato hay que sostenerlo a mano en nueve metodos, y el dia que uno se olvide el que llama se
// entera leyendo del lugar equivocado -- en silencio, porque leer de un offset que no era sigue
// devolviendo bytes. Sin posicion propia no hay nada que sincronizar: hay un solo numero.
//
// Lo que se hereda de esa eleccion, para bien y para mal, esta documentado en `FileChannel`: cada
// lectura va al disco y cada escritura reescribe el archivo entero. Es O(n) por operacion --escribir
// un byte al final de un archivo de un mega mueve un mega-- y a cambio, cuando `write` vuelve, los
// bytes **estan**. Para esta clase el precio pesa mas que para las otras, porque el uso tipico de un
// archivo de acceso aleatorio es justamente muchas escrituras chicas salteadas. Quien tenga eso y le
// importe la velocidad quiere un buffer propio, no esta clase.
//
// <h2>Los modos `rws` y `rwd` se aceptan porque se cumplen</h2>
//
// Los dos piden que cada escritura llegue al dispositivo antes de volver --`rwd` los datos, `rws`
// datos y metadatos-- y aca eso ya pasa siempre, por lo de arriba. No se ignoran: se cumplen de
// entrada. La unica diferencia con el JDK es que no cuestan nada.
//
// <h2>Lo que no es igual al JDK, dicho de frente</h2>
//
// `getFD()` devuelve un `FileDescriptor` **invalido** --`valid()` da falso-- porque esta VM no
// modela descriptores; es lo mismo que hacen `FileInputStream` y `FileOutputStream`, y ahi esta
// explicado. Se declara igual porque la respuesta es comprobable: quien pregunte `valid()` recibe un
// "no" y no un handle inventado que despues no sirva.
public class RandomAccessFile implements DataOutput, DataInput, Closeable {

    private final FileChannel canal;

    // Si el modo permite escribir. Se guarda aparte del canal porque el rechazo tiene que salir como
    // `IOException` --lo que promete el contrato de esta clase-- y no como la
    // `NonWritableChannelException` no chequeada que tiraria el canal: el que llama escribio un
    // `catch (IOException)` y esa se le escaparia por al lado.
    private final boolean escribible;

    private final String ruta;

    private boolean cerrado = false;

    /**
     * Abre `name` en el modo dado.
     *
     * @throws NullPointerException si `name` o `mode` son `null`
     * @throws IllegalArgumentException si el modo no es uno de los cuatro
     * @throws FileNotFoundException si no existe y el modo no lo crea, o si es un directorio
     */
    public RandomAccessFile(String name, String mode) throws FileNotFoundException {
        this(name == null ? null : new File(name), mode);
    }

    /**
     * Abre `file` en el modo dado.
     *
     * <p>`"r"` abre para lectura y **no crea**; los tres `"rw*"` crean el archivo si falta y **no lo
     * truncan** si estaba. Lo segundo es lo que distingue a esta clase de `FileOutputStream`, y es
     * deliberado: se abre un archivo de acceso aleatorio justamente para modificar partes de lo que
     * ya hay.
     *
     * @throws NullPointerException si `file` o `mode` son `null`
     * @throws IllegalArgumentException si el modo no es uno de los cuatro
     * @throws FileNotFoundException si no existe y el modo no lo crea, o si es un directorio
     */
    public RandomAccessFile(File file, String mode) throws FileNotFoundException {
        if (file == null) {
            throw new NullPointerException();
        }
        if (mode == null) {
            throw new NullPointerException();
        }
        boolean escribe;
        if (mode.equals("r")) {
            escribe = false;
        } else if (mode.equals("rw") || mode.equals("rws") || mode.equals("rwd")) {
            escribe = true;
        } else {
            throw new IllegalArgumentException(
                "Illegal mode \"" + mode + "\" must be one of \"r\", \"rw\", \"rws\", or \"rwd\"");
        }
        this.escribible = escribe;
        this.ruta = file.getPath();

        // Un directorio se rechaza aparte y con su propio mensaje, igual que en `FileInputStream`:
        // "es un directorio" y "no esta" mandan a buscar a lugares distintos.
        if (file.isDirectory()) {
            throw new FileNotFoundException(this.ruta + " (Is a directory)");
        }
        FileChannel c;
        try {
            if (escribe) {
                c = FileChannel.open(Path.of(this.ruta), StandardOpenOption.READ,
                        StandardOpenOption.WRITE, StandardOpenOption.CREATE);
            } else {
                c = FileChannel.open(Path.of(this.ruta), StandardOpenOption.READ);
            }
        } catch (NoSuchFileException ex) {
            throw new FileNotFoundException(this.ruta + " (No such file or directory)");
        } catch (IOException ex) {
            throw new FileNotFoundException(this.ruta + " (" + ex.getMessage() + ")");
        }
        this.canal = c;
    }

    // ---- el archivo como tal -------------------------------------------------------------------

    /** El descriptor. Invalido a proposito; ver la nota de la clase. */
    public final FileDescriptor getFD() throws IOException {
        this.exigirAbierto();
        return new FileDescriptor();
    }

    /**
     * El canal, que **comparte la posicion** con este objeto.
     *
     * <p>No es una copia ni una vista: es sobre el que esta clase trabaja. Por eso `seek(5)` deja el
     * canal en 5 y `canal.position(7)` deja `getFilePointer()` en 7, que es exactamente lo que el
     * contrato promete.
     */
    public final FileChannel getChannel() {
        return this.canal;
    }

    public long getFilePointer() throws IOException {
        this.exigirAbierto();
        return this.canal.position();
    }

    /**
     * Mueve el puntero a `pos`.
     *
     * <p>Se puede pasar del final: no falla y no agranda el archivo. Es lo que permite escribir un
     * hueco -- `seek` mas alla y escribir agranda rellenando con ceros -- y leer ahi da fin de
     * archivo.
     *
     * @throws IOException si `pos` es negativa
     */
    public void seek(long pos) throws IOException {
        if (pos < 0) {
            throw new IOException("Negative seek offset");
        }
        this.exigirAbierto();
        this.canal.position(pos);
    }

    public long length() throws IOException {
        this.exigirAbierto();
        return this.canal.size();
    }

    /**
     * Fija el largo del archivo.
     *
     * <p>Acortar **recorta el puntero** si quedaba mas alla del nuevo final; agrandar no lo mueve.
     * Los bytes que aparecen al agrandar son ceros.
     *
     * @throws IOException si `newLength` es negativo o si el archivo se abrio solo para lectura
     */
    public void setLength(long newLength) throws IOException {
        if (newLength < 0) {
            throw new IOException("Negative length");
        }
        this.exigirEscritura();
        long actual = this.canal.size();
        if (newLength < actual) {
            this.canal.truncate(newLength);
        } else if (newLength > actual) {
            // Un solo byte en cero en la ultima posicion: el canal rellena el hueco intermedio, que
            // es la misma mecanica con que se escribe mas alla del final.
            this.canal.write(ByteBuffer.wrap(new byte[1]), newLength - 1);
        }
    }

    public void close() throws IOException {
        if (this.cerrado) {
            return;             // cerrar dos veces no es un error, y el contrato lo dice
        }
        this.cerrado = true;
        this.canal.close();
    }

    // ---- lectura cruda -------------------------------------------------------------------------

    /** El proximo byte como 0..255, o -1 al final. */
    public int read() throws IOException {
        this.exigirAbierto();
        byte[] uno = new byte[1];
        int n = this.canal.read(ByteBuffer.wrap(uno));
        if (n <= 0) {
            return -1;
        }
        return uno[0] & 0xFF;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        this.exigirAbierto();
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            // Cero y no -1 aunque este al final: no se pidio ningun byte, asi que no hubo nada que
            // no se pudiera dar. El JDK distingue estos dos, y un lazo que confunda "no pedi nada"
            // con "no hay mas" termina antes de tiempo.
            return 0;
        }
        return this.canal.read(ByteBuffer.wrap(b, off, len));
    }

    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    public final void readFully(byte[] b) throws IOException {
        this.readFully(b, 0, b.length);
    }

    /**
     * Llena el tramo entero o lanza.
     *
     * <p>La diferencia con `read` es el contrato de fin: `read` devuelve lo que haya, `readFully`
     * exige todo. Es lo que hace falta para leer un `int` -- cuatro bytes o nada, porque tres bytes
     * de un entero no son un entero.
     *
     * @throws EOFException si el archivo se acaba antes
     */
    public final void readFully(byte[] b, int off, int len) throws IOException {
        int leidos = 0;
        while (leidos < len) {
            int n = this.read(b, off + leidos, len - leidos);
            if (n < 0) {
                throw new EOFException();
            }
            leidos = leidos + n;
        }
    }

    /**
     * Salta hasta `n` bytes, sin pasar del final.
     *
     * <p>Devuelve cuantos salto de verdad, que puede ser menos que `n` y es cero para `n` negativo.
     */
    public int skipBytes(int n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        long pos = this.getFilePointer();
        long largo = this.length();
        long nueva = pos + n;
        if (nueva > largo) {
            nueva = largo;
        }
        this.seek(nueva);
        return (int) (nueva - pos);
    }

    // ---- escritura cruda -----------------------------------------------------------------------

    public void write(int b) throws IOException {
        this.exigirEscritura();
        byte[] uno = new byte[1];
        uno[0] = (byte) b;
        this.canal.write(ByteBuffer.wrap(uno));
    }

    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        this.exigirEscritura();
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return;
        }
        this.canal.write(ByteBuffer.wrap(b, off, len));
    }

    // ---- DataInput -----------------------------------------------------------------------------
    //
    // Todo lo de abajo es formato, no acceso: big-endian, complemento a dos, IEEE 754 y UTF-8
    // modificado, exactamente como `DataInputStream`. Se escribe sobre `read()` y no sobre el canal
    // para que el avance del puntero sea uno solo.

    public final boolean readBoolean() throws IOException {
        return this.readUnsignedByte() != 0;
    }

    public final byte readByte() throws IOException {
        return (byte) this.readUnsignedByte();
    }

    public final int readUnsignedByte() throws IOException {
        int c = this.read();
        if (c < 0) {
            throw new EOFException();
        }
        return c;
    }

    public final short readShort() throws IOException {
        return (short) this.readUnsignedShort();
    }

    public final int readUnsignedShort() throws IOException {
        int a = this.readUnsignedByte();
        int b = this.readUnsignedByte();
        return (a << 8) | b;
    }

    public final char readChar() throws IOException {
        return (char) this.readUnsignedShort();
    }

    public final int readInt() throws IOException {
        int a = this.readUnsignedByte();
        int b = this.readUnsignedByte();
        int c = this.readUnsignedByte();
        int d = this.readUnsignedByte();
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    public final long readLong() throws IOException {
        long alta = this.readInt() & 0xFFFFFFFFL;
        long baja = this.readInt() & 0xFFFFFFFFL;
        return (alta << 32) | baja;
    }

    public final float readFloat() throws IOException {
        return Float.intBitsToFloat(this.readInt());
    }

    public final double readDouble() throws IOException {
        return Double.longBitsToDouble(this.readLong());
    }

    /**
     * Una linea, terminada en `\n`, `\r` o `\r\n`; `null` si ya estaba al final.
     *
     * <p>**Cada byte se convierte a un char tal cual**, sin decodificar. Es lo que dice el contrato
     * y por eso esta clase no sirve para leer texto que no sea de un byte por caracter: un acento en
     * UTF-8 son dos bytes y salen como dos chars distintos de el. Para texto hay `BufferedReader`
     * sobre un `InputStreamReader`, que si decodifica.
     */
    public final String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean algo = false;
        while (true) {
            int c = this.read();
            if (c < 0) {
                break;
            }
            algo = true;
            if (c == '\n') {
                break;
            }
            if (c == '\r') {
                // Mirar el byte siguiente y devolverlo si no era el `\n` del par: sin esto un
                // archivo con `\r` sueltos perderia el primer caracter de cada linea.
                long antes = this.getFilePointer();
                int sig = this.read();
                if (sig != '\n' && sig >= 0) {
                    this.seek(antes);
                }
                break;
            }
            sb.append((char) c);
        }
        if (!algo) {
            return null;
        }
        return sb.toString();
    }

    public final String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    // ---- DataOutput ----------------------------------------------------------------------------

    public final void writeBoolean(boolean v) throws IOException {
        this.write(v ? 1 : 0);
    }

    public final void writeByte(int v) throws IOException {
        this.write(v);
    }

    public final void writeShort(int v) throws IOException {
        this.write((v >>> 8) & 0xFF);
        this.write(v & 0xFF);
    }

    public final void writeChar(int v) throws IOException {
        this.writeShort(v);
    }

    public final void writeInt(int v) throws IOException {
        this.write((v >>> 24) & 0xFF);
        this.write((v >>> 16) & 0xFF);
        this.write((v >>> 8) & 0xFF);
        this.write(v & 0xFF);
    }

    public final void writeLong(long v) throws IOException {
        this.writeInt((int) (v >>> 32));
        this.writeInt((int) v);
    }

    public final void writeFloat(float v) throws IOException {
        this.writeInt(Float.floatToIntBits(v));
    }

    public final void writeDouble(double v) throws IOException {
        this.writeLong(Double.doubleToLongBits(v));
    }

    /** Un byte por caracter, quedandose con los ocho bits de abajo. Ver la nota de `readLine`. */
    public final void writeBytes(String s) throws IOException {
        int i = 0;
        while (i < s.length()) {
            this.write(s.charAt(i) & 0xFF);
            i = i + 1;
        }
    }

    /** Dos bytes por caracter, big-endian, y **sin largo delante**: no es autodelimitado. */
    public final void writeChars(String s) throws IOException {
        int i = 0;
        while (i < s.length()) {
            this.writeChar(s.charAt(i));
            i = i + 1;
        }
    }

    /**
     * El texto en UTF-8 modificado, con dos bytes de largo delante.
     *
     * @throws UTFDataFormatException si la codificacion pasa de 65535 bytes -- el largo va en dos
     *     bytes y no hay donde poner mas
     */
    public final void writeUTF(String s) throws IOException {
        int largo = s.length();
        int utf = 0;
        int i = 0;
        while (i < largo) {
            int c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                utf = utf + 1;
            } else if (c > 0x07FF) {
                utf = utf + 3;
            } else {
                utf = utf + 2;
            }
            i = i + 1;
        }
        if (utf > 65535) {
            throw new UTFDataFormatException("encoded string too long: " + utf + " bytes");
        }
        this.writeShort(utf);
        i = 0;
        while (i < largo) {
            int c = s.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                this.write(c);
            } else if (c > 0x07FF) {
                this.write(0xE0 | ((c >> 12) & 0x0F));
                this.write(0x80 | ((c >> 6) & 0x3F));
                this.write(0x80 | (c & 0x3F));
            } else {
                this.write(0xC0 | ((c >> 6) & 0x1F));
                this.write(0x80 | (c & 0x3F));
            }
            i = i + 1;
        }
    }

    // ---- guardias ------------------------------------------------------------------------------

    private void exigirAbierto() throws IOException {
        if (this.cerrado) {
            throw new IOException("Stream Closed");
        }
    }

    private void exigirEscritura() throws IOException {
        this.exigirAbierto();
        if (!this.escribible) {
            throw new IOException("Access denied");
        }
    }
}
