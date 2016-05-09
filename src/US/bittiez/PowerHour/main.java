package US.bittiez.PowerHour;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by tadtaylor on 5/7/16.
 */
public class main extends JavaPlugin implements Listener{
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

    public boolean onCommand(CommandSender who, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("powerhour")) {
            if (who instanceof Player) {
                Player player = (Player)who;

                if(args.length < 1){ //Did not specify any arguments
                    player.sendMessage(powerHourMsg("----- Available commands: -----"));

                    if(player.hasPermission(PERMISSION.reload))
                        player.sendMessage(powerHourMsg("/PowerHour reload | Reloads config files."));
                } else { //There is arguments
                    switch (args[0]){
                        case "reload":
                            if(player.hasPermission(PERMISSION.reload)) {
                                loadConfig();
                                loadLangFile();
                                player.sendMessage(powerHourMsg("Config reloaded!"));
                            }
                            break;
                    }
                }

            }
            return true;
        }
        return false;
    }


    private String powerHourMsg(String msg){
        return ChatColor.DARK_AQUA + "[Power Hour] " + ChatColor.AQUA + msg;
    }

    private void loadConfig(){
        config.options().copyDefaults();
        saveDefaultConfig();
    }

    private void saveMainConfig(){
        this.saveConfig();
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