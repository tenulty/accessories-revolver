package io.wispforest.accessories.client.gui;

import D;
import I;
import Z;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.client.AccessoriesMenu;
import io.wispforest.accessories.client.GuiGraphicsUtils;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import io.wispforest.accessories.impl.SlotGroupImpl;
import io.wispforest.accessories.mixin.client.ScreenAccessor;
import io.wispforest.accessories.networking.holder.HolderProperty;
import io.wispforest.accessories.networking.holder.SyncHolderChange;
import io.wispforest.accessories.networking.server.MenuScroll;
import io.wispforest.accessories.pond.ContainerScreenExtension;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.apache.commons.lang3.Range;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.glfw.GLFW;

import java.lang.Math;
import java.util.*;
import java.util.Map.Entry;

public class AccessoriesScreen extends AbstractContainerScreen<AccessoriesMenu> implements ContainerScreenExtension {

    private static final ResourceLocation SLOT = Accessories.of("textures/gui/slot.png");

    private static final ResourceLocation ACCESSORIES_INVENTORY_LOCATION = Accessories.of("textures/gui/container/accessories_inventory.png");

    private static final ResourceLocation BACKGROUND_PATCH = Accessories.of("background_patch");

    private static final ResourceLocation SCROLL_BAR_PATCH = Accessories.of("scroll_bar_patch");
    private static final ResourceLocation SCROLL_BAR = Accessories.of("scroll_bar");

    static {
        GuiGraphicsUtils.register(BACKGROUND_PATCH, GuiGraphicsUtils.NineSlicingDimensionImpl.of(Accessories.of("textures/gui/sprites/background_patch.png"), 15, 15, 5));
        GuiGraphicsUtils.register(SCROLL_BAR_PATCH, GuiGraphicsUtils.NineSlicingDimensionImpl.of(Accessories.of("textures/gui/sprites/scroll_bar_patch.png"), 6, 6, 2));
        GuiGraphicsUtils.register(SCROLL_BAR, GuiGraphicsUtils.NineSlicingDimensionImpl.of(Accessories.of("textures/gui/sprites/scroll_bar.png"), 6, 6, 2));
    }

    private static final ResourceLocation HORIZONTAL_TABS = Accessories.of("textures/gui/container/horizontal_tabs_small.png");

    public static final SpriteGetter<AbstractButton> SPRITES_12X12 = SpriteGetter.ofButton(Accessories.of("textures/gui/sprites/widget/12x12/button.png"), Accessories.of("textures/gui/sprites/widget/12x12/button_disabled.png"), Accessories.of("textures/gui/sprites/widget/12x12/button_highlighted.png"));
    public static final SpriteGetter<AbstractButton> SPRITES_8X8 = SpriteGetter.ofButton(Accessories.of("textures/gui/sprites/widget/8x8/button.png"), Accessories.of("textures/gui/sprites/widget/8x8/button_disabled.png"), Accessories.of("textures/gui/sprites/widget/8x8/button_highlighted.png"));

    private static final ResourceLocation BACk_ICON = Accessories.of("textures/gui/sprites/widget/back.png");

    private static final ResourceLocation LINE_HIDDEN = Accessories.of("textures/gui/sprites/widget/line_hidden.png");
    private static final ResourceLocation LINE_SHOWN = Accessories.of("textures/gui/sprites/widget/line_shown.png");

    private static final ResourceLocation UNUSED_SLOTS_HIDDEN = Accessories.of("textures/gui/sprites/widget/unused_slots_hidden.png");
    private static final ResourceLocation UNUSED_SLOTS_SHOWN = Accessories.of("textures/gui/sprites/widget/unused_slots_shown.png");

    public static Vector4i SCISSOR_BOX = new Vector4i();
    // are we currently rendering an entity in a screen
    public static boolean IS_RENDERING_UI_ENTITY = false;
    // are we currently rendering the entity that lines should be drawn to
    public static boolean IS_RENDERING_LINE_TARGET = false;

    public static boolean COLLECT_ACCESSORY_POSITIONS = false;

    public static final Map<String, Vector3d> NOT_VERY_NICE_POSITIONS = new HashMap<>();

    public static boolean FORCE_TOOLTIP_LEFT = false;

    private final List<Pair<Vector3d, Vector3d>> accessoryLines = new ArrayList<>();

    private final List<Vector3d> accessoryPositions = new ArrayList<>();

    private final Map<AccessoriesInternalSlot, ToggleButton> cosmeticButtons = new LinkedHashMap<>();

    private int currentTabPage = 1;

    private int scrollBarHeight = 0;

    private boolean isScrolling = false;

    public AccessoriesScreen(AccessoriesMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, Component.m_237119_());

