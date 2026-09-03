package javax.print.attribute;

import java.io.Serializable;
import java.util.Locale;

// La clase de sintaxis de los atributos cuyo valor es texto con un locale asociado.
//
// El locale importa: el mismo texto en distinto idioma son dos valores distintos, y entra en
// `equals` y en `hashCode`. Un locale null se reemplaza por el de la maquina; un texto null es un
// error.
public abstract class TextSyntax implements Serializable, Cloneable {

    private static final long serialVersionUID = -8130648736378144102L;

    private String value;
    private Locale locale;

    protected TextSyntax(String value, Locale locale) {
        this.value = verify(value);
        this.locale = verify(locale);
    }

    private static String verify(String value) {
        if (value == null) {
            throw new NullPointerException(" value is null");
        }
        return value;
    }

    // Un locale null no es un error: significa "el de por aca".
    private static Locale verify(Locale locale) {
        if (locale == null) {
            return Locale.getDefault();
        }
        return locale;
    }

    public String getValue() {
        return this.value;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public int hashCode() {
        return this.value.hashCode() ^ this.locale.hashCode();
    }

    public boolean equals(Object object) {
        if (!(object instanceof TextSyntax)) {
            return false;
        }
        TextSyntax other = (TextSyntax) object;
        return this.value.equals(other.value) && this.locale.equals(other.locale);
    }

    // Solo el texto: el locale no se muestra.
    public String toString() {
        return this.value;
    }
}
