package java.rmi.dgc;

import java.io.Serializable;
import java.rmi.server.UID;
import java.security.SecureRandom;

/**
 * Un identificador de maquina virtual, unico entre todas las VM.
 *
 * <p>Un {@link UID} solo es unico **dentro** de su VM: dos VM arrancadas a la vez pueden emitir el
 * mismo. El recolector distribuido necesita distinguir clientes que viven en procesos distintos, y
 * por eso `VMID` le pega adelante al `UID` un bloque de bytes aleatorio fijado una sola vez por
 * proceso: dos VM coinciden solo si coinciden en los ocho bytes **y** en el `UID`.
 *
 * <p>El JDK uso durante anios la direccion de red de la maquina en vez del bloque aleatorio. Lo
 * cambio porque filtraba la IP del host en un objeto serializado que viaja por la red, y porque
 * ocho bytes de un generador criptografico chocan menos que una IP que se repite en cada NAT.
 * Nosotros hacemos lo mismo, por las mismas dos razones.
 *
 * @see java.rmi.dgc.DGC
 */
public final class VMID implements Serializable {

    private static final long serialVersionUID = -538642295484486218L;

    /**
     * Los ocho bytes del proceso. Se sacan una sola vez: si salieran por instancia, dos `VMID` de
     * la misma VM se verian como de VM distintas, que es exactamente lo contrario de para lo que
     * existe la clase.
     */
    private static final byte[] randomBytes;

    static {
        byte[] bytes = new byte[8];
        new SecureRandom().nextBytes(bytes);
        randomBytes = bytes;
    }

    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /** Los bytes del proceso; el nombre viene de cuando eran la direccion de red. */
    private byte[] addr;

    /** El identificador dentro de esta VM. */
    private UID uid;

    /** Un identificador nuevo, distinto de todos los de esta VM y (casi seguro) de los de otras. */
    public VMID() {
        this.addr = randomBytes;
        this.uid = new UID();
    }

    /**
     * Si este `VMID` es unico entre todas las VM.
     *
     * @deprecated Siempre da `true`. Existia para avisar cuando el identificador salia de una
     *     direccion de red que no se habia podido averiguar; desde que sale de un generador
     *     criptografico no hay caso en que no sea unico, y el metodo no tiene nada que decir.
     * @return `true`, siempre
     */
    @Deprecated
    public static boolean isUnique() {
        return true;
    }

    public int hashCode() {
        return this.uid.hashCode();
    }

    /**
     * Si el otro es un `VMID` con el mismo bloque de proceso y el mismo {@link UID}.
     *
     * <p>Los dos tienen que coincidir: el `UID` solo por si mismo se repite entre VM, y el bloque
     * de proceso solo por si mismo no distingue dos objetos de la misma VM.
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VMID)) {
            return false;
        }
        VMID otro = (VMID) obj;
        if (!this.uid.equals(otro.uid)) {
            return false;
        }
        if (this.addr == null || otro.addr == null) {
            return this.addr == otro.addr;
        }
        if (this.addr.length != otro.addr.length) {
            return false;
        }
        for (int i = 0; i < this.addr.length; i++) {
            if (this.addr[i] != otro.addr[i]) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.addr != null) {
            for (int i = 0; i < this.addr.length; i++) {
                int b = this.addr[i] & 0xFF;
                sb.append(HEX[b >> 4]).append(HEX[b & 0xF]);
            }
        }
        sb.append(':').append(this.uid);
        return sb.toString();
    }
}
