package com.example.equiride;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenHelper;
import androidx.room.RoomOpenHelper.Delegate;
import androidx.room.RoomOpenHelper.ValidationResult;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.room.util.TableInfo.Column;
import androidx.room.util.TableInfo.ForeignKey;
import androidx.room.util.TableInfo.Index;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Callback;
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile HorseDao _horseDao;

  private volatile RideDao _rideDao;

  @Override
  protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration configuration) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(configuration, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("CREATE TABLE IF NOT EXISTS `horses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `walkSpeed` REAL NOT NULL, `trotSpeed` REAL NOT NULL, `gallopSpeed` REAL NOT NULL)");
        _db.execSQL("CREATE TABLE IF NOT EXISTS `rides` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `horseId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `distance` REAL NOT NULL, `walkPortion` REAL NOT NULL, `trotPortion` REAL NOT NULL, `gallopPortion` REAL NOT NULL, `geoJson` TEXT NOT NULL, FOREIGN KEY(`horseId`) REFERENCES `horses`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        _db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        _db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'b78986db727dc6bc0102cbedbb116e60')");
      }

      @Override
      public void dropAllTables(SupportSQLiteDatabase _db) {
        _db.execSQL("DROP TABLE IF EXISTS `horses`");
        _db.execSQL("DROP TABLE IF EXISTS `rides`");
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onDestructiveMigration(_db);
          }
        }
      }

      @Override
      public void onCreate(SupportSQLiteDatabase _db) {
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onCreate(_db);
          }
        }
      }

      @Override
      public void onOpen(SupportSQLiteDatabase _db) {
        mDatabase = _db;
        _db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(_db);
        if (mCallbacks != null) {
          for (int _i = 0, _size = mCallbacks.size(); _i < _size; _i++) {
            mCallbacks.get(_i).onOpen(_db);
          }
        }
      }

      @Override
      public void onPreMigrate(SupportSQLiteDatabase _db) {
        DBUtil.dropFtsSyncTriggers(_db);
      }

      @Override
      public void onPostMigrate(SupportSQLiteDatabase _db) {
      }

      @Override
      public RoomOpenHelper.ValidationResult onValidateSchema(SupportSQLiteDatabase _db) {
        final HashMap<String, TableInfo.Column> _columnsHorses = new HashMap<String, TableInfo.Column>(5);
        _columnsHorses.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHorses.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHorses.put("walkSpeed", new TableInfo.Column("walkSpeed", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHorses.put("trotSpeed", new TableInfo.Column("trotSpeed", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHorses.put("gallopSpeed", new TableInfo.Column("gallopSpeed", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHorses = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHorses = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHorses = new TableInfo("horses", _columnsHorses, _foreignKeysHorses, _indicesHorses);
        final TableInfo _existingHorses = TableInfo.read(_db, "horses");
        if (! _infoHorses.equals(_existingHorses)) {
          return new RoomOpenHelper.ValidationResult(false, "horses(com.example.equiride.Horse).\n"
                  + " Expected:\n" + _infoHorses + "\n"
                  + " Found:\n" + _existingHorses);
        }
        final HashMap<String, TableInfo.Column> _columnsRides = new HashMap<String, TableInfo.Column>(8);
        _columnsRides.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("horseId", new TableInfo.Column("horseId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("distance", new TableInfo.Column("distance", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("walkPortion", new TableInfo.Column("walkPortion", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("trotPortion", new TableInfo.Column("trotPortion", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("gallopPortion", new TableInfo.Column("gallopPortion", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRides.put("geoJson", new TableInfo.Column("geoJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRides = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysRides.add(new TableInfo.ForeignKey("horses", "CASCADE", "NO ACTION",Arrays.asList("horseId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesRides = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRides = new TableInfo("rides", _columnsRides, _foreignKeysRides, _indicesRides);
        final TableInfo _existingRides = TableInfo.read(_db, "rides");
        if (! _infoRides.equals(_existingRides)) {
          return new RoomOpenHelper.ValidationResult(false, "rides(com.example.equiride.Ride).\n"
                  + " Expected:\n" + _infoRides + "\n"
                  + " Found:\n" + _existingRides);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "b78986db727dc6bc0102cbedbb116e60", "dd9c11bf9c817a4a301b8dcd57c39930");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
        .name(configuration.name)
        .callback(_openCallback)
        .build();
    final SupportSQLiteOpenHelper _helper = configuration.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "horses","rides");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `horses`");
      _db.execSQL("DELETE FROM `rides`");
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
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(HorseDao.class, HorseDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(RideDao.class, RideDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  public List<Migration> getAutoMigrations(
      @NonNull Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecsMap) {
    return Arrays.asList();
  }

  @Override
  public HorseDao horseDao() {
    if (_horseDao != null) {
      return _horseDao;
    } else {
      synchronized(this) {
        if(_horseDao == null) {
          _horseDao = new HorseDao_Impl(this);
        }
        return _horseDao;
      }
    }
  }

  @Override
  public RideDao rideDao() {
    if (_rideDao != null) {
      return _rideDao;
    } else {
      synchronized(this) {
        if(_rideDao == null) {
          _rideDao = new RideDao_Impl(this);
        }
        return _rideDao;
      }
    }
  }
}
