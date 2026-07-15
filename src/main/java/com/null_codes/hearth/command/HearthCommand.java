package com.null_codes.hearth.command;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.LongArgumentType.getLong;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.null_codes.hearth.model.Property;
import com.null_codes.hearth.service.PropertyChangeManager;
import com.null_codes.hearth.service.PropertyChangeWorkload;
import com.null_codes.hearth.service.PropertyManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

/** Builds Hearth's native Brigadier command tree. */
public final class HearthCommand {

  private static final int DEFAULT_RADIUS = 8;
  private static final int MAX_STRESS_CHANGES = 100_000;

  private final PropertyManager propertyManager;
  private final PropertyChangeManager changeManager;
  private final Executor mainThreadExecutor;

  public HearthCommand(
      PropertyManager propertyManager,
      PropertyChangeManager changeManager,
      Executor mainThreadExecutor) {
    this.propertyManager = propertyManager;
    this.changeManager = changeManager;
    this.mainThreadExecutor = mainThreadExecutor;
  }

  public LiteralCommandNode<CommandSourceStack> createCommand() {
    LiteralArgumentBuilder<CommandSourceStack> root =
        Commands.literal("hearth")
            .requires(
                source ->
                    source.getSender().hasPermission("hearth.admin")
                        && propertyManager.isReady()
                        && changeManager.isReady());

    root.then(createBranch());
    root.then(renameBranch());
    root.then(removeBranch());
    root.then(
        Commands.literal("list")
            .executes(context -> execute(context.getSource(), () -> list(context.getSource()))));
    root.then(historyBranch());
    root.then(stressBranch());
    return root.build();
  }

  private LiteralArgumentBuilder<CommandSourceStack> createBranch() {
    RequiredArgumentBuilder<CommandSourceStack, String> name =
        Commands.argument("name", StringArgumentType.word())
            .executes(
                context ->
                    execute(
                        context.getSource(),
                        () ->
                            create(
                                context.getSource(), getString(context, "name"), DEFAULT_RADIUS)));
    name.then(
        Commands.argument("radius", IntegerArgumentType.integer(0))
            .executes(
                context ->
                    execute(
                        context.getSource(),
                        () ->
                            create(
                                context.getSource(),
                                getString(context, "name"),
                                getInteger(context, "radius")))));
    return Commands.literal("create").then(name);
  }

  private LiteralArgumentBuilder<CommandSourceStack> renameBranch() {
    return Commands.literal("rename")
        .then(
            activePropertyArgument()
                .then(
                    Commands.argument("name", StringArgumentType.word())
                        .executes(
                            context ->
                                execute(
                                    context.getSource(),
                                    () ->
                                        rename(
                                            context.getSource(),
                                            getString(context, "property"),
                                            getString(context, "name"))))));
  }

  private LiteralArgumentBuilder<CommandSourceStack> removeBranch() {
    return Commands.literal("remove")
        .then(
            activePropertyArgument()
                .executes(
                    context ->
                        execute(
                            context.getSource(),
                            () -> remove(context.getSource(), getString(context, "property")))));
  }

  private LiteralArgumentBuilder<CommandSourceStack> historyBranch() {
    return Commands.literal("history")
        .then(
            Commands.argument("property", StringArgumentType.word())
                .suggests(this::suggestHistoryProperties)
                .executes(
                    context ->
                        execute(
                            context.getSource(),
                            () -> history(context.getSource(), getString(context, "property")))));
  }

  private LiteralArgumentBuilder<CommandSourceStack> stressBranch() {
    RequiredArgumentBuilder<CommandSourceStack, Integer> count =
        Commands.argument("count", IntegerArgumentType.integer(0, MAX_STRESS_CHANGES))
            .executes(
                context ->
                    execute(
                        context.getSource(),
                        () ->
                            stress(
                                context.getSource(),
                                getString(context, "property"),
                                getInteger(context, "count"),
                                42L)));
    count.then(
        Commands.argument("seed", LongArgumentType.longArg())
            .executes(
                context ->
                    execute(
                        context.getSource(),
                        () ->
                            stress(
                                context.getSource(),
                                getString(context, "property"),
                                getInteger(context, "count"),
                                getLong(context, "seed")))));
    return Commands.literal("stress").then(activePropertyArgument().then(count));
  }

  private RequiredArgumentBuilder<CommandSourceStack, String> activePropertyArgument() {
    return Commands.argument("property", StringArgumentType.word())
        .suggests(this::suggestActiveProperties);
  }

  private CompletableFuture<Suggestions> suggestActiveProperties(
      CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
    return suggest(builder, activePropertyUuids());
  }

  private CompletableFuture<Suggestions> suggestHistoryProperties(
      CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
    LinkedHashSet<UUID> candidates = new LinkedHashSet<>(activePropertyUuids());
    candidates.addAll(changeManager.getPropertyUuids());
    return suggest(builder, candidates);
  }

