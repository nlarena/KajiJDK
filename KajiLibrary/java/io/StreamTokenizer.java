package java.io;

import java.util.Arrays;

// KajiLibrary's java.io.StreamTokenizer -- parte un flujo de caracteres en tokens.
//
// Es un lexer configurable y **autocontenido**: no depende de nada del sistema, solo lee caracteres
// y decide donde termina cada uno. Por eso se puede escribir entero y sin concesiones.
//
// La configuracion vive en una tabla de 256 entradas, una por caracter, con las banderas de que es
// ese caracter: espacio, digito, letra, comilla, comentario. `wordChars`, `whitespaceChars`,
// `quoteChar` y compania no hacen otra cosa que prender bits ahi. Que la tabla sea de 256 y no de
// 65536 es del contrato original --es de 1995-- y tiene una consecuencia que conviene saber:
// **todo caracter por encima de 255 se trata como letra**, sin excepcion. Un ideograma es parte de
// una palabra y no se puede configurar para que no lo sea.
//
// El resultado de cada `nextToken()` no vuelve como valor de retorno sino repartido en tres campos
// publicos --`ttype` dice que salio, `sval` el texto si fue palabra o cadena, `nval` el numero si
// fue numero--. Es una interfaz de otra epoca y hay que respetarla: son campos publicos, cualquiera
// puede leerlos y escribirlos.
public class StreamTokenizer {

    // De donde se lee. Uno de los dos es null; ver `read()`.
    private Reader reader = null;
    private InputStream input = null;

    private char[] buf = new char[20];

    /**
     * El proximo caracter, ya leido pero todavia no consumido.
     *
     * <p>Vale `NEED_CHAR` cuando no hay ninguno guardado y hay que ir a buscarlo, y `SKIP_LF`
     * cuando lo que hay que hacer es leer uno y descartarlo si resulta ser un `\n` -- que es como
     * se trata la segunda mitad de un `\r\n` sin contar la linea dos veces.
     */
    private int peekc = NEED_CHAR;

    private static final int NEED_CHAR = Integer.MAX_VALUE;
    private static final int SKIP_LF = Integer.MAX_VALUE - 1;

    private boolean pushedBack;
    private boolean forceLower;

    /** La linea actual. Arranca en 1, no en 0: es para mensajes de error humanos. */
    private int LINENO = 1;

    private boolean eolIsSignificantP = false;
    private boolean slashSlashCommentsP = false;
    private boolean slashStarCommentsP = false;

    private byte[] ctype = new byte[256];

    private static final byte CT_WHITESPACE = 1;
    private static final byte CT_DIGIT = 2;
    private static final byte CT_ALPHA = 4;
    private static final byte CT_QUOTE = 8;
    private static final byte CT_COMMENT = 16;

    /**
     * Que fue el ultimo token: uno de los `TT_*`, o el codigo del caracter si fue un caracter
     * suelto (un `+` sale como `43`).
     */
    public int ttype = TT_NOTHING;

    /** Todavia no se leyo ningun token. */
    private static final int TT_NOTHING = -4;

    /** Se acabo el stream. */
    public static final int TT_EOF = -1;

    /** Fin de linea, solo si se pidio `eolIsSignificant(true)`. */
    public static final int TT_EOL = '\n';

    /** El token fue un numero; esta en `nval`. */
    public static final int TT_NUMBER = -2;

    /** El token fue una palabra; esta en `sval`. */
    public static final int TT_WORD = -3;

    /** El texto del ultimo token, si fue palabra o cadena entrecomillada. */
    public String sval;

    /** El valor del ultimo token, si fue numero. */
    public double nval;

    // La sintaxis por omision: letras ASCII y el rango alto son palabra, todo lo que esta por
    // debajo del espacio es blanco, `/` abre comentario, y las dos comillas delimitan cadenas.
    private StreamTokenizer() {
        this.wordChars('a', 'z');
        this.wordChars('A', 'Z');
        this.wordChars(128 + 32, 255);
        this.whitespaceChars(0, ' ');
        this.commentChar('/');
        this.quoteChar('"');
        this.quoteChar('\'');
        this.parseNumbers();
    }

    /**
     * Lee de un stream de bytes.
     *
     * @deprecated Trata cada byte como un caracter, o sea que solo anda con codificaciones de un
     *     byte. Lo correcto es envolverlo: `new StreamTokenizer(new InputStreamReader(is, cs))`.
     */
    @Deprecated
    public StreamTokenizer(InputStream is) {
        this();
        if (is == null) {
            throw new NullPointerException();
        }
        this.input = is;
    }

    /** Lee de un stream de caracteres. Este es el que hay que usar. */
    public StreamTokenizer(Reader r) {
        this();
        if (r == null) {
            throw new NullPointerException();
        }
        this.reader = r;
    }

