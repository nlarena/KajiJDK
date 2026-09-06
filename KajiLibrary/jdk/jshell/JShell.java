package jdk.jshell;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.tools.StandardJavaFileManager;

import jdk.jshell.spi.ExecutionControlProvider;

/**
 * El motor del interprete: guarda el estado de una sesion de Java interactivo.
 *
 * <h2>Que es lo que guarda</h2>
 *
 * <p>La lista de fragmentos evaluados y en que situacion quedo cada uno. Eso es todo el estado de
 * una sesion: no hay archivo ni proyecto, solo la historia de lo que se escribio. De ahi salen
 * {@link #snippets}, {@link #status} y la posibilidad de deshacer con {@link #drop}.
 *
 * <h2>Las dos mitades</h2>
 *
 * <p>Evaluar un fragmento son dos cosas distintas: compilarlo, que pasa en esta maquina virtual, y
 * ejecutarlo, que pasa en otra. La segunda esta afuera a proposito --{@code jdk.jshell.spi} y
 * {@code jdk.jshell.execution}-- porque el codigo del usuario no puede compartir maquina virtual con
 * el interprete: un {@code System.exit()} escrito en la sesion se llevaria puesto al interprete
 * entero, y una variable estatica del usuario podria pisar las del motor.
 *
 * <p>Compilar es lo que no se puede sacar afuera: el interprete tiene que meter el fragmento adentro
 * de una clase sintetica, compilarla, y despues traducir las posiciones de los errores de vuelta al
 * texto que el usuario escribio. Eso lo hace con el compilador que devuelve
 * {@code javax.tools.ToolProvider.getSystemJavaCompiler()}.
 *
 * <h2>Por que evaluar produce una lista</h2>
 *
 * <p>Porque un fragmento arrastra a los demas. Reescribir un metodo deja al anterior en
 * {@link Snippet.Status#OVERWRITTEN} y puede volver valido a un tercero que lo estaba esperando.
 * {@link SnippetEvent#causeSnippet} distingue el que se evaluo de los arrastrados.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>El estado de la sesion, los avisos, el ciclo de vida y toda la construccion funcionan de
 * verdad. Lo que no hay es el compilador: en esta biblioteca
 * {@code ToolProvider.getSystemJavaCompiler()} devuelve {@code null}, asi que no hay con que armar
 * la clase sintetica ni con que compilarla. {@link #eval} tira {@link IllegalStateException} y los
 * metodos de {@link #sourceCodeAnalysis} tiran {@link UnsupportedOperationException}. Todo lo demas
 * --que es la mayor parte-- anda: la sesion arranca vacia y se comporta como una sesion vacia.
 *
 * @since 9
 */
public class JShell implements AutoCloseable {

    private final InputStream in;
    private final PrintStream out;
    private final PrintStream err;
    private final JShellConsole console;
    private final Supplier<String> tempVariableNameGenerator;
    private final BiFunction<Snippet, Integer, String> idGenerator;
    private final List<String> extraRemoteVMOptions;
    private final List<String> extraCompilerOptions;
    private final ExecutionControlProvider executionControlProvider;
    private final Map<String, String> executionControlParameters;
    private final String executionControlSpec;
    private final Function<StandardJavaFileManager, StandardJavaFileManager> fileManagerMapper;

    private final List<Snippet> snippets = new ArrayList<Snippet>();
    private final List<String> classpath = new ArrayList<String>();
    private final Map<Subscription, Consumer<SnippetEvent>> oyentesDeFragmento =
            new LinkedHashMap<Subscription, Consumer<SnippetEvent>>();
    private final Map<Subscription, Consumer<JShell>> oyentesDeCierre =
            new LinkedHashMap<Subscription, Consumer<JShell>>();

    private SourceCodeAnalysis analisis;
    private boolean closed;

    JShell(Builder b) {
        this.in = b.in;
        this.out = b.out;
        this.err = b.err;
        this.console = b.console;
        this.tempVariableNameGenerator = b.tempVariableNameGenerator;
        this.idGenerator = b.idGenerator;
        this.extraRemoteVMOptions = b.extraRemoteVMOptions;
        this.extraCompilerOptions = b.extraCompilerOptions;
        this.executionControlProvider = b.executionControlProvider;
        this.executionControlParameters = b.executionControlParameters;
        this.executionControlSpec = b.executionControlSpec;
        this.fileManagerMapper = b.fileManagerMapper;
    }

