(ns e85th.commons.tel
  "Telephone related functions"
  (:refer-clojure :exclude [format])
  (:require [clojure.spec.alpha :as s]
            [e85th.commons.ex :as ex]
            [clojure.string :as str])
  (:import [com.google.i18n.phonenumbers PhoneNumberUtil
            PhoneNumberUtil$PhoneNumberFormat PhoneNumberUtil$MatchType
            Phonenumber$PhoneNumber]))

(def phone-nbr-exception :exception.domain/phone-number)

(defn phone-nbr-ex
  [type msg]
  (ex/exception {:domain phone-nbr-exception
                 :type type
                 :msg msg}))

(def ^{:doc "US ISO country code"}
  default-country-code "US")

(def ^{:private true :doc "PhoneNumberUtil instance"}
  phone-nbr-util (PhoneNumberUtil/getInstance))

(def ^{:private true}
  e164-format PhoneNumberUtil$PhoneNumberFormat/E164)

(def ^{:private true}
  national-format PhoneNumberUtil$PhoneNumberFormat/NATIONAL)


(defn phone-number?
  [x]
  (instance? Phonenumber$PhoneNumber x))


(defn coerce-e164
  "Coerces a number to be an E164 like number, by guessing at what
  maybe a valid number without a country code."
  [s]
  (when (str/blank? s)
    (throw (phone-nbr-ex :phone.number/nil "Can't coerce a blank phone number.")))
  (let [s      (str/trim s)
        digits (->> s (filter #(Character/isDigit %)) (apply str))]
    (cond
      (str/starts-with? s "+") (str "+" digits)
      (= 10 (count digits))    (str "+1" digits)
      :else                    (str "+" digits))))

(defn str->phone-number
  "Parse a *valid* phone number otherwise returns nil"
  ([nbr]
   (str->phone-number nbr default-country-code))
  ([nbr iso-country-code]
   (try
     (.parse ^PhoneNumberUtil phone-nbr-util (coerce-e164 nbr) iso-country-code)
     (catch Exception ex
       nil))))

(def ^{:doc "Tests to see if it is an instance of Phonenumber$PhoneNumber"}
  phone-number-instance? (partial instance? Phonenumber$PhoneNumber))

;; PhoneNumber Protocol
(defprotocol IPhoneNumber
  (valid? [this] "Answer if phone is valid.")

  (match?
    [this other]
    [this other iso-country-code]
    "Answers if phone numbers match.")

  (parse
    [this]
    [this iso-country-code]
    "Parses to a Phonenumber$PhoneNumber instance or return nil if parse failed")

  (normalize [this]
    "Normalize a phone number to E164 format.")

  (format
    [this]
    [this phone-nbr-fmt]
    "Format the phone number according to the national convention by default. phone-nbr-fmt is an instance of PhoneNumberUtil$PhoneNumberFormat"))


(extend-protocol IPhoneNumber

  ;; - PhoneNumber
  Phonenumber$PhoneNumber
  (valid? [this]
    (.isValidNumber ^PhoneNumberUtil phone-nbr-util this))

  (parse
    ([this] this)
    ([this iso-country-code] this))

  (match?
   ([this other]
     (match? this other default-country-code))
    ([this other iso-country-code]
     (= PhoneNumberUtil$MatchType/EXACT_MATCH
        (.isNumberMatch ^PhoneNumberUtil phone-nbr-util this ^Phonenumber$PhoneNumber (parse other iso-country-code)))))

  (format
    ([this]
     (format this national-format))
    ([this phone-nbr-fmt]
     (assert (instance? PhoneNumberUtil$PhoneNumberFormat phone-nbr-fmt))
     (.format ^PhoneNumberUtil phone-nbr-util this phone-nbr-fmt)))

  (normalize [this]
    (format this e164-format))

  ;; - String
  String
  (parse
    ([this]
     (parse this default-country-code))
    ([this iso-country-code]
     (str->phone-number this iso-country-code)))
  (match?
    ([this other]
     (match? this other default-country-code))
    ([this other iso-country-code]
     (match? (parse this) (parse other) iso-country-code)))
  (valid? [this]
    (or (some-> this parse valid?) false))
  (format
    ([this]
     (-> this parse format))
    ([this phone-nbr-fmt]
     (-> this parse (format phone-nbr-fmt))))
  (normalize [this]
    (-> this parse normalize))

  ;; - nil
  nil
  (valid? [this] false)
  (parse
    ([this] (parse this default-country-code))
    ([this iso-country-code]
     (throw (phone-nbr-ex :phone.number/nil "Can't parse nil to a phone number."))))
  (match?
    ([this other] false)
    ([this other iso-country-code] false))
  (format
    ([this] (format this nil))
    ([this phone-nbr-fmt]
     (throw (phone-nbr-ex :phone.number/nil "Can't format a nil phone number."))))
  (normalize [this]
    (throw (phone-nbr-ex :phone.number/nil "Can't normalize a nil phone number."))))


(def invalid? (complement valid?))