    // La unica lectura de la clase. Los dos campos son excluyentes y uno de los dos esta puesto
    // siempre: los constructores no dejan construir un tokenizer sin fuente.
    private int read() throws IOException {
        if (this.reader != null) {
            return this.reader.read();
        }
        if (this.input != null) {
            return this.input.read();
        }
        throw new IllegalStateException();
    }

    /** Deja la tabla en blanco: **ningun** caracter tiene significado especial. */
    public void resetSyntax() {
        for (int i = this.ctype.length; --i >= 0; ) {
            this.ctype[i] = 0;
        }
    }

    /** Los caracteres de `low` a `hi` son parte de una palabra. */
    public void wordChars(int low, int hi) {
        int l = low;
        int h = hi;
        if (l < 0) {
            l = 0;
        }
        if (h >= this.ctype.length) {
            h = this.ctype.length - 1;
        }
        while (l <= h) {
            this.ctype[l] = (byte) (this.ctype[l] | CT_ALPHA);
            l = l + 1;
        }
    }

    /** Los caracteres de `low` a `hi` separan tokens y no forman parte de ninguno. */
    public void whitespaceChars(int low, int hi) {
        int l = low;
        int h = hi;
        if (l < 0) {
            l = 0;
        }
        if (h >= this.ctype.length) {
            h = this.ctype.length - 1;
        }
        while (l <= h) {
            this.ctype[l] = CT_WHITESPACE;
            l = l + 1;
        }
    }

    /** Los caracteres de `low` a `hi` no tienen ningun significado especial: salen solos. */
    public void ordinaryChars(int low, int hi) {
        int l = low;
        int h = hi;
        if (l < 0) {
            l = 0;
        }
        if (h >= this.ctype.length) {
            h = this.ctype.length - 1;
        }
        while (l <= h) {
            this.ctype[l] = 0;
            l = l + 1;
        }
    }

    /** El caracter `ch` no tiene significado especial: sale solo. */
    public void ordinaryChar(int ch) {
        if (ch >= 0 && ch < this.ctype.length) {
            this.ctype[ch] = 0;
        }
    }

    /** Desde `ch` hasta el fin de linea es comentario. */
    public void commentChar(int ch) {
        if (ch >= 0 && ch < this.ctype.length) {
            this.ctype[ch] = CT_COMMENT;
        }
    }

    /**
     * `ch` abre y cierra una cadena.
     *
     * <p>Dentro de la cadena se interpretan los escapes de C --`\n`, `\t`, `\\`, y los octales
     * `\0` a `\377`--, y el token sale con `ttype` igual a la comilla y el texto ya desescapado en
     * `sval`.
     */
    public void quoteChar(int ch) {
        if (ch >= 0 && ch < this.ctype.length) {
            this.ctype[ch] = CT_QUOTE;
        }
    }

    /**
     * Los digitos, el punto y el menos forman numeros.
     *
     * <p>El numero se arma en un `double` y **no hay enteros**: `1` sale como `1.0`. Tampoco hay
     * notacion exponencial -- `1e5` se parte en el numero `1.0` y la palabra `e5`.
     */
    public void parseNumbers() {
        for (int i = '0'; i <= '9'; i++) {
            this.ctype[i] = (byte) (this.ctype[i] | CT_DIGIT);
        }
        this.ctype['.'] = (byte) (this.ctype['.'] | CT_DIGIT);
        this.ctype['-'] = (byte) (this.ctype['-'] | CT_DIGIT);
    }

    /** Si los fines de linea salen como token `TT_EOL` en vez de contar como blanco. */
    public void eolIsSignificant(boolean flag) {
        this.eolIsSignificantP = flag;
    }

    /** Si `/* ... *&#47;` es comentario. */
    public void slashStarComments(boolean flag) {
        this.slashStarCommentsP = flag;
    }

    /** Si `//` abre comentario hasta el fin de linea. */
    public void slashSlashComments(boolean flag) {
        this.slashSlashCommentsP = flag;
    }

    /** Si las palabras se pasan a minusculas antes de dejarlas en `sval`. */
    public void lowerCaseMode(boolean fl) {
        this.forceLower = fl;
    }