    /**
     * El identificador con el que se saca un aviso que se habia pedido.
     *
     * <p>No tiene metodos a proposito: lo unico que se puede hacer con el es devolverlo a
     * {@link JShell#unsubscribe}. Que sea un objeto opaco y no un numero evita que se lo confunda
     * con el de otro interprete.
     */
    public static class Subscription {

        Subscription() {
        }
    }

    /**
     * Arma un interprete con opciones.
     *
     * <p>Todos los metodos devuelven el mismo constructor, para poder encadenarlos. Lo que no se
     * diga toma su valor por omision: la entrada y la salida de la maquina virtual, el motor de
     * ejecucion predeterminado, y ninguna opcion extra.
     */
    public static class Builder {

        InputStream in = new java.io.ByteArrayInputStream(new byte[0]);
        PrintStream out = System.out;
        PrintStream err = System.err;
        JShellConsole console;
        Supplier<String> tempVariableNameGenerator;
        BiFunction<Snippet, Integer, String> idGenerator;
        List<String> extraRemoteVMOptions = new ArrayList<String>();
        List<String> extraCompilerOptions = new ArrayList<String>();
        ExecutionControlProvider executionControlProvider;
        Map<String, String> executionControlParameters;
        String executionControlSpec;
        Function<StandardJavaFileManager, StandardJavaFileManager> fileManagerMapper;

        Builder() {
        }

        /**
         * De donde lee el codigo del usuario cuando pide entrada.
         *
         * @param in la entrada
         * @return este constructor
         */
        public Builder in(InputStream in) {
            this.in = in;
            return this;
        }

        /**
         * A donde escribe el codigo del usuario.
         *
         * @param out la salida
         * @return este constructor
         */
        public Builder out(PrintStream out) {
            this.out = out;
            return this;
        }

        /**
         * A donde escribe sus errores el codigo del usuario.
         *
         * @param err la salida de errores
         * @return este constructor
         */
        public Builder err(PrintStream err) {
            this.err = err;
            return this;
        }

        /**
         * Que consola ve el codigo del usuario.
         *
         * <p>Es lo que hace que {@code System.console()} funcione dentro de la sesion: la consola de
         * verdad la tiene el interprete, no el codigo del usuario, que corre en otra maquina
         * virtual.
         *
         * @param console la consola, o {@code null} para que no haya
         * @return este constructor
         * @since 22
         */
        public Builder console(JShellConsole console) {
            this.console = console;
            return this;
        }

        /**
         * Con que nombres se llaman las variables que el interprete inventa.
         *
         * <p>Son las que guardan el resultado de una expresion suelta. Por omision se llaman
         * {@code $1}, {@code $2} y asi.
         *
         * @param generator de donde salen los nombres, o {@code null} para los de siempre
         * @return este constructor
         */
        public Builder tempVariableNameGenerator(Supplier<String> generator) {
            this.tempVariableNameGenerator = generator;
            return this;
        }

        /**
         * Con que identificadores se numeran los fragmentos.
         *
         * <p>Recibe el fragmento y el numero que le tocaria. Sirve para que un programa que hospeda
         * al interprete use su propia numeracion.
         *
         * @param generator de donde salen los identificadores, o {@code null} para los de siempre
         * @return este constructor
         */
        public Builder idGenerator(BiFunction<Snippet, Integer, String> generator) {
            this.idGenerator = generator;
            return this;
        }

        /**
         * Opciones extra para la maquina virtual donde corre el codigo del usuario.
         *
         * @param options las opciones
         * @return este constructor
         */
        public Builder remoteVMOptions(String... options) {
            for (int i = 0; i < options.length; i++) {
                this.extraRemoteVMOptions.add(options[i]);
            }
            return this;
        }

        /**
         * Opciones extra para el compilador.
         *
         * @param options las opciones
         * @return este constructor
         */
        public Builder compilerOptions(String... options) {
            for (int i = 0; i < options.length; i++) {
                this.extraCompilerOptions.add(options[i]);
            }
            return this;
        }

