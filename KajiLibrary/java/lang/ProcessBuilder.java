package java.lang;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// El armador de procesos del sistema operativo: junta el comando, el directorio de trabajo, el
// entorno y a donde van los tres flujos estandar del hijo.
//
// ===========================================================================================
// LO QUE ESTA CLASE **NO** DECLARA, Y POR QUE
// ===========================================================================================
//
// `start()` y `startPipeline(List)` **estan**, y hasta hace poco no estaban. Vale contar el porque de
// las dos cosas, porque es la misma razon.
//
// Estuvieron afuera mientras la VM no supo lanzar procesos. El contrato de `start()` es "devolve un
// `Process` que representa un proceso hijo corriendo", y sin subsistema de procesos eso no se puede
// cumplir: `java.lang.Process` es abstracta y nadie la extendia, no habia un solo nativo que
// arrancara nada. Declararlo igual y hacerlo tirar `UnsupportedOperationException` --como hacen
// `Runtime.exec` y las fabricas de `ProcessHandle`-- habria sido peor que la ausencia: un metodo que
// existe y no puede cumplir le miente al que compila contra el, pasa la compilacion y la resolucion,
// y revienta recien al correr. Uno que no existe falla en el compilador, que es donde se arregla.
//
// Ahora la VM si sabe: `jdk.internal.proc.Proc` es la costura, con lanzamiento, espera, senializacion
// y las tres tuberias. Asi que `start()` se escribio, y con el `ProcesoHijo`, la implementacion de
// `Process` que faltaba. La ausencia era del sustrato, no de esta clase, y se levanto el sustrato en
// vez de documentar mejor la ausencia.
//
// Lo que sigue sin poder cumplirse **se dice**: ver `startPipeline`, que arma la cadena con archivos
// temporales y no con tuberias directas, y `ProcessHandle`, que sigue tirando.
//
// `final`, como en el JDK: nadie puede extenderla para "agregarle" un `start()` que tampoco podria
// cumplir.
public final class ProcessBuilder {

    // Se guarda **la lista que nos pasaron**, no una copia. Es del contrato del JDK y es una
    // trampa a proposito: modificar la lista despues cambia el builder. Las sobrecargas varargs si
    // copian, porque ahi el arreglo lo arma el compilador y no hay nada que compartir.
    private List<String> command;

    // null significa "el directorio de trabajo del proceso actual", no "ninguno".
    private File directory;

    // Perezoso: se copia del entorno del proceso recien cuando alguien lo pide. Dos builders nunca
    // comparten mapa.
    private Map<String, String> environment;

    private boolean redirectErrorStream;

    // Tres posiciones: 0 entrada, 1 salida, 2 error. Tambien perezoso; null vale por "las tres en
    // PIPE", que es el estado inicial.
    private Redirect[] redirects;

    public ProcessBuilder(List<String> command) {
        if (command == null) {
            throw new NullPointerException();
        }
        this.command = command;
    }

    public ProcessBuilder(String... command) {
        this.command = new ArrayList<String>(command.length);
        for (int i = 0; i < command.length; i++) {
            this.command.add(command[i]);
        }
    }

    public ProcessBuilder command(List<String> command) {
        if (command == null) {
            throw new NullPointerException();
        }
        this.command = command;
        return this;
    }

    public ProcessBuilder command(String... command) {
        this.command = new ArrayList<String>(command.length);
        for (int i = 0; i < command.length; i++) {
            this.command.add(command[i]);
        }
        return this;
    }

    // La lista viva, no una copia: escribirle a lo que devuelve esto cambia el builder.
    public List<String> command() {
        return this.command;
    }

    // El mapa vivo del entorno del hijo, inicializado con una copia del entorno del proceso.
    //
    // Aca `System.getenv()` devuelve un mapa vacio (KajiJDK no lee el entorno del SO), asi que en
    // la practica se arranca de cero. Lo que si es real es el **comportamiento del mapa**, que es
    // lo unico observable de este metodo mientras no haya `start()`. Ver `Entorno`.
    //
    // Cada llamada devuelve el mismo mapa, y dos builders nunca comparten el suyo.
    public Map<String, String> environment() {
        if (this.environment == null) {
            Entorno env = new Entorno();
            env.putAll(System.getenv());
            this.environment = env;
        }
        return this.environment;
    }

