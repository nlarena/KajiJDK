package com.sun.security.jgss;

import javax.security.auth.Subject;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSName;

/**
 * El puente entre GSS-API y JAAS.
 *
 * <p>Un solo metodo, y hace una sola cosa: pasar una identidad del mundo de GSS --un
 * {@link GSSName} y una {@link GSSCredential}-- al mundo de {@link Subject}, que es donde la espera
 * todo lo que autoriza en Java. Sin el, un programa que autentica por GSS-API no tiene con que
 * llamar a {@code Subject.doAs}.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>{@link #createSubject} no esta implementado. La conversion **no es generica**: hay que
 * traducir el `GSSName` al `KerberosPrincipal` correspondiente y la `GSSCredential` a los
 * {@code KerberosTicket} y {@code KerberosKey} que lleva adentro, y eso pide el proveedor de
 * Kerberos entero --que esta biblioteca no tiene. Lanza
 * {@link UnsupportedOperationException} con el motivo.
 *
 * <p>La alternativa seria devolver un `Subject` vacio, o con el nombre metido como principal
 * generico. Seria peor: un `Subject` sin las credenciales de Kerberos **parece** una identidad
 * valida, pasa por `Subject.doAs`, y falla mucho despues --en la primera llamada que necesite el
 * ticket-- sin ninguna pista de que la identidad venia mal armada desde aca.
 */
public class GSSUtil {

    /** No se instancia: es una clase de utilidad. */
    private GSSUtil() {
    }

    /**
     * El {@link Subject} que corresponde a esa identidad GSS.
     *
     * <p><b>No implementado en esta biblioteca.</b> Ver la nota de la clase.
     *
     * @param principals el nombre, o `null`
     * @param credentials las credenciales, o `null`
     * @throws UnsupportedOperationException siempre, en esta biblioteca
     */
    public static Subject createSubject(GSSName principals, GSSCredential credentials) {
        throw new UnsupportedOperationException(
                "cannot build a Subject from GSS identities: the conversion is Kerberos-specific "
                + "(KerberosPrincipal, KerberosTicket, KerberosKey) and no Kerberos mechanism is "
                + "present in this library");
    }
}
