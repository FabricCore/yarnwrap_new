package ws.siri.yarnwrap.mapping;

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
        System.out.println(internal.getClass().getName());
        this.internal = internal;

        Optional<ClassMapping> mapping = JavaClass.getMapping(internal.getClass());

        if (mapping.isPresent()) {
            String name = mapping.get().getName(0);
            name = name == null ? mapping.get().getSrcName() : name;

            Optional<JavaLike> javaLike = MappingTree.getRoot().getRelative(Arrays.asList(name.split("/|\\$")));

            if(javaLike.isPresent() && javaLike.get() instanceof JavaClass) {
                this.type = Optional.of((JavaClass) javaLike.get());
            } else {
                this.type = Optional.empty();
            }
        } else {
            type = Optional.empty();
        }
    }

    @Override
    public String toString() {
        return String.format("JavaObject(%s)", internal);
    }

    @Override
    public @NotNull Optional<JavaLike> getRelative(List<String> path) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRelative'");
    }

    @Override
    public @NotNull String[] getQualifier() {
        if(type.isPresent()) {
            return type.get().getQualifier();
        } else {
            return internal.getClass().getName().split("\\.|\\$");
        }
    }

    @Override
    public @NotNull String stringQualifier() {
        if(type.isPresent()) {
            return type.get().stringQualifier();
        } else {
            return internal.getClass().getName().replace('.', '/');
        }
    }
}
