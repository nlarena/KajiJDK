package jdk.jshell.spi;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import jdk.jshell.JShellConsole;

/**
 * Lo que `jshell` le presta a un {@link ExecutionControl} para que haga su trabajo.
 *
 * <p>Es la direccion contraria del SPI: {@link ExecutionControl} es lo que el motor le ofrece a
 * `jshell`, y esto es lo que `jshell` le ofrece al motor. Y lo que le ofrece son las tres cosas que
 * el motor no puede fabricarse solo:
 *
 * <ul>
 *   <li>los flujos del usuario, que el motor tiene que **conectar** a la VM donde ejecuta;
 *   <li>las opciones extra con las que arrancar esa VM;
 *   <li>una forma de avisar que se murio, que es {@link #closeDown()}.
 * </ul>
 *
 * <p>Lo implementa `jshell`, no el motor.
 *
 * @since 9
 */
public interface ExecutionEnv {

    /** De donde el codigo del usuario lee. */
    InputStream userIn();

    /** Donde el codigo del usuario escribe. */
    PrintStream userOut();

    /** Donde el codigo del usuario escribe sus errores. */
    PrintStream userErr();

    /**
     * Las opciones extra para la VM remota.
     *
     * <p>Salen de los `-R` de la linea de comandos. El motor las agrega a las suyas si lanza una VM
     * aparte, y las ignora si ejecuta en el mismo proceso.
     */
    List<String> extraRemoteVMOptions();

    /**
     * Avisa que el motor se murio.
     *
     * <p>Lo llama el **motor**, no `jshell`: es como le cuenta que se quedo sin VM del otro lado y
     * que hay que rearmarlo. `jshell` lo usa para no seguir mandandole fragmentos.
     */
    void closeDown();

    /**
     * La consola que ve el codigo del usuario, si hay una.
     *
     * <p>Vacio significa que no hay consola, y no que no se sepa: el codigo del usuario que llame a
     * `System.console()` tiene que recibir `null`.
     *
     * <p>Es `default` porque llego con Java 22, y un motor escrito antes tiene que seguir
     * compilando.
     */
    default Optional<JShellConsole> console() {
        return Optional.empty();
    }
}
