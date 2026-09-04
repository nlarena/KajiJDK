package java.lang.runtime;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles$Lookup;
import java.lang.invoke.TypeDescriptor;

/**
 * El bootstrap de los tres métodos de un {@code record}.
 *
 * <h2>Por qué un bootstrap y no código emitido</h2>
 *
 * <p>Un {@code record} promete {@code equals}, {@code hashCode} y {@code toString} derivados de sus
 * componentes. El compilador podría emitir los tres cuerpos en cada record —comparar campo por
 * campo, mezclar hashes, concatenar—, y esa fue la primera idea. El problema es que congelaría la
 * definición exacta de "derivados" en cada class file jamás compilado: cambiar cómo se mezcla un
 * hash obligaría a recompilar el mundo.
 *
 * <p>Con un {@code invokedynamic} el class file sólo dice <em>cuáles</em> son los componentes; el
 * JDK decide en tiempo de ejecución qué hacer con ellos. Es el mismo movimiento que hizo
 * {@link java.lang.invoke.StringConcatFactory} con la concatenación.
 *
 * <h2>Los tres, desde una sola entrada</h2>
 *
 * <p>{@link #bootstrap} atiende a los tres, y los distingue por el <strong>nombre del call site</strong>
 * —el parámetro {@code methodName}—, no por la firma. Es la razón de que la tabla
 * {@code BootstrapMethods} de un record tenga una sola entrada y tres call sites apuntándole.
 *
 * <h2>Acá lo hace la VM</h2>
 *
 * <p>Esta VM implementa el bootstrap en Rust y reconoce la clase por su nombre, así que este cuerpo
 * no corre nunca; la declaración existe para que el descriptor del call site resuelva y para que
 * quien lea la biblioteca encuentre la clase donde el JDK la tiene. Es el mismo trato que reciben
 * {@code LambdaMetafactory} y {@code StringConcatFactory}, y está anotado en {@code intrinsecos.md}.
 *
 * @since 16
 */
public final class ObjectMethods {

    private ObjectMethods() {
    }

    /**
     * Arma el {@code equals}, el {@code hashCode} o el {@code toString} de un record.
     *
     * <p>{@code MethodHandles$Lookup} con el nombre <strong>binario</strong> y no
     * {@code MethodHandles.Lookup}: el nombre Java de un tipo anidado de otro archivo no resuelve
     * (#101), y esquivarlo con un {@code import} emite {@code LLookup;}, una clase que no existe en
     * ningún paquete (#208). El nombre binario da el descriptor exacto del JDK, que es lo que un
     * call site necesita para ligar.
     *
     * @param lookup el contexto de acceso del record que se está enlazando
     * @param methodName cuál de los tres se pide: {@code "equals"}, {@code "hashCode"} o
     *     {@code "toString"}
     * @param type la forma del call site
     * @param recordClass el record
     * @param names los nombres de los componentes, separados por punto y coma
     * @param getters un getter por componente, en el mismo orden que {@code names}
     */
    public static Object bootstrap(MethodHandles$Lookup lookup, String methodName,
            TypeDescriptor type, Class<?> recordClass, String names, MethodHandle... getters)
            throws Throwable {
        throw new UnsupportedOperationException("los metodos de un record los arma la VM");
    }
}
