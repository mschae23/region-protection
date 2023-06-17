package de.martenschaefer.regionprotection.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import de.martenschaefer.regionprotection.RegionProtectionMod;
import de.martenschaefer.regionprotection.region.RegionRuleEnforcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {
    @Inject(method = "canPlayerModifyAt", at = @At("RETURN"), cancellable = true)
    private void onCanPlayerModifyAt(PlayerEntity player, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!RegionProtectionMod.getConfig().enabled()) {
            return;
        }

        ActionResult result = RegionRuleEnforcer.onWorldModify(player, pos);

        if (result == ActionResult.FAIL) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                // The client might have modified the world on its side, so make sure to let it know.
                serverPlayer.networkHandler.sendPacket(new BlockUpdateS2CPacket(serverPlayer.getWorld(), pos));

                if (serverPlayer.getWorld().getBlockState(pos).hasBlockEntity()) {
                    BlockEntity blockEntity = serverPlayer.getWorld().getBlockEntity(pos);

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
