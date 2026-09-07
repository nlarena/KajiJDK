package sun.misc;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * El acceso directo a la memoria y a la disposicion de los objetos, sin pasar por el sistema de
 * tipos.
 *
 * <h2>Por que existe una clase asi</h2>
 *
 * <p>Porque la biblioteca de Java se implementa a si misma. {@code java.util.concurrent} necesita
 * comparar-y-cambiar; {@code ByteBuffer} directo necesita memoria fuera del monton; la
 * deserializacion necesita crear un objeto sin llamar a su constructor. Nada de eso se puede
 * escribir en Java, y esta clase es el agujero por donde la VM lo ofrece.
 *
 * <p>Nunca fue API publica: vive en {@code sun.misc}, el paquete que por convencion significa "no
 * es para vos". Que este exportado por el modulo {@code jdk.unsupported} es un reconocimiento de la
 * realidad --media industria depende de ella-- y no una promesa.
 *
 * <h2>Por que {@link #getUnsafe} falla</h2>
 *
 * <p>Falla tambien en el JDK: lanza {@code SecurityException} salvo que quien llama este cargado
 * por el cargador del sistema. Es la razon de que todo el mundo la consiga por reflexion sobre el
 * campo {@code theUnsafe} en vez de llamar a este metodo.
 *
 * <p>Aca hace lo mismo, y no es un agregado: es el comportamiento que la clase tiene para cualquier
 * codigo de aplicacion.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Los ciento y pico de metodos lanzan {@link UnsupportedOperationException} nombrando lo que
 * falta. No es pereza: cada uno de ellos <strong>es</strong> una operacion de la VM. Leer un campo
 * por su desplazamiento en bytes, reservar memoria fuera del monton, crear un objeto sin
 * constructor o suspender un hilo no son cosas que se puedan implementar en Java --si se pudieran,
 * esta clase no existiria--.
 *
 * <p>La alternativa seria devolver ceros y no escribir nada, y eso es exactamente el caso que la
 * casa evita: un {@code compareAndSwapInt} que contesta {@code true} sin haber cambiado nada
 * produce estructuras concurrentes silenciosamente corruptas.
 *
 * <p>Lo que si esta es la <strong>forma</strong>: la clase, el campo {@code theUnsafe} con su
 * nombre exacto, y las firmas. Codigo que la use compila; el dia que la VM tenga las primitivas,
 * los cuerpos se llenan sin que nadie tenga que recompilar contra otra cosa.
 */
public final class Unsafe {

    private static final String NO_HAY =
            "esta operacion es una primitiva de la VM (acceso a memoria por desplazamiento, "
            + "reserva fuera del monton, creacion sin constructor o bloqueo de hilos) y esta VM no "
            + "la expone";

    /**
     * Lo que devuelve {@link #staticFieldOffset} para un campo que no tiene desplazamiento.
     *
     * <p>Es la unica constante de esta clase con un valor que significa algo: las demas describen
     * la disposicion en memoria de esta VM, y esta VM no la expone.
     */
    public static final int INVALID_FIELD_OFFSET = -1;

    // Las constantes de disposicion de arreglos.
    //
    // En el JDK no son constantes de compilacion: se calculan al inicializar la clase preguntandole
    // a la VM como acomoda cada tipo de arreglo. Aca se calculan igual --en un bloque estatico, no
    // como literales-- para que no queden incrustadas en quien las lee, que es lo que pasaria si
    // fueran `= 0` a secas.
    //
    // Valen CERO, y cero no es un desplazamiento ni una escala real: ningun arreglo de Java empieza
    // en el byte 0 de su propio objeto, porque antes esta la cabecera. O sea que el valor se lee
    // como "no se sabe" y no como un dato.
    //
    // Que eso no pueda causar dano callado no es casualidad: TODOS los metodos que consumirian
    // estos numeros --los `get`/`put` por desplazamiento-- lanzan excepcion. No hay forma de que un
    // calculo hecho con ellos termine leyendo la memoria equivocada, porque nunca llega a leerla.

    /** El desplazamiento del primer elemento de un {@code boolean[]}; ver la nota de arriba. */
    public static final int ARRAY_BOOLEAN_BASE_OFFSET;

    /** El desplazamiento del primer elemento de un {@code byte[]}; ver la nota de arriba. */
    public static final int ARRAY_BYTE_BASE_OFFSET;

    /** El desplazamiento del primer elemento de un {@code short[]}; ver la nota de arriba. */
    public static final int ARRAY_SHORT_BASE_OFFSET;

    /** El desplazamiento del primer elemento de un {@code char[]}; ver la nota de arriba. */
    public static final int ARRAY_CHAR_BASE_OFFSET;

    /** El desplazamiento del primer elemento de un {@code int[]}; ver la nota de arriba. */
    public static final int ARRAY_INT_BASE_OFFSET;

    /** El desplazamiento del primer elemento de un {@code long[]}; ver la nota de arriba. */
    public static final int ARRAY_LONG_BASE_OFFSET;

    /** El desplazamiento del primer elemento de un {@code float[]}; ver la nota de arriba. */
    public static final int ARRAY_FLOAT_BASE_OFFSET;

    /** El desplazamiento del primer elemento de un {@code double[]}; ver la nota de arriba. */
    public static final int ARRAY_DOUBLE_BASE_OFFSET;

    /** El desplazamiento del primer elemento de un {@code Object[]}; ver la nota de arriba. */
    public static final int ARRAY_OBJECT_BASE_OFFSET;

    /** Cuantos bytes ocupa cada elemento de un {@code boolean[]}; ver la nota de arriba. */
    public static final int ARRAY_BOOLEAN_INDEX_SCALE;

    /** Cuantos bytes ocupa cada elemento de un {@code byte[]}; ver la nota de arriba. */
    public static final int ARRAY_BYTE_INDEX_SCALE;

    /** Cuantos bytes ocupa cada elemento de un {@code short[]}; ver la nota de arriba. */
    public static final int ARRAY_SHORT_INDEX_SCALE;

    /** Cuantos bytes ocupa cada elemento de un {@code char[]}; ver la nota de arriba. */
    public static final int ARRAY_CHAR_INDEX_SCALE;

    /** Cuantos bytes ocupa cada elemento de un {@code int[]}; ver la nota de arriba. */
    public static final int ARRAY_INT_INDEX_SCALE;

    /** Cuantos bytes ocupa cada elemento de un {@code long[]}; ver la nota de arriba. */
    public static final int ARRAY_LONG_INDEX_SCALE;

    /** Cuantos bytes ocupa cada elemento de un {@code float[]}; ver la nota de arriba. */
    public static final int ARRAY_FLOAT_INDEX_SCALE;

    /** Cuantos bytes ocupa cada elemento de un {@code double[]}; ver la nota de arriba. */
    public static final int ARRAY_DOUBLE_INDEX_SCALE;

    /** Cuantos bytes ocupa cada elemento de un {@code Object[]}; ver la nota de arriba. */
    public static final int ARRAY_OBJECT_INDEX_SCALE;

    /** El tamano de un puntero en esta VM, en bytes; ver la nota de arriba. */
    public static final int ADDRESS_SIZE;

    static {
        // Un bloque estatico y no literales: asi no se incrustan en quien las lee, igual que en el
        // JDK, donde salen de preguntarle a la VM.
        final int sinDato = 0;
        ARRAY_BOOLEAN_BASE_OFFSET = sinDato;
        ARRAY_BYTE_BASE_OFFSET = sinDato;
        ARRAY_SHORT_BASE_OFFSET = sinDato;
        ARRAY_CHAR_BASE_OFFSET = sinDato;
        ARRAY_INT_BASE_OFFSET = sinDato;
        ARRAY_LONG_BASE_OFFSET = sinDato;
        ARRAY_FLOAT_BASE_OFFSET = sinDato;
        ARRAY_DOUBLE_BASE_OFFSET = sinDato;
        ARRAY_OBJECT_BASE_OFFSET = sinDato;
        ARRAY_BOOLEAN_INDEX_SCALE = sinDato;
        ARRAY_BYTE_INDEX_SCALE = sinDato;
        ARRAY_SHORT_INDEX_SCALE = sinDato;
        ARRAY_CHAR_INDEX_SCALE = sinDato;
        ARRAY_INT_INDEX_SCALE = sinDato;
        ARRAY_LONG_INDEX_SCALE = sinDato;
        ARRAY_FLOAT_INDEX_SCALE = sinDato;
        ARRAY_DOUBLE_INDEX_SCALE = sinDato;
        ARRAY_OBJECT_INDEX_SCALE = sinDato;
        ADDRESS_SIZE = sinDato;
    }

    /**
     * La unica instancia.
     *
     * <p>Privada y con ese nombre a proposito: es el campo que el ecosistema entero alcanza por
     * reflexion, y renombrarlo romperia mas codigo que sacar la clase.
     */
    private static final Unsafe theUnsafe = new Unsafe();

    private Unsafe() {
    }


    /**
     * La instancia, si quien llama esta cargado por el cargador del sistema.
     *
     * @return la instancia
     * @throws SecurityException para cualquier otro llamador, que es lo mismo que hace el JDK
     */
    public static Unsafe getUnsafe() {
        throw new SecurityException("Unsafe");
    }


    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public int getInt(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putInt(Object o, long offset, int x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public Object getObject(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putObject(Object o, long offset, Object x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public boolean getBoolean(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putBoolean(Object o, long offset, boolean x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public byte getByte(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putByte(Object o, long offset, byte x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public short getShort(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putShort(Object o, long offset, short x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public char getChar(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putChar(Object o, long offset, char x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public long getLong(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putLong(Object o, long offset, long x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public float getFloat(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putFloat(Object o, long offset, float x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public double getDouble(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putDouble(Object o, long offset, double x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public byte getByte(long address) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putByte(long address, byte x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public short getShort(long address) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putShort(long address, short x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public char getChar(long address) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putChar(long address, char x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public int getInt(long address) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putInt(long address, int x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public long getLong(long address) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putLong(long address, long x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public float getFloat(long address) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putFloat(long address, float x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public double getDouble(long address) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee o escribe un campo por su desplazamiento en bytes, sin pasar por el sistema de tipos ni
     * por el control de acceso.
     *
     * @param address la direccion de memoria
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putDouble(long address, double x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Memoria fuera del monton, pedida al sistema y no al recolector: quien la pide la tiene que
     * liberar.
     *
     * @param address la direccion de memoria
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public long getAddress(long address) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Memoria fuera del monton, pedida al sistema y no al recolector: quien la pide la tiene que
     * liberar.
     *
     * @param address la direccion de memoria
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putAddress(long address, long x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Memoria fuera del monton, pedida al sistema y no al recolector: quien la pide la tiene que
     * liberar.
     *
     * @param bytes cuantos bytes
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public long allocateMemory(long bytes) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Memoria fuera del monton, pedida al sistema y no al recolector: quien la pide la tiene que
     * liberar.
     *
     * @param address la direccion de memoria
     * @param bytes cuantos bytes
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public long reallocateMemory(long address, long bytes) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Memoria fuera del monton, pedida al sistema y no al recolector: quien la pide la tiene que
     * liberar.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param bytes cuantos bytes
     * @param value el byte con el que rellenar
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void setMemory(Object o, long offset, long bytes, byte value) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Memoria fuera del monton, pedida al sistema y no al recolector: quien la pide la tiene que
     * liberar.
     *
     * @param address la direccion de memoria
     * @param bytes cuantos bytes
     * @param value el byte con el que rellenar
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void setMemory(long address, long bytes, byte value) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Memoria fuera del monton, pedida al sistema y no al recolector: quien la pide la tiene que
     * liberar.
     *
     * @param srcBase el objeto de origen, o {@code null}
     * @param srcOffset el desplazamiento de origen
     * @param destBase el objeto de destino, o {@code null}
     * @param destOffset el desplazamiento de destino
     * @param bytes cuantos bytes
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset,
            long bytes) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Memoria fuera del monton, pedida al sistema y no al recolector: quien la pide la tiene que
     * liberar.
     *
     * @param srcAddress la direccion de origen
     * @param destAddress la direccion de destino
     * @param bytes cuantos bytes
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void copyMemory(long srcAddress, long destAddress, long bytes) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Memoria fuera del monton, pedida al sistema y no al recolector: quien la pide la tiene que
     * liberar.
     *
     * @param address la direccion de memoria
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void freeMemory(long address) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Averigua la disposicion en memoria que la VM eligio. Es lo que convierte un {@code Field} en
     * el desplazamiento que los accesos por offset necesitan.
     *
     * @param f el campo
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public long objectFieldOffset(reflect.Field f) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Averigua la disposicion en memoria que la VM eligio. Es lo que convierte un {@code Field} en
     * el desplazamiento que los accesos por offset necesitan.
     *
     * @param f el campo
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public long staticFieldOffset(reflect.Field f) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Averigua la disposicion en memoria que la VM eligio. Es lo que convierte un {@code Field} en
     * el desplazamiento que los accesos por offset necesitan.
     *
     * @param f el campo
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public Object staticFieldBase(reflect.Field f) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Averigua la disposicion en memoria que la VM eligio. Es lo que convierte un {@code Field} en
     * el desplazamiento que los accesos por offset necesitan.
     *
     * @param arrayClass la clase del arreglo
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public int arrayBaseOffset(Class<?> arrayClass) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Averigua la disposicion en memoria que la VM eligio. Es lo que convierte un {@code Field} en
     * el desplazamiento que los accesos por offset necesitan.
     *
     * @param arrayClass la clase del arreglo
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public int arrayIndexScale(Class<?> arrayClass) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Averigua la disposicion en memoria que la VM eligio. Es lo que convierte un {@code Field} en
     * el desplazamiento que los accesos por offset necesitan.
     *
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public int addressSize() {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Averigua la disposicion en memoria que la VM eligio. Es lo que convierte un {@code Field} en
     * el desplazamiento que los accesos por offset necesitan.
     *
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public int pageSize() {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Crea una instancia <strong>sin llamar a ningun constructor</strong>. Es como los marcos de
     * serializacion reconstruyen un objeto sin ejecutar su inicializacion.
     *
     * @param cls la clase a instanciar
     * @return no vuelve
     * @throws InstantiationException declarada por la firma; no llega a lanzarse
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public Object allocateInstance(Class<?> cls) throws InstantiationException {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lanza una excepcion verificada sin declararla, rompiendo a proposito la comprobacion del
     * compilador.
     *
     * @param ee la excepcion a lanzar
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void throwException(Throwable ee) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Compara y cambia en un solo paso indivisible. Es la primitiva sobre la que esta construido
     * todo {@code java.util.concurrent}.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param expected el valor que se espera encontrar
     * @param x el valor
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public final boolean compareAndSwapObject(Object o, long offset, Object expected, Object x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Compara y cambia en un solo paso indivisible. Es la primitiva sobre la que esta construido
     * todo {@code java.util.concurrent}.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param expected el valor que se espera encontrar
     * @param x el valor
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public final boolean compareAndSwapInt(Object o, long offset, int expected, int x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Compara y cambia en un solo paso indivisible. Es la primitiva sobre la que esta construido
     * todo {@code java.util.concurrent}.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param expected el valor que se espera encontrar
     * @param x el valor
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public final boolean compareAndSwapLong(Object o, long offset, long expected, long x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public Object getObjectVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putObjectVolatile(Object o, long offset, Object x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public int getIntVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putIntVolatile(Object o, long offset, int x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public boolean getBooleanVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putBooleanVolatile(Object o, long offset, boolean x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public byte getByteVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putByteVolatile(Object o, long offset, byte x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public short getShortVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putShortVolatile(Object o, long offset, short x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public char getCharVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putCharVolatile(Object o, long offset, char x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public long getLongVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putLongVolatile(Object o, long offset, long x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public float getFloatVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putFloatVolatile(Object o, long offset, float x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public double getDoubleVolatile(Object o, long offset) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Como el acceso comun, pero con semantica {@code volatile}: la lectura ve lo ultimo escrito
     * por cualquier hilo y la escritura se hace visible para todos.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putDoubleVolatile(Object o, long offset, double x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Escritura con barrera de salida pero sin barrera de entrada: mas barata que la
     * {@code volatile}, y suficiente cuando lo unico que importa es que lo escrito antes se vea
     * antes.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putOrderedObject(Object o, long offset, Object x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Escritura con barrera de salida pero sin barrera de entrada: mas barata que la
     * {@code volatile}, y suficiente cuando lo unico que importa es que lo escrito antes se vea
     * antes.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putOrderedInt(Object o, long offset, int x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Escritura con barrera de salida pero sin barrera de entrada: mas barata que la
     * {@code volatile}, y suficiente cuando lo unico que importa es que lo escrito antes se vea
     * antes.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param x el valor
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void putOrderedLong(Object o, long offset, long x) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Suspende y despierta un hilo. Es la primitiva de bloqueo sobre la que se apoya
     * {@code LockSupport} y, por encima, todos los candados de la biblioteca.
     *
     * @param thread el hilo a despertar
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void unpark(Object thread) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Suspende y despierta un hilo. Es la primitiva de bloqueo sobre la que se apoya
     * {@code LockSupport} y, por encima, todos los candados de la biblioteca.
     *
     * @param isAbsolute si el plazo es un instante absoluto o un tiempo relativo
     * @param time el plazo
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void park(boolean isAbsolute, long time) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * La carga promedio del sistema, la misma que informa el sistema operativo.
     *
     * @param loadavg el arreglo donde dejar los promedios
     * @param nelems cuantos promedios pedir
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public int getLoadAverage(double[] loadavg, int nelems) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee y modifica en un solo paso indivisible, devolviendo el valor anterior.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param delta cuanto sumar
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public final int getAndAddInt(Object o, long offset, int delta) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee y modifica en un solo paso indivisible, devolviendo el valor anterior.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param delta cuanto sumar
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public final long getAndAddLong(Object o, long offset, long delta) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee y modifica en un solo paso indivisible, devolviendo el valor anterior.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param newValue el valor nuevo
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public final int getAndSetInt(Object o, long offset, int newValue) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee y modifica en un solo paso indivisible, devolviendo el valor anterior.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param newValue el valor nuevo
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public final long getAndSetLong(Object o, long offset, long newValue) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Lee y modifica en un solo paso indivisible, devolviendo el valor anterior.
     *
     * @param o el objeto, o {@code null} para una direccion absoluta
     * @param offset el desplazamiento en bytes dentro del objeto
     * @param newValue el valor nuevo
     * @return no vuelve
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public final Object getAndSetObject(Object o, long offset, Object newValue) {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Barrera de memoria: ordena los accesos de este hilo respecto de los que ven los demas.
     *
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void loadFence() {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Barrera de memoria: ordena los accesos de este hilo respecto de los que ven los demas.
     *
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void storeFence() {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Barrera de memoria: ordena los accesos de este hilo respecto de los que ven los demas.
     *
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void fullFence() {
        throw new UnsupportedOperationException(NO_HAY);
    }

    /**
     * Libera la memoria de un {@code ByteBuffer} directo sin esperar al recolector.
     *
     * @param directBuffer el buffer directo
     * @throws UnsupportedOperationException siempre; ver la nota de la clase
     */
    public void invokeCleaner(java.nio.ByteBuffer directBuffer) {
        throw new UnsupportedOperationException(NO_HAY);
    }

}
