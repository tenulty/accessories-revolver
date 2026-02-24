package io.wispforest.accessories.client.gui;

import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.resources.ResourceLocation;

public interface SpriteGetter<T> {
    ResourceLocation getLocation(T t);

    static <T extends AbstractButton> SpriteGetter<T> ofButton(ResourceLocation ...locations) {
        return t -> !t.f_93623_ ? locations[1] : (t.m_274382_() ? locations[2] : locations[0]);
    }

    static <T extends ToggleButton> SpriteGetter<T> ofToggle(ResourceLocation ...locations) {
        return t -> !t.toggled() ? locations[1] : (t.m_274382_() ? locations[2] : locations[0]);
    }
}
