package ws.siri.yarnwrap.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;

public class JavaClass implements JavaLike {
    private ClassMapping mapping = null;
    private HashMap<String, JavaClass> children = new HashMap<>();

    public ClassMapping getMapping() {
        return mapping;
    }

    @Override
    public String[] getQualifier() {
        String[] chunks = stringQualifier().split("\\$");
        List<String> qualifier = new ArrayList<>(Arrays.asList(chunks[0].split("/")));
        qualifier.addAll(Arrays.asList(chunks).subList(1, chunks.length));
        return qualifier.toArray(new String[0]);
    }

    @Override
    public String stringQualifier() {
        String name = mapping.getName(0);
        return name == null ? mapping.getSrcName() : mapping.getName(0);
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
        } else {
            if (children.containsKey(remainingClassPath.getFirst())) {
                children.get(remainingClassPath.getFirst()).setMapping(mapping,
                        remainingClassPath.subList(1, remainingClassPath.size()));
            } else {
                children.put(remainingClassPath.getFirst(),
                        new JavaClass(mapping, remainingClassPath.subList(1, remainingClassPath.size())));
            }
        }
    }

    public static void insertClass(ClassMapping mapping, String className, JavaPackage parent) {
        List<String> classPath = List.of(className.split("\\$"));

        Optional<JavaLike> immediateChild = parent.getRelative(classPath.getFirst());

        if (immediateChild.isEmpty()) {
            parent.insertClass(new JavaClass(mapping, classPath.subList(1, classPath.size())), classPath.getFirst());
        } else {
            if (immediateChild.get() instanceof JavaClass) {
                ((JavaClass) immediateChild.get()).setMapping(mapping, classPath.subList(1, classPath.size()));
            } else {
                throw new RuntimeException("Expected class, found package");
            }
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

    @Override
    public String toString() {
        List<String> entries = new ArrayList<>();

        if (mapping != null) {
            System.out.println("got here");
            entries.add(String.format("%s -> %s", stringQualifier(), mapping.getSrcName()));
        }

        children.forEach((ignored, mapping) -> entries.add(mapping.toString()));

        return String.join("\n", entries);
    }
}
