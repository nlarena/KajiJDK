package com.sun.jdi.request;

import com.sun.jdi.Mirror;

/**
 * Un pedido de aviso: la mitad de JDI que dice <strong>que</strong> se quiere saber.
 *
 * <h2>Se configura apagado y despues se prende</h2>
 *
 * <p>Un pedido nace deshabilitado. Los filtros --por hilo, por clase, por instancia-- solo se
 * pueden poner mientras esta apagado, y {@link #enable} lo activa. Intentar filtrar un pedido ya
 * habilitado tira {@link InvalidRequestStateException}.
 *
 * <p>El orden no es capricho: los filtros se traducen a configuracion de la otra VM, y cambiarlos
 * con el pedido activo dejaria eventos en vuelo con el filtro viejo.
 *
 * <h2>La politica de suspension es la decision importante</h2>
 *
 * <p>{@link #setSuspendPolicy} decide que se congela cuando el evento llega: {@link #SUSPEND_NONE}
 * nada, {@link #SUSPEND_EVENT_THREAD} el hilo que lo genero, {@link #SUSPEND_ALL} el programa
 * entero.
 *
 * <p>{@code SUSPEND_ALL} es lo que un depurador quiere para un punto de interrupcion y lo peor
 * posible para un evento frecuente: congela todo, miles de veces por segundo.
 *
 * <h2>Filtrar es una optimizacion, no una comodidad</h2>
 *
 * <p>Sin filtro, cada ocurrencia cruza la conexion. Un {@code MethodEntryRequest} sin filtrar sobre
 * un programa real manda millones de eventos y lo vuelve inusable. El filtro se aplica <strong>del
 * lado de la VM depurada</strong>, que es lo que evita el viaje.
 *
 * @since 1.3
 */
public interface EventRequest extends Mirror {

    /** No suspender nada cuando el evento llegue. */
    int SUSPEND_NONE = 0;

    /** Suspender solo el hilo que genero el evento. */
    int SUSPEND_EVENT_THREAD = 1;

    /**
     * Suspender todos los hilos.
     *
     * <p>Es lo que un punto de interrupcion quiere y lo peor posible para un evento frecuente.
     */
    int SUSPEND_ALL = 2;

    /**
     * Si enabled.
     *
     * @return el resultado
     */
    boolean isEnabled();

    /**
     * Fija el enabled.
     *
     * @param flag el boolean
     */
    void setEnabled(boolean flag);

    /**
     * El enable.
     */
    void enable();

    /**
     * El disable.
     */
    void disable();

    /**
     * Filtra por count; solo con el pedido deshabilitado.
     *
     * @param index el int
     */
    void addCountFilter(int index);

    /**
     * Fija el suspend policy.
     *
     * @param index el int
     */
    void setSuspendPolicy(int index);

    /**
     * El suspend policy.
     *
     * @return el resultado
     */
    int suspendPolicy();

    /**
     * El put property.
     *
     * @param obj el Object
     * @param obj2 el Object
     */
    void putProperty(Object obj, Object obj2);

    /**
     * El property.
     *
     * @param obj el Object
     * @return el resultado
     */
    Object getProperty(Object obj);
}
