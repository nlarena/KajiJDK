import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.CardLayout;
import java.awt.Checkbox;
import java.awt.CheckboxGroup;
import java.awt.CheckboxMenuItem;
import java.awt.Choice;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.List;
import java.awt.Panel;
import java.awt.Point;
import java.awt.ScrollPane;
import java.awt.Scrollbar;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleValue;

/**
 * Los widgets de AWT y las distribuciones.
 *
 * <p>El mismo archivo da -1 con el JDK 25 corriendo **sus** clases, sin pantalla.
 *
 * <p>No mira nada dibujado, porque no hay con qué. Mira lo que es estado y reglas: que un grupo de
 * casillas deje exactamente una marcada, que una lista desplegable no pueda quedar sin selección,
 * que el valor de una barra nunca llegue al máximo, que la selección de una lista se corra cuando se
 * mete algo en el medio, y que cada distribución ubique a los hijos donde dice.
 */
public class AwtWidgetTest {

    static int failures = 0;

    static void ok(String que, boolean bien) {
        if (!bien) {
            failures = failures + 1;
            System.out.println("FALLA: " + que);
        }
    }

    /** Un componente de tamaño fijo, para que las distribuciones tengan qué medir. */
    static class Cuadro extends Canvas {
        Cuadro(int w, int h) {
            this.setSize(w, h);
        }

        public Dimension getPreferredSize() {
            return this.getSize();
        }

        public Dimension getMinimumSize() {
            return this.getSize();
        }
    }

    public static int run() throws Exception {
        etiquetaYBoton();
        casillas();
        entradaDeMenu();
        desplegable();
        lista();
        barra();
        texto();
        panelDeDesplazamiento();
        distribuciones();
        gridBag();
        accesibilidad();
        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    static void etiquetaYBoton() {
        Label l = new Label("hola", Label.RIGHT);
        ok("la etiqueta guarda el texto", "hola".equals(l.getText()));
        ok("y la alineación", l.getAlignment() == Label.RIGHT);
        boolean tiro = false;
        try {
            l.setAlignment(77);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok("una alineación inventada tira", tiro);
        ok("y no la cambió", l.getAlignment() == Label.RIGHT);

        Button b = new Button("Aceptar");
        ok("sin comando propio, el comando es la leyenda",
                "Aceptar".equals(b.getActionCommand()));
        b.setActionCommand("ok");
        ok("con comando propio, es el comando", "ok".equals(b.getActionCommand()));
        b.setLabel("OK");
        ok("cambiar la leyenda no toca el comando", "ok".equals(b.getActionCommand()));
        b.setActionCommand(null);
        ok("y volver a null lo devuelve a la leyenda", "OK".equals(b.getActionCommand()));

        final String[] visto = new String[1];
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                visto[0] = e.getActionCommand();
            }
        };
        b.addActionListener(al);
        ok("el oyente quedó puesto", b.getActionListeners().length == 1);
        b.dispatchEvent(new ActionEvent(b, ActionEvent.ACTION_PERFORMED, b.getActionCommand()));
        ok("y recibió el comando", "OK".equals(visto[0]));
        b.removeActionListener(al);
        ok("sacarlo lo saca", b.getActionListeners().length == 0);
        ok("getListeners ve lo mismo", b.getListeners(ActionListener.class).length == 0);
    }