    /**
     * Lee el proximo token y lo deja en `ttype`, `sval` y `nval`.
     *
     * @return el mismo valor que queda en `ttype`
     */
    public int nextToken() throws IOException {
        if (this.pushedBack) {
            this.pushedBack = false;
            return this.ttype;
        }
        byte[] ct = this.ctype;
        this.sval = null;

        int c = this.peekc;
        if (c < 0) {
            c = NEED_CHAR;
        }
        if (c == SKIP_LF) {
            c = this.read();
            if (c < 0) {
                this.ttype = TT_EOF;
                return this.ttype;
            }
            if (c == '\n') {
                c = NEED_CHAR;
            }
        }
        if (c == NEED_CHAR) {
            c = this.read();
            if (c < 0) {
                this.ttype = TT_EOF;
                return this.ttype;
            }
        }
        // Se guarda ya mismo por las dudas: si algo mas abajo devuelve sin tocar `peekc`, la
        // proxima llamada tiene que ir a buscar un caracter nuevo y no repetir este.
        this.ttype = c;
        this.peekc = NEED_CHAR;

        int tipo;
        if (c < 256) {
            tipo = ct[c];
        } else {
            tipo = CT_ALPHA;
        }

        // ---- blancos ----
        while ((tipo & CT_WHITESPACE) != 0) {
            if (c == '\r') {
                this.LINENO = this.LINENO + 1;
                if (this.eolIsSignificantP) {
                    this.peekc = SKIP_LF;
                    this.ttype = TT_EOL;
                    return this.ttype;
                }
                c = this.read();
                if (c == '\n') {
                    c = this.read();
                }
            } else {
                if (c == '\n') {
                    this.LINENO = this.LINENO + 1;
                    if (this.eolIsSignificantP) {
                        this.ttype = TT_EOL;
                        return this.ttype;
                    }
                }
                c = this.read();
            }
            if (c < 0) {
                this.ttype = TT_EOF;
                return this.ttype;
            }
            if (c < 256) {
                tipo = ct[c];
            } else {
                tipo = CT_ALPHA;
            }
        }

        // ---- numeros ----
        if ((tipo & CT_DIGIT) != 0) {
            boolean neg = false;
            if (c == '-') {
                c = this.read();
                // Un `-` que no arranca un numero es un `-` y nada mas. Sin esta vuelta atras,
                // `a - b` se leeria como `a` y el numero `-b`.
                if (c != '.' && (c < '0' || c > '9')) {
                    this.peekc = c;
                    this.ttype = '-';
                    return this.ttype;
                }
                neg = true;
            }
            double v = 0;
            int decexp = 0;
            int seendot = 0;
            while (true) {
                if (c == '.' && seendot == 0) {
                    seendot = 1;
                } else if ('0' <= c && c <= '9') {
                    v = v * 10 + (c - '0');
                    decexp = decexp + seendot;
                } else {
                    break;
                }
                c = this.read();
            }
            this.peekc = c;
            if (decexp != 0) {
                // Una sola division al final en vez de dividir digito a digito: acumular el entero
                // y escalarlo una vez pierde menos precision que ir sumando fracciones.
                double denom = 10;
                decexp = decexp - 1;
                while (decexp > 0) {
                    denom = denom * 10;
                    decexp = decexp - 1;
                }
                v = v / denom;
            }
            if (neg) {
                this.nval = -v;
            } else {
                this.nval = v;
            }
            this.ttype = TT_NUMBER;
            return this.ttype;
        }

        // ---- palabras ----
        if ((tipo & CT_ALPHA) != 0) {
            int i = 0;
            while (true) {
                if (i >= this.buf.length) {
                    this.buf = Arrays.copyOf(this.buf, this.buf.length * 2);
                }
                this.buf[i] = (char) c;
                i = i + 1;
                c = this.read();
                if (c < 0) {
                    tipo = CT_WHITESPACE;
                } else if (c < 256) {
                    tipo = ct[c];
                } else {
                    tipo = CT_ALPHA;
                }
                // Los digitos continuan una palabra aunque no la empiecen: `a1` es un token.
                if ((tipo & (CT_ALPHA | CT_DIGIT)) == 0) {
                    break;
                }
            }
            this.peekc = c;
            this.sval = String.copyValueOf(this.buf, 0, i);
            if (this.forceLower) {
                this.sval = this.sval.toLowerCase();
            }
            this.ttype = TT_WORD;
            return this.ttype;
        }

        // ---- cadenas entrecomilladas ----
        if ((tipo & CT_QUOTE) != 0) {
            this.ttype = c;
            int i = 0;
            // Hace falta un caracter de adelanto permanente (`d`) porque un escape octal se come
            // hasta tres digitos y hay que poder devolver el que sobro.
            int d = this.read();
            while (d >= 0 && d != this.ttype && d != '\n' && d != '\r') {
                if (d == '\\') {
                    c = this.read();
                    int first = c;
                    if (c >= '0' && c <= '7') {
                        c = c - '0';
                        int c2 = this.read();
                        if ('0' <= c2 && c2 <= '7') {
                            c = (c << 3) + (c2 - '0');
                            c2 = this.read();
                            // `first <= '3'` es lo que impide que `\477` se lea como un octal de
                            // tres digitos: no entra en un byte.
                            if ('0' <= c2 && c2 <= '7' && first <= '3') {
                                c = (c << 3) + (c2 - '0');
                                d = this.read();
                            } else {
                                d = c2;
                            }
                        } else {
                            d = c2;
                        }
                    } else {
                        if (c == 'a') {
                            c = 0x7;
                        } else if (c == 'b') {
                            c = '\b';
                        } else if (c == 'f') {
                            c = 0xC;
                        } else if (c == 'n') {
                            c = '\n';
                        } else if (c == 'r') {
                            c = '\r';
                        } else if (c == 't') {
                            c = '\t';
                        } else if (c == 'v') {
                            c = 0xB;
                        }
                        d = this.read();
                    }
                } else {
                    c = d;
                    d = this.read();
                }
                if (i >= this.buf.length) {
                    this.buf = Arrays.copyOf(this.buf, this.buf.length * 2);
                }
                this.buf[i] = (char) c;
                i = i + 1;
            }

            // Si se corto por la comilla de cierre, esa comilla se consume; si se corto por un fin
            // de linea o por el fin del stream, ese caracter se devuelve para el proximo token.
            if (d == this.ttype) {
                this.peekc = NEED_CHAR;
            } else {
                this.peekc = d;
            }
            this.sval = String.copyValueOf(this.buf, 0, i);
            return this.ttype;
        }

        // ---- comentarios que empiezan con `/` ----
        if (c == '/' && (this.slashSlashCommentsP || this.slashStarCommentsP)) {
            c = this.read();
            if (c == '*' && this.slashStarCommentsP) {
                int prevc = 0;
                while (true) {
                    c = this.read();
                    if (c == '/' && prevc == '*') {
                        break;
                    }
                    if (c == '\r') {
                        this.LINENO = this.LINENO + 1;
                        c = this.read();
                        if (c == '\n') {
                            c = this.read();
                        }
                    } else if (c == '\n') {
                        this.LINENO = this.LINENO + 1;
                        c = this.read();
                    }
                    if (c < 0) {
                        this.ttype = TT_EOF;
                        return this.ttype;
                    }
                    prevc = c;
                }
                return this.nextToken();
            } else if (c == '/' && this.slashSlashCommentsP) {
                while (true) {
                    c = this.read();
                    if (c == '\n' || c == '\r' || c < 0) {
                        break;
                    }
                }
                this.peekc = c;
                return this.nextToken();
            } else {
                // No era ni `//` ni `/*`. Si ademas `/` esta declarado como caracter de comentario
                // por su cuenta, sigue abriendo un comentario de linea; si no, es un `/` suelto.
                if ((ct['/'] & CT_COMMENT) != 0) {
                    while (true) {
                        c = this.read();
                        if (c == '\n' || c == '\r' || c < 0) {
                            break;
                        }
                    }
                    this.peekc = c;
                    return this.nextToken();
                } else {
                    this.peekc = c;
                    this.ttype = '/';
                    return this.ttype;
                }
            }
        }

        // ---- comentarios de un caracter cualquiera ----
        if ((tipo & CT_COMMENT) != 0) {
            while (true) {
                c = this.read();
                if (c == '\n' || c == '\r' || c < 0) {
                    break;
                }
            }
            this.peekc = c;
            return this.nextToken();
        }

        // ---- cualquier otro caracter sale solo ----
        this.ttype = c;
        return this.ttype;
    }

