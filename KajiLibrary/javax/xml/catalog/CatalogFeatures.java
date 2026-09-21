package javax.xml.catalog;

import java.util.HashMap;
import java.util.Map;

/**
 * KajiLibrary's javax.xml.catalog.CatalogFeatures -- how a catalog behaves.
 *
 * <p>Four options, immutable once built. It is built with {@link #builder}, or the defaults are
 * taken with {@link #defaults}.
 *
 * <h2>The four</h2>
 *
 * <ul>
 *   <li>{@code FILES}: the catalogs to use, as URIs separated by semicolons. No default;
 *   <li>{@code PREFER}: which identifier wins when the document carries both. By default
 *       {@code "public"};
 *   <li>{@code DEFER}: whether chained catalogs are read only when needed. By default {@code
 *     "true"};
 *   <li>{@code RESOLVE}: what to do when there is no match. By default {@code "strict"}.
 * </ul>
 *
 * <h2>Where each value comes from</h2>
 *
 * <p>It is looked for in three places, in this order: what was given to the builder, the system
 * property of the same name, and the default value. That is what allows changing an already
 * compiled program's catalog with a {@code -D} on the command line.
 *
 * <h2>{@code RESOLVE} is strict by default</h2>
 *
 * <p>It is the trap: not finding an entry <b>throws</b> {@link CatalogException} instead of letting
 * it through. It is right for a deployment --if the catalog does not cover something, better to
 * find out-- and it surprises whoever tries it for the first time with an incomplete catalog.
 */
public class CatalogFeatures {

    /** The property of the catalog files. */
    static final String CATALOG_FILES = "javax.xml.catalog.files";

    /** The one of the preference. */
    static final String CATALOG_PREFER = "javax.xml.catalog.prefer";

    /** The one of deferred reading. */
    static final String CATALOG_DEFER = "javax.xml.catalog.defer";

    /** The one of what to do without a match. */
    static final String CATALOG_RESOLVE = "javax.xml.catalog.resolve";

    /** The system identifier wins. */
    static final String PREFER_SYSTEM = "system";

    /** The public one wins. */
    static final String PREFER_PUBLIC = "public";

    /** Read the chained ones only when they are needed. */
    static final String DEFER_TRUE = "true";

    /** Read them all at startup. */
    static final String DEFER_FALSE = "false";

    /** Without a match, an error. */
    static final String RESOLVE_STRICT = "strict";

    /** Without a match, carry on along the normal path. */
    static final String RESOLVE_CONTINUE = "continue";

    /** Without a match, return something empty. */
    static final String RESOLVE_IGNORE = "ignore";

    /** What was set explicitly. */
    private final Map<Feature, String> values;

    /**
     * The four features.
     *
     * <p>Each one knows its system property and its default value, and that is why
     * {@link #getPropertyName} and {@link #defaultValue} live here and not in a separate map.
     */
    public enum Feature {

        /** The catalogs to use, separated by semicolons. No default. */
        FILES(CATALOG_FILES, null),

        /** Which identifier wins. */
        PREFER(CATALOG_PREFER, PREFER_PUBLIC),

        /** Whether the chained ones are read on the fly. */
        DEFER(CATALOG_DEFER, DEFER_TRUE),

        /** What to do without a match. */
        RESOLVE(CATALOG_RESOLVE, RESOLVE_STRICT);

        /** The equivalent system property. */
        private final String name;

        /** The value if nobody says anything. */
        private final String defaultValue;

        Feature(String name, String value) {
            this.name = name;
            this.defaultValue = value;
        }

        /** The equivalent system property. */
        public String getPropertyName() {
            return this.name;
        }

        /** The value if nobody says anything; null for {@link #FILES}. */
        public String defaultValue() {
            return this.defaultValue;
        }

        /** Whether that is the name of its property. */
        boolean equalsPropertyName(String propertyName) {
            return this.name.equals(propertyName);
        }

        /** Whatever the system property says, or null. */
        String getValue() {
            try {
                return System.getProperty(this.name);
            } catch (Throwable e) {
                return null;
            }
        }

        /** Whether the system property is set. */
        boolean hasSystemProperty() {
            return getValue() != null;
        }
    }

    /** The builder. A separate object so that {@link CatalogFeatures} can be immutable. */
    public static class Builder {

        /** What has been set so far. */
        Map<Feature, String> values = new HashMap<Feature, String>();

        /** Only reached through {@link CatalogFeatures#builder}. */
        Builder() {
        }

        /**
         * Sets a feature.
         *
         * @throws NullPointerException if the feature or the value is null
         * @throws IllegalArgumentException if the value is not one of the accepted ones; the values
         *     are case-sensitive
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

        /** The features, built. */
        public CatalogFeatures build() {
            return new CatalogFeatures(this);
        }

        /** The accepted values of each feature; {@code FILES} accepts anything. */
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

    /** Reached through {@link Builder#build}. */
    CatalogFeatures(Builder builder) {
        this.values = new HashMap<Feature, String>(builder.values);
    }

    /** The defaults, with nothing set by hand. */
    public static CatalogFeatures defaults() {
        return builder().build();
    }

    /**
     * The value of that feature.
     *
     * <p>What was set by hand, otherwise the system property, otherwise the default value. See the
     * class note.
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

    /** A new builder. */
    public static Builder builder() {
        return new Builder();
    }
}
