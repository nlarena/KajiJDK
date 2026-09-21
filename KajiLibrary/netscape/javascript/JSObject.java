package netscape.javascript;

/**
 * A JavaScript object, seen from Java.
 *
 * <p>The whole class is <strong>abstract</strong> and that is what one has to understand about it:
 * it does not represent data but a <em>reference to something that lives on the other side</em>.
 * Whoever implements it is the bridge of the browser, which knows how to talk to its JavaScript
 * engine; from Java the only thing one sees is that a name in text is resolved over there and an
 * {@link Object} comes back.
 *
 * <p>Hence every signature returns {@code Object} and every one can throw {@link JSException}:
 * JavaScript does not have the types of Java, so what comes back is only known at run time, and a
 * name that does not exist over there is an error that cannot be foreseen here.
 *
 * <p>The distinction between <em>member</em> and <em>slot</em> is the same one JavaScript makes
 * between a named property and an array index.
 *
 * @deprecated the applet model, which is the only thing that instantiated this, has been deprecated
 *     since Java 9 and marked for removal since 17.
 */
@Deprecated(since = "9", forRemoval = true)
public abstract class JSObject {

    /**
     * For the subclasses of the bridge.
     *
     * <p>It is {@code protected} and not public because nobody manufactures a {@code JSObject}: one
     * receives one the engine already had.
     */
    protected JSObject() {
    }

    /** It calls the method {@code methodName} of this object with those arguments. */
    public abstract Object call(String methodName, Object... args) throws JSException;

    /** It evaluates {@code s} as JavaScript code, in the context of this object. */
    public abstract Object eval(String s) throws JSException;

    /** The value of the property {@code name}. */
    public abstract Object getMember(String name) throws JSException;

    /** It sets {@code value} on the property {@code name}, creating it if it was not there. */
    public abstract void setMember(String name, Object value) throws JSException;

    /** It deletes the property {@code name}. */
    public abstract void removeMember(String name) throws JSException;

    /** The value at the index {@code index}, for the objects JavaScript treats as arrays. */
    public abstract Object getSlot(int index) throws JSException;

    /** It sets {@code value} at the index {@code index}. */
    public abstract void setSlot(int index, Object value) throws JSException;
}
