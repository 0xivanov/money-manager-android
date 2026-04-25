package org.moneymanager.signals

interface PurchaseSignalSource {
    fun start(onPurchaseDetected: () -> Unit)

    fun stop()
}
