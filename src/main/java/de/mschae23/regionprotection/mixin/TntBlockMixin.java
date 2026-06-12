/*
 * Copyright (C) 2026  mschae23
 *
 * This file is part of Region protection.
 *
 * Region protection is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.mschae23.regionprotection.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.explosion.Explosion;
import de.mschae23.regionprotection.RegionProtectionMod;
import de.mschae23.regionprotection.region.RegionRuleEnforcer;
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

    @Inject(method = "primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)Z", at = @At(value = "NEW", target = "(Lnet/minecraft/world/World;DDDLnet/minecraft/entity/LivingEntity;)Lnet/minecraft/entity/TntEntity;", ordinal = 0), cancellable = true)
    private static void injectBeforeSummonTntEntity(World world, BlockPos pos, LivingEntity igniter, CallbackInfoReturnable<Boolean> cir) {
        if (RegionProtectionMod.getConfig().enabled() && world instanceof ServerWorld serverWorld) {
            ActionResult result = igniter instanceof ServerPlayerEntity player ?
                RegionRuleEnforcer.onExplosionIgnite(player, pos) : RegionRuleEnforcer.onExplosionIgnite(serverWorld, pos);

            if (result == ActionResult.FAIL) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "onUseWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/TntBlock;primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)Z", ordinal = 0), cancellable = true)
    private void injectInteract(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (RegionProtectionMod.getConfig().enabled() && player instanceof ServerPlayerEntity serverPlayer) {
            ActionResult result = RegionRuleEnforcer.onExplosionIgnite(serverPlayer, pos);

            if (result == ActionResult.FAIL) {
                RegionRuleEnforcer.sendDeniedText(serverPlayer);
                cir.setReturnValue(ActionResult.PASS);
            }
        }
    }

    @Inject(method = "onDestroyedByExplosion", at = @At(value = "NEW", target = "(Lnet/minecraft/world/World;DDDLnet/minecraft/entity/LivingEntity;)Lnet/minecraft/entity/TntEntity;", ordinal = 0), cancellable = true)
    private void injectDestroyedByOtherExplosion(ServerWorld world, BlockPos pos, Explosion explosion, CallbackInfo ci) {
        if (RegionProtectionMod.getConfig().enabled()) {
            ActionResult result = RegionRuleEnforcer.onExplosionIgnite(world, pos);

            if (result == ActionResult.FAIL) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "neighborUpdate", at = @At(value = "INVOKE", target = "net/minecraft/block/TntBlock.primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Z", ordinal = 0), cancellable = true)
    private void injectIgnitedByRedstone(BlockState state, World world, BlockPos pos, Block sourceBlock, WireOrientation wireOrientation, boolean notify, CallbackInfo ci) {
        if (RegionProtectionMod.getConfig().enabled() && world instanceof ServerWorld serverWorld) {
            ActionResult result = RegionRuleEnforcer.onExplosionIgnite(serverWorld, pos);

            if (result == ActionResult.FAIL) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onBlockAdded", at = @At(value = "INVOKE", target = "net/minecraft/block/TntBlock.primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Z", ordinal = 0), cancellable = true)
    private void injectIgnitedByRedstoneWhenPlaced(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify, CallbackInfo ci) {
        if (RegionProtectionMod.getConfig().enabled() && world instanceof ServerWorld serverWorld) {
            ActionResult result = RegionRuleEnforcer.onExplosionIgnite(serverWorld, pos);

            if (result == ActionResult.FAIL) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onProjectileHit", at = @At(value = "INVOKE", target = "net/minecraft/block/TntBlock.primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/LivingEntity;)Z", ordinal = 0), cancellable = true)
    private void injectIgnitedByProjectile(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile, CallbackInfo ci) {
        if (RegionProtectionMod.getConfig().enabled() && world instanceof ServerWorld serverWorld) {
            ActionResult result = RegionRuleEnforcer.onExplosionIgnite(serverWorld, hit.getBlockPos());

            if (result == ActionResult.FAIL) {
                ci.cancel();
            }
        }
    }
}
