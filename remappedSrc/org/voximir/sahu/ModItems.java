package org.voximir.sahu;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import org.voximir.sahu.items.TaurusPT247Item;

import java.util.function.Function;

import static org.voximir.sahu.Sahu.LOGGER;

public class ModItems {

    public static final Item TAURUS_PT_24_7 = register("taurus_pt_24_7", TaurusPT247Item::new, new Item.Properties().stacksTo(1));
    public static final Item NINE_MM = register("9mm", Item::new, new Item.Properties());

    private static ResourceKey<Item> keyOf(String id) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Sahu.MOD_ID, id));
    }

    private static void addToCreativeTabs(Item item, ResourceKey<CreativeModeTab> tab) {
        ItemGroupEvents.modifyEntriesEvent(tab)
                .register((itemGroup) -> itemGroup.accept(item));
    }

    public static <T extends Item> T register(String id, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
        ResourceKey<Item> key = keyOf(id);
        T item = itemFactory.apply(settings.setId(key));

        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        addToCreativeTabs(TAURUS_PT_24_7, ModItemGroups.SAHU_CREATIVE_TAB_KEY);
        addToCreativeTabs(NINE_MM, ModItemGroups.SAHU_CREATIVE_TAB_KEY);

        LOGGER.info("ModItems initialized");
    }
}
