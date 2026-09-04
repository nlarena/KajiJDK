import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.spi.AsynchronousChannelProvider;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

// El equivalente de SpiProvSel para los canales asincronicos. Ver la nota de alla sobre por que es
// publico y con constructor publico.
public class SpiProvAsy extends AsynchronousChannelProvider {

    public SpiProvAsy() {
    }

    public AsynchronousChannelGroup openAsynchronousChannelGroup(int nThreads,
            ThreadFactory threadFactory) throws IOException {
        return null;
    }

    public AsynchronousChannelGroup openAsynchronousChannelGroup(ExecutorService executor,
            int initialSize) throws IOException {
        return null;
    }

    public AsynchronousServerSocketChannel openAsynchronousServerSocketChannel(
            AsynchronousChannelGroup group) throws IOException {
        return null;
    }

    public AsynchronousSocketChannel openAsynchronousSocketChannel(AsynchronousChannelGroup group)
            throws IOException {
        return null;
    }
}
