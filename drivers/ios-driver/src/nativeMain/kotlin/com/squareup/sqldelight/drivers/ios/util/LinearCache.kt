package com.squareup.sqldelight.drivers.ios.util

import co.touchlab.stately.concurrency.Lock
import co.touchlab.stately.concurrency.withLock
import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

internal class LinearCache<T>() {
  private val CACHE_INCREMENT_SIZE = 50
  private val entries: AtomicReference<Array<AtomicReference<T?>>>
  private val lock = Lock()

  init {
    val starter = Array<AtomicReference<T?>>(CACHE_INCREMENT_SIZE) {
      AtomicReference(null)
    }

    entries = AtomicReference(starter.freeze())
  }

  fun cleanUp(block: (T) -> Unit) = lock.withLock {
    val eForReal = entries.value

    entries.value = emptyArray()

    eForReal.forEach {
      val t = it.value
      t?.let(block)
    }
  }

  fun remove(index: Int): T? = lock.withLock {
    val e = entries.value
    if (e.size > index) {
      val result = e[index].value
      e[index].value = null
      result
    } else {
      null
    }
  }

  fun put(index: Int, t: T): T? = lock.withLock {

    ensureSize(index + 1)

    t.freeze()

    val ref = entries.value[index]
    val result = ref.value
    ref.value = t
    result
  }

  private fun ensureSize(size: Int) {
    while (entries.value.size < size) {
      val e2 = entries.value
      val newArray = Array<AtomicReference<T?>>(e2.size + CACHE_INCREMENT_SIZE) {
        if (it < e2.size) {
          e2[it]
        } else {
          AtomicReference(null)
        }
      }
      entries.value = newArray.freeze()
    }
  }

  val size: Int
    get() = lock.withLock { entries.value.filter { it.value != null }.size }

  internal val testAccessEntries: Array<AtomicReference<T?>>
    get() = entries.value
}