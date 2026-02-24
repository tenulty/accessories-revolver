package io.wispforest.accessories.utils;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.endec.MinecraftEndecs;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.StructEndecBuilder;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AttributeUtils {
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void addTransientAttributeModifiers(LivingEntity livingEntity, AccessoryAttributeBuilder attributes) {
        if(attributes.isEmpty()) return;

        var attributeMap = livingEntity.m_21204_();
        var capability = livingEntity.accessoriesCapability();

        var containers = capability.getContainers();

        attributes.getSlotModifiers().asMap().forEach((s, modifiers) -> {
            var container = containers.get(s);

            if(container == null) return;

            modifiers.stream()
                    .filter(modifier -> !container.hasModifier(modifier.m_22209_()))
                    .forEach(container::addTransientModifier);
        });

        attributes.getAttributeModifiers(true).asMap().forEach((holder, modifiers) -> {
            var instance = attributeMap.m_22146_(holder);

            if(instance == null) return;

            modifiers.stream()
                    .filter(modifier -> !instance.m_22109_(modifier))
                    .forEach(instance::m_22118_);
        });
    }

    public static void removeTransientAttributeModifiers(LivingEntity livingEntity, AccessoryAttributeBuilder attributes) {
        if(attributes.isEmpty()) return;

        var attributeMap = livingEntity.m_21204_();
        var capability = livingEntity.accessoriesCapability();

        var containers = capability.getContainers();

        attributes.getSlotModifiers().asMap().forEach((s, modifiers) -> {
            var container = containers.get(s);

            if(container == null) return;

            modifiers.stream()
                    .map(AttributeModifier::m_22209_)
                    .forEach(container::removeModifier);
        });

        attributes.getAttributeModifiers(true).asMap().forEach((holder, modifiers) -> {
            var instance = attributeMap.m_22146_(holder);

            if(instance == null) return;

            modifiers.stream()
                    .map(AttributeModifier::m_22209_)
                    .forEach(instance::m_22120_);
        });
    }

    public static final StructEndec<AttributeModifier> ATTRIBUTE_MODIFIER_ENDEC = StructEndecBuilder.of(
            BuiltInEndecs.UUID.fieldOf("uuid", AttributeModifier::m_22209_),
            Endec.STRING.fieldOf("name", AttributeModifier::m_22214_),
            Endec.DOUBLE.fieldOf("amount", AttributeModifier::m_22218_),
            Endec.forEnum(AttributeModifier.Operation.class).fieldOf("operation", AttributeModifier::m_22217_),
            AttributeModifier::new
    );

    public static ResourceLocation getLocation(String name) {
        var safeName = name.toLowerCase(Locale.ROOT)
                .replace(" ", "_")
                .replaceAll("(?![a-z0-9/._-]).|\n", "");

        var location = ResourceLocation.m_135820_(safeName);

        if (location == null) location = Accessories.of(safeName);

        return location;
    }

    public static Pair<String, UUID> getModifierData(ResourceLocation location) {
        var name = location.toString();
        var id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));

        return Pair.of(name, id);
    }
}
