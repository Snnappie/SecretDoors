package com.github.snnappie.secretdoors;

import com.github.snnappie.secretdoors.listeners.BlockListener;
import com.github.snnappie.secretdoors.listeners.PlayerListener;
import com.github.snnappie.secretdoors.listeners.PowerListener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public class SecretDoors extends JavaPlugin {
	private HashMap<Block, SecretDoor> doors = new HashMap<>();
	private HashMap<Block, SecretTrapdoor> trapdoors = new HashMap<>();

    private HashMap<SecretDoor, CloseDoorTask> doorTasks = new HashMap<>();

    public int closeTime;

    // TODO: Still not sure if I'm happy about this re-refactoring
    /**
     * Permission strings
     */
    public static final String PERMISSION_SD_USE    = "secretdoors.use";

    /**
     * Config strings
     */
    public static final String CONFIG_PERMISSIONS_ENABLED   = "use-permissions";
    public static final String CONFIG_ENABLE_TIMERS         = "enable-timers";
    public static final String CONFIG_ENABLE_REDSTONE       = "enable-redstone";
    public static final String CONFIG_ENABLE_TRAPDOORS      = "enable-trapdoors";

    public static final String CONFIG_CLOSE_TIME            = "close-time-seconds";

	public void onDisable() {
		for (Block door : this.doors.keySet()) {
			this.doors.get(door).close();
		}
		
		for (Block ladder : trapdoors.keySet()) {
			trapdoors.get(ladder).close();
		}
	}

	public void onEnable() {

        // listeners
		getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
		getServer().getPluginManager().registerEvents(new PowerListener(this), this);
		getServer().getPluginManager().registerEvents(new BlockListener(this), this);

        // config
        getConfig().options().copyDefaults(true);
		saveConfig();
        this.closeTime = getConfig().getInt(CONFIG_CLOSE_TIME);
	}

	// handles commands
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length != 1)
			return false;

		if (cmd.getName().equalsIgnoreCase("secretdoors")) {
			if (args[0].equalsIgnoreCase("reload")) {
				if (getConfig().getBoolean(CONFIG_PERMISSIONS_ENABLED)) {
					if (!sender.hasPermission("secretdoors.reload")) {
						return false;
					}
				}
				reloadConfig();
                this.closeTime = getConfig().getInt(CONFIG_CLOSE_TIME);
				sender.sendMessage(ChatColor.RED + "Secret Doors config reloaded");
				return true;
			}
		}
		return false;
	}

	public SecretDoor addDoor(SecretDoor door) {
		this.doors.put(door.getKey(), door);

        // add a task to close the door after the time
        if (getConfig().getBoolean(CONFIG_ENABLE_TIMERS)) {
            CloseDoorTask task =  new CloseDoorTask(this, door);
            task.runTaskLater(this, 20 * this.closeTime);
            doorTasks.put(door, task);
        }

		return door;
	}

	
	public boolean isSecretDoor(Block door) {
		return this.doors.containsKey(door);
	}

	public void closeDoor(Block door) {
		if (isSecretDoor(door)) {

            SecretDoor secretDoor = this.doors.remove(door);
            secretDoor.close();
            // remove and cancel the auto-task if the user manually closed the door
            if (getConfig().getBoolean(CONFIG_ENABLE_TIMERS))
                doorTasks.remove(secretDoor).cancel();
		}
	}

    public void closeDoorAuto(Block door) {
        if (isSecretDoor(door)) {

            SecretDoor secretDoor = this.doors.remove(door);
            secretDoor.autoClose();
            doorTasks.remove(secretDoor);
        }
    }
	
	
	
	public void addTrapdoor(SecretTrapdoor door) {
		if (door.getKey().getType() == Material.LADDER) {
			trapdoors.put(door.getKey(), door);
		}
	}
	
	public void closeTrapdoor(Block ladder) {
		if (ladder.getType() == Material.LADDER) {
			if (isSecretTrapdoor(ladder)) {
				trapdoors.get(ladder).close();
				trapdoors.remove(ladder);
			}
		}
	}
	
	public boolean isSecretTrapdoor(Block ladder) {
        return ladder.getType() == Material.LADDER && trapdoors.containsKey(ladder);
    }
}