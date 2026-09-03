package java.lang.foreign;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

// La fabrica de layouts y el recorredor de caminos. De paquete: es el lugar donde viven las
// validaciones compartidas, para que las quince implementaciones no las repitan cada una a su
// manera -- que es como terminan divergiendo.
final class Layouts {

    private Layouts() {
    }

    // ---- validaciones compartidas ----------------------------------------------------------------

    static String exigirNombre(String nombre) {
        if (nombre == null) {
            throw new IllegalArgumentException("el nombre no puede ser null");
        }
        return nombre;
    }

    // Un alineamiento tiene que ser una potencia de dos positiva. La comprobacion es el truco de
    // siempre: un numero es potencia de dos si tiene exactamente un bit encendido, y `n & (n-1)`
    // apaga el bit mas bajo.
    static long exigirAlineamiento(long alineamiento) {
        if (alineamiento <= 0L || (alineamiento & (alineamiento - 1L)) != 0L) {
            throw new IllegalArgumentException(
                    "el alineamiento tiene que ser una potencia de dos positiva: " + alineamiento);
        }
        return alineamiento;
    }

    static ByteOrder exigirOrden(ByteOrder orden) {
        if (orden == null) {
            throw new IllegalArgumentException("el orden no puede ser null");
        }
        return orden;
    }

    static void completarNombre(StringBuilder sb, String nombre) {
        if (nombre != null) {
            sb.append('(');
            sb.append(nombre);
            sb.append(')');
        }
    }

    // ---- las constantes de ValueLayout ------------------------------------------------------------
    //
    // El orden por defecto es little-endian **fijo**, y no el de la maquina. El JDK usa
    // `ByteOrder.nativeOrder()`; aca no hay forma de preguntarselo a la VM, y elegir mal seria peor
    // que elegir fijo -- todas las plataformas donde esto corre hoy son little-endian, y el que
    // necesite la otra lo dice con `withOrder`.

