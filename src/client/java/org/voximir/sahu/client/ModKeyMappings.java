package org.voximir.sahu;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

import static org.voximir.sahu.Sahu.MOD_ID;

public class ModKeyMappings {

    public static final KeyBinding.Category SAHU_CATEGORY = new KeyBinding.Category(Identifier.of(MOD_ID, "sahu_category"));

    public static final KeyBinding RELOAD_KEYBIND = registerKeyBindings("key.sahu.reload", InputUtil.GLFW_KEY_R);
    public static final KeyBinding SWITCH_KEYBIND = registerKeyBindings("key.sahu.switch", InputUtil.GLFW_KEY_B);

    private static KeyBinding registerKeyBindings(String path, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                    path,
                    InputUtil.Type.KEYSYM,
                    defaultKey,
                    SAHU_CATEGORY
            ));
    }

    public static void initialize() {
        Sahu.LOGGER.info("ModKeyMappings initialized");
    }
}
