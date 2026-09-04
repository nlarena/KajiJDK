import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.FlatteningPathIterator;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

// Prueba de comportamiento de java.awt.geom contra el JDK real.
//
// Como es el oraculo: este mismo archivo compila y corre en las dos VMs. Todo caso escribe una linea
// `clave=valor` en un transcript; la tabla ESPERADO de mas abajo es el transcript que **imprimio el
// JDK 25**, pegado tal cual. `run()` recorre los dos y devuelve -1 solo si coinciden linea por linea.
// Los valores esperados no los eligio quien escribio la biblioteca: salen del JDK.
//
// Sobre la exactitud de los double: se comparan por su `String.valueOf`, y eso **es** comparacion de
// bits. Double.toString devuelve la representacion decimal mas corta que identifica univocamente al
// valor, asi que dos double distintos nunca dan la misma cadena, y ademas distingue `0.0` de `-0.0`
// y marca `NaN` e `Infinity`. No se usa `Double.doubleToLongBits` por un motivo concreto y no por
// gusto: el javac de esta casa no resuelve `java.lang.Double` en una unidad de compilacion que
// tambien nombre a `Point2D.Double`, y esta prueba necesita nombrar las clases anidadas.
//
// Donde hay tolerancia esta dicho en el caso y en el nombre de la clave (sufijo `~`), y es siempre
// por un calculo que la justifica: raices de polinomios por formula cerrada (solveCubic pasa por
// acos/cos/pow) y distancias con raiz cuadrada. Todo lo demas --sumas, restas, productos,
// comparaciones, cruces, outcodes-- se exige exacto, porque lo es.
public class CgeomAwtTest {

    static StringBuilder out = new StringBuilder();

    static void p(String clave, String valor) {
        out.append(clave).append('=').append(valor).append('\n');
    }

    static void p(String clave, double v) {
        p(clave, String.valueOf(v));
    }

    static void p(String clave, boolean v) {
        p(clave, String.valueOf(v));
    }

    static void p(String clave, int v) {
        p(clave, String.valueOf(v));
    }

    // Redondea a 9 cifras significativas para los casos con tolerancia declarada.
    static void aprox(String clave, double v) {
        if (v != v) {
            p(clave, "NaN");
            return;
        }
        if (v == 0.0) {
            p(clave, "0");
            return;
        }
        double escala = Math.pow(10.0, 9.0 - Math.ceil(Math.log10(Math.abs(v))));
        p(clave, String.valueOf(Math.rint(v * escala) / escala));
    }

    // --- AffineTransform -----------------------------------------------------------------------

    static void afin() {
        AffineTransform id = new AffineTransform();
        p("at.id.tipo", id.getType());
        p("at.id.det", id.getDeterminant());
        p("at.id.isIdentity", id.isIdentity());

        AffineTransform tr = AffineTransform.getTranslateInstance(3.0, -4.0);
        p("at.tr.tipo", tr.getType());
        p("at.tr.det", tr.getDeterminant());
        p("at.tr.m02", tr.getTranslateX());
        p("at.tr.m12", tr.getTranslateY());

        // Rotacion de cuadrante: el JDK reconoce los multiplos de 90 grados y pone 0 y 1 exactos en
        // vez de dejar el 6.1e-17 que devuelve Math.cos(PI/2). Si esto sale "6.123233995736766E-17"
        // la implementacion no esta haciendo el caso especial.
        AffineTransform rot90 = AffineTransform.getRotateInstance(Math.PI / 2.0);
        p("at.rot90.m00", rot90.getScaleX());
        p("at.rot90.m01", rot90.getShearX());
        p("at.rot90.m10", rot90.getShearY());
        p("at.rot90.m11", rot90.getScaleY());
        p("at.rot90.tipo", rot90.getType());
        AffineTransform rot180 = AffineTransform.getRotateInstance(Math.PI);
        p("at.rot180.m00", rot180.getScaleX());
        p("at.rot180.m11", rot180.getScaleY());

        // Escala negativa sobre un cero: el signo del cero tiene que sobrevivir.
        AffineTransform escNeg = AffineTransform.getScaleInstance(-1.0, -1.0);
        Point2D cero = escNeg.transform(new Point2D.Double(0.0, 0.0), null);
        p("at.escNeg.cero.x", cero.getX());
        p("at.escNeg.cero.y", cero.getY());
        p("at.escNeg.tipo", escNeg.getType());

        // La composicion no conmuta: mismos dos factores, distinto orden, distinto punto.
        AffineTransform a = AffineTransform.getTranslateInstance(10.0, 0.0);
        AffineTransform b = AffineTransform.getScaleInstance(2.0, 2.0);
        AffineTransform ab = new AffineTransform(a);
        ab.concatenate(b);
        AffineTransform ba = new AffineTransform(a);
        ba.preConcatenate(b);
        Point2D q = new Point2D.Double(1.0, 1.0);
        Point2D rab = ab.transform(q, null);
        Point2D rba = ba.transform(q, null);
        p("at.concat.x", rab.getX());
        p("at.concat.y", rab.getY());
        p("at.preconcat.x", rba.getX());
        p("at.preconcat.y", rba.getY());

        // Determinante cero: tiene que tirar, no devolver una matriz con infinitos.
        AffineTransform sing = new AffineTransform(1.0, 2.0, 2.0, 4.0, 0.0, 0.0);
        p("at.sing.det", sing.getDeterminant());
        String veredicto;
        try {
            sing.createInverse();
            veredicto = "NO TIRO";
        } catch (NoninvertibleTransformException e) {
            veredicto = "NoninvertibleTransformException";
        }
        p("at.sing.inversa", veredicto);
        // Escala cero en un solo eje: tambien es singular.
        AffineTransform sing2 = AffineTransform.getScaleInstance(0.0, 5.0);
        String veredicto2;
        try {
            sing2.createInverse();
            veredicto2 = "NO TIRO";
        } catch (NoninvertibleTransformException e) {
            veredicto2 = "NoninvertibleTransformException";
        }
        p("at.sing2.inversa", veredicto2);

        // Ida y vuelta por una transformacion invertible.
        AffineTransform g = new AffineTransform(2.0, 1.0, -1.0, 3.0, 5.0, -2.0);
        p("at.gen.det", g.getDeterminant());
        try {
            AffineTransform ginv = g.createInverse();
            Point2D ida = g.transform(new Point2D.Double(7.0, 11.0), null);
            Point2D vuelta = ginv.transform(ida, null);
            p("at.gen.ida.x", ida.getX());
            p("at.gen.ida.y", ida.getY());
            aprox("at.gen.vuelta.x~", vuelta.getX());
            aprox("at.gen.vuelta.y~", vuelta.getY());
        } catch (NoninvertibleTransformException e) {
            p("at.gen.ida.x", "TIRO");
        }

        // El destino implicito: Double si el origen era Double, Float en cualquier otro caso.
        Point2D dstD = id.transform(new Point2D.Double(1.0, 2.0), null);
        Point2D dstF = id.transform(new Point2D.Float(1.0f, 2.0f), null);
        p("at.dst.deDouble", dstD.getClass().getName());
        p("at.dst.deFloat", dstF.getClass().getName());

        // deltaTransform ignora la traslacion.
        Point2D d = tr.deltaTransform(new Point2D.Double(1.0, 1.0), null);
        p("at.delta.x", d.getX());
        p("at.delta.y", d.getY());

        // Arreglos, incluido el caso solapado (mismo arreglo de origen y destino).
        double[] pts = {0.0, 0.0, 1.0, 0.0, 1.0, 1.0};
        g.transform(pts, 0, pts, 0, 3);
        p("at.arr.0", pts[0]);
        p("at.arr.1", pts[1]);
        p("at.arr.4", pts[4]);
        p("at.arr.5", pts[5]);

        AffineTransform sh = AffineTransform.getShearInstance(2.0, 0.0);
        p("at.shear.tipo", sh.getType());
        p("at.shear.det", sh.getDeterminant());
        AffineTransform flip = new AffineTransform(1.0, 0.0, 0.0, -1.0, 0.0, 0.0);
        p("at.flip.tipo", flip.getType());
    }

