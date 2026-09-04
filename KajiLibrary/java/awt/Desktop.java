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
 * El escritorio: abrir un archivo con el programa que le corresponde, mandar un mail, ir a una
 * página.
 *
 * <p>La idea es delegar. En vez de traer un visor de PDF, el programa le pide al escritorio que abra
 * el archivo y el sistema decide con qué. Lo mismo con `mailto:` y con las direcciones web.
 *
 * <p>Como {@link Taskbar}, cada cosa se admite o no por separado y hay que preguntar con
 * {@link #isSupported} antes de usarla. Y como en {@link SystemTray}, la instancia es única y se pide
 * con {@link #getDesktop}.
 *
 * <p><strong>Acá no hay escritorio</strong>: {@link #isDesktopSupported} da `false` y
 * {@link #getDesktop} tira {@link HeadlessException}, que es lo que hace el JDK sin pantalla. Los
 * métodos de instancia están declarados porque son parte de la clase, pero no existe ninguna
 * instancia desde la que llamarlos.
 *
 * <p><strong>{@link #setDefaultMenuBar} es el único método de todo `java.awt` cuya firma nombra un
 * tipo de Swing</strong>, y esta biblioteca no trae Swing. Para poder declararlo hay dos clases
 * puestas como lugar reservado —{@link javax.swing.JComponent} y {@link javax.swing.JMenuBar}—, que
 * se anuncian como lo que son: un nombre con la jerarquía correcta y ningún miembro. El método está
 * entero de todas formas, porque acá tira igual que los otros veinticuatro.
 */
public class Desktop {

    /** Cada cosa que un escritorio puede saber hacer. */
    public static enum Action {

        /** Abrir un archivo con el programa que le corresponde. */
        OPEN,

        /** Abrirlo para editarlo. */
        EDIT,

        /** Imprimirlo. */
        PRINT,

        /** Abrir el programa de correo. */
        MAIL,

        /** Abrir una dirección en el navegador. */
        BROWSE,

        /** Avisar cuando el programa pasa a primer plano. */
        APP_EVENT_FOREGROUND,

        /** Avisar cuando se lo esconde. */
        APP_EVENT_HIDDEN,

        /** Avisar cuando se lo vuelve a abrir. */
        APP_EVENT_REOPENED,

        /** Avisar cuando la pantalla se duerme. */
        APP_EVENT_SCREEN_SLEEP,

        /** Avisar cuando el sistema se duerme. */
        APP_EVENT_SYSTEM_SLEEP,

        /** Avisar cuando cambia la sesión del usuario. */
        APP_EVENT_USER_SESSION,

        /** Atender la entrada "Acerca de" del menú del sistema. */
        APP_ABOUT,

        /** Atender la entrada "Preferencias". */
        APP_PREFERENCES,

        /** Atender el pedido de abrir archivos hecho desde el escritorio. */
        APP_OPEN_FILE,

        /** Atender el pedido de imprimir archivos. */
        APP_PRINT_FILE,

        /** Atender el pedido de abrir una dirección. */
        APP_OPEN_URI,

        /** Atender el pedido de salir. */
        APP_QUIT_HANDLER,

        /** Elegir cómo se sale. */
        APP_QUIT_STRATEGY,

        /** Permitir que el sistema mate el programa sin avisar. */
        APP_SUDDEN_TERMINATION,

        /** Pedir pasar a primer plano. */
        APP_REQUEST_FOREGROUND,

        /** Abrir la ayuda del programa. */
        APP_HELP_VIEWER,

        /** Poner la barra de menú del programa en la del sistema. */
        APP_MENU_BAR,

        /** Abrir el directorio de un archivo y dejarlo seleccionado. */
        BROWSE_FILE_DIR,

        /** Mandar un archivo a la papelera. */
        MOVE_TO_TRASH
    }

    /** El único escritorio, si alguna vez se llega a pedir. */
    private static Desktop unico;

    /** No se instancia desde afuera. */
    private Desktop() {
    }

    /**
     * El escritorio de esta sesión.
     *
     * @throws HeadlessException siempre acá: sin pantalla no hay escritorio
     * @throws UnsupportedOperationException si hay pantalla pero el escritorio no se puede manejar
     */
    public static synchronized Desktop getDesktop() {
        if (GraphicsEnvironment.isHeadless()) {
            throw new HeadlessException();
        }
        if (!isDesktopSupported()) {
            throw new UnsupportedOperationException("Desktop API is not supported on the current platform");
        }
        if (unico == null) {
            unico = new Desktop();
        }
        return unico;
    }

    /**
     * Si esta plataforma tiene un escritorio manejable.
     *
     * @return `false` siempre
     */
    public static boolean isDesktopSupported() {
        return false;
    }

    /**
     * Si admite esa acción.
     *
     * @return `false` para todas
     * @throws NullPointerException si la acción es `null`
     */
    public boolean isSupported(Action action) {
        if (action == null) {
            throw new NullPointerException("action");
        }
        return false;
    }

    /**
     * Abre el archivo con el programa que le corresponde.
     *
     * @throws NullPointerException si el archivo es `null`
     * @throws IllegalArgumentException si el archivo no existe
     * @throws UnsupportedOperationException si no se admite {@link Action#OPEN}
     * @throws IOException si no hay ningún programa asociado o si falló al arrancar
     */
    public void open(File file) throws IOException {
        this.comprobarArchivo(file);
        this.exigir(Action.OPEN);
    }

    /**
     * Lo abre para editarlo.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#EDIT}
     * @throws IOException si no hay editor asociado
     */
    public void edit(File file) throws IOException {
        this.comprobarArchivo(file);
        this.exigir(Action.EDIT);
    }

    /**
     * Lo imprime.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#PRINT}
     * @throws IOException si no hay programa que sepa imprimirlo
     */
    public void print(File file) throws IOException {
        this.comprobarArchivo(file);
        this.exigir(Action.PRINT);
    }

    /**
     * Abre esa dirección en el navegador.
     *
     * @throws NullPointerException si la dirección es `null`
     * @throws UnsupportedOperationException si no se admite {@link Action#BROWSE}
     * @throws IOException si el navegador no arrancó
     */
    public void browse(URI uri) throws IOException {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        this.exigir(Action.BROWSE);
    }

    /**
     * Abre el programa de correo con un mensaje en blanco.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#MAIL}
     * @throws IOException si no arrancó
     */
    public void mail() throws IOException {
        this.exigir(Action.MAIL);
    }

    /**
     * Abre el programa de correo con lo que diga esa dirección `mailto:`.
     *
     * @throws NullPointerException si la dirección es `null`
     * @throws IllegalArgumentException si el esquema no es `mailto`
     * @throws UnsupportedOperationException si no se admite {@link Action#MAIL}
     * @throws IOException si no arrancó
     */
    public void mail(URI mailtoURI) throws IOException {
        if (mailtoURI == null) {
            throw new NullPointerException("mailtoURI");
        }
        if (!"mailto".equalsIgnoreCase(mailtoURI.getScheme())) {
            throw new IllegalArgumentException("URI scheme is not \"mailto\"");
        }
        this.exigir(Action.MAIL);
    }

    /**
     * Abre el directorio del archivo y lo deja seleccionado.
     *
     * <p>Es lo que hace "Mostrar en la carpeta": no abre el archivo, muestra dónde está.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#BROWSE_FILE_DIR}
     */
    public void browseFileDirectory(File file) {
        this.comprobarArchivo(file);
        this.exigir(Action.BROWSE_FILE_DIR);
    }

    /**
     * Manda el archivo a la papelera.
     *
     * <p>Es distinto de borrarlo: se puede recuperar.
     *
     * @return `true` si llegó a la papelera
     * @throws UnsupportedOperationException si no se admite {@link Action#MOVE_TO_TRASH}
     */
    public boolean moveToTrash(File file) {
        this.comprobarArchivo(file);
        this.exigir(Action.MOVE_TO_TRASH);
        return false;
    }

    /**
     * Registra un oyente de eventos del sistema.
     *
     * <p>Un oyente `null` se ignora, que es lo que hace el JDK: registrar nada es no hacer nada.
     *
     * @throws UnsupportedOperationException si el escritorio no admite esa clase de evento
     */
    public void addAppEventListener(SystemEventListener listener) {
        if (listener == null) {
            return;
        }
        throw new UnsupportedOperationException("The current platform doesn't support this event");
    }

    /**
     * Saca un oyente de eventos del sistema.
     *
     * @throws UnsupportedOperationException si el escritorio no admite esa clase de evento
     */
    public void removeAppEventListener(SystemEventListener listener) {
        if (listener == null) {
            return;
        }
        throw new UnsupportedOperationException("The current platform doesn't support this event");
    }

    /**
     * Quién atiende la entrada "Acerca de" del menú del sistema.
     *
     * @param aboutHandler el manejador, o `null` para volver al de fábrica
     * @throws UnsupportedOperationException si no se admite {@link Action#APP_ABOUT}
     */
    public void setAboutHandler(AboutHandler aboutHandler) {
        this.exigir(Action.APP_ABOUT);
    }

    /**
     * Quién atiende "Preferencias".
     *
     * <p>Pasar `null` **esconde la entrada** del menú, que es distinto de dejarla sin hacer nada.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#APP_PREFERENCES}
     */
    public void setPreferencesHandler(PreferencesHandler preferencesHandler) {
        this.exigir(Action.APP_PREFERENCES);
    }

    /**
     * Pone esa barra de menús en la del sistema.
     *
     * <p>Es de macOS: la barra de menús del programa va en la franja de arriba de la pantalla, fuera
     * de la ventana. En el resto de los sistemas nunca se admite, y acá tampoco.
     *
     * <p>El {@link javax.swing.JMenuBar} que recibe es un **lugar reservado** de esta biblioteca, no
     * el de Swing: alcanza para declarar el método, que es todo lo que hace falta, porque el método
     * tira antes de mirarlo.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#APP_MENU_BAR}
     */
    public void setDefaultMenuBar(javax.swing.JMenuBar menuBar) {
        this.exigir(Action.APP_MENU_BAR);
    }

    /**
     * Quién atiende el pedido de abrir archivos hecho desde el escritorio.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#APP_OPEN_FILE}
     */
    public void setOpenFileHandler(OpenFilesHandler openFileHandler) {
        this.exigir(Action.APP_OPEN_FILE);
    }

    /**
     * Quién atiende el pedido de imprimir archivos.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#APP_PRINT_FILE}
     */
    public void setPrintFileHandler(PrintFilesHandler printFileHandler) {
        this.exigir(Action.APP_PRINT_FILE);
    }

    /**
     * Quién atiende el pedido de abrir una dirección.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#APP_OPEN_URI}
     */
    public void setOpenURIHandler(OpenURIHandler openURIHandler) {
        this.exigir(Action.APP_OPEN_URI);
    }

    /**
     * Quién atiende el pedido de salir.
     *
     * <p>El manejador recibe una respuesta y **tiene que contestarla**: hasta que conteste, el
     * sistema espera. Es lo que permite preguntar "¿guardo los cambios?" antes de cerrar.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#APP_QUIT_HANDLER}
     */
    public void setQuitHandler(QuitHandler quitHandler) {
        this.exigir(Action.APP_QUIT_HANDLER);
    }

    /**
     * Elige cómo se sale del programa.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#APP_QUIT_STRATEGY}
     */
    public void setQuitStrategy(QuitStrategy strategy) {
        this.exigir(Action.APP_QUIT_STRATEGY);
    }

    /**
     * Deja que el sistema mate el programa sin avisar.
     *
     * <p>Sirve para acelerar el apagado: si el programa no tiene nada que guardar, no hace falta
     * darle la oportunidad de negarse.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#APP_SUDDEN_TERMINATION}
     */
    public void enableSuddenTermination() {
        this.exigir(Action.APP_SUDDEN_TERMINATION);
    }

    /**
     * Vuelve a exigir que se le avise antes de matarlo.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#APP_SUDDEN_TERMINATION}
     */
    public void disableSuddenTermination() {
        this.exigir(Action.APP_SUDDEN_TERMINATION);
    }

    /**
     * Pide pasar a primer plano.
     *
     * @param allWindows si traer todas las ventanas o sólo la de adelante
     * @throws UnsupportedOperationException si no se admite {@link Action#APP_REQUEST_FOREGROUND}
     */
    public void requestForeground(boolean allWindows) {
        this.exigir(Action.APP_REQUEST_FOREGROUND);
    }

    /**
     * Abre la ayuda del programa.
     *
     * @throws UnsupportedOperationException si no se admite {@link Action#APP_HELP_VIEWER}
     */
    public void openHelpViewer() {
        this.exigir(Action.APP_HELP_VIEWER);
    }

    /** Tira si esa acción no se admite. */
    private void exigir(Action a) {
        if (!this.isSupported(a)) {
            throw new UnsupportedOperationException("The " + a.name()
                    + " action is not supported on the current platform!");
        }
    }

    /** Que el archivo exista. */
    private void comprobarArchivo(File file) {
        if (file == null) {
            throw new NullPointerException("file");
        }
        if (!file.exists()) {
            throw new IllegalArgumentException("The file: " + file.getPath() + " doesn't exist.");
        }
    }
}
