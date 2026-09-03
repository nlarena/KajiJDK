package java.io;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * KajiLibrary's java.io.ObjectOutputStream -- el lado que escribe del formato de serializacion.
 *
 * <p>Produce **los mismos bytes que el JDK**, y eso no es un lujo: el unico proposito del formato es
 * que otra JVM lea lo que esta escribio. Un flujo parecido pero no igual no sirve para nada, asi que
 * la prueba de esta clase no es "se puede volver a leer" sino la comparacion byte por byte contra el
 * flujo que produce el JDK real para el mismo objeto.
 *
 * <h2>Las tres capas</h2>
 *
 * <p>Un flujo serializado tiene tres cosas encimadas, y confundirlas es de donde salen casi todos
 * los errores de formato:
 *
 * <ol>
 *   <li><b>Los bytes de bloque.</b> Todo lo que se escribe con los metodos de {@link DataOutput}
 *       --{@code writeInt}, {@code write(int)}-- mientras el flujo esta en "modo bloque" se junta en
 *       un buffer y sale envuelto en un registro {@code TC_BLOCKDATA} con su largo adelante. Sin ese
 *       largo, el lector no sabria donde termina lo que escribio un {@code writeObject} a mano.
 *   <li><b>Las manijas.</b> Cada objeto, cadena, clase y descriptor que sale al flujo se numera
 *       desde {@code baseWireHandle}, y la segunda vez que aparece sale como
 *       {@code TC_REFERENCE + numero}. Es lo que hace que un grafo con ciclos termine, y lo que
 *       conserva la identidad compartida: dos campos que apuntaban al mismo objeto siguen
 *       apuntando al mismo objeto del otro lado.
 *   <li><b>Los descriptores.</b> Antes de los datos de un objeto va la forma de su clase: nombre,
 *       {@code serialVersionUID}, banderas y campos, y despues el descriptor de su superclase. La
 *       cadena de descriptores es lo que permite leer un objeto cuya clase de este lado tiene otros
 *       campos que la del otro.
 * </ol>
 *
 * <h2>Lo que se escribe de cada objeto</h2>
 *
 * <p>Los datos salen **de arriba hacia abajo** de la jerarquia: primero los campos de la superclase
 * serializable mas alta y al final los de la clase concreta. El orden importa porque el lector
 * reconstruye en el mismo sentido, y porque una clase que agrega campos abajo tiene que poder
 * leerse con un lector que solo conoce la parte de arriba.
 *
 * <p>Una clase que declara {@code private void writeObject(ObjectOutputStream)} escribe **ella** su
 * porcion, en modo bloque y terminada con {@code TC_ENDBLOCKDATA}. Ese cierre es lo que le permite
 * al lector saltear datos que no entiende: sin el, una clase que escribio de mas dejaria el flujo
 * desalineado para siempre.
 *
 * <h2>{@code writeReplace} no se consulta</h2>
 *
 * <p>La especificacion permite que una clase declare {@code Object writeReplace()} para mandar otro
 * objeto en su lugar. Aca **no se llama**, y es la unica desviacion del formato que tiene esta
 * clase: hacerlo bien pide reproducir las reglas de accesibilidad de la especificacion --el metodo
 * vale si es privado de la clase, o si es accesible desde ella por herencia dentro del mismo
 * paquete-- y equivocarse en ese detalle en cualquiera de los dos sentidos escribe un objeto
 * distinto del que el JDK escribiria, en silencio. Se avisa aca en vez de adivinar. Su contraparte
 * {@code readResolve} tampoco se llama en {@link ObjectInputStream}, asi que los dos lados de esta
 * biblioteca siguen entendiendose entre si.
 */
public class ObjectOutputStream extends OutputStream implements ObjectOutput, ObjectStreamConstants {

    private final SalidaBloques bout;
    private final Manijas manijas = new Manijas();
    // Los descriptores se cachean **por flujo**: `ObjectStreamClass.lookup` devuelve una instancia
    // nueva cada vez, y las manijas se reparten por identidad. Sin el cache, la misma clase sacaria
    // un descriptor completo por cada objeto en vez de una referencia de cinco bytes.
    private Class<?>[] cacheClases = new Class<?>[16];
    private ObjectStreamClass[] cacheDescs = new ObjectStreamClass[16];
    private int cacheCuantos;

    // Las firmas de tipo ya escritas, con la manija que les toco. Existe para que la misma firma
    // --`Ljava/lang/String;` aparece en casi todo descriptor-- salga una sola vez y despues por
    // referencia, que es lo que hace el JDK y por lo tanto lo que hay que escribir para producir sus
    // mismos bytes. El JDK lo consigue internando la firma en `ObjectStreamField` y buscandola por
    // identidad en la tabla de manijas; esta VM no tiene `String.intern` --su nativo no esta--, asi
    // que la tabla busca por valor. Lo unico que no coincide es un caso que no ocurre: un usuario
    // que escribiera con `writeObject` el literal interno igual a una firma compartiria manija con
    // ella en el JDK y no aca.
    private String[] firmas = new String[8];
    private int[] manijasFirmas = new int[8];
    private int cuantasFirmas;

    private int protocolo = ObjectStreamConstants.PROTOCOL_VERSION_2;
    private boolean reemplazoHabilitado;
    // El constructor sin argumentos es para una subclase que reimplementa todo. Con `delegado` en
    // `true` no hay flujo abajo, y `writeObject` va a `writeObjectOverride`.
    private final boolean delegado;

    private PutFieldImpl putActual;
    private Object objetoActual;
    private ObjectStreamClass descActual;