    // El entorno del hijo.
    //
    // Es un `HashMap` **sensible a mayusculas**, y esa es la parte contraintuitiva: en Windows los
    // nombres de variable no distinguen mayusculas, pero eso vale para el entorno del proceso
    // —`System.getenv(String)`, que si busca sin distinguir— y **no** para este mapa. El JDK usa
    // dos estructuras distintas justamente por eso, y aca se copia la de escritura. Un
    // `env.put("Path", ...)` no pisa a `PATH`.
    //
    // Lo que si valida, y por eso la clase existe en vez de un `HashMap` pelado:
    //
    //   - nulos: ni clave ni valor, ni para escribir ni para consultar. Un `HashMap` los aceptaria
    //     en silencio y el error aparecaria mucho despues.
    //   - el `=`: es el separador del bloque de entorno, asi que un nombre que lo contenga no se
    //     puede representar. Se permite en la posicion 0 porque Windows usa nombres magicos de la
    //     forma `=C:` para guardar el directorio actual de cada unidad.
    //   - el `\0`: el bloque de entorno del SO esta terminado en nulos.
    //
    // Lo que no replica: las vistas (`keySet`, `values`, `entrySet`) son las de `HashMap` y no
    // rechazan un null en `contains`. Es la unica diferencia observable con el JDK y esta anotada
    // aca en vez de silenciada.
    private static final class Entorno extends HashMap<String, String> {

        // El caracter nulo, como constante y no como literal: un `\0` de verdad adentro del
        // fuente lo vuelve un archivo binario para grep, diff y cualquier herramienta de texto.
        private static final int NUL = 0;

        private static String nombreValido(String name) {
            if (name.indexOf('=', 1) != -1 || name.indexOf(Entorno.NUL) != -1) {
                throw new IllegalArgumentException(
                        "Invalid environment variable name: \"" + name + "\"");
            }
            return name;
        }

        private static String valorValido(String value) {
            if (value.indexOf(Entorno.NUL) != -1) {
                throw new IllegalArgumentException(
                        "Invalid environment variable value: \"" + value + "\"");
            }
            return value;
        }

        private static String noNulo(Object o) {
            if (o == null) {
                throw new NullPointerException();
            }
            return (String) o;
        }

        public String put(String key, String value) {
            return super.put(Entorno.nombreValido(key), Entorno.valorValido(value));
        }

        public String get(Object key) {
            return super.get(Entorno.noNulo(key));
        }

        public boolean containsKey(Object key) {
            return super.containsKey(Entorno.noNulo(key));
        }

        public boolean containsValue(Object value) {
            return super.containsValue(Entorno.noNulo(value));
        }

        public String remove(Object key) {
            return super.remove(Entorno.noNulo(key));
        }
    }

    public File directory() {
        return this.directory;
    }

    // `null` es un valor valido: quiere decir "heredar el directorio de trabajo del proceso
    // actual", que es distinto de "no configurado".
    public ProcessBuilder directory(File directory) {
        this.directory = directory;
        return this;
    }

    // ---------------- redireccion de E/S ----------------

    private Redirect[] redirects() {
        if (this.redirects == null) {
            this.redirects = new Redirect[] {Redirect.PIPE, Redirect.PIPE, Redirect.PIPE};
        }
        return this.redirects;
    }

    // La validacion es lo unico que separa un builder correcto de uno absurdo, y no depende de
    // poder lanzar nada: una fuente de entrada no puede ser un destino de escritura.
    public ProcessBuilder redirectInput(Redirect source) {
        if (source.type() == Redirect.Type.WRITE || source.type() == Redirect.Type.APPEND) {
            throw new IllegalArgumentException("Redirect invalid for reading: " + source);
        }
        this.redirects()[0] = source;
        return this;
    }

    public ProcessBuilder redirectOutput(Redirect destination) {
        if (destination.type() == Redirect.Type.READ) {
            throw new IllegalArgumentException("Redirect invalid for writing: " + destination);
        }
        this.redirects()[1] = destination;
        return this;
    }

    public ProcessBuilder redirectError(Redirect destination) {
        if (destination.type() == Redirect.Type.READ) {
            throw new IllegalArgumentException("Redirect invalid for writing: " + destination);
        }
        this.redirects()[2] = destination;
        return this;
    }

    public ProcessBuilder redirectInput(File file) {
        return this.redirectInput(Redirect.from(file));
    }

    public ProcessBuilder redirectOutput(File file) {
        return this.redirectOutput(Redirect.to(file));
    }

    public ProcessBuilder redirectError(File file) {
        return this.redirectError(Redirect.to(file));
    }