  private static CompletableFuture<Suggestions> suggest(
      SuggestionsBuilder builder, Collection<UUID> candidates) {
    for (String suggestion : UuidPrefixResolver.suggestions(candidates, builder.getRemaining())) {
      builder.suggest(suggestion);
    }
    return builder.buildFuture();
  }

  private List<UUID> activePropertyUuids() {
    List<UUID> propertyUuids = new ArrayList<>();
    for (Property property : propertyManager.getProperties()) {
      propertyUuids.add(property.uuid());
    }
    return propertyUuids;
  }

  private int execute(CommandSourceStack source, Supplier<CompletableFuture<?>> action) {
    try {
      action
          .get()
          .whenComplete(
              (ignored, failure) -> {
                if (failure != null) {
                  mainThreadExecutor.execute(
                      () ->
                          source
                              .getSender()
                              .sendMessage("Hearth: " + rootCause(failure).getMessage()));
                }
              });
      return Command.SINGLE_SUCCESS;
    } catch (IllegalArgumentException exception) {
      source.getSender().sendMessage("Hearth: " + exception.getMessage());
      return 0;
    }
  }

  private static Throwable rootCause(Throwable failure) {
    Throwable cause = failure;
    while (cause instanceof CompletionException && cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause;
  }

  private CompletableFuture<Void> create(CommandSourceStack source, String name, int radius) {
    if (!(source.getExecutor() instanceof Player player)) {
      throw new IllegalArgumentException("Only a player can create a property.");
    }

    World world = player.getWorld();
    int x = source.getLocation().getBlockX();
    int z = source.getLocation().getBlockZ();
    Property property =
        new Property(
            UUID.randomUUID(),
            player.getUniqueId(),
            name,
            world.getUID(),
            new BoundingBox(
                x - radius,
                world.getMinHeight(),
                z - radius,
                x + radius + 1,
                world.getMaxHeight(),
                z + radius + 1),
            System.currentTimeMillis());

    return propertyManager
        .register(property)
        .thenRunAsync(
            () ->
                source
                    .getSender()
                    .sendMessage(
                        "Hearth: created " + property.name() + " (" + property.uuid() + ")."),
            mainThreadExecutor);
  }

  private CompletableFuture<Void> rename(CommandSourceStack source, String value, String name) {
    Property existing = requiredProperty(value);
    Property renamed =
        new Property(
            existing.uuid(),
            existing.owner(),
            name,
            existing.world(),
            existing.region(),
            existing.timestamp());
    return propertyManager
        .update(renamed)
        .thenRunAsync(
            () ->
                source
                    .getSender()
                    .sendMessage(
                        "Hearth: renamed property "
                            + renamed.uuid()
                            + " to "
                            + renamed.name()
                            + "."),
            mainThreadExecutor);
  }

  private CompletableFuture<Void> remove(CommandSourceStack source, String value) {
    Property property = requiredProperty(value);
    return propertyManager
        .remove(property.uuid())
        .thenAcceptAsync(
            ignored ->
                source
                    .getSender()
                    .sendMessage(
                        "Hearth: removed " + property.name() + " (" + property.uuid() + ")."),
            mainThreadExecutor);
  }

  private CompletableFuture<Void> list(CommandSourceStack source) {
    List<Property> properties = propertyManager.getProperties();
    CommandSender sender = source.getSender();
    sender.sendMessage("Hearth: " + properties.size() + " properties registered.");
    for (Property property : properties) {
      sender.sendMessage("- " + property.uuid() + " " + property.name());
    }
    return CompletableFuture.completedFuture(null);
  }

  private CompletableFuture<Void> history(CommandSourceStack source, String value) {
    LinkedHashSet<UUID> candidates = new LinkedHashSet<>(activePropertyUuids());
    candidates.addAll(changeManager.getPropertyUuids());
    UUID propertyUuid = UuidPrefixResolver.resolve(candidates, value);
    int count = changeManager.getChanges(propertyUuid).size();
    source
        .getSender()
        .sendMessage("Hearth: property " + propertyUuid + " has " + count + " recorded changes.");
    return CompletableFuture.completedFuture(null);
  }

  private CompletableFuture<Void> stress(
      CommandSourceStack source, String value, int count, long seed) {
    Property property = requiredProperty(value);
    long started = System.nanoTime();
    return PropertyChangeWorkload.generate(
            changeManager, property.uuid(), property.world(), count, seed)
        .thenAcceptAsync(
            recordedCount -> {
              long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
              source
                  .getSender()
                  .sendMessage(
                      "Hearth: persisted "
                          + recordedCount
                          + " deterministic changes in "
                          + elapsedMillis
                          + " ms (seed "
                          + seed
                          + ").");
            },
            mainThreadExecutor);
  }

  private Property requiredProperty(String value) {
    UUID uuid = UuidPrefixResolver.resolve(activePropertyUuids(), value);
    return propertyManager.get(uuid).orElseThrow();
  }
}
