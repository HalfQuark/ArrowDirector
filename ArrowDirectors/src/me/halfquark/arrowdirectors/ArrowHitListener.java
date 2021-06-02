package me.halfquark.arrowdirectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class ArrowHitListener implements Listener {

	@EventHandler
	public void onArrowHit(EntityDamageByEntityEvent e) {
		if(!(e.getDamager() instanceof Arrow))
			return;
		Arrow arrow = (Arrow) e.getDamager();
		if(!arrow.hasMetadata("ArrowDamage"))
			return;
		double dmg = arrow.getMetadata("ArrowDamage").get(0).asDouble();
		e.setDamage(dmg);
		if(ArrowDirectors.CONFIG.getBoolean("DamageCooldown"))
			return;
		if(!(e.getEntity() instanceof LivingEntity))
			return;
		Bukkit.getScheduler().runTaskLaterAsynchronously(ArrowDirectors.instance, ()->{
            ((LivingEntity) e.getEntity()).setNoDamageTicks(0);
        }, 2L);
	}
	
}
