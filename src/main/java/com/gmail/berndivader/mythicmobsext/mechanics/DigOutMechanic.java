package com.gmail.berndivader.mythicmobsext.mechanics;

import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.INoTargetSkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.api.skills.ThreadSafetyLevel;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.gmail.berndivader.mythicmobsext.Main;
import com.gmail.berndivader.mythicmobsext.NMS.NMSUtils;
import com.gmail.berndivader.mythicmobsext.externals.*;
import com.gmail.berndivader.mythicmobsext.utils.Utils;
import com.gmail.berndivader.mythicmobsext.volatilecode.Volatile;

import java.io.File;

@ExternalAnnotation(name = "digout", author = "BerndiVader")
public class DigOutMechanic extends SkillMechanic implements INoTargetSkill {
	long speed;
	int particle_amount;
	Sound sound;

	public DigOutMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
		super(manager, file, skill, mlc);
		this.line = skill;
		this.threadSafetyLevel = ThreadSafetyLevel.SYNC_ONLY;
		
		speed = mlc.getInteger("speed", 5);
		particle_amount = mlc.getInteger("amount", 5);
		try {
			sound = Sound.valueOf(mlc.getString("sound", "block_stone_hit").toUpperCase());
		} catch (Exception ex) {
			Main.logger.warning("Invalid sound type in " + skill + " reset to default");
			sound = Sound.BLOCK_STONE_HIT;
		}
	}

	@Override
	public SkillResult cast(SkillMetadata data) {
		if (!data.getCaster().getEntity().isLiving())
			return SkillResult.CONDITION_FAILED;
		final LivingEntity entity = (LivingEntity) data.getCaster().getEntity().getBukkitEntity();

		Location particle_location = entity.getLocation().getBlock().getLocation();
		Location location = particle_location.clone().add(0.5, 0, 0.5);
		Block block = location.getBlock().getRelative(BlockFace.DOWN);
		if (block.isLiquid() || block.getType() == Material.AIR)
			return SkillResult.CONDITION_FAILED;

		//final String particle_name = "blockcrack_" + block.getType().getId() + "_" + block.getData();

		boolean gravity = entity.hasGravity();
		boolean invulnerable = entity.isInvulnerable();
		NMSUtils.setEntityMotion(entity, new Vector(0, 0, 0));
		NMSUtils.setInvulnerable(entity, true);
		entity.setInvulnerable(true);
		entity.setGravity(false);
		final double y = entity.getHeight() + 0.5d;
		final double y_step = y / 10;
		location.subtract(0, y, 0);
		final double x = location.getX();
		final double z = location.getZ();
		entity.teleport(location);

		new BukkitRunnable() {
			int stage = 0;
			double delta_x, delta_z;

			@Override
			public void run() {
				if (entity == null || entity.isDead())
					this.cancel();
				Volatile.handler.playBlockBreak(entity.getEntityId(), block.getLocation(), stage);
				Volatile.handler.playAnimationPacket(entity, 0);
				location.getWorld().playSound(location, sound, 1.5f, 1f);
				delta_x = Main.random.nextDouble();
				delta_z = Main.random.nextDouble();
				if (Utils.serverV < 13) {
				} else {
				}
				if (stage > 9) {
					NMSUtils.setInvulnerable(entity, invulnerable);
					entity.setGravity(gravity);
					this.cancel();
				}
				Location l = entity.getLocation();
				l.setX(x);
				l.setZ(z);
				l.add(0, y_step, 0);
				entity.teleport(l);
				stage++;
			}
		}.runTaskTimer(Main.getPlugin(), 1l, speed);

		return SkillResult.SUCCESS;
	}
}