        /**
         * Que motor de ejecucion usar, nombrado.
         *
         * <p>El nombre lleva los parametros adentro, separados por comas, porque asi se lo puede
         * pasar por la linea de comandos: {@code "jdi:launch(true)"}.
         *
         * @param name el nombre del motor con sus parametros
         * @return este constructor
         */
        public Builder executionEngine(String name) {
            this.executionControlSpec = name;
            return this;
        }

        /**
         * Que motor de ejecucion usar, dado directamente.
         *
         * @param executionControlProvider el motor
         * @param executionControlParameters sus parametros, o {@code null} para los de omision
         * @return este constructor
         */
        public Builder executionEngine(ExecutionControlProvider executionControlProvider,
                Map<String, String> executionControlParameters) {
            this.executionControlProvider = executionControlProvider;
            this.executionControlParameters = executionControlParameters;
            return this;
        }

        /**
         * Como envolver el manejador de archivos del compilador.
         *
         * <p>Sirve para que un programa que hospeda al interprete le dé al compilador sus propias
         * fuentes: las de un editor que todavia no guardo, por ejemplo.
         *
         * @param mapper que hacer con el manejador
         * @return este constructor
         * @since 15
         */
        public Builder fileManager(
                Function<StandardJavaFileManager, StandardJavaFileManager> mapper) {
            this.fileManagerMapper = mapper;
            return this;
        }

        /**
         * Arma el interprete.
         *
         * @return el interprete, con la sesion vacia
         * @throws IllegalStateException si no se lo pudo armar
         */
        public JShell build() throws IllegalStateException {
            return new JShell(this);
        }
    }

    /**
     * Un interprete con todo por omision.
     *
     * @return el interprete
     * @throws IllegalStateException si no se lo pudo armar
     */
    public static JShell create() throws IllegalStateException {
        return builder().build();
    }

    /**
     * Un constructor de interpretes.
     *
     * @return el constructor
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * El analizador de codigo de este interprete.
     *
     * <p>En esta biblioteca no hay compilador, y sin compilador no hay analisis: el objeto que se
     * devuelve tira {@link UnsupportedOperationException} en sus diez metodos en vez de contestar
     * cualquier cosa. Ver la nota de {@link SourceCodeAnalysis}.
     *
     * @return el analizador
     */
    public SourceCodeAnalysis sourceCodeAnalysis() {
        if (this.analisis == null) {
            this.analisis = new SinCompilador();
        }
        return this.analisis;
    }

    /**
     * Evalua ese codigo y devuelve lo que le paso a cada fragmento afectado.
     *
     * <p>En esta biblioteca no hay compilador --{@code ToolProvider.getSystemJavaCompiler()}
     * devuelve {@code null}--, asi que no hay con que armar la clase sintetica ni con que
     * compilarla, y esto tira siempre.
     *
     * @param input el codigo
     * @return los sucesos
     * @throws IllegalStateException si el interprete esta cerrado, o si no hay compilador
     */
    public List<SnippetEvent> eval(String input) throws IllegalStateException {
        comprobarVivo();
        throw new IllegalStateException(
                "no hay compilador: ToolProvider.getSystemJavaCompiler() devuelve null");
    }

    /**
     * Borra un fragmento de la sesion.
     *
     * <p>Los que dependian de el quedan sin resolver, y eso tambien viene en la lista.
     *
     * @param snippet el fragmento
     * @return los sucesos
     * @throws IllegalStateException si el interprete esta cerrado
     * @throws NullPointerException si el fragmento es {@code null}
     * @throws IllegalArgumentException si el fragmento no es de este interprete
     */
    public List<SnippetEvent> drop(Snippet snippet) throws IllegalStateException {
        comprobarVivo();
        comprobarFragmento(snippet);
        return new ArrayList<SnippetEvent>();
    }

    /**
     * Agrega eso al camino de clases con el que se compila y se ejecuta.
     *
     * @param path un archivo o directorio
     * @throws IllegalStateException si el interprete esta cerrado
     * @throws NullPointerException si el camino es {@code null}
     */
    public void addToClasspath(String path) {
        comprobarVivo();
        if (path == null) {
            throw new NullPointerException("path");
        }
        this.classpath.add(path);
    }

