package com.null_codes.hearth;

import com.null_codes.hearth.command.HearthCommand;
import com.null_codes.hearth.listener.PropertyEventListener;
import com.null_codes.hearth.service.PropertyChangeManager;
import com.null_codes.hearth.service.PropertyManager;
import com.null_codes.hearth.storage.SqliteHearthStore;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public class HearthPlugin extends JavaPlugin {

  private PropertyManager propertyManager;
  private PropertyChangeManager propertyChangeManager;
  private SqliteHearthStore store;

  @Override
  public void onEnable() {

    if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
      throw new IllegalStateException("Could not create the Hearth data directory.");
    }
    store = new SqliteHearthStore(getDataFolder().toPath().resolve("hearth.db"));
    propertyManager = new PropertyManager(store);
    propertyChangeManager = new PropertyChangeManager(store);

    getLifecycleManager()
        .registerEventHandler(
            LifecycleEvents.COMMANDS,
            event ->
                event
                    .registrar()
                    .register(
                        new HearthCommand(propertyManager, propertyChangeManager).createCommand(),
                        "Manage properties and generate profiling workloads."));

    PluginManager pm = getServer().getPluginManager();
    pm.registerEvents(
        new PropertyEventListener(getLogger(), propertyManager, propertyChangeManager), this);
  }

  @Override
  public void onDisable() {
    if (store != null) store.close();
  }
}
