package com.wkeqin.ntqqbattery.ui.activity

import android.content.Context

class SavedInstanceStatePatchedClassReferencer(
    private val baseReferencer: ClassLoader,
    private val hostReferencer: ClassLoader?
) : ClassLoader(Context::class.java.classLoader) {

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        try {
            return super.loadClass(name, resolve)
        } catch (_: ClassNotFoundException) {
        }

        if (name == "androidx.lifecycle.ReportFragment" && hostReferencer != null) {
            try {
                return hostReferencer.loadClass(name)
            } catch (_: ClassNotFoundException) {
            }
        }
        return baseReferencer.loadClass(name)
    }
}
