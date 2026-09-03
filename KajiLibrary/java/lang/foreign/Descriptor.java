package java.lang.foreign;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// La implementacion de `FunctionDescriptor`. De paquete: se llega por `FunctionDescriptor.of`.
//
// Inmutable, como los layouts y por la misma razon: un descriptor se comparte, y un `append` que
// mutara le cambiaria la firma a todo el que lo tenga.
final class Descriptor implements FunctionDescriptor {

    private final MemoryLayout retorno;
    private final List<MemoryLayout> argumentos;

    private Descriptor(MemoryLayout retorno, List<MemoryLayout> argumentos) {
        this.retorno = retorno;
        this.argumentos = argumentos;
    }

    static FunctionDescriptor crear(MemoryLayout retorno, MemoryLayout[] argumentos) {
        return new Descriptor(retorno, enLista(argumentos));
    }

    private static List<MemoryLayout> enLista(MemoryLayout[] ls) {
        if (ls == null) {
            throw new IllegalArgumentException("los argumentos no pueden ser null");
        }
        List<MemoryLayout> out = new ArrayList<MemoryLayout>();
        int i = 0;
        while (i < ls.length) {
            if (ls[i] == null) {
                throw new IllegalArgumentException("un argumento es null");
            }
            out.add(ls[i]);
            i = i + 1;
        }
        return out;
    }

    public Optional<MemoryLayout> returnLayout() {
        return Optional.ofNullable(this.retorno);
    }

    public List<MemoryLayout> argumentLayouts() {
        return Collections.unmodifiableList(this.argumentos);
    }

    public FunctionDescriptor changeReturnLayout(MemoryLayout newReturn) {
        if (newReturn == null) {
            throw new IllegalArgumentException("el retorno no puede ser null; use dropReturnLayout");
        }
        return new Descriptor(newReturn, this.argumentos);
    }

    public FunctionDescriptor dropReturnLayout() {
        return new Descriptor(null, this.argumentos);
    }

    public FunctionDescriptor appendArgumentLayouts(MemoryLayout... addedLayouts) {
        return this.insertArgumentLayouts(this.argumentos.size(), addedLayouts);
    }

    public FunctionDescriptor insertArgumentLayouts(int index, MemoryLayout... addedLayouts) {
        if (index < 0 || index > this.argumentos.size()) {
            throw new IllegalArgumentException("posicion fuera de rango: " + index);
        }
        List<MemoryLayout> nuevos = new ArrayList<MemoryLayout>(this.argumentos);
        nuevos.addAll(index, enLista(addedLayouts));
        return new Descriptor(this.retorno, nuevos);
    }

    public MethodType toMethodType() {
        Class<?> ret = this.retorno == null ? Void.TYPE : portador(this.retorno);
        Class<?>[] params = new Class<?>[this.argumentos.size()];
        int i = 0;
        while (i < this.argumentos.size()) {
            params[i] = portador(this.argumentos.get(i));
            i = i + 1;
        }
        return MethodType.methodType(ret, params);
    }

    // Un layout compuesto no tiene tipo Java propio: un struct no "es" un `int` ni un `MemorySegment`
    // en la firma del metodo -- el enlazador decide como pasarlo segun la convencion de llamada, y
    // esa decision no vive aca. Se rechaza en vez de elegir una.
    private static Class<?> portador(MemoryLayout l) {
        if (!(l instanceof ValueLayout)) {
            throw new UnsupportedOperationException(
                    "solo un layout de valor tiene un tipo Java que lo transporte: " + l);
        }
        return ((ValueLayout) l).carrier();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Descriptor)) {
            return false;
        }
        Descriptor otro = (Descriptor) obj;
        boolean mismoRetorno = this.retorno == null ? otro.retorno == null
                : this.retorno.equals(otro.retorno);
        return mismoRetorno && this.argumentos.equals(otro.argumentos);
    }

    public int hashCode() {
        return this.argumentos.hashCode() * 31
                + (this.retorno == null ? 0 : this.retorno.hashCode());
    }

    // `(j8)i4` con retorno, `(i4)v` sin el. La `v` de `void` no es un layout: es la marca de que no
    // hay ninguno, y por eso se imprime aparte.
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        int i = 0;
        while (i < this.argumentos.size()) {
            sb.append(this.argumentos.get(i).toString());
            i = i + 1;
        }
        sb.append(')');
        if (this.retorno == null) {
            sb.append('v');
        } else {
            sb.append(this.retorno.toString());
        }
        return sb.toString();
    }
}
