package com.squareup.sqldelight.drivers.ios.util

import co.touchlab.sqliter.Statement
import co.touchlab.stately.collections.SharedLruCache
import co.touchlab.stately.concurrency.ThreadLocalRef
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze
import kotlin.native.ref.WeakReference

class LRUStatementCache():StatementCache {

  private val lru = SharedLruCache<Int, AtomicReference<Statement?>>(50) {
    val statement = it.value.value
    statement?.finalizeStatement()
    it.value.value = null
  }

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

  override fun get(index: Int): Statement? = lru.get(index)?.value

  override fun put(index: Int, t: Statement): Statement? {
    t.freeze()

    val statementRef = lru.get(index)
    return if (statementRef == null) {
      lru.put(index, AtomicReference(t))
      null
    } else {
      val current = statementRef.value
      statementRef.value = t
      current
    }
  }

  override fun remove(index: Int): Statement? {
    val ref = lru.get(index)
    return if (ref != null) {
      val statement = ref.value
      ref.value = null
      statement
    } else {
      null
    }
  }

  override fun cleanUp() {
    lru.removeAll(skipCallback = false)
  }
}