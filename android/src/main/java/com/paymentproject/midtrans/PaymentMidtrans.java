package com.paymentproject.midtrans;

import android.app.Activity;
import android.widget.Toast;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableNativeMap;

import com.midtrans.sdk.corekit.callback.TransactionFinishedCallback;
import com.midtrans.sdk.corekit.core.Constants;
import com.midtrans.sdk.corekit.core.LocalDataHandler;
import com.midtrans.sdk.corekit.core.MidtransSDK;
import com.midtrans.sdk.corekit.core.SdkCoreFlowBuilder;
import com.midtrans.sdk.corekit.core.TransactionRequest;
import com.midtrans.sdk.corekit.core.UIKitCustomSetting;
import com.midtrans.sdk.corekit.core.themes.CustomColorTheme;
import com.midtrans.sdk.corekit.models.BankType;
import com.midtrans.sdk.corekit.models.UserAddress;
import com.midtrans.sdk.corekit.models.UserDetail;
import com.midtrans.sdk.corekit.models.snap.CreditCard;
import com.midtrans.sdk.corekit.models.ItemDetails;
import com.midtrans.sdk.corekit.models.snap.TransactionResult;
import com.midtrans.sdk.uikit.SdkUIFlowBuilder;

import static com.midtrans.sdk.corekit.core.BaseSdkBuilder.CORE_FLOW;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class PaymentMidtrans extends ReactContextBaseJavaModule {

    private static final int CORE_FLOW = 1;
    private static final int UI_FLOW = 2;
    private int mysdkFlow = UI_FLOW;
    private ReactApplicationContext reactContext;
    private String DEFAULT_TEXT = "open_sans_regular.ttf";
    private String SEMI_BOLD_TEXT = "open_sans_semibold.ttf";
    private String BOLD_TEXT = "open_sans_bold.ttf";

    public PaymentMidtrans(ReactApplicationContext reactContext){
      super(reactContext);
        this.reactContext = reactContext;
    }


    @Override
    public String getName() {
        return "PaymentMidtrans";
    }

    @Override
    public Map<String, Object> getConstants() {
      final Map<String, Object> constants = new HashMap<>();
        constants.put("card_click_normal", "Normal");
        constants.put("card_click_two_click", "Two Click");
        constants.put("card_click_one_click", "One Click");
      return constants;
    }

    /**
     * Initialize Veritrans SDK using SdkCoreFlowBuilder.
     */
    @ReactMethod
    private void initSDK(String clientkey, String baseUrl) {
        // SDK initiation for coreflow
        if (mysdkFlow == CORE_FLOW) {
            SdkCoreFlowBuilder.init(reactContext, clientkey, baseUrl)
                    .enableLog(true)
                    .buildSDK();
        } else {
            // Init custom settings
//            UIKitCustomSetting uisetting = new UIKitCustomSetting();
//            uisetting.setShowPaymentStatus(true);

            // SDK initiation for UIflow
            SdkUIFlowBuilder.init(reactContext, clientkey, baseUrl, new TransactionFinishedCallback() {
                @Override
                public void onTransactionFinished(TransactionResult result) {
                    // Handle finished transaction here.
                }
            }).enableLog(true)
                    .buildSDK();
        }
    }


    /**
     *
     *  @param optionConect
     *      - clienKey (String) clientKey from MAP (Merchant Admin Portal)
     *      - urlMerchant (String) url merchant to request token
     *
     *   @param transRequest
     *       - transactionId = id of transaction
     *       - totalAmount = total amount must be paid
     *
     *   @param itemDetails ReadableArray, holds information about item purchased by user TransactionRequest takes an array list of item details
     *                      required for Mandiri Bill and BCA klikPay, Optional for other payment
     *
     *   @param creditCardOptions
     *       - saveCard = save card to Snap (true or false)
     *       - saveToken = save card token as 'one click' token (true or false)
     *       - paymentMode = mode payment use credit Card ("Normal", "Two Click", "One Click")
     *       - secure = using 3D secure (true, false)
     *
     *   @param resultCheckOut is callback status transaction
     *
     **/
    @ReactMethod
    public void checkOut(ReadableMap optionConect, ReadableMap transRequest, ReadableArray itemDetails, ReadableMap creditCardOptions, ReadableMap mapUserDetail, ReadableMap optionColorTheme, ReadableMap optionFont, Callback resultCheckOut){
        Object self;

        //setUser Detail
        UserDetail userDetail = new UserDetail();
        userDetail.setUserFullName(mapUserDetail.getString("fullName"));
        userDetail.setEmail(mapUserDetail.getString("email"));
        userDetail.setPhoneNumber(mapUserDetail.getString("phoneNumber"));
        userDetail.setUserId(mapUserDetail.getString("userId"));

        ArrayList<UserAddress> userAddresses = new ArrayList<>();
        UserAddress userAddress = new UserAddress();
        userAddress.setAddress(mapUserDetail.getString("address"));
        userAddress.setCity(mapUserDetail.getString("city"));
        userAddress.setCountry(mapUserDetail.getString("country"));
        userAddress.setZipcode(mapUserDetail.getString("zipCode"));
        userAddress.setAddressType(Constants.ADDRESS_TYPE_BOTH);
        userAddresses.add(userAddress);

        //Custom color Theme
        CustomColorTheme colorTheme = new CustomColorTheme(
          optionColorTheme.getString("primary"),
          optionColorTheme.getString("primaryDark"),
          optionColorTheme.getString("secondary")
        );
        String defaultText = optionFont == null ? optionFont.getString("defaultText") : DEFAULT_TEXT;
        String semiBoldText = optionFont == null ? optionFont.getString("semiBoldText") : SEMI_BOLD_TEXT;
        String boldText = optionFont == null ? optionFont.getString("boldText") : BOLD_TEXT ;
        // SDK initiation for UIflow
        SdkUIFlowBuilder.init(
                reactContext,
                optionConect.getString("clientKey"),
                optionConect.getString("urlMerchant"),
                new TransactionCallback(resultCheckOut)
        ).enableLog(true)
         .setDefaultText(defaultText)
         .setSemiBoldText(semiBoldText)
         .setBoldText(boldText)
         .setColorTheme(colorTheme)
         .buildSDK();

         userDetail.setUserAddresses(userAddresses);
         LocalDataHandler.saveObject("user_details", userDetail);

        TransactionRequest transactionRequest = new TransactionRequest(transRequest.getString("transactionId"), transRequest.getInt("totalAmount"));

        setItemDetail(itemDetails, transactionRequest);

        CreditCard ccOptions = new CreditCard();
        ccOptions.setSaveCard(creditCardOptions.getBoolean("saveCard"));
        ccOptions.setSecure(creditCardOptions.getBoolean("saveToken"));
        //ccOptions.setChannel(CreditCard.MIGS);
        transactionRequest.setCreditCard(ccOptions);
        transactionRequest.setCardPaymentInfo(creditCardOptions.getString("paymentMode"), creditCardOptions.getBoolean("secure"));

        MidtransSDK.getInstance().setTransactionRequest(transactionRequest);
        MidtransSDK.getInstance().startPaymentUiFlow(getCurrentActivity());
    }


    /**
     * required for Mandiri Bill and BCA klikPay, Optional for other payment
     * @param itemDetails ReadableArray, holds information about item purchased by user TransactionRequest takes an array list of item details
     * @param transactionRequest object to request payment
     */
    private void setItemDetail(ReadableArray itemDetails, TransactionRequest transactionRequest){
        ArrayList<ItemDetails> itemDetailsList = new ArrayList<>();
        for(int a=0; a < itemDetails.size(); a++){
            ReadableMap rmItem = itemDetails.getMap(a);
            String id = rmItem.getString("id");
            int price = rmItem.getInt("price");
            int qty = rmItem.getInt("qty");
            String name = rmItem.getString("name");
            itemDetailsList.add(new ItemDetails(id, price, qty, name));
        }
        transactionRequest.setItemDetails(itemDetailsList);
    }
}
