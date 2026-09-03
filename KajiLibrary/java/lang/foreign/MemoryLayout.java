package java.lang.foreign;

import java.util.List;
import java.util.Optional;

/**
 * KajiLibrary's java.lang.foreign.MemoryLayout -- la **forma** de una region de memoria: cuanto
 * ocupa, como esta alineada, y --si es compuesta-- que hay adentro y en que orden.
 *
 * <p>Lo primero que conviene entender es que un layout **no es memoria**: es una descripcion. No
 * hay nada reservado, nada que liberar, nada que se pueda leer ni escribir. Un `MemoryLayout` es al
 * `MemorySegment` lo que un tipo es a un valor.
 *
 * <p>Y por eso esta mitad del paquete se puede escribir entera en Java puro, mientras que la otra
 * --el enlazador nativo-- no. Ver la nota de {@link Linker}.
 *
 * <h2>El alineamiento, que es de donde salen casi todas las sorpresas</h2>
 *
 * <p>Cada layout dice cuantos bytes ocupa **y** a que multiplo tiene que empezar. Un `int` ocupa 4 y
 * se alinea a 4; un `long` ocupa 8 y se alinea a 8. Componer no es solo sumar tamanios: un
 * {@link #structLayout} exige que cada miembro caiga en un offset multiplo de su alineamiento, y si
 * no, **falla en vez de acomodar**. Poner un `long` despues de un `int` no compila un struct: hay
 * que escribir el relleno a mano con {@link #paddingLayout}.
 *
 * <p>Eso puede parecer inflexible y es a proposito: un struct que se acomoda solo tiene un layout
 * que depende de la plataforma, y todo este paquete existe para describir memoria **de forma
 * exacta**. El relleno explicito es la unica manera de que la descripcion diga lo mismo en todos
 * lados.
 *
 * <h2>Lo que esta biblioteca no trae, y por que</h2>
 *
 * <p>Quedan afuera los cinco metodos que fabrican un `VarHandle` o un `MethodHandle`
 * --`varHandle`, `arrayElementVarHandle`, `byteOffsetHandle`, `sliceHandle`, `scaleHandle`--. No es
 * una decision de alcance: es que hoy **no hay manera de escribir el sitio de llamada** que los
 * usa. Los cinco devuelven un objeto cuyo unico uso es invocarlo, y esa invocacion no compila a
 * bytecode valido en esta cadena de herramientas. Cuatro hechos, los cuatro comprobados:
 *
 * <ol>
 *   <li><b>La VM no sabe nada de `VarHandle`.</b> `grep -rn VarHandle src/` da cero: no hay
 *       interceptacion en `invokevirtual` ni implementacion nativa. `MethodHandle` si esta
 *       interceptado, pero el intrinseco lee de la instancia los campos `owner`/`name`/
 *       `descriptor`/`kind` --los de un handle *directo*--, que
 *       {@link java.lang.invoke.MethodHandle} de esta biblioteca no tiene; y un handle directo
 *       tampoco podria llevar adentro el layout y el camino que estos cinco metodos necesitan.
 *   <li><b>Un `native` sin implementacion no tira: mata el proceso.</b> La VM hace `panic!` en
 *       `natives.rs` ("no native implementation for ..."). Los 31 accesores de `VarHandle` son
 *       `public final native`, asi que un `varHandle()` que devolviera algo entregaria un objeto
 *       que al primer `get` voltea la VM. Eso es estrictamente peor que la ausencia.
 *   <li><b>Nuestro javac no implementa el polimorfismo de firma (JLS 15.12.3).</b> Trata
 *       `vh.get(seg, 0L)` como un varargs comun: boxea en `Object[]` y emite
 *       `get:([Ljava/lang/Object;)Ljava/lang/Object;`. La JVM real rechaza ese sitio con
 *       `WrongMethodTypeException`, y al reves el descriptor que emite el javac real
 *       --`get:()Ljava/lang/Object;`-- no resuelve contra nuestra `VarHandle`. Repro en
 *       `scratchpad/sigpoly/`.
 *   <li><b>Hacerlos andar exigiria mentir en la superficie publica.</b> La unica forma de que un
 *       cuerpo Java atienda esos accesores es sacarles `native` --un modificador observable, en 31
 *       miembros publicos de `java.lang.invoke.VarHandle`-- y aun asi solo andaria compilado con
 *       nuestro javac. Un miembro que falta es un subconjunto legal; uno que anda en una sola
 *       cadena de herramientas, no.
 * </ol>
 *
 * <p>El acceso por offset --{@link MemorySegment#get(ValueLayout.OfInt, long)} y compania-- hace lo
 * mismo sin el handle, y ese si esta: `layout.byteOffset(camino)` da el offset y el `get`/`set` del
 * segmento lo lee. Lo que se pierde es poder *guardar* ese acceso en un objeto y componerlo; el
 * dia que la VM intercepte `VarHandle`, estos cinco son media hora de trabajo sobre `byteOffset`,
 * `select` y `scale`, que ya estan.
 */
