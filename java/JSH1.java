import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import jdk.jshell.execution.LocalExecutionControl;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControl.ClassBytecodes;

/**
 * Comprueba {@code jdk.jshell.execution} contra el JDK 25.
 *
 * <p>Hace lo que hace JShell: le pasa a un motor el bytecode de una clase ya compilada, le pide que
 * llame a sus metodos y que lea sus variables, y compara cada respuesta con la del JDK real.
 *
 * <p>El bytecode sale de {@code java/JSH1x.class}, que es un fragmento de mentira compilado de
 * antemano. Compilar de verdad seria meter el compilador en la prueba, y lo que se esta probando es
 * el motor de ejecucion.
 *
 * <p>{@link #donde()} devuelve el indice de la primera respuesta que no coincide, o -1: un entero
 * alcanza porque {@code run-headless} no vacia la consola.
 */
public class JSH1 {


    static final String[] ESPERADO = {
        "load|ok",
        "invoke-f|42",
        "invoke-g|\"eco\"",
        "invoke-nulo|null",
        "var-V|7",
        "var-T|\"hola\"",
        "excepcion|java.lang.IllegalStateException|a proposito",
        "metodo-inexistente|jdk.jshell.spi.ExecutionControl$InternalException",
        "var-inexistente|jdk.jshell.spi.ExecutionControl$InternalException",
        "clase-inexistente|jdk.jshell.spi.ExecutionControl$InternalException",
        "extension|jdk.jshell.spi.ExecutionControl$NotImplementedException",
        "stop-en-vacio|ok",
    };

    static String falla(Throwable e) {
        String m = e.getMessage();
        // Las trazas llevan numeros de linea y nombres de archivo que no tienen por que coincidir
        // entre las dos bibliotecas; lo que se compara es el tipo y el mensaje.
        return e.getClass().getName() + "|" + m;
    }

    static ClassBytecodes[] fragmento() throws IOException {
        final byte[] b = Files.readAllBytes(Paths.get("java/JSH1x.class"));
        return new ClassBytecodes[] {new ClassBytecodes("JSH1x", b)};
    }

    /** Lo que el motor contesta, una linea por pregunta. */
    static String[] actual() throws Exception {
        final java.util.List<String> a = new java.util.ArrayList<String>();
        final ExecutionControl ec = new LocalExecutionControl();

        try {
            ec.load(fragmento());
            a.add("load|ok");
        } catch (Throwable e) {
            a.add("load|" + falla(e));
        }

        String[][] llamadas = {
            {"f", "42"}, {"g", null}, {"nulo", null},
        };
        for (int i = 0; i < llamadas.length; i++) {
            try {
                a.add("invoke-" + llamadas[i][0] + "|" + ec.invoke("JSH1x", llamadas[i][0]));
            } catch (Throwable e) {
                a.add("invoke-" + llamadas[i][0] + "|" + falla(e));
            }
        }

        String[] vars = {"V", "T"};
        for (int i = 0; i < vars.length; i++) {
            try {
                a.add("var-" + vars[i] + "|" + ec.varValue("JSH1x", vars[i]));
            } catch (Throwable e) {
                a.add("var-" + vars[i] + "|" + falla(e));
            }
        }

        // Una excepcion del usuario tiene que llegar convertida, no cruda.
        try {
            ec.invoke("JSH1x", "revienta");
            a.add("excepcion|sin error");
        } catch (ExecutionControl.UserException e) {
            a.add("excepcion|" + e.causeExceptionClass() + "|" + e.getMessage());
        } catch (Throwable e) {
            a.add("excepcion|" + falla(e));
        }

        // Y las cosas que no existen.
        try {
            ec.invoke("JSH1x", "noExiste");
            a.add("metodo-inexistente|sin error");
        } catch (Throwable e) {
            a.add("metodo-inexistente|" + e.getClass().getName());
        }
        try {
            ec.varValue("JSH1x", "noExiste");
            a.add("var-inexistente|sin error");
        } catch (Throwable e) {
            a.add("var-inexistente|" + e.getClass().getName());
        }
        try {
            ec.invoke("NoHay", "f");
            a.add("clase-inexistente|sin error");
        } catch (Throwable e) {
            a.add("clase-inexistente|" + e.getClass().getName());
        }

        // Lo que este motor declara no soportar.
        try {
            ec.extensionCommand("loQueSea", null);
            a.add("extension|sin error");
        } catch (Throwable e) {
            a.add("extension|" + e.getClass().getName());
        }

        // Cortar sin nada corriendo no puede fallar: la peticion puede llegar entre medio.
        try {
            ec.stop();
            a.add("stop-en-vacio|ok");
        } catch (Throwable e) {
            a.add("stop-en-vacio|" + falla(e));
        }

        ec.close();
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
