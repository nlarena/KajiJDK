package java.io;

// KajiLibrary's java.io.StringBufferInputStream -- los caracteres de un `String`, servidos como
// bytes.
//
// **Deprecada desde JDK 1.1, y hay que entender por que para no reproducir el error al usarla.**
// Convierte cada `char` a byte quedandose con los 8 bits de abajo, o sea que **no codifica: trunca**.
// Para texto ASCII coincide con UTF-8 por casualidad; para cualquier otra cosa --una enie, un
// acento, un ideograma-- produce un byte que no representa ese caracter en ninguna codificacion.
// El reemplazo correcto es `StringReader` si se quiere leer caracteres, o
// `new ByteArrayInputStream(s.getBytes(cs))` si de verdad se quieren bytes.
//
// El truncamiento **se reproduce a proposito**. Es el comportamiento especificado de esta clase, y
// un programa que la use estara compensandolo de alguna manera; "arreglarlo" aca codificando en
// UTF-8 le cambiaria los bytes abajo de los pies a ese programa, que es exactamente la clase de
// sorpresa que esta biblioteca no quiere dar. Una clase deprecada se implementa como esta dicha,
// no como estaria bien.
//
// Los tres campos son `protected` y parte del contrato publico: una subclase puede mirarlos y
// hasta moverlos, asi que no se pueden volver privados ni cambiar de significado.
public class StringBufferInputStream extends InputStream {

    /** El texto del que se leen los bytes. */
    protected String buffer;

    /** El proximo caracter a leer. */
    protected int pos;

    /** Cuantos caracteres tiene `buffer`. */
    protected int count;

    public StringBufferInputStream(String s) {
        this.buffer = s;
        this.count = s.length();
        this.pos = 0;
    }

    // `& 0xFF` sobre el char: aca esta el truncamiento del que habla la nota de arriba.
    public synchronized int read() {
        if (this.pos >= this.count) {
            return -1;
        }
        int b = this.buffer.charAt(this.pos) & 0xFF;
        this.pos = this.pos + 1;
        return b;
    }

    public synchronized int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (this.pos >= this.count) {
            return -1;
        }
        int disponibles = this.count - this.pos;
        int n = len;
        if (n > disponibles) {
            n = disponibles;
        }
        if (n <= 0) {
            return 0;
        }
        for (int i = 0; i < n; i++) {
            b[off + i] = (byte) this.buffer.charAt(this.pos + i);
        }
        this.pos = this.pos + n;
        return n;
    }

    public synchronized long skip(long n) {
        long k = n;
        if (k < 0) {
            return 0;
        }
        long restantes = this.count - this.pos;
        if (k > restantes) {
            k = restantes;
        }
        this.pos = this.pos + (int) k;
        return k;
    }

    public synchronized int available() {
        return this.count - this.pos;
    }

    // Vuelve al principio del texto, no a una marca: esta clase no tiene marcas.
    public synchronized void reset() {
        this.pos = 0;
    }
}
