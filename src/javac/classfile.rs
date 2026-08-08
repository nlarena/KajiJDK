//! Lector **mínimo** de `.class` para el *class finder* del compilador — su **propio**
//! lector, desacoplado de la JVM (`src/jvm`). Extrae solo lo necesario para construir un
//! símbolo de tipo externo: nombre, supertipos, y las firmas de campos y métodos. No
//! interpreta bytecode.
//!
//! Lee **dos** vistas de cada firma:
//!
//! - el **descriptor** (`(I)Ljava/lang/Object;`), que es la firma ya *borrada* — lo que la JVM
//!   usa para el dispatch;
//! - el atributo **`Signature`** (JVMS §4.7.9.1), que conserva los genéricos
//!   (`<E:Ljava/lang/Object;>...Ljava/util/Collection<TE;>;`).
//!
//! Sin el segundo, `java.util.List` sería `List` y no `List<E>`, y `list.get(0)` devolvería
//! `Object` en vez de `E`. Por eso acá vive también el parser de la gramática de signatures.

use super::ast::{PrimType, Type, TypeArg, TypeParam};

/// `ACC_VARARGS` (JVMS §4.6): el método se declaró con `...`. Lo necesita la fase 3 del
/// overload resolution — el descriptor solo dice `[I`, no distingue `int[]` de `int...`.
const ACC_VARARGS: u16 = 0x0080;
const ACC_STATIC: u16 = 0x0008;
const ACC_ABSTRACT: u16 = 0x0400;
const ACC_INTERFACE: u16 = 0x0200;

/// La info que el finder necesita de un `.class`. Los nombres vienen en forma *dotted*
/// (`java.lang.Object`); los descriptores ya están convertidos a [`Type`].
pub struct ExternalClass {
    pub name: String,
    /// `ACC_INTERFACE` — para registrarla con el `TypeKind` correcto (lo necesita la detección del
    /// SAM de una interfaz funcional).
    pub is_interface: bool,
    pub super_name: Option<String>,
    pub interfaces: Vec<String>,
    pub fields: Vec<ExtField>,
    pub methods: Vec<ExtMethod>,
    /// La firma **genérica** de la clase, si la tiene (§4.7.9.1).
    pub signature: Option<ClassSig>,
}

pub struct ExtField {
    pub name: String,
    /// El tipo del descriptor (borrado).
    pub ty: Type,
    /// El tipo **genérico** del atributo `Signature`, si lo tiene.
    pub generic_ty: Option<Type>,
}

pub struct ExtMethod {
    pub name: String,
    pub params: Vec<Type>,
    pub ret: Type,
    pub varargs: bool,
    /// `ACC_ABSTRACT` — lo necesita la detección del **SAM** de una interfaz funcional: un método
    /// `default` (con cuerpo) no es abstracto y no cuenta.
    pub is_abstract: bool,
    /// `ACC_STATIC` — un método estático de interfaz tampoco cuenta para el SAM.
    pub is_static: bool,
    /// La firma **genérica** del atributo `Signature`, si la tiene.
    pub signature: Option<MethodSig>,
}

/// Una `ClassSignature` parseada (§4.7.9.1): `<E:Ljava/lang/Object;>Ljava/lang/Object;Ljava/util/Collection<TE;>;`
pub struct ClassSig {
    pub type_params: Vec<TypeParam>,
    pub super_type: Option<Type>,
    pub interfaces: Vec<Type>,
}

/// Una `MethodSignature` parseada: `<T:Ljava/lang/Object;>([TT;)[TT;`
pub struct MethodSig {
    pub type_params: Vec<TypeParam>,
    pub params: Vec<Type>,
    pub ret: Type,
}

