package com.null_codes.hearth.listener;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.null_codes.hearth.model.Property;
import com.null_codes.hearth.service.PropertyManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class PropertyEventListener implements Listener {

  private final Logger logger;
  private final PropertyManager propertyManager;

  public PropertyEventListener(Logger logger, PropertyManager propertyManager) {
    this.logger = logger;
    this.propertyManager = propertyManager;
  }

  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    Block block = event.getBlock();

    Property property = propertyManager.findProperty(block.getLocation());
    if (property == null) return;

    logger.log(Level.INFO, "Block placed in property {0}: {1}", new String[]{property.name(), block.toString()});
  }

  @EventHandler
  public void onBlockBreak(BlockBreakEvent event) {
    Block block = event.getBlock();

    Property property = propertyManager.findProperty(block.getLocation());
    if (property == null) return;

    logger.log(Level.INFO, "Block broken in property {0}: {1}", new String[]{property.name(), block.toString()});
  }

  @EventHandler
  public void onBlockBurn(BlockBurnEvent event) {
    Block block = event.getBlock();

    Property property = propertyManager.findProperty(block.getLocation());
    if (property == null) return;

    logger.log(Level.INFO, "Block burned in property {0}: {1}", new String[]{property.name(), block.toString()});
  }

  @EventHandler
  public void onEntityExplode(EntityExplodeEvent event) {
    Entity entity = event.getEntity();

    Property property = propertyManager.findProperty(entity.getLocation());
    if (property == null) return;

    logger.log(Level.INFO, "Entity exploded in property {0}: {1}", new String[]{property.name(), entity.toString()});
  }

}
