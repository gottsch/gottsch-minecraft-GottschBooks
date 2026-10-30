/*
 * This file is part of Gottsch Books.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * Gottsch Books is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Gottsch Books is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Gottsch Books.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.neo.gottschbooks.core;

import mod.gottsch.neo.gottschbooks.GottschBooks;
import mod.gottsch.neo.gottschbooks.api.BookDef;
import mod.gottsch.neo.gottschbooks.api.BookItem;
import mod.gottsch.neo.gottschbooks.api.Books;
import mod.gottsch.neo.gottschbooks.core.network.OpenBookPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/**
 * Gottsch Books books on vanilla lecterns. Any {@link BookItem} stack that names a book goes on an empty lectern
 * (no {@code #minecraft:lectern_books} tag needed, so a mod's own book items work too). Right-clicking the lectern
 * opens the book for that player, instead of vanilla's lectern screen, which can't show it, and counts as reading it
 * ({@code gottschbooks:book_read}, for tracked books). Sneaking with both hands empty takes the book off. See
 * {@link LecternUse} for the rules.
 * <p>
 * The lectern keeps the stack as it is, so breaking the lectern drops the book with its id. Vanilla's comparator
 * signal follows a written book's page; a Gottsch Books book has no vanilla pages, so the signal is steady while it
 * is on the lectern.
 * <p>
 * Common code: the server does the work (the client doesn't know which book a lectern holds), and the client only
 * cancels a placing click, so it doesn't also open the book in hand.
 *
 * @author Mark Gottschling on 10/28/2026
 */
@EventBusSubscriber(modid = GottschBooks.MOD_ID)
public final class LecternBooks {

    private LecternBooks() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Player player = event.getEntity();
        if (!(state.getBlock() instanceof LecternBlock) || player.isSpectator()) {
            return;
        }
        boolean hasBook = state.getValue(LecternBlock.HAS_BOOK);
        LecternBlockEntity lectern = level.getBlockEntity(pos) instanceof LecternBlockEntity be ? be : null;
        if (lectern == null || (hasBook && level.isClientSide())) {
            return;
        }
        Optional<ResourceLocation> onLectern = hasBook ? bookId(lectern.getBook()) : Optional.empty();
        ItemStack held = event.getItemStack();
        LecternUse use = LecternUse.of(hasBook, onLectern.isPresent(), bookId(held).isPresent(),
                event.getHand() == InteractionHand.MAIN_HAND, player.isSecondaryUseActive(),
                player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty());
        switch (use) {
            case PLACE -> {
                // vanilla's own placing: takes one from the stack, sets the block, plays the sound
                if (!LecternBlock.tryPlaceBook(player, level, pos, state, held)) {
                    return;
                }
                event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
            }
            case READ -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    read(serverPlayer, onLectern.get());
                }
                event.setCancellationResult(InteractionResult.CONSUME);
            }
            case TAKE -> {
                take(player, level, pos, state, lectern);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
            case NONE -> {
                return;
            }
        }
        event.setCanceled(true);
    }

    /** which book {@code stack} is, when it is a Gottsch Books book */
    private static Optional<ResourceLocation> bookId(ItemStack stack) {
        return stack.getItem() instanceof BookItem item ? item.bookId(stack) : Optional.empty();
    }

    private static void read(ServerPlayer player, ResourceLocation id) {
        PacketDistributor.sendToPlayer(player, new OpenBookPayload(id, ""));
        player.awardStat(Stats.INTERACT_WITH_LECTERN);
        if (Books.getDef(id, player.registryAccess()).filter(BookDef::tracked).isPresent()) {
            Books.markRead(player, id);
        }
    }

    /** as vanilla's "Take Book": into the player's inventory, or at their feet when it's full */
    private static void take(Player player, Level level, BlockPos pos, BlockState state, LecternBlockEntity lectern) {
        ItemStack book = lectern.getBook().copy();
        lectern.setBook(ItemStack.EMPTY);
        LecternBlock.resetBookState(player, level, pos, state, false);
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
    }
}
