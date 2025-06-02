package ws.siri.yarnwrap.mapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

public class MappingTree {
    private static VisitableMappingTree mappingTree = new MemoryMappingTree();
    private static JavaPackage packageRoot = JavaPackage.root();

    public static void init() {
        walkMappings(
                FabricLoader.getInstance().getModContainer("yarnwrap").get().findPath("yarn").get(),
                mappingTree);
        // walkMappings(java.nio.file.Path.of("src/main/resources/yarn"), mappingTree);
        // testing version
        mappingTree.getClasses().stream().forEach((entry) -> packageRoot.addClass(entry));
    }

    private static void walkMappings(Path dir, VisitableMappingTree tree) {
        try {
            try (Stream<Path> stream = Files.list(dir)) {
                stream.forEach((entry) -> {
                    if (Files.isDirectory(entry))
                        walkMappings(entry, tree);
                    else if (FilenameUtils.getExtension(entry.getFileName().toString()).equals("mapping"))
                        try {
                            MappingReader.read(entry, tree);
                        } catch (IOException ignored) {
                        }
                });
            }
        } catch (IOException ignored) {
        }
    }

    public static JavaPackage getRoot() {
        return packageRoot;
    }
}