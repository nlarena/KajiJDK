import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * `ProcessBuilder.start()` y `startPipeline()`: lanzar procesos de verdad.
 *
 * <p>Estos dos metodos no existian hasta hace un rato, y no por olvido: la VM no tenia con que lanzar
 * un proceso, asi que un `Process` que no representara ninguno habria sido un miembro que miente. Lo
 * que se levanto fue el sustrato --`jdk.internal.proc.Proc`, nueve nativos-- y recien despues se
 * escribieron los metodos.
 *
 * <p>Por eso esta prueba **no comprueba que los metodos existan** sino que hacen lo que prometen: que
 * el hijo corre, que su salida llega, que su codigo de salida es el que dio, que `exitValue()` tira
 * mientras sigue vivo, y que lo que se le escribe por la entrada lo recibe.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25, corriendo **su** `ProcessBuilder`. Eso lo vuelve
 * un oraculo: los numeros esperados no los inventamos, los dicta el JDK.
 *
 * <p>Todos los comandos son `cmd.exe /c ...`, que es lo que hay en esta plataforma. Si algun dia esto
 * corre en otra, la lista de comandos es lo unico que cambia.
 */
public class ProcSpawnTest {

    static int fallas = 0;

    static void ok(String que, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + que);
            fallas = fallas + 1;
        }
    }

    static ProcessBuilder cmd(String linea) {
        List<String> c = new ArrayList<String>();
        c.add("cmd.exe");
        c.add("/c");
        c.add(linea);
        return new ProcessBuilder(c);
    }

    /** Todo lo que el proceso escribio en ese flujo, como texto sin espacios en los bordes. */
    static String drenar(InputStream in) throws Exception {
        byte[] buf = new byte[4096];
        int usados = 0;
        int n = in.read(buf, usados, buf.length - usados);
        while (n > 0) {
            usados = usados + n;
            if (usados >= buf.length) {
                break;
            }
            n = in.read(buf, usados, buf.length - usados);
        }
        return new String(buf, 0, usados, StandardCharsets.UTF_8).trim();
    }

    public static int run() throws Exception {
        fallas = 0;

        // ---- lo basico: corre, su salida llega, su codigo se sabe
        Process p = ProcSpawnTest.cmd("echo marca-uno").start();
        String salida = ProcSpawnTest.drenar(p.getInputStream());
        int codigo = p.waitFor();
        ok("la salida del hijo llega", "marca-uno".equals(salida));
        ok("el codigo de salida es 0", codigo == 0);
        ok("y despues no esta vivo", !p.isAlive());
        ok("exitValue coincide con waitFor", p.exitValue() == 0);
        // `waitFor` se puede llamar todas las veces que uno quiera y siempre da lo mismo. No es un
        // detalle: la primera version fallaba acá porque el `wait` de abajo solo se puede llamar una.
        ok("waitFor es idempotente", p.waitFor() == 0);

        // ---- un codigo de salida distinto de cero se reporta tal cual
        Process malo = ProcSpawnTest.cmd("exit 3").start();
        ok("un exit 3 se reporta como 3", malo.waitFor() == 3);

        // ---- la entrada del hijo
        Process eco = ProcSpawnTest.cmd("more").start();
        OutputStream hacia = eco.getOutputStream();
        hacia.write("desde-el-padre".getBytes(StandardCharsets.UTF_8));
        // Cerrar la entrada es como se le dice "no viene mas": sin esto `more` espera para siempre.
        hacia.close();
        String vuelta = ProcSpawnTest.drenar(eco.getInputStream());
        eco.waitFor();
        ok("lo que se le escribe al hijo, el hijo lo ve", vuelta.contains("desde-el-padre"));

        // ---- exitValue mientras sigue vivo TIRA, no espera
        Process lento = ProcSpawnTest.cmd("ping -n 3 127.0.0.1 > nul").start();
        boolean tiro = false;
        try {
            lento.exitValue();
        } catch (IllegalThreadStateException e) {
            tiro = true;
        }
        ok("exitValue tira mientras sigue vivo", tiro);
        ok("y isAlive dice que si", lento.isAlive());
        ok("el pid es positivo", lento.pid() > 0L);
        lento.destroy();
        lento.waitFor();
        ok("despues de destroy no esta vivo", !lento.isAlive());

        // ---- redirigir la salida a un archivo
        File archivo = new File("procspawn-salida.tmp");
        if (archivo.exists()) {
            archivo.delete();
        }
        Process aArchivo = ProcSpawnTest.cmd("echo al-archivo")
                .redirectOutput(archivo).start();
        aArchivo.waitFor();
        ok("el archivo se escribio", archivo.exists() && archivo.length() > 0L);
        String delArchivo = new String(java.nio.file.Files.readAllBytes(archivo.toPath()),
                StandardCharsets.UTF_8).trim();
        ok("y tiene lo que el hijo imprimio", "al-archivo".equals(delArchivo));
        archivo.delete();

        // ---- redirectErrorStream: el error va por la misma tuberia que la salida
        Process unido = ProcSpawnTest.cmd("echo normal & echo aError 1>&2")
                .redirectErrorStream(true).start();
        String juntos = ProcSpawnTest.drenar(unido.getInputStream());
        unido.waitFor();
        ok("con redirectErrorStream salen los dos por la salida",
                juntos.contains("normal") && juntos.contains("aError"));

        // ---- sin unir, el error sale por su propio flujo
        Process separado = ProcSpawnTest.cmd("echo aError 1>&2").start();
        String porError = ProcSpawnTest.drenar(separado.getErrorStream());
        separado.waitFor();
        ok("sin unir, el error sale por getErrorStream", porError.contains("aError"));

        // ---- descartar
        Process descartado = ProcSpawnTest.cmd("echo nadie-lo-lee")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        ok("descartar no rompe nada", descartado.waitFor() == 0);

        // ---- el directorio de trabajo
        Process enDir = ProcSpawnTest.cmd("cd").directory(new File(".")).start();
        String donde = ProcSpawnTest.drenar(enDir.getInputStream());
        enDir.waitFor();
        ok("el directorio de trabajo se respeta", donde.length() > 0);

        // ---- el builder se puede reusar y cada start() da un proceso nuevo
        ProcessBuilder reusable = ProcSpawnTest.cmd("echo dos-veces");
        Process a = reusable.start();
        Process b = reusable.start();
        ok("dos procesos distintos", a.pid() != b.pid());
        ok("los dos dan lo mismo", "dos-veces".equals(ProcSpawnTest.drenar(a.getInputStream()))
                && "dos-veces".equals(ProcSpawnTest.drenar(b.getInputStream())));
        a.waitFor();
        b.waitFor();

        // ---- validaciones de start()
        boolean vacio = false;
        try {
            new ProcessBuilder(new ArrayList<String>()).start();
        } catch (IndexOutOfBoundsException e) {
            vacio = true;
        }
        ok("un comando vacio es IndexOutOfBounds", vacio);

        boolean inexistente = false;
        try {
            List<String> c = new ArrayList<String>();
            c.add("kaji-no-existe-este-programa.exe");
            new ProcessBuilder(c).start();
        } catch (java.io.IOException e) {
            inexistente = true;
        }
        ok("un ejecutable que no existe es IOException", inexistente);

        // ---- la cadena
        List<ProcessBuilder> cadena = new ArrayList<ProcessBuilder>();
        cadena.add(ProcSpawnTest.cmd("echo por-la-cadena"));
        cadena.add(ProcSpawnTest.cmd("more"));
        List<Process> ps = ProcessBuilder.startPipeline(cadena);
        ok("la cadena devuelve un proceso por builder", ps.size() == 2);
        String finDeCadena = ProcSpawnTest.drenar(ps.get(1).getInputStream());
        for (Process q : ps) {
            q.waitFor();
        }
        ok("lo que entra por el primero sale por el ultimo",
                finDeCadena.contains("por-la-cadena"));

        // Una cadena vacia **no tira**: devuelve una lista vacia. Lo dice el JDK 25 medido, no su
        // javadoc viejo -- esta comprobacion fallo primero contra `java` real y ahi se corrigio la
        // expectativa, que es para lo que sirve tener un oraculo.
        ok("una cadena vacia devuelve vacio",
                ProcessBuilder.startPipeline(new ArrayList<ProcessBuilder>()).isEmpty());

        if (fallas == 0) {
            return -1;
        }
        return fallas;
    }

    public static void main(String[] a) throws Exception {
        System.out.println("ProcSpawnTest " + ProcSpawnTest.run());
    }
}
