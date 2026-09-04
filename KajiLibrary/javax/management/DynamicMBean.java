package javax.management;

/**
 * El MBean que se describe a si mismo en tiempo de ejecucion.
 *
 * <p>Es el contrapunto del MBean estandar. Uno estandar declara su interfaz en metodos Java y el
 * agente la descubre por reflexion sobre los nombres; uno dinamico no tiene interfaz que mirar --
 * devuelve su {@link MBeanInfo} desde {@link #getMBeanInfo()} y atiende los pedidos por nombre.
 *
 * <p>La consecuencia practica: un MBean dinamico puede cambiar sus atributos entre dos llamadas.
 * Es como se instrumentan cosas cuya forma no se conoce al compilar --una tabla de configuracion,
 * un modelo cargado de un archivo-- sin generar clases.
 */
public interface DynamicMBean {

    /** Lee un atributo por nombre. */
    Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException, ReflectionException;

    /** Escribe un atributo. */
    void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
                   MBeanException, ReflectionException;

    /**
     * Lee varios.
     *
     * <p>No declara excepciones: los atributos que fallan se omiten de la respuesta, y por eso la
     * lista devuelta puede ser mas corta que el pedido.
     */
    AttributeList getAttributes(String[] attributes);

    /** Escribe varios; devuelve los que efectivamente se escribieron. */
    AttributeList setAttributes(AttributeList attributes);

    /**
     * Invoca una operacion.
     *
     * @param signature los nombres de las clases de los parametros, para desambiguar sobrecargas
     */
    Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException;

    /** Que atributos, operaciones, constructores y notificaciones tiene <b>ahora</b>. */
    MBeanInfo getMBeanInfo();
}
