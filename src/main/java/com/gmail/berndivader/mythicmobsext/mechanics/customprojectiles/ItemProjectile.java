package com.gmail.berndivader.mythicmobsext.mechanics.customprojectiles;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.adapters.AbstractLocation;
import io.lumine.mythic.api.adapters.AbstractVector;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.*;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.bukkit.utils.numbers.Numbers;
import io.lumine.mythic.core.items.MythicItem;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.utils.BlockUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import com.gmail.berndivader.mythicmobsext.Main;
import com.gmail.berndivader.mythicmobsext.externals.*;
import com.gmail.berndivader.mythicmobsext.utils.EntityCacheHandler;
import com.gmail.berndivader.mythicmobsext.utils.MythicHitBox;
import com.gmail.berndivader.mythicmobsext.utils.Utils;
import com.gmail.berndivader.mythicmobsext.utils.math.MathUtils;
import com.gmail.berndivader.mythicmobsext.volatilecode.Volatile;

@ExternalAnnotation(name = "itemprojectile", author = "BerndiVader")
public class ItemProjectile extends CustomProjectile implements ITargetedEntitySkill, ITargetedLocationSkill {

	ItemStack item_stack;
	protected float pEntitySpin;
	protected float pEntityPitchOffset;
	short durability;

	public ItemProjectile(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
		super(manager, file, skill, mlc);

		String i = mlc.getString(new String[] { "pobject", "projectileblock", "pitem" }, "DIRT");
		Optional<MythicItem> optional = Utils.mythicmobs.getItemManager().getItem(i);
		if (optional.isPresent()) {
			item_stack = BukkitAdapter.adapt(optional.get().generateItemStack(1));
		} else {
			try {
				item_stack = new ItemStack(Material.valueOf(i.toUpperCase()));
			} catch (Exception e) {
				item_stack = new ItemStack(Material.DIRT);
			}
		}
		this.pEntitySpin = mlc.getFloat("pspin", 0.0F);
		this.pEntityPitchOffset = mlc.getFloat("ppOff", 360.0f);
		this.tickInterval = 2;
		this.ticksPerSecond = 20.0f / this.tickInterval;
		this.durability = (short) MathUtils.clamp(mlc.getInteger("durability", Short.MIN_VALUE), Short.MIN_VALUE,
				Short.MAX_VALUE);
	}

	@Override
	public SkillResult castAtLocation(SkillMetadata data, AbstractLocation target) {
		try {
			new ProjectileRunner(data, this.item_stack, target.clone().add(0.0, this.targetYOffset, 0.0));
			return SkillResult.SUCCESS;
		} catch (Exception ex) {
			return SkillResult.ERROR;
		}
	}

