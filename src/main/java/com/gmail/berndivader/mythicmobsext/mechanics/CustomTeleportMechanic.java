package com.gmail.berndivader.mythicmobsext.mechanics;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.*;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.adapters.BukkitEntity;
import io.lumine.mythic.bukkit.adapters.BukkitPlayer;
import io.lumine.mythic.core.config.MythicLineConfigImpl;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.lumine.mythic.core.skills.*;
import io.lumine.mythic.core.skills.placeholders.parsers.PlaceholderStringImpl;
import io.lumine.mythic.core.skills.targeters.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.gmail.berndivader.mythicmobsext.Main;
import com.gmail.berndivader.mythicmobsext.NMS.NMSUtils;
import com.gmail.berndivader.mythicmobsext.externals.ExternalAnnotation;
import com.gmail.berndivader.mythicmobsext.targeters.CustomTargeters;
import com.gmail.berndivader.mythicmobsext.utils.Utils;
import com.gmail.berndivader.mythicmobsext.utils.math.MathUtils;

@ExternalAnnotation(name = "customteleport", author = "BerndiVader")
public class CustomTeleportMechanic extends SkillMechanic implements ITargetedEntitySkill, ITargetedLocationSkill {
	PlaceholderString stargeter;
	String FinalSignal, inBetweenLastSignal, inBetweenNextSignal, stricts[], excludes[];
	boolean inFrontOf, isLocations, returnToStart, sortTargets, targetInsight, ignoreOwner, ignorePitch;
	double delay, noise, maxTargets, frontOffset, sideOffset, yOffset;

	public CustomTeleportMechanic(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
		super(manager, file, skill, mlc);
		this.line = skill;
		this.threadSafetyLevel = ThreadSafetyLevel.SYNC_ONLY;
		
		this.noise = mlc.getDouble(new String[] { "noise", "n", "radius", "r" }, 0D);
		this.delay = mlc.getDouble(new String[] { "teleportdelay", "tdelay", "td" }, 0D);
		this.frontOffset = mlc.getDouble(new String[] { "frontoffest", "fo" }, 0D);
		this.sideOffset = mlc.getDouble(new String[] { "sideoffset", "so" }, 0D);
		this.yOffset = mlc.getDouble(new String[] { "yoffset", "yo" }, 0d);
		if ((this.maxTargets = mlc.getDouble(new String[] { "maxtargets", "mt" }, 0D)) < 0)
			this.maxTargets = 0D;
		this.inFrontOf = mlc.getBoolean(new String[] { "infrontof", "infront", "front", "f" }, false);
		this.returnToStart = mlc.getBoolean(new String[] { "returntostart", "return", "rs" }, false);
		this.targetInsight = mlc.getBoolean(new String[] { "targetinsight", "insight", "is" }, false);
		this.ignoreOwner = mlc.getBoolean(new String[] { "ignoreowner", "io" }, false);
		this.ignorePitch = mlc.getBoolean(new String[] { "ignorepitch", "ip" }, false);
		this.inBetweenLastSignal = mlc.getString(new String[] { "betweenlastentitysignal", "bls" }, null);
		this.inBetweenNextSignal = mlc.getString(new String[] { "betweennextentitysignal", "bns" }, null);
		this.FinalSignal = mlc.getString(new String[] { "finalsignal", "final", "fs" }, null);
		this.excludes = mlc.getString("exclude", "").split(",");
		this.stricts = mlc.getString("stricts", "").split(",");

		String s = mlc.getString(new String[] { "destination", "dest", "d", "location", "loc", "l" }, "@self");
		s = s.replaceAll("<&lc>", "{");
		s = s.replaceAll("<&rc>", "}");
		s = s.replaceAll("<&eq>", "=");
		s = s.replaceAll("<&sc>", ";");
		s = s.replaceAll("<&cm>", ",");
		s = s.replaceAll("<&lb>", "[");
		s = s.replaceAll("<&rb>", "]");
		String parse = s = s.substring(1, s.length() - 1);
		if (!parse.startsWith("@"))
			parse = "@" + parse;
		this.stargeter = new PlaceholderStringImpl(parse);
	}

