package com.paymentproject.midtrans;

import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.midtrans.sdk.corekit.callback.TransactionFinishedCallback;
import com.midtrans.sdk.corekit.models.snap.TransactionResult;

/**
 * Created by sahli on 4/3/17.
 */

public class TransactionCallback implements TransactionFinishedCallback {

    Callback callback;

    TransactionCallback(Callback callback){
        this.callback = callback;
    }

    @Override
    public void onTransactionFinished(TransactionResult transactionResult) {
        if(transactionResult.isTransactionCanceled()){
            Log.d("trans canceled", "Payment Canceled");
        }else{
            Log.d("result", transactionResult.getStatus());
            this.callback.invoke(transactionResult.getStatus());
        }
    }
}
