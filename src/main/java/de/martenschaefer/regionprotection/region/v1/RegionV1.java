package de.martenschaefer.regionprotection.region.v1;

import de.martenschaefer.config.api.ModConfig;
import de.martenschaefer.regionprotection.region.RegionV2;
import de.martenschaefer.regionprotection.region.shape.RegionShapes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;

public record RegionV1(String key, int level, RegionShapes shapes) implements ModConfig<RegionV2> {
    public static final Codec<RegionV1> OLD_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("key").forGetter(region -> region.key),
        Codec.INT.fieldOf("level").forGetter(region -> region.level),
        RegionShapes.CODEC.fieldOf("shapes").forGetter(region -> region.shapes)
    ).apply(instance, RegionV1::new));

    public static final ModConfig.Type<RegionV2, RegionV1> TYPE = new ModConfig.Type<>(1, OLD_CODEC);

    @Override
    public Type<RegionV2, ?> type() {
        return TYPE;
    }

    @Override
    public RegionV2 latest() {
        return new RegionV2(this.key, this.level, this.shapes, new Reference2ReferenceOpenHashMap<>());
    }

    @Override
    public boolean shouldUpdate() {
        return true;
    }
}
