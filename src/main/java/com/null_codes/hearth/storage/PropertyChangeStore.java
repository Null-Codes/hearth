package com.null_codes.hearth.storage;

import com.null_codes.hearth.model.PropertyChange;
import java.util.List;

/** Defines append-only property history persistence. */
public interface PropertyChangeStore {
  List<PropertyChange> loadChanges();

  void insert(PropertyChange change);
}
