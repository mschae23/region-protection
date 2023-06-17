package de.martenschaefer.regionprotection.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import de.martenschaefer.regionprotection.RegionProtectionMod;
import de.martenschaefer.regionprotection.region.RegionRuleEnforcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntBlock.class)
public class TntBlockMixin extends Block {
    public TntBlockMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)V", at = @At(value = "NEW", target = "(Lnet/minecraft/world/World;DDDLnet/minecraft/entity/LivingEntity;)Lnet/minecraft/entity/TntEntity;", ordinal = 0), cancellable = true)
    private static void injectBeforeSummonTntEntity(World world, BlockPos pos, LivingEntity igniter, CallbackInfo ci) {
        if (RegionProtectionMod.getConfig().enabled() && world instanceof ServerWorld serverWorld) {
            ActionResult result = igniter instanceof ServerPlayerEntity player ?
                RegionRuleEnforcer.onExplosionIgnite(player, pos) : RegionRuleEnforcer.onExplosionIgnite(serverWorld, pos);

            if (result == ActionResult.FAIL) {
                ci.cancel();
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Inject(method = "onUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/TntBlock;primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)V", ordinal = 0), cancellable = true)
    private void injectInteract(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (RegionProtectionMod.getConfig().enabled() && player instanceof ServerPlayerEntity serverPlayer) {
            ActionResult result = RegionRuleEnforcer.onExplosionIgnite(serverPlayer, pos);

            if (result == ActionResult.FAIL) {
                RegionRuleEnforcer.sendDeniedText(serverPlayer);
                cir.setReturnValue(super.onUse(state, world, pos, player, hand, hit));
            }
        }
    }

    @Inject(method = "onDestroyedByExplosion", at = @At(value = "NEW", target = "(Lnet/minecraft/world/World;DDDLnet/minecraft/entity/LivingEntity;)Lnet/minecraft/entity/TntEntity;", ordinal = 0), cancellable = true)
    private void injectDestroyedByOtherExplosion(World world, BlockPos pos, Explosion explosion, CallbackInfo ci) {
        if (RegionProtectionMod.getConfig().enabled() && world instanceof ServerWorld serverWorld) {
            ActionResult result = RegionRuleEnforcer.onExplosionIgnite(serverWorld, pos);

            if (result == ActionResult.FAIL) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "neighborUpdate", at = @At(value = "INVOKE", target = "net/minecraft/block/TntBlock.primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V", ordinal = 0), cancellable = true)
    private void injectIgnitedByRedstone(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify, CallbackInfo ci) {
        if (RegionProtectionMod.getConfig().enabled() && world instanceof ServerWorld serverWorld) {
            ActionResult result = RegionRuleEnforcer.onExplosionIgnite(serverWorld, pos);

            if (result == ActionResult.FAIL) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onBlockAdded", at = @At(value = "INVOKE", target = "net/minecraft/block/TntBlock.primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V", ordinal = 0), cancellable = true)
    private void injectIgnitedByRedstoneWhenPlaced(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
        if (RegionProtectionMod.getConfig().enabled() && world instanceof ServerWorld serverWorld) {
            ActionResult result = RegionRuleEnforcer.onExplosionIgnite(serverWorld, pos);

            if (result == ActionResult.FAIL) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onProjectileHit", at = @At(value = "INVOKE", target = "net/minecraft/block/TntBlock.primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)V", ordinal = 0), cancellable = true)
    private void injectIgnitedByProjectile(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile, CallbackInfo ci) {
        if (RegionProtectionMod.getConfig().enabled() && world instanceof ServerWorld serverWorld) {
            ActionResult result = RegionRuleEnforcer.onExplosionIgnite(serverWorld, hit.getBlockPos());

            if (result == ActionResult.FAIL) {
                ci.cancel();
            }
        }
    }
}
