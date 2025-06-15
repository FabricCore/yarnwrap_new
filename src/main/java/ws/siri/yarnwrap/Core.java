package ws.siri.yarnwrap;

import net.fabricmc.api.ModInitializer;
import ws.siri.yarnwrap.mapping.JavaClass;
import ws.siri.yarnwrap.mapping.JavaLike;
import ws.siri.yarnwrap.mapping.MappingTree;

public class Core implements ModInitializer {
    public static final String MOD_ID = "yarnwrap";

    @Override
    public void onInitialize() {
        MappingTree.init();
    }

    public static void main(String[] args) throws Exception {
        MappingTree.init();
        JavaClass obj = (JavaClass) JavaLike.getWithPath("net.minecraft.client.MinecraftClient").get();
        System.out.println(obj.getRelative("<init>"));
    }
}