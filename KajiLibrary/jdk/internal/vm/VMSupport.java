package jdk.internal.vm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * KajiLibrary's jdk.internal.vm.VMSupport — el puente por el que la VM le pasa cosas a Java.
 *
 * <p>Junta tres trabajos que no se parecen entre sí y que están acá por la misma razón: los tres
 * cruzan la frontera entre el runtime y la biblioteca. Serializar propiedades para que un agente las
 * lea desde otro proceso, traducir excepciones que nacieron del lado del compilador JIT, y codificar
 * anotaciones para el mismo.
 *
 * <h2>Lo que queda afuera, y por qué</h2>
 *
 * <p>Cuatro de los diez miembros del JDK no están. No es la misma razón para los cuatro, y conviene
 * separarlas porque no todas envejecen igual.
 *
 * <ul>
 * <li><strong>{@code encodeThrowable(Throwable, long, int)}</strong> y
 *     <strong>{@code decodeAndThrowThrowable(int, long, boolean, boolean)}</strong> — el {@code long}
 *     es una **dirección de memoria cruda** donde escribir o de donde leer los bytes. Esta VM no
 *     tiene memoria direccionable desde Java; el buffer no existe y no hay nada que apuntar.</li>
 * <li><strong>{@code encodeAnnotations(Collection)}</strong> — hay que leerle los miembros a cada
 *     anotación. El JDK no los lee por reflexión: usa
 *     {@code sun.reflect.annotation.AnnotationSupport.memberValues(a)}, que castea
 *     {@code Proxy.getInvocationHandler(a)} a {@code AnnotationInvocationHandler} y le pide el mapa
 *     ya armado. **Acá ese camino no existe**: una anotación de esta VM no es un `Proxy` sino una
 *     clase sintética que fabrica el compilador (`Marcada$$Anno$0`; ablación en
 *     `scratchpad/zz350/A3.java`), así que no hay `InvocationHandler` de dónde sacar el mapa. El
 *     único camino portable que queda es invocar los métodos miembro sobre la instancia, y eso
 *     **voltea la VM** (`index out of bounds` en el intérprete). La ablación separa los pasos:
 *     `getAnnotations()` y `annotationType()` andan (`A1`), `getDeclaredMethods()` sobre el tipo anda
 *     y devuelve el miembro (`A2`), y el que mata es `Method.invoke` **sobre la instancia de la
 *     anotación** (`A4`).</li>
 * <li><strong>{@code encodeAnnotations(byte[], Class, ConstantPool, boolean, Class[])}</strong> — su
 *     tipo de parámetro {@link jdk.internal.reflect.ConstantPool} **ya está en esta biblioteca**, así
 *     que ése dejó de ser el motivo. Quedan tres, y alcanza con cualquiera: el cuerpo del JDK es
 *     {@code AnnotationParser.parseSelectAnnotations(raw, cp, ...)}, y `sun.reflect.annotation` no
 *     está acá; ese parseo necesita un `ConstantPool` **con datos**, y el nuestro no puede tener
 *     ninguno porque la VM no expone su pool (está dicho en esa clase); y termina delegando en
 *     {@code encodeAnnotations(Collection)}, que es el bloqueo de arriba. Escribir `ConstantPool` era
 *     necesario para poder siquiera nombrar la firma, pero está lejos de ser suficiente.</li>
 * </ul>
 *
 * <p>{@link #decodeAnnotations(byte[], AnnotationDecoder)} **sí está, y completo**. Antes figuraba
 * como bloqueado, con la razón de `encodeAnnotations(Collection)` copiada encima; era falsa.
 * Decodificar no toca ninguna anotación ni ninguna clase: lee bytes y le entrega lo que encuentra al
 * {@link AnnotationDecoder} que le pasan, que es quien decide con qué representarlo. No necesita
 * reflexión, ni el pool de constantes, ni memoria nativa — sólo un `DataInputStream`.
 *
 * <p>La interfaz anidada {@link AnnotationDecoder} es una declaración pura y su contrato no depende
 * de que haya quien la use.
 */
public class VMSupport {

    // Las propiedades del agente son un mapa aparte del de sistema: las escribe quien se enchufa al
    // proceso, no el proceso. Vacio y no `null`, porque "no hay agente" es un estado y no la falta de
    // respuesta -- el que llama itera el resultado sin preguntar.
    private static final Properties AGENTE = new Properties();

    public VMSupport() {
    }

    /**
     * Las propiedades que dejó un agente enchufado al proceso.
     *
     * <p>Vacías: esta VM no acepta agentes. Es `synchronized` como en el JDK porque el mapa lo puede
     * escribir un hilo de afuera mientras otro lo lee.
     */
    public static synchronized Properties getAgentProperties() {
        return VMSupport.AGENTE;
    }

    /**
     * Las propiedades de sistema, serializadas.
     *
     * <p>El formato es el que el JDK usa acá: pares `clave=valor` separados por saltos de línea, en
     * UTF-8. Es deliberadamente tonto porque del otro lado lo lee código que puede estar corriendo en
     * otro proceso y que no va a deserializar objetos de Java.
     *
     * @throws IOException si falla al armar el arreglo
     */
    public static byte[] serializePropertiesToByteArray() throws IOException {
        return VMSupport.serializar(System.getProperties());
    }

    /** Las del agente, en el mismo formato. */
    public static byte[] serializeAgentPropertiesToByteArray() throws IOException {
        return VMSupport.serializar(VMSupport.getAgentProperties());
    }

    /**
     * El directorio temporal de la VM.
     *
     * <p><strong>Acá no es `native`, y el JDK sí lo declara así.</strong> Es la única divergencia de
     * modificador del archivo y conviene justificarla: en esta VM, un método `native` sin
     * implementación registrada no tira una excepción — **voltea el proceso**. Así que declararlo
     * `native` para respetar el modificador daría un miembro que mata al programa que lo llame, y
     * escribirlo en Java da uno que contesta lo mismo que el del JDK. Entre respetar una palabra y
     * respetar el comportamiento, gana el comportamiento.
     *
     * <p>La respuesta sale de `java.io.tmpdir`, que es de donde el nativo del JDK la saca también.
     *
     * <p><strong>Hoy devuelve `null`</strong>, porque esta VM no define esa propiedad ni expone
     * variables de entorno (`System.getenv("TMP")` también da `null`). Se deja así, y no se inventa
     * una ruta: un directorio que se nombra y no existe es peor que la ausencia de respuesta —el que
     * llama descubre el problema recién al escribir, y con un error que no señala la causa. El día
     * que la VM defina `java.io.tmpdir`, este método empieza a devolverlo sin tocar nada.
     */
    public static String getVMTemporaryDirectory() {
        return System.getProperty("java.io.tmpdir");
    }

    private static byte[] serializar(Properties props) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Map.Entry<Object, Object> e : props.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            if (k == null || v == null) {
                continue;
            }
            out.write(String.valueOf(k).getBytes(StandardCharsets.UTF_8));
            out.write('=');
            out.write(String.valueOf(v).getBytes(StandardCharsets.UTF_8));
            out.write('\n');
        }
        return out.toByteArray();
    }

    /**
     * Cómo reconstruir una anotación decodificada.
     *
     * <p>Las cuatro variables de tipo son los cuatro mundos que el que decodifica elige: {@code T} el
     * tipo, {@code A} la anotación armada, {@code E} un valor de enum, {@code X} un error. La
     * interfaz no fabrica nada — le dice al decodificador *qué* encontró y deja que él decida con qué
     * representarlo. Eso es lo que permite que el mismo decodificador sirva para armar anotaciones de
     * verdad o para armar una descripción de ellas sin cargar sus clases.
     *
     * <p>{@link #newErrorValue} es la parte que suele sorprender: un valor que no se puede resolver
     * **no es una excepción**, es un valor más. Una anotación que menciona una clase que ya no está
     * tiene que poder describirse igual, con el error adentro, en vez de hacer fallar la lectura
     * entera.
     */
    public interface AnnotationDecoder<T, A, E, X> {

        /** El tipo que corresponde a ese descriptor. */
        T resolveType(String name);

        /** Una anotación de ese tipo con esos miembros. */
        A newAnnotation(T type, Map.Entry<String, Object>[] elements);

        /** Un valor de enum de ese tipo y ese nombre. */
        E newEnumValue(T enumType, String name);

        /** Un valor que no se pudo resolver, con el motivo. */
        X newErrorValue(String description);
    }

    /**
     * Reconstruye las anotaciones que {@code encodeAnnotations} serializó.
     *
     * <p>El formato es el del JDK y no uno nuestro, porque del otro lado puede haber un compilador
     * JIT escrito aparte: un entero de largo, y después esa cantidad de anotaciones. Cada anotación
     * es el nombre binario de su tipo, otro largo, y esa cantidad de pares nombre/valor donde el
     * valor arranca con un byte de etiqueta --las de {@code JVM_SIGNATURE} para los primitivos,
     * {@code 's'} texto, {@code 'c'} clase, {@code 'e'} constante de enum, {@code '@'} anotación
     * anidada, {@code '['} arreglo y {@code 'x'} un valor que no se pudo resolver--.
     *
     * <p>El largo va **en uno o en cuatro bytes**: si entra en siete bits se escribe uno solo con el
     * bit alto prendido, y si no, un `int` de cuatro. Por eso al leer se mira el signo del primer
     * byte, que es la marca de cuál de las dos formas vino. Una anotación típica tiene dos o tres
     * miembros, así que el caso corto es el que pasa siempre.
     *
     * <p>Este método **no toca ninguna anotación ni carga ninguna clase**: cada tipo que aparece se
     * lo pasa a {@link AnnotationDecoder#resolveType} y cada valor raro a
     * {@link AnnotationDecoder#newErrorValue}, así que el que llama puede leer anotaciones que
     * mencionan clases que no están sin que la lectura se caiga. Es lo que lo hace implementable acá
     * y lo que lo separa de {@code encodeAnnotations}.
     *
     * @return una lista inmutable con lo que el decodificador fabricó, en el orden en que vinieron
     */
    @SuppressWarnings("unchecked")
    public static <T, A, E, X> List<A> decodeAnnotations(byte[] encoded,
                                                         AnnotationDecoder<T, A, E, X> decoder) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(encoded));
            int n = VMSupport.leerLargo(dis);
            Object[] out = new Object[n];
            for (int i = 0; i < n; i++) {
                out[i] = VMSupport.leerAnotacion(dis, decoder);
            }
            return (List<A>) List.of(out);
        } catch (Exception e) {
            // Como en el JDK: un arreglo mal formado es un error del que lo produjo, no una
            // condicion que el que llama pueda manejar.
            throw new InternalError(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, A, E, X> A leerAnotacion(DataInputStream dis,
                                                AnnotationDecoder<T, A, E, X> decoder)
            throws IOException {
        T tipo = decoder.resolveType(dis.readUTF());
        int n = VMSupport.leerLargo(dis);
        Map.Entry[] miembros = new Map.Entry[n];
        for (int i = 0; i < n; i++) {
            String nombre = dis.readUTF();
            byte etiqueta = dis.readByte();
            miembros[i] = Map.entry(nombre, VMSupport.leerValor(dis, decoder, etiqueta));
        }
        return decoder.newAnnotation(tipo, (Map.Entry<String, Object>[]) miembros);
    }

    private static <T, A, E, X> Object leerValor(DataInputStream dis,
                                                 AnnotationDecoder<T, A, E, X> decoder,
                                                 byte etiqueta) throws IOException {
        switch (etiqueta) {
            case 'B': return Byte.valueOf(dis.readByte());
            case 'C': return Character.valueOf(dis.readChar());
            case 'D': return Double.valueOf(dis.readDouble());
            case 'F': return Float.valueOf(dis.readFloat());
            case 'I': return Integer.valueOf(dis.readInt());
            case 'J': return Long.valueOf(dis.readLong());
            case 'S': return Short.valueOf(dis.readShort());
            case 'Z': return Boolean.valueOf(dis.readBoolean());
            case 's': return dis.readUTF();
            case 'c': return decoder.resolveType(dis.readUTF());
            case 'e': {
                // En dos pasos y no anidado: el orden de las dos lecturas es parte del formato, y
                // dejarlo a la evaluacion de argumentos lo esconde.
                T tipoEnum = decoder.resolveType(dis.readUTF());
                return decoder.newEnumValue(tipoEnum, dis.readUTF());
            }
            case '@': return VMSupport.leerAnotacion(dis, decoder);
            case '[': return VMSupport.leerArreglo(dis, decoder);
            case 'x': return decoder.newErrorValue(dis.readUTF());
            default: throw new InternalError("etiqueta no soportada: " + etiqueta);
        }
    }

    // Los arreglos vuelven como `List` inmutable y no como arreglo del tipo componente: el que
    // decodifica eligio con que representar cada valor, asi que el tipo del elemento es suyo y no
    // nuestro, y no hay arreglo concreto que podamos fabricar sin adivinarlo.
    private static <T, A, E, X> Object leerArreglo(DataInputStream dis,
                                                   AnnotationDecoder<T, A, E, X> decoder)
            throws IOException {
        byte comp = dis.readByte();
        // El enum es el unico que trae su tipo ANTES del largo, porque es uno solo para todo el
        // arreglo. Por eso se lee aca y no adentro del bucle.
        T tipoEnum = comp == 'e' ? decoder.resolveType(dis.readUTF()) : null;
        int n = VMSupport.leerLargo(dis);
        Object[] out = new Object[n];
        for (int i = 0; i < n; i++) {
            switch (comp) {
                case 'B': out[i] = Byte.valueOf(dis.readByte()); break;
                case 'C': out[i] = Character.valueOf(dis.readChar()); break;
                case 'D': out[i] = Double.valueOf(dis.readDouble()); break;
                case 'F': out[i] = Float.valueOf(dis.readFloat()); break;
                case 'I': out[i] = Integer.valueOf(dis.readInt()); break;
                case 'J': out[i] = Long.valueOf(dis.readLong()); break;
                case 'S': out[i] = Short.valueOf(dis.readShort()); break;
                case 'Z': out[i] = Boolean.valueOf(dis.readBoolean()); break;
                case 's': out[i] = dis.readUTF(); break;
                case 'c': out[i] = decoder.resolveType(dis.readUTF()); break;
                case 'e': out[i] = decoder.newEnumValue(tipoEnum, dis.readUTF()); break;
                case '@': out[i] = VMSupport.leerAnotacion(dis, decoder); break;
                default: throw new InternalError("etiqueta de componente no soportada: " + comp);
            }
        }
        return List.of(out);
    }

    // El largo viene en un byte con el bit alto prendido si entra en siete bits, y si no en cuatro.
    // El primer byte leido con signo es negativo exactamente en el caso corto, y eso es lo que
    // distingue las dos formas.
    //
    // Los tres bytes de la forma larga se leen con `readUnsignedByte` y no con `read` como en el JDK.
    // Es la unica diferencia con el original y es a proposito: `read` devuelve -1 al llegar al final
    // en vez de fallar, asi que un arreglo truncado justo ahi daba un largo enorme armado con esos
    // -1 y recien reventaba mas adelante, lejos de la causa. `readUnsignedByte` tira `EOFException`
    // en el byte que falta. Con bytes validos las dos formas dan lo mismo.
    private static int leerLargo(DataInputStream dis) throws IOException {
        int b1 = dis.readByte();
        if (b1 < 0) {
            return b1 & 0x7F;
        }
        int b2 = dis.readUnsignedByte();
        int b3 = dis.readUnsignedByte();
        int b4 = dis.readUnsignedByte();
        return (b1 << 24) + (b2 << 16) + (b3 << 8) + b4;
    }
}
