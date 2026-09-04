package javax.xml.catalog;

import java.util.HashMap;
import java.util.Map;

/**
 * KajiLibrary's javax.xml.catalog.CatalogFeatures -- como se comporta un catalogo.
 *
 * <p>Cuatro opciones, inmutables una vez armadas. Se construye con {@link #builder}, o se toman las de
 * omision con {@link #defaults}.
 *
 * <h2>Las cuatro</h2>
 *
 * <ul>
 *   <li>{@code FILES}: los catalogos a usar, como URI separados por punto y coma. Sin omision;
 *   <li>{@code PREFER}: cual identificador gana cuando el documento trae los dos. Por omision
 *       {@code "public"};
 *   <li>{@code DEFER}: si los catalogos encadenados se leen recien cuando hacen falta. Por omision
 *       {@code "true"};
 *   <li>{@code RESOLVE}: que hacer cuando no hay coincidencia. Por omision {@code "strict"}.
 * </ul>
 *
 * <h2>De donde sale cada valor</h2>
 *
 * <p>Se busca en tres lugares, en este orden: lo que se le puso al constructor, la propiedad del
 * sistema del mismo nombre, y el valor de omision. Eso es lo que permite cambiar el catalogo de un
 * programa ya compilado con un {@code -D} en la linea de comandos.
 *
 * <h2>{@code RESOLVE} por omision es estricto</h2>
 *
 * <p>Es la trampa: no encontrar una entrada <b>lanza</b> {@link CatalogException} en lugar de dejar
 * pasar. Es lo correcto para un despliegue --si el catalogo no cubre algo, mejor enterarse-- y
 * sorprende a quien lo prueba por primera vez con un catalogo incompleto.
 */
public class CatalogFeatures {

    /** La propiedad de los archivos de catalogo. */
    static final String CATALOG_FILES = "javax.xml.catalog.files";

    /** La de la preferencia. */
    static final String CATALOG_PREFER = "javax.xml.catalog.prefer";

    /** La de la lectura diferida. */
    static final String CATALOG_DEFER = "javax.xml.catalog.defer";

    /** La de que hacer sin coincidencia. */
    static final String CATALOG_RESOLVE = "javax.xml.catalog.resolve";

    /** Gana el identificador de sistema. */
    static final String PREFER_SYSTEM = "system";

    /** Gana el publico. */
    static final String PREFER_PUBLIC = "public";

    /** Leer los encadenados recien cuando hagan falta. */
    static final String DEFER_TRUE = "true";

    /** Leerlos todos al arrancar. */
    static final String DEFER_FALSE = "false";

    /** Sin coincidencia, error. */
    static final String RESOLVE_STRICT = "strict";

    /** Sin coincidencia, seguir por el camino normal. */
    static final String RESOLVE_CONTINUE = "continue";

    /** Sin coincidencia, devolver algo vacio. */
    static final String RESOLVE_IGNORE = "ignore";

    /** Lo que se le puso explicitamente. */
    private final Map<Feature, String> values;

    /**
     * Las cuatro caracteristicas.
     *
     * <p>Cada una conoce su propiedad del sistema y su valor de omision, y por eso
     * {@link #getPropertyName} y {@link #defaultValue} viven aca y no en un mapa aparte.
     */
    public enum Feature {

        /** Los catalogos a usar, separados por punto y coma. Sin omision. */
        FILES(CATALOG_FILES, null),

        /** Cual identificador gana. */
        PREFER(CATALOG_PREFER, PREFER_PUBLIC),

        /** Si los encadenados se leen al vuelo. */
        DEFER(CATALOG_DEFER, DEFER_TRUE),

        /** Que hacer sin coincidencia. */
        RESOLVE(CATALOG_RESOLVE, RESOLVE_STRICT);

        /** La propiedad del sistema equivalente. */
        private final String name;

        /** El valor si nadie dice nada. */
        private final String defaultValue;

        Feature(String name, String value) {
            this.name = name;
            this.defaultValue = value;
        }

        /** La propiedad del sistema equivalente. */
        public String getPropertyName() {
            return this.name;
        }

        /** El valor si nadie dice nada; null para {@link #FILES}. */
        public String defaultValue() {
            return this.defaultValue;
        }

        /** Si ese es el nombre de su propiedad. */
        boolean equalsPropertyName(String propertyName) {
            return this.name.equals(propertyName);
        }

        /** Lo que diga la propiedad del sistema, o null. */
        String getValue() {
            try {
                return System.getProperty(this.name);
            } catch (Throwable e) {
                return null;
            }
        }

        /** Si la propiedad del sistema esta puesta. */
        boolean hasSystemProperty() {
            return getValue() != null;
        }
    }

    /** El armador. Un objeto aparte para que {@link CatalogFeatures} pueda ser inmutable. */
    public static class Builder {

        /** Lo que se fue poniendo. */
        Map<Feature, String> values = new HashMap<Feature, String>();

        /** Solo se llega por {@link CatalogFeatures#builder}. */
        Builder() {
        }

        /**
         * Fija una caracteristica.
         *
         * @throws NullPointerException si la caracteristica o el valor son null
         * @throws IllegalArgumentException si el valor no es uno de los aceptados; los valores
         *     distinguen mayusculas
         */
        public Builder with(Feature feature, String value) {
            if (feature == null) {
                throw new NullPointerException();
            }
            if (value == null) {
                throw CatalogMessages.nullArgument(feature.name());
            }
            validate(feature, value);
            this.values.put(feature, value);
            return this;
        }

        /** Las caracteristicas ya armadas. */
        public CatalogFeatures build() {
            return new CatalogFeatures(this);
        }

        /** Los valores aceptados de cada caracteristica; {@code FILES} acepta cualquier cosa. */
        private static void validate(Feature feature, String value) {
            if (feature == Feature.PREFER) {
                if (!value.equals(PREFER_SYSTEM) && !value.equals(PREFER_PUBLIC)) {
                    throw CatalogMessages.invalidArgument(value, feature.name());
                }
            } else if (feature == Feature.DEFER) {
                if (!value.equals(DEFER_TRUE) && !value.equals(DEFER_FALSE)) {
                    throw CatalogMessages.invalidArgument(value, feature.name());
                }
            } else if (feature == Feature.RESOLVE) {
                if (!value.equals(RESOLVE_STRICT) && !value.equals(RESOLVE_CONTINUE)
                    && !value.equals(RESOLVE_IGNORE)) {
                    throw CatalogMessages.invalidArgument(value, feature.name());
                }
            }
        }
    }

    /** Se llega por {@link Builder#build}. */
    CatalogFeatures(Builder builder) {
        this.values = new HashMap<Feature, String>(builder.values);
    }

    /** Las de omision, sin nada puesto a mano. */
    public static CatalogFeatures defaults() {
        return builder().build();
    }

    /**
     * El valor de esa caracteristica.
     *
     * <p>Lo puesto a mano, si no la propiedad del sistema, si no el valor de omision. Ver la nota de
     * la clase.
     */
    public String get(Feature cf) {
        String explicit = this.values.get(cf);
        if (explicit != null) {
            return explicit;
        }
        String fromProperty = cf.getValue();
        if (fromProperty != null) {
            return fromProperty;
        }
        return cf.defaultValue();
    }

    /** Un armador nuevo. */
    public static Builder builder() {
        return new Builder();
    }
}
