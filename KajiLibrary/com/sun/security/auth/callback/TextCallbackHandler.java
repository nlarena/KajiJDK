package com.sun.security.auth.callback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * The callback handler that talks over the terminal.
 *
 * <p>JAAS separates <em>which</em> datum is needed from <em>how</em> it is asked for: a login
 * module says that it is missing a user name by building a {@link NameCallback}, and it
 * neither knows nor cares whether that ends in a graphical dialogue, in a configuration file
 * or -- as here -- in a text prompt. This class is the simplest possible implementation of
 * that side: it writes the prompt and reads a line.
 *
 * <p>It recognizes four callbacks and <strong>rejects the rest</strong> with
 * {@link UnsupportedCallbackException}, which is exactly the contract: a handler is not
 * obliged to know how to answer everything, and saying "I do not know how to ask for this" is
 * a valid answer and one that may be told from having failed.
 *
 * <h2>A limitation that is worth knowing</h2>
 *
 * <p>{@link PasswordCallback#isEchoOn} asks that the password should <em>not</em> be shown
 * while it is typed. Turning the echo off is not Java's business but the terminal's -- the JDK
 * does it through {@link System#console()}, which gives access to the device's raw mode -- and
 * this VM does not expose that console. It is read all the same, over {@link System#in}, and
 * <strong>the password is seen</strong>. It is said here and not hidden: a handler that
 * promises not to show it and shows it is worse than one that says so.
 */
public class TextCallbackHandler implements CallbackHandler {

    /** A new handler. It has no state: everything it needs arrives in each {@link #handle}. */
    public TextCallbackHandler() {
    }

    /**
     * It attends to each callback of the array, in order.
     *
     * @throws UnsupportedCallbackException with the first one it does not know how to attend to --
     *     and with <em>that</em> callback inside, so that whoever called may see which it was
     */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        for (int i = 0; i < callbacks.length; i++) {
            Callback c = callbacks[i];
            if (c instanceof TextOutputCallback) {
                show((TextOutputCallback) c);
            } else if (c instanceof NameCallback) {
                askName((NameCallback) c, input);
            } else if (c instanceof PasswordCallback) {
                askPassword((PasswordCallback) c, input);
            } else if (c instanceof ConfirmationCallback) {
                confirm((ConfirmationCallback) c, input);
            } else {
                throw new UnsupportedCallbackException(c);
            }
        }
    }

    private void show(TextOutputCallback c) throws IOException {
        // The unknown type is an `IllegalArgumentException` and not an
                // `UnsupportedCallbackException`: the callback is supported, what is wrong is its
                // contents. Confusing them would tell whoever called to try with another handler,
                // when the problem is theirs. This would like to be a `switch`, and it is not
                // because of finding #461: the constant folding that runs over the labels of a
                // `case` only looks at the types of the current compilation unit, so
                // `TextOutputCallback.INFORMATION` -- a constant of another file -- is rejected as
                // if it were not a constant. Outside a label the same constant folds well, which is
                // precisely what this chain does.
        int type = c.getMessageType();
        if (type == TextOutputCallback.INFORMATION) {
            System.err.println(c.getMessage());
        } else if (type == TextOutputCallback.WARNING) {
            System.err.println("Warning: " + c.getMessage());
        } else if (type == TextOutputCallback.ERROR) {
            System.err.println("Error: " + c.getMessage());
        } else {
            throw new IllegalArgumentException("unknown message type: "
                    + String.valueOf(type));
        }
    }

    private void askName(NameCallback c, BufferedReader input) throws IOException {
        String defaultName = c.getDefaultName();
        if (defaultName == null) {
            System.err.print(c.getPrompt() + " ");
        } else {
            System.err.print(c.getPrompt() + " [" + defaultName + "] ");
        }
        System.err.flush();
        String line = input.readLine();
        // An empty line means "leave me the one that was already there", not "my name is the
                // empty string".
        if (line == null || line.isEmpty()) {
            c.setName(defaultName);
        } else {
            c.setName(line);
        }
    }

    private void askPassword(PasswordCallback c, BufferedReader input) throws IOException {
        System.err.print(c.getPrompt() + " ");
        System.err.flush();
        String line = input.readLine();
        if (line == null) {
            c.setPassword(null);
            return;
        }
        c.setPassword(line.toCharArray());
    }

    private void confirm(ConfirmationCallback c, BufferedReader input) throws IOException {
        String[] options = c.getOptions();
        boolean ownOptions = options != null;
        if (!ownOptions) {
            options = optionsOf(c.getOptionType());
        }
        if (c.getPrompt() != null) {
            System.err.println(c.getPrompt());
        }
        for (int i = 0; i < options.length; i++) {
            System.err.println(String.valueOf(i) + ". " + options[i]);
        }
        System.err.print("Choose [" + String.valueOf(c.getDefaultOption()) + "] ");
        System.err.flush();

        String line = input.readLine();
        int chosen = c.getDefaultOption();
        if (line != null && !line.isEmpty()) {
            try {
                chosen = Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                chosen = c.getDefaultOption();
            }
        }
        if (chosen < 0 || chosen >= options.length) {
            chosen = c.getDefaultOption();
        }
        // With one's own options the index IS the answer. With the predefined ones it has to be
                // translated: in `YES_NO_OPTION` position 0 of the list is `YES`, which is worth 0
                // -- but in `OK_CANCEL_OPTION` position 0 is `OK`, which is worth 3. Returning the
                // raw index there would be answering `YES` when the user said `OK`.
        if (ownOptions) {
            c.setSelectedIndex(chosen);
        } else {
            c.setSelectedIndex(valuesOf(c.getOptionType())[chosen]);
        }
    }

    private String[] optionsOf(int type) {
        // A chain of `if` and not a `switch`: see #461, the same as in `show`.
        if (type == ConfirmationCallback.YES_NO_OPTION) {
            return new String[] { "Yes", "No" };
        }
        if (type == ConfirmationCallback.YES_NO_CANCEL_OPTION) {
            return new String[] { "Yes", "No", "Cancel" };
        }
        if (type == ConfirmationCallback.OK_CANCEL_OPTION) {
            return new String[] { "OK", "Cancel" };
        }
        throw new IllegalArgumentException("unknown option type: " + String.valueOf(type));
    }

    private int[] valuesOf(int type) {
        if (type == ConfirmationCallback.YES_NO_OPTION) {
            return new int[] { ConfirmationCallback.YES, ConfirmationCallback.NO };
        }
        if (type == ConfirmationCallback.YES_NO_CANCEL_OPTION) {
            return new int[] { ConfirmationCallback.YES, ConfirmationCallback.NO,
                    ConfirmationCallback.CANCEL };
        }
        if (type == ConfirmationCallback.OK_CANCEL_OPTION) {
            return new int[] { ConfirmationCallback.OK, ConfirmationCallback.CANCEL };
        }
        throw new IllegalArgumentException("unknown option type: " + String.valueOf(type));
    }
}
