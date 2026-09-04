package jdk.dynalink.support;

import java.lang.invoke.MethodHandle;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jdk.dynalink.CallSiteDescriptor;
import jdk.dynalink.linker.GuardedInvocation;

/**
 * Un sitio de invocacion que <strong>acumula</strong> invocaciones: la cache polimorfica.
 *
 * <h2>Como queda armado el destino</h2>
 *
 * <p>Las invocaciones se encadenan una adentro de la otra. Si la guarda de la primera falla se prueba
 * la segunda, si esa falla la tercera, y asi hasta que se acaban y recien ahi se vuelve a enlazar.
 * Un sitio que ve tres tipos de receptor termina con las tres invocaciones puestas y no enlaza nunca
 * mas.
 *
 * <p>Es la respuesta al caso que {@link SimpleRelinkableCallSite} hace patologico: dos tipos
 * alternandose, donde el sitio monomorfico reenlaza en cada llamada.
 *
 * <h2>Por que la cadena tiene tope</h2>
 *
 * <p>Porque encadenar deja de rendir. Cada eslabon es una guarda mas que se evalua antes de llegar
 * al que sirve, asi que una cadena de cincuenta es mas lenta que volver a enlazar. Y el JIT no puede
 * incorporar en linea una cadena arbitrariamente larga, con lo cual pasado cierto punto el sitio se
 * vuelve mas lento cuanto mas aprende.
 *
 * <p>El tope es {@link #getMaxChainLength}, ocho por omision, y esta como metodo {@code protected}
 * para que una subclase lo cambie sabiendo lo que hace.
 *
 * <h2>Que pasa al llegar al tope</h2>
 *
 * <p>Se descarta la invocacion mas vieja para hacerle lugar a la nueva. Es una cache por antiguedad,
 * no por frecuencia: no se lleva la cuenta de cual se usa mas porque contar en el camino caliente
 * costaria mas que lo que la mejor politica ahorraria.
 *
 * <h2>Las invalidadas se limpian solas</h2>
 *
 * <p>Antes de armar el destino se descartan las invocaciones cuyos switch points ya se bajaron. Es
 * el momento natural para hacerlo: ya se esta recorriendo la cadena entera, y una invocacion muerta
 * ocuparia un lugar del tope sin poder servir nunca.
 *
 * @since 9
 */
public class ChainedCallSite extends AbstractRelinkableCallSite {

    /**
     * Las invocaciones vigentes, de la mas vieja a la mas nueva.
     *
     * <p>Es una lista enlazada porque las dos operaciones que se hacen son agregar al final y sacar
     * del principio, y las dos son constantes ahi.
     */
    private final List<GuardedInvocation> invocations = new LinkedList<GuardedInvocation>();

    /**
     * Un sitio con ese descriptor.
     *
     * @param descriptor el descriptor
     */
    public ChainedCallSite(final CallSiteDescriptor descriptor) {
        super(descriptor);
    }

    /**
     * Cuantas invocaciones se guardan como maximo.
     *
     * <p>Ocho, como el JDK. Redefinirlo hacia arriba solo tiene sentido si se midio que el sitio ve
     * mas tipos que eso y que la cadena larga sigue rindiendo.
     *
     * @return el tope
     */
    protected int getMaxChainLength() {
        return 8;
    }

    /**
     * Agrega una invocacion a la cadena.
     *
     * @param guardedInvocation la invocacion con su guarda
     * @param relinkAndInvoke el camino de respaldo, que vuelve a enlazar
     */
    public void relink(final GuardedInvocation guardedInvocation,
            final MethodHandle relinkAndInvoke) {
        relinkInternal(guardedInvocation, relinkAndInvoke, false);
    }

    /**
     * Tira la cadena entera y arranca de nuevo con esta invocacion.
     *
     * <p>Lo pide el enlazador cuando decidio que el sitio es inestable: si el receptor cambia todo el
     * tiempo, acumular invocaciones solo gasta memoria y agrega guardas que van a fallar.
     *
     * @param guardedInvocation la invocacion con su guarda
     * @param relinkAndInvoke el camino de respaldo, que vuelve a enlazar
     */
    public void resetAndRelink(final GuardedInvocation guardedInvocation,
            final MethodHandle relinkAndInvoke) {
        relinkInternal(guardedInvocation, relinkAndInvoke, true);
    }

    private void relinkInternal(final GuardedInvocation invocation, final MethodHandle fallback,
            final boolean reset) {
        if (reset) {
            invocations.clear();
        } else {
            // Sacar las muertas antes de contar: si no, una invalidada podria empujar afuera a una
            // que todavia sirve.
            final Iterator<GuardedInvocation> it = invocations.iterator();
            while (it.hasNext()) {
                if (it.next().hasBeenInvalidated()) {
                    it.remove();
                }
            }
            while (invocations.size() >= getMaxChainLength()) {
                invocations.remove(0);
            }
        }
        invocations.add(invocation);

        // El armado va del final hacia el principio: la ultima agregada queda mas adentro, con el
        // respaldo real detras, y cada una anterior la envuelve. Asi la primera de la lista es la
        // primera que se prueba.
        MethodHandle destino = fallback;
        for (int i = invocations.size() - 1; i >= 0; i--) {
            destino = invocations.get(i).compose(destino);
        }
        setTarget(destino);
    }
}
