package com.sun.jdi.connect;

import com.sun.jdi.VirtualMachine;
import java.io.IOException;
import java.util.Map;

/**
 * El conector que se pega a una VM que **ya esta corriendo**.
 *
 * <p>Es el caso mas comun fuera del desarrollo: un servidor que arranco con
 * {@code -agentlib:jdwp=transport=dt_socket,server=y,suspend=n} y al que uno se conecta despues,
 * sin reiniciarlo.
 *
 * <p>La VM depurada es la que escucha; el depurador es el que llama. Es al reves de
 * {@link ListeningConnector}.
 */
public interface AttachingConnector extends Connector {

    /**
     * Se pega a la VM que describan esos argumentos.
     *
     * @param arguments el mapa que salio de {@link #defaultArguments()}, con los valores puestos
     * @return la VM depurada
     * @throws IOException si no se pudo llegar al otro extremo
     * @throws IllegalConnectorArgumentsException si algun argumento falta o no sirve
     */
    VirtualMachine attach(Map<String, ? extends Connector.Argument> arguments)
            throws IOException, IllegalConnectorArgumentsException;
}