public interface MemoryLayout {

    /** Cuantos bytes ocupa. */
    long byteSize();

    /** A que multiplo de bytes tiene que empezar. */
    long byteAlignment();

    /** El nombre de este layout, si se le puso uno. */
    Optional<String> name();

    /** El mismo layout con ese nombre. */
    MemoryLayout withName(String name);

    /** El mismo layout sin nombre. */
    MemoryLayout withoutName();

    /**
     * El mismo layout con otro alineamiento.
     *
     * @throws IllegalArgumentException si no es una potencia de dos positiva
     */
    MemoryLayout withByteAlignment(long byteAlignment);

    /**
     * El offset en bytes al que lleva ese camino dentro de este layout.
     *
     * <p>Un camino es una sucesion de pasos --"el miembro `y`", "el elemento 3"-- y cada paso baja
     * un nivel. Es la forma de nombrar una posicion dentro de una estructura anidada sin calcular
     * offsets a mano, que es exactamente donde se cometen los errores.
     */
    long byteOffset(PathElement... elements);

    /** El layout al que lleva ese camino. */
    MemoryLayout select(PathElement... elements);

    /**
     * Un {@link java.lang.invoke.VarHandle} que lee y escribe el valor que hay al final de ese
     * camino.
     *
     * <p>Sus coordenadas son el segmento, el desplazamiento del layout raiz dentro de el, y **un
     * `long` por cada paso abierto** del camino (los `sequenceElement()` sin indice). O sea que
     * `estructura.varHandle(groupElement("x"))` se accede con `vh.get(seg, 0L)` y
     * `secuencia.varHandle(sequenceElement(), groupElement("x"))` con `vh.get(seg, 0L, i)`.
     *
     * <p>Es lo mismo que hace `seg.get(distribucion, layout.byteOffset(camino) + desplazamiento)`,
     * con una diferencia que es toda la gracia: el acceso queda **guardado en un objeto**, se puede
     * pasar, componer y usar sin repetir el camino en cada lugar.
     */
    default java.lang.invoke.VarHandle varHandle(PathElement... elements) {
        return Layouts.handleDeCamino(this, 0L, elements);
    }

    /**
     * El de arriba para un **arreglo** de este layout: agrega un indice de elemento al principio.
     *
     * <p>Equivale a `sequenceLayout(this).varHandle(sequenceElement(), camino)`, y por eso sus
     * coordenadas llevan un `long` mas que las de {@link #varHandle}: el indice del elemento, que se
     * multiplica por {@link #byteSize()}.
     */
    default java.lang.invoke.VarHandle arrayElementVarHandle(PathElement... elements) {
        return Layouts.handleDeCamino(this, this.byteSize(), elements);
    }

    /**
     * Un {@link java.lang.invoke.MethodHandle} que calcula el desplazamiento de ese camino:
     * `(long base, long… indices) -> long`.
     *
     * <p>Es {@link #byteOffset} guardado en un objeto, con los pasos abiertos del camino como
     * argumentos. Sirve para componer -- lo que `byteOffset` no permite, porque devuelve un numero y
     * no una operacion.
     */
    default java.lang.invoke.MethodHandle byteOffsetHandle(PathElement... elements) {
        return Layouts.handleDeOffset(this, elements);
    }

