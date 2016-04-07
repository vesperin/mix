package com.vesperin.base.locators;

import com.vesperin.base.Context;

import java.util.List;
import java.util.Objects;

/**
 * @author Huascar Sanchez
 */
public class ProgramUnitLocator implements UnitLocator {
  private final Context context;
  private final Record record;

  /**
   * Constructs a new {@code ProgramUnitLocator} with a {@code Context} as
   * a value.
   *
   * @param context THe Java {@code Context}.
   */
  public ProgramUnitLocator(Context context) {
    this.context = context;
    this.record = new Record();
  }

  @Override public List<UnitLocation> locate(ProgramUnit unit) {
    track(unit.getIdentifier(), unit);

    return unit.getLocations(context);
  }


  Context getContext() {
    return context;
  }

  private void track(String key, ProgramUnit hint) {
    record.key  = Objects.requireNonNull(key);
    record.hint = Objects.requireNonNull(hint);
  }

  @Override public String toString() {
    final String target = record.key;
    final ProgramUnit hint = record.hint;
    return "Search for " + target + " " + hint + " in " + getContext();
  }


  static class Record {
    String key;
    ProgramUnit hint;
  }
}
