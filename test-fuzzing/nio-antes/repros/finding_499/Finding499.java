// #499 -- una clase anonima en un INICIALIZADOR DE CAMPO no llega al generador de bytecode.
//
// Anda dentro de un metodo y falla en un inicializador, sea de instancia o estatico. El error no es
// del resolvedor (que es lo de #465) sino del generador: "necesita una clase sintetica anidada".
public class Finding499 {

    interface F {
        boolean f(String s);
    }

    // --- falla ---------------------------------------------------------------------------------
    static F campoEstatico = new F() {
        public boolean f(String s) {
            return false;
        }
    };

    F campoDeInstancia = new F() {
        public boolean f(String s) {
            return false;
        }
    };

    // --- anda ----------------------------------------------------------------------------------
    static F dentroDeUnMetodo() {
        return new F() {
            public boolean f(String s) {
                return false;
            }
        };
    }

    // --- el rodeo ------------------------------------------------------------------------------
    static class Impl implements F {
        public boolean f(String s) {
            return false;
        }
    }

    static F conClaseConNombre = new Impl();
}