        this.f_97728_ = 97;
        this.f_97730_ = 42069;
    }

    private static final int upperPadding = 8;

    public int getPanelHeight() {
        return getPanelHeight(upperPadding);
    }

    public int getPanelHeight(int upperPadding) {
        return 14 + (Math.min(f_97732_.totalSlots, 8) * 18) + upperPadding;
    }

    public int getPanelWidth() {
        int width = 8 + 18 + 18;

        if (f_97732_.isCosmeticsOpen()) width += 18 + 2;

        if (!f_97732_.overMaxVisibleSlots) width -= 12;

        return width;
    }

    public int getStartingPanelX() {
        int x = this.f_97735_ - ((f_97732_.isCosmeticsOpen()) ? 72 : 52);

        if (!f_97732_.overMaxVisibleSlots) x += 12;

        return x;
    }

    public int leftPos() {
        return this.f_97735_;
    }

    public int topPos() {
        return this.f_97736_;
    }

    public final LivingEntity targetEntityDefaulted() {
        var targetEntity = this.f_97732_.targetEntity();

        return (targetEntity != null) ? targetEntity : this.f_96541_.f_91074_;
    }

    protected boolean insideScrollbar(double mouseX, double mouseY) {
        int x = getStartingPanelX() + 13;
        int y = this.f_97736_ + 7 + upperPadding;

        int height = getPanelHeight() - 22;
        int width = 8;

        return this.f_97732_.overMaxVisibleSlots && (mouseX >= x && mouseY >= y && mouseX < (x + width) && mouseY < (y + height));
    }

    @Override
    public boolean m_6375_(double mouseX, double mouseY, int button) {
        var bl = super.m_6375_(mouseX, mouseY, button);

        if (this.m_7222_() instanceof Button) ((ScreenAccessor) this).call$clearFocus();

        if (this.insideScrollbar(mouseX, mouseY)) {
            this.isScrolling = true;

            return true;
        }

        if (Accessories.getConfig().clientData.showGroupTabs && this.f_97732_.maxScrollableIndex() > 0) {
            int x = getStartingPanelX();
            int y = this.f_97736_;

            for (var value : this.getGroups(x, y).values()) {
                if (!value.isInBounds((int) Math.round(mouseX), (int) Math.round(mouseY))) continue;

                var index = value.startingIndex;

                if (index > this.f_97732_.maxScrollableIndex()) index = this.f_97732_.maxScrollableIndex();

                if (index != this.f_97732_.scrolledIndex) {
                    AccessoriesInternals.getNetworkHandler().sendToServer(new MenuScroll(index, false));

                    Minecraft.m_91087_().m_91106_()
                            .m_120367_(SimpleSoundInstance.m_263171_(SoundEvents.f_12490_, 1.0F));
                }

                break;
            }
        }

        return bl;
    }

    @Override
    public boolean m_6348_(double mouseX, double mouseY, int button) {
        if (!this.insideScrollbar(mouseX, mouseY) && button == GLFW.GLFW_MOUSE_BUTTON_1) this.isScrolling = false;

        return super.m_6348_(mouseX, mouseY, button);
    }

    @Override
    protected void m_7286_(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        this.m_280273_(guiGraphics);

        int leftPos = this.f_97735_;
        int topPos = this.f_97736_;

        guiGraphics.m_280218_(ACCESSORIES_INVENTORY_LOCATION, leftPos, topPos, 0, 0, this.f_97726_, this.f_97727_);

        //--

        var scissorStart = new Vector2i(leftPos + 26, topPos + 8);
        var scissorEnd = new Vector2i(leftPos + 26 + 124, topPos + 8 + 70);
        var size = new Vector2i((scissorEnd.x - scissorStart.x) / 2, scissorEnd.y - scissorStart.y);

        SCISSOR_BOX.set(scissorStart.x, scissorStart.y, scissorEnd.x, scissorEnd.y);

        // --

        COLLECT_ACCESSORY_POSITIONS = Accessories.getConfig().clientData.hoverOptions.hoveredOptions.line || Accessories.getConfig().clientData.hoverOptions.hoveredOptions.clickbait;

        IS_RENDERING_UI_ENTITY = true;

        IS_RENDERING_LINE_TARGET = true;

        renderEntityInInventoryFollowingMouseRotated(guiGraphics, scissorStart, size, scissorStart, scissorEnd, mouseX, mouseY, 0);

        IS_RENDERING_LINE_TARGET = false;

        renderEntityInInventoryFollowingMouseRotated(guiGraphics, new Vector2i(scissorStart).add(size.x, 0), size, scissorStart, scissorEnd, mouseX, mouseY, 180);

        IS_RENDERING_UI_ENTITY = false;

        COLLECT_ACCESSORY_POSITIONS = false;


//        HOVERED_SLOT_TYPE = null;

        //--

        var pose = guiGraphics.m_280168_();

        pose.m_85836_();
        pose.m_252880_(0.0F, 0.0F, 0);

        int x = getStartingPanelX();
        int y = this.f_97736_;

        int height = getPanelHeight();
        int width = getPanelWidth();

        //guiGraphics.blitSprite(AccessoriesScreen.BACKGROUND_PATCH, x + 6, y, width, height); //147
        GuiGraphicsUtils.blitSpriteBatched(guiGraphics, AccessoriesScreen.BACKGROUND_PATCH, x + 6, y, width, height); //147

        if (f_97732_.overMaxVisibleSlots) {
            //guiGraphics.blitSprite(AccessoriesScreen.SCROLL_BAR_PATCH, x + 13, y + 7 + upperPadding, 8, height - 22);
            GuiGraphicsUtils.blitSpriteBatched(guiGraphics, AccessoriesScreen.SCROLL_BAR_PATCH, x + 13, y + 7 + upperPadding, 8, height - 22);
        }

        pose.m_85849_();

        //--

        pose.m_85836_();
        pose.m_252880_(-1, -1, 0);

//        for (Slot slot : this.menu.slots) {
//            if (!(slot.container instanceof ExpandedSimpleContainer) || !slot.isActive()) continue;
//
//            if (slot instanceof AccessoriesInternalSlot accessoriesSlot && !accessoriesSlot.getItem().isEmpty()) {
//                var positionKey = accessoriesSlot.accessoriesContainer.getSlotName() + accessoriesSlot.getContainerSlot();
//
//                var vec = NOT_VERY_NICE_POSITIONS.getOrDefault(positionKey, null);
//
//                if (!accessoriesSlot.isCosmetic && vec != null && (menu.areLinesShown())) {
//                    var start = new Vector3d(slot.x + this.leftPos + 17, slot.y + this.topPos + 9, 5000);
//                    var vec3 = vec.add(0, 0, 5000);
//
//                    this.accessoryLines.add(Pair.of(start, vec3));}
//            }
//        }

        GuiGraphicsUtils.batched(guiGraphics, SLOT, this.f_97732_.f_38839_, (bufferBuilder, poseStack, slot) -> {
            if (!(slot.f_40218_ instanceof ExpandedSimpleContainer) || !slot.m_6659_()) return;

            GuiGraphicsUtils.blit(bufferBuilder, poseStack, slot.f_40220_ + this.f_97735_, slot.f_40221_ + this.f_97736_, 18);
        });

        if (getHoveredSlot() != null && getHoveredSlot() instanceof AccessoriesInternalSlot slot && slot.m_6659_() && !slot.m_7993_().m_41619_()) {
            if (NOT_VERY_NICE_POSITIONS.containsKey(slot.accessoriesContainer.getSlotName() + slot.m_150661_())) {
                this.accessoryPositions.add(NOT_VERY_NICE_POSITIONS.get(slot.accessoriesContainer.getSlotName() + slot.m_150661_()));

                var positionKey = slot.accessoriesContainer.getSlotName() + slot.m_150661_();
                var vec = NOT_VERY_NICE_POSITIONS.getOrDefault(positionKey, null);

                if (!slot.isCosmetic && vec != null && (f_97732_.areLinesShown())) {
                    var start = new Vector3d(slot.f_40220_ + this.f_97735_ + 17, slot.f_40221_ + this.f_97736_ + 9, 5000);
                    var vec3 = vec.add(0, 0, 5000);

                    this.accessoryLines.add(Pair.of(start, vec3));}
            }
        }

        pose.m_85849_();
    }

    @Override
    public boolean m_6050_(double mouseX, double mouseY, double scrollY) {
        if (insideScrollbar(mouseX, mouseY) || (Accessories.getConfig().clientData.allowSlotScrolling && this.f_97734_ instanceof AccessoriesInternalSlot)) {
            int index = (int) Math.max(Math.min(-scrollY + this.f_97732_.scrolledIndex, this.f_97732_.maxScrollableIndex()), 0);

            if (index != f_97732_.scrolledIndex) {
                AccessoriesInternals.getNetworkHandler().sendToServer(new MenuScroll(index, false));

                return true;
            }
        }

        return super.m_6050_(mouseX, mouseY, scrollY);
    }

    @Override
    public boolean m_7979_(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.isScrolling) {
            int patchYOffset = this.f_97736_ + 7 + upperPadding;
            int height = getPanelHeight();

            this.f_97732_.smoothScroll = Mth.m_14036_((float) (mouseY - patchYOffset) / (height - 22f), 0.0f, 1.0f); //(menu.smoothScroll + (dragY / (getPanelHeight(upperPadding) - 24)))

            int index = Math.round(this.f_97732_.smoothScroll * this.f_97732_.maxScrollableIndex());

            if (index != f_97732_.scrolledIndex) {
                AccessoriesInternals.getNetworkHandler().sendToServer(new MenuScroll(index, true));

                return true;
            }
        }

        return super.m_7979_(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void m_88315_(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.m_88315_(guiGraphics, mouseX, mouseY, partialTick);

        for (var cosmeticButton : this.cosmeticButtons.values()) {
            cosmeticButton.m_88315_(guiGraphics, mouseX, mouseY, partialTick);
        }

        int x = getStartingPanelX();
        int y = this.f_97736_;

        int panelHeight = getPanelHeight();

        if (this.f_97732_.overMaxVisibleSlots) {
            int startingY = y + upperPadding + 8;

            startingY += this.f_97732_.smoothScroll * (panelHeight - 24 - this.scrollBarHeight);

            GuiGraphicsUtils.blitSpriteBatched(guiGraphics, AccessoriesScreen.SCROLL_BAR, x + 14, startingY, 6, this.scrollBarHeight);
        }

        //--

        var pose = guiGraphics.m_280168_();

        if (Accessories.getConfig().clientData.showGroupTabs) {
            for (var entry : getGroups(x, y).entrySet()) {
                var group = entry.getKey();
                var pair = entry.getValue();

                var vector = pair.dimensions();

                int v = (pair.isSelected()) ? vector.w : vector.w * 3;

                guiGraphics.m_280163_(HORIZONTAL_TABS, vector.x, vector.y, 0, v, vector.z, vector.w, 19, vector.w * 4); //32,128

                pose.m_85836_();

                pose.m_252880_(vector.x + 3, vector.y + 3, 0);
                pose.m_252880_(1, 1, 0);

                if (pair.isSelected) pose.m_252880_(2, 0, 0);

                // MultiDraw?
                guiGraphics.m_280163_(group.icon().m_246208_("textures/").m_266382_(".png"),0, 0, 0, 0, 8, 8, 8,8);

                pose.m_85849_();
            }
        }

        //--

        if (Accessories.getConfig().clientData.hoverOptions.hoveredOptions.clickbait) {
            accessoryPositions.forEach(pos -> {
                var matrix = guiGraphics.m_280168_();

                matrix.m_85836_();

                matrix.m_252880_((float) pos.x - 128, (float) pos.y - 128, 200);

                matrix.m_85841_(0.5f, 0.5f, 0);

                guiGraphics.m_280398_(Accessories.of("textures/gui/sprites/highlight/clickbait.png"), 0, 0, 0, 0, 0, 512, 512, 512, 512);

                matrix.m_85849_();
            });
            this.accessoryPositions.clear();
        }

        this.m_280072_(guiGraphics, mouseX, mouseY);

        if (!this.accessoryLines.isEmpty() && Accessories.getConfig().clientData.hoverOptions.hoveredOptions.line) {
            var buf = guiGraphics.m_280091_().m_6299_(RenderType.f_110371_);
            var lastPose = guiGraphics.m_280168_().m_85850_().m_252943_();

            for (Pair<Vector3d, Vector3d> line : this.accessoryLines) {

                var normalVec = line.second().sub(line.first(), new Vector3d()).normalize().get(new Vector3f());

                double segments = Math.max(10, ((int) (line.first().distance(line.second()) * 10)) / 100);
                segments *= 2;

                var movement = (System.currentTimeMillis() / (segments * 1000) % 1);
                var delta = movement % (2 / (segments)) % segments;

                var firstVec = line.first().get(new Vector3f());

                if (delta > 0.05) {
                    buf.m_5483_(firstVec.x(), firstVec.y(), firstVec.z())
                            .m_6122_(255, 255, 255, 255)
                            .m_86008_(OverlayTexture.f_118083_)
                            .m_85969_(LightTexture.f_173042_)
                            .m_252939_(lastPose, normalVec.x, normalVec.y, normalVec.z)
                            .m_5752_();

                    var pos = new Vector3d(
                            Mth.m_14139_(delta - 0.05, line.first().x, line.second().x),
                            Mth.m_14139_(delta - 0.05, line.first().y, line.second().y),
                            Mth.m_14139_(delta - 0.05, line.first().z, line.second().z)
                    ).get(new Vector3f());

                    buf.m_5483_(pos.x(), pos.y(), pos.z())
                            .m_6122_(255, 255, 255, 255)
                            .m_86008_(OverlayTexture.f_118083_)
                            .m_85969_(LightTexture.f_173042_)
                            .m_252939_(lastPose, normalVec.x, normalVec.y, normalVec.z)
                            .m_5752_();
                }
                for (int i = 0; i < segments / 2; i++) {
                    var delta1 = ((i * 2) / segments + movement) % 1;
                    var delta2 = ((i * 2 + 1) / segments + movement) % 1;

                    var pos1 = new Vector3d(
                            Mth.m_14139_(delta1, line.first().x, line.second().x),
                            Mth.m_14139_(delta1, line.first().y, line.second().y),
                            Mth.m_14139_(delta1, line.first().z, line.second().z)
                    ).get(new Vector3f());
                    var pos2 = (delta2 > delta1 ? new Vector3d(
                            Mth.m_14139_(delta2, line.first().x, line.second().x),
                            Mth.m_14139_(delta2, line.first().y, line.second().y),
                            Mth.m_14139_(delta2, line.first().z, line.second().z)
                    ) : line.second()).get(new Vector3f());

                    buf.m_5483_(pos1.x(), pos1.y(), pos1.z())
                            .m_6122_(255, 255, 255, 255)
                            .m_86008_(OverlayTexture.f_118083_)
                            .m_85969_(LightTexture.f_173042_)
                            .m_252939_(lastPose, normalVec.x, normalVec.y, normalVec.z)
                            .m_5752_();
                    buf.m_5483_(pos2.x(), pos2.y(), pos2.z())
                            .m_6122_(255, 255, 255, 255)
                            .m_86008_(OverlayTexture.f_118083_)
                            .m_85969_(LightTexture.f_173042_)
                            .m_252939_(lastPose, normalVec.x, normalVec.y, normalVec.z)
                            .m_5752_();
                }
            }

            f_96541_.m_91269_().m_110104_().m_109912_(RenderType.f_110371_);

            this.accessoryLines.clear();
        }
    }

    private Button backButton = null;

    private Button cosmeticToggleButton = null;
    private Button linesToggleButton = null;

    private Button unusedSlotsToggleButton = null;
    private Button uniqueSlotsToggleButton = null;

    private Button tabUpButton = null;
    private Button tabDownButton = null;

    @Override
    protected void m_7856_() {
        super.m_7856_();

        this.currentTabPage = 1;

        this.cosmeticButtons.clear();

        this.backButton = this.m_142416_(
                Button.m_253074_(Component.m_237119_(), (btn) -> this.f_96541_.m_91152_(new InventoryScreen(f_96541_.f_91074_)))
                        .m_252987_(this.f_97735_ + 141, this.f_97736_ + 9, 8, 8)
                        .m_257505_(Tooltip.m_257550_(Component.m_237115_(Accessories.translation("back.screen"))))
                        .m_253136_()).adjustRendering((button, guiGraphics, sprite, x, y, width, height) -> {
            guiGraphics.m_280163_(SPRITES_8X8.getLocation(button), x, y, width, height, 8, 8, 8, 8);

            var pose = guiGraphics.m_280168_();

//            pose.pushPose();
//            pose.translate(0.5, 0.5, 0.0);
//
//            guiGraphics.blitSprite(BACk_ICON, x, y, width - 1, height - 1);
//
//            pose.popPose();

            return true;
        });

        var cosmeticsOpen = this.f_97732_.isCosmeticsOpen();

        this.cosmeticToggleButton = this.m_142416_(
                Button.m_253074_(Component.m_237119_(), (btn) -> {
                            AccessoriesInternals.getNetworkHandler()
                                    .sendToServer(SyncHolderChange.of(HolderProperty.COSMETIC_PROP, this.m_6262_().owner(), bl -> !bl));
                        })
                        .m_257505_(cosmeticsToggleTooltip(cosmeticsOpen))
                        .m_252987_(this.f_97735_ - 27 + (cosmeticsOpen ? -20 : 0), this.f_97736_ + 7, (cosmeticsOpen ? 38 : 18), 6)
                        .m_253136_());

        var btnOffset = this.f_97736_ + 7;

        this.unusedSlotsToggleButton = this.m_142416_(
                Button.m_253074_(Component.m_237119_(), (btn) -> {
                            AccessoriesInternals.getNetworkHandler()
                                    .sendToServer(SyncHolderChange.of(HolderProperty.UNUSED_PROP, this.m_6262_().owner(), bl -> !bl));
                        })
                        .m_257505_(unusedSlotsToggleButton(this.f_97732_.areUnusedSlotsShown()))
                        .m_252987_(this.f_97735_ + 154, btnOffset, 12, 12)
                        .m_253136_()).adjustRendering((button, guiGraphics, sprite, x, y, width, height) -> {
            guiGraphics.m_280163_(SPRITES_12X12.getLocation(button), x, y, width, height, 12, 12, 12, 12);
            guiGraphics.m_280163_((this.f_97732_.areUnusedSlotsShown() ? UNUSED_SLOTS_SHOWN : UNUSED_SLOTS_HIDDEN), x, y, width, height, 12, 12, 12, 12);

            return true;
        });

        btnOffset += 15;

        if (Accessories.getConfig().clientData.showUniqueRendering) {
            var anyUniqueSlots = EntitySlotLoader.getEntitySlots(this.targetEntityDefaulted()).values()
                    .stream()
                    .anyMatch(slotType -> UniqueSlotHandling.isUniqueSlot(slotType.name()));

            if (anyUniqueSlots) {
                this.uniqueSlotsToggleButton = this.m_142416_(
                        Button.m_253074_(Component.m_237119_(), (btn) -> {
                                    AccessoriesInternals.getNetworkHandler()
                                            .sendToServer(SyncHolderChange.of(HolderProperty.UNIQUE_PROP, this.m_6262_().owner(), bl -> !bl));
                                })
                                .m_257505_(uniqueSlotsToggleButton(this.f_97732_.areUniqueSlotsShown()))
                                .m_252987_(this.f_97735_ + 154, btnOffset, 12, 12)
                                .m_253136_()).adjustRendering((button, guiGraphics, sprite, x, y, width, height) -> {
                    guiGraphics.m_280163_(SPRITES_12X12.getLocation(button), x, y, width, height, 12, 12, 12, 12);
                    guiGraphics.m_280163_((this.f_97732_.areUniqueSlotsShown() ? UNUSED_SLOTS_SHOWN : UNUSED_SLOTS_HIDDEN), x, y, width, height, 12, 12, 12, 12);

                    return true;
                });

                btnOffset += 15;
            }
        }

//        if (Accessories.getConfig().clientData.showLineRendering) {
//            this.linesToggleButton = this.addRenderableWidget(
//                    Button.builder(Component.empty(), (btn) -> {
//                                AccessoriesInternals.getNetworkHandler()
//                                        .sendToServer(SyncHolderChange.of(HolderProperty.LINES_PROP, this.getMenu().owner(), bl -> !bl));
//                            })
//                            .bounds(this.leftPos + 154, btnOffset, 12, 12)
//                            //.bounds(this.leftPos - (this.menu.isCosmeticsOpen() ? 59 : 39), this.topPos + 7, 8, 6)
//                            .build()).adjustRendering((button, guiGraphics, sprite, x, y, width, height) -> {
//                guiGraphics.blit(SPRITES_12X12.getLocation(button), x, y, width, height, 12, 12, 12, 12);
//                guiGraphics.blit((this.menu.areLinesShown() ? LINE_SHOWN : LINE_HIDDEN), x, y, width, height, 12, 12, 12, 12);
//
//                return true;
//            });
//        }

        int accessoriesSlots = 0;

        for (Slot slot : this.f_97732_.f_38839_) {
            if (!(slot instanceof AccessoriesInternalSlot accessoriesSlot && !accessoriesSlot.isCosmetic)) continue;

            var slotButton = ToggleButton.ofSlot(slot.f_40220_ + this.f_97735_ + 13, slot.f_40221_ + this.f_97736_ - 2, 300, accessoriesSlot);

            slotButton.f_93624_ = accessoriesSlot.m_6659_();
            slotButton.f_93623_ = accessoriesSlot.m_6659_();

            cosmeticButtons.put(accessoriesSlot, this.m_7787_(slotButton));

            accessoriesSlots++;
        }

        if (tabPageCount() > 1) {
            this.tabDownButton = this.m_142416_(
                    Button.m_253074_(Component.m_237113_("⬆"), button -> this.onTabPageChange(true))
                            .m_252987_(this.f_97735_ - 56, this.f_97736_ - 11, 10, 10)
                            .m_253136_());

            this.tabDownButton.f_93623_ = false;

            var height = getPanelHeight();

            this.tabUpButton = this.m_142416_(
                    Button.m_253074_(Component.m_237113_("⬇"), button -> this.onTabPageChange(false))
                            .m_252987_(this.f_97735_ - 56, this.f_97736_ + height + 0, 10, 10)
                            .m_253136_());

            this.tabUpButton.m_257544_(Tooltip.m_257550_(Component.m_237113_("Page 2")));

            this.tabUpButton.f_93623_ = tabPageCount() != 1;
        }

        this.f_97732_.setScrollEvent(this::updateAccessoryToggleButtons);

        this.scrollBarHeight = Mth.m_269140_(Math.min(accessoriesSlots / 20f, 1.0f), 101, 31);

        if (this.scrollBarHeight % 2 == 0) this.scrollBarHeight++;
    }

    private void onTabPageChange(boolean isDown) {
        if ((this.currentTabPage <= 1 && isDown) || (this.currentTabPage > tabPageCount() && !isDown)) {
            return;
        }

        this.currentTabPage += (isDown) ? -1 : 1;

        var lowerLabel = "Page " + (this.currentTabPage - 1);
        var upperLabel = "Page " + (this.currentTabPage + 1);

        this.tabDownButton.m_257544_(Tooltip.m_257550_(Component.m_237113_(lowerLabel)));
        this.tabUpButton.m_257544_(Tooltip.m_257550_(Component.m_237113_(upperLabel)));

//        this.tabDownButton.setMessage(Component.literal(lowerLabel));
//        this.tabUpButton.setMessage(Component.literal(upperLabel));

        if (this.currentTabPage <= 1) {
            this.tabDownButton.f_93623_ = false;
        } else if (!this.tabDownButton.f_93623_) {
            this.tabDownButton.f_93623_ = true;
        }

        if (this.currentTabPage >= tabPageCount()) {
            this.tabUpButton.f_93623_ = false;
        } else if (!this.tabUpButton.f_93623_) {
            this.tabUpButton.f_93623_ = true;
        }
    }

    public void updateButtons(String name) {
        switch (name) {
            case "lines" -> updateLinesButton();
            case "cosmetic" -> updateCosmeticToggleButton();
            case "unused_slots" -> updateUnusedSlotToggleButton();
            case "unique_slots" -> updateUniqueSlotToggleButton();
        }
    }

    public void updateLinesButton() {
//        if (Accessories.getConfig().clientData.showLineRendering) {
//            this.linesToggleButton.setTooltip(linesToggleTooltip(this.menu.areLinesShown()));
//        }
    }

    public void updateCosmeticToggleButton() {
        var btn = this.cosmeticToggleButton;
        btn.m_93674_(this.f_97732_.isCosmeticsOpen() ? 38 : 18);
        btn.m_252865_(btn.m_252754_() + (this.f_97732_.isCosmeticsOpen() ? -20 : 20));
        btn.m_257544_(cosmeticsToggleTooltip(this.f_97732_.isCosmeticsOpen()));
    }

    public void updateUnusedSlotToggleButton() {
        this.unusedSlotsToggleButton.m_257544_(unusedSlotsToggleButton(this.f_97732_.areUnusedSlotsShown()));
        this.f_97732_.reopenMenu();
    }

    public void updateUniqueSlotToggleButton() {
        this.uniqueSlotsToggleButton.m_257544_(uniqueSlotsToggleButton(this.f_97732_.areUniqueSlotsShown()));
        this.f_97732_.reopenMenu();
    }

    public void updateAccessoryToggleButtons() {
        for (var entry : cosmeticButtons.entrySet()) {
            var accessoriesSlot = entry.getKey();
            var btn = entry.getValue();

            if (!accessoriesSlot.m_6659_()) {
                btn.f_93623_ = false;
                btn.f_93624_ = false;
            } else {
                btn.m_257544_(toggleTooltip(accessoriesSlot.accessoriesContainer.shouldRender(accessoriesSlot.m_150661_())));

                btn.m_252865_(accessoriesSlot.f_40220_ + this.f_97735_ + 13);
                btn.m_253211_(accessoriesSlot.f_40221_ + this.f_97736_ - 2);

                btn.toggled(accessoriesSlot.accessoriesContainer.shouldRender(accessoriesSlot.m_150661_()));

                btn.f_93623_ = true;
                btn.f_93624_ = true;
            }
        }
    }

    private static Tooltip cosmeticsToggleTooltip(boolean value) {
        return createToggleTooltip("slot.cosmetics", value);
    }

    private static Tooltip linesToggleTooltip(boolean value) {
        return createToggleTooltip("lines", value);
    }

    private static Tooltip unusedSlotsToggleButton(boolean value) {
        return createToggleTooltip("unused_slots", value);
    }

    private static Tooltip uniqueSlotsToggleButton(boolean value) {
        return createToggleTooltip("unique_slots", value);
    }

    private static Tooltip toggleTooltip(boolean value) {
        return createToggleTooltip("display", value);
    }

    private static Tooltip createToggleTooltip(String type, boolean value) {
        var key = type + ".toggle." + (!value ? "show" : "hide");

        return Tooltip.m_257550_(Component.m_237115_(Accessories.translation(key)));
    }

    @Override
    public @Nullable Boolean isHovering(Slot slot, double mouseX, double mouseY) {
        for (var child : this.m_6702_()) {
            if (child instanceof ToggleButton btn && btn.m_5953_(mouseX, mouseY)) return false;
        }

        return ContainerScreenExtension.super.isHovering(slot, mouseX, mouseY);
    }

    @Override
    protected void m_280072_(GuiGraphics guiGraphics, int x, int y) {
        if (this.f_97734_ instanceof AccessoriesInternalSlot slot) {
            FORCE_TOOLTIP_LEFT = true;

            if (slot.m_7993_().m_41619_() && slot.accessoriesContainer.slotType() != null) {
                guiGraphics.m_280677_(Minecraft.m_91087_().f_91062_, slot.getTooltipData(), Optional.empty(), x, y);

                return;
            }
        }

        if (Accessories.getConfig().clientData.showGroupTabs) {
            int panelX = getStartingPanelX();
            int panelY = this.f_97736_;

            for (var entry : getGroups(panelX, panelY).entrySet()) {
                if (!entry.getValue().isInBounds(x, y)) continue;

                var tooltipData = new ArrayList<Component>();
                var group = entry.getKey();

                tooltipData.add(Component.m_237115_(group.translation()));
                if (UniqueSlotHandling.isUniqueGroup(group.name(), true)) tooltipData.add(Component.m_237113_(group.name()).m_130944_(ChatFormatting.BLUE, ChatFormatting.ITALIC));

                guiGraphics.m_280677_(Minecraft.m_91087_().f_91062_, tooltipData, Optional.empty(), x, y);

                break;
            }
        }

        super.m_280072_(guiGraphics, x, y);

        FORCE_TOOLTIP_LEFT = false;
    }

    @Override
    protected boolean m_7467_(double mouseX, double mouseY, int x, int y, int mouseButton) {
        int leftPos = this.f_97735_;
        int topPos = this.f_97736_;

        boolean insideMainPanel = (mouseX >= leftPos && mouseX <= leftPos + this.f_97726_)
                && (mouseY >= topPos && mouseY <= topPos + this.f_97727_);

        int sidePanelX = getStartingPanelX();
        int sidePanelY = topPos;

        boolean insideSidePanel = (mouseX >= sidePanelX && mouseX <= sidePanelX + this.getPanelWidth() + this.f_97726_)
                && (mouseY >= sidePanelY && mouseY <= sidePanelY + this.getPanelHeight());

        boolean insideGroupPanel = false;

        if (Accessories.getConfig().clientData.showGroupTabs && this.f_97732_.maxScrollableIndex() > 0) {
            for (var value : this.getGroups(sidePanelX, sidePanelY).values()) {
                if (value.isInBounds((int) Math.round(mouseX), (int) Math.round(mouseY))) {
                    insideGroupPanel = true;
                    break;
                }
            }
        }

        return !(insideMainPanel || insideSidePanel || insideGroupPanel);
    }

    public static int tabPageCount() {
        var groups = SlotGroupLoader.INSTANCE.getGroups(true, true);

        return (int) Math.ceil(groups.size() / 9f);
    }

    // MAX 9
    private Map<SlotGroup, SlotGroupData> getGroups(int x, int y) {
        var groups = this.m_6262_().validGroups().stream()
                .sorted(Comparator.comparingInt(SlotGroup::order).reversed())
                .toList();

        if (tabPageCount() > 1) {
            var lowerBound = (this.currentTabPage - 1) * 9;
            var upperBound = lowerBound + 9;

            if (upperBound > groups.size()) upperBound = groups.size();

            groups = groups.subList(lowerBound, upperBound);
        }

        var bottomIndex = this.f_97732_.scrolledIndex;
        var upperIndex = bottomIndex + 8 - 1;

        var scrollRange = Range.between(bottomIndex, upperIndex, Integer::compareTo);

        var targetEntity = this.targetEntityDefaulted();
        var containers = targetEntity.accessoriesCapability().getContainers();

        var slotToSize = new HashMap<String, Integer>();

        for (var slotType : EntitySlotLoader.getEntitySlots(targetEntity).values()) {
            var usedSlots = this.m_6262_().usedSlots();
            if (usedSlots != null && !usedSlots.contains(slotType)) continue;

            var container = containers.get(slotType.name());
            if(container == null) continue;

            slotToSize.put(slotType.name(), container.getAccessories().m_6643_());
        }

        int currentIndexOffset = 0;

        var groupToIndex = new HashMap<SlotGroup, Integer>();
        var selectedGroup = new HashSet<String>();

        for (var group : groups) {
            var groupSize = slotToSize.entrySet().stream()
                    .filter(entry -> group.slots().contains(entry.getKey()))
                    .mapToInt(Map.Entry::getValue)
                    .sum();

            if (groupSize <= 0) continue;

            var groupMinIndex = currentIndexOffset;
            var groupMaxIndex = groupMinIndex + groupSize - 1;

            var groupRange = Range.between(groupMinIndex, groupMaxIndex, Integer::compareTo);

            if (groupRange.isOverlappedBy(scrollRange)) {
                selectedGroup.add(group.name());
            }

            groupToIndex.put(group, groupMinIndex);

            currentIndexOffset += groupSize;
        }

        int maxHeight = getPanelHeight() - 4;

        int width = 19;//32;
        int height = 16;//28;

        int tabY = y + 4;
        int tabX = x - (width - 10);

        int yOffset = 0;

        var groupValues = new HashMap<SlotGroup, SlotGroupData>();

        for (var group : groups) {
            if ((yOffset + height) > maxHeight) break;

            var selected = selectedGroup.contains(group.name());

            int xOffset = (selected) ? 0 : 2;

            var index = groupToIndex.get(group);

            if (index == null) continue;

            groupValues.put(group, new SlotGroupData(new Vector4i(tabX + xOffset, tabY + yOffset, width - xOffset, height), selected, index));

            yOffset += height + 1;
        }

        return groupValues;
    }

    private static SlotGroupImpl copy(SlotGroup group) {
        return new SlotGroupImpl(group.name() + 1, group.order(), group.slots(), group.icon());
    }

    private record SlotGroupData(Vector4i dimensions, boolean isSelected, int startingIndex) {
        private boolean isInBounds(int x, int y) {
            return (x > dimensions.x) && (y > dimensions.y) && (x < dimensions.x + dimensions.z) && (y < dimensions.y + dimensions.w);
        }
    }

    //--

    private void renderEntityInInventoryFollowingMouseRotated(GuiGraphics guiGraphics, Vector2i pos, Vector2i size, Vector2i scissorStart, Vector2i scissorEnd, float mouseX, float mouseY, float rotation) {
        int scale = 30;
        float yOffset = 0.0625F;
        var entity = this.targetEntityDefaulted();

        float f = (float) (pos.x + pos.x + size.x) / 2.0F;
        float g = (float) (pos.y + pos.y + size.y) / 2.0F;
        guiGraphics.m_280588_(scissorStart.x, scissorStart.y, scissorEnd.x, scissorEnd.y);
        float h = (float) Math.atan(((scissorStart.x + scissorStart.x + size.x) / 2f - mouseX) / 40.0F);
        float i = (float) Math.atan(((scissorStart.y + scissorStart.y + size.y) / 2f - mouseY) / 40.0F);
        Quaternionf quaternionf = (new Quaternionf()).rotateZ(3.1415927F).rotateY((float) (rotation * (Math.PI / 180)));
        Quaternionf quaternionf2 = (new Quaternionf()).rotateX(i * 20.0F * 0.017453292F);
        quaternionf.mul(quaternionf2);
        float j = entity.f_20883_;
        float k = entity.m_146908_();
        float l = entity.m_146909_();
        float m = entity.f_20886_;
        float n = entity.f_20885_;
        entity.f_20883_ = 180.0F + h * 30.0F;
        entity.m_146922_(180.0F + h * 40.0F);
        entity.m_146926_(-i * 20.0F);
        entity.f_20885_ = entity.m_146908_();
        entity.f_20886_ = entity.m_146908_();
        //Vector3f vector3f = new Vector3f(0.0F, entity.getBbHeight() / 2.0F + yOffset, 0.0F);
        InventoryScreen.m_280432_(guiGraphics, (int) f, (int) (g + (entity.m_20206_() * 17 + yOffset)), 28, quaternionf, quaternionf2, entity);
        entity.f_20883_ = j;
        entity.m_146922_(k);
        entity.m_146926_(l);
        entity.f_20886_ = m;
        entity.f_20885_ = n;
        guiGraphics.m_280618_();
    }

    public Slot getHoveredSlot() {
        return this.f_97734_;
    }
}
