import java.util.HashMap;
import java.util.Map;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.DynamicMBean;
import javax.management.JMX;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

/**
 * El proxy MXBean: lo que viaja son tipos abiertos, y el proxy los traduce.
 *
 * <p>Es lo unico que separa a un MXBean de un MBean comun, y es lo que hace que un cliente pueda
 * leer un MBean <b>sin tener ninguna de las clases del servidor</b>: en vez de un `Punto` viaja un
 * `CompositeData`, en vez de un `Color` viaja su nombre.
 *
 * <p>El MBean de esta prueba es un {@link DynamicMBean} escrito a mano que entrega los valores ya
 * en forma abierta. Es a proposito: asi lo que se prueba es <b>la conversion del proxy</b> y nada
 * mas, sin depender de que el servidor sepa exponer un MXBean. Un `StandardMBean(x, I.class, true)`
 * habria mezclado las dos mitades y una falla no diria cual de las dos esta mal.
 *
 * <p>Las expectativas salieron de preguntarle al JDK 25, no de leer la especificacion. Las dos que
 * no son obvias: los items de un `CompositeData` llevan el nombre de la <b>propiedad</b> en
 * minuscula --`getNombreLargo` da `nombreLargo`, no `NombreLargo`-- y la firma de una operacion
 * viaja con los tipos <b>abiertos</b>, que para un `int` sigue siendo `int`.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25 corriendo SU `javax.management`.
 */
public class MXBeanProxyTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    /** Un tipo compuesto: no es un MXBean, asi que viaja como sus datos y no como una referencia. */
    public interface Punto {
        int getX();

        int getY();

        String getNombreLargo();
    }

    public enum Color { ROJO, VERDE }

    /** La interfaz de administracion. */
    public interface CajaMXBean {
        String getNombre();

        int getEdad();

        Color getColor();

        Color[] getColores();

        Punto getPunto();

        void setNombre(String nombre);

        int sumar(int a, int b);
    }

    /** El MBean, que entrega y recibe **tipos abiertos**. Ver la nota de la clase. */
    public static final class Caja implements DynamicMBean {

        String nombre = "kaji";

        static CompositeType tipoDelPunto() throws Exception {
            // Los items van con el nombre de la propiedad en minuscula y en orden alfabetico, que es
            // lo que hace el JDK.
            return new CompositeType(
                    Punto.class.getName(), Punto.class.getName(),
                    new String[] {"nombreLargo", "x", "y"},
                    new String[] {"nombreLargo", "x", "y"},
                    new OpenType<?>[] {SimpleType.STRING, SimpleType.INTEGER, SimpleType.INTEGER});
        }

        public Object getAttribute(String attribute) throws javax.management.MBeanException {
            try {
                if ("Nombre".equals(attribute)) {
                    return this.nombre;
                }
                if ("Edad".equals(attribute)) {
                    return Integer.valueOf(7);
                }
                if ("Color".equals(attribute)) {
                    return "VERDE";
                }
                if ("Colores".equals(attribute)) {
                    return new String[] {"ROJO", "VERDE"};
                }
                if ("Punto".equals(attribute)) {
                    Map<String, Object> items = new HashMap<String, Object>();
                    items.put("nombreLargo", "origen");
                    items.put("x", Integer.valueOf(3));
                    items.put("y", Integer.valueOf(4));
                    return new CompositeDataSupport(Caja.tipoDelPunto(), items);
                }
            } catch (Exception e) {
                throw new javax.management.MBeanException(e);
            }
            throw new javax.management.MBeanException(
                    new IllegalArgumentException("no hay atributo " + attribute));
        }

        public void setAttribute(Attribute attribute) {
            if ("Nombre".equals(attribute.getName())) {
                this.nombre = (String) attribute.getValue();
            }
        }

        public AttributeList getAttributes(String[] attributes) {
            AttributeList l = new AttributeList();
            for (String a : attributes) {
                try {
                    l.add(new Attribute(a, this.getAttribute(a)));
                } catch (Exception e) {
                    // Un atributo que no esta se omite, que es lo que manda el contrato.
                }
            }
            return l;
        }

        public AttributeList setAttributes(AttributeList attributes) {
            return attributes;
        }

        public Object invoke(String actionName, Object[] params, String[] signature) {
            if ("sumar".equals(actionName)) {
                return Integer.valueOf(((Integer) params[0]).intValue()
                        + ((Integer) params[1]).intValue());
            }
            return null;
        }

        public MBeanInfo getMBeanInfo() {
            MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[] {
                new MBeanAttributeInfo("Nombre", "java.lang.String", "", true, true, false),
                new MBeanAttributeInfo("Edad", "int", "", true, false, false),
                new MBeanAttributeInfo("Color", "java.lang.String", "", true, false, false),
                new MBeanAttributeInfo("Colores", "[Ljava.lang.String;", "", true, false, false),
                new MBeanAttributeInfo("Punto", CompositeData.class.getName(), "", true, false,
                                       false),
            };
            MBeanOperationInfo[] ops = new MBeanOperationInfo[] {
                new MBeanOperationInfo("sumar", "", new MBeanParameterInfo[] {
                    new MBeanParameterInfo("a", "int", ""),
                    new MBeanParameterInfo("b", "int", ""),
                }, "int", MBeanOperationInfo.ACTION),
            };
            return new MBeanInfo(Caja.class.getName(), "", attrs, null, ops, null);
        }
    }

    public static int run() throws Exception {
        failures = 0;

        MBeanServer servidor = MBeanServerFactory.newMBeanServer();
        ObjectName nombre = new ObjectName("kaji:type=Caja");
        servidor.registerMBean(new Caja(), nombre);

        CajaMXBean caja = JMX.newMXBeanProxy(servidor, nombre, CajaMXBean.class);
        ok("el proxy se crea", caja != null);

        // ---- los tipos simples viajan tal cual
        ok("una cadena llega entera", "kaji".equals(caja.getNombre()));
        ok("un int tambien", caja.getEdad() == 7);

        // ---- una enumeracion viaja como su nombre y vuelve como la constante
        ok("una enumeracion vuelve como constante", caja.getColor() == Color.VERDE);

        // ---- un arreglo de enumeraciones, elemento por elemento
        Color[] colores = caja.getColores();
        ok("el arreglo llega con los dos", colores != null && colores.length == 2);
        ok("y son las constantes", colores[0] == Color.ROJO && colores[1] == Color.VERDE);

        // ---- un tipo compuesto viaja como CompositeData y vuelve como el tipo
        Punto p = caja.getPunto();
        ok("el compuesto vuelve", p != null);
        ok("con sus dos numeros", p.getX() == 3 && p.getY() == 4);
        ok("y con su cadena", "origen".equals(p.getNombreLargo()));

        // ---- escribir tambien convierte
        caja.setNombre("otro");
        ok("lo escrito se lee", "otro".equals(caja.getNombre()));

        // ---- una operacion, con su firma
        ok("una operacion llega y vuelve", caja.sumar(2, 3) == 5);

        // ---- el proxy responde a Object sin salir a la red
        ok("toString no es null", caja.toString() != null);
        ok("es igual a si mismo", caja.equals(caja));

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("MXBeanProxyTest " + MXBeanProxyTest.run());
    }
}
