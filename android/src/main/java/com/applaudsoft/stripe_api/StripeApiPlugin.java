package com.applaudsoft.stripe_api;

import android.content.Context;
import android.text.TextUtils;

import com.stripe.android.ApiResultCallback;
import com.stripe.android.Stripe;
import com.stripe.android.StripeError;
import com.stripe.android.exception.InvalidRequestException;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;
import com.stripe.android.model.StripeMapUtil;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Map;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterPlugin
 */
public class StripeApiPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    private Stripe stripe;
    ApiResultCallback<Source> sourceCallback;
    private GooglePayDelegate gpayDelegate;

    private MethodChannel channel;
    private Context appContext;


    @Override
    public void onAttachedToEngine(@NonNull FlutterPlugin.FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "stripe_api");
        channel.setMethodCallHandler(this);
        appContext = flutterPluginBinding.getApplicationContext();
        gpayDelegate = new GooglePayDelegate();
    }


    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        appContext = null;
        gpayDelegate = null;
    }


    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        gpayDelegate.setActivity(binding.getActivity());
        binding.addActivityResultListener(gpayDelegate);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        gpayDelegate.setActivity(null);
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        gpayDelegate.setActivity(null);
    }

    @Override
    public void onMethodCall(MethodCall call, @NotNull final Result result) {
        if (call.method.equals("init")) {
            String publishableKey = call.argument("publishableKey");
            if (TextUtils.isEmpty(publishableKey)) {
                result.error("Stripe publishableKey cannot be empty", null, null);
                return;
            }
            gpayDelegate.setStripeApiKey(publishableKey);
            stripe = new Stripe(appContext, publishableKey);
            result.success(null);
        } else if (call.method.equals("createSourceFromCard")) {
            Map<String, ?> cardMap = call.arguments();
//            Card card = new Card(
//                    (String) cardMap.get("number"),
//                    (Integer) cardMap.get("exp_month"),
//                    (Integer) cardMap.get("exp_year"),
//                    (String) cardMap.get("cvc"),
//                    (String) cardMap.get("name"),
//                    (String) cardMap.get("address_line1"),
//                    (String) cardMap.get("address_line2"),
//                    (String) cardMap.get("address_city"),
//                    (String) cardMap.get("address_state"),
//                    (String) cardMap.get("address_zip"),
//                    (String) cardMap.get("address_country"),
//                    (String) cardMap.get("currency"),
//                    null);
            Card card = new Card.Builder(
                    (String) cardMap.get("number"),
                    (Integer) cardMap.get("exp_month"),
                    (Integer) cardMap.get("exp_year"),
                    (String) cardMap.get("cvc"))
                    .name((String) cardMap.get("name"))
                    .addressLine1((String) cardMap.get("address_line1"))
                    .addressLine2((String) cardMap.get("address_line2"))
                    .addressCity((String) cardMap.get("address_city"))
                    .addressState((String) cardMap.get("address_state"))
                    .addressZip((String) cardMap.get("address_zip"))
                    .addressCountry((String) cardMap.get("address_country"))
                    .currency((String) cardMap.get("currency"))
                    .build();
            sourceCallback = new ApiResultCallback<Source>() {
                public void onSuccess(@NonNull Source source) {
                    Map<String, Object> map = StripeMapUtil.SourceUtil.toMap(source);
                    removeNullAndEmptyParamsIncl(map);
                    result.success(map);
                }

                public void onError(@NonNull Exception error) {
                    if (error instanceof InvalidRequestException) {
                        StripeError stripeError = ((InvalidRequestException) error).getStripeError();
                        if (stripeError != null) {
                            result.error(stripeError.getCode(), stripeError.getMessage(), null);
                            return;
                        }
                    }
                    String message = error.getMessage() != null ? error.getMessage() : error.toString();
                    result.error(null, message, null);
                }
            };

            stripe.createSource(SourceParams.createCardParams(card), sourceCallback);
        } else if (call.method.equals("createSourceFromAliPay")) {
            sourceCallback = new ApiResultCallback<Source>() {
                public void onSuccess(@NonNull Source source) {
                    Map<String, Object> map = StripeMapUtil.SourceUtil.toMap(source);
                    removeNullAndEmptyParamsIncl(map);
                    result.success(map);
                }

                public void onError(@NonNull Exception error) {
                    String message = error.getMessage() != null ? error.getMessage() : error.toString();
                    result.error(message, null, null);
                }
            };

            Map<String, ?> sourceParams = call.arguments();
            stripe.createSource(SourceParams.createAlipayReusableParams(
                    (String) sourceParams.get("currency"),
                    (String) sourceParams.get("name"),
                    (String) sourceParams.get("email"),
                    (String) sourceParams.get("return_url")
            ), sourceCallback);
        } else if (call.method.equals("isGooglePayAvailable")) {
            gpayDelegate.isGooglePayAvailable(result);
        } else if (call.method.equals("isApplePayAvailable")) {
            result.success(false);
        } else if (call.method.equals("cardFromGooglePay")) { // returns {token: tokenId, card: stripeCard}
            Map<String, ?> params = call.arguments();
            Boolean billingAddressRequired = (Boolean) params.get("billing_address_required");
            billingAddressRequired = billingAddressRequired != null ? billingAddressRequired : false;
            Double amount = (Double) params.get("amount");
            amount = amount == null ? 1 : amount;
            gpayDelegate.cardFromGooglePay(billingAddressRequired, amount, result);
        } else {
            result.notImplemented();
        }
    }

    public static void removeNullAndEmptyParamsIncl(@NonNull Map<String, Object> mapToEdit) {
        // Remove all null values; they cause validation errors
        for (String key : new HashSet<>(mapToEdit.keySet())) {
            if (mapToEdit.get(key) == null) {
                mapToEdit.remove(key);
            }
            if (mapToEdit.get(key) == JSONObject.NULL) {
                mapToEdit.remove(key);
            }

            if (mapToEdit.get(key) instanceof CharSequence) {
                CharSequence sequence = (CharSequence) mapToEdit.get(key);
                if (TextUtils.isEmpty(sequence)) {
                    mapToEdit.remove(key);
                }
            }

            if (mapToEdit.get(key) instanceof Map) {
                Map<String, Object> stringObjectMap = (Map<String, Object>) mapToEdit.get(key);
                removeNullAndEmptyParamsIncl(stringObjectMap);
            }
        }
    }
}
