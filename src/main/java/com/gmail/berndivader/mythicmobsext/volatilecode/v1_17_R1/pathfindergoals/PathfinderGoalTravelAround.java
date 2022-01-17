package com.gmail.berndivader.mythicmobsext.volatilecode.v1_17_R1.pathfindergoals;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Optional;
import java.util.AbstractMap.SimpleEntry;

import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;

import net.minecraft.world.entity.EntityInsentient;
import net.minecraft.world.entity.ai.goal.PathfinderGoal;
import net.minecraft.world.entity.ai.navigation.Navigation;
import net.minecraft.world.entity.ai.navigation.NavigationAbstract;
import net.minecraft.world.entity.ai.navigation.NavigationFlying;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.event.entity.EntityTeleportEvent;

import com.gmail.berndivader.mythicmobsext.utils.Utils;

import io.lumine.xikage.mythicmobs.mobs.ActiveMob;

public class PathfinderGoalTravelAround extends PathfinderGoal {

	ArrayList<SimpleEntry<Vec3D, Boolean>> travelpoints;

	private final EntityInsentient d;
	private Vec3D v;
	private Optional<ActiveMob> mM;
	private Vec3D aV;
	private final double f;
	private final double mR, tR;
	World a;
	private final NavigationAbstract g;
	private int h, travel_index;
	float b;
	float c;
	private float i;
	private boolean iF, iT;

	public PathfinderGoalTravelAround(EntityInsentient entity, double d0, double mR, double tR, boolean iT) {
		this.d = entity;
		this.f = d0;
		this.a = entity.getWorld();
		g = entity.getNavigation();
		this.travelpoints = new ArrayList<>();
		this.travel_index = 0;
		this.v = nextCheckPoint();
		a(EnumSet.of(PathfinderGoal.Type.a, PathfinderGoal.Type.b));
		this.mR = mR;
		this.tR = tR;
		this.iF = false;
		this.iT = iT;
		if ((!(entity.getNavigation() instanceof Navigation))
				&& (!(entity.getNavigation() instanceof NavigationFlying))) {
			throw new IllegalArgumentException("Unsupported mob type for TravelAroundGoal");
		}
		this.mM = Utils.mobmanager.getActiveMob(entity.getUniqueID());
	}

	@Override
	public boolean a() {
		this.aV = new Vec3D(d.locX(), d.locY(), d.locZ());
		if (this.v != null) {
			if (this.iT || this.d.getGoalTarget() == null || !this.d.getGoalTarget().isAlive()) {
				double ds = v.distanceSquared(this.aV);
				if (ds > this.mR) {
					return true;
				} else if (this.iF && ds > 2.0D) {
					return true;
				} else {
					this.v = nextCheckPoint();
					if (this.iF) {
						if (this.mM.isPresent()) {
							ActiveMob am = this.mM.get();
							am.signalMob(null, v == null ? Utils.signal_GOAL_TRAVELEND : Utils.signal_GOAL_TRAVELPOINT);
						}
						this.iF = false;
					}
				}
			}
		} else {
			this.v = nextCheckPoint();
			if (this.iF) {
				if (this.mM.isPresent()) {
					ActiveMob am = this.mM.get();
					am.signalMob(null, v == null ? Utils.signal_GOAL_TRAVELEND : Utils.signal_GOAL_TRAVELPOINT);
				}
				this.iF = false;
			}
		}
		return false;
	}

	@Override
	public boolean b() {
		return (!g.n()) && v.distanceSquared(this.aV) > 2.0D;
	}

	@Override
	public void c() {
		h = 0;
		i = d.a(PathType.i); // WATER
		d.a(PathType.i, 0.0F);
		if (!this.iF) {
			if (this.mM.isPresent()) {
				ActiveMob am = this.mM.get();
				am.signalMob(null, "GOAL_TRAVELSTART");
			}
		}
		this.iF = true;
	}

	@Override
	public void d() {
		g.q();
		d.a(PathType.i, i); // WATER
		if (v.distanceSquared(this.aV) < 10.0D) {
			this.iF = false;
			this.v = null;
		}
	}

	@Override
	public void e() {
		d.getControllerLook().a(v.getX(), v.getY(), v.getZ(), 10.0F, d.fa());
		if (h-- <= 0) {
			h = 10;
			if (!g.a(v.getX(), v.getY(), v.getZ(), f) && (!d.isLeashed()) && (!d.isPassenger())
					&& v.distanceSquared(this.aV) > this.tR) {
				CraftEntity entity = d.getBukkitEntity();
				Location to = new Location(entity.getWorld(), v.getX(), v.getY(), v.getZ(), d.getYRot(), d.getXRot());
				EntityTeleportEvent event = new EntityTeleportEvent(entity, entity.getLocation(), to);
				d.getWorld().getCraftServer().getPluginManager().callEvent(event);
				if (event.isCancelled())
					return;
				to = event.getTo();
				d.setPositionRotation(to.getX(), to.getY(), to.getZ(), to.getYaw(), to.getPitch());
				g.q();
				return;
			}
		}
	}

	protected boolean a(int i, int j, int k, int l, int i1) {
		BlockPosition blockposition = new BlockPosition(i + l, k - 1, j + i1);
		IBlockData iblockdata = a.getType(blockposition);
		return (iblockdata.c(a, blockposition, EnumDirection.a) == 1) && (a.isEmpty(blockposition.up())) // DOWN
				&& (a.isEmpty(blockposition.up(2)));
	}

	protected Vec3D nextCheckPoint() {
		int size = this.travelpoints.size();
		Vec3D vector = null;
		if (size > 0) {
			if (travel_index >= size)
				travel_index = 0;
			SimpleEntry<Vec3D, Boolean> entry = travelpoints.get(travel_index);
			vector = entry.getKey();
			if (entry.getValue())
				travelpoints.remove(travel_index);
			travel_index++;
		}
		return vector;
	}

	public void addTravelPoint(com.gmail.berndivader.mythicmobsext.utils.Vec3D vector, boolean remove) {
		travelpoints
				.add(new SimpleEntry<>(new Vec3D(vector.getX(), vector.getY(), vector.getZ()), remove));
	}

	public void clearTravelPoints() {
		travelpoints = new ArrayList<>();
	}

}