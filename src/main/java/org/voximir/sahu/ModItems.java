package org.voximir.sahu;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.voximir.sahu.items.TaurusPT247Item;

import java.util.function.Function;

import static org.voximir.sahu.Sahu.LOGGER;

public class ModItems {

    public static final Item TAURUS_PT_24_7 = register("taurus_pt_24_7", TaurusPT247Item::new, new Item.Settings().maxCount(1));

    private static RegistryKey<Item> keyOf(String id) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(Sahu.MOD_ID, id));
    }

    private static void addToCreativeTabs(Item item, RegistryKey<ItemGroup> tab) {
        ItemGroupEvents.modifyEntriesEvent(tab)
                .register((itemGroup) -> itemGroup.add(item));
    }

    public static <T extends Item> T register(String id, Function<Item.Settings, T> itemFactory, Item.Settings settings) {
        RegistryKey<Item> key = keyOf(id);
        T item = itemFactory.apply(settings.registryKey(key));

        return Registry.register(Registries.ITEM, key, item);
    }

    public static void initialize() {
        addToCreativeTabs(TAURUS_PT_24_7, ModItemGroups.SAHU_CREATIVE_TAB_KEY);

        LOGGER.info("ModItems initialized");
    }
}
