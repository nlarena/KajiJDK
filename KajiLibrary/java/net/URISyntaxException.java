package java.net;

// Checked exception that says WHERE a URI string stopped being a URI: guarda la entrada
// completa, el motivo, y el indice del caracter que la rompio. `getMessage()` los junta.
//
// KajiLibrary: superficie identica a la del JDK (6 miembros). El unico rodeo esta en
// `getMessage()`: el JDK arma "motivo at index N: entrada" y nuestro `String` no tiene
// `valueOf(int)`, asi que el numero se escribe a mano con `digits()` — diez lineas de
// aritmetica sobre `char`, que es lo mismo que haria cualquier `Integer.toString`.
public class URISyntaxException extends Exception {

    private final String input;
    private final String reason;
    private final int index;

    public URISyntaxException(String input, String reason, int index) {
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

    public URISyntaxException(String input, String reason) {
        this(input, reason, -1);
    }

    public String getInput() {
        return this.input;
    }

    public String getReason() {
        return this.reason;
    }

    // -1 cuando el motivo no apunta a una posicion concreta.
    public int getIndex() {
        return this.index;
    }

    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.reason);
        if (this.index > -1) {
            sb.append(" at index ");
            sb.append(digits(this.index));
        }
        sb.append(": ");
        sb.append(this.input);
        return sb.toString();
    }

    // `String.valueOf(int)` no existe en esta biblioteca; esto es su equivalente minimo.
    private static String digits(int value) {
        if (value == 0) {
            return "0";
        }
        StringBuilder rev = new StringBuilder();
        int n = value;
        while (n > 0) {
            int d = n % 10;
            rev.append((char) ('0' + d));
            n = n / 10;
        }
        StringBuilder out = new StringBuilder();
        int i = rev.length() - 1;
        while (i >= 0) {
            out.append(rev.charAt(i));
            i = i - 1;
        }
        return out.toString();
    }
}
