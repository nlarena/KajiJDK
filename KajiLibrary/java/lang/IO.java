package java.lang;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

// Line-based input/output for small programs (JEP 512, Java 25): `println`, `print` and `readln`
// without having to name `System.out` or build a `BufferedReader` by hand.
//
// It exists so the first program somebody writes does not start by explaining what a `PrintStream`
// is. Its three decisions of shape follow from that, and they are not arbitrary:
//
//   - There is no `printf`. A cryptic format language is no use to somebody just starting, and it
//     drags the `Locale` problem along as well. The JDK left it out on purpose and so does this.
//   - Everything is `static` and the class is `final` with a private constructor: there is no
//     instance to create and no state to configure.
//   - `readln` returns `null` at end of input, it does not throw. It is a
//     `while ((s = readln()) != null)`.
//
// Careful about mixing: from the first `readln` on, the decoder may have consumed more bytes from
// `System.in` than it returned (the `InputStreamReader`'s buffer), so reading straight from
// `System.in` afterwards gives unpredictable results. It is the JDK's contract and not a limit of
// ours: the class is meant to be the only one that touches standard input.
public final class IO {

    // There are no instances. An `Error` is thrown and not an `UnsupportedOperationException`
    // because getting here is
    // impossible from compiled code --there is no visible constructor-- and can only happen through
    // forced reflection, which is a programming error and not an unsupported operation.
    private IO() {
        throw new Error("no instances");
    }

    public static void println(Object obj) {
        System.out.println(obj);
    }

    public static void println() {
        System.out.println();
    }

    // `print` does flush the buffer and `println` does not. The reason is the prompt: whoever writes
    // `print("name: ")` and then `readln()` has to see the prompt **before** being asked to type, and
    // with no line break `System.out`'s per-line autoflush does not guarantee it. With
    // `println` that autoflush is already enough.
    public static void print(Object obj) {
        java.io.PrintStream out = System.out;
        out.print(obj);
        out.flush();
    }

    // It returns the line without the separator, or `null` if the input ended with nothing read.
    //
    // The `IOException` is wrapped in an `IOError` instead of propagating: this class's audience
    // should not have to write a `try`/`catch` to read a line, and a standard input that fails is not
    // a condition a program like that can recover from.
    public static String readln() {
        try {
            return IO.reader().readLine();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    public static String readln(String prompt) {
        IO.print(prompt);
        return IO.readln();
    }

    // The cached reader. It is touched only from `reader()`, which is synchronised.
    private static BufferedReader br;

    // The decoder is built late, on the first read, and not in a static initialiser: if the program
    // never reads, the cost is not paid and no bytes are stolen from `System.in`.
    //
    // The encoding comes from `stdin.encoding` and falls back to UTF-8 if the property is absent or
    // names something that does not exist. The lenient form of `forName` is used for exactly that
    // reason: the value comes from the environment's configuration, and a misspelling there should
    // not bring the program down.
    static synchronized BufferedReader reader() {
        if (IO.br == null) {
            String enc = System.getProperty("stdin.encoding", "");
            Charset cs = Charset.forName(enc, StandardCharsets.UTF_8);
            IO.br = new BufferedReader(new InputStreamReader(System.in, cs));
        }
        return IO.br;
    }
}
