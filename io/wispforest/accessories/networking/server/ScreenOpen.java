package io.wispforest.accessories.networking.server;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.networking.BaseAccessoriesPacket;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record ScreenOpen(int entityId, boolean targetLookEntity) implements BaseAccessoriesPacket {

    public static final Endec<ScreenOpen> ENDEC = StructEndecBuilder.of(
            Endec.VAR_INT.fieldOf("entityId", ScreenOpen::entityId),
            Endec.BOOLEAN.fieldOf("targetLookEntity", ScreenOpen::targetLookEntity),
            ScreenOpen::new
    );

    public static ScreenOpen of(@Nullable LivingEntity livingEntity){
        return new ScreenOpen(livingEntity != null ? livingEntity.m_19879_() : -1, false);
    }

    public static ScreenOpen of(boolean targetLookEntity){
        return new ScreenOpen(-1, targetLookEntity);
    }

    @Override
    public void handle(Player player) {
        LivingEntity livingEntity = null;

        if(this.entityId != -1) {
            var entity = player.m_9236_().m_6815_(this.entityId);

            if(entity instanceof LivingEntity living) livingEntity = living;
        } else if(this.targetLookEntity) {
            Accessories.attemptOpenScreenPlayer((ServerPlayer) player);

            return;
        }

        ItemStack carriedStack = null;

        var oldMenu = player.f_36096_;

        var currentCarriedStack = oldMenu.m_142621_();

        if(!currentCarriedStack.m_41619_()) {
            carriedStack = currentCarriedStack;

            oldMenu.m_142503_(ItemStack.f_41583_);
        }

        Accessories.openAccessoriesMenu(player, livingEntity, carriedStack);
    }
}