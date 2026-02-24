package io.wispforest.accessories.mixin.client;

import I;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.networking.server.ScreenOpen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends EffectRenderingInventoryScreen<InventoryMenu> {

    public InventoryScreenMixin(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Unique
    private Button accessoryButton = null;

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;"))
    private void injectAccessoryButton(CallbackInfo ci){
        var xOffset = Accessories.getConfig().clientData.inventoryButtonXOffset;
        var yOffset = Accessories.getConfig().clientData.inventoryButtonYOffset;

        accessoryButton = this.m_142416_(
                Button.m_253074_(Component.m_237119_(), button -> {
                    AccessoriesClient.attemptToOpenScreen();
                }).m_252987_(this.f_97735_ + xOffset, this.f_97736_ + yOffset, 8, 8)
                        .m_257505_(Tooltip.m_257550_(Component.m_237115_(Accessories.translation("open.screen"))))
                        .m_253136_()
        ).adjustRendering((button, guiGraphics, sprite, x, y, width, height) -> {
            guiGraphics.m_280163_(AccessoriesScreen.SPRITES_8X8.getLocation(button), x, y, width, height, 8, 8, 8, 8);

            return true;
        });
    }

    @Inject(method = "method_19891", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;setPosition(II)V"))
    private void adjustAccessoryButton(CallbackInfo ci){
        if(this.accessoryButton == null) return;

        var xOffset = Accessories.getConfig().clientData.inventoryButtonXOffset;
        var yOffset = Accessories.getConfig().clientData.inventoryButtonYOffset;

        accessoryButton.m_264152_(this.f_97735_ + xOffset, this.f_97736_ + yOffset);
    }
}
