package net.minecraft;

import java.util.ArrayList;

public class MinecraftLibrary {
    public String name;
    public ArrayList<Rule> rules;
    public String url;
    public String formatted;
    public Natives natives;
    
    public boolean allow() {
        boolean flag = false;
        if ((this.rules == null) || (this.rules.isEmpty())) {
            flag = true;
        } else {
            for (int j = 0; j < this.rules.size(); j++) {
                Rule r = (Rule)this.rules.get(j);
                if (r.action.equals("disallow")) {
                    if (r.os != null && (r.os.name() == null || r.os.name().trim().equals("") || r.os.name().toLowerCase().equals(Util.getPlatform().toString()))) {
                        flag = false;
                        break;
                    }
                }
                else {
                    if (r.os != null && (r.os.name() == null || r.os.name().trim().equals("") || r.os.name().toLowerCase().equals(Util.getPlatform().toString()))) {
                        flag = true;
                    }
                    else if (r.os == null) {
                        flag = true;
                    }
                }
            }
        }
        return flag;
    }
}
