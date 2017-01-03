package net.minecraft;

public class MinecraftCoreMod extends MinecraftMod {
    @Override
    public String getPath() {
        return "coremods/" + GameUpdater.getUpdater("dummy").getMCVersion() + "/" + name;
    }
}
