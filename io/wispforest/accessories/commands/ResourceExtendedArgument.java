package io.wispforest.accessories.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.attributes.SlotAttribute;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class ResourceExtendedArgument<T> implements ArgumentType<Holder<T>> {

    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");

    private static final DynamicCommandExceptionType ERROR_NOT_SUMMONABLE_ENTITY = new DynamicCommandExceptionType(
            object -> Component.m_237110_("entity.not_summonable", object)
    );
    public static final Dynamic2CommandExceptionType ERROR_UNKNOWN_RESOURCE = new Dynamic2CommandExceptionType(
            (object, object2) -> Component.m_237110_("argument.resource.not_found", object, object2)
    );
    public static final Dynamic3CommandExceptionType ERROR_INVALID_RESOURCE_TYPE = new Dynamic3CommandExceptionType(
            (object, object2, object3) -> Component.m_237110_("argument.resource.invalid_type", object, object2, object3)
    );

    final ResourceKey<? extends Registry<T>> registryKey;
    private final HolderLookup<T> registryLookup;

    private final Function<ResourceLocation, @Nullable T> additionalLookup;
    private final Supplier<Stream<ResourceLocation>> additionalSuggestions;

    public ResourceExtendedArgument(CommandBuildContext context, ResourceKey<? extends Registry<T>> registryKey, Function<ResourceLocation, @Nullable T> additionalLookup, Supplier<Stream<ResourceLocation>> additionalSuggestions) {
        this.registryKey = registryKey;
        this.registryLookup = context.m_227133_(registryKey);

        this.additionalLookup = additionalLookup;
        this.additionalSuggestions = additionalSuggestions;
    }

    public static <T> ResourceExtendedArgument<T> resource(CommandBuildContext context, ResourceKey<? extends Registry<T>> registryKey, Function<ResourceLocation, @Nullable T> additionalLookup, Supplier<Stream<ResourceLocation>> additionalSuggestions) {
        return new ResourceExtendedArgument<>(context, registryKey, additionalLookup, additionalSuggestions);
    }

    public static ResourceExtendedArgument<Attribute> attributes(CommandBuildContext context) {
        return new ResourceExtendedArgument<>(context, Registries.f_256728_, location -> {
            String possibleSlotName;

            if (location.m_135827_().equals(Accessories.MODID)) {
                possibleSlotName = location.m_135815_();
            } else {
                possibleSlotName = location.toString();
            }

            var slotType = SlotTypeLoader.INSTANCE.getSlotTypes(false).get(possibleSlotName);

            return (slotType != null) ? SlotAttribute.getSlotAttribute(possibleSlotName) : null;
        }, () -> {
            return SlotTypeLoader.INSTANCE.getSlotTypes(false).values()
                    .stream()
                    .map(SlotType::name)
                    .map(s -> s.contains(":") ? ResourceLocation.m_135820_(s) : Accessories.of(s));
        });
    }

    public static Holder<Attribute> getAttribute(CommandContext<CommandSourceStack> commandContext, String string){
        return getResource(commandContext, string);
    }

    public static <T> Holder<T> getResource(CommandContext<CommandSourceStack> context, String argument){
        return (Holder<T>) context.getArgument(argument, Holder.class);
    }

    public Holder<T> parse(StringReader builder) throws CommandSyntaxException {
        ResourceLocation resourceLocation = ResourceLocation.m_135818_(builder);
        ResourceKey<T> resourceKey = ResourceKey.m_135785_(this.registryKey, resourceLocation);

        var entry = this.registryLookup.m_254902_(resourceKey)
                .map(tReference -> (Holder<T>) tReference)
                .or(() -> Optional.ofNullable(this.additionalLookup.apply(resourceLocation)).map(Holder::m_205709_));

        return entry.orElseThrow(() -> ERROR_UNKNOWN_RESOURCE.createWithContext(builder, resourceLocation, this.registryKey.m_135782_()));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        var registryKeys = this.registryLookup.m_255209_().map(ResourceKey::m_135782_);
        var extraEntries = this.additionalSuggestions.get();

        return SharedSuggestionProvider.m_82957_(Stream.concat(registryKeys, extraEntries), suggestionsBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
