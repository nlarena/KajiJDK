// Los layouts de `java.lang.foreign`: la mitad del paquete que **no toca memoria**.
//
// **Se comprueba contra `java` real corriendo lo mismo.** Un layout es una descripcion, y lo que se
// le pide es que describa **exactamente** lo mismo que el JDK: mismo tamanio, mismo alineamiento,
// mismos offsets, y --a proposito-- el mismo `toString`, que es lo que alguien lee cuando una
// estructura no cuadra. Si nuestro `i4` se imprimiera distinto habria que traducir mentalmente cada
// vez.
//
// Lo que mas se cuida es la **regla de alineamiento de los structs**, que es donde este API sorprende:
// un `long` detras de un `int` no forma un struct, hay que escribir el relleno. Que falle es la
// respuesta correcta, y aca se comprueba que falle.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.lang.foreign.AddressLayout;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.PaddingLayout;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.UnionLayout;
import java.lang.foreign.ValueLayout;
import java.nio.ByteOrder;

public class LayoutTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    static void es(String esperado, String actual) {
        ok(esperado.equals(actual));
    }

    static void valores() {
        ok(ValueLayout.JAVA_BOOLEAN.byteSize() == 1L);
        ok(ValueLayout.JAVA_BYTE.byteSize() == 1L);
        ok(ValueLayout.JAVA_CHAR.byteSize() == 2L);
        ok(ValueLayout.JAVA_SHORT.byteSize() == 2L);
        ok(ValueLayout.JAVA_INT.byteSize() == 4L);
        ok(ValueLayout.JAVA_LONG.byteSize() == 8L);
        ok(ValueLayout.JAVA_FLOAT.byteSize() == 4L);
        ok(ValueLayout.JAVA_DOUBLE.byteSize() == 8L);
        ok(ValueLayout.ADDRESS.byteSize() == 8L);

        // El alineamiento natural es el tamanio; la version sin alinear es 1. Esa diferencia es la
        // que decide si un `get` sobre un `byte[]` anda o tira.
        ok(ValueLayout.JAVA_INT.byteAlignment() == 4L);
        ok(ValueLayout.JAVA_INT_UNALIGNED.byteAlignment() == 1L);
        ok(ValueLayout.JAVA_LONG.byteAlignment() == 8L);
        ok(ValueLayout.JAVA_LONG_UNALIGNED.byteAlignment() == 1L);
        ok(ValueLayout.JAVA_BYTE.byteAlignment() == 1L);

        // El tipo que transporta cada uno.
        ok(ValueLayout.JAVA_INT.carrier() == Integer.TYPE);
        ok(ValueLayout.JAVA_LONG.carrier() == Long.TYPE);
        ok(ValueLayout.JAVA_BOOLEAN.carrier() == Boolean.TYPE);
        ok(ValueLayout.JAVA_DOUBLE.carrier() == Double.TYPE);

        // El `toString`, que se copia del JDK a proposito: es lo que se lee al depurar.
        es("i4", ValueLayout.JAVA_INT.toString());
        es("j8", ValueLayout.JAVA_LONG.toString());
        es("z1", ValueLayout.JAVA_BOOLEAN.toString());
        es("b1", ValueLayout.JAVA_BYTE.toString());
        es("c2", ValueLayout.JAVA_CHAR.toString());
        es("s2", ValueLayout.JAVA_SHORT.toString());
        es("f4", ValueLayout.JAVA_FLOAT.toString());
        es("d8", ValueLayout.JAVA_DOUBLE.toString());
        es("a8", ValueLayout.ADDRESS.toString());
        es("1%i4", ValueLayout.JAVA_INT_UNALIGNED.toString());
        es("i4(x)", ValueLayout.JAVA_INT.withName("x").toString());
        es("I4", ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN).toString());

        // El nombre es parte de la identidad: dos campos del mismo tipo con nombres distintos no son
        // intercambiables dentro de un struct.
        ok(!ValueLayout.JAVA_INT.equals(ValueLayout.JAVA_INT.withName("x")));
        ok(ValueLayout.JAVA_INT.equals(ValueLayout.JAVA_INT.withName("x").withoutName()));
        ok(ValueLayout.JAVA_INT.hashCode()
                == ValueLayout.JAVA_INT.withName("x").withoutName().hashCode());
        ok(!ValueLayout.JAVA_INT.equals(ValueLayout.JAVA_INT_UNALIGNED));
        ok(!ValueLayout.JAVA_INT.name().isPresent());
        ok(ValueLayout.JAVA_INT.withName("q").name().get().equals("q"));

        ok(ValueLayout.JAVA_INT.order() == ByteOrder.LITTLE_ENDIAN);
        ok(ValueLayout.JAVA_INT.withOrder(ByteOrder.BIG_ENDIAN).order() == ByteOrder.BIG_ENDIAN);

        // Encadenar sin castear: es para lo que existen los ocho subtipos.
        ValueLayout.OfInt enc = ValueLayout.JAVA_INT.withName("a").withByteAlignment(1L);
        ok(enc.byteAlignment() == 1L);
        ok(enc.name().get().equals("a"));
    }

    static void relleno() {
        PaddingLayout p = MemoryLayout.paddingLayout(3);
        ok(p.byteSize() == 3L);
        // Alineamiento 1: una restriccion sobre donde empieza la nada no restringe nada.
        ok(p.byteAlignment() == 1L);
        es("x3", p.toString());

        boolean tiro = false;
        try {
            MemoryLayout.paddingLayout(0);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok(tiro);
    }

    static void secuencias() {
        SequenceLayout s = MemoryLayout.sequenceLayout(3, ValueLayout.JAVA_INT);
        ok(s.byteSize() == 12L);
        // El alineamiento **es el del elemento**: si cada uno cae alineado, la secuencia tambien.
        ok(s.byteAlignment() == 4L);
        ok(s.elementCount() == 3L);
        ok(s.elementLayout().equals(ValueLayout.JAVA_INT));
        es("[3:i4]", s.toString());

        ok(s.withElementCount(5).byteSize() == 20L);

        // Anidar no agrega separacion, solo estructura: aplanar da la misma memoria.
        SequenceLayout anidada = MemoryLayout.sequenceLayout(2, s);
        es("[2:[3:i4]]", anidada.toString());
        es("[6:i4]", anidada.flatten().toString());
        ok(anidada.byteSize() == anidada.flatten().byteSize());

        // Y reshape es la inversa; el -1 se deduce.
        SequenceLayout doce = MemoryLayout.sequenceLayout(12, ValueLayout.JAVA_INT);
        es("[3:[4:i4]]", doce.reshape(3, -1).toString());
        es("[3:[4:i4]]", doce.reshape(3, 4).toString());
        ok(doce.reshape(3, -1).byteSize() == doce.byteSize());

        boolean tiro = false;
        try {
            doce.reshape(5, -1);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok(tiro);

        boolean tiro2 = false;
        try {
            doce.reshape(-1, -1);
        } catch (IllegalArgumentException e) {
            tiro2 = true;
        }
        ok(tiro2);

        // El offset de un elemento, y `scale`, que es la misma cuenta con otra cara.
        ok(s.byteOffset(MemoryLayout.PathElement.sequenceElement(2)) == 8L);
        ok(s.scale(0, 2) == 24L);
        ok(ValueLayout.JAVA_INT.scale(8, 3) == 20L);
        // `select` con un **indice** se rechaza: el indice no cambia el layout que hay ahi, asi que
        // aceptarlo sugeriria que si. Para preguntar que hay, el paso abierto.
        boolean tiroSel = false;
        try {
            s.select(MemoryLayout.PathElement.sequenceElement(1));
        } catch (IllegalArgumentException e) {
            tiroSel = true;
        }
        ok(tiroSel);
        ok(s.select(MemoryLayout.PathElement.sequenceElement()).equals(ValueLayout.JAVA_INT));
    }

    static void estructuras() {
        // Un `long` detras de un `int` **no forma un struct**: el long caeria en el offset 4 y se
        // alinea a 8. Que falle es la respuesta correcta -- un struct que se acomoda solo describe
        // una cosa distinta en cada plataforma.
        boolean tiro = false;
        try {
            MemoryLayout.structLayout(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok(tiro);

        // Con el relleno explicito, si.
        StructLayout st = MemoryLayout.structLayout(
                ValueLayout.JAVA_INT.withName("x"),
                MemoryLayout.paddingLayout(4),
                ValueLayout.JAVA_LONG.withName("y"));
        ok(st.byteSize() == 16L);
        ok(st.byteAlignment() == 8L);
        es("[i4(x)x4j8(y)]", st.toString());
        ok(st.memberLayouts().size() == 3);

        // El offset de un miembro por nombre, que es para lo que existen los caminos.
        ok(st.byteOffset(MemoryLayout.PathElement.groupElement("x")) == 0L);
        ok(st.byteOffset(MemoryLayout.PathElement.groupElement("y")) == 8L);
        ok(st.byteOffset(MemoryLayout.PathElement.groupElement(2)) == 8L);
        es("j8(y)", st.select(MemoryLayout.PathElement.groupElement("y")).toString());

        boolean tiro2 = false;
        try {
            st.byteOffset(MemoryLayout.PathElement.groupElement("noExiste"));
        } catch (IllegalArgumentException e) {
            tiro2 = true;
        }
        ok(tiro2);

        // Un struct de puros bytes no necesita relleno.
        StructLayout bytes = MemoryLayout.structLayout(
                ValueLayout.JAVA_BYTE, ValueLayout.JAVA_BYTE, ValueLayout.JAVA_BYTE);
        ok(bytes.byteSize() == 3L);
        ok(bytes.byteAlignment() == 1L);
    }

    static void uniones() {
        UnionLayout un = MemoryLayout.unionLayout(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG);
        // El mas grande, con el alineamiento mas estricto. Y aca **no** hay regla de offsets: todos
        // empiezan en cero, asi que todos caen bien por construccion.
        ok(un.byteSize() == 8L);
        ok(un.byteAlignment() == 8L);
        es("[i4|j8]", un.toString());
        ok(un.memberLayouts().size() == 2);

        UnionLayout nom = MemoryLayout.unionLayout(
                ValueLayout.JAVA_INT.withName("i"), ValueLayout.JAVA_FLOAT.withName("f"));
        // En una union todos los miembros arrancan en cero: eso **es** la union.
        ok(nom.byteOffset(MemoryLayout.PathElement.groupElement("i")) == 0L);
        ok(nom.byteOffset(MemoryLayout.PathElement.groupElement("f")) == 0L);
    }

    static void direcciones() {
        AddressLayout a = ValueLayout.ADDRESS;
        ok(!a.targetLayout().isPresent());
        AddressLayout conDestino = a.withTargetLayout(ValueLayout.JAVA_INT);
        ok(conDestino.targetLayout().get().equals(ValueLayout.JAVA_INT));
        es("a8:i4", conDestino.toString());
        ok(!conDestino.withoutTargetLayout().targetLayout().isPresent());

        // El paso de dereferencia se rechaza en `select` **y** en `byteOffset`: seguir un puntero
        // sale del layout en el que se esta, asi que ni el offset se mide desde aca ni el destino es
        // una parte de este. En el JDK solo vale para los metodos que fabrican un `VarHandle`.
        boolean tiro = false;
        try {
            conDestino.select(MemoryLayout.PathElement.dereferenceElement());
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok(tiro);

        boolean tiro2 = false;
        try {
            conDestino.byteOffset(MemoryLayout.PathElement.dereferenceElement());
        } catch (IllegalArgumentException e) {
            tiro2 = true;
        }
        ok(tiro2);
    }

    static void anidado() {
        // Una estructura con una secuencia adentro: el caso que hace util tener caminos en vez de
        // sumar offsets a mano.
        StructLayout st = MemoryLayout.structLayout(
                ValueLayout.JAVA_INT.withName("cabecera"),
                MemoryLayout.sequenceLayout(4, ValueLayout.JAVA_INT).withName("datos"));
        ok(st.byteSize() == 20L);
        ok(st.byteOffset(MemoryLayout.PathElement.groupElement("datos")) == 4L);
        ok(st.byteOffset(MemoryLayout.PathElement.groupElement("datos"),
                MemoryLayout.PathElement.sequenceElement(2)) == 12L);
        es("i4", st.select(MemoryLayout.PathElement.groupElement("datos"),
                MemoryLayout.PathElement.sequenceElement()).toString());

        // Un paso sobre **todos** los elementos no designa una posicion, asi que no tiene offset.
        boolean tiro = false;
        try {
            st.byteOffset(MemoryLayout.PathElement.groupElement("datos"),
                    MemoryLayout.PathElement.sequenceElement());
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok(tiro);
        // Pero si sirve para preguntar **que** hay ahi.
        es("i4", st.select(MemoryLayout.PathElement.groupElement("datos"),
                MemoryLayout.PathElement.sequenceElement()).toString());
    }

    public static int run() {
        valores();
        relleno();
        secuencias();
        estructuras();
        uniones();
        direcciones();
        anidado();
        return primerFallo;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
