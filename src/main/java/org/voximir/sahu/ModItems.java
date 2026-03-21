package org.voximir.sahu;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {

    public static Item TAURUS_PT_24_7 = register("taurus_pt_24_7", Item::new, new Item.Settings());

    public static void initialize() {

    }

    public static <T extends Item> T register(String name, Function<Item.Settings, T> itemFactory, Item.Settings settings) {
        Identifier itemId = Identifier.of(Sahu.MOD_ID, name);
        return Registry.register(Registries.ITEM, itemId, itemFactory.apply(settings));
    }
}
