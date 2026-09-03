package java.util.prefs;

// El documento que se le paso a {@link Preferences#importPreferences} no es un arbol de
// preferencias valido.
//
// Se distingue de un `IOException`: aca los bytes llegaron bien y lo que esta mal es lo que dicen.
// Por eso la causa suele ser un error del analizador de XML y no del flujo.
public class InvalidPreferencesFormatException extends Exception {

    private static final long serialVersionUID = -791715184232119669L;

    // Un documento invalido, con la causa que lo detecto.
    public InvalidPreferencesFormatException(Throwable cause) {
        super(cause);
    }

    // Un documento invalido, descrito por `message`.
    public InvalidPreferencesFormatException(String message) {
        super(message);
    }

    // Un documento invalido, con mensaje y causa.
    public InvalidPreferencesFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
