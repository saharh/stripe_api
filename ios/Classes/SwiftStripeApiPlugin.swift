import Flutter
import UIKit
import Stripe

public class SwiftStripeApiPlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    let channel = FlutterMethodChannel(name: "stripe_api", binaryMessenger: registrar.messenger())
    let instance = SwiftStripeApiPlugin()
    registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    if call.method == "init" {
        STPPaymentConfiguration.shared().publishableKey = call.arguments as! String
//        STPPaymentConfiguration.shared().appleMerchantIdentifier = "your apple merchant identifier"
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
            result(self.sourceToDict(source: source!))
        }
        STPAPIClient.shared().createSource(with: sourceParams, completion: sourceCallback)

    } else {
        result(FlutterMethodNotImplemented)
    }
  }
    
    func sourceToDict(source: STPSource) -> [AnyHashable:Any?] {
        let ret = source.allResponseFields
        return ret
    }
}
