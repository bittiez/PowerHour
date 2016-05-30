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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by tadtaylor on 5/7/16.
 */
public class main extends JavaPlugin implements Listener {
    public boolean powerHour = false;
    public Arena powerHourArena = null;
    public Calendar powerHourEnd = null;

    public ArrayList<Date> powerHours;


    private static Logger log;
    private FileConfiguration lang;
    private FileConfiguration deathLoc;
    private FileConfiguration config = getConfig();
    private String langFile = "lang.yml";
    private String deathFile = "deathLocations.yml";
    private int checkDelay = 60;
    private boolean disabled = false;
    private int currentLangVersion = 5;
    private boolean debug = true;

    @Override
    public void onEnable() {
        log = getLogger();

        loadConfig();
        loadLangFile();
        loadDeathLoc();

        this.powerHours = new ArrayList<>();
        getTimesFromConfig();

        if (getAllArenas().size() < 1) {
            disabled = true;
            log.warning("PowerHour did not detect any arenas, please set up at least one!");
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public void run() {
                checkPowerHour();
            }
        }, checkDelay * 20, checkDelay * 20);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
    }

    private void getTimesFromConfig() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");


        List<String> powerHours = config.getStringList("powerHours");
        log.info(powerHours.toString());
        for (String s : powerHours) {
            try {
                Date date = simpleDateFormat.parse(s);
                this.powerHours.add(date);
            } catch (ParseException ex) {
                log.warning("Could not parse the following time: " + s);
            }
        }
    }

    private String replaceTag(String string, String tag, String replacement) {
        if (tag == null || replacement == null || string == null)
            return string;
        return string.replaceAll("(\\[" + tag + "\\])", replacement);
    }

    private String replaceTag(String string, String[] tag, String[] replacement) {
        if (tag.length != replacement.length)
            return string;
        else {
            for (int i = 0; i < tag.length; i++) {
                string = replaceTag(string, tag[i], replacement[i]);
            }
            return string;
        }
    }

    private void onPowerHourEnd(){
        deathLoc = new YamlConfiguration();
        saveDeathFile();
    }

    private void startPowerHour(Calendar cal) {
        if (!powerHour) {
            cal.add(Calendar.MINUTE, config.getInt("length"));
            powerHourEnd = cal;
            log.info("End: " + cal.get(Calendar.HOUR_OF_DAY) + ":" + cal.get(Calendar.MINUTE));
            log.info("Power hour beginning!");

            List<Arena> arenas = getAllArenas();

            if (arenas.size() > 1) {
                Random r = new Random();
                int a = r.nextInt(arenas.size());
                powerHourArena = arenas.get(a);
            } else {
                powerHourArena = arenas.get(0);
            }
            powerHour = true;

            getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', replaceTag(lang.getString("powerHourStart"), "arena", powerHourArena.getName())));
        }
    }

    private void checkPowerHour() {
        if (disabled)
            return;
        long hour, minute;

        Calendar now = Calendar.getInstance();

        hour = now.get(Calendar.HOUR_OF_DAY);
        minute = now.get(Calendar.MINUTE);

        if (!powerHour) {
            Calendar cal = Calendar.getInstance();
            for (Date t : this.powerHours) {
                cal.setTime(t);
                int hr = cal.get(Calendar.HOUR_OF_DAY);
                int mn = cal.get(Calendar.MINUTE);
                Calendar cal2 = cal;
                cal2.add(Calendar.MINUTE, config.getInt("length"));
                int hrEnd = cal2.get(Calendar.HOUR_OF_DAY);
                int mnEnd = cal2.get(Calendar.MINUTE);

                if ((hour >= hr && minute >= mn) && (hour <= hrEnd && minute <= mnEnd)) {
                    //Start power hour!
                    log.info("Start: " + hr + ":" + mn);
                    startPowerHour(Calendar.getInstance());
                    return;
                }
            }
        } else {
            //Check if power hour ending
            if (powerHourEnd != null) {
                int hr = powerHourEnd.get(Calendar.HOUR_OF_DAY);
                int mn = powerHourEnd.get(Calendar.MINUTE);

                if (hour >= hr && minute >= mn) {
                    if(hour == 23 && hr == 0)
                        return;
                    getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', replaceTag(lang.getString("powerHourEnd"), "arena", powerHourArena.getName())));
                    powerHourEnd = null;
                    powerHourArena = null;
                    log.info("Power hour ending!");
                    powerHour = false;
                    onPowerHourEnd();
                }
            } else
                powerHour = false;

        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (disabled)
            return;
        if (powerHour && powerHourArena != null) {
            Player who = event.getPlayer();
            if (who.hasPermission(PERMISSION.onJoinMessage)) {
                if (lang.contains("playerLogin"))
                    who.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            replaceTag(lang.getString("playerLogin"),
                                    new String[]{"player", "arena"},
                                    new String[]{who.getDisplayName(), powerHourArena.getName()})
                    ));
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (disabled)
            return;
        if (powerHour && powerHourArena != null) {
            Player who = e.getEntity();
            ProtectedRegion region = getRegion(powerHourArena.getRegion(), who.getWorld());
            if (region != null) {
                if (region.contains(BukkitUtil.toVector(who.getLocation()))) {
                    e.setKeepInventory(true);
                    e.setKeepLevel(true);
                    Player killer = who.getKiller();
                    if (killer != null) {
                        e.setDeathMessage(ChatColor.translateAlternateColorCodes('&', replaceTag(
                                lang.getString("playerDeathByPlayer"),
                                new String[]{"player",            "killer",                 "arena"},
                                new String[]{who.getDisplayName(), killer.getDisplayName(), powerHourArena.getName()}
                        )));
                        killer.giveExp(config.getInt("killExp"));
                        killer.sendMessage(ChatColor.translateAlternateColorCodes('&', replaceTag(
                                lang.getString("expReward"),
                                        new String[]{"killer",                "player",             "exp"},
                                        new String[]{killer.getDisplayName(), who.getDisplayName(), config.getString("killExp")}
                        )));
                        //Add message here!
                    } else {
                        e.setDeathMessage(ChatColor.translateAlternateColorCodes('&', replaceTag(
                                lang.getString("playerDeath"),
                                new String[]{"player",             "arena"},
                                new String[]{who.getDisplayName(), powerHourArena.getName()}
                        )));
                    }
                    deathLoc.set(who.getName() + ".world", who.getLocation().getWorld().getName());
                    deathLoc.set(who.getName() + ".x", who.getLocation().getX());
                    deathLoc.set(who.getName() + ".y", who.getLocation().getY());
                    deathLoc.set(who.getName() + ".z", who.getLocation().getZ());
                    saveDeathFile();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(final PlayerRespawnEvent event) {
        if (disabled)
            return;
        if (powerHour && powerHourArena != null) {
            if(debug)
                log.info("Player Respawned during PowerHour");
            Player who = event.getPlayer();
            if(deathLoc.contains(who.getName())) {
                if(debug)
                    log.info(who.getName() + " has a death location saved..");

                ProtectedRegion region = getRegion(powerHourArena.getRegion(), Bukkit.getWorld(powerHourArena.getWorld()));
                if (region != null) {
                    Location dlocation = new Location(
                            Bukkit.getWorld(deathLoc.getString(who.getName() + ".world")),
                            deathLoc.getInt(who.getName() + ".x"),
                            deathLoc.getInt(who.getName() + ".y"),
                            deathLoc.getInt(who.getName() + ".z")
                    );
                    if (region.contains(BukkitUtil.toVector(dlocation))) {
                        if(debug)
                            log.info("Player died in the arena! respawning!");
                        event.setRespawnLocation(new Location(Bukkit.getWorld(powerHourArena.getWorld()), powerHourArena.getX(), powerHourArena.getY(), powerHourArena.getZ()));
                    }
                    deathLoc.set(who.getName(), null);
                    saveDeathFile();
                }
            }
        }
    }

    private void reloadPlugin() {
        loadConfig();
        loadLangFile();
        this.powerHours = new ArrayList<>();
        getTimesFromConfig();
    }

    public boolean onCommand(CommandSender who, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("powerhour")) {
            if (who instanceof Player) {
                Player player = (Player) who;

                if (args.length < 1) { //Did not specify any arguments
                    player.sendMessage(powerHourMsg("----- Available commands: -----"));

                    if (player.hasPermission(PERMISSION.reload))
                        player.sendMessage(powerHourMsg("/PowerHour reload | Reloads config files."));
                    if (player.hasPermission(PERMISSION.addArena))
                        player.sendMessage(powerHourMsg("/PowerHour addArena <arenaName> <regionName> | Add a new arena where you are standing, the regionName is the worldguard region name."));
                    if (player.hasPermission(PERMISSION.delArena))
                        player.sendMessage(powerHourMsg("/PowerHour delArena <arenaName> | Deletes the specified arena from the config"));
                    if (player.hasPermission(PERMISSION.listArenas))
                        player.sendMessage(powerHourMsg("/PowerHour list | Lists all arenas set up"));
                    if (player.hasPermission(PERMISSION.checkArena))
                        player.sendMessage(powerHourMsg("/PowerHour checkArena | Checks if you are in an arena, and gives you the arena name if you are"));
                    if (player.hasPermission(PERMISSION.startPowerHour))
                        player.sendMessage(powerHourMsg("/PowerHour startPowerHour | Forces a PowerHour to start"));
                    if(player.hasPermission(PERMISSION.cancelPowerHour))
                        player.sendMessage(powerHourMsg("/PowerHour cancel | Cancel the current power hour"));


                } else { //There is arguments
                    switch (args[0].toLowerCase()) {
                        case "reload":
                            if (player.hasPermission(PERMISSION.reload)) {
                                reloadPlugin();
                                player.sendMessage(powerHourMsg("Config reloaded!"));
                            }
                            break;
                        case "addarena":
                            if (player.hasPermission(PERMISSION.addArena)) {
                                if (args.length < 3) {
                                    player.sendMessage(powerHourMsg("You did not specify everything needed for the Arena! Proper usage:"));
                                    player.sendMessage(powerHourMsg("/PowerHour addArena arenaName regionName"));
                                } else {
                                    ProtectedRegion region = getRegion(args[2], player.getWorld());

                                    if (region == null) {
                                        player.sendMessage(powerHourMsg("That region does not seem to exist, please try again."));
                                        break;
                                    }

                                    Arena newArena = new Arena();
                                    newArena.setName(args[1]);
                                    newArena.setWorld(player.getWorld().getName());
                                    Block loc = player.getLocation().getBlock();
                                    newArena.setX(loc.getX());
                                    newArena.setY(loc.getY());
                                    newArena.setZ(loc.getZ());
                                    newArena.setRegion(region.getId());
                                    if (saveNewArena(newArena)) {
                                        player.sendMessage(powerHourMsg("Added the " + newArena.getName() + " arena!"));
                                        if (disabled) {
                                            disabled = false;
                                            log.info("First arena added, re-enabling PowerHour!");
                                        }
                                    } else
                                        player.sendMessage(powerHourMsg("Could not save the arena, something went wrong (Make sure there are no other arenas with the same name)."));
                                }
                            }
                            break;
                        case "delarena":
                            if (player.hasPermission(PERMISSION.delArena)) {
                                if (args.length < 2) {
                                    player.sendMessage(powerHourMsg("You did not specify a name of the arena to delete! Proper usage:"));
                                    player.sendMessage(powerHourMsg("/PowerHour delArena arenaName"));
                                } else {
                                    if (config.contains("arenas." + args[1])) {
                                        config.set("arenas." + args[1], null);
                                        saveConfig();
                                        player.sendMessage(powerHourMsg("Deleted the " + args[1] + " arena!"));
                                        if (getAllArenas().size() < 1) {
                                            disabled = true;
                                            player.sendMessage(powerHourMsg("There are no arenas configured, PowerHour will be disabled until an arena is added."));
                                        }
                                    } else
                                        player.sendMessage(args[1] + " does not seem to exist!");
                                }
                            }
                            break;
                        case "list":
                            if (player.hasPermission(PERMISSION.listArenas)) {
                                List<Arena> arenaList = getAllArenas();
                                StringBuilder sb = new StringBuilder();
                                for (Arena s : arenaList) {
                                    sb.append(s.getName() + ", ");
                                }

                                player.sendMessage(powerHourMsg(sb.toString()));
                            }
                            break;
                        case "checkarena":
                            if (player.hasPermission(PERMISSION.checkArena)) {
                                List<Arena> arenas = getAllArenas();
                                boolean inRegion = false;
                                for (Arena a : arenas) {
                                    ProtectedRegion region = getRegion(a.getRegion(), Bukkit.getWorld(a.getWorld()));
                                    if (region != null) {
                                        Location loc = player.getLocation();
                                        if (region.contains(BukkitUtil.toVector(loc))) {
                                            player.sendMessage(powerHourMsg("You are in " + a.getName() + " arena!"));
                                            inRegion = true;
                                        }
                                    }
                                }
                                if (!inRegion) {
                                    player.sendMessage(powerHourMsg("You are not in any arena!"));
                                }
                            }
                            break;
                        case "startpowerhour":
                            if (player.hasPermission(PERMISSION.startPowerHour)) {
                                player.sendMessage(powerHourMsg("Attempting to start a PowerHour. (This will not work if a PowerHour is already running)"));
                                startPowerHour(Calendar.getInstance());
                            }
                            break;
                        case "cancel":
                            if(player.hasPermission(PERMISSION.cancelPowerHour)){
                                powerHourArena = null;
                                powerHour = false;
                                onPowerHourEnd();
                                player.sendMessage(powerHourMsg("PowerHour has been canceled if it was running."));
                            }
                            break;
                    }
                }

            } else {
                /*
                    Console Sender
                 */
                switch (args[0].toLowerCase()) {
                    case "reload":
                        reloadPlugin();
                        who.sendMessage("Config reloaded!");
                        break;
                    case "startpowerhour":
                        who.sendMessage("Attempting to start a PowerHour(This will not work if a PowerHour is already running)");
                        startPowerHour(Calendar.getInstance());
                        break;
                    case "cancel":
                        powerHourArena = null;
                        powerHour = false;
                        onPowerHourEnd();
                        who.sendMessage("PowerHour has been canceled if it was running.");
                        break;
                }
            }
            return true;
        }
        return false;
    }

    private ProtectedRegion getRegion(String name, World world) {
        RegionContainer container = getWorldGuard().getRegionContainer();
        RegionManager regions = container.get(world);
        ProtectedRegion region;
        if (regions != null) {
            region = regions.getRegion(name);
            if (region != null)
                return region;
        }
        return null;
    }

    private List<Arena> getAllArenas() {
        if (config.contains("arenas")) {
            Set arenaList = config.getConfigurationSection("arenas").getKeys(false);
            List<Arena> arenas = new ArrayList<>();
            for (Object s : arenaList) {
                Arena a = getArena(s.toString());
                if (a != null)
                    arenas.add(a);
            }
            return arenas;
        } else return new ArrayList<>();
    }

    private Arena getArena(String name) {
        if (config.contains("arenas." + name)) {
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

    private boolean saveNewArena(Arena arena) {
        if (!config.contains("arenas." + arena.getName())) {
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

    private String powerHourMsg(String msg) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        return ChatColor.DARK_AQUA + "[Power Hour] " + ChatColor.AQUA + msg;
    }

    private void loadConfig() {
        config.options().copyDefaults();
        saveDefaultConfig();
    }

    private void saveMainConfig() {
        this.saveConfig();
    }

    private void saveLangFile(Boolean setDefaults) {
        if (setDefaults) {
            lang.set("version", currentLangVersion);
            lang.set("name", "&6Power Hour");
            lang.set("powerHourStart", "&6PowerHour is beginning in the &5[arena] &6arena!");
            lang.set("powerHourEnd", "&6PowerHour is ending for the &5[arena] &6arena!");
            lang.set("playerDeath", "&5[player] has died during PowerHour in the &5[arena] &6arena!");
            lang.set("playerDeathByPlayer", "&5[killer] &6killed &5[player] &6during PowerHour in the &5[arena] &6arena!");
            lang.set("playerLogin", "&6Hey &5[player]&6! PowerHour is currently active at the &5[arena] &6arena!");
            lang.set("expReward", "&6Hey &5[killer] you were awarded &5[exp] &6exp for killing &5[player]&6!");
        }

        try {
            lang.save(new File(this.getDataFolder(), langFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLangFile() {
        File langFile = new File(this.getDataFolder(), this.langFile);
        if (!langFile.exists()) {
            try {
                langFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            lang = new YamlConfiguration();
            saveLangFile(true);
        }
        if (langFile.exists()) {
            lang = YamlConfiguration.loadConfiguration(langFile);
            if (lang.getInt("version") < currentLangVersion) {
                try {
                    lang.save(new File(this.getDataFolder(), this.langFile + ".old"));
                    lang = new YamlConfiguration();
                    saveLangFile(true);
                    log.warning("You had an old " + this.langFile + " file, renamed it to " + this.langFile + ".old and saved a new version. Please copy over any modified strings to the new file.");
                } catch (Exception e) {
                    log.warning("Power Hour disabled, unable to read/write lang.yml file inside the PowerHour folder. (" + this.getDataFolder().getAbsolutePath() + "/" + this.langFile + ".old)");
                    getServer().getPluginManager().disablePlugin(this);
                }
            }
        } else {
            log.warning("Power Hour disabled, unable to read/create the lang.yml file inside the PowerHour folder. (" + this.getDataFolder().getAbsolutePath() + "/" + this.langFile + ")");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void saveDeathFile(){
        try {
            deathLoc.save(new File(this.getDataFolder(), deathFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadDeathLoc() {
        File deathLocFile = new File(this.getDataFolder(), this.deathFile);
        if (!deathLocFile.exists()) {
            try {
                deathLocFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            deathLoc = new YamlConfiguration();
        }
        if (deathLocFile.exists()) {
            deathLoc = YamlConfiguration.loadConfiguration(deathLocFile);
        } else {
            log.warning("Power Hour disabled, unable to read/create the "+deathFile+" file inside the PowerHour folder. (" + this.getDataFolder().getAbsolutePath() + "/" + deathFile + ")");
            getServer().getPluginManager().disablePlugin(this);
        }
    }
}