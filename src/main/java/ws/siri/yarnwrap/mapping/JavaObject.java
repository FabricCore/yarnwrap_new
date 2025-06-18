package ws.siri.yarnwrap.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;

/**
 * Wrapper for a JavaObject.
 * 
 * If the contained object is in yarn mappings,
 * will allow deobfuscated names to be used for fields and functions.
 */
public class JavaObject implements JavaLike {
    /**
     * Contained object
     */
    public final Object internal;
    /**
     * Only nonempty if contained object is in yarn mappings
     */
    public final Optional<JavaClass> type;

    /**
     * @param internal object to wrap
     */
    public JavaObject(Object internal) {
        // TODO can maybe use JavaClass.getWithClass
        this.internal = internal;

        Optional<ClassMapping> mapping = JavaClass.getMapping(internal.getClass());

        if (mapping.isPresent()) {
            String name = mapping.get().getName(0);
            name = name == null ? mapping.get().getSrcName() : name;

            Optional<Object> javaLike = MappingTree.getRoot().getRelative(Arrays.asList(name.split("/|\\$")));

            if (javaLike.isPresent() && javaLike.get() instanceof JavaClass) {
                this.type = Optional.of((JavaClass) javaLike.get());
            } else {
                this.type = Optional.empty();
            }
        } else {
            type = Optional.empty();
        }
    }

    /**
     * @return the underlying object
     */
    public Object get() {
        return internal;
    }

    @Override
    public String toString() {
        return String.format("JavaObject(%s%s)", internal,
                type.isPresent() ? " -> " + type.get().stringQualifier() : "");
    }

    /**
     * this will throw an error if the field exists, but could not be set for
     * whatever reason
     * 
     * @param name  name of field
     * @param value new value of field
     * @return returns true if updated, no otherwise
     */
    public boolean setField(String name, Object value) {
        value = unwrapAll(value);

        try {
            Optional<Field> field;
            if (type.isPresent()) {
                field = type.get().getField(name, false);
            } else {
                field = JavaClass.getSrcField(name, internal.getClass(), false);
            }

            if (field.isPresent()) {
                field.get().set(null, value);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not set field `" + name + "` because " + e);
        }
    }

    @Override
    public @NotNull Optional<Object> getRelative(List<String> path) {
        if (path.size() != 1)
            return Optional.empty();

        if (path.getFirst().equals("<init>")) {
            Constructor<?>[] constructors = JavaClass.getConstructor(internal.getClass());

            if (constructors.length == 0)
                return Optional.empty();

            return Optional.of(new JavaFunction(constructors, stringQualifier() + "$<init>", this));
        }

        String name = path.getFirst();

        Optional<Field> field;
        if (type.isPresent()) {
            field = type.get().getField(name, false);
        } else {
            field = JavaClass.getSrcField(name, internal.getClass(), false);
        }

        if (field.isPresent())
            try {
                return Optional.of(autoWrap(field.get().get(internal)));
            } catch (Exception e) {
                throw new RuntimeException("Could not get field for object: " + e);
            }

        List<Method> methods = new ArrayList<>(Arrays.stream(internal.getClass().getDeclaredMethods())
                .filter((method) -> method.getName().equals(name)).toList());

        if (type.isPresent()) {
            methods.addAll(Arrays.asList(type.get().getMethod(name, false)));
        } else {
            methods.addAll(Arrays.asList(JavaClass.getSrcMethod(name, false, internal.getClass())));
        }

        if (methods.isEmpty())
            return Optional.empty();
        return Optional.of(new JavaFunction(methods.toArray(Method[]::new), stringQualifier() + "$" + name, this));
    }

    @Override
    public @NotNull String[] getQualifier() {
        if (type.isPresent()) {
            return type.get().getQualifier();
        } else {
            return internal.getClass().getName().split("\\.|\\$");
        }
    }

    @Override
    public @NotNull String stringQualifier() {
        if (type.isPresent()) {
            return type.get().stringQualifier();
        } else {
            return internal.getClass().getName().replace('.', '/');
        }
    }

    /**
     * List of all class/classes that beging with this path to use the raw value
     * instead of wrapping it
     */
    public static HashSet<String> doNotWrap = new HashSet<>(Arrays.asList(new String[] {
            "java"
    }));

    /**
     * Add a class to not wrap
     * 
     * @param name
     * @return
     */
    public static boolean addNoWrap(String name) {
        return doNotWrap.add(name);
    }

    /**
     * Check if an object should be wrapped, returns true if not in the noWrap set
     * 
     * @param source
     * @return
     */
    public static boolean shouldWrap(Object source) {
        if(source == null) return false;

        if (source instanceof JavaObject) {
            return shouldWrap(((JavaObject) source).internal);
        }

        String[] path = source.getClass().getName().split("\\.|\\$|/");
        for (int i = 1; i < path.length; i++) {
            if (doNotWrap.contains(String.join(".", Arrays.asList(path).subList(0, i).toArray(String[]::new)))) {
                return false;
            }
        }

        return true;
    }

    /**
     * remove all wrappings from an object
     * 
     * @param source
     * @return
     */
    public static Object unwrapAll(Object source) {
        if (source instanceof JavaObject) {
            return unwrapAll(((JavaObject) source).internal);
        }

        return source;
    }

    /**
     * automatically decide whether an object should be wrapped
     * 
     * @param source
     * @return
     */
    public static Object autoWrap(Object source) {
        if (shouldWrap(source)) {
            if (source instanceof JavaLike) {
                return source;
            }

            return new JavaObject(source);
        }

        return unwrapAll(source);
    }
}
