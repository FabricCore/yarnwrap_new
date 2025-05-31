package yarnwrap.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;

public class JavaClass implements JavaLike {
    private ClassMapping mapping = null;
    public HashMap<String, JavaClass> children = new HashMap<>();

    public ClassMapping getMapping() {
        return mapping;
    }

    private JavaClass() {
    }

    private JavaClass(ClassMapping mapping, List<String> className) {
        if (className.isEmpty()) {
            this.mapping = mapping;
        } else {
            children.put(className.getFirst(), new JavaClass(mapping, className.subList(1, className.size())));
        }
    }

    private void setMapping(ClassMapping mapping, List<String> remainingClassPath) {
        if (remainingClassPath.isEmpty()) {
            this.mapping = mapping;
        } else if (children.containsKey(remainingClassPath.getFirst())) {
            children.get(remainingClassPath.getFirst()).setMapping(mapping,
                    remainingClassPath.subList(1, remainingClassPath.size()));
        } else {
            children.put(remainingClassPath.getFirst(),
                    new JavaClass(mapping, remainingClassPath.subList(1, remainingClassPath.size())));
        }
    }

    public static void insertClass(ClassMapping mapping, String className, JavaPackage parent) {
        List<String> classPath = List.of(className.split("\\$"));

        if (parent.getRelative(classPath.getFirst()) == null) {
            parent.insertClass(new JavaClass(mapping, classPath), classPath.getFirst());
        } else {
            JavaLike maybeClass = parent.getRelative(classPath.getFirst());

            if (maybeClass instanceof JavaClass) {
                ((JavaClass) maybeClass).setMapping(mapping, classPath.subList(1, classPath.size()));
            } else {
                throw new RuntimeException("Expected class, found package");
            }
        }
    }

    @Override
    public JavaLike getRelative(List<String> path) {
        if (path.isEmpty()) {
            return this;
        } else if (children.containsKey(path.getFirst())) {
            return children.get(path.getFirst());
        } else {
            throw new RuntimeException(String.format("no such class `%s`", String.join(".", path)));
        }
    }

    @Override
    public String toString() {
        List<String> entries = new ArrayList<>();

        if(this.mapping != null) {
            entries.add(String.format("%s -> %s", this.mapping.getName(0), this.mapping.getSrcName()));
        }

        children.forEach((ignored, mapping) -> entries.add(mapping.toString()));
        
        return String.join("\n", entries);
    }
}
