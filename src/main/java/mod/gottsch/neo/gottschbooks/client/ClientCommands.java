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
package mod.gottsch.neo.gottschbooks.client;

import com.mojang.brigadier.CommandDispatcher;
import mod.gottsch.neo.gottschbooks.GottschBooks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * {@code /gottschbooks check [book]}: lists what is wrong in every loaded book, or in one, in chat and in the log.
 * Books are client assets, resource packs included, so the check runs on the client, for anyone.
 * {@code /gottschbooks lookup hint [on|off]} shows or hides the item lookup's tooltip line. These share their root
 * with the server's {@code /gottschbooks open}; NeoForge hands the server anything the client tree doesn't know.
 *
 * @author Mark Gottschling on 10/27/2026
 */
@EventBusSubscriber(modid = GottschBooks.MOD_ID, value = Dist.CLIENT)
public final class ClientCommands {
    /** problems shown in chat; the log gets them all */
    private static final int CHAT_LIMIT = 30;

    private ClientCommands() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        register(event.getDispatcher());
    }

    static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(GottschBooks.MOD_ID)
                .then(Commands.literal("check")
                        .executes(c -> check(c.getSource(), BookLoader.all().values()))
                        .then(Commands.argument("book", ResourceLocationArgument.id())
                                .suggests((c, builder) -> SharedSuggestionProvider.suggestResource(BookLoader.all().keySet(), builder))
                                .executes(c -> {
                                    ResourceLocation id = ResourceLocationArgument.getId(c, "book");
                                    Optional<LoadedBook> book = BookLoader.get(id);
                                    if (book.isEmpty()) {
                                        c.getSource().sendFailure(Component.translatable("gottschbooks.command.check.missing", id.toString()));
                                        return 0;
                                    }
                                    return check(c.getSource(), List.of(book.get()));
                                })))
                .then(Commands.literal("lookup")
                        .then(Commands.literal("hint")
                                .executes(c -> hint(c.getSource()))
                                .then(Commands.literal("on").executes(c -> hint(c.getSource(), true)))
                                .then(Commands.literal("off").executes(c -> hint(c.getSource(), false))))));
    }

    /** turns the item lookup's tooltip line on or off, saved to the client config */
    private static int hint(CommandSourceStack source, boolean show) {
        ClientConfig.LOOKUP_HINT.set(show);
        ClientConfig.LOOKUP_HINT.save();
        return hint(source);
    }

    /** says whether the item lookup's tooltip line is showing */
    private static int hint(CommandSourceStack source) {
        boolean show = ClientConfig.LOOKUP_HINT.get();
        source.sendSuccess(() -> show
                ? Component.translatable("gottschbooks.command.lookup.hint.on")
                : Component.translatable("gottschbooks.command.lookup.hint.off", ItemLookup.KEY.getTranslatedKeyMessage()), false);
        return show ? 1 : 0;
    }

    private static int check(CommandSourceStack source, Collection<LoadedBook> books) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return 0;
        }
        List<BookCheck.Problem> problems = new ArrayList<>();
        books.stream().sorted(Comparator.comparing(book -> book.id().toString()))
                .forEach(book -> problems.addAll(BookCheck.check(book, level)));
        problems.sort(Comparator.comparing(BookCheck.Problem::file));

        for (int i = 0; i < problems.size(); i++) {
            BookCheck.Problem problem = problems.get(i);
            GottschBooks.LOGGER.warn("{}: {}", problem.file(), problem.message());
            if (i < CHAT_LIMIT) {
                source.sendSuccess(() -> line(problem), false);
            }
        }
        if (problems.size() > CHAT_LIMIT) {
            source.sendSuccess(() -> Component.translatable("gottschbooks.command.check.more", problems.size() - CHAT_LIMIT)
                    .withStyle(ChatFormatting.GRAY), false);
        }
        Component summary = problems.isEmpty()
                ? Component.translatable("gottschbooks.command.check.clean", books.size()).withStyle(ChatFormatting.GREEN)
                : Component.translatable("gottschbooks.command.check.problems", books.size(), problems.size())
                        .withStyle(ChatFormatting.YELLOW);
        source.sendSuccess(() -> summary, false);
        return problems.size();
    }

    /** the file (click to copy its path) and what is wrong */
    private static Component line(BookCheck.Problem problem) {
        Component file = Component.literal(problem.file()).withStyle(style -> style.withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, problem.file()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy.click"))));
        return Component.empty().append(file).append(Component.literal(": " + problem.message()));
    }
}
