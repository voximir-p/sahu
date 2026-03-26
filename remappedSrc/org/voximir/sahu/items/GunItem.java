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
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
 * {@link #fire(ServerPlayer)} for custom behavior (e.g. projectile guns).
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

    public GunItem(Properties settings, GunProperties properties) {
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

    public void startAim(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) player.level());

        setAiming(stack, true);
        stopTriggeredAnim(player, instanceId, "aim_controller", "unaim");
        triggerAnim(player, instanceId, "aim_controller", "aim");
    }

    public void stopAim(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        long instanceId = GeoItem.getOrAssignId(stack, (ServerLevel) player.level());

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

    protected void emitGunGameEvent(ServerPlayer player, Holder<GameEvent> event) {
        player.level().gameEvent(
                event,
                player.getEyePosition(),
                GameEvent.Context.of(player)
        );
    }

    // ── Firing logic ─────────────────────────────────────────────────────

    public boolean tryFire(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();

        if (player.getCooldowns().isOnCooldown(stack)) return false;

        int ammo = getAmmo(stack);
        if (ammo <= 0) {
            Level world = player.level();
            world.playSound(
                    null, player,
                    properties.sounds().emptySound(),
                    SoundSource.PLAYERS,
                    1.0f, 1.0f
            );
            emitGunGameEvent(player, GameEvent.ITEM_INTERACT_START);
            FiringManager.stopFiring(player.getUUID());
            return false;
        }

        fire(player);
        setAmmo(stack, ammo - 1);

        // Trigger GeckoLib shoot animation
        triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) player.level()), "gun_controller", "shoot");

        float yawKick = player.getRandom().nextBoolean()
                ? properties.recoilYaw()
                : -properties.recoilYaw();
        ServerPlayNetworking.send(player, new RecoilS2CPayload(
                properties.recoilPitch(), yawKick, properties.recoilDuration()
        ));

        if (getFireMode(stack) == FireMode.SINGLE) {
            FiringManager.stopFiring(player.getUUID());
        }

        return true;
    }

    // ── Default hitscan fire implementation ──────────────────────────────

    protected void fire(ServerPlayer player) {
        ServerLevel world = player.level();
        ItemStack stack = player.getMainHandItem();

        Vec3 start = player.getEyePosition();
        Vec3 direction = applyInaccuracy(player.getViewVector(1.0f), getCurrentAccuracy(player, stack), world.random);
        Vec3 end = start.add(direction.scale(properties.range()));

        // Block raycast (passes through non-solid blocks)
        BlockHitResult blockHit = raycastFiltered(world, start, end, direction, player);

        // Entity raycast
        AABB searchBox = player.getBoundingBox()
                .expandTowards(direction.scale(properties.range()))
                .inflate(1.0);

        Entity closestEntity = null;
        Vec3 closestEntityHitPos = null;
        double closestEntityDistSq = properties.range() * properties.range();

        for (Entity entity : world.getEntities(player, searchBox, e -> !e.isSpectator() && e.isPickable())) {
            AABB entityBox = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> hitPos = entityBox.clip(start, end);

            if (hitPos.isPresent()) {
                double distSq = start.distanceToSqr(hitPos.get());
                if (distSq < closestEntityDistSq) {
                    closestEntityDistSq = distSq;
                    closestEntity = entity;
                    closestEntityHitPos = hitPos.get();
                }
            }
        }

        // Choose the closest hit (entity vs block)
        Vec3 hitPos = blockHit.getLocation();
        double blockDistSq = start.distanceToSqr(blockHit.getLocation());
        boolean entityIsCloser = closestEntity != null && closestEntityDistSq < blockDistSq;

        if (entityIsCloser) {
            hitPos = closestEntityHitPos;
        }

        // Fluid interactions (water splashes + lava stopping)
        Vec3 lavaHit = handleFluidInteractions(world, start, hitPos, direction);
        boolean hitLava = false;

        if (lavaHit != null && start.distanceToSqr(lavaHit) < start.distanceToSqr(hitPos)) {
            hitPos = lavaHit;
            hitLava = true;
            entityIsCloser = false;
        }

        // Block interactions (bells, target blocks, etc.)
        if (!entityIsCloser && !hitLava && blockHit.getType() != HitResult.Type.MISS) {
            handleBlockInteractions(world, blockHit, player);
        }

        // Entity damage (skipped if lava blocked the path)
        if (entityIsCloser && closestEntity instanceof LivingEntity living) {
            float damage = properties.baseDamage();

            // Headshot detection — top 25% of bounding box
            AABB box = living.getBoundingBox();
            double headThreshold = box.minY + box.getYsize() * 0.75;
            if (closestEntityHitPos.y >= headThreshold) {
                damage *= properties.headshotMultiplier();
                player.displayClientMessage(Component.literal("§c✦ Headshot!"), true);

                world.playSound(
                        null, player,
                        SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS,
                        0.8f, 2.0f
                );
            }

            living.hurtServer(world, player.damageSources().playerAttack(player), damage);
        }

        // Impact particles
        if (hitPos != null) {
            if (hitLava) {
                world.sendParticles(
                        ParticleTypes.LAVA,
                        hitPos.x, hitPos.y, hitPos.z,
                        2, 0.2, 0.2, 0.2, 0.01
                );
                world.playSound(null, BlockPos.containing(hitPos),
                        SoundEvents.LAVA_EXTINGUISH,
                        SoundSource.BLOCKS, 0.4f, 1.0f);
            } else {
                world.sendParticles(
                        ParticleTypes.CRIT,
                        hitPos.x, hitPos.y, hitPos.z,
                        5, 0.1, 0.1, 0.1, 0.01
                );
            }
        }

        // Muzzle smoke
        world.sendParticles(
                ParticleTypes.SMOKE,
                player.getX(), player.getEyeY(), player.getZ(),
                3, 0.05, 0.05, 0.05, 0.01
        );

        // Sound
        world.playSound(
                null, player,
                properties.sounds().shootSound(),
                SoundSource.PLAYERS,
                1.0f,
                0.9f + world.random.nextFloat() * 0.2f
        );
        emitGunGameEvent(player, GameEvent.PROJECTILE_SHOOT);
    }

    private float getCurrentAccuracy(ServerPlayer player, ItemStack stack) {
        float baseAccuracy = isAiming(stack) ? properties.aimedAccuracy() : properties.hipFireAccuracy();
        float movementModifier;

        if (player.isSwimming() || player.isSprinting() || !player.onGround()) {
            movementModifier = -0.25f;
        } else if (player.isShiftKeyDown()) {
            movementModifier = 0.05f;
        } else if (isWalking(player)) {
            movementModifier = -0.10f;
        } else {
            movementModifier = 0.0f;
        }

        return Mth.clamp(baseAccuracy + movementModifier, 0.0f, 1.0f);
    }

    private static boolean isWalking(ServerPlayer player) {
        double horizontalSpeedSq = player.getDeltaMovement().horizontalDistanceSqr();

        return horizontalSpeedSq > 0.0004;
    }

    private static Vec3 applyInaccuracy(Vec3 direction, float accuracy, net.minecraft.util.RandomSource random) {
        float clampedAccuracy = Mth.clamp(accuracy, 0.0f, 1.0f);
        float inaccuracy = 1.0f - clampedAccuracy;

        if (inaccuracy <= 0.0f) {
            return direction;
        }

        float maxSpreadRadians = (float) Math.toRadians(12.0f * inaccuracy);
        float yawOffset = (random.nextFloat() * 2.0f - 1.0f) * maxSpreadRadians;
        float pitchOffset = (random.nextFloat() * 2.0f - 1.0f) * maxSpreadRadians;

        return direction.yRot(yawOffset).xRot(pitchOffset).normalize();
    }

    // ── Switch mode ──────────────────────────────────────────────────────

    public void switchMode(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        FireMode current = getFireMode(stack);
        FireMode next = current.next();
        setFireMode(stack, next);

        player.displayClientMessage(
                Component.literal("Firing mode: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(next.getDisplayName()).withStyle(ChatFormatting.YELLOW)),
                true
        );

        Level world = player.level();
        world.playSound(
                null, player,
                properties.sounds().switchSound(),
                SoundSource.PLAYERS,
                1.0f, 1.0f
        );
        emitGunGameEvent(player, GameEvent.ITEM_INTERACT_START);
        if (!player.getCooldowns().isOnCooldown(stack)) {
            player.getCooldowns().addCooldown(stack, 1);
        }
    }

    // ── Reload ───────────────────────────────────────────────────────────

    public void tryReload(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        Level world = player.level();

        if (player.getCooldowns().isOnCooldown(stack) || getAmmo(stack) == getMaxAmmo())
            return;

        boolean hasAmmo = getAmmo(stack) > 0;

        // Trigger GeckoLib reload animation (tactical = magin, full = reload)
        triggerAnim(player, GeoItem.getOrAssignId(stack, (ServerLevel) player.level()),
                "gun_controller", hasAmmo ? "magin" : "reload");

        world.playSound(
                null, player,
                properties.sounds().getReloadSound(hasAmmo),
                SoundSource.PLAYERS,
                1.0f, 1.0f
        );
        emitGunGameEvent(player, GameEvent.ITEM_INTERACT_FINISH);

        setReloading(stack, true);
        player.getCooldowns().addCooldown(stack, properties.getReloadDuration(hasAmmo));
    }

    public void tickReloadCompletion(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (isReloading(stack) && !player.getCooldowns().isOnCooldown(stack)) {
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
    private static Vec3 handleFluidInteractions(ServerLevel world, Vec3 start, Vec3 bulletEnd, Vec3 direction) {
        double totalDist = start.distanceTo(bulletEnd);
        double stepSize = 0.25;
        boolean wasInWater = world.getFluidState(BlockPos.containing(start)).is(FluidTags.WATER);

        for (double d = stepSize; d <= totalDist; d += stepSize) {
            Vec3 pos = start.add(direction.scale(d));
            BlockPos blockPos = BlockPos.containing(pos);
            FluidState fluidState = world.getFluidState(blockPos);

            if (fluidState.is(FluidTags.LAVA)) {
                return pos;
            }

            boolean inWater = fluidState.is(FluidTags.WATER);
            if (inWater != wasInWater) {
                world.sendParticles(
                        ParticleTypes.SPLASH,
                        pos.x, pos.y, pos.z,
                        8, 0.15, 0.05, 0.15, 0.05
                );
                world.playSound(null, blockPos,
                        SoundEvents.GENERIC_SPLASH,
                        SoundSource.BLOCKS, 0.6f, 1.4f);
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

    protected static BlockHitResult raycastFiltered(ServerLevel world, Vec3 start, Vec3 end, Vec3 direction, Entity shooter) {
        BlockHitResult result = world.clip(new ClipContext(
                start, end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                shooter
        ));

        int iterations = 200;
        while (iterations-- > 0 && result.getType() != HitResult.Type.MISS) {
            BlockPos hitBlock = result.getBlockPos();
            BlockState state = world.getBlockState(hitBlock);

            if (isBulletBreakableGlass(state)) {
                world.destroyBlock(hitBlock, false, shooter);
            } else {
                return result;
            }

            Vec3 past = result.getLocation().add(direction.scale(0.01));
            result = world.clip(new ClipContext(
                    past, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    shooter
            ));
        }

        return result;
    }

    private static void handleBlockInteractions(ServerLevel world, BlockHitResult blockHit, ServerPlayer player) {
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Bells — ring if hit on valid side
        if (block instanceof BellBlock bellBlock) {
            bellBlock.onHit(world, state, blockHit, player, true);
        }

        // Target blocks — emit redstone signal based on accuracy
        if (block instanceof TargetBlock) {
            int power = calculateTargetPower(blockHit);
            if (!world.getBlockTicks().hasScheduledTick(pos, block)) {
                world.setBlock(pos, state.setValue(BlockStateProperties.POWER, power), Block.UPDATE_ALL);
                world.scheduleTick(pos, block, 8);
            }
        }

        // Decorated pots — shatter on hit
        if (block instanceof DecoratedPotBlock) {
            world.setBlock(pos, state.setValue(BlockStateProperties.CRACKED, true), Block.UPDATE_NONE);
            world.destroyBlock(pos, true, player);
        }

        // Amethyst — chime sound
        if (block instanceof AmethystBlock || block instanceof AmethystClusterBlock) {
            world.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 0.5f + world.random.nextFloat() * 1.2f);
        }

        // Wooden buttons — press on hit
        if (block instanceof ButtonBlock buttonBlock && state.is(BlockTags.WOODEN_BUTTONS) && !state.getValue(BlockStateProperties.POWERED)) {
            buttonBlock.press(state, world, pos, null);
        }
    }

    private static int calculateTargetPower(BlockHitResult hitResult) {
        Vec3 pos = hitResult.getLocation();
        Direction direction = hitResult.getDirection();
        double d = Math.abs(Mth.frac(pos.x) - 0.5);
        double e = Math.abs(Mth.frac(pos.y) - 0.5);
        double f = Math.abs(Mth.frac(pos.z) - 0.5);
        Direction.Axis axis = direction.getAxis();
        double g;
        if (axis == Direction.Axis.Y) {
            g = Math.max(d, f);
        } else if (axis == Direction.Axis.Z) {
            g = Math.max(d, e);
        } else {
            g = Math.max(e, f);
        }
        return Math.max(1, Mth.ceil(15.0 * Mth.clamp((0.5 - g) / 0.5, 0.0, 1.0)));
    }

    private static boolean isBulletBreakableGlass(BlockState state) {
        // Full glass blocks (clear, stained, tinted) — IMPERMEABLE tag covers these, minus ice
        if (state.is(BlockTags.IMPERMEABLE) && !state.is(Blocks.ICE)) {
            return true;
        }
        // Glass panes (but not iron bars or copper panes which also extend PaneBlock)
        return state.getBlock() instanceof StainedGlassPaneBlock
                || state.is(Blocks.GLASS_PANE);
    }
}
