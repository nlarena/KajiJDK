package java.text;

import java.math.BigDecimal;

/**
 * Evalúa las reglas de plural del CLDR, que son las que deciden si un número lleva "1 archivo" o
 * "2 archivos".
 *
 * <p>La usa {@link CompactNumberFormat} cuando un patrón compacto trae variantes por categoría. No
 * es pública: en el JDK esta lógica también es interna, y exponerla obligaría a decidir una API que
 * el estándar no define.
 *
 * <p><b>Por qué hace falta un evaluador y no una tabla.</b> Las categorías no se deducen del
 * número: el ruso pone "few" del 2 al 4 salvo del 12 al 14, el polaco distingue por el resto de la
 * decena Y de la centena, y el francés cuenta el 0 como singular. La regla es una expresión, y el
 * CLDR la publica como texto — así que lo honesto es evaluarla, no adivinar tres casos.
 *
 * <p>Sintaxis soportada, que es la del estándar:
 * {@code categoría ':' condición} separadas por {@code ';'}, con
 * {@code condición = and ('or' and)*}, {@code and = relación ('and' relación)*},
 * {@code relación = operando ['%' n] ('='|'!=') rango (',' rango)*} y
 * {@code rango = n | n '..' m}. Los operandos son los seis del CLDR: {@code n} (valor absoluto),
 * {@code i} (parte entera), {@code v} y {@code w} (cantidad de decimales con y sin ceros finales),
 * {@code f} y {@code t} (los decimales como entero, con y sin ceros finales). Las muestras
 * ({@code @integer}, {@code @decimal}) se ignoran, que es lo que corresponde: son documentación.
 */
final class ReglasDePlural {

    private ReglasDePlural() {
    }

    /**
     * La categoría que corresponde al valor, o {@code "other"} si ninguna regla da.
     *
     * <p>{@code "other"} no es un valor de relleno: el CLDR garantiza que todo locale la tiene y
     * que es la que se aplica cuando no hay regla más específica.
     */
    static String categoria(String reglas, BigDecimal valor) {
        if (reglas == null || reglas.length() == 0) {
            return "other";
        }
        Operandos op = new Operandos(valor);
        int i = 0;
        while (i < reglas.length()) {
            int fin = reglas.indexOf(';', i);
            if (fin < 0) {
                fin = reglas.length();
            }
            String regla = reglas.substring(i, fin);
            int dosPuntos = regla.indexOf(':');
            if (dosPuntos > 0) {
                String cat = regla.substring(0, dosPuntos).trim();
                String cond = regla.substring(dosPuntos + 1, regla.length());
                int muestra = cond.indexOf('@');
                if (muestra >= 0) {
                    cond = cond.substring(0, muestra);
                }
                if (ReglasDePlural.condicion(cond.trim(), op)) {
                    return cat;
                }
            }
            i = fin + 1;
        }
        return "other";
    }

    private static boolean condicion(String s, Operandos op) {
        if (s.length() == 0) {
            return true;
        }
        String[] partes = ReglasDePlural.separar(s, " or ");
        for (int i = 0; i < partes.length; i = i + 1) {
            if (ReglasDePlural.conjuncion(partes[i], op)) {
                return true;
            }
        }
        return false;
    }

    private static boolean conjuncion(String s, Operandos op) {
        String[] partes = ReglasDePlural.separar(s, " and ");
        for (int i = 0; i < partes.length; i = i + 1) {
            if (!ReglasDePlural.relacion(partes[i].trim(), op)) {
                return false;
            }
        }
        return true;
    }

    private static boolean relacion(String s, Operandos op) {
        boolean negada = false;
        int corte = s.indexOf("!=");
        int largoOp = 2;
        if (corte >= 0) {
            negada = true;
        } else {
            corte = s.indexOf('=');
            largoOp = 1;
            if (corte < 0) {
                return false;
            }
        }
        String izq = s.substring(0, corte).trim();
        String der = s.substring(corte + largoOp, s.length()).trim();

        long valor;
        int mod = izq.indexOf('%');
        if (mod >= 0) {
            long base = op.valor(izq.substring(0, mod).trim());
            long m = ReglasDePlural.entero(izq.substring(mod + 1, izq.length()).trim());
            if (m == 0) {
                return false;
            }
            valor = base % m;
        } else {
            valor = op.valor(izq);
        }

        boolean dentro = false;
        String[] rangos = ReglasDePlural.separar(der, ",");
        for (int i = 0; i < rangos.length; i = i + 1) {
            String r = rangos[i].trim();
            int puntos = r.indexOf("..");
            if (puntos >= 0) {
                long a = ReglasDePlural.entero(r.substring(0, puntos).trim());
                long b = ReglasDePlural.entero(r.substring(puntos + 2, r.length()).trim());
                if (valor >= a && valor <= b) {
                    dentro = true;
                }
            } else if (valor == ReglasDePlural.entero(r)) {
                dentro = true;
            }
        }
        if (negada) {
            return !dentro;
        }
        return dentro;
    }

    private static long entero(String s) {
        if (s.length() == 0) {
            return Long.MIN_VALUE;
        }
        long v = 0;
        for (int i = 0; i < s.length(); i = i + 1) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return Long.MIN_VALUE;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }

    // Separación literal por un delimitador; no hay anidamiento en esta gramática, así que no hace
    // falta un tokenizador.
    private static String[] separar(String s, String delim) {
        int n = 1;
        int i = s.indexOf(delim);
        while (i >= 0) {
            n = n + 1;
            i = s.indexOf(delim, i + delim.length());
        }
        String[] out = new String[n];
        int k = 0;
        int desde = 0;
        i = s.indexOf(delim);
        while (i >= 0) {
            out[k] = s.substring(desde, i);
            k = k + 1;
            desde = i + delim.length();
            i = s.indexOf(delim, desde);
        }
        out[k] = s.substring(desde, s.length());
        return out;
    }

    /** Los seis operandos que el CLDR define sobre un número, calculados una sola vez. */
    private static final class Operandos {

        private final long n;
        private final long i;
        private final long v;
        private final long w;
        private final long f;
        private final long t;

        Operandos(BigDecimal valor) {
            BigDecimal abs = valor.abs();
            this.i = abs.setScale(0, java.math.RoundingMode.DOWN).longValue();
            this.n = this.i;
            int escala = abs.scale();
            if (escala < 0) {
                escala = 0;
            }
            this.v = escala;
            BigDecimal fraccion = abs.subtract(new BigDecimal(this.i));
            this.f = fraccion.movePointRight(escala).setScale(0, java.math.RoundingMode.DOWN)
                    .longValue();
            BigDecimal sinCeros = fraccion.stripTrailingZeros();
            int escala2 = sinCeros.scale();
            if (escala2 < 0) {
                escala2 = 0;
            }
            this.w = escala2;
            this.t = sinCeros.movePointRight(escala2).setScale(0, java.math.RoundingMode.DOWN)
                    .longValue();
        }

        long valor(String operando) {
            if (operando.equals("n")) {
                return this.n;
            }
            if (operando.equals("i")) {
                return this.i;
            }
            if (operando.equals("v")) {
                return this.v;
            }
            if (operando.equals("w")) {
                return this.w;
            }
            if (operando.equals("f")) {
                return this.f;
            }
            if (operando.equals("t")) {
                return this.t;
            }
            return Long.MIN_VALUE;
        }
    }
}
