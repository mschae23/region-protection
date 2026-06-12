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

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import de.mschae23.regionprotection.RegionProtectionMod;
import de.mschae23.regionprotection.region.RegionRuleEnforcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Shadow
    public abstract ActionResult place(ItemPlacementContext context);

    @Inject(method = "place(Lnet/minecraft/item/ItemPlacementContext;)Lnet/minecraft/util/ActionResult;", at = @At("HEAD"), cancellable = true)
    private void onPlace(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!RegionProtectionMod.getConfig().enabled()) {
            return;
        }

        ActionResult result = RegionRuleEnforcer.onBlockPlace(context.getPlayer(), context.getBlockPos());

        if (result != ActionResult.PASS) {
            if (context.getPlayer() instanceof ServerPlayerEntity player) {
                // The client might have placed the block on its side, so make sure to let it know.
                player.networkHandler.sendPacket(new BlockUpdateS2CPacket(context.getWorld(), context.getBlockPos()));

                if (context.getWorld().getBlockState(context.getBlockPos()).hasBlockEntity()) {
                    BlockEntity blockEntity = context.getWorld().getBlockEntity(context.getBlockPos());

                    if (blockEntity != null) {
                        Packet<ClientPlayPacketListener> updatePacket = blockEntity.toUpdatePacket();

                        if (updatePacket != null) {
                            player.networkHandler.sendPacket(updatePacket);
                        }
                    }
                }

                // Sync the player's inventory, as it may have used the block item already.
                player.getInventory().markDirty();
                player.playerScreenHandler.updateToClient();
            }

            cir.setReturnValue(result);
        }
    }
}
