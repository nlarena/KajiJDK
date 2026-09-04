package javax.sound.midi;

import java.util.List;

/**
 * KajiLibrary's javax.sound.midi.MidiDevice -- un dispositivo MIDI.
 *
 * <p>Un puerto fisico, un sintetizador por software, un secuenciador. Todos se abren, se cierran, y
 * reparten receptores y transmisores.
 *
 * <h2>Receptor y transmisor, desde el dispositivo</h2>
 *
 * <p>Es la misma inversion de nombres que en el audio muestreado, y por la misma razon:
 *
 * <ul>
 *   <li>un {@link Receiver} del dispositivo es donde el programa <b>escribe</b>;
 *   <li>un {@link Transmitter} del dispositivo es de donde el programa <b>lee</b>.
 * </ul>
 *
 * <p>Un puerto de <b>salida</b> MIDI provee receptores; uno de <b>entrada</b> provee transmisores. Un
 * dispositivo que devuelve 0 en {@link #getMaxReceivers} no acepta que le manden nada.
 *
 * <h2>{@link #getMaxReceivers} puede devolver -1</h2>
 *
 * <p>Significa "sin limite", no "ninguno". Confundirlo lleva a codigo que se niega a usar un
 * dispositivo perfectamente bueno.
 *
 * <h2>Se cierra cuando se cierra su ultimo receptor</h2>
 *
 * <p>Un dispositivo que se abrio implicitamente --al pedirle un receptor sin haberlo abierto-- se
 * cierra solo cuando se cierra el ultimo. Uno abierto a mano con {@link #open} no: ese hay que
 * cerrarlo a mano.
 */
public interface MidiDevice extends AutoCloseable {

    /** Como se llama. */
    MidiDevice.Info getDeviceInfo();

    /**
     * Reserva el recurso del sistema.
     *
     * @throws MidiUnavailableException si esta ocupado
     */
    void open() throws MidiUnavailableException;

    /** Lo libera, y cierra todo lo que haya repartido. */
    void close();

    /** Si esta abierto. */
    boolean isOpen();

    /** El reloj del dispositivo, en microsegundos, o -1 si no lleva. */
    long getMicrosecondPosition();

    /** Cuantos receptores puede dar a la vez; -1 es sin limite. Ver la nota de la clase. */
    int getMaxReceivers();

    /** Cuantos transmisores puede dar a la vez; -1 es sin limite. */
    int getMaxTransmitters();

    /**
     * Un receptor nuevo.
     *
     * @throws MidiUnavailableException si no puede dar mas
     */
    Receiver getReceiver() throws MidiUnavailableException;

    /** Los que ya dio y siguen abiertos. */
    List<Receiver> getReceivers();

    /**
     * Un transmisor nuevo.
     *
     * @throws MidiUnavailableException si no puede dar mas
     */
    Transmitter getTransmitter() throws MidiUnavailableException;

    /** Los que ya dio y siguen abiertos. */
    List<Transmitter> getTransmitters();

    /**
     * Como se llama un dispositivo.
     *
     * <p>Cuatro cadenas para mostrar. La igualdad es por <b>identidad</b>: dos dispositivos con el
     * mismo nombre siguen siendo dos.
     *
     * <p>{@link #toString} devuelve solo el nombre, no las cuatro cosas.
     */
    class Info {

        /** El nombre. */
        private final String name;

        /** Quien lo hizo. */
        private final String vendor;

        /** Que es. */
        private final String description;

        /** Que version. */
        private final String version;

        /** Protegido: estos datos los define quien implementa el dispositivo. */
        protected Info(String name, String vendor, String description, String version) {
            this.name = name;
            this.vendor = vendor;
            this.description = description;
            this.version = version;
        }

        /** Por identidad. Ver la nota de la clase. */
        @Override
        public final boolean equals(Object obj) {
            return super.equals(obj);
        }

        /** El de identidad. */
        @Override
        public final int hashCode() {
            return super.hashCode();
        }

        /** El nombre. */
        public final String getName() {
            return this.name;
        }

        /** Quien lo hizo. */
        public final String getVendor() {
            return this.vendor;
        }

        /** Que es. */
        public final String getDescription() {
            return this.description;
        }

        /** Que version. */
        public final String getVersion() {
            return this.version;
        }

        /** Solo el nombre. Ver la nota de la clase. */
        @Override
        public final String toString() {
            return this.name;
        }
    }
}
