package com.example.proxima

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class User(val uid: String, val username: String, val email: String, val latitude: Double, val longitude: Double) : Parcelable{
    constructor(): this("", "", "",0.0, 0.0)

}