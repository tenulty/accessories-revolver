package io.wispforest.accessories.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.accessories.pond.AccessoriesFrameBufferExtension;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL30C;

public class PostEffectBuffer {

    private RenderTarget framebuffer = null;
    private int prevBuffer = 0;
    private int textureFilter = -1;

    public void clear() {
        this.ensureInitialized();

        int previousBuffer = GlStateManager.getBoundFramebuffer();
        this.framebuffer.m_83954_(Minecraft.f_91002_);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousBuffer);
    }

    public void beginWrite(boolean clear, int blitFromMain) {
        this.ensureInitialized();

        this.prevBuffer = GlStateManager.getBoundFramebuffer();
        if (clear) this.framebuffer.m_83954_(Minecraft.f_91002_);

        if (blitFromMain != 0) {
            GlStateManager._glBindFramebuffer(GL30C.GL_READ_FRAMEBUFFER, this.prevBuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.framebuffer.f_83920_);
            GL30.glBlitFramebuffer(
                    0, 0,
                    this.framebuffer.f_83915_, this.framebuffer.f_83916_,
                    0, 0,
                    this.framebuffer.f_83915_, this.framebuffer.f_83916_,
                    blitFromMain, GL11.GL_NEAREST
            );
        }

        this.framebuffer.m_83947_(false);
    }

    public void endWrite() {
        GlStateManager._glBindFramebuffer(GL30C.GL_FRAMEBUFFER, this.prevBuffer);
    }

    public void draw(boolean blend) {
        if (blend) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }

        RenderSystem.backupProjectionMatrix();
        this.framebuffer.m_83957_(this.framebuffer.f_83915_, this.framebuffer.f_83916_, !blend);
        RenderSystem.restoreProjectionMatrix();
    }

    public void draw(float[] color) {
        var modulator = Minecraft.m_91087_().f_91063_.f_172635_.f_173312_;
        modulator.m_5805_(color[0], color[1], color[2], color[3]);
        this.draw(true);
        modulator.m_5805_(1f, 1f, 1f, 1f);
    }

    public RenderTarget buffer() {
        this.ensureInitialized();
        return this.framebuffer;
    }

    private void ensureInitialized() {
        if (this.framebuffer != null) return;

        this.framebuffer = new TextureTarget(Minecraft.m_91087_().m_91385_().f_83915_, Minecraft.m_91087_().m_91385_().f_83916_, true, Minecraft.f_91002_);
        this.framebuffer.m_83931_(0, 0, 0, 0);

        AccessoriesClient.WINDOW_RESIZE_CALLBACK_EVENT.register((client, window) -> {
            this.framebuffer.m_83941_(window.m_85441_(), window.m_85442_(), Minecraft.f_91002_);
            if (this.textureFilter != -1) {
                this.framebuffer.m_83936_(this.textureFilter);
            }
        });
    }

}
