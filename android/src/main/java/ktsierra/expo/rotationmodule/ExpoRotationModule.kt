package ktsierra.expo.rotationmodule

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.content.pm.ActivityInfo
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.abs


class ExpoRotationModule : Module() {
  companion object {
    const val NAME = "ExpoRotationModule"
    const val TAG = "ExpoRotationModule"
  }

  private var orientationListener: android.view.OrientationEventListener? = null
  private var desiredAxis: String? = null
  private var lastWrittenRotation: Int = -1

  // Sensor-related fields for tilt detection and debouncing
  private var sensorManager: SensorManager? = null
  private var accelSensor: Sensor? = null
  private var accelListener: SensorEventListener? = null
  // tilt threshold (degrees) below which device is considered "flat"
  private val tiltThresholdDegrees = 20f
  // debounce: must see same rot candidate this many times before writing
  private var stableCount = 0
  private val stableRequired = 3
  private var lastRotCandidate = -1
  // last measured inclination in degrees (0 = flat, 90 = upright)
  private var lastInclinationDeg = 90f

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

    // Expose helpers (optional)
    Function("startOrientationListener") {
      val ctx = appContext.activityProvider?.currentActivity ?: appContext.reactContext!!
      startOrientationListener(ctx)
      return@Function null
    }

    Function("stopOrientationListener") {
      orientationListener?.disable()
      orientationListener = null
      desiredAxis = null
      lastWrittenRotation = -1
      // unregister accelerometer listener if registered
      try {
        if (sensorManager != null && accelListener != null) {
          sensorManager?.unregisterListener(accelListener)
        }
      } catch (e: Exception) {
        Log.e(TAG, "stopOrientationListener sensor unregister error", e)
      }
      accelListener = null
      accelSensor = null
      sensorManager = null
      // reset debounce
      stableCount = 0
      lastRotCandidate = -1
      lastInclinationDeg = 90f
      return@Function null
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
            0, 2 -> "PORTRAIT"
            1, 3 -> "LANDSCAPE"
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

        when (state) {
          "AUTOROTATE" -> {
            // Restore system auto-rotate
            val resolver: ContentResolver = ctx.contentResolver
            Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 1)
            // unregister listener if present
            orientationListener?.disable()
            orientationListener = null
            desiredAxis = null
            lastWrittenRotation = -1
          }
          "PORTRAIT" -> {
            // disable system auto-rotate and enable axis-lock with sensor flips on portrait axis
            val resolver: ContentResolver = ctx.contentResolver
            Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            desiredAxis = "PORTRAIT"
            startOrientationListener(ctx)
          }
          "LANDSCAPE" -> {
            val resolver: ContentResolver = ctx.contentResolver
            Settings.System.putInt(resolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            desiredAxis = "LANDSCAPE"
            startOrientationListener(ctx)
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

  private fun startOrientationListener(ctx: Context) {
    // If listener already exists, do nothing
    if (orientationListener != null) return

    // set up accelerometer to measure inclination
    sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    accelListener = object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0].toDouble()
        val y = event.values[1].toDouble()
        val z = event.values[2].toDouble()
        val g = sqrt(x * x + y * y + z * z)
        if (g > 0.0) {
          var inclRad = atan2(z, sqrt(x * x + y * y))
          var inclDeg = Math.toDegrees(inclRad).toFloat()
          inclDeg = abs(inclDeg)
          // clamp
          if (inclDeg > 90f) inclDeg = 90f
          lastInclinationDeg = inclDeg
        }
      }

      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }
    if (accelSensor != null) {
      // Use UI delay for reasonable responsiveness
      sensorManager?.registerListener(accelListener, accelSensor, SensorManager.SENSOR_DELAY_UI)
    }

    orientationListener = object : android.view.OrientationEventListener(ctx) {
      override fun onOrientationChanged(orientation: Int) {
        // orientation: 0..359 degrees, or ORIENTATION_UNKNOWN (-1)
        if (orientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) return
        // Normalize to one of the 4 Android rotations: 0, 90, 180, 270
        val rot = when {
          orientation in 315..359 || orientation in 0..44 -> 0
          orientation in 45..134 -> 1
          orientation in 135..224 -> 2
          else -> 3
        }

        try {
          val resolver: ContentResolver = ctx.contentResolver

          // If device is nearly flat, skip writing to avoid noise
          if (lastInclinationDeg < tiltThresholdDegrees) {
            lastRotCandidate = -1
            stableCount = 0
            return
          }

          // Debounce: require several consistent buckets
          if (rot == lastRotCandidate) {
            stableCount += 1
          } else {
            lastRotCandidate = rot
            stableCount = 1
          }
          if (stableCount < stableRequired) return

          // desiredAxis controls whether we want portrait or landscape axis.
          when (desiredAxis) {
            "PORTRAIT" -> {
              // We want 0 or 2 depending on angle
              val target = if (rot == 2) 2 else 0
              if (lastWrittenRotation != target) {
                Settings.System.putInt(resolver, Settings.System.USER_ROTATION, target)
                lastWrittenRotation = target
              }
            }
            "LANDSCAPE" -> {
              // We want 1 or 3 depending on angle
              val target = if (rot == 1) 3 else if (rot == 3) 1 else if (rot == 2) 1 else 3
              // swapped mapping: 90 -> 3, 270 -> 1, fallback 180->1, 0->3
              if (lastWrittenRotation != target) {
                Settings.System.putInt(resolver, Settings.System.USER_ROTATION, target)
                lastWrittenRotation = target
              }
            }
          }
        } catch (e: Exception) {
          Log.e(TAG, "orientation listener write error", e)
        }
      }
    }

    orientationListener?.enable()
  }
}
