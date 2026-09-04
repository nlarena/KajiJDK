package javax.sound.midi;

/**
 * KajiLibrary's javax.sound.midi.MidiChannel -- uno de los dieciseis canales de un sintetizador.
 *
 * <p>Es la API de conveniencia: en lugar de armar un {@link ShortMessage} y mandarlo por un
 * {@link Receiver}, se llama a un metodo. Hace exactamente lo mismo.
 *
 * <h2>Las notas se apagan de dos formas</h2>
 *
 * <p>{@link #noteOff(int, int)} lleva la velocidad de <b>soltado</b> --con que rapidez se levanta el
 * dedo--, que algunos instrumentos usan para cambiar como se apaga el sonido. {@link #noteOff(int)}
 * es el atajo cuando eso no importa.
 *
 * <h2>{@link #allNotesOff} y {@link #allSoundOff} no son lo mismo</h2>
 *
 * <p>El primero suelta las notas: lo que este sonando se apaga como se apagaria naturalmente, con su
 * resonancia. El segundo <b>corta</b> el sonido de inmediato.
 *
 * <p>Para un boton de panico se quiere el segundo. El primero deja sonando una nota con pedal.
 *
 * <h2>Mute y solo son del secuenciador, no del sonido</h2>
 *
 * <p>{@link #setMute} y {@link #setSolo} no tocan el volumen: le dicen al secuenciador que no mande
 * --o que mande solo-- los eventos de este canal. Y son opcionales: un sintetizador que no los soporte
 * los ignora y {@link #getMute} sigue devolviendo false.
 *
 * <h2>{@link #localControl}</h2>
 *
 * <p>Apagado, el teclado deja de tocar su propio sintetizador. Es lo que se hace al secuenciar: si no,
 * cada nota suena dos veces --una por el teclado y otra por el eco del secuenciador-- con un retardo
 * audible.
 */
public interface MidiChannel {

    /** Toca una nota. Velocidad 0 la apaga; ver {@link ShortMessage}. */
    void noteOn(int noteNumber, int velocity);

    /** La suelta, con velocidad de soltado. Ver la nota de la clase. */
    void noteOff(int noteNumber, int velocity);

    /** La suelta. */
    void noteOff(int noteNumber);

    /** Presion sobre una tecla ya pulsada. */
    void setPolyPressure(int noteNumber, int pressure);

    /** Cuanta presion tiene esa tecla. */
    int getPolyPressure(int noteNumber);

    /** Presion sobre el canal entero. */
    void setChannelPressure(int pressure);

    /** Cuanta presion tiene el canal. */
    int getChannelPressure();

    /** Mueve un controlador. */
    void controlChange(int controller, int value);

    /** En cuanto esta ese controlador. */
    int getController(int controller);

    /** Cambia el sonido dentro del banco actual. */
    void programChange(int program);

    /** Cambia de banco y de sonido. */
    void programChange(int bank, int program);

    /** Que sonido esta puesto. */
    int getProgram();

    /** Mueve la rueda de tono; 8192 es el centro. */
    void setPitchBend(int bend);

    /** Donde esta la rueda de tono. */
    int getPitchBend();

    /** Vuelve todos los controladores a su valor de omision. */
    void resetAllControllers();

    /** Suelta todas las notas. Ver la nota de la clase. */
    void allNotesOff();

    /** Corta todo el sonido de inmediato. Ver la nota de la clase. */
    void allSoundOff();

    /**
     * Conecta o desconecta el teclado de su propio sintetizador. Ver la nota de la clase.
     *
     * @return como quedo; puede no ser lo que se pidio
     */
    boolean localControl(boolean on);

    /** Modo monofonico: una sola nota a la vez. */
    void setMono(boolean on);

    /** Si esta en monofonico. */
    boolean getMono();

    /** Modo omni: responder a todos los canales. */
    void setOmni(boolean on);

    /** Si esta en omni. */
    boolean getOmni();

    /** Silencia este canal en el secuenciador. Ver la nota de la clase. */
    void setMute(boolean mute);

    /** Si esta silenciado. */
    boolean getMute();

    /** Deja sonar solo este canal. Ver la nota de la clase. */
    void setSolo(boolean soloState);

    /** Si esta en solo. */
    boolean getSolo();
}
