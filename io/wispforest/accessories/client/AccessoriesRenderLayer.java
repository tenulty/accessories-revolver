package io.wispforest.accessories.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.client.gui.AccessoriesInternalSlot;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.compat.AccessoriesConfig.ClientData.HoverOptions;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static io.wispforest.accessories.client.gui.AccessoriesScreen.*;

import F;
import J;
import Z;


/**
 * Render layer used to render equipped Accessories for a given {@link LivingEntity}.
 * This is only applied to {@link LivingEntityRenderer} that have a model that
 * extends {@link HumanoidModel}
 */
public class AccessoriesRenderLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final PostEffectBuffer BUFFER = new PostEffectBuffer();

    private static final float increment = 0.1f;

    private static Map<String, Float> brightnessMap = new HashMap<>();
    private static Map<String, Float> opacityMap = new HashMap<>();

    private static long lastUpdated20th = 0;

    public AccessoriesRenderLayer(RenderLayerParent<T, M> renderLayerParent) {
        super(renderLayerParent);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource multiBufferSource,
            int light,
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        var highlightOptions = Accessories.getConfig().clientData.hoverOptions;

        var capability = AccessoriesCapability.get(entity);

        if (capability == null) return;

        var calendar = Calendar.getInstance();

        float scale = (float) (1 + (0.5 * (0.75 + (Math.sin((System.currentTimeMillis()) / 250d)))));

        var renderingLines = AccessoriesScreen.COLLECT_ACCESSORY_POSITIONS;

        var useCustomerBuffer = IS_RENDERING_UI_ENTITY;

        if (!renderingLines && !AccessoriesScreen.NOT_VERY_NICE_POSITIONS.isEmpty()) {
            AccessoriesScreen.NOT_VERY_NICE_POSITIONS.clear();
        }

        if (multiBufferSource instanceof MultiBufferSource.BufferSource bufferSource) {
            bufferSource.m_109911_();
        }

        var current20th = calendar.getTimeInMillis() / 50;
        var shouldUpdate = lastUpdated20th != current20th;

        if (shouldUpdate) lastUpdated20th = current20th;

        var screen = Minecraft.m_91087_().f_91080_;

        AccessoriesInternalSlot selected = null;

        if (screen instanceof AccessoriesScreen accessoriesScreen && accessoriesScreen.getHoveredSlot() instanceof AccessoriesInternalSlot slot) {
            selected = slot;
        }

        for (var entry : capability.getContainers().entrySet()) {

            var container = entry.getValue();

            var accessories = container.getAccessories();
            var cosmetics = container.getCosmeticAccessories();

            var containerSelected = selected != null && selected.accessoriesContainer.slotType() == container.slotType();

            for (int i = 0; i < accessories.m_6643_(); i++) {

                var isSelected = containerSelected && selected.m_150661_() == i;

                if (shouldUpdate) {
                    var currentBrightness = brightnessMap.getOrDefault(entry.getKey() + i, 1f);
                    var currentOpacity = opacityMap.getOrDefault(entry.getKey() + i, 1f);

                    if (selected != null && !isSelected) {
                        brightnessMap.put(entry.getKey() + i, Math.max(highlightOptions.unHoveredOptions.darkenedBrightness, currentBrightness - increment));
                        opacityMap.put(entry.getKey() + i, Math.max(highlightOptions.unHoveredOptions.darkenedOpacity, currentOpacity - increment));
                    } else {
                        brightnessMap.put(entry.getKey() + i, Math.min(1, currentBrightness + increment));
                        opacityMap.put(entry.getKey() + i, Math.min(1, currentOpacity + increment));
                    }
                }

                var stack = accessories.m_8020_(i);
                var cosmeticStack = cosmetics.m_8020_(i);

                if (!cosmeticStack.m_41619_()) stack = cosmeticStack;

                if (stack.m_41619_()) continue;

                var renderer = AccessoriesRendererRegistry.getRender(stack);

                if (renderer == null || !renderer.shouldRender(container.shouldRender(i))) continue;

                poseStack.m_85836_();

                var mpoatv = new MPOATVConstructingVertexConsumer();

                var bufferedGrabbedFlag = new MutableBoolean(false);

                MultiBufferSource innerBufferSource = (renderType) -> {
                    bufferedGrabbedFlag.setValue(true);

                    return useCustomerBuffer ?
                            VertexMultiConsumer.m_86168_(multiBufferSource.m_6299_(renderType), mpoatv) :
                            multiBufferSource.m_6299_(renderType);
                };

                if (!IS_RENDERING_UI_ENTITY || isSelected || selected == null || highlightOptions.unHoveredOptions.renderUnHovered) {
                    renderer.render(
                            stack,
                            SlotReference.of(entity, container.getSlotName(), i),
                            poseStack,
                            m_117386_(),
                            innerBufferSource,
                            light,
                            limbSwing,
                            limbSwingAmount,
                            partialTicks,
                            ageInTicks,
                            netHeadYaw,
                            headPitch
                    );
                }

                float[] colorValues = null;

                if (useCustomerBuffer && bufferedGrabbedFlag.getValue()) {
                    if (multiBufferSource instanceof MultiBufferSource.BufferSource bufferSource) {
                        if (highlightOptions.hoveredOptions.brightenHovered && isSelected) {
                            if (calendar.get(Calendar.MONTH) + 1 == 5 && calendar.get(Calendar.DATE) == 16) {
                                var hue = (float) ((System.currentTimeMillis() / 20d % 360d) / 360d);

                                var color = new Color(Mth.m_14169_(hue, 1, 1));

                                colorValues = new float[]{color.getRed() / 128f, color.getGreen() / 128f, color.getBlue() / 128f, 1};
                            } else {
                                var mul = highlightOptions.hoveredOptions.cycleBrightness ? scale : 1.5f;
                                colorValues = new float[]{mul, mul, mul, 1};
                            }
                        } else if (highlightOptions.unHoveredOptions.darkenUnHovered) {
                            var darkness = brightnessMap.getOrDefault(entry.getKey() + i, 1f);
                            colorValues = new float[]{darkness, darkness, darkness, opacityMap.getOrDefault(entry.getKey() + i, 1f)};
                        }

                        if (colorValues != null) {
                            BUFFER.beginWrite(true, GL30.GL_DEPTH_BUFFER_BIT);
                            bufferSource.m_109911_();
                            BUFFER.endWrite();

                            BUFFER.draw(colorValues);

                            var frameBuffer = BUFFER.buffer();

                            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, frameBuffer.f_83920_);
                            GL30.glBlitFramebuffer(
                                    0,
                                    0,
                                    frameBuffer.f_83915_,
                                    frameBuffer.f_83916_,
                                    0,
                                    0,
                                    frameBuffer.f_83915_,
                                    frameBuffer.f_83916_,
                                    GL30.GL_DEPTH_BUFFER_BIT,
                                    GL30.GL_NEAREST
                            );
                            Minecraft.m_91087_().m_91385_().m_83947_(false);
                        } else {
                            bufferSource.m_109911_();
                        }
                    }

                    if (renderingLines && AccessoriesScreen.IS_RENDERING_LINE_TARGET) {
                        AccessoriesScreen.NOT_VERY_NICE_POSITIONS.put(container.getSlotName() + i, mpoatv.meanPos());
                    }
                }

                poseStack.m_85849_();
            }
        }
    }
}
