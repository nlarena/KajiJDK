import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.DefaultRowSorter;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SizeSequence;
import javax.swing.SortOrder;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;

/**
 * La secuencia de tamanos y los filtros de fila, contra el JDK.
 *
 * <p>Las dos son logica pura -- no tocan pantalla ni aspecto -- asi que la comparacion cubre todo lo
 * que hacen, bordes incluidos: indices fuera de rango, entradas de tamano cero, columnas que la
 * fila no tiene, y numeros de tipos distintos que valen lo mismo.
 */
public class Filas1 {

    static void linea(String s) {
        System.out.println("//" + s);
    }

    static void tamanos() {
        linea("--- la secuencia de tamanos ---");
        SizeSequence vacia = new SizeSequence();
        linea("vacia sizes=" + Arrays.toString(vacia.getSizes())
                + " pos(0)=" + vacia.getPosition(0) + " pos(5)=" + vacia.getPosition(5)
                + " idx(0)=" + vacia.getIndex(0) + " idx(9)=" + vacia.getIndex(9));

        SizeSequence n = new SizeSequence(4);
        linea("cuatro en cero sizes=" + Arrays.toString(n.getSizes())
                + " pos(2)=" + n.getPosition(2) + " idx(0)=" + n.getIndex(0));

        SizeSequence u = new SizeSequence(3, 10);
        linea("tres de 10 sizes=" + Arrays.toString(u.getSizes())
                + " total=" + u.getPosition(3));

        SizeSequence s = new SizeSequence(new int[] {5, 10, 0, 20, 1});
        linea("sizes=" + Arrays.toString(s.getSizes()));
        StringBuilder pos = new StringBuilder();
        for (int i = -1; i <= 6; i++) {
            pos.append(" ").append(i).append("=").append(s.getPosition(i));
        }
        linea("posiciones" + pos);
        StringBuilder idx = new StringBuilder();
        for (int p = -1; p <= 37; p = p + 1) {
            if (p == -1 || p == 0 || p == 4 || p == 5 || p == 14 || p == 15 || p == 34
                    || p == 35 || p == 36 || p == 37) {
                idx.append(" ").append(p).append("=").append(s.getIndex(p));
            }
        }
        linea("indices" + idx);
        StringBuilder tam = new StringBuilder();
        for (int i = -1; i <= 5; i++) {
            tam.append(" ").append(i).append("=").append(s.getSize(i));
        }
        linea("tamanos" + tam);

        s.setSize(2, 7);
        linea("entrada 2 a 7 sizes=" + Arrays.toString(s.getSizes())
                + " pos(3)=" + s.getPosition(3));
        s.setSize(9, 100);
        linea("fuera de rango sizes=" + Arrays.toString(s.getSizes()));
        // El indice negativo no entra: el JDK le suma el tamano a la entrada cero, por como esta
        // hecha su estructura interna. Ver la nota de SizeSequence.setSize.

        s.insertEntries(1, 2, 3);
        linea("dos de 3 en 1 sizes=" + Arrays.toString(s.getSizes()));
        s.insertEntries(0, 1, 99);
        linea("uno al principio sizes=" + Arrays.toString(s.getSizes()));
        s.insertEntries(s.getSizes().length, 1, 8);
        linea("uno al final sizes=" + Arrays.toString(s.getSizes()));
        s.removeEntries(0, 2);
        linea("saco dos del principio sizes=" + Arrays.toString(s.getSizes()));
        s.removeEntries(2, 3);
        linea("saco tres del medio sizes=" + Arrays.toString(s.getSizes()));

        // El arreglo que sale es una copia: tocarlo no cambia la secuencia.
        int[] copia = s.getSizes();
        if (copia.length > 0) {
            copia[0] = 12345;
        }
        linea("es copia=" + Arrays.toString(s.getSizes()));

        SizeSequence ceros = new SizeSequence(new int[] {0, 0, 4, 0, 0});
        linea("con ceros sizes=" + Arrays.toString(ceros.getSizes())
                + " pos(2)=" + ceros.getPosition(2) + " idx(0)=" + ceros.getIndex(0)
                + " idx(3)=" + ceros.getIndex(3) + " idx(4)=" + ceros.getIndex(4));
    }

