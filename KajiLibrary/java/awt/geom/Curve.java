package java.awt.geom;

// Helper interno (no es API): cuenta cruces de un camino contra un punto o contra un rectangulo.
// Es el motor de `contains`/`intersects` de Path2D, CubicCurve2D, QuadCurve2D, Line2D y Area.
//
// Hay dos cuentas distintas y conviene no mezclarlas:
//
//   * **Cruces de punto** (`pointCrossingsFor*`): se tira un rayo desde (px,py) hacia -X y se suma
//     +1 por cada segmento que lo cruza hacia abajo y -1 hacia arriba. Con WIND_NON_ZERO el punto
//     esta dentro si la suma no es cero; con WIND_EVEN_ODD, si es impar. La asimetria de los
//     bordes (`>=` de un lado, `<` del otro) es lo que hace que un vertice compartido por dos
//     segmentos se cuente una sola vez -- si se usara `<=` en los dos, un punto a la altura exacta
//     de un vertice contaria doble y daria "afuera" donde debe dar "adentro".
//
//   * **Cruces de rectangulo** (`rectCrossingsFor*`): igual pero contra los cuatro bordes, con un
//     valor centinela RECT_INTERSECTS que corta la cuenta ni bien se sabe que el borde del camino
//     entra al rectangulo. Ese centinela es lo que distingue `intersects` (borde tocado O interior
//     no vacio) de `contains` (interior no vacio Y borde intacto): con un solo numero no alcanza.
//
// Las curvas se resuelven subdividiendo en el punto medio hasta que el trozo es indistinguible de
// un segmento. El limite de 52 niveles es la mantisa de un double: mas subdivisiones no cambian
// nada porque los puntos medios ya no se mueven.
class Curve {

    /** El borde del camino entra al rectangulo: la cuenta de cruces ya no significa nada. */
    static final int RECT_INTERSECTS = 0x80000000;

    private Curve() {
    }

    // --- cruces contra un punto ------------------------------------------------------------------

    static int pointCrossingsForPath(PathIterator pi, double px, double py) {
        if (pi.isDone()) {
            return 0;
        }
        double[] coords = new double[6];
        if (pi.currentSegment(coords) != PathIterator.SEG_MOVETO) {
            throw new IllegalPathStateException("missing initial moveto "
                    + "in path definition");
        }
        pi.next();
        double movx = coords[0];
        double movy = coords[1];
        double curx = movx;
        double cury = movy;
        double endx;
        double endy;
        int crossings = 0;
        while (!pi.isDone()) {
            int seg = pi.currentSegment(coords);
            if (seg == PathIterator.SEG_MOVETO) {
                if (cury != movy) {
                    crossings = crossings
                            + pointCrossingsForLine(px, py, curx, cury, movx, movy);
                }
                movx = coords[0];
                curx = coords[0];
                movy = coords[1];
                cury = coords[1];
            } else if (seg == PathIterator.SEG_LINETO) {
                endx = coords[0];
                endy = coords[1];
                crossings = crossings + pointCrossingsForLine(px, py, curx, cury, endx, endy);
                curx = endx;
                cury = endy;
            } else if (seg == PathIterator.SEG_QUADTO) {
                endx = coords[2];
                endy = coords[3];
                crossings = crossings + pointCrossingsForQuad(px, py, curx, cury,
                        coords[0], coords[1], endx, endy, 0);
                curx = endx;
                cury = endy;
            } else if (seg == PathIterator.SEG_CUBICTO) {
                endx = coords[4];
                endy = coords[5];
                crossings = crossings + pointCrossingsForCubic(px, py, curx, cury,
                        coords[0], coords[1], coords[2], coords[3], endx, endy, 0);
                curx = endx;
                cury = endy;
            } else {
                // SEG_CLOSE
                if (cury != movy) {
                    crossings = crossings
                            + pointCrossingsForLine(px, py, curx, cury, movx, movy);
                }
                curx = movx;
                cury = movy;
            }
            pi.next();
        }
        // Un subcamino sin CLOSE se cierra igual a los efectos de "dentro/fuera".
        if (cury != movy) {
            crossings = crossings + pointCrossingsForLine(px, py, curx, cury, movx, movy);
        }
        return crossings;
    }

    static int pointCrossingsForLine(double px, double py,
                                     double x0, double y0,
                                     double x1, double y1) {
        if (py < y0 && py < y1) {
            return 0;
        }
        if (py >= y0 && py >= y1) {
            return 0;
        }
        // py esta estrictamente entre y0 e y1, asi que y0 != y1 y no hay division por cero.
        if (px >= x0 && px >= x1) {
            return 0;
        }
        if (px < x0 && px < x1) {
            if (y0 < y1) {
                return 1;
            }
            return -1;
        }
        double xintercept = x0 + (py - y0) * (x1 - x0) / (y1 - y0);
        if (px >= xintercept) {
            return 0;
        }
        if (y0 < y1) {
            return 1;
        }
        return -1;
    }

