//! `java.lang.reflect.Method.invoke` — la llamada cuyo destino se decide en tiempo de ejecución.
//!
//! Vive en el intérprete y no en el puente de nativas por una razón estructural: `invoke` tiene
//! que **correr bytecode**, y el puente de nativas sólo puede computar y devolver. `call_java` es
//! lo que hace posible lo primero, y sólo `Exec` lo tiene.
//!
//! Lo que hace, en orden:
//!
//! 1. Lee el `Method` — que es un objeto Java corriente con campos, no una identidad opaca —
//!    y reconstruye el **descriptor** desde `parameterTypes` y `returnType`. El descriptor es la
//!    mitad de la identidad de un método: dos métodos del mismo nombre son distintos si difieren
//!    en él, y sin reconstruirlo no hay forma de resolver el correcto.
//! 2. **Desempaqueta** los argumentos. Llegan como `Object[]`, así que un parámetro `int` viene
//!    dentro de un `Integer` y hay que sacarlo; un parámetro de referencia pasa tal cual.
//! 3. Empuja el frame con `call_java` y lo corre hasta el final.
//! 4. **Empaqueta** el resultado. La firma devuelve `Object`, así que un `int` de vuelta tiene
//!    que salir dentro de un `Integer`, y un método `void` devuelve `null`.
//!
//! Los pasos 2 y 4 son el precio de que la reflexión hable en `Object` y el intérprete en valores
//! de la máquina: son la misma frontera que el autoboxing del lenguaje, cruzada a mano porque acá
//! el tipo no se conoce hasta que se lee el `Method`.

use crate::jvm::interpreter::bytecode_interpreter::objects_operations::field_offset;
use crate::jvm::interpreter::bytecode_interpreter::{
    array_operations, class_operations, objects_operations, Exec, Step,
};
use crate::jvm::interpreter::frame::Value;
use crate::jvm::interpreter::natives;

/// El wrapper de cada primitivo, el campo donde guarda su valor, y el descriptor de su `valueOf`.
/// Una sola tabla para las dos direcciones: desempaquetar lee el campo, empaquetar llama al
/// `valueOf` — que es el que respeta la cache de −128..127, y por eso no se aloca el objeto a
/// mano.
const BOXES: [(&str, &str, &str); 8] = [
    ("int", "java/lang/Integer", "(I)Ljava/lang/Integer;"),
    ("long", "java/lang/Long", "(J)Ljava/lang/Long;"),
    ("double", "java/lang/Double", "(D)Ljava/lang/Double;"),
    ("float", "java/lang/Float", "(F)Ljava/lang/Float;"),
    ("short", "java/lang/Short", "(S)Ljava/lang/Short;"),
    ("byte", "java/lang/Byte", "(B)Ljava/lang/Byte;"),
    ("char", "java/lang/Character", "(C)Ljava/lang/Character;"),
    ("boolean", "java/lang/Boolean", "(Z)Ljava/lang/Boolean;"),
];

fn box_of(primitive: &str) -> Option<(&'static str, &'static str)> {
    BOXES.iter().find(|(name, _, _)| *name == primitive).map(|(_, class, valueof)| (*class, *valueof))
}

/// Qué wrapper sirve para qué parámetro primitivo: el tipo exacto, más las **ampliaciones** que
/// el lenguaje permite sin pérdida de rango (JLS §5.1.2). `boolean` y `char` casi no participan —
/// `boolean` no se amplía a nada y `char` se amplía hacia arriba pero nadie se amplía hacia él,
/// porque no tiene signo y ningún tipo con signo cabe entero adentro.
const WIDENS: [(&str, &[&str]); 8] = [
    ("boolean", &["java/lang/Boolean"]),
    ("byte", &["java/lang/Byte"]),
    ("char", &["java/lang/Character"]),
    ("short", &["java/lang/Short", "java/lang/Byte"]),
    (
        "int",
        &["java/lang/Integer", "java/lang/Short", "java/lang/Character", "java/lang/Byte"],
    ),
    (
        "long",
        &[
            "java/lang/Long",
            "java/lang/Integer",
            "java/lang/Short",
            "java/lang/Character",
            "java/lang/Byte",
        ],
    ),
    (
        "float",
        &[
            "java/lang/Float",
            "java/lang/Long",
            "java/lang/Integer",
            "java/lang/Short",
            "java/lang/Character",
            "java/lang/Byte",
        ],
    ),
    (
        "double",
        &[
            "java/lang/Double",
            "java/lang/Float",
            "java/lang/Long",
            "java/lang/Integer",
            "java/lang/Short",
            "java/lang/Character",
            "java/lang/Byte",
        ],
    ),
];

