package jdk.internal.vm;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's jdk.internal.vm.ThreadDumper — vuelca los hilos vivos, en texto o en JSON.
 *
 * <p>Es lo que hay detrás de `jcmd Thread.dump_to_file`. Las dos formas no son la misma cosa escrita
 * distinto: la de texto está pensada para que la lea una persona, y la de JSON para que la lea una
 * herramienta, con el árbol de contenedores explícito.
 *
 * <p>Los hilos se enumeran recorriendo el `ThreadGroup` raíz, que es lo que hay: esta VM no lleva un
 * registro central de hilos vivos.
 *
 * <p><strong>Y hoy ese recorrido no encuentra nada.</strong> En esta VM `ThreadGroup` no lleva
 * registro de sus miembros: `activeCount()` devuelve 0 y `enumerate()` no escribe nada, aun con
 * hilos corriendo (repro en `scratchpad/zz350/A5.java`). Así que el volcado trae **sólo el hilo que
 * lo pide**, que se agrega aparte y a mano —igual que en
 * {@link ThreadContainers#root()}, y por la misma razón: un volcado que dice que no hay ningún hilo
 * vivo mientras alguien lo está pidiendo no es un volcado incompleto, es uno equivocado—. El día que
 * `ThreadGroup` lleve registro, esto empieza a traer a todos sin tocar nada.
 *
 * <p><strong>Las pilas van vacías.</strong> {@link Thread#getStackTrace} sobre **otro** hilo necesita
 * que la VM lo pare en un punto seguro y lea sus cuadros, y esta VM no expone eso. Cada hilo aparece
 * con su nombre, su estado y su identidad; el arreglo de cuadros queda vacío en vez de traer la pila
 * del hilo equivocado, que es el error que haría inútil al volcado entero.
 */
public class ThreadDumper {

    private ThreadDumper() {
    }

    /**
     * Vuelca en texto a un archivo.
     *
     * @param outputFile a dónde escribir
     * @param okToOverwrite si se puede pisar un archivo que ya está
     * @return el mensaje de respuesta, que es lo que `jcmd` le muestra al usuario
     */
    public static byte[] dumpThreads(String outputFile, boolean okToOverwrite) {
        return ThreadDumper.aArchivo(outputFile, okToOverwrite, ThreadDumper.textoDeHilos());
    }

    /** Vuelca en JSON a un archivo. */
    public static byte[] dumpThreadsToJson(String outputFile, boolean okToOverwrite) {
        return ThreadDumper.aArchivo(outputFile, okToOverwrite, ThreadDumper.jsonDeHilos());
    }

    /** Vuelca en texto a un flujo. */
    public static void dumpThreads(OutputStream out) throws IOException {
        out.write(ThreadDumper.textoDeHilos().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /** Vuelca en JSON a un flujo. */
    public static void dumpThreadsToJson(OutputStream out) throws IOException {
        out.write(ThreadDumper.jsonDeHilos().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    // El error se devuelve como mensaje y no como excepcion porque el que llama es `jcmd` desde
    // afuera del proceso: lo unico que puede hacer con una falla es mostrarsela al usuario.
    private static byte[] aArchivo(String outputFile, boolean okToOverwrite, String contenido) {
        if (outputFile == null) {
            return "no se indico archivo de salida".getBytes(StandardCharsets.UTF_8);
        }
        Path p = Path.of(outputFile);
        if (!okToOverwrite && Files.exists(p)) {
            return ("el archivo ya existe: " + outputFile).getBytes(StandardCharsets.UTF_8);
        }
        try {
            Files.write(p, contenido.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ("no se pudo escribir " + outputFile + ": " + e).getBytes(StandardCharsets.UTF_8);
        }
        return ("volcado escrito en " + outputFile).getBytes(StandardCharsets.UTF_8);
    }

    // Se garantiza al menos el hilo que pregunta, igual que `ThreadContainers.ContenedorRaiz.threads()`
    // y por la misma razon: en esta VM `ThreadGroup` no lleva registro de sus miembros --`activeCount()`
    // da 0 y `enumerate()` no devuelve nada aun con hilos corriendo--, asi que sin esta garantia el
    // recorrido daba una lista **vacia** y el volcado salia sin un solo hilo mientras alguien lo estaba
    // pidiendo. Un volcado que dice que no hay hilos vivos no es un volcado parcial, es uno que miente.
    private static List<Thread> vivos() {
        ThreadGroup g = Thread.currentThread().getThreadGroup();
        while (g != null && g.getParent() != null) {
            g = g.getParent();
        }
        List<Thread> out = new ArrayList<Thread>();
        if (g != null) {
            // Con holgura: entre contar y enumerar pueden aparecer hilos, y un arreglo justo los
            // perderia.
            Thread[] buf = new Thread[g.activeCount() + 16];
            int n = g.enumerate(buf, true);
            for (int i = 0; i < n; i++) {
                if (buf[i] != null) {
                    out.add(buf[i]);
                }
            }
        }
        Thread yo = Thread.currentThread();
        if (!out.contains(yo)) {
            out.add(yo);
        }
        return out;
    }

    private static String textoDeHilos() {
        StringBuilder sb = new StringBuilder();
        sb.append("Volcado de hilos - ").append(System.currentTimeMillis()).append('\n');
        for (Thread t : ThreadDumper.vivos()) {
            sb.append('\n').append('"').append(t.getName()).append('"')
              .append(" #").append(t.threadId())
              .append(t.isDaemon() ? " daemon" : "")
              .append(" prio=").append(t.getPriority())
              .append('\n')
              .append("   java.lang.Thread.State: ").append(t.getState()).append('\n');
        }
        return sb.toString();
    }

    // JSON escrito a mano y no con una biblioteca: el volcado tiene que poder salir cuando el proceso
    // ya esta en problemas, y ese no es momento de cargar clases nuevas ni de asignar de mas.
    private static String jsonDeHilos() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"threadDump\": {\n");
        sb.append("    \"time\": \"").append(System.currentTimeMillis()).append("\",\n");
        sb.append("    \"runtimeVersion\": \"")
          .append(ThreadDumper.escapar(System.getProperty("java.version", "desconocida")))
          .append("\",\n");
        sb.append("    \"threadContainers\": [\n      {\n");
        sb.append("        \"container\": \"<root>\",\n");
        sb.append("        \"parent\": null,\n");
        sb.append("        \"threads\": [\n");
        List<Thread> hilos = ThreadDumper.vivos();
        for (int i = 0; i < hilos.size(); i++) {
            Thread t = hilos.get(i);
            sb.append("          {\n");
            sb.append("            \"tid\": \"").append(t.threadId()).append("\",\n");
            sb.append("            \"name\": \"").append(ThreadDumper.escapar(t.getName())).append("\",\n");
            sb.append("            \"state\": \"").append(t.getState()).append("\",\n");
            sb.append("            \"stack\": []\n");
            sb.append("          }").append(i + 1 < hilos.size() ? "," : "").append('\n');
        }
        sb.append("        ]\n      }\n    ]\n  }\n}\n");
        return sb.toString();
    }

    // Un nombre de hilo lo elige el programa: puede traer comillas o barras y romper el JSON.
    private static String escapar(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < ' ') {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
