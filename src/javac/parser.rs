//! El **parser**: consume los [`Token`]s del lexer y construye el [`ast`](super::ast)
//! mediante *recursive descent*, con precedencia por *precedence climbing* para las
//! expresiones y *backtracking* acotado para las ambigüedades clásicas (declaración local
//! vs. expresión, y *cast* vs. paréntesis).
//!
//! Hito **B1**. Cubre `package`/`import`, clases/interfaces/enums/records/`@interface` con sus
//! miembros, **genéricos** (parámetros de tipo con cotas, argumentos de tipo y wildcards), las
//! expresiones con toda la precedencia, las **lambdas** (§15.27), las **referencias a método**
//! (§15.13), las **clases anónimas** (§15.9.5) y las **anotaciones** (§9.7, retenidas en el AST).
//!
//! ## Lambdas
//!
//! Una lambda es una *assignment-expression*, así que se decide en [`Parser::assignment`], antes de
//! la cascada de precedencia. El nudo es distinguir un `(` que abre parámetros de uno que abre un
//! *cast* o un paréntesis: lo resuelve [`Parser::lambda_ahead`], un barrido no destructivo que
//! balancea delimitadores hasta el `)` de cierre y mira si lo sigue una flecha `->` — la misma señal
//! que usa javac. La forma sin paréntesis (`x -> …`) no es ambigua. Cubre las cinco formas de
//! parámetro (inferido, tipado, `var`, `final`, y cero/varios) y las dos de cuerpo (expresión y
//! bloque). Se **parsea** entero; **tiparla** (contra su *functional interface*) y **emitirla** (con
//! `invokedynamic`) son de B2/B3, y hasta entonces la corta la barrera del emisor.
//!
//! ## Referencias a método
//!
//! `qualifier :: name`, con `new` para las de constructor. Casi todas caen en `postfix` como un
//! sufijo más de `::` sobre lo ya parseado (`String::length`, `System.out::println`, `this::run`,
//! `ArrayList::new`): el izquierdo queda como **expresión** y la semántica decidirá si era un tipo
//! (§15.13.1). La excepción es `T[]::new` —una referencia a **constructor de array**—, cuyo
//! izquierdo es un tipo y no una expresión: se reconoce por adelantado ([`Parser::array_ctor_ref_ahead`],
//! por los corchetes **vacíos** que la separan de un `arr[0]::…`) y guarda el qualifier como
//! [`MethodRefQualifier::Type`]. Igual que la lambda: se parsea, pero resolverla y emitirla son de
//! B2/B3, incluido el **type witness** de la referencia (`C::<T>m`, ver abajo).
//!
//! ## Type witness
//!
//! Argumentos de tipo **explícitos** antes del nombre de un método (`Collections.<String>emptyList()`,
//! §15.12.2.1) o de una referencia (`C::<T>m`, §15.13). Se leen justo después del `.`/`::`, con la
//! misma [`Parser::type_args`] que un tipo parametrizado (así el split de `>>` también aplica), y se
//! guardan en el `type_args` del [`ExprKind::Call`]/[`ExprKind::MethodRef`] —vacío en el caso
//! corriente—. No hay ambigüedad con el `<` **menor-que**: el witness solo aparece tras un `.`/`::`,
//! donde un operador no puede ir. A diferencia de la lambda, este **sí se usa**: `attribute` lo aplica
//! como sustitución que **fija** los parámetros de tipo del método en vez de inferirlos, cerrando el
//! hueco de que un override deliberado (`this.<Integer>id(x)`) quedara ignorado. Queda afuera el
//! witness de **constructor** (`new <T>Foo()`, §15.9), una forma rarísima.
//!
//! ## Clases anónimas
//!
//! `new Type(args) { members }` (§15.9.5): tras los argumentos, un `{` abre un **cuerpo de clase**,
//! que se parsea con la misma [`Parser::class_body`] que el de una declaración de tipo y se guarda
//! en el `Option` del [`ExprKind::NewObject`] (`None` en el `new` corriente). El cuerpo se pasa con
//! nombre vacío, así ningún método se toma por constructor —una anónima no puede declararlos
//! (§15.9.5.1)—. Se parsea entero (incluidas las **anidadas**); compilarla (una clase sintética
//! anidada, con captura del entorno) es de B3.
//!
//! ## Sentencias (§14)
//!
//! Están **todas**: bloque, declaración local (múltiple, `final`, y con declarador **al estilo C**
//! `int y[]`), **clase local** (§14.3), `;`, etiquetada, expresión, `if`/`else`, `assert`, `switch`
//! (dos puntos y flecha, con *patterns* y guardas), `while`, `do`, `for` (básico, multi-init,
//! infinito) y `for-each` (con `var`), `break`/`continue` **con y sin etiqueta**, `return`, `throw`,
//! `synchronized`, `try`/`catch`/`finally` con *multi-catch* y recursos, y `yield`.
//!
//! La **clase local** se reconoce en [`Parser::block_stmt`] antes que la declaración de variable,
//! porque su detección ([`Parser::local_class_ahead`]) mira más allá de los modificadores: `final
//! class C` es una clase, `final int x` una variable. Se parsea con la misma maquinaria que una
//! anidada; entrarla a la tabla y compilarla son de una fase posterior.
//!
//! ## Anotaciones (§9.7)
//!
//! Ya **no se descartan**: [`Parser::modifiers`] las lee junto a los modificadores —van en la misma
//! posición— y las **devuelve** para colgarlas de la declaración ([`ClassDecl`], [`MethodDecl`],
//! [`FieldDecl`], [`Param`]). Se parsean enteras: marcador (`@Override`), valor único
//! (`@SuppressWarnings("x")`), pares con nombre (`@Foo(a = 1, b = {…})`), arreglos y anotaciones
//! **anidadas** (§9.7.1). El emisor todavía no las escribe como atributos `RuntimeVisibleAnnotations`
//! —eso es B3—, pero ya están en el AST para el chequeo de `@Override` y la reflexión futura.
//!
//! ## Lo que falta
//!
//! Cerrado el grueso del lenguaje, quedan tres bordes menores:
//!
//! - Las anotaciones en **posiciones de borde**: sobre un parámetro de tipo (`<@Foo T>`) o una
//!   constante de `enum` — ahí [`Parser::skip_annotation`] todavía las salta (no hay dónde colgarlas).
//! - Las **anotaciones de tipo/uso** (§9.7.4: `List<@NonNull String>`, `@Foo int[]`) — un mecanismo
//!   aparte del de las declaraciones.
//! - El **type witness de constructor** (`new <T>Foo()`) — forma rarísima, todavía da error.
//!
//! ## Una lección que dejó una auditoría
//!
//! Los bloques de inicialización se **parseaban y se descartaban**: el código desaparecía sin que
//! nada fallara. Hoy se conservan los dos (`static { }` → [`Member::StaticInit`], `{ }` →
//! [`Member::InstanceInit`]). Vale como recordatorio: *parsear y tirar* es peor que no parsear,
//! porque no deja rastro.

use super::ast::*;
use super::token::{Token, TokenKind};
use super::{Error, Result};

/// Parsea una lista de tokens (terminada en `Eof`) a una unidad de compilación.
pub fn parse(tokens: Vec<Token>) -> Result<CompilationUnit> {
    Parser { tokens, pos: 0, gt_splits: Vec::new() }.compilation_unit()
}

struct Parser {
    tokens: Vec<Token>,
    pos: usize,
    /// *Undo log* de los cortes de `>>`/`>>>` (ver [`Parser::eat_gt`]): posición y kind original.
    gt_splits: Vec<(usize, TokenKind)>,
}

/// Un punto al que volver en el *backtracking*: el cursor **y** cuántos cortes de `>` llevábamos.
#[derive(Clone, Copy)]
struct Mark {
    pos: usize,
    splits: usize,
}

impl Parser {
    // ---- cursor ----

    /// Marca la posición actual, para poder volver con [`Parser::reset`].
    fn mark(&self) -> Mark {
        Mark { pos: self.pos, splits: self.gt_splits.len() }
    }

    /// Vuelve a `m`, **deshaciendo** los cortes de `>` hechos desde entonces. Sin esto, un `>>`
    /// ya partido quedaría como `>` en el stream y la relectura perdería un `>`.
    fn reset(&mut self, m: Mark) {
        while self.gt_splits.len() > m.splits {
            let (i, kind) = self.gt_splits.pop().expect("hay splits que deshacer");
            self.tokens[i].kind = kind;
        }
        self.pos = m.pos;
    }

    /// Consume **un** `>` que cierra genéricos. El lexer junta `>>`/`>>>` en un token (son
    /// operadores de shift), así que acá hay que **partirlos**: se consume uno y el resto queda
    /// en el stream. Es lo que hace javac (`Token.split()`), pero con undo log para que el
    /// backtracking no deje el stream corrompido.
    fn eat_gt(&mut self) -> bool {
        use TokenKind as T;
        let rest = match self.peek_kind() {
            T::Gt => {
                self.bump();
                return true;
            }
            T::GtGt => T::Gt,
            T::GtGtGt => T::GtGt,
            _ => return false,
        };
        let i = self.pos.min(self.tokens.len() - 1);
        self.gt_splits.push((i, self.tokens[i].kind));
        self.tokens[i].kind = rest;
        true
    }

    fn peek(&self) -> &Token {
        &self.tokens[self.pos.min(self.tokens.len() - 1)]
    }

    fn peek_kind(&self) -> TokenKind {
        self.peek().kind
    }

    fn kind_at(&self, offset: usize) -> TokenKind {
        self.tokens[(self.pos + offset).min(self.tokens.len() - 1)].kind
    }

    fn at(&self, kind: TokenKind) -> bool {
        self.peek_kind() == kind
    }

    fn bump(&mut self) -> Token {
        let tok = self.tokens[self.pos.min(self.tokens.len() - 1)].clone();
        if self.pos < self.tokens.len() - 1 {
            self.pos += 1;
        }
        tok
    }

    fn eat(&mut self, kind: TokenKind) -> bool {
        if self.at(kind) {
            self.bump();
            true
        } else {
            false
        }
    }

    fn expect(&mut self, kind: TokenKind) -> Result<Token> {
        if self.at(kind) {
            Ok(self.bump())
        } else {
            Err(self.error(format!("se esperaba {:?}, se encontró {:?}", kind, self.peek_kind())))
        }
    }

    fn expect_ident(&mut self) -> Result<String> {
        if self.at(TokenKind::Identifier) {
            Ok(self.bump().text)
        } else {
            Err(self.error(format!("se esperaba un identificador, se encontró {:?}", self.peek_kind())))
        }
    }

    fn error(&self, message: impl Into<String>) -> Error {
        let t = self.peek();
        Error { message: message.into(), line: t.line, col: t.col }
    }

    /// La posición del token actual — para adjuntar a las declaraciones del AST.
    fn pos(&self) -> Pos {
        let t = self.peek();
        Pos { line: t.line, col: t.col }
    }

    // ---- unidad de compilación ----

    fn compilation_unit(&mut self) -> Result<CompilationUnit> {
        let package = if self.eat(TokenKind::Package) {
            let name = self.qualified_name()?;
            self.expect(TokenKind::Semi)?;
            Some(name)
        } else {
            None
        };

        let mut imports = Vec::new();
        while self.at(TokenKind::Import) {
            imports.push(self.import()?);
        }

        let mut types = Vec::new();
        while !self.at(TokenKind::Eof) {
            if self.eat(TokenKind::Semi) {
                continue; // `;` suelto entre tipos
            }
            let (modifiers, annotations) = self.modifiers()?;
            types.push(self.class_decl(modifiers, annotations)?);
        }
        Ok(CompilationUnit { package, imports, types })
    }

    fn import(&mut self) -> Result<Import> {
        self.expect(TokenKind::Import)?;
        let is_static = self.eat(TokenKind::Static);
        let mut path = self.expect_ident()?;
        let mut wildcard = false;
        while self.at(TokenKind::Dot) {
            self.bump();
            if self.eat(TokenKind::Star) {
                wildcard = true;
                break;
            }
            path.push('.');
            path.push_str(&self.expect_ident()?);
        }
        self.expect(TokenKind::Semi)?;
        Ok(Import { path, is_static, wildcard })
    }

    // ---- modificadores (y saltar anotaciones) ----

    /// Los **modificadores** de una declaración, con las **anotaciones** que los acompañan (§8.1.1,
    /// §9.7): en Java las dos cosas van intercaladas en la misma posición (`@Override public`,
    /// `public @Deprecated`), así que se leen juntas. Las anotaciones ya **no se descartan**: se
    /// devuelven para que el llamador las cuelgue de la declaración.
    fn modifiers(&mut self) -> Result<(Vec<Modifier>, Vec<Annotation>)> {
        use TokenKind as T;
        let mut mods = Vec::new();
        let mut annotations = Vec::new();
        loop {
            // `@interface` es una **declaración** de tipo, no una anotación de uso: no la comas.
            while self.at(T::MonkeysAt) && self.kind_at(1) != T::Interface {
                annotations.push(self.annotation()?);
            }
            let m = match self.peek_kind() {
                T::Public => Modifier::Public,
                T::Private => Modifier::Private,
                T::Protected => Modifier::Protected,
                T::Static => Modifier::Static,
                T::Final => Modifier::Final,
                T::Abstract => Modifier::Abstract,
                T::Native => Modifier::Native,
                T::Synchronized => Modifier::Synchronized,
                T::Transient => Modifier::Transient,
                T::Volatile => Modifier::Volatile,
                T::Strictfp => Modifier::Strictfp,
                T::Default => Modifier::Default,
                _ => break,
            };
            self.bump();
            mods.push(m);
        }
        Ok((mods, annotations))
    }

