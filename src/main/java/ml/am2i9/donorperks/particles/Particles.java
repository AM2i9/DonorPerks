package ml.am2i9.donorperks.particles;

import ml.am2i9.donorperks.DonorPerks;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;

public class Particles implements CommandExecutor, Listener {

    private File playerConfigFile;
    private FileConfiguration playerParticleConfig;

    private File particleConfigFile;
    private FileConfiguration particleConfig;


    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length > 0) {
                if (args[0].equals("reload")) {
                    this.loadParticles(DonorPerks.getInstance());
                } else {
                    this.getPlayerParticleConfig().set(player.getUniqueId().toString(), args[0]);
                }
            }
        }
        return true;
    }

    @EventHandler
    public boolean onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(DonorPerks.getInstance(),
                () -> this.showParticles(player), 0L, 2 * 20);
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

    public void savePlayerParticleConfig(DonorPerks plugin){
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

    public void showParticles(Player player) {
        String particleID = this.getPlayerParticleConfig().getString(player.getUniqueId().toString());
        FileConfiguration config = getParticleConfig();
        try {
            Particle effect = Particle.valueOf(config.getString(particleID + ".particle"));
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
}
