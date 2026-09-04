import javax.print.attribute.Attribute;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.AttributeSetUtilities;
import javax.print.attribute.DocAttribute;
import javax.print.attribute.DocAttributeSet;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.HashAttributeSet;
import javax.print.attribute.HashDocAttributeSet;
import javax.print.attribute.HashPrintJobAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.IntegerSyntax;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.PrintServiceAttribute;
import javax.print.attribute.UnmodifiableSetException;

// Comportamiento de los AttributeSet de javax.print.attribute, para correr con las dos VMs y
// comparar.
//
// Este archivo no usa nada de javax.print.attribute.standard: define sus propios atributos, que es
// justo lo que hace falta para probar el sistema de tipos sin depender del paquete de arriba. Los
// tres que define cubren los tres casos que importan:
//
//   - `Copias`, un atributo de valor entero que pertenece a tres alcances a la vez;
//   - `Papel` con dos subclases, que es la indireccion de categoria: dos clases distintas que
//     contestan la misma pregunta y por lo tanto se pisan en un conjunto (el caso `Media` real);
//   - `Marca`, de servicio, que existe para poder probar que un conjunto de pedido lo rechaza.
//
// Compila igual contra el javax.print.attribute del JDK real que contra el nuestro, asi que `run()`
// devolviendo -1 en los dos lados quiere decir que las dos implementaciones coinciden.
public class PrnAttrSetTest {

    // Un atributo de valor entero, de los tres alcances de trabajo.
    static class Copias extends IntegerSyntax
            implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {
        Copias(int v) {
            super(v);
        }

        public final Class<? extends Attribute> getCategory() {
            return Copias.class;
        }

        public final String getName() {
            return "copies";
        }
    }

    // La indireccion de categoria: `Papel` es la pregunta y las dos subclases son dos maneras de
    // contestarla. Las dos reportan `Papel.class`, asi que un conjunto no puede tener las dos.
    abstract static class Papel extends EnumSyntax
            implements DocAttribute, PrintRequestAttribute, PrintJobAttribute {
        Papel(int v) {
            super(v);
        }

        public final Class<? extends Attribute> getCategory() {
            return Papel.class;
        }

        public final String getName() {
            return "media";
        }
    }

    static class PapelPorTamano extends Papel {
        PapelPorTamano(int v) {
            super(v);
        }
    }

    static class PapelPorBandeja extends Papel {
        PapelPorBandeja(int v) {
            super(v);
        }
    }

    // De servicio: describe la impresora, no el trabajo. No hay atributo que sea de pedido y de
    // servicio a la vez, y por eso sirve de intruso en las dos direcciones.
    static class Marca extends IntegerSyntax implements PrintServiceAttribute {
        Marca(int v) {
            super(v);
        }

        public final Class<? extends Attribute> getCategory() {
            return Marca.class;
        }

        public final String getName() {
            return "printer-make-and-model";
        }
    }

