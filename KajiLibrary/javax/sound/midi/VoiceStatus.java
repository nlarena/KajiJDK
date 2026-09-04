package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.VoiceStatus -- que esta haciendo una voz de un sintetizador.
 *
 * <p>Una <b>voz</b> es una nota sonando. Un sintetizador tiene un numero fijo --su polifonia-- y
 * cuando se acaban, la nota nueva le roba la voz a la mas vieja. Este objeto es la foto de una de
 * ellas.
 *
 * <p>Los seis campos son <b>publicos y mutables</b>, que es raro para el JDK. La razon es el
 * rendimiento: {@code Synthesizer.getVoiceStatus()} devuelve un arreglo entero y se lo llama muchas
 * veces por segundo para dibujar un medidor; con seis accesores por voz y sesenta y cuatro voces, el
 * costo se notaria.
 *
 * <p>{@link #active} decide todo: si es false, los otros cinco no significan nada y no hay que
 * leerlos.
 */
public class VoiceStatus {

    /** Si esta sonando. Ver la nota de la clase: si es false, lo demas no vale. */
    public boolean active = false;

    /** En que canal MIDI. */
    public int channel = 0;

    /** De que banco salio el sonido. */
    public int bank = 0;

    /** Que programa. */
    public int program = 0;

    /** Que nota, de 0 a 127. */
    public int note = 0;

    /** Con que fuerza. */
    public int volume = 0;

    /** Todo en cero y sin sonar. */
    public VoiceStatus() {
    }
}
