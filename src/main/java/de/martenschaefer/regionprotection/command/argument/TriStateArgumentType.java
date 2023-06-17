package de.martenschaefer.regionprotection.command.argument;

import net.minecraft.command.argument.EnumArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.fabricmc.fabric.api.util.TriState;
import de.martenschaefer.regionprotection.util.StringTriState;
import com.mojang.brigadier.context.CommandContext;

@Deprecated
public class TriStateArgumentType extends EnumArgumentType<StringTriState> {
    private TriStateArgumentType() {
        super(StringTriState.CODEC, StringTriState::values);
    }

    public static TriStateArgumentType triState() {
        return new TriStateArgumentType();
    }

    public static TriState getTriState(CommandContext<ServerCommandSource> context, String id) {
        return context.getArgument(id, StringTriState.class).getState();
    }
}
