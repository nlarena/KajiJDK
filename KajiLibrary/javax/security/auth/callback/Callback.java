package javax.security.auth.callback;

/**
 * KajiLibrary's javax.security.auth.callback.Callback -- a request for data, without saying how it
 * is asked for.
 *
 * <p>The interface is <b>empty</b>, and that is its whole design. A login module needs the user's
 * name and password, but cannot know whether the program using it is a desktop application, a
 * server without a terminal or an automated test. Instead of choosing for it, it builds a {@code
 * NameCallback} and a {@code PasswordCallback}, passes them to a {@link CallbackHandler} the
 * program gave it, and reads the answers.
 *
 * <p>From there comes the only rule that matters: <b>the one who asks does not choose the
 * medium</b>. A module that wrote to {@code System.console()} directly would be useless on a
 * server; one that builds callbacks works in the three cases without changing a line.
 */
public interface Callback {
}
