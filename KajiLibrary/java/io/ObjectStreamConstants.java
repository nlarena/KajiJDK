package java.io;

// KajiLibrary's java.io.ObjectStreamConstants -- las constantes del formato de serializacion de
// Java.
//
// **Esto no es una eleccion de implementacion: es el formato, y esta especificado byte a byte** en
// la Java Object Serialization Specification. Los valores de abajo son los que hay, no los que a
// alguien le parecieron comodos, porque un `.ser` escrito por una VM lo tiene que poder leer otra.
// Cambiar cualquiera de estos numeros no "cambia nuestro formato": produce un archivo que ningun
// lector de Java entiende.
//
// Vale por si sola aunque nadie la use: con estas constantes se puede reconocer o recorrer un
// stream serializado sin depender de las clases que lo producen, y son puro dato verificable contra
// la especificacion. Hoy ademas tiene sus dos usuarios, `ObjectOutputStream` y `ObjectInputStream`.
//
// Un stream serializado empieza siempre con `STREAM_MAGIC` y `STREAM_VERSION`: los cuatro bytes
// `AC ED 00 05`. Si un archivo no arranca asi, no es un stream de serializacion de Java.
//
// Nada falta aca. `ObjectOutputStream` y `ObjectInputStream` **ya estan**, y producen y leen los
// mismos bytes que el JDK: la prueba es `java/IoTest.java`, que le da al lector los flujos que
// escribio el JDK real y compara el resultado, en las dos VM.
public interface ObjectStreamConstants {

    // -------------------------------------------------------------------------------------------
    // La cabecera
    // -------------------------------------------------------------------------------------------

    /** Los dos primeros bytes de todo stream serializado: `0xACED`, como `short` con signo. */
    short STREAM_MAGIC = (short) 0xaced;

    /** La version del formato. Vale 5 desde JDK 1.2 y no cambio desde entonces. */
    short STREAM_VERSION = 5;

    // -------------------------------------------------------------------------------------------
    // Los codigos de tipo: que viene a continuacion en el stream
    // -------------------------------------------------------------------------------------------

    /** El primero de los codigos. Es un piso, no un codigo: `TC_NULL` vale lo mismo. */
    byte TC_BASE = 0x70;

    /** Una referencia nula. */
    byte TC_NULL = (byte) 0x70;

    /** Una referencia a un objeto que ya salio en el stream, por su handle. */
    byte TC_REFERENCE = (byte) 0x71;

    /** La descripcion de una clase: nombre, serialVersionUID, banderas y campos. */
    byte TC_CLASSDESC = (byte) 0x72;

    /** Un objeto nuevo. */
    byte TC_OBJECT = (byte) 0x73;

    /** Un `String` de hasta 65535 bytes en UTF modificado. */
    byte TC_STRING = (byte) 0x74;

    /** Un arreglo. */
    byte TC_ARRAY = (byte) 0x75;

    /** Un `Class`. */
    byte TC_CLASS = (byte) 0x76;

    /** Un bloque de datos primitivos de hasta 255 bytes, con el largo en un byte. */
    byte TC_BLOCKDATA = (byte) 0x77;

    /** El final de los datos de un objeto escritos por su propio `writeObject`. */
    byte TC_ENDBLOCKDATA = (byte) 0x78;

    /** Borra la tabla de handles: lo que ya salio vuelve a escribirse entero. */
    byte TC_RESET = (byte) 0x79;

    /** Un bloque de datos primitivos con el largo en cuatro bytes, para los que no entran en uno. */
    byte TC_BLOCKDATALONG = (byte) 0x7A;

    /** Una excepcion ocurrida mientras se escribia. */
    byte TC_EXCEPTION = (byte) 0x7B;

    /** Un `String` de mas de 65535 bytes, con el largo en ocho bytes. */
    byte TC_LONGSTRING = (byte) 0x7C;

    /** La descripcion de una clase proxy dinamica. */
    byte TC_PROXYCLASSDESC = (byte) 0x7D;

    /** Una constante de enum: se serializa por nombre, no por campos. */
    byte TC_ENUM = (byte) 0x7E;

    /** El ultimo de los codigos. Es un techo, no un codigo. */
    byte TC_MAX = (byte) 0x7E;

    // -------------------------------------------------------------------------------------------
    // Los handles
    // -------------------------------------------------------------------------------------------

    /**
     * El primer handle que se reparte. Cada objeto, string o descriptor de clase que sale por
     * primera vez se queda con el siguiente numero, y las apariciones posteriores se escriben como
     * `TC_REFERENCE` mas ese numero.
     *
     * <p>Ese es el mecanismo que hace que la serializacion preserve <b>la forma del grafo</b> y no
     * solo los valores: dos campos que apuntan al mismo objeto siguen apuntando al mismo objeto
     * despues de deserializar, y un ciclo no cuelga al escritor.
     */
    int baseWireHandle = 0x7E0000;

    // -------------------------------------------------------------------------------------------
    // Las banderas de un descriptor de clase
    // -------------------------------------------------------------------------------------------

    /** La clase define su propio `writeObject`, asi que sus datos vienen en bloques. */
    byte SC_WRITE_METHOD = 0x01;

    /** Los datos del `Externalizable` vienen en bloques (protocolo 2). */
    byte SC_BLOCK_DATA = 0x08;

    /** La clase es `Serializable`. */
    byte SC_SERIALIZABLE = 0x02;

    /** La clase es `Externalizable`: se escribe y se lee a si misma. */
    byte SC_EXTERNALIZABLE = 0x04;

    /** La clase es un enum. */
    byte SC_ENUM = 0x10;

    // -------------------------------------------------------------------------------------------
    // Los permisos
    // -------------------------------------------------------------------------------------------

    /** Permite cambiar un objeto por otro al escribir o al leer. */
    SerializablePermission SUBSTITUTION_PERMISSION =
        new SerializablePermission("enableSubstitution");

    /** Permite subclasear los streams de objetos y cambiar como se escriben o se leen. */
    SerializablePermission SUBCLASS_IMPLEMENTATION_PERMISSION =
        new SerializablePermission("enableSubclassImplementation");

    /** Permite poner el filtro de deserializacion de toda la VM. */
    SerializablePermission SERIAL_FILTER_PERMISSION =
        new SerializablePermission("serialFilter");

    // -------------------------------------------------------------------------------------------
    // Las versiones del protocolo
    // -------------------------------------------------------------------------------------------

    /**
     * El protocolo de JDK 1.1. Los datos de un `Externalizable` van sin delimitar, asi que un
     * lector que no conozca la clase no puede saltearlos.
     */
    int PROTOCOL_VERSION_1 = 1;

    /**
     * El protocolo de JDK 1.2 en adelante, que es el que se usa. Los datos de un `Externalizable`
     * van en bloques con largo, asi que se pueden saltear sin entenderlos -- que es lo que permite
     * leer un stream con clases que no se tienen.
     */
    int PROTOCOL_VERSION_2 = 2;
}
