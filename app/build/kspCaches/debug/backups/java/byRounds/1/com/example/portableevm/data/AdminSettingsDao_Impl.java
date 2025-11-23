package com.example.portableevm.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AdminSettingsDao_Impl implements AdminSettingsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AdminSettingsEntity> __insertionAdapterOfAdminSettingsEntity;

  public AdminSettingsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAdminSettingsEntity = new EntityInsertionAdapter<AdminSettingsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `admin_settings` (`id`,`password`,`requirePasswordForNewElection`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AdminSettingsEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getPassword() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getPassword());
        }
        final int _tmp = entity.getRequirePasswordForNewElection() ? 1 : 0;
        statement.bindLong(3, _tmp);
      }
    };
  }

  @Override
  public Object upsert(final AdminSettingsEntity settings,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAdminSettingsEntity.insert(settings);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<AdminSettingsEntity> observeSettings() {
    final String _sql = "SELECT * FROM admin_settings WHERE id = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"admin_settings"}, new Callable<AdminSettingsEntity>() {
      @Override
      @Nullable
      public AdminSettingsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPassword = CursorUtil.getColumnIndexOrThrow(_cursor, "password");
          final int _cursorIndexOfRequirePasswordForNewElection = CursorUtil.getColumnIndexOrThrow(_cursor, "requirePasswordForNewElection");
          final AdminSettingsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpPassword;
            if (_cursor.isNull(_cursorIndexOfPassword)) {
              _tmpPassword = null;
            } else {
              _tmpPassword = _cursor.getString(_cursorIndexOfPassword);
            }
            final boolean _tmpRequirePasswordForNewElection;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfRequirePasswordForNewElection);
            _tmpRequirePasswordForNewElection = _tmp != 0;
            _result = new AdminSettingsEntity(_tmpId,_tmpPassword,_tmpRequirePasswordForNewElection);
          } else {
            _result = null;
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
