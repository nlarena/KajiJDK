package jdk.jshell;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Lo que hace falta para que un editor entienda lo que el usuario esta escribiendo.
 *
 * <h2>Para que no alcanza con evaluar</h2>
 *
 * <p>Un interprete de linea de comandos tiene que decidir cosas antes de evaluar nada: si lo que se
 * escribio ya esta completo o el usuario va a seguir en la linea siguiente
 * ({@link #analyzeCompletion}), que puede venir despues del punto ({@link #completionSuggestions}),
 * que documentacion mostrar ({@link #documentation}), que colorear ({@link #highlights}). Ninguna de
 * esas preguntas se contesta ejecutando: se contestan analizando.
 *
 * <h2>Los envoltorios</h2>
 *
 * <p>Un fragmento no se compila tal cual: el interprete lo mete adentro de una clase sintetica,
 * porque un metodo suelto no es un programa Java valido. {@link SnippetWrapper} deja ver ese codigo
 * armado, y sobre todo traducir posiciones entre uno y otro, que es lo que hace que el subrayado de
 * un error caiga donde el usuario escribio y no donde el compilador lo vio.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Los tipos estan completos y las dos enumeraciones funcionan de verdad. El analisis no: para
 * contestar cualquiera de estas preguntas hace falta el analizador de codigo del compilador, y en
 * esta biblioteca {@code javax.tools.ToolProvider.getSystemJavaCompiler()} devuelve {@code null}.
 * Los diez metodos son abstractos, asi que la clase no promete nada; lo que devuelve
 * {@link JShell#sourceCodeAnalysis} tira {@link UnsupportedOperationException} en vez de contestar
 * cualquier cosa.
 *
 * @since 9
 */
public abstract class SourceCodeAnalysis {

    /** Uno. */
    protected SourceCodeAnalysis() {
    }

    /**
     * Que tan completo esta lo que se escribio.
     *
     * <p>Es lo que decide si el interprete evalua o pide otra linea.
     */
    public enum Completeness {

        /** Esta completo tal cual. */
        COMPLETE(true),

        /** Esta completo pero le falta el punto y coma, que el interprete agrega solo. */
        COMPLETE_WITH_SEMI(true),

        /** Seguro que falta: hay una llave o un parentesis sin cerrar. */
        DEFINITELY_INCOMPLETE(false),

        /**
         * Podria ser valido, pero lo mas probable es que falte.
         *
         * <p>Es el caso de un {@code if} sin cuerpo: {@code if (x)} solo es legal, pero nadie lo
         * escribe a proposito. Pedir otra linea acierta casi siempre.
         */
        CONSIDERED_INCOMPLETE(false),

        /** No se escribio nada. */
        EMPTY(false),

        /**
         * No se pudo saber, casi siempre porque hay un error de sintaxis.
         *
         * <p>Cuenta como completo: si esta mal escrito, pedir otra linea no lo va a arreglar, y es
         * mejor mostrar el error.
         */
        UNKNOWN(true);

        private final boolean completo;

        Completeness(boolean completo) {
            this.completo = completo;
        }

        /**
         * Si el interprete puede evaluar ya.
         *
         * @return cierto si no hace falta pedir otra linea
         */
        public boolean isComplete() {
            return this.completo;
        }
    }

    /** Como se ve un pedazo de codigo al colorearlo. */
    public enum Attribute {

        /** Es lo que se declara: el nombre de un metodo o de una variable donde se lo define. */
        DECLARATION,

        /** Esta marcado como obsoleto. */
        DEPRECATED,

        /** Es una palabra reservada. */
        KEYWORD
    }

    /**
     * Un pedazo de codigo y como se ve.
     *
     * <p>Las posiciones van referidas al texto que el usuario escribio.
     *
     * @param start donde empieza
     * @param end donde termina
     * @param attributes como se ve
     * @since 20
     */
    public record Highlight(int start, int end, Set<Attribute> attributes) {
    }

    /** Que tan completo esta lo escrito, y que sobra. */
    public interface CompletionInfo {

        /**
         * Que tan completo esta.
         *
         * @return la respuesta
         */
        Completeness completeness();

        /**
         * Lo que sobro despues del primer fragmento.
         *
         * <p>Escribir dos sentencias en una linea da un fragmento y un resto; el resto se analiza
         * despues, igual que si se hubiera escrito solo.
         *
         * @return lo que sobra, o la cadena vacia
         */
        String remaining();

        /**
         * El primer fragmento, con el punto y coma agregado si hacia falta.
         *
         * @return el codigo del fragmento
         */
        String source();
    }

    /** Algo que se puede escribir en ese lugar. */
    public interface Suggestion {

        /**
         * Lo que habria que escribir.
         *
         * @return el texto
         */
        String continuation();

        /**
         * Si el tipo de lo sugerido es el que hace falta ahi.
         *
         * <p>Sirve para ordenar las sugerencias: lo que encaja va primero.
         *
         * @return cierto si encaja
         */
        boolean matchesType();
    }

    /** La documentacion de algo.
     *
     * @since 9
     */
    public interface Documentation {

        /**
         * La firma de lo documentado.
         *
         * @return la firma
         */
        String signature();

        /**
         * El texto de la documentacion.
         *
         * @return el texto, o {@code null} si no hay
         */
        String javadoc();
    }

    /** El codigo que el interprete arma alrededor de un fragmento para poder compilarlo. */
    public interface SnippetWrapper {

        /**
         * El texto tal como se escribio.
         *
         * @return el codigo del usuario
         */
        String source();

        /**
         * El codigo armado, el que se le pasa al compilador.
         *
         * @return el codigo completo
         */
        String wrapped();

        /**
         * El nombre de la clase sintetica.
         *
         * @return el nombre completo de la clase
         */
        String fullClassName();

        /**
         * De que clase de fragmento se trata.
         *
         * @return la clase
         */
        Snippet.Kind kind();

        /**
         * Donde cae, en el codigo armado, esa posicion del codigo del usuario.
         *
         * @param pos la posicion en el codigo del usuario
         * @return la posicion en el codigo armado
         */
        int sourceToWrappedPosition(int pos);

        /**
         * Donde cae, en el codigo del usuario, esa posicion del codigo armado.
         *
         * <p>Es la traduccion que hace que el subrayado de un error del compilador caiga donde el
         * usuario escribio.
         *
         * @param pos la posicion en el codigo armado
         * @return la posicion en el codigo del usuario, o {@code -1} si cae en el envoltorio
         */
        int wrappedToSourcePosition(int pos);
    }

    /** Los nombres completos que podrian corresponder a un nombre simple. */
    public static final class QualifiedNames {

        private final List<String> names;
        private final int simpleNameLength;
        private final boolean upToDate;
        private final boolean resolvable;

        QualifiedNames(List<String> names, int simpleNameLength, boolean upToDate,
                boolean resolvable) {
            this.names = names == null ? Collections.<String>emptyList() : names;
            this.simpleNameLength = simpleNameLength;
            this.upToDate = upToDate;
            this.resolvable = resolvable;
        }

        /**
         * Los nombres completos que podrian ser.
         *
         * @return los nombres, o una lista vacia si el nombre ya se resuelve o no se encontro nada
         */
        public List<String> getNames() {
            return this.names;
        }

        /**
         * Cuantos caracteres mide el nombre simple.
         *
         * @return la longitud, para saber que reemplazar
         */
        public int getSimpleNameLength() {
            return this.simpleNameLength;
        }

        /**
         * Si el indice de clases estaba al dia cuando se contesto.
         *
         * <p>El indice se arma en segundo plano; si todavia no termino, la respuesta puede quedar
         * corta y conviene volver a preguntar.
         *
         * @return cierto si estaba al dia
         */
        public boolean isUpToDate() {
            return this.upToDate;
        }

        /**
         * Si el nombre ya se resuelve solo, sin agregar nada.
         *
         * @return cierto si ya se resuelve
         */
        public boolean isResolvable() {
            return this.resolvable;
        }
    }

    /**
     * Que tan completo esta lo escrito, y que sobra.
     *
     * @param input el codigo
     * @return la respuesta
     */
    public abstract CompletionInfo analyzeCompletion(String input);

    /**
     * Que se puede escribir en ese lugar.
     *
     * @param input el codigo
     * @param cursor donde esta el cursor
     * @param anchor donde escribe la posicion desde la que reemplazar
     * @return las sugerencias
     */
    public abstract List<Suggestion> completionSuggestions(String input, int cursor, int[] anchor);

    /**
     * La documentacion de lo que hay en ese lugar.
     *
     * @param input el codigo
     * @param cursor donde esta el cursor
     * @param computeJavadoc si tambien hay que traer el texto y no solo la firma
     * @return la documentacion
     */
    public abstract List<Documentation> documentation(String input, int cursor,
            boolean computeJavadoc);

    /**
     * El tipo de la expresion que termina ahi.
     *
     * @param code el codigo
     * @param cursor donde termina la expresion
     * @return el nombre del tipo, o {@code null} si no hay una expresion ahi
     */
    public abstract String analyzeType(String code, int cursor);

    /**
     * Los nombres completos que podrian corresponder al nombre simple que termina ahi.
     *
     * @param code el codigo
     * @param cursor donde termina el nombre
     * @return los candidatos
     */
    public abstract QualifiedNames listQualifiedNames(String code, int cursor);

    /**
     * El codigo armado alrededor de ese fragmento.
     *
     * @param snippet el fragmento
     * @return el envoltorio
     */
    public abstract SnippetWrapper wrapper(Snippet snippet);

    /**
     * Los codigos armados alrededor de los fragmentos que haya en ese codigo.
     *
     * @param input el codigo
     * @return los envoltorios
     */
    public abstract List<SnippetWrapper> wrappers(String input);

    /**
     * Los fragmentos que hay en ese codigo, sin evaluarlos ni agregarlos al interprete.
     *
     * @param input el codigo
     * @return los fragmentos
     */
    public abstract List<Snippet> sourceToSnippets(String input);

    /**
     * Que fragmentos dependen de ese.
     *
     * @param snippet el fragmento
     * @return los que dependen de el
     */
    public abstract Collection<Snippet> dependents(Snippet snippet);

    /**
     * Como se ve cada pedazo de ese codigo al colorearlo.
     *
     * @param input el codigo
     * @return los pedazos con su aspecto
     * @since 20
     */
    public abstract List<Highlight> highlights(String input);
}
