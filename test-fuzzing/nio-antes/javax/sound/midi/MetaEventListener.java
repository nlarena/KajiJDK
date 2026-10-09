package javax.sound.midi;

import java.util.EventListener;

/**
 * KajiLibrary's javax.sound.midi.MetaEventListener -- tells when the sequencer goes through a meta
 * event.
 *
 * <p>Unlike {@link ControllerEventListener}, this one has no filter: they all arrive.
 *
 * <p>Its commonest use is detecting the <b>end of the piece</b>: the meta event of type 0x2F. It is
 * the only clean way of knowing that a sequencer finished, because {@code start()} returns right
 * away.
 *
 * <p>The notice arrives on the sequencer's thread. Blocking it throws playback off.
 */
public interface MetaEventListener extends EventListener {

    /** A meta event went by. */
    void meta(MetaMessage meta);
}
