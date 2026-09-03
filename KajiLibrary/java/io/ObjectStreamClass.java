package java.io;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * KajiLibrary's java.io.ObjectStreamClass -- la forma serializable de una clase: que campos salen al
 * flujo, en que orden y en que lugar.
 *
 * <p>Es una consulta reflexiva y por eso se puede contestar de verdad: "¿que se guardaria de esta
 * clase?" no necesita guardar nada. Sirve tal cual esta para inspeccionar una jerarquia antes de
 * decidir un {@code transient}, o para comparar dos versiones de una clase campo por campo.
 *
 * <h2>Como se eligen los campos</h2>
 *
 * <p>Si la clase declara {@code private static final ObjectStreamField[] serialPersistentFields},
 * esa lista **manda** y los campos reales no se miran: es la forma de fijar la representacion
 * serializada de una clase para que sobreviva a que se le renombren los campos por dentro. Si no la
 * declara, salen los campos **declarados** por la clase --no los heredados, que le tocan a su propio
 * descriptor-- salteando los {@code static} y los {@code transient}.
 *
 * <p>El orden no es el de declaracion sino el del flujo: primitivos primero, y por nombre dentro de
 * cada grupo. El porque esta en {@link ObjectStreamField}.
 *
 * <h2>{@code getSerialVersionUID()} y el dato que la reflexion no da</h2>
 *
 * <p>Cuando la clase declara su {@code serialVersionUID} el valor esta ahi y no hay nada que
 * calcular. Cuando no lo declara, la especificacion manda calcularlo: un SHA-1 sobre una forma
 * canonica que incluye el nombre, los modificadores, las interfaces, los campos, los constructores,
 * los metodos, y --el punto delicado-- **si la clase tiene inicializador estatico**.
 *
 * <p>Ese ultimo dato no se puede averiguar por reflexion: {@code getDeclaredMethods()} filtra
 * {@code <clinit>} a proposito, en esta VM y en el JDK. Por eso el JDK tampoco lo resuelve por
 * reflexion sino con un **nativo**, y aca se hace igual: {@link #hasStaticInitializer} lee el
 * archivo de clase. Sin ese nativo el numero saldria bien para las clases sin bloque estatico y
 * mal para las demas --que son la mayoria, porque cualquier campo estatico con inicializador no
 * constante genera uno--, y un {@code serialVersionUID} equivocado es la peor forma de estar
 * equivocado que tiene esta API: su unico proposito es que dos JVM se pongan de acuerdo en si dos
 * clases son la misma version, y el que llama recibe un {@code long} perfectamente creible sin
 * manera de notar que no coincide con el del JDK.
 */
public final class ObjectStreamClass implements Serializable {

    /** El arreglo vacio, para una clase que no aporta campos. Compartido: no tiene estado. */
    public static final ObjectStreamField[] NO_FIELDS = new ObjectStreamField[0];

    // No es `final` por el lado que lee: un descriptor que viene del flujo nace **sin** clase local
    // --el nombre es lo unico que trajo-- y la consigue recien cuando `resolveClass` la encuentra.
    // Un descriptor armado por reflexion la fija en el constructor y no la cambia nunca mas.
    private Class<?> clase;
    private final String nombre;
    private final ObjectStreamField[] campos;
    // Perezoso y cacheado: calcular el UID es un SHA-1 sobre toda la superficie reflexiva de la
    // clase, y la mayoria de los descriptores no lo piden nunca. `Long` y no `long` porque 0 es un
    // UID valido y haria falta un centinela aparte.
    private Long uid;

    // ---- solo para un descriptor leido de un flujo ----
    // Las banderas `SC_*` tal como vinieron. Importan al leer y no al describir: dicen si el que
    // escribio uso un `writeObject` propio, y por lo tanto si los datos de este tramo estan
    // enmarcados en registros de bloque o van crudos. Adivinarlo mirando la clase local seria el
    // error clasico: la clase de este lado puede tener --o no tener-- el metodo que la del otro
    // tenia, y el marco del flujo lo decidio el que escribio.
    private int banderasFlujo;
    // El descriptor de la superclase, tambien del flujo. La cadena termina en `null`, y ese `null`
    // es el que dice donde se acaba la parte de la jerarquia que aporto datos.
    private ObjectStreamClass superiorFlujo;
    private boolean deFlujo;

    private ObjectStreamClass(Class<?> clase, ObjectStreamField[] campos) {
        this.clase = clase;
        this.nombre = clase.getName();
        // Un enum no tiene forma serializada propia: al flujo va **el nombre de la constante** y
        // nada mas, porque la constante ya existe del otro lado y lo unico que hay que decir es
        // cual es. De ahi las dos excepciones que hace la especificacion y que hay que respetar al
        // byte, porque el JDK las mira al leer y rechaza el flujo si no estan:
        //
        //   - `serialVersionUID` es **cero**, y no la huella calculada. No es que no importe: un
        //     enum no puede cambiar de forma serializada, asi que versionarla no significa nada, y
        //     el lector del JDK trata un valor distinto de cero como flujo invalido.
        //   - **sin campos**, aunque `name` y `ordinal` esten ahi. Escribirlos ademas del nombre
        //     seria decir dos veces la misma cosa, y `ordinal` encima ata el flujo al orden en que
        //     estan declaradas las constantes hoy.
        boolean esEnum = Enum.class.isAssignableFrom(clase);
        this.campos = esEnum ? NO_FIELDS : campos;
        if (esEnum) {
            this.uid = Long.valueOf(0L);
        }
    }

    /**
     * El descriptor tal como venia en el flujo, todavia sin clase local.
     *
     * <p>El UID se fija de entrada con el del flujo en vez de calcularse: para un descriptor leido,
     * el numero **es** el que vino, y calcular el de la clase local daria el de la otra version --
     * justo la que se quiere comparar contra esta, no la que se quiere reportar.
     */
    ObjectStreamClass(String nombre, long uid, int banderas, ObjectStreamField[] campos) {
        this.clase = null;
        this.nombre = nombre;
        this.campos = campos;
        this.uid = Long.valueOf(uid);
        this.banderasFlujo = banderas;
        this.deFlujo = true;
        asignarOffsets(campos);
    }

    /** La clase local que le toco a este descriptor leido, o `null` si de este lado no hay ninguna. */
    void resolvioA(Class<?> cl) {
        this.clase = cl;
    }

    void superiorFlujo(ObjectStreamClass sup) {
        this.superiorFlujo = sup;
    }

    ObjectStreamClass superiorFlujo() {
        return this.superiorFlujo;
    }

    int banderasFlujo() {
        return this.banderasFlujo;
    }

    boolean deFlujo() {
        return this.deFlujo;
    }

    /**
     * El descriptor de `cl`, o `null` si `cl` no es serializable.
     *
     * <p>El `null` es la respuesta, no un error: preguntar por una clase que no se serializa es
     * legitimo --es como se averigua que no se serializa-- y devolver un descriptor vacio haria que
     * "no participa" se confundiera con "participa sin campos", que es lo que le pasa a una
     * {@link Externalizable}.
     *
     * @throws NullPointerException si `cl` es `null`
     */
    public static ObjectStreamClass lookup(Class<?> cl) {
        if (cl == null) {
            throw new NullPointerException();
        }
        if (!Serializable.class.isAssignableFrom(cl)) {
            return null;
        }
        return new ObjectStreamClass(cl, camposDe(cl));
    }

    /**
     * El descriptor de `cl`, sea serializable o no.
     *
     * <p>Existe para poder describir una clase que aparece en un flujo aunque de este lado no
     * implemente {@link Serializable}: sin esto no habria con que nombrarla al informar el
     * desajuste.
     *
     * @throws NullPointerException si `cl` es `null`
     */
    public static ObjectStreamClass lookupAny(Class<?> cl) {
        if (cl == null) {
            throw new NullPointerException();
        }
        if (!Serializable.class.isAssignableFrom(cl)) {
            // Sin campos, y no los suyos: una clase que no se serializa no tiene forma serializada,
            // y listar sus campos sugeriria que alguno saldria al flujo.
            return new ObjectStreamClass(cl, NO_FIELDS);
        }
        return new ObjectStreamClass(cl, camposDe(cl));
    }

    public String getName() {
        return this.nombre;
    }

    public Class<?> forClass() {
        return this.clase;
    }

    /**
     * Los campos que saldrian al flujo, ya ordenados y con sus desplazamientos.
     *
     * <p>Devuelve **una copia** en cada llamada. No es prolijidad: `setOffset` es `protected` pero
     * accesible desde una subclase de {@link ObjectStreamField}, y un descriptor cuyos offsets
     * pudieran moverse desde afuera describiria un formato distinto del que el flujo usa.
     */
    public ObjectStreamField[] getFields() {
        ObjectStreamField[] copia = new ObjectStreamField[this.campos.length];
        System.arraycopy(this.campos, 0, copia, 0, this.campos.length);
        return copia;
    }

    /** El campo llamado `name`, o `null` si no hay ninguno. */
    public ObjectStreamField getField(String name) {
        int i = 0;
        while (i < this.campos.length) {
            if (this.campos[i].getName().equals(name)) {
                return this.campos[i];
            }
            i = i + 1;
        }
        return null;
    }

    /**
     * El numero de version de la forma serializada de esta clase.
     *
     * <p>Si la clase declara {@code static final long serialVersionUID}, ese valor **manda** y no se
     * calcula nada: declararlo es justamente la forma de decir "mi formato no cambio aunque el
     * codigo si". Si no lo declara, sale del SHA-1 de la forma canonica que describe
     * {@link #huellaDe}.
     *
     * <p>Cero para lo que no tiene forma serializada --un arreglo, una primitiva-- que es lo que el
     * JDK devuelve.
     */
    public long getSerialVersionUID() {
        if (this.uid == null) {
            this.uid = Long.valueOf(calcularUid(this.clase));
        }
        return this.uid.longValue();
    }

    /** El formato del JDK: el nombre y despues la linea de la declaracion del SUID. */
    public String toString() {
        return this.nombre + ": static final long serialVersionUID = "
                + this.getSerialVersionUID() + "L;";
    }

    // ---- serialVersionUID ------------------------------------------------------------------------

    // Si la clase tiene `<clinit>`. Nativo por la misma razon que en el JDK: `getDeclaredMethods`
    // filtra `<clinit>`, asi que la reflexion no puede contestarlo y el dato entra en la huella.
    private static native boolean hasStaticInitializer(Class<?> cl);

    /**
     * Una instancia de `cl` con todos sus campos en el valor por defecto y **sin correr ningun
     * constructor**, o `null` si `cl` no se puede instanciar.
     *
     * <p>Es la unica pieza de la deserializacion que no se puede escribir en Java, y por eso es
     * nativa. Reconstruir no es construir: los campos vienen del flujo, y correr el constructor
     * ejecutaria sus efectos --validaciones, contadores, altas en tablas globales-- por un objeto
     * que no se esta creando. Vive aca, package-private, porque su unico llamador legitimo es
     * {@link ObjectInputStream}: expuesta seria una forma de saltearse todo constructor del
     * sistema.
     */
    static native Object allocateInstance(Class<?> cl);

    private static long calcularUid(Class<?> cl) {
        long declarado = declaredUid(cl);
        if (declarado != NO_DECLARADO) {
            return declarado;
        }
        if (!Serializable.class.isAssignableFrom(cl)) {
            // Lo que no se serializa no tiene version de formato. Cero, y no una huella calculada:
            // un numero ahi sugeriria que hay un formato con el que compararse.
            return 0L;
        }
        try {
            byte[] h = sha1(huellaDe(cl));
            // Los ocho primeros bytes del SHA-1, leidos **al reves**: el byte 0 termina en la parte
            // baja del long. No es una eleccion, es lo que hace el JDK, y el numero tiene que dar
            // igual byte por byte o los dos lados no se entienden.
            long uid = 0L;
            int i = 7;
            while (i >= 0) {
                uid = (uid << 8) | ((long) (h[i] & 0xFF));
                i = i - 1;
            }
            return uid;
        } catch (IOException imposible) {
            // La huella se arma sobre un `ByteArrayOutputStream`, que no tiene con que fallar.
            throw new InternalError(imposible);
        }
    }

    // Un `long` no tiene valor "ausente", asi que hace falta un centinela para "la clase no lo
    // declara" -- y no puede ser 0, que es un SUID declarado perfectamente valido.
    private static final long NO_DECLARADO = 0x8000_0000_0000_0001L;

    private static long declaredUid(Class<?> cl) {
        Field f;
        try {
            f = cl.getDeclaredField("serialVersionUID");
        } catch (NoSuchFieldException ex) {
            return NO_DECLARADO;
        }
        int m = f.getModifiers();
        if (!Modifier.isStatic(m) || !Modifier.isFinal(m) || f.getType() != long.class) {
            return NO_DECLARADO;
        }
        f.setAccessible(true);
        return f.getLong(null);
    }

    /**
     * La forma canonica de la que sale el SHA-1, byte por byte como la define la especificacion de
     * serializacion.
     *
     * <p>El orden es fijo y los ordenamientos tambien --interfaces y campos por nombre,
     * constructores por firma, metodos por nombre y despues por firma-- porque el numero tiene que
     * salir igual en dos JVM que vieron la misma clase, y ni la reflexion ni el archivo de clase
     * garantizan un orden de declaracion estable.
     *
     * <p>Los `private` quedan afuera de metodos y constructores, y de los campos solo los
     * `private static` y `private transient`: lo privado que no sale al flujo no es parte del
     * contrato con la otra JVM, asi que renombrarlo no tiene por que cambiar la version.
     */
    private static byte[] huellaDe(Class<?> cl) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(bytes);
        d.writeUTF(cl.getName());

        int mods = cl.getModifiers()
                & (Modifier.PUBLIC | Modifier.FINAL | Modifier.INTERFACE | Modifier.ABSTRACT);
        java.lang.reflect.Method[] metodos = cl.getDeclaredMethods();
        if ((mods & Modifier.INTERFACE) != 0) {
            // Una interfaz sin metodos no lleva ABSTRACT y con metodos si. Parece arbitrario y lo
            // es: viene de como los compiladores viejos marcaban las interfaces, y se conserva
            // porque cambiarlo moveria el UID de todas las interfaces del mundo.
            mods = metodos.length > 0 ? (mods | Modifier.ABSTRACT) : (mods & ~Modifier.ABSTRACT);
        }
        d.writeInt(mods);

        if (cl.isArray()) {
            // **Un arreglo termina aca**: nombre y modificadores, y nada mas. No es una
            // simplificacion nuestra sino una compensacion historica del JDK -- hasta 1.2,
            // `getInterfaces()` de un arreglo devolvia vacio, y cuando empezo a devolver
            // `Cloneable` y `Serializable` habria movido el UID de todos los arreglos del mundo.
            // Se congelo la forma vieja. Sin esto, `int[]` da un numero distinto del real y ningun
            // flujo con arreglos se lee del otro lado.
            return bytes.toByteArray();
        }

        Class<?>[] ifaces = cl.getInterfaces();
        String[] nombresIfaces = new String[ifaces.length];
        int i = 0;
        while (i < ifaces.length) {
            nombresIfaces[i] = ifaces[i].getName();
            i = i + 1;
        }
        ordenarCadenas(nombresIfaces);
        i = 0;
        while (i < nombresIfaces.length) {
            d.writeUTF(nombresIfaces[i]);
            i = i + 1;
        }

        Field[] campos = cl.getDeclaredFields();
        String[] clavesCampos = new String[campos.length];
        i = 0;
        while (i < campos.length) {
            clavesCampos[i] = campos[i].getName();
            i = i + 1;
        }
        ordenarPorClave(clavesCampos, campos);
        i = 0;
        while (i < campos.length) {
            int fm = campos[i].getModifiers()
                    & (Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED | Modifier.STATIC
                            | Modifier.FINAL | Modifier.VOLATILE | Modifier.TRANSIENT);
            if ((fm & Modifier.PRIVATE) == 0
                    || (fm & (Modifier.STATIC | Modifier.TRANSIENT)) == 0) {
                d.writeUTF(campos[i].getName());
                d.writeInt(fm);
                d.writeUTF(campos[i].getType().descriptorString());
            }
            i = i + 1;
        }

        if (hasStaticInitializer(cl)) {
            d.writeUTF("<clinit>");
            d.writeInt(Modifier.STATIC);
            d.writeUTF("()V");
        }

        java.lang.reflect.Constructor<?>[] ctors = cl.getDeclaredConstructors();
        String[] clavesCtors = new String[ctors.length];
        i = 0;
        while (i < ctors.length) {
            clavesCtors[i] = descriptorDe(ctors[i].getParameterTypes(), void.class);
            i = i + 1;
        }
        ordenarPorClave(clavesCtors, ctors);
        i = 0;
        while (i < ctors.length) {
            int cm = ctors[i].getModifiers() & MODS_EJECUTABLE;
            if ((cm & Modifier.PRIVATE) == 0) {
                d.writeUTF("<init>");
                d.writeInt(cm);
                d.writeUTF(descriptorDe(ctors[i].getParameterTypes(), void.class).replace('/', '.'));
            }
            i = i + 1;
        }

        String[] clavesMetodos = new String[metodos.length];
        i = 0;
        while (i < metodos.length) {
            // Nombre y firma en la misma clave: el orden es por nombre y **despues** por firma, y
            // pegarlos con un separador que no puede aparecer en un nombre da ese orden con una
            // sola comparacion de cadenas.
            clavesMetodos[i] = metodos[i].getName() + " "
                    + descriptorDe(metodos[i].getParameterTypes(), metodos[i].getReturnType());
            i = i + 1;
        }
        ordenarPorClave(clavesMetodos, metodos);
        i = 0;
        while (i < metodos.length) {
            int mm = metodos[i].getModifiers() & MODS_EJECUTABLE;
            if ((mm & Modifier.PRIVATE) == 0) {
                d.writeUTF(metodos[i].getName());
                d.writeInt(mm);
                d.writeUTF(descriptorDe(metodos[i].getParameterTypes(),
                        metodos[i].getReturnType()).replace('/', '.'));
            }
            i = i + 1;
        }
        return bytes.toByteArray();
    }

    private static final int MODS_EJECUTABLE = Modifier.PUBLIC | Modifier.PRIVATE
            | Modifier.PROTECTED | Modifier.STATIC | Modifier.FINAL | Modifier.SYNCHRONIZED
            | Modifier.NATIVE | Modifier.ABSTRACT | Modifier.STRICT;

    private static String descriptorDe(Class<?>[] params, Class<?> retorno) {
        StringBuilder sb = new StringBuilder("(");
        int i = 0;
        while (i < params.length) {
            sb.append(params[i].descriptorString());
            i = i + 1;
        }
        sb.append(')').append(retorno.descriptorString());
        return sb.toString();
    }

    /**
     * SHA-1 de `datos`, veinte bytes.
     *
     * <p>Escrito aca adentro y no llamando a {@code java.security.MessageDigest}, que es lo que hace
     * el JDK. La razon es de dependencias y no de gusto: {@code java.io} es la base de media
     * biblioteca --{@code java.security} misma se apoya en el, sus digests son
     * {@link FilterOutputStream}-- y hacer que un {@code serialVersionUID} arrastre el registro de
     * proveedores, {@code Security}, {@code Provider.Service} y el mapa que los indexa es meterle a
     * la base un ciclo hacia una capa muy de arriba. El algoritmo son cincuenta lineas sin estado;
     * el registro de proveedores, no.
     *
     * <p>Es una funcion de resumen usada como **identificador de version**, no como defensa: que
     * SHA-1 este roto para firmar no cambia nada aca, y cambiarlo por otro algoritmo cambiaria
     * todos los UID del mundo.
     */
    private static byte[] sha1(byte[] datos) {
        int[] h = new int[] { 0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0 };

        // El relleno: un bit en 1, ceros, y el largo en **bits** como big-endian de 64. El largo va
        // adentro del hash a proposito -- sin el, "abc" y "abc" con ceros atras darian lo mismo.
        int resto = datos.length % 64;
        int ceros = (resto < 56 ? 56 : 120) - resto;
        byte[] m = new byte[datos.length + ceros + 8];
        System.arraycopy(datos, 0, m, 0, datos.length);
        m[datos.length] = (byte) 0x80;
        long bits = ((long) datos.length) * 8L;
        int i = 0;
        while (i < 8) {
            m[m.length - 1 - i] = (byte) (bits >>> (8 * i));
            i = i + 1;
        }

        int[] w = new int[80];
        int base = 0;
        while (base < m.length) {
            i = 0;
            while (i < 16) {
                int p = base + i * 4;
                w[i] = ((m[p] & 0xFF) << 24) | ((m[p + 1] & 0xFF) << 16)
                        | ((m[p + 2] & 0xFF) << 8) | (m[p + 3] & 0xFF);
                i = i + 1;
            }
            while (i < 80) {
                int x = w[i - 3] ^ w[i - 8] ^ w[i - 14] ^ w[i - 16];
                w[i] = (x << 1) | (x >>> 31);
                i = i + 1;
            }
            int a = h[0];
            int b = h[1];
            int c = h[2];
            int d = h[3];
            int e = h[4];
            i = 0;
            while (i < 80) {
                int f;
                int k;
                if (i < 20) {
                    f = (b & c) | ((~b) & d);
                    k = 0x5A827999;
                } else if (i < 40) {
                    f = b ^ c ^ d;
                    k = 0x6ED9EBA1;
                } else if (i < 60) {
                    f = (b & c) | (b & d) | (c & d);
                    k = 0x8F1BBCDC;
                } else {
                    f = b ^ c ^ d;
                    k = 0xCA62C1D6;
                }
                int t = ((a << 5) | (a >>> 27)) + f + e + k + w[i];
                e = d;
                d = c;
                c = (b << 30) | (b >>> 2);
                b = a;
                a = t;
                i = i + 1;
            }
            h[0] = h[0] + a;
            h[1] = h[1] + b;
            h[2] = h[2] + c;
            h[3] = h[3] + d;
            h[4] = h[4] + e;
            base = base + 64;
        }

        byte[] out = new byte[20];
        i = 0;
        while (i < 5) {
            out[i * 4] = (byte) (h[i] >>> 24);
            out[i * 4 + 1] = (byte) (h[i] >>> 16);
            out[i * 4 + 2] = (byte) (h[i] >>> 8);
            out[i * 4 + 3] = (byte) h[i];
            i = i + 1;
        }
        return out;
    }

    private static void ordenarCadenas(String[] a) {
        int i = 1;
        while (i < a.length) {
            String x = a[i];
            int j = i - 1;
            while (j >= 0 && a[j].compareTo(x) > 0) {
                a[j + 1] = a[j];
                j = j - 1;
            }
            a[j + 1] = x;
            i = i + 1;
        }
    }

    // Ordena `datos` por `claves`, moviendo los dos a la par. Insercion, como el resto de la clase:
    // son unas decenas de miembros y no vale traerse un `sort` generico.
    private static void ordenarPorClave(String[] claves, Object[] datos) {
        int i = 1;
        while (i < claves.length) {
            String ck = claves[i];
            Object cd = datos[i];
            int j = i - 1;
            while (j >= 0 && claves[j].compareTo(ck) > 0) {
                claves[j + 1] = claves[j];
                datos[j + 1] = datos[j];
                j = j - 1;
            }
            claves[j + 1] = ck;
            datos[j + 1] = cd;
            i = i + 1;
        }
    }

    // ---- de donde salen los campos ---------------------------------------------------------------

    private static ObjectStreamField[] camposDe(Class<?> cl) {
        if (cl.isArray() || cl.isPrimitive() || cl.isInterface()) {
            // Un arreglo se escribe con su largo y sus elementos, y una interfaz no tiene estado:
            // en los dos casos no hay campos que enumerar.
            return NO_FIELDS;
        }
        if (Externalizable.class.isAssignableFrom(cl)) {
            // Una externalizable escribe **ella** lo suyo en `writeExternal`. Listar sus campos
            // diria que el flujo los va a guardar solo, que es justo lo que no pasa.
            return NO_FIELDS;
        }
        ObjectStreamField[] declarados = serialPersistentFields(cl);
        if (declarados == null) {
            declarados = deLosCamposReales(cl);
        }
        ordenar(declarados);
        asignarOffsets(declarados);
        return declarados;
    }

    // La lista explicita, si la clase la declara con el tipo, el nombre y los modificadores exactos.
    //
    // Los tres tienen que dar: `private static final ObjectStreamField[]`. La especificacion es
    // estricta ahi porque un campo con ese nombre pero publico o no estatico es otra cosa --puede
    // ser un campo de instancia legitimo-- y tomarlo por la declaracion de formato cambiaria en
    // silencio lo que la clase escribe.
    private static ObjectStreamField[] serialPersistentFields(Class<?> cl) {
        Field f;
        try {
            f = cl.getDeclaredField("serialPersistentFields");
        } catch (NoSuchFieldException ex) {
            return null;
        }
        int m = f.getModifiers();
        if (!Modifier.isPrivate(m) || !Modifier.isStatic(m) || !Modifier.isFinal(m)) {
            return null;
        }
        f.setAccessible(true);
        Object v = f.get(null);
        if (!(v instanceof ObjectStreamField[])) {
            return null;
        }
        ObjectStreamField[] src = (ObjectStreamField[]) v;
        // Copia, por lo mismo que `getFields`: la clase se quedo con una referencia al arreglo y
        // podria tocarlo despues.
        ObjectStreamField[] out = new ObjectStreamField[src.length];
        System.arraycopy(src, 0, out, 0, src.length);
        return out;
    }

    private static ObjectStreamField[] deLosCamposReales(Class<?> cl) {
        Field[] fs = cl.getDeclaredFields();
        int cuantos = 0;
        int i = 0;
        while (i < fs.length) {
            if (participa(fs[i])) {
                cuantos = cuantos + 1;
            }
            i = i + 1;
        }
        ObjectStreamField[] out = new ObjectStreamField[cuantos];
        int k = 0;
        i = 0;
        while (i < fs.length) {
            if (participa(fs[i])) {
                out[k] = new ObjectStreamField(fs[i].getName(), fs[i].getType());
                k = k + 1;
            }
            i = i + 1;
        }
        return out;
    }

    // `static` queda afuera porque pertenece a la clase y no al objeto; `transient`, porque es la
    // unica forma que tiene el autor de decir "esto no se guarda" -- una contrase&ntilde;a en claro,
    // una conexion abierta, un cache que hay que rehacer.
    private static boolean participa(Field f) {
        int m = f.getModifiers();
        return !Modifier.isStatic(m) && !Modifier.isTransient(m);
    }

    // Insercion, no un `sort` de biblioteca: son un pu&ntilde;ado de campos y la comparacion es la
    // de `ObjectStreamField`, que ya tiene escrito el criterio.
    private static void ordenar(ObjectStreamField[] a) {
        int i = 1;
        while (i < a.length) {
            ObjectStreamField x = a[i];
            int j = i - 1;
            while (j >= 0 && a[j].compareTo(x) > 0) {
                a[j + 1] = a[j];
                j = j - 1;
            }
            a[j + 1] = x;
            i = i + 1;
        }
    }

    // Dos contadores independientes, y de ahi que los primitivos vayan primero en el orden: el de
    // los primitivos cuenta **bytes** dentro del bloque de datos, el de las referencias cuenta
    // **posiciones** en la tabla de objetos. Son unidades distintas, y si los dos grupos se
    // intercalaran no habria un solo numero que sirviera para los dos.
    private static void asignarOffsets(ObjectStreamField[] a) {
        int bytes = 0;
        int refs = 0;
        int i = 0;
        while (i < a.length) {
            char c = a[i].getTypeCode();
            if (c == 'L' || c == '[') {
                a[i].setOffset(refs);
                refs = refs + 1;
            } else {
                a[i].setOffset(bytes);
                bytes = bytes + anchoDe(c);
            }
            i = i + 1;
        }
    }

    private static int anchoDe(char c) {
        if (c == 'Z' || c == 'B') {
            return 1;
        }
        if (c == 'C' || c == 'S') {
            return 2;
        }
        if (c == 'J' || c == 'D') {
            return 8;
        }
        return 4;                      // I y F
    }
}
