import Flutter
import UIKit
import Stripe

public class SwiftStripeApiPlugin: NSObject, FlutterPlugin, PKPaymentAuthorizationViewControllerDelegate {
    var _pendingResult : FlutterResult?;
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "stripe_api", binaryMessenger: registrar.messenger())
        let instance = SwiftStripeApiPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        if call.method == "init" {
            let args = call.arguments as! [String:Any]
            STPPaymentConfiguration.shared().publishableKey = args["publishableKey"] as! String
            STPPaymentConfiguration.shared().appleMerchantIdentifier = args["appleMerchantIdentifier"] as? String
            result(nil)
        } else if call.method == "createSource" {
            let args = call.arguments as! [String:Any]
            let cardParams = STPCardParams.init()
            cardParams.number = args["number"] as? String;
            cardParams.expMonth = args["exp_month"] as? UInt ?? 0
            cardParams.expYear = args["exp_year"] as? UInt ?? 0
            cardParams.cvc = args["cvc"] as? String
            cardParams.name = args["name"] as? String
            let address = STPAddress.init()
            address.line1 = args["address_line1"] as? String
            address.line2 = args["address_line2"] as? String
            address.city = args["address_city"] as? String
            address.state = args["address_state"] as? String
            address.postalCode = args["address_zip"] as? String
            address.country = args["address_country"] as? String
            cardParams.address = address
            let sourceParams = STPSourceParams.cardParams(withCard:cardParams)
            let sourceCallback = { (source:STPSource?, error: Error?) -> Void in
                if (error != nil) {
                    result(FlutterError(code: error?.localizedDescription ?? "", message: nil, details: nil))
                }
                result(source?.allResponseFields)
            }
            STPAPIClient.shared().createSource(with: sourceParams, completion: sourceCallback)
        } else if call.method == "isGooglePayAvailable" {
            result(false)
        } else if call.method == "isApplePayAvailable" {
            result(Stripe.deviceSupportsApplePay())
        } else if call.method == "cardFromApplePay" {
            let appleMerchantIdentifier = STPPaymentConfiguration.shared().appleMerchantIdentifier!
            let paymentRequest = Stripe.paymentRequest(withMerchantIdentifier: appleMerchantIdentifier, country: "US", currency: "USD")
            paymentRequest.paymentSummaryItems = [
                PKPaymentSummaryItem(label: "Wabi Virtual Number", amount: 4.00)
            ]
            if Stripe.canSubmitPaymentRequest(paymentRequest) {
                let paymentAuthorizationViewController = PKPaymentAuthorizationViewController(paymentRequest: paymentRequest)!
                paymentAuthorizationViewController.delegate = self
                _pendingResult = result
                let vc = UIApplication.shared.delegate!.window!!.rootViewController!
                vc.present(paymentAuthorizationViewController, animated: true, completion: nil)
            } else {
                result(FlutterError(code: "Cannot submit Payment Request", message: nil, details: nil))
            }
        } else {
            result(FlutterMethodNotImplemented)
        }
    }
    
//    func sourceToDict(source: STPSource) -> [AnyHashable:Any?] {
//        let ret = source.allResponseFields
//        return ret
//    }
    
    private func paymentAuthorizationViewController(_ controller: PKPaymentAuthorizationViewController, didAuthorizePayment payment: PKPayment, completion: @escaping (PKPaymentAuthorizationStatus) -> Void) {
        STPAPIClient.shared().createSource(with: payment) { (source: STPSource?, error: Error?) in
            guard let _ = source, error == nil else {
                self._pendingResult?(FlutterError(code: error?.localizedDescription ?? "Unknown Error", message: nil, details: nil))
                return
            }
            completion(.success)
            let result = ["card" : source?.cardDetails?.allResponseFields,
                          "token": source?.stripeID,
                          ] as [String: Any?]
            self._pendingResult?(result)
            self._pendingResult = nil
            //            submitTokenToBackend(token, completion: { (error: Error?) in
            //                if let error = error {
            //                    // Present error to user...
            //
            //                    // Notify payment authorization view controller
            //                    completion(.failure)
            //                }
            //                else {
            //                    // Save payment success
            //                    paymentSucceeded = true
            //
            //                    // Notify payment authorization view controller
            //                    completion(.success)
            //                }
            //            })
        }
    }
    
    public func paymentAuthorizationViewControllerDidFinish(_ controller: PKPaymentAuthorizationViewController) {
        // Dismiss payment authorization view controller
        controller.dismiss(animated: true, completion: nil)
        //        controller.dismiss(animated: true, completion: {
        //            if (paymentSucceeded) {
        //                // Show a receipt page...
        //            }
        //        })
    }
}
