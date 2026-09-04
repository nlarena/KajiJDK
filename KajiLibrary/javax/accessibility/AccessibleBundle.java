package javax.accessibility;

import java.util.Locale;

/**
 * La base de las categorías con nombre traducible: roles, estados y relaciones.
 *
 * <p>El truco de la clase es que las constantes son **objetos**, no cadenas ni enteros. Cada rol o
 * estado es una instancia única con una clave adentro, así que se comparan por identidad y se pueden
 * mostrar traducidos sin que el programa toque nunca el texto.
 *
 * <p>Eso también es lo que permite que una aplicación invente sus propias categorías: hereda de acá,
 * declara su constante, y el resto del paquete la trata igual que a las de fábrica.
 *
 * <p>Sin catálogo de traducciones, {@link #toDisplayString()} devuelve la clave. Es lo que hace el
 * JDK cuando no encuentra el paquete de recursos del idioma, así que no es un relleno: es la
 * respuesta por omisión, y una clave legible es mejor que una cadena vacía.
 */
public abstract class AccessibleBundle {

    /** La clave que identifica a esta categoría. */
    protected String key;

    /** Para las subclases. */
    public AccessibleBundle() {
    }

    /**
     * El nombre para mostrar, buscado en ese catálogo y ese idioma.
     *
     * <p>Devuelve la clave: esta biblioteca no trae catálogos de traducción.
     */
    protected String toDisplayString(String resourceBundleName, Locale locale) {
        return this.key;
    }

    /** El nombre para mostrar en ese idioma. */
    public String toDisplayString(Locale locale) {
        return this.toDisplayString("com.sun.accessibility.internal.resources.accessibility",
                locale);
    }

    /** El nombre para mostrar en el idioma por omisión. */
    public String toDisplayString() {
        return this.toDisplayString(Locale.getDefault());
    }

    public String toString() {
        return this.toDisplayString();
    }
}
