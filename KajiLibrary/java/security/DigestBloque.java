package java.security;

// La parte que MD5, SHA-1 y toda la familia SHA-2 tienen en comun: bufferear la entrada en bloques
// de tamaño fijo y cerrar con el padding de Merkle-Damgard.
//
// Las cuatro funciones que implementa esta biblioteca son construcciones de Merkle-Damgard y
// comparten la misma estructura exterior: se parte el mensaje en bloques, se aplica una funcion de
// compresion a cada uno arrastrando un estado, y al final se agrega un relleno que **incluye el
// largo del mensaje**. Ese largo al final no es decorativo — sin el, dos mensajes distintos donde
// uno es el otro con ceros adelante darian el mismo hash.
//
// Lo unico que cambia entre algoritmos es la funcion de compresion, el tamaño de bloque y si el
// contador de bits va en little o big endian. Todo eso queda en las subclases.
abstract class DigestBloque extends MessageDigest implements Cloneable {

    // 64 bytes para MD5/SHA-1/SHA-256, 128 para SHA-512.
    final int tamBloque;

    // Cuantos bytes devuelve `digest()`. Puede ser menor que el estado interno: SHA-224 y SHA-384
    // calculan el estado completo y **truncan**, y eso es lo que los hace resistentes a la
    // extension de longitud que si afecta a sus hermanos completos.
    final int largoDigest;

    // El bloque que se esta llenando.
    final byte[] buffer;

    int enBuffer;

    // Bytes totales consumidos desde el ultimo reset. Se necesita entero, no modulo el bloque,
    // porque va escrito en el padding.
    long bytesTotales;

    DigestBloque(String algoritmo, int tamBloque, int largoDigest) {
        super(algoritmo);
        this.tamBloque = tamBloque;
        this.largoDigest = largoDigest;
        this.buffer = new byte[tamBloque];
    }

    // Consume un bloque completo de `b` desde `ofs`.
    abstract void comprimir(byte[] b, int ofs);

    // Vuelca el estado interno en `out`, truncando a `largoDigest`.
    abstract void escribirEstado(byte[] out);

    // Deja el estado en el vector inicial del algoritmo.
    abstract void reiniciarEstado();

    // Una instancia nueva del mismo algoritmo, para clonar.
    abstract DigestBloque nuevoIgual();

    // Copia el estado de compresion de `otro`. El buffer y los contadores los copia `clone`.
    abstract void copiarEstadoDe(DigestBloque otro);

    @Override
    protected final int engineGetDigestLength() {
        return this.largoDigest;
    }

    @Override
    protected final void engineReset() {
        this.enBuffer = 0;
        this.bytesTotales = 0L;
        int i = 0;
        while (i < this.buffer.length) {
            this.buffer[i] = 0;
            i = i + 1;
        }
        this.reiniciarEstado();
    }

    @Override
    protected final void engineUpdate(byte input) {
        this.buffer[this.enBuffer] = input;
        this.enBuffer = this.enBuffer + 1;
        this.bytesTotales = this.bytesTotales + 1L;
        if (this.enBuffer == this.tamBloque) {
            this.comprimir(this.buffer, 0);
            this.enBuffer = 0;
        }
    }

    // Los bloques enteros que ya estan en `input` se comprimen **en el lugar**, sin copiarlos al
    // buffer. Con entradas grandes esa copia seria el costo dominante.
    @Override
    protected final void engineUpdate(byte[] input, int offset, int len) {
        if (len == 0) {
            return;
        }
        if (offset < 0 || len < 0 || offset > input.length - len) {
            throw new ArrayIndexOutOfBoundsException();
        }
        this.bytesTotales = this.bytesTotales + (long) len;
        int ofs = offset;
        int n = len;
        if (this.enBuffer > 0) {
            int falta = this.tamBloque - this.enBuffer;
            int cuanto = n < falta ? n : falta;
            System.arraycopy(input, ofs, this.buffer, this.enBuffer, cuanto);
            this.enBuffer = this.enBuffer + cuanto;
            ofs = ofs + cuanto;
            n = n - cuanto;
            if (this.enBuffer == this.tamBloque) {
                this.comprimir(this.buffer, 0);
                this.enBuffer = 0;
            }
        }
        while (n >= this.tamBloque) {
            this.comprimir(input, ofs);
            ofs = ofs + this.tamBloque;
            n = n - this.tamBloque;
        }
        if (n > 0) {
            System.arraycopy(input, ofs, this.buffer, 0, n);
            this.enBuffer = n;
        }
    }

    @Override
    protected final byte[] engineDigest() {
        this.padear(this.bigEndian(), this.bytesDeLargo());
        byte[] out = new byte[this.largoDigest];
        this.escribirEstado(out);
        this.engineReset();
        return out;
    }

