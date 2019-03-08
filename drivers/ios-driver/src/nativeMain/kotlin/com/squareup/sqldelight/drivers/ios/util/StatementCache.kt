package com.squareup.sqldelight.drivers.ios.util

import co.touchlab.sqliter.Statement

interface StatementCache{
  fun get(index: Int): Statement?
  fun put(index: Int, t: Statement): Statement?
  fun remove(index: Int): Statement?
  fun cleanUp()
}

