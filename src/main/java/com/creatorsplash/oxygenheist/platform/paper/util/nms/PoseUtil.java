package com.creatorsplash.oxygenheist.platform.paper.util.nms;

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class PoseUtil {

    private PoseUtil() {}

    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Pose> DATA_POSE;

    static {
        try {
            Field field = Entity.class.getDeclaredField("DATA_POSE");
            field.setAccessible(true);
            DATA_POSE = (EntityDataAccessor<Pose>) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * If the packet contains a pose entry, returns a new packet with the pose
     * replaced by SWIMMING, Otherwise returns the original packet unchanged
     *
     * <p>Called from {@link DownedPacketInterceptor} on the Netty thread</p>
     */
    public static ClientboundSetEntityDataPacket replacePoseWithSwimming(
            ClientboundSetEntityDataPacket original) {
        int poseId = DATA_POSE.id();
        List<SynchedEntityData.DataValue<?>> items = original.packedItems();

        boolean hasPose = false;
        for (SynchedEntityData.DataValue<?> v : items) {
            if (v.id() == poseId) { hasPose = true; break; }
        }
        if (!hasPose) return original;

        List<SynchedEntityData.DataValue<?>> modified = new ArrayList<>(items.size());
        for (SynchedEntityData.DataValue<?> v : items) {
            modified.add(v.id() == poseId
                    ? SynchedEntityData.DataValue.create(DATA_POSE, Pose.SWIMMING)
                    : v);
        }

        return new ClientboundSetEntityDataPacket(original.id(), modified);
    }

    /* Internals */

    private static SynchedEntityData.DataValue<Pose> poseDataValue(Pose pose) {
        return new SynchedEntityData.DataValue<>(
            DATA_POSE.id(),
            DATA_POSE.serializer(),
            pose
        );
    }

}
