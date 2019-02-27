package com.applaudsoft.stripe_api;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.stripe.android.model.Card;
import com.stripe.android.model.Token;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry;

public class GooglePayDelegate implements PluginRegistry.ActivityResultListener {

    private final Activity activity;
    private PaymentsClient paymentsClient;
    private String stripeApiKey;

    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 100;
    private static final List<Integer> GPAY_ALLOWED_CARD_NETWORKS = Arrays.asList(
            WalletConstants.CARD_NETWORK_INTERAC,
            WalletConstants.CARD_NETWORK_JCB,
            WalletConstants.CARD_NETWORK_OTHER,
            WalletConstants.CARD_NETWORK_AMEX,
            WalletConstants.CARD_NETWORK_DISCOVER,
            WalletConstants.CARD_NETWORK_VISA,
            WalletConstants.CARD_NETWORK_MASTERCARD);
    private static final List<Integer> GPAY_ALLOWED_PAY_METHODS = Arrays.asList(WalletConstants.PAYMENT_METHOD_CARD, WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD);
    private MethodChannel.Result pendingResult;


    public GooglePayDelegate(Activity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("tag", "onActivityResult, requestCode: " + requestCode + ", resultCode: " + resultCode + ", data: " + data);
        switch (requestCode) {
            case LOAD_PAYMENT_DATA_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        PaymentData paymentData = PaymentData.getFromIntent(data);
                        // You can get some data on the user's card, such as the brand and last 4 digits
//                        CardInfo info = paymentData.getCardInfo();
                        // You can also pull the user address from the PaymentData object.
//                        UserAddress address = paymentData.getShippingAddress();
                        // This is the raw JSON string version of your Stripe token.
                        String rawToken = paymentData.getPaymentMethodToken().getToken();
                        Token stripeToken = Token.fromString(rawToken);
                        if (stripeToken != null) {
                            // This chargeToken function is a call to your own server, which should then connect to Stripe's API to finish the charge.
                            Card card = stripeToken.getCard();
                            Map<String, Object> resultMap = new HashMap<>();
                            resultMap.put("card", card.toMap());
                            resultMap.put("token", stripeToken.getId());
                            StripeApiPlugin.removeNullAndEmptyParamsIncl(resultMap);
                            sendSuccess(resultMap);
//                            createUpdateBillingSingle(card.getLast4(), card.getBrand(), card.getCountry(), card.getFunding(), stripeToken.getId())
//                                    .doOnSubscribe(disposable -> showLoadingDialog())
//                                    .compose(bindUntilDestroy())
//                                    .observeOn(mainThread())
//                                    .subscribe(resp -> handleUpdateBillingResult(resp, "google_pay"), new BillingErrorHandler(this).reportOnError("update_billing_request", errorParams()));
                        } else {
                            sendSuccess(null);
                        }
                        break;
                    case Activity.RESULT_CANCELED:
                        sendSuccess(null);
                        break;
                    case AutoResolveHelper.RESULT_ERROR:
                        Status status = AutoResolveHelper.getStatusFromIntent(data);
                        sendError("Google Pay returned error: " + (status != null ? status.getStatusMessage() : ""), null, null);
                        // Log the status for debugging. Generally there is no need to show an error to the user as the Google Payment API will do that
                        break;
                    default:
                        sendError("Unexpected onActivityResult result", null, null);
                        break;
                }
                break;
        }
        return true;
    }

    public void setStripeApiKey(String stripeApiKey) {
        this.stripeApiKey = stripeApiKey;
    }

    public void isGooglePayAvailable(final MethodChannel.Result result) {
        IsReadyToPayRequest request = IsReadyToPayRequest.newBuilder()
                .addAllowedPaymentMethods(GPAY_ALLOWED_PAY_METHODS)
                .addAllowedCardNetworks(GPAY_ALLOWED_CARD_NETWORKS)
                .build();
        Task<Boolean> task = getPaymentClient().isReadyToPay(request);
        task.addOnCompleteListener(new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> taskRes) {
                try {
                    Boolean res = taskRes.getResult(ApiException.class);
                    result.success(res == Boolean.TRUE);
                } catch (Exception exception) {
                    result.error(exception.getMessage(), null, null);
                }
            }
        });
    }

    public void cardFromGooglePay(boolean billingAddressRequired, final MethodChannel.Result result) {
        PaymentDataRequest request = createPaymentDataRequest(billingAddressRequired);
        if (request != null) {
            if (pendingResult != null) {
                sendError("Request in progress", null, null);
            }
            pendingResult = result;
            AutoResolveHelper.resolveTask(
                    paymentsClient.loadPaymentData(request),
                    activity,
                    LOAD_PAYMENT_DATA_REQUEST_CODE);
        } else {
            result.error("PaymentDataRequest == null", null, null);
        }
    }


    private PaymentDataRequest createPaymentDataRequest(boolean billingAddressRequired) {
        PaymentDataRequest.Builder request =
                PaymentDataRequest.newBuilder()
                        .setTransactionInfo(createGPayTransactionInfo())
                        .addAllowedPaymentMethods(GPAY_ALLOWED_PAY_METHODS)
                        .setCardRequirements(
                                CardRequirements.newBuilder()
                                        .setBillingAddressRequired(billingAddressRequired)
                                        .setBillingAddressFormat(WalletConstants.BILLING_ADDRESS_FORMAT_MIN)
                                        .addAllowedCardNetworks(GPAY_ALLOWED_CARD_NETWORKS)
                                        .setAllowPrepaidCards(true)
                                        .build())
                        .setPaymentMethodTokenizationParameters(createTokenizationParameters());
        return request.build();
    }

    @NonNull
    private TransactionInfo createGPayTransactionInfo() {
        return TransactionInfo.newBuilder()
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_NOT_CURRENTLY_KNOWN)
//                .setTotalPrice(String.valueOf(amount))
                .setCurrencyCode("USD")
                .build();

