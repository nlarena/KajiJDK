package javax.swing.text.html;

import java.awt.Component;

import javax.swing.text.AttributeSet;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;

/**
 * La vista de un {@code <object>}: un componente de Java incrustado en la pagina.
 *
 * <h2>Como se elige el componente</h2>
 *
 * <p>El atributo <code>classid</code> nombra una clase. Se carga por reflexion y se construye, y si
 * es un {@code Component} se muestra. Los <code>&lt;param&gt;</code> de adentro se le pasan como
 * propiedades por sus metodos {@code set}.
 *
 * <h2>Que pasa si no se puede</h2>
 *
 * <p>Se muestra un texto que dice que clase falto. No se lanza nada: una pagina con un objeto que
 * no esta tiene que poder verse igual, con un hueco donde estaria.
 *
 * <p>Cargar una clase que nombra el documento es un permiso grande. Quien muestre HTML de afuera
 * deberia decidirlo a proposito, y por eso conviene mirar esta vista antes de habilitarla en un
 * programa que abra paginas ajenas.
 */
public class ObjectView extends ComponentView {

    /** Una vista de objeto sobre ese elemento. */
    public ObjectView(Element elem) {
        super(elem);
    }

    /** Arma el componente que nombra el atributo <code>classid</code>. */
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
            // Cualquier problema termina en el cartel; ver la nota de la clase.
        }
        return getUnloadableRepresentation();
    }

    /** El cartel que se muestra cuando el objeto no se pudo armar. */
    private Component getUnloadableRepresentation() {
        Component comp = new javax.swing.JLabel("??");
        comp.setForeground(java.awt.Color.red);
        return comp;
    }

    /**
     * Le pasa al componente los {@code <param>} de adentro.
     *
     * <p>Cada uno se busca como un metodo {@code setNombre(String)}. Solo se prueban los de un
     * argumento de tipo {@code String}: probar otras conversiones haria que un valor mal escrito
     * llamara a un metodo que no era.
     */
    private void setParameters(Component comp, AttributeSet attr) {
        Element elem = getElement();
        for (int i = 0; i < elem.getElementCount(); i++) {
            Element hijo = elem.getElement(i);
            AttributeSet a = hijo.getAttributes();
            if (a.getAttribute(javax.swing.text.StyleConstants.NameAttribute)
                    != HTML.Tag.PARAM) {
                continue;
            }
            String nombre = (String) a.getAttribute(HTML.Attribute.NAME);
            String valor = (String) a.getAttribute(HTML.Attribute.VALUE);
            if (nombre == null || valor == null) {
                continue;
            }
            String metodo = "set" + Character.toUpperCase(nombre.charAt(0))
                    + nombre.substring(1);
            try {
                java.lang.reflect.Method m = comp.getClass().getMethod(metodo,
                        new Class<?>[] {String.class});
                m.invoke(comp, new Object[] {valor});
            } catch (Throwable e) {
                // Un parametro que el componente no acepta se ignora.
            }
        }
    }
}
