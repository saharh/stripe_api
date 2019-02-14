library flutter_stripe;

import 'dart:async';

import 'package:flutter/services.dart';

import 'ephemeral_key_manager.dart';
import 'model/card.dart';
import 'model/customer.dart';
import 'model/shipping_information.dart';
import 'model/source.dart';
import 'model/token.dart';
import 'stripe_api_handler.dart';

export 'card_number_formatter.dart';
export 'card_utils.dart';
export 'model/card.dart';
export 'model/customer.dart';
export 'model/shipping_information.dart';
export 'model/source.dart';
export 'model/token.dart';

class StripeFlutterPlugin {
  static const MethodChannel _channel = const MethodChannel('stripe_api');

  static Future<void> init(String publishableKey, {String appleMerchantIdentifier}) async {
    await _channel.invokeMethod('init', {"publishableKey": publishableKey, "appleMerchantIdentifier": appleMerchantIdentifier});
    return;
  }

  static Future<Source> createSource(StripeCard card) async {
    final Map<String, dynamic> cardMap = card.toMap();
    Map<dynamic, dynamic> sourceMap = await _channel.invokeMethod('createSource', cardMap);
    Source source = Source.fromJson(sourceMap);
    return source;
  }

  static Future<bool> isGooglePayAvailable() async {
    return await _channel.invokeMethod('isGooglePayAvailable');
  }

  static Future<bool> isApplePayAvailable() async {
    return await _channel.invokeMethod('isApplePayAvailable');
  }

  static Future<Map> cardFromGooglePay() async {
    Map<dynamic, dynamic> map = await _channel.invokeMethod('cardFromGooglePay');
    if (map == null) {
      return null;
    }
    var cardMap = map["card"];
    map["card"] = StripeCard.fromJson(cardMap);
    return map;
  }
  static Future<Map> cardFromApplePay() async {
    Map<dynamic, dynamic> map = await _channel.invokeMethod('cardFromApplePay');
    if (map == null) {
      return null;
    }
    var cardMap = map["card"];
    map["card"] = StripeCard.fromJson(cardMap);
    return map;
  }
}

class Stripe {
  static Stripe _instance;

  final StripeApiHandler _apiHandler = new StripeApiHandler();

  final String publishableKey;
  String stripeAccount;

  Stripe._internal(this.publishableKey);

  static void init(String publishableKey, {String appleMerchantIdentifier}) async {
    if (_instance == null) {
      _validateKey(publishableKey);
      _instance = new Stripe._internal(publishableKey);
      await _instance.initStripe(appleMerchantIdentifier: appleMerchantIdentifier);
    }
  }

  static Stripe get instance {
    if (_instance == null) {
      throw new Exception("Attempted to get instance of Stripe without initialization");
    }
    return _instance;
  }

  Future<Source> createCardSource(StripeCard card) async {
    return await StripeFlutterPlugin.createSource(card);
  }

  Future<Token> createCardToken(StripeCard card) async {
    final cardMap = card.toMap();
    final token = await _apiHandler.createToken(<String, dynamic>{Token.TYPE_CARD: cardMap}, publishableKey);
    return token;
  }

  Future<bool> isGooglePayAvailable() async {
    return await StripeFlutterPlugin.isGooglePayAvailable();
  }

  Future<bool> isApplePayAvailable() async {
    return await StripeFlutterPlugin.isApplePayAvailable();
  }

  Future<Map> cardFromGooglePay() async {
    return await StripeFlutterPlugin.cardFromGooglePay();
  }

  Future<Map> cardFromApplePay() async {
    return await StripeFlutterPlugin.cardFromApplePay();
  }

  Future<Token> createBankAccountToken(StripeCard card) async {
    return null;
  }

  static void _validateKey(String publishableKey) {
    if (publishableKey == null || publishableKey.isEmpty) {
      throw new Exception("Invalid Publishable Key: " +
          "You must use a valid publishable key to create a token.  " +
          "For more info, see https://stripe.com/docs/stripe.js.");
    }

    if (publishableKey.startsWith("sk_")) {
      throw new Exception("Invalid Publishable Key: " +
          "You are using a secret key to create a token, " +
          "instead of the publishable one. For more info, " +
          "see https://stripe.com/docs/stripe.js");
    }
  }

  Future initStripe({String appleMerchantIdentifier}) async {
    StripeFlutterPlugin.init(this.publishableKey, appleMerchantIdentifier: appleMerchantIdentifier);
  }
}

class CustomerSession {
  static final int KEY_REFRESH_BUFFER_IN_SECONDS = 30;

  static CustomerSession _instance;

  final StripeApiHandler _apiHandler = new StripeApiHandler();

  final EphemeralKeyManager _keyManager;

  ///
  CustomerSession._internal(this._keyManager);

  ///
  ///
  ///
  static void initCustomerSession(EphemeralKeyProvider provider) {
    if (_instance == null) {
      final manager = new EphemeralKeyManager(provider, KEY_REFRESH_BUFFER_IN_SECONDS);
      _instance = new CustomerSession._internal(manager);
    }
  }

  ///
  ///
  ///
  static void endCustomerSession() {
    _instance = null;
  }

  ///
  ///
  ///
  static CustomerSession get instance {
    if (_instance == null) {
      throw new Exception("Attempted to get instance of CustomerSession without initialization.");
    }
    return _instance;
  }

  ///
  ///
  ///
  Future<Customer> retrieveCurrentCustomer() async {
    final key = await _keyManager.retrieveEphemeralKey();
    final customer = await _apiHandler.retrieveCustomer(key.customerId, key.secret);
    return customer;
  }

  ///
  ///
  ///
  Future<Source> addCustomerSource(String sourceId) async {
    final key = await _keyManager.retrieveEphemeralKey();
    final source = await _apiHandler.addCustomerSource(key.customerId, sourceId, key.secret);
    return source;
  }

  ///
  ///
  ///
  Future<bool> deleteCustomerSource(String sourceId) async {
    final key = await _keyManager.retrieveEphemeralKey();
    final deleted = await _apiHandler.deleteCustomerSource(key.customerId, sourceId, key.secret);
    return deleted;
  }

  ///
  ///
  ///
  Future<Customer> updateCustomerDefaultSource(String sourceId) async {
    final key = await _keyManager.retrieveEphemeralKey();
    final customer = await _apiHandler.updateCustomerDefaultSource(key.customerId, sourceId, key.secret);
    return customer;
  }

  ///
  ///
  ///
  Future<Customer> updateCustomerShippingInformation(ShippingInformation shippingInfo) async {
    final key = await _keyManager.retrieveEphemeralKey();
    final customer = await _apiHandler.updateCustomerShippingInformation(key.customerId, shippingInfo, key.secret);
    return customer;
  }
}