    // --- Rectangle2D ---------------------------------------------------------------------------

    static void rect() {
        Rectangle2D r = new Rectangle2D.Double(10.0, 10.0, 20.0, 20.0);

        // "Insideness": el borde izquierdo/superior pertenece, el derecho/inferior no.
        p("r.cont.izqsup", r.contains(10.0, 10.0));
        p("r.cont.derinf", r.contains(30.0, 30.0));
        p("r.cont.derSup", r.contains(30.0, 10.0));
        p("r.cont.centro", r.contains(20.0, 20.0));
        p("r.cont.bordeIzq", r.contains(10.0, 20.0));
        p("r.cont.bordeDer", r.contains(30.0, 20.0));

        p("r.outcode.dentro", r.outcode(20.0, 20.0));
        p("r.outcode.izq", r.outcode(0.0, 20.0));
        p("r.outcode.arribaIzq", r.outcode(0.0, 0.0));
        p("r.outcode.esquinaDerInf", r.outcode(30.0, 30.0));

        // Rectangulo de area cero: todo queda "afuera" por los dos lados a la vez.
        Rectangle2D vacio = new Rectangle2D.Double(5.0, 5.0, 0.0, 10.0);
        p("r.vacio.isEmpty", vacio.isEmpty());
        p("r.vacio.outcode", vacio.outcode(5.0, 7.0));
        p("r.vacio.contains", vacio.contains(5.0, 7.0));
        p("r.vacio.intersects", vacio.intersects(0.0, 0.0, 20.0, 20.0));

        // intersectsLine: el caso que mas se implementa mal es el segmento **enteramente adentro**.
        p("r.line.adentro", r.intersectsLine(15.0, 15.0, 25.0, 25.0));
        p("r.line.cruza", r.intersectsLine(0.0, 20.0, 40.0, 20.0));
        p("r.line.afuera", r.intersectsLine(0.0, 0.0, 5.0, 5.0));
        p("r.line.tocaEsquina", r.intersectsLine(0.0, 20.0, 10.0, 10.0));
        p("r.line.rozaBordeIzq", r.intersectsLine(10.0, 0.0, 10.0, 40.0));
        p("r.line.paralelaAfuera", r.intersectsLine(0.0, 5.0, 40.0, 5.0));

        // intersect() NO normaliza: si no se tocan, el destino queda con ancho negativo.
        Rectangle2D a = new Rectangle2D.Double(0.0, 0.0, 4.0, 4.0);
        Rectangle2D b = new Rectangle2D.Double(9.0, 9.0, 4.0, 4.0);
        Rectangle2D dst = new Rectangle2D.Double();
        Rectangle2D.intersect(a, b, dst);
        p("r.intersect.x", dst.getX());
        p("r.intersect.y", dst.getY());
        p("r.intersect.w", dst.getWidth());
        p("r.intersect.h", dst.getHeight());
        p("r.intersect.isEmpty", dst.isEmpty());

        Rectangle2D uni = a.createUnion(b);
        p("r.union.x", uni.getX());
        p("r.union.w", uni.getWidth());
        p("r.union.clase", uni.getClass().getName());
        Rectangle2D inter = a.createIntersection(b);
        p("r.inter.w", inter.getWidth());

        Rectangle2D rf = new Rectangle2D.Float(1.5f, 2.5f, 3.0f, 4.0f);
        Rectangle2D uf = rf.createUnion(new Rectangle2D.Float(0.0f, 0.0f, 1.0f, 1.0f));
        p("r.unionF.clase", uf.getClass().getName());
        p("r.unionF.w", uf.getWidth());
        Rectangle2D ud = rf.createUnion(new Rectangle2D.Double(0.0, 0.0, 1.0, 1.0));
        p("r.unionFD.clase", ud.getClass().getName());

        p("r.add.antes", r.getWidth());
        Rectangle2D r2 = new Rectangle2D.Double(10.0, 10.0, 20.0, 20.0);
        r2.add(40.0, 10.0);
        p("r.add.w", r2.getWidth());

        Rectangle bounds = r.getBounds();
        p("r.bounds.x", bounds.x);
        p("r.bounds.w", bounds.width);
        Rectangle2D frac = new Rectangle2D.Double(1.5, 1.5, 2.25, 2.25);
        Rectangle fb = frac.getBounds();
        // El entero tiene que **contener** al de coma flotante: origen hacia abajo, tamano hacia
        // arriba. 1.5..3.75 entra en 1..4.
        p("r.boundsFrac.x", fb.x);
        p("r.boundsFrac.y", fb.y);
        p("r.boundsFrac.w", fb.width);
        p("r.boundsFrac.h", fb.height);

        p("r.setFromDiag.w", diagW());
        p("r.contRect.si", r.contains(12.0, 12.0, 5.0, 5.0));
        p("r.contRect.borde", r.contains(10.0, 10.0, 20.0, 20.0));
        p("r.contRect.no", r.contains(25.0, 25.0, 10.0, 10.0));
        p("r.interRect.tocaBorde", r.intersects(30.0, 10.0, 5.0, 5.0));
        p("r.interRect.solapa", r.intersects(25.0, 25.0, 10.0, 10.0));
    }

