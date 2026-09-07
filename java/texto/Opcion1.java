import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * El panel de opciones y el selector de archivos, contra el JDK.
 *
 * <p>No se muestra ningun dialogo: sin pantalla no hay ventana. Lo que se compara es la maquina de
 * estados de las dos clases -- que valida que, que avisa que, y que copia se devuelve --, que es lo
 * que se usa igual con dialogo o sin el.
 *
 * <p>Queda afuera todo lo que sale del aspecto instalado: el filtro de "todos los archivos", el
 * texto del boton de aceptar y el titulo por omision los arma el delegado de aspecto, que esta
 * biblioteca todavia no tiene.
 */
public class Opcion1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Anota los avisos, salvo los que dispara el aspecto instalado del JDK. */
    static class Espia implements PropertyChangeListener {

        private final StringBuilder log = new StringBuilder();

        public void propertyChange(PropertyChangeEvent e) {
            String n = e.getPropertyName();
            if ("ancestor".equals(n) || "UI".equals(n) || "componentOrientation".equals(n)) {
                return;
            }
            log.append(" ").append(n);
        }

        String vaciar() {
            String s = log.toString();
            log.setLength(0);
            return s;
        }
    }

    /** Acepta lo que empiece con esa letra. */
    static class PorLetra extends FileFilter {

        private final char letra;

        PorLetra(char letra) {
            this.letra = letra;
        }

        public boolean accept(File f) {
            String n = f.getName();
            return n.length() > 0 && n.charAt(0) == letra;
        }

        public String getDescription() {
            return "empieza con " + letra;
        }
    }

    static String nombres(File[] fs) {
        if (fs == null) {
            return "-";
        }
        StringBuilder b = new StringBuilder();
        b.append(fs.length).append(":");
        for (int i = 0; i < fs.length; i++) {
            b.append(" ").append(fs[i].getName());
        }
        return b.toString();
    }

    static String filtros(FileFilter[] fs) {
        StringBuilder b = new StringBuilder();
        b.append(fs.length).append(":");
        for (int i = 0; i < fs.length; i++) {
            b.append(" ").append(fs[i].getDescription());
        }
        return b.toString();
    }

    static void panel() {
        linea("constantes opciones def=" + JOptionPane.DEFAULT_OPTION + " sn="
                + JOptionPane.YES_NO_OPTION + " snc=" + JOptionPane.YES_NO_CANCEL_OPTION
                + " ac=" + JOptionPane.OK_CANCEL_OPTION);
        linea("constantes respuesta si=" + JOptionPane.YES_OPTION + " no=" + JOptionPane.NO_OPTION
                + " cancelar=" + JOptionPane.CANCEL_OPTION + " aceptar=" + JOptionPane.OK_OPTION
                + " cerrado=" + JOptionPane.CLOSED_OPTION);
        linea("constantes mensaje error=" + JOptionPane.ERROR_MESSAGE + " info="
                + JOptionPane.INFORMATION_MESSAGE + " aviso=" + JOptionPane.WARNING_MESSAGE
                + " pregunta=" + JOptionPane.QUESTION_MESSAGE + " plano="
                + JOptionPane.PLAIN_MESSAGE);
        linea("centinela=" + JOptionPane.UNINITIALIZED_VALUE);

        JOptionPane p = new JOptionPane();
        linea("mensaje por omision=" + p.getMessage() + " tipo=" + p.getMessageType()
                + " opciones=" + p.getOptionType());
        linea("valor=" + p.getValue() + " entrada=" + p.getInputValue()
                + " pide entrada=" + p.getWantsInput());
        linea("botones=" + p.getOptions() + " inicial=" + p.getInitialValue()
                + " lista=" + p.getSelectionValues() + " elegido=" + p.getInitialSelectionValue());
        linea("icono=" + p.getIcon() + " clase de aspecto=" + p.getUIClassID()
                + " maximo por renglon=" + p.getMaxCharactersPerLineCount());

        Espia espia = new Espia();
        p.addPropertyChangeListener(espia);

        p.setMessage("hola");
        p.setMessageType(JOptionPane.WARNING_MESSAGE);
        p.setOptionType(JOptionPane.YES_NO_OPTION);
        linea("puesto mensaje=" + p.getMessage() + " tipo=" + p.getMessageType()
                + " opciones=" + p.getOptionType() + " |" + espia.vaciar());

        try {
            p.setMessageType(9);
            linea("tipo 9 aceptado");
        } catch (RuntimeException e) {
            linea("tipo 9 rechazado=" + (e.getClass() == RuntimeException.class));
        }
        try {
            p.setOptionType(9);
            linea("opciones 9 aceptado");
        } catch (RuntimeException e) {
            linea("opciones 9 rechazado=" + (e.getClass() == RuntimeException.class));
        }
        linea("tras rechazos tipo=" + p.getMessageType() + " opciones=" + p.getOptionType()
                + " |" + espia.vaciar());

        Object[] botones = new Object[] {"uno", "dos", "tres"};
        p.setOptions(botones);
        Object[] copia = p.getOptions();
        copia[0] = "cambiado";
        linea("botones son copia=" + (p.getOptions()[0]) + " |" + espia.vaciar());

        p.setValue("dos");
        linea("valor=" + p.getValue() + " |" + espia.vaciar());

        // Poner opciones de lista prende el pedido de entrada.
        p.setSelectionValues(new Object[] {"a", "b"});
        linea("lista puesta pide entrada=" + p.getWantsInput() + " |" + espia.vaciar());
        p.setSelectionValues(null);
        linea("lista nula pide entrada=" + p.getWantsInput() + " |" + espia.vaciar());
        p.setWantsInput(false);
        p.setInitialSelectionValue("b");
        p.setInputValue("escrito");
        linea("entrada=" + p.getInputValue() + " elegido=" + p.getInitialSelectionValue()
                + " |" + espia.vaciar());

        JOptionPane q = new JOptionPane("m", JOptionPane.ERROR_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION);
        linea("q mensaje=" + q.getMessage() + " tipo=" + q.getMessageType()
                + " opciones=" + q.getOptionType() + " valor=" + q.getValue());
        linea("escritorio de nulo=" + JOptionPane.getDesktopPaneForComponent(null));
    }

    static void selector() {
        linea("constantes dialogo abrir=" + JFileChooser.OPEN_DIALOG + " guardar="
                + JFileChooser.SAVE_DIALOG + " propio=" + JFileChooser.CUSTOM_DIALOG);
        linea("constantes respuesta cancelar=" + JFileChooser.CANCEL_OPTION + " aceptar="
                + JFileChooser.APPROVE_OPTION + " error=" + JFileChooser.ERROR_OPTION);
        linea("constantes modo archivos=" + JFileChooser.FILES_ONLY + " carpetas="
                + JFileChooser.DIRECTORIES_ONLY + " ambos="
                + JFileChooser.FILES_AND_DIRECTORIES);
        linea("comandos=" + JFileChooser.APPROVE_SELECTION + "," + JFileChooser.CANCEL_SELECTION);

        JFileChooser s = new JFileChooser();
        s.setAcceptAllFileFilterUsed(false);
        Espia espia = new Espia();
        s.addPropertyChangeListener(espia);

        linea("clase de aspecto=" + s.getUIClassID() + " tipo=" + s.getDialogType()
                + " modo=" + s.getFileSelectionMode());
        linea("varios=" + s.isMultiSelectionEnabled() + " ocultos escondidos="
                + s.isFileHidingEnabled() + " botones=" + s.getControlButtonsAreShown());
        linea("elegido=" + s.getSelectedFile() + " elegidos=" + nombres(s.getSelectedFiles()));
        linea("accesorio=" + s.getAccessory() + " vista=" + s.getFileView()
                + " arrastre=" + s.getDragEnabled());
        linea("filtro=" + s.getFileFilter() + " filtros=" + filtros(s.getChoosableFileFilters()));
        linea("archivos=" + s.isFileSelectionEnabled() + " carpetas="
                + s.isDirectorySelectionEnabled());
        espia.vaciar();

        s.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        linea("solo carpetas archivos=" + s.isFileSelectionEnabled() + " carpetas="
                + s.isDirectorySelectionEnabled() + " |" + espia.vaciar());
        s.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        linea("ambos archivos=" + s.isFileSelectionEnabled() + " carpetas="
                + s.isDirectorySelectionEnabled() + " |" + espia.vaciar());
        try {
            s.setFileSelectionMode(9);
            linea("modo 9 aceptado");
        } catch (IllegalArgumentException e) {
            linea("modo 9 rechazado: " + e.getMessage());
        }
        try {
            s.setDialogType(9);
            linea("tipo 9 aceptado");
        } catch (IllegalArgumentException e) {
            linea("tipo 9 rechazado: " + e.getMessage());
        }
        linea("tras rechazos modo=" + s.getFileSelectionMode() + " tipo=" + s.getDialogType()
                + " |" + espia.vaciar());

        // Los filtros: se agregan en orden, el primero pasa a ser el que se usa.
        PorLetra pa = new PorLetra('a');
        PorLetra pb = new PorLetra('b');
        s.addChoosableFileFilter(pa);
        linea("un filtro=" + filtros(s.getChoosableFileFilters()) + " en uso="
                + s.getFileFilter().getDescription() + " |" + espia.vaciar());
        s.addChoosableFileFilter(pb);
        linea("dos filtros=" + filtros(s.getChoosableFileFilters()) + " en uso="
                + s.getFileFilter().getDescription() + " |" + espia.vaciar());
        s.addChoosableFileFilter(pa);
        linea("repetido=" + filtros(s.getChoosableFileFilters()) + " |" + espia.vaciar());

        linea("acepta a.txt=" + s.accept(new File("a.txt")) + " b.txt="
                + s.accept(new File("b.txt")));
        s.setFileFilter(pb);
        linea("con b acepta a.txt=" + s.accept(new File("a.txt")) + " b.txt="
                + s.accept(new File("b.txt")) + " |" + espia.vaciar());

        // Sacar el filtro en uso pasa al que quede.
        linea("sacar b=" + s.removeChoosableFileFilter(pb) + " ahora="
                + filtros(s.getChoosableFileFilters()) + " en uso="
                + s.getFileFilter().getDescription() + " |" + espia.vaciar());
        linea("sacar b de nuevo=" + s.removeChoosableFileFilter(pb));
        linea("sacar a=" + s.removeChoosableFileFilter(pa) + " ahora="
                + filtros(s.getChoosableFileFilters()) + " en uso=" + s.getFileFilter()
                + " |" + espia.vaciar());

        FileNameExtensionFilter fx = new FileNameExtensionFilter("textos", "txt", "md");
        s.addChoosableFileFilter(fx);
        linea("extensiones=" + fx.getDescription() + " " + java.util.Arrays.toString(
                fx.getExtensions()));
        linea("acepta a.txt=" + fx.accept(new File("a.txt")) + " a.md="
                + fx.accept(new File("a.md")) + " a.png=" + fx.accept(new File("a.png")));
        espia.vaciar();

        // La eleccion multiple y la copia que devuelve.
        s.setMultiSelectionEnabled(true);
        linea("varios=" + s.isMultiSelectionEnabled() + " |" + espia.vaciar());
        File[] elegidos = new File[] {new File("x.txt"), new File("y.txt")};
        s.setSelectedFiles(elegidos);
        linea("elegidos=" + nombres(s.getSelectedFiles()) + " primero="
                + s.getSelectedFile().getName() + " |" + espia.vaciar());
        File[] devueltos = s.getSelectedFiles();
        devueltos[0] = new File("z.txt");
        linea("es copia=" + s.getSelectedFiles()[0].getName());
        s.setSelectedFiles(null);
        linea("nulos=" + nombres(s.getSelectedFiles()) + " elegido=" + s.getSelectedFile()
                + " |" + espia.vaciar());
        s.setMultiSelectionEnabled(false);
        linea("uno solo=" + s.isMultiSelectionEnabled() + " |" + espia.vaciar());

        s.setFileHidingEnabled(false);
        linea("ocultos escondidos=" + s.isFileHidingEnabled() + " |" + espia.vaciar());
        s.setControlButtonsAreShown(false);
        linea("botones=" + s.getControlButtonsAreShown() + " |" + espia.vaciar());
        s.setControlButtonsAreShown(false);
        linea("botones repetido |" + espia.vaciar());
        s.setDragEnabled(true);
        linea("arrastre=" + s.getDragEnabled());
        s.setApproveButtonMnemonic('a');
        linea("atajo=" + s.getApproveButtonMnemonic() + " |" + espia.vaciar());
        s.setApproveButtonToolTipText("ayuda");
        linea("ayuda=" + s.getApproveButtonToolTipText() + " |" + espia.vaciar());
        s.setDialogTitle("titulo");
        linea("titulo=" + s.getDialogTitle() + " |" + espia.vaciar());
        s.setApproveButtonText("dale");
        linea("boton=" + s.getApproveButtonText() + " |" + espia.vaciar());
        // Volver a abrir o guardar borra el texto propio del boton.
        s.setDialogType(JFileChooser.SAVE_DIALOG);
        linea("guardar boton=" + s.getApproveButtonText() + " |" + espia.vaciar());

        // Un filtro que no acepta lo elegido lo deselecciona. Aca no se comparan los avisos: el
        // aspecto instalado del JDK agrega uno propio de la lista de filtros.
        JFileChooser t = new JFileChooser();
        t.setAcceptAllFileFilterUsed(false);
        t.setSelectedFile(new File("q.png"));
        linea("elegido antes=" + t.getSelectedFile().getName());
        t.setFileFilter(new FileNameExtensionFilter("textos", "txt"));
        linea("elegido despues=" + t.getSelectedFile());
    }

    public static int run() {
        panel();
        selector();
        return 0;
    }
}
