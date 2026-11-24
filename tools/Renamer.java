import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Position;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;

import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Renames decompiler-generated locals/parameters (var12, n2, f3, object2, clientPlayerEntity...)
 * to vanilla-Yarn-style names (i, j, f, bl, client, player, pos, stack...).
 *
 * Scope-aware without a symbol solver: every NameExpr is resolved to the innermost local
 * declaration of that name whose scope contains it. New names are unique per top-level member
 * (including lambdas, anonymous and local classes inside it), never equal a field name or any
 * other identifier used unqualified in the file, so a rename can't shadow or capture anything.
 * Local names don't exist in bytecode, so the result must compile to identical code; verify with
 * tools/BytecodeDiff.java.
 *
 * Usage: java -cp javaparser-core.jar tools/Renamer.java <src-dir>
 */
public class Renamer {
    static final Pattern GENERATED = Pattern.compile(
            "var\\d+x*|var1000\\d+|(n|f|d|l|bl|c|b|s|by|object|string|iterator|e|g|h)\\d*");

    static final Map<String, String> SHORT = new HashMap<>();
    static {
        String[][] m = {
            {"MinecraftClient", "client"}, {"ClientPlayerEntity", "player"}, {"PlayerEntity", "player"},
            {"OtherClientPlayerEntity", "player"}, {"AbstractClientPlayerEntity", "player"},
            {"ClientWorld", "world"}, {"World", "world"}, {"ServerWorld", "world"}, {"WorldChunk", "chunk"},
            {"Chunk", "chunk"}, {"ChunkPos", "chunkPos"}, {"ChunkSection", "section"},
            {"MatrixStack", "matrices"}, {"DrawContext", "context"}, {"VertexConsumer", "vertexConsumer"},
            {"BlockPos", "pos"}, {"Mutable", "mutable"}, {"Vec3d", "vec"}, {"Vec3i", "vec"}, {"Box", "box"},
            {"ItemStack", "stack"}, {"BlockState", "state"}, {"FluidState", "fluidState"},
            {"Text", "text"}, {"MutableText", "text"}, {"Identifier", "id"}, {"Entity", "entity"},
            {"LivingEntity", "entity"}, {"Direction", "direction"}, {"Item", "item"}, {"Block", "block"},
            {"Packet", "packet"}, {"Screen", "screen"}, {"Slot", "slot"}, {"ScreenHandler", "handler"},
            {"Object", "object"}, {"String", "string"}, {"StringBuilder", "sb"}, {"Iterator", "iterator"},
            {"List", "list"}, {"ArrayList", "list"}, {"LinkedList", "list"}, {"Map", "map"}, {"HashMap", "map"},
            {"LinkedHashMap", "map"}, {"TreeMap", "map"}, {"IdentityHashMap", "map"}, {"Set", "set"},
            {"HashSet", "set"}, {"LinkedHashSet", "set"}, {"Collection", "collection"}, {"Optional", "optional"},
            {"Entry", "entry"}, {"Path", "path"}, {"File", "file"}, {"InputStream", "in"}, {"OutputStream", "out"},
            {"JsonObject", "json"}, {"JsonElement", "json"}, {"JsonArray", "array"},
            {"CompletableFuture", "future"}, {"Throwable", "throwable"}, {"Exception", "exception"},
            {"IOException", "exception"}, {"RuntimeException", "exception"}, {"Matrix4f", "matrix"},
            {"Vector3f", "vec"}, {"UUID", "uuid"}, {"GameProfile", "profile"}, {"Random", "random"},
            {"RegistryEntry", "entry"}, {"RegistryKey", "key"}, {"NbtCompound", "nbt"},
            {"PlayerListEntry", "listEntry"}, {"Integer", "integer"}, {"Long", "long_"}, {"Double", "value"},
            {"Float", "value"}, {"Boolean", "flag"}, {"Character", "ch"},
            {"CallbackInfo", "ci"}, {"CallbackInfoReturnable", "cir"},
        };
        for (String[] p : m) SHORT.put(p[0], p[1]);
    }
    static final Set<String> RESERVED = Set.of("java", "javax", "net", "com", "org", "it", "io", "imgui", "fi",
            "jdk", "sun", "this", "super", "var", "record", "yield", "long_");
    static final Set<String> KEYWORDS = Set.of("abstract", "assert", "boolean", "break", "byte", "case", "catch",
            "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface",
            "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static",
            "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null", "in", "out");

