package io.wispforest.accessories.client.gui;

import Z;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.client.GuiGraphicsUtils;
import io.wispforest.accessories.networking.server.SyncCosmeticToggle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

public class ToggleButton extends Button {

    static {
        Function<ResourceLocation, GuiGraphicsUtils.NineSlicingDimensionImpl> func = location -> GuiGraphicsUtils.NineSlicingDimensionImpl.of(location, 200, 20, 3);

        GuiGraphicsUtils.register(Accessories.of("widget/button"), func.apply(Accessories.of("textures/gui/sprites/widget/button.png")));
        GuiGraphicsUtils.register(Accessories.of("widget/button_disabled"), func.apply(Accessories.of("textures/gui/sprites/widget/button_disabled.png")));
        GuiGraphicsUtils.register(Accessories.of("widget/button_highlighted"), func.apply(Accessories.of("textures/gui/sprites/widget/button_highlighted.png")));
    }

    private static final SpriteGetter<ToggleButton> SPRITE_GETTER = SpriteGetter.ofToggle(Accessories.of("widget/button"), Accessories.of("widget/button_disabled"), Accessories.of("widget/button_highlighted"));

    private boolean toggled = false;

    private final int zIndex;

    private final Consumer<ToggleButton> onRender;

    protected ToggleButton(int x, int y, int zIndex, int width, int height, Component message, OnPress onPress, CreateNarration createNarration, Consumer<ToggleButton> onRender) {
        super(x, y, width, height, message, onPress, createNarration);

        this.zIndex = zIndex;
        this.onRender = onRender;
    }

    public static ToggleButton ofSlot(int x, int y, int z, AccessoriesBasedSlot slot) {
        return ToggleButton.toggleBuilder(Component.m_237119_(), btn -> {
                    AccessoriesInternals.getNetworkHandler().sendToServer(SyncCosmeticToggle.of(slot.entity.equals(Minecraft.m_91087_().f_91074_) ? null : slot.entity, slot.accessoriesContainer.slotType(), slot.m_150661_()));
                }).onRender(btn -> {
                    var bl = slot.accessoriesContainer.shouldRender(slot.m_150661_());

                    if (bl == btn.toggled()) return;

                    btn.toggled(bl);
                    btn.m_257544_(accessoriesToggleTooltip(bl));
                }).tooltip(accessoriesToggleTooltip(slot.accessoriesContainer.shouldRender(slot.m_150661_())))
                .zIndex(z)
                .bounds(x, y, 5, 5)
                .build()
                .toggled(slot.accessoriesContainer.shouldRender(slot.m_150661_()));
    }

    private static Tooltip accessoriesToggleTooltip(boolean value) {
        var key = "display.toggle." + (!value ? "show" : "hide");

        return Tooltip.m_257550_(Component.m_237115_(Accessories.translation(key)));
    }

    public ToggleButton toggled(boolean value){
        this.toggled = value;

        return this;
    }

    public boolean toggled(){
        return this.toggled;
    }

    @Override
    public void m_5691_() {
        this.f_93717_.m_93750_(this);
    }

    public static ToggleButton.Builder toggleBuilder(Component message, Button.OnPress onPress) {
        return new ToggleButton.Builder(message, onPress);
    }

    @Override
    protected void m_87963_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.onRender.accept(this);

        var pose = guiGraphics.m_280168_();

        pose.m_85836_();
        pose.m_252880_(0, 0, zIndex);

        guiGraphics.m_280246_(1.0F, 1.0F, 1.0F, this.f_93625_);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        GuiGraphicsUtils.blitSpriteBatched(guiGraphics, SPRITE_GETTER.getLocation(this), this.m_252754_(), this.m_252907_(), this.m_5711_(), this.m_93694_());
        guiGraphics.m_280246_(1.0F, 1.0F, 1.0F, 1.0F);
        int i = this.f_93623_ ? 16777215 : 10526880;
        this.m_280139_(guiGraphics, Minecraft.m_91087_().f_91062_, i | Mth.m_14167_(this.f_93625_ * 255.0F) << 24);

        pose.m_85849_();
    }

    @Environment(EnvType.CLIENT)
    public static class Builder {
        private final Component message;
        private final Button.OnPress onPress;
        @Nullable
        private Tooltip tooltip;
        private int x;
        private int y;
        private int zIndex = 0;
        private int width = 150;
        private int height = 20;
        private Button.CreateNarration createNarration = Button.f_252438_;

        private Consumer<ToggleButton> onRender = toggleButton -> {};

        public Builder(Component message, Button.OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public ToggleButton.Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public ToggleButton.Builder zIndex(int zIndex){
            this.zIndex = zIndex;

            return this;
        }

        public ToggleButton.Builder onRender(Consumer<ToggleButton> consumer){
            this.onRender = consumer;

            return this;
        }

        public ToggleButton.Builder width(int width) {
            this.width = width;
            return this;
        }

        public ToggleButton.Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public ToggleButton.Builder bounds(int x, int y, int width, int height) {
            return this.pos(x, y).size(width, height);
        }

        public ToggleButton.Builder tooltip(@Nullable Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public ToggleButton.Builder createNarration(Button.CreateNarration createNarration) {
            this.createNarration = createNarration;
            return this;
        }

        public ToggleButton build() {
            ToggleButton button = new ToggleButton(this.x, this.y, this.zIndex, this.width, this.height, this.message, this.onPress, this.createNarration, this.onRender);

            button.m_257544_(this.tooltip);
            return button;
        }
    }
}
