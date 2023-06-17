package de.martenschaefer.regionprotection;

import java.util.function.Supplier;
import net.fabricmc.fabric.api.util.TriState;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.util.Tristate;

public final class ModUtils {
    private static LuckPerms luckPerms = null;

    private ModUtils() {
    }

    // General utils

    public static TriState toFabricTriState(Tristate state) {
        return switch (state) {
            case TRUE -> TriState.TRUE;
            case FALSE -> TriState.FALSE;
            case UNDEFINED -> TriState.DEFAULT;
        };
    }

    public static TriState orTriState(TriState state, Supplier<TriState> other) {
        return state == TriState.DEFAULT ? other.get() : state;
    }

    public static LuckPerms getLuckPerms() {
        if (luckPerms == null) {
            luckPerms = LuckPermsProvider.get();
        }

        return luckPerms;
    }
}