    static boolean SEMANTIC = false;

    public static void main(String[] a) throws Exception {
        if (a.length > 1 && a[1].equals("--semantic")) SEMANTIC = true;
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        int files = 0, renamed = 0, failed = 0;
        try (Stream<Path> s = Files.walk(Path.of(a[0]))) {
            for (Path p : s.filter(x -> x.toString().endsWith(".java")).toList()) {
                CompilationUnit cu;
                try { cu = StaticJavaParser.parse(p); } catch (Exception e) { failed++; System.err.println("PARSE FAIL " + p); continue; }
                int n = new Renamer(cu).run();
                if (n > 0) { Files.writeString(p, cu.toString()); renamed += n; }
                files++;
            }
        }
        System.out.printf("files=%d renamedDecls=%d parseFailures=%d%n", files, renamed, failed);
    }

    // ---------------------------------------------------------------------------------------

    final CompilationUnit cu;
    final Set<String> fileExclusions = new HashSet<>();
    Renamer(CompilationUnit cu) { this.cu = cu; }

    record Decl(Node node, SimpleName name, Node scope, Position from, Type type) {}

    int run() {
        for (MethodReferenceExpr mr : cu.findAll(MethodReferenceExpr.class)) {
            if (mr.getScope() instanceof TypeExpr te && te.getType() instanceof ClassOrInterfaceType ct
                    && ct.getScope().isEmpty() && ct.getTypeArguments().isEmpty()
                    && Character.isLowerCase(ct.getNameAsString().charAt(0)))
            {
                NameExpr ne = new NameExpr(ct.getNameAsString());
                te.getRange().ifPresent(ne::setRange);
                mr.setScope(ne);
            }
        }
        // every field name, and every type name, is off-limits
        cu.findAll(FieldDeclaration.class).forEach(f -> f.getVariables().forEach(v -> fileExclusions.add(v.getNameAsString())));
        cu.findAll(EnumConstantDeclaration.class).forEach(e -> fileExclusions.add(e.getNameAsString()));
        cu.findAll(RecordDeclaration.class).forEach(r -> r.getParameters().forEach(pr -> fileExclusions.add(pr.getNameAsString())));
        fileExclusions.addAll(RESERVED);
        fileExclusions.addAll(KEYWORDS);

        // roots: members of every type declaration that is not nested inside a method body
        List<Node> roots = new ArrayList<>();
        for (TypeDeclaration<?> td : cu.findAll(TypeDeclaration.class)) {
            if (insideCallable(td)) continue;          // local class: handled by its enclosing root
            for (BodyDeclaration<?> m : td.getMembers()) {
                if (m instanceof CallableDeclaration || m instanceof InitializerDeclaration || m instanceof FieldDeclaration)
                    roots.add(m);
            }
            if (td instanceof EnumDeclaration ed) ed.getEntries().forEach(roots::add);
        }
        // unqualified names that don't resolve to a local anywhere -> fields / static imports: exclude
        List<Decl> all = new ArrayList<>();
        Map<Node, List<Decl>> perRoot = new LinkedHashMap<>();
        for (Node r : roots) { List<Decl> ds = decls(r); perRoot.put(r, ds); all.addAll(ds); }
        Map<NameExpr, Decl> resolution = new IdentityHashMap<>();
        for (Node r : roots) {
            for (NameExpr ne : r.findAll(NameExpr.class)) {
                if (!inThisRoot(ne, r)) continue;
                Decl d = resolve(ne, perRoot.get(r));
                if (d != null) resolution.put(ne, d); else fileExclusions.add(ne.getNameAsString());
            }
        }
        int count = 0;
        for (Node r : roots) {
            List<Decl> ds = perRoot.get(r);
            Set<String> used = new HashSet<>(fileExclusions);
            List<Decl> todo = new ArrayList<>();
            for (Decl d : ds) {
                if (shouldRename(d)) todo.add(d); else used.add(d.name.getIdentifier());
            }
            if (todo.isEmpty()) continue;
            Map<Decl, List<NameExpr>> refs = new IdentityHashMap<>();
            for (var e : resolution.entrySet()) if (todo.contains(e.getValue()))
                refs.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            Map<String, Integer> counters = new HashMap<>();
            for (Decl d : todo) {
                String sem = SEMANTIC ? semantic(d, refs.getOrDefault(d, List.of())) : null;
                String nn = sem != null ? number(sem, used) : fresh(d, used, counters);
                used.add(nn);
                d.name.setIdentifier(nn);
                for (NameExpr ne : refs.getOrDefault(d, List.of())) ne.setName(nn);
                count++;
            }
        }
        return count;
    }

