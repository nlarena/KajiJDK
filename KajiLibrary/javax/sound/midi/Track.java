package javax.sound.midi;

import java.util.ArrayList;

/**
 * KajiLibrary's javax.sound.midi.Track -- una pista de eventos MIDI ordenados por tiempo.
 *
 * <p>No se construye: se pide con {@code Sequence.createTrack()}. Una pista suelta no tendria contra
 * que medir sus pulsos.
 *
 * <h2>Siempre tiene el fin de pista</h2>
 *
 * <p>Una pista recien creada ya tiene <b>un</b> evento: el meta mensaje de tipo 0x2F, fin de pista, en
 * el pulso 0. Por eso {@code size()} de una pista vacia devuelve 1, que sorprende la primera vez.
 *
 * <p>La pista lo mantiene sola: al agregar un evento mas tarde, el fin de pista se corre para quedar
 * siempre ultimo. Es obligatorio en el formato de archivo, y dejarlo en manos de quien usa la API
 * seria pedir archivos rotos.
 *
 * <h2>Se ordena al insertar</h2>
 *
 * <p>{@link #add} pone el evento en su lugar por pulso, no al final. Los eventos con el mismo pulso
 * quedan en el orden en que se agregaron.
 *
 * <p>Y el mismo objeto {@link MidiEvent} no se puede agregar dos veces: el segundo intento devuelve
 * false. Es identidad, no igualdad -- dos eventos distintos con el mismo contenido si entran los dos.
 */
public final class Track {

    /** Los eventos, ordenados por pulso. */
    private final ArrayList<MidiEvent> events = new ArrayList<MidiEvent>();

    /** El fin de pista, que siempre esta y siempre es el ultimo. */
    private final MidiEvent endOfTrack;

    /** De acceso de paquete: solo {@link Sequence} crea pistas. */
    Track() {
        this.endOfTrack = new ImmutableEndOfTrack();
        this.events.add(this.endOfTrack);
    }

    /**
     * Agrega un evento en su lugar.
     *
     * <p>Ver la nota de la clase: se ordena por pulso y el fin de pista queda ultimo.
     *
     * @return si se agrego; false si es null o si ese mismo objeto ya estaba
     */
    public boolean add(MidiEvent event) {
        if (event == null) {
            return false;
        }
        synchronized (this.events) {
            if (indexOfIdentity(event) >= 0) {
                return false;
            }
            long tick = event.getTick();
            if (tick > this.endOfTrack.getTick()) {
                this.endOfTrack.setTick(tick);
            }
            int at = this.events.size();
            // Se busca desde el final: lo habitual es agregar en orden, y asi eso cuesta una
            // comparacion en lugar de recorrer la pista entera.
            while (at > 0 && this.events.get(at - 1).getTick() > tick) {
                at = at - 1;
            }
            // El fin de pista se queda ultimo aunque comparta el pulso. Se comprueba que siga ahi:
            // se lo puede sacar con remove(), y en ese caso no hay nada que preservar.
            if (at == this.events.size() && at > 0
                && this.events.get(at - 1) == this.endOfTrack) {
                at = at - 1;
            }
            this.events.add(at, event);
            return true;
        }
    }

    /**
     * Saca un evento.
     *
     * @return si estaba
     */
    public boolean remove(MidiEvent event) {
        if (event == null) {
            return false;
        }
        synchronized (this.events) {
            int at = indexOfIdentity(event);
            if (at < 0) {
                return false;
            }
            this.events.remove(at);
            return true;
        }
    }

    /**
     * El evento numero {@code index}.
     *
     * @throws ArrayIndexOutOfBoundsException si no existe
     */
    public MidiEvent get(int index) throws ArrayIndexOutOfBoundsException {
        synchronized (this.events) {
            if (index < 0 || index >= this.events.size()) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            return this.events.get(index);
        }
    }

    /** Cuantos eventos hay, contando el fin de pista. Ver la nota de la clase. */
    public int size() {
        synchronized (this.events) {
            return this.events.size();
        }
    }

    /** En que pulso termina. */
    public long ticks() {
        synchronized (this.events) {
            if (this.events.isEmpty()) {
                return 0;
            }
            return this.events.get(this.events.size() - 1).getTick();
        }
    }

    /** Donde esta ese objeto exacto, o -1. Por identidad; ver la nota de la clase. */
    private int indexOfIdentity(MidiEvent event) {
        int i = 0;
        while (i < this.events.size()) {
            if (this.events.get(i) == event) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }

    /**
     * El evento de fin de pista.
     *
     * <p>De acceso de paquete. Es un {@link MidiEvent} normal salvo por una cosa: su mensaje no se
     * puede cambiar. Si alguien pudiera reescribirlo, la pista dejaria de tener fin y el archivo que
     * saliera de ella no seria valido.
     */
    private static final class ImmutableEndOfTrack extends MidiEvent {

        ImmutableEndOfTrack() {
            super(new EndOfTrackMessage(), 0);
        }
    }

    /** El meta mensaje 0x2F, sin datos, que no se deja modificar. */
    private static final class EndOfTrackMessage extends MetaMessage {

        EndOfTrackMessage() {
            super(new byte[] { (byte) MetaMessage.META, 0x2F, 0 });
        }

        /**
         * No hace nada.
         *
         * <p>Ignorar en silencio es lo que hace el JDK. Lanzar seria mas honesto y romperia codigo que
         * recorre una pista reescribiendo mensajes, que es justamente cuando esto se toca sin querer.
         */
        @Override
        public void setMessage(int type, byte[] data, int length) {
        }

        /** Una copia normal, ya modificable. */
        @Override
        public Object clone() {
            return new MetaMessage(getMessage());
        }
    }
}
