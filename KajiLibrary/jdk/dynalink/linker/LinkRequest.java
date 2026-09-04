package jdk.dynalink.linker;

import jdk.dynalink.CallSiteDescriptor;

/**
 * Lo que se le pide a un enlazador: el sitio de invocacion mas los argumentos reales.
 *
 * <h2>Por que hacen falta los argumentos, si el sitio ya tiene los tipos</h2>
 *
 * <p>Porque los tipos estaticos del sitio son los del lenguaje que llama, no los del objeto que
 * recibe. En un sitio {@code (Object,Object)Object} el descriptor no dice nada util; lo que
 * decide el enlace es que el receptor sea, en tiempo de ejecucion, una instancia de tal clase.
 * Esta interfaz es la que expone ese dato.
 *
 * <h2>Por que la inestabilidad viaja aca adentro</h2>
 *
 * <p>{@link #isCallSiteUnstable} le avisa al enlazador que este sitio ya cambio de opinion
 * demasiadas veces — es megamorfico. Un enlazador que lo sabe puede devolver algo mas generico y
 * mas barato en lugar de una invocacion especializada que va a quedar invalidada enseguida. Sin
 * este dato la unica estrategia posible seria especializar siempre, que es la peor para el caso
 * megamorfico.
 *
 * @since 9
 */
public interface LinkRequest {

    /** El descriptor del sitio de invocacion. */
    CallSiteDescriptor getCallSiteDescriptor();

    /**
     * Los argumentos de la invocacion que disparo el enlace.
     *
     * <p>Devuelve una copia: son mutables y el enlazador no deberia poder tocar los originales.
     */
    Object[] getArguments();

    /**
     * El primer argumento, o {@code null} si no hay ninguno.
     *
     * <p>Es un atajo para el caso abrumadoramente mas comun, que es mirar el receptor. Tambien
     * evita copiar el arreglo entero para leer una sola posicion.
     */
    Object getReceiver();

    /** Si el sitio ya se reenlazo tantas veces que conviene no especializar. */
    boolean isCallSiteUnstable();

    /**
     * El mismo pedido con otro descriptor y otros argumentos.
     *
     * <p>Lo usa un enlazador que descompone una operacion en otra —por ejemplo, resolver el
     * nombre de un metodo y despues delegar la invocacion— sin perder la marca de inestabilidad.
     *
     * @param newCallSiteDescriptor el descriptor nuevo
     * @param newArguments los argumentos nuevos
     * @return el pedido derivado
     */
    LinkRequest replaceArguments(CallSiteDescriptor newCallSiteDescriptor, Object... newArguments);
}
