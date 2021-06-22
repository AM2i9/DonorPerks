package ml.am2i9.donorperks;

import org.bukkit.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

public class Particles implements CommandExecutor, Listener {

    private final DonorPerks plugin;

    // Config files
    private File playerConfigFile;
    private FileConfiguration playerParticleConfig;

    private File particleConfigFile;
    private FileConfiguration particleConfig;

    // Menu stuff
    private Inventory menu;
    private NamespacedKey particleIdKey;

    // A list of currently active particle tags
    private Hashtable<String, Integer> particleTasks = new Hashtable<String, Integer>();

    public Particles(DonorPerks plugin) {

        this.plugin = plugin;

        // Create particle_id tag for menu items
        this.particleIdKey = new NamespacedKey(this.plugin, "particle_id");

        // Load configs
        this.loadParticles(plugin);
        this.loadPlayerParticleConfig(plugin);

        // Create menu
        this.initializeMenu();
    }

    // Particle command
    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 0) {
                if (args[0].equals("reload") && player.isOp()) {
                    this.loadParticles(this.plugin);
                    this.initializeMenu();
                    return true;
                }
            }
            player.openInventory(this.menu);
        }
        return true;
    }

    // Event handlers
    @EventHandler
    public boolean onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        startParticleTask(player);
        return true;
    }

    @EventHandler
    public boolean onPlayerQuit(PlayerQuitEvent e) {
        this.particleTasks.remove(e.getPlayer().getUniqueId().toString());
        return true;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {

        if (e.getInventory() != menu) return;

        e.setCancelled(true);

        final ItemStack clickedItem = e.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir()) return;

        final Player player = (Player) e.getWhoClicked();

        PersistentDataContainer container = clickedItem.getItemMeta().getPersistentDataContainer();

        // Test for the `particle_id` tag on the clicked item
        if(container.has(particleIdKey , PersistentDataType.STRING)) {

            String particleID = container.get(particleIdKey, PersistentDataType.STRING);

            if (particleID.equals("--PARTICLE_OFF")) {
                this.stopPlayerParticle(player);
            } else {
                this.changePlayerParticle(player, particleID);
            }
            player.closeInventory();

        }

    }

    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {

        // Canceling dragging in the menu
        if (e.getInventory().equals(menu)) {
            e.setCancelled(true);
        }

    }

    // Load configs and stored data
    public void loadParticles(DonorPerks plugin) {

        this.particleConfigFile = new File(plugin.getDataFolder(), "particles/particles.yml");

        // Create directory and file if not exists
        if (!this.particleConfigFile.exists()) {
            try {
                this.particleConfigFile.getParentFile().mkdirs();
                this.particleConfigFile.createNewFile();
            } catch ( IOException e ) {}
        }

        this.particleConfig = new YamlConfiguration();

        try {
            this.particleConfig.load(this.particleConfigFile);
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }

    }

    public void loadPlayerParticleConfig(DonorPerks plugin) {

        this.playerConfigFile = new File(plugin.getDataFolder(), "particles/players.yml");

        // Create directory and file if not exists
        if (!this.playerConfigFile.exists()) {
            try {
                this.playerConfigFile.getParentFile().mkdirs();
                this.playerConfigFile.createNewFile();
            } catch ( IOException e ) {}
        }

        this.playerParticleConfig = new YamlConfiguration();

        try {
            this.playerParticleConfig.load(this.playerConfigFile);
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }

    // Save player config
    public void savePlayerParticleConfig(){

        try {
            this.playerParticleConfig.save(this.playerConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getters for player and particle data
    public FileConfiguration getPlayerParticleConfig() {
        return this.playerParticleConfig;
    }

    public FileConfiguration getParticleConfig() {
        return this.particleConfig;
    }

    // Change the players particles
    public void changePlayerParticle(Player player, String particleID) {

        String uuid = player.getUniqueId().toString();

        // Stopping and starting the task with the new particle and interval
        if (this.particleTasks.containsKey(uuid)){
            Bukkit.getScheduler().cancelTask(this.particleTasks.get(uuid));
        }

        this.getPlayerParticleConfig().set(uuid, particleID);
        this.startParticleTask(player);
        this.savePlayerParticleConfig();
    }

    public void stopPlayerParticle(Player player) {

        String uuid = player.getUniqueId().toString();

        if (this.particleTasks.containsKey(uuid)){
            Bukkit.getScheduler().cancelTask(this.particleTasks.get(uuid));
        }
    }

    // Start the bukkit task loop to spawn the particle on the player
    public void startParticleTask(Player player) {
        // Get the interval of the players current particle
        String particleID = this.getPlayerParticleConfig().getString(player.getUniqueId().toString());
        double interval = this.particleConfig.getDouble(particleID + ".interval");

        // Start the bukkit task
        int taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin,
                () -> this.showParticles(player), 0L, (long) (interval * 20));

        // Save the taskID to the `particleTasks` hashtable, so we can access it later
        this.particleTasks.put(player.getUniqueId().toString(), taskID);
    }

    // The bukkit task that spawns the particle
    public void showParticles(Player player) {

        // Read info about the particle
        String particleID = this.getPlayerParticleConfig().getString(player.getUniqueId().toString());
        FileConfiguration config = getParticleConfig();

        // Spawn the particle
        try {
            Particle effect = Particle.valueOf(config.getString(particleID + ".particle").toUpperCase());
            player.spawnParticle(effect, player.getLocation().add(0, .7, 0),
                    config.getInt(particleID + ".count"),
                    config.getInt(particleID + ".offsetX"),
                    config.getInt(particleID + ".offsetY"),
                    config.getInt(particleID + ".offsetZ"),
                    config.getInt(particleID + ".speed"));

        } catch (NullPointerException | IllegalArgumentException e) {
            return;
        }
    }

    // Create particle menu
    public void initializeMenu() {

        // Create a single chest inventory
        this.menu = Bukkit.createInventory(null, 27, "Particles");
        FileConfiguration config = getParticleConfig();

        for (String name : config.getKeys(false)) {
            try {

                this.menu.addItem(createMenuItem(
                        Material.valueOf(config.getString(name + ".item").toUpperCase()),
                        name,
                        config.getString(name + ".display_name"),
                        ChatColor.WHITE,
                        config.getString(name + ".description")));

            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        // Create the `turn off particles` item in last slot of the menu
        this.menu.setItem(this.menu.getSize() - 1, createMenuItem(
                Material.GRAY_DYE,
                "--PARTICLE_OFF",
                "Turn off particles",
                ChatColor.RED,
                ""
        ));
    }

    // Nice little function to create a menu option
    protected ItemStack createMenuItem(final Material material, final String id, final String name, final ChatColor color, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(color + name);

        meta.setLore(Arrays.asList(lore));

        // Add the particle id to the items nbt, so we can use it to know which particle we need to set
        meta.getPersistentDataContainer().set(particleIdKey, PersistentDataType.STRING, id);

        item.setItemMeta(meta);

        return item;
    }

}