    /// Una anotación de uso `@Name`, `@Name(v)` o `@Name(a = 1, b = {…})` (§9.7). El `@` ya está a
    /// la vista.
    fn annotation(&mut self) -> Result<Annotation> {
        use TokenKind as T;
        self.expect(T::MonkeysAt)?;
        let name = self.qualified_name()?;
        let mut args = Vec::new();
        if self.eat(T::LParen) {
            if !self.at(T::RParen) {
                loop {
                    args.push(self.annotation_arg()?);
                    if !self.eat(T::Comma) {
                        break;
                    }
                }
            }
            self.expect(T::RParen)?;
        }
        Ok(Annotation { name, args })
    }

    /// Un par de anotación: `nombre = valor`, o el valor **solo** (elemento implícito `value`). Se
    /// distingue por el `=`: `Identifier =` es un par con nombre, cualquier otra cosa es el valor
    /// posicional.
    fn annotation_arg(&mut self) -> Result<AnnotationArg> {
        use TokenKind as T;
        if self.at(T::Identifier) && self.kind_at(1) == T::Eq {
            let name = self.expect_ident()?;
            self.expect(T::Eq)?;
            let value = self.annotation_value()?;
            Ok(AnnotationArg { name: Some(name), value })
        } else {
            Ok(AnnotationArg { name: None, value: self.annotation_value()? })
        }
    }

    /// El valor de un elemento (§9.7.1): un arreglo `{ … }`, una anotación anidada, o una expresión
    /// (constante, literal de clase, constante de `enum`).
    fn annotation_value(&mut self) -> Result<AnnotationValue> {
        use TokenKind as T;
        if self.at(T::LBrace) {
            self.bump();
            let mut elems = Vec::new();
            while !self.at(T::RBrace) {
                elems.push(self.annotation_value()?);
                if !self.eat(T::Comma) {
                    break;
                }
            }
            self.expect(T::RBrace)?;
            Ok(AnnotationValue::Array(elems))
        } else if self.at(T::MonkeysAt) {
            Ok(AnnotationValue::Nested(Box::new(self.annotation()?)))
        } else {
            // Una expresión de asignación cubre literales, `C.class`, nombres de constante y las
            // constantes aritméticas — todo lo que un valor de anotación puede ser.
            Ok(AnnotationValue::Expr(Box::new(self.expr()?)))
        }
    }

    /// Salta una anotación **sin retenerla** — solo donde todavía no la colgamos de nada (parámetros
    /// de tipo, constantes de `enum`).
    fn skip_annotation(&mut self) -> Result<()> {
        self.annotation()?;
        Ok(())
    }

    /// Consume un par balanceado `open`…`close` (para saltar el cuerpo de una anotación).
    fn skip_balanced(&mut self, open: TokenKind, close: TokenKind) -> Result<()> {
        self.expect(open)?;
        let mut depth = 1;
        while depth > 0 {
            match self.peek_kind() {
                TokenKind::Eof => return Err(self.error("paréntesis sin cerrar")),
                k if k == open => depth += 1,
                k if k == close => depth -= 1,
                _ => {}
            }
            self.bump();
        }
        Ok(())
    }

    // ---- clase / interfaz ----

    fn class_decl(&mut self, modifiers: Vec<Modifier>, annotations: Vec<Annotation>) -> Result<ClassDecl> {
        use TokenKind as T;
        let pos = self.pos();
        // `record`/`@interface` son formas contextuales; `record` exige `record Nombre(` para
        // no confundirse con un campo de tipo `record`.
        let kind = if self.at(T::MonkeysAt) && self.kind_at(1) == T::Interface {
            self.bump();
            self.bump();
            TypeKind::Annotation
        } else if self.eat(T::Interface) {
            TypeKind::Interface
        } else if self.eat(T::Enum) {
            TypeKind::Enum
        } else if self.is_record_decl() {
            self.bump();
            TypeKind::Record
        } else {
            self.expect(T::Class)?;
            TypeKind::Class
        };
        let name = self.expect_ident()?;
        let type_params = self.type_params()?; // parámetros de tipo genéricos, si los hay

        // Componentes de un record: `record Name(comp, comp)`.
        let components = if kind == TypeKind::Record { self.params()? } else { Vec::new() };

        let mut extends = None;
        let mut implements = Vec::new();
        if kind != TypeKind::Record && self.eat(T::Extends) {
            let first = self.parse_type()?;
            if kind == TypeKind::Interface {
                implements.push(first);
                while self.eat(T::Comma) {
                    implements.push(self.parse_type()?);
                }
            } else {
                extends = Some(first);
            }
        }
        if self.eat(T::Implements) {
            implements.push(self.parse_type()?);
            while self.eat(T::Comma) {
                implements.push(self.parse_type()?);
            }
        }

        self.expect(T::LBrace)?;
        // Un `enum` lleva sus constantes antes de los miembros.
        let enum_constants = if kind == TypeKind::Enum { self.enum_constants()? } else { Vec::new() };
        let mut members = Vec::new();
        while !self.at(T::RBrace) && !self.at(T::Eof) {
            self.member(&name, &mut members)?;
        }
        self.expect(T::RBrace)?;
        Ok(ClassDecl { pos, annotations, modifiers, kind, name, type_params, components, extends, implements, enum_constants, members })
    }

    /// ¿Estamos ante `record Nombre(`? (`record` es keyword contextual — un identificador).
    fn is_record_decl(&self) -> bool {
        self.at(TokenKind::Identifier)
            && self.peek().text == "record"
            && self.kind_at(1) == TokenKind::Identifier
            && self.kind_at(2) == TokenKind::LParen
    }

    /// ¿El token actual inicia una **declaración de tipo** (para un tipo anidado)?
    fn at_type_decl(&self) -> bool {
        use TokenKind as T;
        self.at(T::Class)
            || self.at(T::Interface)
            || self.at(T::Enum)
            || self.is_record_decl()
            || (self.at(T::MonkeysAt) && self.kind_at(1) == T::Interface)
    }

    /// `<T, U extends Number & Comparable<U>>` — los **parámetros de tipo** con sus cotas
    /// (JLS §4.4). Vacío si no hay `<`.
    fn type_params(&mut self) -> Result<Vec<TypeParam>> {
        use TokenKind as T;
        let mut params = Vec::new();
        if !self.at(T::Lt) {
            return Ok(params);
        }
        self.bump(); // <
        loop {
            while self.at(T::MonkeysAt) {
                self.skip_annotation()?;
            }
            let name = self.expect_ident()?;
            // `extends A & B` — la primera cota puede ser una clase; las demás, interfaces.
            let mut bounds = Vec::new();
            if self.eat(T::Extends) {
                bounds.push(self.parse_type()?);
                while self.eat(T::Amp) {
                    bounds.push(self.parse_type()?);
                }
            }
            params.push(TypeParam { name, bounds });
            if self.eat(T::Comma) {
                continue;
            }
            if self.eat_gt() {
                break;
            }
            return Err(self.error("se esperaba `,` o `>` en los parámetros de tipo"));
        }
        Ok(params)
    }

    /// `<String, ? extends Number>` — los **argumentos de tipo** de un tipo parametrizado
    /// (JLS §4.5.1). `None` si no hay `<`; `Some(vec![])` es el **diamante** `<>`, que no es lo
    /// mismo que un tipo crudo: pide inferir (§15.9.1).
    fn type_args(&mut self) -> Result<Option<Vec<TypeArg>>> {
        use TokenKind as T;
        if !self.at(T::Lt) {
            return Ok(None);
        }
        self.bump(); // <
        let mut args = Vec::new();
        if self.eat_gt() {
            return Ok(Some(args)); // diamante `<>`
        }
        loop {
            args.push(self.type_arg()?);
            if self.eat(T::Comma) {
                continue;
            }
            if self.eat_gt() {
                break;
            }
            return Err(self.error("se esperaba `,` o `>` en los argumentos de tipo"));
        }
        Ok(Some(args))
    }

    /// Un argumento de tipo: un tipo, o un *wildcard* (`?`, `? extends T`, `? super T`).
    fn type_arg(&mut self) -> Result<TypeArg> {
        use TokenKind as T;
        if self.eat(T::Ques) {
            if self.eat(T::Extends) {
                return Ok(TypeArg::Extends(Box::new(self.parse_type()?)));
            }
            if self.eat(T::Super) {
                return Ok(TypeArg::Super(Box::new(self.parse_type()?)));
            }
            return Ok(TypeArg::Wildcard);
        }
        Ok(TypeArg::Type(self.parse_type()?))
    }

    /// Las constantes de un `enum` al inicio de su cuerpo: `A, B(args), C { body } ;`.
    fn enum_constants(&mut self) -> Result<Vec<EnumConstant>> {
        use TokenKind as T;
        let mut constants = Vec::new();
        if self.at(T::Semi) || self.at(T::RBrace) {
            self.eat(T::Semi);
            return Ok(constants);
        }
        loop {
            while self.at(T::MonkeysAt) {
                self.skip_annotation()?;
            }
            let name = self.expect_ident()?;
            let args = if self.at(T::LParen) { self.args()? } else { Vec::new() };
            // Cuerpo de clase por constante: se salta por ahora.
            if self.at(T::LBrace) {
                self.skip_balanced(T::LBrace, T::RBrace)?;
            }
            constants.push(EnumConstant { name, args });
            if !self.eat(T::Comma) || self.at(T::Semi) || self.at(T::RBrace) {
                break;
            }
        }
        self.eat(T::Semi); // separador opcional entre constantes y miembros
        Ok(constants)
    }

    fn member(&mut self, class_name: &str, members: &mut Vec<Member>) -> Result<()> {
        let (modifiers, annotations) = self.modifiers()?;
        let pos = self.pos();

        if self.eat(TokenKind::Semi) {
            return Ok(()); // `;` suelto
        }
        if self.at(TokenKind::LBrace) {
            // Bloque de inicialización: **estático** (§8.7) o de **instancia** (§8.6). Los dos se
            // conservan — descartarlos perdía el código en silencio.
            let block = self.block()?;
            members.push(if modifiers.contains(&Modifier::Static) {
                Member::StaticInit(block)
            } else {
                Member::InstanceInit(block)
            });
            return Ok(());
        }
        if self.at_type_decl() {
            let nested = self.class_decl(modifiers, annotations)?;
            members.push(Member::Type(nested));
            return Ok(());
        }

        // Parámetros de tipo de un método/constructor genérico (`<T> ...`).
        let type_params = self.type_params()?;

        // Constructor: `Nombre(` con Nombre == la clase.
        if self.at(TokenKind::Identifier)
            && self.peek().text == class_name
            && self.kind_at(1) == TokenKind::LParen
        {
            self.bump(); // nombre
            let params = self.params()?;
            let throws = self.throws_clause()?;
            let body = self.method_body()?;
            members.push(Member::Method(MethodDecl {
                pos,
                annotations,
                modifiers,
                type_params,
                return_type: Type::Void,
                name: class_name.to_string(),
                params,
                throws,
                body,
                is_constructor: true,
            }));
            return Ok(());
        }

        let ty = self.parse_type()?;
        let name = self.expect_ident()?;

        if self.at(TokenKind::LParen) {
            let params = self.params()?;
            let throws = self.throws_clause()?;
            // Elemento de `@interface` con valor por defecto: `Tipo nombre() default valor;`.
            if self.eat(TokenKind::Default) {
                let _ = self.expr()?;
            }
            let body = self.method_body()?;
            members.push(Member::Method(MethodDecl {
                pos,
                annotations,
                modifiers,
                type_params,
                return_type: ty,
                name,
                params,
                throws,
                body,
                is_constructor: false,
            }));
        } else {
            // Campo(s): uno o más declaradores separados por coma.
            let declare = |p: &mut Self, nm: String| -> Result<FieldDecl> {
                // Igual que en un local: los `[]` post-nombre y el inicializador de array. Las
                // anotaciones se replican a cada declarador (`@Foo int a, b;` anota los dos).
                let fty = p.extra_array_dims(ty.clone())?;
                let init = if p.eat(TokenKind::Eq) { Some(p.var_init(&fty)?) } else { None };
                Ok(FieldDecl { pos, annotations: annotations.clone(), modifiers: modifiers.clone(), ty: fty, name: nm, init })
            };
            let first = declare(self, name)?;
            members.push(Member::Field(first));
            while self.eat(TokenKind::Comma) {
                let nm = self.expect_ident()?;
                let f = declare(self, nm)?;
                members.push(Member::Field(f));
            }
            self.expect(TokenKind::Semi)?;
        }
        Ok(())
    }

    /// El cuerpo de un método: un bloque `{...}`, o `;` para `abstract`/`native`.
    fn method_body(&mut self) -> Result<Option<Block>> {
        if self.at(TokenKind::LBrace) {
            Ok(Some(self.block()?))
        } else {
            self.expect(TokenKind::Semi)?;
            Ok(None)
        }
    }

