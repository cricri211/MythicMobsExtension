package com.gmail.berndivader.mythicmobsext.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gmail.berndivader.mythicmobsext.utils.Utils;
import io.lumine.mythic.api.adapters.AbstractEntity;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.skills.conditions.IEntityCondition;
import io.lumine.mythic.bukkit.BukkitAdapter;
import io.lumine.mythic.core.items.MythicItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.gmail.berndivader.mythicmobsext.externals.ExternalAnnotation;

@ExternalAnnotation(name = "ownsmythicitem", author = "Seyarada")
public class OwnsMythicItem extends AbstractCustomCondition implements IEntityCondition {
	private ItemStack mythicItem;

	public OwnsMythicItem(String line, MythicLineConfig mlc) {
		super(line, mlc);
		String baseMLC = mlc.getString(new String[] {"item", "i"}, "STONE");
		try {
			Material baseMaterial = Material.valueOf(baseMLC);
			mythicItem = new ItemStack(baseMaterial);
		} catch (Exception e) {
			Optional<MythicItem> t = Utils.mythicmobs.getItemManager().getItem(baseMLC);
            ItemStack item = BukkitAdapter.adapt(t.get().generateItemStack(1));
            mythicItem = item;
		}
	}

	@Override
	public boolean check(AbstractEntity target) {
		Player p = ( (Player) target.getBukkitEntity() );
		List<Boolean> b = new ArrayList<Boolean>();
		
		b.add(p.getInventory().containsAtLeast(mythicItem, 1));
		b.add(p.getEquipment().getItemInOffHand().isSimilar(mythicItem));
		
		for(Boolean i:b) {
			if(i) return i;
		}
		
		return false;
	}
}
