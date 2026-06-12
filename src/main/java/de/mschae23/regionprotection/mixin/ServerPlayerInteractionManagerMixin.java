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

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import de.mschae23.regionprotection.region.RegionRuleEnforcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
    @Unique
    private boolean disallowedBlockUse = false;

    @Inject(method = "interactItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;", ordinal = 0), cancellable = true)
    private void injectBeforeStackUse(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ActionResult result = RegionRuleEnforcer.onItemUse(player, hand, player.getEntityPos());

        if (result == ActionResult.FAIL) {
            RegionRuleEnforcer.sendDeniedText(player);
            disallowedBlockUse = false;
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;"), cancellable = true)
    private void injectBeforeStackUseOnBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        ActionResult result = RegionRuleEnforcer.onItemUse(player, hand, hitResult.getPos());

        if (result == ActionResult.FAIL) {
            RegionRuleEnforcer.sendDeniedText(player);
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;createScreenHandlerFactory(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/screen/NamedScreenHandlerFactory;"), cancellable = true)
    private void injectBeforeSpectatorOpenContainer(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        ActionResult result = RegionRuleEnforcer.onBlockUse(player, hitResult.getBlockPos());

        if (result == ActionResult.FAIL) {
            RegionRuleEnforcer.sendDeniedText(player);
            cir.setReturnValue(result);
        }
    }

    @Redirect(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;onUse(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult redirectBlockStateUse(BlockState state, World world, PlayerEntity playerEntity, BlockHitResult hitResult, ServerPlayerEntity player, World world2, ItemStack stack) {
        ActionResult result = RegionRuleEnforcer.onBlockUse(player, hitResult.getBlockPos());

        if (result == ActionResult.FAIL) {
            disallowedBlockUse = true;
            return result;
        } else {
            return state.onUse(world, playerEntity, hitResult);
        }
    }

    @Inject(method = "interactBlock", at = @At("RETURN"))
    private void injectBeforeInteractBlockReturn(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (disallowedBlockUse && !cir.getReturnValue().isAccepted()) {
            RegionRuleEnforcer.sendDeniedText(player);
            disallowedBlockUse = false;
        }
    }
}
