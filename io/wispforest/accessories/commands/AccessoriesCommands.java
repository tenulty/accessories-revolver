package io.wispforest.accessories.commands;

import I;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.components.AccessoryItemAttributeModifiers;
import io.wispforest.accessories.api.components.AccessorySlotValidationComponent;
import io.wispforest.accessories.api.components.AccessoryStackSizeComponent;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.utils.AttributeUtils;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Locale;

public class AccessoriesCommands {

    public static final SimpleCommandExceptionType NON_LIVING_ENTITY_TARGET = new SimpleCommandExceptionType(Component.m_237115_("argument.livingEntities.nonLiving"));

    public static final SimpleCommandExceptionType INVALID_SLOT_TYPE = new SimpleCommandExceptionType(new LiteralMessage("Invalid Slot Type"));

    public static final Logger LOGGER = LogUtils.getLogger();

    public static void registerCommandArgTypes() {
        AccessoriesInternals.registerCommandArgumentType(Accessories.of("slot_type"), SlotArgumentType.class, RecordArgumentTypeInfo.of(ctx -> SlotArgumentType.INSTANCE));
        AccessoriesInternals.registerCommandArgumentType(Accessories.of("resource"), ResourceExtendedArgument.class, RecordArgumentTypeInfo.of(ResourceExtendedArgument::attributes));
    }

    public static LivingEntity getOrThrowLivingEntity(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var entity = EntityArgument.m_91452_(ctx, "entity");

        if(!(entity instanceof LivingEntity livingEntity)) {
            throw NON_LIVING_ENTITY_TARGET.create();
        }

        return livingEntity;
    }

