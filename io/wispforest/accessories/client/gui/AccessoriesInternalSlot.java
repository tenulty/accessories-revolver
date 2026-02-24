package io.wispforest.accessories.client.gui;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.SlotType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class AccessoriesInternalSlot extends AccessoriesBasedSlot {

    public final int menuIndex;

    public final boolean isCosmetic;

    private Function<AccessoriesInternalSlot, Boolean> isActive = (slot) -> true;
    private Function<AccessoriesInternalSlot, Boolean> isAccessible = (slot) -> true;

    public AccessoriesInternalSlot(int menuIndex, AccessoriesContainer container, boolean isCosmetic, int slot, int x, int y) {
        super(container, isCosmetic ? container.getCosmeticAccessories() : container.getAccessories(), slot, x, y);

        this.menuIndex = menuIndex;

        this.isCosmetic = isCosmetic;
    }

    public AccessoriesInternalSlot isActive(Function<AccessoriesInternalSlot, Boolean> isActive){
        this.isActive = isActive;

        return this;
    }

    public AccessoriesInternalSlot isAccessible(Function<AccessoriesInternalSlot, Boolean> isAccessible){
        this.isAccessible = isAccessible;

        return this;
    }

    @Override
    protected ResourceLocation icon() {
        return (this.isCosmetic) ? Accessories.of("gui/slot/cosmetic") : super.icon();
    }

    public List<Component> getTooltipData() {
        List<Component> tooltipData = new ArrayList<>();

        var key = this.isCosmetic ? "cosmetic_" : "";

        var slotType = this.accessoriesContainer.slotType();

        tooltipData.add(Component.m_237115_(Accessories.translation(key + "slot.tooltip.singular"))
                .m_130940_(ChatFormatting.GRAY)
                .m_7220_(Component.m_237115_(slotType.translation()).m_130940_(ChatFormatting.BLUE)));

        return tooltipData;
    }

    @Override
    public void m_5852_(ItemStack stack) {
        var prevStack = this.m_7993_();

        super.m_5852_(stack);

        // TODO: SHOULD THIS BE HERE?
//        if(isCosmetic) {
//            var reference = new SlotReference(container.getSlotName(), entity, getContainerSlot());
//
//            AccessoriesAPI.getAccessory(prevStack)
//                    .ifPresent(prevAccessory1 -> prevAccessory1.onUnequip(prevStack, reference));
//
//            AccessoriesAPI.getAccessory(stack)
//                    .ifPresent(accessory1 -> accessory1.onEquip(stack, reference));
//        }
    }

    @Override
    public boolean m_5857_(ItemStack stack) {
        return this.isAccessible.apply(this) && super.m_5857_(stack);
    }

    @Override
    public boolean m_8010_(Player player) {
        return this.isAccessible.apply(this) && (isCosmetic || super.m_8010_(player));
    }

    @Override
    public boolean m_150651_(Player player) {
        return this.isAccessible.apply(this) && super.m_150651_(player);
    }

    @Override
    public boolean m_6659_() {
        return this.isActive.apply(this);
    }
}
