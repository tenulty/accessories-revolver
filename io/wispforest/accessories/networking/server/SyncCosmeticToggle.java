package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.events.AllowEntityModificationCallback;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.networking.BaseAccessoriesPacket;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import java.util.List;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public record SyncCosmeticToggle(@Nullable Integer entityId, String slotName, int slotIndex) implements BaseAccessoriesPacket {

    public static final Endec<SyncCosmeticToggle> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.nullableOf().fieldOf("entityId", SyncCosmeticToggle::entityId),
            Endec.STRING.fieldOf("slotName", SyncCosmeticToggle::slotName),
            Endec.VAR_INT.fieldOf("slotIndex", SyncCosmeticToggle::slotIndex),
            SyncCosmeticToggle::new
    );

    public static SyncCosmeticToggle of(@Nullable LivingEntity livingEntity, SlotType slotType, int slotIndex){
        return new SyncCosmeticToggle(livingEntity != null ? livingEntity.m_19879_() : null, slotType.name(), slotIndex);
    }

    @Override
    public void handle(Player player) {
        if(player.m_9236_().m_5776_()) return;

        LivingEntity targetEntity = player;

        if(this.entityId != null) {
            if(!(player.m_9236_().m_6815_(this.entityId) instanceof LivingEntity livingEntity)) {
                return;
            }

            targetEntity = livingEntity;

            var result = AllowEntityModificationCallback.EVENT.invoker().allowModifications(targetEntity, player, null);

            if(!result.orElse(false)) return;
        }

        var capability = targetEntity.accessoriesCapability();

        if(capability == null) return;

        var slotType = SlotTypeLoader.getSlotType(player.m_9236_(), this.slotName);

        if(slotType == null) return;

        var container = capability.getContainer(slotType);

        var renderOptions = container.renderOptions();

        renderOptions.set(this.slotIndex, !container.shouldRender(this.slotIndex));

        container.markChanged(false);
    }
}
