package com.null_codes.hearth.listener;

import com.null_codes.hearth.model.BlockSnapshot;
import com.null_codes.hearth.model.Property;
import com.null_codes.hearth.model.PropertyChange;
import com.null_codes.hearth.service.PropertyChangeManager;
import com.null_codes.hearth.service.PropertyManager;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.jetbrains.annotations.Nullable;

public class PropertyEventListener implements Listener {

  private final Logger logger;
  private final PropertyManager propertyManager;
  private final PropertyChangeManager changeManager;

  public PropertyEventListener(
      Logger logger, PropertyManager propertyManager, PropertyChangeManager changeManager) {
    this.logger = logger;
    this.propertyManager = propertyManager;
    this.changeManager = changeManager;
  }

  private void recordSimpleDestroy(
      Block block, @Nullable UUID playerUuid, PropertyChange.ChangeCause changeCause) {
    Optional<Property> property = propertyManager.findProperty(block.getLocation());
    if (property.isEmpty()) return;

    BlockSnapshot before = BlockSnapshot.from(block);
    BlockSnapshot after = BlockSnapshot.airAt(block);
    PropertyChange change =
        new PropertyChange(property.get().uuid(), playerUuid, changeCause, before, after);

    record(change);
  }

  private void record(PropertyChange change) {
    changeManager
        .record(change)
        .whenComplete(
            (ignored, failure) -> {
              if (failure != null) {
                logger.log(
                    Level.SEVERE, "Could not persist property change " + change.uuid(), failure);
              }
            });
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    Block block = event.getBlock();
    Optional<Property> property = propertyManager.findProperty(block.getLocation());
    if (property.isEmpty()) return;

    BlockSnapshot before = BlockSnapshot.from(event.getBlockReplacedState());
    BlockSnapshot after = BlockSnapshot.from(event.getBlock());
    PropertyChange change =
        new PropertyChange(
            property.get().uuid(),
            event.getPlayer().getUniqueId(),
            PropertyChange.ChangeCause.PLAYER_PLACE,
            before,
            after);

    record(change);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    recordSimpleDestroy(
        event.getBlock(), event.getPlayer().getUniqueId(), PropertyChange.ChangeCause.PLAYER_BREAK);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBurn(BlockBurnEvent event) {
    recordSimpleDestroy(event.getBlock(), null, PropertyChange.ChangeCause.FIRE);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityExplode(EntityExplodeEvent event) {
    for (Block block : event.blockList()) {
      recordSimpleDestroy(block, null, PropertyChange.ChangeCause.EXPLOSION);
    }
  }
}
