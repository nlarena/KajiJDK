package netscape.javascript;

/**
 * Un objeto de JavaScript, visto desde Java.
 *
 * <p>Toda la clase es <strong>abstracta</strong> y eso es lo que hay que entender de ella: no
 * representa datos sino una <em>referencia a algo que vive del otro lado</em>. Quien la implementa
 * es el puente del navegador, que sabe hablar con su motor de JavaScript; desde Java lo unico que
 * se ve es que un nombre en texto se resuelve alla y vuelve un {@link Object}.
 *
 * <p>De ahi que todas las firmas devuelvan {@code Object} y todas puedan tirar
 * {@link JSException}: JavaScript no tiene los tipos de Java, asi que lo que vuelve solo se conoce
 * en tiempo de ejecucion, y un nombre que no existe alla es un error que aca no se puede prever.
 *
 * <p>La distincion entre <em>miembro</em> y <em>slot</em> es la misma que hace JavaScript entre una
 * propiedad con nombre y un indice de arreglo.
 *
 * @deprecated el modelo de applets, que es lo unico que instanciaba esto, esta en desuso desde
 *     Java 9 y marcado para borrarse desde 17.
 */
@Deprecated(since = "9", forRemoval = true)
public abstract class JSObject {

    /**
     * Para las subclases del puente.
     *
     * <p>Es {@code protected} y no publico porque nadie fabrica un {@code JSObject}: se recibe uno
     * que el motor ya tenia.
     */
    protected JSObject() {
    }

    /** Llama al metodo {@code methodName} de este objeto con esos argumentos. */
    public abstract Object call(String methodName, Object... args) throws JSException;

    /** Evalua {@code s} como codigo JavaScript, en el contexto de este objeto. */
    public abstract Object eval(String s) throws JSException;

    /** El valor de la propiedad {@code name}. */
    public abstract Object getMember(String name) throws JSException;

    /** Le pone {@code value} a la propiedad {@code name}, creandola si no estaba. */
    public abstract void setMember(String name, Object value) throws JSException;

    /** Borra la propiedad {@code name}. */
    public abstract void removeMember(String name) throws JSException;

    /** El valor en el indice {@code index}, para los objetos que JavaScript trata como arreglos. */
    public abstract Object getSlot(int index) throws JSException;

    /** Le pone {@code value} al indice {@code index}. */
    public abstract void setSlot(int index, Object value) throws JSException;
}
