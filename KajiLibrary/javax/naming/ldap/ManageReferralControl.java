package javax.naming.ldap;

/**
 * Pide que el servidor <strong>no</strong> siga las referencias: que devuelva el objeto de
 * referencia en vez de mandar a buscarlo a otro lado.
 *
 * <p>Un directorio LDAP puede estar repartido entre servidores, y cuando uno no tiene lo que le
 * piden devuelve una <em>referencia</em>. Por omision el cliente la sigue, que es lo que se quiere
 * casi siempre. Este control apaga eso.
 *
 * <p>Para que sirve apagarlo: para <strong>administrar la referencia misma</strong>. Sin este
 * control no hay forma de borrar o modificar un objeto de referencia — cualquier operacion sobre el
 * se redirige al servidor al que apunta, que es justamente lo que no se quiere.
 */
public final class ManageReferralControl extends BasicControl {

    private static final long serialVersionUID = 3017756160149982566L;

    /** El OID de este control. */
    public static final String OID = "2.16.840.1.113730.3.4.2";

    /** Critico: si el servidor no lo entiende, la operacion falla. */
    public ManageReferralControl() {
        super(OID, true, null);
    }

    /**
     * @param criticality si la operacion debe fallar cuando el servidor no lo entiende. Ponerlo en
     *     {@code false} aca es casi siempre un error: significaria administrar la referencia si se
     *     puede y seguirla si no, que son dos cosas completamente distintas
     */
    public ManageReferralControl(boolean criticality) {
        super(OID, criticality, null);
    }
}
