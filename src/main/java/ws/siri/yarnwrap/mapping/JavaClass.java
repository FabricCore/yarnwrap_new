package ws.siri.yarnwrap.mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;

/**
 * A single Java class
 */
public class JavaClass implements JavaLike {
    /**
     * Class mapping
     * 
     * Depending on the order which the classes are added,
     * this may be null in case a child class is added before the parent class
     */
    private ClassMapping mapping = null;
    /**
     * All child classes
     */
    private HashMap<String, JavaClass> children = new HashMap<>();

    /**
     * @return ClassMapping of the current class
     */
    public ClassMapping getMapping() {
        return mapping;
    }

    @Override
    public String[] getQualifier() {
        return stringQualifier().split("\\$|\\/");
    }

    @Override
    public String stringQualifier() {
        String name = mapping.getName(0);
        return name == null ? mapping.getSrcName() : name;
    }

    private JavaClass() {}

    private JavaClass(ClassMapping mapping, List<String> className) {
        if (className.isEmpty()) {
            this.mapping = mapping;
        } else {
            children.put(className.getFirst(), new JavaClass(mapping, className.subList(1, className.size())));
        }
    }

    /**
     * called to set a CLassMapping if a JavaClass is already created for a child class
     * @param mapping
     * @param remainingClassPath
     */
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

    /**
     * Create a new class under a JavaPackage
     * @param mapping class mapping
     * @param className class name `ClassParent$ClassChild$ClassGrandchild`
     * @param parent the JavaPackage the class belongs in
     */
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

    /**
     * Get class mapping given a Class<?>
     * @param target Class<?> of the Class
     * @return
     */
    @NotNull
    public static Optional<ClassMapping> getMapping(Class<?> target) {
        return Optional.ofNullable(MappingTree.getMappingTree().getClass(target.getName().replace('.', '/')));
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
        return mapping == null ? "[Mapping missing]" : String.format("%s -> %s", stringQualifier(), mapping.getSrcName());
    }

    // @Override
    // public String toString() {
    //     List<String> entries = new ArrayList<>();

    //     if (mapping != null) {
    //         entries.add(String.format("%s -> %s", stringQualifier(), mapping.getSrcName()));
    //     }

    //     children.forEach((ignored, mapping) -> entries.add(mapping.toString()));

    //     return String.join("\n", entries);
    // }
}
