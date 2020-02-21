package in.pinig.ttvrewards;

import com.sun.istack.internal.NotNull;
import com.sun.org.apache.xerces.internal.xs.StringList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;


import java.util.Collection;
import java.util.List;

public class RewardsHandler extends BukkitRunnable {
    String username;
    String channel;
    String rewardId;
    String message;

    RewardsHandler(@NotNull String channel, @NotNull String username, @NotNull String rewardId, String message) {
        this.username = username;
        this.channel = channel;
        this.rewardId = rewardId;
        this.message = message;
    }

    public void run() {
        Player player = null;
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        for (Player x: players) {
            String chan = Main.channels.get(x.getName());
            if(channel == null) continue;
            if(channel.equals(chan)) player = x;
        }
        if(player == null) return;

        String rewardName = Main.config.getString("rewards." + rewardId + ".name", null);

        if (rewardName != null) {
            String action = Main.config.getString("rewards." + rewardId + ".action", null);
            if(action == null) {
                player.sendMessage(Main.config.getString("strings.prefix") + Main.config.getString("strings.err_missaction").replace("{reward_name}", rewardName));
                return;
            }

            switch(action) {
                case "effect":
                    String effectId = Main.config.getString("rewards." + rewardId + ".effect.id", null);
                    int duration = Main.config.getInt("rewards." + rewardId + ".effect.duration", 0);
                    int amplifier = Main.config.getInt("rewards." + rewardId + ".effect.amplifier", 0);
                    if(effectId == null || duration == 0 || amplifier == 0) {
                        player.sendMessage(Main.config.getString("strings.prefix") + Main.config.getString("strings.err_missarg").replace("{reward_name}", rewardName));
                        return;
                    }

                    PotionEffectType effectType = PotionEffectType.getByName(effectId.toUpperCase());
                    if(effectType == null) {
                        player.sendMessage(Main.config.getString("strings.prefix") + Main.config.getString("strings.err_wrongeffect").replace("{effect_name}", effectId));
                        return;
                    }

                    player.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
                    break;
                case "kill":
                    player.setHealth(0.0);
                    break;
                case "spawn":
                    String mobName = Main.config.getString("rewards." + rewardId + ".spawn.mob", null);
                    if(mobName == null) {
                        player.sendMessage(Main.config.getString("strings.prefix") + Main.config.getString("strings.err_missarg").replace("{reward_name}", rewardName));
                        return;
                    }

                    try {
                        EntityType entityType = EntityType.valueOf(mobName.toUpperCase());

                        Location playerLoc = player.getLocation();
                        playerLoc.setY(playerLoc.getY()+1.0);
                        playerLoc.setZ(playerLoc.getZ()+2.0);
                        player.getWorld().spawnEntity(playerLoc, entityType);
                    } catch(IllegalArgumentException ex) {
                        player.sendMessage(Main.config.getString("strings.prefix") + Main.config.getString("strings.err_unknownmob").replace("{mob_name}", mobName));
                        ex.printStackTrace();
                    }
                    break;
                case "jump":
                    int min = Main.config.getInt("rewards." + rewardId + ".jump.min", -1);
                    int max = Main.config.getInt("rewards." + rewardId + ".jump.max", -1);
                    if(min == -1 || max == -1) {
                        player.sendMessage(Main.config.getString("strings.prefix") + Main.config.getString("strings.err_missarg").replace("{reward_name}", rewardName));
                        return;
                    }

                    Location location = player.getLocation();
                    int randomed = Utils.getRandomInt(min, max);

                    location.setY(location.getY()+randomed);
                    player.teleport(location);
                    break;
                case "tospawn":
                    player.teleport(player.getWorld().getSpawnLocation());
                    break;
                case "teleport":
                    String dest = Main.config.getString("rewards." + rewardId + ".teleport.destination", null);
                    if(dest == null) {
                        player.sendMessage(Main.config.getString("strings.prefix") + Main.config.getString("strings.err_missarg").replace("{reward_name}", rewardName));
                        return;
                    }
                    if(dest.equals("random")) {
                        int minX = Main.config.getInt("rewards." + rewardId + ".teleport.random.minX", -1000);
                        int minZ = Main.config.getInt("rewards." + rewardId + ".teleport.random.minZ", 1000);
                        int maxX = Main.config.getInt("rewards." + rewardId + ".teleport.random.maxX", -1000);
                        int maxZ = Main.config.getInt("rewards." + rewardId + ".teleport.random.maxZ", 1000);
                        int x = Utils.getRandomInt(minX, maxX);
                        int z = Utils.getRandomInt(minZ, maxZ);
                        int y = player.getWorld().getHighestBlockYAt(x, z)+1;
                        Location loc = new Location(player.getWorld(), x, y, z);
                        player.teleport(loc);
                    } else if(dest.equals("list")) {
                        List<String> locations = Main.config.getStringList("rewards." + rewardId + ".teleport.locations");
                        String locStr = locations.get(Utils.getRandomInt(0, locations.size()));
                        String[] locStrSplitted = locStr.split(",");
                        World world = Bukkit.getWorld(locStrSplitted[0]);
                        if(world == null) {
                            player.sendMessage(Main.config.getString("strings.prefix") + Main.config.getString("strings.err_wrongworld").replace("{world_name}", locStrSplitted[0]));
                            return;
                        }

                        try {
                            Location loc = new Location(world, Integer.parseInt(locStrSplitted[1]), Integer.parseInt(locStrSplitted[2]), Integer.parseInt(locStrSplitted[3]));
                            player.teleport(loc);
                        } catch (NumberFormatException ex) {
                            player.sendMessage(Main.config.getString("strings.prefix") + Main.config.getString("strings.err_missarg").replace("{reward_name}", rewardName));
                            ex.printStackTrace();
                        }
                    }
                    break;
                default:
                    System.err.println("Unknown action \"" + action + "\"");
                    return;
            }

            player.sendMessage(Main.config.getString("strings.prefix") + Main.config.getString("strings.receive_reward").replace("{name}", username).replace("{reward_name}", rewardName));
            if(message != null)
                player.sendMessage(Main.config.getString("strings.prefix") + Main.config.getString("strings.receive_message").replace("{message}", message));
        }
    }
}
