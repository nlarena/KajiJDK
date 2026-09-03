package javax.annotation.processing;

// La fabrica de {@link Completion} (JSR 269). Es toda la clase: dos `of` estaticos y un
// constructor privado para que nadie la instancie. La implementacion concreta va en una clase
// anidada privada, igual que en el JDK real: `Completion` no expone constructor, y este es el unico
// camino para obtener uno.
public class Completions {

    // Utilitaria: no se instancia.
    private Completions() {
    }

    /**
     * Un completado con valor y mensaje.
     *
     * @param value el texto a insertar
     * @param message la explicacion que lo acompana
     */
    public static Completion of(String value, String message) {
        return new SimpleCompletion(value, message);
    }

    /**
     * Un completado sin mensaje: el mensaje queda en la cadena vacia, no en `null`.
     *
     * <p>Es lo que hace el JDK real, y es la diferencia que importa: un `null` obligaria a todo
     * consumidor a chequear, cuando "no tengo nada que explicar" ya se dice con "".
     */
    public static Completion of(String value) {
        return new SimpleCompletion(value, "");
    }

    // El unico implementador. Inmutable y sin validacion: el contrato no prohibe un valor nulo, y
    // inventar una excepcion que el JDK no tira seria mentir sobre el comportamiento.
    private static class SimpleCompletion implements Completion {

        private final String value;
        private final String message;

        SimpleCompletion(String value, String message) {
            this.value = value;
            this.message = message;
        }

        public String getValue() {
            return this.value;
        }

        public String getMessage() {
            return this.message;
        }

        public String toString() {
            return "[\"" + this.value + "\", \"" + this.message + "\"]";
        }
    }
}
