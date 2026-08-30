package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// Un lector de texto que parte la entrada en piezas con sentido: palabras, numeros, lineas.
//
// La idea entera cabe en una linea: **la entrada se parte por un patron de delimitadores**, y
// todo lo demas -- `next`, `nextInt`, `hasNextDouble` -- son variaciones sobre eso. El delimitador
// por defecto es "uno o mas espacios en blanco", y se puede cambiar por cualquier expresion
// regular con `useDelimiter`. Por eso Scanner es a la vez el atajo para leer un entero de la
// consola y un partidor de CSV.
//
// Los pares `hasNextX`/`nextX` van juntos y no por casualidad: `hasNextX` mira el proximo token
// **sin consumirlo**, `nextX` lo consume. Esa asimetria es la que hace que un bucle
// `while (sc.hasNextInt()) suma += sc.nextInt();` termine solo cuando aparece algo que no es un
// numero -- sin perderlo.
//
// La trampa clasica queda dicha porque no es un detalle de implementacion sino del diseno:
// `nextInt()` consume el numero y **deja el fin de linea**, asi que el `nextLine()` que sigue
// devuelve el resto vacio de esa linea y no la siguiente. No es un bug, es lo que significa "token".
//
// ---- lo que esta y lo que no --------------------------------------------------------------------
//
// **64 de los 73 miembros del contrato.** Los nueve que faltan son constructores, y faltan por
// tipos que la biblioteca todavia no tiene:
//
//   Scanner(File), (File, String), (File, Charset)                     java.io.File
//   Scanner(Path), (Path, String), (Path, Charset)                     java.nio.file.Path
//   Scanner(ReadableByteChannel) y sus dos variantes                   java.nio.channels
//
// Ninguno de los tres es un problema de Scanner: en cuanto exista `java.io.File` los tres primeros
// son tres lineas.
//
// ---- divergencias deliberadas -------------------------------------------------------------------
//
// | | |
// |---|---|
// | una fuente `InputStream` se lee **entera** al construir | decodificar por trozos parte los caracteres multibyte que caen en el borde, y no hay `InputStreamReader` para hacerlo bien. Consecuencia: un Scanner sobre `System.in` espera al fin de la entrada en vez de leer linea a linea |
// | el buffer no se compacta | lo consumido se conserva, asi que la memoria crece con la entrada. `match()` y `findInLine` se apoyan en eso, y para entradas de tamano razonable no molesta |
// | separador de miles `,` y decimal `.`, fijos | nuestro `Locale` no lleva simbolos numericos, asi que `useLocale` se guarda y no cambia el analisis |
// | `tokens()` y `findAll()` son **ansiosos** | juntan todo y devuelven un Stream sobre el resultado, en vez de producir a demanda |
public final class Scanner implements Iterator<String>, Closeable {

    // "uno o mas espacios en blanco". El JDK usa `\p{javaWhitespace}+`, que es casi lo mismo con
    // mas puntos de codigo.
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    // Los seis finales de linea que reconoce el JDK, con `\r\n` primero para que no se parta en dos.
    private static final Pattern FIN_DE_LINEA =
            Pattern.compile("\r\n|[\n\r  ]");

    // La fuente de la que todavia queda por leer, o null si ya se agoto.
    private Readable source;

    // Todo lo leido hasta ahora. `position` es lo consumido; lo de atras se conserva porque
    // `match()` y `findInLine` miran hacia atras.
    private final StringBuilder buf = new StringBuilder();
    private int position;

    private boolean sourceClosed;
    private boolean closed;

    private Pattern delimiter = ESPACIOS;
    private Locale locale = Locale.getDefault();
    private int radix = 10;

    private MatchResult lastResult;
    private IOException lastException;

    // Los limites del token que localizo `ubicarToken`.
    private int tokenStart;
    private int tokenEnd;

    // ---- construccion ------------------------------------------------------------------------------

    public Scanner(Readable source) {
        if (source == null) {
            throw new NullPointerException();
        }
        this.source = source;
    }