    /// La cláusula `throws E1, E2, …` (§8.4.6); vacía si no está. Ya **no se descarta**: el chequeo
    /// de excepciones chequeadas la necesita.
    fn throws_clause(&mut self) -> Result<Vec<Type>> {
        let mut types = Vec::new();
        if self.eat(TokenKind::Throws) {
            types.push(self.parse_type()?);
            while self.eat(TokenKind::Comma) {
                types.push(self.parse_type()?);
            }
        }
        Ok(types)
    }

    fn params(&mut self) -> Result<Vec<Param>> {
        self.expect(TokenKind::LParen)?;
        let mut params = Vec::new();
        if !self.at(TokenKind::RParen) {
            loop {
                // Un parámetro puede llevar anotaciones y/o `final`, en cualquier orden.
                let mut annotations = Vec::new();
                let mut is_final = false;
                loop {
                    if self.at(TokenKind::MonkeysAt) {
                        annotations.push(self.annotation()?);
                    } else if self.eat(TokenKind::Final) {
                        is_final = true;
                    } else {
                        break;
                    }
                }
                let mut ty = self.parse_type()?;
                // El tipo declarado de un varargs **es el array** (JLS §8.4.1): `int... xs` es un
                // `int[]` dentro del método, y su descriptor es `[I`. El flag solo recuerda que
                // se escribió con `...` (para el overload resolution y `ACC_VARARGS`).
                let varargs = self.eat(TokenKind::Ellipsis);
                if varargs {
                    ty = Type::Array(Box::new(ty));
                }
                let name = self.expect_ident()?;
                // `int a[]` — dimensiones de array después del nombre.
                while self.at(TokenKind::LBracket) && self.kind_at(1) == TokenKind::RBracket {
                    self.bump();
                    self.bump();
                    ty = Type::Array(Box::new(ty));
                }
                params.push(Param { annotations, ty, name, varargs, is_final });
                if !self.eat(TokenKind::Comma) {
                    break;
                }
            }
        }
        self.expect(TokenKind::RParen)?;
        Ok(params)
    }

    // ---- tipos ----

    fn parse_type(&mut self) -> Result<Type> {
        let mut base = self.base_type()?;
        while self.at(TokenKind::LBracket) && self.kind_at(1) == TokenKind::RBracket {
            self.bump();
            self.bump();
            base = Type::Array(Box::new(base));
        }
        Ok(base)
    }

    fn base_type(&mut self) -> Result<Type> {
        use TokenKind as T;
        let ty = match self.peek_kind() {
            T::Int => Type::Prim(PrimType::Int),
            T::Long => Type::Prim(PrimType::Long),
            T::Short => Type::Prim(PrimType::Short),
            T::Byte => Type::Prim(PrimType::Byte),
            T::Char => Type::Prim(PrimType::Char),
            T::Boolean => Type::Prim(PrimType::Boolean),
            T::Float => Type::Prim(PrimType::Float),
            T::Double => Type::Prim(PrimType::Double),
            T::Void => Type::Void,
            T::Identifier => {
                if self.peek().text == "var" {
                    self.bump();
                    return Ok(Type::Var);
                }
                let name = self.qualified_name()?;
                // Con argumentos es un tipo parametrizado; sin `<`, uno crudo.
                return Ok(match self.type_args()? {
                    Some(args) => Type::Parameterized { base: name, args },
                    None => Type::Class(name),
                });
            }
            other => return Err(self.error(format!("se esperaba un tipo, se encontró {other:?}"))),
        };
        self.bump();
        Ok(ty)
    }

    fn qualified_name(&mut self) -> Result<String> {
        let mut name = self.expect_ident()?;
        while self.at(TokenKind::Dot) && self.kind_at(1) == TokenKind::Identifier {
            self.bump();
            name.push('.');
            name.push_str(&self.expect_ident()?);
        }
        Ok(name)
    }

    // ---- sentencias ----

    fn block(&mut self) -> Result<Block> {
        self.expect(TokenKind::LBrace)?;
        let mut stmts = Vec::new();
        while !self.at(TokenKind::RBrace) && !self.at(TokenKind::Eof) {
            self.block_stmt(&mut stmts)?;
        }
        self.expect(TokenKind::RBrace)?;
        Ok(Block(stmts))
    }

    /// Una entrada de bloque: una declaración local (posiblemente varios declaradores) o una
    /// sentencia normal. Reutilizado por el cuerpo `:` de un `switch`.
    fn block_stmt(&mut self, out: &mut Vec<Stmt>) -> Result<()> {
        // Una **clase local** (§14.3) se reconoce antes que la declaración de variable, porque su
        // detección mira más allá de los modificadores (`final class C` no es un local `final`).
        if self.local_class_ahead() {
            let pos = self.pos();
            let (modifiers, annotations) = self.modifiers()?;
            let decl = self.class_decl(modifiers, annotations)?;
            out.push(Stmt::new(pos, StmtKind::LocalClass(decl)));
        } else if let Some(decls) = self.try_local_decls()? {
            self.expect(TokenKind::Semi)?;
            out.extend(decls);
        } else {
            out.push(self.statement()?);
        }
        Ok(())
    }

    /// ¿Lo que viene es una declaración de tipo (con sus modificadores/anotaciones)? Se prueba
    /// consumiendo los modificadores —que reusa la misma lógica que un miembro— y mirando si detrás
    /// queda un `class`/`interface`/`enum`/`record`; después restaura la posición. `final int x` no
    /// matchea: tras el `final` no hay palabra clave de tipo, y cae en la declaración de variable.
    fn local_class_ahead(&mut self) -> bool {
        let save = self.mark();
        let is_decl = self.modifiers().is_ok() && self.at_type_decl();
        self.reset(save);
        is_decl
    }

    /// Intenta parsear una declaración local `Type name [= init] (, name [= init])*` **sin**
    /// consumir el `;` final. Hace *backtracking*: si lo que sigue no es una declaración
    /// (p.ej. `x = 5`, `foo()`), restaura la posición y devuelve `None`.
    fn try_local_decls(&mut self) -> Result<Option<Vec<Stmt>>> {
        let save = self.mark();
        // Un local puede declararse `final` (JLS §14.4); es el único modificador que admite.
        let is_final = self.eat(TokenKind::Final);
        let Ok(ty) = self.parse_type() else {
            self.reset(save);
            return Ok(None);
        };
        if !self.at(TokenKind::Identifier) {
            self.reset(save);
            return Ok(None);
        }
        let mut decls = Vec::new();
        loop {
            let pos = self.pos();
            let name = self.expect_ident()?;
            // Declarador **al estilo C**: `int y[]` es lo mismo que `int[] y` (§10.2), y los `[]`
            // van por variable, no por declaración: en `int a[], b;` solo `a` es array.
            let vty = self.extra_array_dims(ty.clone())?;
            let init = if self.eat(TokenKind::Eq) { Some(self.var_init(&vty)?) } else { None };
            decls.push(Stmt::new(pos, StmtKind::LocalVar { ty: vty, name, init, is_final }));
            if !self.eat(TokenKind::Comma) {
                break;
            }
        }
        Ok(Some(decls))
    }

    /// Una sentencia, con su **posición** adjunta: parsea la forma (`statement_kind`) y la
    /// envuelve una sola vez. La decoración (`local`) la agrega después la pasada 2.
    fn statement(&mut self) -> Result<Stmt> {
        let pos = self.pos();
        let kind = self.statement_kind()?;
        Ok(Stmt::new(pos, kind))
    }

    fn statement_kind(&mut self) -> Result<StmtKind> {
        use TokenKind as T;
        match self.peek_kind() {
            T::LBrace => Ok(StmtKind::Block(self.block()?)),
            T::If => {
                self.bump();
                self.expect(T::LParen)?;
                let cond = self.expr()?;
                self.expect(T::RParen)?;
                let then = Box::new(self.statement()?);
                let els = if self.eat(T::Else) { Some(Box::new(self.statement()?)) } else { None };
                Ok(StmtKind::If { cond, then, els })
            }
            T::While => {
                self.bump();
                self.expect(T::LParen)?;
                let cond = self.expr()?;
                self.expect(T::RParen)?;
                let body = Box::new(self.statement()?);
                Ok(StmtKind::While { cond, body })
            }
            T::For => self.for_stmt(),
            T::Return => {
                self.bump();
                let e = if self.at(T::Semi) { None } else { Some(self.expr()?) };
                self.expect(T::Semi)?;
                Ok(StmtKind::Return(e))
            }
            T::Break => {
                self.bump();
                let label = self.optional_label();
                self.expect(T::Semi)?;
                Ok(StmtKind::Break(label))
            }
            T::Continue => {
                self.bump();
                let label = self.optional_label();
                self.expect(T::Semi)?;
                Ok(StmtKind::Continue(label))
            }
            // Sentencia etiquetada `label: stmt` — un identificador seguido de `:` en posición de
            // sentencia (los `:` de `case`/`default` y del ternario no llegan acá).
            T::Identifier if self.kind_at(1) == T::Colon => {
                let label = self.peek().text.clone();
                self.bump(); // identificador
                self.bump(); // ':'
                let body = self.statement()?;
                Ok(StmtKind::Labeled { label, body: Box::new(body) })
            }
            T::Throw => {
                self.bump();
                let e = self.expr()?;
                self.expect(T::Semi)?;
                Ok(StmtKind::Throw(e))
            }
            T::Synchronized => {
                self.bump();
                self.expect(T::LParen)?;
                let lock = self.expr()?;
                self.expect(T::RParen)?;
                let body = self.block()?;
                Ok(StmtKind::Synchronized { lock, body })
            }
            T::Do => {
                self.bump();
                let body = Box::new(self.statement()?);
                self.expect(T::While)?;
                self.expect(T::LParen)?;
                let cond = self.expr()?;
                self.expect(T::RParen)?;
                self.expect(T::Semi)?;
                Ok(StmtKind::Do { body, cond })
            }
            T::Assert => {
                self.bump();
                let cond = self.expr()?;
                let message = if self.eat(T::Colon) { Some(self.expr()?) } else { None };
                self.expect(T::Semi)?;
                Ok(StmtKind::Assert { cond, message })
            }
            T::Try => self.try_stmt(),
            T::Switch => self.switch_stmt(),
            T::Semi => {
                self.bump();
                Ok(StmtKind::Empty)
            }
            // `yield expr;` — keyword contextual: solo cuando lo que sigue no lo hace operando.
            T::Identifier if self.peek().text == "yield" && self.is_yield_start() => {
                self.bump();
                let e = self.expr()?;
                self.expect(T::Semi)?;
                Ok(StmtKind::Yield(e))
            }
            _ => {
                let e = self.expr()?;
                self.expect(T::Semi)?;
                Ok(StmtKind::Expr(e))
            }
        }
    }

    fn for_stmt(&mut self) -> Result<StmtKind> {
        self.expect(TokenKind::For)?;
        self.expect(TokenKind::LParen)?;

        // ¿for-each?  `for ([final] Type name : iterable)`
        let save = self.mark();
        let is_final = self.eat(TokenKind::Final);
        if let Ok(ty) = self.parse_type() {
            if self.at(TokenKind::Identifier) && self.kind_at(1) == TokenKind::Colon {
                let name = self.expect_ident()?;
                self.expect(TokenKind::Colon)?;
                let iterable = self.expr()?;
                self.expect(TokenKind::RParen)?;
                let body = Box::new(self.statement()?);
                return Ok(StmtKind::ForEach { ty, name, iterable, body, is_final });
            }
        }
        self.reset(save);

        // for clásico.
        let init_pos = self.pos();
        let init = if self.at(TokenKind::Semi) {
            None
        } else if let Some(decls) = self.try_local_decls()? {
            Some(Box::new(if decls.len() == 1 {
                decls.into_iter().next().unwrap()
            } else {
                Stmt::new(init_pos, StmtKind::Block(Block(decls)))
            }))
        } else {
            Some(Box::new(Stmt::new(init_pos, StmtKind::Expr(self.expr()?))))
        };
        self.expect(TokenKind::Semi)?;

        let cond = if self.at(TokenKind::Semi) { None } else { Some(self.expr()?) };
        self.expect(TokenKind::Semi)?;

        let mut update = Vec::new();
        if !self.at(TokenKind::RParen) {
            update.push(self.expr()?);
            while self.eat(TokenKind::Comma) {
                update.push(self.expr()?);
            }
        }
        self.expect(TokenKind::RParen)?;
        let body = Box::new(self.statement()?);
        Ok(StmtKind::For { init, cond, update, body })
    }

    fn try_stmt(&mut self) -> Result<StmtKind> {
        self.expect(TokenKind::Try)?;
        let resources = if self.at(TokenKind::LParen) {
            self.try_resources()?
        } else {
            Vec::new()
        };
        let body = self.block()?;

        let mut catches = Vec::new();
        while self.at(TokenKind::Catch) {
            self.bump();
            self.expect(TokenKind::LParen)?;
            let declared_final = self.eat(TokenKind::Final);
            // Multi-catch: `A | B | C name`.
            let mut types = vec![self.parse_type()?];
            while self.eat(TokenKind::Bar) {
                types.push(self.parse_type()?);
            }
            let name = self.expect_ident()?;
            self.expect(TokenKind::RParen)?;
            let body = self.block()?;
            // La variable de un *multi-catch* es **implícitamente** `final` (JLS §14.20).
            let is_final = declared_final || types.len() > 1;
            catches.push(CatchClause { types, name, body, slot: None, is_final });
        }

        let finally = if self.eat(TokenKind::Finally) { Some(self.block()?) } else { None };
        Ok(StmtKind::Try { resources, body, catches, finally })
    }

