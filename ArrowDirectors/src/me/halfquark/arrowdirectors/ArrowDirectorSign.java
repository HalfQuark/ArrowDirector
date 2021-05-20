package me.halfquark.arrowdirectors;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import net.countercraft.movecraft.combat.movecraftcombat.localisation.I18nSupport;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.utils.ChatUtils;
import net.countercraft.movecraft.utils.MathUtils;

import static me.halfquark.arrowdirectors.ArrowDirectors.*;

import javax.annotation.Nullable;

public class ArrowDirectorSign implements Listener {
    private static final String HEADER = "Arrow Director";

    @EventHandler
    public final void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if(!isSign(block)) {
            return;
        }
        Sign sign = (Sign) event.getClickedBlock().getState();
        if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(HEADER)) {
            return;
        }

        Player player = event.getPlayer();
        Craft foundCraft = null;
        for (Craft tcraft : CraftManager.getInstance().getCraftsInWorld(block.getWorld())) {
            if (tcraft.getHitBox().contains(MathUtils.bukkit2MovecraftLoc(block.getLocation())) &&
                    CraftManager.getInstance().getPlayerFromCraft(tcraft) != null) {
                foundCraft = tcraft;
                break;
            }
        }
        if (foundCraft == null) {
            player.sendMessage(ChatUtils.ERROR_PREFIX + " " + I18nSupport.getInternationalisedString("Sign - Must Be Part Of Craft"));
            return;
        }

        if (!CONFIG.getStringList("ArrowDirectorsAllowed").contains(foundCraft.getType().getCraftName())) {
            player.sendMessage(ChatUtils.ERROR_PREFIX + " " + I18nSupport.getInternationalisedString("Arrow Director - Not Allowed On Craft"));
            return;
        }

        ArrowDirectorManager aa = ArrowDirectors.instance.getArrowDirectors();
        if(event.getAction() == Action.LEFT_CLICK_BLOCK && aa.isDirector(player)){
            aa.removeDirector(event.getPlayer());
            player.sendMessage(I18nSupport.getInternationalisedString("Arrow Director - No Longer Directing"));
            return;
        }

        aa.addDirector(foundCraft, event.getPlayer());
        player.sendMessage(I18nSupport.getInternationalisedString("Arrow Director - Directing"));
    }

    private boolean isSign(@Nullable Block block){
        if (block == null)
            return false;
        final Material type = block.getType();

        return type.name().equals("SIGN_POST") ||
                type.name().equals("SIGN") ||
                type.name().equals("WALL_SIGN") ||
                type.name().endsWith("_SIGN") ||
                type.name().endsWith("_WALL_SIGN");
    }
}