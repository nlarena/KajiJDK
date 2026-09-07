import javax.swing.text.html.CSS;
import javax.swing.text.html.CSS$Attribute;
import javax.swing.text.html.FormSubmitEvent;
import javax.swing.text.html.HTMLFrameHyperlinkEvent;
import javax.swing.text.html.Option;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML$Attribute;
import javax.swing.event.HyperlinkEvent$EventType;

/**
 * Las propiedades de CSS y las clases chicas del paquete HTML, contra el JDK.
 *
 * <p>De cada propiedad interesa el nombre, el valor por omision y si se hereda. Los tres son datos
 * que hay que saber: no se deducen del nombre ni de ninguna regla.
 */
public class Html2 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    public static int run() {
        CSS$Attribute[] xs = CSS.getAllAttributeKeys();
        linea("propiedades=" + xs.length);
        for (int i = 0; i < xs.length; i++) {
            linea("  " + i + " " + xs[i] + " omision=" + xs[i].getDefaultValue()
                    + " hereda=" + xs[i].isInherited());
        }
        String[] busca = {"color", "COLOR", "font-size", "margin-left-ltr", "zz", ""};
        for (int i = 0; i < busca.length; i++) {
            linea("busca [" + busca[i] + "]=" + CSS.getAttribute(busca[i]));
        }
        CSS$Attribute[] copia = CSS.getAllAttributeKeys();
        copia[0] = null;
        linea("copia intacta=" + (CSS.getAllAttributeKeys()[0] == CSS$Attribute.BACKGROUND));

        // Una opcion de lista: el valor sale del atributo o, si no esta, del texto.
        SimpleAttributeSet a1 = new SimpleAttributeSet();
        a1.addAttribute(HTML$Attribute.VALUE, "ar");
        Option o1 = new Option(a1);
        o1.setLabel("Argentina");
        linea("opcion1 texto=" + o1 + " valor=" + o1.getValue() + " marcada=" + o1.isSelected());

        SimpleAttributeSet a2 = new SimpleAttributeSet();
        a2.addAttribute(HTML$Attribute.SELECTED, "selected");
        Option o2 = new Option(a2);
        o2.setLabel("Uruguay");
        linea("opcion2 texto=" + o2 + " valor=" + o2.getValue() + " marcada=" + o2.isSelected());

        // Los atributos se copian al construir: cambiar el original no toca la opcion.
        a2.addAttribute(HTML$Attribute.VALUE, "uy");
        linea("opcion2 despues valor=" + o2.getValue()
                + " atributos=" + o2.getAttributes().getAttributeCount());

        // Un enlace con destino.
        HTMLFrameHyperlinkEvent ev = new HTMLFrameHyperlinkEvent("origen",
                HyperlinkEvent$EventType.ACTIVATED, null, "ver", "_top");
        linea("enlace destino=" + ev.getTarget() + " desc=" + ev.getDescription()
                + " tipo=" + ev.getEventType() + " url=" + ev.getURL());

        linea("metodos=" + FormSubmitEvent.MethodType.GET + ","
                + FormSubmitEvent.MethodType.POST
                + " cantidad=" + FormSubmitEvent.MethodType.values().length
                + " valueOf=" + FormSubmitEvent.MethodType.valueOf("POST"));
        return 0;
    }
}
