package com.stripe.android.model;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import androidx.annotation.NonNull;

public class StripeMapUtil {
    public static class GooglePayUtil {
        public static Token tokenFromGooglePay(@NonNull JSONObject googlePayPaymentData)
                throws JSONException {
            final JSONObject paymentMethodData = googlePayPaymentData
                    .getJSONObject("paymentMethodData");
            final String paymentToken = paymentMethodData
                    .getJSONObject("tokenizationData")
                    .getString("token");

            return Token.fromJson(new JSONObject(paymentToken));
        }

        public static PaymentMethod.BillingDetails billingDetailsFromGooglePay(@NonNull JSONObject googlePayPaymentData) throws JSONException {
            final JSONObject paymentMethodData = googlePayPaymentData
                    .getJSONObject("paymentMethodData");
            final JSONObject googlePayBillingAddress = paymentMethodData
                    .getJSONObject("info")
                    .optJSONObject("billingAddress");

            final PaymentMethod.BillingDetails billingDetails;
            final String email = googlePayPaymentData.optString("email");
            if (googlePayBillingAddress != null) {
                final Address billingAddress = new Address.Builder()
                        .setLine1(googlePayBillingAddress.optString("address1"))
                        .setLine2(googlePayBillingAddress.optString("address2"))
                        .setCity(googlePayBillingAddress.optString("locality"))
                        .setState(googlePayBillingAddress.optString("administrativeArea"))
                        .setCountry(googlePayBillingAddress.optString("countryCode"))
                        .setPostalCode(googlePayBillingAddress.optString("postalCode"))
                        .build();
                billingDetails = new PaymentMethod.BillingDetails.Builder()
                        .setAddress(billingAddress)
                        .setName(googlePayBillingAddress.optString("name"))
                        .setEmail(email)
                        .setPhone(googlePayBillingAddress.optString("phoneNumber"))
                        .build();
            } else {
                billingDetails = new PaymentMethod.BillingDetails.Builder()
                        .setEmail(email)
                        .build();
            }

            return billingDetails;
        }
    }

    public static class CardUtil {
        private static final String VALUE_CARD = "card";
        private static final String FIELD_OBJECT = "object";
        private static final String FIELD_ADDRESS_CITY = "address_city";
        private static final String FIELD_ADDRESS_COUNTRY = "address_country";
        private static final String FIELD_ADDRESS_LINE1 = "address_line1";
        private static final String FIELD_ADDRESS_LINE1_CHECK = "address_line1_check";
        private static final String FIELD_ADDRESS_LINE2 = "address_line2";
        private static final String FIELD_ADDRESS_STATE = "address_state";
        private static final String FIELD_ADDRESS_ZIP = "address_zip";
        private static final String FIELD_ADDRESS_ZIP_CHECK = "address_zip_check";
        private static final String FIELD_BRAND = "brand";
        private static final String FIELD_COUNTRY = "country";
        private static final String FIELD_CURRENCY = "currency";
        private static final String FIELD_CUSTOMER = "customer";
        private static final String FIELD_CVC_CHECK = "cvc_check";
        private static final String FIELD_EXP_MONTH = "exp_month";
        private static final String FIELD_EXP_YEAR = "exp_year";
        private static final String FIELD_FINGERPRINT = "fingerprint";
        private static final String FIELD_FUNDING = "funding";
        private static final String FIELD_METADATA = "metadata";
        private static final String FIELD_NAME = "name";
        private static final String FIELD_LAST4 = "last4";
        private static final String FIELD_ID = "id";
        private static final String FIELD_TOKENIZATION_METHOD = "tokenization_method";

