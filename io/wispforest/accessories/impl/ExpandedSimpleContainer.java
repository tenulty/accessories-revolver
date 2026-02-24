package io.wispforest.accessories.impl;

import Z;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesCapability;
import it.unimi.dsi.fastutil.ints.Int2BooleanArrayMap;
import it.unimi.dsi.fastutil.ints.Int2BooleanMap;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * An implementation of SimpleContainer with easy utilities for iterating over the stacks
 * and holding on to previous stack info
 */
public class ExpandedSimpleContainer extends SimpleContainer implements Iterable<Pair<Integer, ItemStack>> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final AccessoriesContainerImpl container;

    private final String name;
    private final NonNullList<ItemStack> previousItems;
    private final Int2BooleanMap setFlags = new Int2BooleanArrayMap();

    private boolean newlyConstructed;

    public ExpandedSimpleContainer(AccessoriesContainerImpl container, int size, String name) {
        this(container, size, name, true);
    }

    public ExpandedSimpleContainer(AccessoriesContainerImpl container, int size, String name, boolean toggleNewlyConstructed) {
        super(size);

        this.container = container;

        this.m_19164_(container);

        if(toggleNewlyConstructed) this.newlyConstructed = true;

        this.name = name;
        this.previousItems = NonNullList.m_122780_(size, ItemStack.f_41583_);
    }

    public String name() {
        return this.name;
    }

    //--

    public boolean wasNewlyConstructed() {
        var bl = newlyConstructed;

        this.newlyConstructed = false;

        return bl;
    }

    public boolean isSlotFlagged(int slot){
        var bl = setFlags.getOrDefault(slot, false);

        if(bl) setFlags.put(slot, false);

        return bl;
    }

    public void setPreviousItem(int slot, ItemStack stack) {
        if(!validIndex(slot)) return;
        this.previousItems.set(slot, stack);
        if (!stack.m_41619_() && stack.m_41613_() > this.m_6893_()) {
            stack.m_41764_(this.m_6893_());
        }
    }

    public ItemStack getPreviousItem(int slot) {
        return slot >= 0 && slot < this.previousItems.size()
                ? this.previousItems.get(slot)
                : ItemStack.f_41583_;
    }

    //--

    @Override
    public ItemStack m_8020_(int slot) {
        if(!validIndex(slot)) return ItemStack.f_41583_;

        return super.m_8020_(slot);
    }

    @Override
    public ItemStack m_7407_(int slot, int amount) {
        if(!validIndex(slot)) return ItemStack.f_41583_;

        var stack = super.m_7407_(slot, amount);

        if (!stack.m_41619_()) {
            setFlags.put(slot, true);
        }

        return stack;
    }

    @Override
    public ItemStack m_8016_(int slot) {
        if(!validIndex(slot)) return ItemStack.f_41583_;

        // TODO: Concerning the flagging system, should this work for it?

        return super.m_8016_(slot);
    }

    @Override
    public void m_6836_(int slot, ItemStack stack) {
        if(!validIndex(slot)) return;

        super.m_6836_(slot, stack);

        setFlags.put(slot, true);
    }

    // Simple validation method to make sure that the given access is valid before attempting an operation
    public boolean validIndex(int slot){
        var isValid = slot >= 0 && slot < this.m_6643_();

        var nameInfo = (this.name != null ? "Container: " + this.name + ", " : "");

        if(!isValid && AccessoriesInternals.isDevelopmentEnv()){
            try {
                throw new IllegalStateException("Access to a given Inventory was found to be out of the range valid for the container! [Name: " + nameInfo + " Index: " + slot + "]");
            } catch (Exception e) {
                LOGGER.debug("Full Exception: ", e);
            }
        }

        return isValid;
    }

    //--

    @Override
    public void m_7797_(ListTag containerNbt) {
        this.container.containerListenerLock = true;

        var capability = this.container.capability;

        var prevStacks = new ArrayList<ItemStack>();

        for(int i = 0; i < this.m_6643_(); ++i) {
            var currentStack = this.m_8020_(i);

            prevStacks.add(currentStack);

            this.m_6836_(i, ItemStack.f_41583_);
        }

        var invalidStacks = new ArrayList<ItemStack>();
        var decodedStacks = new ArrayList<ItemStack>();

        for(int i = 0; i < containerNbt.size(); ++i) {
            var compoundTag = containerNbt.m_128728_(i);

            int j = compoundTag.m_128451_("Slot");

            var stack = ItemStack.m_41712_(compoundTag);

            decodedStacks.add(stack);

            if (j >= 0 && j < this.m_6643_()) {
                this.m_6836_(j, stack);
            } else {
                invalidStacks.add(stack);
            }
        }

        this.container.containerListenerLock = false;

        if (!capability.entity().m_9236_().m_5776_()) {
            if (!prevStacks.equals(decodedStacks)) {
                this.m_6596_();
            }

            ((AccessoriesHolderImpl) capability.getHolder()).invalidStacks.addAll(invalidStacks);
        }
    }

    @Override
    public ListTag m_7927_() {
        ListTag listTag = new ListTag();

        for(int i = 0; i < this.m_6643_(); ++i) {
            ItemStack itemStack = this.m_8020_(i);

            if (!itemStack.m_41619_()) {
                var compoundTag = new CompoundTag();

                compoundTag.m_128405_("Slot", i);

                listTag.add(itemStack.m_41739_(compoundTag));
            }
        }

        return listTag;
    }

    //--

    @NotNull
    @Override
    public Iterator<Pair<Integer, ItemStack>> iterator() {
        return new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < ExpandedSimpleContainer.this.m_6643_();
            }

            @Override
            public Pair<Integer, ItemStack> next() {
                var pair = new Pair<>(index, ExpandedSimpleContainer.this.m_8020_(index));

                index++;

                return pair;
            }
        };
    }

    public void setFromPrev(ExpandedSimpleContainer prevContainer) {
        prevContainer.forEach(pair -> this.setPreviousItem(pair.getFirst(), pair.getSecond()));
    }

    public void copyPrev(ExpandedSimpleContainer prevContainer) {
        for (int i = 0; i < prevContainer.m_6643_(); i++) {
            if(i >= this.m_6643_()) continue;

            var prevItem = prevContainer.getPreviousItem(i);

            if(!prevItem.m_41619_()) this.setPreviousItem(i, prevItem);
        }
    }
}
