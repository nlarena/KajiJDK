package org.ietf.jgss;

import java.net.InetAddress;

/**
 * KajiLibrary's org.ietf.jgss.ChannelBinding -- ata la autenticacion al canal por donde viaja.
 *
 * <p>Es la defensa contra el intermediario que se limita a <b>reenviar</b>. Sin esto, alguien puede
 * dejarse en el medio, pasar los tokens de un lado al otro sin tocarlos, y quedarse con el canal:
 * los dos extremos se autentican correctamente entre si, pero cada uno esta hablando con el
 * intermediario.
 *
 * <p>Con esto, las dos partes meten en el intercambio una descripcion del canal --las direcciones,
 * y lo que la aplicacion quiera agregar-- y si no coinciden, la autenticacion falla con
 * {@link GSSException#BAD_BINDINGS}. El intermediario no puede hacerlas coincidir porque las
 * direcciones que ve cada extremo son las suyas.
 *
 * <p>Los datos de la aplicacion son lo mas util de los tres campos, y a la vez lo menos usado: ahi
 * es donde va, por ejemplo, el resumen del certificado TLS del canal. Es lo que ata la autenticacion
 * a <b>esa</b> conexion y no a cualquiera entre las mismas dos maquinas.
 *
 * <p>El objeto es inmutable: los tres campos se fijan al construir y no hay setters. Tiene sentido
 * para algo que participa de una decision de seguridad -- si se pudiera cambiar despues de pasarlo,
 * lo que se verifico no seria lo que se pidio.
 */
public class ChannelBinding {

    private final InetAddress initiator;

    private final InetAddress acceptor;

    private final byte[] appData;

    /**
     * Con las dos direcciones.
     *
     * @param initAddr la del que inicia, o null si no se quiere atar a ella
     * @param acceptAddr la del que acepta, o null
     * @param appData lo que la aplicacion quiera agregar, o null
     */
    public ChannelBinding(InetAddress initAddr, InetAddress acceptAddr, byte[] appData) {
        this.initiator = initAddr;
        this.acceptor = acceptAddr;
        this.appData = copyOf(appData);
    }

    /** Solo con los datos de la aplicacion. */
    public ChannelBinding(byte[] appData) {
        this(null, null, appData);
    }

    /** La direccion del que inicia, o null. */
    public InetAddress getInitiatorAddress() {
        return this.initiator;
    }

    /** La del que acepta, o null. */
    public InetAddress getAcceptorAddress() {
        return this.acceptor;
    }

    /** Los datos de la aplicacion, o null. Copia. */
    public byte[] getApplicationData() {
        return copyOf(this.appData);
    }

    /**
     * Iguales si coinciden las tres cosas.
     *
     * <p>Los datos se comparan byte a byte, no por identidad: dos etiquetas con el mismo contenido
     * atan al mismo canal, que es lo unico que importa aca.
     */
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ChannelBinding)) {
            return false;
        }
        ChannelBinding that = (ChannelBinding) other;
        if (!sameAddress(this.initiator, that.initiator)) {
            return false;
        }
        if (!sameAddress(this.acceptor, that.acceptor)) {
            return false;
        }
        if (this.appData == null || that.appData == null) {
            return this.appData == that.appData;
        }
        if (this.appData.length != that.appData.length) {
            return false;
        }
        int i = 0;
        while (i < this.appData.length) {
            if (this.appData[i] != that.appData[i]) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Coherente con {@link #equals}. */
    public int hashCode() {
        if (this.initiator != null) {
            return this.initiator.hashCode();
        }
        if (this.acceptor != null) {
            return this.acceptor.hashCode();
        }
        if (this.appData == null) {
            return 1;
        }
        int result = 1;
        int i = 0;
        while (i < this.appData.length) {
            result = 31 * result + this.appData[i];
            i = i + 1;
        }
        return result;
    }

    /** Dos direcciones que pueden ser null. */
    private static boolean sameAddress(InetAddress a, InetAddress b) {
        return (a == null) ? b == null : a.equals(b);
    }

    /** Copia defensiva de un arreglo que puede ser null. */
    private static byte[] copyOf(byte[] data) {
        if (data == null) {
            return null;
        }
        byte[] copy = new byte[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }
}