    /**
     * Envuelve `out` y **escribe la cabecera ahi mismo**.
     *
     * <p>Escribir en el constructor sorprende, y es lo que manda la especificacion: los cuatro bytes
     * de cabecera tienen que estar antes que cualquier otra cosa, y un lector que se conecta al otro
     * extremo de un socket se queda esperandolos. Si se difirieran al primer {@code writeObject},
     * dos procesos que se abren mutuamente un flujo se trabarian, cada uno esperando la cabecera del
     * otro.
     */
    public ObjectOutputStream(OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException();
        }
        this.bout = new SalidaBloques(out);
        this.delegado = false;
        this.writeStreamHeader();
        this.bout.modoBloque(true);
    }

    /**
     * Para una subclase que reimplementa la serializacion entera.
     *
     * <p>No hay flujo abajo: todo metodo que escribiria bytes tira {@link NotActiveException} o no
     * hace nada, y {@link #writeObject} llama a {@link #writeObjectOverride}. Es `protected` porque
     * solo tiene sentido desde adentro de una subclase.
     */
    protected ObjectOutputStream() throws IOException, SecurityException {
        this.bout = null;
        this.delegado = true;
    }

    // ---- la cabecera y los ganchos de subclase ---------------------------------------------------

    /** Los cuatro bytes con que empieza todo flujo: el magico y la version. */
    protected void writeStreamHeader() throws IOException {
        this.bout.escribirShort(ObjectStreamConstants.STREAM_MAGIC);
        this.bout.escribirShort(ObjectStreamConstants.STREAM_VERSION);
    }

    /**
     * Escribe el descriptor de una clase. Una subclase puede cambiar el formato; lo unico que tiene
     * que sostener es que {@link ObjectInputStream#readClassDescriptor} lo lea igual.
     */
    protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
        this.escribirDescNoProxy(desc);
    }

    /**
     * Gancho para agregarle datos propios al descriptor de `cl`. Vacio aca, como en el JDK: lo que
     * se escriba tiene que leerlo {@code resolveClass} del otro lado.
     */
    protected void annotateClass(Class<?> cl) throws IOException {
    }

    /** Lo mismo para una clase proxy. Vacio, y por la misma razon. */
    protected void annotateProxyClass(Class<?> cl) throws IOException {
    }

    /**
     * A donde va {@link #writeObject} cuando el flujo se construyo con el constructor sin
     * argumentos. Aca no hace nada: la subclase que usa ese constructor es la que tiene que
     * escribir.
     */
    protected void writeObjectOverride(Object obj) throws IOException {
    }

    /**
     * El ultimo filtro antes de escribir: si {@link #enableReplaceObject} esta activo, cada objeto
     * pasa por aca y sale al flujo lo que se devuelva. Identidad aca, como en el JDK.
     */
    protected Object replaceObject(Object obj) throws IOException {
        return obj;
    }

    /**
     * Prende o apaga el filtro de {@link #replaceObject}, y devuelve como estaba.
     *
     * <p>El JDK le pide permiso al gestor de seguridad porque reemplazar objetos al vuelo es una
     * forma de que un flujo diga una cosa y transporte otra. Aca no hay gestor de seguridad, asi que
     * el permiso no se consulta y la bandera se toma tal cual.
     */
    protected boolean enableReplaceObject(boolean enable) throws SecurityException {
        boolean antes = this.reemplazoHabilitado;
        this.reemplazoHabilitado = enable;
        return antes;
    }

    /**
     * Vacia el buffer de bloque al flujo de abajo **sin vaciar el flujo de abajo**.
     *
     * <p>La diferencia con {@link #flush} es exactamente esa, y es la razon de que existan las dos:
     * `drain` cierra el registro de bloque en curso, `flush` ademas empuja los bytes hacia el
     * dispositivo.
     */
    protected void drain() throws IOException {
        this.bout.vaciarBloque();
    }

    // ---- el estado del flujo ---------------------------------------------------------------------

    /**
     * Elige la version del protocolo.
     *
     * <p>La unica diferencia visible es como se enmarcan los datos de una {@link Externalizable}:
     * en {@code PROTOCOL_VERSION_1} salen crudos y en {@code PROTOCOL_VERSION_2} en modo bloque, con
     * su {@code TC_ENDBLOCKDATA}. Suena menor y no lo es: solo la segunda deja saltear el objeto sin
     * entender su clase.
     *
     * @throws IllegalStateException si ya se escribio algun objeto
     * @throws IllegalArgumentException si la version no es una de las dos
     */
    public void useProtocolVersion(int version) throws IOException {
        if (this.manijas.cuantas() != 0) {
            throw new IllegalStateException("stream non-empty");
        }
        if (version != ObjectStreamConstants.PROTOCOL_VERSION_1 && version != ObjectStreamConstants.PROTOCOL_VERSION_2) {
            throw new IllegalArgumentException("unknown version: " + version);
        }
        this.protocolo = version;
    }

    /**
     * Corta la memoria del flujo: manijas y descriptores arrancan de cero.
     *
     * <p>Sirve para no acumular todo el grafo escrito en una conexion larga. El precio es que lo que
     * venga despues se escribe entero de nuevo, y que **la identidad compartida no cruza el corte**:
     * un objeto ya escrito vuelve a salir completo y del otro lado seran dos.
     *
     * @throws IOException si se llama desde adentro de un {@code writeObject} de una clase
     */
    public void reset() throws IOException {
        if (this.profundidadObjeto()) {
            throw new IOException("stream active");
        }
        this.bout.modoBloque(false);
        this.bout.escribirByte(ObjectStreamConstants.TC_RESET);
        this.manijas.limpiar();
        this.cacheCuantos = 0;
        // Las firmas guardan manijas, y despues del corte esas manijas ya no existen: dejarlas
        // haria escribir una referencia a un numero que del otro lado quedo sin asignar.
        this.cuantasFirmas = 0;
        this.bout.modoBloque(true);
    }

    private boolean profundidadObjeto() {
        return this.objetoActual != null;
    }

    // ---- escribir objetos -------------------------------------------------------------------------

    /**
     * Escribe `obj` y todo lo que cuelgue de el.
     *
     * <p>Es `final` como en el JDK: la subclase que quiera cambiar que se escribe tiene
     * {@link #writeObjectOverride} y {@link #replaceObject}, y dejar redefinir la entrada permitiria
     * escribir un objeto salteando las manijas, que es como se rompe un grafo con ciclos.
     */
    public final void writeObject(Object obj) throws IOException {
        if (this.delegado) {
            this.writeObjectOverride(obj);
            return;
        }
        this.escribirObjeto0(obj, false);
    }

    /**
     * Escribe `obj` **sin compartir**: no se lo asocia a una manija reusable, asi que si el mismo
     * objeto vuelve a aparecer sale entero otra vez y del otro lado seran dos objetos distintos.
     *
     * <p>Es lo que se quiere para un campo que tiene que ser privado del objeto que lo contiene --un
     * arreglo interno que nadie mas puede ver-- y evita que el otro lado reciba un alias a algo que
     * de este lado nadie compartia.
     */
    public void writeUnshared(Object obj) throws IOException {
        this.escribirObjeto0(obj, true);
    }

    private void escribirObjeto0(Object obj, boolean sinCompartir) throws IOException {
        boolean bloqueAntes = this.bout.modoBloque(false);
        try {
            if (obj == null) {
                this.bout.escribirByte(ObjectStreamConstants.TC_NULL);
                return;
            }
            if (!sinCompartir) {
                int m = this.manijas.buscar(obj);
                if (m >= 0) {
                    this.bout.escribirByte(ObjectStreamConstants.TC_REFERENCE);
                    this.bout.escribirInt(ObjectStreamConstants.baseWireHandle + m);
                    return;
                }
            }
            // Una `Class` y un `ObjectStreamClass` se escriben antes del filtro de reemplazo: son
            // parte de la descripcion del flujo y no datos del usuario.
            if (obj instanceof Class) {
                this.escribirClase((Class<?>) obj, sinCompartir);
                return;
            }
            if (obj instanceof ObjectStreamClass) {
                this.escribirDesc((ObjectStreamClass) obj, sinCompartir);
                return;
            }

            Object valor = obj;
            if (this.reemplazoHabilitado) {
                Object rep = this.replaceObject(valor);
                if (rep != valor) {
                    valor = rep;
                    // El reemplazo puede haber devuelto algo ya escrito.
                    if (valor == null) {
                        this.bout.escribirByte(ObjectStreamConstants.TC_NULL);
                        return;
                    }
                    if (!sinCompartir) {
                        int m = this.manijas.buscar(valor);
                        if (m >= 0) {
                            this.bout.escribirByte(ObjectStreamConstants.TC_REFERENCE);
                            this.bout.escribirInt(ObjectStreamConstants.baseWireHandle + m);
                            return;
                        }
                    }
                }
            }

            Class<?> cl = valor.getClass();
            if (valor instanceof String) {
                this.escribirCadena((String) valor, sinCompartir);
            } else if (cl.isArray()) {
                this.escribirArreglo(valor, cl, sinCompartir);
            } else if (valor instanceof Enum) {
                this.escribirEnum((Enum<?>) valor, sinCompartir);
            } else if (valor instanceof Serializable) {
                this.escribirObjetoComun(valor, cl, sinCompartir);
            } else {
                throw new NotSerializableException(cl.getName());
            }
        } finally {
            this.bout.modoBloque(bloqueAntes);
        }
    }

    private void escribirCadena(String s, boolean sinCompartir) throws IOException {
        long largo = SalidaBloques.largoUtf(s);
        if (largo <= 0xFFFFL) {
            this.bout.escribirByte(ObjectStreamConstants.TC_STRING);
            this.manijas.asignar(sinCompartir ? null : s);
            this.bout.escribirUtfCorta(s, (int) largo);
        } else {
            // Mas de 64 KB codificados no entran en el largo de dos bytes del formato clasico. El
            // registro largo existe justo para eso y lleva el largo en ocho.
            this.bout.escribirByte(ObjectStreamConstants.TC_LONGSTRING);
            this.manijas.asignar(sinCompartir ? null : s);
            this.bout.escribirLong(largo);
            this.bout.escribirUtfCruda(s);
        }
    }

    /**
     * La firma de tipo de un campo: entera la primera vez, y por referencia despues.
     *
     * <p>La manija que se registra es la que la cadena va a recibir, y por eso se toma **antes** de
     * escribirla: el numero se reparte al escribir, no al registrar.
     */
    private void escribirCadenaTipo(String s) throws IOException {
        if (s == null) {
            this.bout.escribirByte(ObjectStreamConstants.TC_NULL);
            return;
        }
        int i = 0;
        while (i < this.cuantasFirmas) {
            if (this.firmas[i].equals(s)) {
                this.bout.escribirByte(ObjectStreamConstants.TC_REFERENCE);
                this.bout.escribirInt(ObjectStreamConstants.baseWireHandle + this.manijasFirmas[i]);
                return;
            }
            i = i + 1;
        }
        int m = this.manijas.cuantas();
        this.escribirCadena(s, false);
        if (this.cuantasFirmas == this.firmas.length) {
            String[] f2 = new String[this.cuantasFirmas * 2];
            int[] m2 = new int[this.cuantasFirmas * 2];
            System.arraycopy(this.firmas, 0, f2, 0, this.cuantasFirmas);
            System.arraycopy(this.manijasFirmas, 0, m2, 0, this.cuantasFirmas);
            this.firmas = f2;
            this.manijasFirmas = m2;
        }
        this.firmas[this.cuantasFirmas] = s;
        this.manijasFirmas[this.cuantasFirmas] = m;
        this.cuantasFirmas = this.cuantasFirmas + 1;
    }

    private void escribirClase(Class<?> cl, boolean sinCompartir) throws IOException {
        this.bout.escribirByte(ObjectStreamConstants.TC_CLASS);
        this.escribirDesc(this.descDe(cl), false);
        this.manijas.asignar(sinCompartir ? null : cl);
    }

    private void escribirEnum(Enum<?> en, boolean sinCompartir) throws IOException {
        this.bout.escribirByte(ObjectStreamConstants.TC_ENUM);
        // La constante con cuerpo propio es una subclase anonima del enum, y lo que va al flujo es
        // **el enum**: del otro lado la constante se busca por nombre, y el nombre pertenece al tipo
        // declarado, no a la subclase que le dio el cuerpo.
        Class<?> cl = en.getClass();
        if (cl.getSuperclass() != Enum.class) {
            cl = cl.getSuperclass();
        }
        this.escribirDesc(this.descDe(cl), false);
        this.manijas.asignar(sinCompartir ? null : en);
        this.escribirCadena(en.name(), false);
    }

    private void escribirArreglo(Object arr, Class<?> cl, boolean sinCompartir) throws IOException {
        this.bout.escribirByte(ObjectStreamConstants.TC_ARRAY);
        this.escribirDesc(this.descDe(cl), false);
        this.manijas.asignar(sinCompartir ? null : arr);
        Class<?> comp = cl.getComponentType();
        if (comp.isPrimitive()) {
            this.escribirPrimitivos(arr, comp);
        } else {
            Object[] a = (Object[]) arr;
            this.bout.escribirInt(a.length);
            int i = 0;
            while (i < a.length) {
                this.escribirObjeto0(a[i], false);
                i = i + 1;
            }
        }
    }

    private void escribirPrimitivos(Object arr, Class<?> comp) throws IOException {
        if (comp == int.class) {
            int[] a = (int[]) arr;
            this.bout.escribirInt(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.escribirInt(a[i]);
            }
        } else if (comp == byte.class) {
            byte[] a = (byte[]) arr;
            this.bout.escribirInt(a.length);
            this.bout.escribirCrudo(a, 0, a.length);
        } else if (comp == long.class) {
            long[] a = (long[]) arr;
            this.bout.escribirInt(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.escribirLong(a[i]);
            }
        } else if (comp == boolean.class) {
            boolean[] a = (boolean[]) arr;
            this.bout.escribirInt(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.escribirByte(a[i] ? 1 : 0);
            }
        } else if (comp == char.class) {
            char[] a = (char[]) arr;
            this.bout.escribirInt(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.escribirShort(a[i]);
            }
        } else if (comp == short.class) {
            short[] a = (short[]) arr;
            this.bout.escribirInt(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.escribirShort(a[i]);
            }
        } else if (comp == float.class) {
            float[] a = (float[]) arr;
            this.bout.escribirInt(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.escribirInt(Float.floatToIntBits(a[i]));
            }
        } else {
            double[] a = (double[]) arr;
            this.bout.escribirInt(a.length);
            for (int i = 0; i < a.length; i++) {
                this.bout.escribirLong(Double.doubleToLongBits(a[i]));
            }
        }
    }

    private void escribirObjetoComun(Object obj, Class<?> cl, boolean sinCompartir)
            throws IOException {
        ObjectStreamClass desc = this.descDe(cl);
        this.bout.escribirByte(ObjectStreamConstants.TC_OBJECT);
        this.escribirDesc(desc, false);
        this.manijas.asignar(sinCompartir ? null : obj);
        if (obj instanceof Externalizable) {
            this.escribirDatosExternos((Externalizable) obj);
        } else {
            this.escribirDatosSerie(obj, cl);
        }
    }

    private void escribirDatosExternos(Externalizable obj) throws IOException {
        Object objAntes = this.objetoActual;
        PutFieldImpl putAntes = this.putActual;
        this.objetoActual = obj;
        this.putActual = null;
        try {
            if (this.protocolo == ObjectStreamConstants.PROTOCOL_VERSION_1) {
                // El protocolo 1 escribe crudo: sin marco, no se puede saltear lo que no se entiende.
                obj.writeExternal(this);
            } else {
                this.bout.modoBloque(true);
                obj.writeExternal(this);
                this.bout.modoBloque(false);
                this.bout.escribirByte(ObjectStreamConstants.TC_ENDBLOCKDATA);
            }
        } finally {
            this.objetoActual = objAntes;
            this.putActual = putAntes;
        }
    }

    /**
     * Los datos de un objeto comun, **de la superclase serializable mas alta hacia abajo**.
     *
     * <p>Ese sentido es el que hace que un lector que solo conoce la parte de arriba de la jerarquia
     * pueda leer lo que entiende y saltear el resto: si los datos de la subclase vinieran primero, no
     * habria forma de llegar a los de la superclase sin entender antes los de abajo.
     */
    private void escribirDatosSerie(Object obj, Class<?> cl) throws IOException {
        Class<?>[] jerarquia = jerarquiaSerializable(cl);
        int i = 0;
        while (i < jerarquia.length) {
            Class<?> c = jerarquia[i];
            ObjectStreamClass slot = this.descDe(c);
            Method escritor = metodoWriteObject(c);
            Object objAntes = this.objetoActual;
            ObjectStreamClass descAntes = this.descActual;
            PutFieldImpl putAntes = this.putActual;
            this.objetoActual = obj;
            this.descActual = slot;
            this.putActual = null;
            try {
                if (escritor != null) {
                    this.bout.modoBloque(true);
                    escritor.invoke(obj, new Object[] { this });
                    this.bout.modoBloque(false);
                    this.bout.escribirByte(ObjectStreamConstants.TC_ENDBLOCKDATA);
                } else {
                    this.escribirCamposPorDefecto(obj, c, slot);
                }
            } finally {
                this.objetoActual = objAntes;
                this.descActual = descAntes;
                this.putActual = putAntes;
            }
            i = i + 1;
        }
    }

    private void escribirCamposPorDefecto(Object obj, Class<?> cl, ObjectStreamClass desc)
            throws IOException {
        ObjectStreamField[] campos = desc.getFields();
        // Los primitivos primero y pegados, que es como estan ordenados y a lo que apuntan sus
        // offsets; despues las referencias, cada una como un objeto entero.
        int i = 0;
        while (i < campos.length && campos[i].isPrimitive()) {
            Field f = campoReal(cl, campos[i].getName(), campos[i].getType());
            char t = campos[i].getTypeCode();
            if (t == 'B') {
                this.bout.escribirByte(f == null ? 0 : f.getByte(obj));
            } else if (t == 'Z') {
                this.bout.escribirByte(f != null && f.getBoolean(obj) ? 1 : 0);
            } else if (t == 'C') {
                this.bout.escribirShort(f == null ? 0 : f.getChar(obj));
            } else if (t == 'S') {
                this.bout.escribirShort(f == null ? 0 : f.getShort(obj));
            } else if (t == 'I') {
                this.bout.escribirInt(f == null ? 0 : f.getInt(obj));
            } else if (t == 'J') {
                this.bout.escribirLong(f == null ? 0L : f.getLong(obj));
            } else if (t == 'F') {
                this.bout.escribirInt(Float.floatToIntBits(f == null ? 0F : f.getFloat(obj)));
            } else {
                this.bout.escribirLong(Double.doubleToLongBits(f == null ? 0D : f.getDouble(obj)));
            }
            i = i + 1;
        }
        while (i < campos.length) {
            Field f = campoReal(cl, campos[i].getName(), campos[i].getType());
            this.escribirObjeto0(f == null ? null : f.get(obj), campos[i].isUnshared());
            i = i + 1;
        }
    }

    // ---- descriptores -----------------------------------------------------------------------------

    private void escribirDesc(ObjectStreamClass desc, boolean sinCompartir) throws IOException {
        if (desc == null) {
            this.bout.escribirByte(ObjectStreamConstants.TC_NULL);
            return;
        }
        int m = this.manijas.buscar(desc);
        if (m >= 0) {
            this.bout.escribirByte(ObjectStreamConstants.TC_REFERENCE);
            this.bout.escribirInt(ObjectStreamConstants.baseWireHandle + m);
            return;
        }
        this.writeClassDescriptor(desc);
    }

    private void escribirDescNoProxy(ObjectStreamClass desc) throws IOException {
        Class<?> cl = desc.forClass();
        this.bout.escribirByte(ObjectStreamConstants.TC_CLASSDESC);
        this.bout.escribirUtf(desc.getName());
        this.bout.escribirLong(desc.getSerialVersionUID());
        // La manija se asigna **despues del nombre y el UID y antes de los campos**: un campo cuyo
        // tipo es la clase misma tiene que poder referenciarla, y solo puede si ya esta numerada.
        this.manijas.asignar(desc);

        boolean externalizable = Externalizable.class.isAssignableFrom(cl);
        boolean serializable = Serializable.class.isAssignableFrom(cl);
        int banderas = 0;
        if (externalizable) {
            banderas = banderas | ObjectStreamConstants.SC_EXTERNALIZABLE;
            if (this.protocolo != ObjectStreamConstants.PROTOCOL_VERSION_1) {
                banderas = banderas | ObjectStreamConstants.SC_BLOCK_DATA;
            }
        } else if (serializable) {
            banderas = banderas | ObjectStreamConstants.SC_SERIALIZABLE;
        }
        if (!externalizable && metodoWriteObject(cl) != null) {
            banderas = banderas | ObjectStreamConstants.SC_WRITE_METHOD;
        }
        if (Enum.class.isAssignableFrom(cl)) {
            banderas = banderas | ObjectStreamConstants.SC_ENUM;
        }
        this.bout.escribirByte(banderas);

        ObjectStreamField[] campos = desc.getFields();
        this.bout.escribirShort(campos.length);
        int i = 0;
        while (i < campos.length) {
            this.bout.escribirByte(campos[i].getTypeCode());
            this.bout.escribirUtf(campos[i].getName());
            if (!campos[i].isPrimitive()) {
                // El nombre del tipo es **una cadena del flujo con su propia manija**, no un UTF
                // suelto: la misma firma se repite en muchisimos campos y compartirla es la mitad
                // del tamanio de un descriptor grande.
                this.escribirCadenaTipo(campos[i].getTypeString());
            }
            i = i + 1;
        }

        this.bout.modoBloque(true);
        if (cl != null) {
            this.annotateClass(cl);
        }
        this.bout.modoBloque(false);
        this.bout.escribirByte(ObjectStreamConstants.TC_ENDBLOCKDATA);

        this.escribirDesc(this.descSuperior(cl), false);
    }

    // El descriptor de la superclase que sigue participando, o `null` cuando se acabo la parte
    // serializable de la jerarquia. Ese `null` es el que le dice al lector donde termina la cadena.
    private ObjectStreamClass descSuperior(Class<?> cl) {
        if (cl == null || cl.isArray()) {
            return null;
        }
        Class<?> sup = cl.getSuperclass();
        if (sup == null || !Serializable.class.isAssignableFrom(sup)) {
            return null;
        }
        return this.descDe(sup);
    }

    private ObjectStreamClass descDe(Class<?> cl) {
        int i = 0;
        while (i < this.cacheCuantos) {
            if (this.cacheClases[i] == cl) {
                return this.cacheDescs[i];
            }
            i = i + 1;
        }
        ObjectStreamClass d = ObjectStreamClass.lookupAny(cl);
        if (this.cacheCuantos == this.cacheClases.length) {
            Class<?>[] c2 = new Class<?>[this.cacheCuantos * 2];
            ObjectStreamClass[] d2 = new ObjectStreamClass[this.cacheCuantos * 2];
            System.arraycopy(this.cacheClases, 0, c2, 0, this.cacheCuantos);
            System.arraycopy(this.cacheDescs, 0, d2, 0, this.cacheCuantos);
            this.cacheClases = c2;
            this.cacheDescs = d2;
        }
        this.cacheClases[this.cacheCuantos] = cl;
        this.cacheDescs[this.cacheCuantos] = d;
        this.cacheCuantos = this.cacheCuantos + 1;
        return d;
    }

    // ---- campos por nombre (PutField) -------------------------------------------------------------

    /**
     * Un buffer donde ir dejando los campos por nombre en vez de por reflexion, para que
     * {@link #writeFields} los escriba despues.
     *
     * <p>Es la salida para una clase que quiere elegir que valor sale en cada campo declarado --por
     * ejemplo escribir una version normalizada de un campo, o rellenar uno que ya no existe en el
     * codigo-- sin dejar de producir exactamente el mismo formato que la escritura por defecto.
     *
     * @throws NotActiveException si no se esta escribiendo un objeto
     */
    public ObjectOutputStream.PutField putFields() throws IOException {
        if (this.putActual == null) {
            if (this.objetoActual == null || this.descActual == null) {
                throw new NotActiveException("not in call to writeObject");
            }
            this.putActual = new PutFieldImpl(this.descActual);
        }
        return this.putActual;
    }

    /**
     * Escribe lo que se dejo en {@link #putFields}, con el mismo formato que la escritura por
     * defecto.
     *
     * @throws NotActiveException si no se llamo a {@link #putFields} antes
     */
    public void writeFields() throws IOException {
        if (this.putActual == null) {
            throw new NotActiveException("no current PutField object");
        }
        boolean bloqueAntes = this.bout.modoBloque(false);
        try {
            this.putActual.volcar(this);
        } finally {
            this.bout.modoBloque(bloqueAntes);
        }
    }

    /**
     * Escribe los campos por defecto del objeto que se esta serializando.
     *
     * <p>Existe para que un {@code writeObject} propio pueda hacer "lo de siempre y ademas esto":
     * llamarlo primero y despues escribir lo suyo es el patron normal de una clase que agrega datos
     * sin cambiar la forma de los campos.
     *
     * @throws NotActiveException si no se esta escribiendo un objeto
     */
    public void defaultWriteObject() throws IOException {
        if (this.objetoActual == null || this.descActual == null) {
            throw new NotActiveException("not in call to writeObject");
        }
        boolean bloqueAntes = this.bout.modoBloque(false);
        try {
            this.escribirCamposPorDefecto(this.objetoActual, this.descActual.forClass(),
                    this.descActual);
        } finally {
            this.bout.modoBloque(bloqueAntes);
        }
    }

    /** El buffer por nombre de {@link ObjectOutputStream#putFields}. */
    public abstract static class PutField {

        public abstract void put(String name, boolean val);

        public abstract void put(String name, byte val);

        public abstract void put(String name, char val);

        public abstract void put(String name, short val);

        public abstract void put(String name, int val);

        public abstract void put(String name, long val);

        public abstract void put(String name, float val);

        public abstract void put(String name, double val);

        public abstract void put(String name, Object val);

        /**
         * @deprecated Escribia los campos directo al flujo salteando el formato del descriptor, asi
         *     que lo que producia no se podia volver a leer con {@code readFields}. Usar
         *     {@link ObjectOutputStream#writeFields}.
         */
        @Deprecated
        public abstract void write(ObjectOutput out) throws IOException;
    }

    private static final class PutFieldImpl extends ObjectOutputStream.PutField {
        private final ObjectStreamField[] campos;
        private final byte[] primitivos;
        private final Object[] referencias;

        PutFieldImpl(ObjectStreamClass desc) {
            this.campos = desc.getFields();
            int bytes = 0;
            int refs = 0;
            int i = 0;
            while (i < this.campos.length) {
                if (this.campos[i].isPrimitive()) {
                    bytes = bytes + ancho(this.campos[i].getTypeCode());
                } else {
                    refs = refs + 1;
                }
                i = i + 1;
            }
            this.primitivos = new byte[bytes];
            this.referencias = new Object[refs];
        }

        private ObjectStreamField buscar(String nombre, boolean primitivo) {
            int i = 0;
            while (i < this.campos.length) {
                if (this.campos[i].getName().equals(nombre)
                        && this.campos[i].isPrimitive() == primitivo) {
                    return this.campos[i];
                }
                i = i + 1;
            }
            throw new IllegalArgumentException("no such field " + nombre);
        }

        public void put(String name, boolean val) {
            this.primitivos[this.buscar(name, true).getOffset()] = (byte) (val ? 1 : 0);
        }

        public void put(String name, byte val) {
            this.primitivos[this.buscar(name, true).getOffset()] = val;
        }

        public void put(String name, char val) {
            this.ponerCorto(this.buscar(name, true).getOffset(), val);
        }

        public void put(String name, short val) {
            this.ponerCorto(this.buscar(name, true).getOffset(), val);
        }

        public void put(String name, int val) {
            this.ponerEntero(this.buscar(name, true).getOffset(), val);
        }

        public void put(String name, long val) {
            this.ponerLargo(this.buscar(name, true).getOffset(), val);
        }

        public void put(String name, float val) {
            this.ponerEntero(this.buscar(name, true).getOffset(), Float.floatToIntBits(val));
        }

        public void put(String name, double val) {
            this.ponerLargo(this.buscar(name, true).getOffset(), Double.doubleToLongBits(val));
        }

        public void put(String name, Object val) {
            this.referencias[this.buscar(name, false).getOffset()] = val;
        }

        public void write(ObjectOutput out) throws IOException {
            throw new UnsupportedOperationException(
                    "PutField.write no produce un formato que readFields pueda leer; usar writeFields");
        }

        private void ponerCorto(int off, int v) {
            this.primitivos[off] = (byte) (v >>> 8);
            this.primitivos[off + 1] = (byte) v;
        }

        private void ponerEntero(int off, int v) {
            this.primitivos[off] = (byte) (v >>> 24);
            this.primitivos[off + 1] = (byte) (v >>> 16);
            this.primitivos[off + 2] = (byte) (v >>> 8);
            this.primitivos[off + 3] = (byte) v;
        }

        private void ponerLargo(int off, long v) {
            this.ponerEntero(off, (int) (v >>> 32));
            this.ponerEntero(off + 4, (int) v);
        }

        void volcar(ObjectOutputStream oos) throws IOException {
            oos.bout.escribirCrudo(this.primitivos, 0, this.primitivos.length);
            int i = 0;
            while (i < this.campos.length) {
                if (!this.campos[i].isPrimitive()) {
                    oos.escribirObjeto0(this.referencias[this.campos[i].getOffset()],
                            this.campos[i].isUnshared());
                }
                i = i + 1;
            }
        }

        private static int ancho(char c) {
            if (c == 'Z' || c == 'B') {
                return 1;
            }
            if (c == 'C' || c == 'S') {
                return 2;
            }
            if (c == 'J' || c == 'D') {
                return 8;
            }
            return 4;
        }
    }

    // ---- DataOutput: todo esto va al buffer de bloque ---------------------------------------------

    public void write(int val) throws IOException {
        this.bout.escribirByte(val);
    }

    public void write(byte[] buf) throws IOException {
        this.bout.escribirBytes(buf, 0, buf.length);
    }

    public void write(byte[] buf, int off, int len) throws IOException {
        if (buf == null) {
            throw new NullPointerException();
        }
        this.bout.escribirBytes(buf, off, len);
    }

    public void writeBoolean(boolean val) throws IOException {
        this.bout.escribirByte(val ? 1 : 0);
    }

    public void writeByte(int val) throws IOException {
        this.bout.escribirByte(val);
    }

    public void writeShort(int val) throws IOException {
        this.bout.escribirShort(val);
    }

    public void writeChar(int val) throws IOException {
        this.bout.escribirShort(val);
    }

    public void writeInt(int val) throws IOException {
        this.bout.escribirInt(val);
    }

    public void writeLong(long val) throws IOException {
        this.bout.escribirLong(val);
    }

    public void writeFloat(float val) throws IOException {
        this.bout.escribirInt(Float.floatToIntBits(val));
    }

    public void writeDouble(double val) throws IOException {
        this.bout.escribirLong(Double.doubleToLongBits(val));
    }

    /** El byte bajo de cada caracter, y se pierde el alto. Es lo que dice {@link DataOutput}. */
    public void writeBytes(String str) throws IOException {
        int i = 0;
        while (i < str.length()) {
            this.bout.escribirByte(str.charAt(i));
            i = i + 1;
        }
    }

    /** Dos bytes por caracter, sin codificar. */
    public void writeChars(String str) throws IOException {
        int i = 0;
        while (i < str.length()) {
            this.bout.escribirShort(str.charAt(i));
            i = i + 1;
        }
    }

    /** UTF-8 modificado con el largo en dos bytes. **No** es una cadena con manija. */
    public void writeUTF(String str) throws IOException {
        this.bout.escribirUtf(str);
    }

    public void flush() throws IOException {
        this.drain();
        this.bout.vaciarAbajo();
    }

    /**
     * Cierra el flujo de abajo, despues de vaciar lo que quede en el buffer de bloque.
     *
     * <p>Vaciar antes de cerrar no es una cortesia: lo que quedo en el buffer todavia no tiene su
     * registro de bloque escrito, y un flujo cerrado sin eso termina cortado en la mitad de un
     * registro y no se puede leer.
     */
    public void close() throws IOException {
        this.flush();
        this.bout.cerrar();
    }

    // ---- la tabla de manijas ----------------------------------------------------------------------

    /**
     * Objeto -> numero de manija, **por identidad**.
     *
     * <p>Por identidad y no por {@code equals}: dos cadenas iguales pero distintas son dos objetos, y
     * fundirlas cambiaria el grafo que el otro lado reconstruye. El sondeo es lineal sobre un
     * arreglo abierto, con el hash de identidad como punto de partida.
     */
    private static final class Manijas {
        private Object[] claves = new Object[64];
        private int[] valores = new int[64];
        private int ocupadas;
        private int siguiente;

        int cuantas() {
            return this.siguiente;
        }

        int buscar(Object o) {
            int i = this.indice(o);
            while (this.claves[i] != null) {
                if (this.claves[i] == o) {
                    return this.valores[i];
                }
                i = i + 1;
                if (i == this.claves.length) {
                    i = 0;
                }
            }
            return -1;
        }

        /**
         * Numera el proximo objeto. **`null` consume un numero igual**: es como se escribe algo
         * "sin compartir", que ocupa su lugar en la numeracion pero al que nadie puede referenciar.
         */
        int asignar(Object o) {
            int m = this.siguiente;
            this.siguiente = this.siguiente + 1;
            if (o != null) {
                if ((this.ocupadas + 1) * 2 > this.claves.length) {
                    this.agrandar();
                }
                int i = this.indice(o);
                while (this.claves[i] != null) {
                    if (this.claves[i] == o) {
                        return m;
                    }
                    i = i + 1;
                    if (i == this.claves.length) {
                        i = 0;
                    }
                }
                this.claves[i] = o;
                this.valores[i] = m;
                this.ocupadas = this.ocupadas + 1;
            }
            return m;
        }

        void limpiar() {
            this.claves = new Object[64];
            this.valores = new int[64];
            this.ocupadas = 0;
            this.siguiente = 0;
        }

        private int indice(Object o) {
            int h = System.identityHashCode(o);
            return (h ^ (h >>> 16)) & (this.claves.length - 1);
        }

        private void agrandar() {
            Object[] viejasC = this.claves;
            int[] viejasV = this.valores;
            this.claves = new Object[viejasC.length * 2];
            this.valores = new int[viejasC.length * 2];
            int i = 0;
            while (i < viejasC.length) {
                if (viejasC[i] != null) {
                    int j = this.indice(viejasC[i]);
                    while (this.claves[j] != null) {
                        j = j + 1;
                        if (j == this.claves.length) {
                            j = 0;
                        }
                    }
                    this.claves[j] = viejasC[i];
                    this.valores[j] = viejasV[i];
                }
                i = i + 1;
            }
        }
    }

    // ---- el buffer de bloque ----------------------------------------------------------------------

    /**
     * El flujo de abajo con los dos modos del formato.
     *
     * <p>En **modo bloque** todo lo que se escribe se junta y sale envuelto en un registro
     * {@code TC_BLOCKDATA} (o {@code TC_BLOCKDATALONG} si pasa de 255 bytes) con el largo adelante;
     * fuera del modo bloque los bytes salen crudos. Los dos hacen falta: las etiquetas y los
     * descriptores tienen que ir crudos --el lector los interpreta byte a byte-- y lo que escribe un
     * {@code writeObject} de usuario tiene que ir enmarcado, porque es lo unico que le permite al
     * lector saltearlo sin entenderlo.
     */
    private static final class SalidaBloques {
        private static final int MAX = 1024;

        private final OutputStream out;
        private final byte[] buf = new byte[MAX];
        private int cuantos;
        private boolean bloque;

        SalidaBloques(OutputStream out) {
            this.out = out;
        }

        /** Cambia de modo y devuelve el anterior. Cambiar **vacia** lo que hubiera juntado. */
        boolean modoBloque(boolean nuevo) throws IOException {
            boolean antes = this.bloque;
            if (antes != nuevo) {
                this.vaciarBloque();
                this.bloque = nuevo;
            }
            return antes;
        }

        void vaciarBloque() throws IOException {
            if (this.cuantos == 0) {
                return;
            }
            if (this.bloque) {
                if (this.cuantos <= 0xFF) {
                    this.out.write(ObjectStreamConstants.TC_BLOCKDATA);
                    this.out.write(this.cuantos);
                } else {
                    this.out.write(ObjectStreamConstants.TC_BLOCKDATALONG);
                    this.out.write(this.cuantos >>> 24);
                    this.out.write(this.cuantos >>> 16);
                    this.out.write(this.cuantos >>> 8);
                    this.out.write(this.cuantos);
                }
            }
            this.out.write(this.buf, 0, this.cuantos);
            this.cuantos = 0;
        }

        private void unByte(int b) throws IOException {
            if (this.cuantos == MAX) {
                this.vaciarBloque();
            }
            this.buf[this.cuantos] = (byte) b;
            this.cuantos = this.cuantos + 1;
        }

        void escribirByte(int b) throws IOException {
            this.unByte(b);
        }

        void escribirShort(int v) throws IOException {
            this.unByte(v >>> 8);
            this.unByte(v);
        }

        void escribirInt(int v) throws IOException {
            this.unByte(v >>> 24);
            this.unByte(v >>> 16);
            this.unByte(v >>> 8);
            this.unByte(v);
        }

        void escribirLong(long v) throws IOException {
            this.escribirInt((int) (v >>> 32));
            this.escribirInt((int) v);
        }

        void escribirBytes(byte[] b, int off, int len) throws IOException {
            if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            }
            int i = 0;
            while (i < len) {
                this.unByte(b[off + i]);
                i = i + 1;
            }
        }

        /** Sin pasar por el marco de bloque: para los datos crudos de un arreglo o de los campos. */
        void escribirCrudo(byte[] b, int off, int len) throws IOException {
            this.escribirBytes(b, off, len);
        }

        void escribirUtf(String s) throws IOException {
            long largo = largoUtf(s);
            if (largo > 0xFFFFL) {
                throw new UTFDataFormatException("encoded string too long: " + largo + " bytes");
            }
            this.escribirUtfCorta(s, (int) largo);
        }

        void escribirUtfCorta(String s, int largo) throws IOException {
            this.escribirShort(largo);
            this.escribirUtfCruda(s);
        }

        /**
         * UTF-8 **modificado**: el cero sale en dos bytes y no en uno, y cada `char` se codifica por
         * su cuenta --un par suplente son dos secuencias de tres bytes, no una de cuatro--. Las dos
         * diferencias con UTF-8 de verdad existen para que la cadena codificada no contenga nunca un
         * byte cero, que es lo que dejaba pasarla por APIs de C.
         */
        void escribirUtfCruda(String s) throws IOException {
            int i = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c >= 0x0001 && c <= 0x007F) {
                    this.unByte(c);
                } else if (c <= 0x07FF) {
                    this.unByte(0xC0 | ((c >> 6) & 0x1F));
                    this.unByte(0x80 | (c & 0x3F));
                } else {
                    this.unByte(0xE0 | ((c >> 12) & 0x0F));
                    this.unByte(0x80 | ((c >> 6) & 0x3F));
                    this.unByte(0x80 | (c & 0x3F));
                }
                i = i + 1;
            }
        }

        static long largoUtf(String s) {
            long n = 0;
            int i = 0;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c >= 0x0001 && c <= 0x007F) {
                    n = n + 1;
                } else if (c <= 0x07FF) {
                    n = n + 2;
                } else {
                    n = n + 3;
                }
                i = i + 1;
            }
            return n;
        }

        void vaciarAbajo() throws IOException {
            this.out.flush();
        }

        void cerrar() throws IOException {
            this.vaciarBloque();
            this.out.close();
        }
    }

    // ---- reflexion compartida ---------------------------------------------------------------------

    /**
     * Las clases que aportan datos, de la superclase serializable **mas alta** a la clase concreta.
     *
     * <p>Se corta donde deja de haber {@link Serializable}: lo que este mas arriba de eso no tiene
     * forma serializada y sus campos no salen, que es lo que hace que del otro lado esa parte se
     * arme con el constructor y no con el flujo.
     */
    static Class<?>[] jerarquiaSerializable(Class<?> cl) {
        int cuantas = 0;
        Class<?> c = cl;
        while (c != null && Serializable.class.isAssignableFrom(c)) {
            cuantas = cuantas + 1;
            c = c.getSuperclass();
        }
        Class<?>[] out = new Class<?>[cuantas];
        c = cl;
        int i = cuantas - 1;
        while (i >= 0) {
            out[i] = c;
            c = c.getSuperclass();
            i = i - 1;
        }
        return out;
    }

    /**
     * El {@code private void writeObject(ObjectOutputStream)} declarado por `cl`, o `null`.
     *
     * <p>Tiene que ser **privado y declarado por esa clase exacta**: no es un override sino un gancho
     * por posicion en la jerarquia, y uno heredado correria dos veces --una por su propia clase y
     * otra por la subclase-- escribiendo los mismos datos dos veces.
     */
    static Method metodoWriteObject(Class<?> cl) {
        return metodoSerial(cl, "writeObject", ObjectOutputStream.class, void.class);
    }

    static Method metodoSerial(Class<?> cl, String nombre, Class<?> param, Class<?> retorno) {
        Method[] ms = cl.getDeclaredMethods();
        int i = 0;
        while (i < ms.length) {
            Method m = ms[i];
            if (m.getName().equals(nombre)
                    && java.lang.reflect.Modifier.isPrivate(m.getModifiers())
                    && !java.lang.reflect.Modifier.isStatic(m.getModifiers())
                    && m.getReturnType() == retorno) {
                Class<?>[] ps = m.getParameterTypes();
                if (ps.length == 1 && ps[0] == param) {
                    m.setAccessible(true);
                    return m;
                }
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * El campo real detras de un {@link ObjectStreamField}, o `null` si la clase no lo tiene.
     *
     * <p>El `null` pasa de verdad y no es un error: una clase que declara
     * {@code serialPersistentFields} puede nombrar un campo que ya no existe en el codigo --es
     * justamente para eso-- y entonces al flujo va el valor por defecto. El tipo tiene que dar
     * ademas del nombre, porque un campo del mismo nombre pero otro tipo es otro campo.
     */
    static Field campoReal(Class<?> cl, String nombre, Class<?> tipo) {
        Field f;
        try {
            f = cl.getDeclaredField(nombre);
        } catch (NoSuchFieldException ex) {
            return null;
        }
        if (f.getType() != tipo) {
            return null;
        }
        f.setAccessible(true);
        return f;
    }
}