    // ---- declarations & scopes --------------------------------------------------------------

    static boolean insideCallable(Node n) {
        Node p = n.getParentNode().orElse(null);
        while (p != null) {
            if (p instanceof CallableDeclaration || p instanceof LambdaExpr || p instanceof InitializerDeclaration
                    || p instanceof ObjectCreationExpr) return true;
            if (p instanceof TypeDeclaration<?> t && !(p.getParentNode().orElse(null) instanceof LocalClassDeclarationStmt)
                    && !insideCallable(t)) return false;
            p = p.getParentNode().orElse(null);
        }
        return false;
    }

    /** Is node n part of root r (and not inside a nested *member* type, which is its own root)? */
    static boolean inThisRoot(Node n, Node r) {
        Node p = n;
        while (p != null && p != r) {
            if (p instanceof TypeDeclaration<?> t && !insideCallable(t)) return false;
            p = p.getParentNode().orElse(null);
        }
        return p == r;
    }

    List<Decl> decls(Node root) {
        List<Decl> out = new ArrayList<>();
        for (Parameter prm : root.findAll(Parameter.class)) {
            if (!inThisRoot(prm, root)) continue;
            Node owner = prm.getParentNode().orElse(null);
            Node scope = owner instanceof CatchClause cc ? cc.getBody() : owner;
            if (owner instanceof RecordDeclaration) continue;
            if (owner instanceof ConstructorDeclaration cd && cd.getParentNode().orElse(null) instanceof RecordDeclaration) continue;
            if (scope == null) continue;
            out.add(new Decl(prm, prm.getName(), scope, begin(scope), prm.getType()));
        }
        for (VariableDeclarator vd : root.findAll(VariableDeclarator.class)) {
            if (!inThisRoot(vd, root)) continue;
            if (!(vd.getParentNode().orElse(null) instanceof VariableDeclarationExpr vde)) continue;  // fields
            Node holder = vde.getParentNode().orElse(null);
            Node scope; Position from = begin(vd);
            if (holder instanceof ExpressionStmt es) scope = es.getParentNode().orElse(es);
            else if (holder instanceof ForStmt || holder instanceof ForEachStmt || holder instanceof TryStmt) scope = holder;
            else scope = holder;
            out.add(new Decl(vd, vd.getName(), scope, from, vd.getType()));
        }
        for (TypePatternExpr tp : root.findAll(TypePatternExpr.class)) {
            if (!inThisRoot(tp, root)) continue;
            // flow scoping: over-approximate as the enclosing statement's container, from the pattern on
            Node scope = tp.getParentNode().orElse(root);
            while (scope != root && !(scope instanceof BlockStmt) && !(scope instanceof SwitchEntry)
                    && !(scope instanceof LambdaExpr)) scope = scope.getParentNode().orElse(root);
            out.add(new Decl(tp, tp.getName(), scope, begin(tp), tp.getType()));
        }
        out.sort(Comparator.comparing(d -> begin(d.node)));
        return out;
    }

