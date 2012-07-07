(ns ring.middleware.jsonp
  (:use [ring.util.response :only (response content-type)]
        [clojure.test :only (deftest is)]))

(defn- pad-json? [request response]
  (and (get-in request [:params "callback"])
       (re-matches #"application/json(;.*)?" (get-in response [:headers "Content-Type"] ""))))

(defn- add-padding-to-json [request response]
  (-> response
      (content-type "application/javascript")
      (update-in [:body] #(str (get-in request [:params "callback"]) "(" % ");"))))

(defn wrap-json-with-padding [handler]
  (fn [request]
    (let [response (handler request)]
      (if (pad-json? request response)
          (add-padding-to-json request response)
          response))))

(deftest test-json-with-padding
  (let [handler  (constantly (content-type (response "{}") "application/json"))
        response ((wrap-json-with-padding handler) {:params {"callback" "f"}})]
    (is (= (get-in response [:headers "Content-Type"]) "application/javascript"))
    (is (= (:body response) "f({});"))))
