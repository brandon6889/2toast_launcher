package net.minecraft;

public class MinecraftCoreMod extends MinecraftMod {
    public MinecraftCoreMod(String name) {
        super(name);
    }
    
    @Override
    public String getPath() {
        return "coremods/" + GameUpdater.getUpdater("dummy").getMCVersion() + "/" + name;
    }
}
