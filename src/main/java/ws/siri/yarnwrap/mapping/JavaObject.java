package ws.siri.yarnwrap.mapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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

            Optional<JavaLike> javaLike = MappingTree.getRoot().getRelative(Arrays.asList(name.split("/|\\$")));

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
        return String.format("JavaObject(%s%s)", internal, type.isPresent() ? " -> " + type.get().stringQualifier() : "");
    }

    @Override
    public @NotNull Optional<JavaLike> getRelative(List<String> path) {
        if (path.size() != 1)
            return Optional.empty();

        String name = path.getFirst();

        Optional<Object> field;
        if(type.isPresent()) {
            field = type.get().getField(name, internal);
        } else {
            field = JavaClass.getSrcField(name, internal.getClass(), internal);
        }

        if(field.isPresent()) return Optional.of(new JavaObject(field));

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
}