    /**
     * Intenta cortar el codigo del usuario que este corriendo.
     *
     * <p>Puede no lograrlo: si el codigo esta bloqueado esperando entrada o salida, o si atrapa lo
     * que se le manda, sigue corriendo igual.
     */
    public void stop() {
    }

    /**
     * Cierra el interprete y suelta lo que tenga tomado.
     *
     * <p>Avisa una sola vez a los oyentes de cierre, aunque se lo llame de nuevo.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        final List<Consumer<JShell>> copia = new ArrayList<Consumer<JShell>>(
                this.oyentesDeCierre.values());
        for (int i = 0; i < copia.size(); i++) {
            try {
                copia.get(i).accept(this);
            } catch (Throwable t) {
                // Un oyente que tira no puede impedir que el interprete se cierre.
            }
        }
    }

    /**
     * Todos los fragmentos, en orden de identificador.
     *
     * @return los fragmentos
     */
    public Stream<Snippet> snippets() {
        return this.snippets.stream();
    }

    /**
     * Las variables activas.
     *
     * @return las variables
     */
    public Stream<VarSnippet> variables() {
        return filtrar(VarSnippet.class);
    }

    /**
     * Los metodos activos.
     *
     * @return los metodos
     */
    public Stream<MethodSnippet> methods() {
        return filtrar(MethodSnippet.class);
    }

    /**
     * Los tipos declarados activos.
     *
     * @return los tipos
     */
    public Stream<TypeDeclSnippet> types() {
        return filtrar(TypeDeclSnippet.class);
    }

    /**
     * Los imports activos.
     *
     * @return los imports
     */
    public Stream<ImportSnippet> imports() {
        return filtrar(ImportSnippet.class);
    }

    /**
     * En que situacion esta ese fragmento.
     *
     * @param snippet el fragmento
     * @return la situacion
     * @throws NullPointerException si el fragmento es {@code null}
     * @throws IllegalArgumentException si el fragmento no es de este interprete
     */
    public Snippet.Status status(Snippet snippet) {
        comprobarFragmento(snippet);
        return Snippet.Status.NONEXISTENT;
    }

    /**
     * Los errores y avisos de ese fragmento.
     *
     * @param snippet el fragmento
     * @return los diagnosticos
     * @throws NullPointerException si el fragmento es {@code null}
     * @throws IllegalArgumentException si el fragmento no es de este interprete
     */
    public Stream<Diag> diagnostics(Snippet snippet) {
        comprobarFragmento(snippet);
        return new ArrayList<Diag>().stream();
    }

    /**
     * Que le falta a ese fragmento para quedar resuelto.
     *
     * @param snippet el fragmento
     * @return los nombres de lo que le falta
     * @throws NullPointerException si el fragmento es {@code null}
     * @throws IllegalArgumentException si el fragmento no es de este interprete
     */
    public Stream<String> unresolvedDependencies(DeclarationSnippet snippet) {
        comprobarFragmento(snippet);
        return new ArrayList<String>().stream();
    }

    /**
     * El valor de esa variable, ya convertido a texto.
     *
     * @param snippet la variable
     * @return el valor
     * @throws IllegalStateException si el interprete esta cerrado
     * @throws NullPointerException si el fragmento es {@code null}
     * @throws IllegalArgumentException si el fragmento no es de este interprete, o si la variable no
     *     esta definida
     */
    public String varValue(VarSnippet snippet) throws IllegalStateException {
        comprobarVivo();
        comprobarFragmento(snippet);
        throw new IllegalArgumentException("la variable no esta definida: " + snippet);
    }

    /**
     * Pide que se avise cada vez que a un fragmento le pase algo.
     *
     * @param listener a quien avisarle
     * @return con que sacar el aviso
     * @throws IllegalStateException si el interprete esta cerrado
     * @throws NullPointerException si el oyente es {@code null}
     */
    public Subscription onSnippetEvent(Consumer<SnippetEvent> listener)
            throws IllegalStateException {
        comprobarVivo();
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        final Subscription s = new Subscription();
        this.oyentesDeFragmento.put(s, listener);
        return s;
    }

