package com.applaudsoft.stripe_api;

import android.text.TextUtils;

import com.stripe.android.SourceCallback;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Map;

import androidx.annotation.NonNull;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterPlugin
 */
public class StripeApiPlugin implements MethodCallHandler {
    private Stripe stripe;
    Registrar registrar;
    SourceCallback sourceCallback;
    private GooglePayDelegate gpayDelegate;


    public StripeApiPlugin(Registrar registrar, GooglePayDelegate gpayDelegate) {
        this.registrar = registrar;
        this.gpayDelegate = gpayDelegate;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "stripe_api");
        GooglePayDelegate gpayDelegate = new GooglePayDelegate(registrar.activity());
        registrar.addActivityResultListener(gpayDelegate);
        channel.setMethodCallHandler(new StripeApiPlugin(registrar, gpayDelegate));
    }

    @Override
    public void onMethodCall(MethodCall call, final Result result) {
        if (call.method.equals("init")) {
            String publishableKey = call.argument("publishableKey");
            if (TextUtils.isEmpty(publishableKey)) {
                result.error("Stripe publishableKey cannot be empty", null, null);
                return;
            }
            gpayDelegate.setStripeApiKey(publishableKey);
            stripe = new Stripe(registrar.activity(), publishableKey);
            result.success(null);
        } else if (call.method.equals("createSource")) {
            Map<String, ?> cardMap = call.arguments();
            Card card = new Card(
                    (String) cardMap.get("number"),
                    (Integer) cardMap.get("exp_month"),
                    (Integer) cardMap.get("exp_year"),
                    (String) cardMap.get("cvc"),
                    (String) cardMap.get("name"),
                    (String) cardMap.get("address_line1"),
                    (String) cardMap.get("address_line2"),
                    (String) cardMap.get("address_city"),
                    (String) cardMap.get("address_state"),
                    (String) cardMap.get("address_zip"),
                    (String) cardMap.get("address_country"),
                    null);
            sourceCallback = new SourceCallback() {
                @Override
                public void onError(Exception e) {
                    String message = e.getMessage() != null ? e.getMessage() : e.toString();
                    result.error(message, null, null);
                }

                @Override
                public void onSuccess(Source source) {
                    Map<String, Object> map = source.toMap();
                    removeNullAndEmptyParamsIncl(map);
                    result.success(map);
                }
            };
            stripe.createSource(SourceParams.createCardParams(card), sourceCallback);
        } else if (call.method.equals("isGooglePayAvailable")) { // returns boolean
            gpayDelegate.isGooglePayAvailable(result);
        } else if (call.method.equals("cardFromGooglePay")) { // returns {token: tokenId, card: stripeCard}
            Boolean billingAddressRequired = call.arguments();
            billingAddressRequired = billingAddressRequired != null ? billingAddressRequired : false;
            gpayDelegate.cardFromGooglePay(billingAddressRequired, result);
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