    //accessories edit {}
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
                Commands.m_82127_("accessories")
                        .requires(commandSourceStack -> commandSourceStack.m_6761_(Commands.f_165684_))
                        .then(
                                Commands.m_82127_("edit")
                                        .then(
                                                Commands.m_82129_("entity", EntityArgument.m_91449_())
                                                        .executes((ctx) -> {
                                                            var player = ctx.getSource().m_81375_();

                                                            Accessories.openAccessoriesMenu(player, getOrThrowLivingEntity(ctx));

                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            return Accessories.attemptOpenScreenPlayer(ctx.getSource().m_81375_())
                                                    ? 1
                                                    : 0;
                                        })
                        )
                        .then(
                                Commands.m_82127_("slot")
                                        .then(
                                                Commands.m_82127_("add")
                                                        .then(
                                                                Commands.m_82127_("valid")
                                                                        .then(Commands.m_82129_("slot", SlotArgumentType.INSTANCE)
                                                                                .executes(ctx -> adjustSlotValidationOnStack(0, ctx.getSource().m_81375_(), ctx))
                                                                        ))
                                                        .then(
                                                                Commands.m_82127_("invalid")
                                                                        .then(Commands.m_82129_("slot", SlotArgumentType.INSTANCE)
                                                                                .executes(ctx -> adjustSlotValidationOnStack(1, ctx.getSource().m_81375_(), ctx))
                                                                        ))

                                        ).then(
                                                Commands.m_82127_("remove")
                                                        .then(
                                                                Commands.m_82127_("valid")
                                                                        .then(Commands.m_82129_("slot", SlotArgumentType.INSTANCE)
                                                                                .executes(ctx -> adjustSlotValidationOnStack(2, ctx.getSource().m_81375_(), ctx))
                                                                        ))
                                                        .then(
                                                                Commands.m_82127_("invalid")
                                                                        .then(Commands.m_82129_("slot", SlotArgumentType.INSTANCE)
                                                                                .executes(ctx -> adjustSlotValidationOnStack(3, ctx.getSource().m_81375_(), ctx))
                                                                        ))
                                        )
                        )
                        .then(
                                Commands.m_82127_("stack-sizing")
                                        .then(
                                                Commands.m_82127_("useStackSize")
                                                        .then(
                                                                Commands.m_82129_("value", BoolArgumentType.bool())
                                                                        .executes(ctx -> {
                                                                            var player = ctx.getSource().m_81375_();

                                                                            var bl = ctx.getArgument("value", Boolean.class);

                                                                            AccessoriesDataComponents.update(
                                                                                    AccessoriesDataComponents.STACK_SIZE,
                                                                                    player.m_21205_(),
                                                                                    component -> component.useStackSize(bl));

                                                                            return 1;
                                                                        })
                                                        )
                                        ).then(
                                                Commands.m_82129_("value", IntegerArgumentType.integer())
                                                        .executes(ctx -> {
                                                            var player = ctx.getSource().m_81375_();

                                                            var size = ctx.getArgument("value", Integer.class);

                                                            AccessoriesDataComponents.update(
                                                                    AccessoriesDataComponents.STACK_SIZE,
                                                                    player.m_21205_(),
                                                                    component -> component.sizeOverride(size));

                                                            return 1;
                                                        })
                                        )
                        )
                        .then(
                                Commands.m_82127_("attribute")
                                        .then(
                                                Commands.m_82127_("modifier")
                                                        .then(
                                                                Commands.m_82127_("add")
                                                                        .then(
                                                                                Commands.m_82129_("attribute", ResourceExtendedArgument.attributes(context))
                                                                                        .then(
                                                                                                Commands.m_82129_("id", ResourceLocationArgument.m_106984_())
                                                                                                        .then(Commands.m_82129_("value", DoubleArgumentType.doubleArg())
                                                                                                                        .then(createAddLiteral("addition"))
                                                                                                                        .then(createAddLiteral("multiply_base"))
                                                                                                                        .then(createAddLiteral("multiply_total"))
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.m_82127_("remove")
                                                                        .then(
                                                                                Commands.m_82129_("attribute", ResourceExtendedArgument.attributes(context))
                                                                                        .then(
                                                                                                Commands.m_82129_("id", ResourceLocationArgument.m_106984_())
                                                                                                        .executes(
                                                                                                                ctx -> removeModifier(
                                                                                                                        ctx.getSource(),
                                                                                                                        ctx.getSource().m_81375_(),
                                                                                                                        ResourceExtendedArgument.getAttribute(ctx, "attribute"),
                                                                                                                        ResourceLocationArgument.m_107011_(ctx, "id")
                                                                                                                )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.m_82127_("get")
                                                                        .then(
                                                                                Commands.m_82129_("attribute", ResourceExtendedArgument.attributes(context))
                                                                                        .then(
                                                                                                Commands.m_82129_("id", ResourceLocationArgument.m_106984_())
                                                                                                        .executes(
                                                                                                                ctx -> getAttributeModifier(
                                                                                                                        ctx.getSource(),
                                                                                                                        ctx.getSource().m_81375_(),
                                                                                                                        ResourceExtendedArgument.getAttribute(ctx, "attribute"),
                                                                                                                        ResourceLocationArgument.m_107011_(ctx, "id"),
                                                                                                                        1.0
                                                                                                                )
                                                                                                        )
                                                                                                        .then(
                                                                                                                Commands.m_82129_("scale", DoubleArgumentType.doubleArg())
                                                                                                                        .executes(
                                                                                                                                ctx -> getAttributeModifier(
                                                                                                                                        ctx.getSource(),
                                                                                                                                        ctx.getSource().m_81375_(),
                                                                                                                                        ResourceExtendedArgument.getAttribute(ctx, "attribute"),
                                                                                                                                        ResourceLocationArgument.m_107011_(ctx, "id"),
                                                                                                                                        DoubleArgumentType.getDouble(ctx, "scale")
                                                                                                                                )
                                                                                                                        )
                                                                                                        )
                                                                                        )
                                                                        )
                                                        )
                                        )
                        ).then(
                                Commands.m_82127_("log")
                                        .then(
                                                Commands.m_82127_("slots")
                                                        .executes(ctx -> {
                                                            LOGGER.info("All given Slots registered:");

                                                            for (var slotType : SlotTypeLoader.getSlotTypes(ctx.getSource().m_81372_()).values()) {
                                                                LOGGER.info(slotType.toString());
                                                            }

                                                            return 1;
                                                        })
                                        )
                                        .then(
                                                Commands.m_82127_("groups")
                                                        .executes(ctx -> {
                                                            LOGGER.info("All given Slot Groups registered:");

                                                            for (var group : SlotGroupLoader.getGroups(ctx.getSource().m_81372_())) {
                                                                LOGGER.info(group.toString());
                                                            }

                                                            return 1;
                                                        })
                                        )
                                        .then(
                                                Commands.m_82127_("entity_bindings")
                                                        .executes(ctx -> {
                                                            LOGGER.info("All given Entity Bindings registered:");

                                                            EntitySlotLoader.INSTANCE.getEntitySlotData(false).forEach((type, slots) -> {
                                                                LOGGER.info("[{}]: {}", type, slots.keySet());
                                                            });

                                                            return 1;
                                                        })
                                        )
                        )
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createAddLiteral(String literal) {
        var selectedValue = Arrays.stream(AttributeModifier.Operation.values())
                .filter(value -> value.name().toLowerCase(Locale.ROOT).equals(literal))
                .findFirst()
                .orElse(null);

        if(selectedValue == null) throw new IllegalStateException("Unable to handle the given literal as its not a valid AttributeModifier Operation! [Literal: " + literal + "]");

        return Commands.m_82127_(literal)
                .then(
                        Commands.m_82129_("slot", SlotArgumentType.INSTANCE)
                                .then(
                                        Commands.m_82129_("isStackable", BoolArgumentType.bool())
                                                .executes(
                                                        ctx -> addModifier(
                                                                ctx.getSource(),
                                                                ctx.getSource().m_81375_(),
                                                                ResourceExtendedArgument.getAttribute(ctx, "attribute"),
                                                                ResourceLocationArgument.m_107011_(ctx, "id"),
                                                                DoubleArgumentType.getDouble(ctx, "value"),
                                                                selectedValue,
                                                                SlotArgumentType.getSlot(ctx, "slot"),
                                                                BoolArgumentType.getBool(ctx, "isStackable")
                                                        )
                                                )
                                )
                );
    }

    private static int getAttributeModifier(CommandSourceStack commandSourceStack, LivingEntity livingEntity, Holder<Attribute> holder, ResourceLocation resourceLocation, double d) throws CommandSyntaxException {
        var stack = livingEntity.m_21205_();

        var component = AccessoriesDataComponents.readOrDefault(AccessoriesDataComponents.ATTRIBUTES, stack);

        var modifier = component.getModifier(holder.m_203334_(), resourceLocation);

        if (modifier == null) {
            throw ERROR_NO_SUCH_MODIFIER.create(stack.m_41611_(), getAttributeDescription(holder), resourceLocation);
        }

        double e = modifier.m_22218_();

        commandSourceStack.m_288197_(
                () -> Component.m_237110_(
                        "commands.attribute.modifier.value.get.success_itemstack", resourceLocation, getAttributeDescription(holder), stack.m_41611_(), e
                ),
                false
        );

        return (int)(e * d);
    }

    private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType(
            (var1, var2, var3) -> Component.m_237110_("commands.attribute.failed.modifier_already_present_itemstack", var1, var2, var3)
    );

    private static int addModifier(CommandSourceStack commandSourceStack, LivingEntity livingEntity, Holder<Attribute> holder, ResourceLocation resourceLocation, double d, AttributeModifier.Operation operation, String slotName, boolean isStackable) throws CommandSyntaxException {
        var stack = livingEntity.m_21205_();

        var component = AccessoriesDataComponents.readOrDefault(AccessoriesDataComponents.ATTRIBUTES, stack);

        if (component.hasModifier(holder.m_203334_(), resourceLocation)) {
            throw ERROR_MODIFIER_ALREADY_PRESENT.create(resourceLocation, getAttributeDescription(holder), stack.m_41611_());
        }

        var data = AttributeUtils.getModifierData(resourceLocation);

        AccessoriesDataComponents.write(
                AccessoriesDataComponents.ATTRIBUTES,
                stack,
                component.withModifierAdded(holder.m_203334_(), new AttributeModifier(data.second(), data.first(), d, operation), slotName, isStackable));

        commandSourceStack.m_288197_(
                () -> Component.m_237110_(
                        "commands.attribute.modifier.add.success_itemstack", resourceLocation, getAttributeDescription(holder), stack.m_41611_()
                ),
                false
        );

        return 1;
    }

    private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType(
            (var1, var2, var3) -> Component.m_237110_("commands.attribute.failed.no_modifier_itemstack", var1, var2, var3)
    );

    private static int removeModifier(CommandSourceStack commandSourceStack, LivingEntity livingEntity, Holder<Attribute> holder, ResourceLocation resourceLocation) throws CommandSyntaxException {
        MutableBoolean removedModifier = new MutableBoolean(false);

        var stack = livingEntity.m_21205_();

        AccessoriesDataComponents.update(AccessoriesDataComponents.ATTRIBUTES, stack, component -> {
            var size = component.modifiers().size();

            component = component.withoutModifier(holder, resourceLocation);

            if(size != component.modifiers().size()) removedModifier.setTrue();

            return component;
        });

        if(!removedModifier.getValue()) {
            throw ERROR_NO_SUCH_MODIFIER.create(resourceLocation, getAttributeDescription(holder), stack.m_41611_());
        }

        commandSourceStack.m_288197_(
                () -> Component.m_237110_(
                        "commands.attribute.modifier.remove.success_itemstack", resourceLocation, getAttributeDescription(holder), stack.m_41611_()
                ),
                false
        );

        return 1;
    }

    private static Component getAttributeDescription(Holder<Attribute> attribute) {
        return Component.m_237115_(attribute.m_203334_().m_22087_());
    }

    private static int adjustSlotValidationOnStack(int operation, LivingEntity player, CommandContext<CommandSourceStack> ctx) {
        var slotName = SlotArgumentType.getSlot(ctx, "slot");

        AccessoriesDataComponents.update(
                AccessoriesDataComponents.SLOT_VALIDATION,
                player.m_21205_(),
                component -> {
                    return switch (operation) {
                        case 0 -> component.addValidSlot(slotName);
                        case 1 -> component.addInvalidSlot(slotName);
                        case 2 -> component.removeValidSlot(slotName);
                        case 3 -> component.removeInvalidSlot(slotName);
                        default -> throw new IllegalStateException("Unexpected value: " + operation);
                    };
                }
        );

        return 1;
    }

}
