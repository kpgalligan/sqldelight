package com.squareup.sqldelight.drivers.ios.util

import kotlin.native.concurrent.AtomicReference
import kotlin.native.concurrent.freeze

internal class LinearCacheNoLock<T>() {
  private val CACHE_INCREMENT_SIZE = 50
  private val entries: AtomicReference<Array<AtomicReference<T?>>?>

  init {
    val starter = Array<AtomicReference<T?>>(CACHE_INCREMENT_SIZE) {
      AtomicReference(null)
    }

    entries = AtomicReference(starter.freeze())
  }

  fun cleanUp(block: (T) -> Unit) {
    var e: Array<AtomicReference<T?>>?

    while (true) {
      e = entries.value
      if (e != null && entries.compareAndSet(e, emptyArray())){
        break
      }
    }

    val eForReal = e!!
    eForReal.forEach {
      val t = it.value
      t?.let(block)
    }
  }

  fun remove(index: Int): T? {
    val e = internalGetEntries()
    val ref = e[index]
    while (true) {
      val current = ref.value
      if (ref.compareAndSet(current, null)) {
        return current
      }
    }
  }

  fun put(index: Int, t: T): T? {
    t.freeze()
    while (true) {
      val result = internalPut(index, t)
      if (result.success)
        return result.previous
    }
  }

  val size:Int
    get() = internalGetEntries().filter { it.value != null }.size

  internal fun internalGetEntries(): Array<AtomicReference<T?>> {
    var e: Array<AtomicReference<T?>>?

    while (true) {
      e = entries.value
      if (e != null)
        break
    }

    val eForReal = e!!
    if(eForReal.isEmpty())
      throw IllegalStateException("Cache closed")
    return eForReal
  }

  private fun internalPut(index: Int, t: T): PutResult<T> {
    var e2 = internalGetEntries()

    if (e2.size <= index) {
      if (entries.compareAndSet(e2, null)) {
        var newSize = e2.size
        while (newSize <= index){
          newSize += CACHE_INCREMENT_SIZE
        }
        val newArray = Array<AtomicReference<T?>>(newSize) {
          if (it < e2.size) {
            e2[it]
          } else {
            AtomicReference(null)
          }
        }
        if (!entries.compareAndSet(null, newArray.freeze()))
          throw IllegalStateException("Shouldn't ever get here, but concurrency is hard")
        e2 = newArray
      } else {
        return PutResult(false, null)
      }
    }

    val ref = e2[index]
    while (true) {
      val current = ref.value
      if (ref.compareAndSet(current, t)) {
        return PutResult(true, current)
      }
    }
  }

  private data class PutResult<T>(val success: Boolean, val previous: T?)

}