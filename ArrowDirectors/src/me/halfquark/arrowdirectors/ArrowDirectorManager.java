package me.halfquark.arrowdirectors;

import static me.halfquark.arrowdirectors.ArrowDirectors.CONFIG;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.combat.movecraftcombat.config.Config;
import net.countercraft.movecraft.combat.movecraftcombat.directors.DirectorManager;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;

public class ArrowDirectorManager extends DirectorManager {

	private long lastCheck = 0;
	
    public void run() {
    	long ticksElapsed = (System.currentTimeMillis() - lastCheck) / 50;
        if (ticksElapsed <= 3) {
            return;
        }

        processDirectors();
        lastCheck = System.currentTimeMillis();
    }
    
    private void processDirectors() {
    	for (World w : Bukkit.getWorlds()) {
            if (w == null)
                continue;

            ArrayList<Arrow> allArrows = new ArrayList<>(w.getEntitiesByClass(Arrow.class));
            for (Arrow arrow : allArrows) {
                if (arrow.getShooter() instanceof org.bukkit.entity.LivingEntity || w.getPlayers().size() == 0)
                    continue;

                Craft c = CraftManager.getInstance().fastNearestCraftToLoc(arrow.getLocation());
                if (c == null || !hasDirector(c))
                    continue;

                Player p = getDirector(c);

                MovecraftLocation midPoint = c.getHitBox().getMidPoint();
                int distX = Math.abs(midPoint.getX() - arrow.getLocation().getBlockX());
                int distY = Math.abs(midPoint.getY() - arrow.getLocation().getBlockY());
                int distZ = Math.abs(midPoint.getZ() - arrow.getLocation().getBlockZ());
                if (distX > CONFIG.getInt("ArrowDirectorDistance") || distY > CONFIG.getInt("ArrowDirectorDistance") || distZ > CONFIG.getInt("ArrowDirectorDistance"))
                    continue;

                //arrow.setShooter(p);

                if (p.getInventory().getItemInMainHand().getType() != Config.DirectorTool)
                    continue;

                Vector fv = arrow.getVelocity();
                fv = fv.normalize(); // you normalize it for comparison with the new direction to see if we are trying to steer too far
                
                Vector targetVector;
                
                if(CONFIG.getBoolean("Converge")) {
	                Vector ConvergenceVector = p.getLocation().getDirection().multiply(CONFIG.getDouble("ConvergenceDistance"));
	                Location targetLoc = p.getLocation().add(ConvergenceVector);
	                targetVector = targetLoc.toVector().subtract(arrow.getLocation().toVector());
	                targetVector = targetVector.normalize();
                } else {
                	targetVector = p.getLocation().getDirection();
                }
                if (targetVector.getX() - fv.getX() > 0.5) {
                    fv.setX(fv.getX() + 0.5);
                } else if (targetVector.getX() - fv.getX() < -0.5) {
                    fv.setX(fv.getX() - 0.5);
                } else {
                    fv.setX(targetVector.getX());
                }

                if (targetVector.getY() - fv.getY() > 0.5) {
                    fv.setY(fv.getY() + 0.5);
                } else if (targetVector.getY() - fv.getY() < -0.5) {
                    fv.setY(fv.getY() - 0.5);
                } else {
                    fv.setY(targetVector.getY());
                }

                if (targetVector.getZ() - fv.getZ() > 0.5) {
                    fv.setZ(fv.getZ() + 0.5);
                } else if (targetVector.getZ() - fv.getZ() < -0.5) {
                    fv.setZ(fv.getZ() - 0.5);
                } else {
                    fv.setZ(targetVector.getZ());
                }

                fv = fv.multiply(CONFIG.getDouble("ArrowVelocity")); // put the original speed back in, but now along a different trajectory
                Arrow newArrow = w.spawnArrow(arrow.getLocation(), fv, (float) CONFIG.getDouble("ArrowVelocity"), 0);
                newArrow.setFireTicks(arrow.getFireTicks());
                newArrow.setShooter(p);
                arrow.remove();
            }
        }
    }
	
}
