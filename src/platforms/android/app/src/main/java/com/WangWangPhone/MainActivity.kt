package com.WangWangPhone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.WangWangPhone.core.LicenseManager
import com.WangWangPhone.ui.MainContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化授权管理器，从数据库恢复激活状态
        val licenseManager = LicenseManager.getInstance(this)
        licenseManager.initialize()

        setContent {
            MainContainer()
        }
    }
}