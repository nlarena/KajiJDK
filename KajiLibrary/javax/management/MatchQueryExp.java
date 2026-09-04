package javax.management;

/**
 * "El atributo, que es una cadena, coincide con este patron."
 *
 * <p>De paquete: se fabrica con {@link Query#match} y con los tres atajos de subcadena.
 *
 * <p>Su lenguaje de patrones <b>no</b> es el de {@link ObjectName}. Ademas de {@code *} y {@code ?}
 * tiene <b>clases de caracteres</b> entre corchetes, con rangos ({@code [a-z]}) y negacion
 * ({@code [!abc]}), y una barra invertida que escapa el caracter siguiente. Es mas parecido a un
 * glob de shell que a un comodin de JMX, y confundirlos es un error facil.
 */
class MatchQueryExp extends QueryEval implements QueryExp {

    private static final long serialVersionUID = -7156603696948215014L;

    /**
     * @serial el atributo
     */
    private AttributeValueExp exp;

    /**
     * @serial el patron
     */
    private String pattern;

    public MatchQueryExp() {
    }

    public MatchQueryExp(AttributeValueExp a, StringValueExp s) {
        exp = a;
        pattern = s.getValue();
    }

    public AttributeValueExp getAttribute() {
        return exp;
    }

    public String getPattern() {
        return pattern;
    }

    public boolean apply(ObjectName name) throws BadStringOperationException,
            BadBinaryOpValueExpException, BadAttributeValueExpException,
            InvalidApplicationException {
        ValueExp val = exp.apply(name);
        if (!(val instanceof StringValueExp)) {
            return false;
        }
        String s = ((StringValueExp) val).getValue();
        if (s == null) {
            return false;
        }
        return coincide(pattern, s);
    }

    public String toString() {
        return exp + " like " + new StringValueExp(pattern);
    }

    /**
     * Coincidencia estilo glob, sin recursion.
     *
     * <p>Se resuelve con retroceso sobre la ultima {@code *} vista, igual que el comodin de
     * {@link ObjectName}, para que un patron hostil no pueda desbordar la pila.
     */
    private static boolean coincide(String pat, String texto) {
        int p = 0;
        int t = 0;
        int estrella = -1;
        int marca = 0;
        while (t < texto.length()) {
            boolean avanza = false;
            if (p < pat.length()) {
                char c = pat.charAt(p);
                if (c == '\\') {
                    if (p + 1 < pat.length() && pat.charAt(p + 1) == texto.charAt(t)) {
                        p += 2;
                        t++;
                        avanza = true;
                    }
                } else if (c == '[') {
                    int fin = cierre(pat, p);
                    if (fin > 0 && enClase(pat, p, fin, texto.charAt(t))) {
                        p = fin + 1;
                        t++;
                        avanza = true;
                    }
                } else if (c == '?' || c == texto.charAt(t)) {
                    p++;
                    t++;
                    avanza = true;
                }
            }
            if (avanza) {
                continue;
            }
            if (p < pat.length() && pat.charAt(p) == '*') {
                estrella = p;
                marca = t;
                p++;
                continue;
            }
            if (estrella >= 0) {
                p = estrella + 1;
                marca++;
                t = marca;
                continue;
            }
            return false;
        }
        while (p < pat.length() && pat.charAt(p) == '*') {
            p++;
        }
        return p == pat.length();
    }

    /** Indice del {@code ]} que cierra la clase que abre en `inicio`, o -1. */
    private static int cierre(String pat, int inicio) {
        int i = inicio + 1;
        if (i < pat.length() && pat.charAt(i) == '!') {
            i++;
        }
        if (i < pat.length() && pat.charAt(i) == ']') {
            i++;
        }
        while (i < pat.length()) {
            if (pat.charAt(i) == ']') {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static boolean enClase(String pat, int inicio, int fin, char c) {
        int i = inicio + 1;
        boolean negada = false;
        if (i < fin && pat.charAt(i) == '!') {
            negada = true;
            i++;
        }
        boolean hay = false;
        while (i < fin) {
            char a = pat.charAt(i);
            if (i + 2 < fin && pat.charAt(i + 1) == '-') {
                char b = pat.charAt(i + 2);
                if (a <= c && c <= b) {
                    hay = true;
                }
                i += 3;
            } else {
                if (a == c) {
                    hay = true;
                }
                i++;
            }
        }
        return negada ? !hay : hay;
    }
}
