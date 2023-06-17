package de.martenschaefer.regionprotection.region.shape;

import com.mojang.serialization.Codec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.world.World;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class UnionShape implements ProtectionShape {
    public static final Codec<UnionShape> CODEC = ProtectionShape.CODEC.listOf().xmap(UnionShape::new, union -> Arrays.asList(union.scopes));

    private final ProtectionShape[] scopes;

    public UnionShape(ProtectionShape... scopes) {
        this.scopes = scopes;
    }

    private UnionShape(List<ProtectionShape> scopes) {
        this(scopes.toArray(new ProtectionShape[0]));
    }

    @Override
    public ProtectionShapeType<?> getType() {
        return ProtectionShapeType.UNION;
    }

    // package-private for RegionShapes; do not modify returned array
    ProtectionShape[] getScopes() {
        return this.scopes;
    }

    @Override
    public boolean test(ProtectionContext context) {
        for (ProtectionShape shape : this.scopes) {
            if (shape.test(context)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean testDimension(RegistryKey<World> dimension) {
        for (ProtectionShape shape : this.scopes) {
            if (shape.testDimension(dimension)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean intersects(ProtectionShape other) {
        for (ProtectionShape shape : this.scopes) {
            if (shape.intersects(other)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public MutableText display() {
        if (this.scopes.length == 1) {
            return this.scopes[0].display();
        } else if (this.scopes.length == 0) {
            return Text.literal("()");
        }

        MutableText text = Text.literal("(");
        for (int i = 0; i < this.scopes.length; i++) {
            text = text.append(this.scopes[i].display());
            if (i < this.scopes.length - 1) {
                text = text.append("U");
            }
        }
        return text.append(")");
    }

    @Override
    public MutableText displayShort() {
        if (this.scopes.length == 1) {
            return this.scopes[0].display();
        } else if (this.scopes.length == 0) {
            return Text.literal("()");
        }
        return Text.literal(this.scopes.length + " combined shapes");
    }

    @Override
    public Stream<ProtectionShape> flatStream() {
        return Arrays.stream(this.scopes).flatMap(ProtectionShape::flatStream);
    }

    @Override
    public ProtectionShape union(ProtectionShape other) {
        var scopes = new ProtectionShape[this.scopes.length + 1];
        System.arraycopy(this.scopes, 0, scopes, 0, this.scopes.length);
        scopes[scopes.length - 1] = other;
        return new UnionShape(scopes);
    }
}