    // Si el contador de bits final va en big endian. MD5 es el unico que dice que no.
    abstract boolean bigEndian();

    // Cuantos bytes ocupa el contador al final del ultimo bloque: 8 para bloques de 64, 16 para
    // los de 128.
    abstract int bytesDeLargo();

    // El relleno: un bit en 1, ceros, y el largo del mensaje en bits.
    //
    // El caso interesante es cuando el 0x80 y los ceros no entran junto con el contador en el
    // bloque que se estaba llenando: entonces se cierra ese bloque con ceros, se comprime, y el
    // contador va solo en uno mas. Sin eso, un mensaje de largo justo perderia su propio largo.
    private void padear(boolean big, int lenBytes) {
        long bits = this.bytesTotales << 3;
        this.buffer[this.enBuffer] = (byte) 0x80;
        this.enBuffer = this.enBuffer + 1;
        if (this.enBuffer > this.tamBloque - lenBytes) {
            while (this.enBuffer < this.tamBloque) {
                this.buffer[this.enBuffer] = 0;
                this.enBuffer = this.enBuffer + 1;
            }
            this.comprimir(this.buffer, 0);
            this.enBuffer = 0;
        }
        while (this.enBuffer < this.tamBloque - lenBytes) {
            this.buffer[this.enBuffer] = 0;
            this.enBuffer = this.enBuffer + 1;
        }
        if (big) {
            // Los bytes altos del contador —los 8 primeros en SHA-512— quedan en cero: esta
            // implementacion no puede recibir 2^61 bytes.
            int i = this.enBuffer;
            while (i < this.tamBloque - 8) {
                this.buffer[i] = 0;
                i = i + 1;
            }
            int base = this.tamBloque - 8;
            int j = 0;
            while (j < 8) {
                this.buffer[base + j] = (byte) (bits >>> (56 - 8 * j));
                j = j + 1;
            }
        } else {
            int j = 0;
            while (j < 8) {
                this.buffer[this.enBuffer + j] = (byte) (bits >>> (8 * j));
                j = j + 1;
            }
        }
        this.comprimir(this.buffer, 0);
        this.enBuffer = 0;
    }

    // Clonar un digest a medio camino es lo que permite hashear un prefijo comun una sola vez y
    // seguir por dos ramas distintas. Se copia todo a mano —no `Object.clone`— para que quede a la
    // vista que el buffer del clon es propio: compartirlo haria que las dos ramas se pisaran.
    @Override
    public Object clone() throws CloneNotSupportedException {
        DigestBloque c = this.nuevoIgual();
        System.arraycopy(this.buffer, 0, c.buffer, 0, this.tamBloque);
        c.enBuffer = this.enBuffer;
        c.bytesTotales = this.bytesTotales;
        c.copiarEstadoDe(this);
        c.setProvider(this.getProvider());
        return c;
    }

    // Los cuatro algoritmos leen y escriben palabras big endian salvo MD5. Estos helpers estan
    // aca para que las subclases no repitan el corrimiento de bytes.

    static int leerIntBE(byte[] b, int i) {
        return ((b[i] & 0xff) << 24) | ((b[i + 1] & 0xff) << 16)
             | ((b[i + 2] & 0xff) << 8) | (b[i + 3] & 0xff);
    }

    static int leerIntLE(byte[] b, int i) {
        return (b[i] & 0xff) | ((b[i + 1] & 0xff) << 8)
             | ((b[i + 2] & 0xff) << 16) | ((b[i + 3] & 0xff) << 24);
    }

    static long leerLongBE(byte[] b, int i) {
        long v = 0L;
        int j = 0;
        while (j < 8) {
            v = (v << 8) | (long) (b[i + j] & 0xff);
            j = j + 1;
        }
        return v;
    }

    static void escribirIntBE(byte[] out, int i, int v) {
        out[i] = (byte) (v >>> 24);
        out[i + 1] = (byte) (v >>> 16);
        out[i + 2] = (byte) (v >>> 8);
        out[i + 3] = (byte) v;
    }

    static void escribirIntLE(byte[] out, int i, int v) {
        out[i] = (byte) v;
        out[i + 1] = (byte) (v >>> 8);
        out[i + 2] = (byte) (v >>> 16);
        out[i + 3] = (byte) (v >>> 24);
    }

    static int rotIzq(int x, int n) {
        return (x << n) | (x >>> (32 - n));
    }

    static int rotDer(int x, int n) {
        return (x >>> n) | (x << (32 - n));
    }

    static long rotDer(long x, int n) {
        return (x >>> n) | (x << (64 - n));
    }
}
