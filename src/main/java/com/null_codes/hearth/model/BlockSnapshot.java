package com.null_codes.hearth.model;

import org.bukkit.Material;

import java.util.Map;

public record BlockSnapshot(Material material, int x, int y, int z, Map<String, Object> blockData) {}
