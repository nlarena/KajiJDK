package javax.naming.ldap;

import javax.naming.NamingException;

/**
 * Lo implementa un resultado de busqueda que ademas trae controles del servidor.
 *
 * <h2>Por que es una interfaz aparte y no un metodo de {@code SearchResult}</h2>
 *
 * <p>Porque {@code javax.naming} es neutral respecto del protocolo: sirve para LDAP, para DNS, para
 * un directorio de archivos. Los controles son de LDAP, asi que meterlos en el tipo comun ataria la
 * API general a un protocolo particular.
 *
 * <p>La consecuencia practica es que hay que preguntar con {@code instanceof} antes de leerlos.
 */
public interface HasControls {

    /** Los controles que vinieron con este resultado, o {@code null} si no vino ninguno. */
    Control[] getControls() throws NamingException;
}
