package me.halfquark.arrowdirectors;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ArrowDirectors extends JavaPlugin {
	
	public static ArrowDirectors instance;
	public static FileConfiguration CONFIG;
	private ArrowDirectorManager arrowDirectors;

	@Override
	public void onLoad() {
		ArrowDirectorManager.register();
	}
	
	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		getConfig().options().copyDefaults(true);
		instance = this;
		CONFIG = this.getConfig();
		arrowDirectors = new ArrowDirectorManager();
        arrowDirectors.runTaskTimer(this, 0, 1);
        getServer().getPluginManager().registerEvents(arrowDirectors, this);
        getServer().getPluginManager().registerEvents(new ArrowHitListener(), this);
	}
	
	public ArrowDirectorManager getArrowDirectors() {
		return arrowDirectors;
	}
	
}