    /// *try-with-resources*: `( recurso ; recurso )` — cada recurso es un `Type name = expr`
    /// (o una variable existente). Se modelan como declaraciones locales.
    fn try_resources(&mut self) -> Result<Vec<Stmt>> {
        self.expect(TokenKind::LParen)?;
        let mut resources = Vec::new();
        while !self.at(TokenKind::RParen) {
            // El `final` de un recurso lo consume `try_local_decls` (los recursos son locales).
            if let Some(decls) = self.try_local_decls()? {
                resources.extend(decls);
            } else {
                let pos = self.pos();
                resources.push(Stmt::new(pos, StmtKind::Expr(self.expr()?)));
            }
            if !self.eat(TokenKind::Semi) {
                break;
            }
        }
        self.expect(TokenKind::RParen)?;
        Ok(resources)
    }

    fn switch_stmt(&mut self) -> Result<StmtKind> {
        self.expect(TokenKind::Switch)?;
        self.expect(TokenKind::LParen)?;
        let selector = self.expr()?;
        self.expect(TokenKind::RParen)?;
        let cases = self.switch_body()?;
        Ok(StmtKind::Switch { selector, cases })
    }

    /// El cuerpo `{ ... }` de un `switch`, compartido por la sentencia y la expresión.
    /// Soporta forma flecha (`case X ->`) y dos puntos (`case X:` con apilado y *fall-through*),
    /// multi-etiqueta, `default`, `null`, *type patterns* y `guard` (`when`).
    fn switch_body(&mut self) -> Result<Vec<SwitchCase>> {
        use TokenKind as T;
        self.expect(T::LBrace)?;
        let mut cases = Vec::new();
        while !self.at(T::RBrace) && !self.at(T::Eof) {
            let mut labels = Vec::new();
            let mut is_default = false;
            let mut guard = None;
            if self.eat(T::Default) {
                is_default = true;
            } else {
                self.expect(T::Case)?;
                self.case_labels(&mut labels, &mut is_default)?;
                if self.at(T::Identifier) && self.peek().text == "when" {
                    self.bump();
                    guard = Some(self.expr()?);
                }
            }
            if self.eat(T::Arrow) {
                let body = self.arrow_body()?;
                cases.push(SwitchCase { labels, is_default, guard, body: SwitchBody::Arrow(Box::new(body)) });
            } else {
                self.expect(T::Colon)?;
                // Apilar más `case:`/`default:` para el mismo grupo.
                loop {
                    if self.eat(T::Case) {
                        self.case_labels(&mut labels, &mut is_default)?;
                        self.expect(T::Colon)?;
                    } else if self.eat(T::Default) {
                        is_default = true;
                        self.expect(T::Colon)?;
                    } else {
                        break;
                    }
                }
                let mut stmts = Vec::new();
                while !self.at(T::Case) && !self.at(T::Default) && !self.at(T::RBrace) && !self.at(T::Eof) {
                    self.block_stmt(&mut stmts)?;
                }
                cases.push(SwitchCase { labels, is_default, guard, body: SwitchBody::Colon(stmts) });
            }
        }
        self.expect(T::RBrace)?;
        Ok(cases)
    }

    fn case_labels(&mut self, labels: &mut Vec<CaseLabel>, is_default: &mut bool) -> Result<()> {
        use TokenKind as T;
        loop {
            if self.eat(T::Null) {
                labels.push(CaseLabel::Null);
            } else if self.eat(T::Default) {
                *is_default = true; // `case null, default`
            } else if let Some(pat) = self.try_case_pattern()? {
                labels.push(pat);
            } else {
                labels.push(CaseLabel::Constant(self.expr()?));
            }
            if !self.eat(T::Comma) {
                break;
            }
        }
        Ok(())
    }

    /// Un patrón en posición de etiqueta, con *backtracking*: si lo que sigue no es un patrón bien
    /// formado seguido de `->`/`:`/`,`/`when`, restaura y se parsea como constante.
    fn try_case_pattern(&mut self) -> Result<Option<CaseLabel>> {
        use TokenKind as T;
        let save = self.mark();
        let Some(p) = self.try_pattern()? else {
            self.reset(save);
            return Ok(None);
        };
        if matches!(self.peek_kind(), T::Arrow | T::Colon | T::Comma)
            || (self.at(T::Identifier) && self.peek().text == "when")
        {
            return Ok(Some(CaseLabel::Pattern(p)));
        }
        self.reset(save);
        Ok(None)
    }

    /// Un patrón: `Type name` (*type pattern*) o `Rec(p1, p2, …)` (deconstrucción de un `record`,
    /// §14.30.1). Recursivo: los componentes son patrones y pueden volver a deconstruir.
    fn try_pattern(&mut self) -> Result<Option<Pattern>> {
        use TokenKind as T;
        let save = self.mark();
        let Ok(ty) = self.parse_type() else {
            self.reset(save);
            return Ok(None);
        };
        // `Rec( … )` — deconstrucción.
        if self.eat(T::LParen) {
            let mut components = Vec::new();
            if !self.at(T::RParen) {
                loop {
                    let Some(p) = self.try_pattern()? else {
                        self.reset(save);
                        return Ok(None);
                    };
                    components.push(p);
                    if !self.eat(T::Comma) {
                        break;
                    }
                }
            }
            if !self.eat(T::RParen) {
                self.reset(save);
                return Ok(None);
            }
            return Ok(Some(Pattern::Record { ty, components }));
        }
        // `Type name` — el `when` de una guarda no es el nombre del patrón.
        if self.at(T::Identifier) && self.peek().text != "when" {
            let name = self.bump().text;
            return Ok(Some(Pattern::Type { ty, name, slot: None }));
        }
        self.reset(save);
        Ok(None)
    }

    /// El cuerpo de un brazo `->`: un bloque, un `throw`, o una expresión terminada en `;`.
    fn arrow_body(&mut self) -> Result<Stmt> {
        let pos = self.pos();
        match self.peek_kind() {
            TokenKind::LBrace => Ok(Stmt::new(pos, StmtKind::Block(self.block()?))),
            TokenKind::Throw => self.statement(),
            _ => {
                let e = self.expr()?;
                self.expect(TokenKind::Semi)?;
                Ok(Stmt::new(pos, StmtKind::Expr(e)))
            }
        }
    }

    /// `yield` es contextual: cuenta como sentencia salvo que lo que sigue lo vuelva un
    /// Reconstruye el nombre de un tipo a partir de la expresión con que se parseó (`C`, `a.b.C`).
    /// Lo necesita `C.class`, que sintácticamente llega como una cadena de accesos.
    fn type_name_of(e: &Expr) -> Option<String> {
        match &e.kind {
            ExprKind::Name(n) => Some(n.clone()),
            ExprKind::Field { expr, name } => Some(format!("{}.{name}", Self::type_name_of(expr)?)),
            _ => None,
        }
    }

    /// Los `[]` que pueden seguir al **nombre** de una variable (`int y[]`, §10.2): envuelven su tipo
    /// una vez por cada par.
    fn extra_array_dims(&mut self, mut ty: Type) -> Result<Type> {
        while self.at(TokenKind::LBracket) && self.kind_at(1) == TokenKind::RBracket {
            self.bump();
            self.bump();
            ty = Type::Array(Box::new(ty));
        }
        Ok(ty)
    }

    /// El inicializador de una variable: una expresión, o un **inicializador de array** `{a, b, c}`
    /// (§10.6) — que es azúcar de `new T[]{a, b, c}` y necesita el tipo declarado para saber el
    /// elemento. Anida: `int[][] m = {{1,2},{3}}`.
    fn var_init(&mut self, ty: &Type) -> Result<Expr> {
        if !self.at(TokenKind::LBrace) {
            return self.expr();
        }
        let pos = self.pos();
        let elem = match ty {
            Type::Array(inner) => (**inner).clone(),
            other => other.clone(), // mal tipado; que lo reporte la pasada 2
        };
        self.expect(TokenKind::LBrace)?;
        let mut items = Vec::new();
        while !self.at(TokenKind::RBrace) && !self.at(TokenKind::Eof) {
            items.push(self.var_init(&elem)?);
            if !self.eat(TokenKind::Comma) {
                break;
            }
        }
        self.expect(TokenKind::RBrace)?;
        Ok(Expr::new(
            pos,
            ExprKind::NewArray { elem, dims: vec![None], init: Some(items) },
        ))
    }

    /// Una etiqueta opcional tras `break`/`continue` (`break L;` / `continue L;`).
    fn optional_label(&mut self) -> Option<String> {
        if self.at(TokenKind::Identifier) {
            let l = self.peek().text.clone();
            self.bump();
            Some(l)
        } else {
            None
        }
    }

    /// operando/identificador (asignación, `.`, llamada, índice, `++`…).
    fn is_yield_start(&self) -> bool {
        use TokenKind as T;
        !matches!(
            self.kind_at(1),
            T::Eq | T::Dot | T::ColCol | T::LParen | T::LBracket | T::Semi | T::Comma
                | T::PlusEq | T::SubEq | T::StarEq | T::SlashEq | T::PercentEq
                | T::AmpEq | T::BarEq | T::CaretEq | T::LtLtEq | T::GtGtEq | T::GtGtGtEq
                | T::PlusPlus | T::SubSub
        )
    }

    // ---- expresiones ----

    fn expr(&mut self) -> Result<Expr> {
        self.assignment()
    }

    fn assignment(&mut self) -> Result<Expr> {
        // Una lambda es una *assignment-expression* (§15.27), así que se decide **acá arriba**,
        // antes de la cascada de precedencia: su cuerpo necesita una expresión completa.
        if let Some(lambda) = self.try_lambda()? {
            return Ok(lambda);
        }
        let lhs = self.ternary()?;
        if let Some(op) = assign_op(self.peek_kind()) {
            self.bump();
            let value = self.assignment()?; // asociativo a derecha
            // La expresión compuesta arranca donde arranca su operando izquierdo.
            let pos = lhs.pos;
            return Ok(Expr::new(pos, ExprKind::Assign { op, target: Box::new(lhs), value: Box::new(value) }));
        }
        Ok(lhs)
    }

    // ==================== LAMBDAS (§15.27) ====================
    //
    // El nudo del asunto es **decidir** si un `(` abre una lambda, un *cast* o un paréntesis. Se
    // resuelve con un solo predicado de *lookahead* ([`lambda_ahead`]); una vez decidido, el resto
    // es *recursive descent* directo. La forma sin paréntesis (`x -> …`) no tiene ambigüedad: un
    // identificador seguido de `->` no es ninguna otra cosa.

    /// Si lo que viene es una lambda, la parsea; si no, no consume nada.
    fn try_lambda(&mut self) -> Result<Option<Expr>> {
        use TokenKind as T;
        let is_lambda = match self.peek_kind() {
            // `x -> …`: un identificador con una flecha detrás.
            T::Identifier => self.kind_at(1) == T::Arrow,
            // `(…) -> …`: hay que mirar más allá del paréntesis balanceado.
            T::LParen => self.lambda_ahead(),
            _ => false,
        };
        if !is_lambda {
            return Ok(None);
        }
        Ok(Some(self.parse_lambda()?))
    }

    /// **El desambiguador.** Asume que el token actual es `(` y responde: ¿es la lista de parámetros
    /// de una lambda? Lo es *sii* el token que sigue al `)` que la cierra es una flecha `->`.
    ///
    /// Es un barrido **no destructivo** (no toca el cursor ni el *undo log*): balancea los tres
    /// pares de delimitadores para saltar el interior —un tipo genérico o un array en un parámetro
    /// no traen `(`, pero un paréntesis cualquiera sí— hasta el `)` de profundidad cero, y espía el
    /// siguiente. Es la misma señal que usa javac (`analyzeParens`): la flecha, y nada más, delata a
    /// la lambda.
    fn lambda_ahead(&self) -> bool {
        use TokenKind as T;
        let mut depth = 0usize;
        let mut i = self.pos;
        while let Some(tok) = self.tokens.get(i) {
            match tok.kind {
                T::LParen | T::LBracket | T::LBrace => depth += 1,
                T::RParen | T::RBracket | T::RBrace => {
                    depth -= 1;
                    if depth == 0 {
                        return self.tokens.get(i + 1).map(|t| t.kind) == Some(T::Arrow);
                    }
                }
                _ => {}
            }
            i += 1;
        }
        false
    }

    /// Parsea una lambda **ya confirmada** por [`try_lambda`]: como sabemos que hay una flecha, los
    /// parámetros se pueden leer sin *backtracking*.
    fn parse_lambda(&mut self) -> Result<Expr> {
        use TokenKind as T;
        let pos = self.pos();
        let params = if self.at(T::Identifier) {
            // `x -> …`: un único parámetro **de tipo inferido** (sin paréntesis).
            let name = self.bump().text;
            vec![Param { annotations: Vec::new(), ty: Type::Var, name, varargs: false, is_final: false }]
        } else {
            self.expect(T::LParen)?;
            let mut params = Vec::new();
            if !self.at(T::RParen) {
                loop {
                    params.push(self.lambda_param()?);
                    if !self.eat(T::Comma) {
                        break;
                    }
                }
            }
            self.expect(T::RParen)?;
            params
        };
        self.expect(T::Arrow)?;
        // Cuerpo: un bloque `{ … }` o una expresión suelta (§15.27.2).
        let body = if self.at(T::LBrace) {
            LambdaBody::Block(self.block()?)
        } else {
            LambdaBody::Expr(Box::new(self.expr()?))
        };
        Ok(Expr::new(pos, ExprKind::Lambda { params, body: Box::new(body) }))
    }

