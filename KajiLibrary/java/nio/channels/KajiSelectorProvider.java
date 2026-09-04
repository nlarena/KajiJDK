package java.nio.channels;

import java.io.IOException;
import java.net.ProtocolFamily;
import java.net.StandardProtocolFamily;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ServiceConfigurationError;

/**
 * El proveedor con el que esta biblioteca fabrica sus canales de red.
 *
 * <p>Existe desde que la VM tiene costura de red; antes no habia con que fabricar ninguno.
 *
 * <h2>Como se elige entre este y uno instalado</h2>
 *
 * <p>Los {@code open()} de {@link SocketChannel}, {@link ServerSocketChannel} y
 * {@link DatagramChannel} preguntan primero por {@link SelectorProvider#provider()}: si alguien
 * instalo el suyo --por la propiedad de sistema o por {@code META-INF/services}-- ese gana, que es lo
 * que el contrato de `spi` promete y la unica razon por la que ese mecanismo existe. Solo cuando no
 * hay ninguno se usa este.
 *
 * <p>El orden importa y no es simetrico a proposito: un proveedor instalado reemplaza al de la casa,
 * nunca al reves.
 *
 * <h2>Lo que este proveedor no puede dar, y por que lo dice en vez de fingirlo</h2>
 *
 * <p>{@link #openSelector()} y {@link #openPipe()} tiran {@link UnsupportedOperationException}.
 *
 * <p>Un selector multiplexa: espera sobre **muchos** canales a la vez y despierta con los que estan
 * listos. Los canales de esta biblioteca saben decir "todavia no" --de ahi que el modo no bloqueante
 * ande-- pero no hay forma de **esperar por varios sin quemar el procesador**: eso necesita un
 * `select`/`poll` del sistema, y la costura no lo expone. Un selector escrito sobre sondeos cumpliria
 * la firma y gastaria un nucleo entero, y quien lo use para atender mil conexiones --que es para lo
 * que existe-- quedaria peor que con un hilo por conexion.
 *
 * <p>Un pipe, en esta API, son dos canales seleccionables unidos; sin selector no tiene sentido
 * ofrecerlo, y ademas necesitaria una tuberia anonima que la VM tampoco expone.
 *
 * <p>Que estos dos tiren no es lo mismo que faltar: son metodos **abstractos** que esta clase esta
 * obligada a tener, y el contrato de `SelectorProvider` ya prevé un proveedor que no sostenga todo.
 * Los que un programa llamaria --`Selector.open()` y `Pipe.open()`-- **no estan declarados** en esta
 * biblioteca, justamente para que ese programa no compile en vez de fallar corriendo.
 */
final class KajiSelectorProvider extends SelectorProvider {

    /** El de la casa. Uno solo: un proveedor no tiene estado. */
    private static final KajiSelectorProvider PROPIO = new KajiSelectorProvider();

    private KajiSelectorProvider() {
    }

    /**
     * El proveedor que corresponde usar: el instalado si lo hay, el de la casa si no.
     *
     * <p>El {@link ServiceConfigurationError} que tira {@code provider()} cuando no hay ninguno no es
     * un fallo: es la ausencia de configuracion, y esta clase es la respuesta a esa ausencia. Ese
     * metodo no cachea el fallo, asi que instalar un proveedor mas tarde sigue funcionando.
     */
    static SelectorProvider actual() {
        try {
            return SelectorProvider.provider();
        } catch (ServiceConfigurationError e) {
            return PROPIO;
        }
    }

    // Las familias que la costura sabe abrir. `INET6` no queda afuera por capricho: el nativo ata y
    // conecta por nombre, y quien elige la familia de esa direccion es el resolutor del sistema, no
    // esta clase. Pedir IPv6 explicitamente seria prometer algo que no se puede garantizar.
    private static void exigirFamiliaConocida(ProtocolFamily family) {
        if (family == null) {
            throw new NullPointerException("family");
        }
        if (!StandardProtocolFamily.INET.equals(family)) {
            throw new UnsupportedOperationException("Protocol family not supported: " + family);
        }
    }

    public DatagramChannel openDatagramChannel() throws IOException {
        return new KajiDatagramChannel(this);
    }

    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
        KajiSelectorProvider.exigirFamiliaConocida(family);
        return new KajiDatagramChannel(this);
    }

    public ServerSocketChannel openServerSocketChannel() throws IOException {
        return new KajiServerSocketChannel(this);
    }

    public ServerSocketChannel openServerSocketChannel(ProtocolFamily family) throws IOException {
        KajiSelectorProvider.exigirFamiliaConocida(family);
        return new KajiServerSocketChannel(this);
    }

    public SocketChannel openSocketChannel() throws IOException {
        return new KajiSocketChannel(this);
    }

    public SocketChannel openSocketChannel(ProtocolFamily family) throws IOException {
        KajiSelectorProvider.exigirFamiliaConocida(family);
        return new KajiSocketChannel(this);
    }

    public Pipe openPipe() throws IOException {
        throw new UnsupportedOperationException("no hay pipes en esta VM: ver la nota de la clase");
    }

    public AbstractSelector openSelector() throws IOException {
        throw new UnsupportedOperationException(
                "no hay selector en esta VM: ver la nota de la clase");
    }
}