    /**
     * Hace que el proximo `nextToken()` devuelva otra vez el token actual sin leer nada.
     *
     * <p>Es un adelanto de uno y no una pila: llamarlo dos veces seguidas no retrocede dos tokens.
     */
    public void pushBack() {
        if (this.ttype != TT_NOTHING) {
            this.pushedBack = true;
        }
    }

    /** La linea del ultimo token. La primera es la 1. */
    public int lineno() {
        return this.LINENO;
    }

    @Override
    public String toString() {
        String ret;
        if (this.ttype == TT_EOF) {
            ret = "EOF";
        } else if (this.ttype == TT_EOL) {
            ret = "EOL";
        } else if (this.ttype == TT_WORD) {
            ret = this.sval;
        } else if (this.ttype == TT_NUMBER) {
            ret = "n=" + this.nval;
        } else if (this.ttype == TT_NOTHING) {
            ret = "NOTHING";
        } else if (this.ttype < 256 && (this.ctype[this.ttype] & CT_QUOTE) != 0) {
            ret = this.sval;
        } else {
            char[] s = new char[3];
            s[0] = '\'';
            s[2] = '\'';
            s[1] = (char) this.ttype;
            ret = new String(s);
        }
        return "Token[" + ret + "], line " + this.LINENO;
    }
}
