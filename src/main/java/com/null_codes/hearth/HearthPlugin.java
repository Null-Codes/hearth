package com.null_codes.hearth;

import com.null_codes.hearth.command.HearthCommand;
import com.null_codes.hearth.listener.PropertyEventListener;
import com.null_codes.hearth.service.PropertyChangeManager;
import com.null_codes.hearth.service.PropertyManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public class HearthPlugin extends JavaPlugin {

  private PropertyManager propertyManager;
  private PropertyChangeManager propertyChangeManager;

  @Override
  public void onEnable() {

    propertyManager = new PropertyManager();
    propertyChangeManager = new PropertyChangeManager();

    getLifecycleManager()
        .registerEventHandler(
            LifecycleEvents.COMMANDS,
            event ->
                event
                    .registrar()
                    .register(
                        "hearth",
                        "Manage properties and generate profiling workloads.",
                        new HearthCommand(propertyManager, propertyChangeManager)));

    PluginManager pm = getServer().getPluginManager();
    pm.registerEvents(new PropertyEventListener(propertyManager, propertyChangeManager), this);
  }
}