    // La fuente mas simple: el texto ya esta entero.
    public Scanner(String source) {
        if (source == null) {
            throw new NullPointerException();
        }
        this.buf.append(source);
        this.sourceClosed = true;
    }

    /**
     * Un `Scanner` sobre el contenido de un archivo.
     *
     * <p>Se apoya en `FileInputStream`, que lee el archivo entero al abrirse. Vale la nota de aquel:
     * lo que se recorre es la **foto** del momento de construir el `Scanner`, no un archivo que se
     * sigue leyendo -- un cambio posterior no se ve.
     *
     * @throws java.io.FileNotFoundException si no existe, es un directorio, o no se puede leer
     */
    public Scanner(java.io.File source) throws java.io.FileNotFoundException {
        this(source, Charset.defaultCharset());
    }

    public Scanner(java.io.File source, String charsetName) throws java.io.FileNotFoundException {
        this(source, Charset.forName(charsetName));
    }

    public Scanner(java.io.File source, Charset charset) throws java.io.FileNotFoundException {
        this(new java.io.FileInputStream(source), charset);
    }

    /**
     * Idem, por `Path`.
     *
     * <p>Un `Path` de esta biblioteca es una ruta y nada mas, asi que esto es exactamente la forma
     * con `File` pasando por `toString()`. Existen las dos porque el JDK tiene las dos, y porque
     * quien ya tiene un `Path` no deberia tener que convertirlo a mano.
     */
    public Scanner(java.nio.file.Path source) throws java.io.FileNotFoundException {
        this(source, Charset.defaultCharset());
    }

    public Scanner(java.nio.file.Path source, String charsetName)
            throws java.io.FileNotFoundException {
        this(source, Charset.forName(charsetName));
    }

    public Scanner(java.nio.file.Path source, Charset charset)
            throws java.io.FileNotFoundException {
        this(source == null ? null : new java.io.File(source.toString()), charset);
    }

    public Scanner(java.nio.channels.ReadableByteChannel source) {
        this(source, Charset.defaultCharset());
    }

    public Scanner(java.nio.channels.ReadableByteChannel source, String charsetName) {
        this(source, Charset.forName(charsetName));
    }

    /**
     * Un `Scanner` sobre un canal.
     *
     * <p>Esta forma **si** se puede implementar de verdad en KajiJDK, a diferencia de las de `File`
     * y `Path`: la fuente la aporta quien llama, ya abierta, asi que no hace falta que la biblioteca
     * sepa tocar el sistema de archivos.
     *
     * <p>Como las de `InputStream`, lee el canal **entero** de una y despues decodifica. Ojo con el
     * `0`: en un canal es un resultado legitimo --el buffer estaba lleno, o uno no bloqueante no
     * tenia nada listo-- y **no** es fin de flujo, que es `-1`. Tratarlo como fin cortaria la
     * lectura antes de tiempo; por eso el bucle solo termina con el negativo.
     */
    public Scanner(java.nio.channels.ReadableByteChannel source, Charset charset) {
        if (source == null || charset == null) {
            throw new NullPointerException();
        }
        this.sourceClosed = true;
        byte[] todo = new byte[0];
        int usados = 0;
        java.nio.ByteBuffer trozo = java.nio.ByteBuffer.allocate(8192);
        try {
            int n = source.read(trozo);
            while (n >= 0) {
                if (n > 0) {
                    if (usados + n > todo.length) {
                        int nuevo = todo.length * 2;
                        if (nuevo < usados + n) {
                            nuevo = usados + n;
                        }
                        byte[] mas = new byte[nuevo];
                        System.arraycopy(todo, 0, mas, 0, usados);
                        todo = mas;
                    }
                    trozo.flip();
                    trozo.get(todo, usados, n);
                    usados = usados + n;
                    trozo.clear();
                }
                n = source.read(trozo);
            }
        } catch (java.io.IOException e) {
            // Lo leido hasta aca es lo que hay: un `Scanner` no puede propagar una excepcion
            // chequeada desde su constructor sin declararla, y el JDK tampoco la declara para esta
            // forma.
        }
        this.buf.append(new String(todo, 0, usados, charset));
    }

