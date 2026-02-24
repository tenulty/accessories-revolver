package io.wispforest.accessories.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Copy of 1.21 Vanilla class backported to 1.20.1
 */
@ApiStatus.Internal
@Deprecated
public class ArmorSlot extends Slot {
    private final LivingEntity owner;
    private final EquipmentSlot slot;
    @Nullable
    private final ResourceLocation emptyIcon;

    public ArmorSlot(
            Container container, LivingEntity livingEntity, EquipmentSlot equipmentSlot, int i, int j, int k, @Nullable ResourceLocation resourceLocation
    ) {
        super(container, i, j, k);
        this.owner = livingEntity;
        this.slot = equipmentSlot;
        this.emptyIcon = resourceLocation;
    }

    @Override
    public void m_269060_(ItemStack oldStack) {
        var newItem = this.m_7993_();
        var equipable = Equipable.m_269088_(newItem);

        if (equipable != null) owner.m_238392_(slot, oldStack, newItem);

        super.m_269060_(oldStack);
    }

    @Override
    public int m_6641_() {
        return 1;
    }

    @Override
    public boolean m_5857_(ItemStack stack) {
        return this.slot == this.owner.m_147233_(stack);
    }

    @Override
    public boolean m_8010_(Player player) {
        ItemStack itemStack = this.m_7993_();
        return !itemStack.m_41619_() && !player.m_7500_() && EnchantmentHelper.m_44920_(itemStack)
                ? false
                : super.m_8010_(player);
    }

    @Override
    public Pair<ResourceLocation, ResourceLocation> m_7543_() {
        return this.emptyIcon != null ? Pair.of(InventoryMenu.f_39692_, this.emptyIcon) : super.m_7543_();
    }
}
