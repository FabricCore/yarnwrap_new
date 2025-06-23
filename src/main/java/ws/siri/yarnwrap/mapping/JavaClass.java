package ws.siri.yarnwrap.mapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;
import net.fabricmc.mappingio.tree.MappingTree.FieldMapping;

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

    /**
     * Get a JavaClass with a Class
     * 
     * @param type Source class
     * @return return Optional.of if there that class is in fabric mappings
     */
    public static Optional<JavaClass> getWithClass(Class<?> type) {
        Optional<ClassMapping> mapping = JavaClass.getMapping(type);

        if (mapping.isPresent()) {
            String name = mapping.get().getName(0);
            name = name == null ? mapping.get().getSrcName() : name;

            Optional<Object> javaLike = MappingTree.getRoot().getRelative(Arrays.asList(name.split("/|\\$")));

            if (javaLike.isPresent() && javaLike.get() instanceof JavaClass) {
                return Optional.of((JavaClass) javaLike.get());
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    /**
     * get a field using its human readable name
     * 
     * @param name       name of the field
     * @param staticOnly whether only static fields should be returned
     * @return Optional.of if a field is found with that name
     */
    public Optional<Field> getMappedField(String name, boolean staticOnly) {
        Class<?> classObj;
        try {
            classObj = Class.forName(String.join(".", mapping.getSrcName().replace('/', '.')));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("could not find class `%s`: %s", stringQualifier(), e));
        }

        for (FieldMapping fieldMapping : mapping.getFields()) {
            String fieldName = fieldMapping.getName(0);
            if (fieldName == null)
                fieldName = fieldMapping.getSrcName();

            if (!fieldName.equals(name))
                continue;

            try {
                Field field = classObj.getDeclaredField(fieldMapping.getSrcName());
                if (!Modifier.isStatic(field.getModifiers()) && staticOnly)
                    continue;

                return Optional.ofNullable(field);
            } catch (Exception e) {
                throw new RuntimeException(
                        String.format("could not get declared field `%s/%s`: `%s`", stringQualifier(), name, e));
            }
        }

        return Optional.empty();
    }

    /**
     * get a field using its actual Java name (aka obfuscated name, also work for
     * normal Java classes)
     * 
     * @param name       name of the field
     * @param type       Class to get
     * @param staticOnly whether only static fields should be returned
     * @return Optional.of if a field is found with that name
     */
    public static Optional<Field> getSrcField(String name, Class<?> type, boolean staticOnly) {
        try {
            Field found = type.getField(name);
            if (!staticOnly || Modifier.isStatic(found.getModifiers()))
                return Optional.of(found);
        } catch (Exception ignored) {
        }

        for (Class<?> interfaceImpl : type.getInterfaces()) {
            Optional<Field> found = getSrcField(name, interfaceImpl, staticOnly);
            if (found.isPresent())
                return found;
        }

        try {
            return getSrcField(name, type.getSuperclass(), staticOnly);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Combines getSrcField and getField, the field with the human readable name has
     * priority
     * 
     * @param name
     * @param staticOnly
     * @return
     */
    public Optional<Field> getField(String name, boolean staticOnly) {
        Class<?> type;

        try {
            type = Class.forName(String.join(".", mapping.getSrcName().replace('/', '.')));
        } catch (Exception e) {
            throw new RuntimeException("Could not get class when getting field: " + e);
        }

        Optional<Field> res = getMappedField(name, staticOnly);

        if (res.isPresent())
            return res;
        else
            return getSrcField(name, type, staticOnly);
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

    /**
     * get methods with the obfuscated name (works with normal Java classes)
     * 
     * @param name       name of the field
     * @param staticOnly whether only static methods should be returned
     * @param type       Class of the class to look at
     * @return returns all the methods with that name, regardless of the parameters
     */
    public static Method[] getSrcMethod(String name, boolean staticOnly, Class<?> type) {
        List<Method> methods = new ArrayList<>();

        for (Method method : type.getDeclaredMethods()) {
            if (staticOnly && !Modifier.isStatic(method.getModifiers()))
                continue;

            if (method.getName().equals(name))
                methods.add(method);
        }

        for (Class<?> interfaceImpl : type.getInterfaces()) {
            methods.addAll(Arrays.asList(getSrcMethod(name, staticOnly, interfaceImpl)));
        }

        try {
            methods.addAll(Arrays.asList(getSrcMethod(name, staticOnly, type.getSuperclass())));
        } catch (Exception e) {
            // reached Object
        }

        return methods.toArray(Method[]::new);
    }

    // modified from
    // src/main/java/net/fabricmc/mappingio/format/proguard/ProGuardFileWriter.java
    /**
     * take a JVM descriptor and convert it to a Class
     * 
     * @param descriptor
     * @return
     */
    private static Class<?> toJavaType(String descriptor) {
        switch (descriptor.charAt(0)) {
            case 'B':
                return byte.class;
            case 'S':
                return short.class;
            case 'I':
                return int.class;
            case 'J':
                return long.class;
            case 'F':
                return float.class;
            case 'D':
                return double.class;
            case 'C':
                return char.class;
            case 'Z':
                return boolean.class;
            case 'V':
                return void.class;
        }

        StringBuilder result = new StringBuilder();
        int arrayLevel = 0;
        boolean isObject = false;

        for (int i = 0; i < descriptor.length(); i++) {
            switch (descriptor.charAt(i)) {
                case '[':
                    arrayLevel++;
                    break;
                case 'L':
                    isObject = true;
                    while (i + 1 < descriptor.length()) {
                        char c = descriptor.charAt(++i);

                        if (c == '/') {
                            result.append('.');
                        } else if (c == ';') {
                            break;
                        } else {
                            result.append(c);
                        }
                    }

                    break;
                case 'B':
                case 'S':
                case 'I':
                case 'J':
                case 'F':
                case 'D':
                case 'C':
                case 'Z':
                case 'V':
                    result.append(descriptor.charAt(i));
                    break;
                default:
                    throw new IllegalArgumentException("Unknown character in descriptor: " + descriptor.charAt(i));
            }
        }

        if (arrayLevel != 0) {
            if (isObject) {
                result.insert(0, 'L');
            }
            result.insert(0, "[".repeat(arrayLevel));
            result.append(';');
        }

        try {
            return Class.forName(result.toString());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    String.format("class not found when parsing descriptor `%s`: %s", result.toString(), e));
        }
    }

    /**
     * Get all the constructor of the class, given a class
     * 
     * @param type class to get constructor for
     * @return
     */
    public static Constructor<?>[] getConstructor(Class<?> type) {
        return type.getDeclaredConstructors();
        // List<Constructor<?>> constructors = new ArrayList<>();

        // constructors.addAll(Arrays.asList());

        // try {
        // constructors.addAll(Arrays.asList(getConstructor(type.getSuperclass())));
        // } catch (Exception e) {
        // }

        // return constructors.toArray(Constructor<?>[]::new);
    }

    /**
     * get all the constrsuctor for the current wrapped class
     * 
     * @return
     */
    public Constructor<?>[] getConstructor() {
        Class<?> classObj;
        try {
            classObj = Class.forName(String.join(".", mapping.getSrcName().replace('/', '.')));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("could not find class `%s`: %s", stringQualifier(), e));
        }

        return getConstructor(classObj);
    }

    /**
     * get a method by its human readable name
     * 
     * @param name       the name of the method
     * @param staticOnly whether only static methods should be included
     * @return list of all methods with that name
     */
    public Method[] getMappedMethod(String name, boolean staticOnly) {
        List<Method> methods = new ArrayList<>();

        Class<?> classObj;
        try {
            classObj = Class.forName(String.join(".", mapping.getSrcName().replace('/', '.')));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("could not find class `%s`: %s", stringQualifier(), e));
        }

        mapping.getMethods().stream().forEach((methodMapping) -> {
            String methodName = methodMapping.getName(0);
            if (methodName == null)
                methodName = methodMapping.getSrcName();
            if (methodName.equals(name)) {
                try {
                    String desc = methodMapping.getSrcDesc();
                    Class<?>[] args = Arrays
                            .stream(desc.substring(desc.indexOf('(') + 1, desc.indexOf(')')).split(";"))
                            .filter((s) -> s.length() != 0)
                            .map(JavaClass::toJavaType).toArray(Class<?>[]::new);

                    Method method = classObj.getDeclaredMethod(methodMapping.getSrcName(),
                            args);

                    if (!Modifier.isStatic(method.getModifiers()) && staticOnly)
                        return;

                    methods.add(method);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(String.format("could not find method `%s/%s`: %s", stringQualifier(),
                            methodMapping.getSrcName(), e));
                }
            }
        });

        // Optional<JavaLike> parent = getParent();

        // if (parent.isPresent() && parent.get() instanceof JavaClass) {
        // methods.addAll(Arrays.asList(((JavaClass) parent.get()).getMethod(name,
        // staticOnly)));
        // }

        for (Class<?> interfaceImpl : classObj.getInterfaces()) {
            Optional<JavaClass> type = getWithClass(interfaceImpl);

            if (type.isPresent()) {
                methods.addAll(Arrays.asList(type.get().getMappedMethod(name, staticOnly)));
            }
        }

        try {
            Optional<JavaClass> type = getWithClass(classObj.getSuperclass());

            if (type.isPresent()) {
                methods.addAll(Arrays.asList(type.get().getMappedMethod(name, staticOnly)));
            }
        } catch (Exception e) {
            // reached Object
        }

        return methods.toArray(Method[]::new);
    }

    /**
     * combines both getMappedMethod and getSrcMethod
     * 
     * @param name       name of the method to look for
     * @param staticOnly whether only static methods should be included
     * @return the list of all methods with that name
     */
    public Method[] getMethod(String name, boolean staticOnly) {
        List<Method> methods = new ArrayList<>();

        methods.addAll(Arrays.asList(getMappedMethod(name, staticOnly)));
        try {
            methods.addAll(Arrays.asList(getSrcMethod(name, staticOnly,
                    Class.forName(String.join(".", mapping.getSrcName().replace('/', '.'))))));
        } catch (Exception e) {
            throw new RuntimeException(String.format("could not find class `%s`: %s", stringQualifier(), e));
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

        // must be a class, force cast
        Optional<JavaLike> immediateChild = parent.getRelative(classPath.getFirst()).map((item) -> (JavaLike) item);

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

    /**
     * set a static field to a new value, throw an error if the field is present but
     * the could not set the value of the field for reasons
     * 
     * @param name  the name of the field
     * @param value the static
     * @return true if the field is present
     */
    public boolean setField(String name, Object value) {
        value = JavaObject.unwrapAll(value);

        try {
            Optional<Field> field = getField(name, true);
            if (field.isPresent()) {
                field.get().set(null, value);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not set field `" + name + "` because " + e);
        }
    }

    @Override
    public Optional<Object> getRelative(List<String> path) {
        if (path.isEmpty()) {
            return Optional.of(this);
        } else if (children.containsKey(path.getFirst())) {
            return children.get(path.getFirst()).getRelative(path.subList(1, path.size()));
        } else if (path.size() == 1) {
            String name = path.getFirst();

            if (name.equals("<init>")) {
                Constructor<?>[] constructors = getConstructor();

                if (constructors.length == 0)
                    return Optional.empty();

                return Optional.of(new JavaFunction(constructors, stringQualifier() + "$<init>", this));
            }

            Optional<Field> field = getField(name, true);

            if (field.isPresent())
                try {
                    return Optional.of(JavaObject.autoWrap(field.get().get(null)));
                } catch (Exception e) {
                    throw new RuntimeException("Could not get field for class: " + e);
                }

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
        return mapping == null ? "JavaClass([Mapping missing])"
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
