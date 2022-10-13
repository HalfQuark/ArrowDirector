package me.halfquark.arrowdirectors;

import static me.halfquark.arrowdirectors.ArrowDirectors.CONFIG;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.combat.features.directors.Directors;
import net.countercraft.movecraft.combat.localisation.I18nSupport;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.PlayerCraft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.property.BooleanProperty;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static net.countercraft.movecraft.util.ChatUtils.ERROR_PREFIX;

public class ArrowDirectorManager extends Directors implements Listener {
    public static final NamespacedKey ALLOW_ARROW_DIRECTOR_SIGN = new NamespacedKey("movecraft-combat", "allow_arrow_director_sign");
    private static final String HEADER = "Arrow Director";
    private long lastCheck = 0;

    public ArrowDirectorManager() {
        super();
    }

    public static void register() {
        CraftType.registerProperty(new BooleanProperty("allowArrowDirectorSign", ALLOW_ARROW_DIRECTOR_SIGN, type -> true));
    }

    public static void load(@NotNull FileConfiguration config) {

    }

    @Override
    public void run() {
        long ticksElapsed = (System.currentTimeMillis() - lastCheck) / 50;
        if (ticksElapsed <= 3)
            return;

        for (World w : Bukkit.getWorlds()) {
            if (w == null || w.getPlayers().size() == 0)
                continue;

            var allArrows = w.getEntitiesByClass(Arrow.class);
            for (Arrow arrow : allArrows)
                processArrow(arrow);
        }

        lastCheck = System.currentTimeMillis();
    }

    private void processArrow(@NotNull Arrow arrow) {
        if (arrow.getShooter() instanceof org.bukkit.entity.LivingEntity)
            return;

        Craft c = MathUtils.fastNearestCraftToLoc(CraftManager.getInstance().getCrafts(), arrow.getLocation());
        if (!(c instanceof PlayerCraft) || !hasDirector((PlayerCraft) c))
            return;
        Location arrowLoc = arrow.getLocation();
        int arrowFireTicks = arrow.getFireTicks();
        Vector fv = arrow.getVelocity();

        arrow.remove();

        Player p = getDirector((PlayerCraft) c);

        MovecraftLocation midPoint = c.getHitBox().getMidPoint();
        int distX = Math.abs(midPoint.getX() - arrowLoc.getBlockX());
        int distY = Math.abs(midPoint.getY() - arrowLoc.getBlockY());
        int distZ = Math.abs(midPoint.getZ() - arrowLoc.getBlockZ());
        if (distX > CONFIG.getInt("ArrowDirectorDistance") || distY > CONFIG.getInt("ArrowDirectorDistance") || distZ > CONFIG.getInt("ArrowDirectorDistance"))
            return;

        fv = fv.normalize(); // you normalize it for comparison with the new direction to see if we are trying to steer too far

        Vector targetVector;

        if(CONFIG.getBoolean("Converge")) {
            Vector ConvergenceVector = p.getLocation().getDirection().multiply(CONFIG.getDouble("ConvergenceDistance"));
            Location targetLoc = p.getLocation().add(ConvergenceVector);
            targetVector = targetLoc.toVector().subtract(arrowLoc.toVector());
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
        Arrow newArrow = arrowLoc.getWorld().spawnArrow(arrowLoc, fv, (float) CONFIG.getDouble("ArrowVelocity"), 0);
        newArrow.setFireTicks(arrowFireTicks);
        newArrow.setShooter(p);
        newArrow.setMetadata("ArrowDamage", new FixedMetadataValue(ArrowDirectors.instance, CONFIG.getDouble("ArrowDamage")));
    }


    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSignClick(@NotNull PlayerInteractEvent e) {
        var action = e.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK)
            return;

        Block b = e.getClickedBlock();
        if (b == null)
            throw new IllegalStateException();
        var state = b.getState();
        if (!(state instanceof Sign))
            return;

        Sign sign = (Sign) state;
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER))
            return;

        PlayerCraft foundCraft = null;
        for (Craft c : CraftManager.getInstance()) {
            if (!(c instanceof PlayerCraft))
                continue;
            if (!c.getHitBox().contains(MathUtils.bukkit2MovecraftLoc(b.getLocation())))
                continue;
            foundCraft = (PlayerCraft) c;
            break;
        }

        Player p = e.getPlayer();
        if (foundCraft == null) {
            p.sendMessage(ERROR_PREFIX + " " + I18nSupport.getInternationalisedString("Sign - Must Be Part Of Craft"));
            return;
        }

        if (!foundCraft.getType().getBoolProperty(ALLOW_ARROW_DIRECTOR_SIGN)) {
            p.sendMessage(ERROR_PREFIX + " " + I18nSupport.getInternationalisedString("ArrowDirector - Not Allowed On Craft"));
            return;
        }

        if (action == Action.LEFT_CLICK_BLOCK) {
            if (!isDirector(p))
                return;

            removeDirector(p);
            p.sendMessage(I18nSupport.getInternationalisedString("ArrowDirector - No Longer Directing"));
            e.setCancelled(true);
            return;
        }

        clearDirector(p);
        addDirector(foundCraft, p);
        p.sendMessage(I18nSupport.getInternationalisedString("ArrowDirector - Directing"));
    }
}
