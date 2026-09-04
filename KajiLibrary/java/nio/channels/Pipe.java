package java.nio.channels;

import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.SelectorProvider;

/**
 * KajiLibrary's java.nio.channels.Pipe — dos canales unidos, uno que escribe y otro que lee.
 *
 * <p>Es el unico canal selectable que no habla con el mundo: lo que entra por el
 * {@link SinkChannel sumidero} sale por la {@link SourceChannel fuente}, dentro del mismo proceso.
 * Su razon de ser es poder **despertar un selector desde otro hilo**: se registra la fuente, y
 * escribir un byte en el sumidero hace que el `select` vuelva. Es como se implementa
 * {@link Selector#wakeup()} en varias plataformas.
 *
 * <h2>Por que tampoco hay `Pipe.open()`</h2>
 *
 * <p>Aca la razon <strong>no</strong> es la de los canales de red. Un pipe se podria implementar
 * entero en memoria --una cola de bytes entre las dos puntas-- sin tocar el sistema. Lo que lo
 * impide es otra cosa: las dos puntas son {@link AbstractSelectableChannel}, y un canal selectable
 * solo sirve si hay un {@link Selector} donde registrarlo. Sin selectores, un pipe se reduce a una
 * cola de bytes con una interfaz mucho mas cara que la de una cola de bytes, y su unico proposito
 * --despertar a un selector-- no existe.
 *
 * <p>Peor: {@link AbstractSelectableChannel#register} exige un {@link Selector} y le pide la llave.
 * Un pipe fabricado aca compilaria, andaria para leer y escribir, y <strong>tiraria en el momento de
 * registrarlo</strong>, que es justo para lo que uno lo pidio. Eso es la definicion de un metodo que
 * miente, asi que {@code open()} no esta.
 *
 * <p>La clase y sus dos anidadas si estan, con sus tipos y su jerarquia correctos. Cuando esta VM
 * tenga selectores, esto se completa con un {@code open()} y nada de lo de arriba cambia.
 */
public abstract class Pipe {

    protected Pipe() {
    }

    /** La punta por la que se lee. */
    public abstract SourceChannel source();

    /** La punta por la que se escribe. */
    public abstract SinkChannel sink();

    /**
     * La punta de lectura de un pipe.
     *
     * <p>Es una clase y no una interfaz --tambien en el JDK-- porque tiene que heredar toda la
     * maquinaria de canal selectable; lo unico que agrega es fijar {@link #validOps()} en lectura.
     */
    public abstract static class SourceChannel extends AbstractSelectableChannel
            implements ReadableByteChannel, ScatteringByteChannel {

        protected SourceChannel(SelectorProvider provider) {
            super(provider);
        }

        /** Solo lectura: por esta punta no se escribe nunca. */
        public final int validOps() {
            return SelectionKey.OP_READ;
        }
    }

    /** La punta de escritura de un pipe. */
    public abstract static class SinkChannel extends AbstractSelectableChannel
            implements WritableByteChannel, GatheringByteChannel {

        protected SinkChannel(SelectorProvider provider) {
            super(provider);
        }

        /** Solo escritura. */
        public final int validOps() {
            return SelectionKey.OP_WRITE;
        }
    }
}