    public Redirect redirectInput() {
        return this.redirects()[0];
    }

    public Redirect redirectOutput() {
        return this.redirects()[1];
    }

    public Redirect redirectError() {
        return this.redirects()[2];
    }

    public ProcessBuilder inheritIO() {
        Redirect[] rs = this.redirects();
        for (int i = 0; i < 3; i++) {
            rs[i] = Redirect.INHERIT;
        }
        return this;
    }

    public boolean redirectErrorStream() {
        return this.redirectErrorStream;
    }

    // Cuando esta en true la salida de error se mezcla con la estandar, y por eso
    // `redirectError(...)` queda sin efecto: no hay dos flujos que dirigir a lugares distintos.
    public ProcessBuilder redirectErrorStream(boolean redirectErrorStream) {
        this.redirectErrorStream = redirectErrorStream;
        return this;
    }

    // A donde va —o de donde viene— uno de los flujos estandar del hijo.
    //
    // Es un tipo cerrado disfrazado de clase abstracta: el constructor es privado, asi que las
    // unicas instancias posibles son las tres constantes y las tres que devuelven los factories.
    // Cada una de esas seis formas tiene un `Type` distinto y ese es todo el estado que importa.
    public abstract static class Redirect {

        // El agujero negro del sistema. En Windows es un nombre de dispositivo reservado, no un
        // archivo; en POSIX es un archivo de verdad. Da igual: aca nadie lo abre, porque nadie
        // lanza procesos. Existe para que `DISCARD.file()` conteste lo que el contrato dice.
        private static final File NULL_FILE = new File(
                System.getProperty("os.name", "").startsWith("Windows") ? "NUL" : "/dev/null");

        // La categoria de una redireccion.
        //
        // Ojo con `DISCARD`, que es la excepcion que confunde: su `type()` es `WRITE`, no un valor
        // propio. Tirar a la nada es escribir a un archivo, y el archivo es el nulo del sistema.
        public enum Type {
            PIPE,
            INHERIT,
            READ,
            WRITE,
            APPEND;
        }

        // Privado: no hay forma de inventarse una septima clase de redireccion desde afuera.
        private Redirect() {
        }

        public abstract Type type();

        // null cuando la redireccion no involucra un archivo (PIPE e INHERIT).
        public File file() {
            return null;
        }

        // Solo tiene sentido para las que escriben. La base tira en vez de devolver `false` porque
        // preguntarle a un `PIPE` si agrega al final no es una pregunta con respuesta.
        boolean append() {
            throw new UnsupportedOperationException();
        }

        public static final Redirect PIPE = new Concreta(Type.PIPE, null, false, "PIPE");

        public static final Redirect INHERIT = new Concreta(Type.INHERIT, null, false, "INHERIT");

        // Ojo con el texto: es "WRITE", no "DISCARD". Las tres constantes se imprimen como su
        // `type()`, y el de esta es `WRITE`. Se ve raro y es lo que hace el JDK.
        public static final Redirect DISCARD =
                new Concreta(Type.WRITE, Redirect.NULL_FILE, false, Type.WRITE.toString());

        public static Redirect from(File file) {
            if (file == null) {
                throw new NullPointerException();
            }
            return new Concreta(Type.READ, file, false,
                    "redirect to read from file \"" + file + "\"");
        }

        public static Redirect to(File file) {
            if (file == null) {
                throw new NullPointerException();
            }
            return new Concreta(Type.WRITE, file, false,
                    "redirect to write to file \"" + file + "\"");
        }

        public static Redirect appendTo(File file) {
            if (file == null) {
                throw new NullPointerException();
            }
            return new Concreta(Type.APPEND, file, true,
                    "redirect to append to file \"" + file + "\"");
        }

