package com.squareup.sqldelight.drivers.ios.util

import co.touchlab.sqliter.Statement
import co.touchlab.stately.concurrency.ThreadLocalRef
import kotlin.native.concurrent.freeze
import kotlin.native.ref.WeakReference

class WeakLocalStatementCache(private val delegate:StatementCache):StatementCache{
  private val localCache = ThreadLocalRef<LastStatementInfo>()

  private val lc:LastStatementInfo
    get() {
      val l = localCache.get()
      return if(l == null){
        val newInfo = LastStatementInfo()
        localCache.set(newInfo)
        newInfo
      }else{
        l
      }
    }

  override fun get(index: Int): Statement? {
    val statementsWeak = lc.statementsWeak

    val localCacheStatement = if(statementsWeak.size > index){ statementsWeak[index]?.get()}else{null}
    if (localCacheStatement != null)
      return localCacheStatement

    val cachedStatement = delegate.get(index)
    storeStatementLocal(cachedStatement, index)
    return cachedStatement
  }

  private fun storeStatementLocal(cachedStatement: Statement?, index: Int) {
    if (cachedStatement != null) {
      val statementsWeak = lc.statementsWeak

      while (statementsWeak.size <= index)
        statementsWeak.add(null)
      statementsWeak[index] = WeakReference(cachedStatement)
    }
  }

  override fun put(index: Int, t: Statement): Statement? {
    t.freeze()
    storeStatementLocal(t, index)
    return delegate.put(index, t)
  }

  override fun remove(index: Int): Statement? {
    val statementsWeak = lc.statementsWeak
    if(statementsWeak.size > index)
      statementsWeak[index] = null
    return delegate.remove(index)
  }

  override fun cleanUp() {
    val statementsWeak = lc.statementsWeak
    val statementSize = statementsWeak.size
    for (i in 0 until statementSize){
      statementsWeak[i] = null
    }

    delegate.cleanUp()
  }

}

internal class LastStatementInfo {
  var statementsWeak = ArrayList<WeakReference<Statement>?>(30)
}