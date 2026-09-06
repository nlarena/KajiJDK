package jdk.jshell;

/**
 * Un pedazo de codigo Java que el interprete trata como una unidad.
 *
 * <h2>Que es un fragmento</h2>
 *
 * <p>En un archivo, el codigo esta ordenado: declaraciones adentro de una clase, sentencias adentro
 * de un metodo. En un interprete no hay archivo, y lo que el usuario escribe puede ser cualquiera de
 * las dos cosas, o una expresion suelta, o un import. Un fragmento es esa unidad: lo que se escribio
 * de una vez, con el tipo de cosa que resulto ser.
 *
 * <h2>Por que tiene identidad</h2>
 *
 * <p>Un fragmento no cambia nunca: escribir de nuevo un metodo con la misma firma no modifica el
 * fragmento anterior, crea uno nuevo y deja al viejo en {@link Status#OVERWRITTEN}. Eso es lo que
 * permite deshacer, listar la historia, y saber que fragmentos dependian de cual --si se reescribe
 * un metodo, todo lo que lo llamaba tiene que recompilarse--.
 *
 * <p>{@link #id} identifica al fragmento dentro de su interprete, y {@link #source} guarda el texto
 * tal como se escribio, que es lo que se muestra al listar la historia.
 *
 * <h2>Los tres niveles</h2>
 *
 * <p>{@link #kind} dice de que clase de fragmento se trata --import, declaracion de tipo, metodo,
 * variable, expresion, sentencia, o algo que no compilo--. {@link #subKind} afina eso: dentro de
 * `VAR` distingue una declaracion sin valor de una con valor y de la variable temporal que el
 * interprete inventa para guardar el resultado de una expresion suelta. La distincion importa
 * porque de ahi sale si el fragmento produce un valor para mostrar.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Los fragmentos los fabrica el interprete al evaluar, y evaluar necesita un compilador en
 * proceso que esta biblioteca no tiene --ver {@link JShell#eval}--. Las clases estan completas y las
 * tres enumeraciones funcionan de verdad; lo que no hay es quien construya un fragmento.
 *
 * @since 9
 */
public abstract class Snippet {

    private final String id;
    private final String source;
    private final SubKind subkind;

    /** De que clase de fragmento se trata. */
    public enum Kind {

        /** Un `import`. */
        IMPORT(true),

        /** Una declaracion de clase, interfaz, enumeracion, registro o anotacion. */
        TYPE_DECL(true),

        /** Una declaracion de metodo. */
        METHOD(true),

        /** Una declaracion de variable. */
        VAR(true),

        /** Una expresion suelta. */
        EXPRESSION(false),

        /** Una sentencia suelta. */
        STATEMENT(false),

        /** Algo que no se pudo entender. */
        ERRONEOUS(false);

        private final boolean persistente;

        Kind(boolean persistente) {
            this.persistente = persistente;
        }

        /**
         * Si un fragmento de esta clase queda declarado para los que vengan despues.
         *
         * <p>Un metodo o una variable siguen existiendo despues de escribirlos; una expresion se
         * evalua y se termina. La diferencia decide que fragmentos hay que recompilar cuando algo
         * cambia.
         *
         * @return cierto si queda declarado
         */
        public boolean isPersistent() {
            return this.persistente;
        }
    }

    /** En que situacion esta un fragmento dentro del interprete. */
    public enum Status {

        /** Compila y esta activo. */
        VALID(true, true),

        /**
         * Le falta algo que todavia no se declaro, pero se lo pudo definir igual.
         *
         * <p>Es lo que pasa al escribir un metodo que llama a otro que todavia no existe: el
         * interprete lo acepta y lo deja pendiente, porque en una sesion interactiva el orden en que
         * se escriben las cosas no tiene por que ser el orden en que se usan.
         */
        RECOVERABLE_DEFINED(true, true),

        /** Le falta algo y ademas no se lo pudo definir; sigue vivo pero no se puede usar. */
        RECOVERABLE_NOT_DEFINED(true, false),

        /** Lo borro el usuario. */
        DROPPED(false, false),

        /** Lo reemplazo otro fragmento posterior. */
        OVERWRITTEN(false, false),

        /** No compilo y no hay forma de arreglarlo declarando otra cosa. */
        REJECTED(false, false),

        /** No existe en este interprete. */
        NONEXISTENT(false, false);

        private final boolean activo;
        private final boolean definido;

        Status(boolean activo, boolean definido) {
            this.activo = activo;
            this.definido = definido;
        }

        /**
         * Si el fragmento sigue formando parte del estado del interprete.
         *
         * @return cierto si sigue vivo
         */
        public boolean isActive() {
            return this.activo;
        }

        /**
         * Si lo que el fragmento declara existe y se puede usar.
         *
         * @return cierto si esta definido
         */
        public boolean isDefined() {
            return this.definido;
        }
    }

