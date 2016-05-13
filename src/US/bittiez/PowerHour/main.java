package US.bittiez.PowerHour;

import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
                    if(player.hasPermission(PERMISSION.addArena))
                        player.sendMessage(powerHourMsg("/PowerHour addArena <arenaName> <regionName> | Add a new arena where you are standing, the regionName is the worldguard region name."));
                    if(player.hasPermission(PERMISSION.delArena))
                        player.sendMessage(powerHourMsg("/PowerHour delArena <arenaName> | Deletes the specified arena from the config"));
                    if(player.hasPermission(PERMISSION.listArenas))
                        player.sendMessage(powerHourMsg("/PowerHour list | Lists all arenas set up"));
                    if(player.hasPermission(PERMISSION.checkArena))
                        player.sendMessage("/PowerHour checkArena | Checks if you are in an arena, and gives you the arena name if you are");


                } else { //There is arguments
                    switch (args[0].toLowerCase()){
                        case "reload":
                            if(player.hasPermission(PERMISSION.reload)) {
                                loadConfig();
                                loadLangFile();
                                player.sendMessage(powerHourMsg("Config reloaded!"));
                            }
                            break;
                        case "addarena":
                            if(player.hasPermission(PERMISSION.addArena)){
                                if(args.length < 3) {
                                    player.sendMessage(powerHourMsg("You did not specify everything needed for the Arena! Proper usage:"));
                                    player.sendMessage(powerHourMsg("/PowerHour addArena arenaName regionName"));
                                } else {
                                    ProtectedRegion region = getRegion(args[2], player.getWorld());

                                    if(region == null){
                                        player.sendMessage(powerHourMsg("That region does not seem to exist, please try again."));
                                        break;
                                    }

                                    Arena newArena = new Arena();
                                    newArena.setName(args[1]);
                                    newArena.setWorld(player.getWorld().getName());
                                    Block loc  = player.getLocation().getBlock();
                                    newArena.setX(loc.getX());
                                    newArena.setY(loc.getY());
                                    newArena.setZ(loc.getZ());
                                    newArena.setRegion(region.getId());
                                    if (saveNewArena(newArena))
                                        player.sendMessage(powerHourMsg("Added the " + newArena.getName() + " arena!"));
                                    else
                                        player.sendMessage(powerHourMsg("Could not save the arena, something went wrong (Make sure there are no other arenas with the same name)."));
                                }
                            }
                            break;
                        case "delarena":
                            if(player.hasPermission(PERMISSION.delArena)){
                                if(args.length < 2){
                                    player.sendMessage(powerHourMsg("You did not specify a name of the arena to delete! Proper usage:"));
                                    player.sendMessage(powerHourMsg("/PowerHour delArena arenaName"));
                                } else {
                                    if(config.contains("arenas." + args[1])){
                                        config.set("arenas." + args[1], null);
                                        saveConfig();
                                        player.sendMessage(powerHourMsg("Deleted the " + args[1] + " arena!"));
                                    } else
                                        player.sendMessage(args[1] + " does not seem to exist!");
                                }
                            }
                            break;
                        case "list":
                            if(player.hasPermission(PERMISSION.listArenas)){
                                Set arenaList = config.getConfigurationSection("arenas").getKeys(false);
                                StringBuilder sb = new StringBuilder();
                                for(Object s : arenaList){
                                    sb.append(s.toString() + " ");
                                }

                                player.sendMessage(powerHourMsg(sb.toString()));
                            }
                            break;
                        case "checkarena":
                            if(player.hasPermission(PERMISSION.checkArena)){
                                List<Arena> arenas = getAllArenas();
                                boolean inRegion = false;
                                for(Arena a : arenas){
                                    ProtectedRegion region = getRegion(a.getRegion(), Bukkit.getWorld(a.getWorld()));
                                    if(region != null){
                                        Location loc = player.getLocation();
                                        if(region.contains(BukkitUtil.toVector(loc))){
                                            player.sendMessage(powerHourMsg("You are in " + a.getName() + " arena!"));
                                            inRegion = true;
                                        }
                                    }
                                }
                                if(!inRegion){
                                    player.sendMessage(powerHourMsg("You are not in any arena!"));
                                }
                            }
                            break;
                    }
                }

            }
            return true;
        }
        return false;
    }

    private ProtectedRegion getRegion(String name, World world){
        RegionContainer container = getWorldGuard().getRegionContainer();
        RegionManager regions = container.get(world);
        ProtectedRegion region;
        if (regions != null) {
            region = regions.getRegion(name);
            if(region != null)
                return region;
        }
        return null;
    }

    private List<Arena> getAllArenas(){
        Set arenaList = config.getConfigurationSection("arenas").getKeys(false);
        List<Arena> arenas = new ArrayList<>();
        for(Object s : arenaList){
            Arena a = getArena(s.toString());
            if(a != null)
                arenas.add(a);
        }
        return arenas;
    }

    private Arena getArena(String name){
        if(config.contains("arenas." + name)){
            Arena arena = new Arena();
            arena.setName(name);
            arena.setRegion(config.getString("arenas." + name + ".region"));
            arena.setWorld(config.getString("arenas." + name + ".world"));
            arena.setZ(config.getInt("arenas." + name + ".locZ"));
            arena.setY(config.getInt("arenas." + name + ".locY"));
            arena.setX(config.getInt("arenas." + name + ".locX"));
            return arena;
        } else return null;
    }

    private WorldGuardPlugin getWorldGuard() {
        Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");

        // WorldGuard may not be loaded
        if (plugin == null || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }

        return (WorldGuardPlugin) plugin;
    }

    private boolean saveNewArena(Arena arena){
        if(!config.contains("arenas." + arena.getName())) {
            config.set("arenas." + arena.getName() + ".locX", arena.getX());
            config.set("arenas." + arena.getName() + ".locY", arena.getY());
            config.set("arenas." + arena.getName() + ".locZ", arena.getZ());
            config.set("arenas." + arena.getName() + ".world", arena.getWorld());
            config.set("arenas." + arena.getName() + ".region", arena.getRegion());
            saveConfig();
        } else
            return false;
        return true;
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