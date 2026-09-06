import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import jdk.jshell.execution.LocalExecutionControl;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControl.ClassBytecodes;

/**
 * Checks {@code jdk.jshell.execution} against JDK 25.
 *
 * <p>It does what JShell does: it hands an engine the bytecode of an already compiled class, asks it
 * to call its methods and read its variables, and compares each answer with the real JDK's.
 *
 * <p>The bytecode comes from {@code java/JSH1x.class}, a pretend snippet compiled beforehand.
 * Compiling for real would put the compiler into the test, and what is being tested is the execution
 * engine.
 *
 * <p>{@link #where()} returns the index of the first answer that differs, or -1: an integer is
 * enough because {@code run-headless} does not flush the console.
 */
public class JSH1 {


    static final String[] EXPECTED = {
        "load|ok",
        "invoke-f|42",
        "invoke-g|\"echo\"",
        "invoke-nothing|null",
        "var-V|7",
        "var-T|\"hello\"",
        "exception|java.lang.IllegalStateException|on purpose",
        "no-such-method|jdk.jshell.spi.ExecutionControl$InternalException",
        "no-such-var|jdk.jshell.spi.ExecutionControl$InternalException",
        "no-such-class|jdk.jshell.spi.ExecutionControl$InternalException",
        "extension|jdk.jshell.spi.ExecutionControl$NotImplementedException",
        "stop-with-nothing|ok",
    };

    static String failure(Throwable e) {
        String m = e.getMessage();
        // Stack traces carry line numbers and file names that need not match between the two
        // libraries; what is compared is the type and the message.
        return e.getClass().getName() + "|" + m;
    }

    static ClassBytecodes[] snippet() throws IOException {
        final byte[] b = Files.readAllBytes(Paths.get("java/JSH1x.class"));
        return new ClassBytecodes[] {new ClassBytecodes("JSH1x", b)};
    }

    /** What the engine answers, one line per question. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();
        final ExecutionControl ec = new LocalExecutionControl();

        try {
            ec.load(snippet());
            a.add("load|ok");
        } catch (Throwable e) {
            a.add("load|" + failure(e));
        }

        String[][] calls = {
            {"f", "42"}, {"g", null}, {"nothing", null},
        };
        for (int i = 0; i < calls.length; i++) {
            try {
                a.add("invoke-" + calls[i][0] + "|" + ec.invoke("JSH1x", calls[i][0]));
            } catch (Throwable e) {
                a.add("invoke-" + calls[i][0] + "|" + failure(e));
            }
        }

        String[] vars = {"V", "T"};
        for (int i = 0; i < vars.length; i++) {
            try {
                a.add("var-" + vars[i] + "|" + ec.varValue("JSH1x", vars[i]));
            } catch (Throwable e) {
                a.add("var-" + vars[i] + "|" + failure(e));
            }
        }

        // An exception from the user has to arrive converted, not raw.
        try {
            ec.invoke("JSH1x", "blowUp");
            a.add("exception|no error");
        } catch (ExecutionControl.UserException e) {
            a.add("exception|" + e.causeExceptionClass() + "|" + e.getMessage());
        } catch (Throwable e) {
            a.add("exception|" + failure(e));
        }

        // And the things that do not exist.
        try {
            ec.invoke("JSH1x", "doesNotExist");
            a.add("no-such-method|no error");
        } catch (Throwable e) {
            a.add("no-such-method|" + e.getClass().getName());
        }
        try {
            ec.varValue("JSH1x", "doesNotExist");
            a.add("no-such-var|no error");
        } catch (Throwable e) {
            a.add("no-such-var|" + e.getClass().getName());
        }
        try {
            ec.invoke("NotThere", "f");
            a.add("no-such-class|no error");
        } catch (Throwable e) {
            a.add("no-such-class|" + e.getClass().getName());
        }

        // What this engine declares it does not support.
        try {
            ec.extensionCommand("whatever", null);
            a.add("extension|no error");
        } catch (Throwable e) {
            a.add("extension|" + e.getClass().getName());
        }

        // Stopping with nothing running cannot fail: the request may arrive in between.
        try {
            ec.stop();
            a.add("stop-with-nothing|ok");
        } catch (Throwable e) {
            a.add("stop-with-nothing|" + failure(e));
        }

        ec.close();
        return a.toArray(new String[a.size()]);
    }

    /**
     * The index of the first answer that differs from the JDK's, or -1.
     *
     * @return the index, or -1
     */
    public static int where() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != EXPECTED.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(EXPECTED[i])) {
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final String[] a = actual();
        if (args.length > 0) {
            for (int i = 0; i < a.length; i++) {
                System.out.println(a[i]);
            }
            return;
        }
        final int i = where();
        System.out.println(i < 0 ? "no differences"
                : i + ":\n  ours=" + a[i] + "\n  jdk =" + EXPECTED[i]);
    }
}