    static double diagW() {
        Rectangle2D r = new Rectangle2D.Double();
        r.setFrameFromDiagonal(10.0, 10.0, 2.0, 3.0);
        return r.getWidth();
    }

    // --- Line2D --------------------------------------------------------------------------------

    static void linea() {
        p("l.cruzan", Line2D.linesIntersect(0.0, 0.0, 10.0, 10.0, 0.0, 10.0, 10.0, 0.0));
        p("l.paralelas", Line2D.linesIntersect(0.0, 0.0, 10.0, 0.0, 0.0, 5.0, 10.0, 5.0));
        p("l.tocanPunta", Line2D.linesIntersect(0.0, 0.0, 10.0, 0.0, 10.0, 0.0, 20.0, 10.0));
        // Colineales que se superponen: tienen puntos en comun, es true.
        p("l.colinealSolapa", Line2D.linesIntersect(0.0, 0.0, 10.0, 0.0, 5.0, 0.0, 15.0, 0.0));
        // Colineales disjuntos: false.
        p("l.colinealDisjunto", Line2D.linesIntersect(0.0, 0.0, 10.0, 0.0, 20.0, 0.0, 30.0, 0.0));
        // Colineales que se tocan justo en la punta.
        p("l.colinealPunta", Line2D.linesIntersect(0.0, 0.0, 10.0, 0.0, 10.0, 0.0, 20.0, 0.0));
        // Un extremo apoyado en el medio del otro segmento (T).
        p("l.te", Line2D.linesIntersect(0.0, 0.0, 10.0, 0.0, 5.0, 0.0, 5.0, 10.0));
        // Segmento degenerado (un punto) sobre el otro.
        p("l.puntoSobre", Line2D.linesIntersect(0.0, 0.0, 10.0, 0.0, 5.0, 0.0, 5.0, 0.0));
        p("l.puntoFuera", Line2D.linesIntersect(0.0, 0.0, 10.0, 0.0, 5.0, 1.0, 5.0, 1.0));

        p("l.ccw.izq", Line2D.relativeCCW(0.0, 0.0, 10.0, 0.0, 5.0, -5.0));
        p("l.ccw.der", Line2D.relativeCCW(0.0, 0.0, 10.0, 0.0, 5.0, 5.0));
        p("l.ccw.enSegmento", Line2D.relativeCCW(0.0, 0.0, 10.0, 0.0, 5.0, 0.0));
        p("l.ccw.pasadoFin", Line2D.relativeCCW(0.0, 0.0, 10.0, 0.0, 15.0, 0.0));
        p("l.ccw.antesInicio", Line2D.relativeCCW(0.0, 0.0, 10.0, 0.0, -5.0, 0.0));
        p("l.ccw.p1", Line2D.relativeCCW(0.0, 0.0, 10.0, 0.0, 0.0, 0.0));
        p("l.ccw.p2", Line2D.relativeCCW(0.0, 0.0, 10.0, 0.0, 10.0, 0.0));

        p("l.ptSegSq.medio", Line2D.ptSegDistSq(0.0, 0.0, 10.0, 0.0, 5.0, 3.0));
        p("l.ptSegSq.pasado", Line2D.ptSegDistSq(0.0, 0.0, 10.0, 0.0, 14.0, 3.0));
        p("l.ptSegSq.antes", Line2D.ptSegDistSq(0.0, 0.0, 10.0, 0.0, -4.0, 3.0));
        // Segmento degenerado: la distancia al segmento es la distancia al punto...
        p("l.ptSegSq.degenerado", Line2D.ptSegDistSq(5.0, 5.0, 5.0, 5.0, 8.0, 9.0));
        // ...pero la de la recta infinita es NaN, porque esa recta no existe.
        p("l.ptLineSq.degenerado", Line2D.ptLineDistSq(5.0, 5.0, 5.0, 5.0, 8.0, 9.0));
        p("l.ptLineSq.pasado", Line2D.ptLineDistSq(0.0, 0.0, 10.0, 0.0, 14.0, 3.0));
        p("l.ptSeg.medio", Line2D.ptSegDist(0.0, 0.0, 10.0, 0.0, 5.0, 3.0));

        Line2D seg = new Line2D.Double(0.0, 0.0, 10.0, 10.0);
        // Un segmento no encierra area: contains es siempre false.
        p("l.contains.sobreEl", seg.contains(5.0, 5.0));
        p("l.contains.rect", seg.contains(1.0, 1.0, 2.0, 2.0));
        p("l.intersectsRect", seg.intersects(4.0, 4.0, 2.0, 2.0));
        p("l.intersectsRectNo", seg.intersects(20.0, 0.0, 2.0, 2.0));
        Rectangle2D lb = seg.getBounds2D();
        p("l.bounds.x", lb.getX());
        p("l.bounds.w", lb.getWidth());
        p("l.bounds.clase", lb.getClass().getName());
        Line2D segF = new Line2D.Float(3.0f, 4.0f, 1.0f, 2.0f);
        Rectangle2D lbf = segF.getBounds2D();
        p("l.boundsF.clase", lbf.getClass().getName());
        p("l.boundsF.x", lbf.getX());
        p("l.p1.clase", seg.getP1().getClass().getName());
        p("l.p1F.clase", segF.getP1().getClass().getName());
    }

