package javax.security.auth.kerberos;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import javax.security.auth.RefreshFailedException;
import javax.security.auth.Refreshable;

/**
 * KajiLibrary's javax.security.auth.kerberos.KerberosTicket -- un ticket de Kerberos.
 *
 * <p>Lo que el KDC le da a un cliente para hablar con un servicio: los bytes del ticket --cifrados
 * con la clave del servicio, asi que el cliente no los puede leer--, la clave de sesion, y los
 * metadatos que si son legibles: para quien es, hasta cuando vale, que se puede hacer con el.
 *
 * <h2>Las banderas</h2>
 *
 * <p>Son treinta y dos bits de la RFC 4120 y esta clase expone los siete que importan al codigo de
 * usuario: {@link #isForwardable}, {@link #isForwarded}, {@link #isProxiable}, {@link #isProxy},
 * {@link #isPostdated}, {@link #isRenewable} e {@link #isInitial}. {@link #getFlags} da el arreglo
 * completo, siempre de al menos treinta y dos, rellenado con falsos si se dio uno mas corto.
 *
 * <h2>{@link #isCurrent} mira solo el vencimiento</h2>
 *
 * <p>Un ticket es vigente si no esta destruido y no paso su hora de fin. La hora de inicio no cuenta:
 * un ticket posfechado que todavia no empezo se reporta vigente, igual que en el JDK.
 *
 * <h2>{@link #refresh}</h2>
 *
 * <p>Renovar es pedirle al KDC un ticket nuevo con este, y KajiJDK no habla con un KDC: un ticket
 * renovable falla al renovarse con {@link RefreshFailedException} diciendo por que. Uno no renovable
 * o destruido falla antes, con el mismo mensaje que el JDK.
 *
 * <h2>Se destruye</h2>
 *
 * <p>{@link #destroy} borra la clave de sesion y los bytes del ticket. Despues, la clave y los bytes
 * lanzan {@link IllegalStateException}; los metadatos --cliente, fechas, banderas-- devuelven null o
 * falso, que es lo que un ticket que ya no existe puede decir de si mismo.
 */
public class KerberosTicket implements Destroyable, Refreshable, Serializable {

    private static final long serialVersionUID = 7395334370157380539L;

    /** Cuantas banderas tiene un ticket. */
    private static final int NUM_FLAGS = 32;

    /** Las posiciones de las siete banderas que se exponen, en el orden de la RFC 4120. */
    private static final int FORWARDABLE_TICKET_FLAG = 1;
    private static final int FORWARDED_TICKET_FLAG = 2;
    private static final int PROXIABLE_TICKET_FLAG = 3;
    private static final int PROXY_TICKET_FLAG = 4;
    private static final int POSTDATED_TICKET_FLAG = 6;
    private static final int RENEWABLE_TICKET_FLAG = 8;
    private static final int INITIAL_TICKET_FLAG = 9;

    /** Los bytes del ticket, o null si se destruyo. */
    private byte[] asn1Encoding;

    /** La clave de sesion, o null si se destruyo. */
    private EncryptionKey sessionKey;

    /** Las banderas, o null si se destruyo. */
    private boolean[] flags;

    /** Cuando se autentico el cliente, o null. */
    private Date authTime;

    /** Desde cuando vale, o null. */
    private Date startTime;

    /** Hasta cuando vale. */
    private Date endTime;

    /** Hasta cuando se puede renovar, o null. */
    private Date renewTill;

    /** Para quien es. */
    private KerberosPrincipal client;

    /** Para que servicio. */
    private KerberosPrincipal server;

    /** Desde que direcciones se puede usar, o null. */
    private InetAddress[] clientAddresses;

    /** Si ya se borro. */
    private transient boolean destroyed = false;

    /**
     * Un ticket con todo. Los arreglos y las fechas se copian.
     *
     * @param flags las banderas; null es ninguna, y un arreglo mas corto se rellena con falsos
     * @throws IllegalArgumentException si los bytes, el cliente, el servidor, la clave o la hora de
     *     fin son null
     */
    public KerberosTicket(byte[] asn1Encoding, KerberosPrincipal client, KerberosPrincipal server,
                          byte[] sessionKey, int keyType, boolean[] flags, Date authTime,
                          Date startTime, Date endTime, Date renewTill,
                          InetAddress[] clientAddresses) {
        if (sessionKey == null) {
            throw new IllegalArgumentException("Session key for ticket cannot be null");
        }
        if (asn1Encoding == null) {
            throw new IllegalArgumentException("ASN.1 encoding of ticket cannot be null");
        }
        if (client == null) {
            throw new IllegalArgumentException("Client name in ticket cannot be null");
        }
        if (server == null) {
            throw new IllegalArgumentException("Server name in ticket cannot be null");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("End time for ticket validity cannot be null");
        }
        this.asn1Encoding = asn1Encoding.clone();
        this.client = client;
        this.server = server;
        this.sessionKey = new EncryptionKey(sessionKey, keyType);
        if (flags == null) {
            this.flags = new boolean[NUM_FLAGS];
        } else if (flags.length >= NUM_FLAGS) {
            this.flags = flags.clone();
        } else {
            this.flags = new boolean[NUM_FLAGS];
            System.arraycopy(flags, 0, this.flags, 0, flags.length);
        }
        this.authTime = copy(authTime);
        this.startTime = copy(startTime);
        this.endTime = copy(endTime);
        this.renewTill = copy(renewTill);
        this.clientAddresses = clientAddresses == null ? null : clientAddresses.clone();
    }

