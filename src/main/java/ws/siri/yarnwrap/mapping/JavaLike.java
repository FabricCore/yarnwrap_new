package ws.siri.yarnwrap.mapping;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

/**
 * An object with properties
 */
public interface JavaLike {
    /**
     * Get with relative path to the current object
     * @param path
     * @return
     */
    @NotNull
    public Optional<Object> getRelative(List<String> path);

    @NotNull
    default Optional<Object> getRelative(String immediateName) {
        return getRelative(List.of(immediateName));
    }

    @NotNull
    default Optional<Object> getRelative(String[] path) {
        return getRelative(Arrays.asList(path));
    }

    /**
     * Get a class/object with its full path
     * @param path full path, dot separated
     * @return
     */
    @NotNull
    static Optional<Object> getWithPath(String path) {
        return MappingTree.getRoot().getRelative(Arrays.asList(path.split("\\.")));
    }

    /**
     * @return qualifier, i.e. full path split by `.`, `/` and `$`
     */
    @NotNull
    public String[] getQualifier();

    /**
     * @return string qualifier of the class, in format `path/to/package$Class`
     */
    @NotNull
    public String stringQualifier();

    /**
     * @return get immediate parent of class, or the class if called on an object, if exists
     */
    @NotNull
    default Optional<JavaLike> getParent() {
        String[] path = getQualifier();

        if (path.length == 0)
            return Optional.empty();

        // must be JavaLike, force cast
        return MappingTree.getRoot().getRelative(Arrays.asList(path).subList(0, path.length - 1)).map((item) -> (JavaLike) item);
    }
}