    /**
     * Pide que se avise cuando el interprete se cierre.
     *
     * <p>Tambien avisa si la maquina virtual donde corre el codigo del usuario se muere sola, que es
     * lo que pasa cuando alguien escribe {@code System.exit()} en la sesion.
     *
     * @param listener a quien avisarle
     * @return con que sacar el aviso
     * @throws IllegalStateException si el interprete esta cerrado
     * @throws NullPointerException si el oyente es {@code null}
     */
    public Subscription onShutdown(Consumer<JShell> listener) throws IllegalStateException {
        comprobarVivo();
        if (listener == null) {
            throw new NullPointerException("listener");
        }
        final Subscription s = new Subscription();
        this.oyentesDeCierre.put(s, listener);
        return s;
    }

    /**
     * Saca un aviso que se habia pedido.
     *
     * <p>Un identificador de otro interprete no hace nada: sacar algo que no esta no es un error.
     *
     * @param token el identificador que devolvio el pedido
     * @throws NullPointerException si el identificador es {@code null}
     */
    public void unsubscribe(Subscription token) {
        if (token == null) {
            throw new NullPointerException("token");
        }
        this.oyentesDeFragmento.remove(token);
        this.oyentesDeCierre.remove(token);
    }

    /** Los fragmentos activos de esa clase. */
    private <T extends Snippet> Stream<T> filtrar(Class<T> clase) {
        final List<T> out = new ArrayList<T>();
        for (int i = 0; i < this.snippets.size(); i++) {
            final Snippet s = this.snippets.get(i);
            if (clase.isInstance(s) && status(s).isActive()) {
                out.add(clase.cast(s));
            }
        }
        return out.stream();
    }

    private void comprobarVivo() {
        if (this.closed) {
            throw new IllegalStateException("JShell (" + this + ") has been closed.");
        }
    }

    /**
     * Comprueba que el fragmento sea de este interprete.
     *
     * <p>Nunca lo es: los fragmentos los fabrica {@link #eval}, y {@link #eval} no puede fabricar
     * ninguno sin compilador. Cualquier fragmento que llegue aca viene de otro lado.
     */
    private void comprobarFragmento(Snippet snippet) {
        if (snippet == null) {
            throw new NullPointerException("snippet");
        }
        throw new IllegalArgumentException("el fragmento no es de este interprete: " + snippet);
    }

    /** El analizador que no hay, porque no hay compilador con que analizar. */
    private static final class SinCompilador extends SourceCodeAnalysis {

        private static final String MOTIVO =
                "no hay compilador: ToolProvider.getSystemJavaCompiler() devuelve null";

        @Override
        public CompletionInfo analyzeCompletion(String input) {
            throw new UnsupportedOperationException(MOTIVO);
        }

        @Override
        public List<Suggestion> completionSuggestions(String input, int cursor, int[] anchor) {
            throw new UnsupportedOperationException(MOTIVO);
        }

        @Override
        public List<Documentation> documentation(String input, int cursor, boolean computeJavadoc) {
            throw new UnsupportedOperationException(MOTIVO);
        }

        @Override
        public String analyzeType(String code, int cursor) {
            throw new UnsupportedOperationException(MOTIVO);
        }

        @Override
        public QualifiedNames listQualifiedNames(String code, int cursor) {
            throw new UnsupportedOperationException(MOTIVO);
        }

        @Override
        public SnippetWrapper wrapper(Snippet snippet) {
            throw new UnsupportedOperationException(MOTIVO);
        }

        @Override
        public List<SnippetWrapper> wrappers(String input) {
            throw new UnsupportedOperationException(MOTIVO);
        }

        @Override
        public List<Snippet> sourceToSnippets(String input) {
            throw new UnsupportedOperationException(MOTIVO);
        }

        @Override
        public Collection<Snippet> dependents(Snippet snippet) {
            throw new UnsupportedOperationException(MOTIVO);
        }

        @Override
        public List<Highlight> highlights(String input) {
            throw new UnsupportedOperationException(MOTIVO);
        }
    }
}
