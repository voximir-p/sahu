package org.voximir.sahu;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import static org.voximir.sahu.Sahu.LOGGER;
import static org.voximir.sahu.Sahu.MOD_ID;

public class ModItemGroups {

    public static final RegistryKey<ItemGroup> SAHU_CREATIVE_TAB_KEY = keyOf("sahu_creative_tab");
    public static final ItemGroup SAHU_CREATIVE_TAB = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.TAURUS_PT_24_7))
            .displayName(Text.translatable("itemGroup.sahu_creative_tab"))
            .entries((params, output) -> {
                output.add(new ItemStack(ModItems.TAURUS_PT_24_7));
            })
            .build();

    private static RegistryKey<ItemGroup> keyOf(String id) {
        return RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, id));
    }

    public static void initialize() {
        Registry.register(Registries.ITEM_GROUP, SAHU_CREATIVE_TAB_KEY, SAHU_CREATIVE_TAB);

        LOGGER.info("ModItemGroups initialized");
    }
}
