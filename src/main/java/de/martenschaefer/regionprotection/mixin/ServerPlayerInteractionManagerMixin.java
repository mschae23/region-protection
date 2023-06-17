package de.martenschaefer.regionprotection.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import de.martenschaefer.regionprotection.region.RegionRuleEnforcer;
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

    @Inject(method = "interactItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;use(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/TypedActionResult;", ordinal = 0), cancellable = true)
    private void injectBeforeStackUse(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ActionResult result = RegionRuleEnforcer.onItemUse(player, hand, player.getPos());

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

    @Redirect(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;onUse(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;"))
    private ActionResult redirectBlockStateUse(BlockState state, World world, PlayerEntity playerEntity, Hand hand, BlockHitResult hitResult, ServerPlayerEntity player, World world2, ItemStack stack) {
        ActionResult result = RegionRuleEnforcer.onBlockUse(player, hitResult.getBlockPos());

        if (result == ActionResult.FAIL) {
            disallowedBlockUse = true;
            return result;
        } else {
            return state.onUse(world, playerEntity, hand, hitResult);
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
