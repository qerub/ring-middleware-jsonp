(ns ring.middleware.jsonp
  (:use [ring.util.response :only (response content-type)]
        [clojure.test :only (deftest is)]))

(defn- pad-json? [request response]
  (and (or (get-in request [:params :callback])
           (get-in request [:params "callback"]))
       (re-matches #"application/json(;.*)?" (get-in response [:headers "Content-Type"] ""))))

(defn- add-padding-to-json [request response]
  (-> response
      (content-type "application/javascript")
      (update-in [:body] #(str (or (get-in request [:params :callback])
                                   (get-in request [:params "callback"]))
                               "(" % ");"))))

(defn wrap-json-with-padding [handler]
  (fn [request]
    (let [response (handler request)]
      (if (pad-json? request response)
          (add-padding-to-json request response)
          response))))
