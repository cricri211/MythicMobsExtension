package com.gmail.berndivader.mythicmobsext.targeters;

import java.util.HashSet;

import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.skills.SkillExecutor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import com.gmail.berndivader.mythicmobsext.NMS.NMSUtils;
import com.gmail.berndivader.mythicmobsext.externals.*;
import com.gmail.berndivader.mythicmobsext.utils.Utils;
import com.gmail.berndivader.mythicmobsext.utils.Vec3D;

@ExternalAnnotation(name = "targetmotion,triggermotion,selfmotion,ownermotion", author = "BerndiVader")
public class TargetMotionTargeter extends ISelectorLocation {
	String selector;
	int length;
	double dyo;
	boolean iy;

	public TargetMotionTargeter(SkillExecutor manager, MythicLineConfig mlc) {
		super(manager, mlc);
		selector = mlc.getLine().toLowerCase().split("motion")[0];
		dyo = mlc.getDouble("yoffset", 0d);
		iy = mlc.getBoolean("ignorey", true);

	}

	@Override
	public HashSet<AbstractLocation> getLocations(SkillMetadata data) {
		Entity ee = null;
		HashSet<AbstractLocation> targets = new HashSet<>();
		switch (selector) {
		case "target":
			ee = data.getEntityTargets().size() > 0 ? data.getEntityTargets().iterator().next().getBukkitEntity()
					: data.getCaster().getEntity().getTarget() != null
							? data.getCaster().getEntity().getTarget().getBukkitEntity()
							: null;
			break;
		case "trigger":
			if (data.getTrigger() != null)
				ee = data.getTrigger().getBukkitEntity();
			break;
		case "owner":
			ActiveMob am = (ActiveMob) data.getCaster();
			if (am != null && am.getOwner().isPresent()) {
				ee = NMSUtils.getEntity(data.getCaster().getEntity().getBukkitEntity().getWorld(), am.getOwner().get());
			}
			break;
		default:
			ee = data.getCaster().getEntity().getBukkitEntity();
			break;
		}
		if (ee != null) {
			Location s = ee.getLocation().clone(), t;
			s.setPitch(0f);
			s.setYaw(0f);
			t = s.clone();
			Vec3D v3 = ee.getType() == EntityType.PLAYER
					? Utils.players.getOrDefault(ee.getUniqueId(), new Vec3D(0d, 0d, 0d))
					: NMSUtils.getEntityLastMot(ee);
			if (iy)
				v3.setY(0d);
			t.subtract(v3.getX() * length, v3.getY() * (length / 2), v3.getZ() * length).add(0d, dyo, 0d);
			targets.add(BukkitAdapter.adapt(t));
		}
		return applyOffsets(targets);
	}
}
