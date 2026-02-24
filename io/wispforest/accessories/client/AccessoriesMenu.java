package io.wispforest.accessories.client;

import I;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.*;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.client.gui.AccessoriesInternalSlot;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.mixin.SlotAccessor;
import io.wispforest.accessories.networking.server.ScreenOpen;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

public final class AccessoriesMenu extends AbstractContainerMenu {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation BLOCK_ATLAS = new ResourceLocation("textures/atlas/blocks.png");

    public static final ResourceLocation EMPTY_ARMOR_SLOT_SHIELD = new ResourceLocation("item/empty_armor_slot_shield");

    private static final Map<EquipmentSlot, ResourceLocation> TEXTURE_EMPTY_SLOTS = Map.of(
            EquipmentSlot.FEET, new ResourceLocation("item/empty_armor_slot_boots"),
            EquipmentSlot.LEGS, new ResourceLocation("item/empty_armor_slot_leggings"),
            EquipmentSlot.CHEST, new ResourceLocation("item/empty_armor_slot_chestplate"),
            EquipmentSlot.HEAD, new ResourceLocation("item/empty_armor_slot_helmet"));

    private static final EquipmentSlot[] SLOT_IDS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final Player owner;

    @Nullable
    private final LivingEntity targetEntity;

    public int totalSlots = 0;
    public boolean overMaxVisibleSlots = false;

    public int scrolledIndex = 0;

    public float smoothScroll = 0;

    private int maxScrollableIndex = 0;

    private int accessoriesSlotStartIndex = 0;
    private int cosmeticSlotStartIndex = 0;

    private final Set<SlotGroup> validGroups = new HashSet<>();

    private final Map<Integer, Boolean> slotToView = new HashMap<>();

    private Runnable onScrollToEvent = () -> {};

    @Nullable
    private Set<SlotType> usedSlots = null;

