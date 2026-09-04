package java.text;

import java.io.InvalidObjectException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Iteración sobre texto que además lleva <em>atributos</em> pegados a cada tramo.
 *
 * <p>Un {@link CharacterIterator} entrega caracteres; éste entrega caracteres y, en cada posición,
 * el conjunto de pares atributo/valor que rigen ahí. La pregunta que agrega no es "¿qué carácter
 * hay?" sino "¿hasta dónde llega el tramo que comparte estos atributos?" — de ahí
 * {@code getRunStart}/{@code getRunLimit}, que existen para que un renderer pueda pintar de a
 * tramos en vez de consultar atributo por carácter.
 *
 * <p>Ese es también el motivo de que exista en {@code java.text}: es el tipo de retorno de
 * {@code Format.formatToCharacterIterator}, la forma en que un formateador cuenta <em>dónde</em>
 * quedó cada campo del resultado sin que el llamador tenga que reparsear el texto.
 *
 * <p>Las tres sobrecargas de {@code getRunStart} no son azúcar: sin argumento el tramo es el que
 * comparte <em>todos</em> los atributos, con un atributo es el de ese solo, y con un conjunto es el
 * de esos. Un tramo de "negrita" puede cruzar varios tramos de "idioma", así que colapsarlas daría
 * el límite equivocado.
 */
public interface AttributedCharacterIterator extends CharacterIterator {

    /**
     * La clave de un atributo. Es una clase y no un enum ni un {@code String} a propósito: las
     * claves son extensibles (AWT define las suyas, {@link Format} define {@code Format.Field}) y
     * al mismo tiempo tienen que ser comparables por identidad, no por nombre, para que dos
     * paquetes distintos no colisionen usando la misma palabra.
     *
     * <p>Por eso {@code equals} y {@code hashCode} son {@code final} y son los de {@code Object}:
     * la subclase no puede debilitar la identidad. El nombre existe sólo para {@code toString} y
     * para resolver la deserialización.
     */
    public static class Attribute implements Serializable {

        // Registro de las constantes definidas por ESTA clase, para readResolve. Sólo se puebla
        // cuando el objeto construido es un Attribute exacto: una subclase que no lleve su propio
        // registro no debe contaminar el de acá, porque dos subclases distintas pueden usar el
        // mismo nombre legítimamente.
        private static final Map<String, AttributedCharacterIterator.Attribute> INSTANCIAS =
                new HashMap<String, AttributedCharacterIterator.Attribute>();

        private final String name;

        protected Attribute(String name) {
            this.name = name;
            if (this.getClass() == AttributedCharacterIterator.Attribute.class) {
                INSTANCIAS.put(name, this);
            }
        }

        /** El idioma del tramo; el valor es un {@link java.util.Locale}. */
        public static final AttributedCharacterIterator.Attribute LANGUAGE =
                new AttributedCharacterIterator.Attribute("language");

        /**
         * La lectura del tramo — la pronunciación de un texto cuya escritura no la determina.
         * Existe por el japonés: los kanji de un nombre propio no dicen cómo se leen, y el furigana
         * viaja acá.
         */
        public static final AttributedCharacterIterator.Attribute READING =
                new AttributedCharacterIterator.Attribute("reading");

        /** Un segmento entregado por un método de entrada; el valor es {@link Annotation}. */
        public static final AttributedCharacterIterator.Attribute INPUT_METHOD_SEGMENT =
                new AttributedCharacterIterator.Attribute("input_method_segment");

        public final boolean equals(Object obj) {
            return this == obj;
        }

        public final int hashCode() {
            return super.hashCode();
        }

        public String toString() {
            return this.getClass().getName() + "(" + this.name + ")";
        }

        protected String getName() {
            return this.name;
        }

        /**
         * Devuelve la constante equivalente al objeto deserializado, para que la identidad
         * sobreviva a un viaje por un stream.
         *
         * <p>Se implementa aunque KajiLibrary todavía no deserialice: el contrato del método está
         * definido y el cuerpo puede cumplirlo hoy. La guarda del tipo exacto es parte de ese
         * contrato — una subclase que no la reimplemente <em>tiene</em> que fallar, porque si no
         * devolvería una constante de la superclase en lugar de la suya.
         */
        protected Object readResolve() throws InvalidObjectException {
            if (this.getClass() != AttributedCharacterIterator.Attribute.class) {
                throw new InvalidObjectException("subclass didn't correctly implement readResolve");
            }
            AttributedCharacterIterator.Attribute instancia = INSTANCIAS.get(this.getName());
            if (instancia != null) {
                return instancia;
            }
            throw new InvalidObjectException("unknown attribute name");
        }
    }

    int getRunStart();

    int getRunStart(AttributedCharacterIterator.Attribute attribute);

    int getRunStart(Set<? extends AttributedCharacterIterator.Attribute> attributes);

    int getRunLimit();

    int getRunLimit(AttributedCharacterIterator.Attribute attribute);

    int getRunLimit(Set<? extends AttributedCharacterIterator.Attribute> attributes);

    Map<AttributedCharacterIterator.Attribute, Object> getAttributes();

    Object getAttribute(AttributedCharacterIterator.Attribute attribute);

    Set<AttributedCharacterIterator.Attribute> getAllAttributeKeys();
}
