import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * El boton por dentro, sin pintar: la secuencia de un click, el comando, el mnemonico, una Action
 * atada y un ButtonGroup.
 *
 * Todo se imprime con "//" adelante para compararlo con el JDK linea por linea; el valor de
 * retorno es el largo de la bitacora, que no importa.
 */
public class BotonEventos {
    static StringBuilder log = new StringBuilder();

    /** Un escucha de los tres tipos, nombrado y no anonimo (#499). */
    static class Registro implements ActionListener, ChangeListener, ItemListener {
        public void actionPerformed(ActionEvent e) {
            log.append("accion:" + e.getActionCommand() + ":" + (e.getSource() instanceof JButton) + " ");
        }
        public void stateChanged(ChangeEvent e) { log.append("cambio "); }
        public void itemStateChanged(ItemEvent e) {
            log.append("item:" + (e.getStateChange() == ItemEvent.SELECTED ? "sel" : "desel") + " ");
        }
    }

    static class Guardar extends AbstractAction {
        int veces;
        Guardar() {
            super("Guardar");
            putValue(MNEMONIC_KEY, Integer.valueOf(KeyEvent.VK_G));
            putValue(ACTION_COMMAND_KEY, "guardar");
        }
        public void actionPerformed(ActionEvent e) { veces++; }
    }

    static void linea(String s) { System.out.println("//" + s); }

    public static int run() {
        JButton b = new JButton("Ok");
        Registro r = new Registro();
        b.addActionListener(r);
        b.addChangeListener(r);
        b.addItemListener(r);

        b.doClick(0);
        linea("click: " + log);
        log.setLength(0);

        b.setActionCommand("aceptar");
        b.doClick(0);
        linea("comando: " + log);
        log.setLength(0);

        b.setMnemonic('k');
        linea("mnemonico " + b.getMnemonic() + " indice " + b.getDisplayedMnemonicIndex());
        b.setText("Kilo");
        linea("texto nuevo, indice " + b.getDisplayedMnemonicIndex());

        b.setEnabled(false);
        b.doClick(0);
        linea("deshabilitado: '" + log + "' modelo " + b.getModel().isEnabled());
        log.setLength(0);

        Guardar a = new Guardar();
        JButton c = new JButton(a);
        linea("accion: texto=" + c.getText() + " mnem=" + c.getMnemonic() + " idx="
                + c.getDisplayedMnemonicIndex() + " cmd=" + c.getActionCommand()
                + " hab=" + c.isEnabled());
        a.setEnabled(false);
        linea("accion off: hab=" + c.isEnabled() + " modelo=" + c.getModel().isEnabled());
        a.setEnabled(true);
        a.putValue(Action.NAME, "Grabar");
        c.doClick(0);
        linea("accion renombrada: texto=" + c.getText() + " veces=" + a.veces);
        c.setHideActionText(true);
        linea("texto oculto: " + c.getText());
        c.setAction(null);
        c.doClick(0);
        linea("accion soltada: veces=" + a.veces + " texto=" + c.getText());

        JButton g1 = new JButton("1");
        JButton g2 = new JButton("2");
        g2.setSelected(true);
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(g1);
        grupo.add(g2);
        linea("grupo: n=" + grupo.getButtonCount() + " sel2=" + grupo.isSelected(g2.getModel()));
        grupo.setSelected(g1.getModel(), true);
        linea("grupo cambia: sel1=" + g1.isSelected() + " sel2=" + g2.isSelected());
        grupo.clearSelection();
        linea("grupo vacio: sel1=" + g1.isSelected() + " seleccion=" + grupo.getSelection());

        linea("selectedObjects: " + (b.getSelectedObjects() == null) + " "
                + (g1.getSelectedObjects() == null));
        return log.length();
    }
}
