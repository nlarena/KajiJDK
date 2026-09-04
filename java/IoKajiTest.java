import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.LineNumberInputStream;
import java.io.ObjectStreamConstants;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.StreamTokenizer;
import java.io.StringBufferInputStream;
import java.io.StringReader;

/**
 * Comportamiento de lo que se agrego a java.io: StringBufferInputStream, LineNumberInputStream,
 * StreamTokenizer, FileReader/FileWriter, las cuatro Piped* y ObjectStreamConstants.
 *
 * <p>Corre igual en nuestra VM y en la JVM real, y en la real el JDK es el oraculo: cualquier
 * diferencia de comportamiento sale como un codigo distinto de -1 en una sola de las dos.
 *
 * <p>run() devuelve -1 si esta todo bien, o el numero del primer chequeo que fallo.
 */
public class IoKajiTest {

    static int failure = 0;

    static void chk(int n, boolean ok) {
        if (!ok && failure == 0) {
            failure = n;
        }
    }

    static void chkEq(int n, long expected, long actual) {
        if (expected != actual && failure == 0) {
            failure = n;
            System.out.println("chk " + n + ": esperaba " + expected + " y fue " + actual);
        }
    }

    static void chkEq(int n, String expected, String actual) {
        boolean ok;
        if (expected == null) {
            ok = actual == null;
        } else {
            ok = expected.equals(actual);
        }
        if (!ok && failure == 0) {
            failure = n;
            System.out.println("chk " + n + ": esperaba [" + expected + "] y fue [" + actual + "]");
        }
    }

    // ---------------------------------------------------------------------------------------
    // 100: StringBufferInputStream -- incluido el truncamiento a 8 bits, que es su rasgo definitorio
    // ---------------------------------------------------------------------------------------
    static void stringBuffer() throws Exception {
        StringBufferInputStream s = new StringBufferInputStream("AB");
        chkEq(101, 2, s.available());
        chkEq(102, 65, s.read());
        chkEq(103, 66, s.read());
        chkEq(104, -1, s.read());
        s.reset();
        chkEq(105, 65, s.read());

        // Trunca a los 8 bits de abajo: 0x00E9 sale 0xE9, y 0x0141 sale 0x41 ('A').
        StringBufferInputStream t = new StringBufferInputStream("\u00e9\u0141");
        chkEq(106, 0xE9, t.read());
        chkEq(107, 0x41, t.read());

        StringBufferInputStream u = new StringBufferInputStream("abcdef");
        chkEq(108, 2, u.skip(2));
        chkEq(109, 'c', u.read());
        byte[] b = new byte[10];
        chkEq(110, 3, u.read(b, 0, 3));
        chkEq(111, 'd', b[0]);
        chkEq(112, 'f', b[2]);
        chkEq(113, -1, u.read());
    }

    // ---------------------------------------------------------------------------------------
    // 200: LineNumberInputStream -- los tres finales de linea salen como un solo \n
    // ---------------------------------------------------------------------------------------
    static void lineNumber() throws Exception {
        // "a\r\nb\nc\rd": \r\n vale una linea, \n otra, \r suelto otra.
        StringBufferInputStream base = new StringBufferInputStream("a\r\nb\nc\rd");
        LineNumberInputStream l = new LineNumberInputStream(base);
        chkEq(201, 0, l.getLineNumber());
        chkEq(202, 'a', l.read());
        chkEq(203, '\n', l.read());
        chkEq(204, 1, l.getLineNumber());
        chkEq(205, 'b', l.read());
        chkEq(206, '\n', l.read());
        chkEq(207, 2, l.getLineNumber());
        chkEq(208, 'c', l.read());
        chkEq(209, '\n', l.read());
        chkEq(210, 3, l.getLineNumber());
        chkEq(211, 'd', l.read());
        chkEq(212, -1, l.read());

        LineNumberInputStream m =
            new LineNumberInputStream(new StringBufferInputStream("x\r\ny"));
        byte[] b = new byte[8];
        chkEq(213, 3, m.read(b, 0, 8));
        chkEq(214, 'x', b[0]);
        chkEq(215, '\n', b[1]);
        chkEq(216, 'y', b[2]);
        chkEq(217, 1, m.getLineNumber());

        LineNumberInputStream n =
            new LineNumberInputStream(new StringBufferInputStream("p\nq"));
        n.setLineNumber(40);
        chkEq(218, 40, n.getLineNumber());
        n.read();
        n.read();
        chkEq(219, 41, n.getLineNumber());
    }