    // --- curvas --------------------------------------------------------------------------------

    static void curvas() {
        // solveQuadratic: dos raices, una doble, ninguna, lineal, y la constante (-1).
        double[] eq = {-6.0, 1.0, 1.0};
        double[] res = new double[3];
        int n = QuadCurve2D.solveQuadratic(eq, res);
        p("q.dos.n", n);
        ordenar(res, n);
        aprox("q.dos.r0~", res[0]);
        aprox("q.dos.r1~", res[1]);

        double[] eqd = {1.0, -2.0, 1.0};
        n = QuadCurve2D.solveQuadratic(eqd, res);
        p("q.doble.n", n);
        ordenar(res, n);
        aprox("q.doble.r0~", res[0]);

        double[] eqn = {5.0, 0.0, 1.0};
        p("q.ninguna.n", QuadCurve2D.solveQuadratic(eqn, res));

        double[] eql = {-4.0, 2.0, 0.0};
        n = QuadCurve2D.solveQuadratic(eql, res);
        p("q.lineal.n", n);
        aprox("q.lineal.r0~", res[0]);

        // a = 0 y b = 0: la "ecuacion" es una constante. El contrato pide -1.
        double[] eqc = {0.0, 0.0, 0.0};
        p("q.constanteCero.n", QuadCurve2D.solveQuadratic(eqc, res));
        double[] eqc2 = {7.0, 0.0, 0.0};
        p("q.constanteNoCero.n", QuadCurve2D.solveQuadratic(eqc2, res));

        // solveCubic: (t-1)(t-2)(t-3) = t^3 - 6t^2 + 11t - 6
        double[] c3 = {-6.0, 11.0, -6.0, 1.0};
        double[] r3 = new double[4];
        n = CubicCurve2D.solveCubic(c3, r3);
        p("c.tres.n", n);
        ordenar(r3, n);
        aprox("c.tres.r0~", r3[0]);
        aprox("c.tres.r1~", r3[1]);
        aprox("c.tres.r2~", r3[2]);

        // t^3 - 1 = 0: una sola raiz real.
        double[] c1 = {-1.0, 0.0, 0.0, 1.0};
        n = CubicCurve2D.solveCubic(c1, r3);
        p("c.una.n", n);
        aprox("c.una.r0~", r3[0]);

        // Coeficiente cubico nulo: cae en la cuadratica.
        double[] c0 = {-6.0, 1.0, 1.0, 0.0};
        n = CubicCurve2D.solveCubic(c0, r3);
        p("c.degradaACuad.n", n);
        ordenar(r3, n);
        aprox("c.degradaACuad.r0~", r3[0]);

        // Curva cubica concreta.
        CubicCurve2D cc = new CubicCurve2D.Double(0.0, 0.0, 0.0, 10.0, 10.0, 10.0, 10.0, 0.0);
        Rectangle2D cb = cc.getBounds2D();
        // Caja **ajustada**, no la del poligono de control: llega hasta y=7.5, que es donde llega la
        // curva, y no hasta el y=10 de los puntos de control. La comente al reves cuando escribi el
        // caso; el JDK me corrigio.
        //
        // El alto va con tolerancia y los otros tres no, por un motivo concreto: los tres salen de
        // los extremos de la curva, que son datos de entrada, y el alto sale de EVALUAR la curva en
        // la raiz de su derivada. El JDK llega a esa raiz subdividiendo y nosotros resolviendo la
        // cuadratica, asi que los ultimos bits difieren -- el JDK dice 7.500000000000007 y aca sale
        // 7.5, que es el valor exacto. Exigir los bits del JDK seria exigir SU algoritmo, que no es
        // contrato.
        p("c.bounds.x", cb.getX());
        p("c.bounds.y", cb.getY());
        p("c.bounds.w", cb.getWidth());
        aprox("c.bounds.h~", cb.getHeight());
        p("c.bounds.clase", cb.getClass().getName());
        p("c.contains.dentro", cc.contains(5.0, 5.0));
        p("c.contains.fuera", cc.contains(5.0, 9.0));
        p("c.contains.debajo", cc.contains(5.0, -1.0));
        p("c.contains.NaN", cc.contains(Math.sqrt(-1.0), 5.0));
        p("c.intersects.si", cc.intersects(4.0, 4.0, 2.0, 2.0));
        p("c.intersects.no", cc.intersects(20.0, 20.0, 2.0, 2.0));
        p("c.intersects.vacio", cc.intersects(5.0, 5.0, 0.0, 0.0));
        p("c.containsRect.si", cc.contains(4.0, 4.0, 2.0, 2.0));
        p("c.containsRect.no", cc.contains(0.0, 0.0, 10.0, 10.0));
        aprox("c.flatness~", cc.getFlatness());
        p("c.flatnessSq", cc.getFlatnessSq());

        CubicCurve2D izq = new CubicCurve2D.Double();
        CubicCurve2D der = new CubicCurve2D.Double();
        cc.subdivide(izq, der);
        // La subdivision es exacta: solo sumas y divisiones por dos.
        p("c.sub.izq.x2", izq.getX2());
        p("c.sub.izq.y2", izq.getY2());
        p("c.sub.der.x1", der.getX1());
        p("c.sub.der.y1", der.getY1());
        p("c.sub.izq.ctrlx1", izq.getCtrlX1());
        p("c.sub.der.ctrlx2", der.getCtrlX2());

        // Curva cubica degenerada en una recta: todos los puntos alineados.
        CubicCurve2D recta = new CubicCurve2D.Double(0.0, 0.0, 1.0, 1.0, 2.0, 2.0, 3.0, 3.0);
        p("c.recta.flatnessSq", recta.getFlatnessSq());
        p("c.recta.contains", recta.contains(1.5, 1.5));

        // Cuadratica.
        QuadCurve2D qc = new QuadCurve2D.Double(0.0, 0.0, 5.0, 10.0, 10.0, 0.0);
        Rectangle2D qb = qc.getBounds2D();
        // Con tolerancia por lo mismo que `c.bounds.h~`: el JDK dice 5.000000000000007 y aca sale 5.0.
        aprox("q.bounds.h~", qb.getHeight());
        p("q.bounds.w", qb.getWidth());
        p("q.contains.dentro", qc.contains(5.0, 3.0));
        p("q.contains.fuera", qc.contains(5.0, 9.0));
        p("q.intersects.si", qc.intersects(4.0, 1.0, 2.0, 2.0));
        p("q.intersects.no", qc.intersects(20.0, 20.0, 2.0, 2.0));
        aprox("q.flatness~", qc.getFlatness());
        p("q.flatnessSq", qc.getFlatnessSq());
        QuadCurve2D qi = new QuadCurve2D.Double();
        QuadCurve2D qd = new QuadCurve2D.Double();
        qc.subdivide(qi, qd);
        p("q.sub.izq.x2", qi.getX2());
        p("q.sub.izq.y2", qi.getY2());
        p("q.sub.izq.ctrlx", qi.getCtrlX());
        p("q.sub.der.ctrly", qd.getCtrlY());

        QuadCurve2D qrecta = new QuadCurve2D.Double(0.0, 0.0, 1.0, 1.0, 2.0, 2.0);
        p("q.recta.flatnessSq", qrecta.getFlatnessSq());
    }

