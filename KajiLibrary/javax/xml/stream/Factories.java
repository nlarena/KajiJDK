package javax.xml.stream;

/**
 * El descubrimiento de fabricas de StAX, en un solo lugar.
 *
 * <p>Las tres fabricas del paquete siguen el mismo protocolo de JAXP --propiedad de sistema, y si
 * no hay, la implementacion de la plataforma-- asi que el codigo esta aca en vez de repetido tres
 * veces. No es parte de la API: es de paquete a proposito.
 *
 * <p>Lo que no hace, y conviene que quede escrito: no lee {@code $java.home/conf/stax.properties} ni
 * consulta {@link java.util.ServiceLoader}. El primero porque esta biblioteca no instala ese
 * archivo, y el segundo porque el descubrimiento por servicio necesita leer los recursos
 * {@code META-INF/services} del classpath. Quien quiera enchufar otra implementacion tiene la
 * propiedad de sistema, que es el escalon que si funciona.
 */
final class Factories {

    private Factories() {
    }

    /**
     * La clase nombrada por una propiedad de sistema, ya instanciada, o null si no esta puesta.
     */
    static Object fromSystemProperty(String property, Class<?> expected) {
        String className = null;
        try {
            className = System.getProperty(property);
        } catch (SecurityException ignored) {
            // Sin permiso para leerla es lo mismo que no estar puesta.
        }
        if (className == null || className.length() == 0) {
            return null;
        }
        return instantiate(className, null, expected);
    }

    /**
     * Carga e instancia una fabrica por nombre.
     *
     * <p>Todo lo que salga mal se convierte en {@link FactoryConfigurationError}, que es lo que la
     * API promete: nombrar una clase que no sirve es un error de configuracion, no una excepcion
     * que el llamador pueda atender.
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
                        "la clase " + className + " no es un " + expected.getName());
            }
            return o;
        } catch (FactoryConfigurationError e) {
            throw e;
        } catch (Exception e) {
            throw new FactoryConfigurationError(e, "no se pudo instanciar " + className);
        }
    }
}
