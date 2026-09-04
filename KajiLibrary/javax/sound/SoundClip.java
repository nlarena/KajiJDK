package javax.sound;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * KajiLibrary's javax.sound.SoundClip -- reproducir un sonido corto, sin mas.
 *
 * <p>Llego en Java 22 y es la respuesta a un reclamo viejo: reproducir un archivo de sonido con las
 * APIs de {@code javax.sound.sampled} pide abrir una linea, elegir un formato, escribir bytes y
 * cerrar todo. Esto son dos lineas.
 *
 * <h2>Todo lo que puede fallar, falla en la fabrica</h2>
 *
 * <p>{@link #createSoundClip} es lo unico que lanza. Los cinco metodos de instancia no declaran nada
 * y no fallan: si el archivo no se puede reproducir, {@link #canPlay} devuelve false y
 * {@link #play} no hace nada.
 *
 * <p>Es un diseno deliberado, y conviene notarlo: <b>crear el clip no garantiza que suene</b>. Un
 * archivo de texto produce un {@code SoundClip} perfectamente valido con {@code canPlay()} en false.
 * La fabrica solo se queja si el archivo no se puede <b>leer</b>.
 *
 * <h2>{@link #play} vuelve enseguida</h2>
 *
 * <p>La reproduccion sigue en otro hilo. {@link #loop} repite hasta que alguien llame {@link #stop},
 * y llamar {@code play} sobre un clip que ya suena lo reinicia desde el principio.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no tiene salida de audio: ni decodificadores ni acceso a la placa de sonido.
 * {@link #createSoundClip} funciona de verdad --abre el archivo y lanza {@link IOException} si no se
 * puede leer, igual que el JDK-- y el clip que devuelve tiene {@code canPlay()} en false.
 *
 * <p>Eso no es una excusa: es <b>exactamente</b> lo que hace el JDK con un archivo que no sabe
 * decodificar, y esta comprobado contra el JDK 25. Un programa escrito contra esta clase se comporta
 * igual de los dos lados; lo unico que cambia es que aca ningun archivo se puede decodificar.
 */
public final class SoundClip {

    /** Si se pudo decodificar; aca nunca. Ver la nota de la clase. */
    private final boolean playable;

    /** Se llega por {@link #createSoundClip}. */
    private SoundClip(boolean playable) {
        this.playable = playable;
    }

    /**
     * Un clip a partir de ese archivo.
     *
     * <p>Lee el archivo para comprobar que se puede. Que el resultado suene o no se pregunta despues
     * con {@link #canPlay}; ver la nota de la clase.
     *
     * @throws NullPointerException si el archivo es null
     * @throws IOException si no se puede leer
     */
    public static SoundClip createSoundClip(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("file must not be null");
        }
        // Se abre de verdad: es lo que convierte "el archivo no existe" en la IOException que el
        // contrato promete, y no en un clip mudo que engana.
        InputStream in = new FileInputStream(file);
        try {
            in.read();
        } finally {
            in.close();
        }
        return new SoundClip(false);
    }

    /** Si este clip se puede reproducir. Ver la nota de la clase. */
    public boolean canPlay() {
        return this.playable;
    }

    /** Si esta sonando ahora. */
    public boolean isPlaying() {
        return false;
    }

    /** Lo reproduce desde el principio. No hace nada si {@link #canPlay} es false. */
    public void play() {
    }

    /** Lo repite hasta {@link #stop}. No hace nada si {@link #canPlay} es false. */
    public void loop() {
    }

    /** Lo detiene. No hace nada si no estaba sonando. */
    public void stop() {
    }
}
