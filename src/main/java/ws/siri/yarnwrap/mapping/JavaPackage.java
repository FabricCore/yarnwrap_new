package ws.siri.yarnwrap.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;

/**
 * A Java package is a collection of Java classes
 */
public class JavaPackage implements JavaLike {
    /**
     * Full path to package
     */
    public final String[] path;
    /**
     * Child packages and classes
     */
    private HashMap<String, JavaLike> children = new HashMap<>();

    /**
     * Return a new root package
     * @return root package
     */
    public static JavaPackage root() {
        return new JavaPackage();
    }

    @Override
    public String[] getQualifier() {
        return path;
    }

    @Override
    public String stringQualifier() {
        return String.join("/", path);
    }

    /**
     * Add a class mapping, should only be called for the root package
     * @param mapping
     */
    public void addClass(ClassMapping mapping) {
        if (path.length != 0)
            throw new UnsupportedOperationException("addClass should only be used from root package");
        String name = mapping.getName(0);
        insertClass(mapping, List.of((name == null ? mapping.getSrcName() : mapping.getName(0)).split("/")));
    }

    /**
     * Create root package
     */
    private JavaPackage() {
        path = new String[0];
    }

    /**
     * Create a package with a path
     * @param qualifier path/separated/by/forward/slashes
     */
    private JavaPackage(String qualifier) {
        path = qualifier.split("/");
    }

    /**
     * Insert a JavaClass to current package
     * @param javaClass
     * @param immediateName
     */
    public void insertClass(JavaClass javaClass, String immediateName) {
        if (children.containsKey(immediateName)) {
            throw new RuntimeException("Key already exists `" + immediateName + "`");
        } else {
            children.put(immediateName, javaClass);
        }
    }

    @Override
    public Optional<JavaLike> getRelative(List<String> path) {
        if (path.isEmpty()) {
            return Optional.of(this);
        } else if (children.containsKey(path.getFirst())) {
            return children.get(path.getFirst()).getRelative(path.subList(1, path.size()));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Insert class with class mapping
     * @param mapping
     * @param pathRemaining
     */
    public void insertClass(ClassMapping mapping, List<String> pathRemaining) {
        // if is a class
        if (pathRemaining.size() == 1) {
            JavaClass.insertClass(mapping, pathRemaining.getFirst(), this);
            return;
        }

        if (!children.containsKey(pathRemaining.getFirst())) {
            List<String> newPath = new ArrayList<>(Arrays.asList(path));
            newPath.add(pathRemaining.getFirst());
            children.put(pathRemaining.getFirst(), new JavaPackage(String.join("/", newPath)));
        }

        JavaLike immediateChild = children.get(pathRemaining.getFirst());

        if (immediateChild instanceof JavaPackage) {
            ((JavaPackage) immediateChild).insertClass(mapping, pathRemaining.subList(1, pathRemaining.size()));
        } else {
            JavaClass.insertClass(mapping, pathRemaining.getFirst(), this);
        }
    }

    @Override
    public String toString() {
        List<String> entries = new ArrayList<>();
        children.forEach((ignored, javaLike) -> entries.add(javaLike.toString()));

        return String.join("\n", entries);
    }
}
