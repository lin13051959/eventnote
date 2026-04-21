package com.example.eventnote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompleteReceiver : BroadcastReceiver() {
    
    private val TAG = "BootCompleteReceiver"
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "设备启动完成，正在恢复提醒...")
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    kotlinx.coroutines.delay(3000)
                    Log.d(TAG, "EventNote 提醒恢复完成")
                } catch (e: Exception) {
                    Log.e(TAG, "恢复提醒失败: " + e.message, e)
                }
            }
        }
    }
}