    public Scanner(InputStream source) {
        this(source, Charset.defaultCharset());
    }

    public Scanner(InputStream source, String charsetName) {
        this(source, Charset.forName(charsetName));
    }

    // Se lee el stream **entero** y se decodifica de una. Ver la tabla de divergencias.
    public Scanner(InputStream source, Charset charset) {
        if (source == null || charset == null) {
            throw new NullPointerException();
        }
        this.sourceClosed = true;
        byte[] todo = new byte[0];
        int usados = 0;
        byte[] trozo = new byte[8192];
        try {
            int n = source.read(trozo, 0, trozo.length);
            while (n > 0) {
                if (usados + n > todo.length) {
                    int nuevo = todo.length * 2;
                    if (nuevo < usados + n) {
                        nuevo = usados + n;
                    }
                    byte[] mas = new byte[nuevo];
                    System.arraycopy(todo, 0, mas, 0, usados);
                    todo = mas;
                }
                System.arraycopy(trozo, 0, todo, usados, n);
                usados = usados + n;
                n = source.read(trozo, 0, trozo.length);
            }
        } catch (RuntimeException e) {
            // `InputStream.read` de esta biblioteca no declara IOException; si algo falla, se corta
            // la lectura y lo leido hasta aca es lo que hay.
        }
        this.buf.append(new String(todo, 0, usados, charset));
    }

    // ---- el buffer --------------------------------------------------------------------------------

    // Trae mas caracteres de la fuente. Devuelve si trajo alguno.
    private boolean leerMas() {
        if (this.sourceClosed || this.source == null) {
            return false;
        }
        CharBuffer cb = CharBuffer.allocate(1024);
        int n;
        try {
            n = this.source.read(cb);
        } catch (IOException e) {
            this.lastException = e;
            this.sourceClosed = true;
            return false;
        }
        if (n < 0) {
            this.sourceClosed = true;
            return false;
        }
        if (n == 0) {
            // Cero no es fin de entrada: el buffer no tenia lugar. Con uno recien creado no puede
            // pasar, asi que se toma como que no hay nada mas por ahora.
            return false;
        }
        cb.flip();
        char[] leidos = new char[n];
        cb.get(leidos, 0, n);
        this.buf.append(leidos, 0, n);
        return true;
    }

    private void chequearAbierto() {
        if (this.closed) {
            throw new IllegalStateException("Scanner closed");
        }
    }

    // Ubica el proximo token sin consumirlo. Deja los limites en tokenStart/tokenEnd.
    //
    // El bucle es por la lectura incremental: cuando lo que hay en el buffer no alcanza para
    // decidir --los delimitadores llegan hasta el final, o el token no cerro-- se trae mas y se
    // vuelve a empezar.
    private boolean ubicarToken() {
        while (true) {
            int p = this.position;
            Matcher m = this.delimiter.matcher(this.buf);
            m.region(p, this.buf.length());
            if (m.lookingAt()) {
                p = m.end();
                if (p == this.buf.length() && !this.sourceClosed && this.leerMas()) {
                    continue;
                }
            }
            if (p >= this.buf.length()) {
                if (!this.sourceClosed && this.leerMas()) {
                    continue;
                }
                return false; // solo delimitadores hasta el final
            }
            Matcher d = this.delimiter.matcher(this.buf);
            d.region(p, this.buf.length());
            int fin;
            if (d.find()) {
                fin = d.start();
            } else {
                if (!this.sourceClosed && this.leerMas()) {
                    continue;
                }
                fin = this.buf.length();
            }
            this.tokenStart = p;
            this.tokenEnd = fin;
            return true;
        }
    }

    // El proximo token sin consumirlo, o null si no hay.
    private String espiar() {
        this.chequearAbierto();
        if (!this.ubicarToken()) {
            return null;
        }
        return this.buf.substring(this.tokenStart, this.tokenEnd);
    }

