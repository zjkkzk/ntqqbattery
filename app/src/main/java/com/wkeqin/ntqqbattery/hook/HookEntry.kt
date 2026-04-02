package com.wkeqin.ntqqbattery.hook

import com.wkeqin.ntqqbattery.BuildConfig
import com.wkeqin.ntqqbattery.const.PackageName
import com.wkeqin.ntqqbattery.hook.entity.NTQQHooker
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.configs
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {

    override fun onInit() = configs {
        debugLog { 
            tag = "NTQQBattery"
            isRecord = false // 关闭执行路径记录，防止高频 Hook (如 w 方法) 疯狂刷屏
        }
        isDebug = BuildConfig.DEBUG
        isEnableDataChannel = false
    }

    override fun onHook() = encase {
        loadApp(PackageName.QQ) {
            loadHooker(NTQQHooker)
        }
    }
}
