package javax.management;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Convierte un MBean estandar --un objeto mas su interfaz-- en un {@link DynamicMBean}.
 *
 * <p>Sirve para dos cosas distintas que conviene no mezclar. La primera es <b>romper la convencion
 * de nombres</b>: un MBean estandar comun obliga a que `Foo` implemente `FooMBean`, y con esta
 * clase cualquier objeto se expone bajo cualquier interfaz que implemente, se llame como se llame.
 * La segunda es <b>retocar los metadatos</b>: la reflexion sabe la firma pero no sabe la
 * descripcion ni el impacto, y para eso estan los ganchos `getDescription`/`getImpact`, que se
 * redefinen en una subclase.
 *
 * <h2>Como se introspecciona</h2>
 *
 * <p>Se mira la <b>interfaz</b>, nunca la clase de la implementacion. Los metodos se reparten asi:
 *
 * <ul>
 *   <li>{@code T getX()} sin parametros y con `T` distinto de `void` -- atributo `X` de lectura;
 *   <li>{@code boolean isX()} sin parametros -- atributo `X` de lectura, con la forma `is`;
 *   <li>{@code void setX(T)} con un parametro -- atributo `X` de escritura;
 *   <li>todo lo demas -- operacion.
 * </ul>
 *
 * <p>La regla se aplica al pie de la letra y por eso {@code String getX(int)} es una operacion y no
 * un atributo indexado: JMX no tiene atributos indexados, y tratarlo como atributo obligaria a
 * inventar un indice.
 *
 * <h2>El impacto queda en `UNKNOWN`</h2>
 *
 * <p>Y no puede ser de otra manera desde la reflexion: saber si una operacion lee o escribe exige
 * leer el cuerpo. `UNKNOWN` es el valor que la especificacion reserva justamente para "no se sabe";
 * poner `ACTION` o `INFO` a ojo seria afirmar algo que no se midio. Quien lo sepa lo declara
 * redefiniendo {@link #getImpact}.
 *
 * <h2>Lo que esta clase <b>no</b> hace: MXBean</h2>
 *
 * <p>Los constructores con `isMXBean` aceptan `false` --que es exactamente el MBean estandar-- y
 * <b>rechazan `true`</b>. La razon ya no es la que decia esta nota: `javax.management.openmbean`
 * <b>si</b> esta, completo, y la conversion de tipos existe --la escribe {@code MXMapeo}, y de ella
 * vive {@link JMX#newMXBeanProxy}, que es el lado <b>cliente</b>--.
 *
 * <p>Lo que falta es el lado <b>servidor</b>, que es otra cosa y mas trabajo: un
 * `StandardMBean(x, I.class, true)` tiene que publicar un {@code MBeanInfo} cuyos atributos y
 * operaciones esten declarados con los tipos <b>abiertos</b>, y convertir en cada `getAttribute`,
 * `setAttribute` e `invoke`. Mientras eso no este escrito, construir uno igual dejaria un objeto que
 * dice ser MXBean y publica tipos Java crudos: una mentira que recien se descubre del lado del
 * cliente. Rechazar en el constructor la deja donde se puede ver.
 */
public class StandardMBean implements DynamicMBean, MBeanRegistration {

    private Object implementacion;
    private final Class<?> interfaz;

    /** Se arma una vez y se guarda: la interfaz no cambia, y la reflexion no es gratis. */
    private MBeanInfo cache;

    /** Nombre de atributo a metodo, resueltos al construir para no buscar en cada llamada. */
    private final Map<String, Method> getters = new TreeMap<String, Method>();
    private final Map<String, Method> setters = new TreeMap<String, Method>();

    /** Operaciones indexadas por nombre; puede haber varias sobrecargas con el mismo nombre. */
    private final Map<String, List<Method>> operaciones = new LinkedHashMap<String, List<Method>>();

    /**
     * El metodo de la interfaz, resuelto contra la clase de la implementacion.
     *
     * <p>Se cachea porque `getMethod` no es barato y el despacho pasa por aca en cada llamada. Se
     * vacia en {@link #setImplementation}, que es lo unico que puede cambiar la respuesta.
     */
    private final Map<Method, Method> resueltos = new java.util.HashMap<Method, Method>();

    /**
     * Envuelve `implementation` para exponerlo bajo `mbeanInterface`.
     *
     * @throws NotCompliantMBeanException si el objeto no implementa esa interfaz, o si la interfaz
     *         no es coherente --por ejemplo un `getX`/`setX` que no hablan del mismo tipo--
     */
    public <T> StandardMBean(T implementation, Class<T> mbeanInterface)
            throws NotCompliantMBeanException {
        if (implementation == null) {
            throw new IllegalArgumentException("La implementacion no puede ser null");
        }
        this.interfaz = elegirInterfaz(implementation, mbeanInterface);
        revisarImplementa(implementation, this.interfaz);
        this.implementacion = implementation;
        introspeccionar();
    }

    /**
     * Para subclasear: la implementacion es `this`.
     *
     * <p>El orden importa y es distinto del otro constructor: aca no se puede revisar que `this`
     * implemente la interfaz hasta que la subclase termine de construirse... pero `this` ya es de
     * la clase final, asi que la comprobacion es valida ahora mismo.
     */
    protected StandardMBean(Class<?> mbeanInterface) throws NotCompliantMBeanException {
        this.interfaz = elegirInterfaz(this, mbeanInterface);
        revisarImplementa(this, this.interfaz);
        this.implementacion = this;
        introspeccionar();
    }

    /**
     * @param isMXBean tiene que ser `false`; ver la nota sobre MXBean en la clase
     * @throws UnsupportedOperationException si es `true`
     * @throws IllegalArgumentException si el objeto o la interfaz no sirven. Este constructor no
     *         declara `NotCompliantMBeanException` --el del JDK tampoco-- y por eso el
     *         incumplimiento sale envuelto en una excepcion no chequeada.
     */
    public <T> StandardMBean(T implementation, Class<T> mbeanInterface, boolean isMXBean) {
        revisarNoMXBean(isMXBean);
        if (implementation == null) {
            throw new IllegalArgumentException("La implementacion no puede ser null");
        }
        this.interfaz = elegirInterfaz(implementation, mbeanInterface);
        try {
            revisarImplementa(implementation, this.interfaz);
            this.implementacion = implementation;
            introspeccionar();
        } catch (NotCompliantMBeanException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * @param isMXBean tiene que ser `false`
     * @throws UnsupportedOperationException si es `true`
     */
    protected StandardMBean(Class<?> mbeanInterface, boolean isMXBean) {
        revisarNoMXBean(isMXBean);
        this.interfaz = elegirInterfaz(this, mbeanInterface);
        try {
            revisarImplementa(this, this.interfaz);
            this.implementacion = this;
            introspeccionar();
        } catch (NotCompliantMBeanException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static void revisarNoMXBean(boolean isMXBean) {
        if (isMXBean) {
            throw new UnsupportedOperationException(
                "Esta biblioteca no tiene javax.management.openmbean, sin el cual un MXBean no "
                + "puede convertir sus tipos. Se acepta isMXBean=false.");
        }
    }

    /**
     * Si no se dio interfaz, se busca la unica que sirve por convencion de nombre.
     *
     * <p>Se acepta `null` porque el JDK lo acepta: significa "descubrila". La convencion es que la
     * clase `p.Foo` se expone por `p.FooMBean`.
     */
    private static Class<?> elegirInterfaz(Object impl, Class<?> declarada) {
        if (declarada != null) {
            return declarada;
        }
        Class<?> c = impl.getClass();
        while (c != null) {
            String esperada = c.getName() + "MBean";
            for (Class<?> i : c.getInterfaces()) {
                if (i.getName().equals(esperada)) {
                    return i;
                }
            }
            c = c.getSuperclass();
        }
        throw new IllegalArgumentException(
            "No se dio interfaz y no hay ninguna que siga la convencion <Clase>MBean");
    }

    private static void revisarImplementa(Object impl, Class<?> iface)
            throws NotCompliantMBeanException {
        if (!iface.isInterface()) {
            throw new NotCompliantMBeanException(iface.getName() + " no es una interfaz");
        }
        if (!iface.isInstance(impl)) {
            throw new NotCompliantMBeanException(
                impl.getClass().getName() + " no implementa " + iface.getName());
        }
    }

    /** Reparte los metodos de la interfaz en atributos y operaciones. */
    private void introspeccionar() throws NotCompliantMBeanException {
        for (Method m : interfaz.getMethods()) {
            String nombre = m.getName();
            Class<?>[] args = m.getParameterTypes();
            Class<?> ret = m.getReturnType();

            if (args.length == 0 && ret != Void.TYPE && nombre.startsWith("get")
                    && nombre.length() > 3) {
                ponerGetter(nombre.substring(3), m);
            } else if (args.length == 0 && ret == Boolean.TYPE && nombre.startsWith("is")
                    && nombre.length() > 2) {
                ponerGetter(nombre.substring(2), m);
            } else if (args.length == 1 && ret == Void.TYPE && nombre.startsWith("set")
                    && nombre.length() > 3) {
                ponerSetter(nombre.substring(3), m);
            } else {
                List<Method> l = operaciones.get(nombre);
                if (l == null) {
                    l = new ArrayList<Method>();
                    operaciones.put(nombre, l);
                }
                l.add(m);
            }
        }
        // Se revisa recien al final porque un `setX` puede aparecer antes que su `getX`.
        for (Map.Entry<String, Method> e : getters.entrySet()) {
            Method set = setters.get(e.getKey());
            if (set != null && !set.getParameterTypes()[0].equals(e.getValue().getReturnType())) {
                throw new NotCompliantMBeanException(
                    "El atributo " + e.getKey() + " tiene getter y setter de tipos distintos");
            }
        }
    }

    private void ponerGetter(String atributo, Method m) throws NotCompliantMBeanException {
        Method previo = getters.get(atributo);
        if (previo != null && !previo.getReturnType().equals(m.getReturnType())) {
            // Pasa con `getX()` y `isX()` juntos, o heredando de dos interfaces distintas.
            throw new NotCompliantMBeanException(
                "El atributo " + atributo + " tiene dos getters de tipos distintos");
        }
        getters.put(atributo, m);
    }

    private void ponerSetter(String atributo, Method m) throws NotCompliantMBeanException {
        Method previo = setters.get(atributo);
        if (previo != null && !previo.getParameterTypes()[0].equals(m.getParameterTypes()[0])) {
            throw new NotCompliantMBeanException(
                "El atributo " + atributo + " tiene dos setters de tipos distintos");
        }
        setters.put(atributo, m);
    }

    /**
     * Cambia el objeto que hay atras sin rehacer la introspeccion: la interfaz es la misma, asi que
     * los metadatos tambien.
     *
     * @throws NotCompliantMBeanException si el objeto nuevo no implementa la interfaz
     */
    public void setImplementation(Object implementation) throws NotCompliantMBeanException {
        if (implementation == null) {
            throw new IllegalArgumentException("La implementacion no puede ser null");
        }
        revisarImplementa(implementation, interfaz);
        this.implementacion = implementation;
        synchronized (resueltos) {
            resueltos.clear();
        }
    }

    public Object getImplementation() {
        return implementacion;
    }

    /** Final: cambiar la interfaz invalidaria los metadatos ya publicados. */
    public final Class<?> getMBeanInterface() {
        return interfaz;
    }

    public Class<?> getImplementationClass() {
        return implementacion.getClass();
    }

    // ---- DynamicMBean -------------------------------------------------------------------------

    public Object getAttribute(String attribute)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
        Method g = getters.get(attribute);
        if (g == null) {
            throw new AttributeNotFoundException("No hay atributo de lectura: " + attribute);
        }
        return llamar(g, new Object[0]);
    }

    public void setAttribute(Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException,
                   ReflectionException {
        Method s = setters.get(attribute.getName());
        if (s == null) {
            throw new AttributeNotFoundException(
                "No hay atributo de escritura: " + attribute.getName());
        }
        try {
            llamar(s, new Object[] { attribute.getValue() });
        } catch (ReflectionException e) {
            // La reflexion tira `IllegalArgumentException` cuando el valor no es del tipo del
            // parametro; en JMX ese caso tiene su propia excepcion y hay que traducirlo.
            if (e.getTargetException() instanceof IllegalArgumentException) {
                throw new InvalidAttributeValueException(
                    "Valor invalido para " + attribute.getName() + ": " + attribute.getValue());
            }
            throw e;
        }
    }

    /**
     * Lee varios, <b>al mejor esfuerzo</b>: los que fallan simplemente no aparecen en el resultado.
     *
     * <p>Es lo que manda la especificacion y no es un descuido. Este metodo existe para ahorrar
     * viajes de red al leer un tablero entero; si un atributo roto tumbara la llamada, un solo
     * MBean con un problema dejaria ciego a todo el tablero.
     */
    public AttributeList getAttributes(String[] attributes) {
        AttributeList salida = new AttributeList();
        if (attributes == null) {
            return salida;
        }
        for (String nombre : attributes) {
            try {
                salida.add(new Attribute(nombre, getAttribute(nombre)));
            } catch (Exception e) {
                // Se omite a proposito: ver el javadoc.
            }
        }
        return salida;
    }

    /** Escribe varios al mejor esfuerzo; devuelve solo los que efectivamente se escribieron. */
    public AttributeList setAttributes(AttributeList attributes) {
        AttributeList salida = new AttributeList();
        if (attributes == null) {
            return salida;
        }
        for (Attribute a : attributes.asList()) {
            try {
                setAttribute(a);
                salida.add(new Attribute(a.getName(), a.getValue()));
            } catch (Exception e) {
                // Idem.
            }
        }
        return salida;
    }

    public Object invoke(String actionName, Object[] params, String[] signature)
            throws MBeanException, ReflectionException {
        List<Method> candidatos = operaciones.get(actionName);
        if (candidatos == null) {
            throw new ReflectionException(
                new NoSuchMethodException(actionName), "No hay operacion " + actionName);
        }
        String[] firma = (signature == null) ? new String[0] : signature;
        for (Method m : candidatos) {
            if (coincideFirma(m, firma)) {
                return llamar(m, params == null ? new Object[0] : params);
            }
        }
        throw new ReflectionException(
            new NoSuchMethodException(actionName),
            "Ninguna sobrecarga de " + actionName + " coincide con la firma dada");
    }

    private static boolean coincideFirma(Method m, String[] firma) {
        Class<?>[] args = m.getParameterTypes();
        if (args.length != firma.length) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
            if (!args[i].getName().equals(firma[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Llama y traduce lo que salga.
     *
     * <p>La traduccion no es decorativa: JMX distingue <b>de quien es la culpa</b>. Lo que tira el
     * MBean va en {@link MBeanException} --es una falla del recurso administrado--; lo que falla al
     * intentar llamarlo va en {@link ReflectionException} --es una falla del agente--. Un cliente
     * remoto que recibe una u otra sabe si reintentar o avisar.
     */
    private Object llamar(Method m, Object[] args) throws MBeanException, ReflectionException {
        try {
            return resolver(m).invoke(implementacion, args);
        } catch (InvocationTargetException e) {
            Throwable causa = e.getTargetException();
            if (causa instanceof RuntimeException) {
                throw new RuntimeMBeanException((RuntimeException) causa,
                        "El MBean tiro " + causa);
            }
            if (causa instanceof Error) {
                throw new RuntimeErrorException((Error) causa, "El MBean tiro " + causa);
            }
            throw new MBeanException((Exception) causa, "El MBean tiro " + causa);
        } catch (IllegalAccessException e) {
            throw new ReflectionException(e, "No se pudo llamar a " + m.getName());
        } catch (IllegalArgumentException e) {
            throw new ReflectionException(e, "Argumentos invalidos para " + m.getName());
        }
    }

    /**
     * Baja el metodo de la interfaz a la clase que de verdad lo implementa.
     *
     * <p>Semanticamente da lo mismo --es el mismo override-- pero hace falta igual: si la
     * implementacion es de una clase no publica, el {@code Method} de esa clase no se puede invocar
     * sin {@code setAccessible}, mientras que el de la interfaz publica si. El JDK necesita esta
     * bajada por ese motivo y por eso se hace aca tambien.
     *
     * <p>Hubo una segunda razon, que ya no aplica: la VM de KajiJDK no redespachaba en
     * {@code Method.invoke} --corria el cuerpo del metodo declarado-- asi que invocar el
     * {@code Method} de la interfaz reventaba cuando era abstracto. Eso esta arreglado (finding
     * #466); esta bajada quedo porque la primera razon sigue en pie.
     */
    private Method resolver(Method m) {
        synchronized (resueltos) {
            Method ya = resueltos.get(m);
            if (ya != null) {
                return ya;
            }
        }
        Method elegido = m;
        try {
            Method concreto = implementacion.getClass().getMethod(m.getName(),
                                                                  m.getParameterTypes());
            if (java.lang.reflect.Modifier.isPublic(concreto.getDeclaringClass().getModifiers())) {
                elegido = concreto;
            }
        } catch (NoSuchMethodException e) {
            // No deberia pasar --la implementacion cumple la interfaz-- pero si pasa, el de la
            // interfaz sigue siendo la mejor apuesta.
        }
        synchronized (resueltos) {
            resueltos.put(m, elegido);
        }
        return elegido;
    }

    /**
     * Los metadatos, con la cache al medio.
     *
     * <p>Se arman una vez y se guardan. Una subclase que quiera metadatos que cambien redefine
     * {@link #getCachedMBeanInfo} para devolver `null`.
     */
    public MBeanInfo getMBeanInfo() {
        MBeanInfo mi = getCachedMBeanInfo();
        if (mi != null) {
            return mi;
        }
        mi = construirMBeanInfo();
        cacheMBeanInfo(mi);
        return mi;
    }

    private MBeanInfo construirMBeanInfo() {
        // Primera pasada: lo que la reflexion sabe, sin descripciones ni impacto.
        List<MBeanAttributeInfo> atrs = new ArrayList<MBeanAttributeInfo>();
        List<String> nombres = new ArrayList<String>(getters.keySet());
        for (String n : setters.keySet()) {
            if (!nombres.contains(n)) {
                nombres.add(n);
            }
        }
        java.util.Collections.sort(nombres);
        for (String n : nombres) {
            try {
                atrs.add(new MBeanAttributeInfo(n, "Atributo expuesto para administracion",
                                                getters.get(n), setters.get(n)));
            } catch (IntrospectionException e) {
                // No puede pasar: `introspeccionar` ya rechazo los pares incoherentes.
                throw new IllegalStateException(e);
            }
        }

        List<MBeanOperationInfo> ops = new ArrayList<MBeanOperationInfo>();
        for (List<Method> l : operaciones.values()) {
            for (Method m : l) {
                ops.add(new MBeanOperationInfo("Operacion expuesta para administracion", m));
            }
        }

        MBeanConstructorInfo[] ctors = constructoresCrudos();

        MBeanInfo crudo = new MBeanInfo(
            getImplementationClass().getName(),
            interfaz.getName(),
            atrs.toArray(new MBeanAttributeInfo[0]),
            ctors,
            ops.toArray(new MBeanOperationInfo[0]),
            notificacionesDe(implementacion));

        // Segunda pasada: los ganchos. La subclase ve el crudo y decide que cambia. Hacerlo en dos
        // pasadas y no llamando a los ganchos mientras se recorre la reflexion es lo que permite
        // que el gancho reciba un `MBeanInfo` completo, con contexto.
        return aplicarGanchos(crudo);
    }

    private MBeanConstructorInfo[] constructoresCrudos() {
        Constructor<?>[] cs = getImplementationClass().getConstructors();
        MBeanConstructorInfo[] r = new MBeanConstructorInfo[cs.length];
        for (int i = 0; i < cs.length; i++) {
            r[i] = new MBeanConstructorInfo("Constructor publico de la clase", cs[i]);
        }
        return getConstructors(r, implementacion);
    }

    private static MBeanNotificationInfo[] notificacionesDe(Object impl) {
        if (impl instanceof NotificationBroadcaster) {
            return ((NotificationBroadcaster) impl).getNotificationInfo();
        }
        return new MBeanNotificationInfo[0];
    }

    private MBeanInfo aplicarGanchos(MBeanInfo crudo) {
        MBeanAttributeInfo[] atrs = crudo.getAttributes();
        MBeanAttributeInfo[] atrs2 = new MBeanAttributeInfo[atrs.length];
        for (int i = 0; i < atrs.length; i++) {
            MBeanAttributeInfo a = atrs[i];
            atrs2[i] = new MBeanAttributeInfo(a.getName(), a.getType(), getDescription(a),
                                              a.isReadable(), a.isWritable(), a.isIs(),
                                              a.getDescriptor());
        }

        MBeanOperationInfo[] ops = crudo.getOperations();
        MBeanOperationInfo[] ops2 = new MBeanOperationInfo[ops.length];
        for (int i = 0; i < ops.length; i++) {
            MBeanOperationInfo o = ops[i];
            ops2[i] = new MBeanOperationInfo(o.getName(), getDescription(o),
                                             params(o, o.getSignature()), o.getReturnType(),
                                             getImpact(o), o.getDescriptor());
        }

        MBeanConstructorInfo[] cs = crudo.getConstructors();
        MBeanConstructorInfo[] cs2 = new MBeanConstructorInfo[cs.length];
        for (int i = 0; i < cs.length; i++) {
            MBeanConstructorInfo c = cs[i];
            cs2[i] = new MBeanConstructorInfo(c.getName(), getDescription(c),
                                              params(c, c.getSignature()), c.getDescriptor());
        }

        return new MBeanInfo(getClassName(crudo), getDescription(crudo), atrs2, cs2, ops2,
                             crudo.getNotifications(), crudo.getDescriptor());
    }

    private MBeanParameterInfo[] params(MBeanOperationInfo op, MBeanParameterInfo[] ps) {
        MBeanParameterInfo[] r = new MBeanParameterInfo[ps.length];
        for (int i = 0; i < ps.length; i++) {
            r[i] = new MBeanParameterInfo(getParameterName(op, ps[i], i), ps[i].getType(),
                                          getDescription(op, ps[i], i), ps[i].getDescriptor());
        }
        return r;
    }

    private MBeanParameterInfo[] params(MBeanConstructorInfo ct, MBeanParameterInfo[] ps) {
        MBeanParameterInfo[] r = new MBeanParameterInfo[ps.length];
        for (int i = 0; i < ps.length; i++) {
            r[i] = new MBeanParameterInfo(getParameterName(ct, ps[i], i), ps[i].getType(),
                                          getDescription(ct, ps[i], i), ps[i].getDescriptor());
        }
        return r;
    }

    // ---- ganchos: por omision devuelven lo que ya venia -----------------------------------------

    protected String getClassName(MBeanInfo info) {
        return info == null ? null : info.getClassName();
    }

    protected String getDescription(MBeanInfo info) {
        return info == null ? null : info.getDescription();
    }

    protected String getDescription(MBeanFeatureInfo info) {
        return info == null ? null : info.getDescription();
    }

    protected String getDescription(MBeanAttributeInfo info) {
        return getDescription((MBeanFeatureInfo) info);
    }

    protected String getDescription(MBeanConstructorInfo info) {
        return getDescription((MBeanFeatureInfo) info);
    }

    protected String getDescription(MBeanConstructorInfo ctor, MBeanParameterInfo param,
                                    int sequence) {
        return param == null ? null : param.getDescription();
    }

    protected String getParameterName(MBeanConstructorInfo ctor, MBeanParameterInfo param,
                                      int sequence) {
        return param == null ? null : param.getName();
    }

    protected String getDescription(MBeanOperationInfo info) {
        return getDescription((MBeanFeatureInfo) info);
    }

    /** `UNKNOWN` salvo que la subclase sepa mas; ver la nota sobre el impacto en la clase. */
    protected int getImpact(MBeanOperationInfo info) {
        return info == null ? MBeanOperationInfo.UNKNOWN : info.getImpact();
    }

    protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param,
                                      int sequence) {
        return param == null ? null : param.getName();
    }

    protected String getDescription(MBeanOperationInfo op, MBeanParameterInfo param,
                                    int sequence) {
        return param == null ? null : param.getDescription();
    }

    /**
     * Que constructores se publican.
     *
     * <p>Por omision, ninguno si el objeto administrado es <b>otro</b>. Es intencional: publicar
     * los constructores sirve para que un cliente instancie el MBean a traves del agente, y eso
     * solo tiene sentido cuando esta clase <b>es</b> el MBean. Al envolver a un tercero, sus
     * constructores construirian el objeto envuelto y no el envoltorio, que no es lo que el cliente
     * pediria.
     */
    protected MBeanConstructorInfo[] getConstructors(MBeanConstructorInfo[] ctors, Object impl) {
        if (ctors == null) {
            return null;
        }
        if (impl != null && impl != this) {
            return new MBeanConstructorInfo[0];
        }
        return ctors;
    }

    /** Lo guardado, o `null` si todavia no se armo. */
    protected MBeanInfo getCachedMBeanInfo() {
        return cache;
    }

    protected void cacheMBeanInfo(MBeanInfo info) {
        cache = info;
    }

    // ---- MBeanRegistration: se delega si el objeto administrado sabe del tema -------------------

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        if (implementacion instanceof MBeanRegistration) {
            return ((MBeanRegistration) implementacion).preRegister(server, name);
        }
        return name;
    }

    public void postRegister(Boolean registrationDone) {
        if (implementacion instanceof MBeanRegistration) {
            ((MBeanRegistration) implementacion).postRegister(registrationDone);
        }
    }

    public void preDeregister() throws Exception {
        if (implementacion instanceof MBeanRegistration) {
            ((MBeanRegistration) implementacion).preDeregister();
        }
    }

    public void postDeregister() {
        if (implementacion instanceof MBeanRegistration) {
            ((MBeanRegistration) implementacion).postDeregister();
        }
    }
}