    // Deja `lastResult` apuntando al rango dado, para que `match()` tenga algo que devolver.
    //
    // Se arma a mano y no con un Matcher, a proposito. Lo natural seria matchear `[\s\S]*` sobre
    // la region, pero nuestro motor **rechaza** una clase predefinida negada adentro de otra
    // clase (`\S` dentro de `[...]`), y lo dice con todas las letras en
    // `Node.addClassEscape`. Un token ya localizado tampoco necesita ningun motor: sus limites
    // ya se conocen.
    private void registrarMatch(int desde, int hasta) {
        this.lastResult = new ScanMatch(this.buf.substring(desde, hasta), desde, hasta);
    }

    // ---- tokens -------------------------------------------------------------------------------------

    public boolean hasNext() {
        return this.espiar() != null;
    }

    public String next() {
        this.chequearAbierto();
        if (!this.ubicarToken()) {
            throw new NoSuchElementException();
        }
        String t = this.buf.substring(this.tokenStart, this.tokenEnd);
        this.registrarMatch(this.tokenStart, this.tokenEnd);
        this.position = this.tokenEnd;
        return t;
    }

    public boolean hasNext(String pattern) {
        return this.hasNext(Pattern.compile(pattern));
    }

    // El proximo token, pero solo si **entero** matchea el patron.
    public boolean hasNext(Pattern pattern) {
        String t = this.espiar();
        if (t == null) {
            return false;
        }
        return pattern.matcher(t).matches();
    }

    public String next(String pattern) {
        return this.next(Pattern.compile(pattern));
    }

    public String next(Pattern pattern) {
        String t = this.espiar();
        if (t == null) {
            throw new NoSuchElementException();
        }
        if (!pattern.matcher(t).matches()) {
            throw new InputMismatchException();
        }
        return this.next();
    }

    // Iterator lo declara y Scanner lo niega: un Scanner no tiene de donde sacar nada.
    public void remove() {
        throw new UnsupportedOperationException();
    }

    // ---- lineas ----------------------------------------------------------------------------------

    public boolean hasNextLine() {
        this.chequearAbierto();
        return this.buscarFinDeLinea() != null;
    }

    // Devuelve { inicioDelFin, finDelFin } del proximo salto de linea, o { -1, -1 } si la entrada
    // termina sin salto pero con contenido. null si no queda nada.
    private int[] buscarFinDeLinea() {
        while (true) {
            Matcher m = FIN_DE_LINEA.matcher(this.buf);
            m.region(this.position, this.buf.length());
            if (m.find()) {
                int[] out = new int[2];
                out[0] = m.start();
                out[1] = m.end();
                return out;
            }
            if (!this.sourceClosed && this.leerMas()) {
                continue;
            }
            if (this.position < this.buf.length()) {
                int[] out = new int[2];
                out[0] = -1;
                out[1] = -1;
                return out;
            }
            return null;
        }
    }

    // El resto de la linea actual, **sin** el salto -- que si se consume.
    //
    // Es lo que hace que `nextInt()` seguido de `nextLine()` devuelva vacio: el entero dejo el
    // salto sin consumir, y esta llamada se lo lleva.
    public String nextLine() {
        this.chequearAbierto();
        int[] fin = this.buscarFinDeLinea();
        if (fin == null) {
            throw new NoSuchElementException("No line found");
        }
        String linea;
        if (fin[0] < 0) {
            linea = this.buf.substring(this.position, this.buf.length());
            this.registrarMatch(this.position, this.buf.length());
            this.position = this.buf.length();
        } else {
            linea = this.buf.substring(this.position, fin[0]);
            this.registrarMatch(this.position, fin[1]);
            this.position = fin[1];
        }
        return linea;
    }

    // ---- numeros y booleanos -------------------------------------------------------------------------

