import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.plaf.basic.BasicFileChooserUI;

/**
 * El selector de archivos, contra el JDK.
 *
 * <p>Los once iconos vienen de la tabla del aspecto y aca no hay ninguna, asi que no se comparan.
 * Lo que si se compara es todo lo demas: los textos, los mnemonicos, las acciones y las respuestas
 * que el basico da sin dibujar nada.
 */
public class Plaf13 {

    /** Una carpeta del repositorio que no cambia; el camino es absoluto a proposito. */
    static final String CARPETA =
            "C:\\Users\\nicol\\Sources\\Larena\\KajiJVM\\KajiJDK\\repo\\KajiLibrary\\javax\\swing\\undo";

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static String corto(Object o) {
        if (o == null) {
            return "-";
        }
        String c = o.getClass().getName();
        return c.substring(c.lastIndexOf('.') + 1);
    }

    static class Selector extends BasicFileChooserUI {
        Selector(JFileChooser c) {
            super(c);
        }

        String textos() {
            return "abrir='" + openButtonText + "' guardar='" + saveButtonText
                    + "' cancelar='" + cancelButtonText + "' actualizar='" + updateButtonText
                    + "' ayuda='" + helpButtonText + "' abrir carpeta='"
                    + directoryOpenButtonText + "'";
        }

        String tips() {
            return "abrir='" + openButtonToolTipText + "' guardar='" + saveButtonToolTipText
                    + "' cancelar='" + cancelButtonToolTipText
                    + "' actualizar='" + updateButtonToolTipText
                    + "' ayuda='" + helpButtonToolTipText
                    + "' abrir carpeta='" + directoryOpenButtonToolTipText + "'";
        }

        String mnemonicos() {
            return openButtonMnemonic + "/" + saveButtonMnemonic + "/" + cancelButtonMnemonic
                    + "/" + updateButtonMnemonic + "/" + helpButtonMnemonic
                    + "/" + directoryOpenButtonMnemonic;
        }

        boolean carpetaElegida() {
            return isDirectorySelected();
        }

        File carpeta() {
            return getDirectory();
        }

        javax.swing.JButton aprobar(JFileChooser c) {
            return getApproveButton(c);
        }
    }

    static void selectores() {
        linea("--- BasicFileChooserUI ---");
        JFileChooser fc = new JFileChooser(new File(CARPETA));
        Selector u = new Selector(fc);
        linea("comparte instancia="
                + (BasicFileChooserUI.createUI(fc) == BasicFileChooserUI.createUI(fc)));
        u.installUI(fc);
        linea("textos: " + u.textos());
        linea("tips: " + u.tips());
        linea("mnemonicos: " + u.mnemonicos());
        linea("hay modelo=" + (u.getModel() != null)
                + " panel accesorio=" + corto(u.getAccessoryPanel()));
        linea("acciones: aprobar=" + (u.getApproveSelectionAction() != null)
                + " cancelar=" + (u.getCancelSelectionAction() != null)
                + " carpeta nueva=" + (u.getNewFolderAction() != null)
                + " subir=" + (u.getChangeToParentDirectoryAction() != null)
                + " casa=" + (u.getGoHomeAction() != null)
                + " actualizar=" + (u.getUpdateAction() != null));
        linea("aprobar: texto='" + u.getApproveButtonText(fc)
                + "' tip='" + u.getApproveButtonToolTipText(fc)
                + "' mnemonico=" + u.getApproveButtonMnemonic(fc));
        linea("titulo='" + u.getDialogTitle(fc) + "'"
                + " boton por omision=" + u.getDefaultButton(fc)
                + " boton de aprobar=" + u.aprobar(fc));
        linea("nombre de archivo=" + u.getFileName()
                + " nombre de carpeta=" + u.getDirectoryName());
        u.setFileName("x.txt");
        u.setDirectoryName("C:/tmp");
        linea("tras ponerlos: archivo=" + u.getFileName()
                + " carpeta=" + u.getDirectoryName());
        linea("carpeta elegida=" + u.carpetaElegida() + " carpeta=" + u.carpeta());
        linea("vista=" + corto(u.getFileView(fc))
                + " filtro de todos='" + u.getAcceptAllFileFilter(fc).getDescription() + "'");

        // El titulo de un dialogo de guardar es otro.
        JFileChooser guardar = new JFileChooser(new File(CARPETA));
        guardar.setDialogType(JFileChooser.SAVE_DIALOG);
        Selector ug = new Selector(guardar);
        ug.installUI(guardar);
        linea("guardar: titulo='" + ug.getDialogTitle(guardar)
                + "' texto de aprobar='" + ug.getApproveButtonText(guardar)
                + "' tip='" + ug.getApproveButtonToolTipText(guardar) + "'");

        // Y el que puso el programa gana.
        JFileChooser propio = new JFileChooser(new File(CARPETA));
        propio.setDialogTitle("Elegi uno");
        propio.setApproveButtonText("Dale");
        Selector up = new Selector(propio);
        up.installUI(propio);
        linea("con titulo propio='" + up.getDialogTitle(propio)
                + "' texto de aprobar='" + up.getApproveButtonText(propio) + "'");
    }

    public static int run() {
        selectores();
        return 0;
    }
}
