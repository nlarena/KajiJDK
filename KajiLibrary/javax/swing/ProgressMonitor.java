package javax.swing;

import java.awt.Component;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

/**
 * Un cartel de progreso que aparece solo si la tarea resulta ser larga.
 *
 * <h2>Las dos demoras, que son toda la clase</h2>
 *
 * <p>La mayoria de las tareas que uno teme que sean lentas terminan en un parpadeo. Mostrar un
 * cartel para cada una llena la pantalla de ventanas que aparecen y desaparecen.
 *
 * <p>Esta clase espera dos veces. Primero {@link #setMillisToDecideToPopup} -- medio segundo por
 * omision -- sin hacer nada: si la tarea termina ahi, nunca hubo cartel. Cumplido ese plazo,
 * <em>estima</em> cuanto falta a partir de lo que se avanzo hasta ahora, y solo si la estimacion
 * supera {@link #setMillisToPopup} muestra el cartel. Una tarea que en medio segundo ya va por el
 * 90% no lo merece.
 *
 * <p>Esa estimacion es la razon de que {@link #setProgress} haya que llamarlo seguido: sin avances
 * no hay de donde estimar.
 *
 * <h2>Cancelar es una pregunta, no una orden</h2>
 *
 * <p>{@link #isCanceled} dice que el usuario apreto Cancelar. No detiene nada: la tarea tiene que
 * mirarlo y decidir. Es lo correcto -- solo la tarea sabe como abandonar sin dejar las cosas por la
 * mitad -- y es lo que hay que recordar, porque una tarea que no lo consulta muestra un boton de
 * cancelar que no cancela.
 *
 * <h2>Sin pantalla</h2>
 *
 * <p>Toda la cuenta -- los limites, el avance, las dos demoras, la estimacion -- ocurre igual. Lo
 * que no aparece es el cartel, y por lo tanto {@link #isCanceled} siempre da falso: no hay boton que
 * apretar.
 */
public class ProgressMonitor implements Accessible {

    protected AccessibleContext accessibleContext;

    private final Component parentComponent;
    private Object message;
    private String note;
    private int min;
    private int max;
    private int v;
    private int millisToDecideToPopup = 500;
    private int millisToPopup = 2000;
    private long T0;
    private boolean canceled;
    private boolean cerrado;
    private JDialog dialog;
    private JProgressBar myBar;
    private JLabel noteLabel;

    /**
     * Un monitor para una tarea que va de {@code min} a {@code max}.
     *
     * <p>Todavia no muestra nada; ver la nota de la clase.
     */
    public ProgressMonitor(Component parentComponent, Object message, String note, int min,
            int max) {
        this.parentComponent = parentComponent;
        this.message = message;
        this.note = note;
        this.min = min;
        this.max = max;
        this.v = min;
        this.T0 = System.currentTimeMillis();
    }

    /**
     * Anota cuanto se avanzo, y decide si toca mostrar el cartel.
     *
     * <p>Llegar al maximo lo cierra: la tarea termino.
     */
    public void setProgress(int nv) {
        v = nv;
        if (nv >= max) {
            close();
            return;
        }
        if (cerrado) {
            return;
        }
        if (dialog != null) {
            actualizar();
            return;
        }
        long dur = System.currentTimeMillis() - T0;
        if (dur < millisToDecideToPopup) {
            return;
        }
        // Se estima el total a partir de lo que se avanzo, y se muestra solo si lo que falta lo
        // justifica. Ver la nota de la clase.
        int avance = nv - min;
        if (avance <= 0) {
            return;
        }
        long estimado = dur * (max - min) / avance;
        if (estimado - dur < millisToPopup) {
            return;
        }
        mostrar();
    }

    /** Arma y muestra el cartel; sin pantalla no llega a mostrarse. */
    private void mostrar() {
        myBar = new JProgressBar();
        myBar.setMinimum(min);
        myBar.setMaximum(max);
        myBar.setValue(v);
        if (note != null) {
            noteLabel = new JLabel(note);
        }
        try {
            JOptionPane pane = new JOptionPane(new Object[] {message, noteLabel, myBar},
                    JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null,
                    new Object[] {UIManager.getString("OptionPane.cancelButtonText") != null
                            ? UIManager.getString("OptionPane.cancelButtonText") : "Cancel"},
                    null);
            dialog = pane.createDialog(parentComponent,
                    UIManager.getString("ProgressMonitor.progressText") != null
                            ? UIManager.getString("ProgressMonitor.progressText") : "Progress...");
            dialog.setVisible(true);
        } catch (java.awt.HeadlessException e) {
            // Sin pantalla no hay cartel; la cuenta sigue igual. Ver la nota de la clase.
            dialog = null;
        }
    }

    private void actualizar() {
        if (myBar != null) {
            myBar.setValue(v);
        }
    }

    /**
     * Cierra el cartel si estaba puesto.
     *
     * <p>Se puede llamar aunque nunca haya aparecido; es lo normal cuando la tarea fue rapida.
     */
    public void close() {
        cerrado = true;
        if (dialog != null) {
            dialog.setVisible(false);
            dialog.dispose();
            dialog = null;
        }
        myBar = null;
        noteLabel = null;
    }

    public int getMinimum() {
        return min;
    }

    public void setMinimum(int m) {
        min = m;
        if (myBar != null) {
            myBar.setMinimum(m);
        }
    }

    public int getMaximum() {
        return max;
    }

    public void setMaximum(int m) {
        max = m;
        if (myBar != null) {
            myBar.setMaximum(m);
        }
    }

    /** Si el usuario apreto Cancelar; ver la nota de la clase. */
    public boolean isCanceled() {
        return canceled;
    }

    /** Cuanto se espera antes de siquiera pensar en mostrar el cartel. */
    public void setMillisToDecideToPopup(int millisToDecideToPopup) {
        this.millisToDecideToPopup = millisToDecideToPopup;
    }

    public int getMillisToDecideToPopup() {
        return millisToDecideToPopup;
    }

    /** Cuanto tiene que faltar, estimado, para que valga la pena mostrarlo. */
    public void setMillisToPopup(int millisToPopup) {
        this.millisToPopup = millisToPopup;
    }

    public int getMillisToPopup() {
        return millisToPopup;
    }

    /**
     * El texto que cambia mientras la tarea avanza.
     *
     * <p>Nulo al construir el monitor significa que no va a haber ninguno, y ponerlo despues no lo
     * agrega: el cartel se arma una vez y no cambia de forma.
     */
    public void setNote(String note) {
        this.note = note;
        if (noteLabel != null) {
            noteLabel.setText(note);
        }
    }

    public String getNote() {
        return note;
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
