package com.sun.jdi.connect;

import com.sun.jdi.VirtualMachine;
import java.io.IOException;
import java.util.Map;

/**
 * El conector que **arranca** la VM depurada y se conecta a ella.
 *
 * <p>Es el que usa un IDE cuando uno le da a "depurar": lanza el proceso con las opciones de JDWP
 * ya puestas y espera a que la conexion quede armada. La VM arranca **suspendida** --si no, el
 * programa podria terminar antes de que el depurador ponga el primer punto de interrupcion.
 *
 * <p>Es el unico de los tres que puede fallar con {@link VMStartException}: es el unico que tiene
 * un proceso propio que puede haber arrancado mal.
 */
public interface LaunchingConnector extends Connector {

    /**
     * Arranca la VM que describan esos argumentos y se conecta.
     *
     * @param arguments el mapa que salio de {@link #defaultArguments()}, con los valores puestos
     * @return la VM depurada, suspendida
     * @throws IOException si no se pudo llegar al otro extremo
     * @throws IllegalConnectorArgumentsException si algun argumento falta o no sirve
     * @throws VMStartException si la VM arranco pero la conexion no; la excepcion trae el proceso,
     *     y hay que leerle los flujos y terminarlo
     */
    VirtualMachine launch(Map<String, ? extends Connector.Argument> arguments)
            throws IOException, IllegalConnectorArgumentsException, VMStartException;
}
