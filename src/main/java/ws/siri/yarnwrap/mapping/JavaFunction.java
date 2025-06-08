package ws.siri.yarnwrap.mapping;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import ws.siri.yarnwrap.common.ScriptFunction;

public class JavaFunction implements ScriptFunction, JavaLike {
    public final HashMap<Class<?>[], Method> methods = new HashMap<>();
    public final String qualifier;
    public final JavaLike parent;

    public JavaFunction(Method[] potentialMethods, String qualifier, JavaLike parent) {
        Arrays.stream(potentialMethods)
                .forEach((method) -> methods.put(method.getParameterTypes(), method));
        this.qualifier = qualifier;
        this.parent = parent;
    }

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

    private boolean areEquivalent(Class<?> a, Class<?> b) {
        if (a.isAssignableFrom(b))
            return true;

        if (a.isPrimitive())
            a = primitiveWrapper(a);
        if (b.isPrimitive())
            b = primitiveWrapper(b);

        return a == b;
    }

    public Object run(Object... args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if (args[0] instanceof JavaObject) {
                args[0] = ((JavaObject) args[0]).get();
            }
        }

        Class<?>[] argTypes = Arrays.stream(args).map((arg) -> arg.getClass()).toArray(Class<?>[]::new);

        Method method = null;

        if (methods.containsKey(argTypes)) {
            method = methods.get(argTypes);
        } else {
            signatureLoop: for (Class<?>[] signature : methods.keySet()) {
                if (signature.length != argTypes.length)
                    continue;

                for (int i = 0; i < signature.length; i++) {
                    if (!areEquivalent(signature[i], argTypes[i]))
                        continue signatureLoop;
                }

                method = methods.get(signature);
                break signatureLoop;
            }

            if (method == null)
                throw new UnsupportedOperationException(
                        String.format("no method implementation with arguments `%s`", Arrays.toString(argTypes)));
        }

        if (Modifier.isStatic(method.getModifiers())) {
            return method.invoke(null, args);
        } else if (parent instanceof JavaObject) {
            return method.invoke(((JavaObject) parent).internal, args);
        } else {
            throw new UnsupportedOperationException(
                    String.format("no static implementation with arguments `%s`", Arrays.toString(argTypes)));
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
}
