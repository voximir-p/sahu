package org.voximir.sahu.items;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.voximir.sahu.FireMode;
import org.voximir.sahu.FiringManager;
import org.voximir.sahu.ModDataComponents;
import org.voximir.sahu.packets.RecoilS2CPayload;

import java.util.Optional;

/**
 * Base gun item with data-driven configuration via {@link GunProperties}.
 * Provides a default hitscan firing implementation. Subclasses can override
 * {@link #fire(ServerPlayerEntity)} for custom behavior (e.g. projectile guns).
 */
public class GunItem extends Item {

    protected final GunProperties properties;

    public GunItem(Settings settings, GunProperties properties) {
        super(settings);
        this.properties = properties;
    }

    public GunProperties getProperties() {
        return properties;
    }

    // ── Convenience accessors ────────────────────────────────────────────

    public int getMaxAmmo() {
        return properties.maxAmmo();
    }

    public int getFireRate() {
        return properties.fireRate();
    }

    // ── Ammo helpers (stack-based) ───────────────────────────────────────

    public int getAmmo(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.AMMO, getMaxAmmo());
    }

    public void setAmmo(ItemStack stack, int value) {
        stack.set(ModDataComponents.AMMO, value);
    }

    // ── Fire mode helpers ────────────────────────────────────────────────

    public FireMode getFireMode(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FIRE_MODE, FireMode.FULL_AUTO);
    }

    public void setFireMode(ItemStack stack, FireMode mode) {
        stack.set(ModDataComponents.FIRE_MODE, mode);
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

        if (player.getItemCooldownManager().isCoolingDown(stack)) return false;

        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            World world = player.getEntityWorld();
            world.playSoundFromEntity(
                    null, player,
                    properties.sounds().emptySound(),
                    SoundCategory.PLAYERS,
                    1.0f, 1.0f
            );
            emitGunGameEvent(player, GameEvent.ITEM_INTERACT_START);
            FiringManager.stopFiring(player.getUuid());
            return false;
        }

        fire(player);
        setAmmo(stack, ammo - 1);

        ServerPlayNetworking.send(player, new RecoilS2CPayload(
                properties.recoilPitch(), properties.recoilYaw(), properties.recoilDuration()
        ));

        if (getFireMode(stack) == FireMode.SINGLE) {
            FiringManager.stopFiring(player.getUuid());
        }

        return true;
    }

    // ── Default hitscan fire implementation ──────────────────────────────

    protected void fire(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();

        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(properties.range()));

        // Block raycast (passes through non-solid blocks)
        BlockHitResult blockHit = raycastFiltered(world, start, end, direction, player);

        // Entity raycast
        Box searchBox = player.getBoundingBox()
                .stretch(direction.multiply(properties.range()))
                .expand(1.0);

        Entity closestEntity = null;
        Vec3d closestEntityHitPos = null;
        double closestEntityDistSq = properties.range() * properties.range();

        for (Entity entity : world.getOtherEntities(player, searchBox, e -> !e.isSpectator() && e.canHit())) {
            Box entityBox = entity.getBoundingBox().expand(0.3);
            Optional<Vec3d> hitPos = entityBox.raycast(start, end);

            if (hitPos.isPresent()) {
                double distSq = start.squaredDistanceTo(hitPos.get());
                if (distSq < closestEntityDistSq) {
                    closestEntityDistSq = distSq;
                    closestEntity = entity;
                    closestEntityHitPos = hitPos.get();
                }
            }
        }

        // Choose closest hit (entity vs block)
        Vec3d hitPos = blockHit.getPos();
        double blockDistSq = start.squaredDistanceTo(blockHit.getPos());

        if (closestEntity != null && closestEntityDistSq < blockDistSq) {
            hitPos = closestEntityHitPos;

            if (closestEntity instanceof LivingEntity living) {
                float damage = properties.baseDamage();

                // Headshot detection — top 25% of bounding box
                Box box = living.getBoundingBox();
                double headThreshold = box.minY + box.getLengthY() * 0.75;
                if (closestEntityHitPos != null && closestEntityHitPos.y >= headThreshold) {
                    damage *= properties.headshotMultiplier();
                    player.sendMessage(Text.literal("\u00A7c\u2726 Headshot!"), true);

                    world.playSoundFromEntity(
                            null, player,
                            SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                            SoundCategory.PLAYERS,
                            0.8f, 2.0f
                    );
                }

                living.damage(world, player.getDamageSources().playerAttack(player), damage);
            }
        }

        // Impact particles
        if (hitPos != null) {
            world.spawnParticles(
                    ParticleTypes.CRIT,
                    hitPos.x, hitPos.y, hitPos.z,
                    5, 0.1, 0.1, 0.1, 0.01
            );
        }

        // Muzzle smoke
        world.spawnParticles(
                ParticleTypes.SMOKE,
                player.getX(), player.getEyeY(), player.getZ(),
                3, 0.05, 0.05, 0.05, 0.01
        );

        // Sound
        world.playSoundFromEntity(
                null, player,
                properties.sounds().shootSound(),
                SoundCategory.PLAYERS,
                1.0f,
                0.9f + world.random.nextFloat() * 0.2f
        );
        emitGunGameEvent(player, GameEvent.PROJECTILE_SHOOT);
    }

    // ── Switch mode ──────────────────────────────────────────────────────

    public void switchMode(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        FireMode current = getFireMode(stack);
        FireMode next = current.next();
        setFireMode(stack, next);

        player.sendMessage(
                Text.literal("Firing mode: ")
                        .formatted(Formatting.GRAY)
                        .append(Text.literal(next.getDisplayName()).formatted(Formatting.YELLOW)),
                true
        );

        World world = player.getEntityWorld();
        world.playSoundFromEntity(
                null, player,
                properties.sounds().switchSound(),
                SoundCategory.PLAYERS,
                1.0f, 1.0f
        );
        emitGunGameEvent(player, GameEvent.ITEM_INTERACT_START);
        player.getItemCooldownManager().set(stack, 1);
    }

    // ── Reload ───────────────────────────────────────────────────────────

    public void tryReload(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        World world = player.getEntityWorld();

        if (player.getItemCooldownManager().isCoolingDown(stack) || getAmmo(stack) == getMaxAmmo())
            return;

        boolean hasAmmo = getAmmo(stack) > 0;

        world.playSoundFromEntity(
                null, player,
                properties.sounds().getReloadSound(hasAmmo),
                SoundCategory.PLAYERS,
                1.0f, 1.0f
        );
        emitGunGameEvent(player, GameEvent.ITEM_INTERACT_FINISH);

        setReloading(stack, true);
        player.getItemCooldownManager().set(stack, properties.getReloadDuration(hasAmmo));
    }

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
}
