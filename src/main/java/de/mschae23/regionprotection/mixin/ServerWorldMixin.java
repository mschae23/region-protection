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
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import de.mschae23.regionprotection.RegionProtectionMod;
import de.mschae23.regionprotection.region.RegionRuleEnforcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
    @Inject(method = "canEntityModifyAt", at = @At("RETURN"), cancellable = true)
    private void onCanEntityModifyAt(Entity entity, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!RegionProtectionMod.getConfig().enabled()) {
            return;
        }

        if (entity instanceof PlayerEntity player) {
            ActionResult result = RegionRuleEnforcer.onWorldModify(player, pos);

            if (result == ActionResult.FAIL) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    // The client might have modified the world on its side, so make sure to let it know.
                    serverPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(serverPlayer.getEntityWorld(), pos));

                    if (serverPlayer.getEntityWorld().getBlockState(pos).hasBlockEntity()) {
                        BlockEntity blockEntity = serverPlayer.getEntityWorld().getBlockEntity(pos);

                        if (blockEntity != null) {
                            Packet<ClientPlayPacketListener> updatePacket = blockEntity.toUpdatePacket();

                            if (updatePacket != null) {
                                serverPlayer.networkHandler.sendPacket(updatePacket);
                            }
                        }
                    }

                    // Sync the player's inventory, as it may have used an item already.
                    serverPlayer.getInventory().markDirty();
                    serverPlayer.playerScreenHandler.updateToClient();
                }

                cir.setReturnValue(false);
            }
        }
    }
}