    public AccessoriesMenu(int containerId, Inventory inventory, @Nullable LivingEntity targetEntity) {
        super(Accessories.ACCESSORIES_MENU_TYPE, containerId);

        this.owner = inventory.f_35978_;
        this.targetEntity = targetEntity;

        var accessoryTarget = targetEntity != null ? targetEntity : owner;

        var capability = AccessoriesCapability.get(accessoryTarget);

        if (capability == null) return;

        //-- Vanilla Slot Setup

        for (int i = 0; i < 4; i++) {
            var equipmentSlot = SLOT_IDS[i];
            ResourceLocation resourceLocation = TEXTURE_EMPTY_SLOTS.get(equipmentSlot);
            this.m_38897_(new ArmorSlot(inventory, owner, equipmentSlot, 39 - i, 8, 8 + i * 18, resourceLocation));
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.m_38897_(new Slot(inventory, j + (i + 1) * 9, 8 + j * 18, 84 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.m_38897_(new Slot(inventory, i, 8 + i * 18, 142));
        }

        this.m_38897_(new Slot(inventory, 40, 152, 62) {
            @Override
            public void m_269060_(ItemStack oldStack) {
                var newStack = this.m_7993_();
                owner.m_238392_(EquipmentSlot.OFFHAND, oldStack, newStack);
                super.m_269060_(oldStack);
            }

            @Override
            public Pair<ResourceLocation, ResourceLocation> m_7543_() {
                return Pair.of(BLOCK_ATLAS, EMPTY_ARMOR_SLOT_SHIELD);
            }
        });

        //--

        if(!this.areUnusedSlotsShown()) {
            this.usedSlots = ImmutableSet.copyOf(AccessoriesAPI.getUsedSlotsFor(targetEntity != null ? targetEntity : owner, owner.m_150109_()));
        }

        int minX = -46, maxX = 60, minY = 8, maxY = 152;

        int yIndex = 0;

        this.accessoriesSlotStartIndex = this.f_38839_.size();

        var slotVisibility = new HashMap<Slot, Boolean>();

        var accessoriesSlots = new ArrayList<AccessoriesInternalSlot>();
        var cosmeticSlots = new ArrayList<AccessoriesInternalSlot>();

        var groups = SlotGroupLoader.getGroups(inventory.f_35978_.m_9236_(), !this.areUniqueSlotsShown());

        var containers = capability.getContainers();

        var slotTypes = groups.stream().sorted(Comparator.comparingInt(SlotGroup::order).reversed())
                .flatMap(slotGroup -> {
                    return slotGroup.slots().stream()
                            .map(s -> {
                                var slotType = SlotTypeLoader.getSlotType(owner.m_9236_(), s);

                                if(this.usedSlots != null && !this.usedSlots.contains(slotType)) return null;

                                this.validGroups.add(slotGroup);

                                return slotType;
                            })
                            .filter(Objects::nonNull)
                            .sorted(Comparator.comparingInt(SlotType::order).reversed());
                }).toList();

        //LOGGER.info("SlotTypes for [{}] Screen: {}", (owner.level().isClientSide() ? "client" : "server"), slotTypes);
        //LOGGER.info("Containers for [{}] Screen: {}", (owner.level().isClientSide() ? "client" : "server"), containers.keySet());

        for (var slot : slotTypes) {
            var accessoryContainer = containers.get(slot.name());

            if (accessoryContainer == null || accessoryContainer.slotType() == null) continue;

            var size = accessoryContainer.getSize();

            for (int i = 0; i < size; i++) {
                int currentY = (yIndex * 18) + minY + 8;

                int currentX = minX;

                var cosmeticSlot = new AccessoriesInternalSlot(yIndex, accessoryContainer, true, i, currentX, currentY)
                                .isActive((slot1) -> this.isCosmeticsOpen() && this.slotToView.getOrDefault(slot1.f_40219_, true))
                                .isAccessible(slot1 -> slot1.isCosmetic && isCosmeticsOpen());

                cosmeticSlots.add(cosmeticSlot);

                slotVisibility.put(cosmeticSlot, !this.overMaxVisibleSlots);

                currentX += 18 + 2;

                var baseSlot = new AccessoriesInternalSlot(yIndex, accessoryContainer, false, i, currentX, currentY)
                                .isActive(slot1 -> this.slotToView.getOrDefault(slot1.f_40219_, true));

                accessoriesSlots.add(baseSlot);

                slotVisibility.put(baseSlot, !this.overMaxVisibleSlots);

                yIndex++;

                if (!this.overMaxVisibleSlots && currentY + 18 > maxY) this.overMaxVisibleSlots = true;
            }
        }

        for (var accessoriesSlot : accessoriesSlots) {
            this.m_38897_(accessoriesSlot);

            slotToView.put(accessoriesSlot.f_40219_, slotVisibility.getOrDefault(accessoriesSlot, false));
        }

        this.cosmeticSlotStartIndex = this.f_38839_.size();

        for (var cosmeticSlot : cosmeticSlots) {
            this.m_38897_(cosmeticSlot);

            this.slotToView.put(cosmeticSlot.f_40219_, slotVisibility.getOrDefault(cosmeticSlot, false));
        }

        this.totalSlots = yIndex;

        this.maxScrollableIndex = this.totalSlots - 8;
    }

    public void setScrollEvent(Runnable event) {
        this.onScrollToEvent = event;
    }

    public boolean scrollTo(int i, boolean smooth) {
        var index = Math.min(Math.max(i, 0), this.maxScrollableIndex);

        if (index == this.scrolledIndex) return false;

        var diff = this.scrolledIndex - index;

        if (!smooth) this.smoothScroll = Mth.m_14036_(index / (float) this.maxScrollableIndex, 0.0f, 1.0f);

        for (Slot slot : this.f_38839_) {
            if (!(slot instanceof AccessoriesInternalSlot accessoriesSlot)) continue;

            ((SlotAccessor) accessoriesSlot).accessories$setY(accessoriesSlot.f_40221_ + (diff * 18));

            var menuIndex = accessoriesSlot.menuIndex;

            this.slotToView.put(accessoriesSlot.f_40219_, (menuIndex >= index && menuIndex < index + 8));
        }

        this.scrolledIndex = index;

        this.onScrollToEvent.run();

        return true;
    }

    public int maxScrollableIndex(){
        return this.maxScrollableIndex;
    }

    @Nullable
    public LivingEntity targetEntity() {
        return this.targetEntity;
    }

    public Player owner() {
        return this.owner;
    }

    public static AccessoriesMenu of(int containerId, Inventory inventory, AccessoriesMenuData data) {
        var targetEntity = data.targetEntityId().map(i -> {
            var entity = inventory.f_35978_.m_9236_().m_6815_(i);

            if(entity instanceof LivingEntity livingEntity) return livingEntity;

            return null;
        }).orElse(null);

        return new AccessoriesMenu(containerId, inventory, targetEntity);
    }

    public boolean showingSlots() {
        return this.usedSlots == null || !this.usedSlots.isEmpty();
    }

    @Nullable
    public Set<SlotType> usedSlots() {
        return this.usedSlots;
    }

    public Set<SlotGroup> validGroups() {
        return this.validGroups;
    }

    public boolean isCosmeticsOpen() {
        return Optional.ofNullable(AccessoriesHolder.get(owner)).map(AccessoriesHolder::cosmeticsShown).orElse(false);
    }

    public boolean areLinesShown() {
        return Optional.ofNullable(AccessoriesHolder.get(owner)).map(AccessoriesHolder::linesShown).orElse(false);
    }

    public boolean areUnusedSlotsShown() {
        return Optional.ofNullable(AccessoriesHolder.get(owner)).map(AccessoriesHolder::showUnusedSlots).orElse(false);
    }

    public boolean areUniqueSlotsShown() {
        return Optional.ofNullable(AccessoriesHolder.get(owner)).map(AccessoriesHolder::showUniqueSlots).orElse(false);
    }

    public void reopenMenu() {
        AccessoriesInternals.getNetworkHandler().sendToServer(ScreenOpen.of(this.targetEntity));
    }

    //--

    @Override
    public boolean m_6875_(Player player) {
        return true;
    }

    @Override
    public ItemStack m_7648_(Player player, int clickedIndex) {
        final var slots = this.f_38839_;
        final var clickedSlot = slots.get(clickedIndex);
        if (!clickedSlot.m_6657_()) return ItemStack.f_41583_;

        ItemStack clickedStack = clickedSlot.m_7993_();
        var oldStack = clickedStack.m_41777_();
        EquipmentSlot equipmentSlot = Mob.m_147233_(oldStack);

        int armorSlots = 4;
        int hotbarSlots = 9;
        int invSlots = 27;

        int armorStart = 0;
        int armorEnd = armorStart - 1 + armorSlots;
        int invStart = armorEnd + 1;
        int invEnd = invStart - 1 + invSlots;
        int hotbarStart = invEnd + 1;
        int hotbarEnd = hotbarStart - 1 + hotbarSlots;
        int offhand = hotbarEnd + 1;

        // If the clicked slot isn't an accessory slot
        if (clickedIndex < this.accessoriesSlotStartIndex) {
            // Try to move to accessories
            if (!this.m_38903_(clickedStack, this.accessoriesSlotStartIndex, this.f_38839_.size(), false)) {
                // If the clicked slot is one of the armor slots
                if (clickedIndex >= armorStart && clickedIndex <= armorEnd) {
                    // Try to move to the inventory or hotbar
                    if (!this.m_38903_(clickedStack, invStart, hotbarEnd, false)) {
                        return ItemStack.f_41583_;
                    }
                    // If the clicked slot can go into an armor slot and said armor slot is empty
                } else if (equipmentSlot.m_20743_() == EquipmentSlot.Type.ARMOR && !this.f_38839_.get(armorEnd - equipmentSlot.m_20749_()).m_6657_()) {
                    // Try to move to the armor slot
                    int targetArmorSlotIndex = armorEnd - equipmentSlot.m_20749_();
                    if (!this.m_38903_(clickedStack, targetArmorSlotIndex, targetArmorSlotIndex + 1, false)) {
                        return ItemStack.f_41583_;
                    }
                    // If the clicked slot can go into the offhand slot and the offhand slot is empty
                } else if (equipmentSlot == EquipmentSlot.OFFHAND && !this.f_38839_.get(offhand).m_6657_()) {
                    // Try to move to the offhand slot
                    if (!this.m_38903_(clickedStack, offhand, offhand + 1, false)) {
                        return ItemStack.f_41583_;
                    }
                    // If the clicked slot is in the hotbar
                } else if (clickedIndex >= hotbarStart && clickedIndex <= hotbarEnd) {
                    // Try to move to the inventory
                    if (!this.m_38903_(clickedStack, invStart, invEnd, false)) {
                        return ItemStack.f_41583_;
                    }
                    // If the clicked slot is in the inventory
                } else if (clickedIndex >= invStart && clickedIndex <= invEnd) {
                    // Try to move to the hotbar
                    if (!this.m_38903_(clickedStack, hotbarStart, hotbarEnd, false)) {
                        return ItemStack.f_41583_;
                    }
                    // Try to move to the inventory or hotbar
                } else if (!this.m_38903_(clickedStack, invStart, hotbarEnd, false)) {
                    return ItemStack.f_41583_;
                }
            }
        } else if (!this.m_38903_(clickedStack, invStart, hotbarEnd, false)) {
            return ItemStack.f_41583_;
        }

        if (clickedStack.m_41619_()) {
            clickedSlot.m_269060_(ItemStack.f_41583_);
        } else {
            clickedSlot.m_6654_();
        }

        if (clickedStack.m_41613_() == oldStack.m_41613_()) {
            return ItemStack.f_41583_;
        }

        clickedSlot.m_142406_(player, clickedStack);

        return oldStack;
    }

    @Override
    protected boolean m_38903_(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
        boolean bl = false;
        int i = startIndex;
        if (reverseDirection) {
            i = endIndex - 1;
        }

        if (stack.m_41753_()) {
            while(!stack.m_41619_() && (reverseDirection ? i >= startIndex : i < endIndex)) {
                Slot slot = this.f_38839_.get(i);
                ItemStack itemStack = slot.m_7993_();

                //Check if the slot dose not permit the given amount
                if(slot.m_5866_(itemStack) < itemStack.m_41613_()) {
                    if (!itemStack.m_41619_() && ItemStack.m_150942_(stack, itemStack)) {
                        int j = itemStack.m_41613_() + stack.m_41613_();
                        if (j <= stack.m_41741_()) {
                            stack.m_41764_(0);
                            itemStack.m_41764_(j);
                            slot.m_6654_();
                            bl = true;
                        } else if (itemStack.m_41613_() < stack.m_41741_()) {
                            stack.m_41774_(stack.m_41741_() - itemStack.m_41613_());
                            itemStack.m_41764_(stack.m_41741_());
                            slot.m_6654_();
                            bl = true;
                        }
                    }
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        if (!stack.m_41619_()) {
            if (reverseDirection) {
                i = endIndex - 1;
            } else {
                i = startIndex;
            }

            while(reverseDirection ? i >= startIndex : i < endIndex) {
                Slot slot = this.f_38839_.get(i);
                ItemStack itemStack = slot.m_7993_();
                if (itemStack.m_41619_() && slot.m_5857_(stack)) {
                    //Use Stack aware form of getMaxStackSize
                    if (stack.m_41613_() > slot.m_5866_(stack)) {
                        slot.m_269060_(stack.m_41620_(slot.m_5866_(stack)));
                    } else {
                        slot.m_269060_(stack.m_41620_(stack.m_41613_()));
                    }

                    slot.m_6654_();
                    bl = true;
                    break;
                }

                if (reverseDirection) {
                    --i;
                } else {
                    ++i;
                }
            }
        }

        return bl;
    }

    //--
}