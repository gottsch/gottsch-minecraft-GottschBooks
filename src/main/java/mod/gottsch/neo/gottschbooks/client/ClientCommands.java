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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@code /gottschbooks check [book]}: lists what is wrong in every loaded book, or in one: the first few in chat, and
 * all of them in the log and in a report, {@code logs/gottschbooks-check.txt}, grouped by file, which chat links to
 * (open it, or copy it whole). Books are client assets, resource packs included, so the check runs on the client, for
 * anyone.
 * {@code /gottschbooks lookup hint [on|off]} shows or hides the item lookup's tooltip line. These share their root
 * with the server's {@code /gottschbooks open}; NeoForge hands the server anything the client tree doesn't know.
 *
 * @author Mark Gottschling on 10/27/2026
 */
@EventBusSubscriber(modid = GottschBooks.MOD_ID, value = Dist.CLIENT)
public final class ClientCommands {
    /** problems shown in chat; the log and the report get them all */
    private static final int CHAT_LIMIT = 10;
    /** the report, under the game folder; each check replaces it */
    private static final String REPORT = "logs/gottschbooks-check.txt";

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
        // written even when clean, so an old report never outlives its fixes
        String report = report(books, problems);
        Path file = Minecraft.getInstance().gameDirectory.toPath().resolve(REPORT);
        boolean saved = save(file, report);
        if (problems.isEmpty()) {
            source.sendSuccess(() -> summary, false);
        } else {
            MutableComponent line = Component.empty().append(summary);
            if (saved) {
                line.append(" ").append(link("gottschbooks.command.check.open", ClickEvent.Action.OPEN_FILE,
                        file.toAbsolutePath().toString(), Component.literal(file.toAbsolutePath().toString())));
            }
            line.append(" ").append(link("gottschbooks.command.check.copy", ClickEvent.Action.COPY_TO_CLIPBOARD, report,
                    Component.translatable("gottschbooks.command.check.copy.hover")));
            source.sendSuccess(() -> line, false);
        }
        if (!saved) {
            source.sendFailure(Component.translatable("gottschbooks.command.check.unsaved", REPORT));
        }
        return problems.size();
    }

    /** "[label]" in green, doing {@code action} with {@code value} when clicked */
    private static Component link(String label, ClickEvent.Action action, String value, Component hover) {
        return Component.translatable(label).withStyle(style -> style.withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(action, value))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }

    /** every problem, grouped under its file, after a heading naming the books checked */
    static String report(Collection<LoadedBook> books, List<BookCheck.Problem> problems) {
        StringBuilder report = new StringBuilder("Gottsch Books check, ")
                .append(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString().replace('T', ' ')).append('\n');
        report.append("Books: ").append(books.stream().map(book -> book.id().toString()).sorted()
                .collect(Collectors.joining(", "))).append('\n');
        report.append(books.size()).append(" book(s), ").append(problems.size()).append(" problem(s)\n");
        String file = null;
        for (BookCheck.Problem problem : problems) {
            if (!problem.file().equals(file)) {
                file = problem.file();
                report.append('\n').append(file).append('\n');
            }
            report.append("  - ").append(problem.message()).append('\n');
        }
        return report.toString();
    }

    private static boolean save(Path file, String report) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, report, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            GottschBooks.LOGGER.warn("couldn't write the check report {}", file, e);
            return false;
        }
    }

    /** the file (click to copy its path) and what is wrong */
    private static Component line(BookCheck.Problem problem) {
        Component file = Component.literal(problem.file()).withStyle(style -> style.withColor(ChatFormatting.GRAY)
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, problem.file()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.copy.click"))));
        return Component.empty().append(file).append(Component.literal(": " + problem.message()));
    }
}
