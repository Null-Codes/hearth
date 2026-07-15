package com.null_codes.hearth.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.null_codes.hearth.service.PropertyChangeManager;
import com.null_codes.hearth.service.PropertyManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.junit.jupiter.api.Test;

class HearthCommandTest {

  @Test
  void buildsExpectedNativeCommandBranches() {
    LiteralCommandNode<CommandSourceStack> command = command();

    assertTrue(
        command.getChildren().stream()
            .map(node -> node.getName())
            .collect(Collectors.toSet())
            .containsAll(Set.of("create", "rename", "remove", "list", "history", "stress")));
    assertNotNull(propertyArgument(command, "remove").getCustomSuggestions());
    assertNotNull(propertyArgument(command, "history").getCustomSuggestions());
    assertNotNull(propertyArgument(command, "stress").getCustomSuggestions());
  }

  @Test
  void rootRequirementUsesServerPermissionCheck() {
    LiteralCommandNode<CommandSourceStack> command = command();

    assertFalse(command.getRequirement().test(source(false)));
    assertTrue(command.getRequirement().test(source(true)));
  }

  private static LiteralCommandNode<CommandSourceStack> command() {
    return new HearthCommand(new PropertyManager(), new PropertyChangeManager()).createCommand();
  }

  @SuppressWarnings("unchecked")
  private static ArgumentCommandNode<CommandSourceStack, String> propertyArgument(
      LiteralCommandNode<CommandSourceStack> command, String branch) {
    return (ArgumentCommandNode<CommandSourceStack, String>)
        command.getChild(branch).getChild("property");
  }

  private static CommandSourceStack source(boolean permitted) {
    CommandSender sender =
        (CommandSender)
            Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class<?>[] {CommandSender.class},
                (proxy, method, arguments) ->
                    method.getName().equals("hasPermission")
                        ? permitted
                        : defaultValue(method.getReturnType()));
    return new CommandSourceStack() {
      @Override
      public Location getLocation() {
        return new Location(null, 0, 0, 0);
      }

      @Override
      public CommandSender getSender() {
        return sender;
      }

      @Override
      public Entity getExecutor() {
        return null;
      }

      @Override
      public CommandSourceStack withLocation(Location location) {
        return this;
      }

      @Override
      public CommandSourceStack withExecutor(Entity executor) {
        return this;
      }
    };
  }

  private static Object defaultValue(Class<?> type) {
    if (type == boolean.class) return false;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    return null;
  }
}