    public static int run() {
        int n;

        // ---- lo basico ----

        n = 1;
        HashAttributeSet s = new HashAttributeSet();
        if (s.size() != 0 || !s.isEmpty() || s.toArray().length != 0) {
            return n;
        }
        n = 2;
        if (!s.add(new Copias(3)) || s.size() != 1 || s.isEmpty()) {
            return n;
        }
        n = 3;
        if (!new Copias(3).equals(s.get(Copias.class))) {
            return n;
        }

        // ---- add devuelve "cambio", no "habia algo" ----

        n = 4;
        // Mismo valor, objeto distinto: el conjunto no cambia.
        if (s.add(new Copias(3)) || s.size() != 1) {
            return n;
        }
        n = 5;
        // Valor distinto, misma categoria: reemplaza y devuelve true.
        if (!s.add(new Copias(5)) || s.size() != 1) {
            return n;
        }
        n = 6;
        if (!new Copias(5).equals(s.get(Copias.class))) {
            return n;
        }

        // ---- la clave es la CATEGORIA, no la clase del atributo ----

        n = 7;
        AttributeSet p = new HashAttributeSet();
        Papel porTamano = new PapelPorTamano(1);
        Papel porBandeja = new PapelPorBandeja(2);
        p.add(porTamano);
        p.add(porBandeja);
        // Dos clases distintas, una sola categoria: el segundo piso al primero.
        if (p.size() != 1) {
            return n;
        }
        n = 8;
        // Se compara por identidad y no por equals a proposito: `Papel` es un EnumSyntax, y
        // EnumSyntax NO redefine equals justamente porque sus valores son singletons.
        if (p.get(Papel.class) != porBandeja) {
            return n;
        }
        n = 9;
        // Y NO se archivan bajo su propia clase.
        if (p.containsKey(PapelPorTamano.class) || p.containsKey(PapelPorBandeja.class)) {
            return n;
        }
        n = 10;
        if (!p.containsKey(Papel.class)) {
            return n;
        }

        // ---- containsKey / containsValue: dos ejes distintos ----

        n = 11;
        if (!s.containsKey(Copias.class) || s.containsKey(Marca.class)) {
            return n;
        }
        n = 12;
        if (!s.containsValue(new Copias(5)) || s.containsValue(new Copias(3))) {
            return n;
        }
        n = 13;
        // Categoria que no es un Attribute: ClassCastException, no false.
        try {
            s.containsKey(String.class);
            return n;
        } catch (ClassCastException e) {
            // esperado
        }
        n = 14;
        // Una categoria null NO es error en containsKey: da false.
        if (s.containsKey(null)) {
            return n;
        }
        n = 15;
        if (s.containsValue(null)) {
            return n;
        }

        // ---- get ----

        n = 16;
        if (s.get(Marca.class) != null) {
            return n;
        }
        n = 17;
        // get SI es estricto con la categoria: no es un Attribute, revienta.
        try {
            s.get(String.class);
            return n;
        } catch (ClassCastException e) {
            // esperado
        }
        n = 18;
        // Y con null tira NullPointerException, no devuelve null.
        try {
            s.get(null);
            return n;
        } catch (NullPointerException e) {
            // esperado
        }

        // ---- remove: por categoria, sin mirar el valor ----

        n = 19;
        AttributeSet r = new HashAttributeSet();
        r.add(new Copias(5));
        // Se pide sacar 999 copias y saca las 5: remove(Attribute) usa solo la categoria.
        if (!r.remove(new Copias(999)) || r.size() != 0) {
            return n;
        }
        n = 20;
        // Ya no esta: false.
        if (r.remove(new Copias(5))) {
            return n;
        }
        n = 21;
        r.add(new Copias(5));
        if (!r.remove(Copias.class) || r.size() != 0 || r.remove(Copias.class)) {
            return n;
        }
        n = 22;
        // null no es error en ninguna de las dos formas: no hace nada y da false.
        if (r.remove((Class<?>) null) || r.remove((Attribute) null)) {
            return n;
        }

        // ---- constructores ----

        n = 23;
        // Del arreglo, en orden: el ultimo de cada categoria gana.
        Attribute[] arr = new Attribute[] {new Copias(1), new PapelPorTamano(1), new Copias(9)};
        AttributeSet c = new HashAttributeSet(arr);
        if (c.size() != 2 || !new Copias(9).equals(c.get(Copias.class))) {
            return n;
        }
        n = 24;
        // Arreglo null y conjunto null: conjunto vacio, no excepcion.
        if (new HashAttributeSet((Attribute[]) null).size() != 0
                || new HashAttributeSet((AttributeSet) null).size() != 0) {
            return n;
        }
        n = 25;
        // Un atributo null si es error.
        try {
            new HashAttributeSet((Attribute) null);
            return n;
        } catch (NullPointerException e) {
            // esperado
        }
        n = 26;
        // De otro conjunto: copia, no vista.
        AttributeSet base = new HashAttributeSet(new Copias(4));
        AttributeSet copia = new HashAttributeSet(base);
        base.add(new Copias(7));
        if (!new Copias(4).equals(copia.get(Copias.class))) {
            return n;
        }

        // ---- addAll ----

        n = 27;
        AttributeSet a1 = new HashAttributeSet(new Copias(2));
        AttributeSet a2 = new HashAttributeSet();
        a2.add(new PapelPorTamano(3));
        if (!a1.addAll(a2) || a1.size() != 2) {
            return n;
        }
        n = 28;
        // Repetir lo mismo no cambia nada.
        if (a1.addAll(a2)) {
            return n;
        }
        n = 29;
        if (new HashAttributeSet().addAll(new HashAttributeSet())) {
            return n;
        }

        // ---- clear / toArray ----

        n = 30;
        AttributeSet t = new HashAttributeSet(new Attribute[] {new Copias(1),
                                                               new PapelPorTamano(1)});
        Attribute[] sacados = t.toArray();
        if (sacados.length != 2) {
            return n;
        }
        n = 31;
        // El orden no esta definido, asi que se comprueba por pertenencia.
        boolean vioCopias = false;
        boolean vioPapel = false;
        for (int i = 0; i < sacados.length; i++) {
            if (sacados[i].getCategory() == Copias.class) {
                vioCopias = true;
            }
            if (sacados[i].getCategory() == Papel.class) {
                vioPapel = true;
            }
        }
        if (!vioCopias || !vioPapel) {
            return n;
        }
        n = 32;
        t.clear();
        if (t.size() != 0 || !t.isEmpty()) {
            return n;
        }

        // ---- equals / hashCode ----

        n = 33;
        AttributeSet e1 = new HashAttributeSet(new Copias(3));
        AttributeSet e2 = new HashAttributeSet(new Copias(3));
        if (!e1.equals(e2) || e1.hashCode() != e2.hashCode()) {
            return n;
        }
        n = 34;
        // Cruza implementaciones: equals pregunta por la interfaz AttributeSet.
        AttributeSet e3 = new HashPrintJobAttributeSet(new Copias(3));
        if (!e1.equals(e3) || !e3.equals(e1)) {
            return n;
        }
        n = 35;
        if (e1.equals(new HashAttributeSet(new Copias(4)))
                || e1.equals(new HashAttributeSet())
                || e1.equals("no soy un conjunto")) {
            return n;
        }
        n = 36;
        // El hash es la suma de los hashes de los atributos.
        AttributeSet h = new HashAttributeSet(new Attribute[] {new Copias(3),
                                                               new PapelPorTamano(4)});
        if (h.hashCode() != new Copias(3).hashCode() + new PapelPorTamano(4).hashCode()) {
            return n;
        }
        n = 37;
        if (new HashAttributeSet().hashCode() != 0) {
            return n;
        }

        // ---- las cuatro variantes restringidas ----

        n = 38;
        DocAttributeSet d = new HashDocAttributeSet(new Copias(1));
        if (d.size() != 1) {
            return n;
        }
        n = 39;
        // `Marca` es de servicio: no entra en un conjunto de documento. La firma no lo puede
        // atajar porque add toma un Attribute, asi que sale por ClassCastException.
        try {
            ((AttributeSet) d).add(new Marca(1));
            return n;
        } catch (ClassCastException e) {
            // esperado
        }
        n = 40;
        // Tampoco por addAll.
        try {
            d.addAll(new HashAttributeSet(new Marca(1)));
            return n;
        } catch (ClassCastException e) {
            // esperado
        }
        n = 41;
        // Un conjunto de pedido tampoco acepta uno de servicio.
        try {
            ((AttributeSet) new HashPrintRequestAttributeSet()).add(new Marca(1));
            return n;
        } catch (ClassCastException e) {
            // esperado
        }
        n = 42;
        // Y uno de servicio no acepta uno de trabajo.
        try {
            ((AttributeSet) new HashPrintServiceAttributeSet()).add(new Copias(1));
            return n;
        } catch (ClassCastException e) {
            // esperado
        }
        n = 43;
        // Pero el de servicio si acepta a `Marca`.
        if (new HashPrintServiceAttributeSet(new Marca(2)).size() != 1) {
            return n;
        }
        n = 44;
        // La asimetria: se puede PREGUNTAR por una categoria que el conjunto nunca podria tener.
        if (d.containsKey(Marca.class) || d.get(Marca.class) != null) {
            return n;
        }
        n = 45;
        if (new HashPrintJobAttributeSet(new Copias(6)).size() != 1
                || new HashPrintRequestAttributeSet(new Copias(6)).size() != 1) {
            return n;
        }
        n = 46;
        // Los constructores desde arreglo y desde conjunto de las variantes.
        DocAttributeSet d46 = new HashDocAttributeSet(new DocAttribute[] {new Copias(1),
                                                                         new Copias(2)});
        if (d46.size() != 1 || !new Copias(2).equals(d46.get(Copias.class))) {
            return n;
        }
        n = 47;
        if (new HashDocAttributeSet(d46).size() != 1) {
            return n;
        }

        // ---- vista de solo lectura ----

        n = 48;
        AttributeSet u = AttributeSetUtilities.unmodifiableView(new HashAttributeSet(
                new Copias(3)));
        if (u.size() != 1 || !new Copias(3).equals(u.get(Copias.class))
                || !u.containsKey(Copias.class) || !u.containsValue(new Copias(3))
                || u.isEmpty() || u.toArray().length != 1) {
            return n;
        }
        n = 49;
        try {
            u.add(new Copias(9));
            return n;
        } catch (UnmodifiableSetException e) {
            // esperado
        }
        n = 50;
        try {
            u.remove(Copias.class);
            return n;
        } catch (UnmodifiableSetException e) {
            // esperado
        }
        n = 51;
        try {
            u.remove(new Copias(3));
            return n;
        } catch (UnmodifiableSetException e) {
            // esperado
        }
        n = 52;
        try {
            u.addAll(new HashAttributeSet());
            return n;
        } catch (UnmodifiableSetException e) {
            // esperado
        }
        n = 53;
        try {
            u.clear();
            return n;
        } catch (UnmodifiableSetException e) {
            // esperado
        }
        n = 54;
        // UnmodifiableSetException es un RuntimeException: no hay que declararla.
        if (!(new UnmodifiableSetException() instanceof RuntimeException)) {
            return n;
        }
        n = 55;
        if (!"hola".equals(new UnmodifiableSetException("hola").getMessage())) {
            return n;
        }
        n = 56;
        // La vista sigue siendo del tipo del envuelto: eso es todo lo que aportan las cuatro
        // sobrecargas, y se comprueba en tiempo de EJECUCION --con instanceof-- porque el tipo
        // estatico lo daria por bueno aunque la fabrica devolviera el envoltorio generico.
        DocAttributeSet ud = AttributeSetUtilities.unmodifiableView(
                new HashDocAttributeSet(new Copias(1)));
        if (ud.size() != 1 || !(ud instanceof DocAttributeSet)) {
            return n;
        }
        n = 156;
        Object uj = AttributeSetUtilities.unmodifiableView(
                new HashPrintJobAttributeSet(new Copias(1)));
        Object ur = AttributeSetUtilities.unmodifiableView(
                new HashPrintRequestAttributeSet(new Copias(1)));
        Object us = AttributeSetUtilities.unmodifiableView(
                new HashPrintServiceAttributeSet(new Marca(1)));
        if (!(uj instanceof javax.print.attribute.PrintJobAttributeSet)
                || !(ur instanceof javax.print.attribute.PrintRequestAttributeSet)
                || !(us instanceof javax.print.attribute.PrintServiceAttributeSet)) {
            return n;
        }
        n = 157;
        // Y la sincronizada igual, que son las otras cuatro clases internas.
        Object sj = AttributeSetUtilities.synchronizedView(new HashPrintJobAttributeSet());
        Object sr = AttributeSetUtilities.synchronizedView(new HashPrintRequestAttributeSet());
        Object ss = AttributeSetUtilities.synchronizedView(new HashPrintServiceAttributeSet());
        Object sd = AttributeSetUtilities.synchronizedView(new HashDocAttributeSet());
        if (!(sj instanceof javax.print.attribute.PrintJobAttributeSet)
                || !(sr instanceof javax.print.attribute.PrintRequestAttributeSet)
                || !(ss instanceof javax.print.attribute.PrintServiceAttributeSet)
                || !(sd instanceof DocAttributeSet)) {
            return n;
        }
        n = 158;
        // El envoltorio generico NO es ninguno de los cuatro: si la fabrica se equivocara de clase
        // interna, las dos comprobaciones de arriba no alcanzarian para notarlo sin esta.
        Object ug = AttributeSetUtilities.unmodifiableView((AttributeSet) new HashAttributeSet());
        if (ug instanceof DocAttributeSet
                || ug instanceof javax.print.attribute.PrintJobAttributeSet) {
            return n;
        }
        n = 57;
        if (AttributeSetUtilities.unmodifiableView(
                    new HashPrintJobAttributeSet(new Copias(1))).size() != 1
                || AttributeSetUtilities.unmodifiableView(
                    new HashPrintRequestAttributeSet(new Copias(1))).size() != 1
                || AttributeSetUtilities.unmodifiableView(
                    new HashPrintServiceAttributeSet(new Marca(1))).size() != 1) {
            return n;
        }
        n = 58;
        // La vista delega el equals, asi que es igual al conjunto que envuelve.
        AttributeSet env = new HashAttributeSet(new Copias(3));
        if (!AttributeSetUtilities.unmodifiableView(env).equals(env)) {
            return n;
        }
        n = 59;
        try {
            AttributeSetUtilities.unmodifiableView((AttributeSet) null);
            return n;
        } catch (NullPointerException e) {
            // esperado
        }
        n = 60;
        // Es una VISTA, no una copia: lo que cambie abajo se ve arriba.
        AttributeSet abajo = new HashAttributeSet();
        AttributeSet arriba = AttributeSetUtilities.unmodifiableView(abajo);
        abajo.add(new Copias(8));
        if (arriba.size() != 1) {
            return n;
        }

        // ---- vista sincronizada ----

        n = 61;
        AttributeSet sy = AttributeSetUtilities.synchronizedView(new HashAttributeSet());
        if (!sy.add(new Copias(2)) || sy.size() != 1
                || !new Copias(2).equals(sy.get(Copias.class))) {
            return n;
        }
        n = 62;
        // A diferencia de la de solo lectura, esta si modifica.
        if (!sy.remove(Copias.class) || !sy.isEmpty()) {
            return n;
        }
        n = 63;
        // Y delega la restriccion en el conjunto de abajo.
        AttributeSet syd = AttributeSetUtilities.synchronizedView(
                (AttributeSet) new HashDocAttributeSet());
        try {
            syd.add(new Marca(1));
            return n;
        } catch (ClassCastException e) {
            // esperado
        }
        n = 64;
        if (AttributeSetUtilities.synchronizedView(new HashDocAttributeSet()).size() != 0
                || AttributeSetUtilities.synchronizedView(
                    new HashPrintJobAttributeSet()).size() != 0
                || AttributeSetUtilities.synchronizedView(
                    new HashPrintRequestAttributeSet()).size() != 0
                || AttributeSetUtilities.synchronizedView(
                    new HashPrintServiceAttributeSet()).size() != 0) {
            return n;
        }
        n = 65;
        try {
            AttributeSetUtilities.synchronizedView((AttributeSet) null);
            return n;
        } catch (NullPointerException e) {
            // esperado
        }

        // ---- las tres verificaciones sueltas ----

        n = 66;
        if (AttributeSetUtilities.verifyAttributeCategory(Copias.class, Attribute.class)
                != Copias.class) {
            return n;
        }
        n = 67;
        // Un Class que no implementa la interfaz.
        try {
            AttributeSetUtilities.verifyAttributeCategory(String.class, Attribute.class);
            return n;
        } catch (ClassCastException e) {
            // esperado
        }
        n = 68;
        // Algo que ni siquiera es un Class: falla en el cast de la primera linea, misma excepcion.
        try {
            AttributeSetUtilities.verifyAttributeCategory("no soy un Class", Attribute.class);
            return n;
        } catch (ClassCastException e) {
            // esperado
        }
        n = 69;
        // null: NullPointerException. En el JDK sale sola, de llamarle isAssignableFrom a la
        // interfaz con null; en KajiLibrary hay un chequeo explicito porque esta VM se cae en vez
        // de tirar ante `Class.isAssignableFrom(null)` (ver el comentario en AttributeSetUtilities).
        // Lo observable --esta excepcion, en este punto-- es lo mismo en los dos.
        try {
            AttributeSetUtilities.verifyAttributeCategory(null, Attribute.class);
            return n;
        } catch (NullPointerException e) {
            // esperado
        }
        n = 70;
        // Contra una subinterfaz: Copias es DocAttribute, Marca no.
        if (AttributeSetUtilities.verifyAttributeCategory(Copias.class, DocAttribute.class)
                != Copias.class) {
            return n;
        }
        n = 71;
        try {
            AttributeSetUtilities.verifyAttributeCategory(Marca.class, DocAttribute.class);
            return n;
        } catch (ClassCastException e) {
            // esperado
        }
        n = 72;
        Copias c72 = new Copias(3);
        if (AttributeSetUtilities.verifyAttributeValue(c72, Attribute.class) != c72
                || AttributeSetUtilities.verifyAttributeValue(c72, DocAttribute.class) != c72) {
            return n;
        }
        n = 73;
        try {
            AttributeSetUtilities.verifyAttributeValue(new Marca(1), DocAttribute.class);
            return n;
        } catch (ClassCastException e) {
            // esperado
        }
        n = 74;
        // Esta si chequea null explicitamente.
        try {
            AttributeSetUtilities.verifyAttributeValue(null, Attribute.class);
            return n;
        } catch (NullPointerException e) {
            // esperado
        }
        n = 75;
        try {
            AttributeSetUtilities.verifyAttributeValue("no soy un atributo", Attribute.class);
            return n;
        } catch (ClassCastException e) {
            // esperado
        }
        n = 76;
        // verifyCategoryForValue no devuelve nada: o pasa o tira.
        AttributeSetUtilities.verifyCategoryForValue(Copias.class, new Copias(3));
        n = 77;
        try {
            AttributeSetUtilities.verifyCategoryForValue(Marca.class, new Copias(3));
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }
        n = 78;
        // La categoria del atributo, no su clase: PapelPorTamano se verifica contra Papel.
        AttributeSetUtilities.verifyCategoryForValue(Papel.class, new PapelPorTamano(1));
        n = 79;
        try {
            AttributeSetUtilities.verifyCategoryForValue(PapelPorTamano.class,
                                                         new PapelPorTamano(1));
            return n;
        } catch (IllegalArgumentException e) {
            // esperado
        }

        // ---- getCategory / getName de los atributos, que es lo que todo esto indexa ----

        n = 80;
        if (new Copias(3).getCategory() != Copias.class
                || !"copies".equals(new Copias(3).getName())) {
            return n;
        }
        n = 81;
        if (new PapelPorTamano(1).getCategory() != Papel.class
                || new PapelPorBandeja(1).getCategory() != Papel.class) {
            return n;
        }

        return -1;
    }
}
