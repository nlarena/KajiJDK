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
 * El manejador de callbacks que habla por la terminal.
 *
 * <p>JAAS separa <em>que</em> dato hace falta de <em>como</em> se pide: un modulo de login dice que
 * le falta un nombre de usuario armando un {@link NameCallback}, y no sabe ni le importa si eso
 * termina en un dialogo grafico, en un archivo de configuracion o —como aca— en un prompt de texto.
 * Esta clase es la implementacion mas simple posible de ese lado: escribe el prompt y lee una linea.
 *
 * <p>Reconoce cuatro callbacks y <strong>rechaza el resto</strong> con
 * {@link UnsupportedCallbackException}, que es exactamente el contrato: un manejador no esta
 * obligado a saber responder todo, y decir "esto no lo se pedir" es una respuesta valida y
 * distinguible de haber fallado.
 *
 * <h2>Una limitacion que conviene saber</h2>
 *
 * <p>{@link PasswordCallback#isEchoOn} pide que la clave <em>no</em> se muestre mientras se tipea.
 * Apagar el eco no es cosa de Java sino de la terminal —el JDK lo hace a traves de
 * {@link System#console()}, que da acceso al modo crudo del dispositivo—, y esta VM no expone esa
 * consola. Se lee igual, por {@link System#in}, y <strong>la clave se ve</strong>. Queda dicho aca y
 * no escondido: un manejador que promete no mostrarla y la muestra es peor que uno que avisa.
 */
public class TextCallbackHandler implements CallbackHandler {

    /** Un manejador nuevo. No tiene estado: todo lo que necesita llega en cada {@link #handle}. */
    public TextCallbackHandler() {
    }

    /**
     * Atiende cada callback del arreglo, en orden.
     *
     * @throws UnsupportedCallbackException con el primero que no sepa atender — y con <em>ese</em>
     *     callback adentro, para que quien llamo pueda ver cual fue
     */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        BufferedReader entrada = new BufferedReader(new InputStreamReader(System.in));
        for (int i = 0; i < callbacks.length; i++) {
            Callback c = callbacks[i];
            if (c instanceof TextOutputCallback) {
                mostrar((TextOutputCallback) c);
            } else if (c instanceof NameCallback) {
                pedirNombre((NameCallback) c, entrada);
            } else if (c instanceof PasswordCallback) {
                pedirClave((PasswordCallback) c, entrada);
            } else if (c instanceof ConfirmationCallback) {
                confirmar((ConfirmationCallback) c, entrada);
            } else {
                throw new UnsupportedCallbackException(c);
            }
        }
    }

    private void mostrar(TextOutputCallback c) throws IOException {
        // El tipo desconocido es `IllegalArgumentException` y no `UnsupportedCallbackException`: el
        // callback si esta soportado, lo que esta mal es su contenido. Confundirlos le diria a quien
        // llamo que pruebe con otro manejador, cuando el problema lo tiene el suyo.
        // Esto querria ser un `switch`, y no lo es por el finding #461: el plegado de constantes
        // que corre sobre las etiquetas de `case` solo mira los tipos de la unidad de compilacion
        // actual, asi que `TextOutputCallback.INFORMATION` —una constante de otro archivo— se
        // rechaza como si no fuera constante. Fuera de una etiqueta la misma constante se pliega
        // bien, que es justamente lo que hace esta cadena.
        int tipo = c.getMessageType();
        if (tipo == TextOutputCallback.INFORMATION) {
            System.err.println(c.getMessage());
        } else if (tipo == TextOutputCallback.WARNING) {
            System.err.println("Advertencia: " + c.getMessage());
        } else if (tipo == TextOutputCallback.ERROR) {
            System.err.println("Error: " + c.getMessage());
        } else {
            throw new IllegalArgumentException("tipo de mensaje desconocido: "
                    + String.valueOf(tipo));
        }
    }

    private void pedirNombre(NameCallback c, BufferedReader entrada) throws IOException {
        String porDefecto = c.getDefaultName();
        if (porDefecto == null) {
            System.err.print(c.getPrompt() + " ");
        } else {
            System.err.print(c.getPrompt() + " [" + porDefecto + "] ");
        }
        System.err.flush();
        String linea = entrada.readLine();
        // Una linea vacia significa "dejame el que ya venia", no "mi nombre es la cadena vacia".
        if (linea == null || linea.isEmpty()) {
            c.setName(porDefecto);
        } else {
            c.setName(linea);
        }
    }

    private void pedirClave(PasswordCallback c, BufferedReader entrada) throws IOException {
        System.err.print(c.getPrompt() + " ");
        System.err.flush();
        String linea = entrada.readLine();
        if (linea == null) {
            c.setPassword(null);
            return;
        }
        c.setPassword(linea.toCharArray());
    }

    private void confirmar(ConfirmationCallback c, BufferedReader entrada) throws IOException {
        String[] opciones = c.getOptions();
        boolean propias = opciones != null;
        if (!propias) {
            opciones = opcionesDe(c.getOptionType());
        }
        if (c.getPrompt() != null) {
            System.err.println(c.getPrompt());
        }
        for (int i = 0; i < opciones.length; i++) {
            System.err.println(String.valueOf(i) + ". " + opciones[i]);
        }
        System.err.print("Elegi [" + String.valueOf(c.getDefaultOption()) + "] ");
        System.err.flush();

        String linea = entrada.readLine();
        int elegida = c.getDefaultOption();
        if (linea != null && !linea.isEmpty()) {
            try {
                elegida = Integer.parseInt(linea.trim());
            } catch (NumberFormatException e) {
                elegida = c.getDefaultOption();
            }
        }
        if (elegida < 0 || elegida >= opciones.length) {
            elegida = c.getDefaultOption();
        }
        // Con opciones propias el indice ES la respuesta. Con las predefinidas hay que traducir: en
        // `YES_NO_OPTION` la posicion 0 de la lista es `YES`, que vale 0 — pero en
        // `OK_CANCEL_OPTION` la posicion 0 es `OK`, que vale 3. Devolver el indice crudo ahi seria
        // contestar `YES` cuando el usuario dijo `OK`.
        if (propias) {
            c.setSelectedIndex(elegida);
        } else {
            c.setSelectedIndex(valoresDe(c.getOptionType())[elegida]);
        }
    }

    private String[] opcionesDe(int tipo) {
        // Cadena de `if` y no `switch`: ver #461, igual que en `mostrar`.
        if (tipo == ConfirmationCallback.YES_NO_OPTION) {
            return new String[] { "Si", "No" };
        }
        if (tipo == ConfirmationCallback.YES_NO_CANCEL_OPTION) {
            return new String[] { "Si", "No", "Cancelar" };
        }
        if (tipo == ConfirmationCallback.OK_CANCEL_OPTION) {
            return new String[] { "Aceptar", "Cancelar" };
        }
        throw new IllegalArgumentException("tipo de opcion desconocido: " + String.valueOf(tipo));
    }

    private int[] valoresDe(int tipo) {
        if (tipo == ConfirmationCallback.YES_NO_OPTION) {
            return new int[] { ConfirmationCallback.YES, ConfirmationCallback.NO };
        }
        if (tipo == ConfirmationCallback.YES_NO_CANCEL_OPTION) {
            return new int[] { ConfirmationCallback.YES, ConfirmationCallback.NO,
                    ConfirmationCallback.CANCEL };
        }
        if (tipo == ConfirmationCallback.OK_CANCEL_OPTION) {
            return new int[] { ConfirmationCallback.OK, ConfirmationCallback.CANCEL };
        }
        throw new IllegalArgumentException("tipo de opcion desconocido: " + String.valueOf(tipo));
    }
}
