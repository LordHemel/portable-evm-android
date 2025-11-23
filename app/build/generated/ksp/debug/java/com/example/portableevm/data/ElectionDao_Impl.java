package com.example.portableevm.data;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LongSparseArray;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.RelationUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ElectionDao_Impl implements ElectionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ElectionEntity> __insertionAdapterOfElectionEntity;

  private final EntityInsertionAdapter<CandidateEntity> __insertionAdapterOfCandidateEntity;

  private final SharedSQLiteStatement __preparedStmtOfIncrementVote;

  private final SharedSQLiteStatement __preparedStmtOfCompleteElection;

  public ElectionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfElectionEntity = new EntityInsertionAdapter<ElectionEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `elections` (`id`,`name`,`startTimestamp`,`endTimestamp`,`isCompleted`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ElectionEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindLong(3, entity.getStartTimestamp());
        if (entity.getEndTimestamp() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getEndTimestamp());
        }
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(5, _tmp);
      }
    };
    this.__insertionAdapterOfCandidateEntity = new EntityInsertionAdapter<CandidateEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `candidates` (`id`,`electionId`,`name`,`buttonNumber`,`votes`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CandidateEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getElectionId());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getButtonNumber());
        statement.bindLong(5, entity.getVotes());
      }
    };
    this.__preparedStmtOfIncrementVote = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE candidates SET votes = votes + 1 WHERE electionId = ? AND buttonNumber = ?";
        return _query;
      }
    };
    this.__preparedStmtOfCompleteElection = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE elections SET isCompleted = 1, endTimestamp = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertElection(final ElectionEntity election,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfElectionEntity.insertAndReturnId(election);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertCandidates(final List<CandidateEntity> candidates,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCandidateEntity.insert(candidates);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementVote(final long electionId, final int buttonNumber,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementVote.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, electionId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, buttonNumber);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfIncrementVote.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object completeElection(final long electionId, final long endTimestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfCompleteElection.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, endTimestamp);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, electionId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfCompleteElection.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<ElectionWithCandidates> observeActiveElection() {
    final String _sql = "SELECT * FROM elections WHERE isCompleted = 0 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"candidates",
        "elections"}, new Callable<ElectionWithCandidates>() {
      @Override
      @Nullable
      public ElectionWithCandidates call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfStartTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimestamp");
            final int _cursorIndexOfEndTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "endTimestamp");
            final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
            final LongSparseArray<ArrayList<CandidateEntity>> _collectionCandidates = new LongSparseArray<ArrayList<CandidateEntity>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionCandidates.containsKey(_tmpKey)) {
                _collectionCandidates.put(_tmpKey, new ArrayList<CandidateEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipcandidatesAscomExamplePortableevmDataCandidateEntity(_collectionCandidates);
            final ElectionWithCandidates _result;
            if (_cursor.moveToFirst()) {
              final ElectionEntity _tmpElection;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpName;
              _tmpName = _cursor.getString(_cursorIndexOfName);
              final long _tmpStartTimestamp;
              _tmpStartTimestamp = _cursor.getLong(_cursorIndexOfStartTimestamp);
              final Long _tmpEndTimestamp;
              if (_cursor.isNull(_cursorIndexOfEndTimestamp)) {
                _tmpEndTimestamp = null;
              } else {
                _tmpEndTimestamp = _cursor.getLong(_cursorIndexOfEndTimestamp);
              }
              final boolean _tmpIsCompleted;
              final int _tmp;
              _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
              _tmpIsCompleted = _tmp != 0;
              _tmpElection = new ElectionEntity(_tmpId,_tmpName,_tmpStartTimestamp,_tmpEndTimestamp,_tmpIsCompleted);
              final ArrayList<CandidateEntity> _tmpCandidatesCollection;
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              _tmpCandidatesCollection = _collectionCandidates.get(_tmpKey_1);
              _result = new ElectionWithCandidates(_tmpElection,_tmpCandidatesCollection);
            } else {
              _result = null;
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ElectionWithCandidates>> observeElections() {
    final String _sql = "SELECT * FROM elections ORDER BY startTimestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"candidates",
        "elections"}, new Callable<List<ElectionWithCandidates>>() {
      @Override
      @NonNull
      public List<ElectionWithCandidates> call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfStartTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimestamp");
            final int _cursorIndexOfEndTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "endTimestamp");
            final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
            final LongSparseArray<ArrayList<CandidateEntity>> _collectionCandidates = new LongSparseArray<ArrayList<CandidateEntity>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionCandidates.containsKey(_tmpKey)) {
                _collectionCandidates.put(_tmpKey, new ArrayList<CandidateEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipcandidatesAscomExamplePortableevmDataCandidateEntity(_collectionCandidates);
            final List<ElectionWithCandidates> _result = new ArrayList<ElectionWithCandidates>(_cursor.getCount());
            while (_cursor.moveToNext()) {
              final ElectionWithCandidates _item;
              final ElectionEntity _tmpElection;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpName;
              _tmpName = _cursor.getString(_cursorIndexOfName);
              final long _tmpStartTimestamp;
              _tmpStartTimestamp = _cursor.getLong(_cursorIndexOfStartTimestamp);
              final Long _tmpEndTimestamp;
              if (_cursor.isNull(_cursorIndexOfEndTimestamp)) {
                _tmpEndTimestamp = null;
              } else {
                _tmpEndTimestamp = _cursor.getLong(_cursorIndexOfEndTimestamp);
              }
              final boolean _tmpIsCompleted;
              final int _tmp;
              _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
              _tmpIsCompleted = _tmp != 0;
              _tmpElection = new ElectionEntity(_tmpId,_tmpName,_tmpStartTimestamp,_tmpEndTimestamp,_tmpIsCompleted);
              final ArrayList<CandidateEntity> _tmpCandidatesCollection;
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              _tmpCandidatesCollection = _collectionCandidates.get(_tmpKey_1);
              _item = new ElectionWithCandidates(_tmpElection,_tmpCandidatesCollection);
              _result.add(_item);
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<ElectionWithCandidates> observeElection(final long id) {
    final String _sql = "SELECT * FROM elections WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, true, new String[] {"candidates",
        "elections"}, new Callable<ElectionWithCandidates>() {
      @Override
      @Nullable
      public ElectionWithCandidates call() throws Exception {
        __db.beginTransaction();
        try {
          final Cursor _cursor = DBUtil.query(__db, _statement, true, null);
          try {
            final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
            final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
            final int _cursorIndexOfStartTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimestamp");
            final int _cursorIndexOfEndTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "endTimestamp");
            final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
            final LongSparseArray<ArrayList<CandidateEntity>> _collectionCandidates = new LongSparseArray<ArrayList<CandidateEntity>>();
            while (_cursor.moveToNext()) {
              final long _tmpKey;
              _tmpKey = _cursor.getLong(_cursorIndexOfId);
              if (!_collectionCandidates.containsKey(_tmpKey)) {
                _collectionCandidates.put(_tmpKey, new ArrayList<CandidateEntity>());
              }
            }
            _cursor.moveToPosition(-1);
            __fetchRelationshipcandidatesAscomExamplePortableevmDataCandidateEntity(_collectionCandidates);
            final ElectionWithCandidates _result;
            if (_cursor.moveToFirst()) {
              final ElectionEntity _tmpElection;
              final long _tmpId;
              _tmpId = _cursor.getLong(_cursorIndexOfId);
              final String _tmpName;
              _tmpName = _cursor.getString(_cursorIndexOfName);
              final long _tmpStartTimestamp;
              _tmpStartTimestamp = _cursor.getLong(_cursorIndexOfStartTimestamp);
              final Long _tmpEndTimestamp;
              if (_cursor.isNull(_cursorIndexOfEndTimestamp)) {
                _tmpEndTimestamp = null;
              } else {
                _tmpEndTimestamp = _cursor.getLong(_cursorIndexOfEndTimestamp);
              }
              final boolean _tmpIsCompleted;
              final int _tmp;
              _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
              _tmpIsCompleted = _tmp != 0;
              _tmpElection = new ElectionEntity(_tmpId,_tmpName,_tmpStartTimestamp,_tmpEndTimestamp,_tmpIsCompleted);
              final ArrayList<CandidateEntity> _tmpCandidatesCollection;
              final long _tmpKey_1;
              _tmpKey_1 = _cursor.getLong(_cursorIndexOfId);
              _tmpCandidatesCollection = _collectionCandidates.get(_tmpKey_1);
              _result = new ElectionWithCandidates(_tmpElection,_tmpCandidatesCollection);
            } else {
              _result = null;
            }
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            _cursor.close();
          }
        } finally {
          __db.endTransaction();
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

  private void __fetchRelationshipcandidatesAscomExamplePortableevmDataCandidateEntity(
      @NonNull final LongSparseArray<ArrayList<CandidateEntity>> _map) {
    if (_map.isEmpty()) {
      return;
    }
    if (_map.size() > RoomDatabase.MAX_BIND_PARAMETER_CNT) {
      RelationUtil.recursiveFetchLongSparseArray(_map, true, (map) -> {
        __fetchRelationshipcandidatesAscomExamplePortableevmDataCandidateEntity(map);
        return Unit.INSTANCE;
      });
      return;
    }
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT `id`,`electionId`,`name`,`buttonNumber`,`votes` FROM `candidates` WHERE `electionId` IN (");
    final int _inputSize = _map.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _stmt = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (int i = 0; i < _map.size(); i++) {
      final long _item = _map.keyAt(i);
      _stmt.bindLong(_argIndex, _item);
      _argIndex++;
    }
    final Cursor _cursor = DBUtil.query(__db, _stmt, false, null);
    try {
      final int _itemKeyIndex = CursorUtil.getColumnIndex(_cursor, "electionId");
      if (_itemKeyIndex == -1) {
        return;
      }
      final int _cursorIndexOfId = 0;
      final int _cursorIndexOfElectionId = 1;
      final int _cursorIndexOfName = 2;
      final int _cursorIndexOfButtonNumber = 3;
      final int _cursorIndexOfVotes = 4;
      while (_cursor.moveToNext()) {
        final long _tmpKey;
        _tmpKey = _cursor.getLong(_itemKeyIndex);
        final ArrayList<CandidateEntity> _tmpRelation = _map.get(_tmpKey);
        if (_tmpRelation != null) {
          final CandidateEntity _item_1;
          final long _tmpId;
          _tmpId = _cursor.getLong(_cursorIndexOfId);
          final long _tmpElectionId;
          _tmpElectionId = _cursor.getLong(_cursorIndexOfElectionId);
          final String _tmpName;
          _tmpName = _cursor.getString(_cursorIndexOfName);
          final int _tmpButtonNumber;
          _tmpButtonNumber = _cursor.getInt(_cursorIndexOfButtonNumber);
          final int _tmpVotes;
          _tmpVotes = _cursor.getInt(_cursorIndexOfVotes);
          _item_1 = new CandidateEntity(_tmpId,_tmpElectionId,_tmpName,_tmpButtonNumber,_tmpVotes);
          _tmpRelation.add(_item_1);
        }
      }
    } finally {
      _cursor.close();
    }
  }
}
