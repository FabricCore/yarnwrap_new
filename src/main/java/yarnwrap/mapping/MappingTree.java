package yarnwrap.mapping;

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
    public static VisitableMappingTree mappingTree = new MemoryMappingTree();
    public static JavaPackage packageRoot = JavaPackage.root();

    public static void init() {
        walkMappings(
                FabricLoader.getInstance().getModContainer("yarnwrap").get().findPath("src/main/resources/yarn").get(),
                mappingTree);
        mappingTree.getClasses().stream().forEach((entry) -> packageRoot.addClass(entry));
    }

    public static void walkMappings(Path dir, VisitableMappingTree tree) {
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
}