    static void ordenar(double[] v, int n) {
        int i = 0;
        while (i < n) {
            int j = i + 1;
            while (j < n) {
                if (v[j] < v[i]) {
                    double t = v[i];
                    v[i] = v[j];
                    v[j] = t;
                }
                j = j + 1;
            }
            i = i + 1;
        }
    }

    // --- figuras rectangulares -----------------------------------------------------------------

    static void figuras() {
        Ellipse2D e = new Ellipse2D.Double(0.0, 0.0, 10.0, 10.0);
        p("e.centro", e.contains(5.0, 5.0));
        // Justo sobre el borde: el punto (10,5) esta en la circunferencia y queda AFUERA.
        p("e.bordeDer", e.contains(10.0, 5.0));
        p("e.bordeIzq", e.contains(0.0, 5.0));
        p("e.esquina", e.contains(0.0, 0.0));
        p("e.fuera", e.contains(9.5, 9.5));
        p("e.intersectsCentro", e.intersects(4.0, 4.0, 2.0, 2.0));
        p("e.intersectsEsquina", e.intersects(0.0, 0.0, 1.0, 1.0));
        p("e.containsRect", e.contains(4.0, 4.0, 2.0, 2.0));
        p("e.vacia.contains", new Ellipse2D.Double(0.0, 0.0, 0.0, 10.0).contains(0.0, 5.0));
        Rectangle2D eb = e.getBounds2D();
        p("e.bounds.w", eb.getWidth());

        RoundRectangle2D rr = new RoundRectangle2D.Double(0.0, 0.0, 20.0, 20.0, 8.0, 8.0);
        p("rr.centro", rr.contains(10.0, 10.0));
        // La esquina redondeada recorta: (0,0) ya no pertenece.
        p("rr.esquina", rr.contains(0.0, 0.0));
        p("rr.medioBorde", rr.contains(0.0, 10.0));
        p("rr.dentroCerca", rr.contains(1.0, 1.0));
        p("rr.intersects", rr.intersects(0.0, 0.0, 2.0, 2.0));
        p("rr.arcW", rr.getArcWidth());

        Arc2D arco = new Arc2D.Double(0.0, 0.0, 10.0, 10.0, 0.0, 90.0, Arc2D.PIE);
        p("a.pie.dentro", arco.contains(6.0, 4.0));
        p("a.pie.fueraCuadrante", arco.contains(4.0, 6.0));
        p("a.pie.centro", arco.contains(5.0, 5.0));
        p("a.tipo", arco.getArcType());
        p("a.extent", arco.getAngleExtent());
        Point2D ini = arco.getStartPoint();
        p("a.start.x", ini.getX());
        p("a.start.y", ini.getY());
        Point2D fin = arco.getEndPoint();
        aprox("a.end.x~", fin.getX());
        aprox("a.end.y~", fin.getY());
        Arc2D abierto = new Arc2D.Double(0.0, 0.0, 10.0, 10.0, 0.0, 90.0, Arc2D.OPEN);
        p("a.open.contains", abierto.contains(6.0, 4.0));
        Arc2D cuerda = new Arc2D.Double(0.0, 0.0, 10.0, 10.0, 0.0, 90.0, Arc2D.CHORD);
        p("a.chord.centro", cuerda.contains(5.0, 5.0));
        p("a.chord.cerca", cuerda.contains(7.0, 3.0));
        Rectangle2D ab = arco.getBounds2D();
        aprox("a.bounds.x~", ab.getX());
        aprox("a.bounds.w~", ab.getWidth());
    }

    // --- Path2D y aplanado ---------------------------------------------------------------------

