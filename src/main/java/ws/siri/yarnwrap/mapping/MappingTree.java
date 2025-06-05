package ws.siri.yarnwrap.mapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;

/**
 * Contains static resources such as the mapping tree and JavaPackage
 * 
 * Should call .init() before accessing anything related to yarnwrap
 */
public class MappingTree {
    /**
     * mapping tree, walked only when .init() is called
     */
    private static VisitableMappingTree mappingTree = new MemoryMappingTree();
    /**
     * package root, empty unless classes are added to it
     */
    private static JavaPackage packageRoot = JavaPackage.root();

    /**
     * Initialises mapping tree using a copy of yarn mappings.
     */
    public static void init() {
        walkMappings(
                FabricLoader.getInstance().getModContainer("yarnwrap").get().findPath("yarn").get(),
                mappingTree);
        // walkMappings(java.nio.file.Path.of("src/main/resources/yarn"), mappingTree);
        // testing version
        mappingTree.getClasses().stream().forEach((entry) -> packageRoot.addClass(entry));
    }

    /**
     * Visite a directory and read all mapping files recursively into a mapping tree.
     * @param dir directory to search for mapping files
     * @param tree mapping tree to write to
     */
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

    /**
     * @return package representing Java root `/`
     */
    @NotNull
    public static JavaPackage getRoot() {
        return packageRoot;
    }

    /**
     * the mapping tree containing all the mapping files
     * @return
     */
    @NotNull
    public static VisitableMappingTree getMappingTree() {
        return mappingTree;
    }
}