//        Price number = null;
//        if (offerData != null) {
//            number = offerData.getPrice().getNumber();
//        } else {
//            Optional<UserResponse> userData = wabiService.lastUserData();
//            if (userData.isPresent() && !userData.get().getPackages().isEmpty()) {
//                number = userData.get().getPackages().get(0).getOfferData().getPrice().getNumber();
//            }
//        }
//        float amount = 4;
//        String currency = "USD";
//        if (number != null) {
//            amount = number.getSum();
//            currency = number.getUnit().toUpperCase(Locale.US);
//        } else { // no offer presented or user has no package
//        amount = 4;
//        currency = "USD";
//        }
//        return TransactionInfo.newBuilder()
//                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
//                .setTotalPrice(String.valueOf(amount))
//                .setCurrencyCode(currency)
//                .build();
    }

    private PaymentMethodTokenizationParameters createTokenizationParameters() {
        return PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                .addParameter("gateway", "stripe")
                .addParameter("stripe:publishableKey", stripeApiKey)
                .addParameter("stripe:version", "2018-11-08")
                .build();
    }

    private PaymentsClient getPaymentClient() {
        if (paymentsClient == null) {
            paymentsClient = Wallet.getPaymentsClient(activity,
                    new Wallet.WalletOptions.Builder()
//                        .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
//                        .setEnvironment(WalletConstants.ENVIRONMENT_PRODUCTION)
                            .setEnvironment(BuildConfig.DEBUG ? WalletConstants.ENVIRONMENT_TEST : WalletConstants.ENVIRONMENT_PRODUCTION)
                            .build());
        }
        return paymentsClient;
    }

    private void sendSuccess(Object o) {
        if (pendingResult != null) {
            pendingResult.success(o);
        }
        pendingResult = null;
    }

    private void sendError(String s, String s1, Object o) {
        if (pendingResult != null) {
            pendingResult.error(s, s1, o);
        }
        pendingResult = null;
    }
}
