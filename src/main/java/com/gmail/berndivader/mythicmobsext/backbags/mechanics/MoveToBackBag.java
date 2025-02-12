package com.gmail.berndivader.mythicmobsext.backbags.mechanics;

import java.io.File;
import java.util.List;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.*;
import io.lumine.mythic.api.skills.placeholders.PlaceholderString;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.gmail.berndivader.mythicmobsext.Main;
import com.gmail.berndivader.mythicmobsext.backbags.BackBagHelper;
import com.gmail.berndivader.mythicmobsext.backbags.BackBagInventory;
import com.gmail.berndivader.mythicmobsext.items.HoldingItem;
import com.gmail.berndivader.mythicmobsext.items.WhereEnum;

public class MoveToBackBag extends SkillMechanic implements INoTargetSkill, ITargetedEntitySkill {
	int backbag_slot;
	WhereEnum what;
	boolean override, tag_where;
	PlaceholderString bag_name;
	String meta_name, slot;
	HoldingItem holding;

	public MoveToBackBag(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
		super(manager, file, skill, mlc);
		this.threadSafetyLevel = ThreadSafetyLevel.SYNC_ONLY;

		what = WhereEnum.getWhere(mlc.getString("what", "head"));
		slot = mlc.getString("slot", "-1");
		backbag_slot = mlc.getInteger("bagslot", -1);
		override = mlc.getBoolean("override", true);
		tag_where = mlc.getBoolean("tag", false);
		meta_name = mlc.getString("meta", "");
		bag_name = mlc.getPlaceholderString(new String[] { "title", "name" }, BackBagHelper.str_name);

		holding = new HoldingItem();
		holding.setWhere(what);
		holding.setSlot(slot);
	}

	@Override
	public SkillResult cast(SkillMetadata data) {
		return castAtEntity(data, data.getCaster().getEntity());
	}

	@Override
	public SkillResult castAtEntity(SkillMetadata data, AbstractEntity abstract_entity) {
		if (abstract_entity.isLiving() && BackBagHelper.hasBackBag(abstract_entity.getUniqueId())) {
			HoldingItem holding = this.holding.clone();
			if (holding != null) {
				holding.parseSlot(data, abstract_entity);
				LivingEntity holder = (LivingEntity) abstract_entity.getBukkitEntity();
				BackBagInventory bag = BackBagHelper.getBagInventory(holder.getUniqueId(),
						bag_name.get(data, abstract_entity));
				if (bag == null)
					return SkillResult.ERROR;
				Inventory inventory = bag.getInventory();
				List<ItemStack> stack = HoldingItem.getContents(holding, holder);
				for (int i1 = 0; i1 < stack.size(); i1++) {
					ItemStack old_item = stack.get(i1);
					if (old_item == null)
						continue;
					ItemStack new_item = old_item.clone();
					if (tag_where)
						HoldingItem.tagWhere(holding, new_item);
					int tmp_slot = Math.min(backbag_slot, inventory.getSize());
					if (backbag_slot == -1) {
						if ((tmp_slot = inventory.firstEmpty()) > -1) {
							inventory.addItem(new_item);
							setMetaVariable(old_item, holder, meta_name, tmp_slot);
						}
					} else {
						if (override) {
							inventory.setItem(tmp_slot, new_item);
							setMetaVariable(old_item, holder, meta_name, tmp_slot);
						} else if (inventory.getItem(tmp_slot) == null
								|| inventory.getItem(tmp_slot).getType() == Material.AIR) {
							inventory.setItem(tmp_slot, new_item);
							setMetaVariable(old_item, holder, meta_name, tmp_slot);
						}
					}
				}
			}
		}
		return SkillResult.SUCCESS;
	}

	static void setMetaVariable(ItemStack old_item, LivingEntity holder, String meta_name, int backbag_slot) {
		old_item.setAmount(0);
		old_item.setType(Material.AIR);
		if (meta_name.length() > 0) {
			holder.setMetadata(meta_name, new FixedMetadataValue(Main.getPlugin(), backbag_slot));
		}
	}
}
