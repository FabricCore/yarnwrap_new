package ws.siri.yarnwrap.mapping;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
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

    public Method[] getMethod(String name, boolean staticOnly) {
        List<Method> methods = new ArrayList<>();

        Class<?> classObj;
        try {
            classObj = Class.forName(String.join(".", mapping.getSrcName().replace('/', '.')));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("could not find class `%s`: %s", stringQualifier(), e));
        }

        mapping.getMethods().stream().forEach((methodMapping) -> {
            String methodName = methodMapping.getName(0);
            if(methodName == null) methodName = methodMapping.getSrcName();
            if (methodName.equals(name)) {
                try {
                    Method method = classObj.getMethod(methodMapping.getSrcName(),
                            methodMapping.getArgs().stream().map((arg) -> {
                                try {
                                    String argName = arg.getSrcName();
                                    if(argName == null) argName = "java/lang/Object"; // probably?
                                    return Class.forName(argName.replace('/', '.'));
                                } catch (ClassNotFoundException e) {
                                    throw new RuntimeException(String.format("could not find class `%s` for args: %s",
                                            arg.getSrcName(), e));
                                }
                            }).toArray(Class<?>[]::new));

                    if (!Modifier.isStatic(method.getModifiers()) && staticOnly)
                        return;

                    methods.add(method);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(String.format("could not find method `%s.%s`: %s", stringQualifier(),
                            methodMapping.getSrcName(), e));
                }
            }
        });

        Optional<JavaLike> parent = getParent();

        if (parent.isPresent() && parent.get() instanceof JavaClass) {
            methods.addAll(Arrays.asList(((JavaClass) parent.get()).getMethod(name, staticOnly)));
        }

        return methods.toArray(Method[]::new);
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

    /**
     * called to set a CLassMapping if a JavaClass is already created for a child
     * class
     * 
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
     * 
     * @param mapping   class mapping
     * @param className class name `ClassParent$ClassChild$ClassGrandchild`
     * @param parent    the JavaPackage the class belongs in
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
     * Get class mapping given a Class&lt;?gt;
     * 
     * @param target Class&lt;?&gt; of the Class
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
        } else if (path.size() == 1) {
            String name = path.getFirst();

            List<Method> methods = new ArrayList<>(Arrays.asList(getMethod(name, true)));
            Optional<JavaLike> parent = getParent();

            if (parent.isPresent() && parent.get() instanceof JavaClass) {
                methods.addAll(Arrays.asList(((JavaClass) parent.get()).getMethod(name, true)));
            }

            if (methods.isEmpty())
                return Optional.empty();
            return Optional.of(new JavaFunction(methods.toArray(Method[]::new), stringQualifier() + "$" + name, this));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return mapping == null ? "[Mapping missing]"
                : String.format("JavaClass(%s -> %s)", stringQualifier(), mapping.getSrcName());
    }

    // @Override
    // public String toString() {
    // List<String> entries = new ArrayList<>();

    // if (mapping != null) {
    // entries.add(String.format("%s -> %s", stringQualifier(),
    // mapping.getSrcName()));
    // }

    // children.forEach((ignored, mapping) -> entries.add(mapping.toString()));

    // return String.join("\n", entries);
    // }
}
