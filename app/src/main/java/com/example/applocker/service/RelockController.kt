
package com.example.applocker.service

import android.os.Handler
import android.os.Looper

class RelockController(
    private val onRelock: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var relockRunnable: Runnable? = null

    fun start(timeoutSeconds: Int) {
        cancel()

        if (timeoutSeconds <= 0) {
            onRelock()
            return
        }

        relockRunnable = Runnable {
            relockRunnable = null
            onRelock()
        }

        handler.postDelayed(
            relockRunnable!!,
            timeoutSeconds * 1000L
        )
    }

    fun cancel() {
        relockRunnable?.let {
            handler.removeCallbacks(it)
        }
        relockRunnable = null
    }
}
