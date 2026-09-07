package jdk.jshell.spi;

/**
 * The mark `jshell` leaves in a snippet that uses something which does not exist yet.
 *
 * <p>It is the piece of the trick that makes a console usable top-down: in `jshell` you can write a
 * method calling another one you are going to define later, and the snippet **compiles**. Since it
 * compiles, if what is missing were not represented somehow the `.class` would not be valid; so
 * `jshell` generates in its place a body that throws this, with an identifier saying **which** of
 * the pending references was touched.
 *
 * <p>The engine recognizes it by its class name, takes its {@link #id()} out and hands it back to
 * `jshell` as an {@link ExecutionControl.ResolutionException}, which is the one `jshell` knows how
 * to translate into "you have not defined such and such yet".
 *
 * <p>It is thrown by the generated code, not by the engine: nobody should build it by hand.
 *
 * @since 9
 */
public class SPIResolutionException extends RuntimeException {

    private final int id;

    /**
     * With that pending-reference identifier.
     *
     * @param id the identifier `jshell` assigned to what is missing
     */
    public SPIResolutionException(int id) {
        super("resolution exception " + id);
        this.id = id;
    }

    /** The pending reference's identifier. */
    public int id() {
        return this.id;
    }
}
