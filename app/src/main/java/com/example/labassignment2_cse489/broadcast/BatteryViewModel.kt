package com.example.labassignment2_cse489.broadcast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

/**
 * Gives the battery screen (Activity 2, battery mode) its live battery status.
 *
 * The LiveData itself decides when to listen (only while the screen is visible). The ViewModel
 * keeps that one instance across rotation, so the last value is shown again the moment the
 * recreated activity starts observing.
 */
class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    val batteryStatus: LiveData<BatteryStatus> = BatteryStatusLiveData(application)
}
