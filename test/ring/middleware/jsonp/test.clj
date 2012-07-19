(ns ring.middleware.jsonp.test
  (:use [ring.middleware.jsonp]
        [ring.util.response :only (response content-type)]
        [clojure.test]))

(deftest test-json-with-padding
  (let [handler  (constantly (content-type (response "{}") "application/json"))
        response ((wrap-json-with-padding handler) {:params {"callback" "f"}})]
    (is (= (get-in response [:headers "Content-Type"]) "application/javascript"))
    (is (= (:body response) "f({});"))))
