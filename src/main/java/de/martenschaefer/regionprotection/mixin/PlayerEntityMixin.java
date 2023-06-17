package de.martenschaefer.regionprotection.mixin;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import de.martenschaefer.regionprotection.RegionProtectionMod;
import de.martenschaefer.regionprotection.region.RegionRuleEnforcer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "canPlaceOn", at = @At("HEAD"), cancellable = true)
    private void onCanPlaceOn(BlockPos pos, Direction facing, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!RegionProtectionMod.getConfig().enabled()) {
            return;
        }

        PlayerEntity player = (PlayerEntity) (Object) this;
        ActionResult result = RegionRuleEnforcer.onBlockPlace(player, pos);

        if (result == ActionResult.FAIL) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                // The client might have placed the block on its side, so make sure to let it know.
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
