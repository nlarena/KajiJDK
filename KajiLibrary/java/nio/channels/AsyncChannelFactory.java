package java.nio.channels;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import java.nio.channels.spi.AsynchronousChannelProvider;

/**
 * Fabrica de los canales asincronicos, para el provider que vive en {@code java.nio.channels.spi}.
 *
 * <p>No es una clase del JDK: es andamiaje nuestro, del mismo tipo que {@link FabricaMapMode}, que
 * vive en este mismo paquete y por la misma razon. Las implementaciones --el group y los tres
 * canales-- son de paquete a proposito: nadie de afuera las construye, y hacerlas publicas
 * agregaria a {@code java.nio.channels} nombres que el JDK no tiene. Pero el provider esta en
 * {@code java.nio.channels.spi}, que es otro paquete y no las ve.
 *
 * <p>Este es el unico puente entre los dos, y por eso lo unico publico de mas que hay: cuatro
 * metodos que devuelven tipos del JDK.
 */
public final class AsyncChannelFactory {

    private AsyncChannelFactory() {
    }

    /**
     * Un group sobre ese pool.
     *
     * @param provider quien lo fabrica
     * @param pool donde corren los manejadores
     * @param ownsPool si el pool lo armo el group y no el que llama
     * @return el group
     */
    public static AsynchronousChannelGroup group(AsynchronousChannelProvider provider,
            ExecutorService pool, boolean ownsPool) {
        return new AsyncChannelGroup(provider, pool, ownsPool);
    }

    /**
     * Un channel de socket asincronico de ese group.
     *
     * @param provider quien lo fabrica
     * @param group a que group pertenece
     * @return el channel
     * @throws IOException si no se puede abrir el socket de abajo
     */
    public static AsynchronousSocketChannel socket(AsynchronousChannelProvider provider,
            AsynchronousChannelGroup group) throws IOException {
        return new AsyncSocketChannelImpl(provider, SocketChannel.open(), (AsyncChannelGroup) group);
    }

    /**
     * Un channel servidor asincronico de ese group.
     *
     * @param provider quien lo fabrica
     * @param group a que group pertenece
     * @return el channel
     * @throws IOException si no se puede abrir el socket de abajo
     */
    public static AsynchronousServerSocketChannel serverSocket(AsynchronousChannelProvider provider,
            AsynchronousChannelGroup group) throws IOException {
        return new AsyncServerSocketChannelImpl(provider, ServerSocketChannel.open(), (AsyncChannelGroup) group);
    }

    /**
     * Un channel de archivo asincronico de ese group.
     *
     * @param channel el channel bloqueante de abajo
     * @param group a que group pertenece
     * @return el channel
     */
    public static AsynchronousFileChannel file(FileChannel channel,
            AsynchronousChannelGroup group) {
        return new AsyncFileChannelImpl((KajiFileChannel) channel, (AsyncChannelGroup) group);
    }

    /**
     * The built-in selector provider, without consulting the installed one.
     *
     * <p>Only {@code SelectorProvider.provider()} uses it, as its last tier: asking for the
     * installed provider from in here would be asking itself.
     *
     * @return the provider, which is a singleton
     */
    public static java.nio.channels.spi.SelectorProvider selectorProvider() {
        return KajiSelectorProvider.builtin();
    }
}
