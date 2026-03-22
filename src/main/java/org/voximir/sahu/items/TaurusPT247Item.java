package org.voximir.sahu.items;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;
import org.voximir.sahu.ModSoundEvents;

import java.util.Optional;

public class TaurusPT247Item extends GunItem {

    private static final double RANGE = 50.0;
    private static final float BASE_DAMAGE = 8.0f;
    private static final float HEADSHOT_MULTIPLIER = 2.0f;

    public TaurusPT247Item(Settings settings) {
        super(settings);
    }

    // ── GunItem contract ─────────────────────────────────────────────────

    @Override public int getFireRate() { return 2; }
    @Override public int getMaxAmmo() { return 17; }
    @Override protected float getRecoilPitch() { return -12.0f; }
    @Override protected float getRecoilYaw() { return 2.5f; }
    @Override protected int getRecoilDuration() { return 5; }

    @Override protected SoundEvent getEmptySound() { return ModSoundEvents.TAURUS_PT_2_47_EMPTY; }
    @Override protected SoundEvent getSwitchSound() { return ModSoundEvents.TAURUS_PT_2_47_SWITCH; }

    @Override
    protected SoundEvent getReloadSound(boolean hasAmmo) {
        return hasAmmo ? ModSoundEvents.TAURUS_PT_2_47_MAGIN : ModSoundEvents.TAURUS_PT_2_47_RELOAD;
    }

    @Override
    protected int getReloadDuration(boolean hasAmmo) {
        return hasAmmo ? 19 : 30;
    }

    // ── Shooting ─────────────────────────────────────────────────────────

    @Override
    protected void fire(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();

        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(RANGE));

        // 1. Block raycast (passes through non-solid blocks)
        BlockHitResult blockHit = raycastFiltered(world, start, end, direction, player);

        // 2. Entity raycast (manual)
        Box searchBox = player.getBoundingBox()
                .stretch(direction.multiply(RANGE))
                .expand(1.0);

        Entity closestEntity = null;
        Vec3d closestEntityHitPos = null;
        double closestEntityDistSq = RANGE * RANGE;

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

        // 3. Choose closest hit (entity vs block)
        Vec3d hitPos = blockHit.getPos();
        double blockDistSq = start.squaredDistanceTo(blockHit.getPos());

        if (closestEntity != null && closestEntityDistSq < blockDistSq) {
            hitPos = closestEntityHitPos;

            if (closestEntity instanceof LivingEntity living) {
                float damage = BASE_DAMAGE;

                // Headshot detection — top 25 % of bounding box
                Box box = living.getBoundingBox();
                double headThreshold = box.minY + box.getLengthY() * 0.75;
                if (closestEntityHitPos != null && closestEntityHitPos.y >= headThreshold) {
                    damage *= HEADSHOT_MULTIPLIER;
                    player.sendMessage(Text.literal("§c✦ Headshot!"), true);

                    // Subtle headshot indicator sound (XP orb, low volume, high pitch)
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

        // Sound — entity-attached so it tracks the player
        world.playSoundFromEntity(
                null, player,
                ModSoundEvents.TAURUS_PT_2_47_SHOOT,
                SoundCategory.PLAYERS,
                1.0f,
                0.9f + world.random.nextFloat() * 0.2f
        );
        emitGunGameEvent(player, GameEvent.PROJECTILE_SHOOT);
    }
}
