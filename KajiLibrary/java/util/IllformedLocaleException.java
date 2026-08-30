package java.util;

// Un argumento de `Locale.Builder` no cumple la sintaxis de BCP 47.
//
// Ademas del mensaje lleva el **indice** donde se rompio, que es lo que la distingue de una
// IllegalArgumentException cualquiera: al validar una etiqueta de idioma el error casi siempre
// esta en un subtag concreto de una cadena larga, y decir "en el caracter 12" es la diferencia
// entre un mensaje util y uno que obliga a adivinar.
public class IllformedLocaleException extends RuntimeException {

    // Donde se rompio, o -1 si no se sabe.
    private int errorIndex = -1;

    // Sin mensaje ni indice.
    public IllformedLocaleException() {
        super();
    }

    // Con el mensaje dado y sin indice.
    public IllformedLocaleException(String message) {
        super(message);
    }

    // Con el mensaje dado y el indice donde se detecto el error.
    public IllformedLocaleException(String message, int errorIndex) {
        super(message + (errorIndex < 0 ? "" : " [at index " + errorIndex + "]"));
        this.errorIndex = errorIndex;
    }

    // El indice donde se rompio, o -1.
    public int getErrorIndex() {
        return this.errorIndex;
    }
}
