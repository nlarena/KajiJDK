import java.awt.Dimension;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.Spring;
import javax.swing.SpringLayout;

/**
 * Los resortes y el acomodador que los usa, contra el JDK.
 *
 * <p>Un {@link Spring} es aritmetica pura, asi que se comparan los cuatro numeros de cada
 * combinacion y tambien que <em>ponerle un valor</em> a una combinacion se reparta bien entre sus
 * partes, que es la mitad menos evidente de la clase.
 *
 * <p>Del acomodador, la historia de restricciones: poner una tercera en el mismo eje descarta la mas
 * vieja, y eso hace que el orden importe.
 */
public class Res1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Los cuatro numeros de un resorte. */
    static String s(Spring r) {
        if (r == null) {
            return "-";
        }
        return "[" + n(r.getMinimumValue()) + " " + n(r.getPreferredValue()) + " "
                + n(r.getMaximumValue()) + " val=" + n(r.getValue()) + "]";
    }

    static String n(int v) {
        return (v == Spring.UNSET) ? "UNSET" : String.valueOf(v);
    }

    static void basicos() {
        linea("--- resortes basicos ---");
        linea("UNSET=" + Spring.UNSET);
        Spring fijo = Spring.constant(7);
        linea("constante 7 " + s(fijo));
        Spring rango = Spring.constant(2, 5, 9);
        linea("constante 2/5/9 " + s(rango));

        rango.setValue(5);
        linea("puesto en el preferido " + s(rango));
        rango.setValue(9);
        linea("puesto en el maximo " + s(rango));
        rango.setValue(2);
        linea("puesto en el minimo " + s(rango));
        rango.setValue(100);
        linea("puesto fuera de rango " + s(rango));
        rango.setValue(Spring.UNSET);
        linea("devuelto a UNSET " + s(rango));

        Spring menos = Spring.minus(rango);
        linea("menos " + s(menos));
        Spring menosMenos = Spring.minus(menos);
        linea("menos menos " + s(menosMenos));

        Spring doble = Spring.scale(rango, 2.0f);
        linea("por 2 " + s(doble));
        Spring mitad = Spring.scale(rango, 0.5f);
        linea("por 0.5 " + s(mitad));
        Spring negativo = Spring.scale(rango, -1.5f);
        linea("por -1.5 " + s(negativo));
        Spring cero = Spring.scale(rango, 0.0f);
        linea("por 0 " + s(cero));

        Spring suma = Spring.sum(Spring.constant(1, 2, 3), Spring.constant(10, 20, 30));
        linea("suma " + s(suma));
        Spring mayor = Spring.max(Spring.constant(1, 2, 30), Spring.constant(10, 20, 3));
        linea("maximo " + s(mayor));

        try {
            Spring.sum(null, fijo);
            linea("suma con nulo aceptada");
        } catch (NullPointerException e) {
            linea("suma con nulo rechazada: " + e.getMessage());
        }
        try {
            Spring.minus(null);
            linea("menos de nulo aceptado");
        } catch (NullPointerException e) {
            linea("menos de nulo rechazado");
        }
        try {
            Spring.scale(null, 2.0f);
            linea("escala de nulo aceptada");
        } catch (NullPointerException e) {
            linea("escala de nulo rechazada");
        }
        try {
            Spring.width(null);
            linea("ancho de nulo aceptado");
        } catch (NullPointerException e) {
            linea("ancho de nulo rechazado");
        }
    }

    static void repartos() {
        linea("--- ponerle valor a una combinacion ---");
        Spring a = Spring.constant(0, 10, 20);
        Spring b = Spring.constant(0, 10, 100);
        Spring suma = Spring.sum(a, b);
        linea("antes suma=" + s(suma) + " a=" + s(a) + " b=" + s(b));
        // El que tiene mas margen se lleva mas: es el reparto por tension.
        suma.setValue(60);
        linea("suma en 60 -> a=" + s(a) + " b=" + s(b) + " suma=" + s(suma));
        suma.setValue(Spring.UNSET);
        linea("limpiada -> a=" + s(a) + " b=" + s(b) + " suma=" + s(suma));
        suma.setValue(10);
        linea("suma en 10 -> a=" + s(a) + " b=" + s(b));
        suma.setValue(Spring.UNSET);

        Spring c = Spring.constant(0, 10, 20);
        Spring d = Spring.constant(0, 30, 100);
        Spring mayor = Spring.max(c, d);
        linea("maximo antes=" + s(mayor) + " c=" + s(c) + " d=" + s(d));
        mayor.setValue(50);
        linea("maximo en 50 -> c=" + s(c) + " d=" + s(d));

        // Un resorte que no puede moverse: su tension es una division por cero.
        Spring quieto = Spring.constant(5);
        Spring conQuieto = Spring.sum(quieto, Spring.constant(0, 10, 20));
        linea("con uno quieto antes=" + s(conQuieto));
        conQuieto.setValue(20);
        linea("puesto en 20 -> quieto=" + s(quieto)
                + " el otro=" + n(conQuieto.getValue() - quieto.getValue()));

        // Poner un valor a traves de una escala vuelve dividido.
        Spring base = Spring.constant(0, 10, 100);
        Spring esc = Spring.scale(base, 2.0f);
        esc.setValue(30);
        linea("escala en 30 -> base=" + s(base) + " escala=" + s(esc));
        Spring neg = Spring.minus(base);
        neg.setValue(-40);
        linea("menos en -40 -> base=" + s(base));
    }

    /** Un panel de medidas fijas. */
    static JPanel caja(int min, int pref, int max) {
        JPanel p = new JPanel();
        p.setMinimumSize(new Dimension(min, min));
        p.setPreferredSize(new Dimension(pref, pref));
        p.setMaximumSize(new Dimension(max, max));
        return p;
    }

    static void deComponente() {
        linea("--- resortes de un componente ---");
        JPanel p = caja(10, 30, 200);
        Spring an = Spring.width(p);
        Spring al = Spring.height(p);
        linea("ancho " + s(an));
        linea("alto " + s(al));
        // El maximo se recorta: ver la nota de Spring.
        JPanel sinTope = new JPanel();
        sinTope.setMinimumSize(new Dimension(1, 1));
        sinTope.setPreferredSize(new Dimension(2, 2));
        sinTope.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        linea("sin tope ancho " + s(Spring.width(sinTope)));
        linea("Short.MAX_VALUE=" + Short.MAX_VALUE);
    }

    static String rect(Rectangle r) {
        return r.x + "," + r.y + "," + r.width + "," + r.height;
    }

    static void acomodar() {
        linea("--- el acomodador ---");
        linea("nombres=" + SpringLayout.NORTH + "," + SpringLayout.SOUTH + ","
                + SpringLayout.EAST + "," + SpringLayout.WEST + ","
                + SpringLayout.HORIZONTAL_CENTER + "," + SpringLayout.VERTICAL_CENTER + ","
                + SpringLayout.BASELINE + "," + SpringLayout.WIDTH + "," + SpringLayout.HEIGHT);

        // La historia: con dos restricciones el resto se deriva.
        SpringLayout.Constraints c = new SpringLayout.Constraints();
        linea("vacias oeste=" + s(c.getConstraint(SpringLayout.WEST))
                + " ancho=" + s(c.getConstraint(SpringLayout.WIDTH))
                + " este=" + s(c.getConstraint(SpringLayout.EAST)));
        c.setX(Spring.constant(10));
        c.setWidth(Spring.constant(50));
        linea("oeste 10 y ancho 50 -> este=" + s(c.getConstraint(SpringLayout.EAST))
                + " centro=" + s(c.getConstraint(SpringLayout.HORIZONTAL_CENTER)));

        // La tercera descarta la mas vieja: el oeste se va y se deriva de las otras dos.
        c.setConstraint(SpringLayout.EAST, Spring.constant(100));
        linea("mas este 100 -> oeste=" + s(c.getConstraint(SpringLayout.WEST))
                + " ancho=" + s(c.getConstraint(SpringLayout.WIDTH))
                + " este=" + s(c.getConstraint(SpringLayout.EAST)));

        SpringLayout.Constraints v = new SpringLayout.Constraints();
        v.setY(Spring.constant(5));
        v.setHeight(Spring.constant(20));
        linea("norte 5 y alto 20 -> sur=" + s(v.getConstraint(SpringLayout.SOUTH))
                + " centro=" + s(v.getConstraint(SpringLayout.VERTICAL_CENTER)));

        SpringLayout.Constraints xy = new SpringLayout.Constraints(Spring.constant(1),
                Spring.constant(2));
        linea("con x e y oeste=" + s(xy.getConstraint(SpringLayout.WEST))
                + " norte=" + s(xy.getConstraint(SpringLayout.NORTH))
                + " ancho=" + s(xy.getConstraint(SpringLayout.WIDTH)));

        SpringLayout.Constraints cuatro = new SpringLayout.Constraints(Spring.constant(1),
                Spring.constant(2), Spring.constant(3), Spring.constant(4));
        linea("con cuatro este=" + s(cuatro.getConstraint(SpringLayout.EAST))
                + " sur=" + s(cuatro.getConstraint(SpringLayout.SOUTH)));

        linea("nombre desconocido=" + cuatro.getConstraint("Noreste"));

        // Colocar de verdad.
        JPanel panel = new JPanel();
        SpringLayout sl = new SpringLayout();
        panel.setLayout(sl);
        JPanel uno = caja(10, 30, 200);
        JPanel dos = caja(10, 40, 200);
        panel.add(uno);
        panel.add(dos);
        sl.putConstraint(SpringLayout.WEST, uno, 5, SpringLayout.WEST, panel);
        sl.putConstraint(SpringLayout.NORTH, uno, 5, SpringLayout.NORTH, panel);
        sl.putConstraint(SpringLayout.WEST, dos, 10, SpringLayout.EAST, uno);
        sl.putConstraint(SpringLayout.NORTH, dos, 0, SpringLayout.NORTH, uno);
        panel.setSize(300, 200);
        sl.layoutContainer(panel);
        linea("colocados uno=" + rect(uno.getBounds()) + " dos=" + rect(dos.getBounds()));

        linea("alineacion x=" + sl.getLayoutAlignmentX(panel)
                + " y=" + sl.getLayoutAlignmentY(panel));
        linea("restricciones de uno son las mismas dos veces="
                + (sl.getConstraints(uno) == sl.getConstraints(uno)));
    }

    static void masAcomodar() {
        linea("--- mas del acomodador ---");

        // Atar por el este: el hijo se estira hasta el borde derecho del contenedor.
        JPanel panel = new JPanel();
        SpringLayout sl = new SpringLayout();
        panel.setLayout(sl);
        JPanel uno = caja(10, 30, 200);
        panel.add(uno);
        sl.putConstraint(SpringLayout.WEST, uno, 5, SpringLayout.WEST, panel);
        sl.putConstraint(SpringLayout.EAST, uno, -5, SpringLayout.EAST, panel);
        sl.putConstraint(SpringLayout.NORTH, uno, 5, SpringLayout.NORTH, panel);
        sl.putConstraint(SpringLayout.SOUTH, uno, -5, SpringLayout.SOUTH, panel);
        panel.setSize(200, 100);
        sl.layoutContainer(panel);
        linea("estirado=" + rect(uno.getBounds()));
        panel.setSize(400, 300);
        sl.layoutContainer(panel);
        linea("mas grande=" + rect(uno.getBounds()));

        // Centrado.
        JPanel p2 = new JPanel();
        SpringLayout sl2 = new SpringLayout();
        p2.setLayout(sl2);
        JPanel dos = caja(10, 40, 200);
        p2.add(dos);
        sl2.putConstraint(SpringLayout.HORIZONTAL_CENTER, dos, 0,
                SpringLayout.HORIZONTAL_CENTER, p2);
        sl2.putConstraint(SpringLayout.VERTICAL_CENTER, dos, 0,
                SpringLayout.VERTICAL_CENTER, p2);
        p2.setSize(200, 100);
        sl2.layoutContainer(p2);
        linea("centrado=" + rect(dos.getBounds()));

        // Un ciclo: A a la derecha de B y B a la derecha de A. No se cuelga.
        JPanel p3 = new JPanel();
        SpringLayout sl3 = new SpringLayout();
        p3.setLayout(sl3);
        JPanel a = caja(10, 20, 100);
        JPanel b = caja(10, 20, 100);
        p3.add(a);
        p3.add(b);
        sl3.putConstraint(SpringLayout.WEST, a, 5, SpringLayout.EAST, b);
        sl3.putConstraint(SpringLayout.WEST, b, 5, SpringLayout.EAST, a);
        p3.setSize(200, 100);
        sl3.layoutContainer(p3);
        linea("con ciclo a=" + rect(a.getBounds()) + " b=" + rect(b.getBounds()));

        // Restricciones tomadas de un componente.
        JPanel c = caja(10, 25, 200);
        c.setBounds(7, 9, 25, 25);
        SpringLayout.Constraints deComp = new SpringLayout.Constraints(c);
        linea("de un componente oeste=" + s(deComp.getConstraint(SpringLayout.WEST))
                + " ancho=" + s(deComp.getConstraint(SpringLayout.WIDTH)));
        linea("y su este=" + s(deComp.getConstraint(SpringLayout.EAST)));

        // Sacar un componente olvida sus restricciones.
        SpringLayout sl4 = new SpringLayout();
        JPanel p4 = new JPanel();
        p4.setLayout(sl4);
        JPanel d = caja(10, 30, 200);
        p4.add(d);
        sl4.putConstraint(SpringLayout.WEST, d, 33, SpringLayout.WEST, p4);
        linea("antes de sacar oeste=" + s(sl4.getConstraint(SpringLayout.WEST, d)));
        sl4.removeLayoutComponent(d);
        linea("despues de sacar oeste=" + s(sl4.getConstraint(SpringLayout.WEST, d)));
    }

    public static int run() {
        basicos();
        repartos();
        deComponente();
        acomodar();
        masAcomodar();
        return 0;
    }
}
