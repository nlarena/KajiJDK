package javax.swing.text.html;

import java.awt.Component;

import javax.swing.text.AttributeSet;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;

/**
 * The view of an {@code <object>}: a Java component embedded in the page.
 *
 * <h2>How the component is chosen</h2>
 *
 * <p>The <code>classid</code> attribute names a class. It is loaded by reflection and built, and
 * if it is a {@code Component} it is shown. The <code>&lt;param&gt;</code>s inside are passed to
 * it as properties through its {@code set} methods.
 *
 * <h2>What happens if it cannot be done</h2>
 *
 * <p>A text is shown saying which class was missing. Nothing is thrown: a page with an object
 * that is not there has to be viewable all the same, with a hole where it would be.
 *
 * <p>Loading a class the document names is a large permission. Whoever shows HTML from outside
 * should decide it on purpose, and that is why it is worth looking at this view before enabling
 * it in a program that opens somebody else's pages.
 */
public class ObjectView extends ComponentView {

    /** An object view on that element. */
    public ObjectView(Element elem) {
        super(elem);
    }

    /** It builds the component the <code>classid</code> attribute names. */
    protected Component createComponent() {
        AttributeSet attr = getElement().getAttributes();
        String classname = (String) attr.getAttribute(HTML.Attribute.CLASSID);
        try {
            Class<?> c = Class.forName(classname, true,
                    Thread.currentThread().getContextClassLoader());
            Object o = c.getDeclaredConstructor().newInstance();
            if (o instanceof Component) {
                Component comp = (Component) o;
                setParameters(comp, attr);
                return comp;
            }
        } catch (Throwable e) {
            // Any problem ends up in the notice; see the class note.
        }
        return getUnloadableRepresentation();
    }

    /** The notice shown when the object could not be built. */
    private Component getUnloadableRepresentation() {
        Component comp = new javax.swing.JLabel("??");
        comp.setForeground(java.awt.Color.red);
        return comp;
    }

    /**
     * It passes the {@code <param>}s inside to the component.
     *
     * <p>Each one is looked up as a {@code setName(String)} method. Only those with a single
     * {@code String} argument are tried: trying other conversions would make a badly written value
     * call a method that was not the right one.
     */
    private void setParameters(Component comp, AttributeSet attr) {
        Element elem = getElement();
        for (int i = 0; i < elem.getElementCount(); i++) {
            Element child = elem.getElement(i);
            AttributeSet a = child.getAttributes();
            if (a.getAttribute(javax.swing.text.StyleConstants.NameAttribute)
                    != HTML.Tag.PARAM) {
                continue;
            }
            String name = (String) a.getAttribute(HTML.Attribute.NAME);
            String value = (String) a.getAttribute(HTML.Attribute.VALUE);
            if (name == null || value == null) {
                continue;
            }
            String method = "set" + Character.toUpperCase(name.charAt(0))
                    + name.substring(1);
            try {
                java.lang.reflect.Method m = comp.getClass().getMethod(method,
                        new Class<?>[] {String.class});
                m.invoke(comp, new Object[] {value});
            } catch (Throwable e) {
                // A parameter the component does not accept is ignored.
            }
        }
    }
}
