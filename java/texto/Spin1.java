import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractSpinnerModel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Los tres modelos de secuencia y el control que los usa, contra el JDK.
 *
 * <p>Lo que mas se compara es <em>de que tipo</em> sale cada valor: el modelo numerico hace la
 * cuenta con el tipo del valor de ahora, y eso decide cosas que no se ven mirando el numero. Un
 * paso de 0.5 sobre un entero avanza de a cero, y hay que verlo para creerlo.
 *
 * <p>Las fechas van con un instante fijo y no con el de ahora, para que las dos corridas comparen
 * lo mismo.
 */
public class Spin1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    /** Cuenta los avisos de cambio. */
    static class Espia implements ChangeListener {

        private int veces = 0;

        public void stateChanged(ChangeEvent e) {
            veces = veces + 1;
        }

        int vaciar() {
            int v = veces;
            veces = 0;
            return v;
        }
    }

    /** Que valor sale y de que tipo. */
    static String v(Object o) {
        if (o == null) {
            return "-";
        }
        String clase = o.getClass().getName();
        int p = clase.lastIndexOf('.');
        return o + "(" + clase.substring(p + 1) + ")";
    }

    static String paso(SpinnerModel m) {
        return v(m.getPreviousValue()) + " < " + v(m.getValue()) + " > " + v(m.getNextValue());
    }

    static void numeros() {
        SpinnerNumberModel n = new SpinnerNumberModel();
        linea("por omision " + paso(n) + " paso=" + v(n.getStepSize())
                + " min=" + v(n.getMinimum()) + " max=" + v(n.getMaximum()));

        Espia espia = new Espia();
        n.addChangeListener(espia);
        linea("oyentes=" + n.getChangeListeners().length);

        n.setValue(Integer.valueOf(5));
        linea("en 5 " + paso(n) + " |avisos=" + espia.vaciar());
        n.setValue(Integer.valueOf(5));
        linea("repetido |avisos=" + espia.vaciar());

        n.setMaximum(Integer.valueOf(6));
        linea("techo 6 " + paso(n) + " |avisos=" + espia.vaciar());
        n.setMaximum(Integer.valueOf(6));
        linea("techo repetido |avisos=" + espia.vaciar());
        n.setValue(Integer.valueOf(6));
        linea("en el techo " + paso(n) + " |avisos=" + espia.vaciar());
        // El valor puede salirse: los limites solo apagan las flechas.
        n.setValue(Integer.valueOf(99));
        linea("fuera de rango " + paso(n) + " numero=" + v(n.getNumber())
                + " |avisos=" + espia.vaciar());
        n.setMaximum(null);
        n.setValue(Integer.valueOf(0));
        espia.vaciar();

        n.setMinimum(Integer.valueOf(0));
        linea("piso 0 " + paso(n) + " |avisos=" + espia.vaciar());
        n.setMinimum(null);
        linea("sin piso " + paso(n) + " |avisos=" + espia.vaciar());
        n.setMinimum(null);
        linea("sin piso repetido |avisos=" + espia.vaciar());

        try {
            n.setStepSize(null);
            linea("paso nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("paso nulo rechazado: " + e.getMessage());
        }
        try {
            n.setValue("hola");
            linea("texto aceptado");
        } catch (IllegalArgumentException e) {
            linea("texto rechazado: " + e.getMessage());
        }
        try {
            n.setValue(null);
            linea("nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("nulo rechazado: " + e.getMessage());
        }
        linea("tras rechazos " + paso(n) + " |avisos=" + espia.vaciar());

        // El tipo del resultado es el del valor de ahora, no el del paso.
        linea("--- el tipo lo manda el valor ---");
        SpinnerNumberModel d = new SpinnerNumberModel(Double.valueOf(1.5), null, null,
                Double.valueOf(0.25));
        linea("doble " + paso(d));
        SpinnerNumberModel f = new SpinnerNumberModel(Float.valueOf(1.5f), null, null,
                Double.valueOf(0.25));
        linea("flotante con paso doble " + paso(f));
        SpinnerNumberModel l = new SpinnerNumberModel(Long.valueOf(7L), null, null,
                Integer.valueOf(3));
        linea("largo con paso entero " + paso(l));
        SpinnerNumberModel s = new SpinnerNumberModel(Short.valueOf((short) 7), null, null,
                Integer.valueOf(3));
        linea("corto " + paso(s));
        SpinnerNumberModel b = new SpinnerNumberModel(Byte.valueOf((byte) 7), null, null,
                Integer.valueOf(3));
        linea("byte " + paso(b));
        SpinnerNumberModel trampa = new SpinnerNumberModel(Integer.valueOf(0), null, null,
                Double.valueOf(0.5));
        linea("entero con paso 0.5 " + paso(trampa));
        SpinnerNumberModel desborda = new SpinnerNumberModel(Byte.valueOf((byte) 127), null, null,
                Integer.valueOf(1));
        linea("byte en 127 " + paso(desborda));

        // Los limites se comparan con compareTo, no se convierten: un tope de otro tipo que el
        // valor revienta con ClassCastException adentro del constructor.
        try {
            new SpinnerNumberModel(Integer.valueOf(3), Double.valueOf(0.5), Double.valueOf(3.5),
                    Integer.valueOf(1));
            linea("limites dobles sobre entero aceptados");
        } catch (ClassCastException e) {
            linea("limites dobles sobre entero revientan");
        }

        linea("--- constructores ---");
        SpinnerNumberModel ei = new SpinnerNumberModel(2, 0, 10, 2);
        linea("enteros " + paso(ei) + " min=" + v(ei.getMinimum()) + " max=" + v(ei.getMaximum()));
        SpinnerNumberModel ed = new SpinnerNumberModel(2.0, 0.0, 10.0, 2.0);
        linea("dobles " + paso(ed) + " min=" + v(ed.getMinimum()) + " max=" + v(ed.getMaximum()));
        try {
            new SpinnerNumberModel(null, null, null, Integer.valueOf(1));
            linea("valor nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("valor nulo rechazado: " + e.getMessage());
        }
        try {
            new SpinnerNumberModel(Integer.valueOf(5), Integer.valueOf(6), null,
                    Integer.valueOf(1));
            linea("valor bajo el piso aceptado");
        } catch (IllegalArgumentException e) {
            linea("valor bajo el piso rechazado: " + e.getMessage());
        }
        try {
            new SpinnerNumberModel(Integer.valueOf(5), null, Integer.valueOf(4),
                    Integer.valueOf(1));
            linea("valor sobre el techo aceptado");
        } catch (IllegalArgumentException e) {
            linea("valor sobre el techo rechazado: " + e.getMessage());
        }
    }

    static void listas() {
        linea("--- listas ---");
        SpinnerListModel m = new SpinnerListModel();
        linea("por omision " + paso(m) + " lista=" + m.getList());

        SpinnerListModel a = new SpinnerListModel(new Object[] {"uno", "dos", "tres"});
        Espia espia = new Espia();
        a.addChangeListener(espia);
        linea("arreglo " + paso(a));
        a.setValue("dos");
        linea("en dos " + paso(a) + " |avisos=" + espia.vaciar());
        a.setValue("dos");
        linea("repetido |avisos=" + espia.vaciar());
        a.setValue("tres");
        linea("en el ultimo " + paso(a) + " |avisos=" + espia.vaciar());
        try {
            a.setValue("cuatro");
            linea("ajeno aceptado");
        } catch (IllegalArgumentException e) {
            linea("ajeno rechazado: " + e.getMessage());
        }
        linea("tras rechazo " + paso(a) + " |avisos=" + espia.vaciar());

        // Cambiar la lista vuelve al primero, aunque el valor de antes siga estando.
        List<String> otra = new ArrayList<String>();
        otra.add("tres");
        otra.add("cuatro");
        a.setList(otra);
        linea("lista nueva " + paso(a) + " |avisos=" + espia.vaciar());
        a.setList(otra);
        linea("misma lista |avisos=" + espia.vaciar());

        // Con repetidos gana el primero.
        SpinnerListModel rep = new SpinnerListModel(new Object[] {"a", "b", "a"});
        rep.setValue("a");
        linea("repetidos " + paso(rep));

        try {
            new SpinnerListModel(new Object[0]);
            linea("arreglo vacio aceptado");
        } catch (IllegalArgumentException e) {
            linea("arreglo vacio rechazado: " + e.getMessage());
        }
        try {
            new SpinnerListModel((List<?>) null);
            linea("lista nula aceptada");
        } catch (IllegalArgumentException e) {
            linea("lista nula rechazada: " + e.getMessage());
        }
        try {
            a.setList(new ArrayList<String>());
            linea("vaciar aceptado");
        } catch (IllegalArgumentException e) {
            linea("vaciar rechazado: " + e.getMessage());
        }
    }

    static void fechas() {
        linea("--- fechas ---");
        // Un instante fijo: 2020-03-15 12:00:00 UTC.
        long fijo = 1584273600000L;
        Date d = new Date(fijo);
        SpinnerDateModel m = new SpinnerDateModel(d, null, null, Calendar.DAY_OF_MONTH);
        Espia espia = new Espia();
        m.addChangeListener(espia);
        linea("campo=" + m.getCalendarField() + " dia=" + Calendar.DAY_OF_MONTH
                + " mes=" + Calendar.MONTH + " anio=" + Calendar.YEAR);
        linea("valor=" + ((Date) m.getValue()).getTime() + " fecha=" + m.getDate().getTime());
        linea("dia sig=" + ((Date) m.getNextValue()).getTime()
                + " ant=" + ((Date) m.getPreviousValue()).getTime());

        m.setCalendarField(Calendar.MONTH);
        linea("por mes sig=" + ((Date) m.getNextValue()).getTime()
                + " ant=" + ((Date) m.getPreviousValue()).getTime()
                + " |avisos=" + espia.vaciar());
        m.setCalendarField(Calendar.MONTH);
        linea("mismo campo |avisos=" + espia.vaciar());
        m.setCalendarField(Calendar.YEAR);
        linea("por anio sig=" + ((Date) m.getNextValue()).getTime()
                + " ant=" + ((Date) m.getPreviousValue()).getTime()
                + " |avisos=" + espia.vaciar());

        // Un mes no dura siempre lo mismo: la diferencia en milisegundos cambia segun el mes.
        m.setCalendarField(Calendar.MONTH);
        long antes = ((Date) m.getPreviousValue()).getTime();
        long despues = ((Date) m.getNextValue()).getTime();
        linea("mes anterior dura=" + (fijo - antes) + " el siguiente dura=" + (despues - fijo));
        espia.vaciar();

        m.setCalendarField(Calendar.DAY_OF_MONTH);
        m.setEnd(new Date(fijo));
        linea("tope en el valor sig=" + m.getNextValue()
                + " ant=" + ((Date) m.getPreviousValue()).getTime()
                + " |avisos=" + espia.vaciar());
        m.setStart(new Date(fijo));
        linea("piso en el valor sig=" + m.getNextValue() + " ant=" + m.getPreviousValue()
                + " |avisos=" + espia.vaciar());
        linea("inicio=" + ((Date) m.getStart()).getTime()
                + " fin=" + ((Date) m.getEnd()).getTime());
        m.setEnd(null);
        m.setStart(null);
        linea("sin topes sig=" + ((Date) m.getNextValue()).getTime()
                + " |avisos=" + espia.vaciar());
        m.setStart(null);
        linea("sin piso repetido |avisos=" + espia.vaciar());

        m.setValue(new Date(fijo + 1000));
        linea("valor nuevo=" + ((Date) m.getValue()).getTime() + " |avisos=" + espia.vaciar());
        m.setValue(new Date(fijo + 1000));
        linea("valor repetido |avisos=" + espia.vaciar());
        try {
            m.setValue("hola");
            linea("texto aceptado");
        } catch (IllegalArgumentException e) {
            linea("texto rechazado: " + e.getMessage());
        }
        try {
            m.setCalendarField(999);
            linea("campo 999 aceptado");
        } catch (IllegalArgumentException e) {
            linea("campo 999 rechazado: " + e.getMessage());
        }
        try {
            new SpinnerDateModel(null, null, null, Calendar.DAY_OF_MONTH);
            linea("fecha nula aceptada");
        } catch (IllegalArgumentException e) {
            linea("fecha nula rechazada: " + e.getMessage());
        }
        try {
            new SpinnerDateModel(d, new Date(fijo + 1), null, Calendar.DAY_OF_MONTH);
            linea("fecha bajo el inicio aceptada");
        } catch (IllegalArgumentException e) {
            linea("fecha bajo el inicio rechazada: " + e.getMessage());
        }
        // DAY_OF_WEEK sirve; ZONE_OFFSET no, aunque tambien sea un campo de Calendar.
        try {
            new SpinnerDateModel(d, null, null, Calendar.DAY_OF_WEEK);
            linea("dia de la semana aceptado");
        } catch (IllegalArgumentException e) {
            linea("dia de la semana rechazado");
        }
        try {
            new SpinnerDateModel(d, null, null, Calendar.ZONE_OFFSET);
            linea("desplazamiento de zona aceptado");
        } catch (IllegalArgumentException e) {
            linea("desplazamiento de zona rechazado");
        }
    }

    static String editor(JSpinner s) {
        String c = s.getEditor().getClass().getName();
        int p = c.lastIndexOf('.');
        return c.substring(p + 1);
    }

    static void control() {
        linea("--- el control ---");
        JSpinner s = new JSpinner();
        linea("clase de aspecto=" + s.getUIClassID() + " modelo="
                + s.getModel().getClass().getSimpleName() + " editor=" + editor(s));
        linea("valor=" + v(s.getValue()) + " sig=" + v(s.getNextValue())
                + " ant=" + v(s.getPreviousValue()));
        linea("oyentes=" + s.getChangeListeners().length);

        Espia espia = new Espia();
        s.addChangeListener(espia);
        s.setValue(Integer.valueOf(4));
        linea("en 4 valor=" + v(s.getValue()) + " |avisos=" + espia.vaciar());
        s.getModel().setValue(Integer.valueOf(9));
        linea("por el modelo=" + v(s.getValue()) + " |avisos=" + espia.vaciar());
        s.removeChangeListener(espia);
        s.setValue(Integer.valueOf(1));
        linea("sin oyente |avisos=" + espia.vaciar() + " oyentes=" + s.getChangeListeners().length);

        // Cambiar el modelo cambia el editor, salvo que se haya puesto uno a mano.
        s.setModel(new SpinnerListModel(new Object[] {"a", "b"}));
        linea("modelo de lista editor=" + editor(s) + " valor=" + v(s.getValue()));
        s.setModel(new SpinnerDateModel(new Date(1584273600000L), null, null,
                Calendar.DAY_OF_MONTH));
        linea("modelo de fecha editor=" + editor(s));

        JSpinner t = new JSpinner(new SpinnerListModel(new Object[] {"x", "y", "z"}));
        linea("editor de lista=" + editor(t) + " " + paso(t.getModel()));
        // Que el editor sepa cual es su control depende de que el aspecto lo haya agregado como
        // hijo, y esta biblioteca no instala aspectos: eso queda afuera de la comparacion.
        t.setEditor(new javax.swing.JLabel("propio"));
        linea("editor propio=" + editor(t));
        t.setModel(new SpinnerListModel(new Object[] {"q"}));
        linea("tras cambiar el modelo sigue=" + editor(t));

        try {
            new JSpinner(null);
            linea("modelo nulo aceptado");
        } catch (NullPointerException e) {
            linea("modelo nulo rechazado: " + e.getMessage());
        }
        try {
            t.setModel(null);
            linea("poner nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("poner nulo rechazado: " + e.getMessage());
        }
        try {
            t.setEditor(null);
            linea("editor nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("editor nulo rechazado: " + e.getMessage());
        }
        // Un editor no le sirve a cualquier modelo.
        try {
            new JSpinner.NumberEditor(t);
            linea("editor numerico sobre lista aceptado");
        } catch (IllegalArgumentException e) {
            linea("editor numerico sobre lista rechazado: " + e.getMessage());
        }

        // Un modelo propio: el control no le pide nada mas que la interfaz.
        JSpinner p = new JSpinner(new Propio());
        linea("modelo propio editor=" + editor(p) + " " + paso(p.getModel()));
    }

    /** Cuenta de dos en dos, sin fin. */
    static class Propio extends AbstractSpinnerModel {

        private int n = 0;

        public Object getValue() {
            return Integer.valueOf(n);
        }

        public void setValue(Object v) {
            n = ((Integer) v).intValue();
            fireStateChanged();
        }

        public Object getNextValue() {
            return Integer.valueOf(n + 2);
        }

        public Object getPreviousValue() {
            return Integer.valueOf(n - 2);
        }
    }

    public static int run() {
        numeros();
        listas();
        fechas();
        control();
        return 0;
    }
}
