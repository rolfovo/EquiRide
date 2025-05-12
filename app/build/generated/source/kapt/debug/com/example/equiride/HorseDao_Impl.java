package com.example.equiride;

import android.database.Cursor;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
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
public final class HorseDao_Impl implements HorseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Horse> __insertionAdapterOfHorse;

  private final EntityDeletionOrUpdateAdapter<Horse> __deletionAdapterOfHorse;

  public HorseDao_Impl(RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHorse = new EntityInsertionAdapter<Horse>(__db) {
      @Override
      public String createQuery() {
        return "INSERT OR ABORT INTO `horses` (`id`,`name`,`walkSpeed`,`trotSpeed`,`gallopSpeed`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Horse value) {
        stmt.bindLong(1, value.getId());
        if (value.getName() == null) {
          stmt.bindNull(2);
        } else {
          stmt.bindString(2, value.getName());
        }
        stmt.bindDouble(3, value.getWalkSpeed());
        stmt.bindDouble(4, value.getTrotSpeed());
        stmt.bindDouble(5, value.getGallopSpeed());
      }
    };
    this.__deletionAdapterOfHorse = new EntityDeletionOrUpdateAdapter<Horse>(__db) {
      @Override
      public String createQuery() {
        return "DELETE FROM `horses` WHERE `id` = ?";
      }

      @Override
      public void bind(SupportSQLiteStatement stmt, Horse value) {
        stmt.bindLong(1, value.getId());
      }
    };
  }

  @Override
  public long insert(final Horse h) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      long _result = __insertionAdapterOfHorse.insertAndReturnId(h);
      __db.setTransactionSuccessful();
      return _result;
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public void delete(final Horse h) {
    __db.assertNotSuspendingTransaction();
    __db.beginTransaction();
    try {
      __deletionAdapterOfHorse.handle(h);
      __db.setTransactionSuccessful();
    } finally {
      __db.endTransaction();
    }
  }

  @Override
  public LiveData<List<Horse>> getAll() {
    final String _sql = "SELECT * FROM horses";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return __db.getInvalidationTracker().createLiveData(new String[]{"horses"}, false, new Callable<List<Horse>>() {
      @Override
      public List<Horse> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfWalkSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "walkSpeed");
          final int _cursorIndexOfTrotSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "trotSpeed");
          final int _cursorIndexOfGallopSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "gallopSpeed");
          final List<Horse> _result = new ArrayList<Horse>(_cursor.getCount());
          while(_cursor.moveToNext()) {
            final Horse _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final double _tmpWalkSpeed;
            _tmpWalkSpeed = _cursor.getDouble(_cursorIndexOfWalkSpeed);
            final double _tmpTrotSpeed;
            _tmpTrotSpeed = _cursor.getDouble(_cursorIndexOfTrotSpeed);
            final double _tmpGallopSpeed;
            _tmpGallopSpeed = _cursor.getDouble(_cursorIndexOfGallopSpeed);
            _item = new Horse(_tmpId,_tmpName,_tmpWalkSpeed,_tmpTrotSpeed,_tmpGallopSpeed);
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

  @Override
  public Horse getById(final long id) {
    final String _sql = "SELECT * FROM horses WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    __db.assertNotSuspendingTransaction();
    final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
    try {
      final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
      final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
      final int _cursorIndexOfWalkSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "walkSpeed");
      final int _cursorIndexOfTrotSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "trotSpeed");
      final int _cursorIndexOfGallopSpeed = CursorUtil.getColumnIndexOrThrow(_cursor, "gallopSpeed");
      final Horse _result;
      if(_cursor.moveToFirst()) {
        final long _tmpId;
        _tmpId = _cursor.getLong(_cursorIndexOfId);
        final String _tmpName;
        if (_cursor.isNull(_cursorIndexOfName)) {
          _tmpName = null;
        } else {
          _tmpName = _cursor.getString(_cursorIndexOfName);
        }
        final double _tmpWalkSpeed;
        _tmpWalkSpeed = _cursor.getDouble(_cursorIndexOfWalkSpeed);
        final double _tmpTrotSpeed;
        _tmpTrotSpeed = _cursor.getDouble(_cursorIndexOfTrotSpeed);
        final double _tmpGallopSpeed;
        _tmpGallopSpeed = _cursor.getDouble(_cursorIndexOfGallopSpeed);
        _result = new Horse(_tmpId,_tmpName,_tmpWalkSpeed,_tmpTrotSpeed,_tmpGallopSpeed);
      } else {
        _result = null;
      }
      return _result;
    } finally {
      _cursor.close();
      _statement.release();
    }
  }

  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
