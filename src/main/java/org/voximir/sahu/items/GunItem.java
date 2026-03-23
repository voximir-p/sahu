package org.voximir.sahu.items;

import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StainedGlassPaneBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.voximir.sahu.FireMode;
import org.voximir.sahu.FiringManager;
import org.voximir.sahu.ModDataComponents;
import org.voximir.sahu.packets.RecoilS2CPayload;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Base gun item with data-driven configuration via {@link GunProperties}.
 * Implements {@link GeoItem} for GeckoLib-powered 3D rendering and animation.
 * Provides a default hitscan firing implementation. Subclasses can override
 * {@link #fire(ServerPlayerEntity)} for custom behavior (e.g. projectile guns).
 */
public class GunItem extends Item implements GeoItem {

    // ── GeckoLib animation definitions ───────────────────────────────────

    private static final RawAnimation SHOOT_ANIM = RawAnimation.begin().thenPlay("shoot");
    private static final RawAnimation RELOAD_ANIM = RawAnimation.begin().thenPlay("reload");
    private static final RawAnimation MAGIN_ANIM = RawAnimation.begin().thenPlay("magin");
    private static final RawAnimation AIM_ANIM = RawAnimation.begin().thenPlay("aim");
    private static final RawAnimation UNAIM_ANIM = RawAnimation.begin().thenPlay("unaim");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected final GunProperties properties;

    public GunItem(Settings settings, GunProperties properties) {
        super(settings);
        this.properties = properties;
        GeoItem.registerSyncedAnimatable(this);
    }

    // ── GeoItem implementation ───────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GunItem>("gun_controller", 2, state -> PlayState.STOP)
                .triggerableAnim("shoot", SHOOT_ANIM)
                .triggerableAnim("reload", RELOAD_ANIM)
                .triggerableAnim("magin", MAGIN_ANIM));
        controllers.add(new AnimationController<GunItem>("aim_controller", 0, state -> PlayState.STOP)
            .triggerableAnim("aim", AIM_ANIM)
            .triggerableAnim("unaim", UNAIM_ANIM));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private GeoItemRenderer<GunItem> renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (this.renderer == null)
                    this.renderer = new GeoItemRenderer<>(GunItem.this);
                return this.renderer;
            }
        });
    }

    public GunProperties getProperties() {
        return properties;
    }

    public void triggerAimAnimation(Entity holder, ItemStack stack) {
        triggerAnim(holder, GeoItem.getId(stack), "aim_controller", "aim");
    }

    public void triggerUnaimAnimation(Entity holder, ItemStack stack) {
        long instanceId = GeoItem.getId(stack);
        stopTriggeredAnim(holder, instanceId, "aim_controller", "aim");
        triggerAnim(holder, instanceId, "aim_controller", "unaim");
    }

    public void startAim(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) player.getEntityWorld());

        setAiming(stack, true);
        stopTriggeredAnim(player, instanceId, "aim_controller", "unaim");
        triggerAnim(player, instanceId, "aim_controller", "aim");
    }

    public void stopAim(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        long instanceId = GeoItem.getOrAssignId(stack, (ServerWorld) player.getEntityWorld());

        setAiming(stack, false);
        stopTriggeredAnim(player, instanceId, "aim_controller", "aim");
        triggerAnim(player, instanceId, "aim_controller", "unaim");
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

    public boolean isAiming(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.AIMING, false);
    }

    public void setAiming(ItemStack stack, boolean value) {
        stack.set(ModDataComponents.AIMING, value);
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

        // Trigger GeckoLib shoot animation
        triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerWorld) player.getEntityWorld()), "gun_controller", "shoot");

        float yawKick = player.getRandom().nextBoolean()
                ? properties.recoilYaw()
                : -properties.recoilYaw();
        ServerPlayNetworking.send(player, new RecoilS2CPayload(
                properties.recoilPitch(), yawKick, properties.recoilDuration()
        ));

        if (getFireMode(stack) == FireMode.SINGLE) {
            FiringManager.stopFiring(player.getUuid());
        }

        return true;
    }

    // ── Default hitscan fire implementation ──────────────────────────────

    protected void fire(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        ItemStack stack = player.getMainHandStack();

        Vec3d start = player.getEyePos();
        Vec3d direction = applyInaccuracy(player.getRotationVec(1.0f), getCurrentAccuracy(player, stack), world.random);
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

        // Choose the closest hit (entity vs block)
        Vec3d hitPos = blockHit.getPos();
        double blockDistSq = start.squaredDistanceTo(blockHit.getPos());
        boolean entityIsCloser = closestEntity != null && closestEntityDistSq < blockDistSq;

        if (entityIsCloser) {
            hitPos = closestEntityHitPos;
        }

        // Fluid interactions (water splashes + lava stopping)
        Vec3d lavaHit = handleFluidInteractions(world, start, hitPos, direction);
        boolean hitLava = false;

        if (lavaHit != null && start.squaredDistanceTo(lavaHit) < start.squaredDistanceTo(hitPos)) {
            hitPos = lavaHit;
            hitLava = true;
            entityIsCloser = false;
        }

        // Entity damage (skipped if lava blocked the path)
        if (entityIsCloser && closestEntity instanceof LivingEntity living) {
            float damage = properties.baseDamage();

            // Headshot detection — top 25% of bounding box
            Box box = living.getBoundingBox();
            double headThreshold = box.minY + box.getLengthY() * 0.75;
            if (closestEntityHitPos.y >= headThreshold) {
                damage *= properties.headshotMultiplier();
                player.sendMessage(Text.literal("§c✦ Headshot!"), true);

                world.playSoundFromEntity(
                        null, player,
                        SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                        SoundCategory.PLAYERS,
                        0.8f, 2.0f
                );
            }

            living.damage(world, player.getDamageSources().playerAttack(player), damage);
        }

        // Impact particles
        if (hitPos != null) {
            if (hitLava) {
                world.spawnParticles(
                        ParticleTypes.LAVA,
                        hitPos.x, hitPos.y, hitPos.z,
                        2, 0.2, 0.2, 0.2, 0.01
                );
                world.playSound(null, BlockPos.ofFloored(hitPos),
                        SoundEvents.BLOCK_LAVA_EXTINGUISH,
                        SoundCategory.BLOCKS, 0.4f, 1.0f);
            } else {
                world.spawnParticles(
                        ParticleTypes.CRIT,
                        hitPos.x, hitPos.y, hitPos.z,
                        5, 0.1, 0.1, 0.1, 0.01
                );
            }
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

    private float getCurrentAccuracy(ServerPlayerEntity player, ItemStack stack) {
        float baseAccuracy = isAiming(stack) ? properties.aimedAccuracy() : properties.hipFireAccuracy();
        float movementModifier;

        if (player.isSwimming() || player.isSprinting() || !player.isOnGround()) {
            movementModifier = -0.25f;
        } else if (player.isSneaking()) {
            movementModifier = 0.05f;
        } else if (isWalking(player)) {
            movementModifier = -0.10f;
        } else {
            movementModifier = 0.0f;
        }

        return MathHelper.clamp(baseAccuracy + movementModifier, 0.0f, 1.0f);
    }

    private static boolean isWalking(ServerPlayerEntity player) {
        double horizontalSpeedSq = player.getVelocity().horizontalLengthSquared();

        return horizontalSpeedSq > 0.0004;
    }

    private static Vec3d applyInaccuracy(Vec3d direction, float accuracy, net.minecraft.util.math.random.Random random) {
        float clampedAccuracy = MathHelper.clamp(accuracy, 0.0f, 1.0f);
        float inaccuracy = 1.0f - clampedAccuracy;

        if (inaccuracy <= 0.0f) {
            return direction;
        }

        float maxSpreadRadians = (float) Math.toRadians(12.0f * inaccuracy);
        float yawOffset = (random.nextFloat() * 2.0f - 1.0f) * maxSpreadRadians;
        float pitchOffset = (random.nextFloat() * 2.0f - 1.0f) * maxSpreadRadians;

        return direction.rotateY(yawOffset).rotateX(pitchOffset).normalize();
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
        if (!player.getItemCooldownManager().isCoolingDown(stack)) {
            player.getItemCooldownManager().set(stack, 1);
        }
    }

    // ── Reload ───────────────────────────────────────────────────────────

    public void tryReload(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        World world = player.getEntityWorld();

        if (player.getItemCooldownManager().isCoolingDown(stack) || getAmmo(stack) == getMaxAmmo())
            return;

        boolean hasAmmo = getAmmo(stack) > 0;

        // Trigger GeckoLib reload animation (tactical = magin, full = reload)
        triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerWorld) player.getEntityWorld()),
                "gun_controller", hasAmmo ? "magin" : "reload");

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

    // ── Fluid interaction along bullet path ──────────────────────────────

    /**
     * Steps along the bullet path checking for fluid transitions.
     * Spawns splash particles/sounds at water entry/exit points.
     * Returns the position where the bullet enters lava (null if it doesn't).
     */
    private static Vec3d handleFluidInteractions(ServerWorld world, Vec3d start, Vec3d bulletEnd, Vec3d direction) {
        double totalDist = start.distanceTo(bulletEnd);
        double stepSize = 0.25;
        boolean wasInWater = world.getFluidState(BlockPos.ofFloored(start)).isIn(FluidTags.WATER);

        for (double d = stepSize; d <= totalDist; d += stepSize) {
            Vec3d pos = start.add(direction.multiply(d));
            BlockPos blockPos = BlockPos.ofFloored(pos);
            FluidState fluidState = world.getFluidState(blockPos);

            if (fluidState.isIn(FluidTags.LAVA)) {
                return pos;
            }

            boolean inWater = fluidState.isIn(FluidTags.WATER);
            if (inWater != wasInWater) {
                world.spawnParticles(
                        ParticleTypes.SPLASH,
                        pos.x, pos.y, pos.z,
                        8, 0.15, 0.05, 0.15, 0.05
                );
                world.playSound(null, blockPos,
                        SoundEvents.ENTITY_GENERIC_SPLASH,
                        SoundCategory.BLOCKS, 0.6f, 1.4f);
            }
            wasInWater = inWater;
        }
        return null;
    }

    // ── Bullet raycast ───────────────────────────────────────────────────
    // Uses COLLIDER shapes so bullets respect actual block geometry:
    //   - Grass/flowers/torches: no collider → bullet passes through
    //   - Fences: post/rail collider with gaps → bullet passes through holes
    //   - Glass: full collider → bullet shatters the glass and continues
    //   - Solid blocks: bullet stops

    protected static BlockHitResult raycastFiltered(ServerWorld world, Vec3d start, Vec3d end, Vec3d direction, Entity shooter) {
        BlockHitResult result = world.raycast(new RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                shooter
        ));

        int iterations = 200;
        while (iterations-- > 0 && result.getType() != HitResult.Type.MISS) {
            BlockPos hitBlock = result.getBlockPos();
            BlockState state = world.getBlockState(hitBlock);

            if (isBulletBreakableGlass(state)) {
                world.breakBlock(hitBlock, false, shooter);
            } else {
                return result;
            }

            Vec3d past = result.getPos().add(direction.multiply(0.01));
            result = world.raycast(new RaycastContext(
                    past, end,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    shooter
            ));
        }

        return result;
    }

    private static boolean isBulletBreakableGlass(BlockState state) {
        // Full glass blocks (clear, stained, tinted) — IMPERMEABLE tag covers these, minus ice
        if (state.isIn(BlockTags.IMPERMEABLE) && !state.isOf(Blocks.ICE)) {
            return true;
        }
        // Glass panes (but not iron bars or copper panes which also extend PaneBlock)
        return state.getBlock() instanceof StainedGlassPaneBlock
                || state.isOf(Blocks.GLASS_PANE);
    }
}
