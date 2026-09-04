package java.text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * La vista de sólo lectura que {@link AttributedString#getIterator()} entrega.
 *
 * <p>No es pública porque el JDK tampoco la expone: el tipo que el llamador ve es la interfaz
 * {@link AttributedCharacterIterator}. Vive en su propio archivo y no como clase anidada para que
 * los dos lados de la pieza —el escribible y el legible— se lean por separado.
 *
 * <p>Los tramos se calculan acá y no se guardan en el {@code AttributedString}: los atributos se
 * almacenan como una lista de "esta clave, este valor, este rango", y el borde de un tramo es el
 * lugar donde el mapa efectivo cambia. Calcularlo al leer es lo que permite que "gana el último"
 * salga gratis y que un borde que no cambia nada no aparezca como tramo.
 */
final class AttributedStringIterator implements AttributedCharacterIterator {

    private final AttributedString fuente;
    private final int begin;
    private final int end;
    private final Set<AttributedCharacterIterator.Attribute> filtro;
    private int pos;

    AttributedStringIterator(AttributedString fuente,
                             AttributedCharacterIterator.Attribute[] atributos,
                             int begin, int end) {
        if (begin < 0 || end > fuente.texto.length() || begin > end) {
            throw new IllegalArgumentException("Invalid substring range");
        }
        this.fuente = fuente;
        this.begin = begin;
        this.end = end;
        this.pos = begin;
        if (atributos == null) {
            this.filtro = null;
        } else {
            // Un arreglo vacío NO es lo mismo que null: pide explícitamente "ningún atributo".
            Set<AttributedCharacterIterator.Attribute> s =
                    new HashSet<AttributedCharacterIterator.Attribute>();
            for (int i = 0; i < atributos.length; i++) {
                s.add(atributos[i]);
            }
            this.filtro = s;
        }
    }

    private AttributedStringIterator(AttributedStringIterator otro) {
        this.fuente = otro.fuente;
        this.begin = otro.begin;
        this.end = otro.end;
        this.filtro = otro.filtro;
        this.pos = otro.pos;
    }

    // ---- CharacterIterator ----

    public char first() {
        return this.setIndex(this.begin);
    }

    public char last() {
        if (this.end == this.begin) {
            this.pos = this.end;
            return CharacterIterator.DONE;
        }
        return this.setIndex(this.end - 1);
    }

    public char current() {
        if (this.pos < this.begin || this.pos >= this.end) {
            return CharacterIterator.DONE;
        }
        return this.fuente.texto.charAt(this.pos);
    }

    public char next() {
        if (this.pos < this.end) {
            this.pos = this.pos + 1;
        }
        return this.current();
    }

    public char previous() {
        if (this.pos > this.begin) {
            this.pos = this.pos - 1;
            return this.current();
        }
        return CharacterIterator.DONE;
    }

    public char setIndex(int position) {
        if (position < this.begin || position > this.end) {
            throw new IllegalArgumentException("Invalid index");
        }
        this.pos = position;
        return this.current();
    }

    public int getBeginIndex() {
        return this.begin;
    }

    public int getEndIndex() {
        return this.end;
    }

    public int getIndex() {
        return this.pos;
    }

    // Copia a mano, como el resto de la casa: un iterador nuevo con la misma ventana y el mismo
    // cursor es exactamente lo mismo, y no depende del nativo Object.clone().
    public Object clone() {
        return new AttributedStringIterator(this);
    }

    // ---- AttributedCharacterIterator ----

    public int getRunStart() {
        return this.comienzo(this.pos, null);
    }

    public int getRunStart(AttributedCharacterIterator.Attribute attribute) {
        return this.comienzo(this.pos, unitario(attribute));
    }

    public int getRunStart(Set<? extends AttributedCharacterIterator.Attribute> attributes) {
        return this.comienzo(this.pos, copia(attributes));
    }

    public int getRunLimit() {
        return this.limite(this.pos, null);
    }

    public int getRunLimit(AttributedCharacterIterator.Attribute attribute) {
        return this.limite(this.pos, unitario(attribute));
    }

    public int getRunLimit(Set<? extends AttributedCharacterIterator.Attribute> attributes) {
        return this.limite(this.pos, copia(attributes));
    }

    public Map<AttributedCharacterIterator.Attribute, Object> getAttributes() {
        return this.mapaEn(this.pos);
    }

    public Object getAttribute(AttributedCharacterIterator.Attribute attribute) {
        return this.valorEn(this.pos, attribute);
    }

    public Set<AttributedCharacterIterator.Attribute> getAllAttributeKeys() {
        Set<AttributedCharacterIterator.Attribute> s =
                new HashSet<AttributedCharacterIterator.Attribute>();
        for (int i = 0; i < this.fuente.cantidad; i++) {
            // Un tramo que no toca la ventana no aporta clave: el iterador no puede devolver su
            // valor en ninguna posición, así que anunciarlo sería anunciar algo inalcanzable.
            if (this.fuente.hasta[i] > this.begin && this.fuente.desde[i] < this.end) {
                AttributedCharacterIterator.Attribute k = this.fuente.claves[i];
                if (this.filtro == null || this.filtro.contains(k)) {
                    s.add(k);
                }
            }
        }
        return s;
    }

    // ---- interno ----

    private static Set<AttributedCharacterIterator.Attribute> unitario(
            AttributedCharacterIterator.Attribute a) {
        Set<AttributedCharacterIterator.Attribute> s =
                new HashSet<AttributedCharacterIterator.Attribute>();
        s.add(a);
        return s;
    }

    private static Set<AttributedCharacterIterator.Attribute> copia(
            Set<? extends AttributedCharacterIterator.Attribute> in) {
        Set<AttributedCharacterIterator.Attribute> s =
                new HashSet<AttributedCharacterIterator.Attribute>();
        if (in != null) {
            for (AttributedCharacterIterator.Attribute a : in) {
                s.add(a);
            }
        }
        return s;
    }

    private Map<AttributedCharacterIterator.Attribute, Object> mapaEn(int idx) {
        Map<AttributedCharacterIterator.Attribute, Object> m =
                new HashMap<AttributedCharacterIterator.Attribute, Object>();
        if (idx < this.begin || idx >= this.end) {
            return m;
        }
        // En orden de inserción: el último que cubre la posición pisa a los anteriores. Esa es la
        // regla completa de resolución de conflictos, y por eso no hace falta guardar tramos
        // partidos en el AttributedString.
        for (int i = 0; i < this.fuente.cantidad; i++) {
            if (this.fuente.desde[i] <= idx && idx < this.fuente.hasta[i]) {
                AttributedCharacterIterator.Attribute k = this.fuente.claves[i];
                if (this.filtro == null || this.filtro.contains(k)) {
                    m.put(k, this.fuente.valores[i]);
                }
            }
        }
        return m;
    }

    private Object valorEn(int idx, AttributedCharacterIterator.Attribute clave) {
        if (idx < this.begin || idx >= this.end) {
            return null;
        }
        if (this.filtro != null && !this.filtro.contains(clave)) {
            return null;
        }
        Object v = null;
        for (int i = 0; i < this.fuente.cantidad; i++) {
            if (this.fuente.claves[i] == clave
                    && this.fuente.desde[i] <= idx && idx < this.fuente.hasta[i]) {
                v = this.fuente.valores[i];
            }
        }
        return v;
    }

    // Los bordes posibles de un tramo son exactamente los extremos de los rangos declarados, más
    // los de la ventana. Entre dos bordes consecutivos nada puede cambiar, así que alcanza con
    // mirar los bordes en lugar de recorrer carácter por carácter.
    private int[] bordes() {
        int n = this.fuente.cantidad * 2 + 2;
        int[] b = new int[n];
        int k = 0;
        b[k] = this.begin;
        k = k + 1;
        b[k] = this.end;
        k = k + 1;
        for (int i = 0; i < this.fuente.cantidad; i++) {
            b[k] = this.recortar(this.fuente.desde[i]);
            k = k + 1;
            b[k] = this.recortar(this.fuente.hasta[i]);
            k = k + 1;
        }
        // Inserción: n es chico (dos por atributo agregado) y el orden tiene que ser estable y
        // sin duplicados para que los recorridos de comienzo/limite lean cada borde una vez.
        for (int i = 1; i < k; i++) {
            int v = b[i];
            int j = i - 1;
            while (j >= 0 && b[j] > v) {
                b[j + 1] = b[j];
                j = j - 1;
            }
            b[j + 1] = v;
        }
        int m = 0;
        for (int i = 0; i < k; i++) {
            if (i == 0 || b[i] != b[i - 1]) {
                b[m] = b[i];
                m = m + 1;
            }
        }
        int[] out = new int[m];
        for (int i = 0; i < m; i++) {
            out[i] = b[i];
        }
        return out;
    }

    private int recortar(int v) {
        if (v < this.begin) {
            return this.begin;
        }
        if (v > this.end) {
            return this.end;
        }
        return v;
    }

    private int limite(int idx, Set<AttributedCharacterIterator.Attribute> considerados) {
        if (idx >= this.end) {
            return this.end;
        }
        int[] b = this.bordes();
        for (int i = 0; i < b.length; i++) {
            if (b[i] > idx && !this.mismos(b[i], idx, considerados)) {
                return b[i];
            }
        }
        return this.end;
    }

    private int comienzo(int idx, Set<AttributedCharacterIterator.Attribute> considerados) {
        if (idx >= this.end) {
            return this.end;
        }
        int[] b = this.bordes();
        int r = -1;
        for (int i = 0; i < b.length; i++) {
            if (b[i] > idx) {
                break;
            }
            if (this.mismos(b[i], idx, considerados)) {
                if (r < 0) {
                    r = b[i];
                }
            } else {
                r = -1;
            }
        }
        if (r < 0) {
            return this.begin;
        }
        return r;
    }

    private boolean mismos(int a, int c, Set<AttributedCharacterIterator.Attribute> considerados) {
        if (considerados == null) {
            Map<AttributedCharacterIterator.Attribute, Object> ma = this.mapaEn(a);
            Map<AttributedCharacterIterator.Attribute, Object> mc = this.mapaEn(c);
            if (ma.size() != mc.size()) {
                return false;
            }
            for (Map.Entry<AttributedCharacterIterator.Attribute, Object> e : ma.entrySet()) {
                if (!mc.containsKey(e.getKey())) {
                    return false;
                }
                if (!coinciden(e.getValue(), mc.get(e.getKey()))) {
                    return false;
                }
            }
            return true;
        }
        for (AttributedCharacterIterator.Attribute k : considerados) {
            if (!coinciden(this.valorEn(a, k), this.valorEn(c, k))) {
                return false;
            }
        }
        return true;
    }

    private static boolean coinciden(Object a, Object b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
