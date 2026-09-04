package com.sun.security.jgss;

/**
 * Una entrada de datos de autorizacion de un ticket de Kerberos 5.
 *
 * <p>El campo `AuthorizationData` de un ticket es una lista de pares (tipo, bytes) que el KDC mete
 * y que el servicio interpreta. El contenido depende del tipo --el `AD-IF-RELEVANT` de RFC 4120, el
 * `PAC` de Windows-- y esta clase no lo mira: entrega los bytes crudos y deja la interpretacion al
 * que sepa.
 *
 * <p>Es inmutable, y los `byte[]` se copian al entrar y al salir. Sin la copia, quien recibiera la
 * entrada podria modificar los datos de autorizacion de un ticket ya validado, que es exactamente
 * lo que un dato de autorizacion no puede permitir.
 *
 * @see InquireType#KRB5_GET_AUTHZ_DATA
 */
public final class AuthorizationDataEntry {

    private final int type;
    private final byte[] data;

    /**
     * Una entrada con ese tipo y esos datos.
     *
     * @param type el numero de tipo, de los que registra RFC 4120
     * @param data los bytes; se copian
     */
    public AuthorizationDataEntry(int type, byte[] data) {
        this.type = type;
        this.data = data.clone();
    }

    /** El numero de tipo. */
    public int getType() {
        return this.type;
    }

    /** Una copia de los bytes. */
    public byte[] getData() {
        return this.data.clone();
    }

    public String toString() {
        return "AuthorizationDataEntry: type=" + this.type + ", data=" + this.data.length
                + " bytes";
    }
}
