package org.moneymanager.signals

class FakePurchaseSignalSource : PurchaseSignalSource {
    private var onPurchaseDetected: (() -> Unit)? = null

    override fun start(onPurchaseDetected: () -> Unit) {
        this.onPurchaseDetected = onPurchaseDetected
    }

    override fun stop() {
        onPurchaseDetected = null
    }

    fun simulatePurchaseSignal() {
        onPurchaseDetected?.invoke()
    }
}
