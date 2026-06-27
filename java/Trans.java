// Marcado transitivo: el Animal es alcanzable SOLO a través de h.pet (ningún local
// lo referencia directamente). Un marcado solo-raíces lo daría por basura y al
// barrer corrompería h.pet; con reference_slots queda vivo y `return h.pet.legs`=4.
class Trans {
    static int run() {
        Holder h = new Holder();
        h.pet = new Animal();   // el Animal cuelga del campo de h, de nada más
        return h.pet.legs;      // 4
    }
}
