package de.martenschaefer.regionprotection.mixin.shape;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.network.ServerPlayerEntity;
import de.martenschaefer.regionprotection.region.shape.ProtectionShape;
import de.martenschaefer.regionprotection.region.shape.ShapeBuilder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin implements ShapeBuilder {
    private List<ProtectionShape> shapes;

    @Override
    public void start() {
        this.shapes = new ArrayList<>();
    }

    @Override
    public void add(ProtectionShape shape) {
        this.shapes.add(shape);
    }

    @Override
    public ProtectionShape finish() {
        ProtectionShape[] shapes = this.shapes.toArray(new ProtectionShape[0]);
        this.shapes = null;
        return ProtectionShape.union(shapes);
    }

    @Override
    public boolean isBuilding() {
        return this.shapes != null;
    }
}
