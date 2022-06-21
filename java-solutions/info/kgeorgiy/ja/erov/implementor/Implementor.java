package info.kgeorgiy.ja.erov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.*;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.*;
import java.util.zip.ZipEntry;

/**
 * Java Reflections based implementation of {@link Impler}, {@link JarImpler} interfaces.
 *
 * @see java.lang.reflect
 * @author Egor Erov
 */
public class Implementor implements Impler, JarImpler {
    /**
     * A separator in Java package name.
     */
    private static final char PACKAGE_SEPARATOR = '.';
    /**
     * A {@link System} line separator.
     */
    private static final String LINE_SEPARATOR = System.lineSeparator();
    /**
     * Ending of line with expression of Java code.
     */
    private static final String CODE_LINE_END = ";".concat(LINE_SEPARATOR);
    /**
     * Storage of implementation class source code.
     */
    private StringBuilder classBuilder;

    /**
     * Record of {@link Method} that overrides {@link Method#hashCode()} and {@link Method#equals(Object)}
     * for comparison by signature
     *
     * @param method instance for wrap by record
     */
    private record MethodWrapper(Method method) {
        @Override
        public int hashCode() {
            return Objects.hash(method.getReturnType(), method.getName(), Arrays.hashCode(method.getParameterTypes()));
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MethodWrapper other)) {
                return false;
            }
            return method != null &&
                    other.method != null &&
                    method.getReturnType().equals(other.method.getReturnType()) &&
                    method.getName().equals(other.method.getName()) &&
                    Arrays.equals(method.getParameterTypes(), other.method.getParameterTypes());
        }
    }

    /**
     * Call implementing function according to {@code args} values
     * Print error message into {@link System#err} stream, if:
     * <ul>
     *     <li>{@code args} don't match any supported modes;</li>
     *     <li>{@code args} cannot be casted to required types.</li>
     *     <li>{@link ImplerException} was thrown by according implementing function;</li>
     * </ul>
     * @param args describes working mode, currently support:
     *             <ul>
     *                  <li>{@code [class token, root path]} for {@link #implement(Class, Path)}</li>
     *                  <li>{@code ["-jar", class token, jar file path]} for {@link #implementJar(Class, Path)}</li>
     *             </ul>
    */
    public static void main(String[] args) {
        // :NOTE: add empty lines to improve readability
        // REPLY: fixed
        Implementor implementor = new Implementor();

        try {
            if (args.length == 3) {
                if (!args[0].equals("-jar")) {
                    printErrorMessage();
                    return;
                }
                implementor.implementJar(Class.forName(args[1]), Path.of(args[2]));
            } else {
                if (args.length != 2) {
                    printErrorMessage();
                    return;
                }
                implementor.implement(Class.forName(args[0]), Path.of(args[1]));
            }

        } catch (ClassNotFoundException e) {
            System.err.println("Invalid class token: ".concat(e.getMessage()));
        } catch (InvalidPathException e) {
            System.err.println("Invalid path: ".concat(e.getMessage()));
        } catch (ImplerException e) {
            System.err.println("Implementor error: ".concat(e.getMessage()));
        }
    }


    /**
     * Notify of {@link #main(String[])} arguments amount requirement by writing message in {@link System#err}
     */
    private static void printErrorMessage() {
        System.err.println(
                "USAGE: Implementor <class token> <root path>\n" +
                        "   or  Implementor -jar <class token> <jar file path>"
        );
    }


    /**
     * Produce implementing class or interface specified by provided {@code token}.
     *
     * Generated class classes name is same as classes name of the type token with {@code Impl} suffix
     * added. Generated source code is placed in the relevant subdirectory of the specified
     * {@code root} directory.
     *
     * @param token type token to create implementation for.
     * @param root root directory.
     * @throws ImplerException if:
     * <ul>
     *     <li>{@code token} or {@code root} is null;</li>
     *     <li>{@code token} specify non-class or non-interface objec;</li>
     *     <li>{@code token} specify private or final class/private interface;</li>
     *     <li>This exception was thrown by {@link #printClass(Path)}</li>
     * </ul>
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (token == null) {
            throw new ImplerException("token must be non-null");
        }
        if (root == null) {
            throw new ImplerException("root must be non-null");
        }

        if (token.isPrimitive()) {
            // :NOTE: confusing message, it might be an abstract class
            // REPLY: fixed
            throw new ImplerException("Token is not an interface or an abstract class");
        }

        if ((token.getModifiers() & Modifier.PRIVATE) > 0) {
            throw new ImplerException(token.isInterface() ?
                    "Cannot implement private interface" :
                    "Cannot extend private class");
        }

        if (!token.isInterface() && !isAllowedToBeExtended(token)) {
            throw new ImplerException("Cannot implement class without any public ctor");
        }

        classBuilder = new StringBuilder();

        declareClassHeader(token, token.isInterface() ? "implements" : "extends");
        if (!token.isInterface()) {
            declareConstructors(token);
        }
        declareMethods(token);
        declareEndOfClass();

        printClass(getClassExtensionPath(root, token, "java"));
    }


    /**
     * Produces jar file with implementing class or interface specified by provided {@code token}.
     *
     * Generated class classes name is same as classes name of the type token with {@code Impl} suffix
     * added.
     *
     * @param token type token to create implementation for.
     * @param jarFile target jar file.
     * @throws ImplerException if:
     * <ul>
     *     <li>{@code token} or {@code jarFile} is null;</li>
     *     <li>{@code jarFile} is not a {@code .jar} extended file</li>
     *     <li>{@code jarFile} has not a parent directory</li>
     *     <li>{@link IOException} was thrown while creating/deleting directory to store source code</li>
     *     <li>This exception was thrown by {@link #implement(Class, Path)} or {@link #compile(Class, Path)}</li>
     * </ul>
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (token == null) {
            throw new ImplerException("token must be non-null");
        }
        if (jarFile == null || !jarFile.toString().endsWith(".jar")) {
            throw new ImplerException("root must be non-null");
        }
        jarFile = jarFile.toAbsolutePath();

        final Path tempDir;
        try {
            tempDir = Files.createTempDirectory(Path.of("."), "tmp");
        } catch (NullPointerException e) {
            throw new ImplerException("jarFile has no parent to create temp dir", e);
        } catch (IOException e) {
            throw new ImplerException("Cannot create temp dir for implementation: ".concat(e.getMessage()), e);
        }

        try {
            implement(token, tempDir);
            compile(token, getClassExtensionPath(tempDir, token, "java"));
            printJar(token, tempDir, jarFile);
        } finally {
            clearDirectory(tempDir);
        }
    }


    /**
     * Write into {@link Implementor#classBuilder} lines with package and class name declarations,
     * separated by empty line.
     *
     * @param token type token to create declarations for.
     * @param inheritance type of Java inheritance: {@code "implements"} or {@code "extends"}.
     */
    private void declareClassHeader(Class<?> token, String inheritance) {
        String packageName = token.getPackageName();
        if (!packageName.isEmpty()) {
            classBuilder.append("package ")
                    .append(packageName)
                    .append(CODE_LINE_END).append(LINE_SEPARATOR);
        }
        classBuilder.append("public class ").append(getClassName(token)).append(" ")
                .append(inheritance).append(" ").append(token.getCanonicalName())
                .append(" {").append(LINE_SEPARATOR);
    }

    /**
     * Write into {@link #classBuilder} line with closing bracket declaration.
     */
    private void declareEndOfClass() {
        classBuilder.append("}").append(LINE_SEPARATOR);
    }


    /**
     * Check if {@code tokenModifier} includes {@code modifier} via bitwise arithmetics.
     *
     * @param tokenModifier exploring modifier.
     * @param modifier constant from {@link Modifier} class.
     * @return presence of {@code modifier} in {@code tokenModifier}.
     */
    private static boolean hasModifier(int tokenModifier, int modifier) {
        return (tokenModifier & modifier) > 0;
    }

    /**
     * Check {@link Modifier#ABSTRACT} presence in {@code executable} modifiers.
     * Call {@link #hasModifier(int, int)}.
     *
     * @param executable exploring instance.
     * @return whether {@code executable} is abstract.
     */
    private static boolean isAbstract(Executable executable) {
        return hasModifier(executable.getModifiers(), Modifier.ABSTRACT);
    }

    /**
     * Check {@link Modifier#STATIC} presence in {@code executable} modifiers.
     * Call {@link #hasModifier(int, int)}.
     *
     * @param executable exploring instance.
     * @return whether {@code executable} is static.
     */
    private static boolean isStatic(Executable executable) {
        return hasModifier(executable.getModifiers(), Modifier.STATIC);
    }

    /**
     * Check {@link Modifier#STATIC} presence in {@code executable} modifiers.
     * Call {@link #hasModifier(int, int)}.
     *
     * @param executable exploring instance.
     * @return whether {@code executable} is not private.
     */
    private static boolean isNotPrivate(Executable executable) {
        return !hasModifier(executable.getModifiers(), Modifier.PRIVATE);
    }


    /**
     * Split given array into declared and defined method sets.
     *
     * @param methods array for splitting.
     * @param allMethods all declared methods from {@code methods}.
     * @param definedMethods all defined methods from {@code methods}.
     */
    private void filterMethods(Method[] methods, Set<MethodWrapper> allMethods, Set<MethodWrapper> definedMethods) {
        for (Method method : methods) {
            MethodWrapper methodWrapper = new MethodWrapper(method);
            if (isNotPrivate(method)) {
                if (!isAbstract(method) && !allMethods.contains(methodWrapper)) {
                    definedMethods.add(methodWrapper);
                }
                allMethods.add(methodWrapper);
            }
        }
    }


    /**
     * Find only declared {@link Method} in {@code token} inheritance.
     * If {@code token} is a class, search recursively, otherwise get all methods by call {@link Class#getMethods()}
     * and filter them.
     *
     * @param token type token to filter its {@link Method}.
     * @return {@link List} of {@link Method} that were not defined in parent of {@code token}.
     */
    private List<Method> collectAbstractMethods(Class<?> token) {
        // :NOTE: default methods?
        // REPLY: fixed, add filtering
        if (token.isInterface()) {
            return Arrays.stream(token.getMethods()).filter(Implementor::isAbstract).toList();
        }

        // :NOTE: why do you need TreeSet here?
        // REPLY: fixed, add MethodWrapper to override hashCode() & equals(Object) methods and use HashSet
        Set<MethodWrapper> allMethods = new HashSet<>();
        Set<MethodWrapper> definedMethods = new HashSet<>();

        filterMethods(token.getMethods(), allMethods, definedMethods);

        while (token != null) {
            filterMethods(token.getDeclaredMethods(), allMethods, definedMethods);
            token = token.getSuperclass();
        }

        allMethods.removeAll(definedMethods);
        return allMethods.stream().map(MethodWrapper::method).toList();
    }

    /**
     * Cut from given modifiers tags {@link Modifier#ABSTRACT} and {@link Modifier#TRANSIENT}.
     *
     * @param modifier modifier for cutting.
     * @return cut {@code modifier}.
     */
    private int toNonAbstractModifier(int modifier) {
        return modifier & ~(Modifier.ABSTRACT | Modifier.TRANSIENT);
    }


    /**
     * Determine default value of given type.
     *
     * @param typeToken type provided type token.
     * @return type's default value
     */
    private String getDefaultValue(Class<?> typeToken) {
        if (typeToken.equals(void.class)) {
            return "";
        }
        if (typeToken.equals(boolean.class)) {
            return "false";
        }
        return typeToken.isPrimitive() ? "0" : "null";
    }

    /**
     * Produce whitespace-separated {@link #getDefaultValue(Class)} in left.
     *
     * @param typeToken type provided type token.
     * @return whitespace-separated type's default value
     */
    private String getDefaultReturnValue(Class<?> typeToken) {
        String value = getDefaultValue(typeToken);
        return value.isEmpty() ? "" : " ".concat(value);
    }


    /**
     * Produce sequence of {@link Executable} arguments, with optional types and names adding.
     *
     * This method supposes to name arguments by format:
     * {@code "arg".concat(index of argument in {@code types} array)}.
     *
     * @param types array of types tokens of arguments.
     * @param typed shows, if types of arguments will be written.
     * @param named shows, if names of arguments will be written.
     * @return Comma-separated sequence of arguments.
     */
    private String getArgumentEnumeration(Class<?>[] types, boolean typed, boolean named) {
        return IntStream.range(0, types.length)
                .mapToObj(
                        id -> (typed ? types[id].getCanonicalName() : "")
                                .concat(named ? " ".concat(String.format("arg%d", id)) : ""))
                .collect(Collectors.joining(", "));
    }

    /**
     * Produces optional annotation {@link Override}.
     *
     * @param executable processing instance
     * @return annotation block.
     */
    private String getAnnotations(Executable executable) {
        // :NOTE: it is better to extract such check into separate methods: isAbstract(executable)
        // REPLY: fixed, besides, call it in interface methods filtering in collectAbstractMethods(Class<?>)
        if (isAbstract(executable) && !isStatic(executable)) {
            return "\t@Override".concat(LINE_SEPARATOR);
        }
        return "";
    }

    /**
     * Produces optional {@code modifier}.
     *
     * @param executable processing instance
     * @return modifiers block.
     */
    private String getImplementationModifiers(Executable executable) {
        String result = Modifier.toString(toNonAbstractModifier(executable.getModifiers()));
        return result.concat(!result.isEmpty() ? " " : "");
    }

    /**
     * Produces optional throws block for Java method/ctor
     *
     * @param exceptions array of {@link Exception} type tokens.
     * @return throws block.
     */
    private String getExceptions(Class<?>[] exceptions) {
        if (exceptions.length > 0) {
            return " throws ".concat(getArgumentEnumeration(exceptions, true,false));
        }
        return "";
    }


    /**
     * Write into {@link Implementor#classBuilder} Java executable class members declaration.
     *
     * @param executables Java executable class members to define.
     * @param returnTypeGetter formatted return type getting function.
     * @param nameGetter formatted name getting function.
     * @param bodyGetter formatted default executable class member body getting function.
     * @param <E> Java executable class members, instance of {@link Executable}.
     */
    private <E extends Executable> void declareExecutables(List<E> executables,
                                                           Function<E, String> returnTypeGetter,
                                                           Function<E, String> nameGetter,
                                                           Function<E, String> bodyGetter) {
        for (E executable : executables) {
            String returnType = returnTypeGetter.apply(executable);
            classBuilder.append(LINE_SEPARATOR)
                    .append(getAnnotations(executable))

                    .append("\t")
                    .append(getImplementationModifiers(executable))

                    .append(returnType).append(returnType.isEmpty() ? "" : " ")

                    .append(nameGetter.apply(executable)).append("(")
                    .append(getArgumentEnumeration(executable.getParameterTypes(), true, true))
                    .append(")")

                    .append(getExceptions(executable.getExceptionTypes()))
                    .append(" {").append(LINE_SEPARATOR)

                    .append("\t\t")
                    .append(bodyGetter.apply(executable))
                    .append(CODE_LINE_END).append("\t}").append(LINE_SEPARATOR);
        }
    }

    /**
     * Write into {@link Implementor#classBuilder} declared only methods.
     *
     * Calls {@link #declareExecutables(List, Function, Function, Function)}.
     *
     * @param token type token provides implementing class/interface.
     */
    private void declareMethods(Class<?> token) {
        declareExecutables(
                collectAbstractMethods(token),
                method -> method.getReturnType().getCanonicalName(),
                Method::getName,
                method -> "return".concat(getDefaultReturnValue(method.getReturnType()))
        );
    }

    /**
     * Write into {@link Implementor#classBuilder} directly inherited constructors.
     *
     * Calls {@link #declareExecutables(List, Function, Function, Function)}.
     *
     * @param token type token provides implementing class/interface.
     */
    private void declareConstructors(Class<?> token) {
        declareExecutables(
                List.of(token.getDeclaredConstructors()),
                ctor -> "",
                ctor -> getClassName(token),
                ctor -> "super(".concat(getArgumentEnumeration(ctor.getParameterTypes(), false, true))
                        .concat(")")
        );
    }

    /**
     * Determine whether implementation can be inherited from given class.
     *
     * @param token type token of implementing class.
     * @return inheritance allowance
     */
    private boolean isAllowedToBeExtended(Class<?> token) {
        if (hasModifier(token.getModifiers(), Modifier.FINAL) || token.equals(Enum.class)) {
            return false;
        }
        boolean allowed = false;
        for (Constructor<?> ctor : token.getDeclaredConstructors()) {
            allowed |= isNotPrivate(ctor);
        }
        return allowed;
    }


    /**
     * Write {@link Implementor#classBuilder} contents into implementation source file
     * via {@link #escapeUnicode(String)}.
     *
     * @param path implementation source file
     * @throws ImplerException if:
     * <ul>
     *     <li>{@code path} parent's directory cannot be created;</li>
     *     <li>{@link Writer} proceed {@link IOException} during writing file.</li>
     * </ul>
     */
    private void printClass(Path path) throws ImplerException {
        try {
            // :NOTE: NPE
            /*
                REPLY: it cannot be thrown because $path is path to writing file,
                       this way it has root.resolve(package) as parent, where root is non-null (checked in implement())
             */
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            throw new ImplerException("Cannot create directory for class file: ".concat(e.getMessage()), e);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(escapeUnicode(classBuilder.toString()));
        } catch (IOException e) {
            throw new ImplerException("Writing class file error: ".concat(e.getMessage()), e);
        }
    }


    /**
     * Perform specially formatted {@code token} package name.
     *
     * @param token type token of implementing class or interface.
     * @param separator format of package separator performance.
     * @return formatted package name.
     */
    private String getPackageSeparated(Class<?> token, char separator) {
        return token.getPackageName().replace(PACKAGE_SEPARATOR, separator);
    }

    /**
     * Perform implementation class name.
     *
     * @param token type token of implementing class or interface.
     * @return implementation class name.
     */
    private String getClassName(Class<?> token) {
        return token.getSimpleName().concat("Impl");
    }

    /**
     * Perform implementation class name with package.
     *
     * Calls {@link #getPackagedClassName(Class, char)} with {@link File#separatorChar} as {@code separator}.
     *
     * @param token type token of implementing class or interface.
     * @return implementation name with package.
     */
    private String getPackagedClassName(Class<?> token) {
        return getPackagedClassName(token, File.separatorChar);
    }

    /**
     * Perform specially formatted implementation class name with package.
     *
     * @param token type token of implementing class or interface.
     * @param separator format of path separator performance.
     * @return formatted implementation name with package.
     */
    private String getPackagedClassName(Class<?> token, char separator) {
        return getPackageSeparated(token, separator)
                .concat(String.valueOf(separator))
                .concat(getClassName(token));
    }


    /**
     * Produce {@link Path} for implementation extension.
     *
     * @param root directory root.
     * @param token type token of implementing class or interface.
     * @param extension file extension without any separator.
     * @return path to extended implementation file
     */
    private Path getClassExtensionPath(Path root, Class<?> token, String extension) {
        return root.resolve(getPackagedClassName(token).concat(".").concat(extension));
    }

    /**
     * Recursively clean {@code root} directory.
     *
     * @param root directory to clean.
     * @throws ImplerException if {@link IOException} was thrown by {@link Files#delete(Path)}.
     */
    private void clearDirectory(Path root) throws ImplerException {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new ImplerException("Cannot delete tmp dir: ".concat(e.getMessage()), e);
        }
    }


    /**
     * Escape {@code string} characters according Unicode.
     *
     * @param string escaping data
     * @return escaped data
     */
    private String escapeUnicode(String string) {
        StringBuilder stringBuilder = new StringBuilder();
        for (char ch : string.toCharArray()) {
            stringBuilder.append(ch < 0x80 ? String.valueOf(ch) : String.format("\\u%04X", (int) ch));
        }
        return stringBuilder.toString();
    }

    /**
     * Compile given {@code classFile} with classpath according to {@code token}.
     *
     * Inspired kgeorgiy's test code.
     *
     * @param token class token to get location for classpath.
     * @param classFile file to compile.
     * @throws ImplerException if:
     * <ul>
     *     <li>cannot determine classpath;</li>
     *     <li>compiler ends with non-zero exit code</li>
     * </ul>
     */
    private static void compile(Class<?> token, Path classFile) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        final String classpath;
        try {
            classpath = Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new ImplerException("Cannot set classpath: ".concat(e.getMessage()), e);
        }

        final int exitCode = compiler.run(null, null, null,classFile.toString(), "-cp", classpath);
        if (exitCode != 0) {
            throw new ImplerException("Compiling ".concat(classFile.toString())
                    .concat(" ends with exit code ").concat(String.valueOf(exitCode)));
        }
    }

    /**
     * Create an {@link Manifest} with version and generated application name.
     *
     * @return new {@link Manifest}
     */
    private Manifest getManifest() {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(new Attributes.Name("Created-By"), Implementor.class.getCanonicalName());
        return manifest;
    }

    /**
     * Copy contents of compiled class file into jar archive.
     *
     * @param token type token of implementing class or interface.
     * @param root root directory for class file
     * @param jarFile target {@code .jar} file.
     * @throws ImplerException if {@link IOException} was thrown by {@link Files#copy(Path, OutputStream)}
     */
    private void printJar(Class<?> token, Path root, Path jarFile) throws ImplerException {
        try (JarOutputStream jarStream = new JarOutputStream(Files.newOutputStream(jarFile), getManifest())) {

            jarStream.putNextEntry(new ZipEntry(getPackagedClassName(token, '/').concat(".class")));
            Files.copy(getClassExtensionPath(root, token, "class"), jarStream);

        } catch (IOException e) {
            throw new ImplerException("Error of adding .class file into .jar: ".concat(e.getMessage()), e);
        }
    }

}
