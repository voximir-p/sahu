package org.voximir.sahu.items;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;
import org.voximir.sahu.ModSoundEvents;

public class TaurusPT247 extends GunItem {

    public TaurusPT247(Settings settings) {
        super(settings);
    }

    @Override
    public void fire(ServerPlayerEntity player) {
        World world = player.getEntityWorld();

        float pitch = 0.9f + world.random.nextFloat() * 0.2f;
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                ModSoundEvents.TAURUS_PT_247_SHOOT,
                SoundCategory.PLAYERS,
                1.0f,
                pitch
        );
    }
}
