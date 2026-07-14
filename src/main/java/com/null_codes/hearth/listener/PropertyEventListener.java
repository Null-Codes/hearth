package com.null_codes.hearth.listener;

import com.null_codes.hearth.model.BlockSnapshot;
import com.null_codes.hearth.model.Property;
import com.null_codes.hearth.model.PropertyChange;
import com.null_codes.hearth.service.PropertyChangeManager;
import com.null_codes.hearth.service.PropertyManager;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
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
    Property property = propertyManager.findProperty(block.getLocation());
    if (property == null) return;

    BlockSnapshot before = BlockSnapshot.from(block);
    BlockSnapshot after = BlockSnapshot.airAt(block);
    PropertyChange change =
        PropertyChange.create(property.uuid(), playerUuid, changeCause, before, after);

    changeManager.record(change);
  }

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    Block block = event.getBlock();
    Property property = propertyManager.findProperty(block.getLocation());
    if (property == null) return;

    BlockSnapshot before = BlockSnapshot.airAt(event.getBlock());
    BlockSnapshot after = BlockSnapshot.from(event.getBlock());
    PropertyChange change =
        PropertyChange.create(
            property.uuid(),
            event.getPlayer().getUniqueId(),
            PropertyChange.ChangeCause.PLAYER_PLACE,
            before,
            after);

    changeManager.record(change);
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    recordSimpleDestroy(
        event.getBlock(), event.getPlayer().getUniqueId(), PropertyChange.ChangeCause.PLAYER_BREAK);
  }

  @EventHandler
  public void onBlockBurn(BlockBurnEvent event) {
    recordSimpleDestroy(event.getBlock(), null, PropertyChange.ChangeCause.FIRE);
  }

  @EventHandler
  public void onEntityExplode(EntityExplodeEvent event) {
    for (Block block : event.blockList()) {
      Property property = propertyManager.findProperty(block.getLocation());
      if (property == null) continue;
      recordSimpleDestroy(block, null, PropertyChange.ChangeCause.EXPLOSION);
    }
  }
}