    /**
     * Un handle que **recorta** el segmento al layout que hay al final del camino:
     * `(MemorySegment, long base, long… indices) -> MemorySegment`.
     *
     * <p>La rebanada mide exactamente lo que mide ese layout, que es la diferencia con calcular el
     * offset a mano y llamar a `asSlice`: el largo sale del layout y no de quien llama, asi que no se
     * puede equivocar.
     */
    default java.lang.invoke.MethodHandle sliceHandle(PathElement... elements) {
        return Layouts.handleDeRebanada(this, elements);
    }

    /**
     * Un handle que escala un indice por el tamano de este layout: `(long base, long indice) -> long`.
     *
     * <p>Es {@link #scale} guardado en un objeto, y es el bloque con el que se recorre un arreglo sin
     * repetir la multiplicacion en cada lugar.
     */
    default java.lang.invoke.MethodHandle scaleHandle() {
        return java.lang.invoke.VarHandles.escala(this);
    }

    /**
     * El offset de un elemento en un arreglo de este layout: `base + index * byteSize()`.
     *
     * @throws IllegalArgumentException si alguno es negativo
     */
    long scale(long offset, long index);

    // ---- las cuatro fabricas --------------------------------------------------------------------

    /**
     * Relleno: ocupa lugar y no lleva nada.
     *
     * <p>Su alineamiento es **1** a proposito. Un relleno con alineamiento propio impondria una
     * restriccion sobre donde puede caer la nada, que no tiene sentido.
     *
     * @throws IllegalArgumentException si `byteSize` no es positivo
     */
    static PaddingLayout paddingLayout(long byteSize) {
        return Layouts.padding(byteSize);
    }

    /**
     * Una secuencia de `elementCount` copias de `elementLayout`, una detras de otra.
     *
     * @throws IllegalArgumentException si `elementCount` es negativo, o si el tamanio total se pasa
     */
    static SequenceLayout sequenceLayout(long elementCount, MemoryLayout elementLayout) {
        return Layouts.sequence(elementCount, elementLayout);
    }

    /**
     * Los miembros **uno detras del otro**, como los campos de un `struct` de C.
     *
     * @throws IllegalArgumentException si algun miembro cae en un offset que no respeta su propio
     *     alineamiento. Ver la nota de la clase: el relleno va explicito.
     */
    static StructLayout structLayout(MemoryLayout... elements) {
        return Layouts.struct(elements);
    }

    /**
     * Los miembros **superpuestos**, todos empezando en el offset cero, como una `union` de C.
     *
     * <p>El tamanio es el del mas grande y el alineamiento el mas estricto de todos.
     */
    static UnionLayout unionLayout(MemoryLayout... elements) {
        return Layouts.union(elements);
    }

    /**
     * Un paso de un camino dentro de un layout.
     *
     * <p>Existe como tipo propio, y no como un `String` o un `int`, porque los pasos son de clases
     * distintas --por nombre, por indice, por todos los elementos-- y mezclarlos en un solo tipo
     * dejaria que se escriba un camino sin sentido.
     */
    interface PathElement {

        /** El miembro de ese nombre dentro de un grupo. */
        static PathElement groupElement(String name) {
            return Layouts.porNombre(name);
        }

        /** El miembro en esa posicion dentro de un grupo. */
        static PathElement groupElement(long index) {
            return Layouts.porPosicion(index);
        }

        /** **Todos** los elementos de una secuencia: es el paso que abre un rango, no uno solo. */
        static PathElement sequenceElement() {
            return Layouts.todosLosElementos();
        }

        /** El elemento en esa posicion de una secuencia. */
        static PathElement sequenceElement(long index) {
            return Layouts.elemento(index);
        }

        /** Los elementos de una secuencia desde `start`, de a `step`. */
        static PathElement sequenceElement(long start, long step) {
            return Layouts.elementos(start, step);
        }

        /**
         * Sigue un puntero: baja al layout al que apunta una {@link AddressLayout}.
         *
         * <p>Es el unico paso que **sale** del layout en el que se esta, y por eso solo se puede dar
         * sobre una direccion que declare a que apunta (`withTargetLayout`).
         */
        static PathElement dereferenceElement() {
            return Layouts.dereferencia();
        }
    }
}
