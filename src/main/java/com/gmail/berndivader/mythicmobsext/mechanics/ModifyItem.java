package com.gmail.berndivader.mythicmobsext.mechanics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.ITargetedEntitySkill;
import io.lumine.mythic.api.skills.SkillMetadata;
import io.lumine.mythic.api.skills.SkillResult;
import io.lumine.mythic.core.skills.SkillExecutor;
import io.lumine.mythic.core.skills.SkillMechanic;
import io.lumine.mythic.core.skills.SkillString;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.gmail.berndivader.mythicmobsext.Main;
import com.gmail.berndivader.mythicmobsext.externals.*;
import com.gmail.berndivader.mythicmobsext.items.Enchant;
import com.gmail.berndivader.mythicmobsext.items.ModdingItem;
import com.gmail.berndivader.mythicmobsext.items.ModdingItem.ACTION;
import com.gmail.berndivader.mythicmobsext.items.WhereEnum;
import com.gmail.berndivader.mythicmobsext.utils.RandomDouble;
import com.gmail.berndivader.mythicmobsext.utils.Utils;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

@ExternalAnnotation(name = "modifyitem", author = "BerndiVader")
public class ModifyItem extends SkillMechanic implements ITargetedEntitySkill {
	ModdingItem modding_item;

	public ModifyItem(SkillExecutor manager, File file, String skill, MythicLineConfig mlc) {
		super(manager, file, skill, mlc);
		this.line = skill;

		WhereEnum where = Utils.enum_lookup(WhereEnum.class, mlc.getString("what", "HAND").toUpperCase());
		ACTION action = Utils.enum_lookup(ACTION.class, mlc.getString("action", "SET").toUpperCase());
		Material material = null;
		RandomDouble amount = null;
		List<Enchant> enchants = null;
		String[] lore = null;
		String name = null, bag_name = null, duration = null;

		String slot = mlc.getString("slot", "-7331");
		String temp = mlc.getString("material");
		JsonElement json_element = null;

		if (temp != null)
			temp = temp.toUpperCase();
		material = Utils.enum_lookup(Material.class, temp);
		temp = SkillString.parseMessageSpecialChars(mlc.getString("lore"));
		if (temp != null)
			lore = (temp.substring(1, temp.length() - 1)).split(",");
		if ((temp = SkillString.parseMessageSpecialChars(mlc.getString("name"))) != null)
			name = temp.substring(1, temp.length() - 1);
		if ((temp = mlc.getString("amount")) != null)
			amount = new RandomDouble(temp);
		if ((temp = mlc.getString("bagname")) != null)
			bag_name = temp;
		if ((temp = mlc.getString("duration")) != null)
			duration = temp;
		if ((temp = mlc.getString("enchants", null)) != null) {
			String[] arr1 = temp.toUpperCase().split(",");
			int length = arr1.length;
			if (length > 0)
				enchants = new ArrayList<>();
			for (int i1 = 0; i1 < length; i1++) {
				String parse[] = arr1[i1].split(":");
				temp = parse[0];
				Enchantment ench;
				String level = "1";
				if (parse.length > 0) {
					try {
						level = parse[1];
					} catch (Exception ex) {
						Main.logger.warning("Error parsing level in " + line);
						level = "1";
					}
				}
				if ((ench = Enchantment.getByName(name)) != null) {
					enchants.add(new Enchant(ench, level));
				} else {
					Main.logger.warning("Ignore enchantment " + name + " in " + line);
				}
			}
		}
		if ((temp = mlc.getString("nbt", null)) != null) {
			temp = SkillString.parseMessageSpecialChars(temp.substring(1, temp.length() - 1));
			json_element = new JsonParser().parse(temp);
		}
		modding_item = new ModdingItem(where, slot, action, material, lore, name, amount, enchants, duration, bag_name,
				json_element);
	}

	@Override
	public SkillResult castAtEntity(SkillMetadata data, AbstractEntity target) {
		if (target.isLiving()) {
			LivingEntity entity = (LivingEntity) target.getBukkitEntity();
			ItemStack item_stack = modding_item.getItemStackByWhere(data, target, entity);
			if (item_stack != null)
				item_stack = modding_item.applyMods(data, target, item_stack);
			if(target.getBukkitEntity() instanceof Player) {
				((Player)target.getBukkitEntity()).updateInventory();
			}
			return SkillResult.SUCCESS;
		}
		return SkillResult.CONDITION_FAILED;
	}
}