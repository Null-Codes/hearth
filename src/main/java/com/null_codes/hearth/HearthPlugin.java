package com.null_codes.hearth;

import com.null_codes.hearth.command.HearthCommand;
import com.null_codes.hearth.listener.PropertyEventListener;
import com.null_codes.hearth.service.PropertyChangeManager;
import com.null_codes.hearth.service.PropertyManager;
import com.null_codes.hearth.storage.SqliteHearthStore;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
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
                        new HearthCommand(
                                propertyManager,
                                propertyChangeManager,
                                task -> getServer().getScheduler().runTask(this, task))
                            .createCommand(),
                        "Manage properties and generate profiling workloads."));

    CompletableFuture.allOf(propertyManager.ready(), propertyChangeManager.ready())
        .whenComplete(
            (ignored, failure) ->
                getServer()
                    .getScheduler()
                    .runTask(
                        this,
                        () -> {
                          if (failure != null) {
                            getLogger().log(Level.SEVERE, "Could not load Hearth data.", failure);
                            getServer().getPluginManager().disablePlugin(this);
                            return;
                          }
                          if (!isEnabled()) return;

                          PluginManager pluginManager = getServer().getPluginManager();
                          pluginManager.registerEvents(
                              new PropertyEventListener(
                                  getLogger(), propertyManager, propertyChangeManager),
                              this);
                          getLogger().info("Hearth data loaded; property tracking is active.");
                        }));
  }

  @Override
  public void onDisable() {
    if (store != null) store.close();
  }
}
