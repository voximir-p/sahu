package org.voximir.sahu;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import static org.voximir.sahu.Sahu.LOGGER;
import static org.voximir.sahu.Sahu.MOD_ID;

public class ModItemGroups {

    public static final ResourceKey<CreativeModeTab> SAHU_CREATIVE_TAB_KEY = keyOf("sahu_creative_tab");
    public static final CreativeModeTab SAHU_CREATIVE_TAB = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.TAURUS_PT_24_7))
            .title(Component.translatable("itemGroup.sahu_creative_tab"))
            .displayItems((params, output) -> {
                output.accept(new ItemStack(ModItems.TAURUS_PT_24_7));
            })
            .build();

    private static ResourceKey<CreativeModeTab> keyOf(String id) {
        return ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(MOD_ID, id));
    }

    public static void initialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SAHU_CREATIVE_TAB_KEY, SAHU_CREATIVE_TAB);

        LOGGER.info("ModItemGroups initialized");
    }
}