    /** La clase de fragmento, afinada. */
    public enum SubKind {

        /** {@code import java.util.List;} */
        SINGLE_TYPE_IMPORT_SUBKIND(Kind.IMPORT),

        /** {@code import java.util.*;} */
        TYPE_IMPORT_ON_DEMAND_SUBKIND(Kind.IMPORT),

        /** {@code import static java.lang.Math.PI;} */
        SINGLE_STATIC_IMPORT_SUBKIND(Kind.IMPORT),

        /** {@code import static java.lang.Math.*;} */
        STATIC_IMPORT_ON_DEMAND_SUBKIND(Kind.IMPORT),

        /** {@code import module java.base;} */
        MODULE_IMPORT_SUBKIND(Kind.IMPORT),

        /** Una clase. */
        CLASS_SUBKIND(Kind.TYPE_DECL),

        /** Una interfaz. */
        INTERFACE_SUBKIND(Kind.TYPE_DECL),

        /** Una enumeracion. */
        ENUM_SUBKIND(Kind.TYPE_DECL),

        /** Un registro. */
        RECORD_SUBKIND(Kind.TYPE_DECL),

        /** Un tipo de anotacion. */
        ANNOTATION_TYPE_SUBKIND(Kind.TYPE_DECL),

        /** Un metodo. */
        METHOD_SUBKIND(Kind.METHOD),

        /** Una variable declarada sin valor. */
        VAR_DECLARATION_SUBKIND(Kind.VAR, true, true),

        /** Una variable declarada con valor. */
        VAR_DECLARATION_WITH_INITIALIZER_SUBKIND(Kind.VAR, true, true),

        /**
         * La variable que el interprete inventa para guardar el resultado de una expresion suelta.
         *
         * <p>Es lo que hace que escribir {@code 1 + 1} deje algo con nombre a lo que referirse
         * despues, en vez de un resultado que se pierde.
         */
        TEMP_VAR_EXPRESSION_SUBKIND(Kind.VAR, true, true),

        /** Una expresion que es solo el nombre de una variable. */
        VAR_VALUE_SUBKIND(Kind.EXPRESSION, true, true),

        /** Una asignacion. */
        ASSIGNMENT_SUBKIND(Kind.EXPRESSION, true, true),

        /** Cualquier otra expresion. */
        OTHER_EXPRESSION_SUBKIND(Kind.EXPRESSION, true, true),

        /** Una sentencia. */
        STATEMENT_SUBKIND(Kind.STATEMENT, true, false),

        /** Algo que no se pudo entender. */
        UNKNOWN_SUBKIND(Kind.ERRONEOUS);

        private final Kind kind;
        private final boolean ejecutable;
        private final boolean conValor;

        SubKind(Kind kind) {
            this(kind, false, false);
        }

        SubKind(Kind kind, boolean ejecutable, boolean conValor) {
            this.kind = kind;
            this.ejecutable = ejecutable;
            this.conValor = conValor;
        }

        /**
         * Si evaluar un fragmento de esta clase hace correr codigo.
         *
         * <p>Declarar un metodo no corre nada; llamarlo si. La diferencia es la que decide si hace
         * falta la maquina virtual de ejecucion o alcanza con compilar.
         *
         * @return cierto si hace correr codigo
         */
        public boolean isExecutable() {
            return this.ejecutable;
        }

        /**
         * Si evaluar un fragmento de esta clase deja un valor para mostrar.
         *
         * @return cierto si deja un valor
         */
        public boolean hasValue() {
            return this.conValor;
        }

        /**
         * De que clase de fragmento es una afinacion.
         *
         * @return la clase
         */
        public Kind kind() {
            return this.kind;
        }
    }

    Snippet(String id, String source, SubKind subkind) {
        this.id = id;
        this.source = source;
        this.subkind = subkind;
    }

    /**
     * Que fragmento es, dentro de su interprete.
     *
     * <p>No es un numero de orden: el interprete puede generarlos como quiera, y de hecho se puede
     * cambiar como con {@link JShell.Builder#idGenerator}.
     *
     * @return el identificador
     */
    public String id() {
        return this.id;
    }

    /**
     * De que clase de fragmento se trata.
     *
     * @return la clase
     */
    public Kind kind() {
        return this.subkind.kind();
    }

    /**
     * La clase de fragmento, afinada.
     *
     * @return la subclase
     */
    public Snippet.SubKind subKind() {
        return this.subkind;
    }

    /**
     * El texto tal como se escribio.
     *
     * @return el codigo
     */
    public String source() {
        return this.source;
    }

    /**
     * Para leer al depurar.
     *
     * @return el identificador, la subclase y el codigo
     */
    @Override
    public String toString() {
        return this.id + ":" + this.subkind + ":" + this.source;
    }
}