    static void casillas() {
        CheckboxGroup g = new CheckboxGroup();
        Checkbox a = new Checkbox("a", true, g);
        Checkbox b = new Checkbox("b", false, g);
        ok("la primera marcada es la del grupo", g.getSelectedCheckbox() == a);
        b.setState(true);
        ok("marcar la otra la selecciona", g.getSelectedCheckbox() == b);
        ok("y desmarca la primera", !a.getState());
        ok("la segunda quedó marcada", b.getState());

        b.setState(false);
        ok("desmarcar la marcada de un grupo no hace nada", g.getSelectedCheckbox() == b);
        ok("y la casilla sigue marcada", b.getState());
        g.setSelectedCheckbox(null);
        ok("vaciar el grupo sí se puede, pero desde el grupo", g.getSelectedCheckbox() == null);
        ok("y ahí sí queda sin marcar", !b.getState());

        Checkbox suelta = new Checkbox("suelta");
        ok("una casilla suelta no tiene grupo", suelta.getCheckboxGroup() == null);
        ok("y sin marcar no selecciona nada", suelta.getSelectedObjects() == null);
        suelta.setState(true);
        Object[] sel = suelta.getSelectedObjects();
        ok("marcada devuelve su leyenda", sel != null && sel.length == 1
                && "suelta".equals(sel[0]));

        // Una casilla de otro grupo no puede mandar en este.
        CheckboxGroup otro = new CheckboxGroup();
        Checkbox ajena = new Checkbox("ajena", false, otro);
        g.setSelectedCheckbox(a);
        g.setSelectedCheckbox(ajena);
        ok("una casilla de otro grupo se ignora", g.getSelectedCheckbox() == a);
        ok("y no se marca", !ajena.getState());

        final int[] cuenta = new int[1];
        a.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                cuenta[0] = cuenta[0] + 1;
            }
        });
        a.setState(true);
        ok("setState no dispara eventos", cuenta[0] == 0);
        ok("pero sí cambia el estado", a.getState());
        a.dispatchEvent(new ItemEvent(a, ItemEvent.ITEM_STATE_CHANGED, "a", ItemEvent.SELECTED));
        ok("un evento repartido sí llega", cuenta[0] == 1);
    }

    static void entradaDeMenu() {
        CheckboxMenuItem m = new CheckboxMenuItem("ver", true);
        ok("la entrada guarda el estado", m.getState());
        ok("y la leyenda", "ver".equals(m.getLabel()));
        Object[] sel = m.getSelectedObjects();
        ok("tildada devuelve su leyenda", sel != null && sel.length == 1 && "ver".equals(sel[0]));
        m.setState(false);
        ok("destildada no devuelve nada", m.getSelectedObjects() == null);
    }

    static void desplegable() {
        Choice c = new Choice();
        ok("nace vacía y sin selección", c.getItemCount() == 0 && c.getSelectedIndex() == -1);
        ok("y sin item seleccionado", c.getSelectedItem() == null);
        c.add("uno");
        ok("la primera se selecciona sola", c.getSelectedIndex() == 0);
        c.add("dos");
        c.add("tres");
        ok("tiene tres", c.getItemCount() == 3);
        c.select("tres");
        ok("seleccionar por nombre anda", c.getSelectedIndex() == 2);
        c.select("no existe");
        ok("un nombre que no está no cambia nada", c.getSelectedIndex() == 2);

        boolean tiro = false;
        try {
            c.select(9);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok("una posición que no existe tira", tiro);

        c.remove(2);
        ok("sacar la seleccionada pasa a la primera", c.getSelectedIndex() == 0);
        c.removeAll();
        ok("vaciarla la deja sin selección", c.getSelectedIndex() == -1);
        ok("y sin nada", c.getItemCount() == 0);

        boolean tiroNull = false;
        try {
            c.add((String) null);
        } catch (NullPointerException e) {
            tiroNull = true;
        }
        ok("agregar null tira", tiroNull);
    }

    static void lista() {
        List l = new List(3, true);
        ok("guarda cuántos renglones muestra", l.getRows() == 3);
        ok("y que es de selección múltiple", l.isMultipleMode());
        l.add("a");
        l.add("b");
        l.add("c");
        ok("nace sin nada seleccionado", l.getSelectedIndexes().length == 0);
        ok("y getSelectedIndex da -1", l.getSelectedIndex() == -1);

        l.select(0);
        l.select(2);
        int[] sel = l.getSelectedIndexes();
        ok("selecciona dos", sel.length == 2 && sel[0] == 0 && sel[1] == 2);
        ok("con dos, getSelectedIndex da -1", l.getSelectedIndex() == -1);
        String[] items = l.getSelectedItems();
        ok("y devuelve los dos textos", items.length == 2 && "a".equals(items[0])
                && "c".equals(items[1]));

        l.deselect(0);
        ok("deseleccionar deja una", l.getSelectedIndexes().length == 1);
        ok("y ahora sí getSelectedIndex la da", l.getSelectedIndex() == 2);
        l.deselect(2);
        ok("deseleccionar la última la deja vacía", l.getSelectedIndexes().length == 0);
        l.deselect(2);
        ok("deseleccionar lo que no está no hace nada", l.getSelectedIndexes().length == 0);

        ok("devuelve todos los renglones", l.getItems().length == 3);
        l.replaceItem("B", 1);
        ok("reemplazar cambia el texto", "B".equals(l.getItem(1)));

        l.makeVisible(2);
        ok("makeVisible queda anotado", l.getVisibleIndex() == 2);

        List simple = new List();
        ok("una lista por omisión muestra cuatro", simple.getRows() == 4);
        ok("y es de selección simple", !simple.isMultipleMode());
        simple.add("x");
        simple.add("y");
        simple.select(0);
        simple.select(1);
        ok("en simple, seleccionar reemplaza", simple.getSelectedIndex() == 1);
    }

    static void barra() {
        Scrollbar s = new Scrollbar(Scrollbar.HORIZONTAL, 0, 10, 0, 100);
        ok("guarda la orientación", s.getOrientation() == Scrollbar.HORIZONTAL);
        s.setValue(1000);
        ok("el valor no llega al máximo, llega a max-visible", s.getValue() == 90);
        s.setValue(-5);
        ok("ni baja del mínimo", s.getValue() == 0);

        s.setVisibleAmount(50);
        ok("achicar el rango útil recorta el valor", s.getValue() == 0);
        s.setValue(80);
        ok("y el nuevo tope es 50", s.getValue() == 50);

        s.setUnitIncrement(-3);
        ok("un incremento no positivo se vuelve 1", s.getUnitIncrement() == 1);
        s.setBlockIncrement(0);
        ok("lo mismo el de bloque", s.getBlockIncrement() == 1);

        Scrollbar d = new Scrollbar();
        ok("una barra por omisión es vertical", d.getOrientation() == Scrollbar.VERTICAL);
        boolean tiro = false;
        try {
            d.setOrientation(9);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok("una orientación inventada tira", tiro);

        ok("no está siendo ajustada", !d.getValueIsAdjusting());
        d.setValueIsAdjusting(true);
        ok("hasta que se lo diga", d.getValueIsAdjusting());
    }

    static void texto() {
        TextField f = new TextField("hola mundo", 20);
        ok("guarda el texto", "hola mundo".equals(f.getText()));
        ok("y el ancho", f.getColumns() == 20);
        ok("nace editable", f.isEditable());
        ok("y sin eco", !f.echoCharIsSet());
        f.setEchoChar('*');
        ok("el eco queda puesto", f.getEchoChar() == '*');
        ok("y el texto sigue en claro", "hola mundo".equals(f.getText()));

        f.select(5, 100);
        ok("una selección pasada del final se recorta", f.getSelectionEnd() == 10);
        ok("y el principio queda donde se pidió", f.getSelectionStart() == 5);
        ok("el texto seleccionado es el tramo", "mundo".equals(f.getSelectedText()));
        f.select(8, 3);
        ok("una selección dada vuelta se ordena", f.getSelectionStart() == 8
                && f.getSelectionEnd() == 8);
        f.selectAll();
        ok("seleccionar todo va de punta a punta", f.getSelectionStart() == 0
                && f.getSelectionEnd() == 10);
        ok("el cursor es el principio de la selección", f.getCaretPosition() == 0);
        f.setCaretPosition(4);
        ok("ponerlo deja la selección vacía ahí", f.getSelectionStart() == 4
                && f.getSelectionEnd() == 4);
        ok("y el cursor queda ahí", f.getCaretPosition() == 4);

        boolean tiro = false;
        try {
            f.setCaretPosition(-1);
        } catch (IllegalArgumentException e) {
            tiro = true;
        }
        ok("un cursor negativo tira", tiro);


        TextArea a = new TextArea("uno\ndos", 5, 40);
        ok("el área guarda las medidas", a.getRows() == 5 && a.getColumns() == 40);
        ok("y las barras pedidas", a.getScrollbarVisibility() == TextArea.SCROLLBARS_BOTH);
        a.append("\ntres");
        ok("append pega al final", "uno\ndos\ntres".equals(a.getText()));
        a.insert(">", 0);
        ok("insert mete donde se dice", ">uno\ndos\ntres".equals(a.getText()));
        a.replaceRange("X", 0, 4);
        ok("replaceRange reemplaza el tramo", "X\ndos\ntres".equals(a.getText()));

        TextArea sinBarras = new TextArea("", 2, 2, TextArea.SCROLLBARS_NONE);
        ok("las barras pedidas se guardan",
                sinBarras.getScrollbarVisibility() == TextArea.SCROLLBARS_NONE);
    }

    static void panelDeDesplazamiento() {
        ScrollPane sp = new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS);
        ok("guarda la política", sp.getScrollbarDisplayPolicy() == ScrollPane.SCROLLBARS_ALWAYS);
        boolean tiro = false;
        try {
            sp.setLayout(new FlowLayout());
        } catch (Error e) {
            tiro = true;
        }
        ok("no deja cambiarle la distribución", tiro);

        boolean tiroSinHijo = false;
        try {
            sp.getScrollPosition();
        } catch (NullPointerException e) {
            tiroSinHijo = true;
        }
        ok("sin hijo, preguntar la posición tira", tiroSinHijo);

        sp.setSize(100, 100);
        Cuadro grande = new Cuadro(300, 300);
        sp.add(grande);
        ok("tiene un hijo", sp.getComponentCount() == 1);
        sp.add(new Cuadro(10, 10));
        ok("agregar otro saca al primero", sp.getComponentCount() == 1);

        sp.removeAll();
        sp.add(grande);
        sp.doLayout();
        sp.setScrollPosition(50, 60);
        Point p = sp.getScrollPosition();
        ok("desplaza a donde se pidió", p.x == 50 && p.y == 60);
        sp.setScrollPosition(9999, 9999);
        p = sp.getScrollPosition();
        ok("no se pasa del contenido", p.x == 200 && p.y == 200);

        boolean tiroBarra = false;
        try {
            sp.getVAdjustable().setMaximum(10);
        } catch (Error e) {
            tiroBarra = true;
        }
        ok("la barra no deja fijarle el rango", tiroBarra);
        ok("la vertical es vertical",
                sp.getVAdjustable().getOrientation() == java.awt.Adjustable.VERTICAL);
        ok("y la horizontal, horizontal",
                sp.getHAdjustable().getOrientation() == java.awt.Adjustable.HORIZONTAL);
        ok("la rueda desplaza por omisión", sp.isWheelScrollingEnabled());
    }

    static void distribuciones() {
        // --- FlowLayout: en fila, centrado, con separación.
        Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        Cuadro a = new Cuadro(20, 10);
        Cuadro b = new Cuadro(30, 10);
        p.add(a);
        p.add(b);
        p.setSize(200, 50);
        p.doLayout();
        ok("flow arranca después de la separación", a.getX() == 5);
        ok("y el segundo va pegado al primero con separación", b.getX() == 30);
        ok("los dos en el mismo renglón", a.getY() == b.getY());
        Dimension d = p.getPreferredSize();
        ok("la medida preferida suma los hijos y las separaciones",
                d.width == 5 + 20 + 5 + 30 + 5);

        // --- GridLayout: celdas iguales.
        Panel g = new Panel(new GridLayout(2, 2, 0, 0));
        Cuadro[] c = new Cuadro[4];
        for (int i = 0; i < 4; i++) {
            c[i] = new Cuadro(10, 10);
            g.add(c[i]);
        }
        g.setSize(100, 100);
        g.doLayout();
        ok("la grilla parte en celdas iguales", c[0].getWidth() == 50
                && c[0].getHeight() == 50);
        ok("y las ubica en dos filas y dos columnas",
                c[1].getX() == 50 && c[2].getY() == 50 && c[3].getX() == 50
                        && c[3].getY() == 50);

        // --- BorderLayout: el centro se come lo que sobra.
        Panel bl = new Panel(new BorderLayout());
        Cuadro norte = new Cuadro(10, 20);
        Cuadro sur = new Cuadro(10, 30);
        Cuadro oeste = new Cuadro(40, 10);
        Cuadro centro = new Cuadro(1, 1);
        bl.add(norte, BorderLayout.NORTH);
        bl.add(sur, BorderLayout.SOUTH);
        bl.add(oeste, BorderLayout.WEST);
        bl.add(centro, BorderLayout.CENTER);
        bl.setSize(100, 100);
        bl.doLayout();
        ok("el norte ocupa todo el ancho y su alto",
                norte.getWidth() == 100 && norte.getHeight() == 20 && norte.getY() == 0);
        ok("el sur queda abajo", sur.getY() == 70 && sur.getHeight() == 30);
        ok("el oeste va entre los dos", oeste.getX() == 0 && oeste.getY() == 20
                && oeste.getWidth() == 40 && oeste.getHeight() == 50);
        ok("el centro se come lo que sobra", centro.getX() == 40 && centro.getY() == 20
                && centro.getWidth() == 60 && centro.getHeight() == 50);

        // --- CardLayout: una sola visible.
        CardLayout cl = new CardLayout();
        Panel cp = new Panel(cl);
        Cuadro c1 = new Cuadro(10, 10);
        Cuadro c2 = new Cuadro(50, 40);
        cp.add(c1, "uno");
        cp.add(c2, "dos");
        ok("la primera carta se ve", c1.isVisible());
        ok("y la segunda no", !c2.isVisible());
        cl.show(cp, "dos");
        ok("mostrar por nombre da vuelta las dos", !c1.isVisible() && c2.isVisible());
        cl.next(cp);
        ok("después de la última vuelve a la primera", c1.isVisible());
        cl.previous(cp);
        ok("y para atrás también da la vuelta", c2.isVisible());
        Dimension dc = cp.getPreferredSize();
        ok("la medida es la de la carta más grande", dc.width == 50 && dc.height == 40);
    }

    static void gridBag() {
        GridBagLayout gb = new GridBagLayout();
        Panel p = new Panel(gb);
        GridBagConstraints c = new GridBagConstraints();

        Cuadro izq = new Cuadro(20, 10);
        Cuadro der = new Cuadro(30, 10);
        c.gridx = 0;
        c.gridy = 0;
        p.add(izq, c);
        c.gridx = 1;
        p.add(der, c);

        Dimension d = p.getPreferredSize();
        ok("gridbag suma las columnas", d.width == 50);
        ok("y toma el alto de la fila", d.height == 10);

        // Las restricciones se copian: cambiar el objeto no cambia lo ya agregado.
        c.gridx = 9;
        GridBagConstraints guardadas = gb.getConstraints(der);
        ok("las restricciones se guardaron copiadas", guardadas.gridx == 1);

        p.setSize(150, 10);
        p.doLayout();
        ok("sin pesos, la grilla no se estira", izq.getWidth() == 20 && der.getWidth() == 30);

        int[][] dims = gb.getLayoutDimensions();
        ok("informa dos columnas", dims[0].length == 2);
        ok("y una fila", dims[1].length == 1);

        // Con peso, la columna se come lo que sobra; con fill, el hijo la llena.
        GridBagLayout gb2 = new GridBagLayout();
        Panel p2 = new Panel(gb2);
        GridBagConstraints c2 = new GridBagConstraints();
        Cuadro fijo = new Cuadro(20, 10);
        Cuadro elastico = new Cuadro(30, 10);
        c2.gridx = 0;
        c2.weightx = 0;
        p2.add(fijo, c2);
        c2.gridx = 1;
        c2.weightx = 1;
        c2.fill = GridBagConstraints.HORIZONTAL;
        p2.add(elastico, c2);
        p2.setSize(150, 10);
        p2.doLayout();
        ok("la columna sin peso no crece", fijo.getWidth() == 20);
        ok("la de peso se lleva todo el sobrante", elastico.getWidth() == 130);
        ok("y arranca donde termina la otra", elastico.getX() == 20);

        double[][] pesos = gb2.getLayoutWeights();
        ok("informa el peso de cada columna",
                pesos[0].length == 2 && pesos[0][0] == 0.0 && pesos[0][1] == 1.0);

        Point origen = gb2.getLayoutOrigin();
        ok("el origen es el margen del panel", origen.x == 0 && origen.y == 0);

        // Con anchor y sin fill, el hijo queda de su tamaño adentro de la celda.
        GridBagLayout gb3 = new GridBagLayout();
        Panel p3 = new Panel(gb3);
        GridBagConstraints c3 = new GridBagConstraints();
        Cuadro anclado = new Cuadro(20, 10);
        c3.weightx = 1;
        c3.anchor = GridBagConstraints.WEST;
        p3.add(anclado, c3);
        p3.setSize(100, 10);
        p3.doLayout();
        ok("con peso pero sin fill el hijo no crece", anclado.getWidth() == 20);
        ok("y anclado al oeste queda pegado a la izquierda", anclado.getX() == 0);

        // Los insets salen del espacio de la celda, no del componente.
        GridBagLayout gb4 = new GridBagLayout();
        Panel p4 = new Panel(gb4);
        GridBagConstraints c4 = new GridBagConstraints();
        Cuadro conMargen = new Cuadro(20, 10);
        c4.insets = new Insets(2, 3, 4, 5);
        p4.add(conMargen, c4);
        Dimension d4 = p4.getPreferredSize();
        ok("el margen entra en la medida preferida",
                d4.width == 20 + 3 + 5 && d4.height == 10 + 2 + 4);
    }

    static void accesibilidad() {
        Button b = new Button("Aceptar");
        AccessibleContext ac = b.getAccessibleContext();
        ok("el botón tiene contexto accesible", ac != null);
        ok("es un botón", ac.getAccessibleRole() == AccessibleRole.PUSH_BUTTON);
        ok("y su nombre es la leyenda", "Aceptar".equals(ac.getAccessibleName()));
        ok("el contexto es siempre el mismo", ac == b.getAccessibleContext());
        ok("ofrece una acción", ac.getAccessibleAction().getAccessibleActionCount() == 1);

        Checkbox c = new Checkbox("x");
        ok("una casilla suelta es una casilla",
                c.getAccessibleContext().getAccessibleRole() == AccessibleRole.CHECK_BOX);
        Checkbox r = new Checkbox("y", false, new CheckboxGroup());
        ok("una en grupo también es una casilla, no un botón de radio",
                r.getAccessibleContext().getAccessibleRole() == AccessibleRole.CHECK_BOX);
        ok("sin marcar no informa CHECKED", !c.getAccessibleContext()
                .getAccessibleStateSet().contains(AccessibleState.CHECKED));
        c.setState(true);
        ok("marcada sí", c.getAccessibleContext().getAccessibleStateSet()
                .contains(AccessibleState.CHECKED));

        Scrollbar s = new Scrollbar(Scrollbar.VERTICAL, 0, 10, 0, 100);
        AccessibleValue v = s.getAccessibleContext().getAccessibleValue();
        ok("el máximo accesible es el nominal, aunque no se alcance",
                v.getMaximumAccessibleValue().intValue() == 100);
        ok("y el mínimo es el mínimo", v.getMinimumAccessibleValue().intValue() == 0);
        ok("la barra es una barra", s.getAccessibleContext().getAccessibleRole()
                == AccessibleRole.SCROLL_BAR);
        ok("y se informa vertical", s.getAccessibleContext().getAccessibleStateSet()
                .contains(AccessibleState.VERTICAL));

        Panel p = new Panel();
        ok("un panel es un panel",
                p.getAccessibleContext().getAccessibleRole() == AccessibleRole.PANEL);
        Component hijo = new Cuadro(1, 1);
        p.add(hijo);
        ok("y sus hijos accesibles son los del árbol",
                p.getAccessibleContext().getAccessibleChildrenCount() == 1);

        TextField t = new TextField("hola mundo");
        ok("un campo de texto es texto",
                t.getAccessibleContext().getAccessibleRole() == AccessibleRole.TEXT);
        ok("informa que se puede escribir", t.getAccessibleContext()
                .getAccessibleStateSet().contains(AccessibleState.EDITABLE));
        ok("y cuántas letras tiene",
                t.getAccessibleContext().getAccessibleText().getCharCount() == 10);

        TextArea ta = new TextArea("uno\ndos");
        ok("un área es de varios renglones", ta.getAccessibleContext()
                .getAccessibleStateSet().contains(AccessibleState.MULTI_LINE));

        List l = new List(2, true);
        l.add("a");
        l.add("b");
        ok("una lista múltiple lo informa", l.getAccessibleContext()
                .getAccessibleStateSet().contains(AccessibleState.MULTISELECTABLE));
        l.getAccessibleContext().getAccessibleSelection().addAccessibleSelection(1);
        ok("y se la puede operar desde la accesibilidad", l.isIndexSelected(1));

        Choice ch = new Choice();
        ch.add("uno");
        ok("una desplegable es un combo",
                ch.getAccessibleContext().getAccessibleRole() == AccessibleRole.COMBO_BOX);
        ok("y no ofrece AccessibleSelection, igual que en el JDK",
                ch.getAccessibleContext().getAccessibleSelection() == null);
        ok("y declara AccessibleAction sin ofrecer ninguna", ch.getAccessibleContext()
                .getAccessibleAction().getAccessibleActionCount() == 0);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("AwtWidgetTest " + AwtWidgetTest.run());
    }
}
