package com.gmail.berndivader.mythicmobsext.mechanics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.ThreadSafetyLevel;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.gmail.berndivader.mythicmobsext.Main;
import com.gmail.berndivader.mythicmobsext.externals.*;
import com.gmail.berndivader.mythicmobsext.utils.Utils;
import com.gmail.berndivader.mythicmobsext.volatilecode.Volatile;

@ExternalAnnotation(name = "stun,stun_ext", author = "BerndiVader")
public class StunextMechanic extends SkillMechanic implements ITargetedEntitySkill {

	private int duration;
	private boolean f;
	private boolean g;
	private boolean ai, useDuration, cancel_interaction, use_stun;

	static List<UUID> uuids;

	static {
		uuids = new ArrayList<>();
	}

	public StunextMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
		super(manager, file, skill, mlc);
		this.line = skill;
		this.threadSafetyLevel = ThreadSafetyLevel.SYNC_ONLY;

		this.duration = mlc.getInteger(new String[] { "duration", "dur" }, 120);
		this.f = mlc.getBoolean(new String[] { "facing", "face", "f" }, false);
		this.g = mlc.getBoolean(new String[] { "gravity", "g" }, false);
		this.ai = mlc.getBoolean(new String[] { "stopai", "ai" }, false);
		this.useDuration = mlc.getBoolean("useDuration", true);
		use_stun = mlc.getBoolean("usestun", true);
		cancel_interaction = mlc.getBoolean("cancelinteract", false);
	}

	@Override
	public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
		if (target.isLiving() && !target.getBukkitEntity().hasMetadata(Utils.meta_STUNNED)) {
			new StunTracker(target.getLocation(), target, this.duration);
			return SkillResult.SUCCESS;
		}
		return SkillResult.CONDITION_FAILED;
	}

	@EventHandler
	public void cancelInventoryClick(InventoryClickEvent e) {
		if (uuids.contains(e.getWhoClicked().getUniqueId()))
			e.setCancelled(true);
	}

	class StunTracker extends BukkitRunnable implements Listener {

		long count, duration;
		float yaw, pitch;
		double x, y, z;
		boolean facing, gravity, ai, ai_status;
		final LivingEntity final_target;
		final Location final_location;

		public StunTracker(AbstractLocation location, AbstractEntity target, long duration) {
			count = 0l;
			this.duration = duration;
			yaw = location.getYaw();
			pitch = location.getPitch();
			x = location.getX();
			y = location.getY();
			z = location.getZ();

			this.final_target = (LivingEntity) target.getBukkitEntity();
			this.final_location = BukkitAdapter.adapt(location).clone();

			if (final_target.hasMetadata(Utils.meta_STUNNED))
				target.getBukkitEntity().removeMetadata(Utils.meta_STUNNED, Main.getPlugin());
			if (final_target.hasMetadata(Utils.meta_STUNNED + "Time"))
				target.getBukkitEntity().removeMetadata(Utils.meta_STUNNED + "Time", Main.getPlugin());
			this.facing = StunextMechanic.this.f;
			gravity = StunextMechanic.this.g;
			ai = StunextMechanic.this.ai;
			target.getBukkitEntity().setMetadata(Utils.meta_STUNNED, new FixedMetadataValue(Main.getPlugin(), true));
			target.getBukkitEntity().setMetadata(Utils.meta_STUNNED + "Time",
					new FixedMetadataValue(Main.getPlugin(), duration));
			this.ai_status = final_target.hasAI();
			if (cancel_interaction) {
				uuids.remove(final_target.getUniqueId());
				uuids.add(final_target.getUniqueId());
				Main.pluginmanager.registerEvents(this, Main.getPlugin());
			}

			target.getBukkitEntity().setMetadata(Utils.meta_STUNNED, new FixedMetadataValue(Main.getPlugin(), true));
			target.getBukkitEntity().setMetadata(Utils.meta_STUNNED + "Time",
					new FixedMetadataValue(Main.getPlugin(), duration));

			this.runTaskTimer(Main.getPlugin(), 0l, 0l);
		}

		@Override
		public void run() {
			if (final_target == null || final_target.isDead() || count > this.duration
					|| !final_target.hasMetadata(Utils.meta_STUNNED)) {
				if (final_target != null && !final_target.isDead()) {
					final_target.removeMetadata(Utils.meta_STUNNED, Main.getPlugin());
					final_target.removeMetadata(Utils.meta_STUNNED + "Time", Main.getPlugin());
					final_target.setAI(this.ai_status);
				}
				HandlerList.unregisterAll(this);
				uuids.remove(final_target.getUniqueId());
				this.cancel();
			} else {
				if (StunextMechanic.this.use_stun) {
					if (final_target.hasAI() && final_target.isOnGround())
						final_target.setAI(false);
					if (facing) {
						yaw = final_target.getLocation().getYaw();
						pitch = final_target.getLocation().getPitch();
					}
					if (gravity)
						y = final_target.getLocation().getY();
					Volatile.handler.forceSetPositionRotation(this.final_target, x, y, z, yaw, pitch, facing, gravity);
				}
				final_target.removeMetadata(Utils.meta_STUNNED + "Time", Main.getPlugin());
				;
				final_target.setMetadata(Utils.meta_STUNNED + "Time",
						new FixedMetadataValue(Main.getPlugin(), (int) this.duration - count));
			}
			if (useDuration)
				count++;
		}

		@EventHandler(priority = EventPriority.LOW)
		public void cancelClickEvent(InventoryClickEvent e) {
			e.setCancelled(checkForEntity(e.getWhoClicked(), final_target));
		}

		@EventHandler(priority = EventPriority.LOW)
		public void cancelDragEvent(InventoryDragEvent e) {
			e.setCancelled(checkForEntity(e.getWhoClicked(), final_target));
		}

		@EventHandler(priority = EventPriority.LOW)
		public void cancelHotbarEvent(PlayerItemHeldEvent e) {
			e.setCancelled(checkForEntity(e.getPlayer(), final_target));
		}

		@EventHandler(priority = EventPriority.LOW)
		public void cancelConsumeEvent(PlayerItemConsumeEvent e) {
			e.setCancelled(checkForEntity(e.getPlayer(), final_target));
		}

		@EventHandler(priority = EventPriority.LOW)
		public void cancelPickUpEvent(PlayerPickupItemEvent e) {
			e.setCancelled(checkForEntity(e.getPlayer(), final_target));
		}

		@EventHandler(priority = EventPriority.LOW)
		public void cancelDropEvent(PlayerDropItemEvent e) {
			e.setCancelled(checkForEntity(e.getPlayer(), final_target));
		}

	}

	static boolean checkForEntity(LivingEntity target, LivingEntity final_target) {
		return target.getUniqueId() == final_target.getUniqueId();
	}

}
