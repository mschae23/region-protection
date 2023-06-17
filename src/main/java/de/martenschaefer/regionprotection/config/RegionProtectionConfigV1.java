package de.martenschaefer.regionprotection.config;

import de.martenschaefer.config.api.ModConfig;
import com.mojang.serialization.Codec;

public record RegionProtectionConfigV1(boolean enabled) implements ModConfig<RegionProtectionConfigV1> {
    public static final Codec<RegionProtectionConfigV1> TYPE_CODEC = Codec.BOOL.fieldOf("enabled").xmap(RegionProtectionConfigV1::new, RegionProtectionConfigV1::enabled).codec();

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
