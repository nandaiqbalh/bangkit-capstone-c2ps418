package com.ch2ps418.travelapp.data.remote.firebase.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ch2ps418.travelapp.R
import com.ch2ps418.travelapp.data.local.datastore.DataStoreManager
import com.ch2ps418.travelapp.data.remote.firebase.model.Place
import com.ch2ps418.travelapp.presentation.ui.home.HomeActivity
import com.google.common.reflect.TypeToken
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

const val channelId = "notifiaction_channel"
const val channelName = "com.ch2ps418.travelapp"

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

	@Inject
	lateinit var dataStoreManager: DataStoreManager

	// Override onNewToken to get new token
	override fun onNewToken(token: String) {
		super.onNewToken(token)
		// Handle the new FCM token
		Log.d("FCMTOKEN", token)
		// Launch a coroutine to call the suspending function setDeviceToken
		CoroutineScope(Dispatchers.IO).launch {
			dataStoreManager.setDeviceToken(token)
		}
	}
	private val TAG = "MyFirebaseMsgService"

	override fun onMessageReceived(remoteMessage: RemoteMessage) {

		Log.d("FROM", "From: ${remoteMessage.from.toString()}")

		// Check if notification payload is received
		remoteMessage.notification?.let {
			showNotification(it.title!!,it.body!!)

		}

		if (remoteMessage.data.isNotEmpty()) {
			Log.d("PAYLOAD", "Message data payload: ${remoteMessage.data}")

			val tenNearestPlaceJson = remoteMessage.data["tenNearestPlace"]
			val tenNearestPlaces = Gson().fromJson<List<Place>>(
				tenNearestPlaceJson,
				object : TypeToken<List<Place>>() {}.type
			)

			val tenTopPlaceJson = remoteMessage.data["tenTopPlace"]
			val tenTopPlaces = Gson().fromJson<List<Place>>(
				tenTopPlaceJson,
				object : TypeToken<List<Place>>() {}.type
			)

			Log.d("SUBMITTED", tenNearestPlaces.toString())
			Log.d("SUBMITTED", tenTopPlaces.toString())


			// Send a broadcast to notify the UI
			val intent = Intent("MyCustomAction")
			intent.putExtra("tenNearestPlaces", ArrayList(tenNearestPlaces))
			LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

			val intentTopPlaces = Intent("MyCustomActionTopPlaces")
			intentTopPlaces.putExtra("tenTopPlaces", ArrayList(tenTopPlaces))
			LocalBroadcastManager.getInstance(this).sendBroadcast(intentTopPlaces)
		}

		if (remoteMessage.data.isNotEmpty()) {
			Log.d("BODY", "Message data payload: ${remoteMessage.data}")

		}
	}


	// Method to get the custom Design for the display of notification.
	private fun getCustomDesign(title: String, message: String): RemoteViews {
		val remoteViews = RemoteViews(applicationContext.packageName, R.layout.notification)
		remoteViews.setTextViewText(R.id.tv_notification_title, title)
		remoteViews.setTextViewText(R.id.tv_notification_description, message)
		remoteViews.setImageViewResource(R.id.iv_app_logo_notif, R.drawable.app_logo)
		return remoteViews
	}

	// Method to display the notifications
	private fun showNotification(title: String, message: String) {
		// Pass the intent to switch to the MainActivity
		val intent = Intent(this, HomeActivity::class.java)
		// Assign channel ID
		// Here FLAG_ACTIVITY_CLEAR_TOP flag is set to clear the activities present in the activity stack,
		// on the top of the Activity that is to be launched
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
		// Pass the intent to PendingIntent to start the next Activity
		val pendingIntent = PendingIntent.getActivity(
			this,
			0,
			intent,
			PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
		)

		// Create a Builder object using NotificationCompat class.
		var builder: NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, channelId)
			.setSmallIcon(R.drawable.app_logo)
			.setAutoCancel(true)
			.setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
			.setOnlyAlertOnce(true)
			.setContentIntent(pendingIntent)

		// A customized design for the notification can be set only for Android versions 4.1 and above.
		builder = builder.setContent(getCustomDesign(title, message))

		// Create an object of NotificationManager class to notify the user of events that happen in the background.
		val notificationManager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)
		// Check if the Android Version is greater than Oreo
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
			notificationManager?.createNotificationChannel(notificationChannel)
		}
		notificationManager?.notify(0, builder.build())
	}
}