    static ValueLayout.OfBoolean booleano() {
        return new ValorBoolean(1L, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfByte deByte() {
        return new ValorByte(1L, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfChar deChar(long alineamiento) {
        return new ValorChar(alineamiento, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfShort deShort(long alineamiento) {
        return new ValorShort(alineamiento, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfInt deInt(long alineamiento) {
        return new ValorInt(alineamiento, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfLong deLong(long alineamiento) {
        return new ValorLong(alineamiento, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfFloat deFloat(long alineamiento) {
        return new ValorFloat(alineamiento, null, ByteOrder.LITTLE_ENDIAN);
    }

    static ValueLayout.OfDouble deDouble(long alineamiento) {
        return new ValorDouble(alineamiento, null, ByteOrder.LITTLE_ENDIAN);
    }

    static AddressLayout direccion(long alineamiento) {
        return new Direccion(alineamiento, null, ByteOrder.LITTLE_ENDIAN, null);
    }

    // ---- las cuatro fabricas compuestas -----------------------------------------------------------

    static PaddingLayout padding(long tamanio) {
        if (tamanio <= 0L) {
            throw new IllegalArgumentException("el relleno tiene que ocupar algo: " + tamanio);
        }
        return new Relleno(tamanio, 1L, null);
    }

    static SequenceLayout sequence(long cantidad, MemoryLayout elemento) {
        if (elemento == null) {
            throw new IllegalArgumentException("el elemento no puede ser null");
        }
        if (cantidad < 0L) {
            throw new IllegalArgumentException("cantidad negativa: " + cantidad);
        }
        long tam = elemento.byteSize();
        // El desborde se comprueba **antes** de construir: una secuencia cuyo tamanio no entra en un
        // `long` no es un layout invalido que se descubre despues, es uno que no existe.
        if (tam != 0L && cantidad > Long.MAX_VALUE / tam) {
            throw new IllegalArgumentException("el tamanio total no entra en un long");
        }
        return new Secuencia(cantidad, elemento, elemento.byteAlignment(), null);
    }

    static StructLayout struct(MemoryLayout... elementos) {
        List<MemoryLayout> ms = enLista(elementos);
        long alineamiento = 1L;
        long offset = 0L;
        int i = 0;
        while (i < ms.size()) {
            MemoryLayout m = ms.get(i);
            long a = m.byteAlignment();
            // La regla del struct, y la que mas sorprende: cada miembro tiene que caer en un offset
            // multiplo de **su** alineamiento. No se acomoda solo -- el relleno va explicito, y sin
            // el la construccion falla. Ver la nota de `StructLayout`.
            if (offset % a != 0L) {
                throw new IllegalArgumentException(
                        "el miembro " + m + " cae en el offset " + offset
                                + ", que no es multiplo de su alineamiento " + a
                                + "; agregue MemoryLayout.paddingLayout(" + (a - offset % a) + ")");
            }
            if (a > alineamiento) {
                alineamiento = a;
            }
            offset = offset + m.byteSize();
            i = i + 1;
        }
        return new Estructura(ms, alineamiento, null);
    }

    static UnionLayout union(MemoryLayout... elementos) {
        List<MemoryLayout> ms = enLista(elementos);
        long alineamiento = 1L;
        int i = 0;
        while (i < ms.size()) {
            long a = ms.get(i).byteAlignment();
            if (a > alineamiento) {
                alineamiento = a;
            }
            i = i + 1;
        }
        // No hay regla de offsets que comprobar: todos empiezan en cero, y con el alineamiento
        // maximo todos caen bien por construccion.
        return new Union(ms, alineamiento, null);
    }

    private static List<MemoryLayout> enLista(MemoryLayout[] elementos) {
        if (elementos == null) {
            throw new IllegalArgumentException("los miembros no pueden ser null");
        }
        List<MemoryLayout> ms = new ArrayList<MemoryLayout>();
        int i = 0;
        while (i < elementos.length) {
            if (elementos[i] == null) {
                throw new IllegalArgumentException("un miembro es null");
            }
            ms.add(elementos[i]);
            i = i + 1;
        }
        return ms;
    }

    // ---- los pasos de un camino --------------------------------------------------------------------

    static final int POR_NOMBRE = 0;
    static final int POR_POSICION = 1;
    static final int ELEMENTO = 2;
    static final int TODOS = 3;
    static final int RANGO = 4;
    static final int DEREFERENCIA = 5;

    static MemoryLayout.PathElement porNombre(String nombre) {
        return new Paso(POR_NOMBRE, exigirNombre(nombre), 0L, 0L);
    }

    static MemoryLayout.PathElement porPosicion(long indice) {
        return new Paso(POR_POSICION, null, indice, 0L);
    }

    static MemoryLayout.PathElement elemento(long indice) {
        return new Paso(ELEMENTO, null, indice, 0L);
    }

    static MemoryLayout.PathElement todosLosElementos() {
        return new Paso(TODOS, null, 0L, 0L);
    }

    static MemoryLayout.PathElement elementos(long desde, long paso) {
        return new Paso(RANGO, null, desde, paso);
    }

    static MemoryLayout.PathElement dereferencia() {
        return new Paso(DEREFERENCIA, null, 0L, 0L);
    }

    // ---- recorrer un camino ------------------------------------------------------------------------
    //
    // Los dos recorridos --el que suma offsets y el que devuelve el layout-- comparten el mismo
    // paseo y difieren solo en que se llevan. Se escriben juntos para que no se puedan desincronizar:
    // un `select` que baje distinto de como baja `byteOffset` daria un layout que no esta donde el
    // offset dice.

    static long offsetPorCamino(MemoryLayout raiz, MemoryLayout.PathElement... camino) {
        return recorrer(raiz, camino, true).offset;
    }

    static MemoryLayout seleccionarPorCamino(MemoryLayout raiz, MemoryLayout.PathElement... camino) {
        return recorrer(raiz, camino, false).layout;
    }

    /**
     * El {@link java.lang.invoke.VarHandle} de ese camino.
     *
     * <p>Es el tercer recorrido, y el unico que acepta pasos **abiertos**: donde `byteOffset` se
     * planta --un paso sobre todos los elementos no designa una posicion-- este anota cuanto mide el
     * elemento y sigue. Ese numero es el **paso** con el que despues se multiplica el indice que el
     * llamador da al acceder, y es exactamente para lo que el paso abierto existe.
     *
     * @param pasoDeArreglo un paso extra al **principio**, para `arrayElementVarHandle`; cero si no
     */
    static java.lang.invoke.VarHandle handleDeCamino(MemoryLayout raiz, long pasoDeArreglo,
            MemoryLayout.PathElement... camino) {
        java.util.ArrayList<Long> abiertos = new java.util.ArrayList<Long>();
        if (pasoDeArreglo > 0L) {
            abiertos.add(Long.valueOf(pasoDeArreglo));
        }
        Parada fin = recorrer(raiz, camino, true, abiertos);
        long[] pasos = new long[abiertos.size()];
        int i = 0;
        while (i < pasos.length) {
            pasos[i] = abiertos.get(i).longValue();
            i = i + 1;
        }
        return java.lang.invoke.VarHandles.deSegmento(fin.layout, fin.offset, pasos);
    }

    /** Los tres `MethodHandle` de `MemoryLayout`, que comparten el mismo recorrido. */
    static java.lang.invoke.MethodHandle handleDeOffset(MemoryLayout raiz,
            MemoryLayout.PathElement... camino) {
        java.util.ArrayList<Long> abiertos = new java.util.ArrayList<Long>();
        Parada fin = recorrer(raiz, camino, true, abiertos);
        return java.lang.invoke.VarHandles.offsetDeCamino(raiz, fin.layout, fin.offset,
                aLargos(abiertos));
    }

    static java.lang.invoke.MethodHandle handleDeRebanada(MemoryLayout raiz,
            MemoryLayout.PathElement... camino) {
        java.util.ArrayList<Long> abiertos = new java.util.ArrayList<Long>();
        Parada fin = recorrer(raiz, camino, true, abiertos);
        return java.lang.invoke.VarHandles.rebanadaDeCamino(raiz, fin.layout, fin.offset,
                aLargos(abiertos));
    }

    private static long[] aLargos(java.util.List<Long> xs) {
        long[] out = new long[xs.size()];
        int i = 0;
        while (i < out.length) {
            out[i] = xs.get(i).longValue();
            i = i + 1;
        }
        return out;
    }

    private static Parada recorrer(MemoryLayout raiz, MemoryLayout.PathElement[] camino,
            boolean pidiendoOffset) {
        return recorrer(raiz, camino, pidiendoOffset, null);
    }

    // `abiertos` no nulo = se esta armando un `VarHandle`, y entonces un paso sobre todos los
    // elementos no es un error sino un indice libre: se anota su tamano y se baja.
    private static Parada recorrer(MemoryLayout raiz, MemoryLayout.PathElement[] camino,
            boolean pidiendoOffset, java.util.List<Long> abiertos) {
        if (camino == null) {
            throw new IllegalArgumentException("el camino no puede ser null");
        }
        MemoryLayout actual = raiz;
        long offset = 0L;
        int i = 0;
        while (i < camino.length) {
            if (!(camino[i] instanceof Paso)) {
                throw new IllegalArgumentException("paso de camino desconocido: " + camino[i]);
            }
            Paso p = (Paso) camino[i];
            if (p.clase == POR_NOMBRE || p.clase == POR_POSICION) {
                if (!(actual instanceof GroupLayout)) {
                    throw new IllegalArgumentException(
                            "no es un grupo, no tiene miembros: " + actual);
                }
                GroupLayout g = (GroupLayout) actual;
                List<MemoryLayout> ms = g.memberLayouts();
                int pos = p.clase == POR_NOMBRE ? buscarPorNombre(ms, p.nombre) : (int) p.indice;
                if (pos < 0 || pos >= ms.size()) {
                    throw new IllegalArgumentException(
                            "no hay miembro " + (p.nombre != null ? p.nombre : String.valueOf(p.indice))
                                    + " en " + actual);
                }
                // En un struct el offset es la suma de los anteriores; en una union todos empiezan
                // en cero, que es lo que la union **es**.
                if (g instanceof StructLayout) {
                    int k = 0;
                    while (k < pos) {
                        offset = offset + ms.get(k).byteSize();
                        k = k + 1;
                    }
                }
                actual = ms.get(pos);
            } else if (p.clase == ELEMENTO) {
                SequenceLayout s = comoSecuencia(actual);
                if (!pidiendoOffset) {
                    // `select` **rechaza** un indice, y la razon es buena: el indice no cambia el
                    // layout que hay ahi --todos los elementos son iguales-- asi que aceptarlo
                    // sugeriria que si. Para preguntar que hay, el paso abierto.
                    throw new IllegalArgumentException(
                            "select no acepta un elemento indexado; use sequenceElement()");
                }
                if (p.indice < 0L || p.indice >= s.elementCount()) {
                    throw new IndexOutOfBoundsException("elemento " + p.indice + " de " + actual);
                }
                offset = offset + p.indice * s.elementLayout().byteSize();
                actual = s.elementLayout();
            } else if (p.clase == RANGO) {
                SequenceLayout s = comoSecuencia(actual);
                // Un rango no designa ni una posicion ni un layout distinto: se rechaza en los dos.
                throw new IllegalArgumentException(
                        "un rango de elementos no designa ni un offset ni un layout propio");
            } else if (p.clase == TODOS) {
                SequenceLayout s = comoSecuencia(actual);
                if (abiertos != null) {
                    abiertos.add(Long.valueOf(s.elementLayout().byteSize()));
                    actual = s.elementLayout();
                    i = i + 1;
                    continue;
                }
                if (pidiendoOffset) {
                    // Un paso que abre **todos** los elementos no designa una posicion, y por lo
                    // tanto no tiene un offset. En el JDK ese paso existe para construir un
                    // `VarHandle` con un indice libre; aca esos no estan (ver `MemoryLayout`), asi
                    // que el unico uso que le queda es `select`.
                    throw new IllegalArgumentException(
                            "un paso sobre todos los elementos no designa un offset; use"
                                    + " sequenceElement(indice)");
                }
                actual = s.elementLayout();
            } else {
                // DEREFERENCIA. Se rechaza en los dos recorridos, y por la misma razon: seguir un
                // puntero **sale** de este layout, asi que ni el offset se mide desde aca ni el
                // layout de destino es una parte de este.
                //
                // En el JDK el paso existe igual, pero solo para los metodos que fabrican un
                // `VarHandle` --que ahi si pueden dereferenciar al acceder--. Esos no estan en esta
                // biblioteca (ver la nota de `MemoryLayout`), asi que el paso no tiene ningun uso
                // valido y se lo dice de frente en vez de dejar que falle mas adelante.
                if (!(actual instanceof AddressLayout)) {
                    throw new IllegalArgumentException("no es una direccion: " + actual);
                }
                throw new IllegalArgumentException(
                        "un paso de dereferencia solo vale para los metodos que fabrican un"
                                + " VarHandle, que esta biblioteca no trae");
            }
            i = i + 1;
        }
        return new Parada(offset, actual);
    }

    private static SequenceLayout comoSecuencia(MemoryLayout l) {
        if (!(l instanceof SequenceLayout)) {
            throw new IllegalArgumentException("no es una secuencia: " + l);
        }
        return (SequenceLayout) l;
    }

    private static int buscarPorNombre(List<MemoryLayout> ms, String nombre) {
        int i = 0;
        while (i < ms.size()) {
            if (ms.get(i).name().isPresent() && ms.get(i).name().get().equals(nombre)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    static long escalar(MemoryLayout layout, long offset, long index) {
        if (offset < 0L) {
            throw new IllegalArgumentException("offset negativo: " + offset);
        }
        if (index < 0L) {
            throw new IllegalArgumentException("indice negativo: " + index);
        }
        return offset + index * layout.byteSize();
    }
}

// Un paso del camino. Se guarda la clase como `int` y no como enum porque las seis variantes no
// tienen comportamiento propio: el que decide que hacer es el recorredor.
final class Paso implements MemoryLayout.PathElement {

    final int clase;
    final String nombre;
    final long indice;
    final long salto;

    Paso(int clase, String nombre, long indice, long salto) {
        this.clase = clase;
        this.nombre = nombre;
        this.indice = indice;
        this.salto = salto;
    }

    public String toString() {
        if (this.clase == Layouts.POR_NOMBRE) {
            return "groupElement(" + this.nombre + ")";
        }
        if (this.clase == Layouts.POR_POSICION) {
            return "groupElement(" + this.indice + ")";
        }
        if (this.clase == Layouts.ELEMENTO) {
            return "sequenceElement(" + this.indice + ")";
        }
        if (this.clase == Layouts.TODOS) {
            return "sequenceElement()";
        }
        if (this.clase == Layouts.RANGO) {
            return "sequenceElement(" + this.indice + ", " + this.salto + ")";
        }
        return "dereferenceElement()";
    }
}

// Donde quedo un recorrido: el offset acumulado y el layout alcanzado.
final class Parada {

    final long offset;
    final MemoryLayout layout;

    Parada(long offset, MemoryLayout layout) {
        this.offset = offset;
        this.layout = layout;
    }
}