    // ---------------------------------------------------------------------------------------
    // 300: StreamTokenizer
    // ---------------------------------------------------------------------------------------
    static void tokenizer() throws Exception {
        // El \\t de la fuente pone una barra y una t en el stream; el tokenizer las une en un tab.
        StreamTokenizer t = new StreamTokenizer(new StringReader("foo 12.5 \"he\\tllo\" + bar9"));
        chkEq(301, StreamTokenizer.TT_WORD, t.nextToken());
        chkEq(302, "foo", t.sval);
        chkEq(303, StreamTokenizer.TT_NUMBER, t.nextToken());
        chk(304, t.nval == 12.5);
        chkEq(305, '"', t.nextToken());
        chkEq(306, "he\tllo", t.sval);
        chkEq(307, '+', t.nextToken());
        chkEq(308, StreamTokenizer.TT_WORD, t.nextToken());
        chkEq(309, "bar9", t.sval);
        chkEq(310, StreamTokenizer.TT_EOF, t.nextToken());
        chkEq(311, 1, t.lineno());

        // pushBack: el mismo token dos veces sin avanzar.
        StreamTokenizer p = new StreamTokenizer(new StringReader("uno dos"));
        p.nextToken();
        chkEq(312, "uno", p.sval);
        p.pushBack();
        chkEq(313, StreamTokenizer.TT_WORD, p.nextToken());
        chkEq(314, "uno", p.sval);
        chkEq(315, StreamTokenizer.TT_WORD, p.nextToken());
        chkEq(316, "dos", p.sval);

        // Numero negativo y el `-` suelto: `-5` es un numero, `a - b` deja el `-` solo.
        StreamTokenizer neg = new StreamTokenizer(new StringReader("-5 a - b"));
        chkEq(317, StreamTokenizer.TT_NUMBER, neg.nextToken());
        chk(318, neg.nval == -5.0);
        neg.nextToken();
        chkEq(319, '-', neg.nextToken());

        // eolIsSignificant y el conteo de lineas.
        StreamTokenizer eol = new StreamTokenizer(new StringReader("a\nb"));
        eol.eolIsSignificant(true);
        chkEq(320, StreamTokenizer.TT_WORD, eol.nextToken());
        chkEq(321, StreamTokenizer.TT_EOL, eol.nextToken());
        chkEq(322, StreamTokenizer.TT_WORD, eol.nextToken());
        chkEq(323, 2, eol.lineno());

        // slashSlash y slashStar.
        StreamTokenizer com = new StreamTokenizer(new StringReader("a // x\nb /* y */ c"));
        com.slashSlashComments(true);
        com.slashStarComments(true);
        com.nextToken();
        chkEq(324, "a", com.sval);
        com.nextToken();
        chkEq(325, "b", com.sval);
        com.nextToken();
        chkEq(326, "c", com.sval);
        chkEq(327, StreamTokenizer.TT_EOF, com.nextToken());

        // lowerCaseMode solo afecta a las palabras.
        StreamTokenizer low = new StreamTokenizer(new StringReader("ABC"));
        low.lowerCaseMode(true);
        low.nextToken();
        chkEq(328, "abc", low.sval);

        // Escape octal: \101 es 'A'; \tab ya se probo arriba.
        StreamTokenizer oct = new StreamTokenizer(new StringReader("\"\\101\""));
        oct.nextToken();
        chkEq(329, "A", oct.sval);

        // resetSyntax deja todo sin significado: cada caracter sale solo.
        StreamTokenizer rs = new StreamTokenizer(new StringReader("ab"));
        rs.resetSyntax();
        chkEq(330, 'a', rs.nextToken());
        chkEq(331, 'b', rs.nextToken());
    }

