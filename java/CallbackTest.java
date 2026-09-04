import java.util.Arrays;
import java.util.Locale;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.w3c.dom.traversal.NodeFilter;

/**
 * javax.security.auth.callback y las constantes de org.w3c.dom.traversal.
 *
 * <p>Las expectativas salieron de preguntarle al JDK 25. Las cuatro que no son obvias y por las que
 * vale la pena tener la prueba: getSelectedIndex() de ConfirmationCallback devuelve 0 --que tambien
 * es YES-- cuando nadie contesto todavia, con opciones propias getOptionType() da
 * UNSPECIFIED_OPTION, clearPassword() llena de espacios en vez de poner null, y setSelectedIndexes
 * lanza UnsupportedOperationException cuando el callback se creo con seleccion simple.
 */
public class CallbackTest {

    public static int run() {
        int i = 0;

        // ======================================================================================
        // las constantes: son el contrato entre quien pregunta y quien contesta
        // ======================================================================================
        if (ConfirmationCallback.UNSPECIFIED_OPTION != -1) { return i; } i++;
        if (ConfirmationCallback.YES_NO_OPTION != 0) { return i; } i++;
        if (ConfirmationCallback.YES_NO_CANCEL_OPTION != 1) { return i; } i++;
        if (ConfirmationCallback.OK_CANCEL_OPTION != 2) { return i; } i++;
        if (ConfirmationCallback.YES != 0) { return i; } i++;
        if (ConfirmationCallback.NO != 1) { return i; } i++;
        if (ConfirmationCallback.CANCEL != 2) { return i; } i++;
        if (ConfirmationCallback.OK != 3) { return i; } i++;
        if (ConfirmationCallback.INFORMATION != 0) { return i; } i++;
        if (ConfirmationCallback.WARNING != 1) { return i; } i++;
        if (ConfirmationCallback.ERROR != 2) { return i; } i++;
        if (TextOutputCallback.INFORMATION != 0) { return i; } i++;
        if (TextOutputCallback.WARNING != 1) { return i; } i++;
        if (TextOutputCallback.ERROR != 2) { return i; } i++;

        // ======================================================================================
        // NameCallback y TextInputCallback: el default es una sugerencia, no una respuesta
        // ======================================================================================
        NameCallback name = new NameCallback("usuario: ");
        if (!name.getPrompt().equals("usuario: ")) { return i; } i++;
        if (name.getDefaultName() != null) { return i; } i++;
        if (name.getName() != null) { return i; } i++;
        name.setName("juan");
        if (!name.getName().equals("juan")) { return i; } i++;
        // Con default, getName SIGUE siendo null hasta que alguien conteste.
        NameCallback conDefault = new NameCallback("u: ", "ana");
        if (!conDefault.getDefaultName().equals("ana")) { return i; } i++;
        if (conDefault.getName() != null) { return i; } i++;
        if (!(name instanceof Callback)) { return i; } i++;

        boolean threw = false;
        try { new NameCallback(""); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new NameCallback(null); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new NameCallback("u", ""); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;

        TextInputCallback text = new TextInputCallback("texto: ", "hola");
        if (!text.getDefaultText().equals("hola")) { return i; } i++;
        if (text.getText() != null) { return i; } i++;
        text.setText("chau");
        if (!text.getText().equals("chau")) { return i; } i++;
        threw = false;
        try { new TextInputCallback(""); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;

        // ======================================================================================
        // PasswordCallback: char[] y no String, y clearPassword no pone null
        // ======================================================================================
        PasswordCallback pw = new PasswordCallback("clave: ", false);
        if (pw.isEchoOn()) { return i; } i++;
        if (pw.getPassword() != null) { return i; } i++;
        char[] secret = new char[] {'a', 'b', 'c'};
        pw.setPassword(secret);
        if (!new String(pw.getPassword()).equals("abc")) { return i; } i++;
        // Copia al entrar: pisar el arreglo de afuera no toca la clave guardada.
        secret[0] = 'z';
        if (pw.getPassword()[0] != 'a') { return i; } i++;
        // Y copia al salir.
        pw.getPassword()[0] = 'z';
        if (pw.getPassword()[0] != 'a') { return i; } i++;
        // clearPassword LLENA DE ESPACIOS, no pone null: borrar es pisar los bytes.
        pw.clearPassword();
        if (pw.getPassword() == null) { return i; } i++;
        if (pw.getPassword().length != 3) { return i; } i++;
        if (!new String(pw.getPassword()).equals("   ")) { return i; } i++;
        pw.setPassword(null);
        if (pw.getPassword() != null) { return i; } i++;
        // Y limpiar sin clave puesta no rompe.
        pw.clearPassword();
        i++;
        if (new PasswordCallback("p", true).isEchoOn() != true) { return i; } i++;

        // ======================================================================================
        // TextOutputCallback y LanguageCallback
        // ======================================================================================
        TextOutputCallback out = new TextOutputCallback(TextOutputCallback.WARNING, "cuidado");
        if (out.getMessageType() != TextOutputCallback.WARNING) { return i; } i++;
        if (!out.getMessage().equals("cuidado")) { return i; } i++;
        threw = false;
        try { new TextOutputCallback(9, "x"); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new TextOutputCallback(TextOutputCallback.ERROR, ""); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;

        LanguageCallback lang = new LanguageCallback();
        if (lang.getLocale() != null) { return i; } i++;
        lang.setLocale(Locale.FRANCE);
        if (!lang.getLocale().equals(Locale.FRANCE)) { return i; } i++;

        // ======================================================================================
        // ChoiceCallback: la seleccion multiple se decide al construir
        // ======================================================================================
        ChoiceCallback multi = new ChoiceCallback("elegi", new String[] {"x", "y", "z"}, 1, true);
        if (!multi.getPrompt().equals("elegi")) { return i; } i++;
        if (multi.getChoices().length != 3) { return i; } i++;
        if (!multi.getChoices()[0].equals("x")) { return i; } i++;
        if (multi.getDefaultChoice() != 1) { return i; } i++;
        if (!multi.allowMultipleSelections()) { return i; } i++;
        if (multi.getSelectedIndexes() != null) { return i; } i++;
        // getChoices es copia.
        multi.getChoices()[0] = "roto";
        if (!multi.getChoices()[0].equals("x")) { return i; } i++;
        multi.setSelectedIndex(2);
        if (!Arrays.equals(multi.getSelectedIndexes(), new int[] {2})) { return i; } i++;
        multi.setSelectedIndexes(new int[] {0, 2});
        if (!Arrays.equals(multi.getSelectedIndexes(), new int[] {0, 2})) { return i; } i++;

        ChoiceCallback single = new ChoiceCallback("p", new String[] {"x", "y"}, 0, false);
        if (single.allowMultipleSelections()) { return i; } i++;
        // setSelectedIndex anda igual: una sola eleccion cabe en las dos formas.
        single.setSelectedIndex(1);
        if (!Arrays.equals(single.getSelectedIndexes(), new int[] {1})) { return i; } i++;
        // setSelectedIndexes NO: quedarse con una de varias seria elegir por quien pregunto.
        threw = false;
        try { single.setSelectedIndexes(new int[] {0, 1}); }
        catch (UnsupportedOperationException e) { threw = true; }
        if (!threw) { return i; } i++;

        threw = false;
        try { new ChoiceCallback("p", new String[0], 0, false); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new ChoiceCallback("p", new String[] {"a", null}, 0, false); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new ChoiceCallback("p", new String[] {"a", ""}, 0, false); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new ChoiceCallback("p", new String[] {"a"}, 5, false); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new ChoiceCallback("", new String[] {"a"}, 0, false); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;

        // ======================================================================================
        // ConfirmationCallback: los dos modos
        // ======================================================================================
        ConfirmationCallback yesNo = new ConfirmationCallback(ConfirmationCallback.INFORMATION,
            ConfirmationCallback.YES_NO_OPTION, ConfirmationCallback.YES);
        if (yesNo.getPrompt() != null) { return i; } i++;
        if (yesNo.getOptions() != null) { return i; } i++;
        if (yesNo.getMessageType() != ConfirmationCallback.INFORMATION) { return i; } i++;
        if (yesNo.getOptionType() != ConfirmationCallback.YES_NO_OPTION) { return i; } i++;
        if (yesNo.getDefaultOption() != ConfirmationCallback.YES) { return i; } i++;
        // Sin contestar da 0, que TAMBIEN es YES. No hay forma de distinguirlos desde el API.
        if (yesNo.getSelectedIndex() != 0) { return i; } i++;
        yesNo.setSelectedIndex(ConfirmationCallback.NO);
        if (yesNo.getSelectedIndex() != ConfirmationCallback.NO) { return i; } i++;

        // Con opciones propias, getOptionType da UNSPECIFIED_OPTION: asi se distinguen los modos.
        ConfirmationCallback own = new ConfirmationCallback("elegi",
            ConfirmationCallback.WARNING, new String[] {"a", "b", "c"}, 1);
        if (own.getOptionType() != ConfirmationCallback.UNSPECIFIED_OPTION) { return i; } i++;
        if (own.getOptions().length != 3) { return i; } i++;
        if (!own.getOptions()[0].equals("a")) { return i; } i++;
        if (own.getDefaultOption() != 1) { return i; } i++;
        if (!own.getPrompt().equals("elegi")) { return i; } i++;
        own.getOptions()[0] = "roto";
        if (!own.getOptions()[0].equals("a")) { return i; } i++;
        // El de tres argumentos con opciones tampoco lleva prompt.
        if (new ConfirmationCallback(ConfirmationCallback.ERROR, new String[] {"a"}, 0)
                .getPrompt() != null) { return i; } i++;

        // El default se valida CONTRA EL JUEGO elegido.
        threw = false;
        try { new ConfirmationCallback(9, ConfirmationCallback.YES_NO_OPTION, 0); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new ConfirmationCallback(ConfirmationCallback.INFORMATION, 9, 0); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try {
            new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                ConfirmationCallback.YES_NO_OPTION, ConfirmationCallback.CANCEL);
        } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try {
            new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                ConfirmationCallback.OK_CANCEL_OPTION, ConfirmationCallback.YES);
        } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        // OK_CANCEL con CANCEL si vale.
        if (new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                ConfirmationCallback.OK_CANCEL_OPTION, ConfirmationCallback.CANCEL)
                .getDefaultOption() != ConfirmationCallback.CANCEL) { return i; } i++;
        // Y YES_NO_CANCEL acepta las tres.
        if (new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                ConfirmationCallback.YES_NO_CANCEL_OPTION, ConfirmationCallback.CANCEL)
                .getDefaultOption() != ConfirmationCallback.CANCEL) { return i; } i++;

        threw = false;
        try { new ConfirmationCallback(ConfirmationCallback.INFORMATION, new String[0], 0); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try {
            new ConfirmationCallback(ConfirmationCallback.INFORMATION,
                new String[] {"a", null}, 0);
        } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try {
            new ConfirmationCallback(ConfirmationCallback.INFORMATION, new String[] {"a", ""}, 0);
        } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try {
            new ConfirmationCallback(ConfirmationCallback.INFORMATION, new String[] {"a", "b"}, 5);
        } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try {
            new ConfirmationCallback("", ConfirmationCallback.INFORMATION,
                ConfirmationCallback.YES_NO_OPTION, ConfirmationCallback.YES);
        } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try {
            new ConfirmationCallback(null, ConfirmationCallback.INFORMATION,
                ConfirmationCallback.YES_NO_OPTION, ConfirmationCallback.YES);
        } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;

        // ======================================================================================
        // UnsupportedCallbackException lleva CUAL callback quedo sin contestar
        // ======================================================================================
        UnsupportedCallbackException ex = new UnsupportedCallbackException(name, "no se banca");
        if (ex.getCallback() != name) { return i; } i++;
        if (!ex.getMessage().equals("no se banca")) { return i; } i++;
        if (new UnsupportedCallbackException(name).getMessage() != null) { return i; } i++;
        if (!(ex instanceof Exception)) { return i; } i++;

        // ======================================================================================
        // org.w3c.dom.traversal: las constantes del filtro
        // ======================================================================================
        if (NodeFilter.FILTER_ACCEPT != 1) { return i; } i++;
        if (NodeFilter.FILTER_REJECT != 2) { return i; } i++;
        if (NodeFilter.FILTER_SKIP != 3) { return i; } i++;
        if (NodeFilter.SHOW_ALL != 0xFFFFFFFF) { return i; } i++;
        if (NodeFilter.SHOW_ELEMENT != 0x1) { return i; } i++;
        if (NodeFilter.SHOW_ATTRIBUTE != 0x2) { return i; } i++;
        if (NodeFilter.SHOW_TEXT != 0x4) { return i; } i++;
        if (NodeFilter.SHOW_CDATA_SECTION != 0x8) { return i; } i++;
        if (NodeFilter.SHOW_ENTITY_REFERENCE != 0x10) { return i; } i++;
        if (NodeFilter.SHOW_ENTITY != 0x20) { return i; } i++;
        if (NodeFilter.SHOW_PROCESSING_INSTRUCTION != 0x40) { return i; } i++;
        if (NodeFilter.SHOW_COMMENT != 0x80) { return i; } i++;
        if (NodeFilter.SHOW_DOCUMENT != 0x100) { return i; } i++;
        if (NodeFilter.SHOW_DOCUMENT_TYPE != 0x200) { return i; } i++;
        if (NodeFilter.SHOW_DOCUMENT_FRAGMENT != 0x400) { return i; } i++;
        if (NodeFilter.SHOW_NOTATION != 0x800) { return i; } i++;
        // Las mascaras son bits distintos: un OR de todas no puede perder ninguna.
        int todas = NodeFilter.SHOW_ELEMENT | NodeFilter.SHOW_ATTRIBUTE | NodeFilter.SHOW_TEXT
            | NodeFilter.SHOW_CDATA_SECTION | NodeFilter.SHOW_ENTITY_REFERENCE
            | NodeFilter.SHOW_ENTITY | NodeFilter.SHOW_PROCESSING_INSTRUCTION
            | NodeFilter.SHOW_COMMENT | NodeFilter.SHOW_DOCUMENT | NodeFilter.SHOW_DOCUMENT_TYPE
            | NodeFilter.SHOW_DOCUMENT_FRAGMENT | NodeFilter.SHOW_NOTATION;
        if (Integer.bitCount(todas) != 12) { return i; } i++;

        return -1;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
