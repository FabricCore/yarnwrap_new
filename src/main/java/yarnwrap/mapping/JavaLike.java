package yarnwrap.mapping;

import java.util.List;

public interface JavaLike {
    // nullable
    public JavaLike getRelative(List<String> path);

    default JavaLike getRelative(String immediateName) {
        return getRelative(List.of(immediateName));
    }
}