    // ---------------------------------------------------------------------------------------
    // 400: FileWriter / FileReader, ida y vuelta
    // ---------------------------------------------------------------------------------------
    static void files() throws Exception {
        File f = new File("iokajitest_tmp.txt");
        FileWriter w = new FileWriter(f);
        w.write("hola\nmundo");
        w.close();

        FileReader r = new FileReader(f);
        char[] buf = new char[64];
        int n = r.read(buf, 0, 64);
        r.close();
        chkEq(401, 10, n);
        chkEq(402, "hola\nmundo", new String(buf, 0, n));

        // append: se agrega, no se pisa.
        FileWriter w2 = new FileWriter(f, true);
        w2.write("!");
        w2.close();
        FileReader r2 = new FileReader(f);
        int n2 = r2.read(buf, 0, 64);
        r2.close();
        chkEq(403, 11, n2);
        chkEq(404, "hola\nmundo!", new String(buf, 0, n2));

        // sin append: trunca.
        FileWriter w3 = new FileWriter(f);
        w3.write("z");
        w3.close();
        FileReader r3 = new FileReader(f);
        int n3 = r3.read(buf, 0, 64);
        r3.close();
        chkEq(405, 1, n3);
        chkEq(406, "z", new String(buf, 0, n3));

        f.delete();
    }

    // ---------------------------------------------------------------------------------------
    // 500: las tuberias, con dos hilos de verdad
    // ---------------------------------------------------------------------------------------
    static void pipes() throws Exception {
        // Bytes. Se mandan mas de PIPE_SIZE (1024) para forzar al escritor a esperar lugar: si el
        // despertar mutuo estuviera mal, esto se cuelga en vez de fallar.
        final int N = 5000;
        final PipedInputStream in = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(in);
        Thread writer = new Thread(new Runnable() {
            public void run() {
                try {
                    for (int i = 0; i < N; i++) {
                        out.write(i & 0xFF);
                    }
                    out.close();
                } catch (Exception e) {
                    failure = 599;
                }
            }
        });
        writer.start();
        int readCount = 0;
        long sum = 0;
        while (true) {
            int c = in.read();
            if (c < 0) {
                break;
            }
            sum = sum + c;
            readCount = readCount + 1;
        }
        writer.join();
        in.close();
        chkEq(501, N, readCount);
        long expected = 0;
        for (int i = 0; i < N; i++) {
            expected = expected + (i & 0xFF);
        }
        chkEq(502, expected, sum);

        // Bytes en bloque.
        final PipedInputStream in2 = new PipedInputStream();
        final PipedOutputStream out2 = new PipedOutputStream(in2);
        Thread e2 = new Thread(new Runnable() {
            public void run() {
                try {
                    byte[] b = new byte[3000];
                    for (int i = 0; i < b.length; i++) {
                        b[i] = (byte) (i & 0x7F);
                    }
                    out2.write(b, 0, b.length);
                    out2.close();
                } catch (Exception e) {
                    failure = 598;
                }
            }
        });
        e2.start();
        byte[] dest = new byte[3000];
        int total = 0;
        while (total < 3000) {
            int k = in2.read(dest, total, 3000 - total);
            if (k < 0) {
                break;
            }
            total = total + k;
        }
        e2.join();
        chkEq(503, 3000, total);
        chkEq(504, 0, dest[0]);
        chkEq(505, 0x7F, dest[127]);
        chkEq(506, 2999 & 0x7F, dest[2999]);

        // Caracteres.
        final PipedReader pr = new PipedReader();
        final PipedWriter pw = new PipedWriter(pr);
        Thread e3 = new Thread(new Runnable() {
            public void run() {
                try {
                    for (int i = 0; i < 3000; i++) {
                        pw.write('a' + (i % 26));
                    }
                    pw.close();
                } catch (Exception e) {
                    failure = 597;
                }
            }
        });
        e3.start();
        int count = 0;
        long charSum = 0;
        while (true) {
            int c = pr.read();
            if (c < 0) {
                break;
            }
            charSum = charSum + c;
            count = count + 1;
        }
        e3.join();
        pr.close();
        chkEq(507, 3000, count);
        long expectedChars = 0;
        for (int i = 0; i < 3000; i++) {
            expectedChars = expectedChars + ('a' + (i % 26));
        }
        chkEq(508, expectedChars, charSum);
    }

