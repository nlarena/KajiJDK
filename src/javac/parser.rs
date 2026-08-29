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
//! El lenguaje está **completo** en el parser. Los antiguos bordes menores ya están:
//!
//! - Las anotaciones sobre un **parámetro de tipo** (`<@Foo T>`) y sobre una **constante de `enum`**
//!   se **retienen** (en `TypeParam.annotations`/`EnumConstant.annotations`), ya no se descartan.
//! - Las **anotaciones de tipo/uso** (§9.7.4: `List<@NonNull String>`, `@Foo int`) se **aceptan**
//!   ([`Parser::skip_type_annotations`]); se descartan por ser metadata ajena a la *erasure* y a la
//!   emisión (la variante de **nivel de array** `String @A []` queda como cola menor).
//! - El **type witness de constructor** (`new <T>Foo()`) se **acepta** (ya no da error).
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

/// Parsea una lista de tokens (terminada en `Eof`) a una unidad de compilación, con **recuperación
/// de errores** (§ panic-mode): en vez de abortar en el primer error de sintaxis, sincroniza en el
/// borde de sentencia/miembro/tipo y sigue, así devuelve **todos** los errores en una pasada.
pub fn parse(tokens: Vec<Token>) -> (CompilationUnit, Vec<Error>) {
    let mut p = Parser {
        tokens,
        pos: 0,
        gt_splits: Vec::new(),
        errors: Vec::new(),
        pending_type_annos: Vec::new(),
    };
    let unit = p.compilation_unit();
    (unit, p.errors)
}

