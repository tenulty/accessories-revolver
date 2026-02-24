package io.wispforest.accessories.client;

import I;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4i;

/**
 * Mean Position of all the Vertices™ (MPOATV)
 */
public final class MPOATVConstructingVertexConsumer implements VertexConsumer {

    private double minX = Double.MAX_VALUE;
    private double minY = Double.MAX_VALUE;
    private double minZ = Double.MAX_VALUE;
    private double maxX = -Double.MAX_VALUE;
    private double maxY = -Double.MAX_VALUE;
    private double maxZ = -Double.MAX_VALUE;

    private Vector3d meanPos = null;

    public Vector3d meanPos(){
        return this.meanPos;
    }

    @Override
    @NotNull
    public VertexConsumer m_5483_(double x, double y, double z) {
        var leeway = 10;

        var box = AccessoriesScreen.SCISSOR_BOX;

        if ((x >= box.x - leeway && x <= box.z + leeway) && (y >= box.y - leeway && y <= box.w + leeway)) {
            this.minX = Math.min(this.minX, x);
            this.maxX = Math.max(this.maxX, x);

            this.minY = Math.min(this.minY, y);
            this.maxY = Math.max(this.maxY, y);

            this.minZ = Math.min(this.minZ, z);
            this.maxZ = Math.max(this.maxZ, z);
        }

        this.meanPos = new Vector3d((this.minX + this.maxX) / 2, (this.minY + this.maxY) / 2, (this.minZ + this.maxZ) / 2);

        return this;
    }

    @Override public VertexConsumer m_6122_(int i, int j, int k, int l) { return this; }
    @Override public VertexConsumer m_7421_(float f, float g) { return this; }
    @Override public VertexConsumer m_7122_(int i, int j) { return this; }
    @Override public VertexConsumer m_7120_(int i, int j) { return this; }
    @Override public VertexConsumer m_5601_(float f, float g, float h) { return this; }
    @Override public void m_5752_() {}
    @Override public void m_7404_(int defaultR, int defaultG, int defaultB, int defaultA) {}
    @Override public void m_141991_() {}
}