    // Saca el separador de miles y normaliza el signo, para que los parsers de java.lang lo acepten.
    private String limpiarNumero(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c != ',') {
                sb.append(c);
            }
            i = i + 1;
        }
        return sb.toString();
    }

    // El comun de los ocho `hasNextX` numericos: se espia el token, se intenta convertir, y **no**
    // se consume nada. `tipo` elige el parser.
    private boolean puede(int tipo, int radix) {
        String t = this.espiar();
        if (t == null) {
            return false;
        }
        return this.convertible(t, tipo, radix);
    }

    private static final int T_BYTE = 0;
    private static final int T_SHORT = 1;
    private static final int T_INT = 2;
    private static final int T_LONG = 3;
    private static final int T_FLOAT = 4;
    private static final int T_DOUBLE = 5;
    private static final int T_BIGINT = 6;
    private static final int T_BIGDEC = 7;

    private boolean convertible(String t, int tipo, int radix) {
        String s = this.limpiarNumero(t);
        try {
            if (tipo == T_BYTE) {
                Byte.parseByte(s, radix);
            } else if (tipo == T_SHORT) {
                Short.parseShort(s, radix);
            } else if (tipo == T_INT) {
                Integer.parseInt(s, radix);
            } else if (tipo == T_LONG) {
                Long.parseLong(s, radix);
            } else if (tipo == T_FLOAT) {
                Float.parseFloat(s);
            } else if (tipo == T_DOUBLE) {
                Double.parseDouble(s);
            } else if (tipo == T_BIGINT) {
                new BigInteger(s, radix);
            } else {
                new BigDecimal(s);
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    // Y el comun de los `nextX`: se exige que convierta **antes** de consumir, para que un token
    // que no es numero siga estando ahi cuando se lo pida por otra via.
    private String tokenNumerico(int tipo, int radix) {
        String t = this.espiar();
        if (t == null) {
            throw new NoSuchElementException();
        }
        if (!this.convertible(t, tipo, radix)) {
            throw new InputMismatchException();
        }
        this.next();
        return this.limpiarNumero(t);
    }

    public boolean hasNextByte() {
        return this.hasNextByte(this.radix);
    }

    public boolean hasNextByte(int radix) {
        return this.puede(T_BYTE, radix);
    }

    public byte nextByte() {
        return this.nextByte(this.radix);
    }

    public byte nextByte(int radix) {
        return Byte.parseByte(this.tokenNumerico(T_BYTE, radix), radix);
    }

    public boolean hasNextShort() {
        return this.hasNextShort(this.radix);
    }

    public boolean hasNextShort(int radix) {
        return this.puede(T_SHORT, radix);
    }

    public short nextShort() {
        return this.nextShort(this.radix);
    }

    public short nextShort(int radix) {
        return Short.parseShort(this.tokenNumerico(T_SHORT, radix), radix);
    }

    public boolean hasNextInt() {
        return this.hasNextInt(this.radix);
    }

    public boolean hasNextInt(int radix) {
        return this.puede(T_INT, radix);
    }

    public int nextInt() {
        return this.nextInt(this.radix);
    }

    public int nextInt(int radix) {
        return Integer.parseInt(this.tokenNumerico(T_INT, radix), radix);
    }

    public boolean hasNextLong() {
        return this.hasNextLong(this.radix);
    }

    public boolean hasNextLong(int radix) {
        return this.puede(T_LONG, radix);
    }

    public long nextLong() {
        return this.nextLong(this.radix);
    }

    public long nextLong(int radix) {
        return Long.parseLong(this.tokenNumerico(T_LONG, radix), radix);
    }

    public boolean hasNextFloat() {
        return this.puede(T_FLOAT, 10);
    }

    public float nextFloat() {
        return Float.parseFloat(this.tokenNumerico(T_FLOAT, 10));
    }

    public boolean hasNextDouble() {
        return this.puede(T_DOUBLE, 10);
    }

    public double nextDouble() {
        return Double.parseDouble(this.tokenNumerico(T_DOUBLE, 10));
    }

    public boolean hasNextBigInteger() {
        return this.hasNextBigInteger(this.radix);
    }

    public boolean hasNextBigInteger(int radix) {
        return this.puede(T_BIGINT, radix);
    }

    public BigInteger nextBigInteger() {
        return this.nextBigInteger(this.radix);
    }

    public BigInteger nextBigInteger(int radix) {
        return new BigInteger(this.tokenNumerico(T_BIGINT, radix), radix);
    }

    public boolean hasNextBigDecimal() {
        return this.puede(T_BIGDEC, 10);
    }

    public BigDecimal nextBigDecimal() {
        return new BigDecimal(this.tokenNumerico(T_BIGDEC, 10));
    }

    public boolean hasNextBoolean() {
        String t = this.espiar();
        if (t == null) {
            return false;
        }
        return t.equalsIgnoreCase("true") || t.equalsIgnoreCase("false");
    }

    public boolean nextBoolean() {
        String t = this.espiar();
        if (t == null) {
            throw new NoSuchElementException();
        }
        if (!t.equalsIgnoreCase("true") && !t.equalsIgnoreCase("false")) {
            throw new InputMismatchException();
        }
        this.next();
        return t.equalsIgnoreCase("true");
    }

    // ---- busqueda directa, sin pasar por los tokens --------------------------------------------------

    public String findInLine(String pattern) {
        return this.findInLine(Pattern.compile(pattern));
    }

    // El patron, buscado **dentro de la linea actual**. No cruza el salto de linea, que es lo que
    // lo distingue de `findWithinHorizon`.
    public String findInLine(Pattern pattern) {
        this.chequearAbierto();
        int[] fin = this.buscarFinDeLinea();
        int limite;
        if (fin == null) {
            limite = this.buf.length();
        } else if (fin[0] < 0) {
            limite = this.buf.length();
        } else {
            limite = fin[0];
        }
        Matcher m = pattern.matcher(this.buf);
        m.region(this.position, limite);
        if (!m.find()) {
            return null;
        }
        this.lastResult = m.toMatchResult();
        this.position = m.end();
        return m.group();
    }

    public String findWithinHorizon(String pattern, int horizon) {
        return this.findWithinHorizon(Pattern.compile(pattern), horizon);
    }

    // El patron, buscado en los proximos `horizon` caracteres. Con `horizon` en 0 no hay limite.
    public String findWithinHorizon(Pattern pattern, int horizon) {
        this.chequearAbierto();
        if (horizon < 0) {
            throw new IllegalArgumentException("horizon < 0");
        }
        while (!this.sourceClosed) {
            if (horizon > 0 && this.buf.length() - this.position >= horizon) {
                break;
            }
            if (!this.leerMas()) {
                break;
            }
        }
        int limite = this.buf.length();
        if (horizon > 0 && this.position + horizon < limite) {
            limite = this.position + horizon;
        }
        Matcher m = pattern.matcher(this.buf);
        m.region(this.position, limite);
        if (!m.find()) {
            return null;
        }
        this.lastResult = m.toMatchResult();
        this.position = m.end();
        return m.group();
    }

    public Scanner skip(String pattern) {
        return this.skip(Pattern.compile(pattern));
    }

    // Salta lo que matchee **desde la posicion actual**, sin delimitadores de por medio. A
    // diferencia de `find*`, el patron tiene que empezar justo aca.
    public Scanner skip(Pattern pattern) {
        this.chequearAbierto();
        while (!this.sourceClosed && this.leerMas()) {
            // se trae todo lo que se pueda: `skip` no tiene horizonte
        }
        Matcher m = pattern.matcher(this.buf);
        m.region(this.position, this.buf.length());
        if (!m.lookingAt()) {
            throw new NoSuchElementException();
        }
        this.lastResult = m.toMatchResult();
        this.position = m.end();
        return this;
    }

    // ---- streams -----------------------------------------------------------------------------------

    public Stream<String> tokens() {
        this.chequearAbierto();
        ArrayList<String> out = new ArrayList<String>();
        while (this.hasNext()) {
            out.add(this.next());
        }
        String[] a = new String[out.size()];
        int i = 0;
        while (i < a.length) {
            a[i] = out.get(i);
            i = i + 1;
        }
        return Stream.of(a);
    }

    public Stream<String> findAll(String pattern) {
        return this.findAll(Pattern.compile(pattern));
    }

    public Stream<String> findAll(Pattern pattern) {
        this.chequearAbierto();
        while (!this.sourceClosed && this.leerMas()) {
            // idem: se junta todo antes de buscar
        }
        ArrayList<String> out = new ArrayList<String>();
        Matcher m = pattern.matcher(this.buf);
        m.region(this.position, this.buf.length());
        while (m.find()) {
            out.add(m.group());
            this.lastResult = m.toMatchResult();
            this.position = m.end();
        }
        String[] a = new String[out.size()];
        int i = 0;
        while (i < a.length) {
            a[i] = out.get(i);
            i = i + 1;
        }
        return Stream.of(a);
    }

    // ---- configuracion -------------------------------------------------------------------------------

    public Pattern delimiter() {
        return this.delimiter;
    }

    public Scanner useDelimiter(Pattern pattern) {
        this.delimiter = pattern;
        return this;
    }

    public Scanner useDelimiter(String pattern) {
        return this.useDelimiter(Pattern.compile(pattern));
    }

    public Locale locale() {
        return this.locale;
    }

    // Se guarda y no cambia el analisis: ver la tabla de divergencias.
    public Scanner useLocale(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        this.locale = locale;
        return this;
    }

    public int radix() {
        return this.radix;
    }

    public Scanner useRadix(int radix) {
        if (radix < 2 || radix > 36) {
            throw new IllegalArgumentException("radix:" + radix);
        }
        this.radix = radix;
        return this;
    }

    // Vuelve a la configuracion de fabrica -- delimitador, locale y base -- **sin** tocar la
    // posicion. Reiniciar el estado no es rebobinar la entrada.
    public Scanner reset() {
        this.delimiter = ESPACIOS;
        this.locale = Locale.getDefault();
        this.radix = 10;
        return this;
    }

    // ---- estado --------------------------------------------------------------------------------------

    // El resultado de la ultima operacion que matcheo algo.
    public MatchResult match() {
        if (this.lastResult == null) {
            throw new IllegalStateException("No match result available");
        }
        return this.lastResult;
    }

    // La ultima IOException que tiro la fuente, o null.
    //
    // Existe porque los metodos de Scanner **no** declaran IOException: se la traga y la deja
    // disponible aca. Quien lea de un archivo tiene que preguntar, o no se entera de que la lectura
    // se corto por un error en vez de por fin de entrada.
    public IOException ioException() {
        return this.lastException;
    }

    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.sourceClosed = true;
        if (this.source instanceof Closeable) {
            ((Closeable) this.source).close();
        }
        this.source = null;
    }

    public String toString() {
        return "java.util.Scanner[delimiters=" + this.delimiter.pattern()
                + "][position=" + this.position
                + "][match valid=" + (this.lastResult != null)
                + "][closed=" + this.closed
                + "][radix=" + this.radix
                + "][locale=" + this.locale
                + "]";
    }
}

// El MatchResult de una operacion de Scanner que no paso por un Matcher -- `next()` y `nextLine()`,
// que ubican su texto contando delimitadores y no matcheando. Package-private.
//
// Un solo grupo, el cero: no hay subgrupos que reportar porque no hubo patron con parentesis.
final class ScanMatch implements MatchResult {

    private final String texto;
    private final int desde;
    private final int hasta;

    ScanMatch(String texto, int desde, int hasta) {
        this.texto = texto;
        this.desde = desde;
        this.hasta = hasta;
    }

    private void chequear(int group) {
        if (group != 0) {
            throw new IndexOutOfBoundsException("No group " + group);
        }
    }

    public int start() {
        return this.desde;
    }

    public int start(int group) {
        this.chequear(group);
        return this.desde;
    }

    public int end() {
        return this.hasta;
    }

    public int end(int group) {
        this.chequear(group);
        return this.hasta;
    }

    public String group() {
        return this.texto;
    }

    public String group(int group) {
        this.chequear(group);
        return this.texto;
    }

    public int groupCount() {
        return 0;
    }
}