/// Lee los bytes de un `.class`. `None` si no es un class file válido o se trunca.
pub fn read(bytes: &[u8]) -> Option<ExternalClass> {
    let mut r = Reader { b: bytes, pos: 0 };
    if r.u4()? != 0xCAFE_BABE {
        return None;
    }
    r.u2()?; // minor
    r.u2()?; // major
    let pool = read_pool(&mut r)?;

    let access_flags = r.u2()?;
    let name = pool.class_name(r.u2()?)?;
    let super_idx = r.u2()?;
    let super_name = if super_idx == 0 { None } else { pool.class_name(super_idx) };

    let mut interfaces = Vec::new();
    for _ in 0..r.u2()? {
        if let Some(n) = pool.class_name(r.u2()?) {
            interfaces.push(n);
        }
    }

    let mut fields = Vec::new();
    for _ in 0..r.u2()? {
        r.u2()?; // access
        let fname = pool.utf8(r.u2()?)?;
        let desc = pool.utf8(r.u2()?)?;
        let sig = read_attributes(&mut r, &pool)?;
        fields.push(ExtField {
            name: fname,
            ty: parse_field_desc(&desc)?,
            generic_ty: sig.as_deref().and_then(parse_field_signature),
        });
    }

    let mut methods = Vec::new();
    for _ in 0..r.u2()? {
        let access = r.u2()?;
        let mname = pool.utf8(r.u2()?)?;
        let desc = pool.utf8(r.u2()?)?;
        let sig = read_attributes(&mut r, &pool)?;
        let (params, ret) = parse_method_desc(&desc)?;
        methods.push(ExtMethod {
            name: mname,
            params,
            ret,
            varargs: access & ACC_VARARGS != 0,
            is_abstract: access & ACC_ABSTRACT != 0,
            is_static: access & ACC_STATIC != 0,
            signature: sig.as_deref().and_then(parse_method_signature),
        });
    }

    let class_sig = read_attributes(&mut r, &pool)?;
    Some(ExternalClass {
        name,
        is_interface: access_flags & ACC_INTERFACE != 0,
        super_name,
        interfaces,
        fields,
        methods,
        signature: class_sig.as_deref().and_then(parse_class_signature),
    })
}

// ---- cursor de bytes big-endian ----

struct Reader<'a> {
    b: &'a [u8],
    pos: usize,
}

impl<'a> Reader<'a> {
    fn u1(&mut self) -> Option<u8> {
        let v = *self.b.get(self.pos)?;
        self.pos += 1;
        Some(v)
    }
    fn u2(&mut self) -> Option<u16> {
        Some(((self.u1()? as u16) << 8) | self.u1()? as u16)
    }
    fn u4(&mut self) -> Option<u32> {
        Some(((self.u2()? as u32) << 16) | self.u2()? as u32)
    }
    fn take(&mut self, n: usize) -> Option<&'a [u8]> {
        let s = self.b.get(self.pos..self.pos + n)?;
        self.pos += n;
        Some(s)
    }
}

// ---- constant pool (solo lo que usamos: Utf8 y Class; el resto se saltea) ----

enum Const {
    Utf8(String),
    Class(u16),
    Other,
}

struct Pool(Vec<Const>);

impl Pool {
    fn utf8(&self, idx: u16) -> Option<String> {
        match self.0.get(idx as usize)? {
            Const::Utf8(s) => Some(s.clone()),
            _ => None,
        }
    }
    /// El nombre *dotted* (`java.lang.Object`) de una entrada `Class`.
    fn class_name(&self, idx: u16) -> Option<String> {
        match self.0.get(idx as usize)? {
            Const::Class(name_idx) => self.utf8(*name_idx).map(|s| s.replace('/', ".")),
            _ => None,
        }
    }
}

fn read_pool(r: &mut Reader) -> Option<Pool> {
    let count = r.u2()? as usize;
    let mut pool = Vec::with_capacity(count);
    pool.push(Const::Other); // índice 0 no se usa
    let mut i = 1;
    while i < count {
        let tag = r.u1()?;
        match tag {
            1 => {
                let len = r.u2()? as usize;
                let s = String::from_utf8_lossy(r.take(len)?).into_owned();
                pool.push(Const::Utf8(s));
            }
            7 => pool.push(Const::Class(r.u2()?)),
            8 | 16 | 19 | 20 => {
                r.u2()?;
                pool.push(Const::Other);
            }
            15 => {
                r.u1()?;
                r.u2()?;
                pool.push(Const::Other);
            }
            3 | 4 | 9 | 10 | 11 | 12 | 17 | 18 => {
                r.u4()?;
                pool.push(Const::Other);
            }
            5 | 6 => {
                // Long/Double ocupan **dos** slots.
                r.u4()?;
                r.u4()?;
                pool.push(Const::Other);
                pool.push(Const::Other);
                i += 1;
            }
            _ => return None,
        }
        i += 1;
    }
    Some(Pool(pool))
}

