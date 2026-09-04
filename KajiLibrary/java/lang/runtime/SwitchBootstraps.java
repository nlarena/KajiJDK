package java.lang.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles$Lookup;
import java.lang.invoke.MethodType;

/**
 * Los dos bootstraps del {@code switch} con patrones.
 *
 * <h2>Qué cambió cuando el {@code switch} dejó de ser sobre enteros</h2>
 *
 * <p>Un {@code switch} clásico compila a {@code tableswitch} o {@code lookupswitch}: el valor es un
 * entero y la elección es una búsqueda. Un {@code switch} sobre <em>patrones de tipo</em> no puede
 * hacer eso — {@code case String s} no es un número, es una pregunta de {@code instanceof} — y
 * emitir la cadena de {@code instanceof} en el class file volvería a congelar la estrategia.
 *
 * <p>La solución es la de siempre acá: el class file dice cuáles son las etiquetas y un
 * {@code invokedynamic} pide el selector. Lo que devuelven estos dos métodos es un
 * {@link CallSite} cuyo handle contesta <strong>el índice de la primera etiqueta que matchea</strong>,
 * o la cantidad de etiquetas si no matchea ninguna — de vuelta a un entero, que es lo que el
 * {@code tableswitch} de abajo sabe usar.
 *
 * <h2>Por qué son dos y no uno</h2>
 *
 * <p>{@link #enumSwitch} existe porque las etiquetas de un {@code switch} sobre un enum son
 * <em>nombres de constantes</em>, y resolver un nombre a su constante necesita cargar la clase del
 * enum. Hacerlo en el bootstrap —una vez, al ligar— en vez de en cada ejecución es todo el punto.
 *
 * <h2>Acá lo hace la VM</h2>
 *
 * <p>Igual que {@link ObjectMethods}: implementado en Rust y reconocido por nombre, así que estos
 * cuerpos no corren. La declaración está para que el call site resuelva.
 *
 * @since 21
 */
public final class SwitchBootstraps {

    private SwitchBootstraps() {
    }

    /**
     * El selector de un {@code switch} sobre patrones de tipo.
     *
     * @param lookup el contexto de acceso de quien hace el {@code switch}
     * @param invocationName el nombre del call site, que este bootstrap ignora
     * @param invocationType la forma del selector: recibe el valor y el índice desde donde
     *     reanudar, y devuelve un {@code int}
     * @param labels las etiquetas, en el orden en que están escritas — y ese orden importa, porque
     *     gana la primera que matchea
     */
    public static CallSite typeSwitch(MethodHandles$Lookup lookup, String invocationName,
            MethodType invocationType, Object... labels) {
        throw new UnsupportedOperationException("el selector del switch lo arma la VM");
    }

    /**
     * El selector de un {@code switch} sobre un enum, donde las etiquetas pueden ser nombres de
     * constantes además de patrones.
     */
    public static CallSite enumSwitch(MethodHandles$Lookup lookup, String invocationName,
            MethodType invocationType, Object... labels) {
        throw new UnsupportedOperationException("el selector del switch lo arma la VM");
    }
}
