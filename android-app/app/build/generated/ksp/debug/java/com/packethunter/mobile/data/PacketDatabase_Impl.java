package com.packethunter.mobile.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PacketDatabase_Impl extends PacketDatabase {
  private volatile PacketDao _packetDao;

  private volatile AlertDao _alertDao;

  private volatile RuleDao _ruleDao;

  private volatile FilterPresetDao _filterPresetDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `packets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `protocol` TEXT NOT NULL, `sourceIp` TEXT NOT NULL, `destIp` TEXT NOT NULL, `sourcePort` INTEGER NOT NULL, `destPort` INTEGER NOT NULL, `length` INTEGER NOT NULL, `flags` TEXT NOT NULL, `payload` BLOB, `payloadPreview` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `direction` TEXT NOT NULL, `httpMethod` TEXT, `httpUrl` TEXT, `dnsQuery` TEXT, `dnsResponse` TEXT, `tlsSni` TEXT, `tlsCertFingerprint` TEXT, `destCountry` TEXT, `destCity` TEXT, `destLat` REAL, `destLon` REAL, `destAsn` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `alerts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `severity` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `relatedPacketIds` TEXT NOT NULL, `acknowledged` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `metric` TEXT NOT NULL, `condition` TEXT NOT NULL, `threshold` REAL NOT NULL, `timeWindowSeconds` INTEGER NOT NULL, `action` TEXT NOT NULL, `severity` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `filter_presets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `securityFilters` TEXT NOT NULL, `rules` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `lastUsed` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `intercepts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `protocol` TEXT NOT NULL, `sourceIp` TEXT NOT NULL, `destIp` TEXT NOT NULL, `sourcePort` INTEGER NOT NULL, `destPort` INTEGER NOT NULL, `summary` TEXT NOT NULL, `payloadPreview` BLOB, `payloadHex` TEXT NOT NULL, `payloadAscii` TEXT NOT NULL, `filterType` TEXT NOT NULL, `sni` TEXT, `originalHash` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9592997c61563d37fd39c0bb2ba8d1ac')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `packets`");
        db.execSQL("DROP TABLE IF EXISTS `alerts`");
        db.execSQL("DROP TABLE IF EXISTS `rules`");
        db.execSQL("DROP TABLE IF EXISTS `filter_presets`");
        db.execSQL("DROP TABLE IF EXISTS `intercepts`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsPackets = new HashMap<String, TableInfo.Column>(24);
        _columnsPackets.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("protocol", new TableInfo.Column("protocol", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("sourceIp", new TableInfo.Column("sourceIp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("destIp", new TableInfo.Column("destIp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("sourcePort", new TableInfo.Column("sourcePort", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("destPort", new TableInfo.Column("destPort", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("length", new TableInfo.Column("length", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("flags", new TableInfo.Column("flags", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("payload", new TableInfo.Column("payload", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("payloadPreview", new TableInfo.Column("payloadPreview", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("sessionId", new TableInfo.Column("sessionId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("direction", new TableInfo.Column("direction", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("httpMethod", new TableInfo.Column("httpMethod", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("httpUrl", new TableInfo.Column("httpUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("dnsQuery", new TableInfo.Column("dnsQuery", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("dnsResponse", new TableInfo.Column("dnsResponse", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("tlsSni", new TableInfo.Column("tlsSni", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("tlsCertFingerprint", new TableInfo.Column("tlsCertFingerprint", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("destCountry", new TableInfo.Column("destCountry", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("destCity", new TableInfo.Column("destCity", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("destLat", new TableInfo.Column("destLat", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("destLon", new TableInfo.Column("destLon", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPackets.put("destAsn", new TableInfo.Column("destAsn", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPackets = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPackets = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPackets = new TableInfo("packets", _columnsPackets, _foreignKeysPackets, _indicesPackets);
        final TableInfo _existingPackets = TableInfo.read(db, "packets");
        if (!_infoPackets.equals(_existingPackets)) {
          return new RoomOpenHelper.ValidationResult(false, "packets(com.packethunter.mobile.data.PacketInfo).\n"
                  + " Expected:\n" + _infoPackets + "\n"
                  + " Found:\n" + _existingPackets);
        }
        final HashMap<String, TableInfo.Column> _columnsAlerts = new HashMap<String, TableInfo.Column>(8);
        _columnsAlerts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlerts.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlerts.put("severity", new TableInfo.Column("severity", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlerts.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlerts.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlerts.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlerts.put("relatedPacketIds", new TableInfo.Column("relatedPacketIds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAlerts.put("acknowledged", new TableInfo.Column("acknowledged", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAlerts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAlerts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAlerts = new TableInfo("alerts", _columnsAlerts, _foreignKeysAlerts, _indicesAlerts);
        final TableInfo _existingAlerts = TableInfo.read(db, "alerts");
        if (!_infoAlerts.equals(_existingAlerts)) {
          return new RoomOpenHelper.ValidationResult(false, "alerts(com.packethunter.mobile.data.Alert).\n"
                  + " Expected:\n" + _infoAlerts + "\n"
                  + " Found:\n" + _existingAlerts);
        }
        final HashMap<String, TableInfo.Column> _columnsRules = new HashMap<String, TableInfo.Column>(9);
        _columnsRules.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRules.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRules.put("enabled", new TableInfo.Column("enabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRules.put("metric", new TableInfo.Column("metric", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRules.put("condition", new TableInfo.Column("condition", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRules.put("threshold", new TableInfo.Column("threshold", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRules.put("timeWindowSeconds", new TableInfo.Column("timeWindowSeconds", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRules.put("action", new TableInfo.Column("action", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRules.put("severity", new TableInfo.Column("severity", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRules = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRules = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRules = new TableInfo("rules", _columnsRules, _foreignKeysRules, _indicesRules);
        final TableInfo _existingRules = TableInfo.read(db, "rules");
        if (!_infoRules.equals(_existingRules)) {
          return new RoomOpenHelper.ValidationResult(false, "rules(com.packethunter.mobile.data.DetectionRule).\n"
                  + " Expected:\n" + _infoRules + "\n"
                  + " Found:\n" + _existingRules);
        }
        final HashMap<String, TableInfo.Column> _columnsFilterPresets = new HashMap<String, TableInfo.Column>(6);
        _columnsFilterPresets.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilterPresets.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilterPresets.put("securityFilters", new TableInfo.Column("securityFilters", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilterPresets.put("rules", new TableInfo.Column("rules", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilterPresets.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFilterPresets.put("lastUsed", new TableInfo.Column("lastUsed", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFilterPresets = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFilterPresets = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFilterPresets = new TableInfo("filter_presets", _columnsFilterPresets, _foreignKeysFilterPresets, _indicesFilterPresets);
        final TableInfo _existingFilterPresets = TableInfo.read(db, "filter_presets");
        if (!_infoFilterPresets.equals(_existingFilterPresets)) {
          return new RoomOpenHelper.ValidationResult(false, "filter_presets(com.packethunter.mobile.data.FilterPreset).\n"
                  + " Expected:\n" + _infoFilterPresets + "\n"
                  + " Found:\n" + _existingFilterPresets);
        }
        final HashMap<String, TableInfo.Column> _columnsIntercepts = new HashMap<String, TableInfo.Column>(14);
        _columnsIntercepts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIntercepts.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIntercepts.put("protocol", new TableInfo.Column("protocol", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIntercepts.put("sourceIp", new TableInfo.Column("sourceIp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIntercepts.put("destIp", new TableInfo.Column("destIp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIntercepts.put("sourcePort", new TableInfo.Column("sourcePort", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIntercepts.put("destPort", new TableInfo.Column("destPort", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIntercepts.put("summary", new TableInfo.Column("summary", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIntercepts.put("payloadPreview", new TableInfo.Column("payloadPreview", "BLOB", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIntercepts.put("payloadHex", new TableInfo.Column("payloadHex", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIntercepts.put("payloadAscii", new TableInfo.Column("payloadAscii", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIntercepts.put("filterType", new TableInfo.Column("filterType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIntercepts.put("sni", new TableInfo.Column("sni", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsIntercepts.put("originalHash", new TableInfo.Column("originalHash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysIntercepts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesIntercepts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoIntercepts = new TableInfo("intercepts", _columnsIntercepts, _foreignKeysIntercepts, _indicesIntercepts);
        final TableInfo _existingIntercepts = TableInfo.read(db, "intercepts");
        if (!_infoIntercepts.equals(_existingIntercepts)) {
          return new RoomOpenHelper.ValidationResult(false, "intercepts(com.packethunter.mobile.interception.InterceptItem).\n"
                  + " Expected:\n" + _infoIntercepts + "\n"
                  + " Found:\n" + _existingIntercepts);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "9592997c61563d37fd39c0bb2ba8d1ac", "aaaec2466ce5add66d478793e8438fc1");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "packets","alerts","rules","filter_presets","intercepts");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `packets`");
      _db.execSQL("DELETE FROM `alerts`");
      _db.execSQL("DELETE FROM `rules`");
      _db.execSQL("DELETE FROM `filter_presets`");
      _db.execSQL("DELETE FROM `intercepts`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(PacketDao.class, PacketDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AlertDao.class, AlertDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RuleDao.class, RuleDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FilterPresetDao.class, FilterPresetDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public PacketDao packetDao() {
    if (_packetDao != null) {
      return _packetDao;
    } else {
      synchronized(this) {
        if(_packetDao == null) {
          _packetDao = new PacketDao_Impl(this);
        }
        return _packetDao;
      }
    }
  }

  @Override
  public AlertDao alertDao() {
    if (_alertDao != null) {
      return _alertDao;
    } else {
      synchronized(this) {
        if(_alertDao == null) {
          _alertDao = new AlertDao_Impl(this);
        }
        return _alertDao;
      }
    }
  }

  @Override
  public RuleDao ruleDao() {
    if (_ruleDao != null) {
      return _ruleDao;
    } else {
      synchronized(this) {
        if(_ruleDao == null) {
          _ruleDao = new RuleDao_Impl(this);
        }
        return _ruleDao;
      }
    }
  }

  @Override
  public FilterPresetDao filterPresetDao() {
    if (_filterPresetDao != null) {
      return _filterPresetDao;
    } else {
      synchronized(this) {
        if(_filterPresetDao == null) {
          _filterPresetDao = new FilterPresetDao_Impl(this);
        }
        return _filterPresetDao;
      }
    }
  }
}
