package com.infiniteinv;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InfiniteInvMod implements ModInitializer {
    public static final String MOD_ID = "infiniteinv";

    public record OpenInfiniteInvPayload() implements CustomPayload {
        public static final CustomPayload.Id<OpenInfiniteInvPayload> ID = new CustomPayload.Id<>(Identifier.of(MOD_ID, "open_inv"));
        public static final PacketCodec<RegistryByteBuf, OpenInfiniteInvPayload> CODEC = PacketCodec.unit(new OpenInfiniteInvPayload());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(OpenInfiniteInvPayload.ID, OpenInfiniteInvPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OpenInfiniteInvPayload.ID, (payload, context) -> {
            context.server().execute(() -> openPage(context.player(), BigInteger.ONE));
        });
    }

    public static void openPage(ServerPlayerEntity player, BigInteger page) {
        if (page.compareTo(BigInteger.ONE) < 0) page = BigInteger.ONE;
        final BigInteger currentPage = page;
        InfiniteStorageState state = InfiniteStorageState.getServerState(player.getServer());
        SimpleInventory inv = state.getPageInventory(player.getUuid(), currentPage, player.getRegistryManager());

        player.openHandledScreen(new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.literal("∞ Inventario - Pág. " + currentPage.toString());
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity p) {
                return new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, inv, 6) {
                    @Override
                    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity clicker) {
                        if (slotIndex == 45) {
                            state.savePageInventory(player.getUuid(), currentPage, inv, player.getRegistryManager());
                            openPage(player, currentPage.subtract(BigInteger.ONE));
                            return;
                        }
                        if (slotIndex == 53) {
                            state.savePageInventory(player.getUuid(), currentPage, inv, player.getRegistryManager());
                            openPage(player, currentPage.add(BigInteger.ONE));
                            return;
                        }
                        super.onSlotClick(slotIndex, button, actionType, clicker);
                        state.savePageInventory(player.getUuid(), currentPage, inv, player.getRegistryManager());
                    }

                    @Override
                    public void onClosed(PlayerEntity p) {
                        state.savePageInventory(player.getUuid(), currentPage, inv, player.getRegistryManager());
                        super.onClosed(p);
                    }
                };
            }
        });
    }

    public static class InfiniteStorageState extends PersistentState {
        private final Map<String, NbtList> pages = new HashMap<>();

        public static final Type<InfiniteStorageState> TYPE = new Type<>(
            InfiniteStorageState::new,
            InfiniteStorageState::createFromNbt,
            null
        );

        public static InfiniteStorageState getServerState(MinecraftServer server) {
            PersistentStateManager manager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
            return manager.getOrCreate(TYPE, MOD_ID + "_storage");
        }

        public static InfiniteStorageState createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registries) {
            InfiniteStorageState state = new InfiniteStorageState();
            NbtCompound data = tag.getCompound("pages");
            for (String key : data.getKeys()) {
                state.pages.put(key, data.getList(key, 10));
            }
            return state;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
            NbtCompound data = new NbtCompound();
            pages.forEach(data::put);
            nbt.put("pages", data);
            return nbt;
        }

        public SimpleInventory getPageInventory(UUID uuid, BigInteger page, RegistryWrapper.WrapperLookup registries) {
            SimpleInventory inv = new SimpleInventory(54);
            String key = uuid.toString() + "_" + page.toString();
            if (pages.containsKey(key)) {
                inv.readNbtList(pages.get(key), registries);
            }
            return inv;
        }

        public void savePageInventory(UUID uuid, BigInteger page, SimpleInventory inv, RegistryWrapper.WrapperLookup registries) {
            String key = uuid.toString() + "_" + page.toString();
            pages.put(key, inv.toNbtList(registries));
            markDirty();
        }
    }
}
