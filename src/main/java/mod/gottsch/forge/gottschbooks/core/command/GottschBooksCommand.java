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
package mod.gottsch.forge.gottschbooks.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import mod.gottsch.forge.gottschbooks.GottschBooks;
import mod.gottsch.forge.gottschbooks.api.BookDef;
import mod.gottsch.forge.gottschbooks.api.Books;
import mod.gottsch.forge.gottschbooks.api.GottschBooksRegistries;
import mod.gottsch.forge.gottschbooks.core.BookGiving;
import mod.gottsch.forge.gottschbooks.core.network.OpenBookPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegisterCommandsEvent;
import mod.gottsch.forge.gottschbooks.core.network.GottschBooksNetwork;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@code /gottschbooks open <players> <book> [entry]}: opens a book on the players' screens, a manual at an entry
 * ({@code gems}, or {@code gems#ruby}) when given. For tutorials, command blocks and other mods. Operators only
 * (level 2). The server can't check that the book exists, since books are client assets: a missing book shows the
 * player a "missing" message.
 * <p>
 * {@code /gottschbooks get <book>}: any player takes a copy of a book that is free to everyone, one whose entry gives
 * or announces it on first join; it is what the announcement's [Get it] link runs.
 *
 * @author Mark Gottschling on 10/17/2026
 */
@Mod.EventBusSubscriber(modid = GottschBooks.MOD_ID)
public final class GottschBooksCommand {

    private GottschBooksCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(GottschBooks.MOD_ID)
                .then(Commands.literal("open")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("book", ResourceLocationArgument.id())
                                        .executes(c -> open(c.getSource(), EntityArgument.getPlayers(c, "players"),
                                                ResourceLocationArgument.getId(c, "book"), ""))
                                        .then(Commands.argument("entry", StringArgumentType.greedyString())
                                                .executes(c -> open(c.getSource(), EntityArgument.getPlayers(c, "players"),
                                                        ResourceLocationArgument.getId(c, "book"),
                                                        StringArgumentType.getString(c, "entry")))))))
                .then(Commands.literal("get")
                        .requires(CommandSourceStack::isPlayer)
                        .then(Commands.argument("book", ResourceLocationArgument.id())
                                .suggests((c, builder) -> SharedSuggestionProvider.suggestResource(free(c.getSource()), builder))
                                .executes(c -> get(c.getSource().getPlayerOrException(), ResourceLocationArgument.getId(c, "book"))))));
    }

    /** gives the player a free book (one given or announced on first join), unless they already carry it */
    private static int get(ServerPlayer player, ResourceLocation id) {
        Optional<BookDef> def = Books.getDef(id, player.level().registryAccess()).filter(BookDef::free);
        if (def.isEmpty()) {
            player.sendSystemMessage(Component.translatable("gottschbooks.command.get.unavailable", id.toString())
                    .withStyle(ChatFormatting.RED));
            return 0;
        }
        if (BookGiving.carries(player.getInventory(), id)) {
            player.sendSystemMessage(Component.translatable("gottschbooks.command.get.carried",
                    BookGiving.name(BookGiving.stack(def.get(), id), id)));
            return 0;
        }
        ItemStack given = BookGiving.give(player, def.get(), id);
        player.sendSystemMessage(Component.translatable("gottschbooks.command.get", BookGiving.name(given, id)));
        return 1;
    }

    /** the books any player may get */
    private static Stream<ResourceLocation> free(CommandSourceStack source) {
        return source.registryAccess().registry(GottschBooksRegistries.BOOK).stream()
                .flatMap(books -> books.holders().filter(book -> book.value().free()).map(book -> book.key().location()));
    }

    private static int open(CommandSourceStack source, Collection<ServerPlayer> players, ResourceLocation book, String entry) {
        OpenBookPayload payload = new OpenBookPayload(book, entry.strip());
        players.forEach(player -> GottschBooksNetwork.send(player, payload));
        source.sendSuccess(() -> Component.translatable("gottschbooks.command.open", book.toString(), players.size()), true);
        return players.size();
    }
}