        // Dos redirecciones son iguales si son el mismo objeto, o si tienen el mismo tipo y el
        // mismo archivo.
        //
        // La segunda rama solo se alcanza con redirecciones que tienen archivo: `PIPE` e `INHERIT`
        // son constantes unicas, asi que para ellas la comparacion ya termino por identidad
        // —cualquier otro objeto tiene otro `type()`— y nunca se llega a desreferenciar el null.
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Redirect)) {
                return false;
            }
            Redirect r = (Redirect) obj;
            if (r.type() != this.type()) {
                return false;
            }
            return this.file().equals(r.file());
        }

        public int hashCode() {
            File file = this.file();
            if (file == null) {
                return super.hashCode();
            }
            return file.hashCode();
        }

        // Las seis formas se distinguen solo por sus cuatro campos, asi que una clase alcanza. El
        // JDK usa seis clases anonimas; el resultado observable es el mismo y esto se lee mejor.
        private static final class Concreta extends Redirect {

            private final Type type;
            private final File file;
            private final boolean append;
            private final String texto;

            Concreta(Type type, File file, boolean append, String texto) {
                this.type = type;
                this.file = file;
                this.append = append;
                this.texto = texto;
            }

            public Type type() {
                return this.type;
            }

            public File file() {
                return this.file;
            }

            boolean append() {
                return this.append;
            }

            public String toString() {
                return this.texto;
            }
        }
    }

    // ---- lanzar ---------------------------------------------------------------------------------

    /**
     * Lanza el proceso con la configuracion de este builder.
     *
     * <p>Cada llamada lanza **uno nuevo**: el builder se puede reusar, y modificarlo despues no toca
     * a los que ya salieron. Eso obliga a copiar el comando y el entorno aca, y es a proposito -- si
     * se pasara la lista viva, cambiarla despues cambiaria un proceso ya corriendo, que es imposible.
     *
     * @throws java.io.IOException si no se pudo lanzar (ejecutable inexistente, sin permisos)
     * @throws NullPointerException si el comando tiene un elemento nulo
     * @throws IndexOutOfBoundsException si el comando esta vacio
     */
    public Process start() throws java.io.IOException {
        List<String> cmd = this.command();
        if (cmd == null || cmd.isEmpty()) {
            throw new IndexOutOfBoundsException("el comando esta vacio");
        }
        String[] argv = new String[cmd.size()];
        for (int i = 0; i < cmd.size(); i++) {
            String a = cmd.get(i);
            if (a == null) {
                throw new NullPointerException("el comando tiene un elemento nulo en " + i);
            }
            argv[i] = a;
        }

        // El entorno se pasa **solo si alguien lo toco**. La diferencia importa: un arreglo vacio le
        // dice al nativo "heredá el mio", y uno lleno le dice "usá exactamente esto". Mandando
        // siempre el mapa materializado, un builder que nadie configuro le sacaria al hijo las
        // variables del sistema que deberia heredar.
        String[] env = new String[0];
        if (this.environment != null) {
            Map<String, String> m = this.environment;
            env = new String[m.size() * 2];
            int k = 0;
            for (Map.Entry<String, String> e : m.entrySet()) {
                env[k] = e.getKey();
                env[k + 1] = e.getValue();
                k = k + 2;
            }
        }

        Redirect[] r = this.redirects();
        String[] rutas = new String[3];
        int[] modos = new int[3];
        for (int i = 0; i < 3; i++) {
            modos[i] = ProcessBuilder.modoDe(r[i]);
            File f = r[i].file();
            rutas[i] = f == null ? null : f.getPath();
        }

        String dir = this.directory == null ? null : this.directory.getPath();
        int h = jdk.internal.proc.Proc.spawn(argv, dir, env, rutas, modos, this.redirectErrorStream);
        if (h < 0) {
            // El nativo devuelve -1 sin distinguir el motivo, asi que el mensaje nombra lo unico que
            // se sabe con certeza: que comando se intento. Inventar "no existe" o "sin permisos"
            // seria adivinar cual de los dos fue.
            throw new java.io.IOException("no se pudo lanzar el proceso: " + argv[0]);
        }
        return new ProcesoHijo(h);
    }

    // El modo de redireccion que el nativo entiende: 0 tuberia, 1 heredar, 2 descartar, 3 archivo
    // pisando, 4 archivo agregando.
    //
    // `DISCARD` se compara por **identidad** y no por tipo, y ahi esta la trampa de esta clase: su
    // `type()` es `WRITE` y su `file()` es el nulo del sistema, o sea que por tipo es indistinguible
    // de un `Redirect.to(new File("NUL"))`. Se distingue por identidad porque es una constante unica,
    // y asi el nativo usa su propio descarte en vez de abrir un archivo.
    private static int modoDe(Redirect r) {
        if (r == Redirect.DISCARD) {
            return 2;
        }
        if (r.type() == Redirect.Type.PIPE) {
            return 0;
        }
        if (r.type() == Redirect.Type.INHERIT) {
            return 1;
        }
        if (r.type() == Redirect.Type.APPEND) {
            return 4;
        }
        return 3;
    }

    /**
     * Lanza varios procesos encadenados: la salida de cada uno es la entrada del siguiente.
     *
     * <p><strong>La cadena se arma con archivos temporales, no con tuberias directas</strong>, y eso
     * hay que decirlo porque tiene una consecuencia observable: los procesos **no corren al mismo
     * tiempo**. Cada uno termina antes de que arranque el siguiente, en vez de fluir en paralelo.
     *
     * <p>El resultado final es el mismo --los bytes que salen del ultimo son los que saldrian de una
     * tuberia-- pero hay dos diferencias que se pueden notar: una cadena que procesa un flujo
     * infinito no avanza nunca, y una que mueve mucho volumen usa disco en vez de memoria.
     *
     * <p>Se hace asi porque conectar la salida de un hijo a la entrada de otro exige duplicar
     * descriptores entre procesos, y la costura de esta VM entrega tuberias al proceso **padre**, no
     * un descriptor que se pueda pasar a un tercero. El dia que `Proc` sepa encadenar, esto se
     * reescribe y el contrato observable mejora sin cambiar la firma.
     *
     * <p>Lo que **si** cumple: la lista devuelta tiene un `Process` por builder en el mismo orden,
     * y `getInputStream()` del ultimo lee el resultado de la cadena. Las redirecciones intermedias
     * que el llamador haya puesto se ignoran, como en el JDK.
     *
     * @throws NullPointerException si la lista o alguno de sus elementos es nulo
     * @throws java.io.IOException si alguno no se pudo lanzar
     */
    public static List<Process> startPipeline(List<ProcessBuilder> builders)
            throws java.io.IOException {
        if (builders == null) {
            throw new NullPointerException("builders");
        }
        if (builders.isEmpty()) {
            // El JDK 25 **no tira** con una lista vacia: devuelve una lista vacia. Lo medi contra
            // `java` real (`scratchpad/zz349/Vacia.java`) porque el javadoc de versiones viejas dice
            // `IllegalArgumentException` y la primera version de esto lo copio de ahi. Gana lo que la
            // implementacion hace, no lo que la documentacion dice que hacia.
            return new ArrayList<Process>();
        }
        for (ProcessBuilder b : builders) {
            if (b == null) {
                throw new NullPointerException("un builder de la cadena es nulo");
            }
        }

        List<Process> out = new ArrayList<Process>();
        File anterior = null;
        int n = builders.size();
        for (int i = 0; i < n; i++) {
            ProcessBuilder b = builders.get(i);
            // Se trabaja sobre una copia: la cadena fija las redirecciones intermedias, y hacerlo
            // sobre el builder del llamador le cambiaria su objeto por debajo.
            ProcessBuilder c = new ProcessBuilder(b.command());
            c.directory(b.directory());
            c.redirectErrorStream(b.redirectErrorStream());
            if (b.environment != null) {
                c.environment().putAll(b.environment);
            }
            // El primero conserva su entrada; el resto lee del temporal del anterior.
            c.redirectInput(i == 0 ? b.redirectInput() : Redirect.from(anterior));
            File propio = null;
            if (i == n - 1) {
                c.redirectOutput(b.redirectOutput());
            } else {
                propio = ProcessBuilder.temporalDeCadena(i);
                c.redirectOutput(Redirect.to(propio));
            }
            c.redirectError(b.redirectError());
            Process p = c.start();
            // Se espera aca: el siguiente necesita el archivo completo, y no hay tuberia que permita
            // solaparlos. Es la consecuencia directa de lo dicho arriba.
            if (i < n - 1) {
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new java.io.IOException("se interrumpio la cadena", e);
                }
            }
            out.add(p);
            anterior = propio;
        }
        return out;
    }

    // Un nombre de archivo para un eslabon de la cadena. Lleva el indice y un contador para que dos
    // cadenas simultaneas no se pisen: sin eso, dos hilos armando cadenas a la vez usarian el mismo
    // nombre y uno leeria la salida del otro.
    private static int contadorDeCadena = 0;

    private static synchronized File temporalDeCadena(int indice) {
        ProcessBuilder.contadorDeCadena = ProcessBuilder.contadorDeCadena + 1;
        String base = System.getProperty("java.io.tmpdir");
        String nombre = "kaji-pipe-" + ProcessBuilder.contadorDeCadena + "-" + indice + ".tmp";
        return base == null ? new File(nombre) : new File(base, nombre);
    }
}