struct Parser {
    tokens: Vec<Token>,
    pos: usize,
    /// *Undo log* de los cortes de `>>`/`>>>` (ver [`Parser::eat_gt`]): posición y kind original.
    gt_splits: Vec<(usize, TokenKind)>,
    /// Los errores de sintaxis acumulados: la recuperación los junta en vez de abortar en el primero.
    errors: Vec<Error>,
    /// Las **type annotations** (§9.7.4) recolectadas al parsear el último tipo, con su `type_path`.
    /// Cada `parse_type` las acumula; la declaración que lo llama las **drena** ([`Parser::take_type_annos`])
    /// y las guarda con su target. Se vacía al drenar.
    pending_type_annos: Vec<TypeUseAnnot>,
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
        Error::new(message, t.line, t.col)
    }

    /// La posición del token actual — para adjuntar a las declaraciones del AST.
    fn pos(&self) -> Pos {
        let t = self.peek();
        Pos { line: t.line, col: t.col }
    }

    // ---- unidad de compilación ----

    fn compilation_unit(&mut self) -> CompilationUnit {
        let package = if self.eat(TokenKind::Package) {
            match self.package_clause() {
                Ok(n) => Some(n),
                Err(e) => {
                    self.errors.push(e);
                    self.sync_top_level();
                    None
                }
            }
        } else {
            None
        };

        let mut imports = Vec::new();
        while self.at(TokenKind::Import) {
            match self.import() {
                Ok(i) => imports.push(i),
                Err(e) => {
                    self.errors.push(e);
                    self.sync_top_level();
                }
            }
        }

        let mut types = Vec::new();
        let mut module = None;
        while !self.at(TokenKind::Eof) {
            if self.eat(TokenKind::Semi) {
                continue; // `;` suelto entre tipos
            }
            // Recuperación: si una declaración de tipo/módulo falla, se registra el error y se
            // **sincroniza** al comienzo de la próxima, en vez de abortar la unidad entera.
            if let Err(e) = self.top_decl(&mut types, &mut module) {
                self.errors.push(e);
                self.sync_top_level();
            }
        }
        CompilationUnit { package, imports, types, module }
    }

    /// El nombre de un `package` + su `;`.
    fn package_clause(&mut self) -> Result<String> {
        let name = self.qualified_name()?;
        self.expect(TokenKind::Semi)?;
        Ok(name)
    }

    /// Parsea **una** declaración de nivel superior (tipo o módulo) y la agrega.
    fn top_decl(&mut self, types: &mut Vec<ClassDecl>, module: &mut Option<ModuleDecl>) -> Result<()> {
        // El doc comment se captura antes de los modificadores: cuelga del token que abre la declaración.
        let doc = self.peek().doc.clone();
        let (modifiers, annotations) = self.modifiers()?;
        // `module-info.java` (§7.7): una **declaración de módulo** en vez de tipos. `module`/`open`
        // son keywords **restringidas** (identificadores); no llevan modificadores.
        if module.is_none() && modifiers.is_empty() && self.at_module_decl() {
            *module = Some(self.module_decl(annotations)?);
        } else {
            types.push(self.class_decl(doc, modifiers, annotations)?);
        }
        Ok(())
    }

    // ---- recuperación de errores (panic-mode, §ninguno del JLS: es del compilador) ----

    /// Salta al comienzo de la próxima **declaración de tipo** (o EOF), rastreando llaves para no
    /// confundirse con un `{ … }` de un cuerpo a medio parsear. Consume **≥1** token (garantiza avance).
    fn sync_top_level(&mut self) {
        use TokenKind as T;
        let start = self.pos;
        let mut depth = 0usize;
        while !self.at(T::Eof) {
            if depth == 0 && self.pos > start && self.at_type_start() {
                break;
            }
            match self.peek_kind() {
                T::LBrace => depth += 1,
                T::RBrace if depth > 0 => depth -= 1,
                _ => {}
            }
            self.bump();
        }
    }

    /// Salta (rastreando llaves) hasta el próximo `;` (que se **consume**) o el `}` que cierra el
    /// cuerpo actual (clase o bloque), que **no** se consume —lo cierra el llamador—. Es la
    /// sincronización a nivel **miembro** y **sentencia**. Avanza ≥1 o el loop del llamador termina.
    fn sync_to_body_boundary(&mut self) {
        use TokenKind as T;
        let mut depth = 0usize;
        loop {
            match self.peek_kind() {
                T::Eof => break,
                T::RBrace if depth == 0 => break, // cierra el cuerpo: se lo deja al llamador
                T::RBrace => {
                    depth -= 1;
                    self.bump();
                }
                T::LBrace => {
                    depth += 1;
                    self.bump();
                }
                T::Semi if depth == 0 => {
                    self.bump();
                    break;
                }
                _ => {
                    self.bump();
                }
            }
        }
    }

    /// Sincroniza a un límite de **expresión** para la recuperación a nivel expresión: salta
    /// (rastreando `()`/`[]`/`{}`) hasta el próximo `,` `;` `)` `]` `}` de profundidad 0, o EOF —
    /// **sin consumirlo**, para que lo vea el llamador (la lista de argumentos, el `;` del local)—.
    fn sync_to_expr_boundary(&mut self) {
        use TokenKind as T;
        let mut depth = 0usize;
        loop {
            match self.peek_kind() {
                T::Eof => break,
                T::Comma | T::Semi | T::RParen | T::RBracket | T::RBrace if depth == 0 => break,
                T::LParen | T::LBracket | T::LBrace => {
                    depth += 1;
                    self.bump();
                }
                T::RParen | T::RBracket | T::RBrace => {
                    depth -= 1;
                    self.bump();
                }
                _ => {
                    self.bump();
                }
            }
        }
    }

    /// **Recuperación a nivel expresión**: registra `err`, sincroniza hasta un límite de expresión
    /// ([`sync_to_expr_boundary`](Self::sync_to_expr_boundary)) y devuelve un nodo [`ExprKind::Error`]
    /// en `pos`. Así la **estructura de alrededor sobrevive** —el local se sigue declarando, los otros
    /// argumentos se siguen parseando— y la atribución no encadena diagnósticos sobre lo ya roto.
    fn recover_expr(&mut self, pos: Pos, err: Error) -> Expr {
        self.errors.push(err);
        self.sync_to_expr_boundary();
        Expr::new(pos, ExprKind::Error)
    }

    /// ¿El token actual puede **comenzar** una declaración de tipo? (Para sincronizar en top-level.)
    fn at_type_start(&self) -> bool {
        use TokenKind as T;
        matches!(
            self.peek_kind(),
            T::Public
                | T::Private
                | T::Protected
                | T::Static
                | T::Final
                | T::Abstract
                | T::Class
                | T::Interface
                | T::Enum
                | T::MonkeysAt
        ) || (self.at(T::Identifier)
            && matches!(self.peek().text.as_str(), "record" | "module" | "open" | "sealed" | "non"))
    }

    /// ¿Estamos ante una declaración de módulo? `module nombre` o `open module nombre` (§7.7).
    fn at_module_decl(&self) -> bool {
        use TokenKind as T;
        if self.at(T::Identifier) && self.peek().text == "open" {
            self.kind_at(1) == T::Identifier && self.text_at(1) == "module"
        } else {
            self.at(T::Identifier) && self.peek().text == "module" && self.kind_at(1) == T::Identifier
        }
    }

    /// `[open] module nombre.cualificado { directivas }` (§7.7).
    fn module_decl(&mut self, annotations: Vec<Annotation>) -> Result<ModuleDecl> {
        use TokenKind as T;
        let pos = self.pos();
        let open = self.at(T::Identifier) && self.peek().text == "open";
        if open {
            self.bump();
        }
        if !(self.at(T::Identifier) && self.peek().text == "module") {
            return Err(self.error("se esperaba `module`"));
        }
        self.bump();
        let name = self.qualified_name()?;
        self.expect(T::LBrace)?;
        let mut directives = Vec::new();
        while !self.at(T::RBrace) && !self.at(T::Eof) {
            directives.push(self.module_directive()?);
        }
        self.expect(T::RBrace)?;
        Ok(ModuleDecl { pos, annotations, open, name, directives })
    }

    /// Una directiva `requires`/`exports`/`opens`/`uses`/`provides` (§7.7.1–§7.7.4). Todas las
    /// palabras clave (incluidas `transitive`/`to`/`with`) son **restringidas** salvo `static`.
    fn module_directive(&mut self) -> Result<ModuleDirective> {
        use TokenKind as T;
        if !self.at(T::Identifier) {
            return Err(self.error("se esperaba una directiva de módulo"));
        }
        let kw = self.peek().text.clone();
        self.bump();
        let d = match kw.as_str() {
            "requires" => {
                // `requires [transitive] [static] modulo;` — los modificadores en cualquier orden.
                // `transitive` seguido de `;` es en cambio el **nombre** del módulo (`requires transitive;`).
                let (mut transitive, mut is_static) = (false, false);
                loop {
                    if self.at(T::Static) {
                        is_static = true;
                        self.bump();
                    } else if self.at(T::Identifier)
                        && self.peek().text == "transitive"
                        && self.kind_at(1) != T::Semi
                    {
                        transitive = true;
                        self.bump();
                    } else {
                        break;
                    }
                }
                ModuleDirective::Requires { transitive, is_static, name: self.qualified_name()? }
            }
            "exports" | "opens" => {
                let package = self.qualified_name()?;
                let mut to = Vec::new();
                if self.at(T::Identifier) && self.peek().text == "to" {
                    self.bump();
                    to.push(self.qualified_name()?);
                    while self.eat(T::Comma) {
                        to.push(self.qualified_name()?);
                    }
                }
                if kw == "exports" {
                    ModuleDirective::Exports { package, to }
                } else {
                    ModuleDirective::Opens { package, to }
                }
            }
            "uses" => ModuleDirective::Uses { service: self.qualified_name()? },
            "provides" => {
                let service = self.qualified_name()?;
                if !(self.at(T::Identifier) && self.peek().text == "with") {
                    return Err(self.error("se esperaba `with` en un `provides`"));
                }
                self.bump();
                let mut with = vec![self.qualified_name()?];
                while self.eat(T::Comma) {
                    with.push(self.qualified_name()?);
                }
                ModuleDirective::Provides { service, with }
            }
            other => return Err(self.error(format!("directiva de módulo desconocida: `{other}`"))),
        };
        self.expect(T::Semi)?;
        Ok(d)
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
        // §7.5 — un `import` de tipo unico necesita un nombre **cualificado**: un tipo del paquete
        // sin nombre no se puede importar, asi que `import IntStream;` no es Java. Se aceptaba en
        // silencio y el tipo resolvia igual por otro camino, que es lo que lo hacia invisible.
        // El `javac` real lo reporta como error de sintaxis: `'.' expected`.
        if !path.contains('.') && !wildcard {
            return Err(self.error("se esperaba `.`: un `import` necesita un nombre cualificado".to_string()));
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
            // `sealed` / `non-sealed` son keywords **restringidas** (§3.9): sólo modifican una
            // declaración de tipo. El lookahead evita confundir un tipo/campo llamado `sealed`
            // (`sealed x;`) con el modificador. `non-sealed` lo lexea el scanner como `non` `-`
            // `sealed`; acá se reconstruye.
            if self.at(T::Identifier) && self.peek().text == "sealed" && self.type_decl_ahead(1) {
                self.bump();
                mods.push(Modifier::Sealed);
                continue;
            }
            if self.at(T::Identifier)
                && self.peek().text == "non"
                && self.kind_at(1) == T::Sub
                && self.kind_at(2) == T::Identifier
                && self.text_at(2) == "sealed"
                && self.type_decl_ahead(3)
            {
                self.bump(); // non
                self.bump(); // -
                self.bump(); // sealed
                mods.push(Modifier::NonSealed);
                continue;
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

    fn class_decl(&mut self, doc: Option<String>, modifiers: Vec<Modifier>, annotations: Vec<Annotation>) -> Result<ClassDecl> {
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
        let mut extends_annos = Vec::new();
        let mut implements = Vec::new();
        let mut implements_annos: Vec<Vec<TypeUseAnnot>> = Vec::new();
        if kind != TypeKind::Record && self.eat(T::Extends) {
            let first = self.parse_type()?;
            let first_annos = self.take_type_annos();
            if kind == TypeKind::Interface {
                // Un `interface` no tiene superclase: su `extends` son super-interfaces, que van a la
                // lista de `implements` (target `0x10` con `supertype_index` 0-based).
                implements.push(first);
                implements_annos.push(first_annos);
                while self.eat(T::Comma) {
                    implements.push(self.parse_type()?);
                    implements_annos.push(self.take_type_annos());
                }
            } else {
                extends = Some(first);
                extends_annos = first_annos;
            }
        }
        if self.eat(T::Implements) {
            implements.push(self.parse_type()?);
            implements_annos.push(self.take_type_annos());
            while self.eat(T::Comma) {
                implements.push(self.parse_type()?);
                implements_annos.push(self.take_type_annos());
            }
        }

        // `permits A, B` — los subtipos autorizados de un tipo `sealed` (§8.1.6). `permits` es una
        // keyword contextual (un identificador); sólo cuenta acá, antes del cuerpo.
        let mut permits = Vec::new();
        if self.at(T::Identifier) && self.peek().text == "permits" {
            self.bump();
            permits.push(self.parse_type()?);
            while self.eat(T::Comma) {
                permits.push(self.parse_type()?);
            }
        }
        // `permits` no es una posición de type annotation (no hay target JVMS); si alguna quedó en el
        // buffer, se descarta para no filtrarla al primer miembro.
        self.pending_type_annos.clear();

        self.expect(T::LBrace)?;
        // Un `enum` lleva sus constantes antes de los miembros.
        let enum_constants = if kind == TypeKind::Enum { self.enum_constants()? } else { Vec::new() };
        let mut members = Vec::new();
        let mut annotation_defaults = Vec::new();
        while !self.at(T::RBrace) && !self.at(T::Eof) {
            // Recuperación: un miembro mal formado se registra y se sincroniza al próximo `;`/borde,
            // sin abortar el resto del cuerpo de la clase.
            if let Err(e) = self.member(&name, &mut members, &mut annotation_defaults) {
                self.errors.push(e);
                self.sync_to_body_boundary();
            }
        }
        self.expect(T::RBrace)?;
        Ok(ClassDecl { pos, doc, annotations, modifiers, kind, name, type_params, components, extends, extends_annos, implements, implements_annos, permits, enum_constants, members, annotation_defaults })
    }

    /// ¿Estamos ante `record Nombre(`? (`record` es keyword contextual — un identificador).
    fn is_record_decl(&self) -> bool {
        // `record Nombre(` o —para uno **genérico**— `record Nombre<`. En posición de declaración
        // de tipo no hay ambigüedad con un campo de tipo `record` (`record x;`, `<` no lo sigue).
        self.at(TokenKind::Identifier)
            && self.peek().text == "record"
            && self.kind_at(1) == TokenKind::Identifier
            && matches!(self.kind_at(2), TokenKind::LParen | TokenKind::Lt)
    }

    /// El texto del token en `pos + offset` (para lookahead de keywords contextuales).
    fn text_at(&self, offset: usize) -> &str {
        &self.tokens[(self.pos + offset).min(self.tokens.len() - 1)].text
    }

    /// Mirando desde `pos + start` y **saltando** más modificadores (incluidas otras keywords
    /// restringidas y anotaciones), ¿lo que sigue es una declaración de tipo? Es lo que distingue
    /// `sealed class C` (modificador) de `sealed x;` (un tipo/campo llamado `sealed`).
    fn type_decl_ahead(&self, start: usize) -> bool {
        use TokenKind as T;
        let mut i = self.pos + start;
        let last = self.tokens.len() - 1;
        loop {
            let k = self.tokens[i.min(last)].kind;
            let is_mod = matches!(
                k,
                T::Public
                    | T::Private
                    | T::Protected
                    | T::Static
                    | T::Final
                    | T::Abstract
                    | T::Native
                    | T::Synchronized
                    | T::Transient
                    | T::Volatile
                    | T::Strictfp
                    | T::Default
                    | T::MonkeysAt
            );
            let txt = &self.tokens[i.min(last)].text;
            let is_restricted = k == T::Identifier && (txt == "sealed" || txt == "non");
            if is_mod || is_restricted {
                if i >= last {
                    return false;
                }
                i += 1;
                continue;
            }
            return matches!(k, T::Class | T::Interface | T::Enum)
                || (k == T::Identifier && txt == "record");
        }
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
            // Anotaciones sobre el parámetro de tipo (§9.7.4: `<@Foo T>`): ahora se **retienen**.
            let mut annotations = Vec::new();
            while self.at(T::MonkeysAt) {
                annotations.push(self.annotation()?);
            }
            let name = self.expect_ident()?;
            // `extends A & B` — la primera cota puede ser una clase; las demás, interfaces. Las type
            // annotations de cada cota (`<T extends @A A & @B B>`) se drenan **en paralelo** a `bounds`.
            let mut bounds = Vec::new();
            let mut bound_annos: Vec<Vec<TypeUseAnnot>> = Vec::new();
            if self.eat(T::Extends) {
                bounds.push(self.parse_type()?);
                bound_annos.push(self.take_type_annos());
                while self.eat(T::Amp) {
                    bounds.push(self.parse_type()?);
                    bound_annos.push(self.take_type_annos());
                }
            }
            params.push(TypeParam { annotations, name, bounds, bound_annos });
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
        self.type_args_at(&[])
    }

    /// [`type_args`] sabiendo el `type_path` del tipo dueño: el i-ésimo argumento está en
    /// `path + [TypeArgument i]` (para sus type annotations).
    fn type_args_at(&mut self, path: &[TypePathStep]) -> Result<Option<Vec<TypeArg>>> {
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
            let mut arg_path = path.to_vec();
            arg_path.push(TypePathStep::TypeArgument(args.len() as u8));
            args.push(self.type_arg_at(arg_path)?);
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

    /// Un argumento de tipo (`type_arg`) con el `type_path` de este argumento. La cota de un wildcard añade un paso
    /// `WildcardBound` (`List<? extends @A T>` → `@A` en `path + [WildcardBound]`).
    fn type_arg_at(&mut self, path: Vec<TypePathStep>) -> Result<TypeArg> {
        use TokenKind as T;
        if self.eat(T::Ques) {
            if self.eat(T::Extends) {
                let mut p = path;
                p.push(TypePathStep::WildcardBound);
                return Ok(TypeArg::Extends(Box::new(self.parse_type_at(p)?)));
            }
            if self.eat(T::Super) {
                let mut p = path;
                p.push(TypePathStep::WildcardBound);
                return Ok(TypeArg::Super(Box::new(self.parse_type_at(p)?)));
            }
            return Ok(TypeArg::Wildcard);
        }
        Ok(TypeArg::Type(self.parse_type_at(path)?))
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
            // El doc comment cuelga del token que abre la constante (antes de sus anotaciones).
            let doc = self.peek().doc.clone();
            // Anotaciones sobre la constante (`@Deprecated FOO`): ahora se **retienen**.
            let mut annotations = Vec::new();
            while self.at(T::MonkeysAt) {
                annotations.push(self.annotation()?);
            }
            let name = self.expect_ident()?;
            let args = if self.at(T::LParen) { self.args()? } else { Vec::new() };
            // Cuerpo de clase por constante: se salta por ahora.
            if self.at(T::LBrace) {
                self.skip_balanced(T::LBrace, T::RBrace)?;
            }
            constants.push(EnumConstant { doc, annotations, name, args });
            if !self.eat(T::Comma) || self.at(T::Semi) || self.at(T::RBrace) {
                break;
            }
        }
        self.eat(T::Semi); // separador opcional entre constantes y miembros
        Ok(constants)
    }

    fn member(
        &mut self,
        class_name: &str,
        members: &mut Vec<Member>,
        defaults: &mut Vec<(String, AnnotationValue)>,
    ) -> Result<()> {
        // El doc comment se captura antes de los modificadores: cuelga del token que abre el miembro.
        let doc = self.peek().doc.clone();
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
            let nested = self.class_decl(doc, modifiers, annotations)?;
            members.push(Member::Type(nested));
            return Ok(());
        }

        // Parámetros de tipo de un método/constructor genérico (`<T> ...`). `type_params` ya drenó las
        // type annotations de las **cotas** (`<T extends @A Number>`, target 0x11/0x12) a
        // `TypeParam::bound_annos`; el buffer queda limpio para el tipo del miembro.
        let type_params = self.type_params()?;

        // Constructor: `Nombre(` con Nombre == la clase.
        if self.at(TokenKind::Identifier)
            && self.peek().text == class_name
            && self.kind_at(1) == TokenKind::LParen
        {
            self.bump(); // nombre
            let params = self.params()?;
            let (throws, throws_annos) = self.throws_clause()?;
            let body = self.method_body()?;
            members.push(Member::Method(MethodDecl {
                pos,
                doc,
                annotations,
                modifiers,
                type_params,
                return_type: Type::Void,
                name: class_name.to_string(),
                params,
                throws,
                body,
                is_constructor: true,
                return_annos: Vec::new(),
                throws_annos,
            }));
            return Ok(());
        }

        let ty = self.parse_type()?;
        // Las type annotations del tipo del miembro (retorno de un método, o tipo de un campo).
        let type_annos = self.take_type_annos();
        let name = self.expect_ident()?;

        if self.at(TokenKind::LParen) {
            let params = self.params()?;
            let (throws, throws_annos) = self.throws_clause()?;
            // Elemento de `@interface` con valor por defecto: `Tipo nombre() default valor;`. El valor
            // se retiene (como `AnnotationValue`, igual que un argumento de anotación) para el atributo
            // `AnnotationDefault` (§4.7.22) — la reflexión lee estos defaults.
            if self.eat(TokenKind::Default) {
                let value = self.annotation_value()?;
                defaults.push((name.clone(), value));
            }
            let body = self.method_body()?;
            members.push(Member::Method(MethodDecl {
                pos,
                doc,
                annotations,
                modifiers,
                type_params,
                return_type: ty,
                name,
                params,
                throws,
                body,
                is_constructor: false,
                return_annos: type_annos,
                throws_annos,
            }));
        } else {
            // Campo(s): uno o más declaradores separados por coma.
            let declare = |p: &mut Self, nm: String| -> Result<FieldDecl> {
                // Igual que en un local: los `[]` post-nombre y el inicializador de array. Las
                // anotaciones se replican a cada declarador (`@Foo int a, b;` anota los dos).
                let fty = p.extra_array_dims(ty.clone())?;
                let init = if p.eat(TokenKind::Eq) { Some(p.var_init(&fty)?) } else { None };
                Ok(FieldDecl { pos, doc: doc.clone(), annotations: annotations.clone(), modifiers: modifiers.clone(), ty: fty, name: nm, init, type_annos: type_annos.clone() })
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
    /// de excepciones chequeadas la necesita. Devuelve además, **en paralelo**, las type annotations de
    /// cada tipo (`throws @A E1, @B E2` → target `0x17` con el índice en la cláusula).
    fn throws_clause(&mut self) -> Result<(Vec<Type>, Vec<Vec<TypeUseAnnot>>)> {
        let mut types = Vec::new();
        let mut annos: Vec<Vec<TypeUseAnnot>> = Vec::new();
        if self.eat(TokenKind::Throws) {
            types.push(self.parse_type()?);
            annos.push(self.take_type_annos());
            while self.eat(TokenKind::Comma) {
                types.push(self.parse_type()?);
                annos.push(self.take_type_annos());
            }
        }
        Ok((types, annos))
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
                let type_annos = self.take_type_annos();
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
                params.push(Param { annotations, ty, name, varargs, is_final, type_annos });
                if !self.eat(TokenKind::Comma) {
                    break;
                }
            }
        }
        self.expect(TokenKind::RParen)?;
        Ok(params)
    }

    // ---- tipos ----

    /// Drena las type annotations recolectadas al parsear el último tipo (con su `type_path`), para
    /// que la declaración las guarde con su target. Vacía el buffer.
    fn take_type_annos(&mut self) -> Vec<TypeUseAnnot> {
        std::mem::take(&mut self.pending_type_annos)
    }

    fn parse_type(&mut self) -> Result<Type> {
        self.parse_type_at(Vec::new())
    }

    /// Como [`parse_type`], sabiendo el `type_path` de **esta** posición. Recolecta las type
    /// annotations (§9.7.4) en `pending_type_annos`: las **líder** (`@A String`) sobre el tipo base, y
    /// las de **nivel de array** (`String @A []`). El k-ésimo `[` (desde afuera) queda en
    /// `path + [Array × k]`; el tipo base, bajo N arrays, en `path + [Array × N]`. Los argumentos de
    /// tipo los recolecta [`Parser::type_args_at`] con `path + [TypeArgument i]`.
    fn parse_type_at(&mut self, path: Vec<TypePathStep>) -> Result<Type> {
        let leading = self.take_type_annotations()?;
        // Los argumentos de tipo del base (`List<@A String>`) se recolectan **ya**, con path
        // `path + [TypeArgument i]`, sin saber todavía cuántos `[]` lo envuelven. Se marca hasta dónde
        // llega el buffer para, sabido `level`, intercalarles los pasos `Array` que faltan.
        let mark = self.pending_type_annos.len();
        let mut base = self.base_type_at(&path)?;
        let mut arr: Vec<(usize, Annotation)> = Vec::new();
        let mut level = 0usize;
        loop {
            let anns = self.take_type_annotations()?;
            let is_array =
                self.at(TokenKind::LBracket) && self.kind_at(1) == TokenKind::RBracket;
            for a in anns {
                arr.push((level, a));
            }
            if is_array {
                self.bump();
                self.bump();
                base = Type::Array(Box::new(base));
                level += 1;
            } else {
                break;
            }
        }
        // `List<@A String> []` → `@A` en `[ARRAY, TYPE_ARGUMENT(0)]`, no `[TYPE_ARGUMENT(0)]`: el
        // elemento del array está un nivel más abajo. Se inserta `Array × level` justo tras el prefijo
        // `path` (compartido) y antes de los pasos propios del argumento.
        if level > 0 {
            let insert = vec![TypePathStep::Array; level];
            for entry in &mut self.pending_type_annos[mark..] {
                entry.path.splice(path.len()..path.len(), insert.iter().cloned());
            }
        }
        let at = |p: &[TypePathStep], k: usize| -> Vec<TypePathStep> {
            let mut v = p.to_vec();
            v.extend(std::iter::repeat(TypePathStep::Array).take(k));
            v
        };
        let base_path = at(&path, level); // el base está bajo `level` arrays
        for a in leading {
            self.pending_type_annos.push(TypeUseAnnot { path: base_path.clone(), annotation: a });
        }
        for (k, a) in arr {
            self.pending_type_annos.push(TypeUseAnnot { path: at(&path, k), annotation: a });
        }
        Ok(base)
    }

    /// Parsea (y **retiene**) las type annotations líder que preceden a un tipo o a un `[`,
    /// devolviéndolas. Antes se descartaban; ahora van al atributo `RuntimeVisibleTypeAnnotations`.
    fn take_type_annotations(&mut self) -> Result<Vec<Annotation>> {
        let mut anns = Vec::new();
        while self.at(TokenKind::MonkeysAt) {
            anns.push(self.annotation()?);
        }
        Ok(anns)
    }

    fn base_type(&mut self) -> Result<Type> {
        self.base_type_at(&[])
    }

    /// [`base_type`] sabiendo el `type_path` de esta posición, para pasarle a [`Parser::type_args_at`]
    /// el prefijo `path + [TypeArgument i]` de cada argumento.
    fn base_type_at(&mut self, path: &[TypePathStep]) -> Result<Type> {
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
                return Ok(match self.type_args_at(path)? {
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
            // Recuperación: una sentencia mal formada se registra y se sincroniza al próximo `;`/`}`,
            // sin abortar el resto del bloque.
            if let Err(e) = self.block_stmt(&mut stmts) {
                self.errors.push(e);
                self.sync_to_body_boundary();
            }
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
            let doc = self.peek().doc.clone();
            let (modifiers, annotations) = self.modifiers()?;
            let decl = self.class_decl(doc, modifiers, annotations)?;
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
        // `parse_type` empuja las type annotations al buffer; con backtracking hay que devolverlo.
        let annos_mark = self.pending_type_annos.len();
        // Un local puede declararse `final` (JLS §14.4); es el único modificador que admite.
        let is_final = self.eat(TokenKind::Final);
        let Ok(ty) = self.parse_type() else {
            self.pending_type_annos.truncate(annos_mark);
            self.reset(save);
            return Ok(None);
        };
        if !self.at(TokenKind::Identifier) {
            self.pending_type_annos.truncate(annos_mark);
            self.reset(save);
            return Ok(None);
        }
        // Las anotaciones de tipo del tipo declarado (`@A int x`), compartidas por todos los
        // declaradores (`@A int a, b`). Target 0x40; el emisor filtra por `@Target(TYPE_USE)`.
        let type_annos = self.pending_type_annos.split_off(annos_mark);
        let mut decls = Vec::new();
        loop {
            let pos = self.pos();
            let name = self.expect_ident()?;
            // Declarador **al estilo C**: `int y[]` es lo mismo que `int[] y` (§10.2), y los `[]`
            // van por variable, no por declaración: en `int a[], b;` solo `a` es array.
            let vty = self.extra_array_dims(ty.clone())?;
            // Un inicializador que no parsea se recupera con un nodo de error: el local **igual queda
            // declarado**, así que los usos posteriores de la variable no cascadean «no se encuentra».
            let init = if self.eat(TokenKind::Eq) {
                let ipos = self.pos();
                Some(self.var_init(&vty).unwrap_or_else(|err| self.recover_expr(ipos, err)))
            } else {
                None
            };
            decls.push(Stmt::new(
                pos,
                StmtKind::LocalVar { ty: vty, name, init, is_final, type_annos: type_annos.clone() },
            ));
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
                // Una etiqueta constante es una *conditional-expression* (§14.11.1), **no** una
                // *assignment-expression*: usar `expr()` haría que `case RED -> 1` se lea como la
                // lambda `RED -> 1`. `ternary()` corta justo debajo de lambda/asignación.
                labels.push(CaseLabel::Constant(self.ternary()?));
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
            vec![Param { annotations: Vec::new(), ty: Type::Var, name, varargs: false, is_final: false, type_annos: Vec::new() }]
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
            return Ok(Param { annotations: Vec::new(), ty: Type::Var, name, varargs: false, is_final, type_annos: Vec::new() });
        }
        let ty = self.parse_type()?;
        let name = self.expect_ident()?;
        Ok(Param { annotations: Vec::new(), ty, name, varargs: false, is_final, type_annos: Vec::new() })
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
                let type_annos = self.take_type_annos(); // `e instanceof @A T` — target 0x43
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
                )
                .with_type_annos(type_annos);
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

    // El literal entero que sigue a un menos unario, ya con el signo adentro. Devuelve `None` si
    // lo que sigue no es un literal entero, y entonces el llamador arma el `Unary` de siempre.
    fn negated_int_literal(&mut self) -> Option<Expr> {
        use TokenKind as T;
        let tok = self.peek().clone();
        let pos = Pos { line: tok.line, col: tok.col };
        let kind = match tok.kind {
            T::IntLiteral => ExprKind::IntLit(parse_negated_int_literal(&tok.text)?),
            T::LongLiteral => ExprKind::LongLit(parse_negated_int_literal(&tok.text)?),
            _ => return None,
        };
        self.bump();
        Some(Expr::new(pos, kind))
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
            // El menos unario delante de un literal entero **pliega el signo** (§3.10.1). No es
            // una optimización: `2147483648` y `9223372036854775808L` son las magnitudes de los
            // dos MIN_VALUE, que no tienen contraparte positiva, y la gramática los admite
            // **únicamente** aquí. Plegarlos en este punto es lo que los hace parsear sin
            // aceptarlos sueltos, que es lo que la especificación pide.
            if op == UnOp::Neg {
                if let Some(lit) = self.negated_int_literal() {
                    return Ok(lit);
                }
            }
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
        // `parse_type` empuja las type annotations al buffer; si el intento de cast se descarta
        // (backtracking), hay que devolver el buffer a como estaba para no filtrarlas.
        let annos_mark = self.pending_type_annos.len();
        self.bump(); // (
        let Ok(ty) = self.parse_type() else {
            self.pending_type_annos.truncate(annos_mark);
            self.reset(save);
            return Ok(None);
        };
        if !self.at(TokenKind::RParen) {
            self.pending_type_annos.truncate(annos_mark);
            self.reset(save);
            return Ok(None);
        }
        let is_prim_or_array = matches!(ty, Type::Prim(_) | Type::Array(_));
        let ok = is_prim_or_array || cast_operand_start(self.kind_at(1));
        if !ok {
            self.pending_type_annos.truncate(annos_mark);
            self.reset(save);
            return Ok(None);
        }
        self.bump(); // )
        // Las anotaciones del tipo destino (`(@A T) e`, target 0x47) — se sacan **antes** de parsear el
        // operando, que puede tener sus propias (otro cast/`new` anidado).
        let type_annos = self.pending_type_annos.split_off(annos_mark);
        let expr = self.unary()?;
        Ok(Some(
            Expr::new(cast_pos, ExprKind::Cast { ty, expr: Box::new(expr) }).with_type_annos(type_annos),
        ))
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
                // `C[].class` (§15.8.2): el literal de clase de un tipo **array**. Un `[` seguido
                // inmediatamente de `]` no puede ser un acceso a arreglo —el índice no es
                // opcional—, así que la única lectura posible es un tipo array, y detrás de él
                // sólo puede venir `.class`. El camino de los primitivos ya lo hacía (`int[]
                // .class`); éste es el mismo para los tipos de referencia.
                TokenKind::LBracket if self.kind_at(1) == TokenKind::RBracket => {
                    let Some(name) = Self::type_name_of(&e) else {
                        return Err(self.error("`[].class` necesita un nombre de tipo".to_string()));
                    };
                    let mut ty = Type::Class(name);
                    while self.at(TokenKind::LBracket) && self.kind_at(1) == TokenKind::RBracket {
                        self.bump();
                        self.bump();
                        ty = Type::Array(Box::new(ty));
                    }
                    self.expect(TokenKind::Dot)?;
                    self.expect(TokenKind::Class)?;
                    e = Expr::new(pos, ExprKind::ClassLit(ty));
                }
                // `Outer.this` (§15.8.4): el `this` **cualificado** de la clase envolvente. `this` es
                // keyword, así que —como `.class`— se reconoce acá, reconstruyendo el tipo del nombre.
                TokenKind::Dot if self.kind_at(1) == TokenKind::This => {
                    self.bump(); // '.'
                    self.bump(); // 'this'
                    let Some(name) = Self::type_name_of(&e) else {
                        return Err(self.error("`.this` necesita un nombre de tipo".to_string()));
                    };
                    e = Expr::new(pos, ExprKind::QualifiedThis(Type::Class(name)));
                }
                // `outer.new Inner(args)` (§15.9.2): creación **cualificada** de una interna, con
                // `outer` de instancia envolvente. `new` es keyword: se reconoce acá.
                TokenKind::Dot if self.kind_at(1) == TokenKind::New => {
                    self.bump(); // '.'
                    self.bump(); // 'new'
                    let ty = self.base_type()?;
                    let args = self.args()?;
                    let body = if self.at(TokenKind::LBrace) { Some(self.class_body("")?) } else { None };
                    e = Expr::new(
                        pos,
                        ExprKind::NewObject { ty, args, body, outer: Some(Box::new(e)) },
                    );
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
                ExprKind::StringLit(
                    decode_string(&tok.text).map_err(|m| self.error(m.to_string()))?,
                )
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
            // `int.class` / `void.class` / `int[].class` (§15.8.2). El nombre del tipo es una
            // **keyword**, así que no llega por el camino de `Identifier` y hay que reconocerlo acá.
            // Es la **única** forma en que un primitivo aparece donde va una expresión, así que lo
            // que sigue solo puede ser `[]`* `.class`; cualquier otra cosa es un error de sintaxis.
            T::Int
            | T::Long
            | T::Short
            | T::Byte
            | T::Char
            | T::Boolean
            | T::Float
            | T::Double
            | T::Void => {
                let mut ty = self.base_type()?;
                while self.at(T::LBracket) && self.kind_at(1) == T::RBracket {
                    self.bump();
                    self.bump();
                    ty = Type::Array(Box::new(ty));
                }
                self.expect(T::Dot)?;
                self.expect(T::Class)?;
                ExprKind::ClassLit(ty)
            }
            other => return Err(self.error(format!("se esperaba una expresión, se encontró {other:?}"))),
        };
        Ok(Expr::new(pos, e))
    }

    fn new_expr(&mut self) -> Result<Expr> {
        let pos = self.pos();
        self.expect(TokenKind::New)?;
        // *Type witness* de constructor `new <T>Foo()` (§15.9): fija los parámetros de tipo del
        // constructor. Se **acepta** (ya no da error); no afecta la erasure ni la emisión, así que
        // no se retiene — es una forma rarísima.
        if self.at(TokenKind::Lt) {
            self.type_args()?;
        }
        // Anotaciones sobre el tipo creado (`new @A Foo(...)`, `new @A int[]`), target 0x44. El buffer
        // se marca para sacar **solo** las de este `new` (los args/dims anidados empujan las suyas).
        let annos_mark = self.pending_type_annos.len();
        let leading = self.take_type_annotations()?; // `new @A Foo` — sobre el tipo elemento
        let elem = self.base_type_at(&[])?; // args de tipo del elemento → buffer, con su path
        if self.at(TokenKind::LBracket) {
            // `new Elem @A [n] @B []` — cada `[` puede llevar anotaciones sobre esa dimensión de array.
            let mut dims = Vec::new();
            let mut dim_annos: Vec<(usize, Annotation)> = Vec::new();
            loop {
                let anns = self.take_type_annotations()?;
                if !self.at(TokenKind::LBracket) {
                    // Anotaciones sueltas que no preceden a un `[`: no es una posición válida; se sueltan.
                    break;
                }
                for a in anns {
                    dim_annos.push((dims.len(), a));
                }
                self.bump(); // [
                if self.eat(TokenKind::RBracket) {
                    dims.push(None);
                } else {
                    let len = self.expr()?;
                    self.expect(TokenKind::RBracket)?;
                    dims.push(Some(len));
                }
            }
            let ndims = dims.len();
            // El tipo creado es `Elem[]…[]` (`ndims` arrays). El elemento está bajo `ndims` pasos
            // `Array`; la k-ésima dimensión (desde afuera) bajo `k`. Los args de tipo del elemento, que
            // `base_type_at` dejó con path `[TypeArgument…]`, se prefijan con `Array × ndims`.
            let nested = self.pending_type_annos.split_off(annos_mark);
            let mut type_annos: Vec<TypeUseAnnot> = Vec::new();
            let array_prefix = |k: usize| vec![TypePathStep::Array; k];
            for a in leading {
                type_annos.push(TypeUseAnnot { path: array_prefix(ndims), annotation: a });
            }
            for (k, a) in dim_annos {
                type_annos.push(TypeUseAnnot { path: array_prefix(k), annotation: a });
            }
            for mut ta in nested {
                let mut path = array_prefix(ndims);
                path.append(&mut ta.path);
                type_annos.push(TypeUseAnnot { path, annotation: ta.annotation });
            }
            let init = if self.at(TokenKind::LBrace) { Some(self.array_init()?) } else { None };
            Ok(Expr::new(pos, ExprKind::NewArray { elem, dims, init }).with_type_annos(type_annos))
        } else {
            // `new @A Foo(...)`: la anotación líder va con path vacío; las anidadas del tipo elemento
            // (`new Map<@A K, V>()`) con su path. Se sacan **antes** de los args (que pueden anidar).
            let mut type_annos: Vec<TypeUseAnnot> = leading
                .into_iter()
                .map(|a| TypeUseAnnot { path: Vec::new(), annotation: a })
                .collect();
            type_annos.extend(self.pending_type_annos.split_off(annos_mark));
            let args = self.args()?;
            // `new Type(args) { … }` — **clase anónima** (§15.9.5). El cuerpo se parsea con la misma
            // maquinaria que el de una clase; el nombre vacío la marca (y hace que ningún método se
            // tome por constructor, que una anónima no puede declarar, §15.9.5.1).
            let body = if self.at(TokenKind::LBrace) {
                Some(self.class_body("")?)
            } else {
                None
            };
            Ok(Expr::new(pos, ExprKind::NewObject { ty: elem, args, body, outer: None })
                .with_type_annos(type_annos))
        }
    }

    /// El cuerpo `{ member* }` de una clase — reutilizado por la declaración de tipo y por la clase
    /// anónima. Asume que el token actual es `{`.
    fn class_body(&mut self, name: &str) -> Result<Vec<Member>> {
        use TokenKind as T;
        self.expect(T::LBrace)?;
        let mut members = Vec::new();
        let mut defaults = Vec::new();
        while !self.at(T::RBrace) && !self.at(T::Eof) {
            self.member(name, &mut members, &mut defaults)?;
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
            args.push(self.arg_or_recover());
            while self.eat(TokenKind::Comma) {
                args.push(self.arg_or_recover());
            }
        }
        self.expect(TokenKind::RParen)?;
        Ok(args)
    }

    /// Un argumento de llamada, con **recuperación a nivel expresión**: si no parsea, se registra el
    /// error y queda un nodo de error, así los **demás** argumentos se siguen parseando (`f(bad, ok)`
    /// no pierde `ok`, y una sobrecarga puede seguir resolviéndose por los argumentos buenos).
    fn arg_or_recover(&mut self) -> Expr {
        let pos = self.pos();
        self.expr().unwrap_or_else(|err| self.recover_expr(pos, err))
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
    // Los literales **hex/binario/octal** se interpretan como el patrón de bits **sin signo** y se
    // reinterpretan al ancho del tipo (§3.10.1): `0x8000000000000000L` es `Long.MIN_VALUE`, no un
    // overflow. Por eso se parsean como `u64` y se transmutan a `i64` (el emisor los trunca a `int`
    // si el literal era `int`). Solo el **decimal** es un valor con signo directo.
    if let Some(hex) = t.strip_prefix("0x").or_else(|| t.strip_prefix("0X")) {
        u64::from_str_radix(hex, 16).ok().map(|v| v as i64)
    } else if let Some(bin) = t.strip_prefix("0b").or_else(|| t.strip_prefix("0B")) {
        u64::from_str_radix(bin, 2).ok().map(|v| v as i64)
    } else if t.len() > 1 && t.starts_with('0') && t.bytes().all(|b| (b'0'..=b'7').contains(&b)) {
        u64::from_str_radix(&t[1..], 8).ok().map(|v| v as i64)
    } else {
        t.parse::<i64>().ok()
    }
}

// El valor de un literal entero **negado**. Todo lo que `parse_int_literal` acepta se niega
// envolviendo; lo único que aquella rechaza y acá es legal es el decimal 2^63, cuya negación es
// exactamente `Long.MIN_VALUE` (§3.10.1). Se parsea sin signo para poder verlo.
fn parse_negated_int_literal(text: &str) -> Option<i64> {
    if let Some(v) = parse_int_literal(text) {
        return Some(v.wrapping_neg());
    }
    let clean: String = text.chars().filter(|&c| c != '_').collect();
    let t = clean.trim_end_matches(['l', 'L']);
    let v = t.parse::<u64>().ok()?;
    if v == 1u64 << 63 { Some(i64::MIN) } else { None }
}

fn parse_float_literal(text: &str) -> Option<f32> {
    let clean: String = text.chars().filter(|&c| c != '_').collect();
    clean.trim_end_matches(['f', 'F']).parse().ok()
}

fn parse_double_literal(text: &str) -> Option<f64> {
    let clean: String = text.chars().filter(|&c| c != '_').collect();
    clean.trim_end_matches(['d', 'D']).parse().ok()
}

/// Decodifica un literal `char` a la **unidad de codigo UTF-16** que vale (JLS 3.10.4).
///
/// No devuelve un `char` de Rust a proposito: \u d800 es Java valido -es lo que vale
/// `Character.MIN_HIGH_SURROGATE`- y no hay `char` de Rust que lo sostenga, porque un
/// sustituto **suelto** no es un *scalar value* de Unicode. Antes se decodificaba a `char` y
/// el `from_u32` devolvia `None`, o sea *literal char invalido* para un literal legal.
fn decode_char(text: &str) -> Option<u16> {
    let chars: Vec<char> = text.chars().collect();
    if chars.len() < 2 {
        return None;
    }
    let inner = &chars[1..chars.len() - 1]; // sin las comillas
    match unescape_utf16(inner)?[..] {
        [u] => Some(u),
        _ => None, // vacio, o mas de una unidad de codigo
    }
}

/// Decodifica un literal `String`. Va por **UTF-16** y recompone despues, porque un caracter
/// suplementario se escribe como un **par** de escapes (\u d83d \u de00) y decodificar cada
/// escape por separado a un `char` de Rust falla en el primero: un sustituto suelto no es un
/// *scalar value*. Juntos si son un caracter, y `from_utf16` es quien los junta.
///
/// Queda un caso que no se puede representar y se **rechaza fuerte**: un sustituto **suelto**
/// dentro de un `String`. Es Java legal, pero un `String` de Rust no lo sostiene, y sostenerlo
/// pide llevar todos los literales como `Vec<u16>`. Un error claro es mejor que una
/// sustitucion silenciosa por U+FFFD.
fn decode_string(text: &str) -> std::result::Result<String, &'static str> {
    let chars: Vec<char> = text.chars().collect();
    if chars.len() >= 6 && chars[0] == '"' && chars[1] == '"' && chars[2] == '"' {
        return decode_text_block(&chars[3..chars.len() - 3]).ok_or("literal string invalido");
    }
    if chars.len() < 2 {
        return Err("literal string invalido");
    }
    let units = unescape_utf16(&chars[1..chars.len() - 1]).ok_or("literal string invalido")?;
    String::from_utf16(&units)
        .map_err(|_| "un sustituto suelto en un literal String todavia no se soporta")
}

/// ¿Es *white space* horizontal a efectos del destripado de un text block (§3.10.6)? Los
/// terminadores de línea ya se normalizaron a `\n` antes de contar, así que no cuentan acá.
fn is_tb_space(c: char) -> bool {
    c == ' ' || c == '\t' || c == '\u{c}'
}

/// Decodifica el **contenido** de un text block (§3.10.6), sin los `"""` de apertura/cierre. El
/// algoritmo del JLS, en orden: (1) normalizar los terminadores de línea a `\n`; (2) descartar la
/// línea de apertura (tras `"""` solo puede haber *white space* y un terminador); (3) quitar la
/// **indentación incidental** —el mínimo de sangría de las líneas no-blancas **y** la del cierre— y
/// los blancos **finales** de cada línea; (4) recién entonces procesar los escapes (con la
/// **continuación de línea** `\<LF>` y `\s`, que ya no se ve afectada por el destripado).
fn decode_text_block(inner: &[char]) -> Option<String> {
    // (1) Normalizar `\r\n` y `\r` a `\n`.
    let mut norm: Vec<char> = Vec::with_capacity(inner.len());
    let mut i = 0;
    while i < inner.len() {
        if inner[i] == '\r' {
            norm.push('\n');
            if inner.get(i + 1) == Some(&'\n') {
                i += 1;
            }
        } else {
            norm.push(inner[i]);
        }
        i += 1;
    }
    // (2) La línea de apertura: `"""` debe ir seguido de white space y un terminador de línea.
    let nl = norm.iter().position(|&c| c == '\n')?;
    if norm[..nl].iter().any(|&c| !is_tb_space(c)) {
        return None; // caracteres no-blancos tras el delimitador de apertura
    }
    let body = &norm[nl + 1..];

    // Partir el cuerpo en líneas por `\n` (la última es la del delimitador de cierre).
    let mut lines: Vec<&[char]> = Vec::new();
    let mut start = 0;
    for (j, &c) in body.iter().enumerate() {
        if c == '\n' {
            lines.push(&body[start..j]);
            start = j + 1;
        }
    }
    lines.push(&body[start..]);

    // (3a) Indentación mínima: las líneas en blanco no cuentan, **salvo la última** (la del cierre).
    let count = lines.len();
    let mut min = usize::MAX;
    for (idx, line) in lines.iter().enumerate() {
        let is_last = idx == count - 1;
        let blank = line.iter().all(|&c| is_tb_space(c));
        if blank && !is_last {
            continue;
        }
        let indent = line.iter().take_while(|&&c| is_tb_space(c)).count();
        min = min.min(indent);
    }
    let min = if min == usize::MAX { 0 } else { min };

    // (3b) Quitar `min` de sangría por la izquierda y **todos** los blancos finales de cada línea.
    let stripped: Vec<String> = lines
        .iter()
        .map(|line| {
            let mut lo = 0;
            while lo < min && lo < line.len() && is_tb_space(line[lo]) {
                lo += 1;
            }
            let mut hi = line.len();
            while hi > lo && is_tb_space(line[hi - 1]) {
                hi -= 1;
            }
            line[lo..hi].iter().collect::<String>()
        })
        .collect();
    let joined: Vec<char> = stripped.join("\n").chars().collect();

    // (4) Escapes, ya con el destripado hecho: `\s` sobrevive como espacio y `\<LF>` une líneas.
    unescape_impl(&joined, true)
}

/// Decodifica las secuencias de escape de Java en `chars` a un `String`.
/// Como [`unescape`], pero a **unidades de codigo UTF-16**, que es lo que Java manipula: es el
/// unico nivel donde \u d800 se puede representar. Un escape \u es una unidad de codigo tal
/// cual (JLS 3.3), no un *code point*: por eso se empuja sin validar que sea un scalar value.
fn unescape_utf16(chars: &[char]) -> Option<Vec<u16>> {
    let mut out: Vec<u16> = Vec::new();
    let mut i = 0;
    while i < chars.len() {
        if chars[i] == '\\' {
            i += 1;
            let e = *chars.get(i)?;
            match e {
                'n' => out.push(u16::from(b'\n')),
                't' => out.push(u16::from(b'\t')),
                'r' => out.push(u16::from(b'\r')),
                'b' => out.push(0x08),
                'f' => out.push(0x0c),
                's' => out.push(u16::from(b' ')),
                '0' => out.push(0),
                other if matches!(other, '\\' | '\'' | '"') => out.push(other as u16),
                'u' => {
                    // JLS 3.3: una `u` de mas es legal (`\uu0041`), y el valor son cuatro higits.
                    while chars.get(i + 1) == Some(&'u') {
                        i += 1;
                    }
                    let hex: String = chars.get(i + 1..i + 5)?.iter().collect();
                    out.push(u16::from_str_radix(&hex, 16).ok()?);
                    i += 4;
                }
                other => {
                    let mut buf = [0u16; 2];
                    out.extend_from_slice(other.encode_utf16(&mut buf));
                }
            }
        } else {
            let mut buf = [0u16; 2];
            out.extend_from_slice(chars[i].encode_utf16(&mut buf));
        }
        i += 1;
    }
    Some(out)
}

fn unescape(chars: &[char]) -> Option<String> {
    unescape_impl(chars, false)
}

/// Como [`unescape`], pero con `text_block` habilita la **continuación de línea** (§3.10.6): una
/// barra al final de una línea (`\` seguido de `\n`) **suprime** ese salto de línea.
fn unescape_impl(chars: &[char], text_block: bool) -> Option<String> {
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
                '\n' if text_block => {} // continuación de línea: se traga el `\n`
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
        let (cu, errors) = parse(tokenize(src).unwrap());
        assert!(errors.is_empty(), "errores de parseo inesperados: {errors:?}");
        cu
    }

    /// #228 - un `char` de Java es una unidad de codigo UTF-16, no un *scalar value* de
    /// Unicode: un sustituto **suelto** es un literal legal, y es lo que vale
    /// `Character.MIN_HIGH_SURROGATE`. Antes se decodificaba a un `char` de Rust, que no puede
    /// sostenerlo, asi que un literal legal se rechazaba como invalido.
    #[test]
    fn a_lone_surrogate_is_a_valid_char_literal() {
        let stmts = parse_body("char c = '\\ud800'; char d = '\\u0041';");
        let mut vistos: Vec<u16> = Vec::new();
        for s in &stmts {
            if let StmtKind::LocalVar { init: Some(Expr { kind: ExprKind::CharLit(c), .. }), .. } =
                &s.kind
            {
                vistos.push(*c);
            }
        }
        assert_eq!(vistos, vec![0xd800u16, 0x0041u16]);
    }

    /// Y el **par** sustituto en un `String` se recompone en el caracter suplementario que es:
    /// decodificar cada escape por separado fallaba en el primero.
    #[test]
    fn a_surrogate_pair_in_a_string_becomes_one_character() {
        let stmts = parse_body("String s = \"\\ud83d\\ude00\";");
        let StmtKind::LocalVar { init: Some(Expr { kind: ExprKind::StringLit(s), .. }), .. } =
            &stmts[0].kind
        else {
            panic!("una local con un literal String")
        };
        assert_eq!(s.chars().count(), 1, "un solo caracter: {s:?}");
        assert_eq!(s.chars().next().map(u32::from), Some(0x1f600));
    }

    /// #224 - `import IntStream;` no es Java (7.5: del paquete sin nombre no se importa). Se
    /// aceptaba en silencio y el tipo resolvia igual por otro camino.
    #[test]
    fn an_import_without_a_package_is_rejected() {
        let (_, errors) = parse(tokenize("import IntStream; class C {}").unwrap());
        assert!(!errors.is_empty(), "un import sin cualificar es error de sintaxis");
    }

    /// La contraprueba: un `import` cualificado y un on-demand de un paquete de un solo
    /// nombre siguen andando.
    #[test]
    fn a_qualified_import_and_a_single_name_wildcard_still_parse() {
        let cu = parse_src("import java.util.List; import p.*; class C {}");
        assert_eq!(cu.imports.len(), 2);
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

    /// Parsea permitiendo errores, devolviendo la unidad recuperada **y** la lista de errores.
    fn parse_recover(src: &str) -> (CompilationUnit, Vec<Error>) {
        parse(tokenize(src).unwrap())
    }

    // ---- recuperación de errores (panic-mode del parser) ----

    #[test]
    fn recovers_from_bad_member_and_keeps_the_rest() {
        // El primer miembro está roto (falta el tipo de retorno / basura); el segundo es válido.
        let (cu, errors) = parse_recover("class C { void () @@@ ; int ok() { return 1; } }");
        assert!(!errors.is_empty(), "se reporta al menos un error del miembro roto");
        // Se recupera y se sigue parseando: el método válido queda en el AST.
        let names: Vec<_> = cu.types[0]
            .members
            .iter()
            .filter_map(|m| match m {
                Member::Method(m) => Some(m.name.clone()),
                _ => None,
            })
            .collect();
        assert!(names.contains(&"ok".to_string()), "el miembro válido `ok` se recupera: {names:?}");
    }

    #[test]
    fn recovers_from_bad_statement_and_keeps_the_rest() {
        // Sentencia rota en el medio; las de alrededor deben sobrevivir.
        let stmts = {
            let (cu, errors) = parse_recover(
                "class T { void m() { int a = 1; @@ ??? ; int b = 2; } }",
            );
            assert!(!errors.is_empty(), "se reporta el error de la sentencia rota");
            let Member::Method(m) = &cu.types[0].members[0] else { panic!() };
            m.body.clone().unwrap().0
        };
        // Deben quedar al menos las dos declaraciones válidas (a y b).
        let decls = stmts
            .iter()
            .filter(|s| matches!(&s.kind, StmtKind::LocalVar { .. }))
            .count();
        assert!(decls >= 2, "se recuperan las sentencias válidas de alrededor: {decls}");
    }

    #[test]
    fn recovers_a_bad_initializer_with_an_error_node_keeping_the_local() {
        // Recuperación a nivel **expresión**: un inicializador roto se vuelve un `ExprKind::Error`,
        // pero el local `x` **igual queda declarado** (el nodo `LocalVar` sobrevive).
        let (cu, errors) = parse_recover("class C { void m() { int x = @@@; int y = 2; } }");
        assert!(!errors.is_empty());
        let Member::Method(m) = &cu.types[0].members[0] else { panic!() };
        let stmts = m.body.clone().unwrap().0;
        let StmtKind::LocalVar { name, init, .. } = &stmts[0].kind else {
            panic!("esperaba un LocalVar: {:?}", stmts[0].kind)
        };
        assert_eq!(name, "x");
        assert!(
            matches!(init.as_ref().map(|e| &e.kind), Some(ExprKind::Error)),
            "el init debe ser un nodo de error: {init:?}"
        );
        // Y la declaración siguiente (`y`) también se parsea.
        assert!(matches!(&stmts[1].kind, StmtKind::LocalVar { name, .. } if name == "y"));
    }

    #[test]
    fn recovers_a_bad_call_argument_keeping_the_others() {
        // Un argumento roto se vuelve un nodo de error; los **demás** argumentos sobreviven.
        let (cu, errors) = parse_recover("class C { void m() { f(@@@, 5, x); } }");
        assert!(!errors.is_empty());
        let Member::Method(m) = &cu.types[0].members[0] else { panic!() };
        let stmts = m.body.clone().unwrap().0;
        let StmtKind::Expr(e) = &stmts[0].kind else { panic!("{:?}", stmts[0].kind) };
        let ExprKind::Call { args, .. } = &e.kind else { panic!("{:?}", e.kind) };
        assert_eq!(args.len(), 3, "los tres argumentos quedan (el 1º como error): {args:?}");
        assert!(matches!(&args[0].kind, ExprKind::Error));
        assert!(matches!(&args[1].kind, ExprKind::IntLit(5)));
        assert!(matches!(&args[2].kind, ExprKind::Name(n) if n == "x"));
    }

    #[test]
    fn recovers_across_top_level_types() {
        // Primer tipo roto (llave sin cerrar de un miembro basura), segundo tipo válido.
        let (cu, errors) = parse_recover("class Bad { void x( } class Good { }");
        assert!(!errors.is_empty(), "se reporta el error del primer tipo");
        let type_names: Vec<_> = cu.types.iter().map(|t| t.name.clone()).collect();
        assert!(
            type_names.contains(&"Good".to_string()),
            "el segundo tipo se recupera: {type_names:?}",
        );
    }

    #[test]
    fn reports_multiple_errors_in_one_pass() {
        // Dos miembros rotos: el parser debe reportar ambos, no cortar en el primero.
        // (Basura que **lexea** bien pero no parsea: `,` donde se espera un parámetro.)
        let (_cu, errors) = parse_recover("class C { void a( , ; void b( , ; int ok() {} }");
        assert!(errors.len() >= 2, "se acumulan varios errores: {}", errors.len());
    }

    // ---- bordes de B1: anotaciones de tipo/uso y type witness de constructor ----

    #[test]
    fn type_param_annotations_are_retained() {
        let cu = parse_src("class C<@Foo T> { }");
        let tp = &cu.types[0].type_params[0];
        assert_eq!(tp.name, "T");
        assert_eq!(tp.annotations.len(), 1, "la anotación `@Foo` se retiene");
        assert_eq!(tp.annotations[0].name, "Foo");
    }

    #[test]
    fn enum_constant_annotations_are_retained() {
        let cu = parse_src("enum E { @Deprecated A, B }");
        let cs = &cu.types[0].enum_constants;
        assert_eq!(cs[0].name, "A");
        assert_eq!(cs[0].annotations.len(), 1, "la anotación `@Deprecated` se retiene");
        assert_eq!(cs[0].annotations[0].name, "Deprecated");
        assert!(cs[1].annotations.is_empty(), "`B` no lleva anotación");
    }

    #[test]
    fn type_use_annotations_are_accepted() {
        // §9.7.4: se aceptan sin error (se descartan). Cubre el argumento de tipo y el tipo suelto.
        parse_src("class C { java.util.List<@NonNull String> f; @Foo int g() { return 0; } }");
    }

    // ---- javadoc (etapa 1): los doc comments `/** */` se retienen y cuelgan de su declaración ----

    #[test]
    fn doc_comment_is_attached_to_method() {
        let cu = parse_src(
            "class T {\n\
             /** Suma dos enteros. */\n\
             int add(int a, int b) { return a + b; }\n\
             }",
        );
        let Member::Method(m) = &cu.types[0].members[0] else { panic!("esperaba un método") };
        assert_eq!(m.name, "add");
        let doc = m.doc.as_deref().expect("el método debe conservar su doc comment");
        assert!(doc.contains("Suma dos enteros"), "doc capturado: {doc:?}");
    }

    #[test]
    fn plain_block_comment_is_not_a_doc_comment() {
        // `/* normal */` es un comentario de bloque común: no debe adjuntarse como doc.
        let cu = parse_src("class T { /* normal */ int add(int a, int b) { return a + b; } }");
        let Member::Method(m) = &cu.types[0].members[0] else { panic!("esperaba un método") };
        assert_eq!(m.doc, None, "un `/* */` normal no es doc comment");
    }

    #[test]
    fn empty_block_comment_slashstarstarslash_is_not_a_doc_comment() {
        // `/**/` es un bloque vacío, no un doc comment (el char tras `/*` es `*`, pero el siguiente es `/`).
        let cu = parse_src("class T { /**/ int add(int a, int b) { return a + b; } }");
        let Member::Method(m) = &cu.types[0].members[0] else { panic!("esperaba un método") };
        assert_eq!(m.doc, None, "`/**/` no es doc comment");
    }

    #[test]
    fn doc_comment_is_attached_to_class_field_and_enum_constant() {
        let cu = parse_src(
            "/** La clase. */\n\
             class T {\n\
             /** El campo. */ int f;\n\
             }",
        );
        assert!(cu.types[0].doc.as_deref().unwrap().contains("La clase"));
        let Member::Field(fld) = &cu.types[0].members[0] else { panic!("esperaba un campo") };
        assert!(fld.doc.as_deref().unwrap().contains("El campo"));

        let cu2 = parse_src("enum E { /** La constante. */ A, B }");
        let cs = &cu2.types[0].enum_constants;
        assert!(cs[0].doc.as_deref().unwrap().contains("La constante"));
        assert_eq!(cs[1].doc, None, "`B` no lleva doc comment");
    }

    #[test]
    fn a_constructor_type_witness_is_accepted() {
        // §15.9: `new <T>Foo()` ya no da error.
        let e = parse_init("new <String>Foo()");
        assert!(matches!(e.kind, ExprKind::NewObject { .. }), "sigue siendo un `new`");
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
        let ExprKind::NewObject { ty, args, body, .. } =
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
    fn switch_arrow_with_identifier_constant_is_not_a_lambda() {
        // `case RED -> 1` es una etiqueta constante + brazo flecha, **no** la lambda `RED -> 1`.
        let stmts = parse_body("switch (c) { case RED -> 1; case GREEN -> 2; }");
        let StmtKind::Switch { cases, .. } = &stmts[0].kind else { panic!("esperaba Switch") };
        assert_eq!(cases.len(), 2);
        assert!(matches!(cases[0].labels[0], CaseLabel::Constant(_)));
        assert!(matches!(cases[0].body, SwitchBody::Arrow(_)));
    }

    #[test]
    fn sealed_interface_with_permits_parses() {
        let cu = parse_src("sealed interface Shape permits Circle, Square {}");
        assert!(cu.types[0].modifiers.contains(&Modifier::Sealed));
        assert_eq!(cu.types[0].permits.len(), 2, "permits Circle, Square");
    }

    #[test]
    fn non_sealed_class_parses() {
        let cu = parse_src("sealed class B permits S {} non-sealed class S extends B {}");
        assert!(cu.types[1].modifiers.contains(&Modifier::NonSealed));
    }

    #[test]
    fn sealed_as_a_type_name_is_not_a_modifier() {
        // `sealed` fuera de posición de declaración de tipo sigue siendo un identificador: un campo
        // de tipo `sealed` no debe leerse como el modificador.
        let cu = parse_src("class C { sealed foo; }");
        assert!(cu.types[0].modifiers.is_empty());
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
    fn an_array_level_type_annotation_is_accepted() {
        // `String @A []` (§9.7.4): la anotación de **nivel de array** va entre el tipo elemento y el
        // `[`. Se acepta y se descarta (metadata ajena a la *erasure*); el tipo queda `String[]`.
        let stmts = parse_body("String @A [] a = null;");
        let StmtKind::LocalVar { ty, .. } = &stmts[0].kind else { panic!("{:?}", stmts[0].kind) };
        assert_eq!(*ty, Type::Array(Box::new(Type::Class("String".into()))));
    }

    #[test]
    fn an_array_level_annotation_per_dimension() {
        // `int @A [] @B []` — una anotación por dimensión; todas se descartan, el tipo es `int[][]`.
        let stmts = parse_body("int @A [] @B [] m = null;");
        let StmtKind::LocalVar { ty, .. } = &stmts[0].kind else { panic!("{:?}", stmts[0].kind) };
        assert_eq!(*ty, Type::Array(Box::new(Type::Array(Box::new(Type::Prim(PrimType::Int))))));
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

    // ---- text blocks (§3.10.6) ----

    /// El valor decodificado del text block `src` (usado como inicializador).
    fn tb(src: &str) -> String {
        match parse_init(src).kind {
            ExprKind::StringLit(s) => s,
            other => panic!("esperaba un StringLit, salió {other:?}"),
        }
    }

    #[test]
    fn text_block_strips_incidental_indentation() {
        // La sangría común (la de las líneas y la del cierre) se quita; el cierre en su línea deja
        // un `\n` final.
        assert_eq!(tb("\"\"\"\n    Hello\n    World\n    \"\"\""), "Hello\nWorld\n");
    }

    #[test]
    fn text_block_closing_on_the_content_line_has_no_trailing_newline() {
        assert_eq!(tb("\"\"\"\n    Hello\n    World\"\"\""), "Hello\nWorld");
    }

    #[test]
    fn text_block_preserves_relative_indentation() {
        // Solo se quita el mínimo común: la sangría **extra** de `b` se conserva.
        assert_eq!(tb("\"\"\"\n    a\n      b\n    \"\"\""), "a\n  b\n");
    }

    #[test]
    fn text_block_closing_delimiter_sets_the_minimum() {
        // El cierre menos sangrado que el contenido fija el mínimo (2), dejando 6 espacios en `a`.
        assert_eq!(tb("\"\"\"\n        a\n  \"\"\""), "      a\n");
    }

    #[test]
    fn text_block_trailing_whitespace_is_stripped_but_escape_s_survives() {
        // Los blancos finales se quitan; `\s` (que se procesa **después**) preserva el espacio.
        assert_eq!(tb("\"\"\"\n    a   \n    b\\s\n    \"\"\""), "a\nb \n");
    }

    #[test]
    fn text_block_line_continuation_joins_lines() {
        // `\` al final de línea se traga el salto (§3.10.6).
        assert_eq!(tb("\"\"\"\n    a\\\n    b\n    \"\"\""), "ab\n");
    }

    #[test]
    fn text_block_processes_escapes_and_keeps_literal_quotes() {
        // Un `"` suelto es literal; los escapes normales siguen valiendo.
        assert_eq!(tb("\"\"\"\n    a\"b\\tc\n    \"\"\""), "a\"b\tc\n");
    }

    #[test]
    fn text_block_blank_lines_do_not_affect_the_minimum() {
        assert_eq!(tb("\"\"\"\n    a\n\n    b\n    \"\"\""), "a\n\nb\n");
    }

    #[test]
    fn text_block_normalizes_crlf_line_terminators() {
        assert_eq!(tb("\"\"\"\r\n    a\r\n    b\r\n    \"\"\""), "a\nb\n");
    }

    // ---- módulos (§7.7) ----

    #[test]
    fn parses_a_module_with_all_directives() {
        let cu = parse_src(
            "open module com.example.foo { \
               requires java.base; \
               requires transitive com.example.bar; \
               requires static com.example.opt; \
               exports com.example.foo.api; \
               exports com.example.foo.internal to com.trusted, com.other; \
               opens com.example.foo.impl; \
               uses com.example.spi.Service; \
               provides com.example.spi.Service with com.example.foo.Impl1, com.example.foo.Impl2; \
             }",
        );
        let m = cu.module.expect("hay declaración de módulo");
        assert!(m.open);
        assert_eq!(m.name, "com.example.foo");
        assert_eq!(m.directives.len(), 8);
        assert!(matches!(&m.directives[1], ModuleDirective::Requires { transitive: true, name, .. } if name == "com.example.bar"));
        assert!(matches!(&m.directives[2], ModuleDirective::Requires { is_static: true, .. }));
        assert!(matches!(&m.directives[4], ModuleDirective::Exports { to, .. } if to.len() == 2));
        assert!(matches!(&m.directives[7], ModuleDirective::Provides { with, .. } if with.len() == 2));
        assert!(cu.types.is_empty());
    }

    #[test]
    fn requires_transitive_as_a_module_name() {
        // `requires transitive;` — `transitive` seguido de `;` es el **nombre**, no el modificador.
        let cu = parse_src("module m { requires transitive; }");
        let m = cu.module.unwrap();
        assert!(matches!(&m.directives[0], ModuleDirective::Requires { transitive: false, name, .. } if name == "transitive"));
    }

    #[test]
    fn a_plain_module_is_not_open() {
        let cu = parse_src("module m { }");
        let m = cu.module.unwrap();
        assert!(!m.open);
        assert!(m.directives.is_empty());
    }

    // ---- literales enteros (§3.10.1) ----

    #[test]
    fn hex_long_literal_with_the_high_bit_is_a_bit_pattern() {
        // Los literales hex/bin/octal son el **patrón de bits** sin signo, reinterpretado al ancho
        // del tipo: `0x8000000000000000L` es `Long.MIN_VALUE`, no un overflow (lo destapó KajiLibrary).
        assert!(matches!(parse_init("0x8000000000000000L").kind, ExprKind::LongLit(v) if v == i64::MIN));
        assert!(matches!(parse_init("0xFFFFFFFFFFFFFFFFL").kind, ExprKind::LongLit(v) if v == -1));
        assert!(matches!(parse_init("0x7fffffffffffffffL").kind, ExprKind::LongLit(v) if v == i64::MAX));
    }
}