    static int pointCrossingsForQuad(double px, double py,
                                     double x0, double y0,
                                     double xc, double yc,
                                     double x1, double y1, int level) {
        if (py < y0 && py < yc && py < y1) {
            return 0;
        }
        if (py >= y0 && py >= yc && py >= y1) {
            return 0;
        }
        if (px >= x0 && px >= xc && px >= x1) {
            return 0;
        }
        if (px < x0 && px < xc && px < x1) {
            // La curva entera esta a la derecha del rayo: solo importa si cruza la altura py.
            if (py >= y0) {
                if (py < y1) {
                    return 1;
                }
            } else {
                if (py >= y1) {
                    return -1;
                }
            }
            return 0;
        }
        if (level > 52) {
            return pointCrossingsForLine(px, py, x0, y0, x1, y1);
        }
        double x0c = (x0 + xc) / 2.0;
        double y0c = (y0 + yc) / 2.0;
        double xc1 = (xc + x1) / 2.0;
        double yc1 = (yc + y1) / 2.0;
        double xm = (x0c + xc1) / 2.0;
        double ym = (y0c + yc1) / 2.0;
        if (Double.isNaN(xm) || Double.isNaN(ym)) {
            // Un punto de control infinito o NaN no define nada: no se cuenta ningun cruce.
            return 0;
        }
        return pointCrossingsForQuad(px, py, x0, y0, x0c, y0c, xm, ym, level + 1)
                + pointCrossingsForQuad(px, py, xm, ym, xc1, yc1, x1, y1, level + 1);
    }

    static int pointCrossingsForCubic(double px, double py,
                                      double x0, double y0,
                                      double xc0, double yc0,
                                      double xc1, double yc1,
                                      double x1, double y1, int level) {
        if (py < y0 && py < yc0 && py < yc1 && py < y1) {
            return 0;
        }
        if (py >= y0 && py >= yc0 && py >= yc1 && py >= y1) {
            return 0;
        }
        if (px >= x0 && px >= xc0 && px >= xc1 && px >= x1) {
            return 0;
        }
        if (px < x0 && px < xc0 && px < xc1 && px < x1) {
            if (py >= y0) {
                if (py < y1) {
                    return 1;
                }
            } else {
                if (py >= y1) {
                    return -1;
                }
            }
            return 0;
        }
        if (level > 52) {
            return pointCrossingsForLine(px, py, x0, y0, x1, y1);
        }
        double xmid = (xc0 + xc1) / 2.0;
        double ymid = (yc0 + yc1) / 2.0;
        double xc0a = (x0 + xc0) / 2.0;
        double yc0a = (y0 + yc0) / 2.0;
        double xc1a = (xc1 + x1) / 2.0;
        double yc1a = (yc1 + y1) / 2.0;
        double xc0m = (xc0a + xmid) / 2.0;
        double yc0m = (yc0a + ymid) / 2.0;
        double xmc1 = (xmid + xc1a) / 2.0;
        double ymc1 = (ymid + yc1a) / 2.0;
        double xm = (xc0m + xmc1) / 2.0;
        double ym = (yc0m + ymc1) / 2.0;
        if (Double.isNaN(xm) || Double.isNaN(ym)) {
            return 0;
        }
        return pointCrossingsForCubic(px, py, x0, y0, xc0a, yc0a, xc0m, yc0m, xm, ym, level + 1)
                + pointCrossingsForCubic(px, py, xm, ym, xmc1, ymc1, xc1a, yc1a, x1, y1, level + 1);
    }

    // --- cruces contra un rectangulo -------------------------------------------------------------

