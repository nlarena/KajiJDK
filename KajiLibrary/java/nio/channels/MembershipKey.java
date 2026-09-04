package java.nio.channels;

import java.io.IOException;
import java.net.InetAddress;

/**
 * KajiLibrary's java.nio.channels.MembershipKey — el comprobante de estar en un grupo de
 * multidifusion.
 *
 * <p>Es lo que devuelve un `join` y lo unico con lo que despues se puede dar de baja
 * ({@link #drop()}). Que la baja se pida por la llave y no por la direccion no es capricho: un mismo
 * canal puede estar en el mismo grupo por dos placas distintas, y sin la llave no habria forma de
 * decir cual de las dos se quiere soltar.
 *
 * <p>{@link #block} y {@link #unblock} filtran **emisores** dentro del grupo. Sirven para el caso
 * feo y frecuente: un grupo donde alguien inunda, y uno quiere seguir escuchando a los demas. El
 * filtro es del sistema, no del programa, asi que el trafico bloqueado ni siquiera sube.
 *
 * <p>{@code networkInterface()} faltaba porque devuelve `java.net.NetworkInterface`, que no existia
 * en este arbol. Ya existe, y el metodo tambien; es el dato que completa la llave, porque la misma
 * direccion de grupo por dos placas distintas son dos membresias distintas.
 */
public abstract class MembershipKey {

    protected MembershipKey() {
    }

    /** Si la membresia sigue vigente. Deja de estarlo al darla de baja o al cerrar el canal. */
    public abstract boolean isValid();

    /**
     * Da de baja la membresia.
     *
     * <p>Sobre una llave ya invalida no hace nada, y esa idempotencia es a proposito: la baja
     * tambien ocurre sola al cerrar el canal, asi que el `drop()` explicito y el cierre se pisan
     * seguido y ninguno de los dos tiene que fallar por eso.
     */
    public abstract void drop();

    /**
     * Deja de recibir lo que mande `source` dentro de este grupo.
     *
     * @throws IllegalStateException si la membresia se pidio para una fuente especifica: filtrar
     *         dentro de un grupo que ya esta filtrado a un solo emisor no significa nada
     */
    public abstract MembershipKey block(InetAddress source) throws IOException;

    /** Deshace un {@link #block}. */
    public abstract MembershipKey unblock(InetAddress source);

    /** El canal de esta membresia. */
    public abstract MulticastChannel channel();

    /** La direccion del grupo. */
    public abstract InetAddress group();

    /** La fuente, si la membresia se pidio para una sola; `null` si es para todo el grupo. */
    public abstract InetAddress sourceAddress();

    /**
     * La placa por la que se pidio la membresia.
     *
     * <p>Es la mitad que le falta a {@link #group()} para identificar la membresia: el mismo grupo
     * por dos placas son dos llaves, y esta es la que dice cual es cual.
     */
    public abstract java.net.NetworkInterface networkInterface();
}
