package com.null_codes.hearth;

import com.null_codes.hearth.listener.PropertyEventListener;
import com.null_codes.hearth.service.PropertyChangeManager;
import com.null_codes.hearth.service.PropertyManager;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public class HearthPlugin extends JavaPlugin {

  @Override
  public void onEnable() {

    PropertyManager propertyManager = new PropertyManager();
    PropertyChangeManager propertyChangeManager = new PropertyChangeManager();

    PluginManager pm = getServer().getPluginManager();
    pm.registerEvents(new PropertyEventListener(propertyManager, propertyChangeManager), this);
  }
}
