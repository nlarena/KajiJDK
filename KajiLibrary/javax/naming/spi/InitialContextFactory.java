package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.spi.InitialContextFactory -- de donde sale el contexto inicial.
 *
 * <p>El punto de entrada de un proveedor JNDI entero. La aplicacion pone el nombre de la clase que la
 * implementa en la propiedad {@code java.naming.factory.initial}, y todo lo demas --el
 * {@code InitialContext}, sus busquedas, sus subcontextos-- sale de lo que devuelva este metodo.
 *
 * <p>Una implementacion tiene que tener constructor publico sin argumentos: la plataforma la carga
 * por nombre y la instancia por reflexion.
 */
public interface InitialContextFactory {

    /**
     * El contexto inicial para ese ambiente.
     *
     * @throws NamingException si el ambiente no alcanza para crearlo
     */
    Context getInitialContext(Hashtable<?, ?> environment) throws NamingException;
}