/// Lee la tabla de atributos, devolviendo el valor del **`Signature`** si aparece (§4.7.9.1) y
/// salteando todo lo demás. A diferencia de saltear a ciegas, acá hay que resolver el nombre de
/// cada atributo contra el constant pool.
fn read_attributes(r: &mut Reader, pool: &Pool) -> Option<Option<String>> {
    let mut signature = None;
    for _ in 0..r.u2()? {
        let name = pool.utf8(r.u2()?);
        let len = r.u4()? as usize;
        let data = r.take(len)?;
        if name.as_deref() == Some("Signature") && data.len() >= 2 {
            let idx = ((data[0] as u16) << 8) | data[1] as u16;
            signature = pool.utf8(idx);
        }
    }
    Some(signature)
}

// ---- descriptores → Type ----

fn parse_field_desc(d: &str) -> Option<Type> {
    let mut i = 0;
    parse_desc(d.as_bytes(), &mut i)
}

fn parse_method_desc(d: &str) -> Option<(Vec<Type>, Type)> {
    let b = d.as_bytes();
    let mut i = 0;
    if *b.get(i)? != b'(' {
        return None;
    }
    i += 1;
    let mut params = Vec::new();
    while *b.get(i)? != b')' {
        params.push(parse_desc(b, &mut i)?);
    }
    i += 1; // ')'
    let ret = parse_desc(b, &mut i)?;
    Some((params, ret))
}

// ---- gramática de signatures (JVMS §4.7.9.1) ----
//
// A diferencia de un descriptor, una signature conserva los genéricos:
//
//   ClassSignature:  TypeParameters? SuperclassSignature SuperinterfaceSignature*
//   TypeParameters:  < TypeParameter+ >
//   TypeParameter:   Identifier ClassBound InterfaceBound*      (`:` cota de clase, `:` cada iface)
//   ClassTypeSig:    L PackageSpecifier* Identifier TypeArguments? (. Identifier TypeArguments?)* ;
//   TypeArgument:    [+-]? ReferenceTypeSignature | *           (`+`=extends, `-`=super, `*`=?)
//   TypeVariableSig: T Identifier ;
//   MethodSignature: TypeParameters? ( JavaTypeSignature* ) Result ThrowsSignature*

/// Un cursor sobre los bytes de una signature.
struct SigReader<'a> {
    b: &'a [u8],
    i: usize,
}

impl SigReader<'_> {
    fn peek(&self) -> Option<u8> {
        self.b.get(self.i).copied()
    }
    fn eat(&mut self, c: u8) -> bool {
        if self.peek() == Some(c) {
            self.i += 1;
            return true;
        }
        false
    }
    /// Un identificador: hasta el próximo carácter reservado de la gramática.
    fn ident(&mut self) -> Option<String> {
        let start = self.i;
        while let Some(c) = self.peek() {
            if matches!(c, b';' | b':' | b'<' | b'>' | b'/' | b'.') {
                break;
            }
            self.i += 1;
        }
        if self.i == start {
            return None;
        }
        Some(String::from_utf8_lossy(&self.b[start..self.i]).into_owned())
    }
}

pub fn parse_class_signature(s: &str) -> Option<ClassSig> {
    let mut r = SigReader { b: s.as_bytes(), i: 0 };
    let type_params = sig_type_params(&mut r)?;
    let super_type = sig_ref_type(&mut r);
    let mut interfaces = Vec::new();
    while r.peek().is_some() {
        match sig_ref_type(&mut r) {
            Some(t) => interfaces.push(t),
            None => break,
        }
    }
    Some(ClassSig { type_params, super_type, interfaces })
}

pub fn parse_method_signature(s: &str) -> Option<MethodSig> {
    let mut r = SigReader { b: s.as_bytes(), i: 0 };
    let type_params = sig_type_params(&mut r)?;
    if !r.eat(b'(') {
        return None;
    }
    let mut params = Vec::new();
    while r.peek() != Some(b')') {
        params.push(sig_java_type(&mut r)?);
    }
    r.eat(b')');
    let ret = sig_java_type(&mut r)?;
    // Los `^Throws` que puedan seguir no nos interesan todavía.
    Some(MethodSig { type_params, params, ret })
}

pub fn parse_field_signature(s: &str) -> Option<Type> {
    let mut r = SigReader { b: s.as_bytes(), i: 0 };
    sig_java_type(&mut r)
}

