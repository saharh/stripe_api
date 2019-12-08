import 'package:stripe_api/model/model_utils.dart';

import 'model/card.dart';
import 'stripe_text_utils.dart';
import 'text_utils.dart';

const int LENGTH_COMMON_CARD = 16;
const int LENGTH_AMERICAN_EXPRESS = 15;
const int LENGTH_DINERS_CLUB = 14;

const int MAX_LENGTH_COMMON = 19;
// Note that AmEx and Diners Club have the same length
// because Diners Club has one more space, but one less digit.
const int MAX_LENGTH_AMEX_DINERS = 17;

class CardUtils {
  /**
   * Checks the input string to see whether or not it is a valid card number, possibly
   * with groupings separated by spaces or hyphens.
   *
   * @param cardNumber a String that may or may not represent a valid card number
   * @return {@code true} if and only if the input value is a valid card number
   */
  static bool isValidCardNumber(String cardNumber) {
    String normalizedNumber = removeSpacesAndHyphens(cardNumber);
    return isValidLuhnNumber(normalizedNumber) && isValidCardLength(normalizedNumber);
  }

  static bool isValidExpMonth(int expMonth) {
    return expMonth != null && expMonth >= 1 && expMonth <= 12;
  }

  static bool isValidExpYear(int expYear, {DateTime now}) {
    now = now ?? DateTime.now();
    return expYear != null && !ModelUtils.hasYearPassed(expYear, now);
  }

  static bool isValidExpiryDate(int expYear, int expMonth, {DateTime now}) {
    now = now ?? DateTime.now();
    if (!isValidExpMonth(expMonth)) {
      return false;
    }
    if (!isValidExpYear(expYear)) {
      return false;
    }
    return !ModelUtils.hasMonthPassed(expYear, expMonth, now);
  }

  static bool isValidCVC(String cvc, String brand) {
    if (isBlank(cvc)) {
      return false;
    }
    String cvcValue = cvc.trim();
    String updatedType = brand;
    bool validLength = (updatedType == null && cvcValue.length >= 3 && cvcValue.length <= 4) ||
        (StripeCard.AMERICAN_EXPRESS == updatedType && cvcValue.length == 4) ||
        cvcValue.length == 3;

    return ModelUtils.isWholePositiveNumber(cvcValue) && validLength;
  }

  static List<int> getExpiryDate(String value) {
    var split = value.split(new RegExp(r'(\/)'));
    return [int.parse(split[0]), int.parse(split[1])];
  }

  /**
   * Checks the input string to see whether or not it is a valid Luhn number.
   *
   * @param cardNumber a String that may or may not represent a valid Luhn number
   * @return {@code true} if and only if the input value is a valid Luhn number
   */
  static bool isValidLuhnNumber(String cardNumber) {
    if (cardNumber == null) {
      return false;
    }

    bool isOdd = true;
    int sum = 0;

    for (int index = cardNumber.length - 1; index >= 0; index--) {
      var c = cardNumber[index];
      if (!isDigit(c)) {
        return false;
      }

      int digitInteger = getNumericValue(c);
      isOdd = !isOdd;

      if (isOdd) {
        digitInteger *= 2;
      }

      if (digitInteger > 9) {
        digitInteger -= 9;
      }

      sum += digitInteger;
    }

    return sum % 10 == 0;
  }

  /**
   * Checks to see whether the input number is of the correct length, given the assumed brand of
   * the card. This function does not perform a Luhn check.
   *
   * @param cardNumber the card number with no spaces or dashes
   * @param cardBrand a {@link CardBrand} used to get the correct size
   * @return {@code true} if the card number is the correct length for the assumed brand
   */
  static bool isValidCardLength(String cardNumber, {String cardBrand}) {
    if (cardBrand == null) {
      cardBrand = getPossibleCardType(cardNumber, shouldNormalize: false);
    }
    if (cardNumber == null || StripeCard.UNKNOWN == cardBrand) {
      return false;
    }

    int length = cardNumber.length;
    switch (cardBrand) {
      case StripeCard.AMERICAN_EXPRESS:
        return length == LENGTH_AMERICAN_EXPRESS;
      case StripeCard.DINERS_CLUB:
        return length == LENGTH_DINERS_CLUB;
      default:
        return length >= LENGTH_COMMON_CARD && length <= MAX_LENGTH_COMMON;
    }
  }

