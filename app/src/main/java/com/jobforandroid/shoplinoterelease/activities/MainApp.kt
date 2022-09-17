package com.jobforandroid.shoplinoterelease.activities

import android.app.Application
import com.jobforandroid.shoplinoterelease.BD.MainDataBase


class MainApp : Application() {

    val database by lazy { MainDataBase.getDataBase(this) }

}