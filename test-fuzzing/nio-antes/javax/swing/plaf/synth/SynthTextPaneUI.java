package javax.swing.plaf.synth;

import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;

/**
 * Synth's text pane. It changes the prefix and nothing else.
 */
public class SynthTextPaneUI extends SynthEditorPaneUI {

    public SynthTextPaneUI() {
    }

    public static ComponentUI createUI(JComponent c) {
        return new SynthTextPaneUI();
    }

    protected String getPropertyPrefix() {
        return "TextPane.";
    }
}
