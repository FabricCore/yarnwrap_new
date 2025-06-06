package ws.siri.yarnwrap;

import net.fabricmc.api.ModInitializer;
import ws.siri.yarnwrap.mapping.JavaFunction;
import ws.siri.yarnwrap.mapping.JavaObject;
import ws.siri.yarnwrap.mapping.MappingTree;

public class Core implements ModInitializer {
    public static final String MOD_ID = "yarnwrap";

    @Override
    public void onInitialize() {
        MappingTree.init();
    }

    public static void main(String[] args) throws Exception {
        JavaObject obj = new JavaObject("hello");
        JavaFunction func = (JavaFunction) obj.getRelative("replace").get();
        System.out.println(func.run('e', '1'));
    }
}