        @NonNull
        public static Map<String, Object> toMap(Card card) {
            final AbstractMap<String, Object> map = new HashMap<>();
            if (card == null) {
                return map;
            }
            map.put(FIELD_NAME, card.getName());
            map.put(FIELD_ADDRESS_CITY, card.getAddressCity());
            map.put(FIELD_ADDRESS_COUNTRY, card.getAddressCountry());
            map.put(FIELD_ADDRESS_LINE1, card.getAddressLine1());
            map.put(FIELD_ADDRESS_LINE1_CHECK, card.getAddressLine1Check());
            map.put(FIELD_ADDRESS_LINE2, card.getAddressLine2());
            map.put(FIELD_ADDRESS_STATE, card.getAddressState());
            map.put(FIELD_ADDRESS_ZIP, card.getAddressZip());
            map.put(FIELD_ADDRESS_ZIP_CHECK, card.getAddressZipCheck());
            map.put(FIELD_BRAND, card.getBrand() != null ? card.getBrand().getDisplayName() : null);
            map.put(FIELD_CURRENCY, card.getCurrency());
            map.put(FIELD_COUNTRY, card.getCountry());
            map.put(FIELD_CUSTOMER, card.getCustomerId());
            map.put(FIELD_CVC_CHECK, card.getCvcCheck());
            map.put(FIELD_EXP_MONTH, card.getExpMonth());
            map.put(FIELD_EXP_YEAR, card.getExpYear());
            map.put(FIELD_FINGERPRINT, card.getFingerprint());
            map.put(FIELD_FUNDING, card.getFunding() != null ? card.getFunding().name().toLowerCase() : null);
            map.put(FIELD_ID, card.getId());
            map.put(FIELD_LAST4, card.getLast4());
            map.put(FIELD_TOKENIZATION_METHOD, card.getTokenizationMethod() != null ? card.getTokenizationMethod().name() : null);
//            map.put(FIELD_METADATA, card.getMetadata());
            map.put(FIELD_OBJECT, VALUE_CARD);
            removeNullAndEmptyParams(map);
            return map;
        }
    }

    public static class SourceUtil {
        private static final String FIELD_ID = "id";
        private static final String FIELD_OBJECT = "object";
        private static final String FIELD_AMOUNT = "amount";
        private static final String FIELD_CLIENT_SECRET = "client_secret";
        private static final String FIELD_CODE_VERIFICATION = "code_verification";
        private static final String FIELD_CREATED = "created";
        private static final String FIELD_CURRENCY = "currency";
        private static final String FIELD_FLOW = "flow";
        private static final String FIELD_LIVEMODE = "livemode";
//        private static final String FIELD_METADATA = "metadata";
        private static final String FIELD_OWNER = "owner";
        private static final String FIELD_RECEIVER = "receiver";
        private static final String FIELD_REDIRECT = "redirect";
        private static final String FIELD_STATUS = "status";
        private static final String FIELD_TYPE = "type";
        private static final String FIELD_USAGE = "usage";

        @NonNull
        public static Map<String, Object> toMap(Source source) {
            final AbstractMap<String, Object> map = new HashMap<>();
            if (source == null) {
                return map;
            }

            map.put(FIELD_ID, source.getId());
            map.put(FIELD_AMOUNT, source.getAmount());
            map.put(FIELD_CLIENT_SECRET, source.getClientSecret());

            if (source.getCodeVerification() != null) {
                map.put(FIELD_CODE_VERIFICATION, SourceCodeVerificationUtil.toMap(source.getCodeVerification()));
            }

            map.put(FIELD_CREATED, source.getCreated());
            map.put(FIELD_CURRENCY, source.getCurrency());
            map.put(FIELD_FLOW, source.getFlow());
            map.put(FIELD_LIVEMODE, source.isLiveMode());
//            map.put(FIELD_METADATA, source.getMetaData());

            if (source.getOwner() != null) {
                map.put(FIELD_OWNER, SourceOwnerUtil.toMap(source.getOwner()));
            }

            if (source.getReceiver() != null) {
                map.put(FIELD_RECEIVER, SourceReceiverUtil.toMap(source.getReceiver()));
            }

            if (source.getRedirect() != null) {
                map.put(FIELD_REDIRECT, SourceRedirectUtil.toMap(source.getRedirect()));
            }

            map.put(source.getTypeRaw(), source.getSourceTypeData());

            map.put(FIELD_STATUS, source.getStatus());
            map.put(FIELD_TYPE, source.getTypeRaw());
            map.put(FIELD_USAGE, source.getUsage());
            removeNullAndEmptyParams(map);
            return map;
        }
    }

    static class SourceCodeVerificationUtil {
        private static final String FIELD_ATTEMPTS_REMAINING = "attempts_remaining";
        private static final String FIELD_STATUS = "status";

        public static Map<String, Object> toMap(SourceCodeVerification sourceCodeVerification) {
            if (sourceCodeVerification == null) {
                return null;
            }
            final Map<String, Object> map = new HashMap<>();
            map.put(FIELD_ATTEMPTS_REMAINING, sourceCodeVerification.getAttemptsRemaining());
            if (sourceCodeVerification.getStatus() != null) {
                map.put(FIELD_STATUS, sourceCodeVerification.getStatus());
            }
            return map;
        }

    }

    static class SourceOwnerUtil {
        private static final String VERIFIED = "verified_";
        private static final String FIELD_ADDRESS = "address";
        private static final String FIELD_EMAIL = "email";
        private static final String FIELD_NAME = "name";
        private static final String FIELD_PHONE = "phone";
        private static final String FIELD_VERIFIED_ADDRESS = VERIFIED + FIELD_ADDRESS;
        private static final String FIELD_VERIFIED_EMAIL = VERIFIED + FIELD_EMAIL;
        private static final String FIELD_VERIFIED_NAME = VERIFIED + FIELD_NAME;
        private static final String FIELD_VERIFIED_PHONE = VERIFIED + FIELD_PHONE;