    static Position begin(Node n) { return n.getRange().map(r -> r.begin).orElse(new Position(0, 0)); }

    static boolean contains(Node anc, Node n) {
        for (Node p = n; p != null; p = p.getParentNode().orElse(null)) if (p == anc) return true;
        return false;
    }

    static int depth(Node n) { int d = 0; for (Node p = n; p != null; p = p.getParentNode().orElse(null)) d++; return d; }

    Decl resolve(NameExpr ne, List<Decl> ds) {
        String name = ne.getNameAsString();
        Position at = begin(ne);
        Decl best = null;
        for (Decl d : ds) {
            if (!d.name.getIdentifier().equals(name)) continue;
            if (!contains(d.scope, ne)) continue;
            if (at.isBefore(d.from) && !(d.node instanceof Parameter)) continue;
            // a declaration inside a nested class body/lambda doesn't reach outside it
            if (best == null || depth(d.scope) > depth(best.scope)
                    || (depth(d.scope) == depth(best.scope) && begin(d.node).isAfter(begin(best.node)))) best = d;
        }
        return best;
    }

    // ---- naming --------------------------------------------------------------------------------

    boolean shouldRename(Decl d) {
        String n = d.name.getIdentifier();
        if (SEMANTIC) {
            Node par = d.node.getParentNode().orElse(null);
            if (par instanceof CatchClause) return false;
            if (d.type instanceof UnknownType) return false;                       // untyped lambda params
            if (d.node instanceof VariableDeclarator vd && vd.getParentNode().orElse(null) instanceof VariableDeclarationExpr vde
                    && vde.getParentNode().orElse(null) instanceof ForStmt && d.type instanceof PrimitiveType) return false;  // i, j
            if (par instanceof CallableDeclaration<?> cd && cd.isAnnotationPresent("Override")) return false; // keep API-ish params stable
            return true;
        }
        if (GENERATED.matcher(n).matches()) return true;
        if (d.type instanceof UnknownType || d.type instanceof VarType) return false;
        String base = typeBase(d.type);
        return base != null && n.matches(Pattern.quote(decap(base)) + "\\d*");     // e.g. clientPlayerEntity, arrayList2
    }

    static String typeBase(Type t) {
        if (t instanceof ArrayType at) { String b = typeBase(at.getComponentType()); return b == null ? null : b; }
        if (t instanceof ClassOrInterfaceType c) return c.getNameAsString();
        if (t instanceof PrimitiveType p) return p.asString();
        return null;
    }