/// `<T:Ljava/lang/Object;U::Ljava/lang/Comparable<TT;>;>` — cada `:` introduce una cota; la
/// primera (la de clase) puede venir **vacía** cuando la cota es una interfaz.
fn sig_type_params(r: &mut SigReader) -> Option<Vec<TypeParam>> {
    let mut out = Vec::new();
    if !r.eat(b'<') {
        return Some(out); // sin parámetros de tipo
    }
    while !r.eat(b'>') {
        let name = r.ident()?;
        let mut bounds = Vec::new();
        while r.eat(b':') {
            // `T::Lfoo;` — la cota de clase vacía se saltea; la siguiente es la de interfaz.
            if let Some(b) = sig_ref_type(r) {
                bounds.push(b);
            }
        }
        out.push(TypeParam { annotations: Vec::new(), name, bounds, bound_annos: Vec::new() });
        if r.peek().is_none() {
            return None; // truncada
        }
    }
    Some(out)
}

/// Un `JavaTypeSignature`: un primitivo, `V`, o un tipo referencia.
fn sig_java_type(r: &mut SigReader) -> Option<Type> {
    let prim = match r.peek()? {
        b'B' => PrimType::Byte,
        b'C' => PrimType::Char,
        b'D' => PrimType::Double,
        b'F' => PrimType::Float,
        b'I' => PrimType::Int,
        b'J' => PrimType::Long,
        b'S' => PrimType::Short,
        b'Z' => PrimType::Boolean,
        b'V' => {
            r.i += 1;
            return Some(Type::Void);
        }
        _ => return sig_ref_type(r),
    };
    r.i += 1;
    Some(Type::Prim(prim))
}

/// Un `ReferenceTypeSignature`: clase (posiblemente parametrizada), variable de tipo, o array.
fn sig_ref_type(r: &mut SigReader) -> Option<Type> {
    match r.peek()? {
        // `[` JavaTypeSignature
        b'[' => {
            r.i += 1;
            Some(Type::Array(Box::new(sig_java_type(r)?)))
        }
        // `T` Identifier `;`
        b'T' => {
            r.i += 1;
            let name = r.ident()?;
            r.eat(b';');
            Some(Type::Class(name))
        }
        // `L` paquete/Clase `<args>` (`.Inner<args>`)* `;`
        b'L' => {
            r.i += 1;
            let mut name = String::new();
            let mut ty;
            loop {
                let part = r.ident()?;
                if !name.is_empty() {
                    name.push('.');
                }
                name.push_str(&part);
                if r.eat(b'/') {
                    continue; // seguía el paquete
                }
                // Ya tenemos un nombre de clase: puede llevar argumentos.
                let args = sig_type_args(r)?;
                ty = if args.is_empty() {
                    Type::Class(name.clone())
                } else {
                    Type::Parameterized { base: name.clone(), args }
                };
                // `.Inner` — clase anidada: se acumula al nombre cualificado.
                if r.eat(b'.') {
                    continue;
                }
                r.eat(b';');
                break;
            }
            Some(ty)
        }
        _ => None,
    }
}

/// `<TypeArgument+>`, o vacío si no hay `<`.
fn sig_type_args(r: &mut SigReader) -> Option<Vec<TypeArg>> {
    let mut out = Vec::new();
    if !r.eat(b'<') {
        return Some(out);
    }
    while !r.eat(b'>') {
        let arg = match r.peek()? {
            b'*' => {
                r.i += 1;
                TypeArg::Wildcard
            }
            b'+' => {
                r.i += 1;
                TypeArg::Extends(Box::new(sig_ref_type(r)?))
            }
            b'-' => {
                r.i += 1;
                TypeArg::Super(Box::new(sig_ref_type(r)?))
            }
            _ => TypeArg::Type(sig_ref_type(r)?),
        };
        out.push(arg);
        if r.peek().is_none() {
            return None; // truncada
        }
    }
    Some(out)
}

fn parse_desc(b: &[u8], i: &mut usize) -> Option<Type> {
    let c = *b.get(*i)?;
    *i += 1;
    Some(match c {
        b'B' => Type::Prim(PrimType::Byte),
        b'C' => Type::Prim(PrimType::Char),
        b'D' => Type::Prim(PrimType::Double),
        b'F' => Type::Prim(PrimType::Float),
        b'I' => Type::Prim(PrimType::Int),
        b'J' => Type::Prim(PrimType::Long),
        b'S' => Type::Prim(PrimType::Short),
        b'Z' => Type::Prim(PrimType::Boolean),
        b'V' => Type::Void,
        b'[' => Type::Array(Box::new(parse_desc(b, i)?)),
        b'L' => {
            let mut name = String::new();
            while *i < b.len() && b[*i] != b';' {
                name.push(if b[*i] == b'/' { '.' } else { b[*i] as char });
                *i += 1;
            }
            *i += 1; // ';'
            Type::Class(name)
        }
        _ => return None,
    })
}