    static void caminos() {
        Path2D.Double pd = new Path2D.Double();
        pd.moveTo(0.0, 0.0);
        pd.lineTo(10.0, 0.0);
        pd.lineTo(10.0, 10.0);
        pd.lineTo(0.0, 10.0);
        pd.closePath();
        p("p.contains.centro", pd.contains(5.0, 5.0));
        p("p.contains.borde", pd.contains(0.0, 0.0));
        p("p.contains.bordeDer", pd.contains(10.0, 5.0));
        p("p.contains.fuera", pd.contains(15.0, 5.0));
        p("p.containsRect", pd.contains(2.0, 2.0, 5.0, 5.0));
        p("p.intersects", pd.intersects(8.0, 8.0, 5.0, 5.0));
        p("p.windingRule", pd.getWindingRule());
        Point2D cur = pd.getCurrentPoint();
        p("p.current.x", cur.getX());
        p("p.current.y", cur.getY());
        Rectangle2D pb = pd.getBounds2D();
        p("p.bounds.w", pb.getWidth());
        p("p.segmentos", contarSegmentos(pd.getPathIterator(null)));
        p("p.transcript", transcribir(pd.getPathIterator(null)));

        // El mismo camino transformado.
        AffineTransform t = AffineTransform.getTranslateInstance(100.0, 0.0);
        p("p.transcriptTx", transcribir(pd.getPathIterator(t)));

        // Regla par/impar contra no-cero: un cuadrado con otro adentro, mismo sentido.
        Path2D.Double dosCuadrados = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        cuadrado(dosCuadrados, 0.0, 0.0, 20.0);
        cuadrado(dosCuadrados, 5.0, 5.0, 10.0);
        p("p.evenodd.centro", dosCuadrados.contains(10.0, 10.0));
        p("p.evenodd.anillo", dosCuadrados.contains(2.0, 10.0));
        Path2D.Double nonzero = new Path2D.Double(Path2D.WIND_NON_ZERO);
        cuadrado(nonzero, 0.0, 0.0, 20.0);
        cuadrado(nonzero, 5.0, 5.0, 10.0);
        p("p.nonzero.centro", nonzero.contains(10.0, 10.0));

        // Path2D.Float: el iterador en double tiene que ampliar, no reinterpretar.
        Path2D.Float pf = new Path2D.Float();
        pf.moveTo(0.1f, 0.2f);
        pf.lineTo(1.5f, 2.5f);
        p("p.float.transcript", transcribir(pf.getPathIterator(null)));

        // Aplanado de una cubica.
        Path2D.Double curva = new Path2D.Double();
        curva.moveTo(0.0, 0.0);
        curva.curveTo(0.0, 10.0, 10.0, 10.0, 10.0, 0.0);
        p("f.grueso.segmentos", contarSegmentos(curva.getPathIterator(null, 2.0)));
        p("f.fino.segmentos", contarSegmentos(curva.getPathIterator(null, 0.1)));
        p("f.grueso.transcript", transcribir(curva.getPathIterator(null, 2.0)));
        // El aplanado nunca devuelve QUADTO ni CUBICTO.
        p("f.sinCurvas", soloRectas(curva.getPathIterator(null, 0.5)));
        FlatteningPathIterator fpi =
                new FlatteningPathIterator(curva.getPathIterator(null), 0.5, 4);
        p("f.limite", fpi.getRecursionLimit());
        aprox("f.flatness~", fpi.getFlatness());
        p("f.limitado.segmentos", contarSegmentos(
                new FlatteningPathIterator(curva.getPathIterator(null), 0.001, 2)));
        String malaPlanitud;
        try {
            new FlatteningPathIterator(curva.getPathIterator(null), -1.0);
            malaPlanitud = "NO TIRO";
        } catch (IllegalArgumentException ex) {
            malaPlanitud = "IllegalArgumentException";
        }
        p("f.planitudNegativa", malaPlanitud);

        // Aplanar una cuadratica.
        Path2D.Double cuad = new Path2D.Double();
        cuad.moveTo(0.0, 0.0);
        cuad.quadTo(5.0, 10.0, 10.0, 0.0);
        p("f.quad.segmentos", contarSegmentos(cuad.getPathIterator(null, 1.0)));
        p("f.quad.transcript", transcribir(cuad.getPathIterator(null, 1.0)));

        // Aplanar una figura ya plana no la cambia.
        p("f.rect.segmentos", contarSegmentos(pd.getPathIterator(null, 0.1)));

        // GeneralPath es un Path2D.Float con otro nombre.
        GeneralPath gp = new GeneralPath();
        gp.moveTo(0.0f, 0.0f);
        gp.lineTo(4.0f, 0.0f);
        gp.lineTo(4.0f, 4.0f);
        gp.closePath();
        p("gp.contains", gp.contains(3.0, 1.0));
        p("gp.contains.no", gp.contains(1.0, 3.0));
        p("gp.esPath2DFloat", gp instanceof Path2D.Float);
        p("gp.winding", gp.getWindingRule());
        p("gp.windingEvenOdd", new GeneralPath(Path2D.WIND_EVEN_ODD).getWindingRule());
        Shape s = gp;
        p("gp.bounds.w", s.getBounds2D().getWidth());
        GeneralPath copia = new GeneralPath(pd);
        p("gp.copia.contains", copia.contains(5.0, 5.0));

        // createTransformedShape.
        Shape ts = pd.createTransformedShape(AffineTransform.getScaleInstance(2.0, 2.0));
        p("ts.bounds.w", ts.getBounds2D().getWidth());
        p("ts.contains", ts.contains(15.0, 15.0));

        Path2D.Double vacio = new Path2D.Double();
        p("p.vacio.contains", vacio.contains(0.0, 0.0));
        p("p.vacio.segmentos", contarSegmentos(vacio.getPathIterator(null)));
        Rectangle2D vb = vacio.getBounds2D();
        p("p.vacio.bounds.w", vb.getWidth());
    }

    static void cuadrado(Path2D p, double x, double y, double lado) {
        p.moveTo(x, y);
        p.lineTo(x + lado, y);
        p.lineTo(x + lado, y + lado);
        p.lineTo(x, y + lado);
        p.closePath();
    }

    static int contarSegmentos(PathIterator pi) {
        int n = 0;
        double[] c = new double[6];
        while (!pi.isDone()) {
            pi.currentSegment(c);
            n = n + 1;
            pi.next();
        }
        return n;
    }

    static boolean soloRectas(PathIterator pi) {
        double[] c = new double[6];
        while (!pi.isDone()) {
            int t = pi.currentSegment(c);
            if (t == PathIterator.SEG_QUADTO || t == PathIterator.SEG_CUBICTO) {
                return false;
            }
            pi.next();
        }
        return true;
    }

    static String transcribir(PathIterator pi) {
        StringBuilder sb = new StringBuilder();
        double[] c = new double[6];
        while (!pi.isDone()) {
            int t = pi.currentSegment(c);
            sb.append(t);
            int n = 0;
            if (t == PathIterator.SEG_MOVETO || t == PathIterator.SEG_LINETO) {
                n = 2;
            } else if (t == PathIterator.SEG_QUADTO) {
                n = 4;
            } else if (t == PathIterator.SEG_CUBICTO) {
                n = 6;
            }
            int i = 0;
            while (i < n) {
                sb.append(':').append(c[i]);
                i = i + 1;
            }
            sb.append(' ');
            pi.next();
        }
        return sb.toString();
    }

    // --- Point2D y Dimension2D -----------------------------------------------------------------