    static String decap(String s) { return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1); }

    static final Map<String, String[]> PRIM = Map.of(
            "int", new String[]{"i", "j", "k", "m", "n", "o", "p", "q", "r", "t", "u", "v", "w"},
            "long", new String[]{"l"}, "float", new String[]{"f", "g", "h"}, "double", new String[]{"d", "e"},
            "boolean", new String[]{"bl"}, "char", new String[]{"c"}, "byte", new String[]{"b"}, "short", new String[]{"s"});

    String fresh(Decl d, Set<String> used, Map<String, Integer> counters) {
        Type t = d.type;
        boolean catchParam = d.node.getParentNode().orElse(null) instanceof CatchClause;
        if (catchParam) return number("exception", used);
        if (t instanceof UnknownType || t instanceof VarType || t == null) return number("value", used);
        if (t instanceof PrimitiveType pt) {
            String[] pool = PRIM.get(pt.asString());
            for (int round = 1; ; round++)
                for (String b : pool) { String c = round == 1 ? b : b + round; if (!used.contains(c)) return c; }
        }
        if (t instanceof ArrayType at) {
            String b = typeBase(at.getElementType());
            String base = at.getElementType() instanceof PrimitiveType ? b + "s"
                    : plural(SHORT.getOrDefault(b, decap(b)));
            if (at.getArrayLevel() > 1) base = base.replaceAll("s$", "") + "Grid";
            return number(safe(base), used);
        }
        String b = typeBase(t);
        String base = SHORT.getOrDefault(b, decap(b));
        if (b.endsWith("Event") && !SHORT.containsKey(b)) base = "event";
        if (b.endsWith("S2CPacket") || b.endsWith("C2SPacket")) base = decap(b.substring(0, b.length() - 9)) + "Packet";
        else if (b.endsWith("Packet")) base = decap(b);
        if (b.endsWith("Module") && !b.equals("Module")) base = "module";
        if (b.endsWith("Setting")) base = "setting";
        return number(safe(base), used);
    }

    static String plural(String s) {
        if (s.endsWith("s") || s.endsWith("x")) return s + "es";
        if (s.endsWith("y") && s.length() > 1 && "aeiou".indexOf(s.charAt(s.length() - 2)) < 0) return s.substring(0, s.length() - 1) + "ies";
        return s + "s";
    }

    static String safe(String s) { return KEYWORDS.contains(s) || RESERVED.contains(s) ? s + "Value" : s; }

    static String number(String base, Set<String> used) {
        if (!used.contains(base)) return base;
        for (int i = 2; ; i++) if (!used.contains(base + i)) return base + i;
    }

    // ---- semantic naming ----------------------------------------------------------------------

    static final Set<String> GENERIC = Set.of("get", "getValue", "getInt", "getFloat", "getDouble", "getBoolean",
            "getLong", "value", "intValue", "floatValue", "doubleValue", "longValue", "booleanValue", "current");
    static final Set<String> ELEMENT = Set.of("get", "remove", "poll", "peek", "pop", "first", "last", "getFirst",
            "getLast", "removeFirst", "removeLast", "pollFirst", "pollLast", "elementAt");
    static final Set<String> USELESS = Set.of("getInstance", "of", "valueOf", "apply", "call", "build", "copy",
            "toString", "orElse", "orElseThrow", "orElseGet", "next", "iterator", "stream", "collect", "toList",
            "map", "filter", "clone", "getOrDefault", "computeIfAbsent", "min", "max", "abs", "floor", "ceil",
            "round", "sqrt", "clamp", "format", "join", "trim", "strip", "toLowerCase", "toUpperCase", "substring",
            "replace", "replaceAll", "split", "copyOf", "asList", "emptyList", "requireNonNull", "cast", "with",
            "offset", "add", "subtract", "multiply", "normalize", "up", "down", "north", "south", "east", "west",
            "toImmutable", "mutableCopy", "lerp", "wrapDegrees", "hash", "hashCode", "equals", "compareTo",
            "currentTimeMillis", "nanoTime", "parseInt", "parseFloat", "parseDouble", "parseLong", "contains",
            "containsKey", "isEmpty", "matches", "startsWith", "endsWith", "length", "size", "toArray", "keySet",
            "values", "entrySet", "put", "set", "read", "readAllBytes", "getBytes", "charAt", "codePointAt",
            "indexOf", "lastIndexOf", "sum", "average", "count", "orElseNull", "getString", "getName", "unwrap",
            "invoke", "newInstance", "newArrayList", "newHashMap", "random", "nextInt", "nextFloat", "nextDouble",
            "getId", "id", "name", "type", "getType", "pos", "getPos", "getBlockPos",
            "sin", "cos", "tan", "asin", "acos", "atan", "atan2", "pow", "exp", "log", "log10", "signum", "hypot",
            "toRadians", "toDegrees", "floorDiv", "floorMod", "ceilDiv", "fma", "cbrt", "rint", "copySign",
            "fastInverseSqrt", "square", "sign", "floorLog2", "smallestEncompassingPowerOfTwo", "nextGaussian",
            "nextBoolean", "nextLong", "numberOfLeadingZeros", "bitCount", "rotateLeft", "highestOneBit",
            "intBitsToFloat", "floatToIntBits", "doubleToLongBits", "getRed", "getGreen", "getBlue", "getAlpha",
            "argb", "mix", "lerpAngle", "clampedLerp", "smooth", "smoothstep", "clamp01", "ease");
    static final String[] VERBS = {"get", "find", "compute", "calculate", "calc", "create", "make", "build", "read",
            "load", "fetch", "resolve", "pick", "choose", "select", "parse", "lookup", "locate", "determine",
            "collect", "obtain", "acquire", "derive", "extract", "new", "to", "as", "is", "has"};

    Type currentType;

    String semantic(Decl d, List<NameExpr> refs) {
        currentType = d.type;
        Node par = d.node.getParentNode().orElse(null);
        // for-each element: singular of the iterable
        if (d.node instanceof VariableDeclarator vd && vd.getParentNode().orElse(null) instanceof VariableDeclarationExpr vde
                && vde.getParentNode().orElse(null) instanceof ForEachStmt fe) {
            String n = nameOf(unwrapCast(fe.getIterable()));
            return n == null ? null : accept(singular(n), d);
        }
        Expression init = null;
        if (d.node instanceof VariableDeclarator vd) init = vd.getInitializer().orElse(null);
        if (init == null) {
            NameExpr first = null;
            for (NameExpr r : refs)
                if (r.getParentNode().orElse(null) instanceof AssignExpr ae && ae.getTarget() == r
                        && ae.getOperator() == AssignExpr.Operator.ASSIGN && (first == null || begin(r).isBefore(begin(first)))) first = r;
            if (first != null) init = ((AssignExpr) first.getParentNode().get()).getValue();
        }
        if (init != null) {
            String n = fromExpr(unwrapCast(init));
            if (n != null) return accept(n, d);
        }
        // parameters: this.field = param  /  setFoo(param)
        if (d.node instanceof Parameter) {
            for (NameExpr r : refs) {
                Node p = r.getParentNode().orElse(null);
                if (p instanceof AssignExpr ae && ae.getValue() == r && ae.getTarget() instanceof FieldAccessExpr fa
                        && fa.getScope() instanceof ThisExpr) return accept(fa.getNameAsString(), d);
                if (p instanceof MethodCallExpr mc && mc.getArguments().size() == 1 && mc.getArgument(0) == r
                        && mc.getNameAsString().startsWith("set") && mc.getNameAsString().length() > 3)
                    return accept(decap(mc.getNameAsString().substring(3)), d);
            }
        }
        return null;
    }

    static Expression unwrapCast(Expression e) {
        while (true) {
            if (e instanceof CastExpr c) e = c.getExpression();
            else if (e instanceof EnclosedExpr en) e = en.getInner();
            else return e;
        }
    }

    String fromExpr(Expression e) {
        if (e instanceof MethodCallExpr mc && mc.getNameAsString().equals("s") && mc.getArguments().size() == 1
                && (mc.getScope().isEmpty() || mc.getScope().get().toString().equals("EfnWidgets"))
                && mc.getArgument(0) instanceof LiteralStringValueExpr lit) {
            // EfnWidgets.s(14.0F) = 14px scaled to the UI scale
            String v = lit.getValue().replaceAll("[fFdDlL]$", "").replaceAll("\\.0+$", "").replace('.', '_').replace('-', 'm');
            return "px" + v;
        }
        if (e instanceof MethodCallExpr mc) {
            String m = mc.getNameAsString();
            Expression scope = mc.getScope().map(Renamer::unwrapCast).orElse(null);
            if (GENERIC.contains(m) && mc.getArguments().isEmpty() && scope != null) return nameOf(scope);
            if (ELEMENT.contains(m)) { String n = scope == null ? null : nameOf(scope); return n == null ? null : singular(n); }
            if (USELESS.contains(m)) return null;
            String n = noun(m);
            boolean prefixed = n != null && !n.equals(m) && !m.startsWith(n);
            if (!prefixed) {
                // a bare verb-like method name (resolve, raycast, union, ofFloored...) only names
                // primitive/String results; objects keep their type-based name
                boolean staticCall = scope instanceof NameExpr sn && Character.isUpperCase(sn.getNameAsString().charAt(0));
                boolean primLike = currentType instanceof PrimitiveType || (currentType != null && currentType.asString().equals("String"));
                if (primLike) return n;
                if (staticCall || !(currentType instanceof PrimitiveType || (currentType != null && currentType.asString().equals("String"))))
                    return null;
            }
            return n;
        }
        if (e instanceof FieldAccessExpr fa) return fa.getNameAsString().equals("length") ? null : fa.getNameAsString();
        if (e instanceof ArrayAccessExpr aa) { String n = nameOf(unwrapCast(aa.getName())); return n == null ? null : singular(n); }
        return null;
    }

    /** Name of a variable-ish expression: field, local, getter call. */
    String nameOf(Expression e) {
        if (e instanceof FieldAccessExpr fa) return fa.getNameAsString();
        if (e instanceof NameExpr ne) {
            String n = ne.getNameAsString();
            if (Character.isUpperCase(n.charAt(0))) return null;                    // a class, not a variable
            return GENERATED.matcher(n).matches() || n.length() < 3 ? null : n;
        }
        if (e instanceof MethodCallExpr mc && mc.getArguments().isEmpty()) {
            String m = mc.getNameAsString();
            if (m.equals("values") || m.equals("keySet") || m.equals("entrySet")) {
                String n = mc.getScope().map(x -> nameOf(unwrapCast(x))).orElse(null);
                if (m.equals("entrySet")) return "entries";
                return n;
            }
            if (USELESS.contains(m)) return null;
            return noun(m);
        }
        return null;
    }

    static String noun(String m) {
        String r = m;
        boolean stripped = true;
        while (stripped) {
            stripped = false;
            for (String v : VERBS)
                if (r.startsWith(v) && r.length() > v.length() && Character.isUpperCase(r.charAt(v.length()))) {
                    r = decap(r.substring(v.length())); stripped = true; break;
                }
        }
        for (String suf : new String[]{"To", "From", "Of", "For", "At", "With", "In", "Or", "And", "By"})
            if (r.endsWith(suf) && r.length() > suf.length() + 2) { r = r.substring(0, r.length() - suf.length()); break; }
        return r.length() < 2 ? null : r;
    }

    static String singular(String n) {
        if (n.endsWith("ies") && n.length() > 4) return n.substring(0, n.length() - 3) + "y";
        if (n.endsWith("sses") || n.endsWith("xes") || n.endsWith("shes") || n.endsWith("ches")) return n.substring(0, n.length() - 2);
        if (n.endsWith("s") && !n.endsWith("ss") && n.length() > 3) return n.substring(0, n.length() - 1);
        if (n.endsWith("List") || n.endsWith("Set") || n.endsWith("Queue")) {
            String b = n.replaceAll("(List|Set|Queue)$", ""); return b.length() > 1 ? b : null;
        }
        if (n.endsWith("Array") && n.length() > 6) return n.substring(0, n.length() - 5);
        return null;   // can't singularize meaningfully -> fall back to the type-based name
    }

    String accept(String n, Decl d) {
        if (n == null || n.length() < 2 || !Character.isJavaIdentifierStart(n.charAt(0))) return null;
        for (char c : n.toCharArray()) if (!Character.isJavaIdentifierPart(c)) return null;
        n = decap(n);
        if (KEYWORDS.contains(n) || RESERVED.contains(n)) return null;
        // booleans read better as predicates only if derived from is/has/can; otherwise keep the noun
        return n;
    }
}
