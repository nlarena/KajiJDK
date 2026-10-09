package javax.swing;

/**
 * It marks a client property key as the look and feel's property.
 *
 * <h2>What an interface with no methods is for</h2>
 *
 * <p>A {@link JComponent}'s client properties are an open map: anybody puts in what they want.
 * The look and feel uses them too, and on changing the look and feel its own have to be cleaned
 * up without touching the program's. The only way of telling them apart is by the <em>key's
 * type</em>, and that is what this interface is for: a key that implements it belongs to the
 * look and feel and goes away with it.
 *
 * <p>Hence it has no methods. There is nothing to ask the key; knowing what type it is, is
 * enough.
 */
public interface UIClientPropertyKey {
}
