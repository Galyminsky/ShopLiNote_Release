package com.jobforandroid.shoplinoterelease.fragments

import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment() {

    abstract fun onClickNew()

    override fun onDestroy() {
        if (FragmentManager.currentFrag === this) {
            FragmentManager.currentFrag = null
        }
        super.onDestroy()
    }

}
