package java.rmi.server;

/**
 * Un objeto remoto que quiere enterarse cuando ya no lo referencia ningun cliente.
 *
 * <h2>Por que hace falta un aviso explicito</h2>
 *
 * <p>Porque el recolector local no ve las referencias remotas: para el, un objeto exportado esta
 * vivo mientras el runtime de RMI lo tenga. Quien lleva la cuenta de los clientes es el recolector
 * distribuido, y este metodo es como avisa que la cuenta llego a cero — el momento de soltar lo que
 * el objeto tuviera tomado.
 *
 * <p>No es una promesa de finalizacion: el objeto puede volver a ser referenciado despues si alguien
 * conserva su stub, y entonces {@link #unreferenced} se llama de nuevo mas adelante.
 */
public interface Unreferenced {

    /** Ya no queda ningun cliente con una referencia. */
    void unreferenced();
}
