package de.mschae23.regionprotection.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import de.mschae23.config.api.ModConfig;

public record RegionProtectionConfigV1(boolean enabled) implements ModConfig<RegionProtectionConfigV1> {
    public static final MapCodec<RegionProtectionConfigV1> TYPE_CODEC = Codec.BOOL.fieldOf("enabled").xmap(RegionProtectionConfigV1::new, RegionProtectionConfigV1::enabled);

    public static final ModConfig.Type<RegionProtectionConfigV1, RegionProtectionConfigV1> TYPE = new ModConfig.Type<>(1, TYPE_CODEC);

    public static final RegionProtectionConfigV1 DEFAULT =
        new RegionProtectionConfigV1(true);

    @Override
    public ModConfig.Type<RegionProtectionConfigV1, ?> type() {
        return TYPE;
    }

    @Override
    public RegionProtectionConfigV1 latest() {
        return this;
    }

    @Override
    public boolean shouldUpdate() {
        return true;
    }
}
