package javax.xml.stream;

/**
 * StAX factory discovery, in a single place.
 *
 * <p>The package's three factories follow the same JAXP protocol --system property, and if there is
 * none, the platform implementation-- so the code is here instead of repeated three times. It is
 * not part of the API: it is package-private on purpose.
 *
 * <p>What it does not do, and it is as well to have it written down: it does not read {@code
 * $java.home/conf/stax.properties} nor consult {@link java.util.ServiceLoader}. The first because
 * this library does not install that file, and the second because discovery by service needs to
 * read the classpath's {@code META-INF/services} resources. Whoever wants to plug in another
 * implementation has the system property, which is the step that does work.
 */
final class Factories {

    private Factories() {
    }

    /**
     * The class named by a system property, already instantiated, or null if it is not set.
     */
    static Object fromSystemProperty(String property, Class<?> expected) {
        String className = null;
        try {
            className = System.getProperty(property);
        } catch (SecurityException ignored) {
            // Without permission to read it, it is the same as not being set.
        }
        if (className == null || className.length() == 0) {
            return null;
        }
        return instantiate(className, null, expected);
    }

    /**
     * Loads and instantiates a factory by name.
     *
     * <p>Everything that goes wrong turns into {@link FactoryConfigurationError}, which is what the
     * API promises: naming a class that is no good is a configuration error, not an exception the
     * caller can deal with.
     */
    static Object instantiate(String className, ClassLoader loader, Class<?> expected) {
        try {
            Class<?> c;
            if (loader != null) {
                c = Class.forName(className, true, loader);
            } else {
                ClassLoader ctx = Thread.currentThread().getContextClassLoader();
                if (ctx != null) {
                    c = Class.forName(className, true, ctx);
                } else {
                    c = Class.forName(className);
                }
            }
            Object o = c.newInstance();
            if (!expected.isInstance(o)) {
                throw new FactoryConfigurationError(
                        "the class " + className + " is not a " + expected.getName());
            }
            return o;
        } catch (FactoryConfigurationError e) {
            throw e;
        } catch (Exception e) {
            throw new FactoryConfigurationError(e, "could not instantiate " + className);
        }
    }
}
