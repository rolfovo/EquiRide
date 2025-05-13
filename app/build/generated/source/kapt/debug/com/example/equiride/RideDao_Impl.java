package com.example.equiride;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings({"unchecked", "deprecation"})
public final class RideDao_Impl implements RideDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Ride> __insertionAdapterOfRide;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByHorse;

  public RideDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRide = new EntityInsertionAdapter<Ride>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `rides` (`id`,`horseId`,`timestamp`,`durationSeconds`,`distance`,`walkPortion`,`trotPortion`,`gallopPortion`,`geoJson`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Ride value) {
        stmt.bindLong(1, value.getId());
        stmt.bindLong(2, value.getHorseId());
        stmt.bindLong(3, value.getTimestamp());
        stmt.bindLong(4, value.getDurationSeconds());
        stmt.bindDouble(5, value.getDistance());
        stmt.bindDouble(6, value.getWalkPortion());
        stmt.bindDouble(7, value.getTrotPortion());
        stmt.bindDouble(8, value.getGallopPortion());
        if (value.getGeoJson() == null) {
          stmt.bindNull(9);
        } else {
          stmt.bindString(9, value.getGeoJson());
        }
      }
    };
    this.__preparedStmtOfDeleteByHorse = new SharedSQLiteStatement(__db) {
      @Override
      public String createQuery() {
        final String _query = "DELETE FROM rides WHERE horseId = ?";
        return _query;
      }
    };
  }

  @Override
  public void insert(final Ride r) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __insertionAdapterOfRide.insert(r);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void deleteByHorse(final long horseId) {
    __db.assertNotSuspendingTransaction();
    final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByHorse.acquire();
    int _argIndex = 1;
    _stmt.bindLong(_argIndex, horseId);
    __db.beginTransaction();
    try {
      _stmt.executeUpdateDelete();
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
      __preparedStmtOfDeleteByHorse.release(_stmt);
    }
  }

  @Override
  public LiveData<List<Ride>> getByHorse(final long horseId) {
    final String _sql = "SELECT * FROM rides WHERE horseId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, horseId);
    return __db.getInvalidationTracker().createLiveData(new String[]{"rides"}, false, new Callable<List<Ride>>() {
      @Override
      public List<Ride> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHorseId = CursorUtil.getColumnIndexOrThrow(_cursor, "horseId");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfDistance = CursorUtil.getColumnIndexOrThrow(_cursor, "distance");
          final int _cursorIndexOfWalkPortion = CursorUtil.getColumnIndexOrThrow(_cursor, "walkPortion");
          final int _cursorIndexOfTrotPortion = CursorUtil.getColumnIndexOrThrow(_cursor, "trotPortion");
          final int _cursorIndexOfGallopPortion = CursorUtil.getColumnIndexOrThrow(_cursor, "gallopPortion");
          final int _cursorIndexOfGeoJson = CursorUtil.getColumnIndexOrThrow(_cursor, "geoJson");
          final List<Ride> _result = new ArrayList<Ride>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final Ride _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpHorseId;
            _tmpHorseId = _cursor.getLong(_cursorIndexOfHorseId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final long _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getLong(_cursorIndexOfDurationSeconds);
            final double _tmpDistance;
            _tmpDistance = _cursor.getDouble(_cursorIndexOfDistance);
            final double _tmpWalkPortion;
            _tmpWalkPortion = _cursor.getDouble(_cursorIndexOfWalkPortion);
            final double _tmpTrotPortion;
            _tmpTrotPortion = _cursor.getDouble(_cursorIndexOfTrotPortion);
            final double _tmpGallopPortion;
            _tmpGallopPortion = _cursor.getDouble(_cursorIndexOfGallopPortion);
            final String _tmpGeoJson;
            if (_cursor.isNull(_cursorIndexOfGeoJson)) {
              _tmpGeoJson = null;
            } else {
              _tmpGeoJson = _cursor.getString(_cursorIndexOfGeoJson);
            }
            _item = new Ride(_tmpId,_tmpHorseId,_tmpTimestamp,_tmpDurationSeconds,_tmpDistance,_tmpWalkPortion,_tmpTrotPortion,_tmpGallopPortion,_tmpGeoJson);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
