package com.sun.security.jgss;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

/**
 * Una {@link GSSCredential} que sabe hacerse pasar por otro.
 *
 * <p>Es la mitad cliente de la delegacion restringida de Kerberos (S4U2self + S4U2proxy): un
 * servicio de frente --un servidor web-- autentica a un usuario por otro medio y despues necesita
 * hablar con un servicio de atras --una base-- **en nombre de ese usuario**, sin que el usuario le
 * haya delegado nada.
 *
 * <p>Que eso sea seguro depende enteramente del KDC: es el que decide, por politica, para que
 * servicios el de frente puede pedir tickets ajenos. La credencial que sale de
 * {@link #impersonate} no es mas poderosa que lo que el KDC este dispuesto a emitir.
 */
public interface ExtendedGSSCredential extends GSSCredential {

    /**
     * Una credencial para actuar en nombre de `name`.
     *
     * @param name a quien hay que hacerse pasar
     * @return la credencial para ese nombre
     * @throws GSSException si el mecanismo no soporta la suplantacion, o si el KDC la rechaza
     */
    GSSCredential impersonate(org.ietf.jgss.GSSName name) throws GSSException;
}