    /** Para quien es; null si se destruyo. */
    public final KerberosPrincipal getClient() {
        return this.client;
    }

    /** Para que servicio; null si se destruyo. */
    public final KerberosPrincipal getServer() {
        return this.server;
    }

    /**
     * La clave de sesion. Un objeto nuevo cada vez, igual a los anteriores.
     *
     * @throws IllegalStateException si esta destruido
     */
    public final SecretKey getSessionKey() {
        checkAlive();
        return new EncryptionKey(this.sessionKey.getEncoded(), this.sessionKey.getKeyType());
    }

    /**
     * El tipo de la clave de sesion.
     *
     * @throws IllegalStateException si esta destruido
     */
    public final int getSessionKeyType() {
        checkAlive();
        return this.sessionKey.getKeyType();
    }

    /** Si se puede pedir con el un ticket reenviable a otro host. Falso si se destruyo. */
    public final boolean isForwardable() {
        return flag(FORWARDABLE_TICKET_FLAG);
    }

    /** Si se obtuvo reenviando otro. */
    public final boolean isForwarded() {
        return flag(FORWARDED_TICKET_FLAG);
    }

    /** Si se puede pedir con el un ticket para otro host. */
    public final boolean isProxiable() {
        return flag(PROXIABLE_TICKET_FLAG);
    }

    /** Si es para otro host. */
    public final boolean isProxy() {
        return flag(PROXY_TICKET_FLAG);
    }

    /** Si empieza a valer en el futuro. */
    public final boolean isPostdated() {
        return flag(POSTDATED_TICKET_FLAG);
    }

    /** Si se puede renovar. Es la bandera, no que haya hora limite de renovacion. */
    public final boolean isRenewable() {
        return flag(RENEWABLE_TICKET_FLAG);
    }

    /** Si se obtuvo con la contrasena y no con otro ticket. */
    public final boolean isInitial() {
        return flag(INITIAL_TICKET_FLAG);
    }

    /** Las banderas. Una copia; null si se destruyo. */
    public final boolean[] getFlags() {
        return this.flags == null ? null : this.flags.clone();
    }

    /** Cuando se autentico el cliente; null si no se sabe o se destruyo. */
    public final Date getAuthTime() {
        return copy(this.authTime);
    }

    /** Desde cuando vale; si no se dio, la hora de autenticacion. Null si se destruyo. */
    public final Date getStartTime() {
        return copy(this.startTime == null ? this.authTime : this.startTime);
    }

    /** Hasta cuando vale; null si se destruyo. */
    public final Date getEndTime() {
        return copy(this.endTime);
    }

    /** Hasta cuando se puede renovar; null si no es renovable o se destruyo. */
    public final Date getRenewTill() {
        return copy(this.renewTill);
    }

    /** Desde que direcciones se puede usar. Una copia; null si no esta restringido o se destruyo. */
    public final InetAddress[] getClientAddresses() {
        return this.clientAddresses == null ? null : this.clientAddresses.clone();
    }

    /**
     * Los bytes del ticket. Una copia.
     *
     * @throws IllegalStateException si esta destruido
     */
    public final byte[] getEncoded() {
        checkAlive();
        return this.asn1Encoding.clone();
    }

    /** Si no esta destruido y no vencio. Ver la nota de la clase. */
    @Override
    public boolean isCurrent() {
        return !this.destroyed && System.currentTimeMillis() <= this.endTime.getTime();
    }

    /**
     * Intenta renovarlo. Ver la nota de la clase: en KajiJDK siempre falla.
     *
     * @throws RefreshFailedException siempre
     */
    @Override
    public void refresh() throws RefreshFailedException {
        if (this.destroyed) {
            throw new RefreshFailedException("A destroyed ticket cannot be renewd.");
        }
        if (!isRenewable()) {
            throw new RefreshFailedException("This ticket is not renewable");
        }
        throw new RefreshFailedException("Failed to renew Kerberos Ticket for client " + this.client
            + " and server " + this.server + " - KajiJDK has no KDC client to renew it with");
    }

