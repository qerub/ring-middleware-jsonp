(ns ring.middleware.jsonp.test
  (:import (java.io StringBufferInputStream)
           (java.nio.charset Charset))
  (:use [ring.middleware.jsonp]
        [ring.util.response :only (response content-type)]
        [clojure.test]))

(deftest test-json-with-padding
  (are [body]
    (let [default-charset (. (Charset/defaultCharset) name)
          handler  (constantly (content-type (response body) "application/json"))
          response ((wrap-json-with-padding handler) {:params {"callback" "f"}})]
      (is (= (get-in response [:headers "Content-Type"]) (str "application/javascript; charset=" default-charset)))
      (is (= (slurp (:body response)) "f({});")))

    "{}"
    (list "{}")
    (list "{" "}")
    (StringBufferInputStream. "{}")))

(deftest test-json-with-padding-and-defined-charset
  (let [handler  (constantly (content-type (response "{€}") "application/json; charset=utf-8"))
        response ((wrap-json-with-padding handler) {:params {"callback" "f"}})]
    (is (= (get-in response [:headers "Content-Type"]) "application/javascript; charset=utf-8"))
    (is (= (slurp (:body response) :encoding "UTF-8") "f({€});"))))

(deftest test-content-type-check
  (let [json-content-type? #'ring.middleware.jsonp/json-content-type?]
    (is (= true  (json-content-type? "application/json")))
    (is (= false (json-content-type? "application/json!!!")))
    (is (= true  (json-content-type? "application/json; charset=utf-8")))
    (is (= true  (json-content-type? "application/hal+json")))))
