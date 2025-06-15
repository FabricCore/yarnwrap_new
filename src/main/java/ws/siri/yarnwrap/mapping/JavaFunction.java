package ws.siri.yarnwrap.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import ws.siri.yarnwrap.common.ScriptFunction;

/**
 * Represents a set of overloaded methods with the same name, chooses the
 * correct method to execute when called with specific parameters
 */
public class JavaFunction implements ScriptFunction, JavaLike {
    /**
     * collection of the methods and their parameters
     */
    public final HashMap<Class<?>[], Executable> methods = new HashMap<>();
    /**
     * string qualifier for the method
     */
    public final String qualifier;
    /**
     * parent object or class
     */
    public final JavaLike parent;

    /**
     * construct a new JavaFunction
     * 
     * @param potentialMethods list of all methods with that name
     * @param qualifier        string qualifier for the method
     * @param parent           parent object or class
     */
    public JavaFunction(Executable[] potentialMethods, String qualifier, JavaLike parent) {
        Arrays.stream(potentialMethods)
                .forEach((method) -> methods.put(method.getParameterTypes(), method));
        this.qualifier = qualifier;
        this.parent = parent;
    }

    /**
     * converts primitive classes to the Wrapped primitive classes (int -> Integer)
     * 
     * @param primitiveClass
     * @return
     */
    private static Class<?> primitiveWrapper(Class<?> primitiveClass) {
        if (primitiveClass == int.class)
            return Integer.class;
        if (primitiveClass == char.class)
            return Character.class;
        if (primitiveClass == double.class)
            return Double.class;
        if (primitiveClass == float.class)
            return Float.class;
        if (primitiveClass == long.class)
            return Long.class;
        if (primitiveClass == short.class)
            return Short.class;
        if (primitiveClass == byte.class)
            return Byte.class;
        if (primitiveClass == boolean.class)
            return Boolean.class;

        throw new UnsupportedOperationException(primitiveClass.getName() + " is not a primitive");
    }

    /**
     * check if type b can be converted to type a
     * 
     * @param a
     * @param b
     * @return true if such conversion is possible
     */
    public boolean areEquivalent(Class<?> a, Class<?> b) {
        if (a.isAssignableFrom(b))
            return true;

        if (a.isPrimitive())
            a = primitiveWrapper(a);
        if (b.isPrimitive())
            b = primitiveWrapper(b);

        return a == b;
    }

    /**
     * runs the function with specific parameters
     */
    public Object run(Object... args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if (args[0] instanceof JavaObject) {
                args[0] = ((JavaObject) args[0]).get();
            }
        }

        Class<?>[] argTypes = Arrays.stream(args).map((arg) -> arg.getClass()).toArray(Class<?>[]::new);

        Executable executable = null;

        if (methods.containsKey(argTypes)) {
            executable = methods.get(argTypes);
        } else {
            signatureLoop: for (Class<?>[] signature : methods.keySet()) {
                if (signature.length != argTypes.length)
                    continue;

                for (int i = 0; i < signature.length; i++) {
                    if (!areEquivalent(signature[i], argTypes[i]))
                        continue signatureLoop;
                }

                executable = methods.get(signature);
                break signatureLoop;
            }

            if (executable == null)
                throw new UnsupportedOperationException(
                        String.format("no method implementation with arguments `%s`", Arrays.toString(argTypes)));
        }

        if (executable instanceof Constructor) {
            return ((Constructor<?>) executable).newInstance(args);
        } else {
            Method method = (Method) executable;
            if (Modifier.isStatic(method.getModifiers())) {
                return method.invoke(null, args);
            } else if (parent instanceof JavaObject) {
                return method.invoke(((JavaObject) parent).internal, args);
            } else {
                throw new UnsupportedOperationException(
                        String.format("no static implementation with arguments `%s`", Arrays.toString(argTypes)));
            }
        }
    }

    @Override
    public @NotNull Optional<JavaLike> getRelative(List<String> path) {
        return Optional.empty();
    }

    @Override
    public @NotNull String[] getQualifier() {
        return qualifier.split("\\$|/");
    }

    @Override
    public @NotNull String stringQualifier() {
        return qualifier;
    }

    @Override
    public String toString() {
        return String.format("JavaPackage(%s)", stringQualifier());
    }
}