fn widens_to(parameter: &str, wrapper: &str) -> bool {
    WIDENS
        .iter()
        .find(|(name, _)| *name == parameter)
        .is_some_and(|(_, accepted)| accepted.contains(&wrapper))
}

impl Exec<'_> {
    /// `Method.invoke(receptor, args)`. `method` es el objeto `Method`, `locals` son
    /// `[method, receptor, args]` tal como los dejó el sitio de llamada.
    pub(super) fn method_invoke(&mut self, method: usize, locals: &[Value]) -> Step {
        if method == 0 {
            return self.throw_exception("java/lang/NullPointerException");
        }
        let receiver = match locals.get(1) {
            Some(Value::Reference(offset)) => *offset,
            _ => 0,
        };
        let arguments = match locals.get(2) {
            Some(Value::Reference(offset)) => *offset,
            _ => 0,
        };

        // --- 1. Leer el Method y rearmar el descriptor -------------------------------------
        let (owner, name, parameters, returns, flags) = self.read_method_object(method);
        let descriptor = {
            let mut out = String::from("(");
            for p in &parameters {
                out.push_str(&natives::descriptor_of(p));
            }
            out.push(')');
            out.push_str(&natives::descriptor_of(&returns));
            out
        };
        const ACC_STATIC: u16 = 0x0008;
        let is_static = flags & ACC_STATIC != 0;
        if !is_static && receiver == 0 {
            return self.throw_exception("java/lang/NullPointerException");
        }

        // El método se resuelve sobre la clase que lo DECLARA. Un `invoke` no despacha
        // virtualmente por su cuenta: lo hace `call_java` si el callee tiene una entrada de
        // vtable, y si no, la declarante es la única respuesta posible.
        let Some(callee) = self.shared.metaspace.resolve_method(&owner, &name, &descriptor) else {
            return self.throw_exception("java/lang/NoSuchMethodError");
        };

        // --- 2. Desempaquetar los argumentos ------------------------------------------------
        let boxed = if arguments == 0 { Vec::new() } else { self.read_reference_array(arguments) };
        if boxed.len() != parameters.len() {
            return self.throw_exception("java/lang/IllegalArgumentException");
        }
        let mut args: Vec<Value> = Vec::with_capacity(parameters.len() + 1);
        let mut widths: Vec<usize> = Vec::with_capacity(parameters.len() + 1);
        if !is_static {
            args.push(Value::Reference(receiver));
            widths.push(1);
        }
        for (parameter, value) in parameters.iter().zip(boxed.iter()) {
            let reference = match *value {
                Value::Reference(offset) => offset,
                _ => 0,
            };
            match self.unbox(parameter, reference) {
                Some(unboxed) => {
                    widths.push(match unboxed {
                        Value::Long(_) | Value::Double(_) => 2,
                        _ => 1,
                    });
                    args.push(unboxed);
                }
                None => return self.throw_exception("java/lang/IllegalArgumentException"),
            }
        }

        // --- 3. Correrlo --------------------------------------------------------------------
        let result = self.call_java(callee, args, &widths);
        if self.threw() {
            // El metodo invocado tiro. Se envuelve y se **entrega** en este frame: la excepcion
            // quedo aparcada en la pila de operandos por `call_java`, y dejarla ahi sin
            // desenrollar es lo que le deja al frame un valor extra encima — el "operand stack
            // underflow" que aparece una instruccion despues, lejos de la causa.
            self.wrap_invocation_target();
            if let Some(step) = self.take_pending_throw() {
                return step;
            }
            return Step::Continue;
        }

        // --- 4. Empaquetar el resultado -----------------------------------------------------
        let pushed = match result {
            None => Value::Reference(0), // void → null
            Some(value) => match self.rebox(&returns, value) {
                Some(reference) => reference,
                // El `valueOf` del wrapper tiro (o no se pudo resolver): lo que sea que quedo
                // pendiente se entrega igual que arriba.
                None => return self.take_pending_throw().unwrap_or(Step::Continue),
            },
        };
        self.top().push(pushed);
        self.advance_past_call();
        Step::Continue
    }

    /// `Constructor.newInstance(args)`. `ctor` es el objeto `Constructor`, `locals` son
    /// `[ctor, args]`.
    ///
    /// Un `new` de bytecode son dos instrucciones -- `new` aloca y `invokespecial <init>` corre
    /// el cuerpo -- y esto es exactamente eso, con el tipo decidido en tiempo de ejecucion en vez
    /// de escrito en el pool de constantes.
    pub(super) fn constructor_new_instance(&mut self, ctor: usize, locals: &[Value]) -> Step {
        if ctor == 0 {
            return self.throw_exception("java/lang/NullPointerException");
        }
        let arguments = match locals.get(1) {
            Some(Value::Reference(offset)) => *offset,
            _ => 0,
        };
        let (owner, parameters, flags) = self.read_constructor_object(ctor);
        const ACC_ABSTRACT: u16 = 0x0400;
        const ACC_INTERFACE: u16 = 0x0200;
        let _ = ACC_INTERFACE;
        if flags & ACC_ABSTRACT != 0 {
            return self.throw_exception("java/lang/InstantiationException");
        }
        let descriptor = {
            let mut out = String::from("(");
            for p in &parameters {
                out.push_str(&natives::descriptor_of(p));
            }
            out.push_str(")V");
            out
        };
        let Some(callee) = self.shared.metaspace.resolve_method(&owner, "<init>", &descriptor)
        else {
            return self.throw_exception("java/lang/NoSuchMethodError");
        };

        let boxed = if arguments == 0 { Vec::new() } else { self.read_reference_array(arguments) };
        if boxed.len() != parameters.len() {
            return self.throw_exception("java/lang/IllegalArgumentException");
        }

        // La clase tiene que estar inicializada antes de que corra su constructor (JVMS §5.5), y
        // el `new` de bytecode lo garantiza por su cuenta; aca hay que pedirlo.
        self.ensure_initialized(&owner);
        if let Some(step) = self.take_pending_throw() {
            return step;
        }

        let Some(object) =
            objects_operations::try_allocate(&mut self.shared.metaspace, &mut self.shared.heap, &owner)
        else {
            return self.throw_exception("java/lang/OutOfMemoryError");
        };
        // **El objeto se estaciona en la pila de operandos antes de correr el `<init>`**, y no en
        // una variable de Rust. La pila es raiz del GC y el colector la reescribe; una variable
        // de Rust no lo es, y el `<init>` puede alocar y mover el objeto debajo de nosotros. Se
        // vuelve a leer despues, en su posicion posiblemente nueva.
        self.top().push(Value::Reference(object));

        let mut args: Vec<Value> = Vec::with_capacity(parameters.len() + 1);
        let mut widths: Vec<usize> = Vec::with_capacity(parameters.len() + 1);
        args.push(Value::Reference(object));
        widths.push(1);
        for (parameter, value) in parameters.iter().zip(boxed.iter()) {
            let reference = match *value {
                Value::Reference(offset) => offset,
                _ => 0,
            };
            match self.unbox(parameter, reference) {
                Some(unboxed) => {
                    widths.push(match unboxed {
                        Value::Long(_) | Value::Double(_) => 2,
                        _ => 1,
                    });
                    args.push(unboxed);
                }
                None => {
                    self.top().pop();
                    return self.throw_exception("java/lang/IllegalArgumentException");
                }
            }
        }

        self.call_java(callee, args, &widths);
        if self.threw() {
            // La excepcion quedo aparcada ENCIMA del objeto estacionado; sacarla, sacar el
            // objeto, y volver a aparcarla para que se entregue con la pila como estaba.
            if let Some(exception) = self.take_parked_exception() {
                self.top().pop(); // el objeto a medio construir, que ya no sirve
                self.park_exception(exception);
                self.wrap_invocation_target();
                if let Some(step) = self.take_pending_throw() {
                    return step;
                }
            }
            return Step::Continue;
        }
        // El `<init>` no devuelve nada: el resultado es el objeto, releido de donde el GC lo dejo.
        self.advance_past_call();
        Step::Continue
    }

    /// Los campos de un objeto `Constructor`: `(clase declarante, tipos de parametro,
    /// modificadores de la clase)`. Los modificadores que interesan son los de la CLASE y no los
    /// del constructor: lo que decide si se puede instanciar es que la clase sea abstracta.
    fn read_constructor_object(&mut self, ctor: usize) -> (String, Vec<String>, u16) {
        const CTOR: &str = "java/lang/reflect/Constructor";
        let clazz_at = field_offset(&mut self.shared.metaspace, CTOR, "clazz");
        let params_at = field_offset(&mut self.shared.metaspace, CTOR, "parameterTypes");
        let owner_mirror = self.shared.heap.read_u32(ctor + clazz_at) as usize;
        let parameters_array = self.shared.heap.read_u32(ctor + params_at) as usize;
        let owner = natives::mirror_name(&self.shared.metaspace, owner_mirror);
        let mut parameters = Vec::new();
        if parameters_array != 0 {
            let length = self.shared.heap.read_u32(parameters_array + array_operations::LENGTH_OFFSET)
                as usize;
            for i in 0..length {
                let at = array_operations::ARRAY_HEADER_SIZE + i * 4;
                let mirror = self.shared.heap.read_u32(parameters_array + at) as usize;
                parameters.push(natives::mirror_name(&self.shared.metaspace, mirror));
            }
        }
        let flags = self
            .shared
            .metaspace
            .get_or_load(&owner)
            .map(|cf| cf.access_flags)
            .unwrap_or(0);
        (owner, parameters, flags)
    }

    /// Envuelve la excepción que quedó pendiente en un `InvocationTargetException`.
    ///
    /// No es cosmética. Sin ella, quien llama a `invoke` no puede distinguir dos cosas muy
    /// distintas: que **el método invocado** tiró, y que `invoke` no llegó a llamarlo (argumentos
    /// que no encajan, un método que no existe). El envoltorio es lo que pone esa frontera en el
    /// tipo, y es por lo que `getTargetException` existe.
    ///
    /// El orden importa por el GC: el offset del campo se calcula **antes** de alocar —cargar una
    /// clase para averiguarlo aloca— y el destino se vuelve a leer **después**, porque sigue
    /// enraizado en la pila de operandos y una colección pudo haberlo movido.
    fn wrap_invocation_target(&mut self) {
        const ITE: &str = "java/lang/reflect/InvocationTargetException";
        class_operations::load_class(&mut self.shared.metaspace, &mut self.shared.heap, ITE);
        let target_at = field_offset(&mut self.shared.metaspace, ITE, "target");
        let wrapper = self.new_exception_object(ITE);
        let target = self.peek_parked_exception();
        self.shared.heap.store_reference(wrapper, wrapper + target_at, target);
        self.replace_parked_exception(wrapper);
    }

    /// Los campos de un objeto `Method`: `(clase declarante, nombre, tipos de parámetro, tipo de
    /// retorno, modificadores)`, todos como nombres internos.
    fn read_method_object(&mut self, method: usize) -> (String, String, Vec<String>, String, u16) {
        const METHOD: &str = "java/lang/reflect/Method";
        let read = |exec: &mut Self, field: &str| -> usize {
            let at = field_offset(&mut exec.shared.metaspace, METHOD, field);
            exec.shared.heap.read_u32(method + at) as usize
        };
        let owner_mirror = read(self, "clazz");
        let name_reference = read(self, "name");
        let return_mirror = read(self, "returnType");
        let parameters_array = read(self, "parameterTypes");
        let modifiers_at = field_offset(&mut self.shared.metaspace, METHOD, "modifiers");
        let flags = self.shared.heap.read_u32(method + modifiers_at) as u16;

        let owner = natives::mirror_name(&self.shared.metaspace, owner_mirror);
        let name = crate::jvm::interpreter::strings::read(&self.shared.heap, name_reference);
        let returns = natives::mirror_name(&self.shared.metaspace, return_mirror);
        let mut parameters = Vec::new();
        if parameters_array != 0 {
            let length = self.shared.heap.read_u32(parameters_array + array_operations::LENGTH_OFFSET) as usize;
            for i in 0..length {
                let at = array_operations::ARRAY_HEADER_SIZE + i * 4;
                let mirror = self.shared.heap.read_u32(parameters_array + at) as usize;
                parameters.push(natives::mirror_name(&self.shared.metaspace, mirror));
            }
        }
        (owner, name, parameters, returns, flags)
    }

    /// El valor que un argumento aporta para un parámetro de tipo `parameter`, o `None` si no
    /// sirve para ese parámetro.
    ///
    /// **El chequeo de tipo es la mitad del trabajo, no un adorno.** Un `Method` describe una
    /// firma y los argumentos llegan como `Object`: nada en el camino garantiza que coincidan, y
    /// leerle el campo `value` a un objeto que no es el wrapper esperado devuelve lo que haya en
    /// ese offset — un `String` pasado donde va un `int` daría un número perfectamente plausible
    /// en vez de un error. Por eso se pregunta primero de qué clase es el argumento.
    ///
    /// Las conversiones que sí se aceptan son las **ampliaciones** del lenguaje (JLS §5.1.2): un
    /// `Integer` sirve para un parámetro `long`, `float` o `double`, y no al revés. La tabla es
    /// la misma que usa el JDK.
    fn unbox(&mut self, parameter: &str, reference: usize) -> Option<Value> {
        if box_of(parameter).is_none() {
            // Un parámetro de referencia: `null` siempre sirve, y cualquier otra cosa tiene que
            // ser del tipo o de un subtipo.
            if reference == 0 {
                return Some(Value::Reference(0));
            }
            let actual = self.exception_class_name(reference);
            if actual != *parameter
                && !class_operations::is_subtype(&mut self.shared.metaspace, &actual, parameter)
            {
                return None;
            }
            return Some(Value::Reference(reference));
        }
        if reference == 0 {
            return None; // un primitivo no acepta null
        }
        let actual = self.exception_class_name(reference);
        if !widens_to(parameter, &actual) {
            return None;
        }
        // Se lee con el ancho del WRAPPER que llegó, no con el del parámetro: un `Integer` para
        // un parámetro `long` tiene cuatro bytes, y leerle ocho tomaría prestado el campo de al
        // lado.
        let at = field_offset(&mut self.shared.metaspace, &actual, "value");
        let narrow = self.shared.heap.read_u32(reference + at);
        let wide = self.shared.heap.read_u64(reference + at);
        let integral: i64;
        let fractional: f64;
        let is_fractional;
        match actual.as_str() {
            "java/lang/Long" => {
                integral = wide as i64;
                fractional = 0.0;
                is_fractional = false;
            }
            "java/lang/Double" => {
                integral = 0;
                fractional = f64::from_bits(wide);
                is_fractional = true;
            }
            "java/lang/Float" => {
                integral = 0;
                fractional = f32::from_bits(narrow) as f64;
                is_fractional = true;
            }
            // Integer, Short, Byte, Character y Boolean: un slot, con signo salvo `char`.
            _ => {
                integral = narrow as i32 as i64;
                fractional = 0.0;
                is_fractional = false;
            }
        }
        Some(match parameter {
            "long" => Value::Long(integral),
            "float" => {
                if is_fractional {
                    Value::Float(fractional as f32)
                } else {
                    Value::Float(integral as f32)
                }
            }
            "double" => {
                if is_fractional {
                    Value::Double(fractional)
                } else {
                    Value::Double(integral as f64)
                }
            }
            // boolean, byte, char, short e int viajan en un slot de entero.
            _ => Value::Int(integral as i32),
        })
    }

    /// El resultado de la llamada, como el `Object` que `invoke` devuelve. Una referencia ya lo
    /// es; un primitivo se envuelve llamando al `valueOf` de su wrapper, que es lo que respeta la
    /// cache de valores chicos y hace que `invoke` de un `int` chico devuelva el mismo objeto que
    /// el autoboxing del lenguaje.
    fn rebox(&mut self, returns: &str, value: Value) -> Option<Value> {
        let Some((wrapper, valueof)) = box_of(returns) else {
            return Some(value); // ya es una referencia
        };
        class_operations::load_class(&mut self.shared.metaspace, &mut self.shared.heap, wrapper);
        let boxer = self.shared.metaspace.resolve_method(wrapper, "valueOf", valueof)?;
        let width = match value {
            Value::Long(_) | Value::Double(_) => 2,
            _ => 1,
        };
        self.call_java(boxer, vec![value], &[width])
    }
}
