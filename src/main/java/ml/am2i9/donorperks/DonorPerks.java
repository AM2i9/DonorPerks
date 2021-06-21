package ml.am2i9.donorperks;

import ml.am2i9.donorperks.particles.Particles;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public class DonorPerks extends JavaPlugin {

    private Logger log = Bukkit.getLogger();

    private File configPath = this.getDataFolder();

    private Particles particlesPerk = new Particles();

    @Override
    public void onEnable() {

        PluginManager pManager = getServer().getPluginManager();

        // Setup Particles;
        particlesPerk.loadParticles(this);
        particlesPerk.loadPlayerParticleConfig(this);
        pManager.registerEvents(particlesPerk, this);
        this.getCommand("particles").setExecutor(particlesPerk);

        this.log.info("Hello World! Donor perks enabled..");
    }

    @Override
    public void onDisable() {
        particlesPerk.savePlayerParticleConfig(this);
        this.log.info("Goodbye!");
    }

    public static DonorPerks getInstance(){
        return (DonorPerks) Bukkit.getServer().getPluginManager().getPlugin("DonorPerks");
    }

}