    /// Un parámetro de una lista con paréntesis. Puede venir **implícito** (`x`, tipo inferido) o
    /// **explícito** (`int x`, `var x`, `final String s`) — pero **no** mezclado, cosa que chequea
    /// la semántica, no el parser. Se distingue por lo que sigue al identificador: si es `,` o `)`,
    /// era el nombre pelado; si no, había un tipo delante.
    fn lambda_param(&mut self) -> Result<Param> {
        use TokenKind as T;
        let is_final = self.eat(T::Final);
        if self.at(T::Identifier) && matches!(self.kind_at(1), T::Comma | T::RParen) {
            let name = self.bump().text;
            return Ok(Param { annotations: Vec::new(), ty: Type::Var, name, varargs: false, is_final });
        }
        let ty = self.parse_type()?;
        let name = self.expect_ident()?;
        Ok(Param { annotations: Vec::new(), ty, name, varargs: false, is_final })
    }

    fn ternary(&mut self) -> Result<Expr> {
        let cond = self.binary(3)?;
        if self.eat(TokenKind::Ques) {
            let then = self.expr()?;
            self.expect(TokenKind::Colon)?;
            let els = self.expr()?;
            let pos = cond.pos;
            return Ok(Expr::new(
                pos,
                ExprKind::Ternary { cond: Box::new(cond), then: Box::new(then), els: Box::new(els) },
            ));
        }
        Ok(cond)
    }

    fn binary(&mut self, min_prec: u8) -> Result<Expr> {
        let mut lhs = self.unary()?;
        loop {
            // `instanceof` a precedencia relacional (9).
            if self.at(TokenKind::Instanceof) && 9 >= min_prec {
                self.bump();
                let ty = self.parse_type()?;
                // `e instanceof T v` — *pattern* (§14.30.2): el identificador que sigue bindea.
                let binding = if self.at(TokenKind::Identifier) {
                    Some(self.bump().text)
                } else {
                    None
                };
                let pos = lhs.pos;
                lhs = Expr::new(
                    pos,
                    ExprKind::InstanceOf { expr: Box::new(lhs), ty, binding, slot: None },
                );
                continue;
            }
            let Some((op, prec)) = bin_op(self.peek_kind()) else { break };
            if prec < min_prec {
                break;
            }
            self.bump();
            let rhs = self.binary(prec + 1)?; // asociativo a izquierda
            let pos = lhs.pos;
            lhs = Expr::new(pos, ExprKind::Binary { op, lhs: Box::new(lhs), rhs: Box::new(rhs) });
        }
        Ok(lhs)
    }

    fn unary(&mut self) -> Result<Expr> {
        use TokenKind as T;
        let pos = self.pos(); // la del operador prefijo, si lo hay
        let prefix = match self.peek_kind() {
            T::Plus => Some(UnOp::Plus),
            T::Sub => Some(UnOp::Neg),
            T::Bang => Some(UnOp::Not),
            T::Tilde => Some(UnOp::BitNot),
            T::PlusPlus => Some(UnOp::Inc),
            T::SubSub => Some(UnOp::Dec),
            _ => None,
        };
        if let Some(op) = prefix {
            self.bump();
            let expr = self.unary()?;
            return Ok(Expr::new(pos, ExprKind::Unary { op, expr: Box::new(expr), prefix: true }));
        }
        // `T[]::new` — referencia a un **constructor de array** (§15.13.3). Empieza con un tipo
        // (`int[]`, `String[][]`), que no es una expresión, así que se reconoce por adelantado; el
        // resto de las referencias a método las toma `postfix`.
        if self.array_ctor_ref_ahead() {
            return self.array_ctor_ref();
        }
        if let Some(cast) = self.try_cast()? {
            return Ok(cast);
        }
        self.postfix()
    }

    /// ¿Lo que viene es `Tipo []… :: new`? Barrido no destructivo: un tipo (primitivo, o un nombre
    /// cualificado **sin genéricos** —`List<T>[]::new` es ilegal), seguido de uno o más `[]`
    /// **vacíos** y de `::new`. Los corchetes vacíos son la clave: `arr[0]::foo` lleva algo adentro,
    /// así que no matchea y cae al camino de expresión.
    fn array_ctor_ref_ahead(&self) -> bool {
        use TokenKind as T;
        let mut i = 0; // offset desde el cursor
        let is_prim = matches!(
            self.kind_at(i),
            T::Int | T::Long | T::Short | T::Byte | T::Char | T::Boolean | T::Float | T::Double
        );
        if is_prim {
            i += 1;
        } else if self.kind_at(i) == T::Identifier {
            i += 1;
            while self.kind_at(i) == T::Dot && self.kind_at(i + 1) == T::Identifier {
                i += 2;
            }
        } else {
            return false;
        }
        if !(self.kind_at(i) == T::LBracket && self.kind_at(i + 1) == T::RBracket) {
            return false;
        }
        while self.kind_at(i) == T::LBracket && self.kind_at(i + 1) == T::RBracket {
            i += 2;
        }
        // Una referencia a constructor de array es **siempre** `::new`.
        self.kind_at(i) == T::ColCol && self.kind_at(i + 1) == T::New
    }

    fn array_ctor_ref(&mut self) -> Result<Expr> {
        let pos = self.pos();
        let mut ty = self.base_type()?;
        while self.at(TokenKind::LBracket) {
            self.bump();
            self.expect(TokenKind::RBracket)?;
            ty = Type::Array(Box::new(ty));
        }
        self.expect(TokenKind::ColCol)?;
        self.expect(TokenKind::New)?;
        let qualifier = Box::new(MethodRefQualifier::Type(ty));
        Ok(Expr::new(pos, ExprKind::MethodRef { qualifier, name: "new".to_string(), type_args: Vec::new() }))
    }

    /// Intenta `( Type ) unary`. Para tipos primitivos/array es inequívoco; para un tipo
    /// referencia exige que después del `)` venga el inicio de un operando (y **no** `+`/`-`,
    /// para no confundir `(a) - b` con un cast).
    fn try_cast(&mut self) -> Result<Option<Expr>> {
        if !self.at(TokenKind::LParen) {
            return Ok(None);
        }
        let cast_pos = self.pos();
        let save = self.mark();
        self.bump(); // (
        let Ok(ty) = self.parse_type() else {
            self.reset(save);
            return Ok(None);
        };
        if !self.at(TokenKind::RParen) {
            self.reset(save);
            return Ok(None);
        }
        let is_prim_or_array = matches!(ty, Type::Prim(_) | Type::Array(_));
        let ok = is_prim_or_array || cast_operand_start(self.kind_at(1));
        if !ok {
            self.reset(save);
            return Ok(None);
        }
        self.bump(); // )
        let expr = self.unary()?;
        Ok(Some(Expr::new(cast_pos, ExprKind::Cast { ty, expr: Box::new(expr) })))
    }

    fn postfix(&mut self) -> Result<Expr> {
        let mut e = self.primary()?;
        loop {
            // Cada sufijo envuelve lo acumulado: la expresión sigue arrancando donde arrancó
            // el operando de más a la izquierda.
            let pos = e.pos;
            match self.peek_kind() {
                // `C.class` (§15.8.2): `class` es **keyword**, así que no entra por el camino de un
                // acceso a campo y hay que reconocerlo acá. El nombre del tipo se reconstruye a
                // partir de la expresión que lo venía parseando (`C`, `a.b.C`).
                TokenKind::Dot if self.kind_at(1) == TokenKind::Class => {
                    self.bump(); // '.'
                    self.bump(); // 'class'
                    let Some(name) = Self::type_name_of(&e) else {
                        return Err(self.error("`.class` necesita un nombre de tipo".to_string()));
                    };
                    e = Expr::new(pos, ExprKind::ClassLit(Type::Class(name)));
                }
                TokenKind::Dot => {
                    self.bump();
                    // Un *type witness* (`recv.<String>m(...)`, §15.12.2.1): argumentos de tipo
                    // **explícitos** antes del nombre. Solo valen en una invocación, así que si están
                    // exigen el `(args)` detrás. Vacío es el caso normal.
                    let type_args = self.type_args()?.unwrap_or_default();
                    let name = self.expect_ident()?;
                    if self.at(TokenKind::LParen) {
                        let args = self.args()?;
                        let target = Some(Box::new(e));
                        e = Expr::new(pos, ExprKind::Call { target, name, args, type_args });
                    } else {
                        e = Expr::new(pos, ExprKind::Field { expr: Box::new(e), name });
                    }
                }
                TokenKind::LBracket => {
                    self.bump();
                    let index = self.expr()?;
                    self.expect(TokenKind::RBracket)?;
                    e = Expr::new(pos, ExprKind::Index { array: Box::new(e), index: Box::new(index) });
                }
                // `qualifier :: name` (§15.13): referencia a método, o a constructor con `new`. El
                // izquierdo ya está parseado como **expresión** (`obj`, `System.out`, `String`,
                // `this`/`super`); si en verdad era un tipo lo dirá la semántica. El caso `T[]::new`
                // no pasa por acá —un array no es expresión— y lo toma `unary`.
                TokenKind::ColCol => {
                    self.bump();
                    // Type witness también acá: `C::<T>m` (§15.13). Va **entre** el `::` y el nombre.
                    let type_args = self.type_args()?.unwrap_or_default();
                    let name = if self.eat(TokenKind::New) {
                        "new".to_string()
                    } else {
                        self.expect_ident()?
                    };
                    let qualifier = Box::new(MethodRefQualifier::Expr(Box::new(e)));
                    e = Expr::new(pos, ExprKind::MethodRef { qualifier, name, type_args });
                }
                TokenKind::PlusPlus => {
                    self.bump();
                    e = Expr::new(pos, ExprKind::Unary { op: UnOp::Inc, expr: Box::new(e), prefix: false });
                }
                TokenKind::SubSub => {
                    self.bump();
                    e = Expr::new(pos, ExprKind::Unary { op: UnOp::Dec, expr: Box::new(e), prefix: false });
                }
                _ => break,
            }
        }
        Ok(e)
    }

    fn primary(&mut self) -> Result<Expr> {
        use TokenKind as T;
        let tok = self.peek().clone();
        let pos = Pos { line: tok.line, col: tok.col };
        let e = match tok.kind {
            T::IntLiteral => {
                self.bump();
                ExprKind::IntLit(parse_int_literal(&tok.text).ok_or_else(|| self.error("literal entero inválido"))?)
            }
            T::LongLiteral => {
                self.bump();
                ExprKind::LongLit(parse_int_literal(&tok.text).ok_or_else(|| self.error("literal long inválido"))?)
            }
            T::FloatLiteral => {
                self.bump();
                ExprKind::FloatLit(parse_float_literal(&tok.text).ok_or_else(|| self.error("literal float inválido"))?)
            }
            T::DoubleLiteral => {
                self.bump();
                ExprKind::DoubleLit(parse_double_literal(&tok.text).ok_or_else(|| self.error("literal double inválido"))?)
            }
            T::CharLiteral => {
                self.bump();
                ExprKind::CharLit(decode_char(&tok.text).ok_or_else(|| self.error("literal char inválido"))?)
            }
            T::StringLiteral => {
                self.bump();
                ExprKind::StringLit(decode_string(&tok.text).ok_or_else(|| self.error("literal string inválido"))?)
            }
            T::True => {
                self.bump();
                ExprKind::BoolLit(true)
            }
            T::False => {
                self.bump();
                ExprKind::BoolLit(false)
            }
            T::Null => {
                self.bump();
                ExprKind::Null
            }
            T::Identifier => {
                let name = self.bump().text;
                if self.at(T::LParen) {
                    let args = self.args()?;
                    ExprKind::Call { target: None, name, args, type_args: Vec::new() }
                } else {
                    ExprKind::Name(name)
                }
            }
            T::This => {
                self.bump();
                if self.at(T::LParen) {
                    let args = self.args()?;
                    ExprKind::Call { target: None, name: "this".to_string(), args, type_args: Vec::new() }
                } else {
                    ExprKind::This
                }
            }
            T::Super => {
                self.bump();
                if self.at(T::LParen) {
                    let args = self.args()?;
                    ExprKind::Call { target: None, name: "super".to_string(), args, type_args: Vec::new() }
                } else {
                    ExprKind::Super
                }
            }
            // Estos dos se devuelven ya envueltos: `new` se ubica en su propia keyword, y un
            // paréntesis conserva **la posición y la decoración del interior** (no es un nodo).
            T::New => return self.new_expr(),
            T::LParen => {
                self.bump();
                let inner = self.expr()?;
                self.expect(T::RParen)?;
                return Ok(inner);
            }
            T::Switch => {
                self.bump();
                self.expect(T::LParen)?;
                let selector = self.expr()?;
                self.expect(T::RParen)?;
                let cases = self.switch_body()?;
                ExprKind::Switch { selector: Box::new(selector), cases }
            }
            other => return Err(self.error(format!("se esperaba una expresión, se encontró {other:?}"))),
        };
        Ok(Expr::new(pos, e))
    }

