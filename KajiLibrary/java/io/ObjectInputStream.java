package java.io;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * KajiLibrary's java.io.ObjectInputStream -- el lado que lee del formato de serializacion.
 *
 * <p>Es la contraparte exacta de {@link ObjectOutputStream}: lo que aquel escribe, este lo arma de
 * vuelta. La prueba de esta clase no es "lee lo que yo escribi" --eso lo cumple cualquier par de
 * rutinas que se equivoquen igual-- sino que lee **el flujo que produce el JDK real**, byte por
 * byte, y que un flujo escrito aca lo lee el JDK.
 *
 * <h2>Reconstruir no es construir</h2>
 *
 * <p>El objeto se saca de {@link ObjectStreamClass#allocateInstance}, que da una instancia con todos
 * los campos en su valor por defecto y **sin correr ningun constructor**. No es un atajo: correr el
 * constructor ejecutaria sus efectos --validaciones, contadores, altas en tablas globales-- por un
 * objeto que no se esta creando sino leyendo, y ademas pisaria despues con los valores del flujo lo
 * que el constructor acababa de calcular. La especificacion dice exactamente eso, y suena al reves
 * de lo que uno esperaria.
 *
 * <p>La consecuencia que hay que tener presente: **la clase no puede defenderse en el
 * constructor**. Cualquier invariante que dependa de validar en el constructor no vale para un
 * objeto que llego por aca; para eso estan {@code readObject} propio,
 * {@link #registerValidation} y {@link ObjectInputFilter}.
 *
 * <h2>Las tres capas, del lado que lee</h2>
 *
 * <ol>
 *   <li><b>Los bytes de bloque.</b> Todo lo que un {@code writeObject} de usuario escribio salio
 *       envuelto en registros {@code TC_BLOCKDATA} con su largo adelante. Aca eso se desenvuelve, y
 *       --lo importante-- se puede **saltear**: cuando la clase de este lado no tiene el
 *       {@code readObject} que la del otro tenia, sus datos se descartan hasta el
 *       {@code TC_ENDBLOCKDATA} sin necesidad de entenderlos. Sin ese marco, un solo campo de mas
 *       del otro lado desalinearia el flujo para siempre.
 *   <li><b>Las manijas.</b> Cada objeto, cadena, clase y descriptor que sale del flujo se numera en
 *       el mismo orden en que el escritor lo numero, y un {@code TC_REFERENCE} devuelve **el mismo
 *       objeto** que ya se armo. Es lo que hace que un grafo con ciclos termine y lo que conserva
 *       la identidad compartida. La manija se reserva **antes** de leer los campos, justamente para
 *       que un campo que apunta al objeto que lo contiene la encuentre ya numerada.
 *   <li><b>Los descriptores.</b> La forma de la clase viene en el flujo y **manda sobre la de este
 *       lado**: los campos se leen en el orden y con los tipos que dice el flujo, y recien despues
 *       se busca a que campo local le toca cada valor. Un campo que el flujo trae y la clase local
 *       ya no tiene se descarta; uno que la clase local tiene y el flujo no trae queda en su valor
 *       por defecto. Eso es lo que permite leer un objeto escrito por otra version de la clase.
 * </ol>
 *
 * <h2>Lo que esta clase no hace, y por que</h2>
 *
 * <p><b>{@code readResolve} no se consulta.</b> Es la unica desviacion del formato, y es la misma
 * --y por la misma razon-- que {@code writeReplace} en {@link ObjectOutputStream}: decidir si el
 * metodo vale pide reproducir las reglas de accesibilidad de la especificacion (privado de la
 * clase, o accesible desde ella por herencia dentro del mismo paquete) y equivocarse en cualquiera
 * de los dos sentidos devuelve, en silencio, un objeto distinto del que el JDK devolveria. Se avisa
 * aca en vez de adivinar. Como los dos lados de esta biblioteca hacen lo mismo, siguen
 * entendiendose entre si; contra el JDK real la diferencia se nota en las clases que usan
 * {@code readResolve} para preservar un singleton.
 *
 * <p><b>{@code readObjectNoData} no se llama.</b> Solo aplica cuando el flujo **no** trae datos
 * para un tramo de la jerarquia que la clase local si tiene, que es el caso de una superclase
 * agregada despues de escribir. Los campos de ese tramo quedan en su valor por defecto, que es lo
 * que hubiera pasado igual si el metodo no estuviera declarado.
 *
 * <p><b>Una clase que el flujo nombra y de este lado no existe corta la lectura.</b> Sus bytes se
 * consumen enteros --el flujo no queda desalineado a mitad de ese objeto-- y despues sale
 * {@link ClassNotFoundException}. El JDK difiere esa excepcion hasta el final del
 * {@code readObject} de arriba de todo para poder devolver el resto del grafo con ese campo en
 * {@code null}; aca no, porque llevar la excepcion a cuestas por todo el grafo y decidir en cada
 * campo si se propaga o se deja en {@code null} es justamente la clase de detalle que se equivoca
 * en silencio. Lo que se pierde es poder seguir usando el flujo despues del error.
 */
public class ObjectInputStream extends InputStream implements ObjectInput, ObjectStreamConstants {

    /**
     * Lo que se pone en la tabla en lugar de un objeto leido con {@link #readUnshared}.
     *
     * <p>La manija se reserva igual --el escritor la conto y los numeros tienen que coincidir-- pero
     * apunta a esto y no al objeto. Asi, un {@code TC_REFERENCE} posterior a esa manija se detecta y
     * se rechaza en vez de devolver el objeto que justamente se pidio no compartir.
     */
    private static final Object SIN_COMPARTIR = new Object();

    private final EntradaBloques bin;

    /** Numerados en el mismo orden que los numero el escritor; ver la nota de la clase. */
    private Object[] manijas = new Object[16];
    private int cuantasManijas;

    /** Un flujo construido con el constructor sin argumentos: no hay bytes abajo. */
    private final boolean delegado;

    private boolean resolucionHabilitada;
    private ObjectInputFilter filtro;
    private boolean filtroFijado;

    // Estado del tramo que se esta leyendo, para `defaultReadObject` y `readFields`.
    private Object objetoActual;
    private ObjectStreamClass descActual;
    private GetFieldImpl getActual;

    /** Anidamiento de {@code readObject}; el objeto de arriba de todo esta en 1. */
    private int profundidad;

    private ObjectInputValidation[] validaciones = new ObjectInputValidation[4];
    private int[] prioridades = new int[4];
    private int cuantasValidaciones;

    /**
     * Lee la cabecera del flujo y deja todo listo para el primer {@link #readObject}.
     *
     * @throws StreamCorruptedException si los cuatro primeros bytes no son los del formato
     */
    public ObjectInputStream(InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException();
        }
        this.bin = new EntradaBloques(in);
        this.delegado = false;
        this.bin.modoBloque(false);
        this.readStreamHeader();
        this.bin.modoBloque(true);
    }

    /**
     * Para una subclase que reimplementa la deserializacion entera.
     *
     * <p>No hay flujo abajo: {@link #readObject} llama a {@link #readObjectOverride} y todo metodo
     * que leeria bytes falla. Es `protected` porque solo tiene sentido desde adentro de una
     * subclase.
     */
    protected ObjectInputStream() throws IOException, SecurityException {
        this.bin = null;
        this.delegado = true;
    }

    // ---- la cabecera y los ganchos de subclase ---------------------------------------------------

    /** Los cuatro bytes con que empieza todo flujo: el magico y la version. */
    protected void readStreamHeader() throws IOException, StreamCorruptedException {
        short magico = (short) this.bin.leerUnsignedShort();
        short version = (short) this.bin.leerUnsignedShort();
        if (magico != ObjectStreamConstants.STREAM_MAGIC
                || version != ObjectStreamConstants.STREAM_VERSION) {
            throw new StreamCorruptedException("invalid stream header: "
                    + hex4(magico & 0xFFFF) + hex4(version & 0xFFFF));
        }
    }

    /**
     * Lee un descriptor de clase del flujo. El espejo de
     * {@link ObjectOutputStream#writeClassDescriptor}: una subclase que cambio alla el formato tiene
     * que cambiarlo aca igual.
     *
     * <p>Lo que devuelve es un descriptor **del flujo**: nombre, UID, banderas y campos tal como
     * vinieron, todavia sin clase local. Quien la resuelve es {@link #resolveClass}.
     */
    protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
        String nombre = this.bin.leerUtf();
        long uid = this.bin.leerLong();
        int banderas = this.bin.leerUnsignedByte();
        int cuantos = this.bin.leerUnsignedShort();
        ObjectStreamField[] campos = cuantos == 0
                ? ObjectStreamClass.NO_FIELDS
                : new ObjectStreamField[cuantos];
        int i = 0;
        while (i < cuantos) {
            char tipo = (char) this.bin.leerUnsignedByte();
            String nom = this.bin.leerUtf();
            String firma;
            if (tipo == 'L' || tipo == '[') {
                // El nombre del tipo es **una cadena del flujo con su propia manija**, no un UTF
                // suelto: el escritor la comparte entre todos los campos de la misma firma, y
                // leerla como UTF crudo se saltearia una manija y correria todos los numeros.
                firma = this.readTypeString();
            } else {
                firma = String.valueOf(tipo);
            }
            if (firma == null) {
                throw new StreamCorruptedException("null field type string");
            }
            campos[i] = new ObjectStreamField(nom, firma);
            i = i + 1;
        }
        return new ObjectStreamClass(nombre, uid, banderas, campos);
    }

    /**
     * La clase local que le corresponde a un descriptor del flujo.
     *
     * <p>Aca por nombre y nada mas. El JDK busca con el cargador de clases del llamador mas cercano
     * en la pila --lo que le permite resolver una clase que solo ese cargador ve--; esta VM tiene un
     * solo cargador, asi que la busqueda por nombre es la misma respuesta y no una aproximacion.
     */
    protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
        String nombre = desc.getName();
        Class<?> prim = primitivaPorNombre(nombre);
        if (prim != null) {
            return prim;
        }
        return Class.forName(nombre);
    }

    /**
     * La clase proxy que implementa `interfaces`.
     *
     * @throws ClassNotFoundException si alguna de las interfaces no existe de este lado
     */
    protected Class<?> resolveProxyClass(String[] interfaces)
            throws IOException, ClassNotFoundException {
        Class<?>[] cls = new Class<?>[interfaces.length];
        int i = 0;
        while (i < interfaces.length) {
            cls[i] = Class.forName(interfaces[i]);
            i = i + 1;
        }
        return Proxy.getProxyClass(ObjectInputStream.class.getClassLoader(), cls);
    }

    /**
     * A donde va {@link #readObject} cuando el flujo se construyo con el constructor sin argumentos.
     * Devuelve `null` aca, como en el JDK: la subclase que usa ese constructor es la que tiene que
     * leer.
     */
    protected Object readObjectOverride() throws IOException, ClassNotFoundException {
        return null;
    }

    /**
     * El ultimo filtro antes de devolver: si {@link #enableResolveObject} esta activo, cada objeto
     * pasa por aca y se devuelve lo que este metodo diga. Identidad aca, como en el JDK.
     */
    protected Object resolveObject(Object obj) throws IOException {
        return obj;
    }

    /**
     * Prende o apaga el filtro de {@link #resolveObject}, y devuelve como estaba.
     *
     * <p>El JDK le pide permiso al gestor de seguridad, porque cambiar objetos al vuelo es una forma
     * de que el flujo diga una cosa y entregue otra. Aca no hay gestor, asi que la bandera se toma
     * tal cual.
     */
    protected boolean enableResolveObject(boolean enable) {
        boolean antes = this.resolucionHabilitada;
        this.resolucionHabilitada = enable;
        return antes;
    }

    // ---- leer objetos ---------------------------------------------------------------------------

    /**
     * Lee el proximo objeto y todo lo que cuelgue de el.
     *
     * <p>Es `final` como en el JDK: la subclase que quiera cambiar que se lee tiene
     * {@link #readObjectOverride} y {@link #resolveObject}, y dejar redefinir la entrada permitiria
     * leer un objeto salteando las manijas, que es como se rompe un grafo con ciclos.
     *
     * @throws ClassNotFoundException si el flujo nombra una clase que de este lado no existe
     * @throws OptionalDataException si lo que sigue son datos primitivos y no un objeto
     */
    public final Object readObject() throws IOException, ClassNotFoundException {
        if (this.delegado) {
            return this.readObjectOverride();
        }
        return this.leerDeArriba(false);
    }

    /**
     * Lee el proximo objeto **sin compartir**: no queda asociado a una manija reusable, asi que una
     * referencia posterior a el se rechaza en vez de devolverlo.
     *
     * <p>Es la contraparte de {@link ObjectOutputStream#writeUnshared}, y sirve para lo mismo: un
     * campo que tiene que ser privado del objeto que lo contiene.
     *
     * @throws InvalidObjectException si lo que hay en el flujo es una referencia a algo ya leido
     */
    public Object readUnshared() throws IOException, ClassNotFoundException {
        if (this.delegado) {
            return this.readObjectOverride();
        }
        return this.leerDeArriba(true);
    }

    /**
     * Un {@code readObject} de arriba de todo: lee y, si nadie mas estaba leyendo, corre las
     * validaciones que se hayan registrado.
     */
    private Object leerDeArriba(boolean sinCompartir) throws IOException, ClassNotFoundException {
        boolean raiz = this.profundidad == 0;
        Object obj = this.leerObjeto0(sinCompartir);
        if (raiz) {
            this.correrValidaciones();
        }
        return obj;
    }

    private Object leerObjeto0(boolean sinCompartir) throws IOException, ClassNotFoundException {
        boolean bloqueAntes = this.bin.enModoBloque();
        if (bloqueAntes) {
            // Adentro de un `readObject` de usuario, lo que queda del registro de bloque en curso
            // son datos primitivos que el que llama todavia no leyo: pedirle un objeto ahi no es un
            // error del flujo sino un desencuentro entre lo que se escribio y lo que se esta
            // leyendo, y por eso sale como `OptionalDataException` con el largo, que es la unica
            // forma de que el que llama pueda seguir.
            int quedan = this.bin.cuantosQuedan();
            if (quedan > 0) {
                throw new OptionalDataException(quedan);
            }
            this.bin.modoBloque(false);
        }
        this.profundidad = this.profundidad + 1;
        try {
            int tc = this.bin.mirarObligatorio();
            while (tc == ObjectStreamConstants.TC_RESET) {
                if (this.profundidad > 1) {
                    throw new StreamCorruptedException("unexpected reset");
                }
                this.bin.leerByteCrudo();
                this.limpiarManijas();
                tc = this.bin.mirarObligatorio();
            }
            if (tc == ObjectStreamConstants.TC_NULL) {
                this.bin.leerByteCrudo();
                return null;
            }
            if (tc == ObjectStreamConstants.TC_REFERENCE) {
                return this.filtrarSalida(this.leerReferencia(sinCompartir));
            }
            if (tc == ObjectStreamConstants.TC_CLASS) {
                return this.filtrarSalida(this.leerClase(sinCompartir));
            }
            if (tc == ObjectStreamConstants.TC_CLASSDESC
                    || tc == ObjectStreamConstants.TC_PROXYCLASSDESC) {
                return this.filtrarSalida(this.leerDesc(sinCompartir));
            }
            if (tc == ObjectStreamConstants.TC_STRING
                    || tc == ObjectStreamConstants.TC_LONGSTRING) {
                return this.filtrarSalida(this.leerCadena(sinCompartir));
            }
            if (tc == ObjectStreamConstants.TC_ARRAY) {
                this.bin.leerByteCrudo();
                return this.filtrarSalida(this.leerArreglo(sinCompartir));
            }
            if (tc == ObjectStreamConstants.TC_ENUM) {
                this.bin.leerByteCrudo();
                return this.filtrarSalida(this.leerEnum(sinCompartir));
            }
            if (tc == ObjectStreamConstants.TC_OBJECT) {
                this.bin.leerByteCrudo();
                return this.filtrarSalida(this.leerObjetoComun(sinCompartir));
            }
            if (tc == ObjectStreamConstants.TC_EXCEPTION) {
                this.bin.leerByteCrudo();
                // El escritor se cayo a mitad del grafo y dejo escrita la excepcion en lugar del
                // resto. Se lee con la tabla limpia --el JDK hace lo mismo-- porque las manijas del
                // grafo que se abortó no valen para el que la lee.
                this.limpiarManijas();
                Object causa = this.leerObjeto0(false);
                this.limpiarManijas();
                throw new WriteAbortedException("writing aborted", (Exception) causa);
            }
            if (tc == ObjectStreamConstants.TC_BLOCKDATA
                    || tc == ObjectStreamConstants.TC_BLOCKDATALONG) {
                this.bin.modoBloque(true);
                throw new OptionalDataException(this.bin.cuantosQuedan());
            }
            if (tc == ObjectStreamConstants.TC_ENDBLOCKDATA) {
                throw new OptionalDataException(true);
            }
            throw new StreamCorruptedException("invalid type code: " + hex2(tc));
        } finally {
            this.profundidad = this.profundidad - 1;
            if (bloqueAntes) {
                this.bin.modoBloque(true);
            }
        }
    }

    /** El gancho de {@link #resolveObject}, si esta activo. */
    private Object filtrarSalida(Object obj) throws IOException {
        if (this.resolucionHabilitada && obj != null) {
            return this.resolveObject(obj);
        }
        return obj;
    }

    private Object leerReferencia(boolean sinCompartir) throws IOException {
        this.bin.leerByteCrudo();
        int m = this.bin.leerInt() - ObjectStreamConstants.baseWireHandle;
        if (m < 0 || m >= this.cuantasManijas) {
            throw new StreamCorruptedException("invalid handle value: " + hex4(m));
        }
        if (sinCompartir) {
            throw new InvalidObjectException("cannot read back reference as unshared");
        }
        Object o = this.manijas[m];
        if (o == SIN_COMPARTIR) {
            throw new InvalidObjectException("cannot read back reference to unshared object");
        }
        return o;
    }

    private String leerCadena(boolean sinCompartir) throws IOException {
        int tc = this.bin.leerByteCrudo();
        String s;
        if (tc == ObjectStreamConstants.TC_LONGSTRING) {
            long largo = this.bin.leerLong();
            if (largo < 0 || largo > Integer.MAX_VALUE) {
                throw new StreamCorruptedException("long string length out of range: " + largo);
            }
            s = this.bin.leerUtfCruda(largo);
        } else {
            s = this.bin.leerUtf();
        }
        this.asignarManija(sinCompartir ? SIN_COMPARTIR : s);
        return s;
    }

    /**
     * El tipo de un campo de referencia dentro de un descriptor.
     *
     * <p>Package-private y no publico, igual que en el JDK: es una cadena del flujo con manija
     * propia, y leerla desde afuera correria la numeracion.
     */
    String readTypeString() throws IOException {
        int tc = this.bin.mirarObligatorio();
        if (tc == ObjectStreamConstants.TC_NULL) {
            this.bin.leerByteCrudo();
            return null;
        }
        if (tc == ObjectStreamConstants.TC_REFERENCE) {
            return (String) this.leerReferencia(false);
        }
        if (tc == ObjectStreamConstants.TC_STRING || tc == ObjectStreamConstants.TC_LONGSTRING) {
            return this.leerCadena(false);
        }
        throw new StreamCorruptedException("invalid type code: " + hex2(tc));
    }

    private Class<?> leerClase(boolean sinCompartir) throws IOException, ClassNotFoundException {
        this.bin.leerByteCrudo();
        ObjectStreamClass desc = this.leerDesc(false);
        Class<?> cl = desc == null ? null : desc.forClass();
        this.asignarManija(sinCompartir ? SIN_COMPARTIR : cl);
        if (cl == null) {
            throw new ClassNotFoundException(desc == null ? "null class descriptor" : desc.getName());
        }
        return cl;
    }

    // ---- descriptores ---------------------------------------------------------------------------

    private ObjectStreamClass leerDesc(boolean sinCompartir) throws IOException, ClassNotFoundException {
        int tc = this.bin.mirarObligatorio();
        if (tc == ObjectStreamConstants.TC_NULL) {
            this.bin.leerByteCrudo();
            return null;
        }
        if (tc == ObjectStreamConstants.TC_REFERENCE) {
            Object o = this.leerReferencia(sinCompartir);
            if (!(o instanceof ObjectStreamClass)) {
                throw new StreamCorruptedException("handle does not refer to a class descriptor");
            }
            return (ObjectStreamClass) o;
        }
        if (tc == ObjectStreamConstants.TC_PROXYCLASSDESC) {
            return this.leerDescProxy(sinCompartir);
        }
        if (tc == ObjectStreamConstants.TC_CLASSDESC) {
            return this.leerDescNoProxy(sinCompartir);
        }
        throw new StreamCorruptedException("invalid type code: " + hex2(tc));
    }

    private ObjectStreamClass leerDescNoProxy(boolean sinCompartir)
            throws IOException, ClassNotFoundException {
        this.bin.leerByteCrudo();
        // La manija se reserva **antes** de leer el cuerpo, y con eso queda en el mismo numero que
        // el escritor le dio: el escribe el nombre y el UID antes de numerar, y en esos dos no hay
        // nada que consuma manijas. Un campo cuyo tipo es la clase misma la referencia por este
        // numero, asi que reservarla despues rompería la primera clase recursiva que apareciera.
        int m = this.asignarManija(null);
        ObjectStreamClass desc = this.readClassDescriptor();
        if (!sinCompartir) {
            this.manijas[m] = desc;
        } else {
            this.manijas[m] = SIN_COMPARTIR;
        }

        // El bloque de anotacion se lee en modo bloque, para que un `resolveClass` redefinido pueda
        // leer lo que su `annotateClass` escribio.
        this.bin.modoBloque(true);
        Class<?> cl = null;
        try {
            cl = this.resolveClass(desc);
        } catch (ClassNotFoundException noEsta) {
            cl = null;
        }
        this.saltearDatosPropios();
        desc.resolvioA(cl);
        desc.superiorFlujo(this.leerDesc(false));
        return desc;
    }

    private ObjectStreamClass leerDescProxy(boolean sinCompartir)
            throws IOException, ClassNotFoundException {
        this.bin.leerByteCrudo();
        int m = this.asignarManija(null);
        int cuantas = this.bin.leerInt();
        if (cuantas < 0) {
            throw new StreamCorruptedException("invalid interface count: " + cuantas);
        }
        String[] nombres = new String[cuantas];
        int i = 0;
        while (i < cuantas) {
            nombres[i] = this.bin.leerUtf();
            i = i + 1;
        }
        this.bin.modoBloque(true);
        Class<?> cl = null;
        try {
            cl = this.resolveProxyClass(nombres);
        } catch (ClassNotFoundException noEsta) {
            cl = null;
        }
        this.saltearDatosPropios();
        // Un proxy no aporta campos propios y no lleva UID: su forma serializada es la de su
        // manejador, que viaja como campo de la superclase `java.lang.reflect.Proxy`.
        ObjectStreamClass desc = new ObjectStreamClass(
                cl == null ? "" : cl.getName(), 0L,
                ObjectStreamConstants.SC_SERIALIZABLE, ObjectStreamClass.NO_FIELDS);
        desc.resolvioA(cl);
        this.manijas[m] = sinCompartir ? SIN_COMPARTIR : desc;
        desc.superiorFlujo(this.leerDesc(false));
        return desc;
    }

    /**
     * Consume lo que un {@code annotateClass} o un {@code writeObject} de usuario haya escrito de
     * mas, hasta el {@code TC_ENDBLOCKDATA} que lo cierra.
     *
     * <p>Es lo unico que hace que una version vieja del codigo pueda leer un flujo escrito por una
     * nueva: los datos que no se entienden se tiran sin tener que interpretarlos. Los objetos que
     * aparezcan sueltos ahi adentro **si** hay que leerlos, no saltearlos: cada uno consume manijas,
     * y descartarlos por largo correria la numeracion de todo lo que viene despues.
     */
    private void saltearDatosPropios() throws IOException, ClassNotFoundException {
        for (;;) {
            if (this.bin.enModoBloque()) {
                this.bin.saltarBloque();
                this.bin.modoBloque(false);
            }
            int tc = this.bin.mirarObligatorio();
            if (tc == ObjectStreamConstants.TC_BLOCKDATA
                    || tc == ObjectStreamConstants.TC_BLOCKDATALONG) {
                this.bin.modoBloque(true);
            } else if (tc == ObjectStreamConstants.TC_ENDBLOCKDATA) {
                this.bin.leerByteCrudo();
                return;
            } else {
                this.leerObjeto0(false);
            }
        }
    }

    // ---- objetos, arreglos y enums ---------------------------------------------------------------

    private Object leerArreglo(boolean sinCompartir) throws IOException, ClassNotFoundException {
        ObjectStreamClass desc = this.leerDesc(false);
        int largo = this.bin.leerInt();
        if (largo < 0) {
            throw new StreamCorruptedException("invalid array length: " + largo);
        }
        Class<?> cl = desc == null ? null : desc.forClass();
        Class<?> comp = cl == null ? null : cl.getComponentType();
        this.consultarFiltro(cl, largo);

        Object arr = comp == null ? null : Array.newInstance(comp, largo);
        int m = this.asignarManija(sinCompartir ? SIN_COMPARTIR : arr);
        if (comp != null && comp.isPrimitive()) {
            this.leerPrimitivos(arr, comp, largo);
        } else {
            // `(Object[])` y no `Array.set`: todo arreglo de referencias **es** un `Object[]`, y el
            // almacenamiento comprueba el tipo del elemento igual (`ArrayStoreException`). Es la
            // misma via que usa el escritor, y no depende de un nativo de reflexion.
            Object[] a = (Object[]) arr;
            int i = 0;
            while (i < largo) {
                Object v = this.leerObjeto0(false);
                if (a != null) {
                    a[i] = v;
                }
                i = i + 1;
            }
        }
        if (!sinCompartir) {
            this.manijas[m] = arr;
        }
        if (arr == null) {
            throw new ClassNotFoundException(desc == null ? "null array descriptor" : desc.getName());
        }
        return arr;
    }

    private void leerPrimitivos(Object arr, Class<?> comp, int largo) throws IOException {
        int i = 0;
        if (comp == Byte.TYPE) {
            byte[] a = (byte[]) arr;
            while (i < largo) {
                a[i] = (byte) this.bin.leerUnsignedByte();
                i = i + 1;
            }
        } else if (comp == Boolean.TYPE) {
            boolean[] a = (boolean[]) arr;
            while (i < largo) {
                a[i] = this.bin.leerUnsignedByte() != 0;
                i = i + 1;
            }
        } else if (comp == Character.TYPE) {
            char[] a = (char[]) arr;
            while (i < largo) {
                a[i] = (char) this.bin.leerUnsignedShort();
                i = i + 1;
            }
        } else if (comp == Short.TYPE) {
            short[] a = (short[]) arr;
            while (i < largo) {
                a[i] = (short) this.bin.leerUnsignedShort();
                i = i + 1;
            }
        } else if (comp == Integer.TYPE) {
            int[] a = (int[]) arr;
            while (i < largo) {
                a[i] = this.bin.leerInt();
                i = i + 1;
            }
        } else if (comp == Long.TYPE) {
            long[] a = (long[]) arr;
            while (i < largo) {
                a[i] = this.bin.leerLong();
                i = i + 1;
            }
        } else if (comp == Float.TYPE) {
            float[] a = (float[]) arr;
            while (i < largo) {
                a[i] = Float.intBitsToFloat(this.bin.leerInt());
                i = i + 1;
            }
        } else {
            double[] a = (double[]) arr;
            while (i < largo) {
                a[i] = Double.longBitsToDouble(this.bin.leerLong());
                i = i + 1;
            }
        }
    }

    private Object leerEnum(boolean sinCompartir) throws IOException, ClassNotFoundException {
        ObjectStreamClass desc = this.leerDesc(false);
        Class<?> cl = desc == null ? null : desc.forClass();
        this.consultarFiltro(cl, -1);
        int m = this.asignarManija(sinCompartir ? SIN_COMPARTIR : null);
        String nombre = this.leerCadena(false);
        Object valor = null;
        if (cl != null) {
            // Por nombre y no por ordinal: reordenar las constantes de un enum es un cambio que la
            // fuente permite y que no toca la forma serializada, y leer por posicion devolveria
            // otra constante en silencio.
            valor = constanteEnum(cl, nombre);
            if (valor == null) {
                throw new InvalidObjectException("enum constant " + nombre + " does not exist in "
                        + cl.getName());
            }
        }
        if (!sinCompartir) {
            this.manijas[m] = valor;
        }
        if (cl == null) {
            throw new ClassNotFoundException(desc == null ? "null enum descriptor" : desc.getName());
        }
        return valor;
    }

    private static Object constanteEnum(Class<?> cl, String nombre) {
        Object[] cs = cl.getEnumConstants();
        if (cs == null) {
            return null;
        }
        int i = 0;
        while (i < cs.length) {
            if (((Enum<?>) cs[i]).name().equals(nombre)) {
                return cs[i];
            }
            i = i + 1;
        }
        return null;
    }

    private Object leerObjetoComun(boolean sinCompartir) throws IOException, ClassNotFoundException {
        ObjectStreamClass desc = this.leerDesc(false);
        if (desc == null) {
            throw new StreamCorruptedException("null class descriptor for object");
        }
        Class<?> cl = desc.forClass();
        this.consultarFiltro(cl, -1);

        Object obj = null;
        if (cl != null) {
            obj = ObjectStreamClass.allocateInstance(cl);
            if (obj == null) {
                // Una interfaz, una abstracta o algo que esta VM no puede instanciar. No se puede
                // seguir: los datos se podrian consumir, pero no habria donde ponerlos y devolver
                // `null` haria pasar un flujo ilegible por uno que traia un `null`.
                throw new InvalidClassException(desc.getName(), "unable to create instance");
            }
        }
        int m = this.asignarManija(sinCompartir ? SIN_COMPARTIR : obj);
        if ((desc.banderasFlujo() & ObjectStreamConstants.SC_EXTERNALIZABLE) != 0) {
            this.leerDatosExternos(obj, desc);
        } else {
            this.leerDatosSerie(obj, desc);
        }
        if (!sinCompartir) {
            this.manijas[m] = obj;
        }
        if (cl == null) {
            throw new ClassNotFoundException(desc.getName());
        }
        return obj;
    }

    private void leerDatosExternos(Object obj, ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {
        // `SC_BLOCK_DATA` es lo que distingue al protocolo 2 del 1: con el, lo que escribio
        // `writeExternal` viene enmarcado y se puede saltear; sin el viene crudo, y si de este lado
        // no hay con que leerlo no queda mas remedio que abandonar.
        boolean enmarcado = (desc.banderasFlujo() & ObjectStreamConstants.SC_BLOCK_DATA) != 0;
        Externalizable ext = obj instanceof Externalizable ? (Externalizable) obj : null;
        if (ext == null && !enmarcado) {
            throw new StreamCorruptedException(
                    "unreadable external data for " + desc.getName() + " (protocol 1)");
        }
        Object objAntes = this.objetoActual;
        ObjectStreamClass descAntes = this.descActual;
        GetFieldImpl getAntes = this.getActual;
        this.objetoActual = obj;
        this.descActual = null;
        this.getActual = null;
        try {
            if (enmarcado) {
                this.bin.modoBloque(true);
            }
            if (ext != null) {
                ext.readExternal(this);
            }
        } finally {
            this.objetoActual = objAntes;
            this.descActual = descAntes;
            this.getActual = getAntes;
        }
        if (enmarcado) {
            this.saltearDatosPropios();
        }
    }

    /**
     * Los datos de un objeto comun, **de la superclase serializable mas alta hacia abajo**, que es
     * el sentido en que el escritor los puso.
     */
    private void leerDatosSerie(Object obj, ObjectStreamClass desc)
            throws IOException, ClassNotFoundException {
        ObjectStreamClass[] tramos = cadenaDeFlujo(desc);
        int i = 0;
        while (i < tramos.length) {
            ObjectStreamClass slot = tramos[i];
            Class<?> cl = slot.forClass();
            boolean conMetodo = (slot.banderasFlujo() & ObjectStreamConstants.SC_WRITE_METHOD) != 0;
            Method lector = cl == null ? null : metodoReadObject(cl);
            if (obj != null && lector != null) {
                Object objAntes = this.objetoActual;
                ObjectStreamClass descAntes = this.descActual;
                GetFieldImpl getAntes = this.getActual;
                this.objetoActual = obj;
                this.descActual = slot;
                this.getActual = null;
                try {
                    if (conMetodo) {
                        this.bin.modoBloque(true);
                    }
                    lector.invoke(obj, new Object[] { this });
                } finally {
                    this.objetoActual = objAntes;
                    this.descActual = descAntes;
                    this.getActual = getAntes;
                }
            } else {
                // Sin metodo local: los campos por defecto igual hay que consumirlos, tenga o no
                // esta VM donde ponerlos. Si el escritor uso un `writeObject` propio, los campos
                // estan solo si el llamo a `defaultWriteObject`; lo demas lo limpia el salteo de
                // abajo, que es exactamente para lo que existe el marco de bloque.
                this.leerCamposPorDefecto(obj, cl, slot);
            }
            if (conMetodo) {
                this.saltearDatosPropios();
            }
            i = i + 1;
        }
    }

    /** La cadena de descriptores del flujo, invertida: `[0]` es la superclase mas alta. */
    private static ObjectStreamClass[] cadenaDeFlujo(ObjectStreamClass desc) {
        int cuantos = 0;
        ObjectStreamClass d = desc;
        while (d != null) {
            cuantos = cuantos + 1;
            d = d.superiorFlujo();
        }
        ObjectStreamClass[] out = new ObjectStreamClass[cuantos];
        d = desc;
        int i = cuantos - 1;
        while (i >= 0) {
            out[i] = d;
            d = d.superiorFlujo();
            i = i - 1;
        }
        return out;
    }

    /**
     * Lee los campos de un tramo tal como los describe el flujo y los deja en `obj`.
     *
     * <p>El orden y los tipos los manda **el flujo**, no la clase local: primero todos los
     * primitivos pegados, despues las referencias. Recien con el valor ya leido se busca a que campo
     * local le toca, y si no hay ninguno se descarta. Hacerlo al reves --recorrer los campos locales
     * y buscar el valor-- desalinearia el flujo apenas la otra version tuviera un campo de mas.
     *
     * <p>`obj` puede ser `null`: es como se consume el tramo de una clase que de este lado no
     * existe sin perder la alineacion.
     */
    private void leerCamposPorDefecto(Object obj, Class<?> cl, ObjectStreamClass slot)
            throws IOException, ClassNotFoundException {
        ObjectStreamField[] campos = slot.getFields();
        // La forma serializada **local** es la que decide a que campo se puede escribir: un campo
        // `transient` o `static` de este lado no participa, y una clase con `serialPersistentFields`
        // decide ella cuales son los suyos. Ir directo a `getDeclaredField` se saltearia las dos
        // reglas y escribiria en campos que la clase habia sacado del formato a proposito.
        ObjectStreamClass local = (obj == null || cl == null) ? null : ObjectStreamClass.lookup(cl);
        int i = 0;
        while (i < campos.length && campos[i].isPrimitive()) {
            char t = campos[i].getTypeCode();
            Field f = campoLocal(local, cl, campos[i]);
            // Los `setXxx` con tipo y no `set` con envoltorio: son el espejo exacto de los
            // `getXxx` que uso el escritor, y no dependen de que el desenvoltorio de `set`
            // acierte la conversion.
            if (t == 'B') {
                byte v = (byte) this.bin.leerUnsignedByte();
                if (f != null) {
                    f.setByte(obj, v);
                }
            } else if (t == 'Z') {
                boolean v = this.bin.leerUnsignedByte() != 0;
                if (f != null) {
                    f.setBoolean(obj, v);
                }
            } else if (t == 'C') {
                char v = (char) this.bin.leerUnsignedShort();
                if (f != null) {
                    f.setChar(obj, v);
                }
            } else if (t == 'S') {
                short v = (short) this.bin.leerUnsignedShort();
                if (f != null) {
                    f.setShort(obj, v);
                }
            } else if (t == 'I') {
                int v = this.bin.leerInt();
                if (f != null) {
                    f.setInt(obj, v);
                }
            } else if (t == 'J') {
                long v = this.bin.leerLong();
                if (f != null) {
                    f.setLong(obj, v);
                }
            } else if (t == 'F') {
                float v = Float.intBitsToFloat(this.bin.leerInt());
                if (f != null) {
                    f.setFloat(obj, v);
                }
            } else {
                double v = Double.longBitsToDouble(this.bin.leerLong());
                if (f != null) {
                    f.setDouble(obj, v);
                }
            }
            i = i + 1;
        }
        while (i < campos.length) {
            Object v = this.leerObjeto0(campos[i].isUnshared());
            Field f = campoLocal(local, cl, campos[i]);
            if (f != null && (v == null || f.getType().isInstance(v))) {
                f.set(obj, v);
            }
            i = i + 1;
        }
    }

    /**
     * El campo local que le corresponde a un campo del flujo, o `null` si no hay ninguno que sirva.
     *
     * <p>Tienen que coincidir **el nombre y la firma**: un campo con el mismo nombre y otro tipo es
     * otro campo, y meterle el valor del flujo escribiria basura donde el que compilo esperaba lo
     * suyo. Devolver `null` no es un error: es como se descarta un campo que la version que escribio
     * tenia y esta ya no.
     */
    private static Field campoLocal(ObjectStreamClass local, Class<?> cl, ObjectStreamField campo) {
        if (local == null) {
            return null;
        }
        ObjectStreamField lf = local.getField(campo.getName());
        if (lf == null) {
            return null;
        }
        String firmaLocal = lf.isPrimitive()
                ? String.valueOf(lf.getTypeCode()) : lf.getTypeString();
        String firmaFlujo = campo.isPrimitive()
                ? String.valueOf(campo.getTypeCode()) : campo.getTypeString();
        if (!firmaLocal.equals(firmaFlujo)) {
            return null;
        }
        return ObjectOutputStream.campoReal(cl, lf.getName(), lf.getType());
    }

    /**
     * El {@code private void readObject(ObjectInputStream)} declarado por `cl`, o `null`.
     *
     * <p>Tiene que ser **privado y declarado por esa clase exacta**, por lo mismo que su contraparte
     * de escritura: no es un override sino un gancho por posicion en la jerarquia, y uno heredado
     * correria dos veces leyendo los mismos datos.
     */
    static Method metodoReadObject(Class<?> cl) {
        return ObjectOutputStream.metodoSerial(cl, "readObject", ObjectInputStream.class, void.class);
    }

    // ---- lo que un readObject de usuario puede llamar ---------------------------------------------

    /**
     * Lee los campos por defecto del tramo que se esta deserializando.
     *
     * <p>Existe para que un {@code readObject} propio pueda hacer "lo de siempre y ademas esto":
     * llamarlo primero y despues leer lo suyo es el patron normal de una clase que agrego datos sin
     * cambiar la forma de sus campos.
     *
     * @throws NotActiveException si no se esta leyendo un objeto
     */
    public void defaultReadObject() throws IOException, ClassNotFoundException {
        if (this.objetoActual == null || this.descActual == null) {
            throw new NotActiveException("not in call to readObject");
        }
        boolean bloqueAntes = this.bin.modoBloque(false);
        try {
            this.leerCamposPorDefecto(this.objetoActual, this.descActual.forClass(), this.descActual);
        } finally {
            this.bin.modoBloque(bloqueAntes);
        }
    }

    /**
     * Los campos del tramo en curso, por nombre, para leerlos sin depender de que la clase local
     * todavia declare cada uno.
     *
     * <p>Es la contraparte de {@link ObjectOutputStream#putFields}, y la salida de una clase que
     * cambio sus campos: {@link GetField#get(String, int)} y sus hermanos devuelven el valor por
     * omision cuando el flujo no traia ese campo, asi que una version nueva puede leer un flujo
     * viejo sin adivinar que traia.
     *
     * @throws NotActiveException si no se esta leyendo un objeto
     */
    public ObjectInputStream.GetField readFields() throws IOException, ClassNotFoundException {
        if (this.objetoActual == null || this.descActual == null) {
            throw new NotActiveException("not in call to readObject");
        }
        if (this.getActual == null) {
            GetFieldImpl g = new GetFieldImpl(this.descActual);
            boolean bloqueAntes = this.bin.modoBloque(false);
            try {
                g.leer(this);
            } finally {
                this.bin.modoBloque(bloqueAntes);
            }
            this.getActual = g;
        }
        return this.getActual;
    }

    /**
     * Pide que se llame a {@code obj.validateObject()} cuando el grafo entero este armado.
     *
     * <p>Sirve para las invariantes que cruzan varios objetos: adentro de un {@code readObject} las
     * referencias del objeto pueden apuntar a instancias cuyos campos todavia no se llenaron, asi
     * que comprobarlas ahi da falsos negativos. Las validaciones corren de mayor a menor prioridad.
     *
     * @throws NotActiveException si no se esta leyendo un objeto
     * @throws InvalidObjectException si `obj` es `null`
     */
    public void registerValidation(ObjectInputValidation obj, int prio)
            throws NotActiveException, InvalidObjectException {
        if (this.profundidad == 0) {
            throw new NotActiveException("stream inactive");
        }
        if (obj == null) {
            throw new InvalidObjectException("null callback");
        }
        if (this.cuantasValidaciones == this.validaciones.length) {
            ObjectInputValidation[] v2 = new ObjectInputValidation[this.cuantasValidaciones * 2];
            int[] p2 = new int[this.cuantasValidaciones * 2];
            System.arraycopy(this.validaciones, 0, v2, 0, this.cuantasValidaciones);
            System.arraycopy(this.prioridades, 0, p2, 0, this.cuantasValidaciones);
            this.validaciones = v2;
            this.prioridades = p2;
        }
        this.validaciones[this.cuantasValidaciones] = obj;
        this.prioridades[this.cuantasValidaciones] = prio;
        this.cuantasValidaciones = this.cuantasValidaciones + 1;
    }

    private void correrValidaciones() throws InvalidObjectException {
        int cuantas = this.cuantasValidaciones;
        if (cuantas == 0) {
            return;
        }
        ObjectInputValidation[] vs = this.validaciones;
        int[] ps = this.prioridades;
        // La tabla se vacia **antes** de correr nada: una validacion que fallara dejaria si no las
        // suyas registradas para el proximo `readObject` de este mismo flujo.
        this.validaciones = new ObjectInputValidation[4];
        this.prioridades = new int[4];
        this.cuantasValidaciones = 0;
        // De mayor a menor prioridad, y estable entre iguales (insercion sobre pocos elementos: son
        // las que una lectura registro, no una coleccion).
        int i = 1;
        while (i < cuantas) {
            ObjectInputValidation v = vs[i];
            int p = ps[i];
            int j = i - 1;
            while (j >= 0 && ps[j] < p) {
                vs[j + 1] = vs[j];
                ps[j + 1] = ps[j];
                j = j - 1;
            }
            vs[j + 1] = v;
            ps[j + 1] = p;
            i = i + 1;
        }
        i = 0;
        while (i < cuantas) {
            vs[i].validateObject();
            i = i + 1;
        }
    }

    // ---- el filtro ------------------------------------------------------------------------------

    /** El filtro instalado en este flujo, o `null` si no hay ninguno. */
    public final ObjectInputFilter getObjectInputFilter() {
        return this.filtro;
    }

    /**
     * Instala el filtro que decide que clases puede reconstruir este flujo.
     *
     * <p>Se puede fijar **una sola vez y antes de leer nada**: un filtro que se pudiera cambiar a
     * mitad de un grafo no seria una politica sino una sugerencia, porque bastaria que el propio
     * flujo llevara a ejecutar el cambio para desactivarlo.
     *
     * @throws IllegalStateException si ya se fijo uno, o si ya se leyo algun objeto
     */
    public final void setObjectInputFilter(ObjectInputFilter filter) {
        if (this.filtroFijado) {
            throw new IllegalStateException("filter can not be set more than once");
        }
        if (this.cuantasManijas != 0) {
            throw new IllegalStateException("filter can not be set after an object has been read");
        }
        this.filtro = filter;
        this.filtroFijado = true;
    }

    /**
     * Le pregunta al filtro por la clase que esta por armarse.
     *
     * @throws InvalidClassException si el filtro la rechaza
     */
    private void consultarFiltro(Class<?> cl, long largoArreglo) throws InvalidClassException {
        if (this.filtro == null) {
            return;
        }
        ObjectInputFilter.Status s = this.filtro.checkInput(
                new InfoFiltro(cl, largoArreglo, this.profundidad,
                        this.cuantasManijas, this.bin.bytesLeidos()));
        if (s == null || s == ObjectInputFilter.Status.REJECTED) {
            throw new InvalidClassException("filter status: " + s);
        }
    }

    private static final class InfoFiltro implements ObjectInputFilter.FilterInfo {
        private final Class<?> clase;
        private final long largo;
        private final long profundidad;
        private final long referencias;
        private final long bytes;

        InfoFiltro(Class<?> clase, long largo, long profundidad, long referencias, long bytes) {
            this.clase = clase;
            this.largo = largo;
            this.profundidad = profundidad;
            this.referencias = referencias;
            this.bytes = bytes;
        }

        public Class<?> serialClass() {
            return this.clase;
        }

        public long arrayLength() {
            return this.largo;
        }

        public long depth() {
            return this.profundidad;
        }

        public long references() {
            return this.referencias;
        }

        public long streamBytes() {
            return this.bytes;
        }
    }

    // ---- las manijas ----------------------------------------------------------------------------

    private int asignarManija(Object o) {
        if (this.cuantasManijas == this.manijas.length) {
            Object[] n = new Object[this.cuantasManijas * 2];
            System.arraycopy(this.manijas, 0, n, 0, this.cuantasManijas);
            this.manijas = n;
        }
        this.manijas[this.cuantasManijas] = o;
        this.cuantasManijas = this.cuantasManijas + 1;
        return this.cuantasManijas - 1;
    }

    private void limpiarManijas() {
        int i = 0;
        while (i < this.cuantasManijas) {
            this.manijas[i] = null;
            i = i + 1;
        }
        this.cuantasManijas = 0;
    }

    // ---- DataInput y InputStream ------------------------------------------------------------------

    public int read() throws IOException {
        return this.bin.leer();
    }

    public int read(byte[] buf, int off, int len) throws IOException {
        if (buf == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > buf.length - off) {
            throw new IndexOutOfBoundsException();
        }
        return this.bin.leer(buf, off, len);
    }

    public int available() throws IOException {
        return this.bin.disponibles();
    }

    public void close() throws IOException {
        this.bin.cerrar();
    }

    public boolean readBoolean() throws IOException {
        return this.bin.leerUnsignedByte() != 0;
    }

    public byte readByte() throws IOException {
        return (byte) this.bin.leerUnsignedByte();
    }

    public int readUnsignedByte() throws IOException {
        return this.bin.leerUnsignedByte();
    }

    public char readChar() throws IOException {
        return (char) this.bin.leerUnsignedShort();
    }

    public short readShort() throws IOException {
        return (short) this.bin.leerUnsignedShort();
    }

    public int readUnsignedShort() throws IOException {
        return this.bin.leerUnsignedShort();
    }

    public int readInt() throws IOException {
        return this.bin.leerInt();
    }

    public long readLong() throws IOException {
        return this.bin.leerLong();
    }

    public float readFloat() throws IOException {
        return Float.intBitsToFloat(this.bin.leerInt());
    }

    public double readDouble() throws IOException {
        return Double.longBitsToDouble(this.bin.leerLong());
    }

    public void readFully(byte[] buf) throws IOException {
        this.readFully(buf, 0, buf.length);
    }

    public void readFully(byte[] buf, int off, int len) throws IOException {
        if (off < 0 || len < 0 || len > buf.length - off) {
            throw new IndexOutOfBoundsException();
        }
        int i = 0;
        while (i < len) {
            buf[off + i] = (byte) this.bin.leerUnsignedByte();
            i = i + 1;
        }
    }

    public int skipBytes(int len) throws IOException {
        int i = 0;
        while (i < len) {
            if (this.bin.leer() < 0) {
                return i;
            }
            i = i + 1;
        }
        return i;
    }

    /**
     * Una linea de bytes, cada uno ensanchado a `char`.
     *
     * @deprecated No convierte de bytes a caracteres: cada byte se vuelve el `char` de su valor, lo
     *     que solo coincide con el texto para Latin-1. Para leer texto va un {@link BufferedReader}
     *     sobre un {@link InputStreamReader} con el juego de caracteres explicito.
     */
    @Deprecated
    public String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int c = this.bin.leer();
        if (c < 0) {
            return null;
        }
        while (c >= 0 && c != '\n') {
            if (c == '\r') {
                // El `\r\n` se consume entero, pero un `\r` suelto tambien termina la linea: mirar
                // el siguiente byte sin consumirlo es la unica forma de distinguirlos sin comerse
                // el primer caracter de la linea que viene.
                if (this.bin.mirar() == '\n') {
                    this.bin.leer();
                }
                return sb.toString();
            }
            sb.append((char) c);
            c = this.bin.leer();
        }
        return sb.toString();
    }

    public String readUTF() throws IOException {
        return this.bin.leerUtf();
    }

    // ---- GetField --------------------------------------------------------------------------------

    /**
     * Los campos del tramo en curso, leidos por nombre.
     *
     * <p>Todo `get` lleva un valor por omision, y ese es el punto: el flujo pudo haber sido escrito
     * por una version de la clase que no tenia ese campo, y lo que se devuelve entonces es el valor
     * que el que llama eligio. {@link #defaulted} distingue las dos situaciones cuando importa.
     */
    public abstract static class GetField {

        /** El descriptor del tramo, tal como vino del flujo. */
        public abstract ObjectStreamClass getObjectStreamClass();

        /**
         * Si `name` **no** vino en el flujo y por lo tanto su `get` devolveria el valor por omision.
         *
         * @throws IllegalArgumentException si `name` no es un campo de este tramo ni de la clase
         */
        public abstract boolean defaulted(String name) throws IOException;

        public abstract boolean get(String name, boolean val) throws IOException;

        public abstract byte get(String name, byte val) throws IOException;

        public abstract char get(String name, char val) throws IOException;

        public abstract short get(String name, short val) throws IOException;

        public abstract int get(String name, int val) throws IOException;

        public abstract long get(String name, long val) throws IOException;

        public abstract float get(String name, float val) throws IOException;

        public abstract double get(String name, double val) throws IOException;

        public abstract Object get(String name, Object val) throws IOException, ClassNotFoundException;
    }

    /**
     * Los valores del tramo, ya leidos del flujo y guardados hasta que alguien los pida.
     *
     * <p>Se leen todos de una y no de a uno bajo demanda porque el flujo es secuencial: dejar la
     * lectura para cuando se llame a `get` ataria el orden de los `get` al orden del formato, y
     * quien pidiera dos campos al reves leeria uno en lugar del otro.
     */
    private static final class GetFieldImpl extends ObjectInputStream.GetField {
        private final ObjectStreamClass desc;
        private final ObjectStreamField[] campos;
        private final byte[] primitivos;
        private final Object[] referencias;

        GetFieldImpl(ObjectStreamClass desc) {
            this.desc = desc;
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

        void leer(ObjectInputStream in) throws IOException, ClassNotFoundException {
            int i = 0;
            while (i < this.campos.length && this.campos[i].isPrimitive()) {
                in.bin.leerCrudo(this.primitivos, this.campos[i].getOffset(),
                        ancho(this.campos[i].getTypeCode()));
                i = i + 1;
            }
            while (i < this.campos.length) {
                this.referencias[this.campos[i].getOffset()] =
                        in.leerObjeto0(this.campos[i].isUnshared());
                i = i + 1;
            }
        }

        public ObjectStreamClass getObjectStreamClass() {
            return this.desc;
        }

        public boolean defaulted(String name) throws IOException {
            return this.buscar(name, (char) 0) == null;
        }

        /**
         * El campo `name` dentro del flujo, o `null` si el flujo no lo trajo pero la clase local si
         * lo tiene --que es el caso en que corresponde devolver el valor por omision--.
         *
         * <p>Las tres respuestas son distintas y hay que distinguirlas. Que el flujo no traiga un
         * campo que la clase tiene es lo normal al leer algo escrito por una version anterior.
         * Preguntar por un nombre que **no existe en ninguno de los dos lados** no es eso: es que el
         * que llama se equivoco de nombre --o de tipo--, y devolverle su propio valor por omision le
         * escondería el error para siempre, porque nunca veria nada distinto de lo que el paso.
         *
         * @throws IllegalArgumentException si `name` con ese tipo no es un campo ni del flujo ni de
         *     la clase
         */
        private ObjectStreamField buscar(String name, char tipo) {
            if (name == null) {
                throw new NullPointerException();
            }
            ObjectStreamField f = coincide(this.campos, name, tipo);
            if (f != null) {
                return f;
            }
            ObjectStreamClass local = this.desc.forClass() == null
                    ? null : ObjectStreamClass.lookup(this.desc.forClass());
            if (local != null && coincide(local.getFields(), name, tipo) != null) {
                return null;
            }
            throw new IllegalArgumentException("no such field " + name + " with type "
                    + nombreTipo(tipo));
        }

        private static ObjectStreamField coincide(ObjectStreamField[] cs, String name, char tipo) {
            int i = 0;
            while (i < cs.length) {
                if (cs[i].getName().equals(name)) {
                    char t = cs[i].getTypeCode();
                    // `tipo == 0` es la consulta de `defaulted`, que pregunta solo por el nombre.
                    // `'L'` pedido casa con cualquier referencia --arreglos incluidos-- porque la
                    // firma de `get(String, Object)` no puede decir cual; los primitivos tienen que
                    // dar exacto, que un `int` no es un `short`.
                    if (tipo == 0 || (tipo == 'L' ? (t == 'L' || t == '[') : t == tipo)) {
                        return cs[i];
                    }
                    return null;
                }
                i = i + 1;
            }
            return null;
        }

        private static String nombreTipo(char tipo) {
            if (tipo == 'Z') {
                return "boolean";
            }
            if (tipo == 'B') {
                return "byte";
            }
            if (tipo == 'C') {
                return "char";
            }
            if (tipo == 'S') {
                return "short";
            }
            if (tipo == 'I') {
                return "int";
            }
            if (tipo == 'J') {
                return "long";
            }
            if (tipo == 'F') {
                return "float";
            }
            if (tipo == 'D') {
                return "double";
            }
            return "class java.lang.Object";
        }

        public boolean get(String name, boolean val) throws IOException {
            ObjectStreamField f = this.buscar(name, 'Z');
            return f == null ? val : this.primitivos[f.getOffset()] != 0;
        }

        public byte get(String name, byte val) throws IOException {
            ObjectStreamField f = this.buscar(name, 'B');
            return f == null ? val : this.primitivos[f.getOffset()];
        }

        public char get(String name, char val) throws IOException {
            ObjectStreamField f = this.buscar(name, 'C');
            return f == null ? val : (char) this.corto(f.getOffset());
        }

        public short get(String name, short val) throws IOException {
            ObjectStreamField f = this.buscar(name, 'S');
            return f == null ? val : (short) this.corto(f.getOffset());
        }

        public int get(String name, int val) throws IOException {
            ObjectStreamField f = this.buscar(name, 'I');
            return f == null ? val : this.entero(f.getOffset());
        }

        public long get(String name, long val) throws IOException {
            ObjectStreamField f = this.buscar(name, 'J');
            return f == null ? val : this.largo(f.getOffset());
        }

        public float get(String name, float val) throws IOException {
            ObjectStreamField f = this.buscar(name, 'F');
            return f == null ? val : Float.intBitsToFloat(this.entero(f.getOffset()));
        }

        public double get(String name, double val) throws IOException {
            ObjectStreamField f = this.buscar(name, 'D');
            return f == null ? val : Double.longBitsToDouble(this.largo(f.getOffset()));
        }

        public Object get(String name, Object val) throws IOException, ClassNotFoundException {
            ObjectStreamField f = this.buscar(name, 'L');
            return f == null ? val : this.referencias[f.getOffset()];
        }

        private int corto(int off) {
            return ((this.primitivos[off] & 0xFF) << 8) | (this.primitivos[off + 1] & 0xFF);
        }

        private int entero(int off) {
            return ((this.primitivos[off] & 0xFF) << 24)
                    | ((this.primitivos[off + 1] & 0xFF) << 16)
                    | ((this.primitivos[off + 2] & 0xFF) << 8)
                    | (this.primitivos[off + 3] & 0xFF);
        }

        private long largo(int off) {
            return (((long) this.entero(off)) << 32) | (((long) this.entero(off + 4)) & 0xFFFFFFFFL);
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
            return 4;                  // I y F
        }
    }

    // ---- la capa de bloques -----------------------------------------------------------------------

    /**
     * La capa que desenvuelve los registros de bloque, espejo de la `SalidaBloques` del escritor.
     *
     * <p>En modo bloque cada lectura sale del registro {@code TC_BLOCKDATA} en curso, y cuando se
     * acaba **no se sigue de largo**: hay que ver si lo que viene es otro registro o el
     * {@code TC_ENDBLOCKDATA} que cierra. Fuera de modo bloque los bytes salen crudos, que es como
     * se leen los typecodes y los campos por defecto.
     *
     * <p>Alcanza con **un byte de anticipo**: todo lo que hay que decidir --si sigue un registro,
     * si sigue un typecode-- se decide mirando el proximo byte, y nunca hay que devolver mas de uno.
     */
    private static final class EntradaBloques {
        private final InputStream in;

        /** El byte leido y no consumido, o -1 si no hay ninguno guardado. */
        private int mirado = -1;
        private boolean bloque;
        /** Bytes que quedan del registro de bloque en curso. */
        private int restan;
        private long consumidos;

        EntradaBloques(InputStream in) {
            this.in = in;
        }

        boolean enModoBloque() {
            return this.bloque;
        }

        /** Cambia de modo y devuelve el anterior. */
        boolean modoBloque(boolean nuevo) throws IOException {
            boolean antes = this.bloque;
            if (antes != nuevo) {
                if (antes && this.restan > 0) {
                    // Salir de modo bloque con datos sin consumir haria que el proximo byte de datos
                    // se leyera como typecode: todo lo que viniera despues quedaria desalineado, y
                    // el sintoma aparecería muy lejos de la causa.
                    throw new IllegalStateException("unread block data");
                }
                this.bloque = nuevo;
                this.restan = 0;
            }
            return antes;
        }

        long bytesLeidos() {
            return this.consumidos;
        }

        /** El proximo byte sin consumirlo, o -1 en fin de flujo. */
        int mirar() throws IOException {
            if (this.mirado < 0) {
                this.mirado = this.in.read();
            }
            return this.mirado;
        }

        int leerByteCrudo() throws IOException {
            int b = this.mirar();
            this.mirado = -1;
            if (b >= 0) {
                this.consumidos = this.consumidos + 1;
            }
            return b;
        }

        /**
         * Deja `restan` con los bytes disponibles del registro en curso, leyendo la cabecera del
         * siguiente si hiciera falta. Al volver, `restan == 0` significa que lo que sigue **no** es
         * un registro de bloque.
         */
        private void asegurarBloque() throws IOException {
            while (this.bloque && this.restan == 0) {
                int tc = this.mirar();
                if (tc == ObjectStreamConstants.TC_BLOCKDATA) {
                    this.leerByteCrudo();
                    int n = this.leerByteCrudo();
                    if (n < 0) {
                        throw new EOFException();
                    }
                    this.restan = n;
                } else if (tc == ObjectStreamConstants.TC_BLOCKDATALONG) {
                    this.leerByteCrudo();
                    int n = this.leerIntCrudo();
                    if (n < 0) {
                        throw new StreamCorruptedException("illegal block data header length: " + n);
                    }
                    this.restan = n;
                } else {
                    return;
                }
            }
        }

        /** Cuantos bytes de datos quedan en el registro en curso. */
        int cuantosQuedan() throws IOException {
            this.asegurarBloque();
            return this.restan;
        }

        void saltarBloque() throws IOException {
            for (;;) {
                this.asegurarBloque();
                if (this.restan == 0) {
                    return;
                }
                while (this.restan > 0) {
                    if (this.leerByteCrudo() < 0) {
                        throw new EOFException();
                    }
                    this.restan = this.restan - 1;
                }
            }
        }

        /** Un byte de datos; falla en fin de flujo. Es lo que usan los `readXxx` de `DataInput`. */
        private int unByte() throws IOException {
            int b = this.leer();
            if (b < 0) {
                throw new EOFException();
            }
            return b;
        }

        /** Un byte de datos, o -1 cuando no hay mas. Es lo que usa `read()`. */
        int leer() throws IOException {
            if (this.bloque) {
                this.asegurarBloque();
                if (this.restan == 0) {
                    return -1;
                }
                this.restan = this.restan - 1;
            }
            return this.leerByteCrudo();
        }

        int leer(byte[] buf, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            int i = 0;
            while (i < len) {
                int b = this.leer();
                if (b < 0) {
                    return i == 0 ? -1 : i;
                }
                buf[off + i] = (byte) b;
                i = i + 1;
            }
            return i;
        }

        /** Exactamente `len` bytes de datos; falla si no estan. */
        void leerCrudo(byte[] buf, int off, int len) throws IOException {
            int i = 0;
            while (i < len) {
                buf[off + i] = (byte) this.unByte();
                i = i + 1;
            }
        }

        /** El proximo typecode. No hay modo bloque aca: un typecode nunca va enmarcado. */
        int mirarObligatorio() throws IOException {
            int b = this.mirar();
            if (b < 0) {
                throw new EOFException();
            }
            return b;
        }

        int leerUnsignedByte() throws IOException {
            return this.unByte();
        }

        int leerUnsignedShort() throws IOException {
            return (this.unByte() << 8) | this.unByte();
        }

        int leerInt() throws IOException {
            return (this.unByte() << 24) | (this.unByte() << 16) | (this.unByte() << 8)
                    | this.unByte();
        }

        private int leerIntCrudo() throws IOException {
            int a = this.leerByteCrudo();
            int b = this.leerByteCrudo();
            int c = this.leerByteCrudo();
            int d = this.leerByteCrudo();
            if ((a | b | c | d) < 0) {
                throw new EOFException();
            }
            return (a << 24) | (b << 16) | (c << 8) | d;
        }

        long leerLong() throws IOException {
            long alta = this.leerInt();
            long baja = this.leerInt();
            return (alta << 32) | (baja & 0xFFFFFFFFL);
        }

        String leerUtf() throws IOException {
            return this.leerUtfCruda(this.leerUnsignedShort());
        }

        /**
         * UTF-8 **modificado**: el cero viene en dos bytes y cada `char` esta codificado por su
         * cuenta, asi que un par suplente son dos secuencias de tres bytes y no una de cuatro.
         * Decodificarlo con un UTF-8 de verdad daria mal justo esos dos casos.
         */
        String leerUtfCruda(long bytes) throws IOException {
            StringBuilder sb = new StringBuilder();
            long leidos = 0;
            while (leidos < bytes) {
                int b1 = this.unByte();
                leidos = leidos + 1;
                if (b1 < 0x80) {
                    if (b1 == 0) {
                        throw new UTFDataFormatException("malformed input: zero byte");
                    }
                    sb.append((char) b1);
                } else if ((b1 & 0xE0) == 0xC0) {
                    if (leidos >= bytes) {
                        throw new UTFDataFormatException("malformed input: partial character at end");
                    }
                    int b2 = this.unByte();
                    leidos = leidos + 1;
                    if ((b2 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException("malformed input around byte " + leidos);
                    }
                    sb.append((char) (((b1 & 0x1F) << 6) | (b2 & 0x3F)));
                } else if ((b1 & 0xF0) == 0xE0) {
                    if (leidos + 1 >= bytes) {
                        throw new UTFDataFormatException("malformed input: partial character at end");
                    }
                    int b2 = this.unByte();
                    int b3 = this.unByte();
                    leidos = leidos + 2;
                    if ((b2 & 0xC0) != 0x80 || (b3 & 0xC0) != 0x80) {
                        throw new UTFDataFormatException("malformed input around byte " + leidos);
                    }
                    sb.append((char) (((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F)));
                } else {
                    throw new UTFDataFormatException("malformed input around byte " + leidos);
                }
            }
            return sb.toString();
        }

        int disponibles() throws IOException {
            if (this.bloque) {
                this.asegurarBloque();
                return this.restan;
            }
            int n = this.in.available();
            return this.mirado >= 0 ? n + 1 : n;
        }

        void cerrar() throws IOException {
            this.in.close();
        }
    }

    // ---- utilidades ------------------------------------------------------------------------------

    private static Class<?> primitivaPorNombre(String n) {
        if (n.equals("int")) {
            return Integer.TYPE;
        }
        if (n.equals("long")) {
            return Long.TYPE;
        }
        if (n.equals("double")) {
            return Double.TYPE;
        }
        if (n.equals("float")) {
            return Float.TYPE;
        }
        if (n.equals("byte")) {
            return Byte.TYPE;
        }
        if (n.equals("short")) {
            return Short.TYPE;
        }
        if (n.equals("char")) {
            return Character.TYPE;
        }
        if (n.equals("boolean")) {
            return Boolean.TYPE;
        }
        if (n.equals("void")) {
            return Void.TYPE;
        }
        return null;
    }

    private static String hex2(int v) {
        String s = Integer.toHexString(v & 0xFF);
        return s.length() < 2 ? "0" + s : s;
    }

    private static String hex4(int v) {
        String s = Integer.toHexString(v & 0xFFFF);
        while (s.length() < 4) {
            s = "0" + s;
        }
        return s;
    }
}
