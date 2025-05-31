package yarnwrap.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import net.fabricmc.mappingio.tree.MappingTree.ClassMapping;

public class JavaPackage implements JavaLike {
    public final String[] path;
    private HashMap<String, JavaLike> children = new HashMap<>();

    public static JavaPackage root() {
        return new JavaPackage();
    }

    public void addClass(ClassMapping mapping) {
        if(path.length != 0) throw new UnsupportedOperationException("addClass should only be used from root package");
        insertClass(mapping, List.of(mapping.getName(0).split("/")));
    }

    private JavaPackage() {
        path = new String[0];
    }

    private JavaPackage(String qualifier) {
        path = qualifier.split("/");
    }

    public void insertClass(JavaClass javaClass, String immediateName) {
        if(children.containsKey(immediateName)) {
            throw new RuntimeException("Key already exists `" + immediateName + "`");
        } else {
            children.put(immediateName, javaClass);
        }
    }

    @Override
    public JavaLike getRelative(List<String> path) {
        if(path.isEmpty()) {
            return this;
        } else if(children.containsKey(path.getFirst())) {
            return children.get(path.getFirst());
        } else {
            return null;
        }
    }

    public void insertClass(ClassMapping mapping, List<String> pathRemaining) {
        // if is a class
        if(pathRemaining.size() == 1) {
            JavaClass.insertClass(mapping, pathRemaining.getFirst(), this);
            return;
        }

        if(!children.containsKey(pathRemaining.getFirst())) {
            List<String> newPath = new ArrayList<>(Arrays.asList(path));
            newPath.add(pathRemaining.getFirst());
            children.put(pathRemaining.getFirst(), new JavaPackage(String.join("/", newPath)));
        }

        JavaLike immediateChild = children.get(pathRemaining.getFirst());
            
            if(immediateChild instanceof JavaPackage) {
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
