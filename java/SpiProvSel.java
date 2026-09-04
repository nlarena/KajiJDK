import java.io.IOException;
import java.net.ProtocolFamily;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Pipe;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

// Proveedor de juguete para SpiProvTest. Publico y con constructor publico sin argumentos porque
// es lo que `SelectorProvider.provider()` exige del escalon de la propiedad de sistema (y lo que la
// JVM real exige tambien: usa `getConstructor()`, que solo mira los publicos).
//
// NO redefine las dos sobrecargas concretas con ProtocolFamily: los chequeos 1 y 2 miran
// justamente la omision heredada.
public class SpiProvSel extends SelectorProvider {

    // Cual de las dos sobrecargas de openDatagramChannel corrio, y con que familia.
    public int ultima;
    public String familia;

    public SpiProvSel() {
    }

    public DatagramChannel openDatagramChannel() throws IOException {
        this.ultima = 1;
        this.familia = null;
        return null;
    }

    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
        this.ultima = 2;
        this.familia = family == null ? null : family.name();
        return null;
    }

    public Pipe openPipe() throws IOException {
        return null;
    }

    public AbstractSelector openSelector() throws IOException {
        return null;
    }

    public ServerSocketChannel openServerSocketChannel() throws IOException {
        return null;
    }

    public SocketChannel openSocketChannel() throws IOException {
        return null;
    }
}