	@Override
	public SkillResult castAtLocation(SkillMetadata data, AbstractLocation target) {
		return this.doMechanic(data, target);
	}

	@Override
	public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
		return this.doMechanic(data, target);
	}

	@SuppressWarnings("unchecked")
	private SkillResult doMechanic(SkillMetadata data, Object target) {
		AbstractEntity entityTarget;
		AbstractLocation startLocation;
		String targeter = this.stargeter.get(data);
		if (target.getClass().equals(BukkitEntity.class) || target.getClass().equals(BukkitPlayer.class)) {
			targeter = this.stargeter.get(data, (AbstractEntity) target);
			entityTarget = (AbstractEntity) target;
			startLocation = ((AbstractEntity) target).getLocation();
		} else {
			Bukkit.getLogger().warning("A location is not a valid source for advanced teleport mechanic!");
			return SkillResult.ERROR;
		}
		HashSet<Object> osources = (HashSet<Object>) getDestination(targeter, data);
		if (osources == null || !osources.iterator().hasNext())
			return SkillResult.CONDITION_FAILED;
		this.isLocations = osources.iterator().next() instanceof AbstractLocation;
		if (this.maxTargets > 0 && osources.size() > this.maxTargets) {
			HashSet<Object> lsrc = new HashSet<>();
			Iterator<?> it = osources.iterator();
			int c = 1;
			while (it.hasNext()) {
				if (c > this.maxTargets) {
					osources.clear();
					osources.addAll(lsrc);
					break;
				}
				lsrc.add(it.next());
				c++;
			}
		}
		if (!this.isLocations) {
			if (this.ignoreOwner && data.getCaster() instanceof ActiveMob am) {
				if (am.getOwner().isPresent()) {
					World w = data.getCaster().getEntity().getBukkitEntity().getWorld();
					osources.remove(BukkitAdapter
							.adapt(NMSUtils.getEntity(w, ((ActiveMob) data.getCaster()).getOwner().get())));
				}
			}
		}

		new BukkitRunnable() {
			AbstractEntity sourceEntity = entityTarget;
			AbstractEntity lastEntity;
			boolean isLoc = isLocations;
			boolean ifo = inFrontOf;
			boolean is = targetInsight;
			Iterator<?> it = osources.iterator();
			double n = noise;
			double fo = CustomTeleportMechanic.this.frontOffset;
			double so = CustomTeleportMechanic.this.sideOffset;
			double yo = CustomTeleportMechanic.this.yOffset;
			boolean ip = CustomTeleportMechanic.this.ignorePitch;
			String bls = inBetweenLastSignal;
			String bns = inBetweenNextSignal;
			String fs = FinalSignal;
			AbstractLocation start = startLocation;

			@Override
			public void run() {
				block: {
					if (!this.it.hasNext()) {
						if (returnToStart)
							this.sourceEntity.teleport(this.start);
						if (this.fs != null) {
							if (this.isLoc) {
								((ActiveMob) data.getCaster()).signalMob(null, this.fs);
							} else {
								((ActiveMob) data.getCaster()).signalMob((AbstractEntity) target, this.fs);
							}
						}
						this.cancel();
						return;
					}
					Object target = this.it.next();
					if (this.isLoc) {
						if (this.n > 0)
							target = Utils.findSafeSpawnLocation((AbstractLocation) target, (int) this.n, 0,
									((ActiveMob) data.getCaster()).getType().getMythicEntity().getHeight(), false);
						Location ll = BukkitAdapter.adapt((AbstractLocation) target);
						if (this.bns != null)
							((ActiveMob) data.getCaster()).signalMob(null, this.bns);
						if (ip)
							ll.setPitch(0);
						if (fo != 0.0d || so != 0.0d) {
							Vector foV = MathUtils.getFrontBackOffsetVector(ll.getDirection(), this.fo);
							Vector soV = MathUtils.getSideOffsetVectorFixed(ll.getYaw(), this.so, false);
							ll.add(foV);
							ll.add(soV);
						}
						target = BukkitAdapter.adapt(ll);
						this.sourceEntity.teleport(((AbstractLocation) target).add(0, yo, 0));
						if (this.bls != null)
							((ActiveMob) data.getCaster()).signalMob(null, this.bls);
					} else {
						if (lastEntity == null)
							lastEntity = this.sourceEntity;
						AbstractEntity t = (AbstractEntity) target;
						target = ((AbstractEntity) target).getLocation();
						if (this.is && !this.sourceEntity.hasLineOfSight(t))
							break block;
						if (this.ifo)
							target = t.getLocation()
									.add(t.getLocation().getDirection().setY(0).normalize().multiply(2));
						if (this.n > 0)
							target = Utils.findSafeSpawnLocation(((AbstractLocation) target), (int) this.n, 0,
									(int)BukkitAdapter.adapt(data.getCaster().getEntity()).getHeight(), false);
						if (this.bns != null)
							((ActiveMob) data.getCaster()).signalMob(t, this.bns);
						Location ll = BukkitAdapter.adapt((AbstractLocation) target);
						if (ip)
							ll.setPitch(0);
						if (fo != 0.0d || so != 0.0d) {
							Vector foV = MathUtils.getFrontBackOffsetVector(ll.getDirection(), this.fo);
							Vector soV = MathUtils.getSideOffsetVectorFixed(ll.getYaw(), this.so, false);
							ll.add(foV);
							ll.add(soV);
						}
						target = BukkitAdapter.adapt(ll);
						this.sourceEntity.teleport(((AbstractLocation) target).add(0, yo, 0));
						if (this.bls != null)
							((ActiveMob) data.getCaster()).signalMob(this.lastEntity, this.bls);
						this.lastEntity = t;
					}
				}
			}
		}.runTaskTimer(Main.getPlugin(), 0L, (long) this.delay);
		return SkillResult.SUCCESS;
	}

	protected static Collection<?> getDestination(String target, SkillMetadata skilldata) {
		SkillMetadata data = new SkillMetadataImpl(SkillTriggers.API, skilldata.getCaster(), skilldata.getTrigger(),
				skilldata.getOrigin(), null, null, 1.0f);
		Optional<SkillTargeter> maybeTargeter;
		maybeTargeter = Optional.of(Utils.parseSkillTargeter(target));
		if (maybeTargeter.isPresent()) {
			SkillTargeter targeter = maybeTargeter.get();
			if (targeter instanceof CustomTargeter) {
				String s1 = target.substring(1);
				MythicLineConfig mlc = new MythicLineConfigImpl(s1);
				String s2 = s1.contains("{") ? s1.substring(0, s1.indexOf("{")) : s1;
				if ((targeter = CustomTargeters.getCustomTargeter(s2, mlc, targeter.getManager())) == null)
					targeter = new TriggerTargeter(targeter.getManager(), mlc);
			}
			if (targeter instanceof IEntitySelector) {
				data.setEntityTargets(((IEntitySelector) targeter).getEntities(data));
				((IEntitySelector) targeter).filter(data, false);
				return data.getEntityTargets();
			}
			if (targeter instanceof ILocationSelector) {
				data.setLocationTargets(((ILocationSelector) targeter).getLocations(data));
				((ILocationSelector) targeter).filter(data);
			} else if (targeter instanceof OriginTargeter) {
				data.setLocationTargets(((OriginTargeter) targeter).getLocations(data));
			} else if (targeter instanceof TargetLocationTargeter) {
				HashSet<AbstractLocation> lTargets = new HashSet<AbstractLocation>();
				lTargets.add(data.getTrigger().getLocation());
				data.setLocationTargets(lTargets);
			}
			if (targeter instanceof ConsoleTargeter) {
				data.setEntityTargets(null);
				data.setLocationTargets(null);
			}
			return data.getLocationTargets();
		}
		return null;
	}

}