    /** Una fila cualquiera, con sus valores y su identificador. */
    static class Fila extends RowFilter.Entry<String, Integer> {

        private final Object[] valores;
        private final int id;

        Fila(int id, Object[] valores) {
            this.id = id;
            this.valores = valores;
        }

        public String getModel() {
            return "modelo";
        }

        public int getValueCount() {
            return valores.length;
        }

        public Object getValue(int index) {
            return valores[index];
        }

        public Integer getIdentifier() {
            return Integer.valueOf(id);
        }
    }

    static void pasa(String que, RowFilter<String, Integer> f, Fila[] filas) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < filas.length; i++) {
            b.append(f.include(filas[i]) ? "1" : "0");
        }
        linea(que + " -> " + b);
    }

    static void filtros() {
        linea("--- los filtros de fila ---");
        Fila[] filas = new Fila[] {
            new Fila(0, new Object[] {"Argentina", Integer.valueOf(45), new Date(1000L)}),
            new Fila(1, new Object[] {"Brasil", Double.valueOf(45.0), new Date(2000L)}),
            new Fila(2, new Object[] {"armenia", Long.valueOf(7L), new Date(500L)}),
            new Fila(3, new Object[] {null, Integer.valueOf(-3), new Date(1000L)}),
            new Fila(4, new Object[] {"Chile"}),
        };
        linea("entrada 0 valores=" + filas[0].getValueCount() + " modelo=" + filas[0].getModel()
                + " id=" + filas[0].getIdentifier());
        linea("texto de un nulo=[" + filas[3].getStringValue(0) + "]");
        linea("texto de un numero=[" + filas[0].getStringValue(1) + "]");

        RowFilter<String, Integer> ar = RowFilter.<String, Integer>regexFilter("ar");
        pasa("regex 'ar' en todas las columnas", ar, filas);
        RowFilter<String, Integer> ar0 = RowFilter.<String, Integer>regexFilter("ar", 0);
        pasa("regex 'ar' solo en la 0", ar0, filas);
        RowFilter<String, Integer> may = RowFilter.<String, Integer>regexFilter("^A", 0);
        pasa("regex '^A' en la 0", may, filas);
        // Es find y no matches: alcanza con que aparezca.
        RowFilter<String, Integer> med = RowFilter.<String, Integer>regexFilter("rasi", 0);
        pasa("regex 'rasi' en la 0", med, filas);
        // Una columna que la fila no tiene se saltea, no revienta: la ultima fila tiene un solo
        // valor. La columna 2 guarda fechas y no se usa aca, porque el texto de una fecha depende
        // de la zona horaria y eso no es lo que se esta midiendo.
        RowFilter<String, Integer> lejos = RowFilter.<String, Integer>regexFilter("4", 1);
        pasa("regex 4 en la columna 1", lejos, filas);

        RowFilter<String, Integer> mayor = RowFilter.<String, Integer>numberFilter(
                RowFilter.ComparisonType.AFTER, Integer.valueOf(10), 1);
        pasa("numero > 10 en la 1", mayor, filas);
        RowFilter<String, Integer> menor = RowFilter.<String, Integer>numberFilter(
                RowFilter.ComparisonType.BEFORE, Integer.valueOf(10), 1);
        pasa("numero < 10 en la 1", menor, filas);
        // Un entero 45 y un doble 45.0 comparan iguales.
        RowFilter<String, Integer> igual = RowFilter.<String, Integer>numberFilter(
                RowFilter.ComparisonType.EQUAL, Integer.valueOf(45), 1);
        pasa("numero == 45 en la 1", igual, filas);
        RowFilter<String, Integer> distinto = RowFilter.<String, Integer>numberFilter(
                RowFilter.ComparisonType.NOT_EQUAL, Integer.valueOf(45), 1);
        pasa("numero != 45 en la 1", distinto, filas);
        RowFilter<String, Integer> todas = RowFilter.<String, Integer>numberFilter(
                RowFilter.ComparisonType.AFTER, Integer.valueOf(0));
        pasa("numero > 0 en todas", todas, filas);

        RowFilter<String, Integer> antes = RowFilter.<String, Integer>dateFilter(
                RowFilter.ComparisonType.BEFORE, new Date(1000L), 2);
        pasa("fecha < 1000 en la 2", antes, filas);
        RowFilter<String, Integer> desp = RowFilter.<String, Integer>dateFilter(
                RowFilter.ComparisonType.AFTER, new Date(1000L), 2);
        pasa("fecha > 1000 en la 2", desp, filas);
        RowFilter<String, Integer> justo = RowFilter.<String, Integer>dateFilter(
                RowFilter.ComparisonType.EQUAL, new Date(1000L), 2);
        pasa("fecha == 1000 en la 2", justo, filas);

        List<RowFilter<String, Integer>> dos = new ArrayList<RowFilter<String, Integer>>();
        dos.add(ar0);
        dos.add(mayor);
        pasa("o(ar en 0, >10 en 1)", RowFilter.<String, Integer>orFilter(dos), filas);
        pasa("y(ar en 0, >10 en 1)", RowFilter.<String, Integer>andFilter(dos), filas);
        pasa("no(ar en 0)", RowFilter.<String, Integer>notFilter(ar0), filas);
        pasa("no(no(ar en 0))", RowFilter.<String, Integer>notFilter(
                RowFilter.<String, Integer>notFilter(ar0)), filas);

        List<RowFilter<String, Integer>> ninguno = new ArrayList<RowFilter<String, Integer>>();
        pasa("o de nada", RowFilter.<String, Integer>orFilter(ninguno), filas);
        pasa("y de nada", RowFilter.<String, Integer>andFilter(ninguno), filas);

        linea("constantes " + Arrays.toString(RowFilter.ComparisonType.values())
                + " valueOf=" + RowFilter.ComparisonType.valueOf("EQUAL"));

        try {
            RowFilter.<String, Integer>regexFilter(null);
            linea("regex nula aceptada");
        } catch (NullPointerException e) {
            linea("regex nula rechazada por NullPointerException");
        }
        try {
            RowFilter.<String, Integer>regexFilter("a", -1);
            linea("columna negativa aceptada");
        } catch (IllegalArgumentException e) {
            linea("columna negativa rechazada: " + e.getMessage());
        }
        try {
            RowFilter.<String, Integer>numberFilter(null, Integer.valueOf(1));
            linea("tipo nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("tipo nulo rechazado: " + e.getMessage());
        }
        try {
            RowFilter.<String, Integer>numberFilter(RowFilter.ComparisonType.EQUAL, null);
            linea("numero nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("numero nulo rechazado: " + e.getMessage());
        }
        try {
            RowFilter.<String, Integer>dateFilter(RowFilter.ComparisonType.EQUAL, null);
            linea("fecha nula aceptada");
        } catch (NullPointerException e) {
            linea("fecha nula rechazada por NullPointerException");
        }
        try {
            RowFilter.<String, Integer>dateFilter(null, new Date(0L));
            linea("tipo nulo en fecha aceptado");
        } catch (IllegalArgumentException e) {
            linea("tipo nulo en fecha rechazado: " + e.getMessage());
        }
        try {
            RowFilter.<String, Integer>numberFilter(RowFilter.ComparisonType.EQUAL, null, -1);
            linea("nulo y columna negativa aceptados");
        } catch (IllegalArgumentException e) {
            linea("gana la columna negativa: " + e.getMessage());
        }
        try {
            RowFilter.<String, Integer>orFilter(null);
            linea("coleccion nula aceptada");
        } catch (NullPointerException e) {
            linea("coleccion nula rechazada por NullPointerException");
        }
        try {
            List<RowFilter<String, Integer>> conNulo =
                    new ArrayList<RowFilter<String, Integer>>();
            conNulo.add(null);
            RowFilter.<String, Integer>andFilter(conNulo);
            linea("filtro nulo adentro aceptado");
        } catch (IllegalArgumentException e) {
            linea("filtro nulo adentro rechazado: " + e.getMessage());
        }
        try {
            RowFilter.<String, Integer>notFilter(null);
            linea("negar nulo aceptado");
        } catch (IllegalArgumentException e) {
            linea("negar nulo rechazado: " + e.getMessage());
        }
    }

    /**
     * Una tabla de prueba: filas de objetos, columnas por posicion.
     *
     * <p>El envoltorio va adentro porque {@code ModelWrapper} es <em>protegida</em>: solo una
     * subclase del ordenador puede nombrarla. Los tres accesores tambien estan porque
     * {@code getModelWrapper} y {@code useToString} son protegidos.
     */
    static class Tabla extends DefaultRowSorter<Object[][], Integer> {

        private final Object[][] datos;

        Tabla(Object[][] datos) {
            this.datos = datos;
            setModelWrapper(new Envoltorio(datos));
        }

        Object valor(int fila, int columna) {
            return datos[fila][columna];
        }

        boolean porTexto(int columna) {
            return useToString(columna);
        }

        /** El envoltorio que le dice al ordenador de donde salen las filas. */
        class Envoltorio extends DefaultRowSorter.ModelWrapper<Object[][], Integer> {

            private final Object[][] filas;

            Envoltorio(Object[][] filas) {
                this.filas = filas;
            }

            public Object[][] getModel() {
                return filas;
            }

            public int getColumnCount() {
                return filas.length == 0 ? 0 : filas[0].length;
            }

            public int getRowCount() {
                return filas.length;
            }

            public Object getValueAt(int row, int column) {
                return filas[row][column];
            }

            public Integer getIdentifier(int row) {
                return Integer.valueOf(row);
            }
        }
    }

    /** Anota los avisos del ordenador. */
    static class EspiaOrden implements RowSorterListener {

        private final StringBuilder log = new StringBuilder();

        public void sorterChanged(RowSorterEvent e) {
            log.append(" ").append(e.getType());
        }

        String vaciar() {
            String s = log.toString();
            log.setLength(0);
            return s;
        }
    }

    /** Las filas en el orden en que se ven, por su primera columna. */
    static String vista(Tabla t) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < t.getViewRowCount(); i++) {
            int m = t.convertRowIndexToModel(i);
            b.append(" ").append(t.valor(m, 0));
        }
        return b.toString();
    }

    /** La traduccion de modelo a vista, con -1 en lo filtrado. */
    static String aVista(Tabla t) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < t.getModelRowCount(); i++) {
            b.append(" ").append(t.convertRowIndexToView(i));
        }
        return b.toString();
    }

    static String claves(RowSorter<?> t) {
        List<? extends RowSorter.SortKey> ks = t.getSortKeys();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < ks.size(); i++) {
            // Por una local y no directo: ver el hallazgo #517 del compilador.
            RowSorter.SortKey k = ks.get(i);
            b.append(" ").append(k.getColumn()).append(":").append(k.getSortOrder());
        }
        return b.toString().length() == 0 ? " -" : b.toString();
    }

    static void ordenar() {
        linea("--- el ordenador de filas ---");
        Object[][] datos = new Object[][] {
            {"pera", Integer.valueOf(3), "x"},
            {"Banana", Integer.valueOf(1), "y"},
            {"manzana", Integer.valueOf(3), "x"},
            {"uva", Integer.valueOf(2), "z"},
            {null, Integer.valueOf(1), "y"},
        };
        Tabla t = new Tabla(datos);
        linea("filas=" + t.getModelRowCount() + " vistas=" + t.getViewRowCount()
                + " claves=" + claves(t) + " maximo=" + t.getMaxSortKeys()
                + " reordena al cambiar=" + t.getSortsOnUpdates());
        linea("modelo es el arreglo=" + (t.getModel() == datos)
                + " filtro=" + t.getRowFilter());
        linea("sin orden vista=" + vista(t) + " |a vista=" + aVista(t));

        EspiaOrden espia = new EspiaOrden();
        t.addRowSorterListener(espia);

        t.toggleSortOrder(0);
        linea("clic en 0 claves=" + claves(t) + " vista=" + vista(t)
                + " |" + espia.vaciar());
        t.toggleSortOrder(0);
        linea("otro clic claves=" + claves(t) + " vista=" + vista(t) + " |" + espia.vaciar());
        t.toggleSortOrder(1);
        linea("clic en 1 claves=" + claves(t) + " vista=" + vista(t) + " |" + espia.vaciar());
        t.toggleSortOrder(2);
        linea("clic en 2 claves=" + claves(t) + " vista=" + vista(t) + " |" + espia.vaciar());
        t.toggleSortOrder(0);
        linea("clic en 0 de nuevo claves=" + claves(t) + " vista=" + vista(t));

        t.setMaxSortKeys(2);
        linea("maximo 2 claves=" + claves(t));

        // Una columna que no se puede ordenar ignora el clic.
        t.setSortKeys(null);
        t.setSortable(1, false);
        linea("columna 1 ordenable=" + t.isSortable(1) + " la 0=" + t.isSortable(0));
        espia.vaciar();
        t.toggleSortOrder(1);
        linea("clic en la no ordenable claves=" + claves(t) + " |" + espia.vaciar());
        t.setSortable(1, true);

        // Un comparador propio manda sobre la comparacion por texto.
        List<RowSorter.SortKey> unaClave = new ArrayList<RowSorter.SortKey>();
        unaClave.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        t.setComparator(0, new PorLargo());
        t.setSortKeys(unaClave);
        linea("por largo vista=" + vista(t) + " usa texto=" + t.porTexto(0)
                + " la 1 usa texto=" + t.porTexto(1));
        t.setComparator(0, null);
        t.setSortKeys(null);
        t.setSortKeys(unaClave);
        linea("sin comparador vista=" + vista(t) + " usa texto=" + t.porTexto(0));

        // Los nulos van primero y no llegan al comparador.
        List<RowSorter.SortKey> desc = new ArrayList<RowSorter.SortKey>();
        desc.add(new RowSorter.SortKey(0, SortOrder.DESCENDING));
        t.setSortKeys(desc);
        linea("descendente vista=" + vista(t));

        // Dos claves: la segunda desempata; el ultimo desempate es el indice de modelo.
        List<RowSorter.SortKey> dos = new ArrayList<RowSorter.SortKey>();
        dos.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        dos.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        t.setSortKeys(dos);
        linea("por 1 y despues 0 vista=" + vista(t));
        List<RowSorter.SortKey> soloUna = new ArrayList<RowSorter.SortKey>();
        soloUna.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        t.setSortKeys(soloUna);
        linea("solo por 1, estable vista=" + vista(t));

        // El filtro cambia las dos numeraciones.
        espia.vaciar();
        t.setRowFilter(RowFilter.<Object[][], Integer>regexFilter("a", 0));
        linea("con filtro vistas=" + t.getViewRowCount() + " vista=" + vista(t)
                + " |a vista=" + aVista(t) + " |" + espia.vaciar());
        t.setSortKeys(null);
        linea("sin orden pero con filtro vista=" + vista(t) + " |a vista=" + aVista(t));
        t.setRowFilter(null);
        linea("sin nada vistas=" + t.getViewRowCount() + " |a vista=" + aVista(t));

        try {
            t.convertRowIndexToModel(99);
            linea("indice 99 aceptado");
        } catch (IndexOutOfBoundsException e) {
            linea("indice 99 rechazado");
        }
        try {
            t.convertRowIndexToView(-1);
            linea("indice -1 aceptado");
        } catch (IndexOutOfBoundsException e) {
            linea("indice -1 rechazado");
        }
        try {
            t.toggleSortOrder(9);
            linea("columna 9 aceptada");
        } catch (IndexOutOfBoundsException e) {
            linea("columna 9 rechazada");
        }
        try {
            t.setMaxSortKeys(0);
            linea("maximo 0 aceptado");
        } catch (IllegalArgumentException e) {
            linea("maximo 0 rechazado: " + e.getMessage());
        }
        try {
            List<RowSorter.SortKey> mala = new ArrayList<RowSorter.SortKey>();
            mala.add(new RowSorter.SortKey(9, SortOrder.ASCENDING));
            t.setSortKeys(mala);
            linea("clave con columna 9 aceptada");
        } catch (IllegalArgumentException e) {
            linea("clave con columna 9 rechazada: " + e.getMessage());
        }

        // Cambiar la estructura del modelo borra el orden.
        t.setSortKeys(soloUna);
        t.modelStructureChanged();
        linea("tras cambiar la estructura claves=" + claves(t) + " vista=" + vista(t));
    }

    /** Ordena por el largo del texto, para ver que un comparador propio manda. */
    static class PorLargo implements java.util.Comparator<Object> {

        public int compare(Object a, Object b) {
            return String.valueOf(a).length() - String.valueOf(b).length();
        }
    }

    public static int run() {
        tamanos();
        filtros();
        ordenar();
        return 0;
    }
}
