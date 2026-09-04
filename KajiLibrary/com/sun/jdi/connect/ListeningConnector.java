package com.sun.jdi.connect;

import com.sun.jdi.VirtualMachine;
import java.io.IOException;
import java.util.Map;

/**
 * El conector que **espera** a que la VM depurada se conecte al depurador.
 *
 * <p>Los papeles estan al reves de {@link AttachingConnector}: aca el depurador escucha y la VM
 * llama, que es lo que pasa cuando el programa arranca con {@code server=n} y una direccion. Sirve
 * cuando la VM esta detras de algo que no deja conectarse hacia ella, o cuando arranca sola --en un
 * arranque del sistema, en un contenedor-- y el depurador no controla el momento.
 *
 * <p>Por eso el ciclo son tres pasos y no uno: {@link #startListening} devuelve la direccion que
 * hay que pasarle a la VM, {@link #accept} espera la conexion, y {@link #stopListening} cierra.
 */
public interface ListeningConnector extends Connector {

    /**
     * Si este conector puede aceptar varias conexiones sobre la misma escucha.
     *
     * <p>Con `false` hay que volver a {@link #startListening} para cada VM; y como la direccion
     * puede cambiar, no se puede repartir de antemano.
     */
    boolean supportsMultipleConnections();

    /**
     * Empieza a escuchar.
     *
     * @param arguments el mapa que salio de {@link #defaultArguments()}, con los valores puestos
     * @return la direccion a la que la VM se tiene que conectar, en el formato del transporte
     * @throws IOException si no se pudo abrir la escucha
     * @throws IllegalConnectorArgumentsException si algun argumento falta o no sirve
     */
    String startListening(Map<String, ? extends Connector.Argument> arguments)
            throws IOException, IllegalConnectorArgumentsException;

    /**
     * Deja de escuchar.
     *
     * <p>Los argumentos tienen que ser los mismos que los de {@link #startListening}: es como se
     * identifica cual de las escuchas se cierra.
     *
     * @throws IOException si no se pudo cerrar
     * @throws IllegalConnectorArgumentsException si algun argumento falta o no sirve
     */
    void stopListening(Map<String, ? extends Connector.Argument> arguments)
            throws IOException, IllegalConnectorArgumentsException;

    /**
     * Espera a que una VM se conecte.
     *
     * <p>Bloquea. Si los argumentos traen un plazo y se vence, sale
     * {@link TransportTimeoutException}, que es una {@link IOException}.
     *
     * @param arguments los mismos que los de {@link #startListening}
     * @return la VM depurada
     * @throws IOException si fallo la espera o se vencio el plazo
     * @throws IllegalConnectorArgumentsException si algun argumento falta o no sirve
     */
    VirtualMachine accept(Map<String, ? extends Connector.Argument> arguments)
            throws IOException, IllegalConnectorArgumentsException;
}
