package javax.naming.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ReferralException;

/**
 * El servidor no tiene lo que se le pidio y dice donde buscarlo.
 *
 * <h2>Por que una excepcion y no un valor de retorno</h2>
 *
 * <p>Porque una referencia interrumpe la operacion: lo que se pidio no esta <em>aca</em>. Modelarla
 * como resultado obligaria a que toda llamada devolviera "o el dato o una redireccion", y eso
 * contaminaria la API entera por un caso que casi nunca pasa.
 *
 * <p>Lo raro de esta excepcion es que se <strong>continua</strong>: {@link #getReferralContext}
 * devuelve un contexto ya apuntando al otro servidor, y ahi se repite la operacion. Puede haber
 * varias referencias encadenadas, asi que el patron es un bucle que atrapa, sigue y reintenta.
 *
 * <p>La sobrecarga con {@link Control}{@code []} es lo que agrega LDAP sobre
 * {@link ReferralException}: los controles del contexto original no viajan solos al servidor nuevo,
 * y hay que decidir cuales llevar.
 */
public abstract class LdapReferralException extends ReferralException {

    private static final long serialVersionUID = -1668992791764950804L;

    /** Con un mensaje. */
    protected LdapReferralException(String explanation) {
        super(explanation);
    }

    /** Sin mensaje. */
    protected LdapReferralException() {
        super();
    }

    /** Un contexto apuntando al servidor referido. */
    public abstract Context getReferralContext() throws NamingException;

    /** Igual, con otro entorno. */
    public abstract Context getReferralContext(Hashtable<?, ?> env) throws NamingException;

    /**
     * Igual, con otro entorno y esos controles de conexion.
     *
     * <p>Los controles no se heredan del contexto original: el servidor nuevo puede no soportarlos,
     * y mandarlos como criticos alla haria fallar la operacion que se estaba intentando salvar.
     */
    public abstract Context getReferralContext(Hashtable<?, ?> env, Control[] reqCtls)
            throws NamingException;
}