    /** Borra la clave y los bytes. Ver la nota de la clase. Destruir dos veces no hace nada. */
    @Override
    public void destroy() throws DestroyFailedException {
        if (!this.destroyed) {
            Arrays.fill(this.asn1Encoding, (byte) 0);
            this.asn1Encoding = null;
            this.client = null;
            this.server = null;
            this.sessionKey.destroy();
            this.sessionKey = null;
            this.flags = null;
            this.authTime = null;
            this.startTime = null;
            this.endTime = null;
            this.renewTill = null;
            this.clientAddresses = null;
            this.destroyed = true;
        }
    }

    /** Si ya se borro. */
    @Override
    public boolean isDestroyed() {
        return this.destroyed;
    }

    /** Un volcado legible: los bytes en hexadecimal, los principales, las banderas y las fechas. */
    @Override
    public String toString() {
        if (this.destroyed) {
            return "Destroyed KerberosTicket";
        }
        StringBuilder text = new StringBuilder();
        text.append("Ticket (hex) = \n").append(HexDump.dump(this.asn1Encoding)).append("\n");
        text.append("Client Principal = ").append(this.client).append("\n");
        text.append("Server Principal = ").append(this.server).append("\n");
        text.append("Session Key = ")
            .append(this.sessionKey.toString().substring("EncryptionKey: ".length())).append("\n");
        text.append("Forwardable Ticket ").append(isForwardable()).append("\n");
        text.append("Forwarded Ticket ").append(isForwarded()).append("\n");
        text.append("Proxiable Ticket ").append(isProxiable()).append("\n");
        text.append("Proxy Ticket ").append(isProxy()).append("\n");
        text.append("Postdated Ticket ").append(isPostdated()).append("\n");
        text.append("Renewable Ticket ").append(isRenewable()).append("\n");
        text.append("Initial Ticket ").append(isInitial()).append("\n");
        text.append("Auth Time = ").append(this.authTime).append("\n");
        text.append("Start Time = ").append(this.startTime).append("\n");
        text.append("End Time = ").append(this.endTime).append("\n");
        text.append("Renew Till = ").append(this.renewTill).append("\n");
        text.append("Client Addresses ");
        if (this.clientAddresses == null) {
            text.append(" Null ");
        } else {
            int i = 0;
            while (i < this.clientAddresses.length) {
                text.append("clientAddresses[").append(i).append("] = ")
                    .append(this.clientAddresses[i]);
                i = i + 1;
            }
            text.append("\n");
        }
        return text.toString();
    }

    /** Uno destruido vale 17. */
    @Override
    public int hashCode() {
        int result = 17;
        if (this.destroyed) {
            return result;
        }
        result = result * 37 + Arrays.hashCode(this.asn1Encoding);
        result = result * 37 + this.endTime.hashCode();
        result = result * 37 + this.client.hashCode();
        result = result * 37 + this.server.hashCode();
        result = result * 37 + this.sessionKey.hashCode();
        if (this.authTime != null) {
            result = result * 37 + this.authTime.hashCode();
        }
        if (this.startTime != null) {
            result = result * 37 + this.startTime.hashCode();
        }
        if (this.renewTill != null) {
            result = result * 37 + this.renewTill.hashCode();
        }
        // Sin direcciones el arreglo es null y su hash es cero, pero el paso se da igual.
        result = result * 37 + Arrays.hashCode(this.clientAddresses);
        return result * 37 + Arrays.hashCode(this.flags);
    }

    /** Iguales si todo coincide; uno destruido solo es igual a si mismo. */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof KerberosTicket)) {
            return false;
        }
        KerberosTicket that = (KerberosTicket) other;
        if (this.destroyed || that.destroyed) {
            return false;
        }
        if (!Arrays.equals(this.asn1Encoding, that.asn1Encoding)
            || !this.endTime.equals(that.endTime)
            || !this.client.equals(that.client)
            || !this.server.equals(that.server)
            || !this.sessionKey.equals(that.sessionKey)
            || !Arrays.equals(this.flags, that.flags)
            || !Arrays.equals(this.clientAddresses, that.clientAddresses)) {
            return false;
        }
        return sameDate(this.authTime, that.authTime) && sameDate(this.startTime, that.startTime)
            && sameDate(this.renewTill, that.renewTill);
    }

    /** La bandera numero {@code index}; falsa si se destruyo. */
    private boolean flag(int index) {
        return this.flags != null && this.flags[index];
    }

    /** Lanza si ya se destruyo. */
    private void checkAlive() {
        if (this.destroyed) {
            throw new IllegalStateException("This ticket is no longer valid");
        }
    }

    /** Una copia de la fecha, o null. */
    private static Date copy(Date date) {
        return date == null ? null : new Date(date.getTime());
    }

    /** Si dos fechas que pueden ser null son la misma. */
    private static boolean sameDate(Date a, Date b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
