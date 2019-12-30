package net.blay09.mods.farmingforblockheads.registry.market;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.blay09.mods.farmingforblockheads.FarmingForBlockheads;
import net.blay09.mods.farmingforblockheads.api.FarmingForBlockheadsAPI;
import net.blay09.mods.farmingforblockheads.api.IMarketCategory;
import net.blay09.mods.farmingforblockheads.api.IMarketRegistryDefaultHandler;
import net.blay09.mods.farmingforblockheads.api.MarketRegistryReloadEvent;
import net.blay09.mods.farmingforblockheads.registry.MarketCategory;
import net.blay09.mods.farmingforblockheads.registry.MarketRegistry;
import net.blay09.mods.farmingforblockheads.registry.json.ItemStackSerializer;
import net.blay09.mods.farmingforblockheads.registry.json.MarketRegistryDataSerializer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = FarmingForBlockheads.MOD_ID)
public class MarketRegistryLoader implements IResourceManagerReloadListener {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(ResourceLocation.class, new ResourceLocation.Serializer())
            .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
            .registerTypeAdapter(MarketRegistryData.class, new MarketRegistryDataSerializer())
            .create();

    private static final List<Exception> registryErrors = new ArrayList<>();

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        MarketRegistry.INSTANCE.reset();
        registryErrors.clear();

        MinecraftForge.EVENT_BUS.post(new MarketRegistryReloadEvent.Pre());

        for (ResourceLocation resourceLocation : resourceManager.getAllResourceLocations("farmingforblockheads_compat", it -> it.endsWith(".json"))) {
            try (IResource resource = resourceManager.getResource(resourceLocation)) {
                InputStreamReader reader = new InputStreamReader(resource.getInputStream());
                load(gson.fromJson(reader, MarketRegistryData.class));
            } catch (Exception e) {
                FarmingForBlockheads.logger.error("Parsing error loading Farming for Blockheads data file at {}", resourceLocation, e);
                registryErrors.add(e);
            }
        }

        File configFile = new File(FMLPaths.CONFIGDIR.get().toFile(), "MarketRegistry.json");
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                load(gson.fromJson(reader, MarketRegistryData.class));
            } catch (Exception e) {
                FarmingForBlockheads.logger.error("Parsing error loading Farming for Blockheads data from MarketRegistry.json", e);
                registryErrors.add(e);
            }
        } else {
            try (FileWriter reader = new FileWriter(configFile)) {
                gson.toJson(new MarketRegistryData(), reader);
            } catch (IOException ignored) {
            }
        }

        MarketRegistry.INSTANCE.registerDefaults();

        MinecraftForge.EVENT_BUS.post(new MarketRegistryReloadEvent.Post());
    }

    private void load(MarketRegistryData data) {
        if (data.getModId() != null && !ModList.get().isLoaded(data.getModId())) {
            return;
        }

        if (data.getGroup() != null) {
            FarmingForBlockheadsAPI.registerMarketDefaultHandler(data.getGroup().getName(), new IMarketRegistryDefaultHandler() {
                @Override
                public void register(ItemStack defaultPayment, int defaultAmount) {
                    loadMarketData(data);
                }

                @Override
                public boolean isEnabledByDefault() {
                    return data.getGroup().isEnabledByDefault();
                }

                @Override
                public ItemStack getDefaultPayment() {
                    return new ItemStack(Items.EMERALD);
                }
            });
        } else {
            loadMarketData(data);
        }
    }

    private void loadMarketData(MarketRegistryData data) {
        if (data.getCustomCategories() != null) {
            data.getCustomCategories().forEach((key, categoryData) -> {
                ResourceLocation resourceLocation = new ResourceLocation(key);
                MarketRegistry.INSTANCE.registerCategory(new MarketCategory(resourceLocation, categoryData.getName(), categoryData.getIcon(), categoryData.getSortIndex()));
            });
        }

        if (data.getEntryOverrides() != null) {
            data.getEntryOverrides().forEach(MarketRegistry.INSTANCE::registerEntryOverride);
        }

        if (data.getGroupOverrides() != null) {
            data.getGroupOverrides().forEach(MarketRegistry.INSTANCE::registerGroupOverride);
        }

        if (data.getCustomEntries() != null) {
            data.getCustomEntries().forEach(it -> {
                ResourceLocation categoryKey = it.getCategory();
                if (categoryKey == null) {
                    if (data.getGroup() != null) {
                        categoryKey = data.getGroup().getDefaultCategory();
                    } else {
                        categoryKey = new ResourceLocation("farmingforblockheads:other");
                    }
                }
                IMarketCategory category = MarketRegistry.getCategory(categoryKey);

                ItemStack payment = it.getPayment();
                if (payment == null) {
                    if (data.getGroup() != null) {
                        payment = data.getGroup().getDefaultPayment();
                    } else {
                        payment = new ItemStack(Items.EMERALD);
                    }
                }

                MarketRegistry.INSTANCE.registerEntry(it.getOutput(), payment, category);
            });
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!registryErrors.isEmpty()) {
            event.getPlayer().sendStatusMessage(getErrorTextComponent("There were registry errors in the FarmingForBlockheads market data. See the log for full details."), false);
            for (Exception registryError : registryErrors) {
                event.getPlayer().sendStatusMessage(getErrorTextComponent("- " + registryError.getMessage()), false);
            }
        }
    }

    private static StringTextComponent getErrorTextComponent(String message) {
        StringTextComponent result = new StringTextComponent(message);
        result.getStyle().setColor(TextFormatting.RED);
        return result;
    }
}