    fn new_expr(&mut self) -> Result<Expr> {
        let pos = self.pos();
        self.expect(TokenKind::New)?;
        let elem = self.base_type()?;
        if self.at(TokenKind::LBracket) {
            let mut dims = Vec::new();
            while self.at(TokenKind::LBracket) {
                self.bump();
                if self.eat(TokenKind::RBracket) {
                    dims.push(None);
                } else {
                    let len = self.expr()?;
                    self.expect(TokenKind::RBracket)?;
                    dims.push(Some(len));
                }
            }
            let init = if self.at(TokenKind::LBrace) { Some(self.array_init()?) } else { None };
            Ok(Expr::new(pos, ExprKind::NewArray { elem, dims, init }))
        } else {
            let args = self.args()?;
            // `new Type(args) { … }` — **clase anónima** (§15.9.5). El cuerpo se parsea con la misma
            // maquinaria que el de una clase; el nombre vacío la marca (y hace que ningún método se
            // tome por constructor, que una anónima no puede declarar, §15.9.5.1).
            let body = if self.at(TokenKind::LBrace) {
                Some(self.class_body("")?)
            } else {
                None
            };
            Ok(Expr::new(pos, ExprKind::NewObject { ty: elem, args, body }))
        }
    }

    /// El cuerpo `{ member* }` de una clase — reutilizado por la declaración de tipo y por la clase
    /// anónima. Asume que el token actual es `{`.
    fn class_body(&mut self, name: &str) -> Result<Vec<Member>> {
        use TokenKind as T;
        self.expect(T::LBrace)?;
        let mut members = Vec::new();
        while !self.at(T::RBrace) && !self.at(T::Eof) {
            self.member(name, &mut members)?;
        }
        self.expect(T::RBrace)?;
        Ok(members)
    }

    fn array_init(&mut self) -> Result<Vec<Expr>> {
        self.expect(TokenKind::LBrace)?;
        let mut elems = Vec::new();
        if !self.at(TokenKind::RBrace) {
            loop {
                if self.at(TokenKind::RBrace) {
                    break; // coma final
                }
                elems.push(self.expr()?);
                if !self.eat(TokenKind::Comma) {
                    break;
                }
            }
        }
        self.expect(TokenKind::RBrace)?;
        Ok(elems)
    }

    fn args(&mut self) -> Result<Vec<Expr>> {
        self.expect(TokenKind::LParen)?;
        let mut args = Vec::new();
        if !self.at(TokenKind::RParen) {
            args.push(self.expr()?);
            while self.eat(TokenKind::Comma) {
                args.push(self.expr()?);
            }
        }
        self.expect(TokenKind::RParen)?;
        Ok(args)
    }
}

// ---- tablas de operadores ----

/// Operador binario y su precedencia (mayor = liga más fuerte) para el token, si aplica.
fn bin_op(kind: TokenKind) -> Option<(BinOp, u8)> {
    use BinOp as B;
    use TokenKind as T;
    Some(match kind {
        T::Star => (B::Mul, 12),
        T::Slash => (B::Div, 12),
        T::Percent => (B::Rem, 12),
        T::Plus => (B::Add, 11),
        T::Sub => (B::Sub, 11),
        T::LtLt => (B::Shl, 10),
        T::GtGt => (B::Shr, 10),
        T::GtGtGt => (B::UShr, 10),
        T::Lt => (B::Lt, 9),
        T::Gt => (B::Gt, 9),
        T::LtEq => (B::Le, 9),
        T::GtEq => (B::Ge, 9),
        T::EqEq => (B::Eq, 8),
        T::BangEq => (B::Ne, 8),
        T::Amp => (B::BitAnd, 7),
        T::Caret => (B::BitXor, 6),
        T::Bar => (B::BitOr, 5),
        T::AmpAmp => (B::And, 4),
        T::BarBar => (B::Or, 3),
        _ => return None,
    })
}

fn assign_op(kind: TokenKind) -> Option<AssignOp> {
    use AssignOp as A;
    use TokenKind as T;
    Some(match kind {
        T::Eq => A::Assign,
        T::PlusEq => A::Add,
        T::SubEq => A::Sub,
        T::StarEq => A::Mul,
        T::SlashEq => A::Div,
        T::PercentEq => A::Rem,
        T::AmpEq => A::And,
        T::BarEq => A::Or,
        T::CaretEq => A::Xor,
        T::LtLtEq => A::Shl,
        T::GtGtEq => A::Shr,
        T::GtGtGtEq => A::UShr,
        _ => return None,
    })
}

/// ¿Este token puede iniciar el operando de un *cast* a tipo referencia? (Excluye `+`/`-`
/// para no confundir `(a) - b` con un cast.)
fn cast_operand_start(kind: TokenKind) -> bool {
    use TokenKind::*;
    matches!(
        kind,
        Identifier | IntLiteral | LongLiteral | FloatLiteral | DoubleLiteral | CharLiteral
            | StringLiteral | True | False | Null | This | Super | LParen | New | Bang | Tilde
    )
}

// ---- decodificación de literales ----

fn parse_int_literal(text: &str) -> Option<i64> {
    let clean: String = text.chars().filter(|&c| c != '_').collect();
    let t = clean.trim_end_matches(['l', 'L']);
    if let Some(hex) = t.strip_prefix("0x").or_else(|| t.strip_prefix("0X")) {
        i64::from_str_radix(hex, 16).ok()
    } else if let Some(bin) = t.strip_prefix("0b").or_else(|| t.strip_prefix("0B")) {
        i64::from_str_radix(bin, 2).ok()
    } else if t.len() > 1 && t.starts_with('0') && t.bytes().all(|b| (b'0'..=b'7').contains(&b)) {
        i64::from_str_radix(&t[1..], 8).ok()
    } else {
        t.parse::<i64>().ok()
    }
}

fn parse_float_literal(text: &str) -> Option<f32> {
    let clean: String = text.chars().filter(|&c| c != '_').collect();
    clean.trim_end_matches(['f', 'F']).parse().ok()
}

fn parse_double_literal(text: &str) -> Option<f64> {
    let clean: String = text.chars().filter(|&c| c != '_').collect();
    clean.trim_end_matches(['d', 'D']).parse().ok()
}

fn decode_char(text: &str) -> Option<char> {
    let chars: Vec<char> = text.chars().collect();
    if chars.len() < 2 {
        return None;
    }
    let inner = &chars[1..chars.len() - 1]; // sin las comillas
    let decoded = unescape(inner)?;
    let mut it = decoded.chars();
    let c = it.next()?;
    if it.next().is_some() {
        return None; // más de un carácter
    }
    Some(c)
}

fn decode_string(text: &str) -> Option<String> {
    let chars: Vec<char> = text.chars().collect();
    if chars.len() >= 6 && chars[0] == '"' && chars[1] == '"' && chars[2] == '"' {
        // Text block: quitamos las triples comillas (sin el destripado de indentación real).
        let inner = &chars[3..chars.len() - 3];
        let start = if inner.first() == Some(&'\n') { 1 } else { 0 };
        return unescape(&inner[start..]);
    }
    if chars.len() < 2 {
        return None;
    }
    unescape(&chars[1..chars.len() - 1])
}

