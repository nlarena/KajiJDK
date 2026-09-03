package java.lang.classfile;

// Lo que sabe leer y escribir un atributo con un nombre dado. Un lector encuentra un nombre en el
// archivo, busca el mapeador, y le pide que interprete el cuerpo; un escritor hace el camino
// inverso. Los mapeadores de los atributos que el JVMS define están en {@link Attributes}.
public interface AttributeMapper<A extends Attribute<A>> {

    /** El nombre del atributo, tal como aparece en el `Utf8`. */
    String name();

    /**
     * Lee el atributo. `pos` es el offset del primer byte del **cuerpo**, es decir después del
     * `attribute_name_index` y del `attribute_length`.
     */
    A readAttribute(AttributedElement enclosing, ClassReader cf, int pos);

    /** Escribe el atributo completo —nombre, largo y cuerpo— en `buf`. */
    void writeAttribute(BufWriter buf, A attr);

    /** Si el atributo puede aparecer más de una vez en el mismo lugar. Por defecto, no. */
    default boolean allowMultiple() {
        return false;
    }

    /** Qué le pasa a este atributo cuando la clase se transforma. */
    AttributeStability stability();

    /**
     * Cuánto sobrevive un atributo a una transformación de la clase que lo contiene. El orden de las
     * constantes va de lo más estable a lo menos, y es el criterio con el que una transformación
     * decide si puede copiar el atributo tal cual o tiene que descartarlo.
     */
    public enum AttributeStability {

        /** No depende del pool ni de las posiciones del código: se copia siempre. */
        STATELESS,
        /** Depende del pool de constantes, pero no del código. */
        CP_REFS,
        /** Depende de las posiciones dentro del arreglo `code`. */
        LABELS,
        /** El formato no se conoce; se copia byte a byte y puede quedar mal. */
        UNKNOWN,
        /** No se puede trasladar: una transformación lo descarta. */
        UNSTABLE;
    }
}
