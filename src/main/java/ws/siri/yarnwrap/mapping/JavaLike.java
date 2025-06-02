package ws.siri.yarnwrap.mapping;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

public interface JavaLike {
    @NotNull
    public Optional<JavaLike> getRelative(List<String> path);

    @NotNull
    default Optional<JavaLike> getRelative(String immediateName) {
        return getRelative(List.of(immediateName));
    }

    @NotNull
    default Optional<JavaLike> getRelative(String[] path) {
        return getRelative(Arrays.asList(path));
    }

    @NotNull
    public String[] getQualifier();

    @NotNull
    public String stringQualifier();

    default Optional<JavaLike> getParent() {
        String[] path = getQualifier();

        if (path.length == 0)
            return Optional.empty();

        return MappingTree.getRoot().getRelative(Arrays.asList(path).subList(0, path.length - 1));
    }
}