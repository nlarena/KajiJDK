package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MidiDeviceReceiver -- un receptor que sabe de que dispositivo es.
 *
 * <p>Agrega un metodo sobre {@link Receiver}. Existe porque un programa que maneja varios dispositivos
 * termina con una pila de receptores sueltos y sin forma de saber cual es de cual.
 *
 * <p>Se consulta con {@code instanceof}: {@code MidiDevice.getReceiver()} declara {@link Receiver} a
 * secas, aunque en la practica todos los del JDK devuelven uno de estos.
 */
public interface MidiDeviceReceiver extends Receiver {

    /** De que dispositivo es. */
    MidiDevice getMidiDevice();
}