/// Decodifica las secuencias de escape de Java en `chars` a un `String`.
fn unescape(chars: &[char]) -> Option<String> {
    let mut out = String::new();
    let mut i = 0;
    while i < chars.len() {
        if chars[i] == '\\' {
            i += 1;
            let e = *chars.get(i)?;
            match e {
                'n' => out.push('\n'),
                't' => out.push('\t'),
                'r' => out.push('\r'),
                'b' => out.push('\u{8}'),
                'f' => out.push('\u{c}'),
                's' => out.push(' '),
                '0' => out.push('\0'),
                '\\' => out.push('\\'),
                '\'' => out.push('\''),
                '"' => out.push('"'),
                'u' => {
                    let hex: String = chars.get(i + 1..i + 5)?.iter().collect();
                    let code = u32::from_str_radix(&hex, 16).ok()?;
                    out.push(char::from_u32(code)?);
                    i += 4;
                }
                other => out.push(other),
            }
        } else {
            out.push(chars[i]);
        }
        i += 1;
    }
    Some(out)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::javac::lexer::tokenize;

    fn parse_src(src: &str) -> CompilationUnit {
        parse(tokenize(src).unwrap()).unwrap()
    }

    /// Parsea el cuerpo de un método envuelto en una clase, devolviendo sus sentencias.
    fn parse_body(body: &str) -> Vec<Stmt> {
        let src = format!("class T {{ void m() {{ {body} }} }}");
        let cu = parse_src(&src);
        let Member::Method(m) = &cu.types[0].members[0] else { panic!() };
        m.body.clone().unwrap().0
    }

    /// La expresión inicializadora de `int/var x = <expr>;` — para probar formas de expresión.
    fn parse_init(expr: &str) -> Expr {
        let stmts = parse_body(&format!("var x = {expr};"));
        let StmtKind::LocalVar { init: Some(e), .. } = &stmts[0].kind else {
            panic!("estructura inesperada: {:?}", stmts[0])
        };
        e.clone()
    }

    // ---- lambdas (§15.27) ----

    #[test]
    fn a_bare_single_param_lambda() {
        // `x -> x + 1`: un parámetro **inferido**, cuerpo expresión.
        let ExprKind::Lambda { params, body } = parse_init("x -> x + 1").kind else {
            panic!("esperaba una lambda")
        };
        assert_eq!(params.len(), 1);
        assert_eq!(params[0].name, "x");
        assert_eq!(params[0].ty, Type::Var, "sin anotación ⇒ tipo inferido");
        assert!(matches!(*body, LambdaBody::Expr(_)));
    }

    #[test]
    fn a_zero_param_block_lambda() {
        let ExprKind::Lambda { params, body } = parse_init("() -> { return 1; }").kind else {
            panic!("esperaba una lambda")
        };
        assert!(params.is_empty());
        assert!(matches!(*body, LambdaBody::Block(_)));
    }

    #[test]
    fn a_multi_param_inferred_lambda() {
        let ExprKind::Lambda { params, .. } = parse_init("(a, b) -> a + b").kind else {
            panic!("esperaba una lambda")
        };
        assert_eq!(params.len(), 2);
        assert!(params.iter().all(|p| p.ty == Type::Var));
    }

    #[test]
    fn an_explicitly_typed_lambda() {
        let ExprKind::Lambda { params, .. } = parse_init("(int x, String s) -> x").kind else {
            panic!("esperaba una lambda")
        };
        assert_eq!(params[0].ty, Type::Prim(PrimType::Int));
        assert_eq!(params[1].ty, Type::Class("String".into()));
    }

    #[test]
    fn a_var_typed_lambda_param() {
        let ExprKind::Lambda { params, .. } = parse_init("(var s) -> s").kind else {
            panic!("esperaba una lambda")
        };
        assert_eq!(params[0].ty, Type::Var);
    }

    #[test]
    fn a_curried_lambda_nests() {
        // `x -> y -> x + y`: el cuerpo de la primera es **otra** lambda.
        let ExprKind::Lambda { body, .. } = parse_init("x -> y -> x + y").kind else {
            panic!("esperaba una lambda")
        };
        let LambdaBody::Expr(inner) = *body else { panic!("cuerpo expresión") };
        assert!(matches!(inner.kind, ExprKind::Lambda { .. }), "la de adentro también es lambda");
    }

    #[test]
    fn a_cast_is_not_mistaken_for_a_lambda() {
        // `((String) o)` — el desambiguador NO debe verlo como lambda: no hay `->` tras el `)`.
        let ExprKind::Cast { ty, .. } = parse_init("(String) o").kind else {
            panic!("esperaba un cast, no una lambda")
        };
        assert_eq!(ty, Type::Class("String".into()));
    }

    #[test]
    fn nested_parens_are_not_a_lambda() {
        // `((1 + 2))` colapsa a la suma; ningún nodo Lambda.
        assert!(matches!(parse_init("((1 + 2))").kind, ExprKind::Binary { op: BinOp::Add, .. }));
    }

    #[test]
    fn a_parenthesized_lambda_still_parses() {
        // `(x -> x)` — lambda dentro de paréntesis: el interior se re-examina y se reconoce.
        assert!(matches!(parse_init("(x -> x)").kind, ExprKind::Lambda { .. }));
    }

    #[test]
    fn a_lambda_as_a_call_argument() {
        // En posición de argumento el desambiguador también tiene que disparar.
        let ExprKind::Call { args, .. } = parse_init("run(() -> {})").kind else {
            panic!("esperaba una llamada")
        };
        assert!(matches!(args[0].kind, ExprKind::Lambda { .. }));
    }

    // ---- anotaciones (§9.7) ----

    #[test]
    fn an_annotation_on_a_class_is_retained() {
        let cu = parse_src("@Deprecated class T {}");
        let annos = &cu.types[0].annotations;
        assert_eq!(annos.len(), 1);
        assert_eq!(annos[0].name, "Deprecated");
        assert!(annos[0].args.is_empty(), "marcador: sin argumentos");
    }

    #[test]
    fn an_annotation_on_a_method_is_retained() {
        let src = "class T { @Override public String toString() { return null; } }";
        let Member::Method(m) = &parse_src(src).types[0].members[0] else { panic!() };
        assert_eq!(m.annotations.len(), 1);
        assert_eq!(m.annotations[0].name, "Override");
    }

    #[test]
    fn a_single_value_annotation() {
        // `@SuppressWarnings("x")` — el valor va sin nombre (elemento implícito `value`).
        let src = "class T { @SuppressWarnings(\"x\") int f; }";
        let Member::Field(f) = &parse_src(src).types[0].members[0] else { panic!() };
        let arg = &f.annotations[0].args[0];
        assert!(arg.name.is_none(), "posicional");
        assert!(matches!(&arg.value, AnnotationValue::Expr(e) if matches!(e.kind, ExprKind::StringLit(_))));
    }

    #[test]
    fn a_named_pair_annotation() {
        let src = "class T { @Foo(name = \"bar\", count = 3) void m() {} }";
        let Member::Method(m) = &parse_src(src).types[0].members[0] else { panic!() };
        let args = &m.annotations[0].args;
        assert_eq!(args.len(), 2);
        assert_eq!(args[0].name.as_deref(), Some("name"));
        assert_eq!(args[1].name.as_deref(), Some("count"));
    }

    #[test]
    fn an_array_valued_annotation() {
        let src = "class T { @SuppressWarnings({\"a\", \"b\"}) void m() {} }";
        let Member::Method(m) = &parse_src(src).types[0].members[0] else { panic!() };
        let AnnotationValue::Array(elems) = &m.annotations[0].args[0].value else {
            panic!("esperaba un arreglo")
        };
        assert_eq!(elems.len(), 2);
    }

    #[test]
    fn a_parameter_annotation_is_retained() {
        let src = "class T { void m(@NonNull String s, final int i) {} }";
        let Member::Method(m) = &parse_src(src).types[0].members[0] else { panic!() };
        assert_eq!(m.params[0].annotations.len(), 1);
        assert_eq!(m.params[0].annotations[0].name, "NonNull");
        assert!(m.params[1].is_final, "el `final` del segundo se conserva");
    }

    #[test]
    fn a_nested_annotation() {
        let src = "class T { @Outer(@Inner) void m() {} }";
        let Member::Method(m) = &parse_src(src).types[0].members[0] else { panic!() };
        assert!(matches!(&m.annotations[0].args[0].value, AnnotationValue::Nested(_)));
    }

    // ---- type witness (§15.12.2.1 / §15.13) ----

    #[test]
    fn a_type_witness_on_a_call() {
        // `Collections.<String>emptyList()` — argumentos de tipo explícitos antes del método.
        let ExprKind::Call { name, type_args, .. } =
            parse_init("java.util.Collections.<String>emptyList()").kind
        else {
            panic!("esperaba una llamada")
        };
        assert_eq!(name, "emptyList");
        assert_eq!(type_args, vec![TypeArg::Type(Type::Class("String".into()))]);
    }

    #[test]
    fn a_multi_arg_type_witness() {
        let ExprKind::Call { type_args, .. } = parse_init("obj.<A, B>convert(x)").kind else {
            panic!("esperaba una llamada")
        };
        assert_eq!(type_args.len(), 2);
    }

    #[test]
    fn a_witness_on_this_and_super() {
        for src in ["this.<Integer>id(5)", "super.<String>make()"] {
            let ExprKind::Call { type_args, .. } = parse_init(src).kind else {
                panic!("esperaba una llamada para `{src}`")
            };
            assert_eq!(type_args.len(), 1, "para `{src}`");
        }
    }

    #[test]
    fn a_type_witness_on_a_method_ref() {
        let ExprKind::MethodRef { name, type_args, .. } = parse_init("Foo::<String>bar").kind else {
            panic!("esperaba una referencia a método")
        };
        assert_eq!(name, "bar");
        assert_eq!(type_args, vec![TypeArg::Type(Type::Class("String".into()))]);
    }

    #[test]
    fn a_call_without_witness_has_empty_type_args() {
        let ExprKind::Call { type_args, .. } = parse_init("foo.bar(x)").kind else {
            panic!("esperaba una llamada")
        };
        assert!(type_args.is_empty());
    }

    #[test]
    fn less_than_is_not_a_type_witness() {
        // `a < b` — el `<` no viene tras un `.`/`::`, así que es el operador, no un witness.
        assert!(matches!(parse_init("a < b").kind, ExprKind::Binary { op: BinOp::Lt, .. }));
    }

    // ---- clases locales (§14.3) ----

    #[test]
    fn a_local_class_declaration() {
        let stmts = parse_body("class C { int x; int get() { return x; } } C c = new C();");
        let StmtKind::LocalClass(decl) = &stmts[0].kind else {
            panic!("esperaba una clase local, salió {:?}", stmts[0].kind)
        };
        assert_eq!(decl.name, "C");
        assert_eq!(decl.members.len(), 2);
        // La sentencia siguiente sí es un local normal.
        assert!(matches!(stmts[1].kind, StmtKind::LocalVar { .. }));
    }

    #[test]
    fn a_modified_local_class() {
        // `final class` es una clase local; el modificador se conserva.
        let stmts = parse_body("final class C {}");
        let StmtKind::LocalClass(decl) = &stmts[0].kind else { panic!("esperaba una clase local") };
        assert!(decl.modifiers.contains(&Modifier::Final));
    }

    #[test]
    fn a_final_local_is_not_a_class() {
        // `final int x` no debe caer en el camino de clase local: es una variable.
        let stmts = parse_body("final int x = 5;");
        assert!(matches!(stmts[0].kind, StmtKind::LocalVar { is_final: true, .. }));
    }

    #[test]
    fn local_records_interfaces_and_enums() {
        for (src, kind) in [
            ("record R(int a) {}", TypeKind::Record),
            ("interface I { void f(); }", TypeKind::Interface),
            ("enum E { A, B }", TypeKind::Enum),
        ] {
            let stmts = parse_body(src);
            let StmtKind::LocalClass(decl) = &stmts[0].kind else {
                panic!("esperaba una clase local para `{src}`")
            };
            assert_eq!(decl.kind, kind, "para `{src}`");
        }
    }

    // ---- clases anónimas (§15.9.5) ----

    #[test]
    fn an_anonymous_class_implementing_an_interface() {
        let ExprKind::NewObject { ty, args, body } =
            parse_init("new Runnable() { public void run() {} }").kind
        else {
            panic!("esperaba un new con cuerpo")
        };
        assert_eq!(ty, Type::Class("Runnable".into()));
        assert!(args.is_empty());
        let members = body.expect("la anónima tiene cuerpo");
        assert_eq!(members.len(), 1);
        assert!(matches!(members[0], Member::Method(_)));
    }

    #[test]
    fn an_anonymous_class_passes_constructor_args() {
        let ExprKind::NewObject { args, body, .. } =
            parse_init("new Thread(\"w\") { public void run() {} }").kind
        else {
            panic!("esperaba un new con cuerpo")
        };
        assert_eq!(args.len(), 1, "el argumento va al super de la anónima");
        assert!(body.is_some());
    }

    #[test]
    fn an_anonymous_body_holds_fields_and_initializers() {
        let ExprKind::NewObject { body, .. } =
            parse_init("new Object() { int c = 0; { c = 1; } }").kind
        else {
            panic!("esperaba un new con cuerpo")
        };
        let members = body.unwrap();
        assert!(members.iter().any(|m| matches!(m, Member::Field(_))));
        assert!(members.iter().any(|m| matches!(m, Member::InstanceInit(_))));
    }

    #[test]
    fn a_plain_new_has_no_body() {
        // `new Foo()` sin llaves: el cuerpo es `None`, sigue siendo el caso corriente.
        let ExprKind::NewObject { body, .. } = parse_init("new Foo()").kind else {
            panic!("esperaba un new")
        };
        assert!(body.is_none());
    }

    #[test]
    fn anonymous_classes_nest() {
        let ExprKind::NewObject { body, .. } =
            parse_init("new Object() { Runnable r = new Runnable() { public void run() {} }; }").kind
        else {
            panic!("esperaba un new con cuerpo")
        };
        let Member::Field(f) = &body.unwrap()[0] else { panic!("un campo") };
        let ExprKind::NewObject { body: inner, .. } = &f.init.as_ref().unwrap().kind else {
            panic!("el init es otro new")
        };
        assert!(inner.is_some(), "la anónima de adentro también tiene cuerpo");
    }

    // ---- referencias a método (§15.13) ----

    #[test]
    fn a_type_method_ref() {
        // `String::length` — el qualifier se parsea como **expresión** (un `Name`); tipo o valor lo
        // decide la semántica.
        let ExprKind::MethodRef { qualifier, name, .. } = parse_init("String::length").kind else {
            panic!("esperaba una referencia a método")
        };
        assert_eq!(name, "length");
        assert!(matches!(*qualifier, MethodRefQualifier::Expr(_)));
    }

    #[test]
    fn a_field_chain_method_ref() {
        // `System.out::println` — el qualifier es un acceso a campo.
        let ExprKind::MethodRef { qualifier, name, .. } = parse_init("System.out::println").kind else {
            panic!("esperaba una referencia a método")
        };
        assert_eq!(name, "println");
        let MethodRefQualifier::Expr(e) = *qualifier else { panic!("qualifier expresión") };
        assert!(matches!(e.kind, ExprKind::Field { .. }));
    }

    #[test]
    fn a_constructor_ref() {
        let ExprKind::MethodRef { name, .. } = parse_init("java.util.ArrayList::new").kind else {
            panic!("esperaba una referencia a constructor")
        };
        assert_eq!(name, "new");
    }

    #[test]
    fn a_primitive_array_ctor_ref() {
        // `int[]::new` — el qualifier es un **tipo** (un array), no una expresión.
        let ExprKind::MethodRef { qualifier, name, .. } = parse_init("int[]::new").kind else {
            panic!("esperaba una referencia a constructor de array")
        };
        assert_eq!(name, "new");
        let MethodRefQualifier::Type(t) = *qualifier else { panic!("qualifier tipo") };
        assert_eq!(t, Type::Array(Box::new(Type::Prim(PrimType::Int))));
    }

    #[test]
    fn a_multidim_reference_array_ctor_ref() {
        let ExprKind::MethodRef { qualifier, .. } = parse_init("String[][]::new").kind else {
            panic!("esperaba una referencia a constructor de array")
        };
        let MethodRefQualifier::Type(t) = *qualifier else { panic!("qualifier tipo") };
        assert_eq!(
            t,
            Type::Array(Box::new(Type::Array(Box::new(Type::Class("String".into())))))
        );
    }

    #[test]
    fn an_array_index_is_not_a_ctor_ref() {
        // `arr[0]` lleva algo entre corchetes: es un índice, no `T[]::new`.
        assert!(matches!(parse_init("arr[0]").kind, ExprKind::Index { .. }));
    }

    #[test]
    fn a_method_ref_as_a_call_argument() {
        let ExprKind::Call { args, .. } = parse_init("forEach(System.out::println)").kind else {
            panic!("esperaba una llamada")
        };
        assert!(matches!(args[0].kind, ExprKind::MethodRef { .. }));
    }

    #[test]
    fn parses_add_java() {
        let src = "public class Add {\n\
            public static int add(int a, int b) { return a + b; }\n\
            public static int substract(int a, int b) { return a - b; }\n\
            public static void main(String[] args) {\n\
                int r = add(2, 3);\n\
                int x = add(r, 1);\n\
                System.out.println(x);\n\
            }\n\
        }";
        let cu = parse_src(src);
        assert_eq!(cu.types.len(), 1);
        let class = &cu.types[0];
        assert_eq!(class.name, "Add");
        assert_eq!(class.members.len(), 3);
        let Member::Method(main) = &class.members[2] else { panic!("esperaba método") };
        assert_eq!(main.name, "main");
        assert_eq!(main.params.len(), 1);
        assert_eq!(main.params[0].ty, Type::Array(Box::new(Type::Class("String".into()))));
        // El cuerpo de main: 3 sentencias (2 locales + la llamada a println).
        assert_eq!(main.body.as_ref().unwrap().0.len(), 3);
    }

    #[test]
    fn precedence_multiplication_over_addition() {
        // a + b * c  ==  a + (b * c)
        let stmts = parse_body("int r = a + b * c;");
        let StmtKind::LocalVar { init: Some(init), .. } = &stmts[0].kind else {
            panic!("estructura inesperada: {:?}", stmts[0]);
        };
        let ExprKind::Binary { op: BinOp::Add, rhs, .. } = &init.kind else {
            panic!("esperaba una suma en la raíz: {init:?}");
        };
        assert!(matches!(rhs.kind, ExprKind::Binary { op: BinOp::Mul, .. }));
    }

    #[test]
    fn assignment_is_right_associative() {
        // a = b = c
        let stmts = parse_body("a = b = c;");
        let StmtKind::Expr(e) = &stmts[0].kind else { panic!() };
        let ExprKind::Assign { value, .. } = &e.kind else { panic!() };
        assert!(matches!(value.kind, ExprKind::Assign { .. }));
    }

    #[test]
    fn distinguishes_local_decl_from_assignment() {
        let stmts = parse_body("int x = 1; x = 2; foo(x);");
        assert!(matches!(stmts[0].kind, StmtKind::LocalVar { .. }));
        assert!(matches!(&stmts[1].kind, StmtKind::Expr(e) if matches!(e.kind, ExprKind::Assign { .. })));
        assert!(matches!(&stmts[2].kind, StmtKind::Expr(e) if matches!(e.kind, ExprKind::Call { .. })));
    }

    #[test]
    fn if_while_for_and_calls() {
        let stmts = parse_body(
            "for (int i = 0; i < 10; i++) { sum += i; } if (sum > 5) return; while (true) break;",
        );
        assert!(matches!(stmts[0].kind, StmtKind::For { .. }));
        assert!(matches!(stmts[1].kind, StmtKind::If { .. }));
        assert!(matches!(stmts[2].kind, StmtKind::While { .. }));
    }

    #[test]
    fn try_with_multicatch_and_finally() {
        let stmts = parse_body("try { f(); } catch (A | B e) { g(); } finally { h(); }");
        let StmtKind::Try { catches, finally, resources, .. } = &stmts[0].kind else {
            panic!("esperaba Try: {:?}", stmts[0]);
        };
        assert!(resources.is_empty());
        assert_eq!(catches.len(), 1);
        assert_eq!(catches[0].types.len(), 2, "multi-catch A | B");
        assert!(finally.is_some());
    }

    #[test]
    fn try_with_resources() {
        let stmts = parse_body("try (Reader r = open()) { r.read(); }");
        let StmtKind::Try { resources, .. } = &stmts[0].kind else { panic!() };
        assert_eq!(resources.len(), 1);
        assert!(matches!(resources[0].kind, StmtKind::LocalVar { .. }));
    }

    #[test]
    fn switch_arrow_and_multilabel() {
        let stmts = parse_body("switch (x) { case 1, 2 -> f(); case 3 -> { g(); } default -> h(); }");
        let StmtKind::Switch { cases, .. } = &stmts[0].kind else { panic!("esperaba Switch") };
        assert_eq!(cases.len(), 3);
        assert_eq!(cases[0].labels.len(), 2);
        assert!(cases[2].is_default);
    }

    #[test]
    fn switch_colon_fallthrough() {
        let stmts = parse_body("switch (x) { case 1: case 2: y = 1; break; default: y = 0; }");
        let StmtKind::Switch { cases, .. } = &stmts[0].kind else { panic!() };
        assert_eq!(cases.len(), 2, "grupo {{1,2}} + default");
        assert_eq!(cases[0].labels.len(), 2);
    }

    #[test]
    fn switch_expression_with_yield() {
        let stmts = parse_body("int r = switch (x) { case 1 -> 10; default -> { yield 0; } };");
        let StmtKind::LocalVar { init: Some(init), .. } = &stmts[0].kind else { panic!() };
        let ExprKind::Switch { cases, .. } = &init.kind else {
            panic!("esperaba una switch-expression");
        };
        assert_eq!(cases.len(), 2);
    }

    #[test]
    fn labeled_break_and_continue_carry_the_label() {
        let stmts = parse_body("L: while (c) { break L; continue L; }");
        let StmtKind::Labeled { label, body } = &stmts[0].kind else { panic!("{:?}", stmts[0].kind) };
        assert_eq!(label, "L");
        let StmtKind::While { body, .. } = &body.kind else { panic!() };
        let StmtKind::Block(b) = &body.kind else { panic!() };
        assert!(matches!(&b.0[0].kind, StmtKind::Break(Some(l)) if l == "L"), "{:?}", b.0[0].kind);
        assert!(matches!(&b.0[1].kind, StmtKind::Continue(Some(l)) if l == "L"), "{:?}", b.0[1].kind);
    }

    #[test]
    fn plain_break_and_continue_have_no_label() {
        let stmts = parse_body("while (c) { break; continue; }");
        let StmtKind::While { body, .. } = &stmts[0].kind else { panic!() };
        let StmtKind::Block(b) = &body.kind else { panic!() };
        assert!(matches!(b.0[0].kind, StmtKind::Break(None)));
        assert!(matches!(b.0[1].kind, StmtKind::Continue(None)));
    }

    #[test]
    fn a_label_can_sit_on_a_block() {
        let stmts = parse_body("done: { int x = 1; break done; }");
        let StmtKind::Labeled { label, body } = &stmts[0].kind else { panic!("{:?}", stmts[0].kind) };
        assert_eq!(label, "done");
        assert!(matches!(body.kind, StmtKind::Block(_)), "la etiqueta va sobre un bloque");
    }

    #[test]
    fn instance_initializer_block_is_kept() {
        // Antes se parseaba y se **descartaba**: el código se perdía en silencio.
        let cu = parse_src("class C { int x; { x = 1; } }");
        assert!(cu.types[0].members.iter().any(|m| matches!(m, Member::InstanceInit(_))));
    }

    #[test]
    fn c_style_array_declarator_is_accepted() {
        // `int y[]` ≡ `int[] y` (§10.2), y los `[]` van **por variable**: acá solo `a` es array.
        let stmts = parse_body("int a[] = null, b = 1;");
        assert!(matches!(&stmts[0].kind, StmtKind::LocalVar { ty: Type::Array(_), .. }), "{:?}", stmts[0].kind);
        assert!(matches!(&stmts[1].kind, StmtKind::LocalVar { ty: Type::Prim(_), .. }), "{:?}", stmts[1].kind);
    }

    #[test]
    fn an_array_initializer_without_new_is_sugar_for_it() {
        // `{1,2,3}` ≡ `new int[]{1,2,3}` (§10.6).
        let stmts = parse_body("int[] a = {1, 2, 3};");
        let StmtKind::LocalVar { init: Some(e), .. } = &stmts[0].kind else { panic!() };
        let ExprKind::NewArray { init: Some(items), .. } = &e.kind else { panic!("{:?}", e.kind) };
        assert_eq!(items.len(), 3);
    }

    #[test]
    fn instanceof_can_bind_a_pattern_variable() {
        let stmts = parse_body("Object o = null; boolean b = o instanceof String s;");
        let StmtKind::LocalVar { init: Some(e), .. } = &stmts[1].kind else { panic!() };
        assert!(matches!(&e.kind, ExprKind::InstanceOf { binding: Some(n), .. } if n == "s"), "{:?}", e.kind);
    }

    #[test]
    fn static_initializer_block_is_kept() {
        // Antes se descartaba; ahora es un `Member::StaticInit`.
        let cu = parse_src("class C { static int x; static { x = 1; } }");
        assert!(
            cu.types[0].members.iter().any(|m| matches!(m, Member::StaticInit(_))),
            "el bloque static {{ }} se conserva"
        );
    }

    #[test]
    fn parses_generics_and_annotation_type() {
        let cu = parse_src(
            "class Box<T> { <U> U id(U x) { return x; } } @interface Marker { String value() default \"x\"; }",
        );
        assert_eq!(tp_names(&cu.types[0].type_params), ["T"]);
        let Member::Method(m) = &cu.types[0].members[0] else { panic!() };
        assert_eq!(tp_names(&m.type_params), ["U"]);
        assert_eq!(cu.types[1].kind, TypeKind::Annotation);
    }

    // ---- genéricos: representación (etapa A) ----

    fn tp_names(ps: &[TypeParam]) -> Vec<&str> {
        ps.iter().map(|p| p.name.as_str()).collect()
    }

    /// El tipo declarado del primer local del cuerpo.
    fn local_ty(body: &str) -> Type {
        let stmts = parse_body(body);
        let StmtKind::LocalVar { ty, .. } = &stmts[0].kind else { panic!("esperaba un local") };
        ty.clone()
    }

    #[test]
    fn parses_type_arguments() {
        assert_eq!(
            local_ty("List<String> xs = null;"),
            Type::Parameterized {
                base: "List".into(),
                args: vec![TypeArg::Type(Type::Class("String".into()))],
            }
        );
    }

    #[test]
    fn a_raw_type_is_not_parameterized() {
        assert_eq!(local_ty("List xs = null;"), Type::Class("List".into()));
    }

    #[test]
    fn splits_nested_generics_closing_with_shift_token() {
        // El lexer junta `>>` en un token; el parser tiene que **partirlo** para cerrar los dos
        // genéricos anidados.
        let ty = local_ty("Map<String, List<Integer>> m = null;");
        let Type::Parameterized { base, args } = &ty else { panic!("{ty:?}") };
        assert_eq!(base, "Map");
        assert_eq!(args.len(), 2);
        assert_eq!(args[0], TypeArg::Type(Type::Class("String".into())));
        assert_eq!(
            args[1],
            TypeArg::Type(Type::Parameterized {
                base: "List".into(),
                args: vec![TypeArg::Type(Type::Class("Integer".into()))],
            })
        );
    }

    #[test]
    fn splits_triple_nested_generics() {
        // Cierra con `>>>`, que el lexer junta como el shift sin signo.
        let ty = local_ty("List<List<List<String>>> x = null;");
        let Type::Parameterized { args, .. } = &ty else { panic!("{ty:?}") };
        let TypeArg::Type(Type::Parameterized { args: inner, .. }) = &args[0] else { panic!() };
        assert!(matches!(inner[0], TypeArg::Type(Type::Parameterized { .. })));
    }

    #[test]
    fn shift_operators_still_parse_as_shifts() {
        // El corte de `>>` no debe contaminar las expresiones.
        let stmts = parse_body("int x = a >> b; int y = c >>> d;");
        for (i, op) in [BinOp::Shr, BinOp::UShr].into_iter().enumerate() {
            let StmtKind::LocalVar { init: Some(e), .. } = &stmts[i].kind else { panic!() };
            assert!(matches!(e.kind, ExprKind::Binary { op: o, .. } if o == op), "{:?}", e.kind);
        }
    }

    #[test]
    fn backtracking_restores_a_split_shift_token() {
        // `(a<b>>c)`: el parser prueba leerlo como un *cast* a `a<b>` — para lo cual **parte** el
        // `>>` —, falla al no encontrar el `)`, y retrocede. Ahí tiene que **restaurar** el `>>`:
        // si quedara partido, sobraría un solo `>` y se reparsearía como `(a<b) > c` en vez del
        // correcto `a < (b>>c)` (el shift precede al relacional).
        let stmts = parse_body("x = (a<b>>c);");
        let StmtKind::Expr(e) = &stmts[0].kind else { panic!("{:?}", stmts[0].kind) };
        let ExprKind::Assign { value, .. } = &e.kind else { panic!() };
        let ExprKind::Binary { op, rhs, .. } = &value.kind else { panic!("{:?}", value.kind) };
        assert_eq!(*op, BinOp::Lt, "la raíz debe seguir siendo el `<`");
        assert!(
            matches!(rhs.kind, ExprKind::Binary { op: BinOp::Shr, .. }),
            "el `>>` tiene que sobrevivir al backtracking: {:?}",
            rhs.kind
        );
    }

    #[test]
    fn parses_wildcards() {
        let ty = local_ty("List<? extends Number> xs = null;");
        let Type::Parameterized { args, .. } = &ty else { panic!() };
        assert_eq!(args[0], TypeArg::Extends(Box::new(Type::Class("Number".into()))));

        let ty = local_ty("List<? super Integer> xs = null;");
        let Type::Parameterized { args, .. } = &ty else { panic!() };
        assert_eq!(args[0], TypeArg::Super(Box::new(Type::Class("Integer".into()))));

        let ty = local_ty("List<?> xs = null;");
        let Type::Parameterized { args, .. } = &ty else { panic!() };
        assert_eq!(args[0], TypeArg::Wildcard);
    }

    #[test]
    fn diamond_is_not_a_raw_type() {
        // `<>` pide inferir (§15.9.1); no es lo mismo que el tipo crudo `ArrayList`.
        let stmts = parse_body("List<String> xs = new ArrayList<>();");
        let StmtKind::LocalVar { init: Some(e), .. } = &stmts[0].kind else { panic!() };
        let ExprKind::NewObject { ty, .. } = &e.kind else { panic!("{:?}", e.kind) };
        assert_eq!(*ty, Type::Parameterized { base: "ArrayList".into(), args: vec![] });
    }

    #[test]
    fn parses_type_parameter_bounds() {
        let cu = parse_src("class C<T extends Number & Comparable<T>, U> {}");
        let tps = &cu.types[0].type_params;
        assert_eq!(tp_names(tps), ["T", "U"]);
        assert_eq!(tps[0].bounds.len(), 2, "`Number & Comparable<T>` son dos cotas");
        assert_eq!(tps[0].bounds[0], Type::Class("Number".into()));
        assert!(matches!(tps[0].bounds[1], Type::Parameterized { .. }));
        assert!(tps[1].bounds.is_empty(), "`U` no declara cotas");
    }

    #[test]
    fn parses_generic_method_signature() {
        let cu = parse_src("class C { <T extends Comparable<T>> T max(List<T> xs) { return null; } }");
        let Member::Method(m) = &cu.types[0].members[0] else { panic!() };
        assert_eq!(tp_names(&m.type_params), ["T"]);
        assert_eq!(m.type_params[0].bounds.len(), 1);
        assert_eq!(
            m.params[0].ty,
            Type::Parameterized {
                base: "List".into(),
                args: vec![TypeArg::Type(Type::Class("T".into()))],
            }
        );
    }

    #[test]
    fn switch_type_pattern_with_guard() {
        let stmts = parse_body(
            "switch (o) { case Integer i when i > 0 -> f(); case String s -> g(); case null -> h(); }",
        );
        let StmtKind::Switch { cases, .. } = &stmts[0].kind else { panic!() };
        assert!(matches!(cases[0].labels[0], CaseLabel::Pattern { .. }));
        assert!(cases[0].guard.is_some());
        assert!(matches!(cases[2].labels[0], CaseLabel::Null));
    }
}
