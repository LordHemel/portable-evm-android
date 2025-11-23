package com.example.portableevm.data;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class EvmDatabase_Impl extends EvmDatabase {
  private volatile ElectionDao _electionDao;

  private volatile AdminSettingsDao _adminSettingsDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `elections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `startTimestamp` INTEGER NOT NULL, `endTimestamp` INTEGER, `isCompleted` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `candidates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `electionId` INTEGER NOT NULL, `name` TEXT NOT NULL, `buttonNumber` INTEGER NOT NULL, `votes` INTEGER NOT NULL, FOREIGN KEY(`electionId`) REFERENCES `elections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_candidates_electionId` ON `candidates` (`electionId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `admin_settings` (`id` INTEGER NOT NULL, `password` TEXT, `requirePasswordForNewElection` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e28e20016c8315424ba7b63d2b6ef6b7')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `elections`");
        db.execSQL("DROP TABLE IF EXISTS `candidates`");
        db.execSQL("DROP TABLE IF EXISTS `admin_settings`");
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
        db.execSQL("PRAGMA foreign_keys = ON");
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
        final HashMap<String, TableInfo.Column> _columnsElections = new HashMap<String, TableInfo.Column>(5);
        _columnsElections.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsElections.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsElections.put("startTimestamp", new TableInfo.Column("startTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsElections.put("endTimestamp", new TableInfo.Column("endTimestamp", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsElections.put("isCompleted", new TableInfo.Column("isCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysElections = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesElections = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoElections = new TableInfo("elections", _columnsElections, _foreignKeysElections, _indicesElections);
        final TableInfo _existingElections = TableInfo.read(db, "elections");
        if (!_infoElections.equals(_existingElections)) {
          return new RoomOpenHelper.ValidationResult(false, "elections(com.example.portableevm.data.ElectionEntity).\n"
                  + " Expected:\n" + _infoElections + "\n"
                  + " Found:\n" + _existingElections);
        }
        final HashMap<String, TableInfo.Column> _columnsCandidates = new HashMap<String, TableInfo.Column>(5);
        _columnsCandidates.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCandidates.put("electionId", new TableInfo.Column("electionId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCandidates.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCandidates.put("buttonNumber", new TableInfo.Column("buttonNumber", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCandidates.put("votes", new TableInfo.Column("votes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCandidates = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCandidates.add(new TableInfo.ForeignKey("elections", "CASCADE", "NO ACTION", Arrays.asList("electionId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesCandidates = new HashSet<TableInfo.Index>(1);
        _indicesCandidates.add(new TableInfo.Index("index_candidates_electionId", false, Arrays.asList("electionId"), Arrays.asList("ASC")));
        final TableInfo _infoCandidates = new TableInfo("candidates", _columnsCandidates, _foreignKeysCandidates, _indicesCandidates);
        final TableInfo _existingCandidates = TableInfo.read(db, "candidates");
        if (!_infoCandidates.equals(_existingCandidates)) {
          return new RoomOpenHelper.ValidationResult(false, "candidates(com.example.portableevm.data.CandidateEntity).\n"
                  + " Expected:\n" + _infoCandidates + "\n"
                  + " Found:\n" + _existingCandidates);
        }
        final HashMap<String, TableInfo.Column> _columnsAdminSettings = new HashMap<String, TableInfo.Column>(3);
        _columnsAdminSettings.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAdminSettings.put("password", new TableInfo.Column("password", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAdminSettings.put("requirePasswordForNewElection", new TableInfo.Column("requirePasswordForNewElection", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAdminSettings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAdminSettings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAdminSettings = new TableInfo("admin_settings", _columnsAdminSettings, _foreignKeysAdminSettings, _indicesAdminSettings);
        final TableInfo _existingAdminSettings = TableInfo.read(db, "admin_settings");
        if (!_infoAdminSettings.equals(_existingAdminSettings)) {
          return new RoomOpenHelper.ValidationResult(false, "admin_settings(com.example.portableevm.data.AdminSettingsEntity).\n"
                  + " Expected:\n" + _infoAdminSettings + "\n"
                  + " Found:\n" + _existingAdminSettings);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "e28e20016c8315424ba7b63d2b6ef6b7", "f6cb8a0b08a1ede388f333998e588ea9");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "elections","candidates","admin_settings");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `elections`");
      _db.execSQL("DELETE FROM `candidates`");
      _db.execSQL("DELETE FROM `admin_settings`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
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
    _typeConvertersMap.put(ElectionDao.class, ElectionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AdminSettingsDao.class, AdminSettingsDao_Impl.getRequiredConverters());
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
  public ElectionDao electionDao() {
    if (_electionDao != null) {
      return _electionDao;
    } else {
      synchronized(this) {
        if(_electionDao == null) {
          _electionDao = new ElectionDao_Impl(this);
        }
        return _electionDao;
      }
    }
  }

  @Override
  public AdminSettingsDao adminSettingsDao() {
    if (_adminSettingsDao != null) {
      return _adminSettingsDao;
    } else {
      synchronized(this) {
        if(_adminSettingsDao == null) {
          _adminSettingsDao = new AdminSettingsDao_Impl(this);
        }
        return _adminSettingsDao;
      }
    }
  }
}