  static String getPossibleCardType(String cardNumber, {bool shouldNormalize = true}) {
    if (isBlank(cardNumber)) {
      return StripeCard.UNKNOWN;
    }

    String spacelessCardNumber = cardNumber;
    if (shouldNormalize) {
      spacelessCardNumber = removeSpacesAndHyphens(cardNumber);
    }

    if (hasAnyPrefix(spacelessCardNumber, StripeCard.PREFIXES_AMERICAN_EXPRESS)) {
      return StripeCard.AMERICAN_EXPRESS;
    } else if (hasAnyPrefix(spacelessCardNumber, StripeCard.PREFIXES_DISCOVER)) {
      return StripeCard.DISCOVER;
    } else if (hasAnyPrefix(spacelessCardNumber, StripeCard.PREFIXES_JCB)) {
      return StripeCard.JCB;
    } else if (hasAnyPrefix(spacelessCardNumber, StripeCard.PREFIXES_DINERS_CLUB)) {
      return StripeCard.DINERS_CLUB;
    } else if (hasAnyPrefix(spacelessCardNumber, StripeCard.PREFIXES_VISA)) {
      return StripeCard.VISA;
    } else if (hasAnyPrefix(spacelessCardNumber, StripeCard.PREFIXES_MASTERCARD)) {
      return StripeCard.MASTERCARD;
    } else if (hasAnyPrefix(spacelessCardNumber, StripeCard.PREFIXES_UNIONPAY)) {
      return StripeCard.UNIONPAY;
    } else {
      return StripeCard.UNKNOWN;
    }
  }

  static int getLengthForBrand(String cardBrand) {
    if (StripeCard.AMERICAN_EXPRESS == cardBrand || StripeCard.DINERS_CLUB == cardBrand) {
      return MAX_LENGTH_AMEX_DINERS;
    } else {
      return MAX_LENGTH_COMMON;
    }
  }

  /**
   * Converts an unchecked String value to a {@link CardBrand} or {@code null}.
   *
   * @param possibleCardType a String that might match a {@link CardBrand} or be empty.
   * @return {@code null} if the input is blank, else the appropriate {@link CardBrand}.
   */
  static String asCardBrand(String possibleCardType) {
    if (possibleCardType == null || possibleCardType.trim().isEmpty) {
      return null;
    }

    if (StripeCard.AMERICAN_EXPRESS == possibleCardType) {
      return StripeCard.AMERICAN_EXPRESS;
    } else if (StripeCard.MASTERCARD == possibleCardType) {
      return StripeCard.MASTERCARD;
    } else if (StripeCard.DINERS_CLUB == possibleCardType) {
      return StripeCard.DINERS_CLUB;
    } else if (StripeCard.DISCOVER == possibleCardType) {
      return StripeCard.DISCOVER;
    } else if (StripeCard.JCB == possibleCardType) {
      return StripeCard.JCB;
    } else if (StripeCard.VISA == possibleCardType) {
      return StripeCard.VISA;
    } else if (StripeCard.UNIONPAY == possibleCardType) {
      return StripeCard.UNIONPAY;
    } else {
      return StripeCard.UNKNOWN;
    }
  }

  /**
   * Converts an unchecked String value to a {@link FundingType} or {@code null}.
   *
   * @param possibleFundingType a String that might match a {@link FundingType} or be empty
   * @return {@code null} if the input is blank, else the appropriate {@link FundingType}
   */
  static String asFundingType(String possibleFundingType) {
    if (possibleFundingType == null || possibleFundingType.trim().isEmpty) {
      return null;
    }

    if (StripeCard.FUNDING_CREDIT == possibleFundingType) {
      return StripeCard.FUNDING_CREDIT;
    } else if (StripeCard.FUNDING_DEBIT == possibleFundingType) {
      return StripeCard.FUNDING_DEBIT;
    } else if (StripeCard.FUNDING_PREPAID == possibleFundingType) {
      return StripeCard.FUNDING_PREPAID;
    } else {
      return StripeCard.FUNDING_UNKNOWN;
    }
  }
}
