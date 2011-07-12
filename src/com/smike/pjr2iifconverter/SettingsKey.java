package com.smike.pjr2iifconverter;

public enum SettingsKey {
  DELETE_PJRS_ON_CONVERT("delete_pjrs_on_convert"),
  IIF_OUTPUT_FILE("iif_output_file"),
  ACCOUNT_ID_MAP_FILE("account_id_map_file"),
  PJR_LOCATION("pjr_location");

  private String key;
  private SettingsKey(String key) {
    this.key = key;
  }

  public String getKey() { return key; }
  public String toString() { return getKey(); }

  public static SettingsKey getEnum(String key) {
    for (SettingsKey settingsKey : SettingsKey.values()) {
      if (settingsKey.getKey().equals(key)) {
        return settingsKey;
      }
    }
    return null;
  }
}