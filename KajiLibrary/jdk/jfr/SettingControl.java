package jdk.jfr;

import java.util.Set;

/**
 * Un ajuste propio de un evento: como se lee, como se escribe y como se combinan varios.
 *
 * <h2>Por que hace falta {@link #combine}</h2>
 *
 * <p>Es la parte no obvia, y es la razon de que esta clase exista en vez de un simple par de
 * accesores.
 *
 * <p>Puede haber <strong>varias grabaciones a la vez</strong>, cada una con su configuracion. Si
 * una pide umbral de 10 ms y otra de 100 ms, el evento tiene que emitirse con el umbral que
 * satisfaga a las dos —10 ms, el mas exigente— porque si no, la primera grabacion se pierde eventos
 * que pidio.
 *
 * <p>{@link #combine} recibe todos los valores pedidos y devuelve el que se va a usar. Para un
 * umbral eso es el minimo; para un booleano de "grabar la pila" es el {@code true}; para algo
 * enumerado puede ser otra cosa. Solo el que definio el ajuste sabe cual, y por eso lo decide el.
 *
 * <h2>El valor es siempre texto</h2>
 *
 * <p>Porque tiene que poder venir de un archivo de configuracion, de {@code jcmd} y de la API, y el
 * unico formato que sirve para los tres es una cadena. Interpretarla es trabajo de la subclase.
 *
 * @since 9
 */
public abstract class SettingControl {

    /** Para las subclases. */
    protected SettingControl() {
    }

    /**
     * El valor que resulta de combinar los que pidieron todas las grabaciones activas.
     *
     * <p>Se llama cada vez que una grabacion arranca, para y cambia sus ajustes.
     *
     * @param settingValues los valores pedidos; nunca vacio
     * @return el valor a usar
     */
    public abstract String combine(Set<String> settingValues);

    /**
     * Fija el valor efectivo.
     *
     * <p>Lo llama JFR con el resultado de {@link #combine}, no el usuario.
     *
     * @param settingValue el valor
     */
    public abstract void setValue(String settingValue);

    /**
     * El valor efectivo.
     *
     * @return el valor
     */
    public abstract String getValue();
}
