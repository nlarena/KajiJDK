package com.sun.security.jgss;

import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;

/**
 * Un {@link GSSContext} con lo que GSS-API estandar no da.
 *
 * <p>Las tres operaciones tienen la misma razon de ser: el estandar deja fuera cosas que un
 * programa que usa Kerberos de verdad necesita --mirar adentro del contexto, y la delegacion
 * restringida-- y esta interfaz las agrega sin tocar la interfaz portable.
 *
 * <p>No se implementa: la devuelve el proveedor. Un `GSSContext` obtenido de
 * {@code GSSManager.createContext} se puede probar con `instanceof` y usar como `ExtendedGSSContext`
 * si el mecanismo lo soporta.
 */
public interface ExtendedGSSContext extends GSSContext {

    /**
     * Pregunta uno de los datos internos del contexto.
     *
     * <p>Solo tiene sentido con el contexto ya establecido; antes, la respuesta no existe todavia.
     * El tipo de lo que devuelve depende de `type`: ver {@link InquireType}.
     *
     * @param type que se pregunta
     * @return la respuesta, del tipo que documente `type`
     * @throws GSSException si el contexto no esta establecido, o si el mecanismo no sabe responder
     *     esa consulta
     */
    Object inquireSecContext(InquireType type) throws GSSException;

    /**
     * Pide que la delegacion quede sujeta a la politica del KDC.
     *
     * <p>Es mas estricto que {@code requestCredDeleg}: alli el cliente decide delegar y el servicio
     * recibe la credencial completa. Aca la decision la toma el KDC, que marca el ticket como
     * `OK-AS-DELEGATE` solo para los servicios en los que confia. Sirve para no entregarle la
     * identidad del usuario a cualquier servicio al que se conecte.
     *
     * <p>Hay que llamarlo **antes** de establecer el contexto; despues no tiene efecto.
     *
     * @throws GSSException si el mecanismo no soporta la politica de delegacion
     */
    void requestDelegPolicy(boolean state) throws GSSException;

    /**
     * Si la politica de delegacion quedo en efecto.
     *
     * <p>Antes de establecer el contexto informa lo que se pidio; despues, lo que se consiguio, que
     * puede ser menos.
     */
    boolean getDelegPolicyState();
}
