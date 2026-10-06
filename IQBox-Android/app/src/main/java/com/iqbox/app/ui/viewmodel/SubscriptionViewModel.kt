package com.iqbox.app.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import com.android.billingclient.api.ProductDetails
import com.iqbox.app.data.billing.BillingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SubscriptionViewModel(context: Context) : ViewModel() {
    val billingManager = BillingManager(context)
    
    val isConnected: StateFlow<Boolean> = billingManager.isConnected
    val weeklyDetails: StateFlow<ProductDetails?> = billingManager.weeklyDetails
    val monthlyDetails: StateFlow<ProductDetails?> = billingManager.monthlyDetails
    val isPremium: StateFlow<Boolean> = billingManager.isPremium
    val purchaseMessage: StateFlow<String?> = billingManager.purchaseMessage
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        billingManager.initialize()
    }
    
    fun purchaseWeekly(activity: Activity) {
        billingManager.weeklyDetails.value?.let {
            billingManager.launchPurchase(activity, it)
        }
    }
    
    fun purchaseMonthly(activity: Activity) {
        billingManager.monthlyDetails.value?.let {
            billingManager.launchPurchase(activity, it)
        }
    }
    
    fun clearMessage() {
        billingManager.clearMessage()
    }
    
    override fun onCleared() {
        super.onCleared()
        billingManager.destroy()
    }
}
