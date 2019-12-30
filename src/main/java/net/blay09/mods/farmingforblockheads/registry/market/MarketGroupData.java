package net.blay09.mods.farmingforblockheads.registry.market;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class MarketGroupData {
    private String name;
    private boolean enabledByDefault;
    private ItemStack defaultPayment;
    private ResourceLocation defaultCategory;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    public void setEnabledByDefault(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public ItemStack getDefaultPayment() {
        return defaultPayment;
    }

    public void setDefaultPayment(ItemStack defaultPayment) {
        this.defaultPayment = defaultPayment;
    }

    public ResourceLocation getDefaultCategory() {
        return defaultCategory;
    }

    public void setDefaultCategory(ResourceLocation defaultCategory) {
        this.defaultCategory = defaultCategory;
    }
}
