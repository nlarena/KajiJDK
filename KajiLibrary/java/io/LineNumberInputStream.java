package java.io;

// KajiLibrary's java.io.LineNumberInputStream -- cuenta lineas sobre un stream de bytes.
//
// **Deprecada desde JDK 1.1**, y por la misma razon que `StringBufferInputStream`: cuenta lineas
// sobre *bytes*, no sobre caracteres, asi que solo funciona con codificaciones en las que un byte
// es un caracter. En UTF-16 los ceros intercalados le arruinan la cuenta. El reemplazo es
// `LineNumberReader`, que hace lo mismo un nivel mas arriba, donde ya hay caracteres.
//
// Lo que si hace bien es **normalizar los tres finales de linea**: `\r`, `\n` y `\r\n` salen todos
// como un unico `\n`. Eso obliga a mirar un byte adelante cuando se ve un `\r` --hay que saber si
// lo que sigue es un `\n` para no contar dos lineas donde hay una-- y ese byte adelantado se guarda
// en `pushBack`. Es todo el truco de la clase.
//
// Los campos son de paquete, como en el JDK: no son contrato, pero `mark`/`reset` tienen que poder
// guardarlos y restaurarlos juntos.
public class LineNumberInputStream extends FilterInputStream {

    // El byte que se leyo de mas mirando adelante despues de un `\r`, o -1 si no hay ninguno.
    int pushBack = -1;

    int lineNumber;

    int markLineNumber = 0;

    int markPushBack = -1;

    public LineNumberInputStream(InputStream in) {
        super(in);
    }

    // Un `\r` suelto y un `\r\n` valen los dos por una linea y salen los dos como `\n`. Para
    // distinguirlos hay que leer el siguiente byte: si es `\n` se descarta --ya se conto la linea--
    // y si no, se guarda para la proxima lectura.
    public int read() throws IOException {
        int c = this.pushBack;
        if (c != -1) {
            this.pushBack = -1;
        } else {
            c = this.in.read();
        }
        if (c == '\r') {
            this.pushBack = this.in.read();
            if (this.pushBack == '\n') {
                this.pushBack = -1;
            }
            this.lineNumber = this.lineNumber + 1;
            return '\n';
        }
        if (c == '\n') {
            this.lineNumber = this.lineNumber + 1;
            return '\n';
        }
        return c;
    }

    // Byte a byte y no en bloque, porque la traduccion de finales de linea puede consumir dos bytes
    // de abajo por cada uno que sale. Es lento y es lo que hace el JDK; la clase esta deprecada.
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        int c = this.read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte) c;
        int i = 1;
        while (i < len) {
            c = this.read();
            if (c == -1) {
                break;
            }
            b[off + i] = (byte) c;
            i = i + 1;
        }
        return i;
    }

    // Saltar tambien pasa por `read`: saltar bytes crudos contaria mal las lineas.
    public long skip(long n) throws IOException {
        long saltados = 0;
        while (saltados < n) {
            if (this.read() == -1) {
                break;
            }
            saltados = saltados + 1;
        }
        return saltados;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }

    // **La mitad**, y no es un error de calculo: en el peor caso todo lo que viene son `\r\n`, y
    // cada par de bytes de abajo se convierte en un solo byte de aca. `available` promete un piso,
    // no una estimacion, asi que tiene que suponer el peor caso. El `+1` es el byte adelantado, que
    // ya esta leido y sale seguro.
    public int available() throws IOException {
        int abajo = this.in.available() / 2;
        if (this.pushBack == -1) {
            return abajo;
        }
        return abajo + 1;
    }

    // El numero de linea y el byte adelantado se guardan junto con la marca de abajo: restaurar la
    // posicion sin restaurar los dos dejaria la cuenta corrida.
    public void mark(int readlimit) {
        this.markLineNumber = this.lineNumber;
        this.markPushBack = this.pushBack;
        this.in.mark(readlimit);
    }

    public void reset() throws IOException {
        this.lineNumber = this.markLineNumber;
        this.pushBack = this.markPushBack;
        this.in.reset();
    }
}
