package com.sun.security.jgss;

/**
 * Que se le puede preguntar a un contexto ya establecido.
 *
 * <p>Es el argumento de {@link ExtendedGSSContext#inquireSecContext}, y cada constante trae un tipo
 * de retorno distinto --el metodo devuelve {@code Object} justamente por eso. Las seis son de
 * Kerberos 5: GSS-API no define ninguna consulta portable, asi que todo lo que se puede preguntar
 * es especifico del mecanismo.
 */
public enum InquireType {

    /**
     * La clave de sesion. Devuelve una {@link javax.crypto.SecretKey}.
     *
     * @deprecated Da la clave sin decir con que tipo de cifrado de Kerberos va; use
     *     {@link #KRB5_GET_SESSION_KEY_EX}, que devuelve las dos cosas.
     */
    @Deprecated
    KRB5_GET_SESSION_KEY,

    /** La clave de sesion junto con su tipo de cifrado. Devuelve un `EncryptionKey` de Kerberos. */
    KRB5_GET_SESSION_KEY_EX,

    /** Las banderas del ticket de servicio. Devuelve un {@code boolean[]} de 32 posiciones. */
    KRB5_GET_TKT_FLAGS,

    /** Los datos de autorizacion del ticket. Devuelve un {@link AuthorizationDataEntry}`[]`. */
    KRB5_GET_AUTHZ_DATA,

    /** El instante de autenticacion del ticket. Devuelve una {@link java.util.Date}. */
    KRB5_GET_AUTHTIME,

    /** La credencial delegada, en formato KRB-CRED. Devuelve un `KerberosCredMessage`. */
    KRB5_GET_KRB_CRED
}
