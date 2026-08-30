import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.MatchResult;

// Comportamiento de java.util.Scanner: tokens, numeros, lineas, delimitadores y busqueda directa.
//
// Lo que mas se prueba son los pares hasNextX/nextX, porque el punto de Scanner es que `hasNextX`
// mire sin consumir: un `hasNextInt()` que consumiera dejaria el token perdido para el `next()`
// que viene atras, y ningun bucle funcionaria.
public class ScanTest {

    public static int run() {
        int r = 0;

        // ---- tokens ------------------------------------------------------------------------------
        Scanner s = new Scanner("uno dos  tres");
        r = r + (s.hasNext() ? 1 : 0);
        r = r + (s.next().equals("uno") ? 2 : 0);
        r = r + (s.next().equals("dos") ? 4 : 0);
        r = r + (s.next().equals("tres") ? 8 : 0);
        r = r + (s.hasNext() ? 0 : 16);
        boolean seNego = false;
        try {
            s.next();
        } catch (NoSuchElementException e) {
            seNego = true;
        }
        r = r + (seNego ? 32 : 0);

        // espacios al principio y al final: no son tokens
        Scanner conBordes = new Scanner("   a  b   ");
        r = r + (conBordes.next().equals("a") ? 64 : 0);
        r = r + (conBordes.next().equals("b") ? 128 : 0);
        r = r + (conBordes.hasNext() ? 0 : 256);

        // ---- numeros -----------------------------------------------------------------------------
        Scanner n = new Scanner("42 -7 3.5 true xyz");
        r = r + (n.hasNextInt() ? 512 : 0);
        r = r + n.nextInt() * 1024;                       // 43008
        r = r + (n.nextInt() == -7 ? 2048 : 0);
        r = r + (n.hasNextInt() ? 0 : 4096);              // "3.5" no es int
        r = r + (n.hasNextDouble() ? 8192 : 0);
        r = r + (n.nextDouble() == 3.5d ? 16384 : 0);
        r = r + (n.hasNextBoolean() ? 32768 : 0);
        r = r + (n.nextBoolean() ? 65536 : 0);

        // el token que no convierte NO se consume
        boolean noConvierte = false;
        try {
            n.nextInt();
        } catch (InputMismatchException e) {
            noConvierte = true;
        }
        r = r + (noConvierte ? 131072 : 0);
        r = r + (n.next().equals("xyz") ? 262144 : 0);    // seguia ahi

        // ---- bases -------------------------------------------------------------------------------
        Scanner hex = new Scanner("ff 10");
        hex.useRadix(16);
        r = r + (hex.radix() == 16 ? 1 : 0);
        r = r + (hex.nextInt() == 255 ? 2 : 0);
        r = r + (hex.nextInt() == 16 ? 4 : 0);
        Scanner porArg = new Scanner("1010");
        r = r + (porArg.nextInt(2) == 10 ? 8 : 0);
        Scanner grande = new Scanner("ffffffffffffffffff");
        r = r + (grande.nextBigInteger(16).bitLength() == 72 ? 16 : 0);

        // separador de miles
        Scanner miles = new Scanner("1,234");
        r = r + (miles.nextInt() == 1234 ? 32 : 0);

        // ---- lineas -------------------------------------------------------------------------------
        Scanner l = new Scanner("primera\nsegunda\r\ntercera");
        r = r + (l.hasNextLine() ? 64 : 0);
        r = r + (l.nextLine().equals("primera") ? 128 : 0);
        r = r + (l.nextLine().equals("segunda") ? 256 : 0);   // \r\n cuenta como uno
        r = r + (l.nextLine().equals("tercera") ? 512 : 0);
        r = r + (l.hasNextLine() ? 0 : 1024);

        // La trampa clasica: nextInt deja el fin de linea, asi que el nextLine que sigue devuelve
        // el resto VACIO de esa linea y no la siguiente.
        Scanner trampa = new Scanner("42\nresto");
        int leido = trampa.nextInt();
        String pegado = trampa.nextLine();
        r = r + (leido == 42 && pegado.equals("") ? 2048 : 0);
        r = r + (trampa.nextLine().equals("resto") ? 4096 : 0);

        // ---- delimitadores -------------------------------------------------------------------------
        // Con el delimitador en `,` --y no en `,+`-- dos comas seguidas dejan un token VACIO en el
        // medio. Es lo correcto para un CSV, donde una celda vacia es una celda.
        Scanner csv = new Scanner("a,b,,c");
        csv.useDelimiter(",");
        r = r + (csv.next().equals("a") ? 8192 : 0);
        r = r + (csv.next().equals("b") ? 16384 : 0);
        r = r + (csv.next().equals("") ? 32768 : 0);
        r = r + (csv.next().equals("c") ? 65536 : 0);
        r = r + (csv.delimiter().pattern().equals(",") ? 131072 : 0);

        // reset vuelve el delimitador al de fabrica
        Scanner vuelta = new Scanner("x y");
        vuelta.useDelimiter(",");
        vuelta.reset();
        r = r + (vuelta.next().equals("x") ? 262144 : 0);

        // ---- busqueda directa ------------------------------------------------------------------------
        Scanner b = new Scanner("clave=valor\notra=cosa");
        r = r + (b.findInLine("[a-z]+=").equals("clave=") ? 1 : 0);
        r = r + (b.next().equals("valor") ? 2 : 0);
        // findInLine no cruza el salto de linea
        Scanner corta = new Scanner("nada\nobjetivo");
        r = r + (corta.findInLine("objetivo") == null ? 4 : 0);
        // findWithinHorizon si
        Scanner larga = new Scanner("nada\nobjetivo");
        r = r + (larga.findWithinHorizon("objetivo", 0).equals("objetivo") ? 8 : 0);

        Scanner salta = new Scanner("###hola");
        salta.skip("#+");
        r = r + (salta.next().equals("hola") ? 16 : 0);
        boolean saltoMal = false;
        try {
            new Scanner("hola").skip("#+");
        } catch (NoSuchElementException e) {
            saltoMal = true;
        }
        r = r + (saltoMal ? 32 : 0);

        // match() describe lo ultimo que matcheo
        Scanner m = new Scanner("abc def");
        m.next();
        MatchResult mr = m.match();
        r = r + (mr.group().equals("abc") ? 64 : 0);
        r = r + (mr.start() == 0 && mr.end() == 3 ? 128 : 0);

        // ---- otras fuentes ----------------------------------------------------------------------------
        byte[] bytes = { 104, 111, 108, 97, 32, 55 };            // "hola 7"
        Scanner desdeStream = new Scanner(new ByteArrayInputStream(bytes));
        r = r + (desdeStream.next().equals("hola") ? 256 : 0);
        r = r + (desdeStream.nextInt() == 7 ? 512 : 0);

        // Un Readable que entrega de a poco: prueba la lectura incremental, que es el camino que la
        // fuente String nunca ejercita.
        Scanner goteo = new Scanner(new Gotero("alfa beta gamma"));
        r = r + (goteo.next().equals("alfa") ? 1024 : 0);
        r = r + (goteo.next().equals("beta") ? 2048 : 0);
        r = r + (goteo.next().equals("gamma") ? 4096 : 0);
        r = r + (goteo.hasNext() ? 0 : 8192);

        // CharBuffer es Readable
        Scanner desdeBuffer = new Scanner(CharBuffer.wrap("uno 2"));
        r = r + (desdeBuffer.next().equals("uno") ? 16384 : 0);
        r = r + (desdeBuffer.nextInt() == 2 ? 32768 : 0);

        // ---- tokens() y estado ---------------------------------------------------------------------------
        Scanner conteo = new Scanner("a b c d");
        r = r + (conteo.tokens().count() == 4L ? 1 : 0);

        Scanner cerrado = new Scanner("x");
        cerrado.close();
        boolean yaCerrado = false;
        try {
            cerrado.next();
        } catch (IllegalStateException e) {
            yaCerrado = true;
        }
        r = r + (yaCerrado ? 2 : 0);
        r = r + (new Scanner("x").ioException() == null ? 4 : 0);
        r = r + (new Scanner("x").toString().indexOf("java.util.Scanner[") == 0 ? 8 : 0);

        return r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}

// Un Readable que entrega de a tres caracteres, para forzar varias lecturas.
class Gotero implements Readable {

    private final String texto;
    private int pos;

    Gotero(String texto) {
        this.texto = texto;
    }

    public int read(CharBuffer cb) throws IOException {
        if (this.pos >= this.texto.length()) {
            return -1;
        }
        int n = 3;
        if (this.pos + n > this.texto.length()) {
            n = this.texto.length() - this.pos;
        }
        int i = 0;
        while (i < n) {
            cb.put(this.texto.charAt(this.pos + i));
            i = i + 1;
        }
        this.pos = this.pos + n;
        return n;
    }
}