	@Override
	public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
		return this.castAtLocation(data, target.getLocation().add(0.0, target.getEyeHeight() / 2.0, 0.0));
	}

	public class ProjectileRunner implements IParentSkill, Runnable {
		private SkillMetadata data;
		private boolean cancelled;
		private SkillCaster am;
		private float power;
		private float gravity;
		private long startTime;
		private AbstractLocation startLocation;
		private AbstractLocation currentLocation, oldLocation;
		private AbstractVector currentVelocity;
		private int currentX;
		private int currentZ;
		private int taskId;
		private Set<AbstractEntity> inRange;
		private HashSet<AbstractEntity> targets;
		private Map<AbstractEntity, Long> immune;
		private Item pItem;
		private float pSpin;
		private double pVOff;
		private double pFOff;
		private boolean pFaceDir, targetable, eyedir;
		private float currentBounce, bounceReduce;

		public ProjectileRunner(SkillMetadata data, ItemStack item_stack, AbstractLocation target) {

			float noise;
			this.cancelled = false;
			this.gravity = 0.0f;
			this.inRange = ConcurrentHashMap.newKeySet();
			this.targets = new HashSet<AbstractEntity>();
			this.immune = new HashMap<AbstractEntity, Long>();
			this.cancelled = false;
			this.data = data;
			this.data.setCallingEvent(this);
			this.am = data.getCaster();
			this.power = data.getPower();
			this.startTime = System.currentTimeMillis();
			this.pSpin = ItemProjectile.this.pEntitySpin;
			this.pFaceDir = ItemProjectile.this.pFaceDirection;
			this.pVOff = ItemProjectile.this.pVOffset;
			this.pFOff = ItemProjectile.this.pFOffset;
			this.targetable = ItemProjectile.this.targetable;
			this.eyedir = ItemProjectile.this.eyedir;
			this.bounceReduce = ItemProjectile.this.bounceReduce;
			this.currentBounce = ItemProjectile.this.projectileVelocity;
			double velocity = 0.0;

			if (ItemProjectile.this.type == ProjectileType.METEOR) {
				this.startLocation = target.clone();
				this.startLocation.add(0.0, ItemProjectile.this.heightFromSurface, 0.0);
				if (ItemProjectile.this.projectileGravity <= 0.0f) {
					this.gravity = ItemProjectile.this.projectileVelocity;
					this.gravity = this.gravity > 0.0f ? this.gravity / ItemProjectile.this.ticksPerSecond : 0.0f;
				} else {
					this.gravity = ItemProjectile.this.projectileGravity > 0.0f
							? ItemProjectile.this.projectileGravity / ItemProjectile.this.ticksPerSecond
							: 0.0f;
				}
				velocity = 0.0;
			} else {
				this.startLocation = ItemProjectile.this.sourceIsOrigin ? data.getOrigin().clone()
						: data.getCaster().getEntity().getLocation().clone();
				velocity = ItemProjectile.this.projectileVelocity / ItemProjectile.this.ticksPerSecond;
				if (ItemProjectile.this.startYOffset != 0.0f) {
					this.startLocation.setY(this.startLocation.getY() + ItemProjectile.this.startYOffset);
				}
				if (ItemProjectile.this.startForwardOffset != 0.0f) {
					Vector v = MathUtils.getFrontBackOffsetVector(
							BukkitAdapter.adapt(this.startLocation).getDirection(),
							ItemProjectile.this.startForwardOffset);
					AbstractVector av = new AbstractVector(v.getX(), v.getY(), v.getZ());
					this.startLocation.add(av);
				}
				if (ItemProjectile.this.startSideOffset != 0.0f) {
					Vector v = MathUtils.getSideOffsetVectorFixed(this.startLocation.getYaw(),
							ItemProjectile.this.startSideOffset, false);
					AbstractVector av = new AbstractVector(v.getX(), v.getY(), v.getZ());
					this.startLocation.add(av);
				}
			}
			this.currentLocation = this.startLocation.clone();
			if (this.currentLocation == null)
				return;
			if (!this.eyedir) {
				this.currentVelocity = target.toVector().subtract(this.currentLocation.toVector()).normalize();
			} else {
				LivingEntity bukkitEntity = (LivingEntity) this.am.getEntity().getBukkitEntity();
				AbstractLocation al = BukkitAdapter.adapt(bukkitEntity.getEyeLocation());
				this.currentVelocity = al.getDirection().normalize();
			}
			if (ItemProjectile.this.projectileVelocityHorizOffset != 0.0f
					|| ItemProjectile.this.projectileVelocityHorizNoise > 0.0f) {
				noise = 0.0f;
				if (ItemProjectile.this.projectileVelocityHorizNoise > 0.0f) {
					noise = (float) (ItemProjectile.this.projectileVelocityHorizNoiseBase
							+ Numbers.randomDouble() * ItemProjectile.this.projectileVelocityHorizNoise);
				}
				this.currentVelocity.rotate(ItemProjectile.this.projectileVelocityHorizOffset + noise);
			}
			if (ItemProjectile.this.startSideOffset != 0.0f) {
				// empty if block
			}
			if (ItemProjectile.this.projectileVelocityVertOffset != 0.0f
					|| ItemProjectile.this.projectileVelocityVertNoise > 0.0f) {
				noise = 0.0f;
				if (ItemProjectile.this.projectileVelocityVertNoise > 0.0f) {
					noise = (float) (ItemProjectile.this.projectileVelocityVertNoiseBase
							+ Numbers.randomDouble() * ItemProjectile.this.projectileVelocityVertNoise);
				}
				this.currentVelocity
						.add(new AbstractVector(0.0f, ItemProjectile.this.projectileVelocityVertOffset + noise, 0.0f))
						.normalize();
			}
			if (ItemProjectile.this.hugSurface) {
				this.currentLocation.setY(((int) this.currentLocation.getY()) + ItemProjectile.this.heightFromSurface);
				this.currentVelocity.setY(0).normalize();
			}
			if (ItemProjectile.this.powerAffectsVelocity) {
				this.currentVelocity.multiply(this.power);
			}
			this.currentVelocity.multiply(velocity);
			if (ItemProjectile.this.projectileGravity > 0.0f) {
				this.currentVelocity.setY(this.currentVelocity.getY() - this.gravity);
			}

			this.taskId = Main.taskManager.scheduleTask(this, 0, ItemProjectile.this.tickInterval);
			if (ItemProjectile.this.hitPlayers || ItemProjectile.this.hitNonPlayers) {
				this.inRange.addAll(this.currentLocation.getWorld().getLivingEntities());
				this.inRange.removeIf(e -> {
					if (e != null) {
						if (e.getUniqueId().equals(this.am.getEntity().getUniqueId())
								|| e.getBukkitEntity().hasMetadata(Utils.noTargetVar)) {
							return true;
						}
						if (!ItemProjectile.this.hitPlayers && e.isPlayer()) {
							return true;
						}
						if (!ItemProjectile.this.hitNonPlayers && !e.isPlayer()) {
							return true;
						}
					} else {
						return true;
					}
					return false;
				});
			}
			if (ItemProjectile.this.onStartSkill.isPresent() && ItemProjectile.this.onStartSkill.get().isUsable(data)) {
				SkillMetadata sData = data.deepClone();
				HashSet<AbstractLocation> targets = new HashSet<AbstractLocation>();
				targets.add(this.startLocation);
				sData.setLocationTargets(targets);
				sData.setOrigin(this.currentLocation.clone());
				ItemProjectile.this.onStartSkill.get().execute(sData);
			}
			ItemStack i = new ItemStack(item_stack);
			if (durability > Short.MIN_VALUE)
				i.setDurability(ItemProjectile.this.durability);
			Location l = BukkitAdapter.adapt(this.currentLocation.clone().add(this.currentVelocity));
			this.pItem = l.getWorld().dropItem(l, i);
			EntityCacheHandler.add(this.pItem);
			this.pItem.setMetadata(Utils.mpNameVar, new FixedMetadataValue(Main.getPlugin(), null));
			if (!this.targetable)
				this.pItem.setMetadata(Utils.noTargetVar, new FixedMetadataValue(Main.getPlugin(), null));
			this.pItem.setTicksLived(Integer.MAX_VALUE);
			this.pItem.setInvulnerable(true);
			this.pItem.setGravity(false);
			this.pItem.setPickupDelay(Integer.MAX_VALUE);
			Volatile.handler.setItemMotion(this.pItem, l, null);
			Volatile.handler.teleportEntityPacket(this.pItem);
			Volatile.handler.changeHitBox(this.pItem, 0, 0, 0);
		}

		public void modifyVelocity(double v) {
			this.currentVelocity = this.currentVelocity.multiply(v);
		}

		public void modifyPower(float p) {
			this.power *= p;
		}

		public void modifyGravity(float p) {
			this.gravity *= p;
		}

		@SuppressWarnings({ "unchecked" })
		@Override
		public void run() {
			if (this.cancelled) {
				return;
			}
			if (this.am != null && this.am.getEntity().isDead()) {
				this.stop();
				return;
			}
			if (this.startTime + ItemProjectile.this.duration < System.currentTimeMillis()) {
				this.stop();
				return;
			}
			this.oldLocation = this.currentLocation.clone();
			this.currentLocation.add(this.currentVelocity);

			if (ItemProjectile.this.hugSurface) {
				if (this.currentLocation.getBlockX() != this.currentX
						|| this.currentLocation.getBlockZ() != this.currentZ) {
					boolean ok;
					int attempts;
					Block b = BukkitAdapter
							.adapt(this.currentLocation.subtract(0.0, ItemProjectile.this.heightFromSurface, 0.0))
							.getBlock();
					if (BlockUtil.isPathable(b)) {
						attempts = 0;
						ok = false;
						while (attempts++ < 10) {
							if (BlockUtil.isPathable(b = b.getRelative(BlockFace.DOWN))) {
								this.currentLocation.add(0.0, -1.0, 0.0);
								continue;
							}
							ok = true;
							break;
						}
						if (!ok) {
							this.stop();
							return;
						}
					} else {
						attempts = 0;
						ok = false;
						while (attempts++ < 10) {
							b = b.getRelative(BlockFace.UP);
							this.currentLocation.add(0.0, 1.0, 0.0);
							if (!BlockUtil.isPathable(b))
								continue;
							ok = true;
							break;
						}
						if (!ok) {
							this.stop();
							return;
						}
					}
					this.currentLocation
							.setY(((int) this.currentLocation.getY()) + ItemProjectile.this.heightFromSurface);
					this.currentX = this.currentLocation.getBlockX();
					this.currentZ = this.currentLocation.getBlockZ();
				}
			} else if (ItemProjectile.this.projectileGravity != 0.0f) {
				if (ItemProjectile.this.bounce
						&& !BlockUtil.isPathable(BukkitAdapter.adapt(this.currentLocation).getBlock())) {
					if (this.currentBounce < 0.0F) {
						this.stop();
						return;
					}
					this.currentBounce -= this.bounceReduce;
					if (ItemProjectile.this.onBounceSkill.isPresent()
							&& ItemProjectile.this.onBounceSkill.get().isUsable(this.data)) {
						SkillMetadata sData = this.data.deepClone();
						sData.setCaster(this.am);
						sData.setTrigger(sData.getCaster().getEntity());
						sData.setOrigin(this.currentLocation);
						ItemProjectile.this.onBounceSkill.get().execute(sData);
					}
					this.currentVelocity.setY(this.currentBounce / ItemProjectile.this.ticksPerSecond);
				}
				this.currentVelocity.setY(this.currentVelocity.getY()
						- ItemProjectile.this.projectileGravity / ItemProjectile.this.ticksPerSecond);
			}
			if (ItemProjectile.this.stopOnHitGround
					&& !BlockUtil.isPathable(BukkitAdapter.adapt(this.currentLocation).getBlock())) {
				this.stop();
				return;
			}
			if (this.currentLocation.distanceSquared(this.startLocation) >= ItemProjectile.this.maxDistanceSquared) {
				this.stop();
				return;
			}
			Location loc = BukkitAdapter.adapt(this.currentLocation).clone();
			Location eloc = BukkitAdapter.adapt(this.oldLocation).clone();
			Volatile.handler.setItemMotion(this.pItem, eloc, eloc);
			this.pItem.setVelocity(loc.toVector().subtract(eloc.toVector()).multiply(0.5));
			if (this.inRange != null) {
				MythicHitBox hitBox = new MythicHitBox(this.currentLocation, ItemProjectile.this.hitRadius,
						ItemProjectile.this.verticalHitRadius);
				for (AbstractEntity e : this.inRange) {
					if (e.isDead() || !hitBox.contains(e.getLocation().add(0.0, 0.6, 0.0)))
						continue;
					this.targets.add(e);
					this.immune.put(e, System.currentTimeMillis());
					break;
				}
				this.immune.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis() - 2000);
			}
			if (ItemProjectile.this.onTickSkill.isPresent()
					&& ItemProjectile.this.onTickSkill.get().isUsable(this.data)) {
				SkillMetadata sData = this.data.deepClone();
				AbstractLocation location = this.currentLocation.clone();
				HashSet<AbstractLocation> targets = new HashSet<AbstractLocation>();
				targets.add(location);
				sData.setLocationTargets(targets);
				sData.setOrigin(location);
				ItemProjectile.this.onTickSkill.get().execute(sData);
			}
			if (this.targets.size() > 0) {
				this.doHit((HashSet<AbstractEntity>) this.targets.clone());
				if (ItemProjectile.this.stopOnHitEntity) {
					this.stop();
				}
			}
			this.targets.clear();
		}

		private void doHit(HashSet<AbstractEntity> targets) {
			if (ItemProjectile.this.onHitSkill.isPresent()) {
				SkillMetadata sData = this.data.deepClone();
				sData.setEntityTargets(targets);
				sData.setOrigin(this.currentLocation.clone());
				if (ItemProjectile.this.onHitSkill.get().isUsable(sData)) {
					ItemProjectile.this.onHitSkill.get().execute(sData);
				}
			}
		}

		private void stop() {
			if (ItemProjectile.this.onEndSkill.isPresent()
					&& ItemProjectile.this.onEndSkill.get().isUsable(this.data)) {
				SkillMetadata sData = this.data.deepClone();
				ItemProjectile.this.onEndSkill.get()
						.execute(sData.setOrigin(this.currentLocation).setLocationTarget(this.currentLocation));
			}
			Main.taskManager.cancelTask(this.taskId);
			this.pItem.remove();
			this.cancelled = true;
		}

		@Override
		public void setCancelled() {
			this.stop();
		}

		@Override
		public boolean getCancelled() {
			return this.cancelled;
		}
	}
}