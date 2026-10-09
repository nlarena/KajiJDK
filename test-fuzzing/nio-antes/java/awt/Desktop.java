package java.awt;

import java.awt.desktop.AboutHandler;
import java.awt.desktop.OpenFilesHandler;
import java.awt.desktop.OpenURIHandler;
import java.awt.desktop.PreferencesHandler;
import java.awt.desktop.PrintFilesHandler;
import java.awt.desktop.QuitHandler;
import java.awt.desktop.QuitStrategy;
import java.awt.desktop.SystemEventListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * The desktop: opening a file with the program it belongs to, sending mail, going to a page.
 *
 * <p>The idea is to delegate. Instead of shipping a PDF viewer, the program asks the desktop to
 * open the file and the system decides with what. The same goes for `mailto:` and for web
 * addresses.
 *
 * <p>As in {@link Taskbar}, each thing is supported or not on its own and has to be asked about
 * with {@link #isSupported} before being used. And as in {@link SystemTray}, the instance is unique
 * and is asked for with {@link #getDesktop}.
 *
 * <p><strong>Here there is no desktop</strong>: {@link #isDesktopSupported} gives `false` and
 * {@link #getDesktop} throws {@link HeadlessException}, which is what the JDK does without a
 * screen. The instance methods are declared because they are part of the class, but no instance
 * exists to call them on.
 *
 * <p><strong>{@link #setDefaultMenuBar} is the only method of `java.awt` whose signature names a
 * Swing type outside the accessibility classes</strong> —the accessible context of
 * {@link TextComponent} returns a {@code javax.swing.text.AttributeSet}— and this library ships no
 * Swing. To be able to declare it there are two classes put in as placeholders
 * —{@link javax.swing.JComponent} and {@link javax.swing.JMenuBar}—, which announce themselves as
 * what they are: a name with the right hierarchy and no members. The method is written in full all
 * the same, because here it throws just like the other twenty-one.
 */
public class Desktop {

    /** Each thing a desktop may know how to do. */
    public static enum Action {

        /** Open a file with the program it belongs to. */
        OPEN,

        /** Open it for editing. */
        EDIT,

        /** To print it. */
        PRINT,

        /** Open the mail program. */
        MAIL,

        /** Open an address in the browser. */
        BROWSE,

        /** Report when the program comes to the foreground. */
        APP_EVENT_FOREGROUND,

        /** Report when it is hidden. */
        APP_EVENT_HIDDEN,

        /** Report when it is opened again. */
        APP_EVENT_REOPENED,

        /** Report when the screen goes to sleep. */
        APP_EVENT_SCREEN_SLEEP,

        /** Report when the system goes to sleep. */
        APP_EVENT_SYSTEM_SLEEP,

        /** Report when the user session changes. */
        APP_EVENT_USER_SESSION,

        /** Handle the "About" entry of the system menu. */
        APP_ABOUT,

        /** Handle the "Preferences" entry. */
        APP_PREFERENCES,

        /** Handle the request to open files made from the desktop. */
        APP_OPEN_FILE,

        /** Handle the request to print files. */
        APP_PRINT_FILE,

        /** Handle the request to open an address. */
        APP_OPEN_URI,

        /** Handle the request to quit. */
        APP_QUIT_HANDLER,

        /** Choose how quitting happens. */
        APP_QUIT_STRATEGY,

        /** Let the system kill the program without warning. */
        APP_SUDDEN_TERMINATION,

        /** Ask to come to the foreground. */
        APP_REQUEST_FOREGROUND,

        /** Open the program's help. */
        APP_HELP_VIEWER,

        /** Put the program's menu bar in the system's. */
        APP_MENU_BAR,

        /** Open the directory of a file and leave it selected. */
        BROWSE_FILE_DIR,

        /** Send a file to the trash. */
        MOVE_TO_TRASH
    }

    /** The only desktop, if it ever gets asked for. */
    private static Desktop instance;

    /** Not instantiated from outside. */
    private Desktop() {
    }

    /**
     * The desktop of this session.
     *
     * @throws HeadlessException always here: with no screen there is no desktop
     * @throws UnsupportedOperationException if there is a screen but the desktop cannot be driven
     */
    public static synchronized Desktop getDesktop() {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        if (!isDesktopSupported()) {
            throw new UnsupportedOperationException("Desktop API is not supported on the current platform");
        }
        if (instance == null) {
            instance = new Desktop();
        }
        return instance;
    }

    /**
     * Whether this platform has a desktop that can be driven.
     *
     * @return `false` always
     */
    public static boolean isDesktopSupported() {
        return false;
    }

    /**
     * Whether it supports that action.
     *
     * @return `false` for all of them
     * @throws NullPointerException if the action is `null`
     */
    public boolean isSupported(Action action) {
        if (action == null) {
            throw new NullPointerException("action");
        }
        return false;
    }

    /**
     * Opens the file with the program it belongs to.
     *
     * @throws NullPointerException if the file is `null`
     * @throws IllegalArgumentException if the file does not exist
     * @throws UnsupportedOperationException if {@link Action#OPEN} is not supported
     * @throws IOException if there is no program associated with it or it failed to start
     */
    public void open(File file) throws IOException {
        this.checkFile(file);
        this.require(Action.OPEN);
    }

    /**
     * Opens it for editing.
     *
     * @throws UnsupportedOperationException if {@link Action#EDIT} is not supported
     * @throws IOException if there is no editor associated with it
     */
    public void edit(File file) throws IOException {
        this.checkFile(file);
        this.require(Action.EDIT);
    }

    /**
     * Prints it.
     *
     * @throws UnsupportedOperationException if {@link Action#PRINT} is not supported
     * @throws IOException if there is no program that knows how to print it
     */
    public void print(File file) throws IOException {
        this.checkFile(file);
        this.require(Action.PRINT);
    }

    /**
     * Opens that address in the browser.
     *
     * @throws NullPointerException if the address is `null`
     * @throws UnsupportedOperationException if {@link Action#BROWSE} is not supported
     * @throws IOException if the browser did not start
     */
    public void browse(URI uri) throws IOException {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        this.require(Action.BROWSE);
    }

    /**
     * Opens the mail program with a blank message.
     *
     * @throws UnsupportedOperationException if {@link Action#MAIL} is not supported
     * @throws IOException if it did not start
     */
    public void mail() throws IOException {
        this.require(Action.MAIL);
    }

    /**
     * Opens the mail program with whatever that `mailto:` address says.
     *
     * @throws NullPointerException if the address is `null`
     * @throws IllegalArgumentException if the scheme is not `mailto`
     * @throws UnsupportedOperationException if {@link Action#MAIL} is not supported
     * @throws IOException if it did not start
     */
    public void mail(URI mailtoURI) throws IOException {
        if (mailtoURI == null) {
            throw new NullPointerException("mailtoURI");
        }
        if (!"mailto".equalsIgnoreCase(mailtoURI.getScheme())) {
            throw new IllegalArgumentException("URI scheme is not \"mailto\"");
        }
        this.require(Action.MAIL);
    }

    /**
     * Opens the directory of the file and leaves it selected.
     *
     * <p>It is what "Show in folder" does: it does not open the file, it shows where it is.
     *
     * @throws UnsupportedOperationException if {@link Action#BROWSE_FILE_DIR} is not supported
     */
    public void browseFileDirectory(File file) {
        this.checkFile(file);
        this.require(Action.BROWSE_FILE_DIR);
    }

    /**
     * Sends the file to the trash.
     *
     * <p>It is different from deleting it: it can be recovered.
     *
     * @return `true` if it made it to the trash
     * @throws UnsupportedOperationException if {@link Action#MOVE_TO_TRASH} is not supported
     */
    public boolean moveToTrash(File file) {
        this.checkFile(file);
        this.require(Action.MOVE_TO_TRASH);
        return false;
    }

    /**
     * Registers a listener of system events.
     *
     * <p>A `null` listener is ignored, which is what the JDK does: registering nothing is doing
     * nothing.
     *
     * @throws UnsupportedOperationException if the desktop does not support that kind of event
     */
    public void addAppEventListener(SystemEventListener listener) {
        if (listener == null) {
            return;
        }
        throw new UnsupportedOperationException("The current platform doesn't support this event");
    }

    /**
     * Removes a listener of system events.
     *
     * @throws UnsupportedOperationException if the desktop does not support that kind of event
     */
    public void removeAppEventListener(SystemEventListener listener) {
        if (listener == null) {
            return;
        }
        throw new UnsupportedOperationException("The current platform doesn't support this event");
    }

    /**
     * Who handles the "About" entry of the system menu.
     *
     * @param aboutHandler the handler, or `null` to go back to the default one
     * @throws UnsupportedOperationException if {@link Action#APP_ABOUT} is not supported
     */
    public void setAboutHandler(AboutHandler aboutHandler) {
        this.require(Action.APP_ABOUT);
    }

    /**
     * Who handles "Preferences".
     *
     * <p>Passing `null` **hides the entry** of the menu, which is different from leaving it doing
     * nothing.
     *
     * @throws UnsupportedOperationException if {@link Action#APP_PREFERENCES} is not supported
     */
    public void setPreferencesHandler(PreferencesHandler preferencesHandler) {
        this.require(Action.APP_PREFERENCES);
    }

    /**
     * Puts that menu bar in the system's.
     *
     * <p>It is a macOS thing: the program's menu bar goes in the strip at the top of the screen,
     * outside the window. On the other systems it is never supported, and here it is not either.
     *
     * <p>The {@link javax.swing.JMenuBar} it takes is a **placeholder** of this library, not
     * Swing's: it is enough to declare the method, which is all that is needed, because the method
     * throws before looking at it.
     *
     * @throws UnsupportedOperationException if {@link Action#APP_MENU_BAR} is not supported
     */
    public void setDefaultMenuBar(javax.swing.JMenuBar menuBar) {
        this.require(Action.APP_MENU_BAR);
    }

    /**
     * Who handles the request to open files made from the desktop.
     *
     * @throws UnsupportedOperationException if {@link Action#APP_OPEN_FILE} is not supported
     */
    public void setOpenFileHandler(OpenFilesHandler openFileHandler) {
        this.require(Action.APP_OPEN_FILE);
    }

    /**
     * Who handles the request to print files.
     *
     * @throws UnsupportedOperationException if {@link Action#APP_PRINT_FILE} is not supported
     */
    public void setPrintFileHandler(PrintFilesHandler printFileHandler) {
        this.require(Action.APP_PRINT_FILE);
    }

    /**
     * Who handles the request to open an address.
     *
     * @throws UnsupportedOperationException if {@link Action#APP_OPEN_URI} is not supported
     */
    public void setOpenURIHandler(OpenURIHandler openURIHandler) {
        this.require(Action.APP_OPEN_URI);
    }

    /**
     * Who handles the request to quit.
     *
     * <p>The handler receives a response and **has to answer it**: until it answers, the system
     * waits. It is what makes it possible to ask "shall I save the changes?" before closing.
     *
     * @throws UnsupportedOperationException if {@link Action#APP_QUIT_HANDLER} is not supported
     */
    public void setQuitHandler(QuitHandler quitHandler) {
        this.require(Action.APP_QUIT_HANDLER);
    }

    /**
     * Chooses how the program quits.
     *
     * @throws UnsupportedOperationException if {@link Action#APP_QUIT_STRATEGY} is not supported
     */
    public void setQuitStrategy(QuitStrategy strategy) {
        this.require(Action.APP_QUIT_STRATEGY);
    }

    /**
     * Lets the system kill the program without warning.
     *
     * <p>It serves to speed up the shutdown: if the program has nothing to save, there is no need
     * to give it the chance to refuse.
     *
     * @throws UnsupportedOperationException if {@link Action#APP_SUDDEN_TERMINATION} is not
     *     supported
     */
    public void enableSuddenTermination() {
        this.require(Action.APP_SUDDEN_TERMINATION);
    }

    /**
     * Demands again that it be warned before being killed.
     *
     * @throws UnsupportedOperationException if {@link Action#APP_SUDDEN_TERMINATION} is not
     *     supported
     */
    public void disableSuddenTermination() {
        this.require(Action.APP_SUDDEN_TERMINATION);
    }

    /**
     * Asks to come to the foreground.
     *
     * @param allWindows whether to bring every window or only the front one
     * @throws UnsupportedOperationException if {@link Action#APP_REQUEST_FOREGROUND} is not
     *     supported
     */
    public void requestForeground(boolean allWindows) {
        this.require(Action.APP_REQUEST_FOREGROUND);
    }

    /**
     * Opens the program's help.
     *
     * @throws UnsupportedOperationException if {@link Action#APP_HELP_VIEWER} is not supported
     */
    public void openHelpViewer() {
        this.require(Action.APP_HELP_VIEWER);
    }

    /** Throws if that action is not supported. */
    private void require(Action a) {
        if (!this.isSupported(a)) {
            throw new UnsupportedOperationException("The " + a.name()
                    + " action is not supported on the current platform!");
        }
    }

    /** That the file exists. */
    private void checkFile(File file) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("The file: " + file.getPath() + " doesn't exist.");
        }
    }
}
