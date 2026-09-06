import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.execution.StreamingExecutionControl;
import jdk.jshell.execution.Util;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControl.ClassBytecodes;

/**
 * Checks {@code jdk.jshell.execution}'s protocol end to end.
 *
 * <h2>What it puts together</h2>
 *
 * <p>Both ends inside the same process, joined by two pipes. On one side a
 * {@link StreamingExecutionControl}, which is what JShell uses; on the other a
 * {@link Util#forwardExecutionControl} serving against a {@link DirectExecutionControl}, which is
 * what runs in the remote process. In between, the whole protocol: the mark, the command names, the
 * result codes and the exceptions taken apart.
 *
 * <p>No socket is needed to test it, and that is why it can be tested: all the protocol needs is a
 * pair of streams.
 *
 * <h2>The order the streams are opened in</h2>
 *
 * <p>{@link ObjectOutputStream} writes a header when built and {@link ObjectInputStream} reads it
 * when built. Both sides create their output first and flush it; the other way round, both sit
 * waiting for a header nobody wrote, with no symptom other than two still threads.
 */
public class JSH2 {



    static final String[] EXPECTED = {
        "load|ok",
        "invoke-f|42",
        "invoke-g|\"echo\"",
        "invoke-nothing|null",
        "var-V|7",
        "var-T|\"hello\"",
        "exception|java.lang.IllegalStateException|on purpose|trace-not-null=true",
        "redefine|jdk.jshell.spi.ExecutionControl$NotImplementedException",
        "extension|jdk.jshell.spi.ExecutionControl$NotImplementedException",
        "classpath|ok",
        "engine-finished|true",
        "engine-no-failure|true",
    };

    static ClassBytecodes[] snippet() throws Exception {
        final byte[] b = Files.readAllBytes(Paths.get("java/JSH1x.class"));
        return new ClassBytecodes[] {new ClassBytecodes("JSH1x", b)};
    }

    /** What the engine on the far side of the protocol answers, one line per question. */
    static String[] actual() throws Exception {
        final PipedInputStream toTheEngine = new PipedInputStream();
        final PipedOutputStream fromJShell = new PipedOutputStream(toTheEngine);
        final PipedInputStream toJShell = new PipedInputStream();
        final PipedOutputStream fromTheEngine = new PipedOutputStream(toJShell);

        final Throwable[] engineFailure = new Throwable[1];
        final Thread engine = new Thread(new Runnable() {
            public void run() {
                try {
                    final ObjectOutputStream out = new ObjectOutputStream(fromTheEngine);
                    out.flush();
                    final ObjectInputStream in = new ObjectInputStream(toTheEngine);
                    Util.forwardExecutionControl(new DirectExecutionControl(), in, out);
                } catch (Throwable e) {
                    engineFailure[0] = e;
                }
            }
        }, "engine");
        engine.setDaemon(true);
        engine.start();

        final ObjectOutputStream out = new ObjectOutputStream(fromJShell);
        out.flush();
        final ObjectInputStream in = new ObjectInputStream(toJShell);
        final ExecutionControl ec = new StreamingExecutionControl(out, in);

        final java.util.List<String> a = new java.util.ArrayList<String>();
        try {
            ec.load(snippet());
            a.add("load|ok");
        } catch (Throwable e) {
            a.add("load|" + e.getClass().getName() + "|" + e.getMessage());
        }
        final String[] methods = {"f", "g", "nothing"};
        for (int i = 0; i < methods.length; i++) {
            try {
                a.add("invoke-" + methods[i] + "|" + ec.invoke("JSH1x", methods[i]));
            } catch (Throwable e) {
                a.add("invoke-" + methods[i] + "|" + e.getClass().getName());
            }
        }
        final String[] vars = {"V", "T"};
        for (int i = 0; i < vars.length; i++) {
            try {
                a.add("var-" + vars[i] + "|" + ec.varValue("JSH1x", vars[i]));
            } catch (Throwable e) {
                a.add("var-" + vars[i] + "|" + e.getClass().getName());
            }
        }
        // The user's exception has to cross the protocol taken apart and arrive whole.
        try {
            ec.invoke("JSH1x", "blowUp");
            a.add("exception|no error");
        } catch (ExecutionControl.UserException e) {
            // The stack trace is not part of the comparison: this VM does not capture the stack
            // natively --it is said in `java/lang/Throwable.java`-- so here it comes out empty and
            // in the JDK it does not. That the array travels whole is what does depend on this
            // package, and that is checked in traceTravels().
            a.add("exception|" + e.causeExceptionClass() + "|" + e.getMessage()
                    + "|trace-not-null=" + (e.getStackTrace() != null));
        } catch (Throwable e) {
            a.add("exception|" + e.getClass().getName() + "|" + e.getMessage());
        }
        // And what the engine on the far side declares it does not support.
        try {
            ec.redefine(snippet());
            a.add("redefine|no error");
        } catch (Throwable e) {
            a.add("redefine|" + e.getClass().getName());
        }
        try {
            ec.extensionCommand("whatever", null);
            a.add("extension|no error");
        } catch (Throwable e) {
            a.add("extension|" + e.getClass().getName());
        }
        try {
            ec.addToClasspath(".");
            a.add("classpath|ok");
        } catch (Throwable e) {
            a.add("classpath|" + e.getClass().getName());
        }
        ec.close();
        engine.join(3000);
        a.add("engine-finished|" + !engine.isAlive());
        a.add("engine-no-failure|" + (engineFailure[0] == null));
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

    /**
     * That the stack trace array crosses the protocol with the length it was sent with.
     *
     * <p>One built by hand is sent, so as not to depend on the VM capturing the stack. What is being
     * tested is the protocol, not the capture.
     *
     * @return 0 if it arrived the same, or the code of what failed
     */
    public static int traceTravels() {
        try {
            final StackTraceElement[] t = {
                new StackTraceElement("A", "m", "A.java", 10),
                new StackTraceElement("B", "n", "B.java", 20),
            };
            final java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            final ObjectOutputStream oo = new ObjectOutputStream(bo);
            oo.writeObject(t);
            oo.flush();
            final ObjectInputStream oi = new ObjectInputStream(
                    new java.io.ByteArrayInputStream(bo.toByteArray()));
            final StackTraceElement[] v = (StackTraceElement[]) oi.readObject();
            if (v == null || v.length != 2) {
                return 1;
            }
            if (!v[0].getClassName().equals("A") || v[1].getLineNumber() != 20) {
                return 2;
            }
            return 0;
        } catch (Throwable e) {
            return 3;
        }
    }

    /**
     * Locates the difference in more detail: {@code index * 100 + field}.
     *
     * @return the location, or -1 if there is no difference
     */
    public static int detail() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        for (int i = 0; i < EXPECTED.length && i < a.length; i++) {
            if (a[i].equals(EXPECTED[i])) {
                continue;
            }
            final String[] x = a[i].split("[|]", -1);
            final String[] y = EXPECTED[i].split("[|]", -1);
            for (int j = 0; j < Math.max(x.length, y.length); j++) {
                if (j >= x.length || j >= y.length || !x[j].equals(y[j])) {
                    return i * 100 + j;
                }
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
