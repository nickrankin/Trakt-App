package com.nickrankin.traktapp.model

import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nickrankin.traktapp.model.datamodel.BaseDataModel

class SplitViewViewModel: ViewModel() {
    private val _currentPrimaryFragment: MutableLiveData<String> = MutableLiveData()
    val currentPrimaryFragment: LiveData<String?> = _currentPrimaryFragment

    private val _currentSecondaryFragment: MutableLiveData<BaseDataModel> = MutableLiveData()
    val currentSecondaryFragment: LiveData<BaseDataModel?> = _currentSecondaryFragment

    fun switchPrimaryFragment(fragmentTag: String?) {
        _currentPrimaryFragment.value = fragmentTag
    }

    fun switchSecondaryFragment(baseDataModel: BaseDataModel?) {
        _currentSecondaryFragment.value = baseDataModel
    }
}