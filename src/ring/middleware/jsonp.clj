(ns ring.middleware.jsonp
  (:use [ring.util.response :only (response content-type)]
        [clojure.test :only (deftest is)]))

(defn- get-param [request param]
  (or (get-in request [:params (keyword param)])
      (get-in request [:params (name    param)])))

(defn- json-content-type? [response]
  (re-matches #"application/json(;.*)?" (get-in response [:headers "Content-Type"] "")))

(defn- pad-json? [callback response]
  (and callback (json-content-type? response)))

(defn- add-padding-to-json [callback response]
  (-> response
      (content-type "application/javascript")
      (update-in [:body] #(str callback "(" % ");"))))

(defn wrap-json-with-padding [handler]
  (fn [request]
    (let [callback (get-param request :callback)
          response (handler request)]
      (if (pad-json? callback response)
          (add-padding-to-json callback response)
          response))))
