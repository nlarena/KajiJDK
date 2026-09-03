package java.io;

import java.io.ObjectInputFilter.FilterInfo;
import java.io.ObjectInputFilter.Status;
import java.util.function.Predicate;

// Las tres implementaciones que devuelven las fabricas de `ObjectInputFilter`.
//
// Viven aca y no adentro de la interfaz porque **todo tipo anidado en una interfaz es publico**
// (JLS 9.5): ponerlas ahi las convertiria en API con nombre propio, y lo que se promete es un
// `ObjectInputFilter`, no una clase en particular. Que el nombre no se pueda escribir es lo que deja
// cambiarlas despues.
//
// El `qué` de cada una esta en el javadoc de la fabrica que la construye; aca solo esta el `como`.
final class Filtros {

    private Filtros() {
    }

    // `allowFilter` y `rejectFilter` son la misma clase con los dos resultados cambiados de lugar.
    // Tenerlas separadas duplicaria una logica de tres lineas para que difirieran el dia que alguien
    // toque una sola.
    static final class PorPredicado implements ObjectInputFilter {

        private final Predicate<Class<?>> predicado;
        private final Status siCumple;
        private final Status siNo;

        PorPredicado(Predicate<Class<?>> predicado, Status siCumple, Status siNo) {
            this.predicado = predicado;
            this.siCumple = siCumple;
            this.siNo = siNo;
        }

        public Status checkInput(FilterInfo info) {
            Class<?> c = info.serialClass();
            if (c == null) {
                return Status.UNDECIDED;
            }
            return this.predicado.test(c) ? this.siCumple : this.siNo;
        }
    }

    static final class Union implements ObjectInputFilter {

        private final ObjectInputFilter primero;
        private final ObjectInputFilter segundo;

        Union(ObjectInputFilter primero, ObjectInputFilter segundo) {
            this.primero = primero;
            this.segundo = segundo;
        }

        public Status checkInput(FilterInfo info) {
            Status a = this.primero.checkInput(info);
            if (a == Status.REJECTED) {
                // Corto antes de consultar al segundo: no cambiaria el resultado, y un filtro puede
                // ser caro o tener efectos (registrar el intento) que no corresponde disparar
                // cuando la decision ya esta tomada.
                return Status.REJECTED;
            }
            Status b = this.segundo.checkInput(info);
            if (b == Status.REJECTED) {
                return Status.REJECTED;
            }
            if (a == Status.ALLOWED || b == Status.ALLOWED) {
                return Status.ALLOWED;
            }
            return Status.UNDECIDED;
        }
    }

    static final class RechazaIndecisos implements ObjectInputFilter {

        private final ObjectInputFilter envuelto;

        RechazaIndecisos(ObjectInputFilter envuelto) {
            this.envuelto = envuelto;
        }

        public Status checkInput(FilterInfo info) {
            Status s = this.envuelto.checkInput(info);
            if (s == Status.UNDECIDED && info.serialClass() != null) {
                return Status.REJECTED;
            }
            return s;
        }
    }
}
