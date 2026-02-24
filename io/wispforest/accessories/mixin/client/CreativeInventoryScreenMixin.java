package io.wispforest.accessories.mixin.client;

import I;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.networking.server.NukeAccessories;
import io.wispforest.accessories.networking.server.ScreenOpen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeInventoryScreenMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {

    public CreativeInventoryScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Unique
    private Button accessoryButton = null;

    @Unique
    private int nukeCoolDown = 0;

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void nukeCooldown(CallbackInfo ci){
        if(this.nukeCoolDown > 0) {
            this.nukeCoolDown--;
        }
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/EffectRenderingInventoryScreen;init()V", shift = At.Shift.AFTER))
    private void injectAccessoryButton(CallbackInfo ci) {
        var xOffset = Accessories.getConfig().clientData.creativeInventoryButtonXOffset;
        var yOffset = Accessories.getConfig().clientData.creativeInventoryButtonYOffset;

        this.accessoryButton = this.m_142416_(
                Button.m_253074_(Component.m_237119_(), button -> {
                            AccessoriesClient.attemptToOpenScreen();
                        }).m_252987_(this.f_97735_ + xOffset, this.f_97736_ + yOffset, 8, 8)
                        .m_257505_(Tooltip.m_257550_(Component.m_237115_(Accessories.translation("open.screen"))))
                        .m_253136_()
        ).adjustRendering((button, guiGraphics, sprite, x, y, width, height) -> {
            guiGraphics.m_280163_(AccessoriesScreen.SPRITES_8X8.getLocation(button), x, y, width, height, 8, 8, 8, 8);

            return true;
        });

        this.accessoryButton.f_93624_ = false;
    }

    @Inject(method = "selectTab", at = @At(value = "TAIL"))
    private void adjustAccessoryButton(CreativeModeTab tab, CallbackInfo ci){
        this.accessoryButton.f_93624_ = tab.m_257962_().equals(CreativeModeTab.Type.INVENTORY);
    }

    @Inject(method = "slotClicked",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/NonNullList;size()I", ordinal = 0, shift = At.Shift.BEFORE))
    private void clearAccessoriesWithClearSlot(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        if(this.nukeCoolDown <= 0) {
            AccessoriesInternals.getNetworkHandler().sendToServer(new NukeAccessories());

            this.nukeCoolDown = 10;
        }
    }

}
