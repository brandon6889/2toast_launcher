package net.minecraft;

public class MinecraftCoreMod extends MinecraftMod {
    public MinecraftCoreMod(String name, String ver) {
        super(name, ver);
    }
    
    @Override
    public String getPath() {
        return "coremods/" + mVer + "/" + mName;
    }
}
