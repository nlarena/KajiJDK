package java.nio.file;

// La cadena no se puede convertir en una ruta.
//
// **No es una `IOException`.** Hereda de `IllegalArgumentException` porque el problema esta en el
// argumento y no en el disco: la cadena esta mal escrita, y eso se sabe sin tocar nada. Por eso
// `Path.of` no declara `throws`.
//
// Guarda el **indice** del caracter que la rompio, que es lo que permite subrayar la posicion exacta
// en un mensaje de error en vez de repetir la cadena entera.
public class InvalidPathException extends IllegalArgumentException {

    private static final long serialVersionUID = 4355821422286746137L;

    private final String input;
    private final String reason;
    private final int index;

    /**
     * @param input la cadena que no sirve
     * @param reason por que no sirve
     * @param index la posicion del caracter culpable, o -1 si no se sabe
     * @throws IllegalArgumentException si `index` es menor que -1
     * @throws NullPointerException si `input` o `reason` son `null`
     */
    public InvalidPathException(String input, String reason, int index) {
        super(reason);
        if (input == null || reason == null) {
            throw new NullPointerException();
        }
        if (index < -1) {
            throw new IllegalArgumentException();
        }
        this.input = input;
        this.reason = reason;
        this.index = index;
    }

    /** Como el otro, con el indice en -1: no se sabe donde esta el problema. */
    public InvalidPathException(String input, String reason) {
        this(input, reason, -1);
    }

    /** La cadena que se intento convertir. */
    public String getInput() {
        return this.input;
    }

    /**
     * La explicacion.
     *
     * <p>Sale de un campo propio y no de `super.getMessage()` --que es de donde lo saca el JDK--
     * por un bug de **esta VM**: un `invokespecial` a un metodo que la superclase nombrada
     * *hereda* en vez de declarar ejecuta el cuerpo con el pool de constantes equivocado y
     * revienta con `getfield: bad FieldRef`. `getMessage()` esta declarado en `Throwable`, no en
     * `IllegalArgumentException`, asi que cae justo en el caso. Guardar la razon aparte da el
     * mismo resultado y no depende de eso.
     */
    public String getReason() {
        return this.reason;
    }

    /** La posicion del caracter culpable, o -1. */
    public int getIndex() {
        return this.index;
    }

    /** `razon: cadena` y, si se sabe, ` at index N` en el medio. */
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getReason());
        if (this.index > -1) {
            sb.append(" at index ");
            sb.append(this.index);
        }
        sb.append(": ");
        sb.append(this.input);
        return sb.toString();
    }
}
