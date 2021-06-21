package ml.am2i9.donorperks.particles;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class CustomParticle {

    public String name;
    public ItemStack displayItem;
    public Particle particle;
    public int count;
    public int speed;

    public void CustomParticle(String name, String particle, String itemName, int count, int speed){
        this.name = name;
        this.count = count;
        this.speed = speed;

        this.particle = Particle.valueOf(particle);

        try {
            ItemStack item = new ItemStack(Material.valueOf(itemName.toUpperCase()));
            this.displayItem = item;
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("Particle '" + name + "' could not be loaded because of an invalid item name. It will not be shown in the selection menu.");
        }

    }
}