        @NonNull
        public static Map<String, Object> toMap(SourceOwner sourceOwner) {
            final AbstractMap<String, Object> map = new HashMap<>();
            if (sourceOwner == null) {
                return map;
            }

            if (sourceOwner.getAddress() != null) {
                map.put(FIELD_ADDRESS, AddressUtil.toMap(sourceOwner.getAddress()));
            }
            map.put(FIELD_EMAIL, sourceOwner.getEmail());
            map.put(FIELD_NAME, sourceOwner.getName());
            map.put(FIELD_PHONE, sourceOwner.getPhone());
            if (sourceOwner.getVerifiedAddress() != null) {
                map.put(FIELD_VERIFIED_ADDRESS, AddressUtil.toMap(sourceOwner.getVerifiedAddress()));
            }
            map.put(FIELD_VERIFIED_EMAIL, sourceOwner.getVerifiedEmail());
            map.put(FIELD_VERIFIED_NAME, sourceOwner.getVerifiedName());
            map.put(FIELD_VERIFIED_PHONE, sourceOwner.getVerifiedPhone());
            removeNullAndEmptyParams(map);
            return map;
        }
    }

    static class AddressUtil {
        private static final String FIELD_CITY = "city";
        /* 2 Character Country Code */
        private static final String FIELD_COUNTRY = "country";
        private static final String FIELD_LINE_1 = "line1";
        private static final String FIELD_LINE_2 = "line2";
        private static final String FIELD_POSTAL_CODE = "postal_code";
        private static final String FIELD_STATE = "state";

        @NonNull
        public static Map<String, Object> toMap(Address address) {
            final HashMap<String, Object> map = new HashMap<>();
            if (address == null) {
                return map;
            }
            map.put(FIELD_CITY, address.getCity());
            map.put(FIELD_COUNTRY, address.getCountry());
            map.put(FIELD_LINE_1, address.getLine1());
            map.put(FIELD_LINE_2, address.getLine2());
            map.put(FIELD_POSTAL_CODE, address.getPostalCode());
            map.put(FIELD_STATE, address.getState());
            return map;
        }
    }

    static class SourceReceiverUtil {
        private static final String FIELD_ADDRESS = "address";
        private static final String FIELD_AMOUNT_CHARGED = "amount_charged";
        private static final String FIELD_AMOUNT_RECEIVED = "amount_received";
        private static final String FIELD_AMOUNT_RETURNED = "amount_returned";

        @NonNull
        public static Map<String, Object> toMap(SourceReceiver sourceReceiver) {
            final HashMap<String, Object> map = new HashMap<>();
            if (sourceReceiver == null) {
                return map;
            }

//            if (!StripeTextUtils.isBlank(mAddress)) {
//                map.put(FIELD_ADDRESS, mAddress);
//            }
            map.put(FIELD_ADDRESS, sourceReceiver.getAddress());
            map.put(FIELD_AMOUNT_CHARGED, sourceReceiver.getAmountCharged());
            map.put(FIELD_AMOUNT_RECEIVED, sourceReceiver.getAmountReceived());
            map.put(FIELD_AMOUNT_RETURNED, sourceReceiver.getAmountReturned());
            return map;
        }
    }

    static class SourceRedirectUtil {
        private static final String FIELD_RETURN_URL = "return_url";
        private static final String FIELD_STATUS = "status";
        private static final String FIELD_URL = "url";

        @NonNull
        public static Map<String, Object> toMap(SourceRedirect sourceRedirect) {
            final AbstractMap<String, Object> map = new HashMap<>();
            if (sourceRedirect == null) {
                return map;
            }
            map.put(FIELD_RETURN_URL, sourceRedirect.getReturnUrl());
            map.put(FIELD_STATUS, sourceRedirect.getStatus());
            map.put(FIELD_URL, sourceRedirect.getUrl());
            removeNullAndEmptyParams(map);
            return map;
        }
    }

    @SuppressWarnings("unchecked")
    public static void removeNullAndEmptyParams(@NonNull Map<String, Object> mapToEdit) {
        // Remove all null values; they cause validation errors
        for (String key : new HashSet<>(mapToEdit.keySet())) {
            if (mapToEdit.get(key) == null) {
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
                removeNullAndEmptyParams(stringObjectMap);
            }
        }
    }
}
