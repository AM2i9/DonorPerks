package ml.am2i9.donorperks;

import ml.am2i9.donorperks.DonorPerks;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
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
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Hashtable;

public class Particles implements CommandExecutor, Listener {

    private final DonorPerks plugin;

    private File playerConfigFile;
    private FileConfiguration playerParticleConfig;

    private File particleConfigFile;
    private FileConfiguration particleConfig;

    private Inventory menu;
    private NamespacedKey particleIdKey;

    private Hashtable<String, Integer> particleTasks = new Hashtable<String, Integer>();

    public Particles(DonorPerks plugin) {

        this.plugin = plugin;

        this.particleIdKey = new NamespacedKey(this.plugin, "particle_id");

        this.loadParticles(plugin);
        this.loadPlayerParticleConfig(plugin);

        this.initializeMenu();
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 0) {
                if (args[0].equals("reload")) {
                    this.loadParticles(this.plugin);
                    return true;
                }
            }
            player.openInventory(this.menu);
        }
        return true;
    }

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

    public void loadParticles(DonorPerks plugin) {
        this.particleConfigFile = new File(plugin.getDataFolder(), "particles/particles.yml");

        this.particleConfig = new YamlConfiguration();

        try {
            this.particleConfig.load(this.particleConfigFile);
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }

    public void loadPlayerParticleConfig(DonorPerks plugin) {
        this.playerConfigFile = new File(plugin.getDataFolder(), "particles/players.yml");

        this.playerParticleConfig = new YamlConfiguration();

        try {
            this.playerParticleConfig.load(this.playerConfigFile);
        } catch (InvalidConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }

    public void savePlayerParticleConfig(){
        try {
            this.playerParticleConfig.save(this.playerConfigFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getPlayerParticleConfig() {
        return this.playerParticleConfig;
    }

    public FileConfiguration getParticleConfig() {
        return this.particleConfig;
    }

    public void changePlayerParticle(Player player, String particleID) {

        String uuid = player.getUniqueId().toString();

        if (this.particleTasks.containsKey(uuid)){
            Bukkit.getScheduler().cancelTask(this.particleTasks.get(uuid));
        }

        this.getPlayerParticleConfig().set(uuid, particleID);
        this.startParticleTask(player, this.getParticleConfig().getDouble(particleID + ".interval"));
    }

    public void stopPlayerParticle(Player player) {

        String uuid = player.getUniqueId().toString();

        if (this.particleTasks.containsKey(uuid)){
            Bukkit.getScheduler().cancelTask(this.particleTasks.get(uuid));
        }
    }

    public void startParticleTask(Player player) {
        String particleID = this.getPlayerParticleConfig().getString(player.getUniqueId().toString());
        this.startParticleTask(player, this.particleConfig.getDouble(particleID + ".interval"));
    }

    public void startParticleTask(Player player, double interval) {
        int taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin,
                () -> this.showParticles(player), 0L, (long) (interval * 20));
        this.particleTasks.put(player.getUniqueId().toString(), taskID);
    }

    public void showParticles(Player player) {
        String particleID = this.getPlayerParticleConfig().getString(player.getUniqueId().toString());
        FileConfiguration config = getParticleConfig();
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

    public void initializeMenu() {
        this.menu = Bukkit.createInventory(null, 27, "Particles");
        FileConfiguration config = getParticleConfig();
        for (String name : config.getKeys(false)) {
            try {
                this.menu.addItem(createMenuItem(
                        Material.valueOf(config.getString(name + ".item").toUpperCase()),
                        name,
                        config.getString(name + ".display_name"),
                        config.getString(name + ".description")));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        this.menu.setItem(this.menu.getSize(), createMenuItem(
                Material.GRAY_DYE,
                "--PARTICLE_OFF",
                "Turn off particle",
                ""
        ));
    }

    protected ItemStack createMenuItem(final Material material, final String id, final String name, final String... lore) {
        final ItemStack item = new ItemStack(material, 1);
        final ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(name);

        meta.setLore(Arrays.asList(lore));

        meta.getPersistentDataContainer().set(particleIdKey, PersistentDataType.STRING, id);

        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (e.getInventory() != menu) return;

        e.setCancelled(true);

        final ItemStack clickedItem = e.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir()) return;

        final Player player = (Player) e.getWhoClicked();

        PersistentDataContainer container = clickedItem.getItemMeta().getPersistentDataContainer();

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
        if (e.getInventory().equals(menu)) {
            e.setCancelled(true);
        }
    }
}
