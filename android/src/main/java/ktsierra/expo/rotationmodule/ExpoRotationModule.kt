package ktsierra.expo.rotationmodule

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoRotationModule : Module() {
  companion object {
    const val NAME = "ExpoRotationModule"
    const val TAG = "ExpoRotationModule"
  }

  override fun definition() = ModuleDefinition {
    Name(NAME)

    AsyncFunction("canWrite") {
      try {
        val ctx = appContext.activityProvider?.currentActivity ?: appContext.reactContext!!
        val can = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          Settings.System.canWrite(ctx)
        } else {
          true
        }
        return@AsyncFunction can
      } catch (e: Exception) {
        Log.e(TAG, "canWrite error", e)
        throw Exception("E_GET_ROTATION: ${e.message}")
      }
    }

    Function("requestWritePermission") {
      try {
        val activity: Activity? = appContext.activityProvider?.currentActivity
        val activityPkg = activity?.applicationContext?.packageName
        val reactPkg = appContext.reactContext?.packageName
        val pkgName = activityPkg ?: reactPkg ?: ""
        Log.i(TAG, "requestWritePermission - activityPkg=$activityPkg reactPkg=$reactPkg chosen=$pkgName")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
          intent.data = Uri.parse("package:$pkgName")
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          Log.i(TAG, "Opening WRITE_SETTINGS for package: $pkgName")
          if (activity != null) {
            activity.startActivity(intent)
          } else {
            appContext.reactContext?.startActivity(intent)
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "requestWritePermission error", e)
      }
      return@Function null
    }

    Function("getPackageName") {
      try {
        val pkg = appContext.activityProvider?.currentActivity?.packageName ?: appContext.reactContext?.packageName ?: ""
        return@Function pkg
      } catch (e: Exception) {
        Log.e(TAG, "getPackageName error", e)
        return@Function ""
      }
    }

    AsyncFunction("getRotationState") {
      try {
        val ctx = appContext.activityProvider?.currentActivity ?: appContext.reactContext!!
        val resolver: ContentResolver = ctx.contentResolver
        val auto = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
          Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 1)
        } else {
          1
        }
        if (auto == 1) {
          return@AsyncFunction "AUTOROTATE"
        } else {
          val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Settings.System.getInt(resolver, Settings.System.USER_ROTATION, 0)
          } else {
            0
          }
          return@AsyncFunction when (rotation) {
            0 -> "PORTRAIT"
            1 -> "LANDSCAPE"
            3 -> "LANDSCAPE"
            else -> "PORTRAIT"
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "getRotationState error", e)
        throw Exception("E_GET_ROTATION: ${e.message}")
      }
    }

    AsyncFunction("setRotationState") { state: String ->
      try {
        val ctx = appContext.activityProvider?.currentActivity ?: appContext.reactContext!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.System.canWrite(ctx)) {
          throw Exception("E_PERMISSION: WRITE_SETTINGS not granted")
        }
        val resolver: ContentResolver = ctx.contentResolver
        when (state) {
          "AUTOROTATE" -> {
            Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 1)
          }
          "PORTRAIT" -> {
            Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            Settings.System.putInt(resolver, Settings.System.USER_ROTATION, 0)
          }
          "LANDSCAPE" -> {
            Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            Settings.System.putInt(resolver, Settings.System.USER_ROTATION, 1)
          }
          else -> {
            throw Exception("E_INVALID_STATE: Invalid rotation state: $state")
          }
        }
        return@AsyncFunction null
      } catch (e: Exception) {
        Log.e(TAG, "setRotationState error", e)
        throw Exception("E_SET_ROTATION: ${e.message}")
      }
    }
  }
}
