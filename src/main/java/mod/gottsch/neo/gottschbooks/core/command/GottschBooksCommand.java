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
package mod.gottsch.neo.gottschbooks.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import mod.gottsch.neo.gottschbooks.GottschBooks;
import mod.gottsch.neo.gottschbooks.core.network.OpenBookPayload;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;

/**
 * {@code /gottschbooks open <players> <book> [entry]}: opens a book on the players' screens, a manual at an entry
 * ({@code gems}, or {@code gems#ruby}) when given. For tutorials, command blocks and other mods. Operators only
 * (level 2). The server can't check that the book exists, since books are client assets: a missing book shows the
 * player a "missing" message.
 *
 * @author Mark Gottschling on 10/17/2026
 */
@EventBusSubscriber(modid = GottschBooks.MOD_ID)
public final class GottschBooksCommand {

    private GottschBooksCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(GottschBooks.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("open")
                        .then(Commands.argument("players", EntityArgument.players())
                                .then(Commands.argument("book", ResourceLocationArgument.id())
                                        .executes(c -> open(c.getSource(), EntityArgument.getPlayers(c, "players"),
                                                ResourceLocationArgument.getId(c, "book"), ""))
                                        .then(Commands.argument("entry", StringArgumentType.greedyString())
                                                .executes(c -> open(c.getSource(), EntityArgument.getPlayers(c, "players"),
                                                        ResourceLocationArgument.getId(c, "book"),
                                                        StringArgumentType.getString(c, "entry"))))))));
    }

    private static int open(CommandSourceStack source, Collection<ServerPlayer> players, ResourceLocation book, String entry) {
        OpenBookPayload payload = new OpenBookPayload(book, entry.strip());
        players.forEach(player -> PacketDistributor.sendToPlayer(player, payload));
        source.sendSuccess(() -> Component.translatable("gottschbooks.command.open", book.toString(), players.size()), true);
        return players.size();
    }
}
