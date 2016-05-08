package US.bittiez.PowerHour;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by tadtaylor on 5/7/16.
 */
public class main extends JavaPlugin {
    private static Logger log;
    private FileConfiguration lang;
    private FileConfiguration config = getConfig();
    private String langFile = "lang.yml";

    @Override
    public void onEnable(){
        log = getLogger();

        loadConfig();
        loadLangFile();
    }






    private void loadConfig(){
        config.options().copyDefaults();
        saveDefaultConfig();
    }

    private void saveLangFile(Boolean setDefaults){
        if(setDefaults) {
            lang.set("version", 1);
            lang.set("name", "&6Power Hour");
        }

        try {
            lang.save(new File(this.getDataFolder(), langFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void loadLangFile(){
        File langFile = new File(this.getDataFolder(), this.langFile);
        if(!langFile.exists()) {
            try {
                langFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            lang = new YamlConfiguration();
            saveLangFile(true);
        }
        if(langFile.exists())
            lang = YamlConfiguration.loadConfiguration(langFile);
        else {
            log.warning("Power Hour disabled, unable to read/create the lang.yml file inside the PowerHour folder. (" + this.getDataFolder().getAbsolutePath() + "/lang.yml)");
            getServer().getPluginManager().disablePlugin(this);
        }
    }
}