    static void puntos() {
        Point2D a = new Point2D.Double(3.0, 4.0);
        p("pt.dist", a.distance(0.0, 0.0));
        p("pt.distSq", a.distanceSq(0.0, 0.0));
        p("pt.equalsFloat", a.equals(new Point2D.Float(3.0f, 4.0f)));
        p("pt.hashIgual", a.hashCode() == new Point2D.Double(3.0, 4.0).hashCode());
        p("pt.toString", a.toString());
        Point2D b = new Point2D.Float(1.5f, 2.5f);
        p("ptF.x", b.getX());
        p("ptF.toString", b.toString());
        p("pt.estatica", Point2D.distance(0.0, 0.0, 3.0, 4.0));
        Point2D c = (Point2D) a.clone();
        c.setLocation(9.0, 9.0);
        p("pt.cloneIndependiente", a.getX());
    }

    static String transcript() {
        out = new StringBuilder();
        afin();
        rect();
        linea();
        curvas();
        figuras();
        caminos();
        puntos();
        return out.toString();
    }

    public static int run() {
        String[] vivo = transcript().split("\n");
        int fallas = 0;
        int i = 0;
        while (i < vivo.length || i < ESPERADO.length) {
            String v = (i < vivo.length) ? vivo[i] : "<falta>";
            String e = (i < ESPERADO.length) ? ESPERADO[i] : "<sobra>";
            if (!v.equals(e)) {
                System.out.println("DIFIERE linea " + i);
                System.out.println("  esperado: " + e);
                System.out.println("  obtenido: " + v);
                fallas = fallas + 1;
            }
            i = i + 1;
        }
        if (fallas != 0) {
            System.out.println("CgeomAwtTest: " + fallas + " diferencias");
            return fallas;
        }
        return -1;
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("--volcar")) {
            System.out.print(transcript());
            return;
        }
        System.out.println("CgeomAwtTest -> " + run());
    }

    // Transcript del JDK 25.0.2, pegado literal. Ver la nota del encabezado.
    static final String[] ESPERADO = {
        "at.id.tipo=0",
        "at.id.det=1.0",
        "at.id.isIdentity=true",
        "at.tr.tipo=1",
        "at.tr.det=1.0",
        "at.tr.m02=3.0",
        "at.tr.m12=-4.0",
        "at.rot90.m00=0.0",
        "at.rot90.m01=-1.0",
        "at.rot90.m10=1.0",
        "at.rot90.m11=0.0",
        "at.rot90.tipo=8",
        "at.rot180.m00=-1.0",
        "at.rot180.m11=-1.0",
        "at.escNeg.cero.x=-0.0",
        "at.escNeg.cero.y=-0.0",
        "at.escNeg.tipo=8",
        "at.concat.x=12.0",
        "at.concat.y=2.0",
        "at.preconcat.x=22.0",
        "at.preconcat.y=2.0",
        "at.sing.det=0.0",
        "at.sing.inversa=NoninvertibleTransformException",
        "at.sing2.inversa=NoninvertibleTransformException",
        "at.gen.det=7.0",
        "at.gen.ida.x=8.0",
        "at.gen.ida.y=38.0",
        "at.gen.vuelta.x~=7.0",
        "at.gen.vuelta.y~=11.0",
        "at.dst.deDouble=java.awt.geom.Point2D$Double",
        "at.dst.deFloat=java.awt.geom.Point2D$Float",
        "at.delta.x=1.0",
        "at.delta.y=1.0",
        "at.arr.0=5.0",
        "at.arr.1=-2.0",
        "at.arr.4=6.0",
        "at.arr.5=2.0",
        "at.shear.tipo=32",
        "at.shear.det=1.0",
        "at.flip.tipo=64",
        "r.cont.izqsup=true",
        "r.cont.derinf=false",
        "r.cont.derSup=false",
        "r.cont.centro=true",
        "r.cont.bordeIzq=true",
        "r.cont.bordeDer=false",
        "r.outcode.dentro=0",
        "r.outcode.izq=1",
        "r.outcode.arribaIzq=3",
        "r.outcode.esquinaDerInf=0",
        "r.vacio.isEmpty=true",
        "r.vacio.outcode=5",
        "r.vacio.contains=false",
        "r.vacio.intersects=false",
        "r.line.adentro=true",
        "r.line.cruza=true",
        "r.line.afuera=false",
        "r.line.tocaEsquina=true",
        "r.line.rozaBordeIzq=true",
        "r.line.paralelaAfuera=false",
        "r.intersect.x=9.0",
        "r.intersect.y=9.0",
        "r.intersect.w=-5.0",
        "r.intersect.h=-5.0",
        "r.intersect.isEmpty=true",
        "r.union.x=0.0",
        "r.union.w=13.0",
        "r.union.clase=java.awt.geom.Rectangle2D$Double",
        "r.inter.w=-5.0",
        "r.unionF.clase=java.awt.geom.Rectangle2D$Float",
        "r.unionF.w=4.5",
        "r.unionFD.clase=java.awt.geom.Rectangle2D$Double",
        "r.add.antes=20.0",
        "r.add.w=30.0",
        "r.bounds.x=10",
        "r.bounds.w=20",
        "r.boundsFrac.x=1",
        "r.boundsFrac.y=1",
        "r.boundsFrac.w=3",
        "r.boundsFrac.h=3",
        "r.setFromDiag.w=8.0",
        "r.contRect.si=true",
        "r.contRect.borde=true",
        "r.contRect.no=false",
        "r.interRect.tocaBorde=false",
        "r.interRect.solapa=true",
        "l.cruzan=true",
        "l.paralelas=false",
        "l.tocanPunta=true",
        "l.colinealSolapa=true",
        "l.colinealDisjunto=false",
        "l.colinealPunta=true",
        "l.te=true",
        "l.puntoSobre=true",
        "l.puntoFuera=false",
        "l.ccw.izq=1",
        "l.ccw.der=-1",
        "l.ccw.enSegmento=0",
        "l.ccw.pasadoFin=1",
        "l.ccw.antesInicio=-1",
        "l.ccw.p1=0",
        "l.ccw.p2=0",
        "l.ptSegSq.medio=9.0",
        "l.ptSegSq.pasado=25.0",
        "l.ptSegSq.antes=25.0",
        "l.ptSegSq.degenerado=25.0",
        "l.ptLineSq.degenerado=NaN",
        "l.ptLineSq.pasado=9.0",
        "l.ptSeg.medio=3.0",
        "l.contains.sobreEl=false",
        "l.contains.rect=false",
        "l.intersectsRect=true",
        "l.intersectsRectNo=false",
        "l.bounds.x=0.0",
        "l.bounds.w=10.0",
        "l.bounds.clase=java.awt.geom.Rectangle2D$Double",
        "l.boundsF.clase=java.awt.geom.Rectangle2D$Float",
        "l.boundsF.x=1.0",
        "l.p1.clase=java.awt.geom.Point2D$Double",
        "l.p1F.clase=java.awt.geom.Point2D$Float",
        "q.dos.n=2",
        "q.dos.r0~=-3.0",
        "q.dos.r1~=2.0",
        "q.doble.n=2",
        "q.doble.r0~=1.0",
        "q.ninguna.n=0",
        "q.lineal.n=1",
        "q.lineal.r0~=2.0",
        "q.constanteCero.n=-1",
        "q.constanteNoCero.n=-1",
        "c.tres.n=3",
        "c.tres.r0~=1.0",
        "c.tres.r1~=2.0",
        "c.tres.r2~=3.0",
        "c.una.n=1",
        "c.una.r0~=1.0",
        "c.degradaACuad.n=2",
        "c.degradaACuad.r0~=-3.0",
        "c.bounds.x=0.0",
        "c.bounds.y=0.0",
        "c.bounds.w=10.0",
        "c.bounds.h~=7.5",
        "c.bounds.clase=java.awt.geom.Rectangle2D$Double",
        "c.contains.dentro=true",
        "c.contains.fuera=false",
        "c.contains.debajo=false",
        "c.contains.NaN=false",
        "c.intersects.si=true",
        "c.intersects.no=false",
        "c.intersects.vacio=false",
        "c.containsRect.si=true",
        "c.containsRect.no=false",
        "c.flatness~=10.0",
        "c.flatnessSq=100.0",
        "c.sub.izq.x2=5.0",
        "c.sub.izq.y2=7.5",
        "c.sub.der.x1=5.0",
        "c.sub.der.y1=7.5",
        "c.sub.izq.ctrlx1=0.0",
        "c.sub.der.ctrlx2=10.0",
        "c.recta.flatnessSq=0.0",
        "c.recta.contains=false",
        "q.bounds.h~=5.0",
        "q.bounds.w=10.0",
        "q.contains.dentro=true",
        "q.contains.fuera=false",
        "q.intersects.si=true",
        "q.intersects.no=false",
        "q.flatness~=10.0",
        "q.flatnessSq=100.0",
        "q.sub.izq.x2=5.0",
        "q.sub.izq.y2=5.0",
        "q.sub.izq.ctrlx=2.5",
        "q.sub.der.ctrly=5.0",
        "q.recta.flatnessSq=0.0",
        "e.centro=true",
        "e.bordeDer=false",
        "e.bordeIzq=false",
        "e.esquina=false",
        "e.fuera=false",
        "e.intersectsCentro=true",
        "e.intersectsEsquina=false",
        "e.containsRect=true",
        "e.vacia.contains=false",
        "e.bounds.w=10.0",
        "rr.centro=true",
        "rr.esquina=false",
        "rr.medioBorde=true",
        "rr.dentroCerca=false",
        "rr.intersects=true",
        "rr.arcW=8.0",
        "a.pie.dentro=true",
        "a.pie.fueraCuadrante=false",
        "a.pie.centro=true",
        "a.tipo=2",
        "a.extent=90.0",
        "a.start.x=10.0",
        "a.start.y=5.0",
        "a.end.x~=5.0",
        "a.end.y~=0",
        "a.open.contains=false",
        "a.chord.centro=false",
        "a.chord.cerca=false",
        "a.bounds.x~=5.0",
        "a.bounds.w~=5.0",
        "p.contains.centro=true",
        "p.contains.borde=true",
        "p.contains.bordeDer=false",
        "p.contains.fuera=false",
        "p.containsRect=true",
        "p.intersects=true",
        "p.windingRule=1",
        "p.current.x=0.0",
        "p.current.y=0.0",
        "p.bounds.w=10.0",
        "p.segmentos=5",
        "p.transcript=0:0.0:0.0 1:10.0:0.0 1:10.0:10.0 1:0.0:10.0 4 ",
        "p.transcriptTx=0:100.0:0.0 1:110.0:0.0 1:110.0:10.0 1:100.0:10.0 4 ",
        "p.evenodd.centro=false",
        "p.evenodd.anillo=true",
        "p.nonzero.centro=true",
        "p.float.transcript=0:0.10000000149011612:0.20000000298023224 1:1.5:2.5 ",
        "f.grueso.segmentos=5",
        "f.fino.segmentos=17",
        "f.grueso.transcript=0:0.0:0.0 1:1.5625:5.625 1:5.0:7.5 1:8.4375:5.625 1:10.0:0.0 ",
        "f.sinCurvas=true",
        "f.limite=4",
        "f.flatness~=0.5",
        "f.limitado.segmentos=5",
        "f.planitudNegativa=IllegalArgumentException",
        "f.quad.segmentos=5",
        "f.quad.transcript=0:0.0:0.0 1:2.5:3.75 1:5.0:5.0 1:7.5:3.75 1:10.0:0.0 ",
        "f.rect.segmentos=5",
        "gp.contains=true",
        "gp.contains.no=false",
        "gp.esPath2DFloat=true",
        "gp.winding=1",
        "gp.windingEvenOdd=0",
        "gp.bounds.w=4.0",
        "gp.copia.contains=true",
        "ts.bounds.w=20.0",
        "ts.contains=true",
        "p.vacio.contains=false",
        "p.vacio.segmentos=0",
        "p.vacio.bounds.w=0.0",
        "pt.dist=5.0",
        "pt.distSq=25.0",
        "pt.equalsFloat=true",
        "pt.hashIgual=true",
        "pt.toString=Point2D.Double[3.0, 4.0]",
        "ptF.x=1.5",
        "ptF.toString=Point2D.Float[1.5, 2.5]",
        "pt.estatica=5.0",
        "pt.cloneIndependiente=3.0",
    };
}
