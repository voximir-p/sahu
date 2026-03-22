package org.voximir.sahu.items;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.voximir.sahu.FiringManager;
import org.voximir.sahu.ModDataComponents;
import org.voximir.sahu.packets.RecoilS2CPayload;

public abstract class GunItem extends Item {

    public static final int MODE_FULL_AUTO = 0;
    public static final int MODE_SINGLE = 1;

    public GunItem(Settings settings) {
        super(settings);
    }

    // ── Ammo helpers (stack-based, server only) ──────────────────────────

    public int getAmmo(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.AMMO, getMaxAmmo());
    }

    public void setAmmo(ItemStack stack, int value) {
        stack.set(ModDataComponents.AMMO, value);
    }

    // ── Fire mode helpers ────────────────────────────────────────────────

    public int getFireMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FIRE_MODE, MODE_FULL_AUTO);
    }

    public void setFireMode(ItemStack stack, int mode) {
        stack.set(ModDataComponents.FIRE_MODE, mode);
    }

    public boolean isSingleFire(ItemStack stack) {
        return getFireMode(stack) == MODE_SINGLE;
    }

    // ── Reload helpers ───────────────────────────────────────────────────

    public boolean isReloading(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.RELOADING, false);
    }

    public void setReloading(ItemStack stack, boolean value) {
        stack.set(ModDataComponents.RELOADING, value);
    }

    // ── Game event emission (Warden / sculk) ─────────────────────────────

    protected void emitGunGameEvent(ServerPlayerEntity player, RegistryEntry<GameEvent> event) {
        player.getEntityWorld().emitGameEvent(
                event,
                player.getEyePos(),
                GameEvent.Emitter.of(player)
        );
    }

    // ── Firing logic ─────────────────────────────────────────────────────

    public boolean tryFire(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();

        // Blocked by reload cooldown
        if (player.getItemCooldownManager().isCoolingDown(stack)) return false;

        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            World world = player.getEntityWorld();
            // Just play the empty click — no HUD or action bar message
            world.playSoundFromEntity(
                    null, player,
                    getEmptySound(),
                    SoundCategory.PLAYERS,
                    1.0f, 1.0f
            );
            emitGunGameEvent(player, GameEvent.ITEM_INTERACT_START);
            FiringManager.stopFiring(player.getUuid());
            return false;
        }

        fire(player);
        setAmmo(stack, ammo - 1);

        // Send recoil to the shooter's client
        ServerPlayNetworking.send(player, new RecoilS2CPayload(
                getRecoilPitch(), getRecoilYaw(), getRecoilDuration()
        ));

        // In single-fire mode, stop after one shot
        if (isSingleFire(stack)) {
            FiringManager.stopFiring(player.getUuid());
        }

        return true;
    }

    // ── Switch mode (shared for all guns) ────────────────────────────────

    public void switchMode(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        int current = getFireMode(stack);
        int next = (current == MODE_FULL_AUTO) ? MODE_SINGLE : MODE_FULL_AUTO;
        setFireMode(stack, next);

        String modeName = (next == MODE_SINGLE) ? "Single" : "Full Auto";
        player.sendMessage(
                Text.literal("Firing mode: ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal(modeName).formatted(Formatting.YELLOW)),
                true
        );

        World world = player.getEntityWorld();
        world.playSoundFromEntity(
                null, player,
                getSwitchSound(),
                SoundCategory.PLAYERS,
                1.0f, 1.0f
        );
        emitGunGameEvent(player, GameEvent.ITEM_INTERACT_START);
        player.getItemCooldownManager().set(stack, 1);
    }

    // ── Reload (shared for all guns) ─────────────────────────────────────

    public void tryReload(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        World world = player.getEntityWorld();

        if (player.getItemCooldownManager().isCoolingDown(stack) || getAmmo(stack) == getMaxAmmo())
            return;

        boolean hasAmmo = getAmmo(stack) > 0;
        SoundEvent sound = getReloadSound(hasAmmo);
        int cooldown = getReloadDuration(hasAmmo);

        world.playSoundFromEntity(
                null, player,
                sound,
                SoundCategory.PLAYERS,
                1.0f, 1.0f
        );
        emitGunGameEvent(player, GameEvent.ITEM_INTERACT_FINISH);

        // Mark as reloading — ammo refills when cooldown ends
        setReloading(stack, true);
        player.getItemCooldownManager().set(stack, cooldown);
    }

    /**
     * Called by FiringManager each tick for all players holding a gun.
     * Completes the reload when the cooldown expires.
     */
    public void tickReloadCompletion(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (isReloading(stack) && !player.getItemCooldownManager().isCoolingDown(stack)) {
            setAmmo(stack, getMaxAmmo());
            setReloading(stack, false);
        }
    }

    // ── Bullet raycast (skips transparent / non-solid blocks) ────────────

    protected static BlockHitResult raycastFiltered(ServerWorld world, Vec3d start, Vec3d end, Vec3d direction, Entity shooter) {
        BlockHitResult result = world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                shooter
        ));

        int iterations = 200;
        while (iterations-- > 0 && result.getType() != HitResult.Type.MISS) {
            BlockPos hitBlock = result.getBlockPos();
            BlockState state = world.getBlockState(hitBlock);

            if (state.isSolidBlock(world, hitBlock)) {
                return result;
            }

            // Advance just past the hit and re-raycast
            Vec3d past = result.getPos().add(direction.multiply(0.01));
            result = world.raycast(new RaycastContext(
                    past, end,
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    shooter
            ));
        }

        return result;
    }

    // ── Abstract contract ────────────────────────────────────────────────

    protected abstract void fire(ServerPlayerEntity player);

    public abstract int getFireRate();

    public abstract int getMaxAmmo();

    protected abstract float getRecoilPitch();

    protected abstract float getRecoilYaw();

    protected abstract int getRecoilDuration();

    protected abstract SoundEvent getEmptySound();

    protected abstract SoundEvent getSwitchSound();

    /** @param hasAmmo true if the magazine still has rounds (tactical reload) */
    protected abstract SoundEvent getReloadSound(boolean hasAmmo);

    /** @param hasAmmo true if the magazine still has rounds (shorter reload) */
    protected abstract int getReloadDuration(boolean hasAmmo);
}
