package de.martenschaefer.regionprotection.util;

import net.fabricmc.fabric.api.util.TriState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public final class RegionProtectionCodecs {
    public static final Codec<TriState> COMPACT_TRISTATE_CODEC = Codec.intRange(0, 2).comapFlatMap(
        value -> switch (value) {
            case 0 -> DataResult.success(TriState.DEFAULT);
            case 1 -> DataResult.success(TriState.FALSE);
            case 2 -> DataResult.success(TriState.TRUE);
            default -> DataResult.error(() -> "Invalid int value for TriState: " + value);
        }, state -> switch (state) {
            case DEFAULT -> 0;
            case FALSE -> 1;
            case TRUE -> 2;
        }
    );

    private RegionProtectionCodecs() {
    }
}