    // ---------------------------------------------------------------------------------------
    // 600: ObjectStreamConstants -- son el formato, tienen que dar exactamente estos numeros
    // ---------------------------------------------------------------------------------------
    static void constants() throws Exception {
        chkEq(601, -21267, ObjectStreamConstants.STREAM_MAGIC);
        chkEq(602, 5, ObjectStreamConstants.STREAM_VERSION);
        chkEq(603, 0x70, ObjectStreamConstants.TC_NULL);
        chkEq(604, 0x71, ObjectStreamConstants.TC_REFERENCE);
        chkEq(605, 0x72, ObjectStreamConstants.TC_CLASSDESC);
        chkEq(606, 0x73, ObjectStreamConstants.TC_OBJECT);
        chkEq(607, 0x74, ObjectStreamConstants.TC_STRING);
        chkEq(608, 0x75, ObjectStreamConstants.TC_ARRAY);
        chkEq(609, 0x76, ObjectStreamConstants.TC_CLASS);
        chkEq(610, 0x77, ObjectStreamConstants.TC_BLOCKDATA);
        chkEq(611, 0x78, ObjectStreamConstants.TC_ENDBLOCKDATA);
        chkEq(612, 0x79, ObjectStreamConstants.TC_RESET);
        chkEq(613, 0x7A, ObjectStreamConstants.TC_BLOCKDATALONG);
        chkEq(614, 0x7B, ObjectStreamConstants.TC_EXCEPTION);
        chkEq(615, 0x7C, ObjectStreamConstants.TC_LONGSTRING);
        chkEq(616, 0x7D, ObjectStreamConstants.TC_PROXYCLASSDESC);
        chkEq(617, 0x7E, ObjectStreamConstants.TC_ENUM);
        chkEq(618, 0x7E0000, ObjectStreamConstants.baseWireHandle);
        chkEq(619, 1, ObjectStreamConstants.SC_WRITE_METHOD);
        chkEq(620, 2, ObjectStreamConstants.SC_SERIALIZABLE);
        chkEq(621, 4, ObjectStreamConstants.SC_EXTERNALIZABLE);
        chkEq(622, 8, ObjectStreamConstants.SC_BLOCK_DATA);
        chkEq(623, 16, ObjectStreamConstants.SC_ENUM);
        chkEq(624, 1, ObjectStreamConstants.PROTOCOL_VERSION_1);
        chkEq(625, 2, ObjectStreamConstants.PROTOCOL_VERSION_2);
        chkEq(626, "enableSubstitution",
              ObjectStreamConstants.SUBSTITUTION_PERMISSION.getName());
        chkEq(627, "enableSubclassImplementation",
              ObjectStreamConstants.SUBCLASS_IMPLEMENTATION_PERMISSION.getName());
        chkEq(628, "serialFilter",
              ObjectStreamConstants.SERIAL_FILTER_PERMISSION.getName());
    }

    public static int run() {
        failure = 0;
        try {
            stringBuffer();
            lineNumber();
            tokenizer();
            files();
            pipes();
            constants();
        } catch (Exception e) {
            System.out.println("excepcion: " + e);
            if (failure == 0) {
                failure = 999;
            }
        }
        if (failure == 0) {
            return -1;
        }
        return failure;
    }

    public static void main(String[] a) {
        System.out.println(run());
    }
}
