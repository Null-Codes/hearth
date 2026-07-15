package com.null_codes.hearth.command;

import com.null_codes.hearth.model.Property;
import com.null_codes.hearth.service.PropertyChangeManager;
import com.null_codes.hearth.service.PropertyChangeWorkload;
import com.null_codes.hearth.service.PropertyManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

/** Provides the minimal operator surface needed to exercise Hearth on a development server. */
public final class HearthCommand implements BasicCommand {

  private static final int DEFAULT_RADIUS = 8;
  private static final int MAX_STRESS_CHANGES = 100_000;

  private final PropertyManager propertyManager;
  private final PropertyChangeManager changeManager;

  public HearthCommand(PropertyManager propertyManager, PropertyChangeManager changeManager) {
    this.propertyManager = propertyManager;
    this.changeManager = changeManager;
  }

  @Override
  public void execute(CommandSourceStack source, String[] args) {
    CommandSender sender = source.getSender();
    if (args.length == 0) {
      sendUsage(sender);
      return;
    }

    try {
      switch (args[0].toLowerCase(Locale.ROOT)) {
        case "create" -> create(sender, args);
        case "rename" -> rename(sender, args);
        case "remove" -> remove(sender, args);
        case "list" -> list(sender);
        case "history" -> history(sender, args);
        case "stress" -> stress(sender, args);
        default -> sendUsage(sender);
      }
    } catch (IllegalArgumentException exception) {
      sender.sendMessage("Hearth: " + exception.getMessage());
    }
  }

  @Override
  public Collection<String> suggest(CommandSourceStack source, String[] args) {
    if (args.length <= 1) {
      return List.of("create", "rename", "remove", "list", "history", "stress");
    }
    return List.of();
  }

  @Override
  public String permission() {
    return "hearth.admin";
  }

  private void create(CommandSender sender, String[] args) {
    if (!(sender instanceof Player player)) {
      throw new IllegalArgumentException("Only a player can create a property.");
    }
    if (args.length < 2 || args.length > 3) {
      throw new IllegalArgumentException("Usage: /hearth create <name> [radius]");
    }

    int radius = args.length == 3 ? parseNonNegativeInt(args[2], "radius") : DEFAULT_RADIUS;
    World world = player.getWorld();
    int x = player.getLocation().getBlockX();
    int z = player.getLocation().getBlockZ();
    Property property =
        new Property(
            UUID.randomUUID(),
            player.getUniqueId(),
            args[1],
            world.getUID(),
            new BoundingBox(
                x - radius,
                world.getMinHeight(),
                z - radius,
                x + radius + 1,
                world.getMaxHeight(),
                z + radius + 1),
            System.currentTimeMillis());

    propertyManager.register(property);
    sender.sendMessage("Hearth: created " + property.name() + " (" + property.uuid() + ").");
  }

  private void rename(CommandSender sender, String[] args) {
    if (args.length != 3) {
      throw new IllegalArgumentException("Usage: /hearth rename <property UUID> <name>");
    }

    Property existing = requiredProperty(args[1]);
    Property renamed =
        new Property(
            existing.uuid(),
            existing.owner(),
            args[2],
            existing.world(),
            existing.region(),
            existing.timestamp());
    propertyManager.update(renamed);
    sender.sendMessage(
        "Hearth: renamed property " + renamed.uuid() + " to " + renamed.name() + ".");
  }

  private void remove(CommandSender sender, String[] args) {
    if (args.length != 2) {
      throw new IllegalArgumentException("Usage: /hearth remove <property UUID>");
    }

    Property property = requiredProperty(args[1]);
    propertyManager.remove(property.uuid());
    sender.sendMessage("Hearth: removed " + property.name() + " (" + property.uuid() + ").");
  }

  private void list(CommandSender sender) {
    List<Property> properties = propertyManager.getProperties();
    sender.sendMessage("Hearth: " + properties.size() + " properties registered.");
    for (Property property : properties) {
      sender.sendMessage("- " + property.uuid() + " " + property.name());
    }
  }

  private void history(CommandSender sender, String[] args) {
    if (args.length != 2) {
      throw new IllegalArgumentException("Usage: /hearth history <property UUID>");
    }

    UUID propertyUuid = UUID.fromString(args[1]);
    int count = changeManager.getChanges(propertyUuid).size();
    sender.sendMessage("Hearth: property " + propertyUuid + " has " + count + " recorded changes.");
  }

  private void stress(CommandSender sender, String[] args) {
    if (args.length < 3 || args.length > 4) {
      throw new IllegalArgumentException("Usage: /hearth stress <property UUID> <count> [seed]");
    }

    Property property = requiredProperty(args[1]);
    int count = parseNonNegativeInt(args[2], "count");
    if (count > MAX_STRESS_CHANGES) {
      throw new IllegalArgumentException("count cannot exceed " + MAX_STRESS_CHANGES);
    }
    long seed = args.length == 4 ? Long.parseLong(args[3]) : 42L;

    long started = System.nanoTime();
    PropertyChangeWorkload.generate(changeManager, property.uuid(), property.world(), count, seed);
    long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
    sender.sendMessage(
        "Hearth: recorded "
            + count
            + " deterministic changes in "
            + elapsedMillis
            + " ms (seed "
            + seed
            + ").");
  }

  private Property requiredProperty(String value) {
    UUID uuid = UUID.fromString(value);
    return propertyManager
        .get(uuid)
        .orElseThrow(() -> new IllegalArgumentException("Unknown property UUID " + uuid + "."));
  }

  private static int parseNonNegativeInt(String value, String label) {
    int parsed = Integer.parseInt(value);
    if (parsed < 0) throw new IllegalArgumentException(label + " cannot be negative.");
    return parsed;
  }

  private static void sendUsage(CommandSender sender) {
    sender.sendMessage(
        "Hearth: /hearth <create|rename|remove|list|history|stress> (requires hearth.admin)");
  }
}