    static int rectCrossingsForPath(PathIterator pi,
                                    double rxmin, double rymin,
                                    double rxmax, double rymax) {
        if (rxmax <= rxmin || rymax <= rymin) {
            return 0;
        }
        if (pi.isDone()) {
            return 0;
        }
        double[] coords = new double[6];
        if (pi.currentSegment(coords) != PathIterator.SEG_MOVETO) {
            throw new IllegalPathStateException("missing initial moveto "
                    + "in path definition");
        }
        pi.next();
        double movx = coords[0];
        double movy = coords[1];
        double curx = movx;
        double cury = movy;
        double endx;
        double endy;
        int crossings = 0;
        while (crossings != RECT_INTERSECTS && !pi.isDone()) {
            int seg = pi.currentSegment(coords);
            if (seg == PathIterator.SEG_MOVETO) {
                if (curx != movx || cury != movy) {
                    crossings = rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax,
                            curx, cury, movx, movy);
                }
                movx = coords[0];
                curx = coords[0];
                movy = coords[1];
                cury = coords[1];
            } else if (seg == PathIterator.SEG_LINETO) {
                endx = coords[0];
                endy = coords[1];
                crossings = rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax,
                        curx, cury, endx, endy);
                curx = endx;
                cury = endy;
            } else if (seg == PathIterator.SEG_QUADTO) {
                endx = coords[2];
                endy = coords[3];
                crossings = rectCrossingsForQuad(crossings, rxmin, rymin, rxmax, rymax,
                        curx, cury, coords[0], coords[1], endx, endy, 0);
                curx = endx;
                cury = endy;
            } else if (seg == PathIterator.SEG_CUBICTO) {
                endx = coords[4];
                endy = coords[5];
                crossings = rectCrossingsForCubic(crossings, rxmin, rymin, rxmax, rymax,
                        curx, cury, coords[0], coords[1], coords[2], coords[3], endx, endy, 0);
                curx = endx;
                cury = endy;
            } else {
                // SEG_CLOSE
                if (curx != movx || cury != movy) {
                    crossings = rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax,
                            curx, cury, movx, movy);
                }
                curx = movx;
                cury = movy;
            }
            pi.next();
        }
        if (crossings != RECT_INTERSECTS && (curx != movx || cury != movy)) {
            crossings = rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax,
                    curx, cury, movx, movy);
        }
        return crossings;
    }

    static int rectCrossingsForLine(int crossings,
                                    double rxmin, double rymin,
                                    double rxmax, double rymax,
                                    double x0, double y0,
                                    double x1, double y1) {
        if (y0 >= rymax && y1 >= rymax) {
            return crossings;
        }
        if (y0 <= rymin && y1 <= rymin) {
            return crossings;
        }
        if (x0 <= rxmin && x1 <= rxmin) {
            return crossings;
        }
        if (x0 >= rxmax && x1 >= rxmax) {
            // Enteramente a la derecha: cuenta como cruce del rayo horizontal, sin tocar el rect.
            if (y0 < y1) {
                if (y0 <= rymin) {
                    crossings = crossings + 1;
                }
                if (y1 >= rymax) {
                    crossings = crossings + 1;
                }
            } else {
                if (y1 <= rymin) {
                    crossings = crossings - 1;
                }
                if (y0 >= rymax) {
                    crossings = crossings - 1;
                }
            }
            return crossings;
        }
        // Un extremo estrictamente adentro ya alcanza para saber que el borde entra.
        if ((x0 > rxmin && x0 < rxmax && y0 > rymin && y0 < rymax)
                || (x1 > rxmin && x1 < rxmax && y1 > rymin && y1 < rymax)) {
            return RECT_INTERSECTS;
        }
        // Se recorta el segmento contra las alturas del rectangulo y se mira donde queda en X.
        double xi0 = x0;
        if (y0 < rymin) {
            xi0 = xi0 + ((rymin - y0) * (x1 - x0) / (y1 - y0));
        } else if (y0 > rymax) {
            xi0 = xi0 + ((rymax - y0) * (x1 - x0) / (y1 - y0));
        }
        double xi1 = x1;
        if (y1 < rymin) {
            xi1 = xi1 + ((rymin - y1) * (x0 - x1) / (y0 - y1));
        } else if (y1 > rymax) {
            xi1 = xi1 + ((rymax - y1) * (x0 - x1) / (y0 - y1));
        }
        if (xi0 <= rxmin && xi1 <= rxmin) {
            return crossings;
        }
        if (xi0 >= rxmax && xi1 >= rxmax) {
            if (y0 < y1) {
                if (y0 <= rymin) {
                    crossings = crossings + 1;
                }
                if (y1 >= rymax) {
                    crossings = crossings + 1;
                }
            } else {
                if (y1 <= rymin) {
                    crossings = crossings - 1;
                }
                if (y0 >= rymax) {
                    crossings = crossings - 1;
                }
            }
            return crossings;
        }
        return RECT_INTERSECTS;
    }

    static int rectCrossingsForQuad(int crossings,
                                    double rxmin, double rymin,
                                    double rxmax, double rymax,
                                    double x0, double y0,
                                    double xc, double yc,
                                    double x1, double y1, int level) {
        if (y0 >= rymax && yc >= rymax && y1 >= rymax) {
            return crossings;
        }
        if (y0 <= rymin && yc <= rymin && y1 <= rymin) {
            return crossings;
        }
        if (x0 <= rxmin && xc <= rxmin && x1 <= rxmin) {
            return crossings;
        }
        if (x0 >= rxmax && xc >= rxmax && x1 >= rxmax) {
            // La curva entera esta a la derecha: los cruces los decide solo el trayecto vertical.
            if (y0 < y1) {
                if (y0 <= rymin && y1 > rymin) {
                    crossings = crossings + 1;
                }
                if (y0 < rymax && y1 >= rymax) {
                    crossings = crossings + 1;
                }
            } else {
                if (y1 <= rymin && y0 > rymin) {
                    crossings = crossings - 1;
                }
                if (y1 < rymax && y0 >= rymax) {
                    crossings = crossings - 1;
                }
            }
            return crossings;
        }
        if ((x0 < rxmax && x0 > rxmin && y0 < rymax && y0 > rymin)
                || (x1 < rxmax && x1 > rxmin && y1 < rymax && y1 > rymin)) {
            return RECT_INTERSECTS;
        }
        if (level > 52) {
            return rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax, x0, y0, x1, y1);
        }
        double x0c = (x0 + xc) / 2.0;
        double y0c = (y0 + yc) / 2.0;
        double xc1 = (xc + x1) / 2.0;
        double yc1 = (yc + y1) / 2.0;
        double xm = (x0c + xc1) / 2.0;
        double ym = (y0c + yc1) / 2.0;
        if (Double.isNaN(xm) || Double.isNaN(ym)) {
            return 0;
        }
        crossings = rectCrossingsForQuad(crossings, rxmin, rymin, rxmax, rymax,
                x0, y0, x0c, y0c, xm, ym, level + 1);
        if (crossings != RECT_INTERSECTS) {
            crossings = rectCrossingsForQuad(crossings, rxmin, rymin, rxmax, rymax,
                    xm, ym, xc1, yc1, x1, y1, level + 1);
        }
        return crossings;
    }

    static int rectCrossingsForCubic(int crossings,
                                     double rxmin, double rymin,
                                     double rxmax, double rymax,
                                     double x0, double y0,
                                     double xc0, double yc0,
                                     double xc1, double yc1,
                                     double x1, double y1, int level) {
        if (y0 >= rymax && yc0 >= rymax && yc1 >= rymax && y1 >= rymax) {
            return crossings;
        }
        if (y0 <= rymin && yc0 <= rymin && yc1 <= rymin && y1 <= rymin) {
            return crossings;
        }
        if (x0 <= rxmin && xc0 <= rxmin && xc1 <= rxmin && x1 <= rxmin) {
            return crossings;
        }
        if (x0 >= rxmax && xc0 >= rxmax && xc1 >= rxmax && x1 >= rxmax) {
            if (y0 < y1) {
                if (y0 <= rymin && y1 > rymin) {
                    crossings = crossings + 1;
                }
                if (y0 < rymax && y1 >= rymax) {
                    crossings = crossings + 1;
                }
            } else {
                if (y1 <= rymin && y0 > rymin) {
                    crossings = crossings - 1;
                }
                if (y1 < rymax && y0 >= rymax) {
                    crossings = crossings - 1;
                }
            }
            return crossings;
        }
        if ((x0 > rxmin && x0 < rxmax && y0 > rymin && y0 < rymax)
                || (x1 > rxmin && x1 < rxmax && y1 > rymin && y1 < rymax)) {
            return RECT_INTERSECTS;
        }
        if (level > 52) {
            return rectCrossingsForLine(crossings, rxmin, rymin, rxmax, rymax, x0, y0, x1, y1);
        }
        double xmid = (xc0 + xc1) / 2.0;
        double ymid = (yc0 + yc1) / 2.0;
        double xc0a = (x0 + xc0) / 2.0;
        double yc0a = (y0 + yc0) / 2.0;
        double xc1a = (xc1 + x1) / 2.0;
        double yc1a = (yc1 + y1) / 2.0;
        double xc0m = (xc0a + xmid) / 2.0;
        double yc0m = (yc0a + ymid) / 2.0;
        double xmc1 = (xmid + xc1a) / 2.0;
        double ymc1 = (ymid + yc1a) / 2.0;
        double xm = (xc0m + xmc1) / 2.0;
        double ym = (yc0m + ymc1) / 2.0;
        if (Double.isNaN(xm) || Double.isNaN(ym)) {
            return 0;
        }
        crossings = rectCrossingsForCubic(crossings, rxmin, rymin, rxmax, rymax,
                x0, y0, xc0a, yc0a, xc0m, yc0m, xm, ym, level + 1);
        if (crossings != RECT_INTERSECTS) {
            crossings = rectCrossingsForCubic(crossings, rxmin, rymin, rxmax, rymax,
                    xm, ym, xmc1, ymc1, xc1a, yc1a, x1, y1, level + 1);
        }
        return crossings;
    }
}
