package javax.sound.midi;

import java.util.EventListener;

/**
 * KajiLibrary's javax.sound.midi.ControllerEventListener -- tells when the sequencer goes through a
 * controller change.
 *
 * <p>It is registered with {@code Sequencer.addControllerEventListener}, which also receives
 * <b>which controllers</b> are of interest. Without that filter a file with automation generates
 * hundreds of events per second.
 *
 * <p>That method returns the array of the ones that <b>actually</b> got registered, which can be
 * smaller than the one asked for. It has to be looked at: asking for a controller the sequencer
 * does not follow does not fail, it simply never arrives.
 *
 * <p>The notice arrives on the sequencer's thread. Blocking it throws playback off.
 */
public interface ControllerEventListener extends EventListener {

    /** A controller change among the ones asked for went by. */
    void controlChange(ShortMessage event);
}
