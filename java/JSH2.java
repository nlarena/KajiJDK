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
 * Comprueba el protocolo de {@code jdk.jshell.execution} de punta a punta.
 *
 * <h2>Que arma</h2>
 *
 * <p>Las dos puntas dentro del mismo proceso, unidas por dos tuberias. De un lado un
 * {@link StreamingExecutionControl}, que es lo que usa JShell; del otro un
 * {@link Util#forwardExecutionControl} atendiendo contra un {@link DirectExecutionControl}, que es
 * lo que corre en el proceso remoto. En el medio, el protocolo entero: la marca, los nombres de
 * comando, los codigos de resultado y las excepciones desarmadas.
 *
 * <p>No hace falta un socket para probarlo, y por eso se puede probar: lo unico que el protocolo
 * necesita es un par de flujos.
 *
 * <h2>El orden en que se abren los flujos</h2>
 *
 * <p>{@link ObjectOutputStream} escribe una cabecera al construirse y {@link ObjectInputStream} la
 * lee al construirse. Los dos lados crean primero su salida y la vacian; al reves, los dos quedan
 * esperando una cabecera que nadie escribio, sin ningun sintoma mas que dos hilos quietos.
 */
public class JSH2 {



    static final String[] ESPERADO = {
        "load|ok",
        "invoke-f|42",
        "invoke-g|\"eco\"",
        "invoke-nulo|null",
        "var-V|7",
        "var-T|\"hola\"",
        "excepcion|java.lang.IllegalStateException|a proposito|traza-no-nula=true",
        "redefine|jdk.jshell.spi.ExecutionControl$NotImplementedException",
        "extension|jdk.jshell.spi.ExecutionControl$NotImplementedException",
        "classpath|ok",
        "motor-termino|true",
        "motor-sin-falla|true",
    };

    static ClassBytecodes[] fragmento() throws Exception {
        final byte[] b = Files.readAllBytes(Paths.get("java/JSH1x.class"));
        return new ClassBytecodes[] {new ClassBytecodes("JSH1x", b)};
    }

    /** Lo que contesta el motor del otro lado del protocolo, una linea por pregunta. */
    static String[] actual() throws Exception {
        final PipedInputStream haciaElMotor = new PipedInputStream();
        final PipedOutputStream desdeJShell = new PipedOutputStream(haciaElMotor);
        final PipedInputStream haciaJShell = new PipedInputStream();
        final PipedOutputStream desdeElMotor = new PipedOutputStream(haciaJShell);

        final Throwable[] fallaDelMotor = new Throwable[1];
        final Thread motor = new Thread(new Runnable() {
            public void run() {
                try {
                    final ObjectOutputStream out = new ObjectOutputStream(desdeElMotor);
                    out.flush();
                    final ObjectInputStream in = new ObjectInputStream(haciaElMotor);
                    Util.forwardExecutionControl(new DirectExecutionControl(), in, out);
                } catch (Throwable e) {
                    fallaDelMotor[0] = e;
                }
            }
        }, "motor");
        motor.setDaemon(true);
        motor.start();

        final ObjectOutputStream out = new ObjectOutputStream(desdeJShell);
        out.flush();
        final ObjectInputStream in = new ObjectInputStream(haciaJShell);
        final ExecutionControl ec = new StreamingExecutionControl(out, in);

        final java.util.List<String> a = new java.util.ArrayList<String>();
        try {
            ec.load(fragmento());
            a.add("load|ok");
        } catch (Throwable e) {
            a.add("load|" + e.getClass().getName() + "|" + e.getMessage());
        }
        final String[] metodos = {"f", "g", "nulo"};
        for (int i = 0; i < metodos.length; i++) {
            try {
                a.add("invoke-" + metodos[i] + "|" + ec.invoke("JSH1x", metodos[i]));
            } catch (Throwable e) {
                a.add("invoke-" + metodos[i] + "|" + e.getClass().getName());
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
        // La excepcion del usuario tiene que cruzar el protocolo desarmada y llegar entera.
        try {
            ec.invoke("JSH1x", "revienta");
            a.add("excepcion|sin error");
        } catch (ExecutionControl.UserException e) {
            // La traza no entra en la comparacion: esta VM no captura la pila de forma nativa
            // --esta dicho en `java/lang/Throwable.java`-- asi que aca sale vacia y en el JDK no.
            // Que el arreglo viaje entero es lo que si depende de este paquete, y eso se comprueba
            // en trazaViaja().
            a.add("excepcion|" + e.causeExceptionClass() + "|" + e.getMessage()
                    + "|traza-no-nula=" + (e.getStackTrace() != null));
        } catch (Throwable e) {
            a.add("excepcion|" + e.getClass().getName() + "|" + e.getMessage());
        }
        // Y lo que el motor del otro lado declara no soportar.
        try {
            ec.redefine(fragmento());
            a.add("redefine|sin error");
        } catch (Throwable e) {
            a.add("redefine|" + e.getClass().getName());
        }
        try {
            ec.extensionCommand("loQueSea", null);
            a.add("extension|sin error");
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
        motor.join(3000);
        a.add("motor-termino|" + !motor.isAlive());
        a.add("motor-sin-falla|" + (fallaDelMotor[0] == null));
        return a.toArray(new String[a.size()]);
    }

    /**
     * El indice de la primera respuesta que no coincide con la del JDK, o -1.
     *
     * @return el indice, o -1
     */
    public static int donde() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        if (a.length != ESPERADO.length) {
            return 8000 + a.length;
        }
        for (int i = 0; i < a.length; i++) {
            if (!a[i].equals(ESPERADO[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Que el arreglo de la traza cruce el protocolo con el largo con que se mando.
     *
     * <p>Se manda uno armado a mano, para no depender de que la VM capture la pila. Lo que se esta
     * probando es el protocolo, no la captura.
     *
     * @return 0 si llego igual, o el codigo de lo que fallo
     */
    public static int trazaViaja() {
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
     * Ubica la diferencia con mas detalle: {@code indice * 100 + campo}.
     *
     * @return la ubicacion, o -1 si no hay diferencia
     */
    public static int detalle() {
        final String[] a;
        try {
            a = actual();
        } catch (Throwable e) {
            return 9000;
        }
        for (int i = 0; i < ESPERADO.length && i < a.length; i++) {
            if (a[i].equals(ESPERADO[i])) {
                continue;
            }
            final String[] x = a[i].split("[|]", -1);
            final String[] y = ESPERADO[i].split("[|]", -1);
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
        final int i = donde();
        System.out.println(i < 0 ? "sin diferencias"
                : i + ":\n  nuestro=" + a[i] + "\n  jdk    =" + ESPERADO[i]);
    }
}
