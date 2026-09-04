package java.nio.channels;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * El comprobante que devuelve {@link KajiDatagramChannel#join}.
 *
 * <p>Guarda las tres cosas que identifican una membresia --grupo, placa y emisor-- porque las tres
 * hacen falta para distinguirla: el mismo grupo por dos placas son dos membresias, y darlas de baja
 * por direccion en vez de por llave no podria decir cual de las dos.
 *
 * <h2>{@code block} y {@code unblock} no estan sostenidos, y se dice</h2>
 *
 * <p>Filtrar emisores dentro de un grupo es un filtro **del sistema**, no del programa: su gracia es
 * que el trafico bloqueado ni siquiera sube. La costura de esta VM no sabe pedirlo, y hacerlo en Java
 * --descartando los paquetes despues de recibirlos-- cumpliria la firma y no la promesa: el trafico
 * seguiria subiendo, el ancho de banda seguiria gastado, y quien uso `block` para defenderse de una
 * inundacion no estaria defendido. Tiran {@link UnsupportedOperationException}, que es lo que el
 * contrato prevé para una pila que no filtra por emisor.
 */
final class KajiMembershipKey extends MembershipKey {

    private final KajiDatagramChannel canal;
    private final InetAddress grupo;
    private final NetworkInterface placa;
    private final InetAddress emisor;

    /** Como se le nombro la placa a la costura; hace falta para dar de baja por el mismo camino. */
    private final String nombreDePlaca;

    private boolean vigente = true;

    KajiMembershipKey(KajiDatagramChannel canal, InetAddress grupo, NetworkInterface placa,
            InetAddress emisor, String nombreDePlaca) {
        this.canal = canal;
        this.grupo = grupo;
        this.placa = placa;
        this.emisor = emisor;
        this.nombreDePlaca = nombreDePlaca;
    }

    public boolean isValid() {
        return this.vigente;
    }

    public void drop() {
        // Sobre una llave ya invalida no hace nada, y esa idempotencia es a proposito: la baja
        // tambien ocurre sola al cerrar el canal, asi que el `drop()` explicito y el cierre se pisan
        // todo el tiempo en cualquier programa que limpie bien.
        if (!this.vigente) {
            return;
        }
        this.vigente = false;
        this.canal.soltar(this);
    }

    public MembershipKey block(InetAddress source) throws IOException {
        throw new UnsupportedOperationException("source filtering not supported");
    }

    public MembershipKey unblock(InetAddress source) {
        throw new UnsupportedOperationException("source filtering not supported");
    }

    public MulticastChannel channel() {
        return this.canal;
    }

    public InetAddress group() {
        return this.grupo;
    }

    public InetAddress sourceAddress() {
        return this.emisor;
    }

    public NetworkInterface networkInterface() {
        return this.placa;
    }

    // ---- lo que necesita el canal -------------------------------------------------------------

    String placa() {
        return this.nombreDePlaca;
    }

    boolean mismaPlaca(NetworkInterface otra) {
        return this.placa == null ? otra == null : this.placa.equals(otra);
    }

    // La invalida sin dar de baja nada: la usa el cierre del canal, que ya cierra el socket y con
    // eso el sistema suelta todas las membresias de una.
    void invalidar() {
        this.vigente = false;
    }
}
