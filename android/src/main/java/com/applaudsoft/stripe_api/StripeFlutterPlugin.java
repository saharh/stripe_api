package com.applaudsoft.stripe_api;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.stripe.android.SourceCallback;
import com.stripe.android.Stripe;
import com.stripe.android.model.Card;
import com.stripe.android.model.Source;
import com.stripe.android.model.SourceParams;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * FlutterPlugin
 */
public class StripeFlutterPlugin implements MethodCallHandler {
    private Stripe stripe;
    Registrar registrar;

    public StripeFlutterPlugin(Registrar registrar) {
        this.registrar = registrar;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "stripe_api");
        channel.setMethodCallHandler(new StripeFlutterPlugin(registrar));
    }

    @Override
    public void onMethodCall(MethodCall call, final Result result) {
        if (call.method.equals("init")) {
            String apiKey = call.arguments();
            stripe = new Stripe(registrar.activity(), apiKey);
            Log.d("tag", "initialized Stripe successfully!");
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
            stripe.createSource(SourceParams.createCardParams(card), new SourceCallback() {
                @Override
                public void onError(Exception error) {
                    result.error(error.getMessage(), null, null);
                }

                @Override
                public void onSuccess(Source source) {
                    Map<String, Object> map = source.toMap();
                    removeNullAndEmptyParamsIncl(map);
                    result.success(map);
                }
            });
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
